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

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.funcs.Func;
import l2jorion.game.skills.funcs.FuncTemplate;

public final class L2Armor extends L2Item
{
	private final int _avoidModifier;
	private final int _pDef;
	private final int _mDef;
	private final int _mpBonus;
	private final int _hpBonus;
	private L2Skill _itemSkill = null; // for passive skill
	
	public L2Armor(final L2ArmorType type, final StatsSet set)
	{
		super(type, set);
		_avoidModifier = set.getInteger("avoid_modify");
		_pDef = set.getInteger("p_def");
		_mDef = set.getInteger("m_def");
		_mpBonus = set.getInteger("mp_bonus", 0);
		_hpBonus = set.getInteger("hp_bonus", 0);
		
		final int sId = set.getInteger("item_skill_id");
		final int sLv = set.getInteger("item_skill_lvl");
		if (sId > 0 && sLv > 0)
		{
			_itemSkill = SkillTable.getInstance().getInfo(sId, sLv);
		}
	}
	
	/**
	 * Returns the type of the armor.
	 * @return L2ArmorType
	 */
	@Override
	public L2ArmorType getItemType()
	{
		return (L2ArmorType) super._type;
	}
	
	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}
	
	/**
	 * Returns the magical defense of the armor
	 * @return int : value of the magic defense
	 */
	public final int getMDef()
	{
		return _mDef;
	}
	
	/**
	 * Returns the physical defense of the armor
	 * @return int : value of the physical defense
	 */
	public final int getPDef()
	{
		return _pDef;
	}
	
	/**
	 * Returns avoid modifier given by the armor
	 * @return int : avoid modifier
	 */
	public final int getAvoidModifier()
	{
		return _avoidModifier;
	}
	
	/**
	 * Returns magical bonus given by the armor
	 * @return int : value of the magical bonus
	 */
	public final int getMpBonus()
	{
		return _mpBonus;
	}
	
	/**
	 * Returns physical bonus given by the armor
	 * @return int : value of the physical bonus
	 */
	public final int getHpBonus()
	{
		return _hpBonus;
	}
	
	/**
	 * Returns passive skill linked to that armor
	 * @return
	 */
	public L2Skill getSkill()
	{
		return _itemSkill;
	}
	
	/**
	 * Returns array of Func objects containing the list of functions used by the armor
	 * @param instance : L2ItemInstance pointing out the armor
	 * @param player : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	@Override
	public Func[] getStatFuncs(final L2ItemInstance instance, final L2Character player)
	{
		final List<Func> funcs = new FastList<>();
		if (_funcTemplates != null)
		{
			for (final FuncTemplate t : _funcTemplates)
			{
				final Env env = new Env();
				env.player = player;
				env.item = instance;
				final Func f = t.getFunc(env, instance);
				if (f != null)
				{
					funcs.add(f);
				}
			}
		}
		return funcs.toArray(new Func[funcs.size()]);
	}
}
