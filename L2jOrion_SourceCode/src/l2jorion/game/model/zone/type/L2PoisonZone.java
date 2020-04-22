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

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

public class L2PoisonZone extends L2ZoneType
{
	protected final Logger LOG = LoggerFactory.getLogger(L2PoisonZone.class);
	protected int _skillId;
	private int _chance;
	private int _initialDelay;
	protected int _skillLvl;
	private int _reuse;
	private boolean _enabled;
	private String _target;
	private Future<?> _task;
	
	public L2PoisonZone(final int id)
	{
		super(id);
		_skillId = 4070;
		_skillLvl = 1;
		_chance = 100;
		_initialDelay = 0;
		_reuse = 30000;
		_enabled = true;
		_target = "pc";
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		switch (name)
		{
			case "skillId":
				_skillId = Integer.parseInt(value);
				break;
			case "skillLvl":
				_skillLvl = Integer.parseInt(value);
				break;
			case "chance":
				_chance = Integer.parseInt(value);
				break;
			case "initialDelay":
				_initialDelay = Integer.parseInt(value);
				break;
			case "default_enabled":
				_enabled = Boolean.parseBoolean(value);
				break;
			case "target":
				_target = String.valueOf(value);
				break;
			case "reuse":
				_reuse = Integer.parseInt(value);
				break;
			default:
				super.setParameter(name, value);
				break;
		}
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if ((character instanceof L2PlayableInstance && _target.equalsIgnoreCase("pc") || character instanceof L2PcInstance && _target.equalsIgnoreCase("pc_only") || character instanceof L2MonsterInstance && _target.equalsIgnoreCase("npc")) && _task == null)
		{
			_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplySkill(/* this */), _initialDelay, _reuse);
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (_characterList.isEmpty() && _task != null)
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	public L2Skill getSkill()
	{
		return SkillTable.getInstance().getInfo(_skillId, _skillLvl);
	}
	
	public String getTargetType()
	{
		return _target;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public void setZoneEnabled(final boolean val)
	{
		_enabled = val;
	}
	
	/*
	 * protected Collection getCharacterList() { return _characterList.values(); }
	 */
	
	class ApplySkill implements Runnable
	{
		// private L2PoisonZone _poisonZone;
		
		// ApplySkill(/*L2PoisonZone zone*/)
		// {
		// _poisonZone = zone;
		// }
		
		@Override
		public void run()
		{
			if (isEnabled())
			{
				for (final L2Character temp : _characterList.values())
				{
					if (temp != null && !temp.isDead())
					{
						if ((temp instanceof L2PlayableInstance && getTargetType().equalsIgnoreCase("pc") || temp instanceof L2PcInstance && getTargetType().equalsIgnoreCase("pc_only") || temp instanceof L2MonsterInstance && getTargetType().equalsIgnoreCase("npc")) && Rnd.get(100) < getChance())
						{
							L2Skill skill = null;
							if ((skill = getSkill()) == null)
							{
								LOG.warn("ATTENTION: error on zone with id " + getId());
								LOG.warn("Skill " + _skillId + "," + _skillLvl + " not present between skills");
							}
							else
								skill.getEffects(temp, temp, false, false, false);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onDieInside(final L2Character l2character)
	{
	}
	
	@Override
	public void onReviveInside(final L2Character l2character)
	{
	}
}
