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
package l2jorion.game.model.actor.status;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PlayableInstance;

public class PlayableStatus extends CharStatus
{
	public PlayableStatus(final L2PlayableInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void reduceHp(final double value, final L2Character attacker)
	{
		reduceHp(value, attacker, true);
	}
	
	@Override
	public void reduceHp(final double value, final L2Character attacker, final boolean awake)
	{
		if (getActiveChar().isDead())
		{
			return;
		}
		
		super.reduceHp(value, attacker, awake);
		/*
		 * if (attacker != null && attacker != getActiveChar()) { // Flag the attacker if it's a L2PcInstance outside a PvP area L2PcInstance player = null; if (attacker instanceof L2PcInstance) player = (L2PcInstance)attacker; else if (attacker instanceof L2Summon) player =
		 * ((L2Summon)attacker).getOwner(); if (player != null) player.updatePvPStatus(getActiveChar()); }
		 */
	}
	
	@Override
	public L2PlayableInstance getActiveChar()
	{
		return (L2PlayableInstance) super.getActiveChar();
	}
}
