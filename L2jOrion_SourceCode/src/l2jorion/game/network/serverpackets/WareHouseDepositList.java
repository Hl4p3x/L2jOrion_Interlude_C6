/*
 * L2jOrion Project - www.l2jorion.com 
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
import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class WareHouseDepositList extends L2GameServerPacket
{
	private static Logger LOG = LoggerFactory.getLogger(WareHouseDepositList.class);
	
	private static final String _S__53_WAREHOUSEDEPOSITLIST = "[S] 41 WareHouseDepositList";
	
	public static final int PRIVATE = 1;
	public static final int CLAN = 2;
	public static final int CASTLE = 3; // not sure
	public static final int FREIGHT = 4; // not sure
	
	private final L2PcInstance _activeChar;
	private final int _playerAdena;
	private final FastList<L2ItemInstance> _items;
	private final int _whType;
	
	public WareHouseDepositList(final L2PcInstance player, final int type)
	{
		_activeChar = player;
		_whType = type;
		_playerAdena = _activeChar.getAdena();
		_items = new FastList<>();
		
		for (final L2ItemInstance temp : _activeChar.getInventory().getAvailableItemsForPackage(true))
		{
			_items.add(temp);
		}
		
		// augmented and shadow items can be stored in private wh
		if (_whType == PRIVATE)
		{
			for (final L2ItemInstance temp : player.getInventory().getItems())
			{
				if (temp != null && !temp.isEquipped() && (temp.isShadowItem() || temp.isAugmented()))
				{
					_items.add(temp);
				}
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x41);
		
		writeH(_whType);
		writeD(_playerAdena);
		final int count = _items.size();
		if (Config.DEBUG)
		{
			LOG.debug("count:" + count);
		}
		writeH(count);
		
		for (final L2ItemInstance item : _items)
		{
			writeH(item.getItem().getType1()); // item type1 //unconfirmed, works
			writeD(item.getObjectId()); // unconfirmed, works
			writeD(item.getItemId()); // unconfirmed, works
			writeD(item.getCount()); // unconfirmed, works
			writeH(item.getItem().getType2()); // item type2 //unconfirmed, works
			writeH(0x00); // ? 100
			writeD(item.getItem().getBodyPart()); // ?
			writeH(item.getEnchantLevel()); // enchant level -confirmed
			writeH(0x00); // ? 300
			writeH(0x00); // ? 200
			writeD(item.getObjectId()); // item id - confimed
			if (item.isAugmented())
			{
				writeD(0x0000FFFF & item.getAugmentation().getAugmentationId());
				writeD(item.getAugmentation().getAugmentationId() >> 16);
			}
			else
			{
				writeQ(0x00);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__53_WAREHOUSEDEPOSITLIST;
	}
}
