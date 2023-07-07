package l2jorion.game.geo.pathfinding.cellnodes;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import l2jorion.Config;

public class CellNodeBuffer
{
	private static final int MAX_ITERATIONS = Config.MAX_ITERATIONS;
	
	private final ReentrantLock _lock = new ReentrantLock();
	private final int _mapSize;
	private final CellNode[][] _buffer;
	
	private int _baseX = 0;
	private int _baseY = 0;
	
	private int _targetX = 0;
	private int _targetY = 0;
	private int _targetZ = 0;
	
	private long _timeStamp = 0;
	private long _lastElapsedTime = 0;
	
	private CellNode _current = null;
	
	public CellNodeBuffer(int size)
	{
		_mapSize = size;
		_buffer = new CellNode[_mapSize][_mapSize];
	}
	
	public final boolean lock()
	{
		return _lock.tryLock();
	}
	
	public final CellNode findPath(int x, int y, int z, int tx, int ty, int tz)
	{
		_timeStamp = System.currentTimeMillis();
		_baseX = x + ((tx - x - _mapSize) / 2); // middle of the line (x,y) - (tx,ty)
		_baseY = y + ((ty - y - _mapSize) / 2); // will be in the center of the buffer
		
		_targetX = tx;
		_targetY = ty;
		_targetZ = tz;
		
		_current = getNode(x, y, z);
		
		_current.setCost(getCost(x, y, z, Config.HIGH_WEIGHT));
		
		for (int count = 0; count < MAX_ITERATIONS; count++)
		{
			if ((_current.getLoc().getNodeX() == _targetX) && (_current.getLoc().getNodeY() == _targetY) && (Math.abs(_current.getLoc().getZ() - _targetZ) < 64))
			{
				return _current; // found
			}
			
			getNeighbors();
			
			if (_current.getNext() == null)
			{
				return null; // no more ways
			}
			
			_current = _current.getNext();
		}
		return null;
	}
	
	public final void free()
	{
		_current = null;
		
		CellNode node;
		for (int i = 0; i < _mapSize; i++)
		{
			for (int j = 0; j < _mapSize; j++)
			{
				node = _buffer[i][j];
				if (node != null)
				{
					node.free();
				}
			}
		}
		
		_lock.unlock();
		_lastElapsedTime = System.currentTimeMillis() - _timeStamp;
	}
	
	public final long getElapsedTime()
	{
		return _lastElapsedTime;
	}
	
	public final List<CellNode> debugPath()
	{
		final List<CellNode> result = new LinkedList<>();
		for (CellNode n = _current; n.getParent() != null; n = (CellNode) n.getParent())
		{
			result.add(n);
			n.setCost(-n.getCost());
		}
		
		for (int i = 0; i < _mapSize; i++)
		{
			for (int j = 0; j < _mapSize; j++)
			{
				CellNode n = _buffer[i][j];
				if ((n == null) || !n.isInUse() || (n.getCost() <= 0))
				{
					continue;
				}
				
				result.add(n);
			}
		}
		return result;
	}
	
	private final void getNeighbors()
	{
		if (_current.getLoc().canGoNone())
		{
			return;
		}
		
		final int x = _current.getLoc().getNodeX();
		final int y = _current.getLoc().getNodeY();
		final int z = _current.getLoc().getZ();
		
		CellNode nodeE = null;
		CellNode nodeS = null;
		CellNode nodeW = null;
		CellNode nodeN = null;
		
		// East
		if (_current.getLoc().canGoEast())
		{
			nodeE = addNode(x + 1, y, z, false);
		}
		
		// South
		if (_current.getLoc().canGoSouth())
		{
			nodeS = addNode(x, y + 1, z, false);
		}
		
		// West
		if (_current.getLoc().canGoWest())
		{
			nodeW = addNode(x - 1, y, z, false);
		}
		
		// North
		if (_current.getLoc().canGoNorth())
		{
			nodeN = addNode(x, y - 1, z, false);
		}
		
		if (Config.ADVANCED_DIAGONAL_STRATEGY)
		{
			// SouthEast
			if ((nodeE != null) && (nodeS != null))
			{
				if (nodeE.getLoc().canGoSouth() && nodeS.getLoc().canGoEast())
				{
					addNode(x + 1, y + 1, z, true);
				}
			}
			
			// SouthWest
			if ((nodeS != null) && (nodeW != null))
			{
				if (nodeW.getLoc().canGoSouth() && nodeS.getLoc().canGoWest())
				{
					addNode(x - 1, y + 1, z, true);
				}
			}
			
			// NorthEast
			if ((nodeN != null) && (nodeE != null))
			{
				if (nodeE.getLoc().canGoNorth() && nodeN.getLoc().canGoEast())
				{
					addNode(x + 1, y - 1, z, true);
				}
			}
			
			// NorthWest
			if ((nodeN != null) && (nodeW != null))
			{
				if (nodeW.getLoc().canGoNorth() && nodeN.getLoc().canGoWest())
				{
					addNode(x - 1, y - 1, z, true);
				}
			}
		}
	}
	
