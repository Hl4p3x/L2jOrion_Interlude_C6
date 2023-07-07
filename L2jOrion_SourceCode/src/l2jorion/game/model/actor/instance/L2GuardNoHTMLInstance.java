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
package l2jorion.game.model.actor.instance;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2AttackableAI;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.knownlist.GuardNoHTMLKnownList;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public final class L2GuardNoHTMLInstance extends L2Attackable
{
	private static Logger LOG = LoggerFactory.getLogger(L2GuardNoHTMLInstance.class);
	
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
	
	public L2GuardNoHTMLInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ReturnTask(), RETURN_INTERVAL, RETURN_INTERVAL + Rnd.nextInt(60000));
	}
	
	@Override
	public final GuardNoHTMLKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof GuardNoHTMLKnownList))
		{
			setKnownList(new GuardNoHTMLKnownList(this));
		}
		return (GuardNoHTMLKnownList) super.getKnownList();
	}
	
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return attacker instanceof L2MonsterInstance;
	}
	
	public void getHomeLocation()
	{
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();
	}
	
	public int getHomeX()
	{
		return _homeX;
	}
	
	@Override
	public void returnHome()
	{
		if (!isInsideRadius(_homeX, _homeY, 150, false))
		{
			clearAggroList();
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_homeX, _homeY, _homeZ, 0));
		}
	}
	
	@Override
	public void onSpawn()
	{
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();
		if (Config.DEBUG)
		{
			LOG.debug(getObjectId() + ": Home location set to" + " X:" + _homeX + " Y:" + _homeY + " Z:" + _homeZ);
		}
		final L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
		if (region != null && !region.isActive())
		{
			((L2AttackableAI) getAI()).stopAITask();
		}
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
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
				if (!isInsideRadius(player, INTERACTION_DISTANCE, false, false))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, null);
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
