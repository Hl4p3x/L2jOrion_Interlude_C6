/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.managers.ItemsOnGroundManager;
import l2jorion.game.model.DropProtection;
import l2jorion.game.model.L2Augmentation;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.knownlist.NullKnownList;
import l2jorion.game.model.extender.BaseExtender.EventType;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.DropItem;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.SpawnItem;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.funcs.Func;
import l2jorion.game.templates.L2Armor;
import l2jorion.game.templates.L2EtcItem;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.IllegalPlayerAction;
import l2jorion.game.util.Util;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public final class L2ItemInstance extends L2Object
{
	private static final Logger LOG = Logger.getLogger(L2ItemInstance.class.getName());
	private static final Logger _logItems = Logger.getLogger("item");
	
	private final DropProtection _dropProtection = new DropProtection();
	
	public static enum ItemLocation
	{
		VOID,
		INVENTORY,
		PAPERDOLL,
		WAREHOUSE,
		CLANWH,
		PET,
		PET_EQUIP,
		LEASE,
		FREIGHT
	}
	
	/** ID of the owner. */
	private int _ownerId;
	private int _dropperObjectId = 0;
	
	/** Quantity of the item. */
	private int _count;
	
	/** Initial Quantity of the item. */
	private int _initCount;
	
	/** Time after restore Item count (in Hours). */
	private int _time;
	
	/** Quantity of the item can decrease. */
	private boolean _decrease = false;
	
	/** ID of the item. */
	private final int _itemId;
	
	/** Object L2Item associated to the item. */
	private final L2Item _item;
	
	/** Location of the item : Inventory, PaperDoll, WareHouse. */
	private ItemLocation _loc;
	
	/** Slot where item is stored. */
	private int _locData;
	
	/** Level of enchantment of the item. */
	private int _enchantLevel;
	
	/** Price of the item for selling. */
	private int _priceSell;
	
	/** Price of the item for buying. */
	private int _priceBuy;
	
	/** Wear Item. */
	private boolean _wear;
	
	/** Augmented Item. */
	private L2Augmentation _augmentation = null;
	
	/** Shadow item. */
	private int _mana = -1;
	
	/** The _consuming mana. */
	private boolean _consumingMana = false;
	
	/** The Constant MANA_CONSUMPTION_RATE. */
	private static final int MANA_CONSUMPTION_RATE = 60000;
	
	/** Custom item types (used loto, race tickets). */
	private int _type1;
	
	/** The _type2. */
	private int _type2;
	
	/** The _drop time. */
	private long _dropTime;
	
	/** The Constant CHARGED_NONE. */
	public static final int CHARGED_NONE = 0;
	
	/** The Constant CHARGED_SOULSHOT. */
	public static final int CHARGED_SOULSHOT = 1;
	
	/** The Constant CHARGED_SPIRITSHOT. */
	public static final int CHARGED_SPIRITSHOT = 1;
	
	/** The Constant CHARGED_BLESSED_SOULSHOT. */
	public static final int CHARGED_BLESSED_SOULSHOT = 2;
	
	/** The Constant CHARGED_BLESSED_SPIRITSHOT. */
	public static final int CHARGED_BLESSED_SPIRITSHOT = 2;
	
	/** Item charged with SoulShot (type of SoulShot). */
	private int _chargedSoulshot = CHARGED_NONE;
	
	/** Item charged with SpiritShot (type of SpiritShot). */
	private int _chargedSpiritshot = CHARGED_NONE;
	
	/** The _charged fishtshot. */
	private boolean _chargedFishtshot = false;
	
	/** The _protected. */
	private boolean _protected;
	
	/** The Constant UNCHANGED. */
	public static final int UNCHANGED = 0;
	
	/** The Constant ADDED. */
	public static final int ADDED = 1;
	
	/** The Constant REMOVED. */
	public static final int REMOVED = 3;
	
	/** The Constant MODIFIED. */
	public static final int MODIFIED = 2;
	
	/** The _last change. */
	private int _lastChange = 2; // 1 ??, 2 modified, 3 removed
	
	/** The _exists in db. */
	private boolean _existsInDb; // if a record exists in DB.
	
	/** The _stored in db. */
	private boolean _storedInDb; // if DB data is up-to-date.
	
	/** The item loot shedule. */
	private ScheduledFuture<?> itemLootShedule = null;
	
	/**
	 * Constructor of the L2ItemInstance from the objectId and the itemId.
	 * @param objectId : int designating the ID of the object in the world
	 * @param itemId : int designating the ID of the item
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public L2ItemInstance(final int objectId, final int itemId) throws IllegalArgumentException
	{
		this(objectId, ItemTable.getInstance().getTemplate(itemId));
	}
	
	/**
	 * Constructor of the L2ItemInstance from the objetId and the description of the item given by the L2Item.
	 * @param objectId : int designating the ID of the object in the world
	 * @param item : L2Item containing informations of the item
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public L2ItemInstance(final int objectId, final L2Item item) throws IllegalArgumentException
	{
		super(objectId);
		
		if (item == null)
		{
			throw new IllegalArgumentException();
		}
		
		super.setKnownList(new NullKnownList(this));
		
		_itemId = item.getItemId();
		_item = item;
		_count = 1;
		_loc = ItemLocation.VOID;
		_mana = _item.getDuration();
	}
	
	/**
	 * Sets the ownerID of the item.
	 * @param process : String Identifier of process triggering this action
	 * @param owner_id : int designating the ID of the owner
	 * @param creator : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void setOwnerId(final String process, final int owner_id, final L2PcInstance creator, final L2Object reference)
	{
		final int oldOwner = _ownerId;
		setOwnerId(owner_id);
		
		if (Config.LOG_ITEMS)
		{
			if (isEquipable() || (getItem().getItemId() == 57))
			{
				LogRecord record = new LogRecord(Level.INFO, "SETOWNER:" + process);
				record.setLoggerName("item");
				record.setParameters(new Object[]
				{
					this,
					creator,
					reference
				});
				_logItems.log(record);
			}
		}
		
		fireEvent(EventType.SETOWNER.name, new Object[]
		{
			process,
			oldOwner
		});
	}
	
	/**
	 * Sets the ownerID of the item.
	 * @param owner_id : int designating the ID of the owner
	 */
	public void setOwnerId(final int owner_id)
	{
		if (owner_id == _ownerId)
		{
			return;
		}
		
		_ownerId = owner_id;
		_storedInDb = false;
	}
	
	/**
	 * Returns the ownerID of the item.
	 * @return int : ownerID of the item
	 */
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	/**
	 * Sets the location of the item.
	 * @param loc : ItemLocation (enumeration)
	 */
	public void setLocation(final ItemLocation loc)
	{
		setLocation(loc, 0);
	}
	
	/**
	 * Sets the location of the item.<BR>
	 * <BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * @param loc : ItemLocation (enumeration)
	 * @param loc_data : int designating the slot where the item is stored or the village for freights
	 */
	public void setLocation(final ItemLocation loc, final int loc_data)
	{
		if (loc == _loc && loc_data == _locData)
		{
			return;
		}
		
		_loc = loc;
		_locData = loc_data;
		_storedInDb = false;
	}
	
	/**
	 * Gets the location.
	 * @return the location
	 */
	public ItemLocation getLocation()
	{
		return _loc;
	}
	
	public boolean isPotion()
	{
		return _item.isPotion();
	}
	
	/**
	 * Returns the quantity of item.
	 * @return int
	 */
	public int getCount()
	{
		return _count;
	}
	
	/**
	 * Sets the quantity of the item.<BR>
	 * <BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * @param process : String Identifier of process triggering this action
	 * @param count : int
	 * @param creator : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void changeCount(final String process, final int count, final L2PcInstance creator, final L2Object reference)
	{
		if (count == 0)
		{
			return;
		}
		
		long old = getCount();
		
		if (count > 0 && _count > Integer.MAX_VALUE - count)
		{
			_count = Integer.MAX_VALUE;
		}
		else
		{
			_count += count;
		}
		
		if (_count < 0)
		{
			_count = 0;
		}
		
		_storedInDb = false;
		
		if (Config.LOG_ITEMS)
		{
			if (isEquipable() || (_item.getItemId() == 57))
			{
				LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
				record.setLoggerName("item");
				record.setParameters(new Object[]
				{
					this,
					"PrevCount(" + old + ")",
					creator,
					reference
				});
				_logItems.log(record);
			}
		}
	}
	
	/**
	 * Change count without trace.
	 * @param process the process
	 * @param count the count
	 * @param creator the creator
	 * @param reference the reference
	 */
	public void changeCountWithoutTrace(final String process, final int count, final L2PcInstance creator, final L2Object reference)
	{
		if (count == 0)
		{
			return;
		}
		if (count > 0 && _count > Integer.MAX_VALUE - count)
		{
			_count = Integer.MAX_VALUE;
		}
		else
		{
			_count += count;
		}
		if (_count < 0)
		{
			_count = 0;
		}
		
		_storedInDb = false;
	}
	
	/**
	 * Sets the quantity of the item.<BR>
	 * <BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * @param count : int
	 */
	public void setCount(final int count)
	{
		if (getCount() == count)
		{
			return;
		}
		
		_count = count >= -1 ? count : 0;
		_storedInDb = false;
	}
	
	/**
	 * Returns if item is equipable.
	 * @return boolean
	 */
	public boolean isEquipable()
	{
		return !(_item instanceof L2EtcItem);
	}
	
	/**
	 * Returns if item is equipped.
	 * @return boolean
	 */
	public boolean isEquipped()
	{
		return _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP;
	}
	
	/**
	 * Returns the slot where the item is stored.
	 * @return int
	 */
	public int getEquipSlot()
	{
		if (Config.ASSERT)
		{
			assert _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP || _loc == ItemLocation.FREIGHT;
		}
		
		return _locData;
	}
	
	/**
	 * Returns the characteristics of the item.
	 * @return L2Item
	 */
	public L2Item getItem()
	{
		return _item;
	}
	
	/**
	 * Gets the custom type1.
	 * @return the custom type1
	 */
	public int getCustomType1()
	{
		return _type1;
	}
	
	/**
	 * Gets the custom type2.
	 * @return the custom type2
	 */
	public int getCustomType2()
	{
		return _type2;
	}
	
	/**
	 * Sets the custom type1.
	 * @param newtype the new custom type1
	 */
	public void setCustomType1(final int newtype)
	{
		_type1 = newtype;
	}
	
	/**
	 * Sets the custom type2.
	 * @param newtype the new custom type2
	 */
	public void setCustomType2(final int newtype)
	{
		_type2 = newtype;
	}
	
	/**
	 * Sets the drop time.
	 * @param time the new drop time
	 */
	public void setDropTime(final long time)
	{
		_dropTime = time;
	}
	
	/**
	 * Gets the drop time.
	 * @return the drop time
	 */
	public long getDropTime()
	{
		return _dropTime;
	}
	
	// Cupid's bow
	/**
	 * Checks if is cupid bow.
	 * @return true, if is cupid bow
	 */
	public boolean isCupidBow()
	{
		if (getItemId() == 9140 || getItemId() == 9141)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if is wear.
	 * @return true, if is wear
	 */
	public boolean isWear()
	{
		return _wear;
	}
	
	/**
	 * Sets the wear.
	 * @param newwear the new wear
	 */
	public void setWear(final boolean newwear)
	{
		_wear = newwear;
	}
	
	/**
	 * Returns the type of item.
	 * @return Enum
	 */
	public Enum<?> getItemType()
	{
		return _item.getItemType();
	}
	
	/**
	 * Returns the ID of the item.
	 * @return int
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Returns the quantity of crystals for crystallization.
	 * @return int
	 */
	public final int getCrystalCount()
	{
		return _item.getCrystalCount(_enchantLevel);
	}
	
	/**
	 * Returns the reference price of the item.
	 * @return int
	 */
	public int getReferencePrice()
	{
		return _item.getReferencePrice();
	}
	
	/**
	 * Returns the name of the item.
	 * @return String
	 */
	public String getItemName()
	{
		return _item.getName();
	}
	
	/**
	 * Returns the price of the item for selling.
	 * @return int
	 */
	public int getPriceToSell()
	{
		return isConsumable() ? (int) (_priceSell * Config.RATE_CONSUMABLE_COST) : _priceSell;
	}
	
	/**
	 * Sets the price of the item for selling <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date.
	 * @param price : int designating the price
	 */
	public void setPriceToSell(final int price)
	{
		_priceSell = price;
		_storedInDb = false;
	}
	
	/**
	 * Returns the price of the item for buying.
	 * @return int
	 */
	// public int getPriceToBuy()
	// {
	// return isConsumable() ? (int) (_priceBuy * Config.RATE_CONSUMABLE_COST) : _priceBuy;
	// }
	
	/**
	 * Sets the price of the item for buying <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date.
	 * @param price : int
	 */
	// public void setPriceToBuy(int price)
	// {
	// _priceBuy = price;
	// _storedInDb = false;
	// }
	
	/**
	 * Returns the last change of the item.
	 * @return int
	 */
	public int getLastChange()
	{
		return _lastChange;
	}
	
	/**
	 * Sets the last change of the item.
	 * @param lastChange : int
	 */
	public void setLastChange(final int lastChange)
	{
		_lastChange = lastChange;
	}
	
	/**
	 * Returns if item is stackable.
	 * @return boolean
	 */
	public boolean isStackable()
	{
		return _item.isStackable();
	}
	
	/**
	 * Returns if item is dropable.
	 * @return boolean
	 */
	public boolean isDropable()
	{
		return isAugmented() ? false : _item.isDropable();
	}
	
	/**
	 * Returns if item is destroyable.
	 * @return boolean
	 */
	public boolean isDestroyable()
	{
		return _item.isDestroyable();
	}
	
	/**
	 * Returns if item is tradeable.
	 * @return boolean
	 */
	public boolean isTradeable()
	{
		return isAugmented() ? false : _item.isTradeable();
	}
	
	public boolean isTradeableItem()
	{
		return _item.isTradeable();
	}
	
	/**
	 * Returns if item is consumable.
	 * @return boolean
	 */
	public boolean isConsumable()
	{
		return _item.isConsumable();
	}
	
	/**
	 * Returns if item is available for manipulation.
	 * @param player the player
	 * @param allowAdena the allow adena
	 * @param allowEquipped
	 * @return boolean
	 */
	public boolean isAvailable(final L2PcInstance player, final boolean allowAdena, final boolean allowEquipped)
	{
		return (!isEquipped() || allowEquipped) // Not equipped
			&& (getObjectId() != player.getFakeArmorObjectId()) // Equipped this Fake Armor
			&& getItem().getType2() != L2Item.TYPE2_QUEST && (getItem().getType2() != L2Item.TYPE2_MONEY || getItem().getType1() != L2Item.TYPE1_SHIELD_ARMOR) && (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item
			&& player.getActiveEnchantItem() != this && (allowAdena || getItemId() != 57) && (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId()) && isTradeable();
	}
	
	public boolean isAvailableItemForPackage(final L2PcInstance player, final boolean allowAdena, final boolean allowEquipped)
	{
		return (!isEquipped() || allowEquipped) && getItem().getType2() != L2Item.TYPE2_QUEST && (getItem().getType2() != L2Item.TYPE2_MONEY || getItem().getType1() != L2Item.TYPE1_SHIELD_ARMOR) && (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item
			&& player.getActiveEnchantItem() != this && (allowAdena || getItemId() != 57) && (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId()) && (isTradeable() ? true : Config.UNTRADABLE_FOR_WAREHOUSE);
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		// this causes the validate position handler to do the pickup if the location is reached.
		// mercenary tickets can only be picked up by the castle owner and GMs.
		if ((!player.isGM()) && (_itemId >= 3960 && _itemId <= 4021 && player.isInParty() || _itemId >= 3960 && _itemId <= 3969 && !player.isCastleLord(1) || _itemId >= 3973 && _itemId <= 3982 && !player.isCastleLord(2) || _itemId >= 3986 && _itemId <= 3995 && !player.isCastleLord(3)
			|| _itemId >= 3999 && _itemId <= 4008 && !player.isCastleLord(4) || _itemId >= 4012 && _itemId <= 4021 && !player.isCastleLord(5) || _itemId >= 5205 && _itemId <= 5214 && !player.isCastleLord(6) || _itemId >= 6779 && _itemId <= 6788 && !player.isCastleLord(7)
			|| _itemId >= 7973 && _itemId <= 7982 && !player.isCastleLord(8) || _itemId >= 7918 && _itemId <= 7927 && !player.isCastleLord(9)))
		{
			if (player.isInParty())
			{
				player.sendMessage("You cannot pickup mercenaries while in a party.");
			}
			else
			{
				player.sendMessage("Only the castle lord can pickup mercenaries.");
			}
			
			player.setTarget(this);
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			if (player.getFreight().getItemByObjectId(this.getObjectId()) != null)
			{
				player.setTarget(this);
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
				player.sendPacket(ActionFailed.STATIC_PACKET);
				
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to pickup Freight Items", IllegalPlayerAction.PUNISH_KICK);
			}
			else
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
			}
		}
	}
	
	/**
	 * Returns the level of enchantment of the item.
	 * @return int
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	/**
	 * Sets the level of enchantment of the item.
	 * @param enchantLevel the new enchant level
	 */
	public void setEnchantLevel(final int enchantLevel)
	{
		if (_enchantLevel == enchantLevel)
		{
			return;
		}
		
		_enchantLevel = enchantLevel;
		_storedInDb = false;
	}
	
	/**
	 * Returns the physical defense of the item.
	 * @return int
	 */
	public int getPDef()
	{
		if (_item instanceof L2Armor)
		{
			return ((L2Armor) _item).getPDef();
		}
		return 0;
	}
	
	/**
	 * Returns whether this item is augmented or not.
	 * @return true if augmented
	 */
	public boolean isAugmented()
	{
		return _augmentation == null ? false : true;
	}
	
	/**
	 * Returns the augmentation object for this item.
	 * @return augmentation
	 */
	public L2Augmentation getAugmentation()
	{
		return _augmentation;
	}
	
	/**
	 * Sets a new augmentation.
	 * @param augmentation the augmentation
	 * @return return true if sucessfull
	 */
	public boolean setAugmentation(final L2Augmentation augmentation)
	{
		if (_augmentation != null)
		{
			return false;
		}
		
		_augmentation = augmentation;
		return true;
	}
	
	/**
	 * Remove the augmentation.
	 */
	public void removeAugmentation()
	{
		if (_augmentation == null)
		{
			return;
		}
		_augmentation.deleteAugmentationData();
		_augmentation = null;
	}
	
	/**
	 * Used to decrease mana (mana means life time for shadow items).
	 */
	public class ScheduleConsumeManaTask implements Runnable
	{
		
		/** The _shadow item. */
		private final L2ItemInstance _shadowItem;
		
		/**
		 * Instantiates a new schedule consume mana task.
		 * @param item the item
		 */
		public ScheduleConsumeManaTask(final L2ItemInstance item)
		{
			_shadowItem = item;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				// decrease mana
				if (_shadowItem != null)
				{
					_shadowItem.decreaseMana(true);
				}
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					t.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Returns true if this item is a shadow item Shadow items have a limited life-time.
	 * @return true, if is shadow item
	 */
	public boolean isShadowItem()
	{
		return _mana >= 0;
	}
	
	/**
	 * Sets the mana for this shadow item <b>NOTE</b>: does not send an inventory update packet.
	 * @param mana the new mana
	 */
	public void setMana(final int mana)
	{
		_mana = mana;
	}
	
	/**
	 * Returns the remaining mana of this shadow item.
	 * @return lifeTime
	 */
	public int getMana()
	{
		return _mana;
	}
	
	/**
	 * Decreases the mana of this shadow item, sends a inventory update schedules a new consumption task if non is running optionally one could force a new task.
	 * @param resetConsumingMana the reset consuming mana
	 */
	public void decreaseMana(final boolean resetConsumingMana)
	{
		if (!isShadowItem())
		{
			return;
		}
		
		if (_mana > 0)
		{
			_mana--;
		}
		
		if (_storedInDb)
		{
			_storedInDb = false;
		}
		if (resetConsumingMana)
		{
			_consumingMana = false;
		}
		
		L2PcInstance player = (L2PcInstance) L2World.getInstance().findObject(getOwnerId());
		if (player != null)
		{
			SystemMessage sm;
			switch (_mana)
			{
				case 10:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10);
					sm.addString(getItemName());
					player.sendPacket(sm);
					break;
				case 5:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5);
					sm.addString(getItemName());
					player.sendPacket(sm);
					break;
				case 1:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1);
					sm.addString(getItemName());
					player.sendPacket(sm);
					break;
			}
			
			if (_mana == 0) // The life time has expired
			{
				sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0);
				sm.addString(getItemName());
				player.sendPacket(sm);
				
				// unequip
				if (isEquipped())
				{
					L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getEquipSlot());
					InventoryUpdate iu = new InventoryUpdate();
					
					for (final L2ItemInstance element : unequiped)
					{
						player.checkSSMatch(null, element);
						iu.addModifiedItem(element);
					}
					
					player.sendPacket(iu);
					
					unequiped = null;
					iu = null;
				}
				
				if (getLocation() != ItemLocation.WAREHOUSE)
				{
					// destroy
					player.getInventory().destroyItem("L2ItemInstance", this, player, null);
					
					// send update
					InventoryUpdate iu = new InventoryUpdate();
					iu.addRemovedItem(this);
					player.sendPacket(iu);
					iu = null;
					
					StatusUpdate su = new StatusUpdate(player.getObjectId());
					su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
					player.sendPacket(su);
					su = null;
				}
				else
				{
					player.getWarehouse().destroyItem("L2ItemInstance", this, player, null);
				}
				
				// delete from world
				L2World.getInstance().removeObject(this);
			}
			else
			{
				// Reschedule if still equipped
				if (!_consumingMana && isEquipped())
				{
					scheduleConsumeManaTask();
				}
				
				if (getLocation() != ItemLocation.WAREHOUSE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(this);
					player.sendPacket(iu);
					iu = null;
				}
			}
			
			sm = null;
		}
		
		player = null;
	}
	
	/**
	 * Schedule consume mana task.
	 */
	private void scheduleConsumeManaTask()
	{
		_consumingMana = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleConsumeManaTask(this), MANA_CONSUMPTION_RATE);
	}
	
	/**
	 * Returns false cause item can't be attacked.
	 * @param attacker the attacker
	 * @return boolean false
	 */
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return false;
	}
	
	/**
	 * Returns the type of charge with SoulShot of the item.
	 * @return int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public int getChargedSoulshot()
	{
		return _chargedSoulshot;
	}
	
	/**
	 * Returns the type of charge with SpiritShot of the item.
	 * @return int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public int getChargedSpiritshot()
	{
		return _chargedSpiritshot;
	}
	
	/**
	 * Gets the charged fishshot.
	 * @return the charged fishshot
	 */
	public boolean getChargedFishshot()
	{
		return _chargedFishtshot;
	}
	
	/**
	 * Sets the type of charge with SoulShot of the item.
	 * @param type : int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public void setChargedSoulshot(final int type)
	{
		_chargedSoulshot = type;
	}
	
	/**
	 * Sets the type of charge with SpiritShot of the item.
	 * @param type : int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public void setChargedSpiritshot(final int type)
	{
		_chargedSpiritshot = type;
	}
	
	/**
	 * Sets the charged fishshot.
	 * @param type the new charged fishshot
	 */
	public void setChargedFishshot(final boolean type)
	{
		_chargedFishtshot = type;
	}
	
	/**
	 * This function basically returns a set of functions from L2Item/L2Armor/L2Weapon, but may add additional functions, if this particular item instance is enhanched for a particular player.
	 * @param player : L2Character designating the player
	 * @return Func[]
	 */
	public Func[] getStatFuncs(final L2Character player)
	{
		return getItem().getStatFuncs(this, player);
	}
	
	/**
	 * Updates database.<BR>
	 * <BR>
	 * <U><I>Concept : </I></U><BR>
	 * <B>IF</B> the item exists in database :
	 * <UL>
	 * <LI><B>IF</B> the item has no owner, or has no location, or has a null quantity : remove item from database</LI>
	 * <LI><B>ELSE</B> : update item in database</LI>
	 * </UL>
	 * <B> Otherwise</B> :
	 * <UL>
	 * <LI><B>IF</B> the item hasn't a null quantity, and has a correct location, and has a correct owner : insert item in database</LI>
	 * </UL>
	 */
	public void updateDatabase()
	{
		if (isWear())
		{
			return;
		}
		
		if (_existsInDb)
		{
			if (_ownerId == 0 || _loc == ItemLocation.VOID || _count == 0 && _loc != ItemLocation.LEASE)
			{
				removeFromDb();
			}
			else
			{
				updateInDb();
			}
		}
		else
		{
			if (_count == 0 && _loc != ItemLocation.LEASE)
			{
				return;
			}
			
			if (_loc == ItemLocation.VOID || _ownerId == 0)
			{
				return;
			}
			
			insertIntoDb();
		}
	}
	
	/**
	 * Returns a L2ItemInstance stored in database from its objectID.
	 * @param objectId : int designating the objectID of the item
	 * @return L2ItemInstance
	 */
	public static L2ItemInstance restoreFromDb(final int objectId)
	{
		L2ItemInstance inst = null;
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT owner_id, object_id, item_id, count, enchant_level, loc, loc_data, price_sell, price_buy, custom_type1, custom_type2, mana_left FROM items WHERE object_id = ?");
			statement.setInt(1, objectId);
			ResultSet rs = statement.executeQuery();
			
			if (rs.next())
			{
				final int owner_id = rs.getInt("owner_id");
				final int item_id = rs.getInt("item_id");
				final int count = rs.getInt("count");
				
				ItemLocation loc = ItemLocation.valueOf(rs.getString("loc"));
				
				final int loc_data = rs.getInt("loc_data");
				final int enchant_level = rs.getInt("enchant_level");
				final int custom_type1 = rs.getInt("custom_type1");
				final int custom_type2 = rs.getInt("custom_type2");
				final int price_sell = rs.getInt("price_sell");
				final int price_buy = rs.getInt("price_buy");
				final int manaLeft = rs.getInt("mana_left");
				
				L2Item item = ItemTable.getInstance().getTemplate(item_id);
				
				if (item == null)
				{
					LOG.log(Level.SEVERE, "Item item_id=" + item_id + " not known, object_id=" + objectId);
					rs.close();
					DatabaseUtils.close(statement);
					CloseUtil.close(con);
					return null;
				}
				
				inst = new L2ItemInstance(objectId, item);
				inst._existsInDb = true;
				inst._storedInDb = true;
				inst._ownerId = owner_id;
				inst._count = count;
				inst._enchantLevel = enchant_level;
				inst._type1 = custom_type1;
				inst._type2 = custom_type2;
				inst._loc = loc;
				inst._locData = loc_data;
				inst._priceSell = price_sell;
				inst._priceBuy = price_buy;
				
				// Setup life time for shadow weapons
				inst._mana = manaLeft;
				
				// consume 1 mana
				if (inst._mana > 0 && inst.getLocation() == ItemLocation.PAPERDOLL)
				{
					inst.decreaseMana(false);
				}
				
				// if mana left is 0 delete this item
				if (inst._mana == 0)
				{
					inst.removeFromDb();
					
					rs.close();
					DatabaseUtils.close(statement);
					CloseUtil.close(con);
					
					return null;
				}
				else if (inst._mana > 0 && inst.getLocation() == ItemLocation.PAPERDOLL)
				{
					inst.scheduleConsumeManaTask();
				}
				
				loc = null;
				item = null;
			}
			else
			{
				LOG.log(Level.SEVERE, "Item object_id=" + objectId + " not found");
				
				rs.close();
				DatabaseUtils.close(statement);
				CloseUtil.close(con);
				
				return null;
			}
			
			rs.close();
			DatabaseUtils.close(statement);
			
			// load augmentation
			statement = con.prepareStatement("SELECT attributes,skill,level FROM augmentations WHERE item_id=?");
			statement.setInt(1, objectId);
			rs = statement.executeQuery();
			
			if (rs.next())
			{
				inst._augmentation = new L2Augmentation(inst, rs.getInt("attributes"), rs.getInt("skill"), rs.getInt("level"), false);
			}
			
			rs.close();
			DatabaseUtils.close(statement);
			rs = null;
			statement = null;
		}
		catch (final Exception e)
		{
			LOG.log(Level.SEVERE, "Could not restore item " + objectId + " from DB", e);
		}
		finally
		{
			CloseUtil.close(con);
			
		}
		
		if (inst != null)
		{
			inst.fireEvent(EventType.LOAD.name, new Object[]
			{
				// con
			});
		}
		
		return inst;
	}
	
	/**
	 * Init a dropped L2ItemInstance and add it in the world as a visible object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion</li>
	 * <li>Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion</li>
	 * <li>Add the L2ItemInstance dropped in the world as a <B>visible</B> object</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of L2World </B></FONT><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Drop item</li>
	 * <li>Call Pet</li><BR>
	 * @param dropper the dropper
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 */
	public final void dropMe(L2Character dropper, int x, int y, int z)
	{
		ThreadPoolManager.getInstance().executeTask(new ItemDropTask(this, dropper, x, y, z));
	}
	
	public class ItemDropTask implements Runnable
	{
		private int _x, _y, _z;
		private final L2Character _dropper;
		private final L2ItemInstance _itm;
		
		public ItemDropTask(L2ItemInstance item, L2Character dropper, int x, int y, int z)
		{
			_x = x;
			_y = y;
			_z = z;
			_dropper = dropper;
			_itm = item;
		}
		
		@Override
		public final void run()
		{
			assert _itm.getPosition().getWorldRegion() == null;
			
			if ((Config.GEODATA) && (_dropper != null))
			{
				Location dropDest = GeoData.getInstance().moveCheck(_dropper.getX(), _dropper.getY(), _dropper.getZ(), _x, _y, _z, _dropper.getInstanceId());
				_x = dropDest.getX();
				_y = dropDest.getY();
				_z = dropDest.getZ();
			}
			
			synchronized (_itm)
			{
				// Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion
				_itm.setIsVisible(true);
				_itm.getPosition().setWorldPosition(_x, _y, _z);
				_itm.getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
			}
			
			_itm.getPosition().getWorldRegion().addVisibleObject(_itm);
			_itm.setDropTime(System.currentTimeMillis());
			_itm.setDropperObjectId(_dropper != null ? _dropper.getObjectId() : 0); // Set the dropper Id for the knownlist packets in sendInfo
			
			// Add the L2ItemInstance dropped in the world as a visible object
			L2World.getInstance().addVisibleObject(_itm, _itm.getPosition().getWorldRegion());
			
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().save(_itm);
			}
			
			_itm.setDropperObjectId(0); // Set the dropper Id back to 0 so it no longer shows the drop packet
		}
	}
	
	/**
	 * Update the database with values of the item.
	 */
	private void updateInDb()
	{
		if (Config.ASSERT)
		{
			assert _existsInDb;
		}
		
		if (_wear)
		{
			return;
		}
		
		if (_storedInDb)
		{
			return;
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,price_sell=?,price_buy=?,custom_type1=?,custom_type2=?,mana_left=? " + "WHERE object_id = ?");
			statement.setInt(1, _ownerId);
			statement.setInt(2, getCount());
			statement.setString(3, _loc.name());
			statement.setInt(4, _locData);
			statement.setInt(5, getEnchantLevel());
			statement.setInt(6, _priceSell);
			statement.setInt(7, _priceBuy);
			statement.setInt(8, getCustomType1());
			statement.setInt(9, getCustomType2());
			statement.setInt(10, getMana());
			statement.setInt(11, getObjectId());
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			DatabaseUtils.close(statement);
			
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.log(Level.SEVERE, "Could not update item " + getObjectId() + " in DB: Reason: ");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
			
		}
		
		if (_existsInDb)
		{
			fireEvent(EventType.STORE.name, (Object[]) null);
		}
	}
	
	/**
	 * Insert the item in database.
	 */
	private void insertIntoDb()
	{
		if (_wear)
		{
			return;
		}
		
		if (Config.ASSERT)
		{
			assert !_existsInDb && getObjectId() != 0;
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,price_sell,price_buy,object_id,custom_type1,custom_type2,mana_left) " + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _ownerId);
			statement.setInt(2, _itemId);
			statement.setInt(3, getCount());
			statement.setString(4, _loc.name());
			statement.setInt(5, _locData);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, _priceSell);
			statement.setInt(8, _priceBuy);
			statement.setInt(9, getObjectId());
			statement.setInt(10, _type1);
			statement.setInt(11, _type2);
			statement.setInt(12, getMana());
			
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			DatabaseUtils.close(statement);
		}
		catch (final SQLIntegrityConstraintViolationException e)
		{
			if (Config.DEBUG)
			{
				LOG.log(Level.SEVERE, "ATTENTION: Update Item instead of Insert one, check player with id " + this.getOwnerId() + " actions on item " + this.getObjectId());
			}
			updateInDb();
			
		}
		catch (final SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Delete item from database.
	 */
	private void removeFromDb()
	{
		if (_wear)
		{
			return;
		}
		
		if (Config.ASSERT)
		{
			assert _existsInDb;
		}
		
		// delete augmentation data
		if (isAugmented())
		{
			_augmentation.deleteAugmentationData();
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE object_id=?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			_existsInDb = false;
			_storedInDb = false;
			DatabaseUtils.close(statement);
			
		}
		catch (final Exception e)
		{
			LOG.log(Level.SEVERE, "Could not delete item " + getObjectId() + " in DB:", e);
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (!_existsInDb)
		{
			fireEvent(EventType.DELETE.name, (Object[]) null);
		}
	}
	
	/**
	 * Returns the item in String format.
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "" + _item;
	}
	
	/**
	 * Reset owner timer.
	 */
	public void resetOwnerTimer()
	{
		if (itemLootShedule != null)
		{
			itemLootShedule.cancel(true);
		}
		itemLootShedule = null;
	}
	
	/**
	 * Sets the item loot shedule.
	 * @param sf the new item loot shedule
	 */
	public void setItemLootShedule(final ScheduledFuture<?> sf)
	{
		itemLootShedule = sf;
	}
	
	/**
	 * Gets the item loot shedule.
	 * @return the item loot shedule
	 */
	public ScheduledFuture<?> getItemLootShedule()
	{
		return itemLootShedule;
	}
	
	/**
	 * Sets the protected.
	 * @param is_protected the new protected
	 */
	public void setProtected(final boolean is_protected)
	{
		_protected = is_protected;
	}
	
	/**
	 * Checks if is protected.
	 * @return true, if is protected
	 */
	public boolean isProtected()
	{
		return _protected;
	}
	
	/**
	 * Checks if is night lure.
	 * @return true, if is night lure
	 */
	public boolean isNightLure()
	{
		return _itemId >= 8505 && _itemId <= 8513 || _itemId == 8485;
	}
	
	/**
	 * Sets the count decrease.
	 * @param decrease the new count decrease
	 */
	public void setCountDecrease(final boolean decrease)
	{
		_decrease = decrease;
	}
	
	/**
	 * Gets the count decrease.
	 * @return the count decrease
	 */
	public boolean getCountDecrease()
	{
		return _decrease;
	}
	
	/**
	 * Sets the inits the count.
	 * @param InitCount the new inits the count
	 */
	public void setInitCount(final int InitCount)
	{
		_initCount = InitCount;
	}
	
	/**
	 * Gets the inits the count.
	 * @return the inits the count
	 */
	public int getInitCount()
	{
		return _initCount;
	}
	
	/**
	 * Restore init count.
	 */
	public void restoreInitCount()
	{
		if (_decrease)
		{
			_count = _initCount;
		}
	}
	
	/**
	 * Sets the time.
	 * @param time the new time
	 */
	public void setTime(final int time)
	{
		if (time > 0)
		{
			_time = time;
		}
		else
		{
			_time = 0;
		}
	}
	
	/**
	 * Gets the time.
	 * @return the time
	 */
	public int getTime()
	{
		return _time;
	}
	
	/**
	 * Returns the slot where the item is stored.
	 * @return int
	 */
	public int getLocationSlot()
	{
		if (Config.ASSERT)
		{
			assert _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP || _loc == ItemLocation.FREIGHT || _loc == ItemLocation.INVENTORY;
		}
		
		return _locData;
	}
	
	/**
	 * Gets the drop protection.
	 * @return the drop protection
	 */
	public final DropProtection getDropProtection()
	{
		return _dropProtection;
	}
	
	/**
	 * Checks if is varka ketra ally quest item.
	 * @return true, if is varka ketra ally quest item
	 */
	public boolean isVarkaKetraAllyQuestItem()
	{
		
		if ((this.getItemId() >= 7211 && this.getItemId() <= 7215) || (this.getItemId() >= 7221 && this.getItemId() <= 7225))
		{
			
			return true;
			
		}
		
		return false;
		
	}
	
	public boolean isOlyRestrictedItem()
	{
		return (Config.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId));
	}
	
	public boolean isHeroItem()
	{
		return ((_itemId >= 6611 && _itemId <= 6621) || (_itemId >= 9388 && _itemId <= 9390) || _itemId == 6842);
	}
	
	public boolean isQuestItem()
	{
		return getItem().isQuestItem();
	}
	
	public boolean checkOlympCondition()
	{
		if (isHeroItem() || isOlyRestrictedItem() || isWear() || (!Config.ALT_OLY_AUGMENT_ALLOW && isAugmented()))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Returns true if item is a Weapon/Shield
	 * @return boolean
	 */
	public boolean isWeapon()
	{
		return (_item instanceof L2Weapon);
	}
	
	public void setDropperObjectId(int id)
	{
		_dropperObjectId = id;
	}
	
	public int getSharedReuseGroup()
	{
		// return _item.getSharedReuseGroup();
		return -1;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (_dropperObjectId != 0)
		{
			activeChar.sendPacket(new DropItem(this, _dropperObjectId));
		}
		else
		{
			activeChar.sendPacket(new SpawnItem(this));
		}
	}
	
	public boolean isFakeArmor()
	{
		return _item.isFakeArmor();
	}
	
	public boolean isFakeWeapon()
	{
		return _item.isFakeWeapon();
	}
}
