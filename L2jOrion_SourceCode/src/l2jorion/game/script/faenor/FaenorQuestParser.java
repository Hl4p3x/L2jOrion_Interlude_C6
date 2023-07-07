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

import javax.script.ScriptContext;

import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.game.script.Parser;
import l2jorion.game.script.ParserFactory;
import l2jorion.game.script.ScriptEngine;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class FaenorQuestParser extends FaenorParser
{
	protected static final Logger LOG = LoggerFactory.getLogger(FaenorQuestParser.class);
	
	@Override
	public void parseScript(final Node questNode, final ScriptContext context)
	{
		if (DEBUG)
		{
			LOG.info("Parsing Quest.");
		}
		
		final String questID = attribute(questNode, "ID");
		
		for (Node node = questNode.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (isNodeName(node, "DROPLIST"))
			{
				parseQuestDropList(node.cloneNode(true), questID);
			}
			else if (isNodeName(node, "DIALOG WINDOWS"))
			{
				// parseDialogWindows(node.cloneNode(true));
			}
			else if (isNodeName(node, "INITIATOR"))
			{
				// parseInitiator(node.cloneNode(true));
			}
			else if (isNodeName(node, "STATE"))
			{
				// parseState(node.cloneNode(true));
			}
		}
	}
	
	private void parseQuestDropList(final Node dropList, final String questID) throws NullPointerException
	{
		if (DEBUG)
		{
			LOG.info("Parsing Droplist.");
		}
		
		for (Node node = dropList.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (isNodeName(node, "DROP"))
			{
				parseQuestDrop(node.cloneNode(true), questID);
			}
		}
	}
	
	private void parseQuestDrop(final Node drop, final String questID)// throws NullPointerException
	{
		if (DEBUG)
		{
			LOG.info("Parsing Drop.");
		}
		
		int npcID;
		int itemID;
		int min;
		int max;
		int chance;
		String[] states;
		try
		{
			npcID = getInt(attribute(drop, "NpcID"));
			itemID = getInt(attribute(drop, "ItemID"));
			min = getInt(attribute(drop, "Min"));
			max = getInt(attribute(drop, "Max"));
			chance = getInt(attribute(drop, "Chance"));
			states = attribute(drop, "States").split(",");
		}
		catch (final NullPointerException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			throw new NullPointerException("Incorrect Drop Data");
		}
		
		if (DEBUG)
		{
			LOG.info("Adding Drop to NpcID: " + npcID);
		}
		
		_bridge.addQuestDrop(npcID, itemID, min, max, chance, questID, states);
	}
	
	static class FaenorQuestParserFactory extends ParserFactory
	{
		@Override
		public Parser create()
		{
			return new FaenorQuestParser();
		}
	}
	
	static
	{
		ScriptEngine.parserFactories.put(getParserName("Quest"), new FaenorQuestParserFactory());
	}
}
