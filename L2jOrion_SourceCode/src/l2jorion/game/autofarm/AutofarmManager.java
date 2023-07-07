package l2jorion.game.autofarm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;

public enum AutofarmManager
{
	INSTANCE;
	
	private final Long iterationSpeedMs = 450L;
	
	private final ConcurrentHashMap<Integer, AutofarmPlayerRoutine> activeFarmers = new ConcurrentHashMap<>();
	protected final ScheduledFuture<?> onUpdateTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(onUpdate(), 1000, iterationSpeedMs);
	
	private Runnable onUpdate()
	{
		return () -> activeFarmers.forEach((integer, autofarmPlayerRoutine) -> autofarmPlayerRoutine.executeRoutine());
	}
	
	public void startFarm(L2PcInstance player)
	{
		if (isAutofarming(player))
		{
			player.sendMessage("You are already autofarming.");
			return;
		}
		
		activeFarmers.put(player.getObjectId(), new AutofarmPlayerRoutine(player));
		player.sendMessage("Autofarming activated.");
	}
	
	public void stopFarm(L2PcInstance player)
	{
		if (!isAutofarming(player))
		{
			player.sendMessage("You are not autofarming.");
			return;
		}
		
		activeFarmers.remove(player.getObjectId());
		player.sendMessage("Autofarming deactivated.");
	}
	
	public void toggleFarm(L2PcInstance player)
	{
		if (isAutofarming(player))
		{
			stopFarm(player);
			return;
		}
		
		startFarm(player);
	}
	
	public Boolean isAutofarming(L2PcInstance player)
	{
		return activeFarmers.containsKey(player.getObjectId());
	}
	
	public void onPlayerLogout(L2PcInstance player)
	{
		stopFarm(player);
	}
	
	public void onDeath(L2PcInstance player)
	{
		if (isAutofarming(player))
		{
			activeFarmers.remove(player.getObjectId());
		}
	}
}
