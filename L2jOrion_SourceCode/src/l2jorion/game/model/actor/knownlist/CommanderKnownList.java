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
package l2jorion.game.model.actor.knownlist;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2CommanderInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class CommanderKnownList extends AttackableKnownList
{
	public CommanderKnownList(final L2CommanderInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
		{
			return false;
		}
		
		if (getActiveChar().getHomeX() == 0)
		{
			getActiveChar().getHomeLocation();
		}
		
		// Check if siege is in progress
		if (getActiveChar().getFort() != null && getActiveChar().getFort().getSiege().getIsInProgress())
		{
			L2PcInstance player = null;
			
			if (object instanceof L2PcInstance)
			{
				player = (L2PcInstance) object;
			}
			else if (object instanceof L2Summon)
			{
				player = ((L2Summon) object).getOwner();
			}
			
			// Check if player is not the defender
			if (player != null && (player.getClan() == null || getActiveChar().getFort().getSiege().getAttackerClan(player.getClan()) != null))
			{
				// LOG.info(getActiveChar().getName()+": PK "+player.getObjectId()+" entered scan range");
				if (getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);// (L2Character)object);
				}
			}
		}
		
		return true;
	}
	
	@Override
	public final L2CommanderInstance getActiveChar()
	{
		return (L2CommanderInstance) super.getActiveChar();
	}
}
