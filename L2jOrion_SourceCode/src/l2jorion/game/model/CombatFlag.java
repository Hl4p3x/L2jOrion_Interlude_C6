/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.SystemMessage;

public class CombatFlag
{
	protected L2PcInstance _player = null;
	public int playerId = 0;
	private L2ItemInstance _item = null;
	
	private final Location _location;
	public L2ItemInstance itemInstance;
	
	private final int _itemId;
	
	public CombatFlag(/* int fort_id, */final int x, final int y, final int z, final int heading, final int item_id)
	{
		// _fortId = fort_id;
		_location = new Location(x, y, z, heading);
		// _heading = heading;
		_itemId = item_id;
	}
	
	public synchronized void spawnMe()
	{
		L2ItemInstance i;
		
		// Init the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
		i = ItemTable.getInstance().createItem("Combat", _itemId, 1, null, null);
		i.spawnMe(_location.getX(), _location.getY(), _location.getZ());
		itemInstance = i;
		i = null;
	}
	
	public synchronized void unSpawnMe()
	{
		if (_player != null)
		{
			dropIt();
		}
		
		if (itemInstance != null)
		{
			itemInstance.decayMe();
		}
	}
	
	public void activate(final L2PcInstance player, final L2ItemInstance item)
	{
		// if the player is mounted, attempt to unmount first. Only allow picking up
		// the comabt flag if unmounting is successful.
		if (player.isMounted())
		{
			if (!player.dismount())
			{
				player.sendMessage("You may not pick up this item while riding in this territory.");
				return;
			}
		}
		
		// Player holding it data
		_player = player;
		playerId = _player.getObjectId();
		itemInstance = null;
		
		// Add skill
		giveSkill();
		
		// Equip with the weapon
		_item = item;
		_player.getInventory().equipItemAndRecord(_item);
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
		sm.addItemName(_item.getItemId());
		_player.sendPacket(sm);
		sm = null;
		
		// Refresh inventory
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_item);
			_player.sendPacket(iu);
			iu = null;
		}
		else
		{
			_player.sendPacket(new ItemList(_player, false));
		}
		
		// Refresh player stats
		_player.broadcastUserInfo();
		// _player.setCombatFlagEquipped(true);
		
	}
	
	public void dropIt()
	{
		// Reset player stats
		// _player.setCombatFlagEquipped(false);
		removeSkill();
		_player.destroyItem("DieDrop", _item, null, false);
		_item = null;
		_player.broadcastUserInfo();
		_player = null;
		playerId = 0;
	}
	
	public void giveSkill()
	{
		_player.addSkill(SkillTable.getInstance().getInfo(3318, 1), false);
		_player.addSkill(SkillTable.getInstance().getInfo(3358, 1), false);
		_player.sendSkillList();
	}
	
	public void removeSkill()
	{
		_player.removeSkill(SkillTable.getInstance().getInfo(3318, 1), false);
		_player.removeSkill(SkillTable.getInstance().getInfo(3358, 1), false);
		_player.sendSkillList();
	}
	
}
