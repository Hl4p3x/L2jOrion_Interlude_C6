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

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public final class L2ChestInstance extends L2MonsterInstance
{
	private volatile boolean _isInteracted;
	private volatile boolean _specialDrop;
	
	public L2ChestInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		_isInteracted = false;
		_specialDrop = false;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_isInteracted = false;
		_specialDrop = false;
		setMustRewardExpSp(true);
	}
	
	public synchronized boolean isInteracted()
	{
		return _isInteracted;
	}
	
	public synchronized void setInteracted()
	{
		_isInteracted = true;
	}
	
	public synchronized boolean isSpecialDrop()
	{
		return _specialDrop;
	}
	
	public synchronized void setSpecialDrop()
	{
		_specialDrop = true;
	}
	
	@Override
	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character lastAttacker)
	{
		int id = getTemplate().getNpcId();
		
		if (!_specialDrop)
		{
			if (id >= 18265 && id <= 18286)
			{
				id += 3536;
			}
			else if (id == 18287 || id == 18288)
			{
				id = 21671;
			}
			else if (id == 18289 || id == 18290)
			{
				id = 21694;
			}
			else if (id == 18291 || id == 18292)
			{
				id = 21717;
			}
			else if (id == 18293 || id == 18294)
			{
				id = 21740;
			}
			else if (id == 18295 || id == 18296)
			{
				id = 21763;
			}
			else if (id == 18297 || id == 18298)
			{
				id = 21786;
			}
		}
		
		super.doItemDrop(NpcTable.getInstance().getTemplate(id), lastAttacker);
	}
	
	public void chestTrap(final L2Character player)
	{
		int trapSkillId = 0;
		final int rnd = Rnd.get(120);
		
		if (getTemplate().getLevel() >= 61)
		{
			if (rnd >= 90)
			{
				trapSkillId = 4139;// explosion
			}
			else if (rnd >= 50)
			{
				trapSkillId = 4118;// area paralysys
			}
			else if (rnd >= 20)
			{
				trapSkillId = 1167;// poison cloud
			}
			else
			{
				trapSkillId = 223;// sting
			}
		}
		else if (getTemplate().getLevel() >= 41)
		{
			if (rnd >= 90)
			{
				trapSkillId = 4139;// explosion
			}
			else if (rnd >= 60)
			{
				trapSkillId = 96;// bleed
			}
			else if (rnd >= 20)
			{
				trapSkillId = 1167;// poison cloud
			}
			else
			{
				trapSkillId = 4118;// area paralysys
			}
		}
		else if (getTemplate().getLevel() >= 21)
		{
			if (rnd >= 80)
			{
				trapSkillId = 4139;// explosion
			}
			else if (rnd >= 50)
			{
				trapSkillId = 96;// bleed
			}
			else if (rnd >= 20)
			{
				trapSkillId = 1167;// poison cloud
			}
			else
			{
				trapSkillId = 129;// poison
			}
		}
		else
		{
			if (rnd >= 80)
			{
				trapSkillId = 4139;// explosion
			}
			else if (rnd >= 50)
			{
				trapSkillId = 96;// bleed
			}
			else
			{
				trapSkillId = 129;// poison
			}
		}
		
		player.sendPacket(SystemMessage.sendString("There was a trap!"));
		handleCast(player, trapSkillId);
	}
	
	private boolean handleCast(final L2Character player, final int skillId)
	{
		int skillLevel = 1;
		final byte lvl = getTemplate().getLevel();
		if (lvl > 20 && lvl <= 40)
		{
			skillLevel = 3;
		}
		else if (lvl > 40 && lvl <= 60)
		{
			skillLevel = 5;
		}
		else if (lvl > 60)
		{
			skillLevel = 6;
		}
		
		if (player.isDead() || !player.isVisible() || !player.isInsideRadius(this, getDistanceToWatchObject(player), false, false))
		{
			return false;
		}
		
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		
		if (player.getFirstEffect(skill) == null)
		{
			skill.getEffects(this, player, false, false, false);
			broadcastPacket(new MagicSkillUser(this, player, skill.getId(), skillLevel, skill.getHitTime(), 0));
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		if (super.isMovementDisabled())
		{
			return true;
		}
		
		if (isInteracted())
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	@Override
	public boolean isMonster()
	{
		return false;
	}
}
