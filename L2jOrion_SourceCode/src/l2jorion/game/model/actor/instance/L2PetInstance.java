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
import java.util.concurrent.Future;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.sql.L2PetDataTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.managers.ItemsOnGroundManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2PetData;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.L2World;
import l2jorion.game.model.PcInventory;
import l2jorion.game.model.PetInventory;
import l2jorion.game.model.actor.stat.PetStat;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.PetInventoryUpdate;
import l2jorion.game.network.serverpackets.PetItemList;
import l2jorion.game.network.serverpackets.PetStatusShow;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.StopMove;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.taskmanager.DecayTaskManager;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class L2PetInstance extends L2Summon
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2PetInstance.class);
	
	private int _curFed;
	private final PetInventory _inventory;
	private final int _controlItemId;
	private boolean _respawned;
	private final boolean _mountable;
	private Future<?> _feedTask;
	private int _feedTime;
	protected boolean _feedMode;
	private L2PetData _data;
	private long _expBeforeDeath = 0;
	private int _curWeightPenalty = 0;
	
	private static final int FOOD_ITEM_CONSUME_COUNT = 5;
	
	public final L2PetData getPetData()
	{
		if (_data == null)
		{
			_data = L2PetDataTable.getInstance().getPetData(getTemplate().getNpcId(), getStat().getLevel());
		}
		
		return _data;
	}
	
	public final void setPetData(L2PetData value)
	{
		_data = value;
	}
	
	public void checkFed()
	{
		if (getCurrentFed() >= 0.50 * getMaxFed())
		{
			if (!isRunning())
			{
				setRunning();
			}
		}
		else
		{
			if (isRunning())
			{
				setWalking();
			}
		}
	}
	
	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				// if pet is attacking
				if (isAttackingNow())
				{
					// if its not already on battleFeed mode
					if (!_feedMode)
					{
						startFeed(true); // switching to battle feed
					}
					else
					// if its on battleFeed mode
					if (_feedMode)
					{
						startFeed(false); // normal feed
					}
				}
				
				if (getCurrentFed() > FOOD_ITEM_CONSUME_COUNT)
				{
					// eat
					setCurrentFed(getCurrentFed() - FOOD_ITEM_CONSUME_COUNT);
				}
				else
				{
					// go back to pet control item, or simply said, unsummon it
					setCurrentFed(0);
					stopFeed();
					unSummon(getOwner());
					getOwner().sendMessage("Your pet is too hungry to stay summoned.");
				}
				
				final int foodId = L2PetDataTable.getFoodItemId(getTemplate().npcId);
				if (foodId == 0)
				{
					return;
				}
				
				L2ItemInstance food = null;
				food = getInventory().getItemByItemId(foodId);
				if (food != null)
				{
					if (getCurrentFed() < 0.55 * getMaxFed())
					{
						if (destroyItem("Feed", food.getObjectId(), 1, null, false))
						{
							setCurrentFed(getCurrentFed() + 100);
							if (getOwner() != null)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
								sm.addItemName(foodId);
								getOwner().sendPacket(sm);
							}
						}
					}
				}
				
				checkFed();
				
				broadcastStatusUpdate();
			}
			catch (final Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public synchronized static L2PetInstance spawnPet(final L2NpcTemplate template, final L2PcInstance owner, final L2ItemInstance control)
	{
		if (L2World.getInstance().getPet(owner.getObjectId()) != null)
		{
			return null;
		}
		
		final L2PetInstance pet = restore(control, template, owner);
		
		if (pet != null)
		{
			pet.setTitle(owner.getName());
			L2World.getInstance().addPet(owner.getObjectId(), pet);
		}
		
		return pet;
	}
	
	public L2PetInstance(final int objectId, final L2NpcTemplate template, final L2PcInstance owner, final L2ItemInstance control)
	{
		super(objectId, template, owner);
		super.setStat(new PetStat(this));
		
		_controlItemId = control.getObjectId();
		
		if (template.npcId == 12564)
		{
			getStat().setLevel((byte) getOwner().getLevel());
		}
		else
		{
			getStat().setLevel(template.level);
		}
		
		_inventory = new PetInventory(this);
		
		final int npcId = template.npcId;
		_mountable = L2PetDataTable.isMountable(npcId);
	}
	
	@Override
	public PetStat getStat()
	{
		if (super.getStat() == null || !(super.getStat() instanceof PetStat))
		{
			setStat(new PetStat(this));
		}
		return (PetStat) super.getStat();
	}
	
	@Override
	public double getLevelMod()
	{
		return (100.0 - 11 + getLevel()) / 100.0;
	}
	
	public boolean isRespawned()
	{
		return _respawned;
	}
	
	@Override
	public int getSummonType()
	{
		return 2;
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		final boolean isOwner = player.getObjectId() == getOwner().getObjectId();
		final boolean thisIsTarget = player.getTarget() != null && player.getTarget().getObjectId() == getObjectId();
		
		if (isOwner && thisIsTarget)
		{
			if (isOwner && player != getOwner())
			{
				updateRefOwner(player);
			}
			
			player.sendPacket(new PetStatusShow(this));
		}
		else
		{
			player.setTarget(this);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public int getControlItemId()
	{
		return _controlItemId;
	}
	
	public L2ItemInstance getControlItem()
	{
		return getOwner().getInventory().getItemByObjectId(_controlItemId);
	}
	
	public int getCurrentFed()
	{
		return _curFed;
	}
	
	public void setCurrentFed(final int num)
	{
		_curFed = num > getMaxFed() ? getMaxFed() : num;
	}
	
	@Override
	public void setPkKills(final int pkKills)
	{
		_pkKills = pkKills;
	}
	
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		for (final L2ItemInstance item : getInventory().getItems())
		{
			if (item.getLocation() == L2ItemInstance.ItemLocation.PET_EQUIP && item.getItem().getBodyPart() == L2Item.SLOT_R_HAND)
			{
				return item;
			}
		}
		
		return null;
	}
	
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		final L2ItemInstance weapon = getActiveWeaponInstance();
		
		if (weapon == null)
		{
			return null;
		}
		
		return (L2Weapon) weapon.getItem();
	}
	
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public PetInventory getInventory()
	{
		return _inventory;
	}
	
	@Override
	public boolean destroyItem(final String process, final int objectId, final int count, final L2Object reference, final boolean sendMessage)
	{
		L2ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		getOwner().sendPacket(petIU);
		
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(item.getItemId());
			getOwner().sendPacket(sm);
		}
		
		return true;
	}
	
	@Override
	public boolean destroyItem(final String process, final L2ItemInstance item, final int count, final L2Object reference, final boolean sendMessage)
	{
		
		if (item == null)
		{
			if (sendMessage)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		getOwner().sendPacket(petIU);
		
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(item.getItemId());
			getOwner().sendPacket(sm);
		}
		return true;
	}
	
	@Override
	public boolean destroyItemByItemId(final String process, final int itemId, final int count, final L2Object reference, final boolean sendMessage)
	{
		L2ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			return false;
		}
		
		// Send Pet inventory update packet
		PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		getOwner().sendPacket(petIU);
		
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(itemId);
			getOwner().sendPacket(sm);
		}
		
		return true;
	}
	
	@Override
	public void doPickupItem(final L2Object object)
	{
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
		
		broadcastPacket(sm);
		
		if (!(object instanceof L2ItemInstance))
		{
			LOG.warn("Trying to pickup wrong target." + object);
			getOwner().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		L2ItemInstance target = (L2ItemInstance) object;
		
		if (target.getItemId() > 8599 && target.getItemId() < 8615)
		{
			SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
			smsg.addItemName(target.getItemId());
			getOwner().sendPacket(smsg);
			return;
		}
		
		if (CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
			smsg.addItemName(target.getItemId());
			getOwner().sendPacket(smsg);
			return;
		}
		
		synchronized (target)
		{
			if (!target.isVisible())
			{
				getOwner().sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				final SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target.getItemId());
				getOwner().sendPacket(smsg);
				return;
			}
			
			if (target.getOwnerId() != 0 && target.getOwnerId() != getOwner().getObjectId() && !getOwner().isInLooterParty(target.getOwnerId()))
			{
				getOwner().sendPacket(ActionFailed.STATIC_PACKET);
				
				if (target.getItemId() == 57)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addNumber(target.getCount());
					getOwner().sendPacket(smsg);
					smsg = null;
				}
				else if (target.getCount() > 1)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
					smsg.addItemName(target.getItemId());
					smsg.addNumber(target.getCount());
					getOwner().sendPacket(smsg);
				}
				else
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target.getItemId());
					getOwner().sendPacket(smsg);
				}
				
				return;
			}
			
			if (target.getItemLootShedule() != null && (target.getOwnerId() == getOwner().getObjectId() || getOwner().isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			target.pickupMe(this);
			
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		getInventory().addItem("Pickup", target, getOwner(), this);
		PetItemList iu = new PetItemList(this);
		getOwner().sendPacket(iu);
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		if (getFollowStatus())
		{
			followOwner();
		}
	}
	
	@Override
	public void deleteMe(final L2PcInstance owner)
	{
		super.deleteMe(owner);
		destroyControlItem(owner);
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer, true))
		{
			return false;
		}
		
		stopFeed();
		getOwner().sendPacket(SystemMessageId.MAKE_SURE_YOU_RESSURECT_YOUR_PET_WITHIN_20_MINUTES);
		DecayTaskManager.getInstance().addDecayTask(this, 1200000);
		deathPenalty();
		return true;
	}
	
	@Override
	public void doRevive()
	{
		if (_curFed > getMaxFed() / 10)
		{
			_curFed = getMaxFed() / 10;
		}
		
		getOwner().removeReviving();
		
		super.doRevive();
		
		// stopDecay
		DecayTaskManager.getInstance().cancelDecayTask(this);
		startFeed(false);
	}
	
	@Override
	public void doRevive(final double revivePower)
	{
		restoreExp(revivePower);
		doRevive();
	}
	
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}
	
	public void refreshOverloaded() // TODO find a way to apply effect without adding skill. For now it's deactivated.
	{
		int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			int weightproc = getCurrentLoad() * 1000 / maxLoad;
			int newWeightPenalty;
			
			if (weightproc < 500)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0)
				{
					// addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty), false);
					setIsOverloaded(getCurrentLoad() >= maxLoad);
				}
				else
				{
					// removeSkill(4270, false);
					setIsOverloaded(false);
				}
			}
		}
	}
	
	@Override
	public void broadcastStatusUpdate()
	{
		refreshOverloaded();
		
		super.broadcastStatusUpdate();
	}
	
	public L2ItemInstance transferItem(final String process, final int objectId, final int count, final Inventory target, final L2PcInstance actor, final L2Object reference)
	{
		L2ItemInstance oldItem = getInventory().getItemByObjectId(objectId);
		final L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, actor, reference);
		
		if (newItem == null)
		{
			return null;
		}
		
		PetInventoryUpdate petIU = new PetInventoryUpdate();
		if (oldItem.getCount() > 0 && oldItem != newItem)
		{
			petIU.addModifiedItem(oldItem);
		}
		else
		{
			petIU.addRemovedItem(oldItem);
		}
		
		getOwner().sendPacket(petIU);
		
		if (target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
			InventoryUpdate playerUI = new InventoryUpdate();
			if (newItem.getCount() > count)
			{
				playerUI.addModifiedItem(newItem);
			}
			else
			{
				playerUI.addNewItem(newItem);
			}
			targetPlayer.sendPacket(playerUI);
			
			StatusUpdate playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			petIU = new PetInventoryUpdate();
			if (newItem.getCount() > count)
			{
				petIU.addRemovedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
		}
		return newItem;
	}
	
	@Override
	public void giveAllToOwner()
	{
		try
		{
			Inventory petInventory = getInventory();
			L2ItemInstance[] items = petInventory.getItems();
			petInventory = null;
			for (final L2ItemInstance item : items)
			{
				L2ItemInstance giveit = item;
				if (giveit.getItem().getWeight() * giveit.getCount() + getOwner().getInventory().getTotalWeight() < getOwner().getMaxLoad())
				{
					giveItemToOwner(giveit);
				}
				else
				{
					dropItemHere(giveit);
				}
			}
		}
		catch (final Exception e)
		{
			LOG.error("Give all items error", e);
		}
	}
	
	public void giveItemToOwner(final L2ItemInstance item)
	{
		try
		{
			getInventory().transferItem("PetTransfer", item.getObjectId(), item.getCount(), getOwner().getInventory(), getOwner(), this);
			PetInventoryUpdate petiu = new PetInventoryUpdate();
			ItemList PlayerUI = new ItemList(getOwner(), false);
			petiu.addRemovedItem(item);
			getOwner().sendPacket(petiu);
			getOwner().sendPacket(PlayerUI);
		}
		catch (final Exception e)
		{
			LOG.error("Error while giving item to owner", e);
		}
	}
	
	public void destroyControlItem(final L2PcInstance owner)
	{
		L2World.getInstance().removePet(owner.getObjectId());
		
		// delete from inventory
		try
		{
			L2ItemInstance removedItem = owner.getInventory().destroyItem("PetDestroy", getControlItemId(), 1, getOwner(), this);
			
			InventoryUpdate iu = new InventoryUpdate();
			iu.addRemovedItem(removedItem);
			owner.sendPacket(iu);
			
			StatusUpdate su = new StatusUpdate(owner.getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, owner.getCurrentLoad());
			owner.sendPacket(su);
			
			owner.broadcastUserInfo();
			
			L2World world = L2World.getInstance();
			world.removeObject(removedItem);
		}
		catch (final Exception e)
		{
			LOG.error("Error while destroying control item", e);
		}
		
		// pet control item no longer exists, delete the pet from the db
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
			statement.setInt(1, getControlItemId());
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("could not delete pet", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void dropAllItems()
	{
		try
		{
			L2ItemInstance[] items = getInventory().getItems();
			for (final L2ItemInstance item : items)
			{
				dropItemHere(item);
			}
		}
		catch (final Exception e)
		{
			LOG.error("Pet Drop Error", e);
		}
	}
	
	public void dropItemHere(final L2ItemInstance dropit)
	{
		dropItemHere(dropit, false);
	}
	
	public void dropItemHere(L2ItemInstance dropit, final boolean protect)
	{
		
		dropit = getInventory().dropItem("Drop", dropit.getObjectId(), dropit.getCount(), getOwner(), this);
		
		if (dropit != null)
		{
			
			if (protect)
			{
				dropit.getDropProtection().protect(getOwner());
			}
			
			LOG.debug("Item id to drop: " + dropit.getItemId() + " amount: " + dropit.getCount());
			dropit.dropMe(this, getX(), getY(), getZ() + 100);
		}
	}
	
	@Override
	public boolean isMountable()
	{
		return _mountable;
	}
	
	private static L2PetInstance restore(final L2ItemInstance control, final L2NpcTemplate template, final L2PcInstance owner)
	{
		Connection con = null;
		
		L2PetInstance pet = null;
		
		try
		{
			if (template.type.compareToIgnoreCase("L2BabyPet") == 0)
			{
				pet = new L2BabyPetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
			}
			else
			{
				pet = new L2PetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
			}
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, karma, pkkills, fed FROM pets WHERE item_obj_id=?");
			statement.setInt(1, control.getObjectId());
			ResultSet rset = statement.executeQuery();
			if (!rset.next())
			{
				DatabaseUtils.close(rset);
				DatabaseUtils.close(statement);
				
				CloseUtil.close(con);
				
				return pet;
			}
			
			pet._respawned = true;
			pet.setName(rset.getString("name"));
			
			pet.getStat().setLevel(rset.getByte("level"));
			pet.getStat().setExp(rset.getLong("exp"));
			pet.getStat().setSp(rset.getInt("sp"));
			
			pet.getStatus().setCurrentHp(rset.getDouble("curHp"));
			pet.getStatus().setCurrentMp(rset.getDouble("curMp"));
			pet.getStatus().setCurrentCp(pet.getMaxCp());
			
			// pet.setKarma(rset.getInt("karma"));
			pet.setPkKills(rset.getInt("pkkills"));
			pet.setCurrentFed(rset.getInt("fed"));
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
			
		}
		catch (final Exception e)
		{
			LOG.error("Could not restore pet data", e);
			
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return pet;
	}
	
	@Override
	public void store()
	{
		if (getControlItemId() == 0)
		{
			return;
		}
		
		String req;
		if (!isRespawned())
		{
			req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,karma,pkkills,fed,item_obj_id) " + "VALUES (?,?,?,?,?,?,?,?,?,?)";
		}
		else
		{
			req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,karma=?,pkkills=?,fed=? " + "WHERE item_obj_id = ?";
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(req);
			statement.setString(1, getName());
			statement.setInt(2, getStat().getLevel());
			statement.setDouble(3, getStatus().getCurrentHp());
			statement.setDouble(4, getStatus().getCurrentMp());
			statement.setLong(5, getStat().getExp());
			statement.setInt(6, getStat().getSp());
			statement.setInt(7, getKarma());
			statement.setInt(8, getPkKills());
			statement.setInt(9, getCurrentFed());
			statement.setInt(10, getControlItemId());
			statement.executeUpdate();
			DatabaseUtils.close(statement);
			_respawned = true;
		}
		catch (final Exception e)
		{
			LOG.error("could not store pet data", e);
		}
		finally
		{
			CloseUtil.close(con);
			
		}
		
		L2ItemInstance itemInst = getControlItem();
		if (itemInst != null && itemInst.getEnchantLevel() != getStat().getLevel())
		{
			itemInst.setEnchantLevel(getStat().getLevel());
			itemInst.updateDatabase();
		}
	}
	
	public synchronized void stopFeed()
	{
		if (_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
		}
	}
	
	public synchronized void startFeed(final boolean battleFeed)
	{
		stopFeed();
		if (!isDead())
		{
			if (battleFeed)
			{
				_feedMode = true;
				_feedTime = _data.getPetFeedBattle();
			}
			else
			{
				_feedMode = false;
				_feedTime = _data.getPetFeedNormal();
			}
			// pet feed time must be different than 0. Changing time to bypass divide by 0
			if (_feedTime <= 0)
			{
				_feedTime = 1;
			}
			
			_feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 60000 / _feedTime, 60000 / _feedTime);
		}
	}
	
	@Override
	public synchronized void unSummon(final L2PcInstance owner)
	{
		stopFeed();
		stopHpMpRegeneration();
		super.unSummon(owner);
		
		if (!isDead())
		{
			L2World.getInstance().removePet(owner.getObjectId());
		}
	}
	
	public void restoreExp(final double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			getStat().addExp(Math.round((_expBeforeDeath - getStat().getExp()) * restorePercent / 100));
			_expBeforeDeath = 0;
		}
	}
	
	private void deathPenalty()
	{
		final int lvl = getStat().getLevel();
		final double percentLost = -0.07 * lvl + 6.5;
		
		// Calculate the Experience loss
		final long lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
		
		_expBeforeDeath = getStat().getExp();
		
		// Set the new Experience value of the L2PetInstance
		getStat().addExp(-lostExp);
	}
	
	@Override
	public void addExpAndSp(final long addToExp, final int addToSp)
	{
		if (getNpcId() == 12564)
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.SINEATER_XP_RATE), addToSp);
		}
		else
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.PET_XP_RATE), addToSp);
		}
	}
	
	public boolean sinEater()
	{
		return getNpcId() == 12564;
	}
	
	@Override
	public long getExpForThisLevel()
	{
		return getStat().getExpForLevel(getLevel());
	}
	
	@Override
	public long getExpForNextLevel()
	{
		return getStat().getExpForLevel(getLevel() + 1);
	}
	
	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}
	
	public int getMaxFed()
	{
		return getStat().getMaxFeed();
	}
	
	@Override
	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}
	
	@Override
	public int getCriticalHit(final L2Character target, final L2Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}
	
	@Override
	public int getEvasionRate(final L2Character target)
	{
		return getStat().getEvasionRate(target);
	}
	
	@Override
	public int getRunSpeed()
	{
		return getStat().getRunSpeed();
	}
	
	@Override
	public int getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}
	
	@Override
	public int getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}
	
	@Override
	public int getMAtk(final L2Character target, final L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}
	
	@Override
	public int getMDef(final L2Character target, final L2Skill skill)
	{
		return getStat().getMDef(target, skill);
	}
	
	@Override
	public int getPAtk(final L2Character target)
	{
		return getStat().getPAtk(target);
	}
	
	@Override
	public int getPDef(final L2Character target)
	{
		return getStat().getPDef(target);
	}
	
	@Override
	public final int getSkillLevel(final int skillId)
	{
		if (_skills == null || _skills.get(skillId) == null)
		{
			return -1;
		}
		final int lvl = getLevel();
		return lvl > 70 ? 7 + (lvl - 70) / 5 : lvl / 10;
	}
	
	public void updateRefOwner(final L2PcInstance owner)
	{
		final int oldOwnerId = getOwner().getObjectId();
		
		setOwner(owner);
		L2World.getInstance().removePet(oldOwnerId);
		L2World.getInstance().addPet(oldOwnerId, this);
	}
}
