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

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2AttackableAI;
import l2jorion.game.enums.AchType;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.knownlist.GuardKnownList;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public final class L2GuardInstance extends L2Attackable
{
	
	private static Logger LOG = LoggerFactory.getLogger(L2GuardInstance.class);
	
	private int _homeX;
	private int _homeY;
	private int _homeZ;
	
	private static final int RETURN_INTERVAL = 60000;
	
	public class ReturnTask implements Runnable
	{
		@Override
		public void run()
		{
			if (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			{
				returnHome();
			}
		}
	}
	
	public L2GuardInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
		
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ReturnTask(), RETURN_INTERVAL, RETURN_INTERVAL + Rnd.nextInt(60000));
	}
	
	@Override
	public boolean isGuard()
	{
		return true;
	}
	
	@Override
	public final GuardKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof GuardKnownList))
		{
			setKnownList(new GuardKnownList(this));
		}
		return (GuardKnownList) super.getKnownList();
	}
	
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return attacker instanceof L2MonsterInstance;
	}
	
	public int getHomeX()
	{
		return _homeX;
	}
	
	@Override
	public void returnHome()
	{
		if (!isInsideRadius(getSpawn().getLocx(), getSpawn().getLocy(), 150, false))
		{
			clearAggroList();
			
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz(), 0));
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();
		
		if (Config.DEBUG)
		{
			LOG.debug(getObjectId() + ": Home location set to" + " X:" + _homeX + " Y:" + _homeY + " Z:" + _homeZ);
		}
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
		if (region != null && !region.isActive())
		{
			((L2AttackableAI) getAI()).stopAITask();
		}
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		return "data/html/guard/" + pom + ".htm";
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (getObjectId() != player.getTargetId())
		{
			player.setTarget(this);
		}
		else
		{
			if (containsTarget(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				if (!canInteract(player))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					MoveToPawn sp = new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE);
					player.sendPacket(sp);
					Broadcast.toKnownPlayers(player, sp);
					
					SocialAction sa = new SocialAction(getObjectId(), Rnd.nextInt(8));
					broadcastPacket(sa);
					
					showChatWindow(player, 0);
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		final L2PcInstance player = killer.getActingPlayer();
		if (player != null && (player.getLevel() - getLevel() <= 10))
		{
			player.getAchievement().increase(AchType.GUARD);
		}
		
		return true;
	}
	
	@Override
	public boolean isMonster()
	{
		return false;
	}
}
