package l2jorion.game.cache;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;

public class WarehouseCacheManager
{
	private static WarehouseCacheManager _instance;
	
	protected final FastMap<L2PcInstance, Long> _cachedWh;
	protected final long _cacheTime;
	
	public static WarehouseCacheManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new WarehouseCacheManager();
		}
		
		return _instance;
	}
	
	private WarehouseCacheManager()
	{
		_cacheTime = Config.WAREHOUSE_CACHE_TIME * 60000L;
		_cachedWh = new FastMap<L2PcInstance, Long>().shared();
		
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new CacheScheduler(), 120000, 60000);
	}
	
	public void addCacheTask(final L2PcInstance pc)
	{
		_cachedWh.put(pc, System.currentTimeMillis());
	}
	
	public void remCacheTask(final L2PcInstance pc)
	{
		_cachedWh.remove(pc);
	}
	
	public class CacheScheduler implements Runnable
	{
		@Override
		public void run()
		{
			final long cTime = System.currentTimeMillis();
			for (final L2PcInstance pc : _cachedWh.keySet())
			{
				if (cTime - _cachedWh.get(pc) > _cacheTime)
				{
					pc.clearWarehouse();
					
					_cachedWh.remove(pc);
				}
			}
		}
	}
}