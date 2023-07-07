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
package l2jorion.game.model.actor.instance;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.Location;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

public class L2TownPetInstance extends L2NpcInstance
{
	int randomX, randomY, spawnX, spawnY;
	
	public L2TownPetInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new RandomWalkTask(), 2000, Rnd.get(4000, 8000));
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else
		{
			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
		}
		
		player.sendPacket(new ActionFailed());
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		spawnX = getX();
		spawnY = getY();
	}
	
	public class RandomWalkTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!isInActiveRegion())
			{
				return;
			}
			
			randomX = spawnX + Rnd.get(2 * 50) - 50;
			randomY = spawnY + Rnd.get(2 * 50) - 50;
			setRunning();
			if ((randomX != getX()) && (randomY != getY()))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(randomX, randomY, getZ(), 0));
			}
		}
	}
}