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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.GetOnVehicle;
import l2jorion.game.network.serverpackets.ValidateLocation;

public final class ValidatePosition extends L2GameClientPacket
{
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private int _data;
	
	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
		_data = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || activeChar.isTeleporting() || activeChar.inObserverMode())
		{
			return;
		}
		
		final int realX = activeChar.getX();
		final int realY = activeChar.getY();
		final int realZ = activeChar.getZ();
		
		if (_x == 0 && _y == 0)
		{
			if (realX != 0) // in this case this seems like a client error
			{
				return;
			}
		}
		
		// check falling if previous client Z is less then
		if (activeChar.isFalling(_z))
		{
			return;
		}
		
		int dx, dy, dz;
		double distance;
		
		if (activeChar.isInBoat())
		{
			if (Config.COORD_SYNCHRONIZE == 2)
			{
				dx = _x - activeChar.getInVehiclePosition().getX();
				dy = _y - activeChar.getInVehiclePosition().getY();
				dz = _z - activeChar.getInVehiclePosition().getZ();
				distance = ((dx * dx) + (dy * dy));
				if (distance > 250000)
				{
					sendPacket(new GetOnVehicle(activeChar.getObjectId(), _data, activeChar.getInVehiclePosition()));
				}
			}
			return;
		}
		
		dx = _x - realX;
		dy = _y - realY;
		dz = _z - realZ;
		distance = dx * dx + dy * dy;
		
		if (distance < 360000) // if too large, messes observation
		{
			if (Config.COORD_SYNCHRONIZE == -1) // Only Z coordinate synched to server,
			{
				activeChar.getPosition().setXYZ(realX, realY, _z);
				return;
			}
			
			if (Config.COORD_SYNCHRONIZE == 1)
			{
				if (!activeChar.isMoving() || !activeChar.validateMovementHeading(_heading))
				{
					if (distance < 2500)
					{
						activeChar.getPosition().setXYZ(realX, realY, _z);
					}
					else
					{
						activeChar.getPosition().setXYZ(_x, _y, _z);
					}
				}
				else
				{
					activeChar.getPosition().setXYZ(realX, realY, _z);
				}
				
				activeChar.setHeading(_heading);
				return;
			}
			
			if (Config.COORD_SYNCHRONIZE == 2)
			{
				if ((distance > 250000 || Math.abs(dz) > 200))
				{
					if (Math.abs(dz) > 200 && Math.abs(dz) < 1500 && Math.abs(_z - activeChar.getClientZ()) < 800)
					{
						activeChar.setXYZ(realX, realY, realZ);
					}
					else
					{
						activeChar.sendPacket(new ValidateLocation(activeChar));
					}
				}
			}
		}
		
		if (Config.COORD_SYNCHRONIZE == 3)
		{
			switch (activeChar.getAI().getIntention())
			{
				case AI_INTENTION_ATTACK:
				case AI_INTENTION_CAST:
				case AI_INTENTION_FOLLOW:
					activeChar.setXYZ(realX, realY, realZ);
					break;
				default:
					activeChar.setXYZ(_x, _y, realZ);
					break;
			}
			
		}
		
		activeChar.setClientX(_x);
		activeChar.setClientY(_y);
		activeChar.setClientZ(_z);
		activeChar.setClientHeading(_heading);
		activeChar.setLastServerPosition(realX, realY, realZ);
	}
	
	@Override
	public String getType()
	{
		return "[C] 48 ValidatePosition";
	}
}