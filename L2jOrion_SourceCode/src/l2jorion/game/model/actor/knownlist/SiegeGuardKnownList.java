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
package l2jorion.game.model.actor.knownlist;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeGuardInstance;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;

public class SiegeGuardKnownList extends AttackableKnownList
{
	public SiegeGuardKnownList(final L2SiegeGuardInstance activeChar)
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
		
		if (getActiveChar().getClanHall() != null)
		{
			if (getActiveChar().getClanHall().isSiegableHall() && ((SiegableHall) getActiveChar().getClanHall()).isInSiege())
			{
				if (getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		
		if (getActiveChar().getCastle() != null && getActiveChar().getCastle().getSiege().getIsInProgress())
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
			
			if (player != null && (player.getClan() == null || getActiveChar().getCastle().getSiege().getAttackerClan(player.getClan()) != null))
			{
				if (getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		
		return true;
	}
	
	@Override
	public final L2SiegeGuardInstance getActiveChar()
	{
		return (L2SiegeGuardInstance) super.getActiveChar();
	}
}
