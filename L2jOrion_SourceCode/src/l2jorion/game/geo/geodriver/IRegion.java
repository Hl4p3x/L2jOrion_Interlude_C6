package l2jorion.game.geo.geodriver;

public interface IRegion
{
	public static final int REGION_BLOCKS_X = 256;
	public static final int REGION_BLOCKS_Y = 256;
	
	public static final int REGION_BLOCKS = REGION_BLOCKS_X * REGION_BLOCKS_Y;
	public static final int REGION_CELLS_X = REGION_BLOCKS_X * IBlock.BLOCK_CELLS_X;
	public static final int REGION_CELLS_Y = REGION_BLOCKS_Y * IBlock.BLOCK_CELLS_Y;
	public static final int REGION_CELLS = REGION_CELLS_X * REGION_CELLS_Y;
	
	boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe);
	
	int getNearestZ(int geoX, int geoY, int worldZ);
	
	int getNextLowerZ(int geoX, int geoY, int worldZ);
	
	int getNextHigherZ(int geoX, int geoY, int worldZ);
	
	boolean hasGeo();
}