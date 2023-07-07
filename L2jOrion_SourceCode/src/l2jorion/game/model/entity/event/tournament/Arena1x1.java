package l2jorion.game.model.entity.event.tournament;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2jorion.Config;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class Arena1x1 implements Runnable
{
	private static Logger LOG = LoggerFactory.getLogger(Arena1x1.class);
	// list of participants
	private List<Pair> registered;
	// number of Arenas
	int free = Config.ARENA_EVENT_COUNT_1X1;
	// Arenas
	Arena[] arenas = new Arena[Config.ARENA_EVENT_COUNT_1X1];
	// list of fights going on
	Map<Integer, String> fights = new HashMap<>(Config.ARENA_EVENT_COUNT_1X1);
	
	public Arena1x1()
	{
		registered = new ArrayList<>();
		int[] coord;
		for (int i = 0; i < Config.ARENA_EVENT_COUNT_1X1; i++)
		{
			coord = Config.ARENA_EVENT_LOCS_1X1[i];
			arenas[i] = new Arena(i, coord[0], coord[1], coord[2]);
		}
		
		LOG.info("Initialized Tournament 1x1 Event");
	}
	
	public static Arena1x1 getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public boolean register(L2PcInstance player)
	{
		for (Pair p : registered)
		{
			if (p.getLeader() == player)
			{
				player.sendMessage("Tournament: You already registered!");
				return false;
			}
		}
		return registered.add(new Pair(player));
	}
	
	public boolean isRegistered(L2PcInstance player)
	{
		for (Pair p : registered)
		{
			if (p.getLeader() == player)
			{
				return true;
			}
		}
		return false;
	}
	
	public Map<Integer, String> getFights()
	{
		return fights;
	}
	
	public boolean remove(L2PcInstance player)
	{
		for (Pair p : registered)
		{
			if (p.getLeader() == player)
			{
				registered.remove(p);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public synchronized void run()
	{
		// while server is running
		while (true)
		{
			// if no have participants or arenas are busy wait 1 minute
			if (registered.size() < 2 || free == 0)
			{
				try
				{
					Thread.sleep(Config.ARENA_CALL_INTERVAL_1X1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				continue;
			}
			
			List<Pair> opponents = selectOpponents();
			
			if (opponents != null && opponents.size() == 2)
			{
				Thread T = new Thread(new EvtArenaTask(opponents));
				T.setDaemon(true);
				T.start();
			}
			
			// wait 1 minute for not stress server
			try
			{
				Thread.sleep(Config.ARENA_CALL_INTERVAL_1X1);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("null")
	private List<Pair> selectOpponents()
	{
		List<Pair> opponents = new ArrayList<>();
		Pair pairOne = null, pairTwo = null;
		int tries = 3;
		do
		{
			int first = 0, second = 0;
			if (getRegisteredCount() < 2)
			{
				return opponents;
			}
			
			if (pairOne == null)
			{
				first = Rnd.get(getRegisteredCount());
				pairOne = registered.get(first);
				
				if (pairOne.check())
				{
					opponents.add(0, pairOne);
					registered.remove(first);
				}
				else
				{
					pairOne = null;
					registered.remove(first);
					return null;
				}
			}
			
			if (pairTwo == null)
			{
				second = Rnd.get(getRegisteredCount());
				pairTwo = registered.get(second);
				
				if (pairTwo.check())
				{
					opponents.add(1, pairTwo);
					registered.remove(second);
				}
				else
				{
					pairTwo = null;
					registered.remove(second);
					return null;
				}
			}
		}
		
		while ((pairOne == null || pairTwo == null) && --tries > 0);
		
		return opponents;
	}
	
	public int getRegisteredCount()
	{
		return registered.size();
	}
	
	private class Pair
	{
		private L2PcInstance leader;
		
		public Pair(L2PcInstance leader)
		{
			this.leader = leader;
		}
		
		public L2PcInstance getLeader()
		{
			return leader;
		}
		
		public boolean check()
		{
			if ((leader == null || leader.isOnline() == 0))
			{
				return false;
			}
			return true;
		}
		
		public boolean isDead()
		{
			if ((leader == null || leader.isDead() || leader.isOnline() == 0 || !leader.isArenaAttack()))
			{
				return false;
			}
			
			return !(leader.isDead());
		}
		
		public boolean isAlive()
		{
			if ((leader == null || leader.isDead() || leader.isOnline() == 0 || !leader.isArenaAttack()))
			{
				return false;
			}
			
			return !(leader.isDead());
		}
		
		public void teleportTo(int x, int y, int z)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.restoreCP();
				leader.restoreHPMP();
				leader.teleToLocation(x, y, z);
				leader.broadcastUserInfo();
			}
		}
		
		public void rewards()
		{
			if (leader != null)
			{
				leader.getInventory().addItem("Arena_Event", Config.ARENA_REWARD_ID_1X1, Config.ARENA_REWARD_COUNT_1X1, leader, null);
				leader.sendPacket(new ItemList(leader, true));
			}
			
			sendPacket("Congratulations, you won the event!", 5);
		}
		
		public void setInTournamentEvent(boolean val)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.setInArenaEvent(val);
			}
		}
		
		public void revive()
		{
			if (leader != null && leader.isOnline() == 1 && leader.isDead())
			{
				leader.doRevive();
			}
		}
		
		public void setArenaProtection(boolean val)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.setArenaProtection(val);
				leader.setArena1x1(val);
				
			}
		}
		
		public void removeBuff()
		{
			if (leader != null && leader.isOnline() == 1)
			{
				for (L2Effect effect : leader.getAllEffects())
				{
					if (effect.getSkill().getId() == 406 || effect.getSkill().getId() == 139 || effect.getSkill().getId() == 176 || effect.getSkill().getId() == 420)
					{
						leader.stopSkillEffects(effect.getSkill().getId());
						leader.enableSkill(effect.getSkill());
					}
				}
			}
		}
		
		public void removeSummon()
		{
			if (leader != null && leader.isOnline() == 1)
			{
				// Remove Summon's buffs
				if (leader.getPet() != null)
				{
					L2Summon summon = leader.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof L2PetInstance)
					{
						summon.unSummon(leader);
					}
				}
				
				if (leader.getMountType() == 1 || leader.getMountType() == 2)
				{
					leader.dismount();
				}
			}
		}
		
		public void setImobilised(boolean val)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.setIsInvul(val);
				leader.setIsParalyzed(val);
			}
		}
		
		public void setArenaAttack(boolean val)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.setArenaAttack(val);
				leader.broadcastUserInfo();
			}
		}
		
		public void sendPacket(String message, int duration)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
		}
		
		public void initCountdown(int duration)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new Countdown(leader, duration), 0);
			}
		}
		
		public void setFlag()
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.stopPvPFlag();
				leader.setPvpFlag(1);
				leader.broadcastUserInfo();
			}
		}
		
		public void setUnFlag()
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.setPvpFlag(0);
				leader.broadcastUserInfo();
			}
		}
	}
	
	private class EvtArenaTask implements Runnable
	{
		private final Pair pairOne;
		private final Pair pairTwo;
		private final int pOneX, pOneY, pOneZ, pTwoX, pTwoY, pTwoZ;
		private Arena arena;
		
		public EvtArenaTask(List<Pair> opponents)
		{
			pairOne = opponents.get(0);
			pairTwo = opponents.get(1);
			L2PcInstance leader = pairOne.getLeader();
			pOneX = leader.getX();
			pOneY = leader.getY();
			pOneZ = leader.getZ();
			leader = pairTwo.getLeader();
			pTwoX = leader.getX();
			pTwoY = leader.getY();
			pTwoZ = leader.getZ();
		}
		
		@Override
		public void run()
		{
			free--;
			portPairsToArena();
			pairOne.setFlag();
			pairTwo.setFlag();
			pairOne.initCountdown(20);
			pairTwo.initCountdown(20);
			
			try
			{
				Thread.sleep(Config.ARENA_WAIT_INTERVAL_1X1);
			}
			catch (InterruptedException e1)
			{
				e1.printStackTrace();
			}
			
			pairOne.sendPacket("The battle starts NOW! Good Fight!", 5);
			pairTwo.sendPacket("The battle starts NOW! Good Fight!", 5);
			pairOne.setImobilised(false);
			pairTwo.setImobilised(false);
			pairOne.setArenaAttack(true);
			pairTwo.setArenaAttack(true);
			pairOne.removeBuff();
			pairTwo.removeBuff();
			pairOne.removeSummon();
			pairTwo.removeSummon();
			
			while (check())
			{
				// check players status each seconds
				try
				{
					Thread.sleep(Config.ARENA_CHECK_INTERVAL_1X1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
					break;
				}
			}
			finishDuel();
			free++;
		}
		
		private void finishDuel()
		{
			fights.remove(arena.id);
			rewardWinner();
			pairOne.revive();
			pairTwo.revive();
			pairOne.setUnFlag();
			pairTwo.setUnFlag();
			pairOne.teleportTo(pOneX, pOneY, pOneZ);
			pairTwo.teleportTo(pTwoX, pTwoY, pTwoZ);
			pairOne.setInTournamentEvent(false);
			pairTwo.setInTournamentEvent(false);
			pairOne.setArenaProtection(false);
			pairTwo.setArenaProtection(false);
			pairOne.setArenaAttack(false);
			pairTwo.setArenaAttack(false);
			arena.setFree(true);
		}
		
		private void rewardWinner()
		{
			if (pairOne.isAlive() && !pairTwo.isAlive())
			{
				pairOne.rewards();
			}
			else if (pairTwo.isAlive() && !pairOne.isAlive())
			{
				pairTwo.rewards();
			}
		}
		
		private boolean check()
		{
			return (pairOne.isDead() && pairTwo.isDead());
		}
		
		private void portPairsToArena()
		{
			for (Arena arena : arenas)
			{
				if (arena.isFree)
				{
					this.arena = arena;
					arena.setFree(false);
					pairOne.teleportTo(arena.x - Config.TM_DISTANCE, arena.y, arena.z);
					pairTwo.teleportTo(arena.x + Config.TM_DISTANCE, arena.y, arena.z);
					pairOne.setImobilised(true);
					pairTwo.setImobilised(true);
					pairOne.setInTournamentEvent(true);
					pairTwo.setInTournamentEvent(true);
					fights.put(this.arena.id, pairOne.getLeader().getName() + " vs " + pairTwo.getLeader().getName());
					break;
				}
			}
		}
	}
	
	private class Arena
	{
		protected int x, y, z;
		protected boolean isFree = true;
		int id;
		
		public Arena(int id, int x, int y, int z)
		{
			this.id = id;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void setFree(boolean val)
		{
			isFree = val;
		}
	}
	
	protected class Countdown implements Runnable
	{
		private final L2PcInstance _player;
		private int _time;
		
		public Countdown(L2PcInstance player, int time)
		{
			_time = time;
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player.isOnline() == 1)
			{
				switch (_time)
				{
					case 300:
					case 240:
					case 180:
					case 120:
					case 60:
					case 45:
					case 30:
					case 20:
					case 15:
					case 10:
						_player.sendMessage(_time + " seconds to start the battle!");
						break;
					case 5:
					case 4:
					case 3:
					case 2:
						_player.sendMessage(_time + " seconds to start the battle!");
						_player.sendPacket(new ExShowScreenMessage(_time + " seconds to start the battle!", 1 * 1000));
						break;
					case 1:
						_player.sendMessage(_time + " second to start the battle!");
						_player.sendPacket(new ExShowScreenMessage(_time + " second to start the battle!", 1 * 1000));
						break;
				}
				
				if (_time > 0)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new Countdown(_player, _time - 1), 1000);
				}
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final Arena1x1 INSTANCE = new Arena1x1();
	}
}