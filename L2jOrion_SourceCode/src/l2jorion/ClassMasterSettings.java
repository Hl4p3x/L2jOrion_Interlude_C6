package l2jorion;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class ClassMasterSettings
{
	private final Map<Integer, Map<Integer, Integer>> _claimItems;
	private final Map<Integer, Map<Integer, Integer>> _rewardItems;
	private final Map<Integer, Boolean> _allowedClassChange;
	
	public ClassMasterSettings(String _configLine)
	{
		_claimItems = new HashMap<>();
		_rewardItems = new HashMap<>();
		_allowedClassChange = new HashMap<>();
		if (_configLine != null)
		{
			parseConfigLine(_configLine.trim());
		}
	}
	
	private void parseConfigLine(String _configLine)
	{
		final StringTokenizer st = new StringTokenizer(_configLine, ";");
		while (st.hasMoreTokens())
		{
			final int job = Integer.parseInt(st.nextToken());
			_allowedClassChange.put(job, true);
			Map<Integer, Integer> _items = new HashMap<>();
			if (st.hasMoreTokens())
			{
				final StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
				while (st2.hasMoreTokens())
				{
					final StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
					final int _itemId = Integer.parseInt(st3.nextToken());
					final int _quantity = Integer.parseInt(st3.nextToken());
					_items.put(_itemId, _quantity);
				}
			}
			
			_claimItems.put(job, _items);
			_items = new HashMap<>();
			if (st.hasMoreTokens())
			{
				final StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
				
				while (st2.hasMoreTokens())
				{
					final StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
					final int _itemId = Integer.parseInt(st3.nextToken());
					final int _quantity = Integer.parseInt(st3.nextToken());
					_items.put(_itemId, _quantity);
				}
			}
			_rewardItems.put(job, _items);
		}
	}
	
	public boolean isAllowed(int job)
	{
		if (_allowedClassChange == null)
		{
			return false;
		}
		
		if (_allowedClassChange.containsKey(job))
		{
			return _allowedClassChange.get(job);
		}
		return false;
	}
	
	public Map<Integer, Integer> getRewardItems(int job)
	{
		if (_rewardItems.containsKey(job))
		{
			return _rewardItems.get(job);
		}
		return null;
	}
	
	public Map<Integer, Integer> getRequireItems(int job)
	{
		if (_claimItems.containsKey(job))
		{
			return _claimItems.get(job);
		}
		return null;
	}
}