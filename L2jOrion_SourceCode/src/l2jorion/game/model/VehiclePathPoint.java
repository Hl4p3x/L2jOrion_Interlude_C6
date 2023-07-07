package l2jorion.game.model;

public class VehiclePathPoint
{
	public int x;
	public int y;
	public int z;
	public int moveSpeed;
	public int rotationSpeed;
	
	public VehiclePathPoint(int _x, int _y, int _z)
	{
		x = _x;
		y = _y;
		z = _z;
		moveSpeed = 350;
		rotationSpeed = 4000;
	}
	
	public VehiclePathPoint(int _x, int _y, int _z, int _m, int _r)
	{
		x = _x;
		y = _y;
		z = _z;
		moveSpeed = _m;
		rotationSpeed = _r;
	}
}