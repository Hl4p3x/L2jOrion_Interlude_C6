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

package l2jorion.game.handler.item;

import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2GourdInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;

public class JackpotSeed implements IItemHandler
{
	private L2GourdInstance _gourd = null;
	
	private static int[] _itemIds =
	{
		6389, // small seed
		6390, // large seed
		5922,
		5923,
		5924,
		5925,
		5926
	};
	
	private static int[] _npcIds =
	{
		12774, // Young Pumpkin
		12777, // Large Young Pumpkin
		21316,
		21317,
		21318,
		21319,
		21320
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		L2PcInstance activeChar = (L2PcInstance) playable;
		
		if (activeChar.getInstanceId() != 0)
		{
			activeChar.sendMessage("You can't use item in this zone.");
			return;
		}
		
		if (_gourd != null && activeChar.getName().equalsIgnoreCase(_gourd.getOwner()))
		{
			activeChar.sendMessage("At first, kill your first one.");
			return;
		}
		
		L2NpcTemplate template = null;
		final int itemId = item.getItemId();
		
		for (int i = 0; i < _itemIds.length; i++)
		{
			if (_itemIds[i] == itemId)
			{
				template = NpcTable.getInstance().getTemplate(_npcIds[i]);
				break;
			}
		}
		
		if (template == null)
		{
			return;
		}
		
		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setId(IdFactory.getInstance().getNextId());
			
			spawn.setLocx(activeChar.getX());
			spawn.setLocy(activeChar.getY());
			spawn.setLocz(activeChar.getZ());
			
			_gourd = (L2GourdInstance) spawn.spawnOne();
			L2World.getInstance().storeObject(_gourd);
			_gourd.setOwner(activeChar.getName());
			
			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
			
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			sm.addString("Created " + template.getName() + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz());
			activeChar.sendPacket(sm);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			sm.addString("Target is not ingame.");
			activeChar.sendPacket(sm);
		}
		
	}
	
	@Override
	public int[] getItemIds()
	{
		return _itemIds;
	}
}
