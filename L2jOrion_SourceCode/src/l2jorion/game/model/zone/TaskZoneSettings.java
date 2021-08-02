/*
 * Copyright (C) 2004-2016 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.zone;

import java.util.concurrent.Future;

public class TaskZoneSettings extends AbstractZoneSettings
{
	private Future<?> _task;
	
	public Future<?> getTask()
	{
		return _task;
	}
	
	public void setTask(Future<?> task)
	{
		_task = task;
	}
	
	@Override
	public void clear()
	{
		if (_task != null)
		{
			_task.cancel(true);
			_task = null;
		}
	}
}
