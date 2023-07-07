package l2jorion.game.geo.geodriver.blocks;

import java.nio.ByteBuffer;

import l2jorion.game.geo.geodriver.IBlock;

public class MultilayerBlock implements IBlock
{
	private final byte[] _data;
	
	public MultilayerBlock(ByteBuffer bb)
	{
		int start = bb.position();
		
		for (int blockCellOffset = 0; blockCellOffset < IBlock.BLOCK_CELLS; blockCellOffset++)
		{
			byte nLayers = bb.get();
			if ((nLayers <= 0) || (nLayers > 125))
			{
				throw new RuntimeException("L2JGeoDriver: Geo file corrupted! Invalid layers count!");
			}
			
			bb.position(bb.position() + (nLayers * 2));
		}
		
		_data = new byte[bb.position() - start];
		bb.position(start);
		bb.get(_data);
	}
	
	private short _getNearestLayer(int geoX, int geoY, int worldZ)
	{
		int startOffset = _getCellDataOffset(geoX, geoY);
		byte nLayers = _data[startOffset];
		int endOffset = startOffset + 1 + (nLayers * 2);
		
		// 1 layer at least was required on loading so this is set at least once on the loop below
		int nearestDZ = 0;
		short nearestData = 0;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			short layerData = _extractLayerData(offset);
			int layerZ = _extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				// exact z
				return layerData;
			}
			
			int layerDZ = Math.abs(layerZ - worldZ);
			if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ))
			{
				nearestDZ = layerDZ;
				nearestData = layerData;
			}
		}
		
		return nearestData;
	}
	
	private int _getCellDataOffset(int geoX, int geoY)
	{
		int cellLocalOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
		int cellDataOffset = 0;
		// move index to cell, we need to parse on each request, OR we parse on creation and save indexes
		for (int i = 0; i < cellLocalOffset; i++)
		{
			cellDataOffset += 1 + (_data[cellDataOffset] * 2);
		}
		// now the index points to the cell we need
		
		return cellDataOffset;
	}
	
	private short _extractLayerData(int dataOffset)
	{
		return (short) ((_data[dataOffset] & 0xFF) | (_data[dataOffset + 1] << 8));
	}
	
	private int _getNearestNSWE(int geoX, int geoY, int worldZ)
	{
		return _extractLayerNswe(_getNearestLayer(geoX, geoY, worldZ));
	}
	
	private int _extractLayerNswe(short layer)
	{
		return (byte) (layer & 0x000F);
	}
	
	private int _extractLayerHeight(short layer)
	{
		layer = (short) (layer & 0x0fff0);
		return layer >> 1;
	}
	
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		return (_getNearestNSWE(geoX, geoY, worldZ) & nswe) == nswe;
	}
	
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		return _extractLayerHeight(_getNearestLayer(geoX, geoY, worldZ));
	}
	
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		int startOffset = _getCellDataOffset(geoX, geoY);
		byte nLayers = _data[startOffset];
		int endOffset = startOffset + 1 + (nLayers * 2);
		
		int lowerZ = Integer.MIN_VALUE;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			short layerData = _extractLayerData(offset);
			
			int layerZ = _extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				// exact z
				return layerZ;
			}
			
			if ((layerZ < worldZ) && (layerZ > lowerZ))
			{
				lowerZ = layerZ;
			}
		}
		
		return lowerZ == Integer.MIN_VALUE ? worldZ : lowerZ;
	}
	
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		int startOffset = _getCellDataOffset(geoX, geoY);
		byte nLayers = _data[startOffset];
		int endOffset = startOffset + 1 + (nLayers * 2);
		
		int higherZ = Integer.MAX_VALUE;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			short layerData = _extractLayerData(offset);
			
			int layerZ = _extractLayerHeight(layerData);
			if (layerZ == worldZ)
			{
				// exact z
				return layerZ;
			}
			
			if ((layerZ > worldZ) && (layerZ < higherZ))
			{
				higherZ = layerZ;
			}
		}
		
		return higherZ == Integer.MAX_VALUE ? worldZ : higherZ;
	}
}
