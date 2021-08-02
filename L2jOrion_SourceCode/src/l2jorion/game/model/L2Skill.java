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
package l2jorion.game.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.HeroSkillTable;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.actor.instance.L2ArtefactInstance;
import l2jorion.game.model.actor.instance.L2ChestInstance;
import l2jorion.game.model.actor.instance.L2ControlTowerInstance;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.actor.instance.L2SummonInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.BaseStats;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.Formulas;
import l2jorion.game.skills.Stats;
import l2jorion.game.skills.conditions.Condition;
import l2jorion.game.skills.effects.EffectCharge;
import l2jorion.game.skills.effects.EffectTemplate;
import l2jorion.game.skills.funcs.Func;
import l2jorion.game.skills.funcs.FuncTemplate;
import l2jorion.game.skills.l2skills.L2SkillCharge;
import l2jorion.game.skills.l2skills.L2SkillChargeDmg;
import l2jorion.game.skills.l2skills.L2SkillChargeEffect;
import l2jorion.game.skills.l2skills.L2SkillCreateItem;
import l2jorion.game.skills.l2skills.L2SkillDefault;
import l2jorion.game.skills.l2skills.L2SkillDrain;
import l2jorion.game.skills.l2skills.L2SkillSeed;
import l2jorion.game.skills.l2skills.L2SkillSignet;
import l2jorion.game.skills.l2skills.L2SkillSignetCasttime;
import l2jorion.game.skills.l2skills.L2SkillSummon;
import l2jorion.game.templates.StatsSet;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public abstract class L2Skill
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2Skill.class);
	
	private static final L2Object[] _emptyTargetList = new L2Object[0];
	
	public static final int SKILL_CUBIC_MASTERY = 143;
	public static final int SKILL_LUCKY = 194;
	public static final int SKILL_CREATE_COMMON = 1320;
	public static final int SKILL_CREATE_DWARVEN = 172;
	public static final int SKILL_CRYSTALLIZE = 248;
	public static final int SKILL_DIVINE_INSPIRATION = 1405;
	
	public static final boolean geoEnabled = Config.GEODATA;
	
	public static final int SKILL_FAKE_INT = 9001;
	public static final int SKILL_FAKE_WIT = 9002;
	public static final int SKILL_FAKE_MEN = 9003;
	public static final int SKILL_FAKE_CON = 9004;
	public static final int SKILL_FAKE_DEX = 9005;
	public static final int SKILL_FAKE_STR = 9006;
	
	private final int _targetConsumeId;
	private final int _targetConsume;
	
	public static enum SkillOpType
	{
		OP_PASSIVE,
		OP_ACTIVE,
		OP_TOGGLE,
		OP_CHANCE
	}
	
	public static enum SkillTargetType
	{
		TARGET_NONE,
		TARGET_SELF,
		TARGET_ONE,
		TARGET_PET,
		
		TARGET_PARTY,
		TARGET_CLAN,
		TARGET_ALLY,
		
		TARGET_AREA,
		TARGET_FRONT_AREA,
		TARGET_BEHIND_AREA,
		
		TARGET_AURA,
		TARGET_FRONT_AURA,
		TARGET_BEHIND_AURA,
		
		TARGET_AREA_UNDEAD,
		
		TARGET_CORPSE_ALLY,
		TARGET_CORPSE_CLAN,
		
		TARGET_PARTY_MEMBER,
		TARGET_PARTY_OTHER,
		TARGET_CORPSE,
		TARGET_UNDEAD,
		TARGET_MULTIFACE,
		// All res types
		TARGET_CORPSE_PLAYER,
		// Pet res scroll
		TARGET_CORPSE_PET,
		TARGET_ITEM,
		TARGET_AREA_CORPSE_MOB,
		TARGET_AREA_AIM_CORPSE,
		TARGET_CORPSE_MOB,
		TARGET_UNLOCKABLE,
		TARGET_HOLY,
		TARGET_ENEMY_SUMMON,
		TARGET_OWNER_PET,
		TARGET_GROUND,
		TARGET_SIEGE,
		TARGET_TYRANNOSAURUS,
		// Npc skills
		TARGET_CLAN_MEMBER
	}
	
	public static enum SkillType
	{
		// Damage
		PDAM,
		MDAM,
		CPDAM,
		MANADAM,
		DOT,
		MDOT,
		DRAIN_SOUL,
		DRAIN(L2SkillDrain.class),
		DEATHLINK,
		FATALCOUNTER,
		BLOW,
		
		// Disablers
		BLEED,
		POISON,
		STUN,
		ROOT,
		SLOW,
		CONFUSION,
		FEAR,
		SLEEP,
		CONFUSE_MOB_ONLY,
		MUTE,
		PARALYZE,
		WEAKNESS,
		GLOOM,
		SURRENDER,
		HEX,
		DOD,
		
		// hp, mp, cp
		HEAL,
		HOT,
		BALANCE_LIFE,
		HEAL_PERCENT,
		HEAL_STATIC,
		COMBATPOINTHEAL,
		COMBATPOINTPERCENTHEAL,
		CPHOT,
		MANAHEAL,
		MANA_BY_LEVEL,
		MANAHEAL_PERCENT,
		MANARECHARGE,
		MPHOT,
		
		// Aggro
		AGGDAMAGE,
		AGGREDUCE,
		AGGREMOVE,
		AGGREDUCE_CHAR,
		AGGDEBUFF,
		
		// Fishing
		FISHING,
		PUMPING,
		REELING,
		
		// MISC
		UNLOCK,
		ENCHANT_ARMOR,
		ENCHANT_WEAPON,
		SOULSHOT,
		SPIRITSHOT,
		SIEGEFLAG,
		TAKECASTLE,
		DELUXE_KEY_UNLOCK,
		SOW,
		HARVEST,
		GET_PLAYER,
		
		// Creation
		COMMON_CRAFT,
		DWARVEN_CRAFT,
		CREATE_ITEM(L2SkillCreateItem.class),
		SUMMON_TREASURE_KEY,
		
		// Summons
		SUMMON(L2SkillSummon.class),
		FEED_PET,
		DEATHLINK_PET,
		STRSIEGEASSAULT,
		ERASE,
		BETRAY,
		
		// Cancel
		CANCEL,
		MAGE_BANE,
		WARRIOR_BANE,
		NEGATE,
		
		BUFF,
		DEBUFF,
		PASSIVE,
		CONT,
		FUSION,
		SIGNET(L2SkillSignet.class),
		SIGNET_CASTTIME(L2SkillSignetCasttime.class),
		
		RESURRECT,
		CHARGE(L2SkillCharge.class),
		CHARGE_EFFECT(L2SkillChargeEffect.class),
		CHARGEDAM(L2SkillChargeDmg.class),
		MHOT,
		DETECT_WEAKNESS,
		LUCK,
		RECALL,
		SUMMON_FRIEND,
		REFLECT,
		SPOIL,
		SWEEP,
		FAKE_DEATH,
		UNBLEED,
		UNPOISON,
		UNDEAD_DEFENSE,
		SEED(L2SkillSeed.class),
		BEAST_FEED,
		FORCE_BUFF,
		CLAN_GATE,
		GIVE_SP,
		COREDONE,
		ZAKENPLAYER,
		ZAKENSELF,
		
		// unimplemented
		NOTDONE,
		DUMMY,
		
		INSTANT_JUMP;
		
		private final Class<? extends L2Skill> _class;
		
		public L2Skill makeSkill(final StatsSet set)
		{
			try
			{
				final Constructor<? extends L2Skill> c = _class.getConstructor(StatsSet.class);
				
				return c.newInstance(set);
			}
			catch (final Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		private SkillType()
		{
			_class = L2SkillDefault.class;
		}
		
		private SkillType(final Class<? extends L2Skill> classType)
		{
			_class = classType;
		}
	}
	
	protected ChanceCondition _chanceCondition = null;
	private final String _flyType;
	private final int _flyRadius;
	private final float _flyCourse;
	
	// elements
	public final static int ELEMENT_WIND = 1;
	public final static int ELEMENT_FIRE = 2;
	public final static int ELEMENT_WATER = 3;
	public final static int ELEMENT_EARTH = 4;
	public final static int ELEMENT_HOLY = 5;
	public final static int ELEMENT_DARK = 6;
	
	// stat effected
	public final static int STAT_PATK = 301; // pAtk
	public final static int STAT_PDEF = 302; // pDef
	public final static int STAT_MATK = 303; // mAtk
	public final static int STAT_MDEF = 304; // mDef
	public final static int STAT_MAXHP = 305; // maxHp
	public final static int STAT_MAXMP = 306; // maxMp
	public final static int STAT_CURHP = 307;
	public final static int STAT_CURMP = 308;
	public final static int STAT_HPREGEN = 309; // regHp
	public final static int STAT_MPREGEN = 310; // regMp
	public final static int STAT_CASTINGSPEED = 311; // sCast
	public final static int STAT_ATKSPD = 312; // sAtk
	public final static int STAT_CRITDAM = 313; // critDmg
	public final static int STAT_CRITRATE = 314; // critRate
	public final static int STAT_FIRERES = 315; // fireRes
	public final static int STAT_WINDRES = 316; // windRes
	public final static int STAT_WATERRES = 317; // waterRes
	public final static int STAT_EARTHRES = 318; // earthRes
	public final static int STAT_HOLYRES = 336; // holyRes
	public final static int STAT_DARKRES = 337; // darkRes
	public final static int STAT_ROOTRES = 319; // rootRes
	public final static int STAT_SLEEPRES = 320; // sleepRes
	public final static int STAT_CONFUSIONRES = 321; // confusRes
	public final static int STAT_BREATH = 322; // breath
	public final static int STAT_AGGRESSION = 323; // aggr
	public final static int STAT_BLEED = 324; // bleed
	public final static int STAT_POISON = 325; // poison
	public final static int STAT_STUN = 326; // stun
	public final static int STAT_ROOT = 327; // root
	public final static int STAT_MOVEMENT = 328; // move
	public final static int STAT_EVASION = 329; // evas
	public final static int STAT_ACCURACY = 330; // accu
	public final static int STAT_COMBAT_STRENGTH = 331;
	public final static int STAT_COMBAT_WEAKNESS = 332;
	public final static int STAT_ATTACK_RANGE = 333; // rAtk
	public final static int STAT_NOAGG = 334; // noagg
	public final static int STAT_SHIELDDEF = 335; // sDef
	public final static int STAT_MP_CONSUME_RATE = 336; // Rate of mp consume per skill use
	public final static int STAT_HP_CONSUME_RATE = 337; // Rate of hp consume per skill use
	public final static int STAT_MCRITRATE = 338; // Magic Crit Rate
	
	// COMBAT DAMAGE MODIFIER SKILLS...DETECT WEAKNESS AND WEAKNESS/STRENGTH
	public final static int COMBAT_MOD_ANIMAL = 200;
	public final static int COMBAT_MOD_BEAST = 201;
	public final static int COMBAT_MOD_BUG = 202;
	public final static int COMBAT_MOD_DRAGON = 203;
	public final static int COMBAT_MOD_MONSTER = 204;
	public final static int COMBAT_MOD_PLANT = 205;
	public final static int COMBAT_MOD_HOLY = 206;
	public final static int COMBAT_MOD_UNHOLY = 207;
	public final static int COMBAT_MOD_BOW = 208;
	public final static int COMBAT_MOD_BLUNT = 209;
	public final static int COMBAT_MOD_DAGGER = 210;
	public final static int COMBAT_MOD_FIST = 211;
	public final static int COMBAT_MOD_DUAL = 212;
	public final static int COMBAT_MOD_SWORD = 213;
	public final static int COMBAT_MOD_POISON = 214;
	public final static int COMBAT_MOD_BLEED = 215;
	public final static int COMBAT_MOD_FIRE = 216;
	public final static int COMBAT_MOD_WATER = 217;
	public final static int COMBAT_MOD_EARTH = 218;
	public final static int COMBAT_MOD_WIND = 219;
	public final static int COMBAT_MOD_ROOT = 220;
	public final static int COMBAT_MOD_STUN = 221;
	public final static int COMBAT_MOD_CONFUSION = 222;
	public final static int COMBAT_MOD_DARK = 223;
	
	// conditional values
	public final static int COND_RUNNING = 0x0001;
	public final static int COND_WALKING = 0x0002;
	public final static int COND_SIT = 0x0004;
	public final static int COND_BEHIND = 0x0008;
	public final static int COND_CRIT = 0x0010;
	public final static int COND_LOWHP = 0x0020;
	public final static int COND_ROBES = 0x0040;
	public final static int COND_CHARGES = 0x0080;
	public final static int COND_SHIELD = 0x0100;
	public final static int COND_GRADEA = 0x010000;
	public final static int COND_GRADEB = 0x020000;
	public final static int COND_GRADEC = 0x040000;
	public final static int COND_GRADED = 0x080000;
	public final static int COND_GRADES = 0x100000;
	
	private static final Func[] _emptyFunctionSet = new Func[0];
	private static final L2Effect[] _emptyEffectSet = new L2Effect[0];
	
	// these two build the primary key
	private final int _id;
	private final int _level;
	
	/** Identifier for a skill that client can't display */
	private int _displayId;
	
	// not needed, just for easier debug
	private final String _name;
	private final SkillOpType _operateType;
	private final boolean _magic;
	private final boolean _staticReuse;
	private final boolean _staticHitTime;
	private final int _mpConsume;
	private final int _mpInitialConsume;
	private final int _hpConsume;
	private final int _itemConsume;
	private final int _itemConsumeId;
	// item consume count over time
	protected int _itemConsumeOT;
	// item consume id over time
	protected int _itemConsumeIdOT;
	// how many times to consume an item
	protected int _itemConsumeSteps;
	// for summon spells:
	// a) What is the total lifetime of summons (in millisecs)
	private final int _summonTotalLifeTime;
	// b) how much lifetime is lost per second of idleness (non-fighting)
	protected int _summonTimeLostIdle;
	// c) how much time is lost per second of activity (fighting)
	protected int _summonTimeLostActive;
	
	// item consume time in milliseconds
	protected int _itemConsumeTime;
	private final int _castRange;
	private final int _effectRange;
	
	// all times in milliseconds
	private final int _hitTime;
	// private final int _skillInterruptTime;
	private final int _coolTime;
	private final int _reuseDelay;
	private final int _buffDuration;
	private final int _reuseHashCode;
	
	/** Target type of the skill : SELF, PARTY, CLAN, PET... */
	private final SkillTargetType _targetType;
	
	private final double _power;
	private final int _magicLevel;
	private final String[] _negateSkillTypes;
	private final String[] _negateEffectTypes;
	private final float _negatePower;
	private final int _negateId;
	private final int _levelDepend;
	
	private final int _skillRadius;
	
	private final SkillType _skillType;
	private final SkillType _effectType;
	private final int _effectPower;
	private final int _effectId;
	private final int _effectLvl;
	
	private final boolean _ispotion;
	private final int _element;
	private final BaseStats _saveVs;
	
	private final boolean _isSuicideAttack;
	private final boolean _isDemonicSkill;
	
	private final Stats _stat;
	
	private final int _condition;
	private final int _conditionValue;
	private final boolean _overhit;
	private final int _weaponsAllowed;
	private final int _armorsAllowed;
	
	private final int _addCrossLearn; // -1 disable, otherwice SP price for others classes, default 1000
	private final float _mulCrossLearn; // multiplay for others classes, default 2
	private final float _mulCrossLearnRace; // multiplay for others races, default 2
	private final float _mulCrossLearnProf; // multiplay for fighter/mage missmatch, default 3
	private final List<ClassId> _canLearn; // which classes can learn
	private final List<Integer> _teachers; // which NPC teaches
	private final int _minPledgeClass;
	
	private final boolean _isOffensive;
	private final int _numCharges;
	private final int _triggeredId;
	private final int _triggeredLevel;
	
	private final boolean _ignoreResists;
	
	private final boolean _bestowed;
	
	private final boolean _isHeroSkill; // If true the skill is a Hero Skill
	
	private final int _baseCritRate; // percent of success for skill critical hit (especially for PDAM & BLOW - they're not affected by rCrit values or buffs). Default loads -1 for all other skills but 0 to PDAM & BLOW
	private final int _lethalEffect1; // percent of success for lethal 1st effect (hit cp to 1 or if mob hp to 50%) (only for PDAM skills)
	private final int _lethalEffect2; // percent of success for lethal 2nd effect (hit cp,hp to 1 or if mob hp to 1) (only for PDAM skills)
	private final int _lethalEffect3;
	private final boolean _directHpDmg; // If true then dmg is being make directly
	private final boolean _isDance; // If true then casting more dances will cost more MP
	private final int _nextDanceCost;
	private final float _sSBoost; // If true skill will have SoulShot boost (power*2)
	private final int _aggroPoints;
	
	private final float _pvpMulti;
	
	protected Condition _preCondition;
	protected Condition _itemPreCondition;
	
	protected FuncTemplate[] _funcTemplates;
	
	protected EffectTemplate[] _effectTemplates;
	protected EffectTemplate[] _effectTemplatesSelf;
	
	private final boolean _nextActionIsAttack;
	
	private final int _minChance;
	private final int _maxChance;
	
	private final boolean _singleEffect;
	
	private final boolean _isDebuff;
	private final boolean _isBuff;
	private final boolean _canBeReflected;
	private final boolean _advancedFlag;
	private final int _advancedMultiplier;
	private final boolean _simultaneousCast;
	
	protected L2Skill(StatsSet set)
	{
		_id = set.getInteger("skill_id", 0);
		_level = set.getInteger("level", 1);
		
		_advancedFlag = set.getBool("advancedFlag", false);
		_advancedMultiplier = set.getInteger("advancedMultiplier", 1);
		
		_displayId = set.getInteger("displayId", _id);
		_name = set.getString("name");
		_operateType = set.getEnum("operateType", SkillOpType.class);
		_magic = set.getBool("isMagic", false);
		_staticReuse = set.getBool("staticReuse", false);
		_staticHitTime = set.getBool("staticHitTime", false);
		_ispotion = set.getBool("isPotion", false);
		_mpConsume = set.getInteger("mpConsume", 0);
		_mpInitialConsume = set.getInteger("mpInitialConsume", 0);
		_hpConsume = set.getInteger("hpConsume", 0);
		_itemConsume = set.getInteger("itemConsumeCount", 0);
		_itemConsumeId = set.getInteger("itemConsumeId", 0);
		_itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
		_itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
		_itemConsumeTime = set.getInteger("itemConsumeTime", 0);
		_itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
		_summonTotalLifeTime = set.getInteger("summonTotalLifeTime", 1200000); // 20 minutes default
		_summonTimeLostIdle = set.getInteger("summonTimeLostIdle", 0);
		_summonTimeLostActive = set.getInteger("summonTimeLostActive", 0);
		
		_castRange = set.getInteger("castRange", 0);
		_effectRange = set.getInteger("effectRange", -1);
		
		_hitTime = set.getInteger("hitTime", 0);
		_coolTime = set.getInteger("coolTime", 0);
		_reuseDelay = set.getInteger("reuseDelay", 0);
		_buffDuration = set.getInteger("buffDuration", 0);
		
		_skillRadius = set.getInteger("skillRadius", 80);
		
		_targetType = set.getEnum("target", SkillTargetType.class);
		_power = set.getFloat("power", 0.f);
		_negateSkillTypes = set.getString("negateSkillTypes", "").split(" ");
		_negateEffectTypes = set.getString("negateEffectTypes", "").split(" ");
		_negatePower = set.getFloat("negatePower", 0.f);
		_negateId = set.getInteger("negateId", 0);
		_magicLevel = set.getInteger("magicLvl", SkillTreeTable.getInstance().getMinSkillLevel(_id, _level));
		_levelDepend = set.getInteger("lvlDepend", 0);
		_ignoreResists = set.getBool("ignoreResists", false);
		_stat = set.getEnum("stat", Stats.class, null);
		
		_skillType = set.getEnum("skillType", SkillType.class);
		_effectType = set.getEnum("effectType", SkillType.class, null);
		_effectPower = set.getInteger("effectPower", 0);
		_effectId = set.getInteger("effectId", 0);
		_effectLvl = set.getInteger("effectLevel", 0);
		
		_element = set.getInteger("element", 0);
		_saveVs = set.getEnum("saveVs", BaseStats.class, BaseStats.NONE);
		
		_condition = set.getInteger("condition", 0);
		_conditionValue = set.getInteger("conditionValue", 0);
		_overhit = set.getBool("overHit", false);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);
		_isDemonicSkill = set.getBool("isDemonicSkill", false);
		_weaponsAllowed = set.getInteger("weaponsAllowed", 0);
		_armorsAllowed = set.getInteger("armorsAllowed", 0);
		
		_addCrossLearn = set.getInteger("addCrossLearn", 1000);
		_mulCrossLearn = set.getFloat("mulCrossLearn", 2.f);
		_mulCrossLearnRace = set.getFloat("mulCrossLearnRace", 2.f);
		_mulCrossLearnProf = set.getFloat("mulCrossLearnProf", 3.f);
		_minPledgeClass = set.getInteger("minPledgeClass", 0);
		_isOffensive = set.getBool("offensive", isSkillTypeOffensive());
		_numCharges = set.getInteger("num_charges", 0);
		_triggeredId = set.getInteger("triggeredId", 0);
		_triggeredLevel = set.getInteger("triggeredLevel", 0);
		
		_bestowed = set.getBool("bestowed", false);
		
		_targetConsume = set.getInteger("targetConsumeCount", 0);
		_targetConsumeId = set.getInteger("targetConsumeId", 0);
		
		if (_operateType == SkillOpType.OP_CHANCE)
		{
			_chanceCondition = ChanceCondition.parse(set);
		}
		
		_isHeroSkill = HeroSkillTable.isHeroSkill(_id);
		
		_baseCritRate = set.getInteger("baseCritRate", _skillType == SkillType.PDAM || _skillType == SkillType.BLOW ? 0 : -1);
		_lethalEffect1 = set.getInteger("lethal1", 0);
		_lethalEffect2 = set.getInteger("lethal2", 0);
		_lethalEffect3 = set.getInteger("lethal3", 0);
		_directHpDmg = set.getBool("dmgDirectlyToHp", false);
		_isDance = set.getBool("isDance", false);
		_nextDanceCost = set.getInteger("nextDanceCost", 0);
		_sSBoost = set.getFloat("SSBoost", 0.f);
		_aggroPoints = set.getInteger("aggroPoints", 0);
		
		_pvpMulti = set.getFloat("pvpMulti", 1.f);
		
		_nextActionIsAttack = set.getBool("nextActionAttack", false);
		
		_minChance = set.getInteger("minChance", 1);
		_maxChance = set.getInteger("maxChance", 100);
		
		String canLearn = set.getString("canLearn", null);
		if (canLearn == null)
		{
			_canLearn = null;
		}
		else
		{
			_canLearn = new FastList<>();
			StringTokenizer st = new StringTokenizer(canLearn, " \r\n\t,;");
			
			while (st.hasMoreTokens())
			{
				String cls = st.nextToken();
				try
				{
					_canLearn.add(ClassId.valueOf(cls));
				}
				catch (final Throwable t)
				{
					LOG.error("Bad class " + cls + " to learn skill", t);
				}
			}
		}
		
		String teachers = set.getString("teachers", null);
		if (teachers == null)
		{
			_teachers = null;
		}
		else
		{
			_teachers = new FastList<>();
			StringTokenizer st = new StringTokenizer(teachers, " \r\n\t,;");
			while (st.hasMoreTokens())
			{
				String npcid = st.nextToken();
				try
				{
					_teachers.add(Integer.parseInt(npcid));
				}
				catch (final Throwable t)
				{
					LOG.error("Bad teacher id " + npcid + " to teach skill", t);
				}
			}
		}
		
		_singleEffect = set.getBool("singleEffect", false);
		
		_isDebuff = set.getBool("isDebuff", false);
		_isBuff = set.getBool("isBuff", false);
		_canBeReflected = set.getBool("canBeReflected", true);
		_flyType = set.getString("flyType", null);
		_flyRadius = set.getInteger("flyRadius", 0);
		_flyCourse = set.getFloat("flyCourse", 0);
		_reuseHashCode = SkillTable.getSkillHashCode(_id, _level);
		_simultaneousCast = set.getBool("simultaneousCast", false);
	}
	
	public abstract void useSkill(L2Character caster, L2Object[] targets);
	
	public boolean is_singleEffect()
	{
		return _singleEffect;
	}
	
	public boolean is_Debuff()
	{
		boolean type_debuff = false;
		
		switch (_skillType)
		{
			case AGGDEBUFF:
			case DEBUFF:
			case STUN:
			case BLEED:
			case CONFUSION:
			case FEAR:
			case PARALYZE:
			case SLEEP:
			case ROOT:
			case SLOW:
			case POISON:
			case MUTE:
			case WEAKNESS:
			case GLOOM:
			case SURRENDER:
			case HEX:
				type_debuff = true;
			default:
				break;
			
		}
		
		return _isDebuff || type_debuff;
	}
	
	public boolean is_Buff()
	{
		boolean type_buff = false;
		
		switch (_skillType)
		{
			case BUFF:
				type_buff = true;
			default:
				break;
			
		}
		
		return _isBuff || type_buff;
	}
	
	public final boolean nextActionIsAttack()
	{
		return _nextActionIsAttack;
	}
	
	public final boolean isPotion()
	{
		return _ispotion;
	}
	
	public final int getArmorsAllowed()
	{
		return _armorsAllowed;
	}
	
	public final int getConditionValue()
	{
		return _conditionValue;
	}
	
	public boolean canBeReflected()
	{
		return _canBeReflected;
	}
	
	public final SkillType getSkillType()
	{
		return _skillType;
	}
	
	public final String getFlyType()
	{
		return _flyType;
	}
	
	public final int getFlyRadius()
	{
		return _flyRadius;
	}
	
	public final float getFlyCourse()
	{
		return _flyCourse;
	}
	
	public final boolean hasEffectWhileCasting()
	{
		return getSkillType() == SkillType.SIGNET_CASTTIME;
	}
	
	public final BaseStats getSavevs()
	{
		return _saveVs;
	}
	
	public final int getElement()
	{
		return _element;
	}
	
	public final SkillTargetType getTargetType()
	{
		return _targetType;
	}
	
	public final int getCondition()
	{
		return _condition;
	}
	
	public final boolean isOverhit()
	{
		return _overhit;
	}
	
	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}
	
	public final boolean isDemonicSkill()
	{
		return _isDemonicSkill;
	}
	
	public final double getPower(final L2Character activeChar)
	{
		if (_skillType == SkillType.FATALCOUNTER && activeChar != null)
		{
			return _power * 3.5 * (1 - activeChar.getCurrentHp() / activeChar.getMaxHp());
		}
		return _power;
	}
	
	public final double getPower()
	{
		return _power;
	}
	
	public final String[] getNegateSkillTypes()
	{
		return _negateSkillTypes;
	}
	
	public final String[] getNegateEffectTypes()
	{
		return _negateEffectTypes;
	}
	
	public final float getNegatePower()
	{
		return _negatePower;
	}
	
	public final int getNegateId()
	{
		return _negateId;
	}
	
	public final int getMagicLevel()
	{
		return _magicLevel;
	}
	
	public final boolean isStaticReuse()
	{
		return _staticReuse;
	}
	
	public final boolean isStaticHitTime()
	{
		return _staticHitTime;
	}
	
	public final int getLevelDepend()
	{
		return _levelDepend;
	}
	
	public final int getEffectPower()
	{
		return _effectPower;
	}
	
	public final int getEffectId()
	{
		return _effectId;
	}
	
	public final int getEffectLvl()
	{
		return _effectLvl;
	}
	
	public final SkillType getEffectType()
	{
		return _effectType;
	}
	
	public final int getBuffDuration()
	{
		return _buffDuration;
	}
	
	public final int getCastRange()
	{
		return _castRange;
	}
	
	public final int getEffectRange()
	{
		return _effectRange;
	}
	
	public final int getHpConsume()
	{
		return _hpConsume;
	}
	
	public final int getId()
	{
		return _id;
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public void setDisplayId(final int id)
	{
		_displayId = id;
	}
	
	public final boolean ignoreResists()
	{
		return _ignoreResists;
	}
	
	public int getTriggeredId()
	{
		return _triggeredId;
	}
	
	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}
	
	public final Stats getStat()
	{
		return _stat;
	}
	
	public final int getItemConsume()
	{
		return _itemConsume;
	}
	
	public final int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	public final int getItemConsumeOT()
	{
		return _itemConsumeOT;
	}
	
	public final int getItemConsumeIdOT()
	{
		return _itemConsumeIdOT;
	}
	
	public final int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}
	
	public final int getTotalLifeTime()
	{
		return _summonTotalLifeTime;
	}
	
	public final int getTimeLostIdle()
	{
		return _summonTimeLostIdle;
	}
	
	public final int getTimeLostActive()
	{
		return _summonTimeLostActive;
	}
	
	public final int getItemConsumeTime()
	{
		return _itemConsumeTime;
	}
	
	public final int getLevel()
	{
		return _level;
	}
	
	public final boolean isMagic()
	{
		return _magic;
	}
	
	public final int getMpConsume()
	{
		return _mpConsume;
	}
	
	public final int getMpInitialConsume()
	{
		return _mpInitialConsume;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	public final int getHitTime()
	{
		return _hitTime;
	}
	
	public final int getCoolTime()
	{
		return _coolTime;
	}
	
	public final int getSkillRadius()
	{
		return _skillRadius;
	}
	
	public final boolean isActive()
	{
		return _operateType == SkillOpType.OP_ACTIVE;
	}
	
	public final boolean isPassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE;
	}
	
	public final boolean isToggle()
	{
		return _operateType == SkillOpType.OP_TOGGLE;
	}
	
	public final boolean isChance()
	{
		return _operateType == SkillOpType.OP_CHANCE;
	}
	
	public ChanceCondition getChanceCondition()
	{
		return _chanceCondition;
	}
	
	public final boolean isDance()
	{
		return _isDance;
	}
	
	public final int getNextDanceMpCost()
	{
		return _nextDanceCost;
	}
	
	public final float getSSBoost()
	{
		return _sSBoost;
	}
	
	public final int getAggroPoints()
	{
		return _aggroPoints;
	}
	
	public final float getPvpMulti()
	{
		return _pvpMulti;
	}
	
	public final boolean useSoulShot()
	{
		return getSkillType() == SkillType.PDAM || getSkillType() == SkillType.STUN || getSkillType() == SkillType.CHARGEDAM || getSkillType() == SkillType.BLOW;
	}
	
	public final boolean useSpiritShot()
	{
		return isMagic();
	}
	
	public final boolean useFishShot()
	{
		return getSkillType() == SkillType.PUMPING || getSkillType() == SkillType.REELING;
	}
	
	public final int getWeaponsAllowed()
	{
		return _weaponsAllowed;
	}
	
	public final int getCrossLearnAdd()
	{
		return _addCrossLearn;
	}
	
	public final float getCrossLearnMul()
	{
		return _mulCrossLearn;
	}
	
	public final float getCrossLearnRace()
	{
		return _mulCrossLearnRace;
	}
	
	public final float getCrossLearnProf()
	{
		return _mulCrossLearnProf;
	}
	
	public final boolean getCanLearn(final ClassId cls)
	{
		return _canLearn == null || _canLearn.contains(cls);
	}
	
	public final boolean canTeachBy(final int npcId)
	{
		return _teachers == null || _teachers.contains(npcId);
	}
	
	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}
	
	public final boolean isPvpSkill()
	{
		switch (_skillType)
		{
			case DOT:
			case AGGREDUCE:
			case AGGDAMAGE:
			case AGGREDUCE_CHAR:
			case CONFUSE_MOB_ONLY:
			case BLEED:
			case CONFUSION:
			case POISON:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case SLOW:
			case FEAR:
			case SLEEP:
			case MDOT:
			case MANADAM:
			case MUTE:
			case WEAKNESS:
			case GLOOM:
			case SURRENDER:
			case HEX:
			case DOD:
			case PARALYZE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case FATALCOUNTER:
			case BETRAY:
				return true;
			default:
				return false;
		}
	}
	
	public final boolean isOffensive()
	{
		return _isOffensive;
	}
	
	public final boolean isHeroSkill()
	{
		return _isHeroSkill;
	}
	
	public final int getNumCharges()
	{
		return _numCharges;
	}
	
	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}
	
	public final int getLethalChance1()
	{
		return _lethalEffect1;
	}
	
	public final int getLethalChance2()
	{
		return _lethalEffect2;
	}
	
	public final int getLethalChance3()
	{
		return _lethalEffect3;
	}
	
	public final boolean getDmgDirectlyToHP()
	{
		return _directHpDmg;
	}
	
	public boolean bestowed()
	{
		return _bestowed;
	}
	
	public boolean triggerAnotherSkill()
	{
		return _triggeredId > 1;
	}
	
	public final boolean isSkillTypeOffensive()
	{
		switch (_skillType)
		{
			case PDAM:
			case MDAM:
			case CPDAM:
			case DOT:
			case BLEED:
			case POISON:
			case AGGDAMAGE:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case SLOW:
			case CONFUSION:
			case ERASE:
			case BLOW:
			case FEAR:
			case DRAIN:
			case SLEEP:
			case CHARGEDAM:
			case CONFUSE_MOB_ONLY:
			case DEATHLINK:
			case DETECT_WEAKNESS:
			case MANADAM:
			case MDOT:
			case MUTE:
			case SOULSHOT:
			case SPIRITSHOT:
			case SPOIL:
			case WEAKNESS:
			case GLOOM:
			case SURRENDER:
			case HEX:
			case DOD:
			case MANA_BY_LEVEL:
			case SWEEP:
			case PARALYZE:
			case DRAIN_SOUL:
			case AGGREDUCE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case AGGREMOVE:
			case AGGREDUCE_CHAR:
			case FATALCOUNTER:
			case BETRAY:
			case DELUXE_KEY_UNLOCK:
			case SOW:
			case HARVEST:
			case INSTANT_JUMP:
				return true;
			default:
				return false;
		}
	}
	
	public final boolean getWeaponDependancy(final L2Character activeChar)
	{
		if (getWeaponDependancy(activeChar, false))
		{
			return true;
		}
		
		final SystemMessage message = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
		message.addSkillName(this.getId());
		activeChar.sendPacket(message);
		
		return false;
	}
	
	public final boolean getWeaponDependancy(final L2Character activeChar, final boolean chance)
	{
		final int weaponsAllowed = getWeaponsAllowed();
		
		if (weaponsAllowed == 0)
		{
			return true;
		}
		
		int mask = 0;
		if (activeChar.getActiveWeaponItem() != null)
		{
			mask |= activeChar.getActiveWeaponItem().getItemType().mask();
		}
		if (activeChar.getSecondaryWeaponItem() != null)
		{
			mask |= activeChar.getSecondaryWeaponItem().getItemType().mask();
		}
		
		if ((mask & weaponsAllowed) != 0)
		{
			return true;
		}
		
		return false;
	}
	
	public boolean checkCondition(final L2Character activeChar, final L2Object target, final boolean itemOrWeapon)
	{
		Condition preCondition = _preCondition;
		
		if (itemOrWeapon)
		{
			preCondition = _itemPreCondition;
		}
		
		if (preCondition == null)
		{
			return true;
		}
		
		Env env = new Env();
		env.player = activeChar;
		if (target instanceof L2Character)
		{
			env.target = (L2Character) target;
		}
		
		env.skill = this;
		if (!preCondition.test(env))
		{
			String msg = preCondition.getMessage();
			if (msg != null)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString(msg);
				activeChar.sendPacket(sm);
			}
			
			return false;
		}
		
		return true;
	}
	
	public final L2Object[] getTargetList(L2Character activeChar)
	{
		return getTargetList(activeChar, false);
	}
	
	public final L2Object getFirstOfTargetList(L2Character activeChar)
	{
		L2Object[] targets;
		
		targets = getTargetList(activeChar, true);
		
		if (targets == null || targets.length == 0)
		{
			return null;
		}
		
		return targets[0];
	}
	
	public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst)
	{
		L2Character target = null;
		
		final L2Object objTarget = activeChar.getTarget();
		
		if (objTarget instanceof L2Character)
		{
			target = (L2Character) objTarget;
		}
		
		return getTargetList(activeChar, onlyFirst, target);
	}
	
	public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target)
	{
		if (isPotion())
		{
			return new L2Character[]
			{
				activeChar
			};
		}
		
		final SkillTargetType targetType = getTargetType();
		final SkillType skillType = getSkillType();
		
		switch (targetType)
		{
			case TARGET_ONE:
			{
				boolean canTargetSelf = false;
				switch (skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
					case REFLECT:
					case UNBLEED:
					case UNPOISON:
					case SEED:
					case COMBATPOINTHEAL:
					case COMBATPOINTPERCENTHEAL:
					case MAGE_BANE:
					case WARRIOR_BANE:
					case BETRAY:
					case BALANCE_LIFE:
					case FORCE_BUFF:
						canTargetSelf = true;
						break;
					default:
						break;
				}
				
				switch (skillType)
				{
					case CONFUSION:
					case DEBUFF:
					case STUN:
					case ROOT:
					case SLOW:
					case FEAR:
					case SLEEP:
					case MUTE:
					case WEAKNESS:
					case GLOOM:
					case SURRENDER:
					case HEX:
					case DOD:
					case PARALYZE:
					case CANCEL:
					case MAGE_BANE:
					case WARRIOR_BANE:
						if (checkPartyClan(activeChar, target))
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
							return _emptyTargetList;
						}
						break;
					default:
						break;
				}
				
				switch (skillType)
				{
					case AGGDEBUFF:
					case DEBUFF:
					case BLEED:
					case CONFUSION:
					case FEAR:
					case PARALYZE:
					case SLEEP:
					case ROOT:
					case SLOW:
					case STUN:
					case WEAKNESS:
					case GLOOM:
					case SURRENDER:
					case HEX:
					case DOD:
					case MUTE:
					case CANCEL:
					case DOT:
					case POISON:
					case AGGREDUCE_CHAR:
					case AGGDAMAGE:
					case AGGREMOVE:
					case MANADAM:
						if (target instanceof L2NpcInstance && !(target instanceof L2MonsterInstance))
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
							return _emptyTargetList;
						}
						break;
					default:
						break;
				}
				
				// Check for null target or any other invalid target
				if (target == null || target.isDead() || target == activeChar && !canTargetSelf)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}
				
				// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
				return new L2Character[]
				{
					target
				};
			}
			case TARGET_SELF:
			case TARGET_GROUND:
			{
				return new L2Character[]
				{
					activeChar
				};
			}
			case TARGET_HOLY:
			{
				if (activeChar instanceof L2PcInstance)
				{
					if (activeChar.getTarget() instanceof L2ArtefactInstance)
					{
						return new L2Character[]
						{
							(L2ArtefactInstance) activeChar.getTarget()
						};
					}
				}
				
				return _emptyTargetList;
			}
			
			case TARGET_PET:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead())
				{
					return new L2Character[]
					{
						target
					};
				}
				
				return _emptyTargetList;
			}
			case TARGET_OWNER_PET:
			{
				if (activeChar instanceof L2Summon)
				{
					target = ((L2Summon) activeChar).getOwner();
					if (target != null && !target.isDead())
					{
						return new L2Character[]
						{
							target
						};
					}
				}
				
				return _emptyTargetList;
			}
			case TARGET_CORPSE_PET:
			{
				if (activeChar instanceof L2PcInstance)
				{
					target = activeChar.getPet();
					if ((target != null) && target.isDead())
					{
						return new L2Character[]
						{
							target
						};
					}
				}
				
				return _emptyTargetList;
			}
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			{
				List<L2Character> targetList = new ArrayList<>();
				
				if (_skillType == SkillType.DUMMY)
				{
					if (onlyFirst)
					{
						return new L2Character[]
						{
							activeChar
						};
					}
					
					final L2PcInstance sourcePlayer = activeChar.getActingPlayer();
					
					targetList.add(activeChar);
					final Collection<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(getSkillRadius());
					for (L2Character obj : objs)
					{
						// Like L2OFF you can cast the skill on peace zone but hasn't any effect
						if (isOffensive() && L2Character.isInsidePeaceZone(target, activeChar))
						{
							continue;
						}
						
						if (!((obj == activeChar) || (obj == sourcePlayer) || (obj instanceof L2NpcInstance) || (obj instanceof L2Attackable)))
						{
							continue;
						}
						
						targetList.add(obj);
					}
				}
				else
				{
					final boolean srcInArena = (activeChar.isInsideZone(ZoneId.ZONE_PVP) && !activeChar.isInsideZone(ZoneId.ZONE_SIEGE));
					
					for (L2Character obj : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, _skillRadius))
					{
						// Like L2OFF you can cast the skill on peace zone but hasn't any effect
						if (isOffensive() && !(obj instanceof L2Attackable) && L2Character.isInsidePeaceZone(target, activeChar))
						{
							continue;
						}
						
						if ((obj instanceof L2Attackable) || (obj instanceof L2PlayableInstance))
						{
							switch (_targetType)
							{
								case TARGET_FRONT_AURA:
									if (!obj.isFront(activeChar))
									{
										continue;
									}
									break;
								case TARGET_BEHIND_AURA:
									if (!obj.isBehind(activeChar))
									{
										continue;
									}
									break;
							}
							
							if (!checkForAreaOffensiveSkills(activeChar, obj, this, srcInArena))
							{
								continue;
							}
							
							if (activeChar instanceof L2PcInstance)
							{
								L2PcInstance activeCh = (L2PcInstance) activeChar;
								if (activeCh.isInOlympiadMode() && !activeCh.isOlympiadStart())
								{
									return targetList.toArray(new L2Character[targetList.size()]);
								}
							}
							
							if (onlyFirst)
							{
								return new L2Character[]
								{
									obj
								};
							}
							
							targetList.add(obj);
						}
					}
				}
				
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_AREA:
			case TARGET_FRONT_AREA:
			case TARGET_BEHIND_AREA:
			{
				List<L2Character> targetList = new ArrayList<>();
				
				if ((((target == null) || (target == activeChar) || target.isAlikeDead()) && (_castRange >= 0)) || (!((target instanceof L2Attackable) || (target instanceof L2PlayableInstance))))
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}
				
				final L2Character origin;
				final boolean srcInArena = (activeChar.isInsideZone(ZoneId.ZONE_PVP) && !activeChar.isInsideZone(ZoneId.ZONE_SIEGE));
				
				if (_castRange >= 0)
				{
					if (!checkForAreaOffensiveSkills(activeChar, target, this, srcInArena))
					{
						return _emptyTargetList;
					}
					
					if (onlyFirst)
					{
						return new L2Character[]
						{
							target
						};
					}
					
					origin = target;
					targetList.add(origin); // Add target to target list
				}
				else
				{
					origin = activeChar;
				}
				
				for (final L2Object obj : activeChar.getKnownList().getKnownObjects().values())
				{
					// Like L2OFF you can cast the skill on peace zone but hasn't any effect
					if (isOffensive() && L2Character.isInsidePeaceZone(target, activeChar))
					{
						continue;
					}
					
					if (!((obj instanceof L2Attackable) || (obj instanceof L2PlayableInstance)))
					{
						continue;
					}
					
					if (obj == origin)
					{
						continue;
					}
					
					if (Util.checkIfInRange(_skillRadius, origin, obj, true))
					{
						switch (_targetType)
						{
							case TARGET_FRONT_AREA:
								if (!((L2Character) obj).isFront(activeChar))
								{
									continue;
								}
								break;
							case TARGET_BEHIND_AREA:
								if (!((L2Character) obj).isBehind(activeChar))
								{
									continue;
								}
								break;
						}
						
						if (!checkForAreaOffensiveSkills(activeChar, (L2Character) obj, this, srcInArena))
						{
							continue;
						}
						
						targetList.add((L2Character) obj);
					}
				}
				
				if (targetList.isEmpty())
				{
					return _emptyTargetList;
				}
				
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_MULTIFACE:
			{
				List<L2Character> targetList = new ArrayList<>();
				if (!(target instanceof L2Attackable) && !(target instanceof L2PcInstance))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
				
				if (!onlyFirst)
				{
					targetList.add(target);
				}
				else
				{
					return new L2Character[]
					{
						target
					};
				}
				
				final int radius = getSkillRadius();
				
				L2PcInstance src = null;
				if (activeChar instanceof L2PcInstance)
				{
					src = (L2PcInstance) activeChar;
				}
				else if (activeChar instanceof L2Summon)
				{
					src = ((L2Summon) activeChar).getOwner();
				}
				
				for (final L2Object obj : activeChar.getKnownList().getKnownObjects().values())
				{
					if (obj == null)
					{
						continue;
					}
					
					if (!Util.checkIfInRange(radius, activeChar, obj, true))
					{
						continue;
					}
					
					// check for Events
					if (src != null)
					{
						if (obj instanceof L2PcInstance)
						{
							
							final L2PcInstance trg = (L2PcInstance) obj;
							if (trg == src)
							{
								continue;
							}
							
							if (((src._inEvent || src._inEventCTF || src._inEventDM || src._inEventTvT || src._inEventVIP) && (!trg._inEvent && !trg._inEventCTF && !trg._inEventDM && !trg._inEventTvT && !trg._inEventVIP))
								|| ((trg._inEvent || trg._inEventCTF || trg._inEventDM || trg._inEventTvT || trg._inEventVIP) && (!src._inEvent && !src._inEventCTF && !src._inEventDM && !src._inEventTvT && !src._inEventVIP)))
							{
								continue;
							}
							
						}
						else if (obj instanceof L2Summon)
						{
							
							final L2PcInstance trg = ((L2Summon) obj).getOwner();
							if (trg == src)
							{
								continue;
							}
							
							if (((src._inEvent || src._inEventCTF || src._inEventDM || src._inEventTvT || src._inEventVIP) && (!trg._inEvent && !trg._inEventCTF && !trg._inEventDM && !trg._inEventTvT && !trg._inEventVIP))
								|| ((trg._inEvent || trg._inEventCTF || trg._inEventDM || trg._inEventTvT || trg._inEventVIP) && (!src._inEvent && !src._inEventCTF && !src._inEventDM && !src._inEventTvT && !src._inEventVIP)))
							{
								continue;
							}
							
						}
					}
					
					if (obj instanceof L2Attackable && obj != target)
					{
						targetList.add((L2Character) obj);
					}
					
					if (targetList.size() == 0)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
						return null;
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY:
			{
				List<L2Character> targetList = new ArrayList<>();
				
				if (onlyFirst)
				{
					return new L2Character[]
					{
						activeChar
					};
				}
				
				targetList.add(activeChar);
				
				L2PcInstance player = activeChar.getActingPlayer();
				if (player == null)
				{
					return new L2Character[]
					{
						activeChar
					};
				}
				
				if (activeChar instanceof L2Summon)
				{
					targetList.add(player);
				}
				else if (activeChar instanceof L2PcInstance)
				{
					if (player.getPet() != null)
					{
						targetList.add(player.getPet());
					}
				}
				
				if (activeChar.getParty() != null)
				{
					List<L2PcInstance> partyList = activeChar.getParty().getPartyMembers();
					
					for (final L2PcInstance partyMember : partyList)
					{
						if (partyMember == null)
						{
							continue;
						}
						
						if (partyMember == player)
						{
							continue;
						}
						
						if (((TvT.is_started() && !Config.TVT_ALLOW_INTERFERENCE) || (CTF.is_started() && !Config.CTF_ALLOW_INTERFERENCE) || (DM.is_started() && !Config.DM_ALLOW_INTERFERENCE)))
						{
							if ((partyMember._inEventTvT && !player._inEventTvT) || (!partyMember._inEventTvT && player._inEventTvT))
							{
								continue;
							}
							if ((partyMember._inEventCTF && !player._inEventCTF) || (!partyMember._inEventCTF && player._inEventCTF))
							{
								continue;
							}
							if ((partyMember._inEventDM && !player._inEventDM) || (!partyMember._inEventDM && player._inEventDM))
							{
								continue;
							}
						}
						
						if (!partyMember.isDead() && Util.checkIfInRange(getSkillRadius(), activeChar, partyMember, true))
						{
							L2PcInstance src = null;
							if (activeChar instanceof L2PcInstance)
							{
								src = (L2PcInstance) activeChar;
							}
							else if (activeChar instanceof L2Summon)
							{
								src = ((L2Summon) activeChar).getOwner();
							}
							
							final L2PcInstance trg = partyMember;
							
							if (src != null)
							{
								if (((src._inEvent || src._inEventCTF || src._inEventDM || src._inEventTvT || src._inEventVIP) && (!trg._inEvent && !trg._inEventCTF && !trg._inEventDM && !trg._inEventTvT && !trg._inEventVIP))
									|| ((trg._inEvent || trg._inEventCTF || trg._inEventDM || trg._inEventTvT || trg._inEventVIP) && (!src._inEvent && !src._inEventCTF && !src._inEventDM && !src._inEventTvT && !src._inEventVIP)))
								{
									continue;
								}
							}
							
							targetList.add(partyMember);
							
							if (partyMember.getPet() != null && !partyMember.getPet().isDead())
							{
								targetList.add(partyMember.getPet());
							}
						}
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY_MEMBER:
			{
				if (target != null && !target.isDead()
					&& (target == activeChar || (activeChar.getParty() != null && target.getParty() != null && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID()) || (activeChar.getPet() == target) || (activeChar == target.getPet())))
				{
					// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
					return new L2Character[]
					{
						target
					};
					
				}
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return null;
			}
			case TARGET_PARTY_OTHER:
			{
				if (target != activeChar && target != null && !target.isDead() && activeChar.getParty() != null && target.getParty() != null && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
				{
					// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
					return new L2Character[]
					{
						target
					};
				}
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return null;
			}
			case TARGET_CORPSE_ALLY:
			case TARGET_ALLY:
			{
				List<L2Character> targetList = new ArrayList<>();
				if (activeChar instanceof L2PcInstance)
				{
					final int radius = getSkillRadius();
					L2PcInstance player = (L2PcInstance) activeChar;
					L2Clan clan = player.getClan();
					
					if (targetType != SkillTargetType.TARGET_CORPSE_ALLY) // if corpse, the caster is not included
					{
						if (player.isInOlympiadMode())
						{
							return new L2Character[]
							{
								player
							};
						}
						
						if (!onlyFirst)
						{
							targetList.add(player);
						}
						else
						{
							return new L2Character[]
							{
								player
							};
						}
					}
					
					L2PcInstance src = null;
					if (activeChar instanceof L2PcInstance)
					{
						src = (L2PcInstance) activeChar;
					}
					else if (activeChar instanceof L2Summon)
					{
						src = ((L2Summon) activeChar).getOwner();
					}
					
					if (clan != null)
					{
						for (final L2Object newTarget : activeChar.getKnownList().getKnownObjects().values())
						{
							if (newTarget == null || !(newTarget instanceof L2PcInstance))
							{
								continue;
							}
							
							final L2PcInstance playerTarget = (L2PcInstance) newTarget;
							
							if (playerTarget.isDead() && targetType != SkillTargetType.TARGET_CORPSE_ALLY)
							{
								continue;
							}
							
							// if ally is different --> clan is different too, so --> continue
							if (player.getAllyId() != 0)
							{
								
								if (playerTarget.getAllyId() != player.getAllyId())
								{
									continue;
								}
								
							}
							else
							{ // check if clan is not the same --> continue
								
								if (player.getClanId() != playerTarget.getClanId())
								{
									continue;
								}
								
							}
							
							// check for Events
							if (src != null)
							{
								if (playerTarget == src)
								{
									continue;
								}
								
								if (((src._inEvent || src._inEventCTF || src._inEventDM || src._inEventTvT || src._inEventVIP) && (!playerTarget._inEvent && !playerTarget._inEventCTF && !playerTarget._inEventDM && !playerTarget._inEventTvT && !playerTarget._inEventVIP))
									|| ((playerTarget._inEvent || playerTarget._inEventCTF || playerTarget._inEventDM || playerTarget._inEventTvT || playerTarget._inEventVIP) && (!src._inEvent && !src._inEventCTF && !src._inEventDM && !src._inEventTvT && !src._inEventVIP)))
								{
									continue;
								}
							}
							
							L2Summon pet = ((L2PcInstance) newTarget).getPet();
							if (pet != null && Util.checkIfInRange(radius, activeChar, pet, true) && !onlyFirst && (targetType == SkillTargetType.TARGET_CORPSE_ALLY && pet.isDead() || targetType == SkillTargetType.TARGET_ALLY && !pet.isDead()) && player.checkPvpSkill(newTarget, this))
							{
								targetList.add(pet);
							}
							
							if (targetType == SkillTargetType.TARGET_CORPSE_ALLY)
							{
								if (!((L2PcInstance) newTarget).isDead())
								{
									continue;
								}
								
								if (getSkillType() == SkillType.RESURRECT && ((L2PcInstance) newTarget).isInsideZone(ZoneId.ZONE_SIEGE))
								{
									continue;
								}
							}
							
							if (!Util.checkIfInRange(radius, activeChar, newTarget, true))
							{
								continue;
							}
							
							if (!GeoData.getInstance().canSeeTarget(activeChar, newTarget))
							{
								continue;
							}
							
							// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
							if (!player.checkPvpSkill(newTarget, this))
							{
								continue;
							}
							
							if (!onlyFirst)
							{
								targetList.add((L2Character) newTarget);
							}
							else
							{
								return new L2Character[]
								{
									(L2Character) newTarget
								};
							}
							
							player.setTarget(newTarget);
						}
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_CLAN:
			case TARGET_CLAN:
			{
				List<L2Character> targetList = new ArrayList<>();
				if (activeChar instanceof L2PcInstance)
				{
					final int radius = getSkillRadius();
					L2PcInstance player = (L2PcInstance) activeChar;
					L2Clan clan = player.getClan();
					
					if (targetType != SkillTargetType.TARGET_CORPSE_CLAN)
					{
						if (player.isInOlympiadMode())
						{
							return new L2Character[]
							{
								player
							};
						}
						
						if (!onlyFirst)
						{
							targetList.add(player);
						}
						else
						{
							return new L2Character[]
							{
								player
							};
						}
					}
					
					if (clan != null)
					{
						// Get all visible objects in a spheric area near the L2Character
						// Get Clan Members
						for (final L2ClanMember member : clan.getMembers())
						{
							L2PcInstance newTarget = member.getPlayerInstance();
							
							if (newTarget == null || newTarget == player)
							{
								continue;
							}
							
							if (player.isInDuel() && (player.getDuelId() != newTarget.getDuelId() || player.getParty() == null && player.getParty() != newTarget.getParty()))
							{
								continue;
							}
							
							final L2PcInstance trg = newTarget;
							final L2PcInstance src = player;
							
							// if src is in event and trg not OR viceversa:
							// to be fixed for mixed events status (in TvT joining phase, someone can attack a partecipating CTF player with area attack)
							if (((src._inEvent || src._inEventCTF || src._inEventDM || src._inEventTvT || src._inEventVIP) && (!trg._inEvent && !trg._inEventCTF && !trg._inEventDM && !trg._inEventTvT && !trg._inEventVIP))
								|| ((trg._inEvent || trg._inEventCTF || trg._inEventDM || trg._inEventTvT || trg._inEventVIP) && (!src._inEvent && !src._inEventCTF && !src._inEventDM && !src._inEventTvT && !src._inEventVIP)))
							{
								continue;
							}
							
							L2Summon pet = newTarget.getPet();
							if (pet != null && Util.checkIfInRange(radius, activeChar, pet, true) && !onlyFirst && (targetType == SkillTargetType.TARGET_CORPSE_CLAN && pet.isDead() || targetType == SkillTargetType.TARGET_CLAN && !pet.isDead()) && player.checkPvpSkill(newTarget, this))
							{
								targetList.add(pet);
							}
							
							if (targetType == SkillTargetType.TARGET_CORPSE_CLAN)
							{
								if (!newTarget.isDead())
								{
									continue;
								}
								
								if (getSkillType() == SkillType.RESURRECT)
								{
									// check target is not in a active siege zone
									Siege siege = SiegeManager.getInstance().getSiege(newTarget);
									if (siege != null && siege.getIsInProgress())
									{
										continue;
									}
								}
							}
							
							if (!Util.checkIfInRange(radius, activeChar, newTarget, true))
							{
								continue;
							}
							
							// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
							if (!player.checkPvpSkill(newTarget, this))
							{
								continue;
							}
							
							if (!onlyFirst)
							{
								targetList.add(newTarget);
							}
							else
							{
								return new L2Character[]
								{
									newTarget
								};
							}
						}
					}
				}
				else if (activeChar instanceof L2NpcInstance)
				{
					// for buff purposes, returns friendly mobs nearby and mob itself
					final L2NpcInstance npc = (L2NpcInstance) activeChar;
					if (npc.getFactionId() == null || npc.getFactionId().isEmpty())
					{
						return new L2Character[]
						{
							activeChar
						};
					}
					targetList.add(activeChar);
					final Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
					for (final L2Object newTarget : objs)
					{
						if (newTarget instanceof L2NpcInstance && npc.getFactionId().equals(((L2NpcInstance) newTarget).getFactionId()))
						{
							if (!Util.checkIfInRange(getCastRange(), activeChar, newTarget, true))
							{
								continue;
							}
							targetList.add((L2NpcInstance) newTarget);
						}
					}
				}
				
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_PLAYER:
			{
				List<L2Character> targetList = new ArrayList<>();
				if (target != null && target.isDead())
				{
					L2PcInstance player = null;
					
					if (activeChar instanceof L2PcInstance)
					{
						player = (L2PcInstance) activeChar;
					}
					
					L2PcInstance targetPlayer = null;
					
					if (target instanceof L2PcInstance)
					{
						targetPlayer = (L2PcInstance) target;
					}
					
					L2PetInstance targetPet = null;
					
					if (target instanceof L2PetInstance)
					{
						targetPet = (L2PetInstance) target;
					}
					
					if (player != null && (targetPlayer != null || targetPet != null))
					{
						boolean condGood = true;
						
						if (getSkillType() == SkillType.RESURRECT)
						{
							Castle castle = null;
							if (targetPlayer != null)
							{
								castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
							}
							else if (targetPet != null)
							{
								castle = CastleManager.getInstance().getCastle(targetPet.getX(), targetPet.getY(), targetPet.getZ());
							}
							
							if (castle != null && castle.getSiege().getIsInProgress())
							{
								if (castle.getSiege().getFlagCount(player.getClan()) > 0 || castle.getSiege().getTowerCount(player.getClan()) > 0)
								{
									condGood = true;
								}
								else
								{
									condGood = false;
									player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
								}
							}
							
							if (targetPlayer != null)
							{
								if (targetPlayer.isReviveRequested())
								{
									if (targetPlayer.isRevivingPet())
									{
										player.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
									}
									else
									{
										player.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
									}
									condGood = false;
								}
							}
							else if (targetPet != null)
							{
								if (targetPet.getOwner() != player)
								{
									if (targetPet.getOwner().isReviveRequested())
									{
										if (targetPet.getOwner().isRevivingPet())
										{
											player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
										}
										else
										{
											player.sendPacket(SystemMessageId.PET_CANNOT_RES); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
										}
										condGood = false;
									}
								}
							}
						}
						
						if (condGood)
						{
							if (!onlyFirst)
							{
								targetList.add(target);
								return targetList.toArray(new L2Object[targetList.size()]);
							}
							
							return new L2Character[]
							{
								target
							};
							
						}
					}
				}
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return null;
			}
			case TARGET_CORPSE_MOB:
			{
				List<L2Character> targetList = new ArrayList<>();
				
				if (!(target instanceof L2Attackable) || !target.isDead())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
				
				if (!onlyFirst)
				{
					targetList.add(target);
					return targetList.toArray(new L2Object[targetList.size()]);
				}
				
				return new L2Character[]
				{
					target
				};
				
			}
			case TARGET_AREA_AIM_CORPSE:
			{
				if (target != null && target.isDead())
				{
					return new L2Character[]
					{
						target
					};
				}
				return null;
			}
			case TARGET_AREA_CORPSE_MOB:
			{
				if ((!(target instanceof L2Attackable)) || !target.isDead())
				{
					Announcements _a = Announcements.getInstance();
					_a.sys("target:" + target.getName());
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}
				
				if (onlyFirst)
				{
					return new L2Character[]
					{
						target
					};
				}
				
				List<L2Character> targetList = new ArrayList<>();
				targetList.add(target);
				
				final boolean srcInArena = (activeChar.isInsideZone(ZoneId.ZONE_PVP) && !activeChar.isInsideZone(ZoneId.ZONE_SIEGE));
				
				for (L2Character obj : target.getKnownList().getKnownCharactersInRadius(getSkillRadius()))
				{
					if (!(obj instanceof L2Attackable || obj instanceof L2PlayableInstance))
					{
						continue;
					}
					
					if (!checkForAreaOffensiveSkills(activeChar, obj, this, srcInArena))
					{
						continue;
					}
					
					targetList.add(obj);
				}
				
				if (targetList.isEmpty())
				{
					return _emptyTargetList;
				}
				
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_UNLOCKABLE:
			{
				List<L2Character> targetList = new ArrayList<>();
				if (!(target instanceof L2DoorInstance) && !(target instanceof L2ChestInstance))
				{
					// Like L2OFF if target isn't door or chest send message of incorrect target
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
					return null;
				}
				
				if (!onlyFirst)
				{
					targetList.add(target);
					return targetList.toArray(new L2Object[targetList.size()]);
				}
				
				return new L2Character[]
				{
					target
				};
				
			}
			case TARGET_ITEM:
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Target type of skill is not currently handled.");
				activeChar.sendPacket(sm);
				return null;
			}
			case TARGET_UNDEAD:
			{
				List<L2Character> targetList = new ArrayList<>();
				if (target instanceof L2NpcInstance || target instanceof L2SummonInstance)
				{
					if (!target.isUndead() || target.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
						return null;
					}
					
					if (!onlyFirst)
					{
						targetList.add(target);
					}
					else
					{
						return new L2Character[]
						{
							target
						};
					}
					
					return targetList.toArray(new L2Object[targetList.size()]);
				}
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return null;
			}
			case TARGET_AREA_UNDEAD:
			{
				List<L2Character> targetList = new ArrayList<>();
				
				for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(getSkillRadius()))
				{
					if (obj instanceof L2NpcInstance)
					{
						target = obj;
					}
					else if (obj instanceof L2SummonInstance)
					{
						target = obj;
					}
					else
					{
						continue;
					}
					
					if (!GeoData.getInstance().canSeeTarget(activeChar, target))
					{
						continue;
					}
					
					if (target.isAlikeDead() || !target.isUndead())
					{
						continue;
					}
					
					if (onlyFirst)
					{
						return new L2Character[]
						{
							obj
						};
					}
					
					targetList.add(obj);
				}
				
				if (targetList.isEmpty())
				{
					return _emptyTargetList;
				}
				
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_ENEMY_SUMMON:
			{
				if (target != null && target instanceof L2Summon)
				{
					L2Summon targetSummon = (L2Summon) target;
					if (activeChar instanceof L2PcInstance && activeChar.getPet() != targetSummon && !targetSummon.isDead() && (targetSummon.getOwner().getPvpFlag() != 0 || targetSummon.getOwner().getKarma() > 0 || targetSummon.getOwner().isInDuel())
						|| targetSummon.getOwner().isInsideZone(ZoneId.ZONE_PVP) && ((L2PcInstance) activeChar).isInsideZone(ZoneId.ZONE_PVP))
					{
						return new L2Character[]
						{
							targetSummon
						};
					}
				}
				return null;
			}
			case TARGET_SIEGE:
			{
				if (target != null && !target.isDead() && (target instanceof L2DoorInstance || target instanceof L2ControlTowerInstance))
				{
					return new L2Character[]
					{
						target
					};
				}
				return _emptyTargetList;
			}
			case TARGET_TYRANNOSAURUS:
			{
				if (target instanceof L2PcInstance)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
				
				if (target instanceof L2MonsterInstance
					&& (((L2MonsterInstance) target).getNpcId() == 22217 || ((L2MonsterInstance) target).getNpcId() == 22216 || ((L2MonsterInstance) target).getNpcId() == 22215 || ((L2MonsterInstance) target).getNpcId() == 22318 || ((L2MonsterInstance) target).getNpcId() == 22319))
				{
					return new L2Character[]
					{
						target
					};
				}
				return null;
			}
			case TARGET_CLAN_MEMBER:
			{
				List<L2Character> targetList = new ArrayList<>();
				
				final L2NpcInstance npc = (L2NpcInstance) activeChar;
				if (npc.getFactionId() == null || npc.getFactionId().isEmpty())
				{
					return new L2Character[]
					{
						activeChar
					};
				}
				
				targetList.add(activeChar);
				for (L2NpcInstance newTarget : activeChar.getKnownList().getKnownType(L2NpcInstance.class))
				{
					if (npc.getFactionId().equals(newTarget.getFactionId()))
					{
						// Bypass buff if target is dead
						if (newTarget.isDead())
						{
							continue;
						}
						
						if (!Util.checkIfInRange(_castRange, activeChar, newTarget, true))
						{
							continue;
						}
						
						targetList.add(newTarget);
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			default:
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("Target type of skill is not currently handled.");
				activeChar.sendPacket(sm);
				return _emptyTargetList;
			}
		}
	}
	
	public final Func[] getStatFuncs(final L2Effect effect, final L2Character player)
	{
		if (!(player instanceof L2PcInstance) && !(player instanceof L2Attackable) && !(player instanceof L2Summon))
		{
			return _emptyFunctionSet;
		}
		
		if (_funcTemplates == null)
		{
			return _emptyFunctionSet;
		}
		
		final List<Func> funcs = new FastList<>();
		
		for (final FuncTemplate t : _funcTemplates)
		{
			final Env env = new Env();
			env.player = player;
			env.skill = this;
			final Func f = t.getFunc(env, this); // skill is owner
			
			if (f != null)
			{
				funcs.add(f);
			}
		}
		if (funcs.size() == 0)
		{
			return _emptyFunctionSet;
		}
		
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	public boolean hasEffects()
	{
		return _effectTemplates != null && _effectTemplates.length > 0;
	}
	
	public final L2Effect[] getEffects(final L2Character effector, final L2Character effected)
	{
		return getEffects(effector, effected, false, false, false);
	}
	
	public final L2Effect[] getEffects(final L2Character effector, final L2Character effected, final boolean ss, final boolean sps, final boolean bss)
	{
		if (isPassive())
		{
			return _emptyEffectSet;
		}
		
		if (_effectTemplates == null)
		{
			return _emptyEffectSet;
		}
		
		if (effector != effected && effected.isInvul())
		{
			return _emptyEffectSet;
		}
		
		final List<L2Effect> effects = new FastList<>();
		
		boolean skillMastery = false;
		
		if (!isToggle() && Formulas.calcSkillMastery(effector))
		{
			skillMastery = true;
		}
		
		final Env env = new Env();
		env.player = effector;
		env.target = effected;
		env.skill = this;
		env.skillMastery = skillMastery;
		
		for (final EffectTemplate et : _effectTemplates)
		{
			boolean success = true;
			
			if (et.effectPower > -1)
			{
				success = Formulas.getInstance().calcEffectSuccess(effector, effected, et, this, ss, sps, bss);
			}
			
			if (success)
			{
				L2Effect e = et.getEffect(env);
				if (e != null)
				{
					effects.add(e);
				}
			}
			
			else if (et.showIcon && effector instanceof L2PcInstance)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
				sm.addString(effected.getName());
				sm.addSkillName(this.getDisplayId());
				((L2PcInstance) effector).sendPacket(sm);
			}
		}
		
		if (effects.size() == 0)
		{
			return _emptyEffectSet;
		}
		
		return effects.toArray(new L2Effect[effects.size()]);
	}
	
	public final L2Effect[] getEffectsSelf(final L2Character effector)
	{
		if (isPassive())
		{
			return _emptyEffectSet;
		}
		
		if (_effectTemplatesSelf == null)
		{
			return _emptyEffectSet;
		}
		
		final List<L2Effect> effects = new FastList<>();
		
		final Env env = new Env();
		env.player = effector;
		env.target = effector;
		env.skill = this;
		
		for (final EffectTemplate et : _effectTemplatesSelf)
		{
			L2Effect e = et.getEffect(env);
			if (e != null)
			{
				// Implements effect charge
				if (e.getEffectType() == L2Effect.EffectType.CHARGE)
				{
					env.skill = SkillTable.getInstance().getInfo(8, effector.getSkillLevel(8));
					final EffectCharge effect = (EffectCharge) env.target.getFirstEffect(L2Effect.EffectType.CHARGE);
					if (effect != null)
					{
						int effectcharge = effect.getLevel();
						if (effectcharge < _numCharges)
						{
							effectcharge++;
							effect.addNumCharges(effectcharge);
							
							if (env.target instanceof L2PcInstance)
							{
								env.target.sendPacket(new EtcStatusUpdate((L2PcInstance) env.target));
								SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
								sm.addNumber(effectcharge);
								env.target.sendPacket(sm);
							}
						}
					}
					else
					{
						effects.add(e);
					}
				}
				else
				{
					effects.add(e);
				}
			}
		}
		if (effects.size() == 0)
		{
			return _emptyEffectSet;
		}
		
		return effects.toArray(new L2Effect[effects.size()]);
	}
	
	public final void attach(final FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			{
				f
			};
		}
		else
		{
			final int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}
	
	public final void attach(final EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[]
			{
				effect
			};
		}
		else
		{
			final int len = _effectTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}
	
	public final void attachSelf(final EffectTemplate effect)
	{
		if (_effectTemplatesSelf == null)
		{
			_effectTemplatesSelf = new EffectTemplate[]
			{
				effect
			};
		}
		else
		{
			final int len = _effectTemplatesSelf.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplatesSelf, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplatesSelf = tmp;
			tmp = null;
		}
	}
	
	public boolean isAbnormalEffectByName(final int abnormalEffect)
	{
		if (isPassive())
		{
			return false;
		}
		
		if (_effectTemplates == null)
		{
			return false;
		}
		
		for (final EffectTemplate et : _effectTemplates)
		{
			if (et.abnormalEffect == abnormalEffect)
			{
				return true;
			}
		}
		return false;
	}
	
	public final void attach(final Condition c, final boolean itemOrWeapon)
	{
		if (itemOrWeapon)
		{
			_itemPreCondition = c;
		}
		else
		{
			_preCondition = c;
		}
	}
	
	public boolean checkPartyClan(final L2Character activeChar, final L2Object target)
	{
		if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance)
		{
			L2PcInstance targetChar = (L2PcInstance) target;
			L2PcInstance activeCh = (L2PcInstance) activeChar;
			
			if (activeCh.isInOlympiadMode() && activeCh.isOlympiadStart() && targetChar.isInOlympiadMode() && targetChar.isOlympiadStart())
			{
				return false;
			}
			
			if (activeCh.isInDuel() && targetChar.isInDuel() && activeCh.getDuelId() == targetChar.getDuelId())
			{
				return false;
			}
			
			if (((activeCh._inEvent || activeCh._inEventCTF || activeCh._inEventDM || activeCh._inEventTvT || activeCh._inEventVIP) && (!targetChar._inEvent && !targetChar._inEventCTF && !targetChar._inEventDM && !targetChar._inEventTvT && !targetChar._inEventVIP))
				|| ((targetChar._inEvent || targetChar._inEventCTF || targetChar._inEventDM || targetChar._inEventTvT || targetChar._inEventVIP) && (!activeCh._inEvent && !activeCh._inEventCTF && !activeCh._inEventDM && !activeCh._inEventTvT && !activeCh._inEventVIP)))
			{
				return true;
			}
			
			if ((activeCh._inEvent && targetChar._inEvent) || (activeCh._inEventDM && targetChar._inEventDM) || (activeCh._inEventTvT && targetChar._inEventTvT) || (activeCh._inEventCTF && targetChar._inEventCTF) || (activeCh._inEventVIP && targetChar._inEventVIP))
			{
				return false;
			}
			
			// Same party
			if (activeCh.getParty() != null && targetChar.getParty() != null && activeCh.getParty().getPartyLeaderOID() == targetChar.getParty().getPartyLeaderOID())
			{
				return true;
			}
			
			// Same clan
			if (activeCh.getClan() != null && targetChar.getClan() != null && activeCh.getClan().getClanId() == targetChar.getClan().getClanId())
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "" + _name + "[id=" + _id + ",lvl=" + _level + "]";
	}
	
	/**
	 * @return Returns the _targetConsumeId.
	 */
	public final int getTargetConsumeId()
	{
		return _targetConsumeId;
	}
	
	/**
	 * @return Returns the targetConsume.
	 */
	public final int getTargetConsume()
	{
		return _targetConsume;
	}
	
	public boolean hasSelfEffects()
	{
		return (_effectTemplatesSelf != null && _effectTemplatesSelf.length > 0);
	}
	
	/**
	 * @return minimum skill/effect land rate (default is 1).
	 */
	public final int getMinChance()
	{
		return _minChance;
	}
	
	/**
	 * @return maximum skill/effect land rate (default is 99).
	 */
	public final int getMaxChance()
	{
		return _maxChance;
	}
	
	/**
	 * @return the _advancedFlag
	 */
	public boolean is_advancedFlag()
	{
		return _advancedFlag;
	}
	
	/**
	 * @return the _advancedMultiplier
	 */
	public int get_advancedMultiplier()
	{
		return _advancedMultiplier;
	}
	
	/**
	 * @return the _reuseHashCode
	 */
	public final int getReuseHashCode()
	{
		return _reuseHashCode;
	}
	
	public boolean isSimultaneousCast()
	{
		return _simultaneousCast;
	}
	
	public static final boolean checkForAreaOffensiveSkills(L2Character caster, L2Character target, L2Skill skill, boolean sourceInArena)
	{
		if (target == null || target.isDead() || target == caster)
		{
			return false;
		}
		
		final L2PcInstance player = caster.getActingPlayer();
		
		if (player != null)
		{
			final L2PcInstance targetPlayer = target.getActingPlayer();
			if (targetPlayer != null)
			{
				if (targetPlayer == caster || targetPlayer == player)
				{
					return false;
				}
				
				if (targetPlayer.inObserverMode())
				{
					return false;
				}
				
				if (skill.isOffensive() && player.getSiegeState() > 0 && player.isInsideZone(ZoneId.ZONE_SIEGE) && player.getSiegeState() == targetPlayer.getSiegeState())
				{
					return false;
				}
				
				if (target.isInsideZone(ZoneId.ZONE_PEACE))
				{
					return false;
				}
				
				if (player.isInParty() && targetPlayer.isInParty())
				{
					// Same party
					if (player.getParty().getPartyLeaderOID() == targetPlayer.getParty().getPartyLeaderOID())
					{
						return false;
					}
					
					// Same commandchannel
					if (player.getParty().getCommandChannel() != null && player.getParty().getCommandChannel() == targetPlayer.getParty().getCommandChannel())
					{
						return false;
					}
				}
				
				if (!sourceInArena && !(targetPlayer.isInsideZone(ZoneId.ZONE_PVP) && !targetPlayer.isInsideZone(ZoneId.ZONE_SIEGE)))
				{
					if (player.getAllyId() != 0 && player.getAllyId() == targetPlayer.getAllyId())
					{
						return false;
					}
					
					if (player.getClanId() != 0 && player.getClanId() == targetPlayer.getClanId())
					{
						return false;
					}
					
					if (!player.checkPvpSkill(targetPlayer, skill, (caster instanceof L2Summon)))
					{
						return false;
					}
				}
			}
		}
		else
		{
			// source is not playable
			if (!(target instanceof L2PlayableInstance))
			{
				return false;
			}
		}
		
		if (geoEnabled && !GeoData.getInstance().canSeeTarget(caster, target))
		{
			return false;
		}
		
		return true;
	}
}