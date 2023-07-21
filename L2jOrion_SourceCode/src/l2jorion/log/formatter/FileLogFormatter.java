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
import java.util.StringJoiner;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import l2jorion.Config;

public class FileLogFormatter extends Formatter
{
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss,SSS");
	
	@Override
	public String format(LogRecord record)
	{
		final StringJoiner sj = new StringJoiner("\t", "", Config.EOL);
		sj.add(dateFormat.format(new Date(record.getMillis())));
		sj.add(record.getLevel().getName());
		sj.add(String.valueOf(record.getThreadID()));
		sj.add(record.getLoggerName());
		sj.add(record.getMessage());
		return sj.toString();
	}
}
