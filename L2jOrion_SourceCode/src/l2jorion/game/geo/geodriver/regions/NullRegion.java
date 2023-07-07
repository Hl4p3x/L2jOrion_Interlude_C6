package l2jorion.game.geo.geodriver.regions;

import l2jorion.game.geo.geodriver.IRegion;

public final class NullRegion implements IRegion
{
	public static final NullRegion INSTANCE = new NullRegion();
	
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return true;
	}
	
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return worldZ;
	}
	
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		return worldZ;
	}
	
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		return worldZ;
	}
	
	@Override
	public boolean hasGeo()
	{
		return false;
	}
}
