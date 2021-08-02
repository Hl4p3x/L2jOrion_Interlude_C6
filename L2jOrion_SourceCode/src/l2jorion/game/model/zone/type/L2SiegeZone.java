/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.model.zone.type;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.managers.CHSiegeManager;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Siegable;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.model.zone.AbstractZoneSettings;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class L2SiegeZone extends L2ZoneType
{
	private int _castleId;
	private int _fortId;
	private int _clanHallId;
	private Castle _castle;
	private final int[] _spawnLoc;
	
	public L2SiegeZone(final int id)
	{
		super(id);
		
		_spawnLoc = new int[3];
		
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
	}
	
	public final class Settings extends AbstractZoneSettings
	{
		private int _siegableId = -1;
		private Siegable _siege = null;
		private boolean _isActiveSiege = false;
		
		protected Settings()
		{
		}
		
		public int getSiegeableId()
		{
			return _siegableId;
		}
		
		protected void setSiegeableId(int id)
		{
			_siegableId = id;
		}
		
		public Siegable getSiege()
		{
			return _siege;
		}
		
		public void setSiege(Siegable s)
		{
			_siege = s;
		}
		
		public boolean isActiveSiege()
		{
			return _isActiveSiege;
		}
		
		public void setActiveSiege(boolean val)
		{
			_isActiveSiege = val;
		}
		
		@Override
		public void clear()
		{
			_siegableId = -1;
			_siege = null;
			_isActiveSiege = false;
		}
	}
	
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		switch (name)
		{
			case "castleId":
				_castleId = Integer.parseInt(value);
				
				if (getSettings().getSiegeableId() != -1)
				{
					throw new IllegalArgumentException("Siege object already defined!");
				}
				
				getSettings().setSiegeableId(_castleId);
				break;
			case "fortId":
				_fortId = Integer.parseInt(value);
				if (getSettings().getSiegeableId() != -1)
				{
					throw new IllegalArgumentException("Siege object already defined!");
				}
				
				getSettings().setSiegeableId(_fortId);
				
				break;
			case "clanHallId":
				_clanHallId = Integer.parseInt(value);
				if (getSettings().getSiegeableId() != -1)
				{
					throw new IllegalArgumentException("Siege object already defined!");
				}
				
				getSettings().setSiegeableId(_clanHallId);
				
				SiegableHall hall = CHSiegeManager.getInstance().getConquerableHalls().get(getSettings().getSiegeableId());
				if (hall != null)
				{
					hall.setSiegeZone(this);
				}
				break;
			case "spawnX":
				_spawnLoc[0] = Integer.parseInt(value);
				break;
			case "spawnY":
				_spawnLoc[1] = Integer.parseInt(value);
				break;
			case "spawnZ":
				_spawnLoc[2] = Integer.parseInt(value);
				break;
			default:
				super.setParameter(name, value);
				break;
		}
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (getSettings().isActiveSiege())
		{
			character.setInsideZone(ZoneId.ZONE_PVP, true);
			character.setInsideZone(ZoneId.ZONE_SIEGE, true);
			character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, true);
			
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
			}
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (getSettings().isActiveSiege())
		{
			character.setInsideZone(ZoneId.ZONE_PVP, false);
			character.setInsideZone(ZoneId.ZONE_SIEGE, false);
			character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, false);
			
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
				
				// Set pvp flag
				if (((L2PcInstance) character).getPvpFlag() == 0)
				{
					((L2PcInstance) character).startPvPFlag();
				}
			}
			
			if (character instanceof L2SiegeSummonInstance)
			{
				((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
			}
			
			if (character instanceof L2PcInstance)
			{
				L2PcInstance activeChar = character.getActingPlayer();
				activeChar.setIsInSiege(false);
			}
		}
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(final L2Character character)
	{
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (getSettings().isActiveSiege())
		{
			for (L2Character character : getCharactersInside())
			{
				if (character != null)
				{
					onEnter(character);
				}
			}
		}
		else
		{
			for (L2Character character : getCharactersInside())
			{
				if (character == null)
				{
					continue;
				}
				try
				{
					character.setInsideZone(ZoneId.ZONE_PVP, false);
					character.setInsideZone(ZoneId.ZONE_SIEGE, false);
					character.setInsideZone(ZoneId.ZONE_NOSUMMONFRIEND, false);
					
					if (character instanceof L2PcInstance)
					{
						((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
					}
					
					if (character instanceof L2SiegeSummonInstance)
					{
						((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
					}
				}
				catch (final NullPointerException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void banishForeigners(final int owningClanId)
	{
		for (final L2Character temp : _characterList.values())
		{
			if (!(temp instanceof L2PcInstance))
			{
				continue;
			}
			
			if (((L2PcInstance) temp).getClanId() == owningClanId)
			{
				continue;
			}
			
			((L2PcInstance) temp).teleToLocation(MapRegionTable.TeleportWhereType.Town);
		}
	}
	
	public void announceToPlayers(final String message)
	{
		for (final L2Character temp : _characterList.values())
		{
			if (temp instanceof L2PcInstance)
			{
				((L2PcInstance) temp).sendMessage(message);
			}
		}
	}
	
	public FastList<L2PcInstance> getAllPlayers()
	{
		final FastList<L2PcInstance> players = new FastList<>();
		
		for (final L2Character temp : _characterList.values())
		{
			if (temp instanceof L2PcInstance)
			{
				players.add((L2PcInstance) temp);
			}
		}
		
		return players;
	}
	
	public int[] getSpawn()
	{
		return _spawnLoc;
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	public int getSiegeObjectId()
	{
		return getSettings().getSiegeableId();
	}
	
	public boolean isSiegeActive()
	{
		if (_castle != null)
		{
			return _castle.isSiegeInProgress();
		}
		
		return false;
	}
	
	public void setSiegeInstance(Siegable siege)
	{
		getSettings().setSiege(siege);
	}
	
	public boolean isActive()
	{
		return getSettings().isActiveSiege();
	}
	
	public void setIsActive(boolean val)
	{
		getSettings().setActiveSiege(val);
	}
}
