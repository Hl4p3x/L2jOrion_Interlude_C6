package l2jorion.game.model.olympiad;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.type.L2OlympiadStadiumZone;

public class OlympiadGameManager implements Runnable
{
	private static final Logger _log = Logger.getLogger(OlympiadGameManager.class.getName());
	
	private volatile boolean _battleStarted = false;
	private final OlympiadGameTask[] _tasks;
	
	protected OlympiadGameManager()
	{
		final Collection<L2OlympiadStadiumZone> zones = ZoneManager.getInstance().getAllZones(L2OlympiadStadiumZone.class);
		if (zones == null || zones.isEmpty())
		{
			throw new Error("No olympiad stadium zones defined !");
		}
		
		_tasks = new OlympiadGameTask[zones.size()];
		int i = 0;
		for (L2OlympiadStadiumZone zone : zones)
		{
			_tasks[i++] = new OlympiadGameTask(zone);
		}
		
		_log.log(Level.INFO, "Olympiad: Loaded " + _tasks.length + " stadiums");
	}
	
	public static final OlympiadGameManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected final boolean isBattleStarted()
	{
		return _battleStarted;
	}
	
	protected final void startBattle()
	{
		_battleStarted = true;
	}
	
	@Override
	public final void run()
	{
		if (Olympiad.getInstance().isOlympiadEnd())
		{
			return;
		}
		
		if (Olympiad.getInstance().inCompPeriod())
		{
			OlympiadGameTask task;
			AbstractOlympiadGame newGame;
			
			List<List<Integer>> readyClassed = OlympiadManager.getInstance().hasEnoughRegisteredClassed();
			boolean readyNonClassed = OlympiadManager.getInstance().hasEnoughRegisteredNonClassed();
			
			if (readyClassed != null || readyNonClassed)
			{
				// set up the games queue
				for (int i = 0; i < _tasks.length; i++)
				{
					task = _tasks[i];
					synchronized (task)
					{
						if (!task.isRunning())
						{
							// Fair arena distribution
							// 0,2,4,6,8.. arenas checked for classed or teams first
							if ((readyClassed != null) && (i % 2) == 0)
							{
								// if no ready teams found check for classed
								newGame = OlympiadGameClassed.createGame(i, readyClassed);
								if (newGame != null)
								{
									task.attachGame(newGame);
									continue;
								}
								readyClassed = null;
							}
							// 1,3,5,7,9.. arenas used for non-classed
							// also other arenas will be used for non-classed if no classed or teams available
							if (readyNonClassed)
							{
								newGame = OlympiadGameNonClassed.createGame(i, OlympiadManager.getInstance().getRegisteredNonClassBased());
								if (newGame != null)
								{
									task.attachGame(newGame);
									continue;
								}
								readyNonClassed = false;
							}
						}
					}
					
					// stop generating games if no more participants
					if (readyClassed == null && !readyNonClassed)
					{
						break;
					}
				}
			}
		}
		else
		{
			// not in competition period
			if (isAllTasksFinished())
			{
				OlympiadManager.getInstance().clearRegistered();
				_battleStarted = false;
				// _log.log(Level.INFO, "Olympiad: All current games finished.");
			}
		}
	}
	
	public final boolean isAllTasksFinished()
	{
		for (OlympiadGameTask task : _tasks)
		{
			if (task.isRunning())
			{
				return false;
			}
		}
		return true;
	}
	
	public final OlympiadGameTask getOlympiadTask(int id)
	{
		if (id < 0 || id >= _tasks.length)
		{
			return null;
		}
		
		return _tasks[id];
	}
	
	public OlympiadGameTask[] getOlympiadTasks()
	{
		return _tasks;
	}
	
	public final int getNumberOfStadiums()
	{
		return _tasks.length;
	}
	
	public final void notifyCompetitorDamage(L2PcInstance player, int damage)
	{
		if (player == null)
		{
			return;
		}
		
		final int id = player.getOlympiadGameId();
		if (id < 0 || id >= _tasks.length)
		{
			return;
		}
		
		final AbstractOlympiadGame game = _tasks[id].getGame();
		if (game != null)
		{
			game.addDamage(player, damage);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final OlympiadGameManager _instance = new OlympiadGameManager();
	}
}