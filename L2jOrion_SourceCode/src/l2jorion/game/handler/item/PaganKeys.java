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
package l2jorion.game.handler.item;

import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.util.random.Rnd;

public class PaganKeys implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		8273,
		8274,
		8275
	};
	public static final int INTERACTION_DISTANCE = 100;
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		
		final int itemId = item.getItemId();
		
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2Object target = activeChar.getTarget();
		
		if (!(target instanceof L2DoorInstance))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final L2DoorInstance door = (L2DoorInstance) target;
		
		target = null;
		
		if (!activeChar.isInsideRadius(door, INTERACTION_DISTANCE, false, false))
		{
			activeChar.sendMessage("Too far.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.getAbnormalEffect() > 0 || activeChar.isInCombat())
		{
			activeChar.sendMessage("You cannot use the key now.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int openChance = 35;
		
		if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
		{
			return;
		}
		
		switch (itemId)
		{
			case 8273: // AnteroomKey
				if (door.getDoorId() == 19160002 || door.getDoorId() == 19160003 || door.getDoorId() == 19160004 || door.getDoorId() == 19160005 || door.getDoorId() == 19160006 || door.getDoorId() == 19160007 || door.getDoorId() == 19160008 || door.getDoorId() == 19160009)
				{
					if (openChance > 0 && Rnd.get(100) < openChance)
					{
						activeChar.sendMessage("You opened Anterooms Door.");
						door.openMe();
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
					}
					else
					{
						// test with: activeChar.sendPacket(new SystemMessage(SystemMessage.FAILED_TO_UNLOCK_DOOR));
						activeChar.sendMessage("You failed to open Anterooms Door.");
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
						final PlaySound playSound = new PlaySound("interfacesound.system_close_01");
						activeChar.sendPacket(playSound);
					}
				}
				else
				{
					activeChar.sendMessage("Incorrect Door.");
				}
				break;
			case 8274: // Chapelkey, Capel Door has a Gatekeeper?? I use this key for Altar Entrance and Chapel_Door
				if (door.getDoorId() == 19160010 || door.getDoorId() == 19160011 || door.getDoorId() == 19160014 || door.getDoorId() == 19160015 || door.getDoorId() == 19160016 || door.getDoorId() == 19160017)
				{
					if (openChance > 0 && Rnd.get(100) < openChance)
					{
						activeChar.sendMessage("You opened Altar Entrance.");
						door.openMe();
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
					}
					else
					{
						activeChar.sendMessage("You failed to open Altar Entrance.");
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
						final PlaySound playSound = new PlaySound("interfacesound.system_close_01");
						activeChar.sendPacket(playSound);
					}
				}
				else
				{
					activeChar.sendMessage("Incorrect Door.");
				}
				break;
			case 8275: // Key of Darkness
				if (door.getDoorId() == 19160012 || door.getDoorId() == 19160013)
				{
					if (openChance > 0 && Rnd.get(100) < openChance)
					{
						activeChar.sendMessage("You opened Door of Darkness.");
						door.openMe();
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
					}
					else
					{
						activeChar.sendMessage("You failed to open Door of Darkness.");
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
						final PlaySound playSound = new PlaySound("interfacesound.system_close_01");
						activeChar.sendPacket(playSound);
					}
				}
				else
				{
					activeChar.sendMessage("Incorrect Door.");
				}
				break;
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}