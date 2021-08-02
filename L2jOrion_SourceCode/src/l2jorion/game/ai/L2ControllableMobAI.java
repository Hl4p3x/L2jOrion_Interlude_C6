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
package l2jorion.game.ai;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.datatables.MobGroupTable;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.MobGroup;
import l2jorion.game.model.actor.instance.L2ControllableMobInstance;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;

public class L2ControllableMobAI extends L2AttackableAI
{
	public static final int AI_IDLE = 1;
	public static final int AI_NORMAL = 2;
	public static final int AI_FORCEATTACK = 3;
	public static final int AI_FOLLOW = 4;
	public static final int AI_CAST = 5;
	public static final int AI_ATTACK_GROUP = 6;
	
	private int _alternateAI;
	
	private boolean _isThinking; // to prevent thinking recursively
	private boolean _isNotMoving;
	
	private L2Character _forcedTarget;
	private MobGroup _targetGroup;
	
	protected void thinkFollow()
	{
		final L2Attackable me = (L2Attackable) _actor;
		
		if (!Util.checkIfInRange(MobGroupTable.FOLLOW_RANGE, me, getForcedTarget(), true))
		{
			final int signX = Rnd.nextInt(2) == 0 ? -1 : 1;
			final int signY = Rnd.nextInt(2) == 0 ? -1 : 1;
			final int randX = Rnd.nextInt(MobGroupTable.FOLLOW_RANGE);
			final int randY = Rnd.nextInt(MobGroupTable.FOLLOW_RANGE);
			
			moveTo(getForcedTarget().getX() + signX * randX, getForcedTarget().getY() + signY * randY, getForcedTarget().getZ());
		}
	}
	
	@Override
	protected void onEvtThink()
	{
		if (isThinking() || _actor.isAllSkillsDisabled())
			return;
		
		setThinking(true);
		
		try
		{
			switch (getAlternateAI())
			{
				case AI_IDLE:
					if (getIntention() != CtrlIntention.AI_INTENTION_ACTIVE)
					{
						setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					}
					break;
				case AI_FOLLOW:
					thinkFollow();
					break;
				case AI_CAST:
					thinkCast();
					break;
				case AI_FORCEATTACK:
					thinkForceAttack();
					break;
				case AI_ATTACK_GROUP:
					thinkAttackGroup();
					break;
				default:
					if (getIntention() == AI_INTENTION_ACTIVE)
					{
						thinkActive();
					}
					else if (getIntention() == AI_INTENTION_ATTACK)
					{
						thinkAttack();
					}
					break;
			}
		}
		finally
		{
			setThinking(false);
		}
	}
	
	@Override
	protected void thinkCast()
	{
		if (getAttackTarget() == null || getAttackTarget().isAlikeDead())
		{
			setAttackTarget(findNextRndTarget());
			clientStopMoving(null);
		}
		
		if (getAttackTarget() == null)
			return;
		
		((L2Attackable) _actor).setTarget(getAttackTarget());
		
		if (!_actor.isMuted())
		{
			// check distant skills
			int max_range = 0;
			for (final L2Skill sk : _actor.getAllSkills())
			{
				if (Util.checkIfInRange(sk.getCastRange(), _actor, getAttackTarget(), true) && !_actor.isSkillDisabled(sk) && _actor.getCurrentMp() > _actor.getStat().getMpConsume(sk))
				{
					_actor.doCast(sk);
					return;
				}
				max_range = Math.max(max_range, sk.getCastRange());
			}
			
			if (!isNotMoving())
			{
				moveToPawn(getAttackTarget(), max_range);
			}
			return;
		}
	}
	
	protected void thinkAttackGroup()
	{
		final L2Character target = getForcedTarget();
		if (target == null || target.isAlikeDead())
		{
			// try to get next group target
			setForcedTarget(findNextGroupTarget());
			clientStopMoving(null);
		}
		
		if (target == null)
			return;
		
		_actor.setTarget(target);
		// as a response, we put the target in a forced attack mode
		final L2ControllableMobInstance theTarget = (L2ControllableMobInstance) target;
		final L2ControllableMobAI ctrlAi = (L2ControllableMobAI) theTarget.getAI();
		ctrlAi.forceAttack(_actor);
		
		final L2Skill[] skills = _actor.getAllSkills();
		final double dist2 = _actor.getPlanDistanceSq(target.getX(), target.getY());
		final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().collisionRadius + target.getTemplate().collisionRadius;
		int max_range = range;
		
		if (!_actor.isMuted() && dist2 > (range + 20) * (range + 20))
		{
			// check distant skills
			for (final L2Skill sk : skills)
			{
				final int castRange = sk.getCastRange();
				if (castRange * castRange >= dist2 && !_actor.isSkillDisabled(sk) && _actor.getCurrentMp() > _actor.getStat().getMpConsume(sk))
				{
					_actor.doCast(sk);
					return;
				}
				max_range = Math.max(max_range, castRange);
			}
			
			if (!isNotMoving())
			{
				moveToPawn(target, range);
			}
			return;
		}
		_actor.doAttack(target);
	}
	
