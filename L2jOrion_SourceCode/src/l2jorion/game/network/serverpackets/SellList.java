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

import java.util.List;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2MerchantInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;

public class SellList extends L2GameServerPacket
{
	private static final String _S__10_SELLLIST = "[S] 10 SellList";
	
	private final L2PcInstance _activeChar;
	private final L2MerchantInstance _lease;
	private final int _money;
	private final List<L2ItemInstance> _selllist = new FastList<>();
	
	public SellList(final L2PcInstance player)
	{
		_activeChar = player;
		_lease = null;
		_money = _activeChar.getAdena();
		doLease();
	}
	
	public SellList(final L2PcInstance player, final L2MerchantInstance lease)
	{
		_activeChar = player;
		_lease = lease;
		_money = _activeChar.getAdena();
		doLease();
	}
	
	private void doLease()
	{
		if (_lease == null)
		{
			for (final L2ItemInstance item : _activeChar.getInventory().getItems())
			{
				if (_activeChar.getLevel() < Config.PROTECTED_START_ITEMS_LVL && Config.LIST_PROTECTED_START_ITEMS.contains(item.getItemId()))
				{
					continue;
				}
				
				if (item != null && !item.isEquipped() && // Not equipped
					item.getItem().isSellable() && // Item is sellable
					item.getItem().getItemId() != 57 && // Adena is not sellable
					(_activeChar.getPet() == null || // Pet not summoned or
						item.getObjectId() != _activeChar.getPet().getControlItemId())) // Pet is summoned and not the item that summoned the pet
				{
					_selllist.add(item);
				}
			}
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x10);
		writeD(_money);
		writeD(_lease == null ? 0x00 : 1000000 + _lease.getTemplate().npcId);
		
		writeH(_selllist.size());
		
		for (final L2ItemInstance item : _selllist)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(0x00);
			writeH(0x00);
			
			if (_lease == null)
			{
				writeD(item.getItem().getReferencePrice() / 2);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__10_SELLLIST;
	}
}