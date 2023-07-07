/*
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

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class VehicleKnownList extends CharKnownList
{
	public VehicleKnownList(L2Character activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		if (!(object instanceof L2PcInstance))
		{
			return 0;
		}
		
		return -1;// object.getKnownList().getDistanceToForgetObject(_activeObject);
	}
	
	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if (!(object instanceof L2PcInstance))
		{
			return 0;
		}
		
		return -1;// object.getKnownList().getDistanceToWatchObject(_activeObject);
	}
}