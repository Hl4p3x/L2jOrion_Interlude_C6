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
package l2jorion.game.model;

import java.util.concurrent.Future;

import l2jorion.Config;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2Potion extends L2Object
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2Character.class);
	
	private Future<?> _potionhpRegTask;
	private Future<?> _potionmpRegTask;
	protected int _milliseconds;
	protected double _effect;
	protected int _duration;
	private int _potion;
	protected Object _mpLock = new Object();
	protected Object _hpLock = new Object();
	
	class PotionHpHealing implements Runnable
	{
		L2Character _instance;
		
		public PotionHpHealing(final L2Character instance)
		{
			_instance = instance;
		}
		
		@Override
		public void run()
		{
			try
			{
				synchronized (_hpLock)
				{
					double nowHp = _instance.getCurrentHp();
					
					if (_duration == 0)
					{
						stopPotionHpRegeneration();
					}
					if (_duration != 0)
					{
						nowHp += _effect;
						_instance.setCurrentHp(nowHp);
						_duration = _duration - _milliseconds / 1000;
						setCurrentHpPotion2();
					}
				}
			}
			catch (final Exception e)
			{
				LOG.warn("Error in hp potion task:" + e);
			}
		}
	}
	
	public L2Potion(final int objectId)
	{
		super(objectId);
	}
	
	public void stopPotionHpRegeneration()
	{
		if (_potionhpRegTask != null)
		{
			_potionhpRegTask.cancel(false);
		}
		
		_potionhpRegTask = null;
		
		if (Config.DEBUG)
		{
			LOG.debug("Potion HP regen stop");
		}
	}
	
	public void setCurrentHpPotion2()
	{
		if (_duration == 0)
		{
			stopPotionHpRegeneration();
		}
		
	}
	
	public void setCurrentHpPotion1(final L2Character activeChar, final int item)
	{
		_potion = item;
		// _target = activeChar;
		
		switch (_potion)
		{
			case 1540:
				double nowHp = activeChar.getCurrentHp();
				
				nowHp += 435;
				
				if (nowHp >= activeChar.getMaxHp())
				{
					nowHp = activeChar.getMaxHp();
				}
				
				activeChar.setCurrentHp(nowHp);
				break;
			case 728:
				double nowMp = activeChar.getMaxMp();
				
				nowMp += 435;
				
				if (nowMp >= activeChar.getMaxMp())
				{
					nowMp = activeChar.getMaxMp();
				}
				
				activeChar.setCurrentMp(nowMp);
				break;
			case 726:
				_milliseconds = 500;
				_duration = 15;
				_effect = 1.5;
				startPotionMpRegeneration(activeChar);
				break;
		}
	}
	
	class PotionMpHealing implements Runnable
	{
		L2Character _instance;
		
		public PotionMpHealing(final L2Character instance)
		{
			_instance = instance;
		}
		
		@Override
		public void run()
		{
			try
			{
				synchronized (_mpLock)
				{
					double nowMp = _instance.getCurrentMp();
					
					if (_duration == 0)
					{
						stopPotionMpRegeneration();
					}
					
					if (_duration != 0)
					{
						nowMp += _effect;
						_instance.setCurrentMp(nowMp);
						_duration = _duration - _milliseconds / 1000;
						setCurrentMpPotion2();
						
					}
				}
			}
			catch (final Exception e)
			{
				LOG.warn("error in mp potion task:" + e);
			}
		}
	}
	
	private void startPotionMpRegeneration(final L2Character activeChar)
	{
		_potionmpRegTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new PotionMpHealing(activeChar), 1000, _milliseconds);
		
		if (Config.DEBUG)
		{
			LOG.debug("Potion MP regen Started");
		}
	}
	
	public void stopPotionMpRegeneration()
	{
		if (_potionmpRegTask != null)
		{
			_potionmpRegTask.cancel(false);
		}
		
		_potionmpRegTask = null;
		
		if (Config.DEBUG)
		{
			LOG.debug("Potion MP regen stop");
		}
	}
	
	public void setCurrentMpPotion2()
	{
		if (_duration == 0)
		{
			stopPotionMpRegeneration();
		}
		
	}
	
	public void setCurrentMpPotion1(final L2Character activeChar, final int item)
	{
		_potion = item;
		// _target = activeChar;
		//
		// switch(_potion)
		// {
		// null
		// }
	}
	
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return false;
	}
}
