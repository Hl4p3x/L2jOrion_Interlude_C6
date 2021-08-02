/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.util;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance.PunishLevel;
import l2jorion.game.network.L2GameClient;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.StringUtil;

/**
 * Flood protector implementation.
 * @author fordfrog
 */
public final class FloodProtectorAction
{
	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(FloodProtectorAction.class);
	/**
	 * Client for this instance of flood protector.
	 */
	private final L2GameClient client;
	/**
	 * Configuration of this instance of flood protector.
	 */
	private final FloodProtectorConfig config;
	/**
	 * Next game tick when new request is allowed.
	 */
	private volatile float _nextGameTick = GameTimeController.getInstance().getGameTicks();
	/**
	 * Request counter.
	 */
	private final AtomicInteger _count = new AtomicInteger(0);
	/**
	 * Flag determining whether exceeding request has been logged.
	 */
	private boolean _logged;
	/**
	 * Flag determining whether punishment application is in progress so that we do not apply punisment multiple times (flooding).
	 */
	private volatile boolean _punishmentInProgress;
	/**
	 * Count from when the floodProtector start to block next action.
	 */
	private final int _untilBlock = 4;
	
	/**
	 * Creates new instance of FloodProtectorAction.
	 * @param client for which flood protection is being created
	 * @param config flood protector configuration
	 */
	public FloodProtectorAction(final L2GameClient client, final FloodProtectorConfig config)
	{
		super();
		this.client = client;
		this.config = config;
	}
	
	private final Hashtable<String, AtomicInteger> received_commands_actions = new Hashtable<>();
	
	/**
	 * Checks whether the request is flood protected or not.
	 * @param command command issued or short command description
	 * @return true if action is allowed, otherwise false
	 */
	public boolean tryPerformAction(final String command)
	{
		// Ignore flood protector for GM char
		if (client != null && client.getActiveChar() != null && client.getActiveChar().isGM())
			return true;
		
		if (!config.ALTERNATIVE_METHOD)
		{
			
			final int curTick = GameTimeController.getInstance().getGameTicks();
			
			if (curTick < _nextGameTick || _punishmentInProgress)
			{
				if (config.LOG_FLOODING && !_logged)
				{
					LOG.warn(" called command " + command + " ~ " + String.valueOf((config.FLOOD_PROTECTION_INTERVAL - (_nextGameTick - curTick)) * GameTimeController.MILLIS_IN_TICK) + " ms after previous command");
					_logged = true;
				}
				
				_count.incrementAndGet();
				
				if (!_punishmentInProgress && config.PUNISHMENT_LIMIT > 0 && _count.get() >= config.PUNISHMENT_LIMIT && config.PUNISHMENT_TYPE != null)
				{
					_punishmentInProgress = true;
					
					if ("kick".equals(config.PUNISHMENT_TYPE))
					{
						kickPlayer();
					}
					else if ("ban".equals(config.PUNISHMENT_TYPE))
					{
						banAccount();
					}
					else if ("jail".equals(config.PUNISHMENT_TYPE))
					{
						jailChar();
					}
					else if ("banchat".equals(config.PUNISHMENT_TYPE))
					{
						banChat();
					}
					
					_punishmentInProgress = false;
				}
				
				// Avoid macro issue
				if (config.FLOOD_PROTECTOR_TYPE == "UseItemFloodProtector" || config.FLOOD_PROTECTOR_TYPE == "ServerBypassFloodProtector")
				{
					// _untilBlock value is 4
					if (_count.get() > _untilBlock)
						return false;
					
					return true;
				}
				
				return false;
			}
			
			if (_count.get() > 0)
			{
				if (config.LOG_FLOODING)
				{
					LOGGER(" issued ", String.valueOf(_count), " extra requests within ~", String.valueOf(config.FLOOD_PROTECTION_INTERVAL * GameTimeController.MILLIS_IN_TICK), " ms");
				}
			}
			
			_nextGameTick = curTick + config.FLOOD_PROTECTION_INTERVAL;
			_logged = false;
			_count.set(0);
			
			return true;
			
		}
		
		final int curTick = GameTimeController.getInstance().getGameTicks();
		
		if (curTick < _nextGameTick || _punishmentInProgress)
		{
			if (config.LOG_FLOODING && !_logged)
			{
				LOG.warn(" called command " + command + " ~ " + String.valueOf((config.FLOOD_PROTECTION_INTERVAL - (_nextGameTick - curTick)) * GameTimeController.MILLIS_IN_TICK) + " ms after previous command");
				_logged = true;
			}
			
			AtomicInteger command_count = null;
			if ((command_count = received_commands_actions.get(command)) == null)
			{
				command_count = new AtomicInteger(0);
				// received_commands_actions.put(command, command_count);
			}
			
			final int count = command_count.incrementAndGet();
			received_commands_actions.put(command, command_count);
			
			// _count.incrementAndGet();
			
			if (!_punishmentInProgress && config.PUNISHMENT_LIMIT > 0 && count >= config.PUNISHMENT_LIMIT && config.PUNISHMENT_TYPE != null)
			{
				_punishmentInProgress = true;
				
				if ("kick".equals(config.PUNISHMENT_TYPE))
				{
					kickPlayer();
				}
				else if ("ban".equals(config.PUNISHMENT_TYPE))
				{
					banAccount();
				}
				else if ("jail".equals(config.PUNISHMENT_TYPE))
				{
					jailChar();
				}
				
				_punishmentInProgress = false;
			}
			
			return false;
		}
		
		AtomicInteger command_count = null;
		if ((command_count = received_commands_actions.get(command)) == null)
		{
			command_count = new AtomicInteger(0);
			received_commands_actions.put(command, command_count);
		}
		
		if (command_count.get() > 0)
		{
			if (config.LOG_FLOODING)
			{
				LOG.warn(" issued " + String.valueOf(command_count) + " extra requests within ~ " + String.valueOf(config.FLOOD_PROTECTION_INTERVAL * GameTimeController.MILLIS_IN_TICK) + " ms");
			}
		}
		
		_nextGameTick = curTick + config.FLOOD_PROTECTION_INTERVAL;
		_logged = false;
		command_count.set(0);
		received_commands_actions.put(command, command_count);
		
		return true;
		
	}
	
