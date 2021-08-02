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

import java.text.NumberFormat;

import l2jorion.Config;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.managers.ClassDamageManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2SiegeClan;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2CubicInstance;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.conditions.ConditionPlayerState;
import l2jorion.game.skills.conditions.ConditionPlayerState.CheckPlayerState;
import l2jorion.game.skills.conditions.ConditionUsingItemType;
import l2jorion.game.skills.effects.EffectTemplate;
import l2jorion.game.skills.funcs.Func;
import l2jorion.game.templates.L2Armor;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.L2PcTemplate;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public final class Formulas
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2Character.class);
	
	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs
	
	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block
	
	static class FuncAddLevel3 extends Func
	{
		static final FuncAddLevel3[] _instancies = new FuncAddLevel3[Stats.NUM_STATS];
		
		static Func getInstance(final Stats stat)
		{
			final int pos = stat.ordinal();
			
			if (_instancies[pos] == null)
			{
				_instancies[pos] = new FuncAddLevel3(stat);
			}
			return _instancies[pos];
		}
		
		private FuncAddLevel3(final Stats pStat)
		{
			super(pStat, 0x10, null);
		}
		
		@Override
		public void calc(final Env env)
		{
			env.value += env.player.getLevel() / 3.0;
		}
	}
	
	static class FuncMultLevelMod extends Func
	{
		static final FuncMultLevelMod[] _instancies = new FuncMultLevelMod[Stats.NUM_STATS];
		
		static Func getInstance(final Stats stat)
		{
			final int pos = stat.ordinal();
			
			if (_instancies[pos] == null)
			{
				_instancies[pos] = new FuncMultLevelMod(stat);
			}
			return _instancies[pos];
		}
		
		private FuncMultLevelMod(final Stats pStat)
		{
			super(pStat, 0x20, null);
		}
		
		@Override
		public void calc(final Env env)
		{
			env.value *= env.player.getLevelMod();
		}
	}
	
	static class FuncMultRegenResting extends Func
	{
		static final FuncMultRegenResting[] _instancies = new FuncMultRegenResting[Stats.NUM_STATS];
		
		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			
			if (_instancies[pos] == null)
			{
				_instancies[pos] = new FuncMultRegenResting(stat);
			}
			
			return _instancies[pos];
		}
		
		private FuncMultRegenResting(Stats pStat)
		{
			super(pStat, 0x20, null);
			setCondition(new ConditionPlayerState(CheckPlayerState.RESTING, true));
		}
		
		@Override
		public void calc(Env env)
		{
			if (!cond.test(env))
			{
				return;
			}
			
			env.value *= 1.45;
		}
	}
	
	static class FuncPAtkMod extends Func
	{
		static final FuncPAtkMod _fpa_instance = new FuncPAtkMod();
		
		static Func getInstance()
		{
			return _fpa_instance;
		}
		
		private FuncPAtkMod()
		{
			super(Stats.POWER_ATTACK, 0x30, null);
		}
		
		@Override
		public void calc(Env env)
		{
			if (env.player instanceof L2PetInstance)
			{
				if (env.player.getActiveWeaponInstance() != null)
				{
					env.value *= BaseStats.STR.calcBonus(env.player);
				}
			}
			else
			{
				env.value *= BaseStats.STR.calcBonus(env.player) * env.player.getLevelMod();
			}
		}
	}
	
	static class FuncMAtkMod extends Func
	{
		static final FuncMAtkMod _fma_instance = new FuncMAtkMod();
		
		static Func getInstance()
		{
			return _fma_instance;
		}
		
		private FuncMAtkMod()
		{
			super(Stats.MAGIC_ATTACK, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			double intb = BaseStats.INT.calcBonus(env.player);
			double lvlb = env.player.getLevelMod();
			env.value *= (lvlb * lvlb) * (intb * intb);
		}
	}
	
	static class FuncMDefMod extends Func
	{
		static final FuncMDefMod _fmm_instance = new FuncMDefMod();
		
		static Func getInstance()
		{
			return _fmm_instance;
		}
		
		private FuncMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			if (env.player instanceof L2PcInstance)
			{
				L2PcInstance p = (L2PcInstance) env.player;
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
				{
					env.value -= 5;
				}
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
				{
					env.value -= 5;
				}
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
				{
					env.value -= 9;
				}
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
				{
					env.value -= 9;
				}
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
				{
					env.value -= 13;
				}
			}
			env.value *= BaseStats.MEN.calcBonus(env.player) * env.player.getLevelMod();
		}
	}
	
	static class FuncPDefMod extends Func
	{
		static final FuncPDefMod _fmm_instance = new FuncPDefMod();
		
		static Func getInstance()
		{
			return _fmm_instance;
		}
		
		private FuncPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			if (env.player instanceof L2PcInstance)
			{
				L2PcInstance p = (L2PcInstance) env.player;
				boolean hasMagePDef = (p.getClassId().isMage() || p.getClassId().getId() == 0x31); // orc mystics are a special case
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null)
				{
					env.value -= 12;
				}
				L2ItemInstance chest = p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				if (chest != null)
				{
					env.value -= hasMagePDef ? 15 : 31;
				}
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null || (chest != null && chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR))
				{
					env.value -= hasMagePDef ? 8 : 18;
				}
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null)
				{
					env.value -= 8;
				}
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) != null)
				{
					env.value -= 7;
				}
			}
			env.value *= env.player.getLevelMod();
		}
	}
	
	static class FuncBowAtkRange extends Func
	{
		private static final FuncBowAtkRange _fbar_instance = new FuncBowAtkRange();
		
		static Func getInstance()
		{
			return _fbar_instance;
		}
		
		private FuncBowAtkRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x10, null);
			setCondition(new ConditionUsingItemType(L2WeaponType.BOW.mask()));
		}
		
		@Override
		public void calc(Env env)
		{
			if (!cond.test(env))
			{
				return;
			}
			env.value += 460;
		}
	}
	
	static class FuncAtkAccuracy extends Func
	{
		static final FuncAtkAccuracy _faa_instance = new FuncAtkAccuracy();
		
		static Func getInstance()
		{
			return _faa_instance;
		}
		
		private FuncAtkAccuracy()
		{
			super(Stats.ACCURACY_COMBAT, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			final int level = env.player.getLevel();
			
			L2Character p = env.player;
			if (p instanceof L2PetInstance)
			{
				env.value += Math.sqrt(env.player.getDEX());
			}
			else
			{
				env.value += Math.sqrt(env.player.getDEX()) * 6;
				env.value += level;
				if (level > 77)
				{
					env.value += (level - 77);
				}
				if (level > 69)
				{
					env.value += (level - 69);
				}
				if (env.player instanceof L2Summon)
				{
					env.value += (level < 60) ? 4 : 5;
				}
			}
		}
	}
	
	static class FuncAtkEvasion extends Func
	{
		static final FuncAtkEvasion _fae_instance = new FuncAtkEvasion();
		
		static Func getInstance()
		{
			return _fae_instance;
		}
		
		private FuncAtkEvasion()
		{
			super(Stats.EVASION_RATE, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			final int level = env.player.getLevel();
			
			L2Character p = env.player;
			if (p instanceof L2PetInstance)
			{
				env.value += Math.sqrt(env.player.getDEX());
			}
			else
			{
				env.value += Math.sqrt(env.player.getDEX()) * 6;
				env.value += level;
				if (level > 77)
				{
					env.value += (level - 77);
				}
				if (level > 69)
				{
					env.value += (level - 69);
				}
			}
		}
	}
	
	static class FuncAtkCritical extends Func
	{
		static final FuncAtkCritical _fac_instance = new FuncAtkCritical();
		
		static Func getInstance()
		{
			return _fac_instance;
		}
		
		private FuncAtkCritical()
		{
			super(Stats.CRITICAL_RATE, 0x09, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= BaseStats.DEX.calcBonus(env.player);
			
			L2Character p = env.player;
			if (!(p instanceof L2PetInstance))
			{
				env.value *= 10;
			}
			
			env.baseValue = env.value;
		}
	}
	
	static class FuncMAtkCritical extends Func
	{
		static final FuncMAtkCritical _fac_instance = new FuncMAtkCritical();
		
		static Func getInstance()
		{
			return _fac_instance;
		}
		
		private FuncMAtkCritical()
		{
			super(Stats.MCRITICAL_RATE, 0x30, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			if (p instanceof L2Summon)
			{
				env.value = 8;
			}
			else if (p instanceof L2PcInstance && p.getActiveWeaponInstance() != null)
			{
				env.value *= BaseStats.WIT.calcBonus(p);
			}
		}
	}
	
	static class FuncMoveSpeed extends Func
	{
		static final FuncMoveSpeed _fms_instance = new FuncMoveSpeed();
		
		static Func getInstance()
		{
			return _fms_instance;
		}
		
		private FuncMoveSpeed()
		{
			super(Stats.RUN_SPEED, 0x30, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= BaseStats.DEX.calcBonus(env.player);
		}
	}
	
	static class FuncPAtkSpeed extends Func
	{
		static final FuncPAtkSpeed _fas_instance = new FuncPAtkSpeed();
		
		static Func getInstance()
		{
			return _fas_instance;
		}
		
		private FuncPAtkSpeed()
		{
			super(Stats.POWER_ATTACK_SPEED, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= BaseStats.DEX.calcBonus(env.player);
		}
	}
	
	static class FuncMAtkSpeed extends Func
	{
		static final FuncMAtkSpeed _fas_instance = new FuncMAtkSpeed();
		
		static Func getInstance()
		{
			return _fas_instance;
		}
		
		private FuncMAtkSpeed()
		{
			super(Stats.MAGIC_ATTACK_SPEED, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= BaseStats.WIT.calcBonus(env.player);
		}
	}
	
	static class FuncHennaSTR extends Func
	{
		static final FuncHennaSTR _fh_instance = new FuncHennaSTR();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaSTR()
		{
			super(Stats.STAT_STR, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
			{
				env.value += pc.getHennaStatSTR();
			}
		}
	}
	
	static class FuncHennaDEX extends Func
	{
		static final FuncHennaDEX _fh_instance = new FuncHennaDEX();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaDEX()
		{
			super(Stats.STAT_DEX, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
			{
				env.value += pc.getHennaStatDEX();
			}
		}
	}
	
	static class FuncHennaINT extends Func
	{
		static final FuncHennaINT _fh_instance = new FuncHennaINT();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaINT()
		{
			super(Stats.STAT_INT, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
			{
				env.value += pc.getHennaStatINT();
			}
		}
	}
	
	static class FuncHennaMEN extends Func
	{
		static final FuncHennaMEN _fh_instance = new FuncHennaMEN();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaMEN()
		{
			super(Stats.STAT_MEN, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
			{
				env.value += pc.getHennaStatMEN();
			}
		}
	}
	
	static class FuncHennaCON extends Func
	{
		static final FuncHennaCON _fh_instance = new FuncHennaCON();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaCON()
		{
			super(Stats.STAT_CON, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
			{
				env.value += pc.getHennaStatCON();
			}
		}
	}
	
	static class FuncHennaWIT extends Func
	{
		static final FuncHennaWIT _fh_instance = new FuncHennaWIT();
		
		static Func getInstance()
		{
			return _fh_instance;
		}
		
		private FuncHennaWIT()
		{
			super(Stats.STAT_WIT, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if (pc != null)
			{
				env.value += pc.getHennaStatWIT();
			}
		}
	}
	
	static class FuncMaxHpAdd extends Func
	{
		static final FuncMaxHpAdd _fmha_instance = new FuncMaxHpAdd();
		
		static Func getInstance()
		{
			return _fmha_instance;
		}
		
		private FuncMaxHpAdd()
		{
			super(Stats.MAX_HP, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.classBaseLevel;
			double hpmod = t.lvlHpMod * lvl;
			double hpmax = (t.lvlHpAdd + hpmod) * lvl;
			double hpmin = (t.lvlHpAdd * lvl) + hpmod;
			env.value += (hpmax + hpmin) / 2;
		}
	}
	
	static class FuncMaxHpMul extends Func
	{
		static final FuncMaxHpMul _fmhm_instance = new FuncMaxHpMul();
		
		static Func getInstance()
		{
			return _fmhm_instance;
		}
		
		private FuncMaxHpMul()
		{
			super(Stats.MAX_HP, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= BaseStats.CON.calcBonus(env.player);
		}
	}
	
	static class FuncMaxCpAdd extends Func
	{
		static final FuncMaxCpAdd _fmca_instance = new FuncMaxCpAdd();
		
		static Func getInstance()
		{
			return _fmca_instance;
		}
		
		private FuncMaxCpAdd()
		{
			super(Stats.MAX_CP, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.classBaseLevel;
			double cpmod = t.lvlCpMod * lvl;
			double cpmax = (t.lvlCpAdd + cpmod) * lvl;
			double cpmin = (t.lvlCpAdd * lvl) + cpmod;
			env.value += (cpmax + cpmin) / 2;
		}
	}
	
	static class FuncMaxCpMul extends Func
	{
		static final FuncMaxCpMul _fmcm_instance = new FuncMaxCpMul();
		
		static Func getInstance()
		{
			return _fmcm_instance;
		}
		
		private FuncMaxCpMul()
		{
			super(Stats.MAX_CP, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= BaseStats.CON.calcBonus(env.player);
		}
	}
	
	static class FuncMaxMpAdd extends Func
	{
		static final FuncMaxMpAdd _fmma_instance = new FuncMaxMpAdd();
		
		static Func getInstance()
		{
			return _fmma_instance;
		}
		
		private FuncMaxMpAdd()
		{
			super(Stats.MAX_MP, 0x10, null);
		}
		
		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.classBaseLevel;
			double mpmod = t.lvlMpMod * lvl;
			double mpmax = (t.lvlMpAdd + mpmod) * lvl;
			double mpmin = (t.lvlMpAdd * lvl) + mpmod;
			env.value += (mpmax + mpmin) / 2;
		}
	}
	
	static class FuncMaxMpMul extends Func
	{
		static final FuncMaxMpMul _fmmm_instance = new FuncMaxMpMul();
		
		static Func getInstance()
		{
			return _fmmm_instance;
		}
		
		private FuncMaxMpMul()
		{
			super(Stats.MAX_MP, 0x20, null);
		}
		
		@Override
		public void calc(Env env)
		{
			env.value *= BaseStats.MEN.calcBonus(env.player);
		}
	}
	
	private static final Formulas _instance = new Formulas();
	
	public static Formulas getInstance()
	{
		return _instance;
	}
	
	private Formulas()
	{
	}
	
	public static int getRegeneratePeriod(L2Character cha)
	{
		if (cha instanceof L2DoorInstance)
		{
			return HP_REGENERATE_PERIOD * 100; // 5 mins
		}
		
		return HP_REGENERATE_PERIOD; // 3s
	}
	
	public Calculator[] getStdNPCCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];
		
		std[Stats.MAX_HP.ordinal()] = new Calculator();
		std[Stats.MAX_HP.ordinal()].addFunc(FuncMaxHpMul.getInstance());
		
		std[Stats.MAX_MP.ordinal()] = new Calculator();
		std[Stats.MAX_MP.ordinal()].addFunc(FuncMaxMpMul.getInstance());
		
		std[Stats.POWER_ATTACK.ordinal()] = new Calculator();
		std[Stats.POWER_ATTACK.ordinal()].addFunc(FuncPAtkMod.getInstance());
		
		std[Stats.MAGIC_ATTACK.ordinal()] = new Calculator();
		std[Stats.MAGIC_ATTACK.ordinal()].addFunc(FuncMAtkMod.getInstance());
		
		std[Stats.POWER_DEFENCE.ordinal()] = new Calculator();
		std[Stats.POWER_DEFENCE.ordinal()].addFunc(FuncPDefMod.getInstance());
		
		std[Stats.MAGIC_DEFENCE.ordinal()] = new Calculator();
		std[Stats.MAGIC_DEFENCE.ordinal()].addFunc(FuncMDefMod.getInstance());
		
		std[Stats.CRITICAL_RATE.ordinal()] = new Calculator();
		std[Stats.CRITICAL_RATE.ordinal()].addFunc(FuncAtkCritical.getInstance());
		
		std[Stats.MCRITICAL_RATE.ordinal()] = new Calculator();
		std[Stats.MCRITICAL_RATE.ordinal()].addFunc(FuncMAtkCritical.getInstance());
		
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		
		std[Stats.POWER_ATTACK_SPEED.ordinal()] = new Calculator();
		std[Stats.POWER_ATTACK_SPEED.ordinal()].addFunc(FuncPAtkSpeed.getInstance());
		
		std[Stats.MAGIC_ATTACK_SPEED.ordinal()] = new Calculator();
		std[Stats.MAGIC_ATTACK_SPEED.ordinal()].addFunc(FuncMAtkSpeed.getInstance());
		
		std[Stats.RUN_SPEED.ordinal()] = new Calculator();
		std[Stats.RUN_SPEED.ordinal()].addFunc(FuncMoveSpeed.getInstance());
		
		return std;
	}
	
	public void addFuncsToNewCharacter(L2Character cha)
	{
		if (cha instanceof L2PcInstance)
		{
			cha.addStatFunc(FuncMaxHpAdd.getInstance());
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxCpAdd.getInstance());
			cha.addStatFunc(FuncMaxCpMul.getInstance());
			cha.addStatFunc(FuncMaxMpAdd.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			cha.addStatFunc(FuncBowAtkRange.getInstance());
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			cha.addStatFunc(FuncHennaSTR.getInstance());
			cha.addStatFunc(FuncHennaDEX.getInstance());
			cha.addStatFunc(FuncHennaINT.getInstance());
			cha.addStatFunc(FuncHennaMEN.getInstance());
			cha.addStatFunc(FuncHennaCON.getInstance());
			cha.addStatFunc(FuncHennaWIT.getInstance());
		}
		else if (cha instanceof L2PetInstance)
		{
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
		}
		else if (cha instanceof L2Summon)
		{
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
		}
	}
	
	public final static double calcHpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseHpReg;
		double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;
		
		if (Config.L2JMOD_CHAMPION_ENABLE && cha.isChampion())
		{
			hpRegenMultiplier *= Config.L2JMOD_CHAMPION_HP_REGEN;
		}
		
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			
			init += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;
			
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				hpRegenMultiplier *= Formulas.calcFestivalRegenModifier(player);
			}
			else
			{
				double siegeModifier = Formulas.calcSiegeRegenModifer(player);
				if (siegeModifier > 0)
				{
					hpRegenMultiplier *= siegeModifier;
				}
			}
			
			if (player.isInsideZone(ZoneId.ZONE_CLANHALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHasHideout();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
					{
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.ZONE_MOTHERTREE))
			{
				hpRegenBonus += 2;
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				hpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				hpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				hpRegenMultiplier *= 0.7; // Running
			}
			
			init *= cha.getLevelMod() * BaseStats.CON.calcBonus(cha);
		}
		
		if (init < 1)
		{
			init = 1;
		}
		
		return cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null) * hpRegenMultiplier + hpRegenBonus;
	}
	
	public final static double calcMpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseMpReg;
		double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;
		
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			
			// Calculate correct baseMpReg value for certain level of PC
			init += 0.3 * ((player.getLevel() - 1) / 10.0);
			
			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				mpRegenMultiplier *= calcFestivalRegenModifier(player);
			}
			
			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.ZONE_MOTHERTREE))
			{
				mpRegenBonus += 1;
			}
			
			if (player.isInsideZone(ZoneId.ZONE_CLANHALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHasHideout();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
					{
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
						}
					}
				}
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				mpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				mpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				mpRegenMultiplier *= 0.7; // Running
			}
			
			// Add MEN bonus
			init *= cha.getLevelMod() * BaseStats.MEN.calcBonus(cha);
		}
		
		if (init < 1)
		{
			init = 1;
		}
		
		return cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null) * mpRegenMultiplier + mpRegenBonus;
	}
	
	/**
	 * Calculate the CP regen rate (base + modifiers).<BR>
	 * <BR>
	 * @param cha
	 * @return
	 */
	public final static double calcCpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseHpReg;
		double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
		double cpRegenBonus = 0;
		
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			
			// Calculate correct baseHpReg value for certain level of PC
			init += player.getLevel() > 10 ? (player.getLevel() - 1) / 10.0 : 0.5;
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				cpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				cpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				cpRegenMultiplier *= 0.7; // Running
			}
		}
		else
		{
			// Calculate Movement bonus
			if (!cha.isMoving())
			{
				cpRegenMultiplier *= 1.1; // Staying
			}
			else if (cha.isRunning())
			{
				cpRegenMultiplier *= 0.7; // Running
			}
		}
		
		// Apply CON bonus
		init *= cha.getLevelMod() * BaseStats.CON.calcBonus(cha);
		if (init < 1)
		{
			init = 1;
		}
		
		return cha.calcStat(Stats.REGENERATE_CP_RATE, init, null, null) * cpRegenMultiplier + cpRegenBonus;
	}
	
	public final static double calcFestivalRegenModifier(L2PcInstance activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;
		
		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0)
		{
			return 0;
		}
		
		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		if (oracle == SevenSigns.CABAL_DAWN)
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		}
		else
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		}
		
		// Check the distance between the player and the player spawn point, in the center of the arena.
		double distToCenter = activeChar.getDistance(festivalCenter[0], festivalCenter[1]);
		
		if (Config.DEBUG)
		{
			LOG.info("Distance: " + distToCenter + ", RegenMulti: " + distToCenter * 2.5 / 50);
		}
		
		return 1.0 - distToCenter * 0.0005; // Maximum Decreased Regen of ~ -65%;
	}
	
	public final static double calcSiegeRegenModifer(L2PcInstance activeChar)
	{
		if (activeChar == null || activeChar.getClan() == null)
		{
			return 0;
		}
		
		Siege siege = SiegeManager.getInstance().getSiege(activeChar.getPosition().getX(), activeChar.getPosition().getY(), activeChar.getPosition().getZ());
		if (siege == null || !siege.getIsInProgress())
		{
			return 0;
		}
		
		L2SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getClanId());
		if (siegeClan == null || siegeClan.getFlag().size() == 0 || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().get(0), true))
		{
			return 0;
		}
		
		return 1.5; // If all is true, then modifer will be 50% more
	}
	
	public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, boolean shld, boolean crit, boolean ss)
	{
		final boolean isPvE = attacker instanceof L2PlayableInstance && target instanceof L2Attackable;
		
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		
		if (ss)
		{
			damage *= 2.;
		}
		
		if (shld)
		{
			defence += target.getShldDef();
		}
		
		if (Config.EXPLLOSIVE_CUSTOM)
		{
			if (isPvE)
			{
				defence *= target.calcStat(Stats.PVE_PHYS_SKILL_DEF, 1, null, null);
				damage *= attacker.calcStat(Stats.PVE_PHYS_SKILL_DMG, 1, null, null);
			}
		}
		
		if (crit)
		{
			double improvedDamageByCriticalVuln = target.calcStat(Stats.CRIT_VULN, damage, target, skill);
			double improvedDamageByCriticalVulnAndAdd = (attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, improvedDamageByCriticalVuln, target, skill));
			
			damage = improvedDamageByCriticalVulnAndAdd;
			
			L2Effect vicious = attacker.getFirstEffect(312);
			if (vicious != null && damage > 1)
			{
				for (Func func : vicious.getStatFuncs())
				{
					Env env = new Env();
					env.player = attacker;
					env.target = target;
					env.skill = skill;
					env.value = damage;
					func.calc(env);
					damage = (int) env.value;
				}
			}
			
		}
		
		// skill add is not influenced by criticals improvements, so it's applied later
		double skillpower = skill.getPower(attacker);
		float ssboost = skill.getSSBoost();
		if (ssboost <= 0)
		{
			damage += skillpower;
		}
		else if (ssboost > 0)
		{
			if (ss)
			{
				skillpower *= ssboost;
				damage += skillpower;
			}
			else
			{
				damage += skillpower;
			}
		}
		
		if (Formulas.calcCrit(skill.getBaseCritRate() * 10 * BaseStats.DEX.calcBonus(attacker)))
		{
			damage *= 2;
		}
		
		damage *= 70. / defence;
		
		// finally, apply the critical multiplier if present (it's not subjected to defense)
		if (crit)
		{
			damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, damage, target, skill);
		}
		
		// get the vulnerability for the instance due to skills (buffs, passives, toggles, etc)
		damage = target.calcStat(Stats.DAGGER_WPN_VULN, damage, target, null);
		// get the natural vulnerability for the template
		if (target instanceof L2NpcInstance)
		{
			damage *= ((L2NpcInstance) target).getTemplate().getVulnerability(Stats.DAGGER_WPN_VULN);
		}
		
		// Weapon random damage
		damage *= attacker.getRandomDamageMultiplier();
		
		// After C4 nobles make 4% more dmg in PvP.
		if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isNoble() && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			damage *= 1.04;
		}
		
		// Sami: Must be removed, after armor resistances are checked.
		// These values are a quick fix to balance dagger gameplay and give
		// armor resistances vs dagger. daggerWpnRes could also be used if a skill
		// was given to all classes. The values here try to be a compromise.
		// They were originally added in a late C4 rev (2289).
		if (target instanceof L2PcInstance)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if (armor != null)
			{
				if (((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_HEAVY;
				}
				if (((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_LIGHT;
				}
				if (((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_ROBE;
				}
			}
		}
		
		if (Config.ENABLE_CLASS_DAMAGES && attacker instanceof L2PcInstance && target instanceof L2PcInstance)
		{
			if (((L2PcInstance) attacker).isInOlympiadMode() && ((L2PcInstance) target).isInOlympiadMode())
			{
				if (Config.ENABLE_CLASS_DAMAGES_IN_OLY)
				{
					damage = damage * ClassDamageManager.getDamageMultiplier((L2PcInstance) attacker, (L2PcInstance) target);
				}
				
			}
			else
			{
				damage = damage * ClassDamageManager.getDamageMultiplier((L2PcInstance) attacker, (L2PcInstance) target);
			}
		}
		return damage < 1 ? 1. : damage;
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target, called separatly for each weapon, if dual-weapon is used.
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill
	 * @param shld
	 * @param crit if the ATTACK have critical success
	 * @param dual if dual weapon is used
	 * @param ss if weapon item was charged by soulshot
	 * @return damage points
	 */
	public final static double calcPhysDam(L2Character attacker, L2Character target, L2Skill skill, boolean shld, boolean crit, boolean dual, boolean ss)
	{
		final boolean isPvE = attacker instanceof L2PlayableInstance && target instanceof L2Attackable;
		
		if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
			{
				return 0;
			}
		}
		
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		
		// Defense bonuses in PvP fight
		if (Config.EXPLLOSIVE_CUSTOM)
		{
			if (isPvE)
			{
				if (skill == null)
				{
					defence *= target.calcStat(Stats.PVE_PHYSICAL_DEF, 1, null, null);
				}
				
				if (skill != null)
				{
					defence *= target.calcStat(Stats.PVE_PHYS_SKILL_DEF, 1, null, null);
					damage *= attacker.calcStat(Stats.PVE_PHYS_SKILL_DMG, 1, null, null);
				}
				
				final L2Weapon weapon = attacker.getActiveWeaponItem();
				if ((weapon != null) && ((weapon.getItemType() == L2WeaponType.BOW)))
				{
					defence *= target.calcStat(Stats.PVE_BOW_DEF, 1, null, null);
					
					if (skill != null)
					{
						defence *= target.calcStat(Stats.PVE_BOW_SKILL_DEF, 1, null, null);
						damage *= attacker.calcStat(Stats.PVE_BOW_SKILL_DMG, 1, null, null);
					}
					else
					{
						defence *= target.calcStat(Stats.PVE_BOW_SKILL_DEF, 1, null, null);
						damage *= attacker.calcStat(Stats.PVE_BOW_DMG, 1, null, null);
					}
				}
				else
				{
					damage *= attacker.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null);
				}
			}
		}
		
		if (ss)
		{
			damage *= 2;
		}
		
		if (skill != null)
		{
			double skillpower = skill.getPower(attacker);
			float ssboost = skill.getSSBoost();
			if (ssboost <= 0)
			{
				damage += skillpower;
			}
			else if (ssboost > 0)
			{
				if (ss)
				{
					skillpower *= ssboost;
					damage += skillpower;
				}
				else
				{
					damage += skillpower;
				}
			}
		}
		
		// In C5 summons make 10 % less dmg in PvP.
		if (attacker instanceof L2Summon && target instanceof L2PcInstance)
		{
			damage *= 0.9;
		}
		
		// After C4 nobles make 4% more dmg in PvP.
		if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isNoble() && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			damage *= 1.04;
		}
		
		// defence modifier depending of the attacker weapon
		L2Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if (weapon != null)
		{
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC:
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST:
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
			}
		}
		
		if (crit)
		{
			// Finally retail like formula
			double cAtkMultiplied = damage + attacker.calcStat(Stats.CRITICAL_DAMAGE, damage, target, skill);
			double cAtkVuln = target.calcStat(Stats.CRIT_VULN, 1, target, null);
			double improvedDamageByCriticalMulAndVuln = cAtkMultiplied * cAtkVuln;
			double improvedDamageByCriticalMulAndAdd = improvedDamageByCriticalMulAndVuln + attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill);
			
			if (Config.DEBUG)
			{
				LOG.info("Attacker '" + attacker.getName() + "' Critical Damage Debug:");
				LOG.info("	-	Initial Damage:  " + damage);
				LOG.info("	-	Damage increased of mult:  " + cAtkMultiplied);
				LOG.info("	-	cAtkVuln Mult:  " + cAtkVuln);
				LOG.info("	-	improvedDamageByCriticalMulAndVuln: " + improvedDamageByCriticalMulAndVuln);
				LOG.info("	-	improvedDamageByCriticalMulAndAdd: " + improvedDamageByCriticalMulAndAdd);
			}
			
			damage = improvedDamageByCriticalMulAndAdd;
			
		}
		
		if (shld && !Config.ALT_GAME_SHIELD_BLOCKS)
		{
			defence += target.getShldDef();
		}
		
		damage = 70 * damage / defence;
		
		if (stat != null)
		{
			// get the vulnerability due to skills (buffs, passives, toggles, etc)
			damage = target.calcStat(stat, damage, target, null);
			if (target instanceof L2NpcInstance)
			{
				// get the natural vulnerability for the template
				damage *= ((L2NpcInstance) target).getTemplate().getVulnerability(stat);
			}
		}
		
		damage += Rnd.nextDouble() * damage / 10;
		
		if (shld && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
			{
				damage = 0;
			}
		}
		
		if (target instanceof L2PcInstance && weapon != null && weapon.getItemType() == L2WeaponType.DAGGER && skill != null)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if (armor != null)
			{
				if (((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_HEAVY;
				}
				if (((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_LIGHT;
				}
				if (((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= Config.ALT_DAGGER_DMG_VS_ROBE;
				}
			}
		}
		
		if (attacker instanceof L2NpcInstance)
		{
			// Skill Race : Undead
			if (((L2NpcInstance) attacker).getTemplate().getRace() == L2NpcTemplate.Race.UNDEAD)
			{
				damage /= attacker.getPDefUndead(target);
			}
			
			if (((L2NpcInstance) attacker).getTemplate().getRace() == L2NpcTemplate.Race.PLANT)
			{
				damage /= attacker.getPDefPlants(target);
			}
			
			if (((L2NpcInstance) attacker).getTemplate().getRace() == L2NpcTemplate.Race.BUG)
			{
				damage /= attacker.getPDefInsects(target);
			}
			
			if (((L2NpcInstance) attacker).getTemplate().getRace() == L2NpcTemplate.Race.ANIMAL)
			{
				damage /= attacker.getPDefAnimals(target);
			}
			
			if (((L2NpcInstance) attacker).getTemplate().getRace() == L2NpcTemplate.Race.BEAST)
			{
				damage /= attacker.getPDefMonsters(target);
			}
			
			if (((L2NpcInstance) attacker).getTemplate().getRace() == L2NpcTemplate.Race.DRAGON)
			{
				damage /= attacker.getPDefDragons(target);
			}
		}
		
		if (target instanceof L2NpcInstance)
		{
			switch (((L2NpcInstance) target).getTemplate().getRace())
			{
				case UNDEAD:
					damage *= attacker.getPAtkUndead(target);
					break;
				case BEAST:
					damage *= attacker.getPAtkMonsters(target);
					break;
				case ANIMAL:
					damage *= attacker.getPAtkAnimals(target);
					break;
				case PLANT:
					damage *= attacker.getPAtkPlants(target);
					break;
				case DRAGON:
					damage *= attacker.getPAtkDragons(target);
					break;
				case ANGEL:
					damage *= attacker.getPAtkAngels(target);
					break;
				case BUG:
					damage *= attacker.getPAtkInsects(target);
					break;
				default:
					break;
			}
		}
		
		if (shld)
		{
			if (100 - Config.ALT_PERFECT_SHLD_BLOCK < Rnd.get(100))
			{
				damage = 1;
				target.sendPacket(new SystemMessage(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS));
			}
		}
		
		if (damage > 0 && damage < 1)
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}
		
		// Dmg bonusses in PvP fight
		if ((attacker instanceof L2PcInstance || attacker instanceof L2Summon) && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			if (skill == null)
			{
				damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			}
		}
		
		if (attacker instanceof L2PcInstance)
		{
			if (((L2PcInstance) attacker).getClassId().isMage())
			{
				damage = damage * Config.ALT_MAGES_PHYSICAL_DAMAGE_MULTI;
			}
			else
			{
				damage = damage * Config.ALT_FIGHTERS_PHYSICAL_DAMAGE_MULTI;
			}
		}
		else if (attacker instanceof L2Summon)
		{
			damage = damage * Config.ALT_PETS_PHYSICAL_DAMAGE_MULTI;
		}
		else if (attacker instanceof L2NpcInstance)
		{
			damage = damage * Config.ALT_NPC_PHYSICAL_DAMAGE_MULTI;
		}
		
		if (Config.ENABLE_CLASS_DAMAGES && attacker instanceof L2PcInstance && target instanceof L2PcInstance)
		{
			if (((L2PcInstance) attacker).isInOlympiadMode() && ((L2PcInstance) target).isInOlympiadMode())
			{
				if (Config.ENABLE_CLASS_DAMAGES_IN_OLY)
				{
					damage = damage * ClassDamageManager.getDamageMultiplier((L2PcInstance) attacker, (L2PcInstance) target);
				}
				
			}
			else
			{
				damage = damage * ClassDamageManager.getDamageMultiplier((L2PcInstance) attacker, (L2PcInstance) target);
				
			}
		}
		return damage;
	}
	
	public final static double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss, boolean mcrit)
	{
		final boolean isPvE = attacker instanceof L2PlayableInstance && target instanceof L2Attackable;
		// Add Matk/Mdef Bonus
		int ssModifier = 1;
		// Add Bonus for Sps/SS
		if (attacker instanceof L2Summon && !(attacker instanceof L2PetInstance))
		{
			if (bss)
			{
				ssModifier = 4;
			}
			else if (ss)
			{
				ssModifier = 2;
			}
			
		}
		else
		{
			L2ItemInstance weapon = attacker.getActiveWeaponInstance();
			if (weapon != null)
			{
				if (bss)
				{
					ssModifier = 4;
				}
				else if (ss)
				{
					ssModifier = 2;
				}
			}
		}
		
		if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
			{
				return 0;
			}
		}
		
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		
		if (Config.EXPLLOSIVE_CUSTOM)
		{
			if (isPvE)
			{
				mDef *= target.calcStat(Stats.PVE_MAGICAL_DEF, 1, null, null);
			}
		}
		
		// apply ss bonus
		mAtk *= ssModifier;
		
		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker) * calcSkillVulnerability(target, skill, skill.getSkillType());
		
		// In C5 summons make 10 % less dmg in PvP.
		if (attacker instanceof L2Summon && target instanceof L2PcInstance)
		{
			damage *= 0.9;
		}
		
		// After C4 nobles make 4% more dmg in PvP.
		if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isNoble() && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			damage *= 1.04;
		}
		
		if (Config.EXPLLOSIVE_CUSTOM)
		{
			if (isPvE)
			{
				if (skill.isMagic())
				{
					damage *= attacker.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
				}
				else
				{
					damage *= attacker.calcStat(Stats.PVE_PHYS_SKILL_DMG, 1, null, null);
				}
			}
		}
		
		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker instanceof L2PcInstance)
			{
				if (calcMagicSuccess(attacker, target, skill) && target.getLevel() - attacker.getLevel() <= 9)
				{
					if (skill.getSkillType() == SkillType.DRAIN)
					{
						attacker.sendPacket(new SystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
					}
					else
					{
						attacker.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
					}
					
					damage /= 2;
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
					sm.addString(target.getName());
					sm.addSkillName(skill.getId());
					attacker.sendPacket(sm);
					
					damage = 1;
				}
			}
			
			if (target instanceof L2PcInstance)
			{
				if (skill.getSkillType() == SkillType.DRAIN)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.RESISTED_S1_DRAIN);
					sm.addString(attacker.getName());
					target.sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.RESISTED_S1_MAGIC);
					sm.addString(attacker.getName());
					target.sendPacket(sm);
				}
			}
		}
		else if (mcrit)
		{
			// damage *= 4;
			damage *= Config.MAGIC_CRITICAL_POWER;
		}
		
		// Pvp bonusses for dmg
		if ((attacker instanceof L2PcInstance || attacker instanceof L2Summon) && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			if (skill.isMagic())
			{
				damage *= attacker.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			}
		}
		
		if (attacker instanceof L2PcInstance)
		{
			if (((L2PcInstance) attacker).getClassId().isMage())
			{
				damage = damage * Config.ALT_MAGES_MAGICAL_DAMAGE_MULTI;
			}
			else
			{
				damage = damage * Config.ALT_FIGHTERS_MAGICAL_DAMAGE_MULTI;
			}
		}
		else if (attacker instanceof L2Summon)
		{
			damage = damage * Config.ALT_PETS_MAGICAL_DAMAGE_MULTI;
		}
		else if (attacker instanceof L2NpcInstance)
		{
			damage = damage * Config.ALT_NPC_MAGICAL_DAMAGE_MULTI;
		}
		
		if (target instanceof L2PlayableInstance)
		{
			damage *= skill.getPvpMulti();
		}
		
		if (skill.getSkillType() == SkillType.DEATHLINK)
		{
			damage = damage * (1.0 - attacker.getStatus().getCurrentHp() / attacker.getMaxHp()) * 2.0;
		}
		
		if (Config.ENABLE_CLASS_DAMAGES && attacker instanceof L2PcInstance && target instanceof L2PcInstance)
		{
			if (((L2PcInstance) attacker).isInOlympiadMode() && ((L2PcInstance) target).isInOlympiadMode())
			{
				if (Config.ENABLE_CLASS_DAMAGES_IN_OLY)
				{
					damage = damage * ClassDamageManager.getDamageMultiplier((L2PcInstance) attacker, (L2PcInstance) target);
				}
			}
			else
			{
				damage = damage * ClassDamageManager.getDamageMultiplier((L2PcInstance) attacker, (L2PcInstance) target);
			}
		}
		return damage;
	}
	
	public static final double calcMagicDam(L2CubicInstance attacker, L2Character target, L2Skill skill, boolean mcrit)
	{
		double damage = calcMagicDam(attacker.getOwner(), target, skill, false, false, mcrit);
		return damage;
	}
	
	/**
	 * @param rate
	 * @return true in case of critical hit
	 */
	public final static boolean calcCrit(double rate)
	{
		return rate > Rnd.get(1000);
	}
	
	/**
	 * Calcul value of blow success
	 * @param activeChar
	 * @param target
	 * @param chance
	 * @return
	 */
	public final boolean calcBlow(L2Character activeChar, L2Character target, int chance)
	{
		return activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 20) / 100), target, null) > Rnd.get(100);
	}
	
	/**
	 * Calcul value of lethal chance
	 * @param activeChar
	 * @param target
	 * @param baseLethal
	 * @param magiclvl
	 * @return
	 */
	public static final double calcLethal(L2Character activeChar, L2Character target, int baseLethal, int magiclvl)
	{
		double chance = 0;
		if (magiclvl > 0)
		{
			int delta = ((magiclvl + activeChar.getLevel()) / 2) - 1 - target.getLevel();
			
			if (delta >= -3)
			{
				chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
			}
			else if ((delta < -3) && (delta >= -9))
			{
				chance = (-3) * (baseLethal / (delta));
			}
			else
			{
				chance = baseLethal / 15;
			}
		}
		else
		{
			chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
		}
		
		return 10 * activeChar.calcStat(Stats.LETHAL_RATE, chance, target, null);
	}
	
	public static final boolean calcLethalHit(final L2Character activeChar, final L2Character target, final L2Skill skill)
	{
		if ((target.isRaid() && Config.ALLOW_RAID_LETHAL) || (!target.isRaid() && !(target instanceof L2DoorInstance) && !(Config.ALLOW_LETHAL_PROTECTION_MOBS && target instanceof L2NpcInstance && (Config.LIST_LETHAL_PROTECTED_MOBS.contains(((L2NpcInstance) target).getNpcId())))))
		{
			if ((!target.isRaid() || Config.ALLOW_RAID_LETHAL) && !(target instanceof L2DoorInstance) && !(target instanceof L2NpcInstance && ((L2NpcInstance) target).getNpcId() == 35062)
				&& !(Config.ALLOW_LETHAL_PROTECTION_MOBS && target instanceof L2NpcInstance && (Config.LIST_LETHAL_PROTECTED_MOBS.contains(((L2NpcInstance) target).getNpcId()))))
			{
				final int chance = Rnd.get(1000);
				// 1nd lethal set CP to 1
				// 2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1)
				if ((skill.getLethalChance2() > 0) && chance < calcLethal(activeChar, target, skill.getLethalChance2(), skill.getMagicLevel()))
				{
					if (target instanceof L2NpcInstance)
					{
						target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar);
						return true;
					}
					else if (target instanceof L2PcInstance) // If is a active player set his HP and CP to 1
					{
						final L2PcInstance player = (L2PcInstance) target;
						if (!player.isInvul())
						{
							if (!(activeChar instanceof L2PcInstance && (((L2PcInstance) activeChar).isGM() && !((L2PcInstance) activeChar).getAccessLevel().canGiveDamage())))
							{
								player.setCurrentHp(1);
								player.setCurrentCp(1);
								player.sendPacket(SystemMessageId.LETHAL_STRIKE);
							}
						}
					}
					activeChar.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
					return true;
				}
				
				if ((skill.getLethalChance1() > 0) && chance < calcLethal(activeChar, target, skill.getLethalChance1(), skill.getMagicLevel()))
				{
					if (target instanceof L2NpcInstance)
					{
						target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar);
					}
					else if (target instanceof L2PcInstance)
					{
						final L2PcInstance player = (L2PcInstance) target;
						if (!player.isInvul())
						{
							if (!(activeChar instanceof L2PcInstance && (((L2PcInstance) activeChar).isGM() && !((L2PcInstance) activeChar).getAccessLevel().canGiveDamage())))
							{
								player.setCurrentCp(1); // Set CP to 1
								player.sendPacket(SystemMessage.sendString("Combat points disappear when hit with a half kill skill"));
								player.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE));
							}
						}
					}
					activeChar.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
					return true;
				}
			}
		}
		
		return true;
	}
	
	public final static boolean calcMCrit(double mRate)
	{
		return mRate > Rnd.get(1000);
	}
	
	/**
	 * @param target
	 * @param dmg
	 * @return true in case when ATTACK is canceled due to hit
	 */
	public final static boolean calcAtkBreak(L2Character target, double dmg)
	{
		if (target instanceof L2PcInstance)
		{
			if (((L2PcInstance) target).getForceBuff() != null)
			{
				return true;
			}
		}
		double init = 0;
		
		if (Config.ALT_GAME_CANCEL_CAST && target.isCastingNow())
		{
			init = 15;
		}
		
		if (Config.ALT_GAME_CANCEL_BOW && target.isAttackingNow())
		{
			L2Weapon wpn = target.getActiveWeaponItem();
			if (wpn != null && wpn.getItemType() == L2WeaponType.BOW)
			{
				init = 15;
			}
		}
		
		if (target.isRaid() || target.isInvul() || init <= 0)
		{
			return false; // No attack break
		}
		
		// Chance of break is higher with higher dmg
		init += Math.sqrt(13 * dmg);
		
		// Chance is affected by target MEN
		init -= (BaseStats.MEN.calcBonus(target) * 100 - 100);
		
		// Calculate all modifiers for ATTACK_CANCEL
		double rate = target.calcStat(Stats.ATTACK_CANCEL, init, null, null);
		
		// Adjust the rate to be between 1 and 99
		if (rate > 99)
		{
			rate = 99;
		}
		else if (rate < 1)
		{
			rate = 1;
		}
		
		return Rnd.get(100) < rate;
	}
	
	/**
	 * Calculate delay (in milliseconds) before next ATTACK
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return
	 */
	public final int calcPAtkSpd(L2Character attacker, L2Character target, double rate)
	{
		if (rate < 2)
		{
			return 2700;
		}
		
		return (int) (470000 / rate);
	}
	
	/**
	 * Calculate delay (in milliseconds) for skills cast
	 * @param attacker
	 * @param target
	 * @param skill
	 * @param skillTime
	 * @return
	 */
	public final int calcMAtkSpd(L2Character attacker, L2Character target, L2Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			return (int) (skillTime * 333 / attacker.getMAtkSpd());
		}
		return (int) (skillTime * 333 / attacker.getPAtkSpd());
	}
	
	/**
	 * Calculate delay (in milliseconds) for skills cast
	 * @param attacker
	 * @param skill
	 * @param skillTime
	 * @return
	 */
	public final int calcMAtkSpd(L2Character attacker, L2Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			return (int) (skillTime * 333 / attacker.getMAtkSpd());
		}
		return (int) (skillTime * 333 / attacker.getPAtkSpd());
	}
	
	/**
	 * @param attacker
	 * @param target
	 * @return true if hit missed (taget evaded)
	 */
	// old
	public static boolean calcHitMiss(L2Character attacker, L2Character target)
	{
		int chance = (80 + (2 * (attacker.getAccuracy() - target.getEvasionRate(attacker)))) * 10;
		// Get additional bonus from the conditions when you are attacking
		chance *= hitConditionBonus.getConditionBonus(attacker, target);
		
		chance = Math.max(chance, 200);
		chance = Math.min(chance, 980);
		
		/*
		 * if (attacker instanceof L2PcInstance) { Announcements _a = Announcements.getInstance(); _a.sys(attacker.getName()+" chance:"+chance+"/1000"); }
		 */
		
		return chance < Rnd.get(1000);
	}
	
	/**
	 * @param attacker
	 * @param target
	 * @return true if shield defence successful
	 */
	public static boolean calcShldUse(L2Character attacker, L2Character target)
	{
		L2Weapon at_weapon = attacker.getActiveWeaponItem();
		double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, null) * BaseStats.DEX.calcBonus(target);
		if (shldRate == 0.0)
		{
			return false;
		}
		
		int degreeside = (int) target.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null);
		if (degreeside < 360 && (!target.isFacing(attacker, degreeside)))
		{
			return false;
		}
		
		// Check for passive skill Aegis (316) or Aegis Stance (318)
		// Like L2OFF you can't parry if your target is behind you
		if (target.getKnownSkill(316) == null && target.getFirstEffect(318) == null)
		{
			if (target.isBehind(attacker) || !target.isFront(attacker) || !attacker.isFront(target))
			{
				return false;
			}
		}
		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		if (at_weapon != null && at_weapon.getItemType() == L2WeaponType.BOW)
		{
			shldRate *= 1.3;
		}
		return shldRate > Rnd.get(100);
	}
	
	public boolean calcMagicAffected(L2Character actor, L2Character target, L2Skill skill)
	{
		SkillType type = skill.getSkillType();
		double defence = 0;
		if (skill.isActive() && skill.isOffensive())
		{
			defence = target.getMDef(actor, skill);
		}
		
		double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(target, skill, skill.getSkillType());
		double d = (attack - defence) / (attack + defence);
		
		if (target.isRaid()
			&& (type == SkillType.CONFUSION || type == SkillType.MUTE || type == SkillType.PARALYZE || type == SkillType.SLOW || type == SkillType.ROOT || type == SkillType.FEAR || type == SkillType.SLEEP || type == SkillType.STUN || type == SkillType.DEBUFF || type == SkillType.AGGDEBUFF))
		{
			if (d > 0 && Rnd.get(1000) == 1)
			{
				return true;
			}
			
			return false;
		}
		
		if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 && skill.is_Debuff())
		{
			return false;
		}
		
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	public static double calcSkillVulnerability(L2Character target, L2Skill skill, SkillType type)
	{
		double multiplier = 1;
		
		if (skill != null)
		{
			Stats stat = skill.getStat();
			if (stat != null)
			{
				switch (stat)
				{
					case AGGRESSION:
						multiplier = target.getTemplate().baseAggressionVuln;
						break;
					case BLEED:
						multiplier = target.getTemplate().baseBleedVuln;
						break;
					case POISON:
						multiplier = target.getTemplate().basePoisonVuln;
						break;
					case STUN:
						multiplier = target.getTemplate().baseStunVuln;
						break;
					case ROOT:
						multiplier = target.getTemplate().baseRootVuln;
						break;
					case MOVEMENT:
						multiplier = target.getTemplate().baseMovementVuln;
						break;
					case CONFUSION:
						multiplier = target.getTemplate().baseConfusionVuln;
						break;
					case SLEEP:
						multiplier = target.getTemplate().baseSleepVuln;
						break;
					case FIRE:
						multiplier = target.getTemplate().baseFireVuln;
						break;
					case WIND:
						multiplier = target.getTemplate().baseWindVuln;
						break;
					case WATER:
						multiplier = target.getTemplate().baseWaterVuln;
						break;
					case EARTH:
						multiplier = target.getTemplate().baseEarthVuln;
						break;
					case HOLY:
						multiplier = target.getTemplate().baseHolyVuln;
						break;
					case DARK:
						multiplier = target.getTemplate().baseDarkVuln;
						break;
					default:
						multiplier = 1;
				}
			}
			
			switch (skill.getElement())
			{
				case L2Skill.ELEMENT_EARTH:
					multiplier = target.calcStat(Stats.EARTH_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_FIRE:
					multiplier = target.calcStat(Stats.FIRE_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_WATER:
					multiplier = target.calcStat(Stats.WATER_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_WIND:
					multiplier = target.calcStat(Stats.WIND_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_HOLY:
					multiplier = target.calcStat(Stats.HOLY_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_DARK:
					multiplier = target.calcStat(Stats.DARK_VULN, multiplier, target, skill);
					break;
			}
			
			if (type != null)
			{
				switch (type)
				{
					case BLEED:
						multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
						break;
					case POISON:
						multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
						break;
					case STUN:
						multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
						break;
					case PARALYZE:
						multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
						break;
					case ROOT:
						multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
						break;
					case SLEEP:
						multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
						break;
					case MUTE:
					case FEAR:
					case BETRAY:
					case AGGREDUCE_CHAR:
						multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
						break;
					case CONFUSION:
						multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, target, null);
						break;
					case DEBUFF:
					case WEAKNESS:
					case GLOOM:
					case SURRENDER:
					case HEX:
					case SLOW:
					case DOD:
						multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
						break;
					case BUFF:
						multiplier = target.calcStat(Stats.BUFF_VULN, multiplier, target, null);
						break;
				}
			}
		}
		return multiplier;
	}
	
	public static double calcSkillProficiency(L2Skill skill, L2Character attacker, L2Character target)
	{
		double multiplier = 1;
		
		if (skill != null)
		{
			// Calculate skilltype vulnerabilities
			SkillType type = skill.getSkillType();
			
			// For additional effects on PDAM and MDAM skills (like STUN, SHOCK, PARALYZE...)
			if (type != null && (type == SkillType.PDAM || type == SkillType.MDAM))
			{
				type = skill.getEffectType();
			}
			
			multiplier = calcSkillTypeProficiency(multiplier, attacker, target, type);
		}
		
		return multiplier;
	}
	
	public static double calcSkillTypeProficiency(double multiplier, L2Character attacker, L2Character target, SkillType type)
	{
		if (type != null)
		{
			switch (type)
			{
				case BLEED:
					multiplier = attacker.calcStat(Stats.BLEED_PROF, multiplier, target, null);
					break;
				case POISON:
					multiplier = attacker.calcStat(Stats.POISON_PROF, multiplier, target, null);
					break;
				case STUN:
					multiplier = attacker.calcStat(Stats.STUN_PROF, multiplier, target, null);
					break;
				case PARALYZE:
					multiplier = attacker.calcStat(Stats.PARALYZE_PROF, multiplier, target, null);
					break;
				case ROOT:
					multiplier = attacker.calcStat(Stats.ROOT_PROF, multiplier, target, null);
					break;
				case SLEEP:
					multiplier = attacker.calcStat(Stats.SLEEP_PROF, multiplier, target, null);
					break;
				case MUTE:
				case FEAR:
				case BETRAY:
				case AGGREDUCE_CHAR:
					multiplier = attacker.calcStat(Stats.DERANGEMENT_PROF, multiplier, target, null);
					break;
				case CONFUSION:
				case CONFUSE_MOB_ONLY:
					multiplier = attacker.calcStat(Stats.CONFUSION_PROF, multiplier, target, null);
					break;
				case DEBUFF:
				case WEAKNESS:
				case GLOOM:
				case SURRENDER:
				case HEX:
				case SLOW:
				case DOD:
					multiplier = attacker.calcStat(Stats.DEBUFF_PROF, multiplier, target, null);
					break;
				default:
			}
		}
		
		return multiplier;
	}
	
	public static double calcSkillStatModifier(L2Skill skill, L2Character target)
	{
		BaseStats saveVs = skill.getSavevs();
		if (saveVs == null)
		{
			switch (skill.getSkillType())
			{
				case STUN:
				case BLEED:
				case POISON:
					saveVs = BaseStats.CON;
					break;
				case SLEEP:
				case DEBUFF:
				case SURRENDER:
				case GLOOM:
				case SLOW:
				case HEX:
				case WEAKNESS:
				case ERASE:
				case ROOT:
				case MUTE:
				case FEAR:
				case BETRAY:
				case CONFUSION:
				case CONFUSE_MOB_ONLY:
				case AGGREDUCE_CHAR:
				case PARALYZE:
					saveVs = BaseStats.MEN;
					break;
				default:
					return 1;
			}
		}
		
		double multiplier = 2 - Math.sqrt(saveVs.calcBonus(target));
		if (multiplier < 0)
		{
			multiplier = 0;
		}
		
		return multiplier;
	}
	
	// XXX calcCubicSkillSuccess
	public static boolean calcCubicSkillSuccess(final L2CubicInstance attacker, final L2Character target, L2Skill skill)
	{
		if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 && skill.canBeReflected() && skill.is_Debuff())
		{
			return false;
		}
		// Add Matk/Mdef Bonus (Pending)
		
		SkillType type = skill.getSkillType();
		if (target.isRaid())
		{
			switch (type)
			{
				case CONFUSION:
				case ROOT:
				case STUN:
				case MUTE:
				case FEAR:
				case DEBUFF:
				case SLOW:
				case PARALYZE:
				case SLEEP:
				case AGGDEBUFF:
					return false;
			}
		}
		
		// Base chance
		double baseRate = skill.getPower();
		
		if (skill.ignoreResists())
		{
			return Rnd.get(100) < baseRate;
		}
		
		double statModifier = skill.getSavevs().calcBonus(target);
		double rate = (baseRate * statModifier);
		
		// Resist modifier.
		double vulnModifier = calcSkillVulnerability(target, skill, type);
		rate *= vulnModifier;
		
		// lvl bonus modifier.
		double deltamod = calcLvlDependModifier(attacker.getOwner(), target, skill);
		rate += deltamod;
		
		if (rate > skill.getMaxChance())
		{
			rate = skill.getMaxChance();
		}
		else if (rate < skill.getMinChance())
		{
			rate = skill.getMinChance();
		}
		
		if (Config.SEND_SKILLS_CHANCE_TO_PLAYERS)
		{
			attacker.getOwner().sendMessage("Cubic's Skill: " + skill.getName() + " chance: " + rate + "%");
		}
		
		return (Rnd.get(100) < rate);
	}
	
	public double mAtkModifier(L2Character attacker, L2Character target, L2Skill skill, boolean sps, boolean bss)
	{
		double mAtkModifier = 1;
		
		if (skill.isMagic())
		{
			double mAtk = attacker.getMAtk(target, skill);
			
			if (bss)
			{
				mAtk *= 4.0;
			}
			else if (sps)
			{
				mAtk *= 2.0;
			}
			
			mAtkModifier = (Math.sqrt(mAtk) / target.getMDef(attacker, skill)) * 11.0;
		}
		
		return mAtkModifier;
	}
	
	// XXX calcSkillSuccess
	public boolean calcSkillSuccess(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean sps, boolean bss)
	{
		if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 && skill.canBeReflected() && skill.is_Debuff())
		{
			return false;
		}
		
		SkillType type = skill.getSkillType();
		
		if (target.isRaid())
		{
			switch (type)
			{
				case CONFUSION:
				case MUTE:
				case SLOW:
				case PARALYZE:
				case ROOT:
				case FEAR:
				case SLEEP:
				case STUN:
				case DEBUFF:
				case AGGDEBUFF:
					return false;
			}
		}
		
		if (target.isInvul())
		{
			switch (type)
			{
				case CONFUSION:
				case MUTE:
				case SLOW:
				case PARALYZE:
				case ROOT:
				case FEAR:
				case SLEEP:
				case STUN:
				case DEBUFF:
				case CANCEL:
				case NEGATE:
				case WARRIOR_BANE:
				case MAGE_BANE:
					return false;
			}
		}
		
		if (target instanceof L2GrandBossInstance)
		{
			switch (type)
			{
				case GLOOM:
				case SURRENDER:
				case HEX:
					return false;
			}
		}
		
		// Base chance
		double baseRate = skill.getPower();
		if (skill.ignoreResists())
		{
			return (Rnd.get(100) < baseRate);
		}
		
		// Skill stat modifier
		double skillStatModifier = calcSkillStatModifier(skill, target);
		// Resists
		double vulnModifier = calcSkillVulnerability(target, skill, type);
		// Add Matk/Mdef bonus modifier
		double mAtkModifier = mAtkModifier(attacker, target, skill, sps, bss);
		// Level modifier.
		double lvlModifier = calcLvlDependModifier(attacker, target, skill);
		// Physics configuration file
		double physics_mult = getChanceMultiplier(skill);
		
		double rate = (baseRate * skillStatModifier * vulnModifier * mAtkModifier * physics_mult) + lvlModifier;
		
		// Announcements _a = Announcements.getInstance();
		// _a.sys("baseRate:" + skill.getPower());
		// _a.sys("skillStatModifier:" + skillStatModifier);
		// _a.sys("vulnModifier:" + vulnModifier);
		// _a.sys("mAtkModifier:" + mAtkModifier);
		// _a.sys("lvlModifier:" + mAtkModifier);
		// _a.sys("physics_mult:" + physics_mult);
		
		if (rate > skill.getMaxChance())
		{
			rate = skill.getMaxChance();
		}
		else if (rate < skill.getMinChance())
		{
			rate = skill.getMinChance();
		}
		
		if (Config.SEND_SKILLS_CHANCE_TO_PLAYERS)
		{
			sendSkillChance(attacker, target, null, skill, rate);
		}
		
		return Rnd.get(100) < rate;
	}
	
	// XXX calcEffectSuccess
	public boolean calcEffectSuccess(L2Character attacker, L2Character target, EffectTemplate effect, L2Skill skill, boolean ss, boolean sps, boolean bss)
	{
		if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 && skill.canBeReflected() && skill.is_Debuff())
		{
			return false;
		}
		
		final SkillType type = effect.effectType;
		// Base chance
		final int baseRate = (int) effect.effectPower;
		
		if (type == null)
		{
			return Rnd.get(100) < baseRate;
		}
		
		if (skill.ignoreResists())
		{
			return Rnd.get(100) < baseRate;
		}
		
		if (target.isRaid())
		{
			switch (type)
			{
				case CONFUSION:
				case MUTE:
				case SLOW:
				case PARALYZE:
				case ROOT:
				case FEAR:
				case SLEEP:
				case STUN:
				case DEBUFF:
				case AGGDEBUFF:
					return false;
			}
		}
		
		if (target instanceof L2GrandBossInstance)
		{
			switch (type)
			{
				case GLOOM:
				case SURRENDER:
				case HEX:
					return false;
			}
		}
		
		// Skill stat modifier
		double skillStatModifier = calcSkillStatModifier(skill, target);
		// Resists
		double vulnModifier = calcSkillVulnerability(target, skill, type);
		// Add Matk/Mdef bonus modifier
		double mAtkModifier = mAtkModifier(attacker, target, skill, sps, bss);
		// Level modifier.
		double lvlModifier = calcLvlDependModifier(attacker, target, skill);
		// Physics configuration file
		double physics_mult = getChanceMultiplier(skill);
		
		double rate = (baseRate * skillStatModifier * vulnModifier * mAtkModifier * physics_mult) + lvlModifier;
		
		if (rate > skill.getMaxChance())
		{
			rate = skill.getMaxChance();
		}
		else if (rate < skill.getMinChance())
		{
			rate = skill.getMinChance();
		}
		
		if (Config.SEND_SKILLS_CHANCE_TO_PLAYERS)
		{
			sendSkillChance(attacker, target, effect, skill, rate);
		}
		
		return (Rnd.get(100) < rate);
	}
	
	public void sendSkillChance(L2Character attacker, L2Character target, EffectTemplate effect, L2Skill skill, double rate)
	{
		NumberFormat defaultFormat = NumberFormat.getNumberInstance();
		defaultFormat.setMinimumFractionDigits(2);
		String formatedChance = defaultFormat.format(rate);
		
		if (effect != null)
		{
			if (attacker instanceof L2PcInstance)
			{
				((L2PcInstance) attacker).sendMessage("" + skill.getName() + " (" + effect.name + ") chance: " + formatedChance + "%");
				
			}
			
			// Get sys msg from player and mob
			if (target instanceof L2PcInstance)
			{
				((L2PcInstance) target).sendMessage("" + attacker.getName() + "'s " + skill.getName() + " (" + effect.name + ") chance: " + formatedChance + "%");
			}
			
			return;
		}
		
		if (attacker instanceof L2PcInstance)
		{
			((L2PcInstance) attacker).sendMessage("Skill: " + skill.getName() + " chance: " + formatedChance + "%");
		}
		
		// Get sys msg from player and mob
		if (target instanceof L2PcInstance)
		{
			((L2PcInstance) target).sendMessage("" + attacker.getName() + "'s Skill: " + skill.getName() + " chance: " + formatedChance + "%");
		}
	}
	
	public static double calcSkillTypeVulnerability(double multiplier, L2Character target, SkillType type)
	{
		if (type != null)
		{
			switch (type)
			{
				case BLEED:
					multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
					break;
				case POISON:
					multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
					break;
				case STUN:
					multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
					break;
				case PARALYZE:
					multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
					break;
				case ROOT:
					multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
					break;
				case SLEEP:
					multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
					break;
				case MUTE:
				case FEAR:
				case BETRAY:
				case AGGDEBUFF:
				case ERASE:
					multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
					break;
				case CONFUSION:
				case CONFUSE_MOB_ONLY:
					multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, target, null);
					break;
				case DEBUFF:
				case SLOW:
				case DOD:
				case WEAKNESS:
				case GLOOM:
				case SURRENDER:
				case HEX:
					multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
					break;
				case BUFF:
					multiplier = target.calcStat(Stats.BUFF_VULN, multiplier, target, null);
					break;
				case CANCEL:
					multiplier = target.calcStat(Stats.CANCEL_VULN, multiplier, target, null);
					break;
				default:
			}
		}
		
		return multiplier;
	}
	
	public static double calcLvlDependModifier(final L2Character attacker, final L2Character target, L2Skill skill)
	{
		if (skill.getLevelDepend() == 0)
		{
			return 0;
		}
		
		final int attackerMod;
		if (skill.getMagicLevel() > 0)
		{
			attackerMod = skill.getMagicLevel();
		}
		else
		{
			attackerMod = attacker.getLevel();
		}
		
		final int delta = attackerMod - target.getLevel();
		int deltamod = delta / 5;
		deltamod = deltamod * 5;
		if (deltamod != delta)
		{
			if (delta < 0)
			{
				deltamod -= 5;
			}
			else
			{
				deltamod += 5;
			}
		}
		
		return deltamod;
	}
	
	public static double getChanceMultiplier(L2Skill skill)
	{
		double multiplier = 1;
		
		if (skill != null && skill.getSkillType() != null)
		{
			switch (skill.getSkillType())
			{
				case BLEED:
					multiplier = Config.BLEED_CHANCE_MODIFIER;
					break;
				case POISON:
					multiplier = Config.POISON_CHANCE_MODIFIER;
					break;
				case STUN:
					multiplier = Config.STUN_CHANCE_MODIFIER;
					break;
				case PARALYZE:
					multiplier = Config.PARALYZE_CHANCE_MODIFIER;
					break;
				case ROOT:
					multiplier = Config.ROOT_CHANCE_MODIFIER;
					break;
				case SLEEP:
					multiplier = Config.SLEEP_CHANCE_MODIFIER;
					break;
				case MUTE:
					multiplier = Config.MUTE_CHANCE_MODIFIER;
					break;
				case FEAR:
				case BETRAY:
				case AGGREDUCE_CHAR:
					multiplier = Config.FEAR_CHANCE_MODIFIER;
					break;
				case CONFUSION:
					multiplier = Config.CONFUSION_CHANCE_MODIFIER;
					break;
				case DEBUFF:
				case SLOW:
				case DOD:
				case WEAKNESS:
				case GLOOM:
				case SURRENDER:
				case HEX:
				case WARRIOR_BANE:
				case MAGE_BANE:
					multiplier = Config.DEBUFF_CHANCE_MODIFIER;
					break;
				case BUFF:
					multiplier = Config.BUFF_CHANCE_MODIFIER;
					break;
			}
		}
		
		return multiplier;
		
	}
	
	public boolean calcBuffSuccess(L2Character target, L2Skill skill)
	{
		int rate = 100 * (int) calcSkillVulnerability(target, skill, skill.getSkillType());
		return Rnd.get(100) < rate;
	}
	
	public static boolean calcMagicSuccess(L2Character attacker, L2Character target, L2Skill skill)
	{
		double lvlDifference = target.getLevel() - (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel());
		int rate = Math.round((float) (Math.pow(1.3, lvlDifference) * 100));
		
		return Rnd.get(10000) > rate;
	}
	
	public boolean calculateUnlockChance(L2Skill skill)
	{
		int level = skill.getLevel();
		int chance = 0;
		switch (level)
		{
			case 1:
				chance = 30;
				break;
			
			case 2:
				chance = 50;
				break;
			
			case 3:
				chance = 75;
				break;
			
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				chance = 100;
				break;
		}
		if (Rnd.get(120) > chance)
		{
			return false;
		}
		return true;
	}
	
	public double calcManaDam(final L2Character attacker, final L2Character target, L2Skill skill, boolean ss, boolean bss)
	{
		if (attacker == null || target == null)
		{
			return 0;
		}
		
		final boolean isPvE = attacker instanceof L2PlayableInstance && target instanceof L2Attackable;
		
		// Mana Burnt = (SQR(M.Atk)*Power*(Target Max MP/97))/M.Def
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();
		
		int ssModifier = 1;
		// Add Bonus for Sps/SS
		if (attacker instanceof L2Summon && !(attacker instanceof L2PetInstance))
		{
			
			if (bss)
			{
				ssModifier = 4;
			}
			else if (ss)
			{
				ssModifier = 2;
			}
			
		}
		else
		{
			L2ItemInstance weapon = attacker.getActiveWeaponInstance();
			if (weapon != null)
			{
				if (bss)
				{
					ssModifier = 4;
				}
				else if (ss)
				{
					ssModifier = 2;
				}
			}
		}
		
		mAtk *= ssModifier;
		
		double damage = Math.sqrt(mAtk) * skill.getPower(attacker) * mp / 97 / mDef;
		damage *= calcSkillVulnerability(target, skill, skill.getSkillType());
		
		if (Config.EXPLLOSIVE_CUSTOM)
		{
			if (isPvE)
			{
				damage *= attacker.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
			}
		}
		
		return damage;
	}
	
	public double calculateSkillResurrectRestorePercent(double baseRestorePercent, L2Character caster)
	{
		double restorePercent = baseRestorePercent;
		double modifier = BaseStats.WIT.calcBonus(caster);
		
		if (restorePercent != 100 && restorePercent != 0)
		{
			
			restorePercent = baseRestorePercent * modifier;
			
			if (restorePercent - baseRestorePercent > 20.0)
			{
				restorePercent = baseRestorePercent + 20.0;
			}
		}
		
		if (restorePercent > 100)
		{
			restorePercent = 100;
		}
		if (restorePercent < baseRestorePercent)
		{
			restorePercent = baseRestorePercent;
		}
		
		return restorePercent;
	}
	
	public static boolean calcPhysicalSkillEvasion(L2Character target, L2Skill skill)
	{
		if (skill.isMagic() || skill.getCastRange() > 40)
		{
			return false;
		}
		
		return Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, null, skill);
	}
	
	public static boolean calcSkillMastery(final L2Character actor)
	{
		if (actor == null)
		{
			return false;
		}
		
		double val = actor.getStat().calcStat(Stats.SKILL_MASTERY, 0, null, null);
		
		if (actor instanceof L2PcInstance)
		{
			if (((L2PcInstance) actor).isMageClass())
			{
				val *= BaseStats.INT.calcBonus(actor);
			}
			else
			{
				val *= BaseStats.STR.calcBonus(actor);
			}
		}
		
		return Rnd.get(100) < val;
	}
	
	/**
	 * Calculate damage caused by falling
	 * @param cha
	 * @param fallHeight
	 * @return damage
	 */
	public static double calcFallDam(L2Character cha, int fallHeight)
	{
		if (!Config.FALL_DAMAGE || fallHeight < 0)
		{
			return 0;
		}
		
		final double damage = cha.calcStat(Stats.FALL, fallHeight * cha.getMaxHp() / 1000, null, null);
		return damage;
	}
	
	/**
	 * Calculated damage caused by charges skills types. - THX aCis The special thing is about the multiplier (56 and not 70), and about the fixed amount of damages
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill
	 * @param shld
	 * @param crit if the ATTACK have critical success
	 * @param ss if weapon item was charged by soulshot
	 * @param _numCharges
	 * @return damage points
	 */
	public static final double calcChargeSkillsDam(L2Character attacker, L2Character target, L2Skill skill, boolean shld, boolean crit, boolean ss, int _numCharges)
	{
		if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance) attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
			{
				return 0;
			}
		}
		
		final boolean isPvP = (attacker instanceof L2PlayableInstance) && (target instanceof L2PlayableInstance);
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		
		if (ss)
		{
			damage *= 2;
		}
		
		if (crit)
		{
			// double cAtkMultiplied = (damage) + attacker.calcStat(Stats.CRITICAL_DAMAGE, damage, target, skill);
			double improvedDamageByCriticalVuln = target.calcStat(Stats.CRIT_VULN, damage, target, skill);
			double improvedDamageByCriticalVulnAndAdd = (attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, improvedDamageByCriticalVuln, target, skill));
			
			if (Config.DEBUG)
			{
				LOG.info("Attacker '" + attacker.getName() + "' Charge Skills Critical Damage Debug:");
				LOG.info("	-	Initial Damage:  " + damage);
				LOG.info("	-	improvedDamageByCriticalVuln: " + improvedDamageByCriticalVuln);
				LOG.info("	-	improvedDamageByCriticalVulnAndAdd: " + improvedDamageByCriticalVulnAndAdd);
			}
			
			damage = improvedDamageByCriticalVulnAndAdd;
		}
		
		if (skill != null)
		{
			double skillpower = skill.getPower(attacker);
			float ssboost = skill.getSSBoost();
			if (ssboost <= 0)
			{
				damage += skillpower;
			}
			else if (ssboost > 0)
			{
				if (ss)
				{
					skillpower *= ssboost;
					damage += skillpower;
				}
				else
				{
					damage += skillpower;
				}
			}
			
			// Charges multiplier, just when skill is used
			if (_numCharges >= 1)
			{
				double chargesModifier = 0.7 + (0.3 * _numCharges);
				damage *= chargesModifier;
			}
			
		}
		
		damage = 56 * damage / defence;
		
		// finally, apply the critical multiplier if present (it's not subjected to defense)
		if (crit)
		{
			damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, damage, target, skill);
		}
		
		// defence modifier depending of the attacker weapon
		L2Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if (weapon != null)
		{
			switch (weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				case BLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.BIGSWORD_WPN_VULN;
					break;
				case BIGBLUNT:
					stat = Stats.BIGBLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC:
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST:
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
			}
		}
		
		if (stat != null)
		{
			damage = target.calcStat(stat, damage, target, null);
		}
		
		// Weapon random damage
		damage *= attacker.getRandomDamageMultiplier();
		
		// After C4 nobles make 4% more dmg in PvP.
		if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isNoble() && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			damage *= 1.04;
		}
		
		// LOG.info(" - Final damage: "+damage);
		
		if (shld && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
			{
				damage = 0;
			}
		}
		
		if (target instanceof L2NpcInstance)
		{
			double multiplier;
			switch (((L2NpcInstance) target).getTemplate().getRace())
			{
				case BEAST:
					multiplier = 1 + ((attacker.getPAtkMonsters(target) - target.getPDefMonsters(target)) / 100);
					damage *= multiplier;
					break;
				case ANIMAL:
					multiplier = 1 + ((attacker.getPAtkAnimals(target) - target.getPDefAnimals(target)) / 100);
					damage *= multiplier;
					break;
				case PLANT:
					multiplier = 1 + ((attacker.getPAtkPlants(target) - target.getPDefPlants(target)) / 100);
					damage *= multiplier;
					break;
				case DRAGON:
					multiplier = 1 + ((attacker.getPAtkDragons(target) - target.getPDefDragons(target)) / 100);
					damage *= multiplier;
					break;
				case ANGEL:
					multiplier = 1 + ((attacker.getPAtkAngels(target) - target.getPDefAngels(target)) / 100);
					damage *= multiplier;
					break;
				case BUG:
					multiplier = 1 + ((attacker.getPAtkInsects(target) - target.getPDefInsects(target)) / 100);
					damage *= multiplier;
					break;
				case GIANT:
					multiplier = 1 + ((attacker.getPAtkGiants(target) - target.getPDefGiants(target)) / 100);
					damage *= multiplier;
					break;
				case MAGICCREATURE:
					multiplier = 1 + ((attacker.getPAtkMagicCreatures(target) - target.getPDefMagicCreatures(target)) / 100);
					damage *= multiplier;
					break;
				default:
					// nothing
					break;
			}
		}
		
		if (shld)
		{
			if (100 - Config.ALT_PERFECT_SHLD_BLOCK < Rnd.get(100))
			{
				damage = 1;
				target.sendPacket(new SystemMessage(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS));
			}
		}
		
		if (damage > 0 && damage < 1)
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}
		
		// Dmg bonusses in PvP fight
		if (isPvP)
		{
			if (skill == null)
			{
				damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			}
		}
		
		if (Config.ENABLE_CLASS_DAMAGES && attacker instanceof L2PcInstance && target instanceof L2PcInstance)
		{
			if (((L2PcInstance) attacker).isInOlympiadMode() && ((L2PcInstance) target).isInOlympiadMode())
			{
				if (Config.ENABLE_CLASS_DAMAGES_IN_OLY)
				{
					damage = damage * ClassDamageManager.getDamageMultiplier((L2PcInstance) attacker, (L2PcInstance) target);
				}
				
			}
			else
			{
				damage = damage * ClassDamageManager.getDamageMultiplier((L2PcInstance) attacker, (L2PcInstance) target);
			}
		}
		return damage;
	}
	
}
