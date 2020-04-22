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
package l2jorion.log.formatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.util.StringUtil;

public class ItemLogFormatter extends Formatter
{
	private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM H:mm:ss");
	
	@Override
	public String format(LogRecord record)
	{
		final Object[] params = record.getParameters();
		final StringBuilder output = StringUtil.startAppend(30 + record.getMessage().length() + (params.length * 50), "[", dateFmt.format(new Date(record.getMillis())), "] ", record.getMessage());
		
		for (Object p : record.getParameters())
		{
			if (p == null)
			{
				continue;
			}
			output.append(", ");
			if (p instanceof L2ItemInstance)
			{
				L2ItemInstance item = (L2ItemInstance) p;
				StringUtil.append(output, "item ", String.valueOf(item.getObjectId()), ":");
				if (item.getEnchantLevel() > 0)
				{
					StringUtil.append(output, "+", String.valueOf(item.getEnchantLevel()), " ");
				}
				
				StringUtil.append(output, item.getItem().getName(), "(", String.valueOf(item.getCount()), ")");
			}
			// else if (p instanceof L2PcInstance)
			// output.append(((L2PcInstance)p).getName());
			else
			{
				output.append(p.toString()/* + ":" + ((L2Object)p).getObjectId() */);
			}
		}
		output.append(Config.EOL);
		
		return output.toString();
	}
	
}
