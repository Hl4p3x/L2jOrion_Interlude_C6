/* This program is free software; you can redistribute it and/or modify
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
package l2jorion.game.taskmanager;

import static l2jorion.game.taskmanager.TaskTypes.TYPE_FIXED_SHEDULED;
import static l2jorion.game.taskmanager.TaskTypes.TYPE_GLOBAL_TASK;
import static l2jorion.game.taskmanager.TaskTypes.TYPE_NONE;
import static l2jorion.game.taskmanager.TaskTypes.TYPE_SHEDULED;
import static l2jorion.game.taskmanager.TaskTypes.TYPE_SPECIAL;
import static l2jorion.game.taskmanager.TaskTypes.TYPE_STARTUP;
import static l2jorion.game.taskmanager.TaskTypes.TYPE_TIME;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.taskmanager.tasks.TaskCleanUp;
import l2jorion.game.taskmanager.tasks.TaskOlympiadSave;
import l2jorion.game.taskmanager.tasks.TaskRaidPointsReset;
import l2jorion.game.taskmanager.tasks.TaskRecom;
import l2jorion.game.taskmanager.tasks.TaskRestart;
import l2jorion.game.taskmanager.tasks.TaskSevenSignsUpdate;
import l2jorion.game.taskmanager.tasks.TaskShutdown;
import l2jorion.game.taskmanager.tasks.TaskWeeklyTopBoard;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public final class TaskManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);
	
	private static TaskManager _instance;
	
	protected static final String[] SQL_STATEMENTS =
	{
		"SELECT id, task, type, last_activation, param1, param2, param3 FROM global_tasks",
		"UPDATE global_tasks SET last_activation=? WHERE id=?",
		"SELECT id FROM global_tasks WHERE task=?",
		"INSERT INTO global_tasks (task,type,last_activation,param1,param2,param3) VALUES(?,?,?,?,?,?)"
	};
	
	private final HashMap<Integer, Task> _tasks = new HashMap<>();
	protected final ArrayList<ExecutedTask> _currentTasks = new ArrayList<>();
	
	public class ExecutedTask implements Runnable
	{
		int id;
		long lastActivation;
		
		Task task;
		TaskTypes type;
		String[] params;
		ScheduledFuture<?> scheduled;
		
		public ExecutedTask(Task ptask, TaskTypes ptype, ResultSet rset) throws SQLException
		{
			task = ptask;
			type = ptype;
			id = rset.getInt("id");
			lastActivation = rset.getLong("last_activation");
			params = new String[]
			{
				rset.getString("param1"),
				rset.getString("param2"),
				rset.getString("param3")
			};
		}
		
		@Override
		public void run()
		{
			task.onTimeElapsed(this);
			lastActivation = System.currentTimeMillis();
			
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[1]);
				statement.setLong(1, lastActivation);
				statement.setInt(2, id);
				statement.executeUpdate();
				statement.close();
			}
			catch (SQLException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("Cannot updated the Global Task " + id + ": " + e.getMessage());
			}
			finally
			{
				CloseUtil.close(con);
			}
			
			if (type == TYPE_SHEDULED || type == TYPE_TIME)
			{
				stopTask();
			}
		}
		
		@Override
		public boolean equals(Object object)
		{
			if (object == null)
			{
				return false;
			}
			return id == ((ExecutedTask) object).id;
		}
		
		@Override
		public int hashCode()
		{
			return id;
		}
		
		public Task getTask()
		{
			return task;
		}
		
		public TaskTypes getType()
		{
			return type;
		}
		
		public int getId()
		{
			return id;
		}
		
		public String[] getParams()
		{
			return params;
		}
		
		public long getLastActivation()
		{
			return lastActivation;
		}
		
		public void stopTask()
		{
			task.onDestroy();
			
			if (scheduled != null)
			{
				scheduled.cancel(true);
			}
			
			_currentTasks.remove(this);
		}
		
	}
	
	public static TaskManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new TaskManager();
		}
		return _instance;
	}
	
	private TaskManager()
	{
		initializate();
		startAllTasks();
	}
	
	private void initializate()
	{
		registerTask(new TaskCleanUp());
		registerTask(new TaskOlympiadSave());
		registerTask(new TaskRaidPointsReset());
		registerTask(new TaskRecom());
		registerTask(new TaskRestart());
		registerTask(new TaskSevenSignsUpdate());
		registerTask(new TaskShutdown());
		
		// registerTask(new TaskDailyMissionsReset());
		
		if (Config.RON_CUSTOM)
		{
			registerTask(new TaskWeeklyTopBoard());
		}
	}
	
	public void registerTask(Task task)
	{
		int key = task.getName().trim().toLowerCase().hashCode();
		if (!_tasks.containsKey(key))
		{
			_tasks.put(key, task);
			task.initializate();
		}
	}
	
	private void startAllTasks()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[0]);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				Task task = _tasks.get(rset.getString("task").trim().toLowerCase().hashCode());
				
				if (task == null)
				{
					continue;
				}
				
				TaskTypes type = TaskTypes.valueOf(rset.getString("type"));
				
				if (type != TYPE_NONE)
				{
					ExecutedTask current = new ExecutedTask(task, type, rset);
					if (launchTask(current))
					{
						_currentTasks.add(current);
					}
				}
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOG.error("error while loading Global Task table " + e);
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private boolean launchTask(ExecutedTask task)
	{
		final ThreadPoolManager scheduler = ThreadPoolManager.getInstance();
		final TaskTypes type = task.getType();
		
		if (type == TYPE_STARTUP)
		{
			task.run();
			return false;
		}
		else if (type == TYPE_SHEDULED)
		{
			long delay = Long.valueOf(task.getParams()[0]);
			task.scheduled = scheduler.scheduleGeneral(task, delay);
			return true;
		}
		else if (type == TYPE_FIXED_SHEDULED)
		{
			long delay = Long.valueOf(task.getParams()[0]);
			long interval = Long.valueOf(task.getParams()[1]);
			
			task.scheduled = scheduler.scheduleGeneralAtFixedRate(task, delay, interval);
			return true;
		}
		else if (type == TYPE_TIME)
		{
			try
			{
				Date desired = DateFormat.getInstance().parse(task.getParams()[0]);
				long diff = desired.getTime() - System.currentTimeMillis();
				if (diff >= 0)
				{
					task.scheduled = scheduler.scheduleGeneral(task, diff);
					return true;
				}
				LOG.info("Task " + task.getId() + " is obsoleted.");
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (type == TYPE_SPECIAL)
		{
			ScheduledFuture<?> result = task.getTask().launchSpecial(task);
			if (result != null)
			{
				task.scheduled = result;
				return true;
			}
		}
		else if (type == TYPE_GLOBAL_TASK)
		{
			long interval = Long.valueOf(task.getParams()[0]) * 86400000L;
			String[] hour = task.getParams()[1].split(":");
			
			if (hour.length != 3)
			{
				LOG.warn("Task " + task.getId() + " has incorrect parameters");
				return false;
			}
			
			Calendar check = Calendar.getInstance();
			check.setTimeInMillis(task.getLastActivation() + interval);
			
			Calendar min = Calendar.getInstance();
			try
			{
				min.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hour[0]));
				min.set(Calendar.MINUTE, Integer.valueOf(hour[1]));
				min.set(Calendar.SECOND, Integer.valueOf(hour[2]));
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				LOG.warn("Bad parameter on task " + task.getId() + ": " + e.getMessage());
				return false;
			}
			
			long delay = min.getTimeInMillis() - System.currentTimeMillis();
			
			if (check.after(min) || delay < 0)
			{
				delay += interval;
			}
			
			task.scheduled = scheduler.scheduleGeneralAtFixedRate(task, delay, interval);
			
			return true;
		}
		
		return false;
	}
	
	public static boolean addUniqueTask(String task, TaskTypes type, String param1, String param2, String param3)
	{
		return addUniqueTask(task, type, param1, param2, param3, 0);
	}
	
	public static boolean addUniqueTask(String task, TaskTypes type, String param1, String param2, String param3, long lastActivation)
	{
		Connection con = null;
		
		boolean output = false;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[2]);
			statement.setString(1, task);
			ResultSet rset = statement.executeQuery();
			
			if (!rset.next())
			{
				statement = con.prepareStatement(SQL_STATEMENTS[3]);
				statement.setString(1, task);
				statement.setString(2, type.toString());
				statement.setLong(3, lastActivation);
				statement.setString(4, param1);
				statement.setString(5, param2);
				statement.setString(6, param3);
				statement.execute();
			}
			
			rset.close();
			statement.close();
			
			output = true;
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			LOG.warn("cannot add the unique task: " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return output;
	}
	
	public static boolean addTask(String task, TaskTypes type, String param1, String param2, String param3)
	{
		return addTask(task, type, param1, param2, param3, 0);
	}
	
	public static boolean addTask(String task, TaskTypes type, String param1, String param2, String param3, long lastActivation)
	{
		Connection con = null;
		
		boolean output = false;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[3]);
			statement.setString(1, task);
			statement.setString(2, type.toString());
			statement.setLong(3, lastActivation);
			statement.setString(4, param1);
			statement.setString(5, param2);
			statement.setString(6, param3);
			statement.execute();
			statement.close();
			
			output = true;
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			LOG.warn("cannot add the task:  " + e.getMessage());
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return output;
	}
}
