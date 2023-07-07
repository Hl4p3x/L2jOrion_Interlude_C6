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
package l2jorion.game.managers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.game.enums.AchType;
import l2jorion.game.model.AchievementHolder;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;
import l2jorion.util.xml.IXmlReader;

public class AchievementManager implements IXmlReader
{
	private static Logger LOG = LoggerFactory.getLogger(AchievementManager.class);
	
	private final Map<AchType, List<AchievementHolder>> _achievements = new LinkedHashMap<>();
	
	public AchievementManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("./data/xml/achievements.xml");
		LOG.info("Loaded {} of {} achievements data.", _achievements.size(), AchType.values().length);
	}
	
	public void reload()
	{
		_achievements.clear();
		load();
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		for (Node a = doc.getFirstChild(); a != null; a = a.getNextSibling())
		{
			if ("list".equalsIgnoreCase(a.getNodeName()))
			{
				for (Node b = a.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("achievement".equalsIgnoreCase(b.getNodeName()))
					{
						final NamedNodeMap attrs = b.getAttributes();
						
						final boolean daily = parseBoolean(attrs, "daily", false);
						final AchType type = AchType.valueOf(parseString(attrs, "type"));
						
						final StatsSet set = new StatsSet();
						
						set.set("daily", daily);
						set.set("icon", parseString(attrs, "icon"));
						set.set("name", parseString(attrs, "name"));
						set.set("desc", parseString(attrs, "desc"));
						
						final List<AchievementHolder> levels = new ArrayList<>();
						List<AchievementHolder> dailyRandomLevel = new ArrayList<>();
						
						for (Node c = b.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("stage".equalsIgnoreCase(c.getNodeName()))
							{
								set.add(parseAttributes(c));
								levels.add(new AchievementHolder(set));
							}
						}
						
						// Add 1 by random
						if (daily)
						{
							dailyRandomLevel.add(0, levels.get(Rnd.get(0, levels.size() - 1)));
						}
						
						_achievements.put(type, (daily ? dailyRandomLevel : levels));
					}
				}
			}
		}
		
	}
	
	public List<AchievementHolder> getStages(AchType type)
	{
		return _achievements.get(type);
	}
	
	public AchievementHolder getAchievement(AchType type, int level)
	{
		return _achievements.get(type).stream().filter(x -> x.getLevel() == level).findFirst().orElse(null);
	}
	
	public Map<AchType, List<AchievementHolder>> getAchievements()
	{
		return _achievements;
	}
	
	// One time
	public List<AchType> getTypeList(L2PcInstance player)
	{
		List<AchType> list = new ArrayList<>();
		for (AchType type : _achievements.keySet())
		{
			if (String.valueOf(type).contains("DAILY"))
			{
				continue;
			}
			
			if (type == AchType.SPOIL && !player.getSkills().containsKey(254))
			{
				continue;
			}
			
			if (type == AchType.LEADER && player.getClan() != null && !player.isClanLeader())
			{
				continue;
			}
			
			if ((type == AchType.CLAN_LEVEL_UP || type == AchType.CASTLE) && !player.isClanLeader())
			{
				continue;
			}
			
			if (type == AchType.ACADEMY && player.getClassId().level() > 1 && player.getAchievement().getLevel(AchType.ACADEMY) != 2)
			{
				continue;
			}
			
			if (type == AchType.MONSTER_CHAMPION && Config.L2JMOD_CHAMPION_FREQUENCY == 0)
			{
				continue;
			}
			
			list.add(type);
		}
		return list;
	}
	
	// Daily
	public List<AchType> getDailyTypeList(L2PcInstance player)
	{
		List<AchType> list = new ArrayList<>();
		for (AchType type : _achievements.keySet())
		{
			if (!(String.valueOf(type).contains("DAILY")))
			{
				continue;
			}
			
			if (type == AchType.SPOIL && !player.getSkills().containsKey(254))
			{
				continue;
			}
			
			if (type == AchType.LEADER && player.getClan() != null && !player.isClanLeader())
			{
				continue;
			}
			
			if ((type == AchType.CLAN_LEVEL_UP || type == AchType.CASTLE) && !player.isClanLeader())
			{
				continue;
			}
			
			if (type == AchType.ACADEMY && player.getClassId().level() > 1 && player.getAchievement().getLevel(AchType.ACADEMY) != 2)
			{
				continue;
			}
			
			if (type == AchType.MONSTER_CHAMPION && Config.L2JMOD_CHAMPION_FREQUENCY == 0)
			{
				continue;
			}
			
			list.add(type);
		}
		
		return list;
	}
	
	public static AchievementManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AchievementManager INSTANCE = new AchievementManager();
	}
}