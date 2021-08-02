package l2jorion.game.network.serverpackets;

import javolution.util.FastMap;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CastleManorManager;
import l2jorion.game.managers.CastleManorManager.CropProcure;
import l2jorion.game.model.entity.siege.Castle;

public class ExShowProcureCropDetail extends L2GameServerPacket
{
	private static final String _S__FE_22_EXSHOWPROCURECROPDETAIL = "[S] FE:22 ExShowProcureCropDetail";
	
	private final int _cropId;
	private final FastMap<Integer, CropProcure> _castleCrops;
	
	public ExShowProcureCropDetail(final int cropId)
	{
		_cropId = cropId;
		_castleCrops = new FastMap<>();
		
		for (final Castle c : CastleManager.getInstance().getCastles())
		{
			final CropProcure cropItem = c.getCrop(_cropId, CastleManorManager.PERIOD_CURRENT);
			if (cropItem != null && cropItem.getAmount() > 0)
			{
				_castleCrops.put(c.getCastleId(), cropItem);
			}
		}
	}
	
	@Override
	public void runImpl()
	{
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0xFE);
		writeH(0x22);
		
		writeD(_cropId); // crop id
		writeD(_castleCrops.size()); // size
		
		for (final int manorId : _castleCrops.keySet())
		{
			final CropProcure crop = _castleCrops.get(manorId);
			writeD(manorId); // manor name
			writeD(crop.getAmount()); // buy residual
			writeD(crop.getPrice()); // buy price
			writeC(crop.getReward()); // reward type
		}
	}
	
	@Override
	public String getType()
	{
		return _S__FE_22_EXSHOWPROCURECROPDETAIL;
	}
	
}
