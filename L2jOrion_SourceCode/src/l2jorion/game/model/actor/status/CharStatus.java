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
package l2jorion.game.model.actor.status;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

import l2jorion.Config;
import l2jorion.bots.FakePlayer;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.managers.DuelManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SummonInstance;
import l2jorion.game.model.actor.stat.CharStat;
import l2jorion.game.model.entity.Duel;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.skills.Formulas;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class CharStatus
{
	protected static final Logger LOG = LoggerFactory.getLogger(CharStatus.class);
	
	private final L2Character _activeChar;
	
	private double _currentCp = 0; // Current CP of the L2Character
	private double _currentHp = 0; // Current HP of the L2Character
	private double _currentMp = 0; // Current MP of the L2Character
	
	private Set<L2Character> _StatusListener;
	
	private Future<?> _regTask;
	
	private byte _flagsRegenActive = 0;
	
	private static final byte REGEN_FLAG_CP = 4;
	private static final byte REGEN_FLAG_HP = 1;
	private static final byte REGEN_FLAG_MP = 2;
	
	public CharStatus(final L2Character activeChar)
	{
		_activeChar = activeChar;
	}
	
	public final void addStatusListener(final L2Character object)
	{
		if (object == getActiveChar())
		{
			return;
		}
		
		synchronized (getStatusListener())
		{
			getStatusListener().add(object);
		}
	}
	
	public void reduceHp(final double value, final L2Character attacker)
	{
		reduceHp(value, attacker, true);
	}
	
	public void reduceHp(double value, final L2Character attacker, final boolean awake)
	{
		if (getActiveChar().isInvul())
		{
			return;
		}
		
		if (getActiveChar() instanceof L2PcInstance)
		{
			if (((L2PcInstance) getActiveChar()).isInDuel())
			{
				// the duel is finishing - players do not recive damage
				if (((L2PcInstance) getActiveChar()).getDuelState() == Duel.DUELSTATE_DEAD)
				{
					return;
				}
				else if (((L2PcInstance) getActiveChar()).getDuelState() == Duel.DUELSTATE_WINNER)
				{
					return;
				}
				
				// cancel duel if player got hit by another player, that is not part of the duel or a monster
				if (!(attacker instanceof L2SummonInstance) && !(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getDuelId() == ((L2PcInstance) getActiveChar()).getDuelId()))
				{
					((L2PcInstance) getActiveChar()).setDuelState(Duel.DUELSTATE_INTERRUPTED);
				}
			}
			if (getActiveChar().isDead() && !getActiveChar().isFakeDeath())
			{
				return; // Disabled == null check so skills like Body to Mind work again untill another solution is found
			}
		}
		else
		{
			if (getActiveChar().isDead())
			{
				return; // Disabled == null check so skills like Body to Mind work again untill another solution is found
			}
			
			if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isInDuel() && !(getActiveChar() instanceof L2SummonInstance && ((L2SummonInstance) getActiveChar()).getOwner().getDuelId() == ((L2PcInstance) attacker).getDuelId())) // Duelling player attacks mob
			{
				((L2PcInstance) attacker).setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}
		}
		
		if (awake && getActiveChar().isSleeping())
		{
			getActiveChar().stopSleeping(null);
		}
		
		if (awake && getActiveChar().isImmobileUntilAttacked())
		{
			getActiveChar().stopImmobileUntilAttacked(null);
		}
		
		if (getActiveChar().isStunned() && Rnd.get(10) == 0)
		{
			getActiveChar().stopStunning(null);
		}
		
		// Add attackers to npc's attacker list
		if (getActiveChar() instanceof L2NpcInstance)
		{
			getActiveChar().addAttackerToAttackByList(attacker);
		}
		
		if (value > 0) // Reduce Hp if any
		{
			value = getCurrentHp() - value; // Get diff of Hp vs value
			
			if (value <= 0)
			{
				// is the dieing one a duelist? if so change his duel state to dead
				if (getActiveChar() instanceof L2PcInstance && ((L2PcInstance) getActiveChar()).isInDuel())
				{
					getActiveChar().disableAllSkills();
					stopHpMpRegeneration();
					attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					attacker.sendPacket(ActionFailed.STATIC_PACKET);
					
					// let the DuelManager know of his defeat
					DuelManager.getInstance().onPlayerDefeat((L2PcInstance) getActiveChar());
					value = 1;
				}
				else
				{
					// Set value to 0 if Hp < 0
					value = 0;
				}
			}
			setCurrentHp(value); // Set Hp
		}
		else
		{
			// If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
			if (getActiveChar() instanceof L2Attackable)
			{
				((L2Attackable) getActiveChar()).overhitEnabled(false);
			}
		}
		
		if (getActiveChar().isDead())
		{
			if (getActiveChar().isUnkillable())
			{
				setCurrentHp(1);
				startHpMpRegeneration();
				return;
			}
			
			getActiveChar().abortAttack();
			getActiveChar().abortCast();
			
			if (getActiveChar() instanceof L2PcInstance)
			{
				if (((L2PcInstance) getActiveChar()).isInOlympiadMode())
				{
					stopHpMpRegeneration();
					return;
				}
			}
			
			// Start the doDie process
			getActiveChar().doDie(attacker);
			
			// now reset currentHp to zero
			setCurrentHp(0);
		}
		else
		{
			// If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
			if (getActiveChar() instanceof L2Attackable)
			{
				((L2Attackable) getActiveChar()).overhitEnabled(false);
			}
		}
	}
	
	/**
	 * Reduce mp.
	 * @param value the value
	 */
	public final void reduceMp(double value)
	{
		if (getActiveChar() instanceof FakePlayer)
		{
			return;
		}
		
		value = getCurrentMp() - value;
		
		if (value < 0)
		{
			value = 0;
		}
		
		setCurrentMp(value);
	}
	
	/**
	 * Remove the object from the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates. Players who must be informed are players that target this L2Character. When a RegenTask is in progress sever just need to go through this list to send Server->Client packet
	 * StatusUpdate.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Untarget a PC or NPC</li><BR>
	 * <BR>
	 * @param object L2Character to add to the listener
	 */
	public final void removeStatusListener(final L2Character object)
	{
		synchronized (getStatusListener())
		{
			getStatusListener().remove(object);
		}
	}
	
	/**
	 * Start the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate the regen task period</li>
	 * <li>Launch the HP/MP/CP Regeneration task with Medium priority</li><BR>
	 * <BR>
	 */
	public synchronized final void startHpMpRegeneration()
	{
		if (_regTask == null && !getActiveChar().isDead())
		{
			// Get the Regeneration periode
			final int period = Formulas.getRegeneratePeriod(getActiveChar());
			
			// Create the HP/MP/CP Regeneration task
			_regTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new RegenTask(), period, period);
		}
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li><BR>
	 * <BR>
	 */
	public synchronized final void stopHpMpRegeneration()
	{
		if (_regTask != null)
		{
			// Stop the HP/MP/CP Regeneration task
			_regTask.cancel(false);
			_regTask = null;
			
			// Set the RegenActive flag to false
			_flagsRegenActive = 0;
		}
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
	 * Gets the current cp.
	 * @return the current cp
	 */
	public final double getCurrentCp()
	{
		return _currentCp;
	}
	
	/**
	 * Sets the current cp direct.
	 * @param newCp the new current cp direct
	 */
	public final void setCurrentCpDirect(final double newCp)
	{
		setCurrentCp(newCp, true, true);
	}
	
	/**
	 * Sets the current cp.
	 * @param newCp the new current cp
	 */
	public final void setCurrentCp(final double newCp)
	{
		setCurrentCp(newCp, true, false);
	}
	
	/**
	 * Sets the current cp.
	 * @param newCp the new cp
	 * @param broadcastPacket the broadcast packet
	 */
	public final void setCurrentCp(final double newCp, final boolean broadcastPacket)
	{
		setCurrentCp(newCp, broadcastPacket, false);
	}
	
	public final void setCurrentCp(double newCp, final boolean broadcastPacket, final boolean direct)
	{
		synchronized (this)
		{
			// Get the Max CP of the L2Character
			final int maxCp = getActiveChar().getStat().getMaxCp();
			
			if (newCp < 0)
			{
				newCp = 0;
			}
			
			if (newCp >= maxCp && !direct)
			{
				// Set the RegenActive flag to false
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	/**
	 * Gets the current hp.
	 * @return the current hp
	 */
	public final double getCurrentHp()
	{
		return _currentHp;
	}
	
	/**
	 * Sets the current hp.
	 * @param newHp the new current hp
	 */
	public final void setCurrentHp(final double newHp)
	{
		setCurrentHp(newHp, true);
	}
	
	/**
	 * Sets the current hp direct.
	 * @param newHp the new current hp direct
	 */
	public final void setCurrentHpDirect(final double newHp)
	{
		setCurrentHp(newHp, true, true);
	}
	
	/**
	 * Sets the current mp direct.
	 * @param newMp the new current mp direct
	 */
	public final void setCurrentMpDirect(final double newMp)
	{
		setCurrentMp(newMp, true, true);
	}
	
	/**
	 * Sets the current hp.
	 * @param newHp the new hp
	 * @param broadcastPacket the broadcast packet
	 */
	public final void setCurrentHp(final double newHp, final boolean broadcastPacket)
	{
		setCurrentHp(newHp, true, false);
	}
	
	/**
	 * Sets the current hp.
	 * @param newHp the new hp
	 * @param broadcastPacket the broadcast packet
	 * @param direct the direct
	 */
	public final void setCurrentHp(final double newHp, final boolean broadcastPacket, final boolean direct)
	{
		synchronized (this)
		{
			// Get the Max HP of the L2Character
			final double maxHp = getActiveChar().getStat().getMaxHp();
			
			if (newHp >= maxHp && !direct)
			{
				// Set the RegenActive flag to false
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				getActiveChar().setIsKilledAlready(false);
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				
				if (!getActiveChar().isDead())
				{
					getActiveChar().setIsKilledAlready(false);
				}
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
			
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	/**
	 * Sets the current hp mp.
	 * @param newHp the new hp
	 * @param newMp the new mp
	 */
	public final void setCurrentHpMp(final double newHp, final double newMp)
	{
		setCurrentHp(newHp, false, false);
		setCurrentMp(newMp, true, false); // send the StatusUpdate only once
	}
	
	/**
	 * Gets the current mp.
	 * @return the current mp
	 */
	public final double getCurrentMp()
	{
		return _currentMp;
	}
	
	/**
	 * Sets the current mp.
	 * @param newMp the new current mp
	 */
	public final void setCurrentMp(final double newMp)
	{
		setCurrentMp(newMp, true);
	}
	
	/**
	 * Sets the current mp.
	 * @param newMp the new mp
	 * @param broadcastPacket the broadcast packet
	 */
	public final void setCurrentMp(final double newMp, final boolean broadcastPacket)
	{
		setCurrentMp(newMp, broadcastPacket, false);
	}
	
	/**
	 * Sets the current mp.
	 * @param newMp the new mp
	 * @param broadcastPacket the broadcast packet
	 * @param direct the direct
	 */
	public final void setCurrentMp(final double newMp, final boolean broadcastPacket, final boolean direct)
	{
		synchronized (this)
		{
			// Get the Max MP of the L2Character
			final int maxMp = getActiveChar().getStat().getMaxMp();
			
			if (newMp >= maxMp && !direct)
			{
				// Set the RegenActive flag to false
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		if (broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}
	
	public final Set<L2Character> getStatusListener()
	{
		if (_StatusListener == null)
		{
			_StatusListener = new CopyOnWriteArraySet<>();
		}
		
		return _StatusListener;
	}
	
	/**
	 * Task of HP/MP/CP regeneration.
	 */
	class RegenTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				final CharStat charstat = getActiveChar().getStat();
				
				// Modify the current CP of the L2Character and broadcast Server->Client packet StatusUpdate
				if (getCurrentCp() < charstat.getMaxCp())
				{
					setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(getActiveChar()), false);
				}
				
				// Modify the current HP of the L2Character and broadcast Server->Client packet StatusUpdate
				if (getCurrentHp() < charstat.getMaxHp())
				{
					setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()), false);
				}
				
				// Modify the current MP of the L2Character and broadcast Server->Client packet StatusUpdate
				if (getCurrentMp() < charstat.getMaxMp())
				{
					setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()), false);
				}
				
				if (!getActiveChar().isInActiveRegion())
				{
					// no broadcast necessary for characters that are in inactive regions.
					// stop regeneration for characters who are filled up and in an inactive region.
					if (getCurrentCp() == charstat.getMaxCp() && getCurrentHp() == charstat.getMaxHp() && getCurrentMp() == charstat.getMaxMp())
					{
						stopHpMpRegeneration();
					}
				}
				else
				{
					getActiveChar().broadcastStatusUpdate(); // send the StatusUpdate packet
				}
			}
			catch (final Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.error("RegenTask failed for " + getActiveChar().getName(), e);
			}
		}
	}
}
