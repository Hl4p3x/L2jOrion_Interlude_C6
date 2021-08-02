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
package l2jorion.game.templates;

import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.game.model.L2DropCategory;
import l2jorion.game.model.L2DropData;
import l2jorion.game.model.L2MinionData;
import l2jorion.game.model.L2NpcAIData;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.skills.Stats;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class L2NpcTemplate extends L2CharTemplate
{
	protected static final Logger LOG = LoggerFactory.getLogger(Quest.class);
	
	public final int npcId;
	public final int idTemplate;
	public final String type;
	public final String name;
	public final boolean serverSideName;
	public final String title;
	public final boolean serverSideTitle;
	public final String sex;
	public final byte level;
	public final int rewardExp;
	public final int rewardSp;
	public final int aggroRange;
	public final int rhand;
	public final int lhand;
	public final int armor;
	public final String factionId;
	public final int factionRange;
	public final int absorbLevel;
	public final AbsorbCrystalType absorbType;
	public final int DropHerb;
	public Race race;
	
	private final boolean _custom;
	
	// Skills AI
	private final FastList<L2Skill> _buffSkills = new FastList<>();
	private final FastList<L2Skill> _negativeSkills = new FastList<>();
	private final FastList<L2Skill> _debuffSkills = new FastList<>();
	private final FastList<L2Skill> _atkSkills = new FastList<>();
	private final FastList<L2Skill> _rootSkills = new FastList<>();
	private final FastList<L2Skill> _stunSkills = new FastList<>();
	private final FastList<L2Skill> _sleepSkills = new FastList<>();
	private final FastList<L2Skill> _paralyzeSkills = new FastList<>();
	private final FastList<L2Skill> _fossilSkills = new FastList<>();
	private final FastList<L2Skill> _immobilizeSkills = new FastList<>();
	private final FastList<L2Skill> _healSkills = new FastList<>();
	private final FastList<L2Skill> _dotSkills = new FastList<>();
	private final FastList<L2Skill> _cotSkills = new FastList<>();
	private final FastList<L2Skill> _universalSkills = new FastList<>();
	private final FastList<L2Skill> _manaSkills = new FastList<>();
	private final FastList<L2Skill> _longRangeSkills = new FastList<>();
	private final FastList<L2Skill> _shortRangeSkills = new FastList<>();
	private final FastList<L2Skill> _generalSkills = new FastList<>();
	private final FastList<L2Skill> _suicideSkills = new FastList<>();
	
	private L2NpcAIData _AIdataStatic = new L2NpcAIData();
	
	public static enum AIType
	{
		FIGHTER,
		ARCHER,
		BALANCED,
		MAGE,
		HEALER,
		CORPSE
	}
	
	public static enum AbsorbCrystalType
	{
		LAST_HIT,
		FULL_PARTY,
		PARTY_ONE_RANDOM
	}
	
	public static enum Race
	{
		UNDEAD,
		MAGICCREATURE,
		BEAST,
		ANIMAL,
		PLANT,
		HUMANOID,
		SPIRIT,
		ANGEL,
		DEMON,
		DRAGON,
		GIANT,
		BUG,
		FAIRIE,
		HUMAN,
		ELVE,
		DARKELVE,
		ORC,
		DWARVE,
		OTHER,
		NONLIVING,
		SIEGEWEAPON,
		DEFENDINGARMY,
		MERCENARIE,
		UNKNOWN
	}
	
	private final StatsSet _npcStatsSet;
	
	private final FastList<L2DropCategory> _categories = new FastList<>();
	
	private final List<L2MinionData> _minions = new FastList<>(0);
	
	private final List<ClassId> _teachInfo = new FastList<>();
	private final Map<Integer, L2Skill> _skills = new FastMap<>();
	private final Map<Stats, Double> _vulnerabilities = new FastMap<>();
	
	private final Map<Quest.QuestEventType, Quest[]> _questEvents = new FastMap<>();
	
	public L2NpcTemplate(final StatsSet set, final boolean custom)
	{
		super(set);
		npcId = set.getInteger("npcId");
		idTemplate = set.getInteger("idTemplate");
		type = set.getString("type");
		name = set.getString("name");
		serverSideName = set.getBool("serverSideName");
		title = set.getString("title");
		serverSideTitle = set.getBool("serverSideTitle");
		sex = set.getString("sex");
		level = set.getByte("level");
		rewardExp = set.getInteger("rewardExp");
		rewardSp = set.getInteger("rewardSp");
		aggroRange = set.getInteger("aggroRange");
		rhand = set.getInteger("rhand");
		lhand = set.getInteger("lhand");
		armor = set.getInteger("armor");
		
		final String f = set.getString("factionId", null);
		if (f == null)
		{
			factionId = "";
		}
		else
		{
			factionId = f.intern();
		}
		
		factionRange = set.getInteger("factionRange", 0);
		absorbLevel = set.getInteger("absorb_level", 0);
		absorbType = AbsorbCrystalType.valueOf(set.getString("absorb_type"));
		DropHerb = set.getInteger("DropHerb", 0);
		_npcStatsSet = set;
		_custom = custom;
	}
	
	public void addTeachInfo(final ClassId classId)
	{
		_teachInfo.add(classId);
	}
	
	public ClassId[] getTeachInfo()
	{
		return _teachInfo.toArray(new ClassId[_teachInfo.size()]);
	}
	
	public boolean canTeach(final ClassId classId)
	{
		// If the player is on a third class, fetch the class teacher
		// information for its parent class.
		if (classId.getId() >= 88)
		{
			return _teachInfo.contains(classId.getParent());
		}
		
		return _teachInfo.contains(classId);
	}
	
	// add a drop to a given category. If the category does not exist, create it.
	public void addDropData(final L2DropData drop, final int categoryType)
	{
		if (!drop.isQuestDrop())
		{
			boolean catExists = false;
			for (final L2DropCategory cat : _categories)
			{
				// if the category exists, add the drop to this category.
				if (cat.getCategoryType() == categoryType)
				{
					cat.addDropData(drop, type.equalsIgnoreCase("L2RaidBoss") || type.equalsIgnoreCase("L2GrandBoss"));
					catExists = true;
					break;
				}
			}
			// if the category doesn't exit, create it and add the drop
			if (!catExists)
			{
				final L2DropCategory cat = new L2DropCategory(categoryType);
				cat.addDropData(drop, type.equalsIgnoreCase("L2RaidBoss") || type.equalsIgnoreCase("L2GrandBoss"));
				_categories.add(cat);
			}
		}
	}
	
	public void addRaidData(final L2MinionData minion)
	{
		_minions.add(minion);
	}
	
	public void addSkill(L2Skill skill)
	{
		if (!skill.isPassive())
		{
			if (skill.isSuicideAttack())
			{
				addSuicideSkill(skill);
			}
			else
			{
				addGeneralSkill(skill);
				switch (skill.getSkillType())
				{
					case BUFF:
						addBuffSkill(skill);
						break;
					
					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case HEAL_STATIC:
					case BALANCE_LIFE:
						addHealSkill(skill);
						break;
					
					case DEBUFF:
						addDebuffSkill(skill);
						addCOTSkill(skill);
						addRangeSkill(skill);
						break;
					
					case ROOT:
						addRootSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;
					
					case SLEEP:
						addSleepSkill(skill);
						addImmobilizeSkill(skill);
						break;
					
					case STUN:
						addRootSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;
					
					case PARALYZE:
						addParalyzeSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;
					
					case PDAM:
					case MDAM:
					case BLOW:
					case DRAIN:
					case CHARGEDAM:
						// case FATAL:
					case DEATHLINK:
					case MANADAM:
						// case CPDAMPERCENT:
						addAtkSkill(skill);
						addUniversalSkill(skill);
						addRangeSkill(skill);
						break;
					
					case POISON:
					case DOT:
					case MDOT:
					case BLEED:
						addDOTSkill(skill);
						addRangeSkill(skill);
						break;
					
					case MUTE:
					case FEAR:
						addCOTSkill(skill);
						addRangeSkill(skill);
						break;
					
					case CANCEL:
					case NEGATE:
						addNegativeSkill(skill);
						addRangeSkill(skill);
						break;
					
					default:
						addUniversalSkill(skill);
						break;
				}
			}
		}
		_skills.put(skill.getId(), skill);
	}
	
	public void addVulnerability(final Stats id, final double vuln)
	{
		_vulnerabilities.put(id, Double.valueOf(vuln));
	}
	
	public double getVulnerability(final Stats id)
	{
		if (_vulnerabilities.get(id) == null)
		{
			return 1;
		}
		
		return _vulnerabilities.get(id);
	}
	
	public double removeVulnerability(final Stats id)
	{
		return _vulnerabilities.remove(id);
	}
	
	/**
	 * Return the list of all possible UNCATEGORIZED drops of this L2NpcTemplate.<BR>
	 * <BR>
	 * @return
	 */
	public FastList<L2DropCategory> getDropData()
	{
		return _categories;
	}
	
	/**
	 * Return the list of all possible item drops of this L2NpcTemplate.<BR>
	 * (ie full drops and part drops, mats, miscellaneous & UNCATEGORIZED)<BR>
	 * <BR>
	 * @return
	 */
	public List<L2DropData> getAllDropData()
	{
		final List<L2DropData> lst = new FastList<>();
		for (final L2DropCategory tmp : _categories)
		{
			lst.addAll(tmp.getAllDrops());
		}
		return lst;
	}
	
	/**
	 * Empty all possible drops of this L2NpcTemplate.<BR>
	 * <BR>
	 */
	public synchronized void clearAllDropData()
	{
		while (_categories.size() > 0)
		{
			_categories.getFirst().clearAllDrops();
			_categories.removeFirst();
		}
		_categories.clear();
	}
	
	/**
	 * Return the list of all Minions that must be spawn with the L2NpcInstance using this L2NpcTemplate.<BR>
	 * <BR>
	 * @return
	 */
	public List<L2MinionData> getMinionData()
	{
		return _minions;
	}
	
	@Override
	public Map<Integer, L2Skill> getSkills()
	{
		return _skills;
	}
	
	public void addQuestEvent(final Quest.QuestEventType EventType, final Quest q)
	{
		if (_questEvents.get(EventType) == null)
		{
			_questEvents.put(EventType, new Quest[]
			{
				q
			});
		}
		else
		{
			final Quest[] _quests = _questEvents.get(EventType);
			final int len = _quests.length;
			
			// if only one registration per npc is allowed for this event type
			// then only register this NPC if not already registered for the specified event.
			// if a quest allows multiple registrations, then register regardless of count
			// In all cases, check if this new registration is replacing an older copy of the SAME quest
			if (!EventType.isMultipleRegistrationAllowed())
			{
				if (_quests[0].getName().equals(q.getName()))
				{
					_quests[0] = q;
				}
				else
				{
					LOG.warn("Quest event not allowed in multiple quests.  Skipped addition of Event Type \"" + EventType + "\" for NPC \"" + name + "\" and quest \"" + q.getName() + "\".");
				}
			}
			else
			{
				// be ready to add a new quest to a new copy of the list, with larger size than previously.
				final Quest[] tmp = new Quest[len + 1];
				// loop through the existing quests and copy them to the new list. While doing so, also
				// check if this new quest happens to be just a replacement for a previously loaded quest.
				// If so, just save the updated reference and do NOT use the new list. Else, add the new
				// quest to the end of the new list
				for (int i = 0; i < len; i++)
				{
					if (_quests[i].getName().equals(q.getName()))
					{
						_quests[i] = q;
						return;
					}
					tmp[i] = _quests[i];
				}
				tmp[len] = q;
				_questEvents.put(EventType, tmp);
			}
		}
	}
	
	public Quest[] getEventQuests(Quest.QuestEventType EventType)
	{
		if (_questEvents.get(EventType) == null)
		{
			return new Quest[0];
		}
		
		return _questEvents.get(EventType);
	}
	
	public StatsSet getStatsSet()
	{
		return _npcStatsSet;
	}
	
	public void setRace(final int raceId)
	{
		switch (raceId)
		{
			case 1:
				race = L2NpcTemplate.Race.UNDEAD;
				break;
			case 2:
				race = L2NpcTemplate.Race.MAGICCREATURE;
				break;
			case 3:
				race = L2NpcTemplate.Race.BEAST;
				break;
			case 4:
				race = L2NpcTemplate.Race.ANIMAL;
				break;
			case 5:
				race = L2NpcTemplate.Race.PLANT;
				break;
			case 6:
				race = L2NpcTemplate.Race.HUMANOID;
				break;
			case 7:
				race = L2NpcTemplate.Race.SPIRIT;
				break;
			case 8:
				race = L2NpcTemplate.Race.ANGEL;
				break;
			case 9:
				race = L2NpcTemplate.Race.DEMON;
				break;
			case 10:
				race = L2NpcTemplate.Race.DRAGON;
				break;
			case 11:
				race = L2NpcTemplate.Race.GIANT;
				break;
			case 12:
				race = L2NpcTemplate.Race.BUG;
				break;
			case 13:
				race = L2NpcTemplate.Race.FAIRIE;
				break;
			case 14:
				race = L2NpcTemplate.Race.HUMAN;
				break;
			case 15:
				race = L2NpcTemplate.Race.ELVE;
				break;
			case 16:
				race = L2NpcTemplate.Race.DARKELVE;
				break;
			case 17:
				race = L2NpcTemplate.Race.ORC;
				break;
			case 18:
				race = L2NpcTemplate.Race.DWARVE;
				break;
			case 19:
				race = L2NpcTemplate.Race.OTHER;
				break;
			case 20:
				race = L2NpcTemplate.Race.NONLIVING;
				break;
			case 21:
				race = L2NpcTemplate.Race.SIEGEWEAPON;
				break;
			case 22:
				race = L2NpcTemplate.Race.DEFENDINGARMY;
				break;
			case 23:
				race = L2NpcTemplate.Race.MERCENARIE;
				break;
			default:
				race = L2NpcTemplate.Race.UNKNOWN;
				break;
		}
	}
	
	public L2NpcTemplate.Race getRace()
	{
		if (race == null)
		{
			race = L2NpcTemplate.Race.UNKNOWN;
		}
		
		return race;
	}
	
	/**
	 * @return the level
	 */
	public byte getLevel()
	{
		return level;
	}
	
	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}
	
	public String getFactionId()
	{
		return factionId;
	}
	
	public int getFactionRange()
	{
		return factionRange;
	}
	
	/**
	 * @return the npcId
	 */
	public int getNpcId()
	{
		return npcId;
	}
	
	public int getTemplateId()
	{
		return idTemplate;
	}
	
	public final boolean isCustom()
	{
		return _custom;
	}
	
	public String getType()
	{
		return type;
	}
	
	public boolean isServerSideName()
	{
		return serverSideName;
	}
	
	/**
	 * @return true if the NPC uses server side title, false otherwise.
	 */
	public boolean isServerSideTitle()
	{
		return serverSideTitle;
	}
	
	public boolean isType(String t)
	{
		return type.equalsIgnoreCase(t);
	}
	
	// NPC ai data
	
	public void setAIData(L2NpcAIData aidata)
	{
		_AIdataStatic = aidata;
	}
	
	public L2NpcAIData getAIDataStatic()
	{
		return _AIdataStatic;
	}
	
	public void addBuffSkill(L2Skill skill)
	{
		_buffSkills.add(skill);
	}
	
	public void addHealSkill(L2Skill skill)
	{
		_healSkills.add(skill);
	}
	
	public void addAtkSkill(L2Skill skill)
	{
		_atkSkills.add(skill);
	}
	
	public void addDebuffSkill(L2Skill skill)
	{
		_debuffSkills.add(skill);
	}
	
	public void addRootSkill(L2Skill skill)
	{
		_rootSkills.add(skill);
	}
	
	public void addSleepSkill(L2Skill skill)
	{
		_sleepSkills.add(skill);
	}
	
	public void addStunSkill(L2Skill skill)
	{
		_stunSkills.add(skill);
	}
	
	public void addParalyzeSkill(L2Skill skill)
	{
		_paralyzeSkills.add(skill);
	}
	
	public void addFossilSkill(L2Skill skill)
	{
		_fossilSkills.add(skill);
	}
	
	public void addNegativeSkill(L2Skill skill)
	{
		_negativeSkills.add(skill);
	}
	
	public void addImmobilizeSkill(L2Skill skill)
	{
		_immobilizeSkills.add(skill);
	}
	
	public void addDOTSkill(L2Skill skill)
	{
		_dotSkills.add(skill);
	}
	
	public void addUniversalSkill(L2Skill skill)
	{
		_universalSkills.add(skill);
	}
	
	public void addCOTSkill(L2Skill skill)
	{
		_cotSkills.add(skill);
	}
	
	public void addManaHealSkill(L2Skill skill)
	{
		_manaSkills.add(skill);
	}
	
	public void addGeneralSkill(L2Skill skill)
	{
		_generalSkills.add(skill);
	}
	
	public void addRangeSkill(L2Skill skill)
	{
		if ((skill.getCastRange() <= 150) && (skill.getCastRange() > 0))
		{
			_shortRangeSkills.add(skill);
		}
		else if (skill.getCastRange() > 150)
		{
			_longRangeSkills.add(skill);
		}
	}
	
	public void addSuicideSkill(L2Skill skill)
	{
		_suicideSkills.add(skill);
	}
	
	public FastList<L2Skill> getUniversalSkills()
	{
		return _universalSkills;
	}
	
	public FastList<L2Skill> getSuicideSkills()
	{
		return _suicideSkills;
	}
	
	public FastList<L2Skill> getNegativeSkills()
	{
		return _negativeSkills;
	}
	
	public FastList<L2Skill> getImmobilizeSkills()
	{
		return _immobilizeSkills;
	}
	
	public FastList<L2Skill> getGeneralSkills()
	{
		return _generalSkills;
	}
	
	public FastList<L2Skill> getHealSkills()
	{
		return _healSkills;
	}
	
	public FastList<L2Skill> getCostOverTimeSkills()
	{
		return _cotSkills;
	}
	
	public FastList<L2Skill> getDebuffSkills()
	{
		return _debuffSkills;
	}
	
	public FastList<L2Skill> getBuffSkills()
	{
		return _buffSkills;
	}
	
	public FastList<L2Skill> getAtkSkills()
	{
		return _atkSkills;
	}
	
	/**
	 * @return the long range skills.
	 */
	public FastList<L2Skill> getLongRangeSkills()
	{
		return _longRangeSkills;
	}
	
	/**
	 * @return the short range skills.
	 */
	public FastList<L2Skill> getShortRangeSkills()
	{
		return _shortRangeSkills;
	}
}
