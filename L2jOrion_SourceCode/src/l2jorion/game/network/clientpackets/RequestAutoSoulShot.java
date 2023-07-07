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

import java.util.Arrays;

import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.SystemMessage;

public final class RequestAutoSoulShot extends PacketClient
{
	private int _itemId;
	private int _type; // 1 = on : 0 = off;
	
	@Override
	protected void readImpl()
	{
		_itemId = readD();
		_type = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		// Like L2OFF you can't use soulshots while sitting
		final int[] shots_ids =
		{
			5789,
			1835,
			1463,
			1464,
			1465,
			1466,
			1467,
			5790,
			2509,
			2510,
			2511,
			2512,
			2513,
			2514,
			3947,
			3948,
			3949,
			3950,
			3951,
			3952,
			10000, // Soulshot: D-grade
			10001, // Soulshot: C-grade
			10002, // Soulshot: B-grade
			10003, // Soulshot: A-grade
			10004, // Soulshot: S-grade
			10005, // Blessed Spiritshot: D-Grade
			10006, // Blessed Spiritshot: C-Grade
			10007, // Blessed Spiritshot: B-Grade
			10008, // Blessed Spiritshot: A-Grade
			10009, // Blessed Spiritshot: S Grade
			10010,
			10011
		};
		
		if (activeChar.isSitting() && Arrays.toString(shots_ids).contains(String.valueOf(_itemId)))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_AUTO_USE_LACK_OF_S1);
			sm.addItemName(_itemId);
			activeChar.sendPacket(sm);
			return;
		}
		
		if (activeChar.getPrivateStoreType() == 0 && activeChar.getActiveRequester() == null && !activeChar.isDead())
		{
			final L2ItemInstance item = activeChar.getInventory().getItemByItemId(_itemId);
			
			if (item != null)
			{
				if (_type == 1)
				{
					// Fishingshots are not automatic on retail
					if (_itemId < 6535 || _itemId > 6540)
					{
						activeChar.addAutoSoulShot(_itemId);
						
						// Attempt to charge first shot on activation
						if (_itemId == 6645 || _itemId == 6646 || _itemId == 6647)
						{
							// Like L2OFF you can active automatic SS only if you have a pet
							if (activeChar.getPet() != null)
							{
								// start the auto soulshot use
								final SystemMessage sm = new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO);
								sm.addString(item.getItemName());
								activeChar.sendPacket(sm);
								activeChar.rechargeAutoSoulShot(true, true, true);
							}
							else
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.NO_SERVITOR_CANNOT_AUTOMATE_USE);
								sm.addString(item.getItemName());
								activeChar.sendPacket(sm);
								return;
							}
						}
						else
						{
							if (activeChar.getActiveWeaponItem() != activeChar.getFistsWeaponItem() && item.getItem().getCrystalType() == activeChar.getActiveWeaponItem().getCrystalType())
							{
								/*
								 * if (((_itemId <= 10011) || (_itemId >= 10005 && _itemId <= 10009) || (_itemId >= 3947 && _itemId <= 3952)) && activeChar.isInOlympiadMode()) { final SystemMessage sm = new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
								 * sm.addString(item.getItemName()); activeChar.sendPacket(sm); } else {
								 */
								// start the auto soulshot use
								final SystemMessage sm = new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO);
								sm.addString(item.getItemName());
								activeChar.sendPacket(sm);
								// }
							}
							else
							{
								if (_itemId >= 2509 && _itemId <= 2514 || _itemId >= 3947 && _itemId <= 3952 || _itemId == 5790)
								{
									activeChar.sendPacket(new SystemMessage(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH));
								}
								else
								{
									activeChar.sendPacket(new SystemMessage(SystemMessageId.SOULSHOTS_GRADE_MISMATCH));
								}
							}
							activeChar.rechargeAutoSoulShot(true, true, false);
						}
					}
				}
				else if (_type == 0)
				{
					activeChar.removeAutoSoulShot(_itemId);
					final SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addString(item.getItemName());
					activeChar.sendPacket(sm);
				}
				
				final ExAutoSoulShot atk = new ExAutoSoulShot(_itemId, _type);
				activeChar.sendPacket(atk);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] CF RequestAutoSoulShot";
	}
}