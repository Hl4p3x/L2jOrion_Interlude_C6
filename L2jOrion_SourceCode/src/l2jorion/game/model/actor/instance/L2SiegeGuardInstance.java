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

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2SiegeGuardAI;
import l2jorion.game.enums.AchType;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.knownlist.SiegeGuardKnownList;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public class L2SiegeGuardInstance extends L2Attackable
{
	private int _homeX;
	private int _homeY;
	private int _homeZ;
	
	public L2SiegeGuardInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
	}
	
	@Override
	public SiegeGuardKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof SiegeGuardKnownList))
		{
			setKnownList(new SiegeGuardKnownList(this));
		}
		
		return (SiegeGuardKnownList) super.getKnownList();
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2SiegeGuardAI(this);
	}
	
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		if (getClanHall() != null)
		{
			if (getClanHall().isSiegableHall() && ((SiegableHall) getClanHall()).isInSiege())
			{
				return true;
			}
		}
		
		return attacker != null && attacker instanceof L2PcInstance && (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress() && !getCastle().getSiege().checkIsDefender(((L2PcInstance) attacker).getClan()));
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
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
	
	public int getHomeY()
	{
		return _homeY;
	}
	
	@Override
	public void returnHome()
	{
		if (!isInsideRadius(_homeX, _homeY, 40, false))
		{
			setisReturningToSpawnPoint(true);
			clearAggroList();
			
			if (hasAI())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_homeX, _homeY, _homeZ, 0));
			}
		}
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
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			if (!isAutoAttackable(player))
			{
				if (!canInteract(player))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					if (player.isMoving())
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, this);
					}
					
					player.broadcastPacket(new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE));
					
					broadcastPacket(new SocialAction(getObjectId(), Rnd.nextInt(8)));
					
					showChatWindow(player, 0);
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void addDamageHate(final L2Character attacker, final int damage, final int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!(attacker instanceof L2SiegeGuardInstance))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		final L2PcInstance player = killer.getActingPlayer();
		if (player != null && (player.getLevel() - getLevel() <= 20))
		{
			player.getAchievement().increase(AchType.GUARD);
		}
		
		return true;
	}
}
