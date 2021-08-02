/* L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.network.serverpackets;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.game.managers.CastleManorManager.CropProcure;
import l2jorion.game.model.L2Manor;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class ExShowSellCropList extends L2GameServerPacket
{
	private static final String _S__FE_21_EXSHOWSELLCROPLIST = "[S] FE:21 ExShowSellCropList";
	
	private int _manorId = 1;
	private final FastMap<Integer, L2ItemInstance> _cropsItems;
	private final FastMap<Integer, CropProcure> _castleCrops;
	
	public ExShowSellCropList(final L2PcInstance player, final int manorId, final FastList<CropProcure> crops)
	{
		_manorId = manorId;
		_castleCrops = new FastMap<>();
		_cropsItems = new FastMap<>();
		
		final FastList<Integer> allCrops = L2Manor.getInstance().getAllCrops();
		for (final int cropId : allCrops)
		{
			final L2ItemInstance item = player.getInventory().getItemByItemId(cropId);
			if (item != null)
			{
				_cropsItems.put(cropId, item);
			}
		}
		
		for (final CropProcure crop : crops)
		{
			if (_cropsItems.containsKey(crop.getId()) && crop.getAmount() > 0)
			{
				_castleCrops.put(crop.getId(), crop);
			}
		}
	}
	
	@Override
	public void runImpl()
	{
		// no long running
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0xFE);
		writeH(0x21);
		
		writeD(_manorId); // manor id
		writeD(_cropsItems.size()); // size
		
		for (final L2ItemInstance item : _cropsItems.values())
		{
			writeD(item.getObjectId()); // Object id
			writeD(item.getItemId()); // crop id
			writeD(L2Manor.getInstance().getSeedLevelByCrop(item.getItemId())); // seed level
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 1)); // reward 1 id
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 2)); // reward 2 id
			
			if (_castleCrops.containsKey(item.getItemId()))
			{
				final CropProcure crop = _castleCrops.get(item.getItemId());
				writeD(_manorId); // manor
				writeD(crop.getAmount()); // buy residual
				writeD(crop.getPrice()); // buy price
				writeC(crop.getReward()); // reward
			}
			else
			{
				writeD(0xFFFFFFFF); // manor
				writeD(0); // buy residual
				writeD(0); // buy price
				writeC(0); // reward
			}
			writeD(item.getCount()); // my crops
		}
	}
	
	@Override
	public String getType()
	{
		return _S__FE_21_EXSHOWSELLCROPLIST;
	}
}
