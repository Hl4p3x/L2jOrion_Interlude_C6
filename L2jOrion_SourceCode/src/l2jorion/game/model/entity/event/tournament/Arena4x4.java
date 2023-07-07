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
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class Arena4x4 implements Runnable
{
	private static Logger LOG = LoggerFactory.getLogger(Arena4x4.class);
	// list of participants
	public static List<Team> registered;
	// number of Arenas
	int free = Config.ARENA_EVENT_COUNT_4X4;
	// Arenas
	Arena[] arenas = new Arena[Config.ARENA_EVENT_COUNT_4X4];
	// list of fights going on
	Map<Integer, String> fights = new HashMap<>(Config.ARENA_EVENT_COUNT_4X4);
	
	public Arena4x4()
	{
		registered = new ArrayList<>();
		int[] coord;
		for (int i = 0; i < Config.ARENA_EVENT_COUNT_4X4; i++)
		{
			coord = Config.ARENA_EVENT_LOCS_4X4[i];
			arenas[i] = new Arena(i, coord[0], coord[1], coord[2]);
		}
		
		LOG.info("Initialized Tournament 4x4 Event");
	}
	
	public static Arena4x4 getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public boolean register(L2PcInstance player, L2PcInstance assist, L2PcInstance assist2, L2PcInstance assist3)
	{
		for (Team p : registered)
		{
			if (p.getLeader() == player || p.getAssist() == player)
			{
				player.sendMessage("Tournament: You already registered!");
				return false;
			}
			else if (p.getLeader() == assist || p.getAssist() == assist)
			{
				player.sendMessage("Tournament: " + assist.getName() + " already registered!");
				return false;
			}
			else if (p.getLeader() == assist2 || p.getAssist2() == assist2)
			{
				player.sendMessage("Tournament: " + assist2.getName() + " already registered!");
				return false;
			}
			else if (p.getLeader() == assist3 || p.getAssist3() == assist3)
			{
				player.sendMessage("Tournament: " + assist3.getName() + " already registered!");
				return false;
			}
		}
		return registered.add(new Team(player, assist, assist2, assist3));
	}
	
	public boolean isRegistered(L2PcInstance player)
	{
		for (Team p : registered)
		{
			if (p.getLeader() == player || p.getAssist() == player || p.getAssist2() == player || p.getAssist3() == player)
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
		for (Team p : registered)
		{
			if (p.getLeader() == player || p.getAssist() == player || p.getAssist2() == player || p.getAssist3() == player)
			{
				// p.removeMessage();
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
					Thread.sleep(Config.ARENA_CALL_INTERVAL_2X2);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				continue;
			}
			List<Team> opponents = selectOpponents();
			if (opponents != null && opponents.size() == 2)
			{
				Thread T = new Thread(new EvtArenaTask(opponents));
				T.setDaemon(true);
				T.start();
			}
			// wait 1 minute for not stress server
			try
			{
				Thread.sleep(Config.ARENA_CALL_INTERVAL_2X2);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("null")
	private List<Team> selectOpponents()
	{
		List<Team> opponents = new ArrayList<>();
		Team pairOne = null, pairTwo = null;
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
	
	private class Team
	{
		private L2PcInstance leader, assist, assist2, assist3;
		
		public Team(L2PcInstance leader, L2PcInstance assist, L2PcInstance assist2, L2PcInstance assist3)
		{
			this.leader = leader;
			this.assist = assist;
			this.assist2 = assist2;
			this.assist3 = assist3;
		}
		
		public L2PcInstance getAssist()
		{
			return assist;
		}
		
		public L2PcInstance getAssist2()
		{
			return assist2;
		}
		
		public L2PcInstance getAssist3()
		{
			return assist3;
		}
		
		public L2PcInstance getLeader()
		{
			return leader;
		}
		
		public boolean check()
		{
			if ((leader == null || leader.isOnline() == 0))
			{
				
				if (assist != null || assist.isOnline() == 1)
				{
					assist.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				if (assist2 != null || assist2.isOnline() == 1)
				{
					assist2.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				if (assist3 != null || assist3.isOnline() == 1)
				{
					assist3.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				return false;
			}
			else if (((assist == null || assist.isOnline() == 0) || (assist2 == null || assist2.isOnline() == 0) || (assist3 == null || assist3.isOnline() == 0)) && (leader != null && leader.isOnline() == 1))
			{
				leader.sendMessage("Tournament: You participation in Event was Canceled.");
				
				if (assist != null || assist.isOnline() == 1)
				{
					assist.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				if (assist2 != null || assist2.isOnline() == 1)
				{
					assist2.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				if (assist3 != null || assist3.isOnline() == 1)
				{
					assist3.sendMessage("Tournament: You participation in Event was Canceled.");
				}
				
				return false;
			}
			return true;
		}
		
		public boolean isDead()
		{
			// if (Config.ARENA_DEBUG)
			// System.out.println("[4x4]: L: ["+leader.getName()+"] / A1: ["+assist.getName()+"] / A2: ["+assist2.getName()+"] / A3: ["+assist3.getName()+"]");
			
			if ((leader == null || leader.isDead() || leader.isOnline() == 0 || !leader.isInArenaEvent()) && (assist == null || assist.isDead() || assist.isOnline() == 0 || !assist.isInArenaEvent()) && (assist2 == null || assist2.isDead() || assist2.isOnline() == 0 || !assist2.isInArenaEvent())
				&& (assist3 == null || assist3.isDead() || assist3.isOnline() == 0 || assist3.isInArenaEvent()))
			{
				return false;
			}
			
			return !(leader.isDead() && assist.isDead() && assist2.isDead() && assist3.isDead());
		}
		
		public boolean isAlive()
		{
			if ((leader == null || leader.isDead() || leader.isOnline() == 0 || !leader.isInArenaEvent()) && (assist == null || assist.isDead() || assist.isOnline() == 0 || !assist.isInArenaEvent()) && (assist2 == null || assist2.isDead() || assist2.isOnline() == 0 || !assist2.isInArenaEvent())
				&& (assist3 == null || assist3.isDead() || assist3.isOnline() == 0 || !assist3.isInArenaEvent()))
			{
				return false;
			}
			
			return !(leader.isDead() && assist.isDead() && assist2.isDead() && assist3.isDead());
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
			
			if (assist != null && assist.isOnline() == 1)
			{
				leader.restoreCP();
				leader.restoreHPMP();
				assist.teleToLocation(x, y + 50, z);
				assist.broadcastUserInfo();
			}
			
			if (assist2 != null && assist2.isOnline() == 1)
			{
				leader.restoreCP();
				leader.restoreHPMP();
				assist2.teleToLocation(x, y - 100, z);
				assist2.broadcastUserInfo();
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				leader.restoreCP();
				leader.restoreHPMP();
				assist3.teleToLocation(x, y - 50, z);
				assist3.broadcastUserInfo();
			}
		}
		
		public void rewards()
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.getInventory().addItem("Arena_Event", Config.ARENA_REWARD_ID_4X4, Config.ARENA_REWARD_COUNT_4X4, leader, null);
				leader.sendPacket(new ItemList(leader, true));
			}
			
			if (assist != null && assist.isOnline() == 1)
			{
				assist.getInventory().addItem("Arena_Event", Config.ARENA_REWARD_ID_4X4, Config.ARENA_REWARD_COUNT_4X4, assist, null);
				assist.sendPacket(new ItemList(assist, true));
			}
			
			if (assist2 != null && assist2.isOnline() == 1)
			{
				assist2.getInventory().addItem("Arena_Event", Config.ARENA_REWARD_ID_4X4, Config.ARENA_REWARD_COUNT_4X4, assist2, null);
				assist2.sendPacket(new ItemList(assist2, true));
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				assist3.getInventory().addItem("Arena_Event", Config.ARENA_REWARD_ID_4X4, Config.ARENA_REWARD_COUNT_4X4, assist3, null);
				assist3.sendPacket(new ItemList(assist3, true));
			}
			sendPacket("Congratulations, your team won the event!", 5);
		}
		
		public void setInTournamentEvent(boolean val)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.setInArenaEvent(val);
			}
			
			if (assist != null && assist.isOnline() == 1)
			{
				assist.setInArenaEvent(val);
			}
			
			if (assist2 != null && assist2.isOnline() == 1)
			{
				assist2.setInArenaEvent(val);
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				assist3.setInArenaEvent(val);
			}
		}
		
		/*
		 * public void removeMessage() { if(leader != null && leader.isOnline()) { leader.sendMessage("SYS: Your participation in the Arena 4x4 event has been removed."); leader.setArenaProtection(false); leader.setArena4x4(false); } if(assist != null && assist.isOnline()) {
		 * assist.sendMessage("SYS: Your participation in the Arena 4x4 event has been removed."); assist.setArenaProtection(false); assist.setArena4x4(false); } if(assist2 != null && assist2.isOnline()) { assist2.sendMessage("SYS: Your participation in the Arena 4x4 event has been removed.");
		 * assist2.setArenaProtection(false); assist2.setArena4x4(false); } if(assist3 != null && assist3.isOnline()) { assist3.sendMessage("SYS: Your participation in the Arena 4x4 event has been removed."); assist3.setArenaProtection(false); assist3.setArena4x4(false); } }
		 */
		
		public void revive()
		{
			if (leader != null && leader.isOnline() == 1 && leader.isDead())
			{
				leader.doRevive();
			}
			
			if (assist != null && assist.isOnline() == 1 && assist.isDead())
			{
				assist.doRevive();
			}
			
			if (assist2 != null && assist2.isOnline() == 1 && assist2.isDead())
			{
				assist2.doRevive();
			}
			
			if (assist3 != null && assist3.isOnline() == 1 && assist3.isDead())
			{
				assist3.doRevive();
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
			
			if (assist != null && assist.isOnline() == 1)
			{
				for (L2Effect effect : assist.getAllEffects())
				{
					if (effect.getSkill().getId() == 406 || effect.getSkill().getId() == 139 || effect.getSkill().getId() == 176 || effect.getSkill().getId() == 420)
					{
						assist.stopSkillEffects(effect.getSkill().getId());
						assist.enableSkill(effect.getSkill());
					}
				}
			}
			
			if (assist2 != null && assist2.isOnline() == 1)
			{
				for (L2Effect effect : assist2.getAllEffects())
				{
					if (effect.getSkill().getId() == 406 || effect.getSkill().getId() == 139 || effect.getSkill().getId() == 176 || effect.getSkill().getId() == 420)
					{
						assist2.stopSkillEffects(effect.getSkill().getId());
						assist2.enableSkill(effect.getSkill());
					}
				}
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				for (L2Effect effect : assist3.getAllEffects())
				{
					if (effect.getSkill().getId() == 406 || effect.getSkill().getId() == 139 || effect.getSkill().getId() == 176 || effect.getSkill().getId() == 420)
					{
						assist3.stopSkillEffects(effect.getSkill().getId());
						assist3.enableSkill(effect.getSkill());
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
			if (assist != null && assist.isOnline() == 1)
			{
				// Remove Summon's buffs
				if (assist.getPet() != null)
				{
					L2Summon summon = assist.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof L2PetInstance)
					{
						summon.unSummon(assist);
					}
				}
				
				if (assist.getMountType() == 1 || assist.getMountType() == 2)
				{
					assist.dismount();
				}
			}
			if (assist2 != null && assist2.isOnline() == 1)
			{
				// Remove Summon's buffs
				if (assist2.getPet() != null)
				{
					L2Summon summon = assist2.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof L2PetInstance)
					{
						summon.unSummon(assist2);
					}
				}
				
				if (assist2.getMountType() == 1 || assist2.getMountType() == 2)
				{
					assist2.dismount();
				}
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				// Remove Summon's buffs
				if (assist3.getPet() != null)
				{
					L2Summon summon = assist3.getPet();
					if (summon != null)
					{
						summon.unSummon(summon.getOwner());
					}
					
					if (summon instanceof L2PetInstance)
					{
						summon.unSummon(assist3);
					}
				}
				
				if (assist3.getMountType() == 1 || assist3.getMountType() == 2)
				{
					assist3.dismount();
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
			if (assist != null && assist.isOnline() == 1)
			{
				assist.setIsInvul(val);
				assist.setIsParalyzed(val);
			}
			if (assist2 != null && assist2.isOnline() == 1)
			{
				assist2.setIsInvul(val);
				assist2.setIsParalyzed(val);
			}
			if (assist3 != null && assist3.isOnline() == 1)
			{
				assist3.setIsInvul(val);
				assist3.setIsParalyzed(val);
			}
		}
		
		public void sendPacket(String message, int duration)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
			
			if (assist != null && assist.isOnline() == 1)
			{
				assist.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
			
			if (assist2 != null && assist2.isOnline() == 1)
			{
				assist2.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				assist3.sendPacket(new ExShowScreenMessage(message, duration * 1000));
			}
		}
		
		public void initCountdown(int duration)
		{
			if (leader != null && leader.isOnline() == 1)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new Countdown(leader, duration), 0);
			}
			
			if (assist != null && assist.isOnline() == 1)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new Countdown(assist, duration), 0);
			}
			
			if (assist2 != null && assist2.isOnline() == 1)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new Countdown(assist2, duration), 0);
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new Countdown(assist3, duration), 0);
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
			
			if (assist != null && assist.isOnline() == 1)
			{
				assist.stopPvPFlag();
				assist.setPvpFlag(1);
				assist.broadcastUserInfo();
			}
			
			if (assist2 != null && assist2.isOnline() == 1)
			{
				assist2.stopPvPFlag();
				assist2.setPvpFlag(1);
				assist2.broadcastUserInfo();
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				assist3.stopPvPFlag();
				assist3.setPvpFlag(1);
				assist3.broadcastUserInfo();
			}
		}
		
		public void setUnFlag()
		{
			if (leader != null && leader.isOnline() == 1)
			{
				leader.setPvpFlag(0);
				leader.broadcastUserInfo();
			}
			
			if (assist != null && assist.isOnline() == 1)
			{
				assist.setPvpFlag(0);
				assist.broadcastUserInfo();
			}
			
			if (assist2 != null && assist2.isOnline() == 1)
			{
				assist2.setPvpFlag(0);
				assist2.broadcastUserInfo();
			}
			
			if (assist3 != null && assist3.isOnline() == 1)
			{
				assist3.setPvpFlag(0);
				assist3.broadcastUserInfo();
			}
		}
	}
	
	private class EvtArenaTask implements Runnable
	{
		private final Team pairOne;
		private final Team pairTwo;
		private final int pOneX, pOneY, pOneZ, pTwoX, pTwoY, pTwoZ;
		private Arena arena;
		
		public EvtArenaTask(List<Team> opponents)
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
				Thread.sleep(Config.ARENA_WAIT_INTERVAL_4X4);
			}
			catch (InterruptedException e1)
			{
				e1.printStackTrace();
			}
			pairOne.sendPacket("The battle starts NOW! Good Fight!", 5);
			pairTwo.sendPacket("The battle starts NOW! Good Fight!", 5);
			pairOne.setImobilised(false);
			pairTwo.setImobilised(false);
			pairOne.removeBuff();
			pairTwo.removeBuff();
			pairOne.removeSummon();
			pairTwo.removeSummon();
			
			while (check())
			{
				// check players status each seconds
				try
				{
					Thread.sleep(Config.ARENA_CHECK_INTERVAL_4X4);
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
			pairOne.setUnFlag();
			pairTwo.setUnFlag();
			pairOne.revive();
			pairTwo.revive();
			pairOne.teleportTo(pOneX, pOneY, pOneZ);
			pairTwo.teleportTo(pTwoX, pTwoY, pTwoZ);
			pairOne.setInTournamentEvent(false);
			pairTwo.setInTournamentEvent(false);
			arena.setFree(true);
		}
		
		private void rewardWinner()
		{
			if (pairOne.isAlive() && !pairTwo.isAlive())
			{
				L2PcInstance leader1 = pairOne.getLeader();
				L2PcInstance leader2 = pairTwo.getLeader();
				
				if (leader1.getClan() != null && leader2.getClan() != null)
				{
					Announcements.getInstance().gameAnnounceToAll("4x4: " + leader1.getClan().getName() + " VS " + leader2.getClan().getName() + ". Winner is: " + leader1.getClan().getName() + "!");
				}
				
				pairOne.rewards();
			}
			else if (pairTwo.isAlive() && !pairOne.isAlive())
			{
				L2PcInstance leader1 = pairTwo.getLeader();
				L2PcInstance leader2 = pairOne.getLeader();
				
				if (leader1.getClan() != null && leader2.getClan() != null)
				{
					Announcements.getInstance().gameAnnounceToAll("4x4: " + leader1.getClan().getName() + " VS " + leader2.getClan().getName() + ". Winner is: " + leader1.getClan().getName() + "!");
				}
				
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
		protected static final Arena4x4 INSTANCE = new Arena4x4();
	}
}