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
package l2jorion.game.model.quest;

import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public final class QuestSpawn
{
	private final Logger LOG = LoggerFactory.getLogger(QuestSpawn.class);
	
	private static QuestSpawn instance;
	
	public static QuestSpawn getInstance()
	{
		if (instance == null)
		{
			instance = new QuestSpawn();
		}
		
		return instance;
	}
	
	public class DeSpawnScheduleTimerTask implements Runnable
	{
		L2NpcInstance _npc = null;
		
		public DeSpawnScheduleTimerTask(final L2NpcInstance npc)
		{
			_npc = npc;
		}
		
		@Override
		public void run()
		{
			_npc.onDecay();
		}
	}
	
	public L2NpcInstance addSpawn(final int npcId, final L2Character cha)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0);
	}
	
	public L2NpcInstance addSpawn(final int npcId, int x, int y, final int z, final int heading, final boolean randomOffset, final int despawnDelay)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, 0);
	}
	
	public L2NpcInstance addSpawn(final int npcId, int x, int y, final int z, final int heading, final boolean randomOffset, final int despawnDelay, int respawnDelay)
	{
		L2NpcInstance result = null;
		try
		{
			final L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
			if (template != null)
			{
				// Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code
				// reaches here, xyz have become 0! Also, a questdev might have purposely set xy to 0,0...however,
				// the spawn code is coded such that if x=y=0, it looks into location for the spawn loc! This will NOT work
				// with quest spawns! For both of the above cases, we need a fail-safe spawn. For this, we use the
				// default spawn location, which is at the player's loc.
				if (x == 0 && y == 0)
				{
					LOG.error("Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}
				
				if (randomOffset)
				{
					int offset;
					
					// Get the direction of the offset
					offset = Rnd.get(2);
					if (offset == 0)
					{
						offset = -1;
					}
					
					// make offset negative
					offset *= Rnd.get(50, 100);
					x += offset;
					
					// Get the direction of the offset
					offset = Rnd.get(2);
					if (offset == 0)
					{
						offset = -1;
					}
					
					// make offset negative
					offset *= Rnd.get(50, 100);
					y += offset;
				}
				
				L2Spawn spawn = new L2Spawn(template);
				spawn.setHeading(heading);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z + 20);
				
				if (respawnDelay > 0)
				{
					spawn.setRespawnDelay(respawnDelay);
				}
				else
				{
					spawn.stopRespawn();
				}
				
				if (!randomOffset)
				{
					spawn.setNoRandomLoc(true);
				}
				
				result = spawn.spawnOne();
				
				if (despawnDelay > 0)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);
				}
				return result;
			}
		}
		catch (final Exception e1)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e1.printStackTrace();
			}
			
			LOG.warn("Could not spawn Npc " + npcId);
		}
		
		return null;
	}
	
}