	protected void thinkForceAttack()
	{
		if (getForcedTarget() == null || getForcedTarget().isAlikeDead())
		{
			clientStopMoving(null);
			setIntention(AI_INTENTION_ACTIVE);
			setAlternateAI(AI_IDLE);
		}
		
		_actor.setTarget(getForcedTarget());
		final L2Skill[] skills = _actor.getAllSkills();
		final double dist2 = _actor.getPlanDistanceSq(getForcedTarget().getX(), getForcedTarget().getY());
		final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().collisionRadius + getForcedTarget().getTemplate().collisionRadius;
		int max_range = range;
		
		if (!_actor.isMuted() && dist2 > (range + 20) * (range + 20))
		{
			// check distant skills
			for (final L2Skill sk : skills)
			{
				final int castRange = sk.getCastRange();
				
				if (castRange * castRange >= dist2 && !_actor.isSkillDisabled(sk) && _actor.getCurrentMp() > _actor.getStat().getMpConsume(sk))
				{
					_actor.doCast(sk);
					return;
				}
				max_range = Math.max(max_range, castRange);
			}
			
			if (!isNotMoving())
			{
				moveToPawn(getForcedTarget(), _actor.getPhysicalAttackRange()/* range */);
			}
			return;
		}
		_actor.doAttack(getForcedTarget());
	}
	
