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
package l2jorion.game.templates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.effects.EffectTemplate;
import l2jorion.game.skills.funcs.Func;
import l2jorion.game.skills.funcs.FuncTemplate;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public abstract class L2Item
{
	private static Map<Integer, String> _Icons = null;
	
	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;
	
	public static final int TYPE2_WEAPON = 0;
	public static final int TYPE2_SHIELD_ARMOR = 1;
	public static final int TYPE2_ACCESSORY = 2;
	public static final int TYPE2_QUEST = 3;
	public static final int TYPE2_MONEY = 4;
	public static final int TYPE2_OTHER = 5;
	public static final int TYPE2_PET_WOLF = 6;
	public static final int TYPE2_PET_HATCHLING = 7;
	public static final int TYPE2_PET_STRIDER = 8;
	public static final int TYPE2_PET_BABY = 9;
	
	public static final int SLOT_NONE = 0x0000;
	public static final int SLOT_UNDERWEAR = 0x0001;
	public static final int SLOT_R_EAR = 0x0002;
	public static final int SLOT_L_EAR = 0x0004;
	public static final int SLOT_NECK = 0x0008;
	public static final int SLOT_R_FINGER = 0x0010;
	public static final int SLOT_L_FINGER = 0x0020;
	public static final int SLOT_HEAD = 0x0040;
	public static final int SLOT_R_HAND = 0x0080;
	public static final int SLOT_L_HAND = 0x0100;
	public static final int SLOT_GLOVES = 0x0200;
	public static final int SLOT_CHEST = 0x0400;
	public static final int SLOT_LEGS = 0x0800;
	public static final int SLOT_FEET = 0x1000;
	public static final int SLOT_BACK = 0x2000;
	public static final int SLOT_LR_HAND = 0x4000;
	public static final int SLOT_FULL_ARMOR = 0x8000;
	public static final int SLOT_HAIR = 0x010000;
	public static final int SLOT_WOLF = 0x020000;
	public static final int SLOT_HATCHLING = 0x100000;
	public static final int SLOT_STRIDER = 0x200000;
	public static final int SLOT_BABYPET = 0x400000;
	public static final int SLOT_FACE = 0x040000;
	public static final int SLOT_DHAIR = 0x080000;
	public static final int SLOT_ALLDRESS = 0x020000;
	
	public static final int CRYSTAL_NONE = 0x00;
	public static final int CRYSTAL_D = 0x01;
	public static final int CRYSTAL_C = 0x02;
	public static final int CRYSTAL_B = 0x03;
	public static final int CRYSTAL_A = 0x04;
	public static final int CRYSTAL_S = 0x05;
	
	private static final int[] crystalItemId =
	{
		0,
		1458,
		1459,
		1460,
		1461,
		1462
	};
	private static final int[] crystalEnchantBonusArmor =
	{
		0,
		11,
		6,
		11,
		19,
		25
	};
	private static final int[] crystalEnchantBonusWeapon =
	{
		0,
		90,
		45,
		67,
		144,
		250
	};
	
	private final int _itemId;
	private final String _name;
	private final int _type1; // needed for item list (inventory)
	private final int _type2; // different lists for armor, weapon, etc
	private final int _weight;
	private final boolean _crystallizable;
	private final boolean _stackable;
	private final int _crystalType; // default to none-grade
	private final int _duration;
	private final int _bodyPart;
	private final int _referencePrice;
	private final int _crystalCount;
	private final boolean _sellable;
	private final boolean _dropable;
	private final boolean _destroyable;
	private final boolean _tradeable;
	
	protected final Enum<?> _type;
	
	protected FuncTemplate[] _funcTemplates;
	protected EffectTemplate[] _effectTemplates;
	protected L2Skill[] _skills;
	
	private static final Func[] _emptyFunctionSet = new Func[0];
	protected static final L2Effect[] _emptyEffectSet = new L2Effect[0];
	
	protected L2Item(final Enum<?> type, final StatsSet set)
	{
		_type = type;
		_itemId = set.getInteger("item_id");
		_name = set.getString("name");
		_type1 = set.getInteger("type1"); // needed for item list (inventory)
		_type2 = set.getInteger("type2"); // different lists for armor, weapon, etc
		_weight = set.getInteger("weight");
		_crystallizable = set.getBool("crystallizable");
		_stackable = set.getBool("stackable", false);
		_crystalType = set.getInteger("crystal_type", CRYSTAL_NONE); // default to none-grade
		_duration = set.getInteger("duration");
		_bodyPart = set.getInteger("bodypart");
		_referencePrice = set.getInteger("price");
		_crystalCount = set.getInteger("crystal_count", 0);
		_sellable = set.getBool("sellable", true);
		_dropable = set.getBool("dropable", true);
		_destroyable = set.getBool("destroyable", true);
		_tradeable = set.getBool("tradeable", true);
	}
	
	public Enum<?> getItemType()
	{
		return _type;
	}
	
	public final int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Returns the ID of the iden
	 * @return int
	 */
	public final int getItemId()
	{
		return _itemId;
	}
	
	public abstract int getItemMask();
	
	public final int getType2()
	{
		return _type2;
	}
	
	public final int getWeight()
	{
		return _weight;
	}
	
	public final boolean isCrystallizable()
	{
		return _crystallizable;
	}
	
	public final int getCrystalType()
	{
		return _crystalType;
	}
	
	public final int getCrystalItemId()
	{
		return crystalItemId[_crystalType];
	}
	
	public final int getItemGrade()
	{
		return getCrystalType();
	}
	
	public final int getCrystalCount()
	{
		return _crystalCount;
	}
	
	public final int getCrystalCount(final int enchantLevel)
	{
		if (enchantLevel > 3)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * (3 * enchantLevel - 6);
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * (2 * enchantLevel - 3);
				default:
					return _crystalCount;
			}
		}
		else if (enchantLevel > 0)
		{
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * enchantLevel;
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * enchantLevel;
				default:
					return _crystalCount;
			}
		}
		else
		{
			return _crystalCount;
		}
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final int getBodyPart()
	{
		return _bodyPart;
	}
	
	public final int getType1()
	{
		return _type1;
	}
	
	public final boolean isStackable()
	{
		return _stackable;
	}
	
	public boolean isConsumable()
	{
		return false;
	}
	
	public final int getReferencePrice()
	{
		return isConsumable() ? (int) (_referencePrice * Config.RATE_CONSUMABLE_COST) : _referencePrice;
	}
	
	public final boolean isSellable()
	{
		return _sellable;
	}
	
	public final boolean isDropable()
	{
		return _dropable;
	}
	
	public final boolean isDestroyable()
	{
		return _destroyable;
	}
	
	/**
	 * Returns if the item can add to trade
	 * @return boolean
	 */
	public final boolean isTradeable()
	{
		return _tradeable;
	}
	
	public boolean isPotion()
	{
		return (getItemType() == L2EtcItemType.POTION);
	}
	
	/**
	 * Returns if item is for hatchling
	 * @return boolean
	 */
	public boolean isForHatchling()
	{
		return _type2 == TYPE2_PET_HATCHLING;
	}
	
	/**
	 * Returns if item is for strider
	 * @return boolean
	 */
	public boolean isForStrider()
	{
		return _type2 == TYPE2_PET_STRIDER;
	}
	
	/**
	 * Returns if item is for wolf
	 * @return boolean
	 */
	public boolean isForWolf()
	{
		return _type2 == TYPE2_PET_WOLF;
	}
	
	/**
	 * Returns if item is for wolf
	 * @return boolean
	 */
	public boolean isForBabyPet()
	{
		return _type2 == TYPE2_PET_BABY;
	}
	
	/**
	 * Returns array of Func objects containing the list of functions used by the item
	 * @param instance : L2ItemInstance pointing out the item
	 * @param player : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	public Func[] getStatFuncs(final L2ItemInstance instance, final L2Character player)
	{
		if (_funcTemplates == null)
		{
			return _emptyFunctionSet;
		}
		final List<Func> funcs = new FastList<>();
		for (final FuncTemplate t : _funcTemplates)
		{
			final Env env = new Env();
			env.player = player;
			env.target = player;
			env.item = instance;
			final Func f = t.getFunc(env, this); // skill is owner
			if (f != null)
			{
				funcs.add(f);
			}
		}
		if (funcs.size() == 0)
		{
			return _emptyFunctionSet;
		}
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	/**
	 * Returns the effects associated with the item.
	 * @param instance : L2ItemInstance pointing out the item
	 * @param player : L2Character pointing out the player
	 * @return L2Effect[] : array of effects generated by the item
	 */
	public L2Effect[] getEffects(final L2ItemInstance instance, final L2Character player)
	{
		if (_effectTemplates == null)
		{
			return _emptyEffectSet;
		}
		final List<L2Effect> effects = new FastList<>();
		for (final EffectTemplate et : _effectTemplates)
		{
			final Env env = new Env();
			env.player = player;
			env.target = player;
			env.item = instance;
			final L2Effect e = et.getEffect(env);
			if (e != null)
			{
				effects.add(e);
			}
		}
		if (effects.size() == 0)
		{
			return _emptyEffectSet;
		}
		return effects.toArray(new L2Effect[effects.size()]);
	}
	
	/**
	 * Returns effects of skills associated with the item.
	 * @param caster : L2Character pointing out the caster
	 * @param target : L2Character pointing out the target
	 * @return L2Effect[] : array of effects generated by the skill
	 */
	public L2Effect[] getSkillEffects(final L2Character caster, final L2Character target)
	{
		if (_skills == null)
		{
			return _emptyEffectSet;
		}
		
		final List<L2Effect> effects = new FastList<>();
		
		for (final L2Skill skill : _skills)
		{
			if (!skill.checkCondition(caster, target, true))
			{
				continue; // Skill condition not met
			}
			
			if (target.getFirstEffect(skill.getId()) != null)
			{
				target.removeEffect(target.getFirstEffect(skill.getId()));
			}
			
			for (final L2Effect e : skill.getEffects(caster, target, false, false, false))
			{
				effects.add(e);
			}
		}
		
		if (effects.size() == 0)
		{
			return _emptyEffectSet;
		}
		return effects.toArray(new L2Effect[effects.size()]);
	}
	
	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * @param f : FuncTemplate to add
	 */
	public void attach(final FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			{
				f
			};
		}
		else
		{
			final int len = _funcTemplates.length;
			final FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}
	
	public void attach(final EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[]
			{
				effect
			};
		}
		else
		{
			final int len = _effectTemplates.length;
			final EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}
	
	public void attach(final L2Skill skill)
	{
		if (_skills == null)
		{
			_skills = new L2Skill[]
			{
				skill
			};
		}
		else
		{
			final int len = _skills.length;
			final L2Skill[] tmp = new L2Skill[len + 1];
			System.arraycopy(_skills, 0, tmp, 0, len);
			tmp[len] = skill;
			_skills = tmp;
		}
	}
	
	@Override
	public String toString()
	{
		return _name;
	}
	
	public boolean isQuestItem()
	{
		return (getItemType() == L2EtcItemType.QUEST);
	}
	
	public static String getItemNameById(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		
		String itemName = "NoName";
		
		if (itemId != 0)
		{
			itemName = item.getName();
		}
		
		return itemName;
	}
	
	public boolean isHeroItem()
	{
		return ((_itemId >= 6611 && _itemId <= 6621) || (_itemId >= 9388 && _itemId <= 9390) || _itemId == 6842);
	}
	
	public static String getItemIcon(int itemId)
	{
		if (_Icons != null && !_Icons.isEmpty())
		{
			return _Icons.get(itemId);
		}
		return null;
	}
	
	public static void LoadAllIcons()
	{
		loadIcons();
	}
	
	private static void loadIcons()
	{
		_Icons = new HashMap<>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM item_icons");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int itemId = rset.getInt("itemId");
				String itemIcon = rset.getString("itemIcon");
				_Icons.put(itemId, itemIcon);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
}
