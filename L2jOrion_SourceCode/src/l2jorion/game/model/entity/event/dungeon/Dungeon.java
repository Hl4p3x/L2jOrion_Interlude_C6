package l2jorion.game.model.entity.event.dungeon;

import java.util.ArrayList;
import java.util.List;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.sql.TeleportLocationTable;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2TeleportLocation;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.event.manager.EventTask;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Dungeon implements EventTask, IVoicedCommandHandler, ICustomByPassHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(Dungeon.class);
	
	private static String _eventName = Config.DG_NAME;
	
	private static boolean _joining = false, _aborted = false, _inProgress = false;
	
	protected static int _joinTime = Config.DG_EVENT_TIME;
	
	static List<L2PcInstance> playersInside = new ArrayList<>();
	
	private String startEventTime;
	
	private class EscapeFinalizer implements Runnable
	{
		L2PcInstance _player;
		L2TeleportLocation _tp;
		
		public EscapeFinalizer(L2PcInstance player, L2TeleportLocation loc)
		{
			_player = player;
			_tp = loc;
		}
		
		@Override
		public void run()
		{
			_player.enableAllSkills();
			
			if (!playersInside.contains((_player)))
			{
				playersInside.add(_player);
			}
			_player.setInstanceId(Config.PVP_ZONE_INSTANCE_ID);
			_player.teleToLocation(_tp.getLocX(), _tp.getLocY(), _tp.getLocZ(), true);
		}
		
	}
	
	public static Dungeon getNewInstance()
	{
		return new Dungeon();
	}
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (activeChar.isInsideZone(ZoneId.ZONE_RANDOM) || activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("You can't use it from this zone.");
			return false;
		}
		
		if (command.equalsIgnoreCase("boom"))
		{
			showMessageWindow(activeChar);
			return true;
		}
		
		return false;
	}
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		if (!is_inProgress())
		{
			player.sendMessage("The zone is not opened yet.");
			return;
		}
		
		if (player.isInsideZone(ZoneId.ZONE_RANDOM) || player.isInOlympiadMode())
		{
			player.sendMessage("You can't use it from this zone.");
			return;
		}
		
		int unstuckTimer = PowerPackConfig.GLOBALGK_TIMEOUT * 1000;
		
		if (parameters.startsWith("goto"))
		{
			try
			{
				int locId = Integer.parseInt(parameters.substring(parameters.indexOf(" ") + 1).trim());
				L2TeleportLocation tpPoint = TeleportLocationTable.getInstance().getTemplate(locId);
				
				if (tpPoint != null)
				{
					if (PowerPackConfig.GLOBALGK_PRICE > 0 && player.getLevel() > Config.FREE_TELEPORT_UNTIL + 1)
					{
						if (player.getInventory().getAdena() < PowerPackConfig.GLOBALGK_PRICE)
						{
							player.sendMessage("You don't have enough Adena.");
							player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							return;
						}
						player.reduceAdena("teleport", PowerPackConfig.GLOBALGK_PRICE, null, true);
					}
					
					if (PowerPackConfig.GLOBALGK_PRICE == -1 && player.getLevel() > Config.FREE_TELEPORT_UNTIL + 1)
					{
						if (player.getInventory().getAdena() < tpPoint.getPrice())
						{
							player.sendMessage("You don't have enough Adena.");
							player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							return;
						}
						player.reduceAdena("teleport", tpPoint.getPrice(), null, true);
					}
					
					if (unstuckTimer > 0 && !(player.isInsideZone(ZoneId.ZONE_PEACE)))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						
						player.setTarget(player);
						player.broadcastPacket(new MagicSkillUser(player, 1050, 1, unstuckTimer, 0));
						player.sendPacket(new SetupGauge(0, unstuckTimer));
						player.setTarget(null);
						
						player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(player, tpPoint), unstuckTimer));
						player.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
						
						return;
					}
					
					if (!playersInside.contains((player)))
					{
						playersInside.add(player);
					}
					player.setInstanceId(Config.PVP_ZONE_INSTANCE_ID);
					player.teleToLocation(tpPoint.getLocX(), tpPoint.getLocY(), tpPoint.getLocZ(), true);
					return;
				}
				player.sendMessage("Teleport, with ID " + locId + " does not exist in the database");
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (parameters.startsWith("exit"))
		{
			if (player.getInstanceId() == 0)
			{
				player.sendMessage("You're not in zone.");
				return;
			}
			
			player.setInstanceId(0);
			if (playersInside.contains((player)))
			{
				playersInside.remove(player);
			}
			
			player.teleToLocation(new Location(83431, 148331, -3400), 50, false);
		}
	}
	
	public static void showMessageWindow(L2PcInstance player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
		String text = HtmCache.getInstance().getHtm("data/html/mods/dungeon/main.htm");
		htm.setHtml(text);
		player.sendPacket(htm);
	}
	
	public void setEventStartTime(final String newTime)
	{
		startEventTime = newTime;
	}
	
	@Override
	public String getEventIdentifier()
	{
		return _eventName;
	}
	
	@Override
	public void run()
	{
		LOG.info("Dungeon: Event notification start");
		eventOnceStart();
	}
	
	public static String get_eventName()
	{
		return _eventName;
	}
	
	@Override
	public String getEventStartTime()
	{
		return startEventTime;
	}
	
	public static void eventOnceStart()
	{
		if (startJoin() && !_aborted)
		{
			if (_joinTime > 0)
			{
				waiter(_joinTime * 60 * 1000); // minutes for join event
			}
			else if (_joinTime <= 0)
			{
				abortEvent();
				return;
			}
		}
	}
	
	public static void abortEvent()
	{
		if (!_joining)
		{
			return;
		}
		
		if (_joining)
		{
			cleanDG();
			_joining = false;
			_inProgress = false;
			Announcements.getInstance().gameAnnounceToAll(_eventName + ": The zone closed.");
			return;
		}
		
		_joining = false;
		_aborted = true;
		
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": The zone closed.");
	}
	
	public static boolean startJoin()
	{
		_inProgress = true;
		_joining = true;
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Zone is opened for " + (Config.DG_EVENT_TIME) + " minutes");
		Announcements.getInstance().gameAnnounceToAll(_eventName + ": Info command .boom");
		
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					showMessageWindow(player);
				}
			}
		}
		
		return true;
	}
	
	private static void waiter(final long interval)
	{
		final long startWaiterTime = System.currentTimeMillis();
		int seconds = (int) (interval / 1000);
		
		while (startWaiterTime + interval > System.currentTimeMillis() && !_aborted)
		{
			seconds--; // Here because we don't want to see two time announce at the same time
			
			if (_joining)
			{
				switch (seconds)
				{
					case 3600: // 1 hour left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 / 60 + " hour(s) till zone close!");
						}
						break;
					case 1800: // 30 minutes left
					case 900: // 15 minutes left
					case 600: // 10 minutes left
					case 300: // 5 minutes left
					case 240: // 4 minutes left
					case 180: // 3 minutes left
					case 120: // 2 minutes left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 + " minutes till zone close!");
						}
						break;
					case 60: // 1 minute left
						
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds / 60 + " minute till zone close!");
						}
						break;
					case 30: // 30 seconds left
					case 15: // 15 seconds left
					case 10: // 10 seconds left
					case 3: // 3 seconds left
					case 2: // 2 seconds left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " seconds till zone close!");
						}
						break;
					case 1: // 1 seconds left
						if (_joining)
						{
							Announcements.getInstance().gameAnnounceToAll(_eventName + ": " + seconds + " second till zone close!");
						}
						break;
					case 0:
						abortEvent();
						getPlayers().stream().forEach(player -> player.setInstanceId(0));
						getPlayers().stream().forEach(player -> player.teleToLocation(new Location(185412, 20475, -3265), 50, false));
						getPlayers().clear();
						break;
				}
			}
			
			final long startOneSecondWaiterStartTime = System.currentTimeMillis();
			
			// Only the try catch with Thread.sleep(1000) give bad countdown on high wait times
			while (startOneSecondWaiterStartTime + 1000 > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (final InterruptedException ie)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						ie.printStackTrace();
					}
				}
			}
		}
	}
	
	public static void cleanDG()
	{
		_inProgress = false;
	}
	
	public static boolean is_inProgress()
	{
		return _inProgress;
	}
	
	@Override
	public String[] getByPassCommands()
	{
		return new String[]
		{
			"dg"
		};
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			"boom"
		};
	}
	
	public static List<L2PcInstance> getPlayers()
	{
		return playersInside;
	}
}