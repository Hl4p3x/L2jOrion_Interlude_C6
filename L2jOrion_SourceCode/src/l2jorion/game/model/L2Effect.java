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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import l2jorion.Config;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExOlympiadSpelledInfo;
import l2jorion.game.network.serverpackets.MagicEffectIcons;
import l2jorion.game.network.serverpackets.PartySpelled;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.effects.EffectTemplate;
import l2jorion.game.skills.funcs.Func;
import l2jorion.game.skills.funcs.FuncTemplate;
import l2jorion.game.skills.funcs.Lambda;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public abstract class L2Effect
{
	static final Logger LOG = LoggerFactory.getLogger(L2Effect.class);
	
	public static enum EffectState
	{
		CREATED,
		ACTING,
		FINISHING
	}
	
	public static enum EffectType
	{
		BUFF,
		DEBUFF,
		CHARGE,
		DMG_OVER_TIME,
		HEAL_OVER_TIME,
		COMBAT_POINT_HEAL_OVER_TIME,
		MANA_DMG_OVER_TIME,
		MANA_HEAL_OVER_TIME,
		MP_CONSUME_PER_LEVEL,
		RELAXING,
		STUN,
		ROOT,
		SLEEP,
		HATE,
		FAKE_DEATH,
		CONFUSION,
		CONFUSE_MOB_ONLY,
		MUTE,
		IMMOBILEUNTILATTACKED,
		FEAR,
		SALVATION,
		SILENT_MOVE,
		SIGNET_EFFECT,
		SIGNET_GROUND,
		SEED,
		PARALYZE,
		STUN_SELF,
		PSYCHICAL_MUTE,
		REMOVE_TARGET,
		TARGET_ME,
		THROW_UP,
		WARP,
		SILENCE_MAGIC_PHYSICAL,
		BETRAY,
		NOBLESSE_BLESSING,
		PHOENIX_BLESSING,
		PETRIFICATION,
		BLUFF,
		BATTLE_FORCE,
		SPELL_FORCE,
		CHARM_OF_LUCK,
		INVINCIBLE,
		PROTECTION_BLESSING,
		INTERRUPT,
		MEDITATION,
		BLOW,
		FUSION,
		CANCEL,
		BLOCK_BUFF,
		BLOCK_DEBUFF,
		PREVENT_BUFF,
		CLAN_GATE,
		SPOIL,
		NEGATE
	}
	
	private static final Func[] _emptyFunctionSet = new Func[0];
	
	private final L2Character _effector;
	
	protected final L2Character _effected;
	
	public L2Skill _skill;
	
	private final Lambda _lambda;
	
	private EffectState _state;
	
	// period, seconds
	private final int _period;
	private int _periodStartTicks;
	private int _periodfirsttime;
	
	// function templates
	private final FuncTemplate[] _funcTemplates;
	
	// initial count
	protected int _totalCount;
	// counter
	private int _count;
	
	// abnormal effect mask
	private final int _abnormalEffect;
	
	public boolean preventExitUpdate;
	
	private boolean _cancelEffect = false;
	
	public final class EffectTask implements Runnable
	{
		protected final int _delay;
		protected final int _rate;
		
		EffectTask(final int pDelay, final int pRate)
		{
			_delay = pDelay;
			_rate = pRate;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (getPeriodfirsttime() == 0)
				{
					setPeriodStartTicks(GameTimeController.getInstance().getGameTicks());
				}
				else
				{
					setPeriodfirsttime(0);
				}
				
				scheduleEffect();
			}
			catch (final Throwable e)
			{
				LOG.error("", e);
			}
		}
	}
	
	private ScheduledFuture<?> _currentFuture;
	private EffectTask _currentTask;
	
	private final String _stackType;
	
	private final float _stackOrder;
	
	private final EffectTemplate _template;
	
	private boolean _inUse = false;
	
	protected L2Effect(final Env env, final EffectTemplate template)
	{
		_template = template;
		_state = EffectState.CREATED;
		_skill = env.skill;
		_effected = env.target;
		_effector = env.player;
		_lambda = template.lambda;
		_funcTemplates = template.funcTemplates;
		_count = template.counter;
		_totalCount = _count;
		int temp = template.period;
		
		if (env.skillMastery)
		{
			temp *= 2;
		}
		
		_period = temp;
		_abnormalEffect = template.abnormalEffect;
		_stackType = template.stackType;
		_stackOrder = template.stackOrder;
		_periodStartTicks = GameTimeController.getInstance().getGameTicks();
		_periodfirsttime = 0;
		
		scheduleEffect();
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getTotalCount()
	{
		return _totalCount;
	}
	
	public void setCount(final int newcount)
	{
		_count = newcount;
	}
	
	public void setFirstTime(int newfirsttime)
	{
		if (_currentFuture != null)
		{
			if (newfirsttime > _period)
			{
				newfirsttime = _period;
			}
			
			_periodStartTicks = GameTimeController.getInstance().getGameTicks() - newfirsttime * GameTimeController.TICKS_PER_SECOND;
			_currentFuture.cancel(false);
			_currentFuture = null;
			_currentTask = null;
			
			_periodfirsttime = newfirsttime;
			final int duration = _period - _periodfirsttime;
			
			_currentTask = new EffectTask(duration * 1000, -1);
			
			if (Config.PREMIUM_BUFF_MULTIPLIER > 1 || Config.RON_CUSTOM)
			{
				if (getEffected() instanceof L2PcInstance)
				{
					L2PcInstance player = (L2PcInstance) getEffected();
					
					float multipier = Config.PREMIUM_BUFF_MULTIPLIER;
					
					if (Config.RON_CUSTOM)
					{
						if (player.getHourBuffs() == 1 || player.getPremiumService() >= 2)
						{
							multipier = 4;
						}
					}
					
					if (Config.ENABLE_MODIFY_SKILL_DURATION && (player.getPremiumService() >= 1 || player.getHourBuffs() == 1))
					{
						if (Config.SKILL_DURATION_LIST.containsKey(_skill.getId()))
						{
							int time = (int) (duration * 1000 * multipier);
							_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, time);
							return;
						}
					}
				}
			}
			
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration * 1000);
		}
	}
	
	public int getPeriod()
	{
		return _period;
	}
	
	public int getTime()
	{
		return (GameTimeController.getInstance().getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}
	
	public int getTaskTime()
	{
		if (_count == _totalCount)
		{
			return 0;
		}
		
		return Math.abs(_count - _totalCount + 1) * _period + getTime() + 1;
	}
	
	public boolean getInUse()
	{
		return _inUse;
	}
	
	public void setInUse(final boolean inUse)
	{
		_inUse = inUse;
	}
	
	public String getStackType()
	{
		return _stackType;
	}
	
	public float getStackOrder()
	{
		return _stackOrder;
	}
	
	public final L2Skill getSkill()
	{
		return _skill;
	}
	
	public final L2Character getEffector()
	{
		return _effector;
	}
	
	public final L2Character getEffected()
	{
		return _effected;
	}
	
	public boolean isSelfEffect()
	{
		return _skill._effectTemplatesSelf != null;
	}
	
	public boolean isHerbEffect()
	{
		if (getSkill().getName().contains("Herb"))
		{
			return true;
		}
		
		return false;
	}
	
	public final double calc()
	{
		final Env env = new Env();
		
		env.player = _effector;
		env.target = _effected;
		env.skill = _skill;
		
		return _lambda.calc(env);
	}
	
	private void startEffectTask(int duration)
	{
		if (duration >= 0)
		{
			stopEffectTask();
			
			_currentTask = new EffectTask(duration, -1);
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration);
		}
		
		if (_state == EffectState.ACTING)
		{
			if (_effected != null)
			{
				_effected.addEffect(this);
			}
			else
			{
				LOG.warn("Effected is null for skill " + _skill.getId() + " on effect " + getEffectType());
			}
		}
	}
	
	private void startEffectTaskAtFixedRate(int delay, int rate)
	{
		stopEffectTask();
		
		_currentTask = new EffectTask(delay, rate);
		_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(_currentTask, delay, rate);
		
		if (_state == EffectState.ACTING)
		{
			if (_effected != null)
			{
				_effected.addEffect(this);
			}
			else
			{
				LOG.warn("Effected is null for skill " + _skill.getId() + " on effect " + getEffectType());
			}
		}
	}
	
	public final void exit()
	{
		exit(false, false);
	}
	
	public final void exit(boolean cancelEffect)
	{
		exit(false, cancelEffect);
	}
	
	public final void exit(boolean preventUpdate, boolean cancelEffect)
	{
		preventExitUpdate = preventUpdate;
		
		_state = EffectState.FINISHING;
		_cancelEffect = cancelEffect;
		
		scheduleEffect();
	}
	
	public void stopEffectTask()
	{
		// Cancel the task
		if (_currentFuture != null)
		{
			_currentFuture.cancel(false);
			
			_currentFuture = null;
			_currentTask = null;
			
			if (_effected != null)
			{
				_effected.removeEffect(this);
			}
			else
			{
				LOG.warn("Effected is null for skill " + _skill.getId() + " on effect " + getEffectType());
			}
		}
	}
	
	public abstract EffectType getEffectType();
	
	public void onStart()
	{
		if (_abnormalEffect != 0)
		{
			getEffected().startAbnormalEffect(_abnormalEffect);
		}
	}
	
	public void onExit()
	{
		if (_abnormalEffect != 0)
		{
			getEffected().stopAbnormalEffect(_abnormalEffect);
		}
	}
	
	public abstract boolean onActionTime();
	
	public final void rescheduleEffect()
	{
		if (_state != EffectState.ACTING)
		{
			scheduleEffect();
		}
		else
		{
			if (_count > 1)
			{
				startEffectTaskAtFixedRate(5, _period * 1000);
				return;
			}
			
			if (_period > 0)
			{
				startEffectTask(_period * 1000);
				return;
			}
		}
	}
	
	public final void scheduleEffect()
	{
		switch (_state)
		{
			case CREATED:
			{
				_state = EffectState.ACTING;
				
				onStart();
				
				if (_skill.isPvpSkill() && getEffected() != null && getEffected() instanceof L2PcInstance && getShowIcon())
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					smsg.addString(_skill.getName());
					getEffected().sendPacket(smsg);
				}
				
				if (_count > 1)
				{
					startEffectTaskAtFixedRate(5, _period * 1000);
					return;
				}
				
				if (_period > 0)
				{
					if (Config.PREMIUM_BUFF_MULTIPLIER > 1 || Config.RON_CUSTOM)
					{
						if (getEffected() instanceof L2PcInstance)
						{
							L2PcInstance player = (L2PcInstance) getEffected();
							
							float multipier = Config.PREMIUM_BUFF_MULTIPLIER;
							
							if (Config.RON_CUSTOM)
							{
								if (player.getHourBuffs() == 1 || player.getPremiumService() >= 2)
								{
									multipier = 4;
								}
							}
							
							if (Config.ENABLE_MODIFY_SKILL_DURATION && player.getPremiumService() >= 1 && !player.isEnteringToWorld() || player.getHourBuffs() == 1)
							{
								if (Config.SKILL_DURATION_LIST.containsKey(_skill.getId()))
								{
									int time = (int) (_period * 1000 * multipier);
									startEffectTask(time);
									return;
								}
							}
						}
					}
					
					if (isHerbEffect())
					{
						if (getEffected().getPet() != null)
						{
							startEffectTask(_period * 1000 / 2);
							return;
						}
						
						if (getEffected() instanceof L2Summon)
						{
							startEffectTask(_period * 1000 / 2);
							return;
						}
					}
					
					startEffectTask(_period * 1000);
					return;
				}
			}
			case ACTING:
			{
				if (_count > 0)
				{
					_count--;
					
					if (getInUse())
					{
						if (onActionTime())
						{
							return;
						}
					}
					else if (_count > 0)
					{
						return;
					}
				}
				
				_state = EffectState.FINISHING;
			}
			case FINISHING:
			{
				onExit();
				
				if (getEffected() != null && getEffected() instanceof L2PcInstance && getShowIcon())
				{
					if (getSkill().isToggle())
					{
						final SystemMessage smsg3 = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ABORTED);
						smsg3.addString(getSkill().getName());
						getEffected().sendPacket(smsg3);
					}
					else if (_cancelEffect)
					{
						SystemMessage smsg3 = new SystemMessage(SystemMessageId.EFFECT_S1_DISAPPEARED);
						smsg3.addString(getSkill().getName());
						getEffected().sendPacket(smsg3);
					}
					else if (_count == 0)
					{
						SystemMessage smsg3 = new SystemMessage(SystemMessageId.S1_HAS_WORN_OFF);
						smsg3.addString(_skill.getName());
						getEffected().sendPacket(smsg3);
					}
				}
				
				stopEffectTask();
			}
		}
	}
	
	public Func[] getStatFuncs()
	{
		if (_funcTemplates == null)
		{
			return _emptyFunctionSet;
		}
		
		final List<Func> funcs = new ArrayList<>();
		
		for (final FuncTemplate t : _funcTemplates)
		{
			final Env env = new Env();
			env.player = getEffector();
			env.target = getEffected();
			env.skill = getSkill();
			
			final Func f = t.getFunc(env, this); // effect is owner
			
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
	
	public final void addIcon(MagicEffectIcons mi)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;
		
		if (task == null || future == null)
		{
			return;
		}
		
		if (!getShowIcon())
		{
			return;
		}
		
		final L2Skill sk = getSkill();
		
		if (task._rate > 0)
		{
			if (sk.isPotion())
			{
				mi.addEffect(sk.getId(), getLevel(), sk.getBuffDuration() - getTaskTime() * 1000);
			}
			else if (!sk.isToggle())
			{
				if (sk.is_Debuff())
				{
					mi.addEffect(sk.getId(), getLevel(), (_count * _period) * 1000);
				}
				else
				{
					mi.addEffect(sk.getId(), getLevel(), (_count * _period) * 1000);
				}
			}
			else
			{
				mi.addEffect(sk.getId(), getLevel(), -1);
			}
		}
		else
		{
			if (sk.getSkillType() == SkillType.DEBUFF)
			{
				mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
			}
			else
			{
				mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
			}
		}
	}
	
	public final void addPartySpelledIcon(PartySpelled ps)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;
		
		if (task == null || future == null)
		{
			return;
		}
		
		L2Skill sk = getSkill();
		ps.addPartySpelledEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
	}
	
	public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;
		
		if (task == null || future == null)
		{
			return;
		}
		
		L2Skill sk = getSkill();
		os.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
	}
	
	public int getLevel()
	{
		return getSkill().getLevel();
	}
	
	public int getPeriodfirsttime()
	{
		return _periodfirsttime;
	}
	
	public void setPeriodfirsttime(final int periodfirsttime)
	{
		_periodfirsttime = periodfirsttime;
	}
	
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	public void setPeriodStartTicks(int periodStartTicks)
	{
		_periodStartTicks = periodStartTicks;
	}
	
	public final boolean getShowIcon()
	{
		return _template.showIcon;
	}
}
