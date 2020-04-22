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
package l2jorion.game.thread;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import l2jorion.Config;
import l2jorion.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadPoolManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(ThreadPoolManager.class);
	
	private static final class RunnableWrapper implements Runnable
	{
		private final Runnable _r;
		
		public RunnableWrapper(final Runnable r)
		{
			_r = r;
		}
		
		@Override
		public final void run()
		{
			try
			{
				_r.run();
			}
			catch (final Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				final Thread t = Thread.currentThread();
				final UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
				if (h != null)
				{
					h.uncaughtException(t, e);
				}
			}
		}
	}
	
	protected ScheduledThreadPoolExecutor _effectsScheduledThreadPool;
	protected ScheduledThreadPoolExecutor _generalScheduledThreadPool;
	protected ScheduledThreadPoolExecutor _aiScheduledThreadPool;
	private final ThreadPoolExecutor _generalPacketsThreadPool;
	private final ThreadPoolExecutor _ioPacketsThreadPool;
	private final ThreadPoolExecutor _generalThreadPool;
	
	/** temp workaround for VM issue */
	private static final long MAX_DELAY = Long.MAX_VALUE / 1000000 / 2;
	
	private boolean _shutdown;
	
	public static ThreadPoolManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected ThreadPoolManager()
	{
		_effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_EFFECTS, new PriorityThreadFactory("EffectsSTPool", Thread.NORM_PRIORITY));
		_generalScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_GENERAL, new PriorityThreadFactory("GeneralSTPool", Thread.NORM_PRIORITY));
		_ioPacketsThreadPool = new ThreadPoolExecutor(Config.IO_PACKET_THREAD_CORE_SIZE, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("I/O Packet Pool", Thread.NORM_PRIORITY + 1));
		_generalPacketsThreadPool = new ThreadPoolExecutor(Config.GENERAL_PACKET_THREAD_CORE_SIZE, Config.GENERAL_PACKET_THREAD_CORE_SIZE + 2, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("Normal Packet Pool", Thread.NORM_PRIORITY + 1));
		_generalThreadPool = new ThreadPoolExecutor(Config.GENERAL_THREAD_CORE_SIZE, Config.GENERAL_THREAD_CORE_SIZE + 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("General Pool", Thread.NORM_PRIORITY));
		_aiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.AI_MAX_THREAD, new PriorityThreadFactory("AISTPool", Thread.NORM_PRIORITY));
		
		scheduleGeneralAtFixedRate(new PurgeTask(), 10 * 60 * 1000L, 5 * 60 * 1000L);
	}
	
	public static long validateDelay(long delay)
	{
		if (delay < 0)
			delay = 0;
		else if (delay > MAX_DELAY)
			delay = MAX_DELAY;
		return delay;
	}
	
	public ScheduledFuture<?> scheduleEffect(final Runnable r, long delay)
	{
		if (_effectsScheduledThreadPool.isShutdown())
			return null;
		
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			return _effectsScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch (final RejectedExecutionException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return null;
		}
	}
	
	public ScheduledFuture<?> scheduleEffectAtFixedRate(final Runnable r, long initial, long delay)
	{
		if (_effectsScheduledThreadPool.isShutdown())
			return null;
		
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			initial = ThreadPoolManager.validateDelay(initial);
			return _effectsScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch (final RejectedExecutionException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return null; /* shutdown, ignore */
		}
	}
	
	@Deprecated
	public boolean removeEffect(final RunnableScheduledFuture<?> r)
	{
		return _effectsScheduledThreadPool.remove(r);
	}
	
	public ScheduledFuture<?> scheduleGeneral(final Runnable r, long delay)
	{
		if (_generalScheduledThreadPool.isShutdown())
			return null;
		
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			return _generalScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch (final RejectedExecutionException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return null; /* shutdown, ignore */
		}
	}
	
	public ScheduledFuture<?> scheduleGeneralAtFixedRate(final Runnable r, long initial, long delay)
	{
		if (_generalScheduledThreadPool.isShutdown())
		{
			return null;
		}
		
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			initial = ThreadPoolManager.validateDelay(initial);
			return _generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch (final RejectedExecutionException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return null;
		}
	}
	
	// @Deprecated
	public boolean removeGeneral(final Runnable r)
	{
		return _generalScheduledThreadPool.remove(r);
	}
	
	public ScheduledFuture<?> scheduleAi(final Runnable r, long delay)
	{
		if (_aiScheduledThreadPool.isShutdown())
			return null;
		
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			return _aiScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch (final RejectedExecutionException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return null;
		}
	}
	
	public ScheduledFuture<?> scheduleAiAtFixedRate(final Runnable r, long initial, long delay)
	{
		if (_aiScheduledThreadPool.isShutdown())
			return null;
		
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			initial = ThreadPoolManager.validateDelay(initial);
			return _aiScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch (final RejectedExecutionException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return null; /* shutdown, ignore */
		}
	}
	
	public void executePacket(final Runnable pkt)
	{
		if (!_generalPacketsThreadPool.isShutdown())
			_generalPacketsThreadPool.execute(pkt);
	}
	
	public void executeCommunityPacket(final Runnable r)
	{
		if (!_generalPacketsThreadPool.isShutdown())
			_generalPacketsThreadPool.execute(r);
	}
	
	public void executeIOPacket(final Runnable pkt)
	{
		if (!_ioPacketsThreadPool.isShutdown())
			_ioPacketsThreadPool.execute(pkt);
	}
	
	public void executeTask(final Runnable r)
	{
		if (!_generalThreadPool.isShutdown())
			_generalThreadPool.execute(r);
	}
	
	public void executeAi(final Runnable r)
	{
		if (!_aiScheduledThreadPool.isShutdown())
			_aiScheduledThreadPool.execute(new RunnableWrapper(r));
	}
	
	public String[] getStats()
	{
		return new String[]
		{
			"STP:",
			" + Effects:",
			" |- ActiveThreads:   " + _effectsScheduledThreadPool.getActiveCount(),
			" |- getCorePoolSize: " + _effectsScheduledThreadPool.getCorePoolSize(),
			" |- PoolSize:        " + _effectsScheduledThreadPool.getPoolSize(),
			" |- MaximumPoolSize: " + _effectsScheduledThreadPool.getMaximumPoolSize(),
			" |- CompletedTasks:  " + _effectsScheduledThreadPool.getCompletedTaskCount(),
			" |- ScheduledTasks:  " + (_effectsScheduledThreadPool.getTaskCount() - _effectsScheduledThreadPool.getCompletedTaskCount()),
			" | -------",
			" + General:",
			" |- ActiveThreads:   " + _generalScheduledThreadPool.getActiveCount(),
			" |- getCorePoolSize: " + _generalScheduledThreadPool.getCorePoolSize(),
			" |- PoolSize:        " + _generalScheduledThreadPool.getPoolSize(),
			" |- MaximumPoolSize: " + _generalScheduledThreadPool.getMaximumPoolSize(),
			" |- CompletedTasks:  " + _generalScheduledThreadPool.getCompletedTaskCount(),
			" |- ScheduledTasks:  " + (_generalScheduledThreadPool.getTaskCount() - _generalScheduledThreadPool.getCompletedTaskCount()),
			" | -------",
			" + AI:",
			" |- ActiveThreads:   " + _aiScheduledThreadPool.getActiveCount(),
			" |- getCorePoolSize: " + _aiScheduledThreadPool.getCorePoolSize(),
			" |- PoolSize:        " + _aiScheduledThreadPool.getPoolSize(),
			" |- MaximumPoolSize: " + _aiScheduledThreadPool.getMaximumPoolSize(),
			" |- CompletedTasks:  " + _aiScheduledThreadPool.getCompletedTaskCount(),
			" |- ScheduledTasks:  " + (_aiScheduledThreadPool.getTaskCount() - _aiScheduledThreadPool.getCompletedTaskCount()),
			"TP:",
			" + Packets:",
			" |- ActiveThreads:   " + _generalPacketsThreadPool.getActiveCount(),
			" |- getCorePoolSize: " + _generalPacketsThreadPool.getCorePoolSize(),
			" |- MaximumPoolSize: " + _generalPacketsThreadPool.getMaximumPoolSize(),
			" |- LargestPoolSize: " + _generalPacketsThreadPool.getLargestPoolSize(),
			" |- PoolSize:        " + _generalPacketsThreadPool.getPoolSize(),
			" |- CompletedTasks:  " + _generalPacketsThreadPool.getCompletedTaskCount(),
			" |- QueuedTasks:     " + _generalPacketsThreadPool.getQueue().size(),
			" | -------",
			" + I/O Packets:",
			" |- ActiveThreads:   " + _ioPacketsThreadPool.getActiveCount(),
			" |- getCorePoolSize: " + _ioPacketsThreadPool.getCorePoolSize(),
			" |- MaximumPoolSize: " + _ioPacketsThreadPool.getMaximumPoolSize(),
			" |- LargestPoolSize: " + _ioPacketsThreadPool.getLargestPoolSize(),
			" |- PoolSize:        " + _ioPacketsThreadPool.getPoolSize(),
			" |- CompletedTasks:  " + _ioPacketsThreadPool.getCompletedTaskCount(),
			" |- QueuedTasks:     " + _ioPacketsThreadPool.getQueue().size(),
			" | -------",
			" + General Tasks:",
			" |- ActiveThreads:   " + _generalThreadPool.getActiveCount(),
			" |- getCorePoolSize: " + _generalThreadPool.getCorePoolSize(),
			" |- MaximumPoolSize: " + _generalThreadPool.getMaximumPoolSize(),
			" |- LargestPoolSize: " + _generalThreadPool.getLargestPoolSize(),
			" |- PoolSize:        " + _generalThreadPool.getPoolSize(),
			" |- CompletedTasks:  " + _generalThreadPool.getCompletedTaskCount(),
			" |- QueuedTasks:     " + _generalThreadPool.getQueue().size(),
			" | -------",
			" + Javolution stats:",
			" |- FastList:        " + FastList.report(),
			" |- FastMap:        " + FastMap.report(),
			" |- FastSet:        " + FastSet.report(),
			" | -------"
		};
	}
	
	private static class PriorityThreadFactory implements ThreadFactory
	{
		private final int _prio;
		private final String _name;
		private final AtomicInteger _threadNumber = new AtomicInteger(1);
		private final ThreadGroup _group;
		
		public PriorityThreadFactory(final String name, final int prio)
		{
			_prio = prio;
			_name = name;
			_group = new ThreadGroup(_name);
		}
		
		@Override
		public Thread newThread(final Runnable r)
		{
			final Thread t = new Thread(_group, r);
			t.setName(_name + "-" + _threadNumber.getAndIncrement());
			t.setPriority(_prio);
			return t;
		}
		
		public ThreadGroup getGroup()
		{
			return _group;
		}
	}
	
	public void shutdown()
	{
		_shutdown = true;
		try
		{
			_effectsScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_ioPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_effectsScheduledThreadPool.shutdown();
			_generalScheduledThreadPool.shutdown();
			_generalPacketsThreadPool.shutdown();
			_ioPacketsThreadPool.shutdown();
			_generalThreadPool.shutdown();
			LOG.info("All ThreadPools are now stopped.");
			
		}
		catch (final InterruptedException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("", e);
		}
	}
	
	public boolean isShutdown()
	{
		return _shutdown;
	}
	
	public void purge()
	{
		_effectsScheduledThreadPool.purge();
		_generalScheduledThreadPool.purge();
		_aiScheduledThreadPool.purge();
		_ioPacketsThreadPool.purge();
		_generalPacketsThreadPool.purge();
		_generalThreadPool.purge();
	}
	
	public String getPacketStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		final ThreadFactory tf = _generalPacketsThreadPool.getThreadFactory();
		if (tf instanceof PriorityThreadFactory)
		{
			final PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			final int count = ptf.getGroup().activeCount();
			final Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			StringUtil.append(sb, "General Packet Thread Pool:\r\n" + "Tasks in the queue: ", String.valueOf(_generalPacketsThreadPool.getQueue().size()), "\r\n" + "Showing threads stack trace:\r\n" + "There should be ", String.valueOf(count), " Threads\r\n");
			for (final Thread t : threads)
			{
				if (t == null)
					continue;
				
				StringUtil.append(sb, t.getName(), "\r\n");
				for (final StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), "\r\n");
				}
			}
		}
		
		sb.append("Packet Tp stack traces printed.\r\n");
		
		return sb.toString();
	}
	
	public String getIOPacketStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		final ThreadFactory tf = _ioPacketsThreadPool.getThreadFactory();
		
		if (tf instanceof PriorityThreadFactory)
		{
			final PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			final int count = ptf.getGroup().activeCount();
			final Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			StringUtil.append(sb, "I/O Packet Thread Pool:\r\n" + "Tasks in the queue: ", String.valueOf(_ioPacketsThreadPool.getQueue().size()), "\r\n" + "Showing threads stack trace:\r\n" + "There should be ", String.valueOf(count), " Threads\r\n");
			
			for (final Thread t : threads)
			{
				if (t == null)
					continue;
				
				StringUtil.append(sb, t.getName(), "\r\n");
				
				for (final StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), "\r\n");
				}
			}
		}
		
		sb.append("Packet Tp stack traces printed.\r\n");
		
		return sb.toString();
	}
	
	public String getGeneralStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		final ThreadFactory tf = _generalThreadPool.getThreadFactory();
		
		if (tf instanceof PriorityThreadFactory)
		{
			final PriorityThreadFactory ptf = (PriorityThreadFactory) tf;
			final int count = ptf.getGroup().activeCount();
			final Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);
			StringUtil.append(sb, "General Thread Pool:\r\n" + "Tasks in the queue: ", String.valueOf(_generalThreadPool.getQueue().size()), "\r\n" + "Showing threads stack trace:\r\n" + "There should be ", String.valueOf(count), " Threads\r\n");
			
			for (final Thread t : threads)
			{
				if (t == null)
					continue;
				
				StringUtil.append(sb, t.getName(), "\r\n");
				
				for (final StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), "\r\n");
				}
			}
		}
		
		sb.append("Packet Tp stack traces printed.\r\n");
		
		return sb.toString();
	}
	
	protected class PurgeTask implements Runnable
	{
		@Override
		public void run()
		{
			_effectsScheduledThreadPool.purge();
			_generalScheduledThreadPool.purge();
			_aiScheduledThreadPool.purge();
		}
	}
	
	private static class SingletonHolder
	{
		protected static final ThreadPoolManager _instance = new ThreadPoolManager();
	}
}