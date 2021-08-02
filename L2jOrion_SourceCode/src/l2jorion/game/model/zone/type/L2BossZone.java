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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import l2jorion.Config;
import l2jorion.game.GameServer;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.util.StringUtil;

public class L2BossZone extends L2ZoneType
{
	private int _timeInvade;
	private int _maxLevel = 0;
	private boolean _enabled = true;
	private boolean _IsFlyingEnable = true;
	private boolean _pvp = false;
	private boolean _AllowCursedWeapons = true;
	private boolean _access = false;
	private boolean _teleportOut = true;
	
	private boolean _NoSummonZone = true;
	
	private final Map<Integer, Long> _playerAllowedReEntryTimes;
	private List<Integer> _playersAllowed;
	
	private int _bossId;
	
	protected volatile List<Integer> _skills = new ArrayList<>();
	
	public L2BossZone(int id)
	{
		super(id);
		
		_playerAllowedReEntryTimes = new ConcurrentHashMap<>();
		_playersAllowed = new CopyOnWriteArrayList<>();
		
		GrandBossManager.getInstance().addZone(this);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		switch (name)
		{
			case "bossId":
				_bossId = Integer.parseInt(value);
				break;
			case "InvadeTime":
				_timeInvade = Integer.parseInt(value);
				break;
			case "maxLevel":
				_maxLevel = Integer.parseInt(value);
				break;
			case "EnabledByDefault":
				_enabled = Boolean.parseBoolean(value);
				break;
			case "flying":
				_IsFlyingEnable = Boolean.parseBoolean(value);
				break;
			case "pvp":
				_pvp = Boolean.parseBoolean(value);
				break;
			case "AllowCursedWeapons":
				_AllowCursedWeapons = Boolean.parseBoolean(value);
				break;
			case "access":
				_access = Boolean.parseBoolean(value);
				break;
			case "teleportOut":
				_teleportOut = Boolean.parseBoolean(value);
				break;
			case "removeSkills":
				
				String[] propertySplit = value.split(";");
				
				for (String skill : propertySplit)
				{
					try
					{
						_skills.add(Integer.parseInt(skill));
						
					}
					catch (final NumberFormatException nfe)
					{
						if (!skill.isEmpty())
						{
							LOG.warn(StringUtil.concat(getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"", propertySplit[0], "\""));
						}
					}
				}
				break;
			case "noSummonZone":
				_NoSummonZone = Boolean.parseBoolean(value);
				break;
			default:
				super.setParameter(name, value);
				break;
		}
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (_enabled)
		{
			if (character != null)
			{
				character.setInsideZone(ZoneId.ZONE_BOSS, true);
			}
			
			if (character instanceof L2PcInstance)
			{
				if (_NoSummonZone)
				{
					character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, true);
				}
				
				final L2PcInstance player = (L2PcInstance) character;
				
				if (Config.BOT_PROTECTOR)
				{
					player.stopBotChecker();
				}
				
				// Asgota custom
				if (_skills != null)
				{
					if (player.getLevel() >= 70)
					{
						for (Integer skillId : _skills)
						{
							player.removeEffect(skillId);
						}
					}
				}
				
				if (player.isGM())
				{
					player.sendMessage("You entered to " + getName());
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
					{
						return;
					}
				}
				
				if (player.isFlying() && !_IsFlyingEnable)
				{
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				
				if (_pvp)
				{
					player.stopPvPFlag();
					player.updatePvPFlag(1);
				}
				
				if (_maxLevel > 0)
				{
					if (player.getLevel() > _maxLevel)
					{
						player.sendMessage(player.getName() + ", your level was too high (Max level: " + _maxLevel + ")! Teleporting to town...");
						player.sendPacket(new ExShowScreenMessage("" + player.getName() + ", your level was too high (Max level: " + _maxLevel + ")! Teleporting to town...", 3000, 0x02, false));
						player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
				}
				
				if (!_AllowCursedWeapons && player.isCursedWeaponEquiped())
				{
					player.sendMessage(player.getName() + ", cursed weapon is not allowed here anymore.");
					player.sendPacket(new ExShowScreenMessage("" + player.getName() + ", cursed weapon is not allowed here anymore.", 3000, 0x02, false));
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				
				if (!player.isEnteringToWorld())
				{
					if (_access)
					{
						allowPlayerEntry(player, 1);
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
				
				if (_teleportOut)
				{
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				
			}
			else if (character instanceof L2Summon)
			{
				final L2PcInstance player = ((L2Summon) character).getOwner();
				if (player != null && (_maxLevel > 0) && player.getLevel() > _maxLevel)
				{
					((L2Summon) character).unSummon(player);
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
				character.setInsideZone(ZoneId.ZONE_BOSS, false);
			}
			
			if (character instanceof L2PcInstance)
			{
				if (_NoSummonZone)
				{
					character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, false);
				}
				
				L2PcInstance player = (L2PcInstance) character;
				
				if (Config.BOT_PROTECTOR)
				{
					player.startBotChecker();
				}
				
				if (player.isGM())
				{
					player.sendMessage("You left " + getName());
					return;
				}
				
				if (_pvp)
				{
					player.stopPvPFlag();
					player.updatePvPStatus();
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
					{
						_playersAllowed.remove(_playersAllowed.indexOf(player.getObjectId()));
					}
					
					_playerAllowedReEntryTimes.remove(player.getObjectId());
				}
			}
		}
	}
	
	public void setZoneEnabled(boolean flag)
	{
		if (_enabled != flag)
		{
			oustAllPlayers();
		}
		
		_enabled = flag;
	}
	
	public int getTimeInvade()
	{
		return _timeInvade;
	}
	
	public void setAllowedPlayers(List<Integer> list)
	{
		if (list != null)
		{
			_playersAllowed = list;
		}
	}
	
	public List<Integer> getAllowedPlayers()
	{
		return _playersAllowed;
	}
	
	public boolean isPlayerAllowed(L2PcInstance player)
	{
		if (player.isGM())
		{
			return true;
		}
		else if (_playersAllowed.contains(player.getObjectId()))
		{
			return true;
		}
		player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
		return false;
	}
	
	public void movePlayersTo(int x, int y, int z)
	{
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (final L2Character character : _characterList.values())
		{
			if (character instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance) character;
				if (player.isOnline() == 1)
				{
					player.teleToLocation(x, y, z);
				}
			}
		}
	}
	
	public void oustAllPlayers()
	{
		if (_characterList == null)
		{
			return;
		}
		
		if (_characterList.isEmpty())
		{
			return;
		}
		
		for (L2Character character : _characterList.values())
		{
			if (character == null)
			{
				continue;
			}
			
			if (character instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) character;
				if (player.isOnline() == 1)
				{
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
			}
		}
		
		if (_playerAllowedReEntryTimes != null)
		{
			_playerAllowedReEntryTimes.clear();
		}
		
		if (_playersAllowed != null)
		{
			_playersAllowed.clear();
		}
	}
	
	public void allowPlayerEntry(L2PcInstance player, int durationInSec)
	{
		if (!player.isGM())
		{
			if (!_playersAllowed.contains(player.getObjectId()))
			{
				_playersAllowed.add(player.getObjectId());
			}
			
			_playerAllowedReEntryTimes.put(player.getObjectId(), System.currentTimeMillis() + durationInSec * 1000);
		}
	}
	
	public void removePlayer(L2PcInstance player)
	{
		if (!player.isGM())
		{
			_playersAllowed.remove(Integer.valueOf(player.getObjectId()));
			_playerAllowedReEntryTimes.remove(player.getObjectId());
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
	public void updateKnownList(L2NpcInstance npc)
	{
		if (_characterList == null || _characterList.isEmpty())
		{
			return;
		}
		
		final Map<Integer, L2PcInstance> npcKnownPlayers = npc.getKnownList().getKnownPlayers();
		for (final L2Character character : _characterList.values())
		{
			if (character == null)
			{
				continue;
			}
			
			if (character instanceof L2PcInstance)
			{
				final L2PcInstance player = (L2PcInstance) character;
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