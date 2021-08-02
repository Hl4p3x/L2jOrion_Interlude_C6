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
package l2jorion.game.model.actor.stat;

import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class PlayableStat extends CharStat
{
	protected static final Logger LOG = LoggerFactory.getLogger(PlayableStat.class);
	
	public PlayableStat(final L2PlayableInstance activeChar)
	{
		super(activeChar);
	}
	
	public boolean addExp(long value)
	{
		if ((getExp() + value) < 0 || (value > 0 && getExp() == getExpForLevel(ExperienceData.getInstance().getMaxLevel()) - 1))
		{
			return true;
		}
		
		if (getExp() + value >= getExpForLevel(ExperienceData.getInstance().getMaxLevel()))
		{
			value = getExpForLevel(ExperienceData.getInstance().getMaxLevel()) - 1 - getExp();
		}
		
		setExp(getExp() + value);
		
		byte level = 1;
		
		for (level = 1; level <= ExperienceData.getInstance().getMaxLevel(); level++)
		{
			if (getExp() >= getExpForLevel(level))
			{
				continue;
			}
			--level;
			break;
		}
		
		if (level != getLevel())
		{
			addLevel((byte) (level - getLevel()));
		}
		
		return true;
	}
	
	public boolean removeExp(long value)
	{
		if (getExp() - value < 0)
		{
			value = getExp() - 1;
		}
		
		setExp(getExp() - value);
		
		byte level = 0;
		
		for (level = 1; level <= ExperienceData.getInstance().getMaxLevel(); level++)
		{
			if (getExp() >= getExpForLevel(level))
			{
				continue;
			}
			
			level--;
			break;
		}
		
		if (level != getLevel())
		{
			addLevel((byte) (level - getLevel()));
		}
		
		return true;
	}
	
	public boolean addExpAndSp(final long addToExp, final int addToSp)
	{
		boolean expAdded = false;
		boolean spAdded = false;
		
		if (addToExp >= 0)
		{
			expAdded = addExp(addToExp);
		}
		
		if (addToSp >= 0)
		{
			spAdded = addSp(addToSp);
		}
		
		return expAdded || spAdded;
	}
	
	public boolean removeExpAndSp(final long removeExp, final int removeSp)
	{
		boolean expRemoved = false;
		boolean spRemoved = false;
		
		if (removeExp > 0)
		{
			expRemoved = removeExp(removeExp);
		}
		
		if (removeSp > 0)
		{
			spRemoved = removeSp(removeSp);
		}
		
		return expRemoved || spRemoved;
	}
	
	public boolean addLevel(byte value)
	{
		if (getLevel() + value > ExperienceData.getInstance().getMaxLevel() - 1)
		{
			if (getLevel() < ExperienceData.getInstance().getMaxLevel() - 1)
			{
				value = (byte) (ExperienceData.getInstance().getMaxLevel() - 1 - getLevel());
			}
			else
			{
				return false;
			}
		}
		
		final boolean levelIncreased = getLevel() + value > getLevel();
		value += getLevel();
		setLevel(value);
		
		// Sync up exp with current level
		if (getExp() >= getExpForLevel(getLevel() + 1) || getExpForLevel(getLevel()) > getExp())
		{
			setExp(getExpForLevel(getLevel()));
		}
		
		if (!levelIncreased && getActiveChar() instanceof L2PcInstance && !((L2PcInstance) (getActiveChar())).isGM())
		{
			((L2PcInstance) (getActiveChar())).checkAllowedSkills();
		}
		
		if (!levelIncreased)
		{
			return false;
		}
		
		getActiveChar().getStatus().setCurrentHp(getActiveChar().getStat().getMaxHp());
		getActiveChar().getStatus().setCurrentMp(getActiveChar().getStat().getMaxMp());
		
		return true;
	}
	
	public boolean addSp(int value)
	{
		if (value < 0)
		{
			LOG.info("wrong usage");
			return false;
		}
		
		final int currentSp = getSp();
		
		if (currentSp == Integer.MAX_VALUE)
		{
			return false;
		}
		
		if (currentSp > Integer.MAX_VALUE - value)
		{
			value = Integer.MAX_VALUE - currentSp;
		}
		
		setSp(currentSp + value);
		
		return true;
	}
	
	public boolean removeSp(int value)
	{
		final int currentSp = getSp();
		
		if (currentSp < value)
		{
			value = currentSp;
		}
		
		setSp(getSp() - value);
		
		return true;
	}
	
	public long getExpForLevel(final int level)
	{
		return level;
	}
	
	@Override
	public L2PlayableInstance getActiveChar()
	{
		return (L2PlayableInstance) super.getActiveChar();
	}
}
