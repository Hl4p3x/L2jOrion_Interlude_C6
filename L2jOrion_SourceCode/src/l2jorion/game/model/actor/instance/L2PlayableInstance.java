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
package l2jorion.game.model.actor.instance;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.knownlist.PlayableKnownList;
import l2jorion.game.model.actor.stat.PlayableStat;
import l2jorion.game.model.actor.status.PlayableStatus;
import l2jorion.game.templates.L2CharTemplate;

public abstract class L2PlayableInstance extends L2Character
{
	private boolean _isNoblesseBlessed = false; // for Noblesse Blessing skill, restores buffs after death
	private boolean _getCharmOfLuck = false; // Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	private boolean _isPhoenixBlessed = false; // for Soul of The Phoenix or Salvation buffs
	private boolean _ProtectionBlessing = false;
	private String _lastTownName = null;
	
	public L2PlayableInstance(final int objectId, final L2CharTemplate template)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
	}
	
	@Override
	public PlayableKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof PlayableKnownList))
		{
			setKnownList(new PlayableKnownList(this));
		}
		return (PlayableKnownList) super.getKnownList();
	}
	
	@Override
	public PlayableStat getStat()
	{
		if (super.getStat() == null || !(super.getStat() instanceof PlayableStat))
		{
			setStat(new PlayableStat(this));
		}
		return (PlayableStat) super.getStat();
	}
	
	@Override
	public PlayableStatus getStatus()
	{
		if (super.getStatus() == null || !(super.getStatus() instanceof PlayableStatus))
		{
			setStatus(new PlayableStatus(this));
		}
		return (PlayableStatus) super.getStatus();
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (killer != null)
		{
			L2PcInstance player = null;
			if (killer instanceof L2PcInstance)
			{
				player = (L2PcInstance) killer;
			}
			else if (killer instanceof L2Summon)
			{
				player = ((L2Summon) killer).getOwner();
			}
			
			if (player != null)
			{
				player.onKillUpdatePvPKarma(this);
			}
		}
		return true;
	}
	
	public boolean checkIfPvP(final L2Character target)
	{
		if (target == null)
		{
			return false; // Target is null
		}
		if (target == this)
		{
			return false; // Target is self
		}
		if (!(target instanceof L2PlayableInstance))
		{
			return false; // Target is not a L2PlayableInstance
		}
		
		L2PcInstance player = null;
		if (this instanceof L2PcInstance)
		{
			player = (L2PcInstance) this;
		}
		else if (this instanceof L2Summon)
		{
			player = ((L2Summon) this).getOwner();
		}
		
		if (player == null)
		{
			return false; // Active player is null
		}
		if (player.getKarma() != 0)
		{
			return false; // Active player has karma
		}
		
		L2PcInstance targetPlayer = null;
		if (target instanceof L2PcInstance)
		{
			targetPlayer = (L2PcInstance) target;
		}
		else if (target instanceof L2Summon)
		{
			targetPlayer = ((L2Summon) target).getOwner();
		}
		
		if (targetPlayer == null)
		{
			return false; // Target player is null
		}
		
		if (targetPlayer == this)
		{
			return false; // Target player is self
		}
		
		if (targetPlayer.getKarma() != 0)
		{
			return false; // Target player has karma
		}
		if (targetPlayer.getPvpFlag() == 0)
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	private L2Effect _lastNoblessEffect = null;
	
	// Support for Noblesse Blessing skill, where buffs are retained
	// after resurrect
	public final boolean isNoblesseBlessed()
	{
		return _isNoblesseBlessed;
	}
	
	/**
	 * Sets the checks if is noblesse blessed.
	 * @param value the new checks if is noblesse blessed
	 */
	public final void setIsNoblesseBlessed(final boolean value)
	{
		_isNoblesseBlessed = value;
	}
	
	public final void startNoblesseBlessing(final L2Effect effect)
	{
		_lastNoblessEffect = effect;
		setIsNoblesseBlessed(true);
		updateAbnormalEffect();
	}
	
	public final void stopNoblesseBlessing(final L2Effect effect)
	{
		// to avoid multiple buffs effects removal
		if (effect != null && _lastNoblessEffect != effect)
		{
			return;
		}
		
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.NOBLESSE_BLESSING);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsNoblesseBlessed(false);
		updateAbnormalEffect();
		_lastNoblessEffect = null;
		
	}
	
	private L2Effect _lastProtectionBlessingEffect = null;
	
	// for Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you
	public final boolean getProtectionBlessing()
	{
		return _ProtectionBlessing;
	}
	
	public final void setProtectionBlessing(final boolean value)
	{
		_ProtectionBlessing = value;
	}
	
	public void startProtectionBlessing(final L2Effect effect)
	{
		_lastProtectionBlessingEffect = effect;
		setProtectionBlessing(true);
		updateAbnormalEffect();
	}
	
	public void stopProtectionBlessing(final L2Effect effect)
	{
		if (effect != null && _lastProtectionBlessingEffect != effect)
		{
			return;
		}
		
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.PROTECTION_BLESSING);
		}
		else
		{
			removeEffect(effect);
		}
		
		setProtectionBlessing(false);
		updateAbnormalEffect();
		_lastProtectionBlessingEffect = null;
	}
	
	private L2Effect _lastPhoenixBlessedEffect = null;
	
	// Support for Soul of the Phoenix and Salvation skills
	public final boolean isPhoenixBlessed()
	{
		return _isPhoenixBlessed;
	}
	
	public final void setIsPhoenixBlessed(final boolean value)
	{
		_isPhoenixBlessed = value;
	}
	
	public final void startPhoenixBlessing(final L2Effect effect)
	{
		_lastPhoenixBlessedEffect = effect;
		setIsPhoenixBlessed(true);
		updateAbnormalEffect();
	}
	
	public final void stopPhoenixBlessing(final L2Effect effect)
	{
		if (effect != null && _lastPhoenixBlessedEffect != effect)
		{
			return;
		}
		
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.PHOENIX_BLESSING);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsPhoenixBlessed(false);
		updateAbnormalEffect();
		_lastPhoenixBlessedEffect = null;
		
	}
	
	public void setLastTownName(String lastTownName)
	{
		_lastTownName = lastTownName;
	}
	
	public String getLastTownName()
	{
		return _lastTownName;
	}
	
	public abstract boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage);
	
	public abstract boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage);
	
	public abstract boolean destroyItem(String process, L2ItemInstance item, int count, L2Object reference, boolean sendMessage);
	
	// Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	private L2Effect _lastCharmOfLuckEffect = null;
	
	public final boolean getCharmOfLuck()
	{
		return _getCharmOfLuck;
	}
	
	public final void setCharmOfLuck(final boolean value)
	{
		_getCharmOfLuck = value;
	}
	
	public final void startCharmOfLuck(final L2Effect effect)
	{
		setCharmOfLuck(true);
		updateAbnormalEffect();
		_lastCharmOfLuckEffect = effect;
	}
	
	public final void stopCharmOfLuck(final L2Effect effect)
	{
		if (effect != null && _lastCharmOfLuckEffect != effect)
		{
			return;
		}
		
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.CHARM_OF_LUCK);
		}
		else
		{
			removeEffect(effect);
		}
		
		setCharmOfLuck(false);
		updateAbnormalEffect();
		_lastCharmOfLuckEffect = null;
	}
	
	public boolean isInFunEvent()
	{
		final L2PcInstance player = getActingPlayer();
		
		return player == null ? false : player.isInFunEvent();
	}
	
	@Override
	public L2PcInstance getActingPlayer()
	{
		if (this instanceof L2PcInstance)
		{
			return (L2PcInstance) this;
		}
		return null;
	}
	
	public void sendMessage(String string)
	{
		
	}
}
