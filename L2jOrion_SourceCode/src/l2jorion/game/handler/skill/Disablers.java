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
package l2jorion.game.handler.skill;

import java.io.IOException;

import l2jorion.Config;
import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2AttackableAI;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.handler.SkillHandler;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Effect.EffectType;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.model.base.CancelReturn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Formulas;
import l2jorion.game.skills.Stats;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class Disablers implements ISkillHandler
{
	
	private static final SkillType[] SKILL_IDS =
	{
		L2Skill.SkillType.STUN,
		L2Skill.SkillType.ROOT,
		L2Skill.SkillType.SLEEP,
		L2Skill.SkillType.CONFUSION,
		L2Skill.SkillType.AGGDAMAGE,
		L2Skill.SkillType.AGGREDUCE,
		L2Skill.SkillType.AGGREDUCE_CHAR,
		L2Skill.SkillType.AGGREMOVE,
		L2Skill.SkillType.UNBLEED,
		L2Skill.SkillType.UNPOISON,
		L2Skill.SkillType.MUTE,
		L2Skill.SkillType.FAKE_DEATH,
		L2Skill.SkillType.CONFUSE_MOB_ONLY,
		L2Skill.SkillType.NEGATE,
		L2Skill.SkillType.CANCEL,
		L2Skill.SkillType.PARALYZE,
		L2Skill.SkillType.ERASE,
		L2Skill.SkillType.MAGE_BANE,
		L2Skill.SkillType.WARRIOR_BANE,
		L2Skill.SkillType.DEBUFF,
		L2Skill.SkillType.SLOW,
		L2Skill.SkillType.GLOOM,
		L2Skill.SkillType.SURRENDER,
		L2Skill.SkillType.HEX,
		L2Skill.SkillType.BETRAY
	};
	
	protected static final Logger LOG = LoggerFactory.getLogger(L2Skill.class.getName());
	
	private String[] _negateSkillTypes = null;
	private String[] _negateEffectTypes = null;
	private float _negatePower = 0.f;
	private int _negateId = 0;
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		SkillType type = skill.getSkillType();
		
		boolean bss = activeChar.checkBss();
		boolean sps = activeChar.checkSps();
		boolean ss = activeChar.checkSs();
		
		for (L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
			{
				continue;
			}
			
			L2Character target = (L2Character) obj;
			
			if (target.isDead())
			{
				continue;
			}
			
			switch (type)
			{
				case FAKE_DEATH:
				{
					skill.getEffects(activeChar, target, ss, sps, bss);
					break;
				}
				case STUN:
					// Calculate skill evasion
					if (Formulas.calcPhysicalSkillEvasion(target, skill))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
						break;
					}
					// Calculate vengeance
					if (target.vengeanceSkill(skill))
					{
						target = activeChar;
					}
				case BETRAY:
				case ROOT:
				case SLEEP:
				case PARALYZE:
				case CONFUSION:
				case MUTE:
				case SLOW:
				case GLOOM:
				case SURRENDER:
				case HEX:
				case DEBUFF:
				{
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						skill.getEffects(activeChar, target, ss, sps, bss);
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addString(target.getName());
							sm.addSkillName(skill.getDisplayId());
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case CONFUSE_MOB_ONLY:
				{
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							if (e.getSkill().getSkillType() == type)
							{
								e.exit(false);
							}
						}
						skill.getEffects(activeChar, target, ss, sps, bss);
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addString(target.getName());
							sm.addSkillName(skill.getDisplayId());
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case AGGDAMAGE:
				{
					if (target instanceof L2Attackable && Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
					}
					
					skill.getEffects(activeChar, target, ss, sps, bss);
					break;
				}
				case AGGREDUCE:
				{
					// these skills needs to be rechecked
					if (target instanceof L2Attackable)
					{
						skill.getEffects(activeChar, target, ss, sps, bss);
						
						double aggdiff = ((L2Attackable) target).getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((L2Attackable) target).getHating(activeChar), target, skill);
						
						if (skill.getPower() > 0)
						{
							((L2Attackable) target).reduceHate(null, (int) skill.getPower());
						}
						else if (aggdiff > 0)
						{
							((L2Attackable) target).reduceHate(null, (int) aggdiff);
						}
					}
					break;
				}
				case AGGREDUCE_CHAR:
				{
					// these skills needs to be rechecked
					if (skill.getName().equals("Bluff"))
					{
						if (target instanceof L2Attackable)
						{
							L2Attackable _target = (L2Attackable) target;
							_target.stopHating(activeChar);
							if (_target.getMostHated() == null)
							{
								((L2AttackableAI) _target.getAI()).setGlobalAggro(-25);
								_target.clearAggroList();
								_target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
								_target.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
								_target.setWalking();
							}
						}
						skill.getEffects(activeChar, target, ss, sps, bss);
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addString(target.getName());
							sm.addSkillName(skill.getId());
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case AGGREMOVE:
				{
					// these skills needs to be rechecked
					if (target instanceof L2Attackable && !target.isRaid())
					{
						if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
						{
							if (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_UNDEAD)
							{
								if (target.isUndead())
								{
									((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
								}
							}
							else
							{
								((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
							}
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
								sm.addString(target.getName());
								sm.addSkillName(skill.getId());
								activeChar.sendPacket(sm);
							}
						}
					}
					break;
				}
				case UNBLEED:
				{
					negateEffect(target, SkillType.BLEED, skill.getPower());
					break;
				}
				case UNPOISON:
				{
					negateEffect(target, SkillType.POISON, skill.getPower());
					break;
				}
				case ERASE:
				{
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss) && !(target instanceof L2SiegeSummonInstance))
					{
						L2PcInstance summonOwner = null;
						L2Summon summonPet = null;
						summonOwner = ((L2Summon) target).getOwner();
						summonPet = summonOwner.getPet();
						summonPet.unSummon(summonOwner);
						SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
						summonOwner.sendPacket(sm);
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addString(target.getName());
							sm.addSkillName(skill.getId());
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case MAGE_BANE:
				{
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							if (e.getStackType().equals("mAtkSpeedUp") || e.getStackType().equals("mAtk") || e.getSkill().getId() == 1059 || e.getSkill().getId() == 1085 || e.getSkill().getId() == 4356 || e.getSkill().getId() == 4355)
							{
								e.exit(true);
							}
						}
						skill.getEffects(activeChar, target, ss, sps, bss);
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addString(target.getName());
							sm.addSkillName(skill.getDisplayId());
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case WARRIOR_BANE:
				{
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							if (e.getStackType().equals("SpeedUp") || e.getStackType().equals("pAtkSpeedUp") || e.getSkill().getId() == 1204 || e.getSkill().getId() == 1086 || e.getSkill().getId() == 4342 || e.getSkill().getId() == 4357)
							{
								e.exit(true);
							}
						}
						skill.getEffects(activeChar, target, ss, sps, bss);
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addString(target.getName());
							sm.addSkillName(skill.getDisplayId());
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case CANCEL:
				{
					if (target.reflectSkill(skill))
					{
						target = activeChar;
					}
					
					// Random max 5
					int maxBuffs = (int) skill.getNegatePower();
					int buffsNumber = Rnd.get(1, maxBuffs);
					
					if (Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							switch (e.getEffectType())
							{
								case SIGNET_GROUND:
								case SIGNET_EFFECT:
									continue;
							}
							
							switch (e.getSkill().getId())
							{
								case 4082:
								case 4215:
								case 5182:
								case 4515:
								case 110:
								case 111:
								case 1323:
								case 1325:
									continue;
							}
							
							if (Config.L2LIMIT_CUSTOM || Config.RON_CUSTOM)
							{
								if (target instanceof L2PcInstance)
								{
									if (!((L2PcInstance) target).getCancelledBuffs().containsKey(e.getSkill()))
									{
										((L2PcInstance) target).getCancelledBuffs().put(e.getSkill(), new int[]
										{
											e.getCount(),
											e.getTime()
										});
									}
								}
							}
							
							e.exit(true);
							
							buffsNumber--;
							if (buffsNumber == 0)
							{
								break;
							}
						}
						
						if (Config.L2LIMIT_CUSTOM || Config.RON_CUSTOM)
						{
							if (target instanceof L2PcInstance)
							{
								if (((L2PcInstance) target).getCancelledBuffs().size() > 0)
								{
									ThreadPoolManager.getInstance().scheduleGeneral(new CancelReturn((L2PcInstance) target, ((L2PcInstance) target).getCancelledBuffs()), 15 * 1000);
								}
							}
						}
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							sm.addString(target.getName());
							sm.addSkillName(skill.getDisplayId());
							activeChar.sendPacket(sm);
						}
					}
					break;
				}
				case NEGATE:
				{
					if (skill.getId() == 2275) // fishing potion
					{
						_negatePower = skill.getNegatePower();
						_negateId = skill.getNegateId();
						negateEffect(target, SkillType.BUFF, _negatePower, _negateId);
					}
					else
					{
						_negateSkillTypes = skill.getNegateSkillTypes();
						_negateEffectTypes = skill.getNegateEffectTypes();
						_negatePower = skill.getNegatePower();
						
						for (String stat : _negateSkillTypes)
						{
							stat = stat.toLowerCase().intern();
							if (stat == "buff")
							{
								int lvlmodifier = 52 + skill.getMagicLevel() * 2;
								if (skill.getMagicLevel() == 12)
								{
									lvlmodifier = (ExperienceData.getInstance().getMaxLevel() - 1);
								}
								
								int landrate = 90;
								if ((target.getLevel() - lvlmodifier) > 0)
								{
									landrate = 90 - 4 * (target.getLevel() - lvlmodifier);
								}
								
								landrate = (int) target.calcStat(Stats.CANCEL_VULN, landrate, target, null);
								
								if (Rnd.get(100) < landrate)
								{
									negateEffect(target, SkillType.BUFF, -1);
								}
							}
							if (stat == "debuff")
							{
								negateEffect(target, SkillType.DEBUFF, -1);
							}
							
							if (stat == "weakness")
							{
								negateEffect(target, SkillType.WEAKNESS, -1);
							}
							
							if (stat == "stun")
							{
								negateEffect(target, SkillType.STUN, -1);
							}
							
							if (stat == "sleep")
							{
								negateEffect(target, SkillType.SLEEP, -1);
							}
							
							if (stat == "mdam")
							{
								negateEffect(target, SkillType.MDAM, -1);
							}
							
							if (stat == "confusion")
							{
								negateEffect(target, SkillType.CONFUSION, -1);
							}
							
							if (stat == "mute")
							{
								negateEffect(target, SkillType.MUTE, -1);
							}
							
							if (stat == "fear")
							{
								negateEffect(target, SkillType.FEAR, -1);
							}
							
							if (stat == "poison")
							{
								negateEffect(target, SkillType.POISON, _negatePower);
							}
							
							if (stat == "bleed")
							{
								negateEffect(target, SkillType.BLEED, _negatePower);
							}
							
							if (stat == "paralyze")
							{
								negateEffect(target, SkillType.PARALYZE, -1);
							}
							
							if (stat == "root")
							{
								negateEffect(target, SkillType.ROOT, -1);
							}
							
							if (stat == "slow")
							{
								negateEffect(target, SkillType.SLOW, -1);
							}
							
							if (stat == "gloom")
							{
								negateEffect(target, SkillType.GLOOM, -1);
							}
							
							if (stat == "surrender")
							{
								negateEffect(target, SkillType.SURRENDER, -1);
							}
							
							if (stat == "hex")
							{
								negateEffect(target, SkillType.HEX, -1);
							}
							
							if (stat == "heal")
							{
								ISkillHandler Healhandler = SkillHandler.getInstance().getSkillHandler(SkillType.HEAL);
								if (Healhandler == null)
								{
									LOG.warn("Couldn't find skill handler for HEAL.");
									continue;
								}
								
								L2Object tgts[] = new L2Object[]
								{
									target
								};
								try
								{
									Healhandler.useSkill(activeChar, skill, tgts);
								}
								catch (IOException e)
								{
									if (Config.ENABLE_ALL_EXCEPTIONS)
									{
										e.printStackTrace();
									}
									
									LOG.warn("", e);
								}
							}
						}
						
						for (String stat : _negateEffectTypes)
						{
							EffectType effect_type = null;
							try
							{
								effect_type = EffectType.valueOf(stat.toUpperCase());
							}
							catch (Exception e)
							{
							}
							
							if (effect_type != null)
							{
								switch (effect_type)
								{
									case BUFF:
									{
										int lvlmodifier = 52 + skill.getMagicLevel() * 2;
										if (skill.getMagicLevel() == 12)
										{
											lvlmodifier = (ExperienceData.getInstance().getMaxLevel() - 1);
										}
										
										int landrate = 90;
										if ((target.getLevel() - lvlmodifier) > 0)
										{
											landrate = 90 - 4 * (target.getLevel() - lvlmodifier);
										}
										
										landrate = (int) target.calcStat(Stats.CANCEL_VULN, landrate, target, null);
										
										if (Rnd.get(100) < landrate)
										{
											target.stopEffects(effect_type);
										}
									}
										break;
									default:
									{
										target.stopEffects(effect_type);
									}
										break;
									
								}
								
							}
							
						}
					}
				}
			}
		}
		
		if (skill.isMagic())
		{
			if (bss)
			{
				activeChar.removeBss();
			}
			else if (sps)
			{
				activeChar.removeSps();
			}
		}
		else
		{
			activeChar.removeSs();
		}
		
		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
		{
			// Replace old effect with new one.
			effect.exit(false);
		}
		skill.getEffectsSelf(activeChar);
		
	} // end void
	
	private void negateEffect(L2Character target, SkillType type, double power)
	{
		negateEffect(target, type, power, 0);
	}
	
	private void negateEffect(L2Character target, SkillType type, double power, int skillId)
	{
		L2Effect[] effects = target.getAllEffects();
		for (L2Effect e : effects)
		{
			if (e.getSkill() != null && e.getSkill().getId() == 4215 || e.getSkill().getId() == 4515 || e.getSkill().getId() == 4551 || e.getSkill().getId() == 4552 || e.getSkill().getId() == 4553 || e.getSkill().getId() == 4554)
			{
				continue; // skills cannot be removed
			}
			else if (power == -1) // if power is -1 the effect is always removed without power/lvl check ^^
			{
				if (e.getSkill().getSkillType() == type || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type))
				{
					if (skillId != 0)
					{
						if (skillId == e.getSkill().getId())
						{
							e.exit(true);
						}
					}
					else
					{
						e.exit(true);
					}
				}
			}
			else if ((e.getSkill().getSkillType() == type && e.getSkill().getPower() <= power) || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type && e.getSkill().getEffectLvl() <= power))
			{
				if (skillId != 0)
				{
					if (skillId == e.getSkill().getId())
					{
						e.exit(true);
					}
				}
				else
				{
					e.exit(true);
				}
			}
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
