package l2jorion.game.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.ArrayUtils;


public final class RunnableStatsManager
{
	protected static final Map<Class<?>, ClassStat> _classStats = new HashMap<>();
	
	private static final class ClassStat
	{
		private String[] _methodNames = new String[0];
		private MethodStat[] _methodStats = new MethodStat[0];
		
		protected ClassStat(final Class<?> clazz)
		{
			_classStats.put(clazz, this);
		}
		
		protected MethodStat getMethodStat(String methodName, final boolean synchronizedAlready)
		{
			for (int i = 0; i < _methodNames.length; i++)
				if (_methodNames[i].equals(methodName))
					return _methodStats[i];
			
			if (!synchronizedAlready)
			{
				synchronized (this)
				{
					return getMethodStat(methodName, true);
				}
			}
			
			methodName = methodName.intern();
			
			final MethodStat methodStat = new MethodStat();
			
			_methodNames = (String[]) ArrayUtils.add(_methodNames, methodName);
			_methodStats = (MethodStat[]) ArrayUtils.add(_methodStats, methodStat);
			
			return methodStat;
		}
	}
	
	protected static final class MethodStat
	{
		private final ReentrantLock _lock = new ReentrantLock();
		
		private long _min = Long.MAX_VALUE;
		private long _max = Long.MIN_VALUE;
		
		protected void handleStats(final long runTime)
		{
			_lock.lock();
			try
			{
				_min = Math.min(_min, runTime);
				_max = Math.max(_max, runTime);
			}
			finally
			{
				_lock.unlock();
			}
		}
	}
	
	private static ClassStat getClassStat(final Class<?> clazz, final boolean synchronizedAlready)
	{
		final ClassStat classStat = _classStats.get(clazz);
		
		if (classStat != null)
			return classStat;
		
		if (!synchronizedAlready)
		{
			synchronized (RunnableStatsManager.class)
			{
				return getClassStat(clazz, true);
			}
		}
		
		return new ClassStat(clazz);
	}
	
	public static void handleStats(final Class<? extends Runnable> clazz, final long runTime)
	{
		handleStats(clazz, "run()", runTime);
	}
	
	public static void handleStats(final Class<?> clazz, final String methodName, final long runTime)
	{
		getClassStat(clazz, false).getMethodStat(methodName, false).handleStats(runTime);
	}
}
