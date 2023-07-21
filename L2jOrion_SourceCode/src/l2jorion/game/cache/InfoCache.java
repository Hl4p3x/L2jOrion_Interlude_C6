package l2jorion.game.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2jorion.game.model.L2DropData;

public class InfoCache
{
	private static final Map<Integer, List<L2DropData>> _droplistCache = new ConcurrentHashMap<>();
	
	public static void addToDroplistCache(final int id, final List<L2DropData> list)
	{
		_droplistCache.put(id, list);
	}
	
	public static List<L2DropData> getFromDroplistCache(final int id)
	{
		return _droplistCache.get(id);
	}
	
	public static void unload()
	{
		_droplistCache.clear();
	}
}
