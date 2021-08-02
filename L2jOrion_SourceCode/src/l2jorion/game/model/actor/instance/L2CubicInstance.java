/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.actor.instance;

import java.util.List;
import java.util.concurrent.Future;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.handler.SkillHandler;
import l2jorion.game.managers.DuelManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Formulas;
import l2jorion.game.skills.l2skills.L2SkillDrain;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class L2CubicInstance
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2CubicInstance.class);
	
	public static final int STORM_CUBIC = 1;
	public static final int VAMPIRIC_CUBIC = 2;
	public static final int LIFE_CUBIC = 3;
	public static final int VIPER_CUBIC = 4;
	public static final int POLTERGEIST_CUBIC = 5;
	public static final int BINDING_CUBIC = 6;
	public static final int AQUA_CUBIC = 7;
	public static final int SPARK_CUBIC = 8;
	public static final int ATTRACT_CUBIC = 9;
	public static final int SMART_CUBIC_EVATEMPLAR = 10;
	public static final int SMART_CUBIC_SHILLIENTEMPLAR = 11;
	public static final int SMART_CUBIC_ARCANALORD = 12;
	public static final int SMART_CUBIC_ELEMENTALMASTER = 13;
	public static final int SMART_CUBIC_SPECTRALMASTER = 14;
	
	// Max range of cubic skills
	public static final int MAX_MAGIC_RANGE = 900;
	
	// Cubic skills
	public static final int SKILL_CUBIC_HEAL = 4051;
	public static final int SKILL_CUBIC_CURE = 5579;
	
	protected L2PcInstance _owner;
	protected L2Character _target;
	protected int _id;
	protected int _matk;
	protected int _activationtime;
	protected int _activationchance;
	protected boolean _active;
	private final boolean _givenByOther;
	protected List<L2Skill> _skills = new FastList<>();
	private Future<?> _disappearTask;
	private Future<?> _actionTask;
	
	public L2CubicInstance(final L2PcInstance owner, final int id, final int level, final int mAtk, final int activationtime, final int activationchance, final int totallifetime, final boolean givenByOther)
	{
		_owner = owner;
		_id = id;
		_matk = mAtk;
		_activationtime = activationtime * 1000;
		_activationchance = activationchance;
		_active = false;
		_givenByOther = givenByOther;
		
		switch (_id)
		{
			case STORM_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4049, level));
				break;
			case VAMPIRIC_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4050, level));
				break;
			case LIFE_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4051, level));
				doAction();
				break;
			case VIPER_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4052, level));
				break;
			case POLTERGEIST_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4053, level));
				_skills.add(SkillTable.getInstance().getInfo(4054, level));
				_skills.add(SkillTable.getInstance().getInfo(4055, level));
				break;
			case BINDING_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4164, level));
				break;
			case AQUA_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4165, level));
				break;
			case SPARK_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(4166, level));
				break;
			case ATTRACT_CUBIC:
				_skills.add(SkillTable.getInstance().getInfo(5115, level));
				_skills.add(SkillTable.getInstance().getInfo(5116, level));
				break;
			case SMART_CUBIC_ARCANALORD:
				// _skills.add(SkillTable.getInstance().getInfo(4049,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4051, 7)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4165, 9)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
			case SMART_CUBIC_ELEMENTALMASTER:
				_skills.add(SkillTable.getInstance().getInfo(4049, 8)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4166, 9)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
			case SMART_CUBIC_SPECTRALMASTER:
				_skills.add(SkillTable.getInstance().getInfo(4049, 8)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4052, 6)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
			case SMART_CUBIC_EVATEMPLAR:
				// _skills.add(SkillTable.getInstance().getInfo(4049,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4053, 8)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				_skills.add(SkillTable.getInstance().getInfo(4165, 9)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
			case SMART_CUBIC_SHILLIENTEMPLAR:
				_skills.add(SkillTable.getInstance().getInfo(4049, 8)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
				_skills.add(SkillTable.getInstance().getInfo(5115, 4)); // have animation
				// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
				// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the
				// cubic skills list
				break;
		}
		_disappearTask = ThreadPoolManager.getInstance().scheduleGeneral(new Disappear(), totallifetime); // disappear
	}
	
	public synchronized void doAction()
	{
		if (_active)
		{
			return;
		}
		
		_active = true;
		
		switch (_id)
		{
			case AQUA_CUBIC:
			case BINDING_CUBIC:
			case SPARK_CUBIC:
			case STORM_CUBIC:
			case POLTERGEIST_CUBIC:
			case VAMPIRIC_CUBIC:
			case VIPER_CUBIC:
			case ATTRACT_CUBIC:
			case SMART_CUBIC_ARCANALORD:
			case SMART_CUBIC_ELEMENTALMASTER:
			case SMART_CUBIC_SPECTRALMASTER:
			case SMART_CUBIC_EVATEMPLAR:
			case SMART_CUBIC_SHILLIENTEMPLAR:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(_activationchance), 0, _activationtime);
				break;
			case LIFE_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(), 0, _activationtime);
				break;
		}
	}
	
	/**
	 * Gets the id.
	 * @return the id
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Gets the owner.
	 * @return the owner
	 */
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	/**
	 * Gets the m critical hit.
	 * @param target the target
	 * @param skill the skill
	 * @return the m critical hit
	 */
	public final int getMCriticalHit(final L2Character target, final L2Skill skill)
	{
		// TODO: Temporary now mcrit for cubics is the baseMCritRate of its owner
		return _owner.getTemplate().baseMCritRate;
	}
	
	/**
	 * Gets the m atk.
	 * @return the m atk
	 */
	public int getMAtk()
	{
		return _matk;
	}
	
	/**
	 * Stop action.
	 */
	public void stopAction()
	{
		_target = null;
		if (_actionTask != null)
		{
			if (!_actionTask.isCancelled())
			{
				_actionTask.cancel(true);
			}
			_actionTask = null;
		}
		_active = false;
	}
	
	/**
	 * Cancel disappear.
	 */
	public void cancelDisappear()
	{
		if (_disappearTask != null)
		{
			_disappearTask.cancel(true);
			_disappearTask = null;
		}
	}
	
	/**
	 * this sets the enemy target for a cubic.
	 */
	public void getCubicTarget()
	{
		try
		{
			_target = null;
			final L2Object ownerTarget = _owner.getTarget();
			if (ownerTarget == null)
			{
				return;
			}
			
			// Duel targeting
			if (_owner.isInDuel())
			{
				final L2PcInstance PlayerA = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerA();
				final L2PcInstance PlayerB = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerB();
				
				if (DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
				{
					final L2Party partyA = PlayerA.getParty();
					final L2Party partyB = PlayerB.getParty();
					L2Party partyEnemy = null;
					
					if (partyA != null)
					{
						if (partyA.getPartyMembers().contains(_owner))
						{
							if (partyB != null)
							{
								partyEnemy = partyB;
							}
							else
							{
								_target = PlayerB;
							}
						}
						else
						{
							partyEnemy = partyA;
						}
					}
					else
					{
						if (PlayerA == _owner)
						{
							if (partyB != null)
							{
								partyEnemy = partyB;
							}
							else
							{
								_target = PlayerB;
							}
						}
						else
						{
							_target = PlayerA;
						}
					}
					if (_target == PlayerA || _target == PlayerB)
					{
						if (_target == ownerTarget)
						{
							return;
						}
					}
					if (partyEnemy != null)
					{
						if (partyEnemy.getPartyMembers().contains(ownerTarget))
						{
							_target = (L2Character) ownerTarget;
						}
						return;
					}
				}
				if (PlayerA != _owner && ownerTarget == PlayerA)
				{
					_target = PlayerA;
					return;
				}
				if (PlayerB != _owner && ownerTarget == PlayerB)
				{
					_target = PlayerB;
					return;
				}
				_target = null;
				return;
			}
			
			// Olympiad targeting
			/*
			 * if (_owner.isInOlympiadMode()) { if (_owner.isOlympiadStart()) { final L2PcInstance[] players = Olympiad.getInstance().getPlayers(_owner.getOlympiadGameId()); if (players != null) { if (_owner.getOlympiadSide() == 1) { if (ownerTarget == players[1]) { _target = players[1]; } else if
			 * (players[1].getPet() != null && ownerTarget == players[1].getPet()) { _target = players[1].getPet(); } } else { if (ownerTarget == players[0]) { _target = players[0]; } else if (players[0].getPet() != null && ownerTarget == players[0].getPet()) { _target = players[0].getPet(); } } } }
			 * return; }
			 */
			if (_owner.isInOlympiadMode())
			{
				if (_owner.isOlympiadStart())
				{
					if (ownerTarget instanceof L2Character)
					{
						final L2PcInstance targetPlayer = ownerTarget.getActingPlayer();
						if (targetPlayer != null && targetPlayer.getOlympiadGameId() == _owner.getOlympiadGameId() && targetPlayer.getOlympiadSide() != _owner.getOlympiadSide())
						{
							_target = (L2Character) ownerTarget;
						}
					}
				}
				return;
			}
			
			// test owners target if it is valid then use it
			if (ownerTarget instanceof L2Character && ownerTarget != _owner.getPet() && ownerTarget != _owner)
			{
				
				// target mob which has aggro on you or your summon
				if (ownerTarget instanceof L2Attackable)
				{
					
					if (((L2Attackable) ownerTarget).getAggroList().get(_owner) != null && !((L2Attackable) ownerTarget).isDead())
					{
						_target = (L2Character) ownerTarget;
						return;
					}
					if (_owner.getPet() != null)
					{
						if (((L2Attackable) ownerTarget).getAggroList().get(_owner.getPet()) != null && !((L2Attackable) ownerTarget).isDead())
						{
							_target = (L2Character) ownerTarget;
							return;
						}
					}
				}
				
				// get target in pvp or in siege
				L2PcInstance enemy = null;
				
				if ((_owner.getPvpFlag() > 0 && !_owner.isInsideZone(ZoneId.ZONE_PEACE)) || _owner.isInsideZone(ZoneId.ZONE_PVP))
				{
					if (!((L2Character) ownerTarget).isDead() && ownerTarget instanceof L2PcInstance)
					{
						enemy = (L2PcInstance) ownerTarget;
					}
					
					if (enemy != null)
					{
						boolean targetIt = true;
						
						if (_owner.getParty() != null)
						{
							if (_owner.getParty().getPartyMembers().contains(enemy))
							{
								targetIt = false;
							}
							else if (_owner.getParty().getCommandChannel() != null)
							{
								if (_owner.getParty().getCommandChannel().getMembers().contains(enemy))
								{
									targetIt = false;
								}
							}
						}
						if (_owner.getClan() != null && !_owner.isInsideZone(ZoneId.ZONE_PVP))
						{
							if (_owner.getClan().isMember(enemy.getName()))
							{
								targetIt = false;
							}
							if (_owner.getAllyId() > 0 && enemy.getAllyId() > 0)
							{
								if (_owner.getAllyId() == enemy.getAllyId())
								{
									targetIt = false;
								}
							}
						}
						if (enemy.getPvpFlag() == 0 && !enemy.isInsideZone(ZoneId.ZONE_PVP))
						{
							targetIt = false;
						}
						if (enemy.isInsideZone(ZoneId.ZONE_PEACE))
						{
							targetIt = false;
						}
						if (_owner.getSiegeState() > 0 && _owner.getSiegeState() == enemy.getSiegeState())
						{
							targetIt = false;
						}
						if (!enemy.isVisible())
						{
							targetIt = false;
						}
						
						if (targetIt)
						{
							_target = enemy;
							return;
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			LOG.error("", e);
		}
	}
	
	/**
	 * The Class Action.
	 */
	private class Action implements Runnable
	{
		
		/** The _chance. */
		private final int _chance;
		
		/**
		 * Instantiates a new action.
		 * @param chance the chance
		 */
		Action(final int chance)
		{
			_chance = chance;
			// run task
		}
		
		@Override
		public void run()
		{
			try
			{
				final L2PcInstance owner = _owner;
				if (owner == null)
				{
					stopAction();
					cancelDisappear();
					return;
				}
				
				if (owner.isDead() || (owner.isOnline() == 0))
				{
					stopAction();
					owner.delCubic(_id);
					owner.broadcastUserInfo();
					cancelDisappear();
					return;
				}
				
				if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(owner))
				{
					if (owner.getPet() != null)
					{
						if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(owner.getPet()))
						{
							stopAction();
							return;
						}
					}
					else
					{
						stopAction();
						return;
					}
				}
				
				// Smart Cubic debuff cancel is 100%
				boolean UseCubicCure = false;
				L2Skill skill = null;
				
				if (_id >= SMART_CUBIC_EVATEMPLAR && _id <= SMART_CUBIC_SPECTRALMASTER)
				{
					final L2Effect[] effects = owner.getAllEffects();
					
					for (final L2Effect e : effects)
					{
						if (e != null && e.getSkill().isOffensive())
						{
							UseCubicCure = true;
							e.exit(true);
						}
					}
				}
				
				if (UseCubicCure)
				{
					// Smart Cubic debuff cancel is needed, no other skill is used in this
					// activation period
					final MagicSkillUser msu = new MagicSkillUser(owner, owner, SKILL_CUBIC_CURE, 1, 0, 0);
					owner.broadcastPacket(msu);
				}
				else if (Rnd.get(100) < _chance)
				{
					skill = _skills.get(Rnd.get(_skills.size()));
					if (skill != null)
					{
						
						if (skill.getId() == SKILL_CUBIC_HEAL)
						{
							// friendly skill, so we look a target in owner's party
							cubicTargetForHeal();
						}
						else
						{
							// offensive skill, we look for an enemy target
							getCubicTarget();
							if (_target == owner || !isInCubicRange(owner, _target))
							{
								_target = null;
							}
						}
						
						final L2Character target = _target;
						
						if ((target != null) && (!target.isDead()))
						{
							if (Config.DEBUG)
							{
								LOG.info("L2CubicInstance: Action.run();");
								LOG.info("Cubic Id: " + _id + " Target: " + target.getName() + " distance: " + Math.sqrt(target.getDistanceSq(owner.getX(), owner.getY(), owner.getZ())));
							}
							
							owner.broadcastPacket(new MagicSkillUser(owner, target, skill.getId(), skill.getLevel(), 0, 0));
							
							final SkillType type = skill.getSkillType();
							final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
							final L2Character[] targets =
							{
								target
							};
							
							if ((type == SkillType.PARALYZE) || (type == SkillType.STUN) || (type == SkillType.ROOT) || (type == SkillType.AGGDAMAGE))
							{
								if (Config.DEBUG)
								{
									LOG.info("L2CubicInstance: Action.run() handler " + type);
								}
								useCubicDisabler(type, L2CubicInstance.this, skill, targets);
							}
							else if (type == SkillType.MDAM)
							{
								if (Config.DEBUG)
								{
									LOG.info("L2CubicInstance: Action.run() handler " + type);
								}
								useCubicMdam(L2CubicInstance.this, skill, targets);
							}
							else if ((type == SkillType.POISON) || (type == SkillType.DEBUFF) || (type == SkillType.DOT))
							{
								if (Config.DEBUG)
								{
									LOG.info("L2CubicInstance: Action.run() handler " + type);
								}
								useCubicContinuous(L2CubicInstance.this, skill, targets);
							}
							else if (type == SkillType.DRAIN)
							{
								if (Config.DEBUG)
								{
									LOG.info("L2CubicInstance: Action.run() skill " + type);
								}
								((L2SkillDrain) skill).useCubicSkill(L2CubicInstance.this, targets);
							}
							else
							{
								handler.useSkill(owner, skill, targets);
								if (Config.DEBUG)
								{
									LOG.info("L2CubicInstance: Action.run(); other handler");
								}
							}
						}
					}
				}
			}
			catch (final Exception e)
			{
				LOG.error("", e);
			}
		}
	}
	
	/**
	 * Use cubic continuous.
	 * @param activeCubic the active cubic
	 * @param skill the skill
	 * @param targets the targets
	 */
	public void useCubicContinuous(final L2CubicInstance activeCubic, final L2Skill skill, final L2Object[] targets)
	{
		for (final L2Character target : (L2Character[]) targets)
		{
			if (target == null || target.isDead())
			{
				continue;
			}
			
			if (skill.isOffensive())
			{
				final boolean acted = Formulas.calcCubicSkillSuccess(activeCubic, target, skill);
				if (!acted)
				{
					activeCubic.getOwner().sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
					continue;
				}
				
			}
			
			// if this is a debuff let the duel manager know about it
			// so the debuff can be removed after the duel
			// (player & target must be in the same duel)
			if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
			{
				final DuelManager dm = DuelManager.getInstance();
				for (final L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
				{
					if (debuff != null)
					{
						dm.onBuff(((L2PcInstance) target), debuff);
					}
				}
			}
			else
			{
				skill.getEffects(activeCubic.getOwner(), target);
			}
		}
	}
	
	/**
	 * Use cubic mdam.
	 * @param activeCubic the active cubic
	 * @param skill the skill
	 * @param targets the targets
	 */
	public void useCubicMdam(final L2CubicInstance activeCubic, final L2Skill skill, final L2Object[] targets)
	{
		for (final L2Character target : (L2Character[]) targets)
		{
			if (target == null)
			{
				continue;
			}
			
			if (target.isAlikeDead())
			{
				if (target instanceof L2PcInstance)
				{
					target.stopFakeDeath(null);
				}
				else
				{
					continue;
				}
			}
			
			final boolean mcrit = Formulas.calcMCrit(activeCubic.getMCriticalHit(target, skill));
			final int damage = (int) Formulas.calcMagicDam(activeCubic, target, skill, mcrit);
			
			if (Config.DEBUG)
			{
				LOG.info("L2SkillMdam: useCubicSkill() -> damage = " + damage);
			}
			
			if (damage > 0)
			{
				// Manage attack or cast break of the target (calculating rate, sending message...)
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}
				
				activeCubic.getOwner().sendDamageMessage(target, damage, mcrit, false, false);
				
				if (skill.hasEffects())
				{
					// activate attacked effects, if any
					target.stopSkillEffects(skill.getId());
					if (target.getFirstEffect(skill) != null)
					{
						target.removeEffect(target.getFirstEffect(skill));
					}
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill))
					{
						skill.getEffects(activeCubic.getOwner(), target);
					}
				}
				
				target.reduceCurrentHp(damage, activeCubic.getOwner());
			}
		}
	}
	
	/**
	 * Use cubic disabler.
	 * @param type the type
	 * @param activeCubic the active cubic
	 * @param skill the skill
	 * @param targets the targets
	 */
	public void useCubicDisabler(final SkillType type, final L2CubicInstance activeCubic, final L2Skill skill, final L2Object[] targets)
	{
		if (Config.DEBUG)
		{
			LOG.info("Disablers: useCubicSkill()");
		}
		
		for (final L2Character target : (L2Character[]) targets)
		{
			if (target == null || target.isDead())
			{
				continue;
			}
			
			switch (type)
			{
				case STUN:
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill))
					{
						// if this is a debuff let the duel manager know about it
						// so the debuff can be removed after the duel
						// (player & target must be in the same duel)
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
						{
							final DuelManager dm = DuelManager.getInstance();
							for (final L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
							{
								if (debuff != null)
								{
									dm.onBuff(((L2PcInstance) target), debuff);
								}
							}
						}
						else
						{
							skill.getEffects(activeCubic.getOwner(), target);
						}
						if (Config.DEBUG)
						{
							LOG.info("Disablers: useCubicSkill() -> success");
						}
					}
					else
					{
						if (Config.DEBUG)
						{
							LOG.info("Disablers: useCubicSkill() -> failed");
						}
					}
					break;
				}
				case PARALYZE: // use same as root for now
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill))
					{
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
						{
							final DuelManager dm = DuelManager.getInstance();
							for (final L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
							{
								if (debuff != null)
								{
									dm.onBuff(((L2PcInstance) target), debuff);
								}
							}
						}
						else
						{
							skill.getEffects(activeCubic.getOwner(), target);
						}
						
						if (Config.DEBUG)
						{
							LOG.info("Disablers: useCubicSkill() -> success");
						}
					}
					else
					{
						if (Config.DEBUG)
						{
							LOG.info("Disablers: useCubicSkill() -> failed");
						}
					}
					break;
				}
				case CANCEL:
				{
					final L2Effect[] effects = target.getAllEffects();
					
					if (effects == null || effects.length == 0)
					{
						break;
					}
					
					final int max_negated_effects = 3;
					int count = 0;
					for (final L2Effect e : effects)
					{
						if (e.getSkill().isOffensive() && count < max_negated_effects)
						{
							// Do not remove raid curse skills
							if (e.getSkill().getId() != 4215 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 4082)
							{
								e.exit(true);
								if (count > -1)
								{
									count++;
								}
							}
						}
					}
					
					break;
				}
				case ROOT:
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill))
					{
						// if this is a debuff let the duel manager know about it
						// so the debuff can be removed after the duel
						// (player & target must be in the same duel)
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isInDuel() && skill.getSkillType() == SkillType.DEBUFF && activeCubic.getOwner().getDuelId() == ((L2PcInstance) target).getDuelId())
						{
							final DuelManager dm = DuelManager.getInstance();
							for (final L2Effect debuff : skill.getEffects(activeCubic.getOwner(), target))
							{
								if (debuff != null)
								{
									dm.onBuff(((L2PcInstance) target), debuff);
								}
							}
						}
						else
						{
							skill.getEffects(activeCubic.getOwner(), target);
						}
						
						if (Config.DEBUG)
						{
							LOG.info("Disablers: useCubicSkill() -> success");
						}
					}
					else
					{
						if (Config.DEBUG)
						{
							LOG.info("Disablers: useCubicSkill() -> failed");
						}
					}
					break;
				}
				case AGGDAMAGE:
				{
					if (Formulas.calcCubicSkillSuccess(activeCubic, target, skill))
					{
						if (target instanceof L2Attackable)
						{
							target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeCubic.getOwner(), (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
						}
						skill.getEffects(activeCubic.getOwner(), target);
						
						if (Config.DEBUG)
						{
							LOG.info("Disablers: useCubicSkill() -> success");
						}
					}
					else
					{
						if (Config.DEBUG)
						{
							LOG.info("Disablers: useCubicSkill() -> failed");
						}
					}
					break;
				}
			}
		}
	}
	
	/**
	 * returns true if the target is inside of the owner's max Cubic range.
	 * @param owner the owner
	 * @param target the target
	 * @return true, if is in cubic range
	 */
	public boolean isInCubicRange(final L2Character owner, final L2Character target)
	{
		if (owner == null || target == null)
		{
			return false;
		}
		
		int x, y, z;
		// temporary range check until real behavior of cubics is known/coded
		final int range = MAX_MAGIC_RANGE;
		
		// just in case because of some regions with MINUS location
		if (!owner.isInsideRadius(target, range, false, false))
		{
			return false;
		}
		
		x = (owner.getX() - target.getX());
		y = (owner.getY() - target.getY());
		z = (owner.getZ() - target.getZ());
		
		return ((x * x) + (y * y) + (z * z) <= (range * range));
	}
	
	/**
	 * this sets the friendly target for a cubic.
	 */
	public void cubicTargetForHeal()
	{
		L2Character target = null;
		double percentleft = 100.0;
		L2Party party = _owner.getParty();
		
		// if owner is in a duel but not in a party duel, then it is the same as he does not have a
		// party
		if (_owner.isInDuel())
		{
			if (!DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
			{
				party = null;
			}
		}
		
		if (party != null && !_owner.isInOlympiadMode())
		{
			// Get all visible objects in a spheric area near the L2Character
			// Get a list of Party Members
			final List<L2PcInstance> partyList = party.getPartyMembers();
			for (final L2Character partyMember : partyList)
			{
				if (!partyMember.isDead())
				{
					// if party member not dead, check if he is in castrange of heal cubic
					if (isInCubicRange(_owner, partyMember))
					{
						// member is in cubic casting range, check if he need heal and if he have
						// the lowest HP
						if (partyMember.getCurrentHp() < partyMember.getMaxHp())
						{
							if (percentleft > (partyMember.getCurrentHp() / partyMember.getMaxHp()))
							{
								percentleft = (partyMember.getCurrentHp() / partyMember.getMaxHp());
								target = partyMember;
							}
						}
					}
				}
				
				if (partyMember.getPet() != null)
				{
					if (partyMember.getPet().isDead())
					{
						continue;
					}
					
					// if party member's pet not dead, check if it is in castrange of heal cubic
					if (!isInCubicRange(_owner, partyMember.getPet()))
					{
						continue;
					}
					
					// member's pet is in cubic casting range, check if he need heal and if he have
					// the lowest HP
					if (partyMember.getPet().getCurrentHp() < partyMember.getPet().getMaxHp())
					{
						if (percentleft > (partyMember.getPet().getCurrentHp() / partyMember.getPet().getMaxHp()))
						{
							percentleft = (partyMember.getPet().getCurrentHp() / partyMember.getPet().getMaxHp());
							target = partyMember.getPet();
						}
					}
				}
			}
		}
		else
		{
			if (_owner.getCurrentHp() < _owner.getMaxHp())
			{
				percentleft = (_owner.getCurrentHp() / _owner.getMaxHp());
				target = _owner;
			}
			if (_owner.getPet() != null)
			{
				if (!_owner.getPet().isDead() && _owner.getPet().getCurrentHp() < _owner.getPet().getMaxHp() && percentleft > (_owner.getPet().getCurrentHp() / _owner.getPet().getMaxHp()) && isInCubicRange(_owner, _owner.getPet()))
				{
					target = _owner.getPet();
				}
			}
		}
		
		_target = target;
	}
	
	/**
	 * Given by other.
	 * @return true, if successful
	 */
	public boolean givenByOther()
	{
		return _givenByOther;
	}
	
	/**
	 * The Class Heal.
	 */
	private class Heal implements Runnable
	{
		
		/**
		 * Instantiates a new heal.
		 */
		Heal()
		{
			// run task
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			if (_owner.isDead() || _owner.isOnline() == 0)
			{
				stopAction();
				_owner.delCubic(_id);
				_owner.broadcastUserInfo();
				cancelDisappear();
				return;
			}
			
			try
			{
				L2Skill skill = null;
				for (final L2Skill sk : _skills)
				{
					if (sk.getId() == SKILL_CUBIC_HEAL)
					{
						skill = sk;
						break;
					}
				}
				
				if (skill != null)
				{
					cubicTargetForHeal();
					final L2Character target = _target;
					if (target != null && !target.isDead())
					{
						if (target.getMaxHp() - target.getCurrentHp() > skill.getPower())
						{
							final L2Character[] targets =
							{
								target
							};
							final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
							if (handler != null)
							{
								handler.useSkill(_owner, skill, targets);
							}
							else
							{
								skill.useSkill(_owner, targets);
							}
							
							final MagicSkillUser msu = new MagicSkillUser(_owner, target, skill.getId(), skill.getLevel(), 0, 0);
							_owner.broadcastPacket(msu);
						}
					}
				}
			}
			catch (final Exception e)
			{
				LOG.error("", e);
			}
		}
	}
	
	/**
	 * The Class Disappear.
	 */
	private class Disappear implements Runnable
	{
		
		/**
		 * Instantiates a new disappear.
		 */
		Disappear()
		{
			// run task
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			stopAction();
			_owner.delCubic(_id);
			_owner.broadcastUserInfo();
		}
	}
}
