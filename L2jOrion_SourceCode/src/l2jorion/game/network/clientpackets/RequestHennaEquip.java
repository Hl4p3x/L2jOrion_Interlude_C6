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
import l2jorion.game.datatables.csv.HennaTable;
import l2jorion.game.datatables.sql.HennaTreeTable;
import l2jorion.game.model.actor.instance.L2HennaInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Henna;
import l2jorion.game.util.Util;

public final class RequestHennaEquip extends L2GameClientPacket
{
	private int _symbolId;
	
	/**
	 * packet type id 0xbb format: cd
	 */
	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
			return;
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("HennaEquip"))
			return;
		
		final L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);
		
		if (template == null)
			return;
		
		final L2HennaInstance temp = new L2HennaInstance(template);
		int _count = 0;
		
		/*
		 * Prevents henna drawing exploit: 1) talk to L2SymbolMakerInstance 2) RequestHennaList 3) Don't close the window and go to a GrandMaster and change your subclass 4) Get SymbolMaker range again and press draw You could draw any kind of henna just having the required subclass...
		 */
		boolean cheater = true;
		
		for (final L2HennaInstance h : HennaTreeTable.getInstance().getAvailableHenna(activeChar.getClassId()))
		{
			if (h.getSymbolId() == temp.getSymbolId())
			{
				cheater = false;
				break;
			}
		}
		
		if (activeChar.getInventory() != null && activeChar.getInventory().getItemByItemId(temp.getItemIdDye()) != null)
		{
			_count = activeChar.getInventory().getItemByItemId(temp.getItemIdDye()).getCount();
		}
		
		if (!cheater && _count >= temp.getAmountDyeRequire() && activeChar.getAdena() >= temp.getPrice() && activeChar.addHenna(temp))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
			sm.addNumber(temp.getItemIdDye());
			activeChar.sendPacket(sm);
			sm = null;
			activeChar.sendPacket(new SystemMessage(SystemMessageId.SYMBOL_ADDED));
			
			// HennaInfo hi = new HennaInfo(temp,activeChar);
			// activeChar.sendPacket(hi);
			
			activeChar.getInventory().reduceAdena("Henna", temp.getPrice(), activeChar, activeChar.getLastFolkNPC());
			final L2ItemInstance dyeToUpdate = activeChar.getInventory().destroyItemByItemId("Henna", temp.getItemIdDye(), temp.getAmountDyeRequire(), activeChar, activeChar.getLastFolkNPC());
			
			// update inventory
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(activeChar.getInventory().getAdenaInstance());
			iu.addModifiedItem(dyeToUpdate);
			activeChar.sendPacket(iu);
			
			final ItemList il = new ItemList(getClient().getActiveChar(), true);
			sendPacket(il);
			
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_DRAW_SYMBOL));
			
			if (!activeChar.isGM() && cheater)
			{
				Util.handleIllegalPlayerAction(activeChar, "Exploit attempt: Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " tryed to add a forbidden henna.", Config.DEFAULT_PUNISH);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] bc RequestHennaEquip";
	}
}
