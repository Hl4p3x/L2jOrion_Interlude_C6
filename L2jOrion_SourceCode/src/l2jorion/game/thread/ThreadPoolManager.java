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

import java.util.concurrent.LinkedBlockingQueue;
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
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.StringUtil;

public class ThreadPoolManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(ThreadPoolManager.class);
	
	private static final class RunnableWrapper implements Runnable
	{
		private final Runnable _runnable;
		
		public RunnableWrapper(Runnable runnable)
		{
			_runnable = runnable;
		}
		
		@Override
		public void run()
		{
			try
			{
				_runnable.run();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	private final ScheduledThreadPoolExecutor _effectsScheduledThreadPool;
	private final ScheduledThreadPoolExecutor _generalScheduledThreadPool;
	private final ScheduledThreadPoolExecutor _aiScheduledThreadPool;
	
	private final ThreadPoolExecutor _generalPacketsThreadPool;
	private final ThreadPoolExecutor _ioPacketsThreadPool;
	private final ThreadPoolExecutor _generalThreadPool;
	
	private boolean _shutdown;
	
	public static ThreadPoolManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public ThreadPoolManager()
	{
		_effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_EFFECTS, new PriorityThreadFactory("Effects[STP]", Thread.MAX_PRIORITY));
		_generalScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_GENERAL, new PriorityThreadFactory("General[STP]", Thread.MAX_PRIORITY));
		_aiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.AI_MAX_THREAD, new PriorityThreadFactory("AI[STP]", Thread.MAX_PRIORITY));
		
		_generalPacketsThreadPool = new ThreadPoolExecutor(Config.GENERAL_PACKET_THREAD_CORE_SIZE, Config.GENERAL_PACKET_THREAD_CORE_SIZE + 2, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("NormalPacket[TP]", Thread.MAX_PRIORITY));
		_ioPacketsThreadPool = new ThreadPoolExecutor(Config.IO_PACKET_THREAD_CORE_SIZE, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("I/O Packet[TP]", Thread.MAX_PRIORITY));
		_generalThreadPool = new ThreadPoolExecutor(Config.GENERAL_THREAD_CORE_SIZE, Config.GENERAL_THREAD_CORE_SIZE + 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("General[TP]", Thread.MAX_PRIORITY));
		
		scheduleGeneralAtFixedRate(() ->
		{
			purge();
		}, 60000, 60000);
	}
	
	public void purge()
	{
		_effectsScheduledThreadPool.purge();
		_generalScheduledThreadPool.purge();
		_aiScheduledThreadPool.purge();
		
		_generalPacketsThreadPool.purge();
		_ioPacketsThreadPool.purge();
		_generalThreadPool.purge();
	}
	
	public ScheduledFuture<?> scheduleEffect(Runnable r, long delay)
	{
		if (_effectsScheduledThreadPool.isShutdown())
		{
			return null;
		}
		
		try
		{
			return _effectsScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return null;
		}
	}
	
	public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable r, long initial, long delay)
	{
		if (_effectsScheduledThreadPool.isShutdown())
		{
			return null;
		}
		
		try
		{
			return _effectsScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return null;
		}
	}
	
	public ScheduledFuture<?> scheduleGeneral(Runnable r, long delay)
	{
		if (_generalScheduledThreadPool.isShutdown())
		{
			return null;
		}
		
		try
		{
			return _generalScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return null;
		}
	}
	
	public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable r, long initial, long delay)
	{
		if (_generalScheduledThreadPool.isShutdown())
		{
			return null;
		}
		
		try
		{
			return _generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return null;
		}
	}
	
	public ScheduledFuture<?> scheduleAi(final Runnable r, long delay)
	{
		if (_aiScheduledThreadPool.isShutdown())
		{
			return null;
		}
		
		try
		{
			return _aiScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return null;
		}
	}
	
	public ScheduledFuture<?> scheduleAiAtFixedRate(final Runnable r, long initial, long delay)
	{
		if (_aiScheduledThreadPool.isShutdown())
		{
			return null;
		}
		
		try
		{
			return _aiScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return null;
		}
	}
	
	public void executePacket(Runnable runnable)
	{
		if (!_generalPacketsThreadPool.isShutdown())
		{
			_generalPacketsThreadPool.execute(new RunnableWrapper(runnable));
		}
	}
	
	public void executeIOPacket(Runnable runnable)
	{
		if (!_ioPacketsThreadPool.isShutdown())
		{
			_ioPacketsThreadPool.execute(new RunnableWrapper(runnable));
		}
	}
	
	public void executeTask(Runnable runnable)
	{
		if (!_generalThreadPool.isShutdown())
		{
			_generalThreadPool.execute(new RunnableWrapper(runnable));
		}
	}
	
	public void executeAi(Runnable runnable)
	{
		if (!_aiScheduledThreadPool.isShutdown())
		{
			_aiScheduledThreadPool.execute(new RunnableWrapper(runnable));
		}
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
	
	public String[] getStatsSTP()
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
		};
	}
	
	public String[] getStatsTP()
	{
		return new String[]
		{
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
		};
	}
	
	private static class PriorityThreadFactory implements ThreadFactory
	{
		private final int _prio;
		private final String _name;
		private final AtomicInteger _threadNumber = new AtomicInteger(1);
		private final ThreadGroup _group;
		
		public PriorityThreadFactory(String name, int prio)
		{
			_prio = prio;
			_name = name;
			_group = new ThreadGroup(_name);
		}
		
		@Override
		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(_group, r);
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
			
			LOG.info("All ThreadPools are now stopped");
			
		}
		catch (InterruptedException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
	
	public boolean isShutdown()
	{
		return _shutdown;
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
				{
					continue;
				}
				
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
				{
					continue;
				}
				
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
				{
					continue;
				}
				
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
	
	private static class SingletonHolder
	{
		protected static final ThreadPoolManager _instance = new ThreadPoolManager();
	}
}