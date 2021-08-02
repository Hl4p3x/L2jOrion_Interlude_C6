/*
 * Copyright (C) 2004-2016 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.entity.siege.hallsiege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2SiegeClan;
import l2jorion.game.model.L2SiegeClan.SiegeClanType;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Siegable;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.NpcSay;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public abstract class ClanHallSiegeEngine extends Quest implements Siegable
{
	public static final Logger LOG = LoggerFactory.getLogger(ClanHallSiegeEngine.class);
	
	private static final String SQL_LOAD_ATTACKERS = "SELECT attacker_id FROM clanhall_siege_attackers WHERE clanhall_id = ?";
	private static final String SQL_SAVE_ATTACKERS = "INSERT INTO clanhall_siege_attackers VALUES (?,?)";
	private static final String SQL_LOAD_GUARDS = "SELECT * FROM clanhall_siege_guards WHERE clanHallId = ?";
	
	public static final int FORTRESS_RESSISTANCE = 21;
	public static final int DEVASTATED_CASTLE = 34;
	public static final int BANDIT_STRONGHOLD = 35;
	public static final int RAINBOW_SPRINGS = 62;
	public static final int BEAST_FARM = 63;
	public static final int FORTRESS_OF_DEAD = 64;
	
	private final Map<Integer, L2SiegeClan> _attackers = new ConcurrentHashMap<>();
	private List<L2Spawn> _guards;
	
	public SiegableHall _hall;
	public ScheduledFuture<?> _siegeTask;
	public boolean _missionAccomplished = false;
	
	public ClanHallSiegeEngine(String name, String descr, final int hallId)
	{
		super(-1, name, descr);
		
		_hall = CHSiegeManager.getInstance().getSiegableHall(hallId);
		_hall.setSiege(this);
		
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareOwner(), _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		LOG.info("{} siege scheduled for {}.", _hall.getName(), getSiegeDate().getTime());
		loadAttackers();
	}
	
	public void loadAttackers()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_LOAD_ATTACKERS);
			
			ps.setInt(1, _hall.getId());
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final int id = rset.getInt("attacker_id");
					L2SiegeClan clan = new L2SiegeClan(id, SiegeClanType.ATTACKER);
					_attackers.put(id, clan);
				}
			}
		}
		catch (Exception e)
		{
			LOG.warn("{}: Could not load siege attackers!", getName(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public final void saveAttackers()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM clanhall_siege_attackers WHERE clanhall_id = ?");
			
			ps.setInt(1, _hall.getId());
			ps.execute();
			
			if (_attackers.size() > 0)
			{
				try (PreparedStatement insert = con.prepareStatement(SQL_SAVE_ATTACKERS))
				{
					for (L2SiegeClan clan : _attackers.values())
					{
						insert.setInt(1, _hall.getId());
						insert.setInt(2, clan.getClanId());
						insert.execute();
						insert.clearParameters();
					}
				}
			}
			LOG.info("{}: Successfully saved attackers to database.", getName());
		}
		catch (Exception e)
		{
			LOG.warn("{}: Couldnt save attacker list!", getName(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public final void loadGuards()
	{
		if (_guards == null)
		{
			Connection con = null;
			_guards = new ArrayList<>();
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(SQL_LOAD_GUARDS);
				
				ps.setInt(1, _hall.getId());
				try (ResultSet rset = ps.executeQuery())
				{
					while (rset.next())
					{
						final L2Spawn spawn = new L2Spawn(rset.getInt("npcId"));
						spawn.setLocx(rset.getInt("x"));
						spawn.setLocy(rset.getInt("y"));
						spawn.setLocz(rset.getInt("z"));
						spawn.setHeading(rset.getInt("heading"));
						spawn.setRespawnDelay(rset.getInt("respawnDelay"));
						spawn.setAmount(1);
						_guards.add(spawn);
					}
				}
			}
			catch (Exception e)
			{
				LOG.warn("{}: Couldn't load siege guards!", getName(), e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	private final void spawnSiegeGuards()
	{
		for (L2Spawn guard : _guards)
		{
			guard.init();
		}
	}
	
	private final void unSpawnSiegeGuards()
	{
		if (_guards != null)
		{
			for (L2Spawn guard : _guards)
			{
				guard.stopRespawn();
				if (guard.getLastSpawn() != null)
				{
					guard.getLastSpawn().deleteMe();
				}
			}
		}
	}
	
	@Override
	public List<L2NpcInstance> getFlag(L2Clan clan)
	{
		List<L2NpcInstance> result = null;
		L2SiegeClan sClan = getAttackerClan(clan);
		if (sClan != null)
		{
			result = sClan.getFlag();
		}
		return result;
	}
	
	public final Map<Integer, L2SiegeClan> getAttackers()
	{
		return _attackers;
	}
	
	@Override
	public boolean checkIsAttacker(L2Clan clan)
	{
		if (clan == null)
		{
			return false;
		}
		
		return _attackers.containsKey(clan.getClanId());
	}
	
	@Override
	public boolean checkIsDefender(L2Clan clan)
	{
		return false;
	}
	
	@Override
	public L2SiegeClan getAttackerClan(int clanId)
	{
		return _attackers.get(clanId);
	}
	
	@Override
	public L2SiegeClan getAttackerClan(L2Clan clan)
	{
		return getAttackerClan(clan.getClanId());
	}
	
	@Override
	public List<L2SiegeClan> getAttackerClans()
	{
		return new ArrayList<>(_attackers.values());
	}
	
	@Override
	public List<L2PcInstance> getAttackersInZone()
	{
		final List<L2PcInstance> attackers = new ArrayList<>();
		for (L2PcInstance pc : _hall.getSiegeZone().getPlayersInside())
		{
			final L2Clan clan = pc.getClan();
			if ((clan != null) && _attackers.containsKey(clan.getClanId()))
			{
				attackers.add(pc);
			}
		}
		return attackers;
	}
	
	@Override
	public L2SiegeClan getDefenderClan(int clanId)
	{
		return null;
	}
	
	@Override
	public L2SiegeClan getDefenderClan(L2Clan clan)
	{
		return null;
	}
	
	@Override
	public List<L2SiegeClan> getDefenderClans()
	{
		return null;
	}
	
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			final L2SiegeClan clan = new L2SiegeClan(_hall.getOwnerId(), SiegeClanType.ATTACKER);
			_attackers.put(clan.getClanId(), new L2SiegeClan(clan.getClanId(), SiegeClanType.ATTACKER));
		}
		
		_hall.free();
		_hall.banishForeigners();
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.REGISTRATION_TERM_FOR_S1_ENDED);
		msg.addString(getName());
		Broadcast.toAllOnlinePlayers(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
		
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStarts(), 3600000);
	}
	
	@Override
	public void startSiege()
	{
		if ((_attackers.size() < 1) && (_hall.getId() != 21)) // Fortress of resistance don't have attacker list
		{
			onSiegeEnds();
			_attackers.clear();
			_hall.updateNextSiege();
			_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareOwner(), _hall.getSiegeDate().getTimeInMillis());
			_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(sm);
			return;
		}
		
		_hall.spawnDoor(false);
		
		_hall.updateSiegeZone(true);
		
		final byte state = 1;
		for (L2SiegeClan sClan : _attackers.values())
		{
			final L2Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			for (L2PcInstance pc : clan.getOnlineMembers(""))
			{
				pc.setSiegeState(state);
				pc.broadcastUserInfo();
				pc.setIsInHideoutSiege(true);
			}
		}
		
		_hall.updateSiegeStatus(SiegeStatus.RUNNING);
		
		loadGuards();
		spawnSiegeGuards();
		
		onSiegeStarts();
		
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEnds(), _hall.getSiegeLenght());
	}
	
	@Override
	public void endSiege()
	{
		SystemMessage end = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_ENDED);
		end.addString(_hall.getName());
		Broadcast.toAllOnlinePlayers(end);
		
		L2Clan winner = getWinner();
		
		SystemMessage finalMsg = null;
		if (_missionAccomplished && (winner != null))
		{
			_hall.setOwner(winner);
			winner.setHasHideout(_hall.getId());
			finalMsg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_VICTORIOUS_OVER_S2_S_SIEGE);
			finalMsg.addString(winner.getName());
			finalMsg.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(finalMsg);
		}
		else
		{
			finalMsg = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_S1_DRAW);
			finalMsg.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(finalMsg);
		}
		_missionAccomplished = false;
		
		_hall.updateSiegeZone(false);
		_hall.updateNextSiege();
		_hall.spawnDoor(false);
		_hall.banishForeigners();
		
		final byte state = 0;
		for (L2SiegeClan sClan : _attackers.values())
		{
			final L2Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			for (L2PcInstance player : clan.getOnlineMembers(""))
			{
				player.setSiegeState(state);
				player.broadcastUserInfo();
				player.setIsInHideoutSiege(false);
			}
		}
		
		// Update pvp flag for winners when siege zone becomes inactive
		for (L2Character chr : _hall.getSiegeZone().getCharactersInside())
		{
			if ((chr != null) && chr instanceof L2PcInstance)
			{
				chr.getActingPlayer().startPvPFlag();
			}
		}
		
		_attackers.clear();
		
		onSiegeEnds();
		
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareOwner(), _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		LOG.info("Siege of {} scheduled for {}.", _hall.getName(), _hall.getSiegeDate().getTime());
		
		_hall.updateSiegeStatus(SiegeStatus.REGISTERING);
		unSpawnSiegeGuards();
	}
	
	@Override
	public void updateSiege()
	{
		cancelSiegeTask();
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareOwner(), _hall.getNextSiegeTime() - 3600000);
		LOG.info("{} siege scheduled for {}.", _hall.getName(), _hall.getSiegeDate().getTime());
	}
	
	public void cancelSiegeTask()
	{
		if (_siegeTask != null)
		{
			_siegeTask.cancel(false);
		}
	}
	
	@Override
	public Calendar getSiegeDate()
	{
		return _hall.getSiegeDate();
	}
	
	public final void broadcastNpcSay(final L2NpcInstance npc, final int type, final String messageId)
	{
		final NpcSay npcSay = new NpcSay(npc.getObjectId(), type, npc.getNpcId(), messageId);
		int region = MapRegionTable.getInstance().getMapRegionLocId(npc.getX(), npc.getY());
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (region == MapRegionTable.getInstance().getMapRegionLocId(player.getX(), player.getY()))
			{
				player.sendPacket(npcSay);
			}
		}
	}
	
	public Location getInnerSpawnLoc(L2PcInstance player)
	{
		return null;
	}
	
	public boolean canPlantFlag()
	{
		return true;
	}
	
	public boolean doorIsAutoAttackable()
	{
		return true;
	}
	
	public void onSiegeStarts()
	{
	}
	
	public void onSiegeEnds()
	{
	}
	
	public abstract L2Clan getWinner();
	
	public class PrepareOwner implements Runnable
	{
		@Override
		public void run()
		{
			prepareOwner();
		}
	}
	
	public class SiegeStarts implements Runnable
	{
		@Override
		public void run()
		{
			startSiege();
		}
	}
	
	public class SiegeEnds implements Runnable
	{
		@Override
		public void run()
		{
			endSiege();
		}
	}
}