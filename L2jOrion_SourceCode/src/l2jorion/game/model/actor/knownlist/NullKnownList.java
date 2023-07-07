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
package l2jorion.game.model.actor.knownlist;

import l2jorion.game.model.L2Object;

public class NullKnownList extends ObjectKnownList
{
	public NullKnownList(final L2Object activeObject)
	{
		super(activeObject);
	}
	
	@Override
	public boolean addKnownObject(final L2Object object)
	{
		return false;
	}
	
	@Override
	public L2Object getActiveObject()
	{
		return super.getActiveObject();
	}
	
	@Override
	public int getDistanceToForgetObject(final L2Object object)
	{
		return 0;
	}
	
	@Override
	public int getDistanceToWatchObject(final L2Object object)
	{
		return 0;
	}
	
	@Override
	public void removeAllKnownObjects()
	{
		// null
	}
	
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		return false;
	}
}
