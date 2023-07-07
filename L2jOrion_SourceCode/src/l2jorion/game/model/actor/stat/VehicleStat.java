package l2jorion.game.model.actor.stat;

import l2jorion.game.model.L2Vehicle;

public class VehicleStat extends CharStat
{
	private float _moveSpeed = 0;
	private int _rotationSpeed = 0;
	
	public VehicleStat(L2Vehicle activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public float getMoveSpeed()
	{
		return _moveSpeed;
	}
	
	public final void setMoveSpeed(float speed)
	{
		_moveSpeed = speed;
	}
	
	public final int getRotationSpeed()
	{
		return _rotationSpeed;
	}
	
	public final void setRotationSpeed(int speed)
	{
		_rotationSpeed = speed;
	}
}