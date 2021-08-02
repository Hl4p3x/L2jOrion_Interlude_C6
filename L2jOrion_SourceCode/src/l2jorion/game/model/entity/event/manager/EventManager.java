/*
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class EventManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(EventManager.class.getName());
	
	private final static String EVENT_MANAGER_CONFIGURATION_FILE = "./config/events/eventmanager.ini";
	
	public static boolean TVT_EVENT_ENABLED;
	public static ArrayList<String> TVT_TIMES_LIST;
	
	public static boolean CTF_EVENT_ENABLED;
	public static ArrayList<String> CTF_TIMES_LIST;
	
	public static boolean DM_EVENT_ENABLED;
	public static ArrayList<String> DM_TIMES_LIST;
	
	public static boolean POLL_ENABLED;
	
	private static EventManager instance = null;
	
	private EventManager()
	{
		loadConfiguration();
	}
	
	public static EventManager getInstance()
	{
		if (instance == null)
		{
			instance = new EventManager();
		}
		return instance;
		
	}
	
	public static void loadConfiguration()
	{
		
		InputStream is = null;
		try
		{
			Properties eventSettings = new Properties();
			is = new FileInputStream(new File(EVENT_MANAGER_CONFIGURATION_FILE));
			eventSettings.load(is);
			
			TVT_EVENT_ENABLED = Boolean.parseBoolean(eventSettings.getProperty("TVTEventEnabled", "false"));
			TVT_TIMES_LIST = new ArrayList<>();
			String[] propertySplit;
			propertySplit = eventSettings.getProperty("TVTStartTime", "").split(";");
			for (String time : propertySplit)
			{
				TVT_TIMES_LIST.add(time);
			}
			
			CTF_EVENT_ENABLED = Boolean.parseBoolean(eventSettings.getProperty("CTFEventEnabled", "false"));
			CTF_TIMES_LIST = new ArrayList<>();
			propertySplit = eventSettings.getProperty("CTFStartTime", "").split(";");
			for (String time : propertySplit)
			{
				CTF_TIMES_LIST.add(time);
			}
			
			DM_EVENT_ENABLED = Boolean.parseBoolean(eventSettings.getProperty("DMEventEnabled", "false"));
			DM_TIMES_LIST = new ArrayList<>();
			propertySplit = eventSettings.getProperty("DMStartTime", "").split(";");
			for (String time : propertySplit)
			{
				DM_TIMES_LIST.add(time);
			}
			
			POLL_ENABLED = Boolean.parseBoolean(eventSettings.getProperty("PollEnabled", "false"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
		}
		finally
		{
			if (is != null)
			{
				try
				{
					is.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public void startEventRegistration()
	{
		if (TVT_EVENT_ENABLED)
		{
			registerTvT();
		}
		
		if (CTF_EVENT_ENABLED)
		{
			registerCTF();
		}
		
		if (DM_EVENT_ENABLED)
		{
			registerDM();
		}
	}
	
	public static void registerTvT()
	{
		TvT.loadData();
		
		if (!TvT.checkStartJoinOk())
		{
			LOG.warn("registerTvT: TvT Event is not setted Properly");
		}
		
		// clear all tvt
		EventsGlobalTask.getInstance().clearEventTasksByEventName(TvT.get_eventName());
		
		for (String time : TVT_TIMES_LIST)
		{
			TvT newInstance = TvT.getNewInstance();
			newInstance.setEventStartTime(time);
			EventsGlobalTask.getInstance().registerNewEventTask(newInstance);
		}
	}
	
	public static void registerCTF()
	{
		CTF.loadData();
		
		if (!CTF.checkStartJoinOk())
		{
			LOG.warn("registerCTF: CTF Event is not setted Properly");
		}
		
		// clear all tvt
		EventsGlobalTask.getInstance().clearEventTasksByEventName(CTF.get_eventName());
		
		for (String time : CTF_TIMES_LIST)
		{
			
			CTF newInstance = CTF.getNewInstance();
			newInstance.setEventStartTime(time);
			EventsGlobalTask.getInstance().registerNewEventTask(newInstance);
		}
		
	}
	
	public static void registerDM()
	{
		DM.loadData();
		
		if (!DM.checkStartJoinOk())
		{
			LOG.warn("registerDM: DM Event is not setted Properly");
		}
		
		// clear all tvt
		EventsGlobalTask.getInstance().clearEventTasksByEventName(DM.get_eventName());
		
		for (String time : DM_TIMES_LIST)
		{
			DM newInstance = DM.getNewInstance();
			newInstance.setEventStartTime(time);
			EventsGlobalTask.getInstance().registerNewEventTask(newInstance);
		}
	}
}