	private final CellNode getNode(int x, int y, int z)
	{
		final int aX = x - _baseX;
		if ((aX < 0) || (aX >= _mapSize))
		{
			return null;
		}
		
		final int aY = y - _baseY;
		if ((aY < 0) || (aY >= _mapSize))
		{
			return null;
		}
		
		CellNode result = _buffer[aX][aY];
		if (result == null)
		{
			result = new CellNode(new NodeLoc(x, y, z));
			_buffer[aX][aY] = result;
		}
		else if (!result.isInUse())
		{
			result.setInUse();
			// reinit node if needed
			if (result.getLoc() != null)
			{
				result.getLoc().set(x, y, z);
			}
			else
			{
				result.setLoc(new NodeLoc(x, y, z));
			}
		}
		
		return result;
	}
	
	private final CellNode addNode(int x, int y, int z, boolean diagonal)
	{
		CellNode newNode = getNode(x, y, z);
		if (newNode == null)
		{
			return null;
		}
		if (newNode.getCost() >= 0)
		{
			return newNode;
		}
		
		final int geoZ = newNode.getLoc().getZ();
		
		final int stepZ = Math.abs(geoZ - _current.getLoc().getZ());
		float weight = diagonal ? Config.DIAGONAL_WEIGHT : Config.LOW_WEIGHT;
		
		if (!newNode.getLoc().canGoAll() || (stepZ > 16))
		{
			weight = Config.HIGH_WEIGHT;
		}
		else
		{
			if (isHighWeight(x + 1, y, geoZ))
			{
				weight = Config.MEDIUM_WEIGHT;
			}
			else if (isHighWeight(x - 1, y, geoZ))
			{
				weight = Config.MEDIUM_WEIGHT;
			}
			else if (isHighWeight(x, y + 1, geoZ))
			{
				weight = Config.MEDIUM_WEIGHT;
			}
			else if (isHighWeight(x, y - 1, geoZ))
			{
				weight = Config.MEDIUM_WEIGHT;
			}
		}
		
		newNode.setParent(_current);
		newNode.setCost(getCost(x, y, geoZ, weight));
		
		CellNode node = _current;
		int count = 0;
		
		while ((node.getNext() != null) && (count < (MAX_ITERATIONS * 4)))
		{
			count++;
			if (node.getNext().getCost() > newNode.getCost())
			{
				// insert node into a chain
				newNode.setNext(node.getNext());
				break;
			}
			node = node.getNext();
		}
		
		if (count == (MAX_ITERATIONS * 4))
		{
			System.err.println("Pathfinding: too long loop detected, cost:" + newNode.getCost());
		}
		
		node.setNext(newNode); // add last
		
		return newNode;
	}
	
	private final boolean isHighWeight(int x, int y, int z)
	{
		final CellNode result = getNode(x, y, z);
		if (result == null)
		{
			return true;
		}
		
		if (!result.getLoc().canGoAll())
		{
			return true;
		}
		
		if (Math.abs(result.getLoc().getZ() - z) > 16)
		{
			return true;
		}
		
		return false;
	}
	
	private final double getCost(int x, int y, int z, float weight)
	{
		final int dX = x - _targetX;
		final int dY = y - _targetY;
		final int dZ = z - _targetZ;
		// Math.abs(dx) + Math.abs(dy) + Math.abs(dz) / 16
		double result = Math.sqrt((dX * dX) + (dY * dY) + ((dZ * dZ) / 256.0));
		if (result > weight)
		{
			result += weight;
		}
		
		if (result > Float.MAX_VALUE)
		{
			result = Float.MAX_VALUE;
		}
		
		return result;
	}
}