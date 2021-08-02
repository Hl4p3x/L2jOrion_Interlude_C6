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
package l2jorion.game.model.actor.stat;

import java.text.NumberFormat;

import l2jorion.Config;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.model.zone.type.L2SwampZone;
import l2jorion.game.skills.Calculator;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.Stats;

/**
 * The Class CharStat.
 */
public class CharStat
{
	/** The _active char. */
	private final L2Character _activeChar;
	
	/** The _exp. */
	private long _exp = 0;
	
	/** The _sp. */
	private int _sp = 0;
	
	/** The _level. */
	private int _level = 1;
	
	// =========================================================
	// Constructor
	/**
	 * Instantiates a new char stat.
	 * @param activeChar the active char
	 */
	public CharStat(final L2Character activeChar)
	{
		_activeChar = activeChar;
	}
	
	// =========================================================
	// Method - Public
	/**
	 * Calculate the new value of the state with modifiers that will be applied on the targeted L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>. Each Calculator (a calculator per state) own a table of Func object. A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...) : <BR>
	 * <BR>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * <BR>
	 * When the calc method of a calculator is launched, each mathematic function is called according to its priority <B>_order</B>. Indeed, Func with lowest priority order is executed firsta and Funcs with the same order are executed in unspecified order. The result of the calculation is stored in
	 * the value property of an Env class instance.<BR>
	 * <BR>
	 * @param stat The stat to calculate the new value with modifiers
	 * @param init The initial value of the stat before applying modifiers
	 * @param target The L2Charcater whose properties will be used in the calculation (ex : CON, INT...)
	 * @param skill The L2Skill whose properties will be used in the calculation (ex : Level...)
	 * @return the double
	 */
	public final double calcStat(final Stats stat, final double init, final L2Character target, final L2Skill skill)
	{
		if (_activeChar == null)
		{
			return init;
		}
		
		final int id = stat.ordinal();
		
		Calculator c = _activeChar.getCalculators()[id];
		
		// If no Func object found, no modifier is applied
		if (c == null || c.size() == 0)
		{
			return init;
		}
		
		// Create and init an Env object to pass parameters to the Calculator
		final Env env = new Env();
		env.player = _activeChar;
		env.target = target;
		env.skill = skill;
		env.value = init;
		env.baseValue = init;
		
		// Launch the calculation
		c.calc(env);
		
		// avoid some troubles with negative stats (some stats should never be
		// negative)
		
		if (env.value <= 0)
		{
			switch (stat)
			{
				case MAX_HP:
				case MAX_MP:
				case MAX_CP:
				case MAGIC_DEFENCE:
				case POWER_DEFENCE:
				case POWER_ATTACK:
				case MAGIC_ATTACK:
				case POWER_ATTACK_SPEED:
				case MAGIC_ATTACK_SPEED:
				case SHIELD_DEFENCE:
				case STAT_CON:
				case STAT_DEX:
				case STAT_INT:
				case STAT_MEN:
				case STAT_STR:
				case STAT_WIT:
					env.value = 1;
			}
		}
		
		return env.value;
	}
	
	/**
	 * Return the Accuracy (base+modifier) of the L2Character in function of the Weapon Expertise Penalty.
	 * @return the accuracy
	 */
	public int getAccuracy()
	{
		if (_activeChar == null)
		{
			return 0;
		}
		
		return (int) (calcStat(Stats.ACCURACY_COMBAT, 0, null, null) / _activeChar.getWeaponExpertisePenalty());
	}
	
	/**
	 * Gets the active char.
	 * @return the active char
	 */
	public L2Character getActiveChar()
	{
		return _activeChar;
	}
	
