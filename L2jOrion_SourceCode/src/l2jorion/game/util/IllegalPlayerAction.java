/*
 * $Header: IllegalPlayerAction.java, 21/10/2005 23:32:02 luisantonioa Exp $
 * 
 * $Author: luisantonioa $ $Date: 21/10/2005 23:32:02 $ $Revision: 1 $ $Log:
 * IllegalPlayerAction.java,v $ Revision 1 21/10/2005 23:32:02 luisantonioa
 * Added copyright notice
 * 
 * 
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
package l2jorion.game.util;

import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.log.Log;

public final class IllegalPlayerAction implements Runnable
{
	// private static Logger _logAudit = Logger.getLogger("audit");
	private final String _message;
	private final int _punishment;
	private final L2PcInstance _actor;
	
	public static final int PUNISH_BROADCAST = 1;
	public static final int PUNISH_KICK = 2;
	public static final int PUNISH_KICKBAN = 3;
	public static final int PUNISH_JAIL = 4;
	
	public IllegalPlayerAction(final L2PcInstance actor, final String message, final int punishment)
	{
		_message = message;
		_punishment = punishment;
		_actor = actor;
		
		switch (punishment)
		{
			case PUNISH_KICK:
				_actor.sendMessage("You will be kicked for illegal action, GM informed.");
				break;
			case PUNISH_KICKBAN:
				_actor.setAccessLevel(-100);
				_actor.setAccountAccesslevel(-100);
				_actor.sendMessage("You are banned for illegal action, GM informed.");
				break;
			case PUNISH_JAIL:
				_actor.sendMessage("Illegal action performed!");
				_actor.sendMessage("You will be teleported to GM Consultation Service area and jailed.");
				break;
		}
	}
	
	@Override
	public void run()
	{
		// final LogRecord record = new LogRecord(Level.INFO, "AUDIT:" + _message);
		// record.setLoggerName("audit");
		// record.setParameters(new Object[]
		// {
		// _actor,
		// _punishment
		// });
		// _logAudit.warn("AUDIT:" + _actor + "" + _message + "" + _punishment);
		String text = "AUDIT:" + _actor + "" + _message + "" + _punishment;
		Log.add(text, "Audit");
		
		GmListTable.broadcastMessageToGMs(_message);
		
		switch (_punishment)
		{
			case PUNISH_BROADCAST:
				return;
			case PUNISH_KICK:
				_actor.logout(true);
				break;
			case PUNISH_KICKBAN:
				_actor.logout(true);
				break;
			case PUNISH_JAIL:
				_actor.setPunishLevel(L2PcInstance.PunishLevel.JAIL, Config.DEFAULT_PUNISH_PARAM);
				break;
		}
	}
}
