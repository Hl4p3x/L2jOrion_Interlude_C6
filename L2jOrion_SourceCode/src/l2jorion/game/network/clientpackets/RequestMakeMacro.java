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
import l2jorion.game.model.L2Macro;
import l2jorion.game.model.L2Macro.L2MacroCmd;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestMakeMacro extends PacketClient
{
	private final Logger LOG = LoggerFactory.getLogger(RequestMakeMacro.class);
	private L2Macro _macro;
	private int _commandsLenght = 0;
	private static final int MAX_MACRO_LENGTH = 12;
	
	/**
	 * packet type id 0xc1 sample c1 d // id S // macro name S // unknown desc S // unknown acronym c // icon c // count c // entry c // type d // skill id c // shortcut id S // command name format: cdSSScc (ccdcS)
	 */
	@Override
	protected void readImpl()
	{
		final int _id = readD();
		final String _name = readS();
		final String _desc = readS();
		final String _acronym = readS();
		final int _icon = readC();
		int _count = readC();
		if (_count < 0)
		{
			_count = 0;
			return;
		}
		if (_count > MAX_MACRO_LENGTH)
		{
			_count = MAX_MACRO_LENGTH;
		}
		
		final L2MacroCmd[] commands = new L2MacroCmd[_count];
		if (Config.DEBUG)
		{
			LOG.info("Make macro id:" + _id + "\tname:" + _name + "\tdesc:" + _desc + "\tacronym:" + _acronym + "\ticon:" + _icon + "\tcount:" + _count);
		}
		for (int i = 0; i < _count; i++)
		{
			final int entry = readC();
			final int type = readC(); // 1 = skill, 3 = action, 4 = shortcut
			final int d1 = readD(); // skill or page number for shortcuts
			final int d2 = readC();
			final String command = readS();
			_commandsLenght += command.length() + 1;
			commands[i] = new L2MacroCmd(entry, type, d1, d2, command);
			if (Config.DEBUG)
			{
				LOG.info("entry:" + entry + "\ttype:" + type + "\td1:" + d1 + "\td2:" + d2 + "\tcommand:" + command);
			}
		}
		_macro = new L2Macro(_id, _icon, _name, _desc, _acronym, commands);
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();

		if (player == null)
			return;
		/*if (player.isSubmitingPin())
		{
			player.sendMessage("Unable to do any action while PIN is not submitted");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}*/
		// Macro exploit fix
		if (!getClient().getFloodProtectors().getMacro().tryPerformAction("make macro"))
			return;

		if (_commandsLenght > 255)
		{
			// Invalid macro. Refer to the Help file for instructions.
			player.sendPacket(new SystemMessage(SystemMessageId.INVALID_MACRO));
			return;
		}

		if (player.getMacroses().getAllMacroses().length > 24)
		{
			// You may create up to 24 macros.
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_CREATE_UP_TO_24_MACROS));
			return;
		}

		if (_macro.name.length() == 0)
		{
			// Enter the name of the macro.
			player.sendPacket(new SystemMessage(SystemMessageId.ENTER_THE_MACRO_NAME));
			return;
		}

		if (_macro.descr.length() > 32)
		{
			// Macro descriptions may contain up to 32 characters.
			player.sendPacket(new SystemMessage(SystemMessageId.MACRO_DESCRIPTION_MAX_32_CHARS));
			return;
		}

		// Security Check
		for (final L2MacroCmd command : _macro.commands)
		{
			
			if (!checkSecurityOnCommand(command))
			{
				
				// Invalid macro. Refer to the Help file for instructions.
				player.sendPacket(new SystemMessage(SystemMessageId.INVALID_MACRO));
				player.sendMessage("SecurityCheck: not more then 2x ',' or 2x ';' in the same command");
				return;
				
			}
			
		}
		
		player.registerMacro(_macro);
	}

	private boolean checkSecurityOnCommand(final L2MacroCmd cmd)
	{
		
		// not more then 2x ;
		if (cmd.cmd != null && cmd.cmd.split(";").length > 2)
		{
			return false;
		}
		
		// not more then 2x ,
		if (cmd.cmd != null && cmd.cmd.split(",").length > 2)
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	public String getType()
	{
		return "[C] C1 RequestMakeMacro";
	}
}
