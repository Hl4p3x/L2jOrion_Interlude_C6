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
package l2jorion.game.model.entity.event.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;

import l2jorion.Config;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class EventsGlobalTask implements Runnable
{
	protected static final Logger LOG = LoggerFactory.getLogger(EventsGlobalTask.class);
	
	private static EventsGlobalTask instance;
	
	private boolean destroy = false;
	
	private final Hashtable<String, ArrayList<EventTask>> time_to_tasks = new Hashtable<>(); // time is in hh:mm
	private final Hashtable<String, ArrayList<EventTask>> eventid_to_tasks = new Hashtable<>();
	
	private EventsGlobalTask()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(this, 5000);
	}
	
	public static EventsGlobalTask getInstance()
	{
		if (instance == null)
		{
			instance = new EventsGlobalTask();
		}
		
		return instance;
	}
	
	public void registerNewEventTask(final EventTask event)
	{
		if (event == null || event.getEventIdentifier() == null || event.getEventIdentifier().equals("") || event.getEventStartTime() == null || event.getEventStartTime().equals(""))
		{
			LOG.error("registerNewEventTask: eventTask must be not null as its identifier and startTime ");
			return;
		}
		
		ArrayList<EventTask> savedTasksForTime = time_to_tasks.get(event.getEventStartTime());
		ArrayList<EventTask> savedTasksForId = eventid_to_tasks.get(event.getEventIdentifier());
		
		if (savedTasksForTime != null)
		{
			if (!savedTasksForTime.contains(event))
			{
				savedTasksForTime.add(event);
			}
		}
		else
		{
			
			savedTasksForTime = new ArrayList<>();
			savedTasksForTime.add(event);
			
		}
		
		time_to_tasks.put(event.getEventStartTime(), savedTasksForTime);
		
		if (savedTasksForId != null)
		{
			
			if (!savedTasksForId.contains(event))
			{
				savedTasksForId.add(event);
			}
		}
		else
		{
			
			savedTasksForId = new ArrayList<>();
			savedTasksForId.add(event);
		}
		
		eventid_to_tasks.put(event.getEventIdentifier(), savedTasksForId);
		
		if (Config.DEBUG)
		{
			LOG.info("Added Event: " + event.getEventIdentifier());
			
			// check Info
			for (final String time : time_to_tasks.keySet())
			{
				final ArrayList<EventTask> tasks = time_to_tasks.get(time);
				
				final Iterator<EventTask> taskIt = tasks.iterator();
				
				while (taskIt.hasNext())
				{
					final EventTask actual_event = taskIt.next();
					LOG.info("	--Registered Event: " + actual_event.getEventIdentifier());
				}
				
			}
			
			for (final String event_id : eventid_to_tasks.keySet())
			{
				
				LOG.info("--Event: " + event_id);
				final ArrayList<EventTask> times = eventid_to_tasks.get(event_id);
				
				final Iterator<EventTask> timesIt = times.iterator();
				
				while (timesIt.hasNext())
				{
					final EventTask actual_time = timesIt.next();
					LOG.info("	--Registered Time: " + actual_time.getEventStartTime());
				}
				
			}
		}
		
	}
	
	public void clearEventTasksByEventName(final String eventId)
	{
		if (eventId == null)
		{
			LOG.error("registerNewEventTask: eventTask must be not null as its identifier and startTime ");
			return;
		}
		
		if (eventId.equalsIgnoreCase("all"))
		{
			
			time_to_tasks.clear();
			eventid_to_tasks.clear();
			
		}
		else
		{
			
			final ArrayList<EventTask> oldTasksForId = eventid_to_tasks.get(eventId);
			
			if (oldTasksForId != null)
			{
				
				for (final EventTask actual : oldTasksForId)
				{
					final ArrayList<EventTask> oldTasksForTime = time_to_tasks.get(actual.getEventStartTime());
					
					if (oldTasksForTime != null)
					{
						oldTasksForTime.remove(actual);
						
						time_to_tasks.put(actual.getEventStartTime(), oldTasksForTime);
					}
				}
				eventid_to_tasks.remove(eventId);
			}
		}
	}
	
	public void deleteEventTask(final EventTask event)
	{
		
		if (event == null || event.getEventIdentifier() == null || event.getEventIdentifier().equals("") || event.getEventStartTime() == null || event.getEventStartTime().equals(""))
		{
			LOG.error("registerNewEventTask: eventTask must be not null as its identifier and startTime ");
			return;
		}
		
		if (this.time_to_tasks.size() < 0)
		{
			return;
		}
		
		final ArrayList<EventTask> oldTasksForId = eventid_to_tasks.get(event.getEventIdentifier());
		final ArrayList<EventTask> oldTasksForTime = time_to_tasks.get(event.getEventStartTime());
		
		if (oldTasksForId != null)
		{
			oldTasksForId.remove(event);
			eventid_to_tasks.put(event.getEventIdentifier(), oldTasksForId);
		}
		
		if (oldTasksForTime != null)
		{
			oldTasksForTime.remove(event);
			time_to_tasks.put(event.getEventStartTime(), oldTasksForTime);
		}
	}
	
	private void checkRegisteredEvents()
	{
		if (this.time_to_tasks.size() < 0)
		{
			return;
		}
		
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		
		final int hour = calendar.get(Calendar.HOUR_OF_DAY);
		final int min = calendar.get(Calendar.MINUTE);
		
		String hourStr = "";
		String minStr = "";
		
		if (hour < 10)
		{
			hourStr = "0" + hour;
		}
		else
		{
			hourStr = "" + hour;
		}
		
		if (min < 10)
		{
			minStr = "0" + min;
		}
		else
		{
			minStr = "" + min;
		}
		
		final String currentTime = hourStr + ":" + minStr;
		
		final ArrayList<EventTask> registeredEventsAtCurrentTime = time_to_tasks.get(currentTime);
		
		if (registeredEventsAtCurrentTime != null)
		{
			for (final EventTask actualEvent : registeredEventsAtCurrentTime)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(actualEvent, 5000);
			}
		}
	}
	
	public void destroyLocalInstance()
	{
		destroy = true;
		instance = null;
	}
	
	@Override
	public void run()
	{
		
		while (!destroy)
		{// start time checker
			
			checkRegisteredEvents();
			
			try
			{
				Thread.sleep(60000); // 1 minute
			}
			catch (final InterruptedException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			
		}
	}
	
}