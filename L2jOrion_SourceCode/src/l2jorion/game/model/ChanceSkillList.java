/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model;

import java.util.concurrent.ConcurrentHashMap;

import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.handler.SkillHandler;
import l2jorion.game.network.serverpackets.MagicSkillLaunched;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.skills.Formulas;

public class ChanceSkillList extends ConcurrentHashMap<L2Skill, ChanceCondition>
{
	private static final long serialVersionUID = -3523525435531L;
	
	private L2Character _owner;
	
	public ChanceSkillList(final L2Character owner)
	{
		super();
		_owner = owner;
	}
	
	public L2Character getOwner()
	{
		return _owner;
	}
	
	public void setOwner(final L2Character owner)
	{
		_owner = owner;
	}
	
	public void onHit(L2Character target, boolean ownerWasHit, boolean wasCrit)
	{
		int event;
		if (ownerWasHit)
		{
			event = ChanceCondition.EVT_ATTACKED | ChanceCondition.EVT_ATTACKED_HIT;
			if (wasCrit)
			{
				event |= ChanceCondition.EVT_ATTACKED_CRIT;
			}
		}
		else
		{
			event = ChanceCondition.EVT_HIT;
			if (wasCrit)
			{
				event |= ChanceCondition.EVT_CRIT;
			}
		}
		
		onEvent(event, target);
	}
	
	public void onSkillHit(final L2Character target, final boolean ownerWasHit, final boolean wasMagic, final boolean wasOffensive)
	{
		int event;
		if (ownerWasHit)
		{
			event = ChanceCondition.EVT_HIT_BY_SKILL;
			if (wasOffensive)
			{
				event |= ChanceCondition.EVT_HIT_BY_OFFENSIVE_SKILL;
				event |= ChanceCondition.EVT_ATTACKED;
			}
			else
			{
				event |= ChanceCondition.EVT_HIT_BY_GOOD_MAGIC;
			}
		}
		else
		{
			event = ChanceCondition.EVT_CAST;
			event |= wasMagic ? ChanceCondition.EVT_MAGIC : ChanceCondition.EVT_PHYSICAL;
			event |= wasOffensive ? ChanceCondition.EVT_MAGIC_OFFENSIVE : ChanceCondition.EVT_MAGIC_GOOD;
		}
		
		onEvent(event, target);
	}
	
	public static boolean canTriggerByCast(L2Character caster, L2Character target, L2Skill trigger)
	{
		switch (trigger.getSkillType())
		{
			case COMMON_CRAFT:
			case DWARVEN_CRAFT:
				return false;
		}
		
		if (trigger.isToggle() || trigger.isPotion() || !trigger.isMagic())
		{
			return false; // No buffing with toggle skills or potions
		}
		
		if (trigger.getId() == 1320)
		{
			return false; // No buffing with Common
		}
		
		if (trigger.isOffensive() && !Formulas.calcMagicSuccess(caster, target, trigger))
		{
			return false; // Low grade skills won't trigger for high level targets
		}
		
		return true;
	}
	
	public void onEvent(int event, L2Character target)
	{
		for (Entry<L2Skill, ChanceCondition> e : entrySet())
		{
			if (e.getValue() != null && e.getValue().trigger(event))
			{
				makeCast(e.getKey(), target);
			}
		}
	}
	
	private void makeCast(L2Skill skill, L2Character target)
	{
		try
		{
			if (skill.getWeaponDependancy(_owner, true))
			{
				L2Skill castedSkill = skill;
				if (castedSkill.triggerAnotherSkill()) // should we use this skill or this skill is just referring to another one
				{
					castedSkill = _owner._skills.get(castedSkill.getTriggeredId());
					if (castedSkill == null)
					{
						return;
					}
				}
				
				final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(castedSkill.getSkillType());
				final L2Object[] targets = castedSkill.getTargetList(_owner, false, target);
				_owner.broadcastPacket(new MagicSkillLaunched(_owner, castedSkill.getDisplayId(), castedSkill.getLevel(), targets));
				_owner.broadcastPacket(new MagicSkillUser(_owner, (L2Character) targets[0], castedSkill.getDisplayId(), castedSkill.getLevel(), 0, 0));
				
				// Launch the magic skill and calculate its effects
				if (handler != null)
				{
					handler.useSkill(_owner, castedSkill, targets);
				}
				else
				{
					castedSkill.useSkill(_owner, targets);
				}
			}
		}
		catch (final Exception e)
		{
		}
	}
}
