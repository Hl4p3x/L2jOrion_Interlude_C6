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
package l2jorion.game.taskmanager;

import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.taskmanager.TaskManager.ExecutedTask;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public abstract class Task
{
	public static Logger LOG = LoggerFactory.getLogger(Task.class);
	
	public void initializate()
	{
		if (Config.DEBUG)
		{
			LOG.info("Task" + getName() + " inializate");
		}
	}
	
	public ScheduledFuture<?> launchSpecial(final ExecutedTask instance)
	{
		return null;
	}
	
	public abstract String getName();
	
	public abstract void onTimeElapsed(ExecutedTask task);
	
	public void onDestroy()
	{
	}
}