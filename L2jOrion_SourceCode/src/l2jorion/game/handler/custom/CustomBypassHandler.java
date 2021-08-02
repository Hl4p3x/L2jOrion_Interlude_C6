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
package l2jorion.game.handler.custom;

import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.idfactory.BitSetIDFactory;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Rebirth;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class CustomBypassHandler
{
	private static Logger LOG = LoggerFactory.getLogger(BitSetIDFactory.class);
	
	private static CustomBypassHandler _instance = null;
	private final Map<String, ICustomByPassHandler> _handlers;
	
	private CustomBypassHandler()
	{
		_handlers = new FastMap<>();
		
		registerCustomBypassHandler(new ExtractableByPassHandler());
	}
	
	public static CustomBypassHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new CustomBypassHandler();
		}
		
		return _instance;
	}
	
	public void registerCustomBypassHandler(final ICustomByPassHandler handler)
	{
		for (final String s : handler.getByPassCommands())
		{
			_handlers.put(s, handler);
		}
	}
	
	public void handleBypass(final L2PcInstance player, final String command)
	{
		String cmd = "";
		String params = "";
		final int iPos = command.indexOf(" ");
		if (iPos != -1)
		{
			cmd = command.substring(7, iPos);
			params = command.substring(iPos + 1);
		}
		else
		{
			cmd = command.substring(7);
		}
		final ICustomByPassHandler ch = _handlers.get(cmd);
		if (ch != null)
		{
			ch.handleCommand(cmd, player, params);
		}
		else
		{
			if (command.startsWith("custom_rebirth"))
			{
				// Check to see if Rebirth is enabled to avoid hacks
				if (!Config.REBIRTH_ENABLE)
				{
					LOG.warn("Player " + player.getName() + " is trying to use rebirth system when it's disabled.");
					return;
				}
				
				Rebirth.getInstance().handleCommand(player, command);
			}
		}
	}
}