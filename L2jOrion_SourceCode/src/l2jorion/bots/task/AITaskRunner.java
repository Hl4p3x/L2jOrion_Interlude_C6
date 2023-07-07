package l2jorion.bots.task;

import java.util.List;

import l2jorion.bots.FakePlayerTaskManager;
import l2jorion.game.thread.ThreadPoolManager;

public class AITaskRunner implements Runnable
{
	@Override
	public void run()
	{
		FakePlayerTaskManager.INSTANCE.adjustTaskSize();
		List<AITask> aiTasks = FakePlayerTaskManager.INSTANCE.getAITasks();
		aiTasks.forEach(aiTask -> ThreadPoolManager.getInstance().executeTask(aiTask));
	}
}