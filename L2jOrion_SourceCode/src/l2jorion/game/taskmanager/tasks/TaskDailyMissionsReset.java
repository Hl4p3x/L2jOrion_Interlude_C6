package l2jorion.game.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import l2jorion.game.managers.AchievementManager;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.taskmanager.Task;
import l2jorion.game.taskmanager.TaskManager;
import l2jorion.game.taskmanager.TaskManager.ExecutedTask;
import l2jorion.game.taskmanager.TaskTypes;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public final class TaskDailyMissionsReset extends Task
{
	public static final String NAME = "daily_missions_reset";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		// Delete daily missions data from database
		deleteDailyMissionsData();
		// Reset daily missions and load old data
		resetPlayerData();
		// Re-load achievements
		AchievementManager.getInstance().reload();
	}
	
	private void resetPlayerData()
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0 && player.getAchievement() != null)
				{
					player.getAchievement().cleanUp();
					player.getAchievement().load();
				}
			}
		}
	}
	
	private void deleteDailyMissionsData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_achievements WHERE type LIKE 'DAILY%'");
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.info(getClass().getSimpleName() + ": Couldn't clean up character_achievements table: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "6:00:00", "");
		// it's for fast test
		// TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "20000", "20000", "");
	}
}