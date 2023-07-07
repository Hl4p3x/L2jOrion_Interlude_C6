package l2jorion.game.geo.geodriver.blocks;

import java.nio.ByteBuffer;

import l2jorion.game.geo.geodriver.IBlock;

public class FlatBlock implements IBlock
{
	private final short _height;
	
	public FlatBlock(ByteBuffer bb)
	{
		_height = bb.getShort();
	}
	
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return true;
	}
	
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return _height;
	}
	
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return _height <= worldZ ? _height : worldZ;
	}
	
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return _height >= worldZ ? _height : worldZ;
	}
}
