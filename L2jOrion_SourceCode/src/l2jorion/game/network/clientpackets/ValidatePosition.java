package l2jorion.game.network.clientpackets;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.serverpackets.GetOnVehicle;
import l2jorion.game.network.serverpackets.ValidateLocation;

public final class ValidatePosition extends PacketClient
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
			if (realX != 0)
			{
				return;
			}
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
		
		// Check falling if previous client Z is less then
		if (activeChar.isFalling(_z))
		{
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
				case AI_INTENTION_FOLLOW:
				{
					if (activeChar.getTarget() instanceof L2PcInstance && activeChar.getTarget() != activeChar && activeChar.getAI().getIntention() != AI_INTENTION_ATTACK)
					{
						activeChar.setXYZ(realX, realY, realZ);
						activeChar.sendPacket(new ValidateLocation(activeChar));
					}
					else
					{
						activeChar.setXYZ(_x, _y, _z);
					}
					break;
				}
				default:
				{
					activeChar.setXYZ(_x, _y, _z);
					break;
				}
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