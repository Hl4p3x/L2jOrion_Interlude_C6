package l2jorion.game.taskmanager;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class KnownListUpdateTaskManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(KnownListUpdateTaskManager.class);
	
	private final static int FULL_UPDATE_TIMER = 100;
	public static boolean updatePass = true;
	
	public static int _fullUpdateTimer = FULL_UPDATE_TIMER;
	
	public static final Set<L2WorldRegion> _failedRegions = ConcurrentHashMap.newKeySet(1);
	
	public KnownListUpdateTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new KnownListUpdate(), 1000, Config.KNOWNLIST_UPDATE_INTERVAL);
	}
	
	private class KnownListUpdate implements Runnable
	{
		public KnownListUpdate()
		{
		}
		
		@Override
		public void run()
		{
			try
			{
				boolean failed;
				for (L2WorldRegion regions[] : L2World.getInstance().getAllWorldRegions())
				{
					for (L2WorldRegion region : regions) // go through all world regions
					{
						// avoid stopping update if something went wrong in updateRegion()
						try
						{
							failed = _failedRegions.contains(region); // failed on last pass
							if (region.isActive()) // and check only if the region is active
							{
								updateRegion(region, (_fullUpdateTimer == FULL_UPDATE_TIMER || failed), updatePass);
							}
							if (failed)
							{
								_failedRegions.remove(region); // if all ok, remove
							}
						}
						catch (Exception e)
						{
							LOG.warn("KnownListUpdateTaskManager: updateRegion(" + _fullUpdateTimer + "," + updatePass + ") failed for region " + region.getName() + ". Full update scheduled. " + e.getMessage(), e);
							_failedRegions.add(region);
						}
					}
				}
				updatePass = !updatePass;
				
				if (_fullUpdateTimer > 0)
				{
					_fullUpdateTimer--;
				}
				else
				{
					_fullUpdateTimer = FULL_UPDATE_TIMER;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void updateRegion(L2WorldRegion region, boolean fullUpdate, boolean forgetObjects)
	{
		Collection<L2Object> vObj = region.getVisibleObjects().values();
		for (L2Object object : vObj)
		{
			if (object == null || !object.isVisible())
			{
				continue;
			}
			
			final boolean factionMobs = (object instanceof L2MonsterInstance && ((L2MonsterInstance) object).getFactionId() != null);
			
			if (forgetObjects)
			{
				object.getKnownList().forgetObjects(fullUpdate || factionMobs);
				continue;
			}
			
			for (L2WorldRegion regi : region.getSurroundingRegions())
			{
				if (object instanceof L2PlayableInstance || factionMobs && regi.isActive() || fullUpdate)
				{
					Collection<L2Object> inrObj = regi.getVisibleObjects().values();
					for (L2Object _object : inrObj)
					{
						if (_object != object)
						{
							object.getKnownList().addKnownObject(_object);
						}
					}
				}
				else if (object instanceof L2Character)
				{
					if (regi.isActive())
					{
						Collection<L2PlayableInstance> inrPls = regi.getVisiblePlayable().values();
						for (L2Object _object : inrPls)
						{
							if (_object != object)
							{
								object.getKnownList().addKnownObject(_object);
							}
						}
					}
				}
			}
		}
	}
	
	public static KnownListUpdateTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final KnownListUpdateTaskManager _instance = new KnownListUpdateTaskManager();
	}
}
