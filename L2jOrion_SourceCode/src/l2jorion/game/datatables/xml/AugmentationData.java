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
package l2jorion.game.datatables.xml;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Augmentation;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.skills.Stats;
import l2jorion.game.skills.holders.IntIntHolder;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class AugmentationData
{
	private static final Logger LOG = LoggerFactory.getLogger(AugmentationData.class);
	
	private static AugmentationData _instance;
	
	public static final AugmentationData getInstance()
	{
		if (_instance == null)
		{
			_instance = new AugmentationData();
		}
		
		return _instance;
	}
	
	// stats
	private static final int STAT_START = 1;
	private static final int STAT_END = 14560;
	private static final int STAT_BLOCKSIZE = 3640;
	// private static final int STAT_NUMBEROF_BLOCKS = 4;
	private static final int STAT_SUBBLOCKSIZE = 91;
	// private static final int STAT_NUMBEROF_SUBBLOCKS = 40;
	
	// skills
	private static final int BLUE_START = 14561;
	// private static final int PURPLE_START = 14578;
	// private static final int RED_START = 14685;
	private static final int SKILLS_BLOCKSIZE = 178;
	
	// basestats
	private static final int BASESTAT_STR = 16341;
	private static final int BASESTAT_CON = 16342;
	private static final int BASESTAT_INT = 16343;
	private static final int BASESTAT_MEN = 16344;
	
	private final FastList<augmentationStat> _augmentationStats[];
	private final Map<Integer, FastList<augmentationSkill>> _blueSkills;
	private final Map<Integer, FastList<augmentationSkill>> _purpleSkills;
	private final Map<Integer, FastList<augmentationSkill>> _redSkills;
	
	private final Map<Integer, IntIntHolder> _allSkills = new HashMap<>();
	
	// =========================================================
	// Constructor
	@SuppressWarnings("unchecked")
	private AugmentationData()
	{
		// LOG.info("Initializing AugmentationData.");
		
		_augmentationStats = new FastList[4];
		_augmentationStats[0] = new FastList<>();
		_augmentationStats[1] = new FastList<>();
		_augmentationStats[2] = new FastList<>();
		_augmentationStats[3] = new FastList<>();
		
		_blueSkills = new FastMap<>();
		_purpleSkills = new FastMap<>();
		_redSkills = new FastMap<>();
		for (int i = 1; i <= 10; i++)
		{
			_blueSkills.put(i, new FastList<augmentationSkill>());
			_purpleSkills.put(i, new FastList<augmentationSkill>());
			_redSkills.put(i, new FastList<augmentationSkill>());
		}
		
		load();
		
		// Use size*4: since theres 4 blocks of stat-data with equivalent size
		LOG.info("AugmentationData: Loaded: " + _augmentationStats[0].size() * 4 + " augmentation stats");
		
		if (Config.DEBUG)
		{
			for (int i = 1; i <= 10; i++)
			{
				LOG.info("AugmentationData: Loaded: " + _blueSkills.get(i).size() + " blue, " + _purpleSkills.get(i).size() + " purple and " + _redSkills.get(i).size() + " red skills for lifeStoneLevel " + i);
			}
		}
		
	}
	
	public static void reload()
	{
		_instance = null;
		getInstance();
	}
	
	// =========================================================
	// Nested Class
	
	public class augmentationSkill
	{
		private final int _skillId;
		private final int _maxSkillLevel;
		private final int _augmentationSkillId;
		
		public augmentationSkill(final int skillId, final int maxSkillLevel, final int augmentationSkillId)
		{
			_skillId = skillId;
			_maxSkillLevel = maxSkillLevel;
			_augmentationSkillId = augmentationSkillId;
		}
		
		public L2Skill getSkill(final int level)
		{
			if (level > _maxSkillLevel)
			{
				return SkillTable.getInstance().getInfo(_skillId, _maxSkillLevel);
			}
			
			return SkillTable.getInstance().getInfo(_skillId, level);
		}
		
		public int getAugmentationSkillId()
		{
			return _augmentationSkillId;
		}
	}
	
	public class augmentationStat
	{
		private final Stats _stat;
		private final int _singleSize;
		private final int _combinedSize;
		private final float _singleValues[];
		private final float _combinedValues[];
		
		public augmentationStat(final Stats stat, final float sValues[], final float cValues[])
		{
			_stat = stat;
			_singleSize = sValues.length;
			_singleValues = sValues;
			_combinedSize = cValues.length;
			_combinedValues = cValues;
		}
		
		public int getSingleStatSize()
		{
			return _singleSize;
		}
		
		public int getCombinedStatSize()
		{
			return _combinedSize;
		}
		
		public float getSingleStatValue(final int i)
		{
			if (i >= _singleSize || i < 0)
			{
				return _singleValues[_singleSize - 1];
			}
			
			return _singleValues[i];
		}
		
		public float getCombinedStatValue(final int i)
		{
			if (i >= _combinedSize || i < 0)
			{
				return _combinedValues[_combinedSize - 1];
			}
			
			return _combinedValues[i];
		}
		
		public Stats getStat()
		{
			return _stat;
		}
	}
	
	// =========================================================
	// Method - Private
	
	private final void load()
	{
		// Load the skillmap
		// Note: the skillmap data is only used when generating new augmentations
		// the client expects a different id in order to display the skill in the
		// items description...
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			int badAugmantData = 0;
			
			File file = new File(Config.DATAPACK_ROOT + "/data/xml/augmentation/augmentation_skillmap.xml");
			if (!file.exists())
			{
				if (Config.DEBUG)
				{
					LOG.info("The augmentation skillmap file is missing.");
				}
				return;
			}
			
			Document doc = factory.newDocumentBuilder().parse(file);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("augmentation".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int skillId = 0;
							final int augmentationId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							String type = "blue";
							int skillLvL = 0;
							
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("skillId".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillId = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if ("skillLevel".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillLvL = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if ("type".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									type = attrs.getNamedItem("val").getNodeValue();
								}
							}
							
							if (skillId == 0)
							{
								if (Config.DEBUG)
								{
									LOG.error("Bad skillId in augmentation_skillmap.xml in the augmentationId:" + augmentationId);
								}
								badAugmantData++;
								continue;
							}
							else if (skillLvL == 0)
							{
								if (Config.DEBUG)
								{
									LOG.error("Bad skillLevel in augmentation_skillmap.xml in the augmentationId:" + augmentationId);
								}
								badAugmantData++;
								continue;
							}
							
							int k = 1;
							while (augmentationId - k * SKILLS_BLOCKSIZE >= BLUE_START)
							{
								k++;
							}
							
							if (type.equalsIgnoreCase("blue"))
							{
								_blueSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
							}
							else if (type.equalsIgnoreCase("purple"))
							{
								_purpleSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
							}
							else
							{
								_redSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
							}
							
							_allSkills.put(augmentationId, new IntIntHolder(skillId, skillLvL));
						}
					}
				}
			}
			
			if (badAugmantData != 0)
			{
				LOG.info("AugmentationData: " + badAugmantData + " bad skill(s) were skipped.");
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Error parsing augmentation_skillmap.xml.", e);
			
			return;
		}
		
		// Load the stats from xml
		for (int i = 1; i < 5; i++)
		{
			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				
				File file = new File(Config.DATAPACK_ROOT + "/data/xml/augmentation/augmentation_stats" + i + ".xml");
				
				if (!file.exists())
				{
					if (Config.DEBUG)
					{
						LOG.info("The augmentation stat data file " + i + " is missing.");
					}
					
					return;
				}
				
				Document doc = factory.newDocumentBuilder().parse(file);
				
				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								String statName = attrs.getNamedItem("name").getNodeValue();
								
								float soloValues[] = null, combinedValues[] = null;
								
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("table".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String tableName = attrs.getNamedItem("name").getNodeValue();
										
										final StringTokenizer data = new StringTokenizer(cd.getFirstChild().getNodeValue());
										final List<Float> array = new FastList<>();
										
										while (data.hasMoreTokens())
										{
											array.add(Float.parseFloat(data.nextToken()));
										}
										
										if (tableName.equalsIgnoreCase("#soloValues"))
										{
											soloValues = new float[array.size()];
											int x = 0;
											
											for (final float value : array)
											{
												soloValues[x++] = value;
											}
										}
										else
										{
											combinedValues = new float[array.size()];
											int x = 0;
											
											for (final float value : array)
											{
												combinedValues[x++] = value;
											}
										}
										
										tableName = null;
									}
								}
								
								// store this stat
								_augmentationStats[(i - 1)].add(new augmentationStat(Stats.valueOfXml(statName), soloValues, combinedValues));
							}
						}
					}
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.error("Error parsing augmentation_stats" + i + ".xml.", e);
				return;
			}
		}
	}
	
	public L2Augmentation generateAugmentationWithSkill(L2ItemInstance item, int id, int level)
	{
		int stat12 = 0;
		int stat34 = 0;
		int lifeStoneLevel = 9;
		int lifeStoneGrade = 3;
		int resultColor = 3;
		
		L2Skill skill = null;
		for (int i : _allSkills.keySet())
		{
			L2Skill sk = _allSkills.get(i).getSkill();
			if (sk.getId() == id)
			{
				if (sk.getLevel() == level)
				{
					skill = sk;
				}
				
				stat34 = i;
				break;
			}
		}
		
		if (skill == null)
		{
			skill = SkillTable.getInstance().getInfo(id, level);
		}
		
		/*
		 * for (int i = 1; i < 10; i++) { _a.sys("i:" + i); if (_blueSkills.get(i).contains(stat34)) { resultColor = 1; lifeStoneLevel = i; break; } else if (_redSkills.get(i).contains(stat34)) { resultColor = 2; lifeStoneLevel = i; break; } else if (_purpleSkills.get(i).contains(stat34)) {
		 * resultColor = 3; lifeStoneLevel = i; break; } }
		 */
		
		int offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + Rnd.get(0, 1) * STAT_BLOCKSIZE + (lifeStoneGrade + resultColor) / 2 * (10 * STAT_SUBBLOCKSIZE) + 1;
		stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		
		return new L2Augmentation(item, ((stat34 << 16) + stat12), skill, true);
	}
	
	public L2Augmentation generateRandomAugmentation(final L2ItemInstance item, final int lifeStoneLevel, final int lifeStoneGrade)
	{
		// Note that stat12 stands for stat 1 AND 2 (same for stat34 ;p )
		// this is because a value can contain up to 2 stat modifications
		// (there are two short values packed in one integer value, meaning 4 stat modifications at max)
		// for more info take a look at getAugStatsById(...)
		
		// Note: lifeStoneGrade: (0 means low grade, 3 top grade)
		// First: determine whether we will add a skill/baseStatModifier or not because this determine which color could be the result
		int skill_Chance = 0;
		int stat34 = 0;
		boolean generateSkill = false;
		int resultColor = 0;
		boolean generateGlow = false;
		
		switch (lifeStoneGrade)
		{
			case 0:
				skill_Chance = Config.AUGMENTATION_NG_SKILL_CHANCE;
				
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_NG_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;
			
			case 1:
				skill_Chance = Config.AUGMENTATION_MID_SKILL_CHANCE;
				
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_MID_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;
			
			case 2:
				skill_Chance = Config.AUGMENTATION_HIGH_SKILL_CHANCE;
				
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_HIGH_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;
			
			case 3:
				skill_Chance = Config.AUGMENTATION_TOP_SKILL_CHANCE;
				
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_TOP_GLOW_CHANCE)
				{
					generateGlow = true;
				}
		}
		
		if (Rnd.get(1, 100) <= skill_Chance)
		{
			generateSkill = true;
		}
		else if (Rnd.get(1, 100) <= Config.AUGMENTATION_BASESTAT_CHANCE)
		{
			stat34 = Rnd.get(BASESTAT_STR, BASESTAT_MEN);
		}
		
		// Second: decide which grade the augmentation result is going to have:
		// 0:yellow, 1:blue, 2:purple, 3:red
		// The chances used here are most likely custom,
		// whats known is: u can also get a red result from a normal grade lifeStone
		// however I will make it so that a higher grade lifeStone will more likely result in a
		// higher grade augmentation... and the augmentation result will at least have the grade
		// of the life stone
		// Second: Calculate the subblock offset for the choosen color,
		// and the level of the lifeStone
		// whats known is: you cant have yellow with skill(or baseStatModifier)
		// noGrade stone can not have Glow, mid only with skill, high has a chance(custom), top allways Glow
		if (stat34 == 0 && !generateSkill)
		{
			resultColor = Rnd.get(0, 100);
			
			if (resultColor <= 15 * lifeStoneGrade + 40)
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 0;
			}
		}
		else
		{
			resultColor = Rnd.get(0, 100);
			
			if (resultColor <= 10 * lifeStoneGrade + 5 || stat34 != 0)
			{
				resultColor = 3;
			}
			else if (resultColor <= 10 * lifeStoneGrade + 10)
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 2;
			}
		}
		
		// is neither a skill nor basestat used for stat34? then generate a normal stat
		int stat12 = 0;
		
		if (stat34 == 0 && !generateSkill)
		{
			final int temp = Rnd.get(2, 3);
			final int colorOffset = resultColor * 10 * STAT_SUBBLOCKSIZE + temp * STAT_BLOCKSIZE + 1;
			int offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + colorOffset;
			
			stat34 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
			
			if (generateGlow && lifeStoneGrade >= 2)
			{
				offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + (temp - 2) * STAT_BLOCKSIZE + lifeStoneGrade * 10 * STAT_SUBBLOCKSIZE + 1;
			}
			else
			{
				offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + (temp - 2) * STAT_BLOCKSIZE + Rnd.get(0, 1) * 10 * STAT_SUBBLOCKSIZE + 1;
			}
			stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		}
		else
		{
			int offset;
			
			if (!generateGlow)
			{
				offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + Rnd.get(0, 1) * STAT_BLOCKSIZE + 1;
			}
			else
			{
				offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + Rnd.get(0, 1) * STAT_BLOCKSIZE + (lifeStoneGrade + resultColor) / 2 * 10 * STAT_SUBBLOCKSIZE + 1;
			}
			stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		}
		
		// generate a skill if neccessary
		L2Skill skill = null;
		if (generateSkill)
		{
			augmentationSkill temp = null;
			switch (resultColor)
			{
				case 1: // blue skill
					temp = _blueSkills.get(lifeStoneLevel).get(Rnd.get(0, _blueSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill(lifeStoneLevel);
					stat34 = temp.getAugmentationSkillId();
					break;
				case 2: // purple skill
					temp = _purpleSkills.get(lifeStoneLevel).get(Rnd.get(0, _purpleSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill(lifeStoneLevel);
					stat34 = temp.getAugmentationSkillId();
					break;
				case 3: // red skill
					temp = _redSkills.get(lifeStoneLevel).get(Rnd.get(0, _redSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill(lifeStoneLevel);
					stat34 = temp.getAugmentationSkillId();
					break;
			}
		}
		if (Config.DEBUG)
		{
			LOG.info("Augmentation success: stat12=" + stat12 + "; stat34=" + stat34 + "; resultColor=" + resultColor + "; level=" + lifeStoneLevel + "; grade=" + lifeStoneGrade);
		}
		
		return new L2Augmentation(item, ((stat34 << 16) + stat12), skill, true);
	}
	
	public L2Augmentation generateAugmentationForMarket(final L2ItemInstance item, int effectId, L2Skill skill)
	{
		return new L2Augmentation(item, effectId, skill, true);
	}
	
	public class AugStat
	{
		private final Stats _stat;
		private final float _value;
		
		public AugStat(final Stats stat, final float value)
		{
			_stat = stat;
			_value = value;
		}
		
		public Stats getStat()
		{
			return _stat;
		}
		
		public float getValue()
		{
			return _value;
		}
	}
	
	/**
	 * Returns the stat and basestat boni for a given augmentation id
	 * @param augmentationId
	 * @return
	 */
	public FastList<AugStat> getAugStatsById(final int augmentationId)
	{
		final FastList<AugStat> temp = new FastList<>();
		// An augmentation id contains 2 short vaues so we gotta seperate them here
		// both values contain a number from 1-16380, the first 14560 values are stats
		// the 14560 stats are devided into 4 blocks each holding 3640 values
		// each block contains 40 subblocks holding 91 stat values
		// the first 13 values are so called Solo-stats and they have the highest stat increase possible
		// after the 13 Solo-stats come 78 combined stats (thats every possible combination of the 13 solo stats)
		// the first 12 combined stats (14-26) is the stat 1 combined with stat 2-13
		// the next 11 combined stats then are stat 2 combined with stat 3-13 and so on...
		// to get the idea have a look @ optiondata_client-e.dat - thats where the data came from :)
		final int stats[] = new int[2];
		stats[0] = 0x0000FFFF & augmentationId;
		stats[1] = augmentationId >> 16;
		
		for (int i = 0; i < 2; i++)
		{
			// its a stat
			if (stats[i] >= STAT_START && stats[i] <= STAT_END)
			{
				int block = 0;
				
				while (stats[i] > STAT_BLOCKSIZE)
				{
					stats[i] -= STAT_BLOCKSIZE;
					block++;
				}
				
				int subblock = 0;
				
				while (stats[i] > STAT_SUBBLOCKSIZE)
				{
					stats[i] -= STAT_SUBBLOCKSIZE;
					subblock++;
				}
				
				if (stats[i] < 14) // solo stat
				{
					final augmentationStat as = _augmentationStats[block].get((stats[i] - 1));
					temp.add(new AugStat(as.getStat(), as.getSingleStatValue(subblock)));
				}
				else
				// twin stat
				{
					stats[i] -= 13; // rescale to 0 (if first of first combined block)
					
					int x = 12; // next combi block has 12 stats
					int rescales = 0; // number of rescales done
					
					while (stats[i] > x)
					{
						stats[i] -= x;
						x--;
						rescales++;
					}
					
					// get first stat
					augmentationStat as = _augmentationStats[block].get(rescales);
					if (rescales == 0)
					{
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock)));
					}
					else
					{
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock * 2 + 1)));
					}
					
					// get 2nd stat
					as = _augmentationStats[block].get(rescales + stats[i]);
					if (as.getStat() == Stats.CRITICAL_DAMAGE)
					{
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock)));
					}
					else
					{
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock * 2)));
					}
					
					as = null;
				}
			}
			// its a base stat
			else if (stats[i] >= BASESTAT_STR && stats[i] <= BASESTAT_MEN)
			{
				switch (stats[i])
				{
					case BASESTAT_STR:
						temp.add(new AugStat(Stats.STAT_STR, 1.0f));
						break;
					case BASESTAT_CON:
						temp.add(new AugStat(Stats.STAT_CON, 1.0f));
						break;
					case BASESTAT_INT:
						temp.add(new AugStat(Stats.STAT_INT, 1.0f));
						break;
					case BASESTAT_MEN:
						temp.add(new AugStat(Stats.STAT_MEN, 1.0f));
						break;
				}
			}
		}
		
		return temp;
	}
}