	/**
	 * Return the Attack Speed multiplier (base+modifier) of the L2Character to get proper animations.
	 * @return the attack speed multiplier
	 */
	public final float getAttackSpeedMultiplier()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (float) (1.1 * getPAtkSpd() / _activeChar.getTemplate().basePAtkSpd);
	}
	
	/**
	 * Return the CON of the L2Character (base+modifier).
	 * @return the cON
	 */
	public final int getCON()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_CON, _activeChar.getTemplate().baseCON, null, null);
	}
	
	/**
	 * Return the Critical Damage rate (base+modifier) of the L2Character.
	 * @param target the target
	 * @param init the init
	 * @return the critical dmg
	 */
	public final double getCriticalDmg(final L2Character target, final double init)
	{
		return calcStat(Stats.CRITICAL_DAMAGE, init, target, null);
	}
	
	/**
	 * Return the Critical Hit rate (base+modifier) of the L2Character.
	 * @param target the target
	 * @param skill the skill
	 * @return the critical hit
	 */
	public int getCriticalHit(final L2Character target, final L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		int criticalHit = (int) (calcStat(Stats.CRITICAL_RATE, _activeChar.getTemplate().baseCritRate, target, skill) * 10.0 + 0.5);
		
		criticalHit /= 10;
		
		// Set a cap of Critical Hit at 500
		if (criticalHit > Config.MAX_PCRIT_RATE)
		{
			criticalHit = Config.MAX_PCRIT_RATE;
		}
		
		return criticalHit;
	}
	
	/**
	 * Return the DEX of the L2Character (base+modifier).
	 * @return the dEX
	 */
	public final int getDEX()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_DEX, _activeChar.getTemplate().baseDEX, null, null);
	}
	
	/**
	 * Return the Attack Evasion rate (base+modifier) of the L2Character.
	 * @param target the target
	 * @return the evasion rate
	 */
	public int getEvasionRate(final L2Character target)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) (calcStat(Stats.EVASION_RATE, 0, target, null) / _activeChar.getArmourExpertisePenalty());
	}
	
	/**
	 * Gets the exp.
	 * @return the exp
	 */
	public long getExp()
	{
		return _exp;
	}
	
	/**
	 * Sets the exp.
	 * @param value the new exp
	 */
	public void setExp(final long value)
	{
		_exp = value;
	}
	
	/**
	 * Return the INT of the L2Character (base+modifier).
	 * @return the iNT
	 */
	public int getINT()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_INT, _activeChar.getTemplate().baseINT, null, null);
	}
	
	/**
	 * Gets the level.
	 * @return the level
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Sets the level.
	 * @param value the new level
	 */
	public void setLevel(final int value)
	{
		_level = value;
	}
	
	/**
	 * Return the Magical Attack range (base+modifier) of the L2Character.
	 * @param skill the skill
	 * @return the magical attack range
	 */
	public final int getMagicalAttackRange(final L2Skill skill)
	{
		if (skill != null)
		{
			return (int) calcStat(Stats.MAGIC_ATTACK_RANGE, skill.getCastRange(), null, skill);
		}
		
		if (_activeChar == null)
		{
			return 1;
		}
		
		return _activeChar.getTemplate().baseAtkRange;
	}
	
	/**
	 * Gets the max cp.
	 * @return the max cp
	 */
	public int getMaxCp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_CP, _activeChar.getTemplate().baseCpMax, null, null);
	}
	
	/**
	 * Gets the max hp.
	 * @return the max hp
	 */
	public int getMaxHp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_HP, _activeChar.getTemplate().baseHpMax, null, null);
	}
	
	/**
	 * Gets the max mp.
	 * @return the max mp
	 */
	public int getMaxMp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_MP, _activeChar.getTemplate().baseMpMax, null, null);
	}
	
	/**
	 * Return the MAtk (base+modifier) of the L2Character for a skill used in function of abnormal effects in progress.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Calculate Magic damage</li> <BR>
	 * <BR>
	 * @param target The L2Character targeted by the skill
	 * @param skill The L2Skill used against the target
	 * @return the m atk
	 */
	public int getMAtk(final L2Character target, final L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		float bonusAtk = 1;
		
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_ATK;
		}
		
		double attack = _activeChar.getTemplate().baseMAtk * bonusAtk;
		
		// Get the skill type to calculate its effect in function of base stats
		// of the L2Character target
		Stats stat = skill == null ? null : skill.getStat();
		
		if (stat != null)
		{
			switch (stat)
			{
				case AGGRESSION:
					attack += _activeChar.getTemplate().baseAggression;
					break;
				case BLEED:
					attack += _activeChar.getTemplate().baseBleed;
					break;
				case POISON:
					attack += _activeChar.getTemplate().basePoison;
					break;
				case STUN:
					attack += _activeChar.getTemplate().baseStun;
					break;
				case ROOT:
					attack += _activeChar.getTemplate().baseRoot;
					break;
				case MOVEMENT:
					attack += _activeChar.getTemplate().baseMovement;
					break;
				case CONFUSION:
					attack += _activeChar.getTemplate().baseConfusion;
					break;
				case SLEEP:
					attack += _activeChar.getTemplate().baseSleep;
					break;
				case FIRE:
					attack += _activeChar.getTemplate().baseFire;
					break;
				case WIND:
					attack += _activeChar.getTemplate().baseWind;
					break;
				case WATER:
					attack += _activeChar.getTemplate().baseWater;
					break;
				case EARTH:
					attack += _activeChar.getTemplate().baseEarth;
					break;
				case HOLY:
					attack += _activeChar.getTemplate().baseHoly;
					break;
				case DARK:
					attack += _activeChar.getTemplate().baseDark;
					break;
			}
		}
		
		// Add the power of the skill to the attack effect
		if (skill != null)
		{
			attack += skill.getPower();
		}
		
		stat = null;
		
		// Calculate modifiers Magic Attack
		return (int) calcStat(Stats.MAGIC_ATTACK, attack, target, skill);
	}
	
	/**
	 * Return the MAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 * @return the m atk spd
	 */
	public int getMAtkSpd()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		float bonusSpdAtk = 1;
		
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusSpdAtk = Config.L2JMOD_CHAMPION_SPD_ATK;
		}
		
		double val = calcStat(Stats.MAGIC_ATTACK_SPEED, _activeChar.getTemplate().baseMAtkSpd * bonusSpdAtk, null, null);
		
		val /= _activeChar.getArmourExpertisePenalty();
		
		if (val > Config.MAX_MATK_SPEED && _activeChar instanceof L2PcInstance)
		{
			val = Config.MAX_MATK_SPEED;
		}
		
		return (int) val;
	}
	
	/**
	 * Return the Magic Critical Hit rate (base+modifier) of the L2Character.
	 * @param target the target
	 * @param skill the skill
	 * @return the m critical hit
	 */
	public final int getMCriticalHit(final L2Character target, final L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		double mrate = calcStat(Stats.MCRITICAL_RATE, (_activeChar.getTemplate().baseMCritRate) * Config.MCRIT_RATE_MUL, target, skill);
		
		if (mrate > Config.MAX_MCRIT_RATE)
		{
			mrate = Config.MAX_MCRIT_RATE;
		}
		
		if (Config.SEND_SKILLS_CHANCE_TO_PLAYERS)
		{
			if (_activeChar instanceof L2PcInstance && ((L2PcInstance) _activeChar).isMageClass())
			{
				double mcChance = (mrate / 1000 * 100);
				NumberFormat defaultFormat = NumberFormat.getNumberInstance();
				if (mcChance >= 0.01)
				{
					defaultFormat.setMinimumFractionDigits(2);
				}
				
				String formatedChance = defaultFormat.format(mcChance);
				((L2PcInstance) _activeChar).sendMessage("Magic Critical chance: " + formatedChance + "%");
			}
		}
		
		return (int) mrate;
	}
	
	public int getMDef(final L2Character target, final L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		// Get the base MDef of the L2Character
		double defence = _activeChar.getTemplate().baseMDef;
		
		// Calculate modifier for Raid Bosses
		if (_activeChar.isRaid())
		{
			defence *= Config.RAID_M_DEFENCE_MULTIPLIER;
		}
		
		// Calculate modifiers Magic Attack
		return (int) calcStat(Stats.MAGIC_DEFENCE, defence, target, skill);
	}
	
	/**
	 * Return the MEN of the L2Character (base+modifier).
	 * @return the mEN
	 */
	public final int getMEN()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_MEN, _activeChar.getTemplate().baseMEN, null, null);
	}
	
	/**
	 * Gets the movement speed multiplier.
	 * @return the movement speed multiplier
	 */
	public final float getMovementSpeedMultiplier()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return getRunSpeed() * 1f / _activeChar.getTemplate().baseRunSpd;
	}
	
	public float getMoveSpeed()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		if (_activeChar.isRunning())
		{
			return getRunSpeed();
		}
		
		return getWalkSpeed();
	}
	
	public final double getMReuseRate(final L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return calcStat(Stats.MAGIC_REUSE_RATE, _activeChar.getTemplate().baseMReuseRate, null, skill);
	}
	
	public final double getPReuseRate(final L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return calcStat(Stats.P_REUSE, _activeChar.getTemplate().baseMReuseRate, null, skill);
	}
	
	public int getPAtk(final L2Character target)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		float bonusAtk = 1;
		
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_ATK;
		}
		
		return (int) calcStat(Stats.POWER_ATTACK, _activeChar.getTemplate().basePAtk * bonusAtk, target, null);
	}
	
	public final double getPAtkAnimals(final L2Character target)
	{
		return calcStat(Stats.PATK_ANIMALS, 1, target, null);
	}
	
	public final double getPAtkDragons(final L2Character target)
	{
		return calcStat(Stats.PATK_DRAGONS, 1, target, null);
	}
	
	/**
	 * Return the PAtk Modifier against angels.
	 * @param target the target
	 * @return the p atk angels
	 */
	public final double getPAtkAngels(final L2Character target)
	{
		return calcStat(Stats.PATK_ANGELS, 1, target, null);
	}
	
	/**
	 * Return the PAtk Modifier against insects.
	 * @param target the target
	 * @return the p atk insects
	 */
	public final double getPAtkInsects(final L2Character target)
	{
		return calcStat(Stats.PATK_INSECTS, 1, target, null);
	}
	
	/**
	 * Return the PAtk Modifier against monsters.
	 * @param target the target
	 * @return the p atk monsters
	 */
	public final double getPAtkMonsters(final L2Character target)
	{
		return calcStat(Stats.PATK_MONSTERS, 1, target, null);
	}
	
	/**
	 * Return the PAtk Modifier against plants.
	 * @param target the target
	 * @return the p atk plants
	 */
	public final double getPAtkPlants(final L2Character target)
	{
		return calcStat(Stats.PATK_PLANTS, 1, target, null);
	}
	
	/**
	 * Return the PAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 * @return the p atk spd
	 */
	public int getPAtkSpd()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		float bonusAtk = 1;
		
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_SPD_ATK;
		}
		
		double val = calcStat(Stats.POWER_ATTACK_SPEED, _activeChar.getTemplate().basePAtkSpd * bonusAtk, null, null);
		
		val /= _activeChar.getArmourExpertisePenalty();
		
		if (val > Config.MAX_PATK_SPEED && _activeChar instanceof L2PcInstance)
		{
			val = Config.MAX_PATK_SPEED;
		}
		
		return (int) val;
	}
	
	/**
	 * Return the PAtk Modifier against undead.
	 * @param target the target
	 * @return the p atk undead
	 */
	public final double getPAtkUndead(final L2Character target)
	{
		return calcStat(Stats.PATK_UNDEAD, 1, target, null);
	}
	
	/**
	 * Gets the p def undead.
	 * @param target the target
	 * @return the p def undead
	 */
	public final double getPDefUndead(final L2Character target)
	{
		return calcStat(Stats.PDEF_UNDEAD, 1, target, null);
	}
	
	/**
	 * Gets the p def plants.
	 * @param target the target
	 * @return the p def plants
	 */
	public final double getPDefPlants(final L2Character target)
	{
		return calcStat(Stats.PDEF_PLANTS, 1, target, null);
	}
	
	/**
	 * Gets the p def insects.
	 * @param target the target
	 * @return the p def insects
	 */
	public final double getPDefInsects(final L2Character target)
	{
		return calcStat(Stats.PDEF_INSECTS, 1, target, null);
	}
	
	/**
	 * Gets the p def animals.
	 * @param target the target
	 * @return the p def animals
	 */
	public final double getPDefAnimals(final L2Character target)
	{
		return calcStat(Stats.PDEF_ANIMALS, 1, target, null);
	}
	
	/**
	 * Gets the p def monsters.
	 * @param target the target
	 * @return the p def monsters
	 */
	public final double getPDefMonsters(final L2Character target)
	{
		return calcStat(Stats.PDEF_MONSTERS, 1, target, null);
	}
	
	/**
	 * Gets the p def dragons.
	 * @param target the target
	 * @return the p def dragons
	 */
	public final double getPDefDragons(final L2Character target)
	{
		return calcStat(Stats.PDEF_DRAGONS, 1, target, null);
	}
	
	/**
	 * Gets the p def angels.
	 * @param target the target
	 * @return the p def angels
	 */
	public final double getPDefAngels(final L2Character target)
	{
		return calcStat(Stats.PDEF_ANGELS, 1, target, null);
	}
	
	/**
	 * Return the PDef (base+modifier) of the L2Character.
	 * @param target the target
	 * @return the p def
	 */
	public int getPDef(final L2Character target)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		// Get the base PDef of the L2Character
		double defence = _activeChar.getTemplate().basePDef;
		
		// Calculate modifier for Raid Bosses
		if (_activeChar.isRaid())
		{
			defence *= Config.RAID_P_DEFENCE_MULTIPLIER;
		}
		
		// Calculate modifiers Magic Attack
		return (int) calcStat(Stats.POWER_DEFENCE, defence, target, null);
	}
	
	/**
	 * Return the Physical Attack range (base+modifier) of the L2Character.
	 * @return the physical attack range
	 */
	public final int getPhysicalAttackRange()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		int range = (int) calcStat(Stats.POWER_ATTACK_RANGE, _activeChar.getTemplate().baseAtkRange, null, null);
		
		return range;
	}
	
	/**
	 * Return the Skill/Spell reuse modifier.
	 * @param target the target
	 * @return the reuse modifier
	 */
	public final double getReuseModifier(final L2Character target)
	{
		return calcStat(Stats.ATK_REUSE, 1, target, null);
	}
	
	/**
	 * Return the RunSpeed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 * @return the run speed
	 */
	public int getRunSpeed()
	{
		int val = (int) calcStat(Stats.RUN_SPEED, _activeChar.getTemplate().baseRunSpd, null, null) + Config.RUN_SPD_BOOST;
		
		if (_activeChar.isInsideZone(ZoneId.ZONE_WATER))
		{
			val /= 2;
		}
		
		if (_activeChar.isFlying())
		{
			val += Config.WYVERN_SPEED;
			return val;
		}
		
		if (_activeChar.isRiding())
		{
			val += Config.STRIDER_SPEED;
			return val;
		}
		
		val /= _activeChar.getArmourExpertisePenalty();
		
		if (val > Config.MAX_RUN_SPEED && !_activeChar.charIsGM())
		{
			val = Config.MAX_RUN_SPEED;
		}
		
		if (getActiveChar().isInsideZone(ZoneId.ZONE_SWAMP))
		{
			final L2SwampZone zone = ZoneManager.getInstance().getZone(getActiveChar(), L2SwampZone.class);
			if (zone != null)
			{
				val *= zone.getMoveBonus();
			}
		}
		
		return val;
	}
	
	/**
	 * Return the WalkSpeed (base+modifier) of the L2Character.
	 * @return the walk speed
	 */
	public final int getWalkSpeed()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		int val = (int) calcStat(Stats.WALK_SPEED, _activeChar.getTemplate().baseWalkSpd, null, null);
		
		if (_activeChar instanceof L2PcInstance)
		{
			val = getRunSpeed() * 70 / 100;
			
			if (getActiveChar().isInsideZone(ZoneId.ZONE_SWAMP))
			{
				final L2SwampZone zone = ZoneManager.getInstance().getZone(getActiveChar(), L2SwampZone.class);
				if (zone != null)
				{
					val *= zone.getMoveBonus();
				}
			}
		}
		
		return val;
	}
	
	/**
	 * Return the ShieldDef rate (base+modifier) of the L2Character.
	 * @return the shld def
	 */
	public final int getShldDef()
	{
		return (int) calcStat(Stats.SHIELD_DEFENCE, 0, null, null);
	}
	
	/**
	 * Gets the sp.
	 * @return the sp
	 */
	public int getSp()
	{
		return _sp;
	}
	
	/**
	 * Sets the sp.
	 * @param value the new sp
	 */
	public void setSp(final int value)
	{
		_sp = value;
	}
	
	/**
	 * Return the STR of the L2Character (base+modifier).
	 * @return the sTR
	 */
	public final int getSTR()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_STR, _activeChar.getTemplate().baseSTR, null, null);
	}
	
	/**
	 * Return the WIT of the L2Character (base+modifier).
	 * @return the wIT
	 */
	public final int getWIT()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_WIT, _activeChar.getTemplate().baseWIT, null, null);
	}
	
	/**
	 * Return the mpConsume.
	 * @param skill the skill
	 * @return the mp consume
	 */
	public final int getMpConsume(final L2Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		
		int mpconsume = skill.getMpConsume();
		
		if (skill.isDance() && _activeChar != null && _activeChar.getDanceCount() > 0)
		{
			mpconsume += _activeChar.getDanceCount() * skill.getNextDanceMpCost();
		}
		
		return (int) calcStat(Stats.MP_CONSUME, mpconsume, null, skill);
	}
	
	/**
	 * Return the mpInitialConsume.
	 * @param skill the skill
	 * @return the mp initial consume
	 */
	public final int getMpInitialConsume(final L2Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MP_CONSUME, skill.getMpInitialConsume(), null, skill);
	}
	
	/**
	 * Return the PDef Modifier against giants.
	 * @param target the target
	 * @return the p def giants
	 */
	public final double getPDefGiants(final L2Character target)
	{
		return calcStat(Stats.PDEF_GIANTS, 1, target, null);
	}
	
	/**
	 * Return the PDef Modifier against giants.
	 * @param target the target
	 * @return the p def magic creatures
	 */
	public final double getPDefMagicCreatures(final L2Character target)
	{
		return calcStat(Stats.PDEF_MCREATURES, 1, target, null);
	}
	
	/**
	 * Return the PAtk Modifier against giants.
	 * @param target the target
	 * @return the p atk giants
	 */
	public final double getPAtkGiants(final L2Character target)
	{
		return calcStat(Stats.PATK_GIANTS, 1, target, null);
	}
	
	/**
	 * Return the PAtk Modifier against magic creatures.
	 * @param target the target
	 * @return the p atk magic creatures
	 */
	public final double getPAtkMagicCreatures(final L2Character target)
	{
		return calcStat(Stats.PATK_MCREATURES, 1, target, null);
	}
}