	protected void thinkAttack()
	{
		if (getAttackTarget() == null || getAttackTarget().isAlikeDead())
		{
			if (getAttackTarget() != null)
			{
				// stop hating
				L2Attackable npc = (L2Attackable) _actor;
				npc.stopHating(getAttackTarget());
				npc = null;
			}
			
			setIntention(AI_INTENTION_ACTIVE);
		}
		else
		{
			// notify aggression
			if (((L2NpcInstance) _actor).getFactionId() != null)
			{
				for (final L2Object obj : _actor.getKnownList().getKnownObjects().values())
				{
					if (!(obj instanceof L2NpcInstance))
					{
						continue;
					}
					
					L2NpcInstance npc = (L2NpcInstance) obj;
					String faction_id = ((L2NpcInstance) _actor).getFactionId();
					
					if (!faction_id.equalsIgnoreCase(npc.getFactionId()))
					{
						continue;
					}
					
					if (_actor.isInsideRadius(npc, npc.getFactionRange(), false, true) && Math.abs(getAttackTarget().getZ() - npc.getZ()) < 200)
					{
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1);
					}
				}
			}
			
			_actor.setTarget(getAttackTarget());
			final L2Skill[] skills = _actor.getAllSkills();
			final double dist2 = _actor.getPlanDistanceSq(getAttackTarget().getX(), getAttackTarget().getY());
			final int range = _actor.getPhysicalAttackRange() + _actor.getTemplate().collisionRadius + getAttackTarget().getTemplate().collisionRadius;
			int max_range = range;
			
			if (!_actor.isMuted() && dist2 > (range + 20) * (range + 20))
			{
				// check distant skills
				for (final L2Skill sk : skills)
				{
					final int castRange = sk.getCastRange();
					if (castRange * castRange >= dist2 && !_actor.isSkillDisabled(sk) && _actor.getCurrentMp() > _actor.getStat().getMpConsume(sk))
					{
						_actor.doCast(sk);
						return;
					}
					max_range = Math.max(max_range, castRange);
				}
				moveToPawn(getAttackTarget(), range);
				return;
			}
			
			// Force mobs to attack anybody if confused.
			L2Character hated;
			if (_actor.isConfused())
			{
				hated = findNextRndTarget();
			}
			else
			{
				hated = getAttackTarget();
			}
			
			if (hated == null)
			{
				setIntention(AI_INTENTION_ACTIVE);
				return;
			}
			
			if (hated != getAttackTarget())
			{
				setAttackTarget(hated);
			}
			
			if (!_actor.isMuted() && skills.length > 0 && Rnd.nextInt(5) == 3)
			{
				for (final L2Skill sk : skills)
				{
					final int castRange = sk.getCastRange();
					
					if (castRange * castRange >= dist2 && !_actor.isSkillDisabled(sk) && _actor.getCurrentMp() < _actor.getStat().getMpConsume(sk))
					{
						_actor.doCast(sk);
						return;
					}
				}
			}
			_actor.doAttack(getAttackTarget());
		}
	}
	
	private void thinkActive()
	{
		setAttackTarget(findNextRndTarget());
		L2Character hated;
		
		if (_actor.isConfused())
		{
			hated = findNextRndTarget();
		}
		else
		{
			hated = getAttackTarget();
		}
		
		if (hated != null)
		{
			_actor.setRunning();
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);
		}
	}
	
	private boolean autoAttackCondition(final L2Character target)
	{
		if (target == null || !(_actor instanceof L2Attackable))
			return false;
		
		final L2Attackable me = (L2Attackable) _actor;
		
		if (target instanceof L2FolkInstance || target instanceof L2DoorInstance)
			return false;
		
		if (target.isAlikeDead() || !me.isInsideRadius(target, me.getAggroRange(), false, false) || Math.abs(_actor.getZ() - target.getZ()) > 100)
			return false;
		
		// Check if the target isn't invulnerable
		if (target.isInvul())
			return false;
		
		// Check if the target is a L2PcInstance
		if (target instanceof L2PcInstance)
		{
			// Check if the target isn't in silent move mode
			if (((L2PcInstance) target).isSilentMoving())
				return false;
		}
		
		if (target instanceof L2NpcInstance)
			return false;
		
		return me.isAggressive();
	}
	
	private L2Character findNextRndTarget()
	{
		int aggroRange = ((L2Attackable) _actor).getAggroRange();
		L2Attackable npc = (L2Attackable) _actor;
		int npcX, npcY, targetX, targetY;
		double dy, dx;
		double dblAggroRange = aggroRange * aggroRange;
		
		List<L2Character> potentialTarget = new FastList<>();
		
		for (L2Object obj : npc.getKnownList().getKnownObjects().values())
		{
			if (!(obj instanceof L2Character))
			{
				continue;
			}
			
			npcX = npc.getX();
			npcY = npc.getY();
			targetX = obj.getX();
			targetY = obj.getY();
			
			dx = npcX - targetX;
			dy = npcY - targetY;
			
			if (dx * dx + dy * dy > dblAggroRange)
			{
				continue;
			}
			
			L2Character target = (L2Character) obj;
			
			if (autoAttackCondition(target))
			{
				potentialTarget.add(target);
			}
		}
		
		if (potentialTarget.size() == 0) // nothing to do
			return null;
		
		// we choose a random target
		int choice = Rnd.nextInt(potentialTarget.size());
		L2Character target = potentialTarget.get(choice);
		
		return target;
	}
	
	private L2ControllableMobInstance findNextGroupTarget()
	{
		return getGroupTarget().getRandomMob();
	}
	
	public L2ControllableMobAI(L2ControllableMobInstance creature)
	{
		super(creature);
		setAlternateAI(AI_IDLE);
	}
	
	public int getAlternateAI()
	{
		return _alternateAI;
	}
	
	public void setAlternateAI(final int _alternateai)
	{
		_alternateAI = _alternateai;
	}
	
	public void forceAttack(final L2Character target)
	{
		setAlternateAI(AI_FORCEATTACK);
		setForcedTarget(target);
	}
	
	public void forceAttackGroup(final MobGroup group)
	{
		setForcedTarget(null);
		setGroupTarget(group);
		setAlternateAI(AI_ATTACK_GROUP);
	}
	
	public void stop()
	{
		setAlternateAI(AI_IDLE);
		clientStopMoving(null);
	}
	
	public void move(final int x, final int y, final int z)
	{
		moveTo(x, y, z);
	}
	
	public void follow(final L2Character target)
	{
		setAlternateAI(AI_FOLLOW);
		setForcedTarget(target);
	}
	
	public boolean isThinking()
	{
		return _isThinking;
	}
	
	public boolean isNotMoving()
	{
		return _isNotMoving;
	}
	
	public void setNotMoving(final boolean isNotMoving)
	{
		_isNotMoving = isNotMoving;
	}
	
	public void setThinking(final boolean isThinking)
	{
		_isThinking = isThinking;
	}
	
	private synchronized L2Character getForcedTarget()
	{
		return _forcedTarget;
	}
	
	private synchronized MobGroup getGroupTarget()
	{
		return _targetGroup;
	}
	
	private synchronized void setForcedTarget(final L2Character forcedTarget)
	{
		_forcedTarget = forcedTarget;
	}
	
	private synchronized void setGroupTarget(final MobGroup targetGroup)
	{
		_targetGroup = targetGroup;
	}
	
}
