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

import java.util.ArrayList;

import l2jorion.game.managers.CastleManorManager.CropProcure;
import l2jorion.game.model.L2Manor;
import l2jorion.game.network.PacketServer;

public class ExShowCropInfo extends PacketServer
{
	private static final String _S__FE_1C_EXSHOWSEEDINFO = "[S] FE:1D ExShowCropInfo";
	
	private ArrayList<CropProcure> _crops;
	private final int _manorId;
	
	public ExShowCropInfo(final int manorId, final ArrayList<CropProcure> crops)
	{
		_manorId = manorId;
		_crops = crops;
		if (_crops == null)
		{
			_crops = new ArrayList<>();
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE); // Id
		writeH(0x1D); // SubId
		writeC(0);
		writeD(_manorId); // Manor ID
		writeD(0);
		writeD(_crops.size());
		for (final CropProcure crop : _crops)
		{
			writeD(crop.getId()); // Crop id
			writeD(crop.getAmount()); // Buy residual
			writeD(crop.getStartAmount()); // Buy
			writeD(crop.getPrice()); // Buy price
			writeC(crop.getReward()); // Reward
			writeD(L2Manor.getInstance().getSeedLevelByCrop(crop.getId())); // Seed Level
			writeC(1); // rewrad 1 Type
			writeD(L2Manor.getInstance().getRewardItem(crop.getId(), 1)); // Rewrad 1 Type Item Id
			writeC(1); // rewrad 2 Type
			writeD(L2Manor.getInstance().getRewardItem(crop.getId(), 2)); // Rewrad 2 Type Item Id
		}
	}
	
	@Override
	public String getType()
	{
		return _S__FE_1C_EXSHOWSEEDINFO;
	}
	
}
