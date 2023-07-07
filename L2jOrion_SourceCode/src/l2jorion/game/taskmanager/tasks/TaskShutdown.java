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

import l2jorion.game.Shutdown;
import l2jorion.game.taskmanager.Task;
import l2jorion.game.taskmanager.TaskManager.ExecutedTask;
import l2jorion.log.Log;

public class TaskShutdown extends Task
{
	public static final String NAME = "shutdown";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(final ExecutedTask task)
	{
		final String text = "Server Shutdown launched..";
		Log.add(text, "Global_task");
		
		final Shutdown handler = new Shutdown(Integer.valueOf(task.getParams()[2]), false);
		handler.start();
	}
}