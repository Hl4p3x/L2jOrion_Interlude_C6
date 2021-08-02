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
import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2FortSiegeGuardAI;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.knownlist.FortSiegeGuardKnownList;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2FortSiegeGuardInstance extends L2Attackable
{
	private static Logger LOG = LoggerFactory.getLogger(L2FortSiegeGuardInstance.class);
	
	public L2FortSiegeGuardInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // inits the knownlist
	}
	
	@Override
	public FortSiegeGuardKnownList getKnownList()
	{
		if (!(super.getKnownList() instanceof FortSiegeGuardKnownList))
		{
			setKnownList(new FortSiegeGuardKnownList(this));
		}
		return (FortSiegeGuardKnownList) super.getKnownList();
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2FortSiegeGuardAI(this);
	}
	
	/**
	 * Return True if a siege is in progress and the L2Character attacker isn't a Defender.<BR>
	 * <BR>
	 * @param attacker The L2Character that the L2SiegeGuardInstance try to attack
	 */
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		if (!(attacker instanceof L2PlayableInstance))
		{
			return false;
		}
		
		boolean isFort = false;
		if (attacker instanceof L2PcInstance)
		{
			isFort = (getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress() && !getFort().getSiege().checkIsDefender(((L2PcInstance) attacker).getClan()));
		}
		else
		{
			isFort = (getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress() && !getFort().getSiege().checkIsDefender(((L2Summon) attacker).getOwner().getClan()));
		}
		
		// Attackable during siege by all except defenders
		return isFort;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	/**
	 * This method forces guard to return to home location previously set
	 */
	@Override
	public void returnHome()
	{
		if (getWalkSpeed() <= 0)
		{
			return;
		}
		if (!isInsideRadius(getSpawn().getLocx(), getSpawn().getLocy(), 40, false))
		{
			if (Config.DEBUG)
			{
				LOG.info(getObjectId() + ": moving home");
			}
			setisReturningToSpawnPoint(true);
			clearAggroList();
			
			if (hasAI())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz(), 0));
			}
		}
	}
	
	/**
	 * Custom onAction behaviour. Note that super() is not called because guards need extra check to see if a player should interact or ATTACK them when clicked.
	 */
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			if (Config.DEBUG)
			{
				LOG.info("new target selected:" + getObjectId());
			}
			
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			my = null;
			
			// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
			su = null;
			
			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				if (Math.abs(player.getZ() - getZ()) < 600) // this max heigth difference might need some tweaking
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
			}
			if (!isAutoAttackable(player))
			{
				if (!canInteract(player))
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Like L2OFF player must rotate to the Npc
					MoveToPawn sp = new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE);
					player.sendPacket(sp);
					Broadcast.toKnownPlayers(player, sp);
				}
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void addDamageHate(final L2Character attacker, final int damage, final int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!(attacker instanceof L2FortSiegeGuardInstance))
		{
			if (attacker instanceof L2PlayableInstance)
			{
				L2PcInstance player = null;
				if (attacker instanceof L2PcInstance)
				{
					player = (L2PcInstance) attacker;
				}
				else if (attacker instanceof L2Summon)
				{
					player = ((L2Summon) attacker).getOwner();
				}
				if (player != null && player.getClan() != null && player.getClan().getHasFort() == getFort().getFortId())
				{
					return;
				}
			}
			super.addDamageHate(attacker, damage, aggro);
		}
	}
}
