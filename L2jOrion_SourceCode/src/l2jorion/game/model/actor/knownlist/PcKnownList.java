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

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Vehicle;
import l2jorion.game.model.actor.instance.L2FenceInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.DeleteObject;
import l2jorion.game.network.serverpackets.SpawnItemPoly;

public class PcKnownList extends PlayableKnownList
{
	public PcKnownList(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	private void sendInfoFrom(L2Object object)
	{
		if (object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
		{
			getActiveChar().sendPacket(new SpawnItemPoly(object));
		}
		else
		{
			object.sendInfo(getActiveChar());
			
			if (object instanceof L2Character)
			{
				L2Character obj = (L2Character) object;
				if (obj.hasAI())
				{
					obj.getAI().describeStateToPlayer(getActiveChar());
				}
			}
		}
	}
	
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
		{
			return false;
		}
		
		sendInfoFrom(object);
		return true;
	}
	
	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if (!super.removeKnownObject(object))
		{
			return false;
		}
		
		getActiveChar().sendPacket(new DeleteObject(object));
		
		return true;
	}
	
	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}
	
	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		if (object instanceof L2Vehicle)
		{
			return 8000;
		}
		
		if (object instanceof L2FenceInstance)
		{
			return 8000;
		}
		
		/*
		 * final int knownlistSize = getKnownObjects().size(); if (knownlistSize <= 25) { return 4200; } if (knownlistSize <= 35) { return 3600; } if (knownlistSize <= 70) { return 3400; }
		 */
		
		return 6000;
	}
	
	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2Vehicle)
		{
			return 8000;
		}
		
		if (object instanceof L2FenceInstance)
		{
			return 8000;
		}
		
		/*
		 * final int knownlistSize = getKnownObjects().size(); if (knownlistSize <= 25) { return 4200; } if (knownlistSize <= 35) { return 3600; } if (knownlistSize <= 70) { return 3400; }
		 */
		
		return 6000;
	}
}
