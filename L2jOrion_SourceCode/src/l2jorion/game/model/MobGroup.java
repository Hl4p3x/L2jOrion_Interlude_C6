/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.model;

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2ControllableMobAI;
import l2jorion.game.datatables.MobGroupTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.model.actor.instance.L2ControllableMobInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.spawn.L2GroupSpawn;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public final class MobGroup
{
	private final L2NpcTemplate _npcTemplate;
	private final int _groupId;
	private final int _maxMobCount;
	
	private List<L2ControllableMobInstance> _mobs;
	
	public MobGroup(final int groupId, final L2NpcTemplate npcTemplate, final int maxMobCount)
	{
		_groupId = groupId;
		_npcTemplate = npcTemplate;
		_maxMobCount = maxMobCount;
	}
	
	public int getActiveMobCount()
	{
		return getMobs().size();
	}
	
	public int getGroupId()
	{
		return _groupId;
	}
	
	public int getMaxMobCount()
	{
		return _maxMobCount;
	}
	
	public List<L2ControllableMobInstance> getMobs()
	{
		if (_mobs == null)
		{
			_mobs = new FastList<>();
		}
		
		return _mobs;
	}
	
	public String getStatus()
	{
		try
		{
			final L2ControllableMobAI mobGroupAI = (L2ControllableMobAI) getMobs().get(0).getAI();
			
			switch (mobGroupAI.getAlternateAI())
			{
				case L2ControllableMobAI.AI_NORMAL:
					return "Idle";
				case L2ControllableMobAI.AI_FORCEATTACK:
					return "Force Attacking";
				case L2ControllableMobAI.AI_FOLLOW:
					return "Following";
				case L2ControllableMobAI.AI_CAST:
					return "Casting";
				case L2ControllableMobAI.AI_ATTACK_GROUP:
					return "Attacking Group";
				default:
					return "Idle";
			}
		}
		catch (final Exception e)
		{
			return "Unspawned";
		}
	}
	
	public L2NpcTemplate getTemplate()
	{
		return _npcTemplate;
	}
	
	public boolean isGroupMember(final L2ControllableMobInstance mobInst)
	{
		for (final L2ControllableMobInstance groupMember : getMobs())
		{
			if (groupMember == null)
			{
				continue;
			}
			
			if (groupMember.getObjectId() == mobInst.getObjectId())
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void spawnGroup(final int x, final int y, final int z)
	{
		if (getActiveMobCount() > 0)
		{
			return;
		}
		
		try
		{
			for (int i = 0; i < getMaxMobCount(); i++)
			{
				L2GroupSpawn spawn = new L2GroupSpawn(getTemplate());
				
				final int signX = Rnd.nextInt(2) == 0 ? -1 : 1;
				final int signY = Rnd.nextInt(2) == 0 ? -1 : 1;
				final int randX = Rnd.nextInt(MobGroupTable.RANDOM_RANGE);
				final int randY = Rnd.nextInt(MobGroupTable.RANDOM_RANGE);
				
				spawn.setLocx(x + signX * randX);
				spawn.setLocy(y + signY * randY);
				spawn.setLocz(z);
				spawn.stopRespawn();
				
				SpawnTable.getInstance().addNewSpawn(spawn, false);
				getMobs().add((L2ControllableMobInstance) spawn.doGroupSpawn());
			}
		}
		catch (final ClassNotFoundException e)
		{
			// null
		}
		catch (final NoSuchMethodException e2)
		{
			// null
		}
	}
	
	public void spawnGroup(final L2PcInstance activeChar)
	{
		spawnGroup(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	}
	
	public void teleportGroup(final L2PcInstance player)
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				final int x = player.getX() + Rnd.nextInt(50);
				final int y = player.getY() + Rnd.nextInt(50);
				
				mobInst.teleToLocation(x, y, player.getZ(), true);
				L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
				ai.follow(player);
				ai = null;
			}
		}
	}
	
	public L2ControllableMobInstance getRandomMob()
	{
		removeDead();
		
		if (getActiveMobCount() == 0)
		{
			return null;
		}
		
		final int choice = Rnd.nextInt(getActiveMobCount());
		
		return getMobs().get(choice);
	}
	
	public void unspawnGroup()
	{
		removeDead();
		
		if (getActiveMobCount() == 0)
		{
			return;
		}
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				mobInst.deleteMe();
			}
			
			SpawnTable.getInstance().deleteSpawn(mobInst.getSpawn(), false);
		}
		
		getMobs().clear();
	}
	
	public void killGroup(final L2PcInstance activeChar)
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				mobInst.reduceCurrentHp(mobInst.getMaxHp() + 1, activeChar);
			}
			
			SpawnTable.getInstance().deleteSpawn(mobInst.getSpawn(), false);
		}
		
		getMobs().clear();
	}
	
	public void setAttackRandom()
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
			ai.setAlternateAI(L2ControllableMobAI.AI_NORMAL);
			ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			ai = null;
		}
	}
	
	public void setAttackTarget(final L2Character target)
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
			ai.forceAttack(target);
			ai = null;
		}
	}
	
	public void setIdleMode()
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
			ai.stop();
			ai = null;
		}
	}
	
	public void returnGroup(final L2Character activeChar)
	{
		setIdleMode();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			final int signX = Rnd.nextInt(2) == 0 ? -1 : 1;
			final int signY = Rnd.nextInt(2) == 0 ? -1 : 1;
			final int randX = Rnd.nextInt(MobGroupTable.RANDOM_RANGE);
			final int randY = Rnd.nextInt(MobGroupTable.RANDOM_RANGE);
			
			L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
			ai.move(activeChar.getX() + signX * randX, activeChar.getY() + signY * randY, activeChar.getZ());
			ai = null;
		}
	}
	
	public void setFollowMode(final L2Character character)
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
			ai.follow(character);
			ai = null;
		}
	}
	
	public void setCastMode()
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
			ai.setAlternateAI(L2ControllableMobAI.AI_CAST);
			ai = null;
		}
	}
	
	public void setNoMoveMode(final boolean enabled)
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
			ai.setNotMoving(enabled);
			ai = null;
		}
	}
	
	protected void removeDead()
	{
		List<L2ControllableMobInstance> deadMobs = new FastList<>();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst != null && mobInst.isDead())
			{
				deadMobs.add(mobInst);
			}
		}
		
		getMobs().removeAll(deadMobs);
		deadMobs = null;
	}
	
	public void setInvul(final boolean invulState)
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst != null)
			{
				mobInst.setInvul(invulState);
			}
		}
	}
	
	public void setAttackGroup(final MobGroup otherGrp)
	{
		removeDead();
		
		for (final L2ControllableMobInstance mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			L2ControllableMobAI ai = (L2ControllableMobAI) mobInst.getAI();
			ai.forceAttackGroup(otherGrp);
			ai.setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			ai = null;
		}
	}
}
