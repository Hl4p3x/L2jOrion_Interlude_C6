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
package l2jorion.game.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import l2jorion.Config;
import l2jorion.game.enums.AchType;
import l2jorion.game.managers.DimensionalRiftManager;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.Earthquake;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

public class DimensionalRift
{
	protected byte _type;
	protected L2Party _party;
	protected List<Byte> _completedRooms = new ArrayList<>();
	private static final long seconds_5 = 5000L;
	protected byte jumps_current = 0;
	
	private Timer teleporterTimer;
	private TimerTask teleporterTimerTask;
	private Timer spawnTimer;
	private TimerTask spawnTimerTask;
	
	private Future<?> earthQuakeTask;
	
	protected byte _choosenRoom = -1;
	private boolean _hasJumped = false;
	protected List<L2PcInstance> _deadPlayers = new CopyOnWriteArrayList<>();
	protected List<L2PcInstance> _revivedInWaitingRoom = new CopyOnWriteArrayList<>();
	private boolean isBossRoom = false;
	
	public DimensionalRift(L2Party party, byte type, byte room)
	{
		DimensionalRiftManager.getInstance().getRoom(type, room).setPartyInside(true);
		
		_type = type;
		_party = party;
		_choosenRoom = room;
		Location coords = getRoomCoord(room);
		
		party.setDimensionalRift(this);
		
		for (L2PcInstance p : party.getPartyMembers())
		{
			p.teleToLocation(coords);
			p.getAchievement().increase(AchType.ENTER_RIFT);
		}
		
		createSpawnTimer(_choosenRoom);
		createTeleporterTimer(true);
	}
	
	public byte getType()
	{
		return _type;
	}
	
	public byte getCurrentRoom()
	{
		return _choosenRoom;
	}
	
