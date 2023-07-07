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

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2SummonAI;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.geo.GeoData;
import l2jorion.game.model.L2Skill.SkillTargetType;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.model.actor.instance.L2SummonInstance;
import l2jorion.game.model.actor.knownlist.SummonKnownList;
import l2jorion.game.model.actor.stat.SummonStat;
import l2jorion.game.model.actor.status.SummonStatus;
import l2jorion.game.model.olympiad.OlympiadGameManager;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcInfo;
import l2jorion.game.network.serverpackets.PetDelete;
import l2jorion.game.network.serverpackets.PetInfo;
import l2jorion.game.network.serverpackets.PetItemList;
import l2jorion.game.network.serverpackets.PetStatusShow;
import l2jorion.game.network.serverpackets.PetStatusUpdate;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.taskmanager.DecayTaskManager;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.L2Weapon;

public abstract class L2Summon extends L2PlayableInstance
{
	protected int _pkKills;
	private L2PcInstance _owner;
	
	private int _attackRange = 36; // Melee range
	
	private boolean _follow = true;
	private boolean _previousFollowStatus = true;
	private int _maxLoad;
	
	private int _chargedSoulShot;
	private int _chargedSpiritShot;
	
	private final int _soulShotsPerHit = 1;
	private final int _spiritShotsPerHit = 1;
	
	protected boolean _showSummonAnimation;
	
	public L2Summon(final int objectId, final L2NpcTemplate template, final L2PcInstance owner)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		
		_showSummonAnimation = true;
		_owner = owner;
		
		getAI();
		