	/**
	 * Kick player from game (close network connection).
	 */
	private void kickPlayer()
	{
		if (client.getActiveChar() != null)
			client.getActiveChar().logout();
		else
			client.closeNow();
		
		LOGGER("Client " + client.toString() + " kicked for flooding");
		
	}
	
	/**
	 * Kick player from game (close network connection).
	 */
	private void banChat()
	{
		if (client.getActiveChar() != null)
		{
			
			final L2PcInstance activeChar = client.getActiveChar();
			
			long newChatBanTime = 60000; // 1 minute
			if (activeChar.getPunishLevel() == PunishLevel.CHAT)
			{
				if (activeChar.getPunishTimer() <= (60000 * 3))
				{ // if less then 3 minutes (MAX CHAT BAN TIME), add 1 minute
					newChatBanTime += activeChar.getPunishTimer();
				}
				else
					newChatBanTime = activeChar.getPunishTimer();
				
			}
			
			activeChar.setPunishLevel(PunishLevel.CHAT, newChatBanTime);
			
		}
		
	}
	
	/**
	 * Bans char account and logs out the char.
	 */
	private void banAccount()
	{
		if (client.getActiveChar() != null)
		{
			client.getActiveChar().setPunishLevel(L2PcInstance.PunishLevel.ACC, config.PUNISHMENT_TIME);
			
			LOGGER(client.getActiveChar().getName() + " banned for flooding");
			
			client.getActiveChar().logout();
		}
		else
			LOGGER(" unable to ban account: no active player");
	}
	
	/**
	 * Jails char.
	 */
	private void jailChar()
	{
		if (client.getActiveChar() != null)
		{
			client.getActiveChar().setPunishLevel(L2PcInstance.PunishLevel.JAIL, config.PUNISHMENT_TIME);
			
			LOG.warn(client.getActiveChar().getName() + " jailed for flooding");
		}
		else
			LOGGER(" unable to jail: no active player");
	}
	
	private void LOGGER(final String... lines)
	{
		final StringBuilder output = StringUtil.startAppend(100, config.FLOOD_PROTECTOR_TYPE, ": ");
		String address = null;
		try
		{
			if (!client.isDetached())
				address = client.getConnection().getInetAddress().getHostAddress();
		}
		catch (final Exception e)
		{
		}
		
		switch (client.getState())
		{
			case IN_GAME:
				if (client.getActiveChar() != null)
				{
					StringUtil.append(output, client.getActiveChar().getName());
					StringUtil.append(output, "(", String.valueOf(client.getActiveChar().getObjectId()), ") ");
				}
			case AUTHED:
				if (client.getAccountName() != null)
					StringUtil.append(output, client.getAccountName(), " ");
			case CONNECTED:
				if (address != null)
					StringUtil.append(output, address);
				break;
			default:
				throw new IllegalStateException("Missing state on switch");
		}
		
		StringUtil.append(output, lines);
		LOG.warn(output.toString());
	}
}