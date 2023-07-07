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
package l2jorion.game.taskmanager.tasks;

import l2jorion.Config;
import l2jorion.game.model.olympiad.Olympiad;
import l2jorion.game.taskmanager.Task;
import l2jorion.game.taskmanager.TaskManager;
import l2jorion.game.taskmanager.TaskManager.ExecutedTask;
import l2jorion.game.taskmanager.TaskTypes;
import l2jorion.log.Log;

public class TaskOlympiadSave extends Task
{
	public static final String NAME = "OlympiadSave";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(final ExecutedTask task)
	{
		try
		{
			if (Olympiad.getInstance().inCompPeriod())
			{
				Olympiad.getInstance().saveOlympiadStatus();
				final String text = "Olympiad System save launched";
				Log.add(text, "Global_task");
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Olympiad System: Failed to save Olympiad configuration: " + e);
		}
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "900000", "1800000", "");
	}
}