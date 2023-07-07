package l2jorion.game.geo.geodriver;

public final class Cell
{
	/** East NSWE flag */
	public static final byte NSWE_EAST = 1 << 0;
	/** West NSWE flag */
	public static final byte NSWE_WEST = 1 << 1;
	/** South NSWE flag */
	public static final byte NSWE_SOUTH = 1 << 2;
	/** North NSWE flag */
	public static final byte NSWE_NORTH = 1 << 3;
	
	/** North-East NSWE flags */
	public static final byte NSWE_NORTH_EAST = NSWE_NORTH | NSWE_EAST;
	/** North-West NSWE flags */
	public static final byte NSWE_NORTH_WEST = NSWE_NORTH | NSWE_WEST;
	/** South-East NSWE flags */
	public static final byte NSWE_SOUTH_EAST = NSWE_SOUTH | NSWE_EAST;
	/** South-West NSWE flags */
	public static final byte NSWE_SOUTH_WEST = NSWE_SOUTH | NSWE_WEST;
	
	/** All directions NSWE flags */
	public static final byte NSWE_ALL = NSWE_EAST | NSWE_WEST | NSWE_SOUTH | NSWE_NORTH;
	
	private Cell()
	{
	}
}