	protected void createTeleporterTimer(final boolean reasonTP)
	{
		if (_party == null)
		{
			return;
		}
		
		if (teleporterTimerTask != null)
		{
			teleporterTimerTask.cancel();
			teleporterTimerTask = null;
		}
		
		if (teleporterTimer != null)
		{
			teleporterTimer.cancel();
			teleporterTimer = null;
		}
		
		if (earthQuakeTask != null)
		{
			earthQuakeTask.cancel(false);
			earthQuakeTask = null;
		}
		
		teleporterTimer = new Timer();
		teleporterTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				if (_choosenRoom > -1)
				{
					DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn().setPartyInside(false);
				}
				
				if (reasonTP && (jumps_current < getMaxJumps()) && (_party.getMemberCount() > _deadPlayers.size()))
				{
					jumps_current++;
					
					_completedRooms.add(_choosenRoom);
					_choosenRoom = -1;
					
					for (L2PcInstance p : _party.getPartyMembers())
					{
						if (!_revivedInWaitingRoom.contains(p))
						{
							teleportToNextRoom(p);
						}
					}
					createTeleporterTimer(true);
					createSpawnTimer(_choosenRoom);
				}
				else
				{
					for (L2PcInstance p : _party.getPartyMembers())
					{
						if (!_revivedInWaitingRoom.contains(p))
						{
							teleportToWaitingRoom(p);
						}
					}
					killRift();
					cancel();
				}
			}
		};
		
		if (reasonTP)
		{
			long jumpTime = calcTimeToNextJump();
			teleporterTimer.schedule(teleporterTimerTask, jumpTime); // Teleporter task, 8-10 minutes
			
			earthQuakeTask = ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				for (L2PcInstance p : _party.getPartyMembers())
				{
					if (!_revivedInWaitingRoom.contains(p))
					{
						p.sendPacket(new Earthquake(p.getX(), p.getY(), p.getZ(), 65, 9));
					}
				}
			}, jumpTime - 7000);
		}
		else
		{
			teleporterTimer.schedule(teleporterTimerTask, seconds_5); // incorrect party member invited.
		}
	}
	
	public void createSpawnTimer(final byte room)
	{
		if (spawnTimerTask != null)
		{
			spawnTimerTask.cancel();
			spawnTimerTask = null;
		}
		
		if (spawnTimer != null)
		{
			spawnTimer.cancel();
			spawnTimer = null;
		}
		
		spawnTimer = new Timer();
		spawnTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				DimensionalRiftManager.getInstance().getRoom(_type, room).spawn();
			}
		};
		
		spawnTimer.schedule(spawnTimerTask, Config.RIFT_SPAWN_DELAY);
	}
	
	public void partyMemberInvited()
	{
		createTeleporterTimer(false);
	}
	
	public void partyMemberExited(L2PcInstance player)
	{
		if (_deadPlayers.contains(player))
		{
			_deadPlayers.remove(player);
		}
		
		if (_revivedInWaitingRoom.contains(player))
		{
			_revivedInWaitingRoom.remove(player);
		}
		
		if ((_party.getMemberCount() < Config.RIFT_MIN_PARTY_SIZE) || (_party.getMemberCount() == 1))
		{
			for (L2PcInstance p : _party.getPartyMembers())
			{
				teleportToWaitingRoom(p);
			}
			killRift();
		}
	}
	
	public void manualTeleport(L2PcInstance player, L2NpcInstance npc)
	{
		if (!player.isInParty() || !player.getParty().isInDimensionalRift())
		{
			return;
		}
		
		if (player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
		}
		
		if (_hasJumped)
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/AlreadyTeleported.htm", npc);
			return;
		}
		
		_hasJumped = true;
		
		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn().setPartyInside(false);
		_completedRooms.add(_choosenRoom);
		_choosenRoom = -1;
		
		for (L2PcInstance p : _party.getPartyMembers())
		{
			teleportToNextRoom(p);
		}
		
		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).setPartyInside(true);
		
		createSpawnTimer(_choosenRoom);
		createTeleporterTimer(true);
	}
	
	public void manualExitRift(L2PcInstance player, L2NpcInstance npc)
	{
		if (!player.isInParty() || !player.getParty().isInDimensionalRift())
		{
			return;
		}
		
		if (player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
		}
		
		for (L2PcInstance p : player.getParty().getPartyMembers())
		{
			teleportToWaitingRoom(p);
		}
		killRift();
	}
	
	protected void teleportToNextRoom(L2PcInstance player)
	{
		if (_choosenRoom == -1)
		{
			List<Byte> emptyRooms;
			do
			{
				emptyRooms = DimensionalRiftManager.getInstance().getFreeRooms(_type);
				// Do not tp in the same room a second time
				emptyRooms.removeAll(_completedRooms);
				// If no room left, find any empty
				if (emptyRooms.isEmpty())
				{
					emptyRooms = DimensionalRiftManager.getInstance().getFreeRooms(_type);
				}
				_choosenRoom = emptyRooms.get(Rnd.get(1, emptyRooms.size()) - 1);
			}
			while (DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).isPartyInside());
		}
		
		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).setPartyInside(true);
		checkBossRoom(_choosenRoom);
		player.teleToLocation(getRoomCoord(_choosenRoom));
	}
	
	protected void teleportToWaitingRoom(L2PcInstance player)
	{
		DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
	}
	
	public void killRift()
	{
		_completedRooms.clear();
		
		if (_party != null)
		{
			_party.setDimensionalRift(null);
		}
		
		_party = null;
		_revivedInWaitingRoom = null;
		_deadPlayers = null;
		
		if (earthQuakeTask != null)
		{
			earthQuakeTask.cancel(false);
			earthQuakeTask = null;
		}
		
		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn().setPartyInside(false);
		DimensionalRiftManager.getInstance().killRift(this);
	}
	
	public Timer getTeleportTimer()
	{
		return teleporterTimer;
	}
	
	public TimerTask getTeleportTimerTask()
	{
		return teleporterTimerTask;
	}
	
	public Timer getSpawnTimer()
	{
		return spawnTimer;
	}
	
	public TimerTask getSpawnTimerTask()
	{
		return spawnTimerTask;
	}
	
	public void setTeleportTimer(Timer t)
	{
		teleporterTimer = t;
	}
	
	public void setTeleportTimerTask(TimerTask tt)
	{
		teleporterTimerTask = tt;
	}
	
	public void setSpawnTimer(Timer t)
	{
		spawnTimer = t;
	}
	
	public void setSpawnTimerTask(TimerTask st)
	{
		spawnTimerTask = st;
	}
	
	private long calcTimeToNextJump()
	{
		int time = Rnd.get(Config.RIFT_AUTO_JUMPS_TIME_MIN, Config.RIFT_AUTO_JUMPS_TIME_MAX) * 1000;
		
		if (isBossRoom)
		{
			return (long) (time * Config.RIFT_BOSS_ROOM_TIME_MUTIPLY);
		}
		return time;
	}
	
	public void memberDead(L2PcInstance player)
	{
		if (!_deadPlayers.contains(player))
		{
			_deadPlayers.add(player);
		}
	}
	
	public void memberRessurected(L2PcInstance player)
	{
		if (_deadPlayers.contains(player))
		{
			_deadPlayers.remove(player);
		}
	}
	
	public void usedTeleport(L2PcInstance player)
	{
		if (!_revivedInWaitingRoom.contains(player))
		{
			_revivedInWaitingRoom.add(player);
		}
		
		if (!_deadPlayers.contains(player))
		{
			_deadPlayers.add(player);
		}
		
		if ((_party.getMemberCount() - _revivedInWaitingRoom.size()) < Config.RIFT_MIN_PARTY_SIZE)
		{
			for (L2PcInstance p : _party.getPartyMembers())
			{
				if ((p != null) && !_revivedInWaitingRoom.contains(p))
				{
					teleportToWaitingRoom(p);
				}
			}
			killRift();
		}
	}
	
	public List<L2PcInstance> getDeadMemberList()
	{
		return _deadPlayers;
	}
	
	public List<L2PcInstance> getRevivedAtWaitingRoom()
	{
		return _revivedInWaitingRoom;
	}
	
	public void checkBossRoom(byte room)
	{
		isBossRoom = DimensionalRiftManager.getInstance().getRoom(_type, room).isBossRoom();
	}
	
	public Location getRoomCoord(byte room)
	{
		return DimensionalRiftManager.getInstance().getRoom(_type, room).getTeleportCoorinates();
	}
	
	public byte getMaxJumps()
	{
		if ((Config.RIFT_MAX_JUMPS <= 8) && (Config.RIFT_MAX_JUMPS >= 1))
		{
			return (byte) Config.RIFT_MAX_JUMPS;
		}
		return 4;
	}
}
