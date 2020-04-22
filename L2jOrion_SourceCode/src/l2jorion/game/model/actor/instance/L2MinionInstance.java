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

import l2jorion.game.ai.L2AttackableAI;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.templates.L2NpcTemplate;

/**
 * This class manages all Minions. In a group mob, there are one master called RaidBoss and several slaves called Minions.
 * @version $Revision: 1.20.4.6 $ $Date: 2005/04/06 16:13:39 $
 */
public final class L2MinionInstance extends L2MonsterInstance
{
	// private static Logger LOG = LoggerFactory.getLogger(L2RaidMinionInstance.class);
	
	/** The master L2Character whose depends this L2MinionInstance on. */
	private L2MonsterInstance _master;
	
	/**
	 * Constructor of L2MinionInstance (use L2Character and L2NpcInstance constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2MinionInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li> <li>Set the name of the L2MinionInstance</li> <li>Create a RandomAnimation Task that will be launched after the calculated
	 * delay if the server allow it</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template the template
	 */
	public L2MinionInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	/**
	 * Return the master of this L2MinionInstance.<BR>
	 * <BR>
	 * @return the leader
	 */
	@Override
	public L2MonsterInstance getLeader()
	{
		return _master;
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.actor.instance.L2MonsterInstance#onSpawn()
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		// Notify Leader that Minion has Spawned
		getLeader().notifyMinionSpawned(this);
		
		if (getLeader() != null)
		{
			setIsRaidMinion(getLeader().isRaid());
		}
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
		if (region != null && !region.isActive())
		{
			((L2AttackableAI) getAI()).stopAITask();
		}
	}
	
	/**
	 * Set the master of this L2MinionInstance.<BR>
	 * <BR>
	 * @param leader The L2Character that leads this L2MinionInstance
	 */
	public void setLeader(final L2MonsterInstance leader)
	{
		_master = leader;
	}
	
	/**
	 * Manages the doDie event for this L2MinionInstance.<BR>
	 * <BR>
	 * @param killer The L2Character that killed this L2MinionInstance.<BR>
	 * <BR>
	 * @return true, if successful
	 */
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		_master.notifyMinionDied(this);
		return true;
	}
}
