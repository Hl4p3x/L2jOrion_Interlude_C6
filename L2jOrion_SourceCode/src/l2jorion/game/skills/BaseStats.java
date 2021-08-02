/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.skills;

import java.io.File;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public enum BaseStats
{
	STR(new STR()),
	INT(new INT()),
	DEX(new DEX()),
	WIT(new WIT()),
	CON(new CON()),
	MEN(new MEN()),
	NONE(new NONE());
	
	protected static final Logger LOG = LoggerFactory.getLogger(BaseStats.class);
	
	public static final int MAX_STAT_VALUE = 100;
	
	protected static final double[] STRbonus = new double[MAX_STAT_VALUE];
	protected static final double[] INTbonus = new double[MAX_STAT_VALUE];
	protected static final double[] DEXbonus = new double[MAX_STAT_VALUE];
	protected static final double[] WITbonus = new double[MAX_STAT_VALUE];
	protected static final double[] CONbonus = new double[MAX_STAT_VALUE];
	protected static final double[] MENbonus = new double[MAX_STAT_VALUE];

	private final BaseStat _stat;
	
	public final String getValue()
	{
		return _stat.getClass().getSimpleName();
	}
	
	private BaseStats(BaseStat s)
	{
		_stat = s;
	}
	
	public final double calcBonus(L2Character actor)
	{
		if (actor != null)
		{
			return _stat.calcBonus(actor);
		}
		
		return 1;
	}
	
	public static final BaseStats valueOfXml(String name)
	{
		name = name.intern();
		for (BaseStats s : values())
		{
			if (s.getValue().equalsIgnoreCase(name))
			{
				return s;
			}
		}
		
		throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
	}
	
	private interface BaseStat
	{
		public double calcBonus(L2Character actor);
	}
	
	protected static final class STR implements BaseStat
	{
		@Override
		public final double calcBonus(L2Character actor)
		{
			if ((actor instanceof L2RaidBossInstance || actor instanceof L2GrandBossInstance) && Config.ALT_RAIDS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2MonsterInstance && Config.ALT_MOBS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PetInstance && Config.ALT_PETS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PcInstance && Config.ALT_PLAYERS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor.getSTR() > MAX_STAT_VALUE)
			{
				LOG.warn("Character " + actor.getName() + " has STR over max value " + MAX_STAT_VALUE + "... Using " + MAX_STAT_VALUE);
				return STRbonus[MAX_STAT_VALUE];
			}
			return STRbonus[actor.getSTR()];
			
		}
	}
	
	protected static final class INT implements BaseStat
	{
		@Override
		public final double calcBonus(L2Character actor)
		{
			if ((actor instanceof L2RaidBossInstance || actor instanceof L2GrandBossInstance) && Config.ALT_RAIDS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2MonsterInstance && Config.ALT_MOBS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PetInstance && Config.ALT_PETS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PcInstance && Config.ALT_PLAYERS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor.getINT() > MAX_STAT_VALUE)
			{
				LOG.warn("Character " + actor.getName() + " has INT over max value " + MAX_STAT_VALUE + "... Using " + MAX_STAT_VALUE);
				return INTbonus[MAX_STAT_VALUE];
			}
			return INTbonus[actor.getINT()];
			
		}
	}
	
	protected static final class DEX implements BaseStat
	{
		@Override
		public final double calcBonus(L2Character actor)
		{
			if ((actor instanceof L2RaidBossInstance || actor instanceof L2GrandBossInstance) && Config.ALT_RAIDS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2MonsterInstance && Config.ALT_MOBS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PetInstance && Config.ALT_PETS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PcInstance && Config.ALT_PLAYERS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor.getDEX() > MAX_STAT_VALUE)
			{
				LOG.warn("Character " + actor.getName() + " has DEX over max value " + MAX_STAT_VALUE + "... Using " + MAX_STAT_VALUE);
				return DEXbonus[MAX_STAT_VALUE];
			}
			return DEXbonus[actor.getDEX()];
		}
	}
	
	protected static final class WIT implements BaseStat
	{
		@Override
		public final double calcBonus(L2Character actor)
		{
			if ((actor instanceof L2RaidBossInstance || actor instanceof L2GrandBossInstance) && Config.ALT_RAIDS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2MonsterInstance && Config.ALT_MOBS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PetInstance && Config.ALT_PETS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PcInstance && Config.ALT_PLAYERS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor.getWIT() > MAX_STAT_VALUE)
			{
				LOG.warn("Character " + actor.getName() + " has WIT over max value " + MAX_STAT_VALUE + "... Using " + MAX_STAT_VALUE);
				return WITbonus[MAX_STAT_VALUE];
			}
			return WITbonus[actor.getWIT()];
		}
	}
	
	protected static final class CON implements BaseStat
	{
		@Override
		public final double calcBonus(L2Character actor)
		{
			if ((actor instanceof L2RaidBossInstance || actor instanceof L2GrandBossInstance) && Config.ALT_RAIDS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2MonsterInstance && Config.ALT_MOBS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PetInstance && Config.ALT_PETS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PcInstance && Config.ALT_PLAYERS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor.getCON() > MAX_STAT_VALUE)
			{
				LOG.warn("Character " + actor.getName() + " has CON over max value " + MAX_STAT_VALUE + "... Using " + MAX_STAT_VALUE);
				return CONbonus[MAX_STAT_VALUE];
			}
			return CONbonus[actor.getCON()];
		}
	}
	
	protected static final class MEN implements BaseStat
	{
		@Override
		public final double calcBonus(L2Character actor)
		{
			if ((actor instanceof L2RaidBossInstance || actor instanceof L2GrandBossInstance) && Config.ALT_RAIDS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2MonsterInstance && Config.ALT_MOBS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PetInstance && Config.ALT_PETS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor instanceof L2PcInstance && Config.ALT_PLAYERS_STATS_BONUS)
			{
				return 1;
			}
			
			if (actor.getMEN() > MAX_STAT_VALUE)
			{
				LOG.warn("Character " + actor.getName() + " has MEN over max value " + MAX_STAT_VALUE + "... Using " + MAX_STAT_VALUE);
				return MENbonus[MAX_STAT_VALUE];
			}
			return MENbonus[actor.getMEN()];
		}
	}
	
	protected static final class NONE implements BaseStat
	{
		@Override
		public final double calcBonus(L2Character actor)
		{
			return 1f;
		}
	}
	
	static
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final File file = new File(Config.DATAPACK_ROOT, "data/xml/statBonus.xml");
		Document doc = null;
		
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				LOG.warn("[BaseStats] Could not parse file: " + e.getMessage(), e);
			}
			
			if (doc != null)
			{
				String statName;
				int val;
				double bonus;
				NamedNodeMap attrs;
				for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
				{
					if ("list".equalsIgnoreCase(list.getNodeName()))
					{
						for (Node stat = list.getFirstChild(); stat != null; stat = stat.getNextSibling())
						{
							statName = stat.getNodeName();
							for (Node value = stat.getFirstChild(); value != null; value = value.getNextSibling())
							{
								if ("stat".equalsIgnoreCase(value.getNodeName()))
								{
									attrs = value.getAttributes();
									try
									{
										val = Integer.parseInt(attrs.getNamedItem("value").getNodeValue());
										bonus = Double.parseDouble(attrs.getNamedItem("bonus").getNodeValue());
									}
									catch (Exception e)
									{
										LOG.warn("[BaseStats] Invalid stats value: " + value.getNodeValue() + ", skipping");
										continue;
									}
									
									if ("STR".equalsIgnoreCase(statName))
										STRbonus[val] = bonus;
									else if ("INT".equalsIgnoreCase(statName))
										INTbonus[val] = bonus;
									else if ("DEX".equalsIgnoreCase(statName))
										DEXbonus[val] = bonus;
									else if ("WIT".equalsIgnoreCase(statName))
										WITbonus[val] = bonus;
									else if ("CON".equalsIgnoreCase(statName))
										CONbonus[val] = bonus;
									else if ("MEN".equalsIgnoreCase(statName))
										MENbonus[val] = bonus;
									else
										LOG.warn("[BaseStats] Invalid stats name: " + statName + ", skipping");
								}
							}
						}
					}
				}
			}
		}
		else
		{
			throw new Error("[BaseStats] File not found: " + file.getName());
		}
	}
}