		setXYZInvisible(owner.getX() + 50, owner.getY() + 100, owner.getZ() + 100);
	}
	
	@Override
	public final SummonKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof SummonKnownList))
		{
			setKnownList(new SummonKnownList(this));
		}
		
		return (SummonKnownList) super.getKnownList();
	}
	
	@Override
	public SummonStat getStat()
	{
		if (super.getStat() == null || !(super.getStat() instanceof SummonStat))
		{
			setStat(new SummonStat(this));
		}
		
		return (SummonStat) super.getStat();
	}
	
	@Override
	public SummonStatus getStatus()
	{
		if (super.getStatus() == null || !(super.getStatus() instanceof SummonStatus))
		{
			setStatus(new SummonStatus(this));
		}
		
		return (SummonStatus) super.getStatus();
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2SummonAI(this);
	}
	
	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}
	
	// this defines the action buttons, 1 for Summon, 2 for Pets
	public abstract int getSummonType();
	
	@Override
	public void updateAbnormalEffect()
	{
		for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if (player != null)
			{
				player.sendPacket(new NpcInfo(this, player));
			}
		}
	}
	
	/**
	 * @return Returns the mountable.
	 */
	public boolean isMountable()
	{
		return false;
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (player == _owner && player.getTarget() == this)
		{
			player.sendPacket(new PetStatusShow(this));
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (player.getTarget() != this)
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			// update status hp&mp
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
			player.sendPacket(su);
		}
		else if (player.getTarget() == this)
		{
			if (isAutoAttackable(player))
			{
				if (Config.GEODATA)
				{
					if (GeoData.getInstance().canSeeTarget(player, this))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						player.onActionRequest();
					}
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					player.onActionRequest();
				}
			}
			else
			{
				// This Action Failed packet avoids player getting stuck when clicking three or more times
				player.sendPacket(ActionFailed.STATIC_PACKET);
				
				if (Config.GEODATA)
				{
					if (GeoData.getInstance().canSeeTarget(player, this))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
			}
		}
	}
	
	public long getExpForThisLevel()
	{
		if (getLevel() >= ExperienceData.getInstance().getMaxPetLevel())
		{
			return 0;
		}
		
		return ExperienceData.getInstance().getExpForLevel(getLevel());
	}
	
	public long getExpForNextLevel()
	{
		if (getLevel() >= ExperienceData.getInstance().getMaxPetLevel() - 1)
		{
			return 0;
		}
		
		return ExperienceData.getInstance().getExpForLevel(getLevel() + 1);
	}
	
	public final int getKarma()
	{
		return getOwner() != null ? getOwner().getKarma() : 0;
	}
	
	public final byte getPvpFlag()
	{
		return getOwner() != null ? getOwner().getPvpFlag() : 0;
	}
	
	public final L2PcInstance getOwner()
	{
		return _owner;
	}
	
	public final int getNpcId()
	{
		return getTemplate().npcId;
	}
	
	@Override
	public void doAttack(final L2Character target)
	{
		if (target == this)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (getOwner() != null && getOwner() == target && !getOwner().isBetrayed())
		{
			sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}
		
		if (isInsidePeaceZone(this, target))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			return;
		}
		
		if (!target.isAttackable())
		{
			if (!(this instanceof L2SiegeSummonInstance))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				return;
			}
		}
		
		/*
		 * if (getOwner() != null && target instanceof L2PcInstance && ((L2PcInstance) target).getSiegeState() == 0 && (!getOwner().checkAntiFarm((L2PcInstance) target))) { sendPacket(ActionFailed.STATIC_PACKET); return; }
		 */
		
		super.doAttack(target);
	}
	
	public void setPkKills(final int pkKills)
	{
		_pkKills = pkKills;
	}
	
	public final int getPkKills()
	{
		return _pkKills;
	}
	
	public final int getMaxLoad()
	{
		return _maxLoad;
	}
	
	public final int getSoulShotsPerHit()
	{
		return _soulShotsPerHit;
	}
	
	public final int getSpiritShotsPerHit()
	{
		return _spiritShotsPerHit;
	}
	
	public void setMaxLoad(final int maxLoad)
	{
		_maxLoad = maxLoad;
	}
	
	public void setChargedSoulShot(final int shotType)
	{
		_chargedSoulShot = shotType;
	}
	
	public void setChargedSpiritShot(final int shotType)
	{
		_chargedSpiritShot = shotType;
	}
	
	public void followOwner()
	{
		setFollowStatus(true);
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}
	
	public boolean doDie(final L2Character killer, final boolean decayed)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (!decayed)
		{
			DecayTaskManager.getInstance().addDecayTask(this);
		}
		
		return true;
	}
	
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}
	
	@Override
	public void onDecay()
	{
		deleteMe(_owner);
	}
	
	@Override
	public void broadcastStatusUpdate()
	{
		super.broadcastStatusUpdate();
		
		if (getOwner() != null && isVisible())
		{
			getOwner().sendPacket(new PetStatusUpdate(this));
		}
	}
	
	public void deleteMe(final L2PcInstance owner)
	{
		getAI().stopFollow();
		owner.sendPacket(new PetDelete(getObjectId(), 2));
		giveAllToOwner();
		decayMe();
		getKnownList().removeAllKnownObjects();
		owner.setPet(null);
	}
	
	public synchronized void unSummon(final L2PcInstance owner)
	{
		if (isVisible() && !isDead())
		{
			stopAllEffects();
			
			getAI().stopFollow();
			owner.sendPacket(new PetDelete(getObjectId(), 2));
			
			store();
			
			giveAllToOwner();
			
			stopAllEffects();
			
			final L2WorldRegion oldRegion = getWorldRegion();
			decayMe();
			if (oldRegion != null)
			{
				oldRegion.removeFromZones(this);
			}
			
			getKnownList().removeAllKnownObjects();
			owner.setPet(null);
			setTarget(null);
		}
	}
	
	public int getAttackRange()
	{
		return _attackRange;
	}
	
	public void setAttackRange(int range)
	{
		if (range < 36)
		{
			range = 36;
		}
		_attackRange = range;
	}
	
	public void setFollowStatus(final boolean state)
	{
		_follow = state;
		
		if (_follow)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getOwner());
		}
		else
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
		}
	}
	
	public boolean getFollowStatus()
	{
		return _follow;
	}
	
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return _owner.isAutoAttackable(attacker);
	}
	
	public int getChargedSoulShot()
	{
		return _chargedSoulShot;
	}
	
	public int getChargedSpiritShot()
	{
		return _chargedSpiritShot;
	}
	
	public int getControlItemId()
	{
		return 0;
	}
	
	public L2Weapon getActiveWeapon()
	{
		return null;
	}
	
	public PetInventory getInventory()
	{
		return null;
	}
	
	public void doPickupItem(final L2Object object)
	{
	}
	
	public void giveAllToOwner()
	{
	}
	
	public void store()
	{
	}
	
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public L2Party getParty()
	{
		if (_owner == null)
		{
			return null;
		}
		
		return _owner.getParty();
	}
	
	@Override
	public boolean isInParty()
	{
		if (_owner == null)
		{
			return false;
		}
		
		return _owner.getParty() != null;
	}
	
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if (skill == null || isDead())
		{
			return;
		}
		
		// Check if the skill is active
		if (skill.isPassive())
		{
			return;
		}
		
		// If a skill is currently being used
		if (isCastingNow())
		{
			return;
		}
		
		// Set current pet skill
		getOwner().setCurrentPetSkill(skill, forceUse, dontMove);
		
		// Get the target for the skill
		L2Object target = null;
		
		switch (skill.getTargetType())
		{
			// OWNER_PET should be cast even if no target has been found
			case TARGET_OWNER_PET:
				target = getOwner();
				break;
			// PARTY, AURA, SELF should be cast even if no target has been found
			case TARGET_PARTY:
			case TARGET_AURA:
			case TARGET_SELF:
				target = this;
				break;
			default:
				// Get the first target of the list
				target = skill.getFirstOfTargetList(this);
				break;
		}
		
		// Check the validity of the target
		if (target == null)
		{
			if (getOwner() != null)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
			}
			return;
		}
		
		if (isSkillDisabled(skill))
		{
			if (getOwner() != null)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
				sm.addString(skill.getName());
				getOwner().sendPacket(sm);
			}
			return;
		}
		
		if (getOwner() != null && skill.isOffensive() && target != this && target instanceof L2PcInstance && ((L2PcInstance) target).getSiegeState() == 0 && (!getOwner().checkAntiFarm((L2PcInstance) target)))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// ************************************* Check Consumables *******************************************
		
		// Check if the summon has enough MP
		if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			// Send a System Message to the caster
			if (getOwner() != null)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			}
			
			return;
		}
		
		// Check if the summon has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster
			if (getOwner() != null)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			}
			
			return;
		}
		
		// ************************************* Check Summon State *******************************************
		
		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			if (getOwner() != null && getOwner() == target && !getOwner().isBetrayed())
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return;
			}
			
			if (isInsidePeaceZone(this, target) && getOwner() != null && !getOwner().getAccessLevel().allowPeaceAttack())
			{
				// If summon or target is in a peace zone, send a system message TARGET_IN_PEACEZONE
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				return;
			}
			
			if (getOwner() != null && getOwner().isInOlympiadMode() && !getOwner().isOlympiadStart())
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Check if the target is attackable
			if (target instanceof L2DoorInstance)
			{
				if (!((L2DoorInstance) target).isAttackable(getOwner()))
				{
					return;
				}
			}
			else
			{
				if (!target.isAttackable() && getOwner() != null && (!getOwner().getAccessLevel().allowPeaceAttack()))
				{
					return;
				}
				
				// Check if a Forced ATTACK is in progress on non-attackable target
				if (!target.isAutoAttackable(this) && !forceUse && skill.getTargetType() != SkillTargetType.TARGET_AURA && skill.getTargetType() != SkillTargetType.TARGET_CLAN && skill.getTargetType() != SkillTargetType.TARGET_ALLY && skill.getTargetType() != SkillTargetType.TARGET_PARTY
					&& skill.getTargetType() != SkillTargetType.TARGET_SELF)
				{
					return;
				}
				
			}
		}
		
		// Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
	}
	
	@Override
	public void setIsImobilised(final boolean value)
	{
		super.setIsImobilised(value);
		
		if (value)
		{
			_previousFollowStatus = getFollowStatus();
			
			// if imobilized temporarly disable follow mode
			if (_previousFollowStatus)
			{
				setFollowStatus(false);
			}
		}
		else
		{
			// if not more imobilized restore previous follow mode
			setFollowStatus(_previousFollowStatus);
		}
	}
	
	public void setOwner(final L2PcInstance newOwner)
	{
		_owner = newOwner;
	}
	
	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * @param showSummonAnimation The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(final boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	@Override
	public boolean isInCombat()
	{
		return getOwner() != null ? getOwner().isInCombat() : false;
	}
	
	@Override
	public final void sendDamageMessage(final L2Character target, final int damage, final boolean mcrit, final boolean pcrit, final boolean miss)
	{
		if (miss || getOwner() == null)
		{
			return;
		}
		
		// Prevents the double spam of system messages, if the target is the owning player.
		if (target.getObjectId() != getOwner().getObjectId())
		{
			if (pcrit || mcrit)
			{
				if (this instanceof L2SummonInstance)
				{
					getOwner().sendPacket(SystemMessageId.CRITICAL_HIT_BY_SUMMONED_MOB);
				}
				else
				{
					getOwner().sendPacket(SystemMessageId.CRITICAL_HIT_BY_PET);
				}
			}
			
			final SystemMessage sm;
			
			if (target.isInvul())
			{
				if (target.isParalyzed())
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.OPPONENT_PETRIFIED);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED);
				}
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE).addNumber(damage);
			}
			
			getOwner().sendPacket(sm);
			
			if (getOwner().isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == getOwner().getOlympiadGameId())
			{
				OlympiadGameManager.getInstance().notifyCompetitorDamage(getOwner(), damage);
			}
		}
	}
	
	@Override
	public L2PcInstance getActingPlayer()
	{
		return getOwner();
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		// Check if the L2PcInstance is the owner of the Pet
		if (activeChar.equals(getOwner()))
		{
			activeChar.sendPacket(new PetInfo(this));
			
			// The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
			updateEffectIcons(true);
			
			if (this instanceof L2PetInstance)
			{
				activeChar.sendPacket(new PetItemList((L2PetInstance) this));
			}
		}
		else
		{
			activeChar.sendPacket(new NpcInfo(this, activeChar));
		}
	}
}
