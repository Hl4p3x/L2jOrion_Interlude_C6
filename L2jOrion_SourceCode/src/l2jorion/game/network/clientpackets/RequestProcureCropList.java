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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CastleManorManager;
import l2jorion.game.managers.CastleManorManager.CropProcure;
import l2jorion.game.model.L2Manor;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2ManorManagerInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;

public class RequestProcureCropList extends PacketClient
{
	private static final String _C__D0_09_REQUESTPROCURECROPLIST = "[C] D0:09 RequestProcureCropList";
	
	private static final int BATCH_LENGTH = 16; // length of the one item
	
	private Crop[] _items = null;
	
	@Override
	protected void readImpl()
	{
		int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;
		
		_items = new Crop[count];
		for (int i = 0; i < count; i++)
		{
			int objId = readD();
			int itemId = readD();
			int manorId = readD();
			int cnt = readD();
			if (objId < 1 || itemId < 1 || manorId < 0 || cnt < 0)
			{
				_items = null;
				return;
			}
			_items[i] = new Crop(objId, itemId, manorId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
			return;
		
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		L2Object manager = player.getLastFolkNPC();
		if (!(manager instanceof L2ManorManagerInstance))
			return;
		
		if (!player.isInsideRadius(manager, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			return;
		
		int castleId = ((L2ManorManagerInstance) manager).getCastle().getCastleId();
		
		// Calculate summary values
		int slots = 0;
		int weight = 0;
		
		for (Crop i : _items)
		{
			if (!i.getCrop())
				continue;
			
			L2Item template = ItemTable.getInstance().getTemplate(i.getReward());
			weight += i.getCount() * template.getWeight();
			
			if (!template.isStackable())
				slots += i.getCount();
			else if (player.getInventory().getItemByItemId(i.getItemId()) == null)
				slots++;
		}
		
		if (!player.getInventory().validateWeight(weight))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}
		
		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}
		
		// Proceed the purchase
		for (Crop i : _items)
		{
			if (i.getReward() == 0)
				continue;
			
			int fee = i.getFee(castleId); // fee for selling to other manors
			
			int rewardPrice = ItemTable.getInstance().getTemplate(i.getReward()).getReferencePrice();
			if (rewardPrice == 0)
				continue;
			
			int rewardItemCount = i.getPrice() / rewardPrice;
			if (rewardItemCount < 1)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1).addItemName(i.getItemId()).addNumber(i.getCount()));
				continue;
			}
			
			if (player.getAdena() < fee)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1).addItemName(i.getItemId()).addNumber(i.getCount()));
				player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				continue;
			}
			
			// check if player have correct items count
			L2ItemInstance item = player.getInventory().getItemByObjectId(i.getObjectId());
			if (item == null || item.getCount() < i.getCount())
				continue;
			
			// try modify castle crop
			if (!i.setCrop())
				continue;
			
			if (fee > 0 && !player.reduceAdena("Manor", fee, manager, true))
				continue;
			
			SystemMessage sm = new SystemMessage(SystemMessageId.TRADED_S2_OF_CROP_S1);
			sm.addItemName(i.getItemId());
			sm.addNumber(i.getCount());
			player.sendPacket(sm);
			
			if (!player.destroyItem("Manor", i.getObjectId(), i.getCount(), manager, true))
				continue;
			
			player.addItem("Manor", i.getReward(), rewardItemCount, manager, true);
		}
	}
	
	private static class Crop
	{
		private final int _objectId;
		private final int _itemId;
		private final int _manorId;
		private final int _count;
		private int _reward = 0;
		private CropProcure _crop = null;
		
		public Crop(int obj, int id, int m, int num)
		{
			_objectId = obj;
			_itemId = id;
			_manorId = m;
			_count = num;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public int getCount()
		{
			return _count;
		}
		
		public int getReward()
		{
			return _reward;
		}
		
		public int getPrice()
		{
			return _crop.getPrice() * _count;
		}
		
		public int getFee(int castleId)
		{
			if (_manorId == castleId)
				return 0;
			
			return (getPrice() / 100) * 5; // 5% fee for selling to other manor
		}
		
		public boolean getCrop()
		{
			try
			{
				_crop = CastleManager.getInstance().getCastleById(_manorId).getCrop(_itemId, CastleManorManager.PERIOD_CURRENT);
			}
			catch (NullPointerException e)
			{
				return false;
			}
			
			if (_crop == null || _crop.getId() == 0 || _crop.getPrice() == 0 || _count == 0)
				return false;
			
			if (_count > _crop.getAmount())
				return false;
			
			if ((Integer.MAX_VALUE / _count) < _crop.getPrice())
				return false;
			
			_reward = L2Manor.getInstance().getRewardItem(_itemId, _crop.getReward());
			return true;
		}
		
		public boolean setCrop()
		{
			synchronized (_crop)
			{
				int amount = _crop.getAmount();
				if (_count > amount)
					return false; // not enough crops
					
				_crop.setAmount(amount - _count);
			}
			
			if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				CastleManager.getInstance().getCastleById(_manorId).updateCrop(_itemId, _crop.getAmount(), CastleManorManager.PERIOD_CURRENT);
			
			return true;
		}
	}
	
	@Override
	public String getType()
	{
		return _C__D0_09_REQUESTPROCURECROPLIST;
	}
}
