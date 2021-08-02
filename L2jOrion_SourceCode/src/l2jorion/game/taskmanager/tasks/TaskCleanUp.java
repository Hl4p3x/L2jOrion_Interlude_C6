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

import l2jorion.game.taskmanager.Task;
import l2jorion.game.taskmanager.TaskManager;
import l2jorion.game.taskmanager.TaskManager.ExecutedTask;
import l2jorion.game.taskmanager.TaskTypes;
import l2jorion.log.Log;

public final class TaskCleanUp extends Task
{
	public static final String NAME = "cleanup";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(final ExecutedTask task)
	{
		System.runFinalization();
		System.gc();
		final String text = "Java Memory Cleanup launched.";
		Log.add(text, "Global_Task");
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "86400000", "86400000", "");
	}
}