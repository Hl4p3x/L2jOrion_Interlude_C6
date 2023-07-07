package l2jorion.game.geo.pathfinding;

import java.util.List;

import l2jorion.game.geo.pathfinding.cellnodes.CellPathFinding;
import l2jorion.game.model.L2World;

public abstract class PathFinding
{
	public static PathFinding getInstance()
	{
		// if (Config.GEODATA)
		// {
		// Higher Memory Usage, Smaller Cpu Usage
		// return GeoPathFinding.getInstance();
		// }
		
		return CellPathFinding.getInstance();
	}
	
	public abstract boolean pathNodesExist(short regionoffset);
	
	public abstract List<AbstractNodeLoc> findPath(int x, int y, int z, int tx, int ty, int tz, int instanceId, boolean playable);
	
	public short getNodePos(int geo_pos)
	{
		return (short) (geo_pos >> 3);
	}
	
	public short getNodeBlock(int node_pos)
	{
		return (short) (node_pos % 256);
	}
	
	public byte getRegionX(int node_pos)
	{
		return (byte) ((node_pos >> 8) + L2World.TILE_X_MIN);
	}
	
	public byte getRegionY(int node_pos)
	{
		return (byte) ((node_pos >> 8) + L2World.TILE_Y_MIN);
	}
	
	public short getRegionOffset(byte rx, byte ry)
	{
		return (short) ((rx << 5) + ry);
	}
	
	public int calculateWorldX(short node_x)
	{
		return L2World.MAP_MIN_X + (node_x * 128) + 48;
	}
	
	public int calculateWorldY(short node_y)
	{
		return L2World.MAP_MIN_Y + (node_y * 128) + 48;
	}
	
	public String[] getStat()
	{
		return null;
	}
}
