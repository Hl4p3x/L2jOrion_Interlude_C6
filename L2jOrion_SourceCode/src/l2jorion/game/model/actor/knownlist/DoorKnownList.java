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

import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2FortSiegeGuardInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeGuardInstance;

public class DoorKnownList extends CharKnownList
{
	public DoorKnownList(final L2DoorInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public final L2DoorInstance getActiveChar()
	{
		return (L2DoorInstance) super.getActiveChar();
	}
	
	@Override
	public int getDistanceToForgetObject(final L2Object object)
	{
		if (object instanceof L2SiegeGuardInstance || object instanceof L2FortSiegeGuardInstance)
		{
			return 800;
		}
		
		if (!(object instanceof L2PcInstance))
		{
			return 0;
		}
		
		return 20000;
	}
	
	@Override
	public int getDistanceToWatchObject(final L2Object object)
	{
		if (object instanceof L2SiegeGuardInstance || object instanceof L2FortSiegeGuardInstance)
		{
			return 600;
		}
		
		if (!(object instanceof L2PcInstance))
		{
			return 0;
		}
		
		return 20000;
	}
}
