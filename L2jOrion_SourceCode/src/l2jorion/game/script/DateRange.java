/*
 * L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.script;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class DateRange
{
	private static Logger LOG = LoggerFactory.getLogger(DateRange.class);
	
	private final Date _startDate, _endDate;
	
	public DateRange(final Date from, final Date to)
	{
		_startDate = from;
		_endDate = to;
	}
	
	public static DateRange parse(final String dateRange, final DateFormat format)
	{
		final String[] date = dateRange.split("-");
		if (date.length == 2)
		{
			try
			{
				final Date start = format.parse(date[0]);
				final Date end = format.parse(date[1]);
				
				return new DateRange(start, end);
			}
			catch (final ParseException e)
			{
				LOG.error("Invalid Date Format.");
				e.printStackTrace();
			}
		}
		return new DateRange(null, null);
	}
	
	public boolean isValid()
	{
		return _startDate == null || _endDate == null;
	}
	
	public boolean isWithinRange(final Date date)
	{
		return date.after(_startDate) && date.before(_endDate);
	}
	
	public Date getEndDate()
	{
		return _endDate;
	}
	
	public Date getStartDate()
	{
		return _startDate;
	}
}