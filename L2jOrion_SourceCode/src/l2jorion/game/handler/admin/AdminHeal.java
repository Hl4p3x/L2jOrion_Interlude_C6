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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package l2jorion.game.handler.admin;

import l2jorion.Config;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

/**
 * This class handles following admin commands: - heal = restores HP/MP/CP on target, name or radius
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminHeal implements IAdminCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(AdminRes.class);
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_heal"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		/*
		 * if(!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel())){ return false; } if(Config.GMAUDIT) { Logger _logAudit = Logger.getLogger("gmaudit"); LogRecord record = new LogRecord(Level.INFO, command); record.setParameters(new Object[] { "GM: " +
		 * activeChar.getName(), " to target [" + activeChar.getTarget() + "] " }); _logAudit.LOGGER(record); }
		 */
		
		if (command.equals("admin_heal"))
		{
			handleRes(activeChar);
		}
		else if (command.startsWith("admin_heal"))
		{
			try
			{
				String healTarget = command.substring(11);
				handleRes(activeChar, healTarget);
				healTarget = null;
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Incorrect target/radius specified.");
				activeChar.sendPacket(sm);
				sm = null;
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleRes(final L2PcInstance activeChar)
	{
		handleRes(activeChar, null);
	}
	
	private void handleRes(final L2PcInstance activeChar, final String player)
	{
		L2Object obj = activeChar.getTarget();
		
		if (player != null)
		{
			L2PcInstance plyr = L2World.getInstance().getPlayer(player);
			
			if (plyr != null)
			{
				obj = plyr;
			}
			else
			{
				try
				{
					final int radius = Integer.parseInt(player);
					for (final L2Object object : activeChar.getKnownList().getKnownObjects().values())
					{
						if (object instanceof L2Character)
						{
							L2Character character = (L2Character) object;
							character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
							
							if (object instanceof L2PcInstance)
							{
								character.setCurrentCp(character.getMaxCp());
							}
							
							character = null;
						}
					}
					activeChar.sendMessage("Healed within " + radius + " unit radius.");
					return;
				}
				catch (final NumberFormatException nbe)
				{
					// ignore
				}
			}
			
			plyr = null;
		}
		
		if (obj == null)
		{
			obj = activeChar;
		}
		
		if (obj instanceof L2Character)
		{
			final L2Character target = (L2Character) obj;
			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());
			
			if (target instanceof L2PcInstance)
			{
				target.setCurrentCp(target.getMaxCp());
			}
			
			if (Config.DEBUG)
			{
				LOG.debug("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") healed character " + target.getName());
			}
			
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
		}
	}
}
