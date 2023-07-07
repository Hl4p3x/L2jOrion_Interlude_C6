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
package l2jorion.game.script.faenor;

import java.util.Date;

import javax.script.ScriptContext;

import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.game.script.DateRange;
import l2jorion.game.script.IntList;
import l2jorion.game.script.Parser;
import l2jorion.game.script.ParserFactory;
import l2jorion.game.script.ScriptEngine;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class FaenorEventParser extends FaenorParser
{
	static Logger LOG = LoggerFactory.getLogger(FaenorEventParser.class);
	
	private DateRange _eventDates = null;
	
	@Override
	public void parseScript(Node eventNode, ScriptContext context)
	{
		final String ID = attribute(eventNode, "ID");
		
		_eventDates = DateRange.parse(attribute(eventNode, "Active"), DATE_FORMAT);
		
		final Date currentDate = new Date();
		if (_eventDates.getEndDate().before(currentDate))
		{
			LOG.info("Event ID: (" + ID + ") has passed... Ignored");
			return;
		}
		
		if (_eventDates.getStartDate().after(currentDate))
		{
			LOG.info("Event ID: (" + ID + ") is not active yet... Ignored");
			
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					parseEventDropAndMessage(eventNode);
				}
			}, _eventDates.getStartDate().getTime() - currentDate.getTime());
			
			return;
		}
		
		parseEventDropAndMessage(eventNode);
	}
	
	protected void parseEventDropAndMessage(final Node eventNode)
	{
		
		for (Node node = eventNode.getFirstChild(); node != null; node = node.getNextSibling())
		{
			
			if (isNodeName(node, "DropList"))
			{
				parseEventDropList(node);
			}
			else if (isNodeName(node, "Message"))
			{
				parseEventMessage(node);
			}
		}
	}
	
	private void parseEventMessage(final Node sysMsg)
	{
		try
		{
			final String type = attribute(sysMsg, "Type");
			final String[] message = attribute(sysMsg, "Msg").split("\n");
			
			if (type.equalsIgnoreCase("OnJoin"))
			{
				_bridge.onPlayerLogin(message, _eventDates);
			}
		}
		catch (final Exception e)
		{
			LOG.warn("Error in event parser.");
			e.printStackTrace();
		}
	}
	
	private void parseEventDropList(final Node dropList)
	{
		for (Node node = dropList.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (isNodeName(node, "AllDrop"))
			{
				parseEventDrop(node);
			}
		}
	}
	
	private void parseEventDrop(final Node drop)
	{
		try
		{
			final int[] items = IntList.parse(attribute(drop, "Items"));
			final int[] count = IntList.parse(attribute(drop, "Count"));
			final double chance = getPercent(attribute(drop, "Chance"));
			
			_bridge.addEventDrop(items, count, chance, _eventDates);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("ERROR(parseEventDrop):" + e.getMessage());
		}
	}
	
	static class FaenorEventParserFactory extends ParserFactory
	{
		@Override
		public Parser create()
		{
			return new FaenorEventParser();
		}
	}
	
	static
	{
		ScriptEngine.parserFactories.put(getParserName("Event"), new FaenorEventParserFactory());
	}
}
