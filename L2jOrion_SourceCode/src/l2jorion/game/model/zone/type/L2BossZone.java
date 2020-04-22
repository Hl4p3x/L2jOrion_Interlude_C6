/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.zone.type;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import l2jorion.Config;
import l2jorion.game.GameServer;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.log.Log;

public class L2BossZone extends L2ZoneType
{
	private String _zoneName;
	private int _timeInvade;
	private int _levelLimit;
	private boolean _enabled = true; // default value, unless overridden by xml...
	private boolean _LvlLimit = false;
	private boolean _IsFlyingEnable = true; // default value, unless overridden by xml...
	private boolean _AutoPvpFlag = false;
	private boolean _AutoPvpFlagBZ = false;
	private boolean _InvadeTimeSystem = false;
	private boolean _AllowCursedWeapons = true;
	
	private final Map<Integer, Long> _playerAllowedReEntryTimes;
	private List<Integer> _playersAllowed;

	private int _bossId;
	
	public L2BossZone(int id, int boss_id)
	{
		super(id);
		_bossId = boss_id;
		_playerAllowedReEntryTimes = new ConcurrentHashMap<>();
		_playersAllowed = new CopyOnWriteArrayList<>();
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("name"))
		{
			_zoneName = value;
		}
		else if (name.equals("InvadeTime"))
		{
			_timeInvade = Integer.parseInt(value);
		}
		else if (name.equals("LvlLimit"))
		{
			_LvlLimit = Boolean.parseBoolean(value);
		}
		else if (name.equals("LevelLimit"))
		{
			_levelLimit = Integer.parseInt(value);
		}
		else if (name.equals("EnabledByDefault"))
		{
			_enabled = Boolean.parseBoolean(value);
		}
		else if (name.equals("flying"))
		{
			_IsFlyingEnable = Boolean.parseBoolean(value);
		}
		else if (name.equals("AutoPvpFlag"))
		{
			_AutoPvpFlag = Boolean.parseBoolean(value);
		}
		else if (name.equals("AutoPvpFlagBZ"))
		{
			_AutoPvpFlagBZ = Boolean.parseBoolean(value);
		}
		else if (name.equals("AllowCursedWeapons"))
		{
			_AllowCursedWeapons = Boolean.parseBoolean(value);
		}
		else if (name.equals("InvadeTimeSystem"))
		{
			_InvadeTimeSystem = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (_enabled)
		{
			if (character != null)
			{
				character.setInsideZone(L2Character.ZONE_BOSS, true);
			}
			
			if (character instanceof L2PcInstance)
			{
				character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
				final L2PcInstance player = (L2PcInstance)character;
				
				if(Config.BOT_PROTECTOR)
				{
					player.stopBotChecker();
				}
				
				if (player.isGM())
				{
					player.sendMessage("You entered to "+ _zoneName +" ID:"+getId());
					return;
				}
				
				// Ignore the check for Van Halter zone id 12014 if player got marks
				if (getId() == 12014)
				{
					final L2ItemInstance VisitorsMark = player.getInventory().getItemByItemId(8064);
					final L2ItemInstance FadedVisitorsMark = player.getInventory().getItemByItemId(8065);
					final L2ItemInstance PagansMark = player.getInventory().getItemByItemId(8067);

					final long mark1 = VisitorsMark == null ? 0 : VisitorsMark.getCount();
					final long mark2 = FadedVisitorsMark == null ? 0 : FadedVisitorsMark.getCount();
					final long mark3 = PagansMark == null ? 0 : PagansMark.getCount();
					
					if (mark1 != 0 || mark2 != 0 || mark3 != 0)
						return;
				}
				
				if (player.isFlying() && !_IsFlyingEnable)
				{
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					String text = "PROTECTION: " + player.getName() + " is teleported to Town ";
					Log.add(text, "Flying_protection");
				}
				
				if (_AutoPvpFlag)
				{
					player.stopPvPFlag();
					player.updatePvPFlag(1);
				}
				
				if (_AutoPvpFlagBZ)
				{
					player.stopPvPFlag();
					player.updatePvPFlag(1);
				}
				
				if (_LvlLimit)
				{
					if (player.getLevel() > _levelLimit)
					{
						player.sendMessage(player.getName()+", your level was too high (Limit: "+_levelLimit+")! Teleporting to town...");
						player.sendPacket(new ExShowScreenMessage(""+ player.getName()+", your level was too high (Limit: "+_levelLimit+")! Teleporting to town...", 3000, 0x02, false));
						player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
				}
				
				if (!_AllowCursedWeapons && player.isCursedWeaponEquiped())
				{
					player.sendMessage(player.getName()+", cursed weapon is not allowed here anymore.");
					player.sendPacket(new ExShowScreenMessage(""+ player.getName()+", cursed weapon is not allowed here anymore.", 3000, 0x02, false));
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				
				if (!player.isEnteringToWorld())
				{
					//Zaken zone
					if (getId() == 12011 && player.isMoving() || getId() == 12011 && player.isTeleporting() || getId() == 12011 && player.isRunning())
					{
						allowPlayerEntry(player,1);
					}
					
					//queen zone
					if (getId() == 12019 && player.isMoving() || getId() == 12019 && player.isTeleporting() || getId() == 12019 && player.isRunning())
					{
						allowPlayerEntry(player,1);
					}
				}
				
				if (_playersAllowed.contains(player.getObjectId()))
				{
					final Long expirationTime = _playerAllowedReEntryTimes.get(player.getObjectId());
					if (expirationTime == null)
					{
						long serverStartTime = GameServer.dateTimeServerStarted.getTimeInMillis();
						if (serverStartTime > (System.currentTimeMillis() - _timeInvade))
						{
							return;
						}
					}
					else
					{
						_playerAllowedReEntryTimes.remove(player.getObjectId());
						if (expirationTime.longValue() > System.currentTimeMillis())
						{
							return;
						}
					}
					_playersAllowed.remove(_playersAllowed.indexOf(player.getObjectId()));
				}
				
				if (_InvadeTimeSystem)
				{
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
			}
			else if (character instanceof L2Summon)
			{
				final L2PcInstance player = ((L2Summon)character).getOwner();
				if (player != null && (_LvlLimit) && player.getLevel() > _levelLimit)
				{
					((L2Summon)character).unSummon(player);
				}
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (_enabled)
		{
			if (character != null)
			{
				character.setInsideZone(L2Character.ZONE_BOSS, false);
			}
			
			if (character instanceof L2PcInstance)
			{
				character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
				L2PcInstance player = (L2PcInstance)character;
				
				if(Config.BOT_PROTECTOR)
				{
					player.startBotChecker();
				}
				
				if (player.isGM())
				{
					player.sendMessage("You left " + _zoneName);
					return;
				}
				if (_AutoPvpFlag)
				{
					player.stopPvPFlag();
					player.updatePvPStatus();
				}
				if (_AutoPvpFlagBZ)
				{
					player.stopPvPFlag();
					player.updatePvPStatus();
				}
				
				// remove effect of frintezza zone
				if (getId() == 12016)
				{
					player.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_DANCE_STUNNED);
					player.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_FLOATING_ROOT);
					player.enableAllSkills();
					player.setIsImobilised(false);
					player.setIsParalyzed(false);
				}
				
				// if the player just got disconnected/logged out, store the dc time so that
				// decisions can be made later about allowing or not the player to log into the zone
				if (player.isOnline() == 0 && _playersAllowed.contains(character.getObjectId()))
				{
					// mark the time that the player left the zone
					_playerAllowedReEntryTimes.put(character.getObjectId(), System.currentTimeMillis() + _timeInvade);
				}
				else
				{
					if (_playersAllowed.contains(player.getObjectId()))
						_playersAllowed.remove(_playersAllowed.indexOf(player.getObjectId()));
					_playerAllowedReEntryTimes.remove(player.getObjectId());
				}
			}
		}
	}

	public void setZoneEnabled(boolean flag)
	{
		if (_enabled != flag)
			oustAllPlayers();

		_enabled = flag;
	}

	public String getZoneName()
	{
		return _zoneName;
	}

	public int getTimeInvade()
	{
		return _timeInvade;
	}

	public void setAllowedPlayers(List<Integer> list)
	{
		if (list != null)
			_playersAllowed = list;
	}

	public List<Integer> getAllowedPlayers()
	{
		return _playersAllowed;
	}

	public boolean isPlayerAllowed(L2PcInstance player)
	{
		if (player.isGM())
			return true;
		else if (_playersAllowed.contains(player.getObjectId()))
			return true;
		else
		{
			String text = "PROTECTION: " + player.getName() + " is teleported to Town ";
			Log.add(text, "Access_denied_RBZONE2");
			player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			return false;
		}
	}
	
	/**
	 * Some GrandBosses send all players in zone to a specific part of the zone,
	 * rather than just removing them all. If this is the case, this command should
	 * be used. If this is no the case, then use oustAllPlayers().
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	public void movePlayersTo(int x, int y, int z)
	{
		if (_characterList.isEmpty())
			return;

		for (final L2Character character : _characterList.values())
		{
			if (character instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance)character;
				if (player.isOnline() == 1)
					player.teleToLocation(x, y, z);
			}
		}
	}

	/**
	 * Occasionally, all players need to be sent out of the zone (for example, if the players are just running around
	 * without fighting for too long, or if all players die, etc). This call sends all online players to town and marks
	 * offline players to be teleported (by clearing their relog expiration times) when they log back in (no real need
	 * for off-line teleport).
	 */
	public void oustAllPlayers()
	{
		if (_characterList == null)
			return;
		
		if (_characterList.isEmpty())
			return;
		
		for (L2Character character : _characterList.values())
		{
			if (character == null)
				continue;
			
			if (character instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) character;
				if (player.isOnline() == 1)
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
		}
		
		if (_playerAllowedReEntryTimes != null)
			_playerAllowedReEntryTimes.clear();

		if (_playersAllowed != null)
			_playersAllowed.clear();
	}

	/**
	 * This function is to be used by external sources, such as quests and AI in order to allow a player for entry into
	 * the zone for some time. Naturally if the player does not enter within the allowed time, he/she will be teleported
	 * out again...
	 * 
	 * @param player reference to the player we wish to allow
	 * @param durationInSec amount of time in seconds during which entry is valid.
	 */
	public void allowPlayerEntry(L2PcInstance player, int durationInSec)
	{
		if (!player.isGM())
		{
			if (!_playersAllowed.contains(player.getObjectId()))
				_playersAllowed.add(player.getObjectId());
			_playerAllowedReEntryTimes.put(player.getObjectId(), System.currentTimeMillis() + durationInSec * 1000);
		}
	}
	
	public void removePlayer(L2PcInstance player)
	{
		if(!player.isGM())
		{
			_playersAllowed.remove(Integer.valueOf(player.getObjectId()));
			_playerAllowedReEntryTimes.remove(player.getObjectId());
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{}

	@Override
	public void onReviveInside(L2Character character)
	{}
	
	public void updateKnownList(L2NpcInstance npc)
	{
		if (_characterList == null || _characterList.isEmpty())
			return;
		
		final Map<Integer, L2PcInstance> npcKnownPlayers = npc.getKnownList().getKnownPlayers();
		for (final L2Character character : _characterList.values())
		{
			if (character == null)
				continue;

			if (character instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance)character;
				if (player.isOnline() == 1 || player.isInOfflineMode())
				{
					npcKnownPlayers.put(player.getObjectId(), player);
				}
			}
		}
		return;
	}

	public int getBossId()
	{
		return _bossId;
	}
}