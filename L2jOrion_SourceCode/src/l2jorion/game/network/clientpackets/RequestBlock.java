/* This program is free software; you can redistribute it and/or modify
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

import l2jorion.game.datatables.sql.CharNameTable;
import l2jorion.game.model.BlockList;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestBlock extends PacketClient
{
	private static Logger LOG = LoggerFactory.getLogger(RequestBlock.class.getName());
	
	private final static int BLOCK = 0;
	private final static int UNBLOCK = 1;
	private final static int BLOCKLIST = 2;
	private final static int ALLBLOCK = 3;
	private final static int ALLUNBLOCK = 4;
	
	private String _name;
	private Integer _type;
	
	@Override
	protected void readImpl()
	{
		_type = readD(); // 0x00 - block, 0x01 - unblock, 0x03 - allblock, 0x04 - allunblock
		
		if (_type == BLOCK || _type == UNBLOCK)
			_name = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		final int targetId = CharNameTable.getInstance().getIdByName(_name);
		final int targetAL = CharNameTable.getInstance().getAccessLevelById(targetId);
		
		if (activeChar == null)
			return;
		
		switch (_type)
		{
			case BLOCK:
			case UNBLOCK:
				// can't use block/unblock for locating invisible characters
				if (targetId <= 0)
				{
					// Incorrect player name.
					activeChar.sendPacket(SystemMessageId.FAILED_TO_REGISTER_TO_IGNORE_LIST);
					return;
				}
				
				if (targetAL > 0)
				{
					// Cannot block a GM character.
					activeChar.sendPacket(SystemMessageId.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_GM);
					return;
				}
				
				if (activeChar.getObjectId() == targetId)
					return;
				
				if (_type == BLOCK)
					BlockList.addToBlockList(activeChar, targetId);
				else
					BlockList.removeFromBlockList(activeChar, targetId);
				break;
			case BLOCKLIST:
				BlockList.sendListToOwner(activeChar);
				break;
			case ALLBLOCK:
				activeChar.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);// Update by rocknow
				BlockList.setBlockAll(activeChar, true);
				break;
			case ALLUNBLOCK:
				activeChar.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);// Update by rocknow
				BlockList.setBlockAll(activeChar, false);
				break;
			default:
				LOG.info("Unknown 0x0a block type: " + _type);
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] A0 RequestBlock";
	}
}