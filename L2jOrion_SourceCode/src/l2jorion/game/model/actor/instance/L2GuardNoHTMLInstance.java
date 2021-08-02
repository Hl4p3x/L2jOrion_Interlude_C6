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
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

/**
 * This class manages all Guards in the world. It inherits all methods from L2Attackable and adds some more such as tracking PK and aggressive L2MonsterInstance.<BR>
 * <BR>
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/06 16:13:40 $
 */
public final class L2GuardNoHTMLInstance extends L2Attackable
{
	
	/** The LOG. */
	private static Logger LOG = LoggerFactory.getLogger(L2GuardNoHTMLInstance.class);
	
	/** The _home x. */
	private int _homeX;
	
	/** The _home y. */
	private int _homeY;
	
	/** The _home z. */
	private int _homeZ;
	
	/** The Constant RETURN_INTERVAL. */
	private static final int RETURN_INTERVAL = 60000;
	
	/**
	 * The Class ReturnTask.
	 */
	public class ReturnTask implements Runnable
	{
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			if (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			{
				returnHome();
			}
		}
	}
	
	/**
	 * Constructor of L2GuardInstance (use L2Character and L2NpcInstance constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2GuardInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li>
	 * <li>Set the name of the L2GuardInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it</li> <BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template the template
	 */
	public L2GuardNoHTMLInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ReturnTask(), RETURN_INTERVAL, RETURN_INTERVAL + Rnd.nextInt(60000));
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.L2Attackable#getKnownList()
	 */
	@Override
	public final GuardNoHTMLKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof GuardNoHTMLKnownList))
		{
			setKnownList(new GuardNoHTMLKnownList(this));
		}
		return (GuardNoHTMLKnownList) super.getKnownList();
	}
	
	/**
	 * Return true if hte attacker is a L2MonsterInstance.<BR>
	 * <BR>
	 * @param attacker the attacker
	 * @return true, if is auto attackable
	 */
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return attacker instanceof L2MonsterInstance;
	}
	
	/**
	 * Set home location of the L2GuardInstance.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Guard will always try to return to this location after it has killed all PK's in range
	 */
	public void getHomeLocation()
	{
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();
		if (Config.DEBUG)
		{
			LOG.debug(getObjectId() + ": Home location set to" + " X:" + _homeX + " Y:" + _homeY + " Z:" + _homeZ);
		}
	}
	
	/**
	 * Gets the home x.
	 * @return the home x
	 */
	public int getHomeX()
	{
		return _homeX;
	}
	
	/**
	 * Notify the L2GuardInstance to return to its home location (AI_INTENTION_MOVE_TO) and clear its _aggroList.<BR>
	 * <BR>
	 */
	@Override
	public void returnHome()
	{
		if (!isInsideRadius(_homeX, _homeY, 150, false))
		{
			if (Config.DEBUG)
			{
				LOG.debug(getObjectId() + ": moving hometo" + " X:" + _homeX + " Y:" + _homeY + " Z:" + _homeZ);
			}
			clearAggroList();
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_homeX, _homeY, _homeZ, 0));
		}
	}
	
	/**
	 * Set the home location of its L2GuardInstance.<BR>
	 * <BR>
	 */
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
		// check the region where this mob is, do not activate the AI if region is inactive.
		final L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
		if (region != null && !region.isActive())
		{
			((L2AttackableAI) getAI()).stopAITask();
		}
	}
	
	/**
	 * Manage actions when a player click on the L2GuardInstance.<BR>
	 * <BR>
	 * <B><U> Actions on first click on the L2GuardInstance (Select it)</U> :</B><BR>
	 * <BR>
	 * <li>Set the L2GuardInstance as target of the L2PcInstance player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li>
	 * <li>Set the L2PcInstance Intention to AI_INTENTION_IDLE</li>
	 * <li>Send a Server->Client packet ValidateLocation to correct the L2GuardInstance position and heading on the client</li> <BR>
	 * <BR>
	 * <B><U> Actions on second click on the L2GuardInstance (Attack it/Interact with it)</U> :</B><BR>
	 * <BR>
	 * <li>If L2PcInstance is in the _aggroList of the L2GuardInstance, set the L2PcInstance Intention to AI_INTENTION_ATTACK</li>
	 * <li>If L2PcInstance is NOT in the _aggroList of the L2GuardInstance, set the L2PcInstance Intention to AI_INTENTION_INTERACT (after a distance verification) and show message</li> <BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action, AttackRequest</li> <BR>
	 * <BR>
	 * @param player The L2PcInstance that start an action on the L2GuardInstance
	 */
	@Override
	public void onAction(final L2PcInstance player)
	{
		// Check if the L2PcInstance already target the L2GuardInstance
		if (getObjectId() != player.getTargetId())
		{
			// Set the L2PcInstance Intention to AI_INTENTION_IDLE
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
			if (Config.DEBUG)
			{
				LOG.debug(player.getObjectId() + ": Targetted guard " + getObjectId());
			}
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance
			// player The color to display in the select window is White
			final MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Check if the L2PcInstance is in the _aggroList of the L2GuardInstance
			if (containsTarget(player))
			{
				if (Config.DEBUG)
				{
					LOG.debug(player.getObjectId() + ": Attacked guard " + getObjectId());
				}
				// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				// Calculate the distance between the L2PcInstance and the L2NpcInstance
				if (!isInsideRadius(player, INTERACTION_DISTANCE, false, false))
				{
					// Set the L2PcInstance Intention to AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
					player.sendPacket(ActionFailed.STATIC_PACKET);
					// Set the L2PcInstance Intention to AI_INTENTION_IDLE
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, null);
				}
			}
		}
	}
}
