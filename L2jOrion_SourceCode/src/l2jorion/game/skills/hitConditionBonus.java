/*
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
package l2jorion.game.skills;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.model.L2Character;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class hitConditionBonus
{
	protected static final Logger LOG = LoggerFactory.getLogger(hitConditionBonus.class);
	
	private static int frontBonus = 0;
	private static int sideBonus = 0;
	private static int backBonus = 0;
	private static int highBonus = 0;
	private static int lowBonus = 0;
	private static int darkBonus = 0;
	
	// private static int rainBonus = 0;
	
	protected static double getConditionBonus(L2Character attacker, L2Character target)
	{
		double mod = 100;
		// Get high or low bonus
		if (attacker.getZ() - target.getZ() > 50)
		{
			mod += hitConditionBonus.highBonus;
		}
		else if (attacker.getZ() - target.getZ() < -50)
		{
			mod += hitConditionBonus.lowBonus;
		}
		
		// Get weather bonus
		if (GameTimeController.getInstance().isNight())
		{
			mod += hitConditionBonus.darkBonus;
			// else if () No rain support yet.
			// chance += hitConditionBonus.rainBonus;
		}
		
		// Get side bonus
		if (attacker.isBehindTarget())
		{
			mod += hitConditionBonus.backBonus;
		}
		else if (attacker.isFrontTarget())
		{
			mod += hitConditionBonus.frontBonus;
		}
		else
		{
			mod += hitConditionBonus.sideBonus;
		}
		
		// If (mod / 10) is less than 0, return 0, because we cant lower more than 100%.
		return Math.max(mod / 100, 0);
	}
	
	static
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		final File file = new File(Config.DATAPACK_ROOT, "data/xml/hitConditionBonus.xml");
		Document doc = null;
		
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				LOG.error("[hitConditionBonus] Could not parse file: " + e.getMessage(), e);
			}
			
			String name;
			for (@SuppressWarnings("null")
			Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
			{
				if ("hitConditionBonus".equalsIgnoreCase(list.getNodeName()) || "list".equalsIgnoreCase(list.getNodeName()))
				{
					for (Node cond = list.getFirstChild(); cond != null; cond = cond.getNextSibling())
					{
						int bonus = 0;
						name = cond.getNodeName();
						try
						{
							if (cond.hasAttributes())
							{
								bonus = Integer.parseInt(cond.getAttributes().getNamedItem("val").getNodeValue());
							}
						}
						catch (Exception e)
						{
							LOG.error("[hitConditionBonus] Could not parse condition: " + e.getMessage(), e);
						}
						finally
						{
							if ("front".equals(name))
							{
								frontBonus = bonus;
							}
							else if ("side".equals(name))
							{
								sideBonus = bonus;
							}
							else if ("back".equals(name))
							{
								backBonus = bonus;
							}
							else if ("high".equals(name))
							{
								highBonus = bonus;
							}
							else if ("low".equals(name))
							{
								lowBonus = bonus;
							}
							else if ("dark".equals(name))
							{
								darkBonus = bonus;
								// else if ("rain".equals(name))
								// rainBonus = bonus;
							}
						}
						
					}
				}
			}
		}
		else
		{
			throw new Error("[hitConditionBonus] File not found: " + file.getName());
		}
	}
}