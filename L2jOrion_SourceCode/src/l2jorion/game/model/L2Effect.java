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

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javolution.util.FastList;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class ...
 * @version $Revision: 1.1.2.1.2.12 $ $Date: 2005/04/11 10:06:07 $
 * @author L2jOrion dev
 */
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
		NEGATE
	}
	
	private static final Func[] _emptyFunctionSet = new Func[0];
	
	// member _effector is the instance of L2Character that cast/used the spell/skill that is
	// causing this effect. Do not confuse with the instance of L2Character that
	// is being affected by this effect.
	private final L2Character _effector;
	
	// member _effected is the instance of L2Character that was affected
	// by this effect. Do not confuse with the instance of L2Character that
	// catsed/used this effect.
	protected final L2Character _effected;
	
	// the skill that was used.
	public L2Skill _skill;
	
	// or the items that was used.
	// private final L2Item _item;
	
	// the value of an update
	private final Lambda _lambda;
	
	// the current state
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
				L2Effect.this.scheduleEffect();
			}
			catch (final Throwable e)
			{
				LOG.error("", e);
			}
		}
	}
	
	private ScheduledFuture<?> _currentFuture;
	private EffectTask _currentTask;
	
	/** The Identifier of the stack group */
	private final String _stackType;
	
	/** The position of the effect in the stack group */
	private final float _stackOrder;
	
	private final EffectTemplate _template;
	
	private boolean _inUse = false;
	
	protected L2Effect(final Env env, final EffectTemplate template)
	{
		_template = template;
		_state = EffectState.CREATED;
		_skill = env.skill;
		// _item = env._item == null ? null : env._item.getItem();
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
			if (newfirsttime > _period) // sanity check
				newfirsttime = _period;
			_periodStartTicks = GameTimeController.getInstance().getGameTicks() - newfirsttime * GameTimeController.TICKS_PER_SECOND;
			_currentFuture.cancel(false);
			_currentFuture = null;
			_currentTask = null;
			_periodfirsttime = newfirsttime;
			final int duration = _period - _periodfirsttime;
			
			_currentTask = new EffectTask(duration * 1000, -1);
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
	
	/**
	 * Returns the elapsed time of the task.
	 * @return Time in seconds.
	 */
	public int getTaskTime()
	{
		if (_count == _totalCount)
			return 0;
		
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
			return true;
		
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
	
	private synchronized void startEffectTask(final int duration)
	{
		if (duration >= 0)
		{
			stopEffectTask();
			_currentTask = new EffectTask(duration, -1);
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration);
		}
		if (_state == EffectState.ACTING)
		{
			// To avoid possible NPE caused by player crash
			if (_effected != null)
				_effected.addEffect(this);
			else
				LOG.warn("Effected is null for skill " + _skill.getId() + " on effect " + getEffectType());
		}
	}
	
	private synchronized void startEffectTaskAtFixedRate(final int delay, final int rate)
	{
		stopEffectTask();
		_currentTask = new EffectTask(delay, rate);
		_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(_currentTask, delay, rate);
		
		if (_state == EffectState.ACTING)
		{
			_effected.addEffect(this);
		}
	}
	
	/**
	 * Stop the L2Effect task and send Server->Client update packet.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Cancel the effect in the the abnormal effect map of the L2Character</li> <li>Stop the task of the L2Effect, remove it and update client magic icone</li><BR>
	 * <BR>
	 */
	public final void exit()
	{
		this.exit(false, false);
	}
	
	public final void exit(final boolean cancelEffect)
	{
		this.exit(false, cancelEffect);
	}
	
	public final void exit(final boolean preventUpdate, final boolean cancelEffect)
	{
		preventExitUpdate = preventUpdate;
		_state = EffectState.FINISHING;
		_cancelEffect = cancelEffect;
		scheduleEffect();
	}
	
	/**
	 * Stop the task of the L2Effect, remove it and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Cancel the task</li> <li>Stop and remove L2Effect from L2Character and update client magic icone</li><BR>
	 * <BR>
	 */
	public synchronized void stopEffectTask()
	{
		// Cancel the task
		if (_currentFuture != null)
		{
			if (!_currentFuture.isCancelled())
				_currentFuture.cancel(false);
			
			_currentFuture = null;
			_currentTask = null;
			
			// To avoid possible NPE caused by player crash
			if (_effected != null)
				_effected.removeEffect(this);
			else
				LOG.warn("Effected is null for skill " + _skill.getId() + " on effect " + getEffectType());
		}
	}
	
	/**
	 * @return effect type
	 */
	public abstract EffectType getEffectType();
	
	/** Notify started */
	public void onStart()
	{
		if (_abnormalEffect != 0)
		{
			getEffected().startAbnormalEffect(_abnormalEffect);
		}
	}
	
	/**
	 * Cancel the effect in the the abnormal effect map of the effected L2Character.<BR>
	 * <BR>
	 */
	public void onExit()
	{
		if (_abnormalEffect != 0)
		{
			getEffected().stopAbnormalEffect(_abnormalEffect);
		}
	}
	
	/**
	 * Return true for continuation of this effect
	 * @return
	 */
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
		if (_state == EffectState.CREATED)
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
				startEffectTask(_period * 1000);
				return;
			}
		}
		
		if (_state == EffectState.ACTING)
		{
			if (_count-- > 0)
			{
				if (getInUse())
				{
					if (onActionTime())
					{
						return; // false causes effect to finish right away
					}
				}
				else if (_count > 0)
				{
					return;
				}
			}
			_state = EffectState.FINISHING;
		}
		
		if (_state == EffectState.FINISHING)
		{
			// Cancel the effect in the the abnormal effect map of the L2Character
			onExit();
			
			// If the time left is equal to zero, send the message
			if (getEffected() != null && getEffected() instanceof L2PcInstance && getShowIcon() && !getEffected().isDead())
			{
				
				// Like L2OFF message S1_HAS_BEEN_ABORTED for toogle skills
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
					smsg3 = null;
				}
				else if (_count == 0)
				{
					SystemMessage smsg3 = new SystemMessage(SystemMessageId.S1_HAS_WORN_OFF);
					smsg3.addString(_skill.getName());
					getEffected().sendPacket(smsg3);
					smsg3 = null;
				}
				
			}
			
			// Stop the task of the L2Effect, remove it and update client magic icone
			stopEffectTask();
			
		}
	}
	
	public Func[] getStatFuncs()
	{
		if (_funcTemplates == null)
			return _emptyFunctionSet;
		final List<Func> funcs = new FastList<>();
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
			return _emptyFunctionSet;
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	public final void addIcon(final MagicEffectIcons mi)
	{
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;
		
		if (task == null || future == null)
			return;
		
		if (_state == EffectState.FINISHING || _state == EffectState.CREATED)
			return;
		
		if (!getShowIcon())
			return;
		
		final L2Skill sk = getSkill();
		
		if (task._rate > 0)
		{
			if (sk.isPotion())
			{
				mi.addEffect(sk.getId(), getLevel(), sk.getBuffDuration() - getTaskTime() * 1000, false);
			}
			else if (!sk.isToggle())
			{
				if (sk.is_Debuff())
					mi.addEffect(sk.getId(), getLevel(), (_count * _period) * 1000, true);
				else
					mi.addEffect(sk.getId(), getLevel(), (_count * _period) * 1000, false);
			}
			else
			{
				mi.addEffect(sk.getId(), getLevel(), -1, true);
			}
		}
		else
		{
			if (sk.getSkillType() == SkillType.DEBUFF)
				mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS) + 1000, true);
			else
				mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS) + 1000, false);
		}
		
		task = null;
		future = null;
	}
	
	public final void addPartySpelledIcon(final PartySpelled ps)
	{
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;
		
		if (task == null || future == null)
			return;
		
		if (_state == EffectState.FINISHING || _state == EffectState.CREATED)
			return;
		
		L2Skill sk = getSkill();
		ps.addPartySpelledEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		
		task = null;
		future = null;
		sk = null;
	}
	
	public final void addOlympiadSpelledIcon(final ExOlympiadSpelledInfo os)
	{
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;
		
		if (task == null || future == null)
			return;
		
		if (_state == EffectState.FINISHING || _state == EffectState.CREATED)
			return;
		
		L2Skill sk = getSkill();
		os.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		
		sk = null;
		task = null;
		future = null;
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
	
	public void setPeriodStartTicks(final int periodStartTicks)
	{
		_periodStartTicks = periodStartTicks;
	}
	
	public final boolean getShowIcon()
	{
		return _template.showIcon;
	}
	
	public EffectState get_state()
	{
		return _state;
	}
	
}
