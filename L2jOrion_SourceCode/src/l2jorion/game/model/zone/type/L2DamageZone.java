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

import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.zone.AbstractZoneSettings;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.TaskZoneSettings;
import l2jorion.game.thread.ThreadPoolManager;

public class L2DamageZone extends L2ZoneType
{
	private int _damageHPPerSec;
	private int _damageMPPerSec;
	
	private int _castleId;
	private Castle _castle;
	
	private int _startTask;
	private int _reuseTask;
	
	public L2DamageZone(int id)
	{
		super(id);
		
		// Setup default damage
		_damageHPPerSec = 200;
		_damageMPPerSec = 0;
		
		// Setup default start / reuse time
		_startTask = 10;
		_reuseTask = 5000;
		
		// no castle by default
		_castleId = 0;
		_castle = null;
		
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new TaskZoneSettings();
		}
		
		setSettings(settings);
	}
	
	@Override
	public TaskZoneSettings getSettings()
	{
		return (TaskZoneSettings) super.getSettings();
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dmgHPSec"))
		{
			_damageHPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("dmgMPSec"))
		{
			_damageMPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("initialDelay"))
		{
			_startTask = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("reuse"))
		{
			_reuseTask = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (character instanceof L2PlayableInstance)
		{
			if ((getSettings().getTask() == null) && ((_damageHPPerSec != 0) || (_damageMPPerSec != 0)))
			{
				L2PcInstance player = character.getActingPlayer();
				if (getCastle() != null) // Castle zone
				{
					if (!(getCastle().getSiege().getIsInProgress() && (player != null) && (player.getSiegeState() != 2))) // Siege and no defender
					{
						return;
					}
				}
				
				synchronized (this)
				{
					if (getSettings().getTask() == null)
					{
						getSettings().setTask(ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplyDamage(this), _startTask, _reuseTask));
					}
				}
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (_characterList.isEmpty() && (getSettings().getTask() != null))
		{
			getSettings().clear();
		}
	}
	
	protected int getHPDamagePerSecond()
	{
		return _damageHPPerSec;
	}
	
	protected int getMPDamagePerSecond()
	{
		return _damageMPPerSec;
	}
	
	protected Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}
		
		return _castle;
	}
	
	private final class ApplyDamage implements Runnable
	{
		private final L2DamageZone _dmgZone;
		private final Castle _castle;
		
		protected ApplyDamage(L2DamageZone zone)
		{
			_dmgZone = zone;
			_castle = zone.getCastle();
		}
		
		@Override
		public void run()
		{
			if (!isEnabled())
			{
				return;
			}
			
			boolean siege = false;
			
			if (_castle != null)
			{
				siege = _castle.getSiege().getIsInProgress();
				if (!siege)
				{
					_dmgZone.getSettings().clear();
					return;
				}
			}
			
			for (L2Character temp : _dmgZone.getCharactersInside())
			{
				if ((temp != null) && !temp.isDead())
				{
					if (siege)
					{
						final L2PcInstance player = temp.getActingPlayer();
						if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
						{
							continue;
						}
					}
					
					if (getHPDamagePerSecond() != 0)
					{
						temp.reduceCurrentHp(_dmgZone.getHPDamagePerSecond(), null);
					}
					if (getMPDamagePerSecond() != 0)
					{
						temp.reduceCurrentMp(_dmgZone.getMPDamagePerSecond());
					}
				}
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
}