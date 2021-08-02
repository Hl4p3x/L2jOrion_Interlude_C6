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
package l2jorion.game.controllers;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import l2jorion.game.ai.additional.Zaken;
import l2jorion.game.managers.DayNightSpawnManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class GameTimeController extends Thread
{
	private static final Logger LOG = LoggerFactory.getLogger(GameTimeController.class);
	
	public static final int TICKS_PER_SECOND = 10;
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;
	public static final int IG_DAYS_PER_DAY = 6;
	public static final int MILLIS_PER_IG_DAY = (3600000 * 24) / IG_DAYS_PER_DAY;
	public static final int SECONDS_PER_IG_DAY = MILLIS_PER_IG_DAY / 1000;
	public static final int MINUTES_PER_IG_DAY = SECONDS_PER_IG_DAY / 60;
	public static final int TICKS_PER_IG_DAY = SECONDS_PER_IG_DAY * TICKS_PER_SECOND;
	public static final int TICKS_SUN_STATE_CHANGE = TICKS_PER_IG_DAY / 4;
	
	private static GameTimeController _instance;
	
	private final Set<L2Character> _movingObjects = ConcurrentHashMap.newKeySet();
	private final long _referenceTime;
	
	private GameTimeController()
	{
		super("GameTimeController");
		super.setDaemon(true);
		super.setPriority(MAX_PRIORITY);
		
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		_referenceTime = c.getTimeInMillis();
		
		super.start();
	}
	
	public static final void init()
	{
		_instance = new GameTimeController();
	}
	
	public final int getGameTime()
	{
		return (getGameTicks() % TICKS_PER_IG_DAY) / MILLIS_IN_TICK;
	}
	
	public final int getGameHour()
	{
		return getGameTime() / 60;
	}
	
	public final int getGameMinute()
	{
		return getGameTime() % 60;
	}
	
	public final boolean isNight()
	{
		return getGameHour() < 6;
	}
	
	public final int getGameTicks()
	{
		return (int) ((System.currentTimeMillis() - _referenceTime) / MILLIS_IN_TICK);
	}
	
	public final void registerMovingObject(final L2Character cha)
	{
		if (cha == null)
		{
			return;
		}
		
		_movingObjects.add(cha);
	}
	
	private final void moveObjects()
	{
		_movingObjects.removeIf(L2Character::updatePosition);
	}
	
	public final void stopTimer()
	{
		super.interrupt();
	}
	
	@Override
	public final void run()
	{
		long nextTickTime, sleepTime;
		boolean isNight = isNight();
		
		if (isNight)
		{
			ThreadPoolManager.getInstance().executeAi(() -> DayNightSpawnManager.getInstance().notifyChangeMode());
		}
		
		while (true)
		{
			nextTickTime = ((System.currentTimeMillis() / MILLIS_IN_TICK) * MILLIS_IN_TICK) + 100;
			
			try
			{
				moveObjects();
			}
			catch (final Throwable e)
			{
				LOG.warn("Unable to move objects:", e);
			}
			
			sleepTime = nextTickTime - System.currentTimeMillis();
			if (sleepTime > 0)
			{
				try
				{
					Thread.sleep(sleepTime);
				}
				catch (final InterruptedException e)
				{
					LOG.warn("InterruptedException:", e);
				}
			}
			
			if (getGameHour() == 0 && getGameMinute() == 0 && !Zaken.openingInitiated)
			{
				ThreadPoolManager.getInstance().executeAi(() -> Zaken.openCloseGates());
			}
			
			if (isNight() != isNight)
			{
				isNight = !isNight;
				
				ThreadPoolManager.getInstance().executeAi(() -> DayNightSpawnManager.getInstance().notifyChangeMode());
			}
		}
	}
	
	public static final GameTimeController getInstance()
	{
		return _instance;
	}
}
