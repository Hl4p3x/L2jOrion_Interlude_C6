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

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.thread.ThreadPoolManager;

public class L2DynamicZone extends L2ZoneType
{
	private final L2WorldRegion _region;
	private final L2Character _owner;
	private Future<?> _task;
	private final L2Skill _skill;
	
	protected void setTask(final Future<?> task)
	{
		_task = task;
	}
	
	public L2DynamicZone(final L2WorldRegion region, final L2Character owner, final L2Skill skill)
	{
		super(-1);
		_region = region;
		_owner = owner;
		_skill = skill;
		
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				remove();
			}
		};
		setTask(ThreadPoolManager.getInstance().scheduleGeneral(r, skill.getBuffDuration()));
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		try
		{
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendMessage("You have entered a temporary zone!");
			}
			
			_skill.getEffects(_owner, character, false, false, false);
		}
		catch (final NullPointerException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			((L2PcInstance) character).sendMessage("You have left a temporary zone!");
		}
		
		if (character == _owner)
		{
			remove();
			return;
		}
		character.stopSkillEffects(_skill.getId());
	}
	
	protected void remove()
	{
		if (_task == null)
			return;
		
		_task.cancel(false);
		_task = null;
		
		_region.removeZone(this);
		
		for (final L2Character member : _characterList.values())
		{
			try
			{
				member.stopSkillEffects(_skill.getId());
			}
			catch (final NullPointerException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
			}
		}
		_owner.stopSkillEffects(_skill.getId());
		
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{
		if (character == _owner)
		{
			remove();
		}
		else
		{
			character.stopSkillEffects(_skill.getId());
		}
	}
	
	@Override
	public void onReviveInside(final L2Character character)
	{
		_skill.getEffects(_owner, character, false, false, false);
	}
	
}
