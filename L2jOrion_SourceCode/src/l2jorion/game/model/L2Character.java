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
package l2jorion.game.model;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_MOVE_TO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

import l2jorion.Config;
import l2jorion.bots.FakePlayer;
import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2AttackableAI;
import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2SummonAI;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.HeroSkillTable;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.csv.MapRegionTable.TeleportWhereType;
import l2jorion.game.geo.GeoData;
import l2jorion.game.geo.pathfinding.AbstractNodeLoc;
import l2jorion.game.geo.pathfinding.PathFinding;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.handler.SkillHandler;
import l2jorion.game.handler.item.Potions;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.DuelManager;
import l2jorion.game.managers.TownManager;
import l2jorion.game.model.L2Skill.SkillTargetType;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2ControlTowerInstance;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2EffectPointInstance;
import l2jorion.game.model.actor.instance.L2GuardInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2NpcWalkerInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance.SkillDat;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.actor.instance.L2RiftInvaderInstance;
import l2jorion.game.model.actor.instance.L2SiegeFlagInstance;
import l2jorion.game.model.actor.knownlist.CharKnownList;
import l2jorion.game.model.actor.position.ObjectPosition;
import l2jorion.game.model.actor.stat.CharStat;
import l2jorion.game.model.actor.status.CharStatus;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.L2Event;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.extender.BaseExtender.EventType;
import l2jorion.game.model.olympiad.OlympiadGameManager;
import l2jorion.game.model.olympiad.OlympiadGameTask;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.model.zone.type.L2TownZone;
import l2jorion.game.network.PacketServer;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.Attack;
import l2jorion.game.network.serverpackets.ChangeMoveType;
import l2jorion.game.network.serverpackets.ChangeWaitType;
import l2jorion.game.network.serverpackets.CharMoveToLocation;
import l2jorion.game.network.serverpackets.ExOlympiadSpelledInfo;
import l2jorion.game.network.serverpackets.FlyToLocation;
import l2jorion.game.network.serverpackets.FlyToLocation.FlyType;
import l2jorion.game.network.serverpackets.MagicEffectIcons;
import l2jorion.game.network.serverpackets.MagicSkillCanceld;
import l2jorion.game.network.serverpackets.MagicSkillLaunched;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcInfo;
import l2jorion.game.network.serverpackets.PartySpelled;
import l2jorion.game.network.serverpackets.PetInfo;
import l2jorion.game.network.serverpackets.Revive;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.StopMove;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.TeleportToLocation;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.skills.Calculator;
import l2jorion.game.skills.Formulas;
import l2jorion.game.skills.Stats;
import l2jorion.game.skills.effects.EffectCharge;
import l2jorion.game.skills.funcs.Func;
import l2jorion.game.skills.holders.ISkillsHolder;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.templates.L2CharTemplate;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public abstract class L2Character extends L2Object implements ISkillsHolder
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2Character.class);
	
	private long attackStance;
	
	private List<L2Character> _attackByList;
	private L2Skill _lastSkillCast;
	private L2Skill _lastPotionCast;
	
	private boolean _isBuffProtected = false;
	private boolean _isAfraid = false;
	private boolean _isConfused = false;
	private boolean _isFakeDeath = false; // Fake death
	private boolean _isFlying = false; // Is flying Wyvern?
	private boolean _isFallsdown = false; // Falls down
	private boolean _isMuted = false; // Cannot use magic
	private boolean _isPsychicalMuted = false; // Cannot use physical skills
	private boolean _isKilledAlready = false;
	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false; // the char is carrying too much
	private boolean _isParalyzed = false;
	private boolean _isRiding = false; // Is Riding strider?
	private boolean _isPendingRevive = false;
	private boolean _isRooted = false; // Cannot move until root timed out
	private boolean _isRunning = false;
	private boolean _isImmobileUntilAttacked = false; // Is in immobile until attacked.
	private boolean _isSleeping = false; // Cannot move/attack until sleep timed out or monster is attacked
	private boolean _isStunned = false; // Cannot move/attack until stun timed out
	private boolean _isBetrayed = false; // Betrayed by own summon
	private boolean _isBlockDebuff = false; // Got blocked de-buff bar
	protected boolean _isTeleporting = false;
	protected boolean _isInvul = false;
	protected boolean _isUnkillable = false;
	protected boolean _isAttackDisabled = false;
	private boolean _isClickedArrowButton = false;
	
	private int _lastHealAmount = 0;
	
	private int _clientX;
	private int _clientY;
	private int _clientZ;
	private int _clientHeading;
	
	private CharStat _stat;
	
	private CharStatus _status;
	
	private L2CharTemplate _template;
	
	protected String _title;
	
	private String _aiClass = "default";
	
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	
	private boolean _champion = false;
	
	private boolean _advanceFlag = false;
	private int _advanceMultiplier = 1;
	
	private Calculator[] _calculators;
	
	private final StampedLock _attackLock = new StampedLock();
	private volatile long _attackEndTime;
	
	protected Map<Integer, L2Skill> _skills;
	protected Map<Integer, L2Skill> _triggeredSkills;
	
	protected ChanceSkillList _chanceSkills;
	
	protected ForceBuff _forceBuff;
	
	private int _VotedSystem;
	
	private boolean _blocked;
	
	private boolean _meditated;
	
	private final byte[] _zones = new byte[ZoneId.getZoneCount()];
	
	public final boolean isInsideZone(ZoneId zone)
	{
		return _zones[zone.ordinal()] > 0;
	}
	
	public final void setInsideZone(ZoneId zone, final boolean state)
	{
		synchronized (_zones)
		{
			if (state)
			{
				_zones[zone.ordinal()]++;
			}
			else if (_zones[zone.ordinal()] > 0)
			{
				_zones[zone.ordinal()]--;
			}
		}
	}
	
	public boolean charIsGM()
	{
		if (this instanceof L2PcInstance)
		{
			if (((L2PcInstance) this).isGM())
			{
				return true;
			}
		}
		return false;
	}
	
	public L2Character(L2CharTemplate template)
	{
		this(IdFactory.getInstance().getNextId(), template);
	}
	
	public L2Character(int objectId, L2CharTemplate template)
	{
		super(objectId);
		getKnownList();
		
		_template = template;
		_triggeredSkills = new HashMap<>();
		_skills = new HashMap<>();
		if ((template != null) && (this instanceof L2NpcInstance))
		{
			_calculators = NPC_STD_CALCULATOR;
			
			_skills = ((L2NpcTemplate) template).getSkills();
			for (Entry<Integer, L2Skill> skill : _skills.entrySet())
			{
				addStatFuncs(skill.getValue().getStatFuncs(null, this), true);
			}
			
			if (!Config.NPC_ATTACKABLE || !(this instanceof L2Attackable) && !(this instanceof L2ControlTowerInstance) && !(this instanceof L2SiegeFlagInstance) && !(this instanceof L2EffectPointInstance))
			{
				setIsInvul(true);
			}
		}
		else
		{
			// Initialize the Map _skills to null
			_skills = new ConcurrentHashMap<>();
			
			_calculators = new Calculator[Stats.NUM_STATS];
			Formulas.getInstance().addFuncsToNewCharacter(this);
			
			if (!(this instanceof L2Attackable) && !this.isAttackable() && !(this instanceof L2DoorInstance))
			{
				setIsInvul(true);
			}
		}
	}
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateInterval = getMaxHp() / 352.0;
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
	}
	
	public void onDecay()
	{
		L2WorldRegion reg = getWorldRegion();
		if (reg != null)
		{
			reg.removeFromZones(this);
		}
		
		decayMe(); // Decay after check
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		revalidateZone(true);
	}
	
	public void addAttackerToAttackByList(L2Character player)
	{
		if (player == null || player == this || getAttackByList() == null || getAttackByList().contains(player))
		{
			return;
		}
		
		getAttackByList().add(player);
	}
	
	protected byte _startingRotationCounter = 4;
	
	public synchronized boolean isStartingRotationAllowed()
	{
		_startingRotationCounter--;
		if (_startingRotationCounter < 0)
		{
			_startingRotationCounter = 4;
		}
		
		if (_startingRotationCounter == 4)
		{
			return true;
		}
		return false;
	}
	
	public void broadcastPacket(PacketServer packet)
	{
		if (this instanceof L2PcInstance)
		{
			sendPacket(packet);
		}
		
		Broadcast.toKnownPlayers(this, packet);
	}
	
	public final void broadcastPacket(PacketServer packet, int radiusInKnownlist)
	{
		if (this instanceof L2PcInstance)
		{
			sendPacket(packet);
		}
		
		Broadcast.toKnownPlayersInRadius(this, packet, radiusInKnownlist);
	}
	
	protected boolean needHpUpdate(int barPixels)
	{
		double currentHp = getCurrentHp();
		
		if (currentHp <= 1.0 || getMaxHp() < barPixels)
		{
			return true;
		}
		
		if (currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck)
		{
			if (currentHp == getMaxHp())
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			return true;
		}
		
		return false;
	}
	
	public void broadcastStatusUpdate()
	{
		if (getStatus().getStatusListener().isEmpty())
		{
			return;
		}
		
		if (!needHpUpdate(352))
		{
			return;
		}
		
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		
		for (L2Character temp : getStatus().getStatusListener())
		{
			if (temp != null)
			{
				temp.sendPacket(su);
			}
		}
	}
	
	public void sendPacket(PacketServer mov)
	{
	}
	
	private boolean _inTownWar;
	
	public final boolean isinTownWar()
	{
		return _inTownWar;
	}
	
	public final void setInTownWar(boolean value)
	{
		_inTownWar = value;
	}
	
	public void teleToLocation(int x, int y, int z, int heading, boolean allowRandomOffset, boolean isFastTeleport, boolean isPvPZone)
	{
		if (Config.TW_DISABLE_GK)
		{
			int x1, y1, z1;
			x1 = getX();
			y1 = getY();
			z1 = getZ();
			L2TownZone Town;
			TownManager.getInstance();
			Town = TownManager.getInstance().getTown(x1, y1, z1);
			if (Town != null && isinTownWar())
			{
				if (Town.getTownId() == Config.TW_TOWN_ID && !Config.TW_ALL_TOWNS)
				{
					return;
				}
				else if (Config.TW_ALL_TOWNS)
				{
					return;
				}
			}
		}
		
		stopMove(null);
		abortAttack();
		abortCast();
		
		setIsTeleporting(true);
		setTarget(null);
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		
		if (Config.RESPAWN_RANDOM_ENABLED && allowRandomOffset)
		{
			x += Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
			y += Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
		}
		
		// z += 5;
		
		// to avoid falling in the end of teleport on this sync system
		if (Config.COORD_SYNCHRONIZE == 3 && this instanceof L2PcInstance && !(this instanceof FakePlayer))
		{
			setClientZ(z);
		}
		
		broadcastPacket(new TeleportToLocation(this, x, y, z, heading, isFastTeleport));
		
		final L2WorldRegion region = getWorldRegion();
		if (region != null)
		{
			region.removeFromZones(this);
		}
		
		decayMe();
		
		if (isPvPZone)
		{
			setInstanceId(Config.PVP_ZONE_INSTANCE_ID);
		}
		
		setXYZ(x, y, z);
		
		if (!(this instanceof L2PcInstance) || (this instanceof FakePlayer) || ((((L2PcInstance) this).getClient() != null) && ((L2PcInstance) this).getClient().isDetached()))
		{
			onTeleported();
		}
		
		revalidateZone(true);
	}
	
	public void onTeleported()
	{
		if (!isTeleporting())
		{
			return;
		}
		
		setIsTeleporting(false); // moved up because of pvp zone
		
		final ObjectPosition pos = getPosition();
		if (pos != null)
		{
			spawnMe(getPosition().getX(), getPosition().getY(), getPosition().getZ());
		}
		
		if (_isPendingRevive)
		{
			doRevive();
		}
		
		final L2Summon pet = getPet();
		if (pet != null && pos != null)
		{
			pet.setFollowStatus(false);
			pet.teleToLocation(pos.getX() + Rnd.get(-100, 100), pos.getY() + Rnd.get(-100, 100), pos.getZ(), getHeading(), false, false, false);
			((L2SummonAI) pet.getAI()).setStartFollowController(true);
			pet.setFollowStatus(true);
		}
	}
	
	protected byte _zoneValidateCounter = 4;
	
	public void revalidateZone(boolean force)
	{
		if (getWorldRegion() == null)
		{
			return;
		}
		
		if (force)
		{
			_zoneValidateCounter = 4;
		}
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
			{
				_zoneValidateCounter = 4;
			}
			else
			{
				return;
			}
		}
		
		getWorldRegion().revalidateZones(this);
	}
	
	public void instantTeleport(int x, int y, int z, boolean isFastTeleport)
	{
		// To avoid falling in the end of teleport on this sync system
		if (Config.COORD_SYNCHRONIZE == 3 && this instanceof L2PcInstance && !(this instanceof FakePlayer))
		{
			setClientZ(z);
		}
		
		broadcastPacket(new TeleportToLocation(this, x, y, z, 0, isFastTeleport));
		
		setXYZ(x, y, z);
		
		broadcastPacket(new ValidateLocation(this));
		broadcastPacket(new StopMove(this));
		
		// Refresh knownlist to avoid - client side bug.
		getKnownList().removeAllKnownObjects();
		getKnownList().findObjects();
	}
	
	public void teleToLocation(Location loc)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), true, false, false);
	}
	
	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, getHeading(), false, false, false);
	}
	
	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset)
	{
		teleToLocation(x, y, z, getHeading(), allowRandomOffset, false, false);
	}
	
	public void teleToLocation(Location loc, boolean allowRandomOffset)
	{
		int x = loc.getX();
		int y = loc.getY();
		int z = loc.getZ();
		
		teleToLocation(x, y, z, getHeading(), allowRandomOffset, false, false);
	}
	
	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(MapRegionTable.getInstance().getTeleToLocation(this, teleportWhere), true);
	}
	
	public void teleToLocation(Location loc, int randomOffset, boolean pvpZone)
	{
		int x = loc.getX();
		int y = loc.getY();
		
		if (randomOffset > 0)
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		
		teleToLocation(x, y, loc.getZ(), loc.getHeading(), false, false, pvpZone);
	}
	
	public void teleToLocation(Location loc, int randomOffset)
	{
		int x = loc.getX();
		int y = loc.getY();
		
		if (randomOffset > 0)
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		
		teleToLocation(x, y, loc.getZ(), loc.getHeading(), false, false, false);
	}
	
	public void doAttack(L2Character target)
	{
		final long stamp = _attackLock.tryWriteLock();
		
		if (stamp == 0)
		{
			return;
		}
		try
		{
			if (target == null || isAttackingDisabled())
			{
				return;
			}
			
			if (isAlikeDead())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (this instanceof L2NpcInstance && target.isAlikeDead())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (this instanceof L2PcInstance && target.isDead() && !target.isFakeDeath())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!getKnownList().knowsObject(target))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (this instanceof L2PcInstance && isDead())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (target instanceof L2DoorInstance && !((L2DoorInstance) target).isAttackable(this))
			{
				return;
			}
			
			if (this instanceof L2PcInstance)
			{
				if (((L2PcInstance) this).inObserverMode())
				{
					sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (getObjectId() == target.getObjectId())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				if (target instanceof L2NpcInstance && Config.DISABLE_ATTACK_NPC_TYPE)
				{
					String mobtype = ((L2NpcInstance) target).getTemplate().type;
					if (!Config.LIST_ALLOWED_NPC_TYPES.contains(mobtype))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
						sm.addString("Npc type " + mobtype + " has Protection - no attack allowed.");
						((L2PcInstance) this).sendPacket(sm);
						((L2PcInstance) this).sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
				
				if (target.isInsidePeaceZone((L2PcInstance) this))
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			L2ItemInstance weaponInst = getActiveWeaponInstance();
			L2Weapon weaponItem = getActiveWeaponItem();
			final L2WeaponType weaponItemType = getAttackType();
			
			if (weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD)
			{
				((L2PcInstance) this).sendPacket(new SystemMessage(SystemMessageId.CANNOT_ATTACK_WITH_FISHING_POLE));
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (this instanceof L2PcInstance && !(this instanceof FakePlayer))
			{
				if (!target.isDoor())
				{
					if (Math.abs(((L2PcInstance) this).getClientZ() - target.getZ()) > 400)
					{
						sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
			}
			
			if (target instanceof L2MonsterInstance || target instanceof L2RaidBossInstance)
			{
				if (Config.RAID_FLAG_LIST.contains(((L2MonsterInstance) target).getNpcId()))
				{
					if (this instanceof L2PcInstance)
					{
						((L2PcInstance) this).updatePvPStatus();
					}
					else if (this instanceof L2Summon)
					{
						((L2Summon) this).getOwner().updatePvPStatus();
					}
				}
			}
			
			if (Config.GET_PVP_FLAG)
			{
				if (target instanceof L2RaidBossInstance)
				{
					if (this instanceof L2PcInstance)
					{
						((L2PcInstance) this).updatePvPStatus();
					}
					else if (this instanceof L2Summon)
					{
						((L2Summon) this).getOwner().updatePvPStatus();
					}
				}
			}
			
			if (Config.GET_PVP_FLAG_FROM_CHAMP)
			{
				if (target.isChampion())
				{
					if (this instanceof L2PcInstance)
					{
						((L2PcInstance) this).updatePvPStatus();
					}
					else if (this instanceof L2Summon)
					{
						((L2Summon) this).getOwner().updatePvPStatus();
					}
				}
			}
			
			if (!GeoData.getInstance().canSeeTarget(this, target))
			{
				sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			target.getKnownList().addKnownObject(this);
			
			if (Config.ALT_GAME_TIREDNESS)
			{
				setCurrentCp(getCurrentCp() - 10);
			}
			
			if (this instanceof L2PcInstance)
			{
				((L2PcInstance) this).rechargeAutoSoulShot(true, false, false);
			}
			else if (this instanceof L2Summon)
			{
				((L2Summon) this).getOwner().rechargeAutoSoulShot(true, false, true);
			}
			
			// Verify if soulshots are charged.
			boolean wasSSCharged;
			if (this instanceof L2Summon && !(this instanceof L2PetInstance))
			{
				wasSSCharged = ((L2Summon) this).getChargedSoulShot() != L2ItemInstance.CHARGED_NONE;
			}
			else
			{
				wasSSCharged = weaponInst != null && weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE;
			}
			
			// Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
			final int timeAtk = calculateTimeBetweenAttacks(target, weaponItem);
			final int timeToHit = timeAtk / 2;
			
			int ssGrade = 0;
			if (weaponItem != null)
			{
				ssGrade = weaponItem.getCrystalType();
			}
			
			Attack attack = new Attack(this, wasSSCharged, ssGrade);
			setAttackingBodypart();
			setHeading(Util.calculateHeadingFrom(this, target));
			
			// Get the Attack Reuse Delay of the L2Weapon
			int reuse = calculateReuseTime(target, weaponItem);
			boolean hitted = false;
			switch (weaponItemType)
			{
				case BOW:
				{
					if (!canUseBow())
					{
						return;
					}
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeToHit + (reuse / 2), TimeUnit.MILLISECONDS);
					hitted = doAttackHitByBow(attack, target, timeAtk, reuse);
					break;
				}
				case POLE:
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitByPole(attack, target, timeToHit);
					break;
				}
				case FIST:
				case DUAL:
				case DUALFIST:
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitByDual(attack, target, timeToHit);
					break;
				}
				default:
				{
					_attackEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeAtk, TimeUnit.MILLISECONDS);
					hitted = doAttackHitSimple(attack, target, timeToHit);
					break;
				}
			}
			
			// Flag the attacker if it's a L2PcInstance outside a PvP area
			L2PcInstance player = null;
			
			if (this instanceof L2PcInstance)
			{
				player = (L2PcInstance) this;
			}
			else if (this instanceof L2Summon)
			{
				player = ((L2Summon) this).getOwner();
			}
			
			if (player != null)
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
				player.updatePvPStatus(target);
			}
			
			if (!hitted)
			{
				if (player != null && player.getScreentxt())
				{
					sendPacket(new SystemMessage(SystemMessageId.MISSED_TARGET));
					abortAttack();
				}
				else
				{
					sendPacket(new SystemMessage(SystemMessageId.MISSED_TARGET2));
					abortAttack();
				}
			}
			else
			{
				/*
				 * if (this instanceof L2PcInstance) { ((L2PcInstance) this).rechargeAutoSoulShot(true, false, false); } else if (this instanceof L2Summon) { ((L2Summon) this).getOwner().rechargeAutoSoulShot(true, false, true); }
				 */
				
				if (player != null)
				{
					if (player.isCursedWeaponEquiped())
					{
						if (!target.isInvul())
						{
							target.setCurrentCp(0);
						}
					}
					else if (player.isHero())
					{
						if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquiped())
						{
							target.setCurrentCp(0);
						}
					}
				}
			}
			
			if (attack.hasHits())
			{
				broadcastPacket(attack);
			}
			
			ThreadPoolManager.getInstance().scheduleAi(() -> getAI().notifyEvent(CtrlEvent.EVT_READY_TO_ACT), timeAtk);
			
			if (Config.L2OFF_PVP_SYSTEM)
			{
				if (player != null && !(target instanceof L2NpcInstance))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, this);
				}
			}
		}
		finally
		{
			_attackLock.unlockWrite(stamp);
		}
	}
	
	public final L2WeaponType getAttackType()
	{
		final L2Weapon weapon = getActiveWeaponItem();
		return (weapon == null) ? L2WeaponType.NONE : weapon.getItemType();
	}
	
	private boolean canUseBow()
	{
		L2Weapon weaponItem = getActiveWeaponItem();
		
		if (weaponItem == null)
		{
			return false;
		}
		
		// Check for arrows and MP
		if (this instanceof L2PcInstance)
		{
			// Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True
			if (!checkAndEquipArrows())
			{
				// Cancel the action because the L2PcInstance have no arrow
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ARROWS));
				return false;
			}
			
			// Checking if target has moved to peace zone - only for player-bow attacks at the moment
			// Other melee is checked in movement code and for offensive spells a check is done every time
			if (getTarget() != null && ((L2Character) getTarget()).isInsidePeaceZone((L2PcInstance) this))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Verify if the bow can be use
			if (_disableBowAttackEndTime <= GameTimeController.getInstance().getGameTicks())
			{
				// Verify if L2PcInstance owns enough MP
				int saMpConsume = (int) getStat().calcStat(Stats.MP_CONSUME, 0, null, null);
				int mpConsume = saMpConsume == 0 ? weaponItem.getMpConsume() : saMpConsume;
				
				if (getCurrentMp() < mpConsume)
				{
					ThreadPoolManager.getInstance().scheduleAi(() -> getAI().notifyEvent(CtrlEvent.EVT_READY_TO_ACT), 500);
					
					sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				getStatus().reduceMp(mpConsume);
				
				// Set the period of bow non re-use
				_disableBowAttackEndTime = 5 * GameTimeController.TICKS_PER_SECOND + GameTimeController.getInstance().getGameTicks();
			}
			else
			{
				// Cancel the action because the bow can't be re-use at this moment
				ThreadPoolManager.getInstance().scheduleAi(() -> getAI().notifyEvent(CtrlEvent.EVT_READY_TO_ACT), 500);
				
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		else if (this instanceof L2NpcInstance)
		{
			if (_disableBowAttackEndTime > GameTimeController.getInstance().getGameTicks())
			{
				return false;
			}
		}
		
		return true;
	}
	
	private boolean doAttackHitByBow(Attack attack, L2Character target, int sAtk, int reuse)
	{
		int damage1 = 0;
		boolean shld1 = false;
		boolean crit1 = false;
		
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Consumme arrows
		reduceArrowCount();
		
		_move = null;
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.hasSoulshot());
		}
		
		if (this instanceof L2PcInstance)
		{
			// Send a system message
			sendPacket(new SystemMessage(SystemMessageId.GETTING_READY_TO_SHOOT_AN_ARROW));
			
			sendPacket(new SetupGauge(SetupGauge.RED, sAtk + reuse));
		}
		
		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		
		// Calculate and set the disable delay of the bow in function of the Attack Speed
		_disableBowAttackEndTime = (sAtk + reuse) / GameTimeController.MILLIS_IN_TICK + GameTimeController.getInstance().getGameTicks();
		
		// Add this hit to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	private boolean doAttackHitByDual(Attack attack, L2Character target, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		boolean shld1 = false;
		boolean shld2 = false;
		boolean crit1 = false;
		boolean crit2 = false;
		
		// Calculate if hits are missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		boolean miss2 = Formulas.calcHitMiss(this, target);
		
		// Check if hit 1 isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient against hit 1
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit 1 is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			
			// Calculate physical damages of hit 1
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, true, attack.hasSoulshot());
			damage1 /= 2;
		}
		
		// Check if hit 2 isn't missed
		if (!miss2)
		{
			// Calculate if shield defense is efficient against hit 2
			shld2 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit 2 is critical
			crit2 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			
			// Calculate physical damages of hit 2
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, true, attack.hasSoulshot());
			damage2 /= 2;
		}
		
		// Create a new hit task with Medium priority for hit 1
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk / 2);
		
		// Create a new hit task with Medium priority for hit 2 with a higher delay
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, attack.hasSoulshot(), shld2), sAtk);
		
		// Add those hits to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		attack.addHit(target, damage2, miss2, crit2, shld2);
		
		// Return true if hit 1 or hit 2 isn't missed
		return !miss1 || !miss2;
	}
	
	private boolean doAttackHitByPole(Attack attack, L2Character target, int sAtk)
	{
		// double angleChar;
		int maxRadius = getPhysicalAttackRange();
		int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);
		
		// Get char's heading degree
		int attackRandomCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 0, null, null) - 1;
		int attackcount = 0;
		
		boolean hitted = doAttackHitSimple(attack, target, 100, sAtk);
		// by retail 100 too
		double attackpercent = 100;
		L2Character temp;
		Collection<L2Object> objs = getKnownList().getKnownObjects().values();
		{
			for (L2Object obj : objs)
			{
				if (obj == target)
				{
					continue; // do not hit twice
				}
				
				// Check if the L2Object is a L2Character
				if (obj instanceof L2Character)
				{
					if (obj instanceof L2PetInstance && this instanceof L2PcInstance && ((L2PetInstance) obj).getOwner() == ((L2PcInstance) this))
					{
						continue;
					}
					
					if (!Util.checkIfInRange(maxRadius, this, obj, false))
					{
						continue;
					}
					
					if (Math.abs(obj.getZ() - getZ()) > 650)
					{
						continue;
					}
					
					if (!isFacing(obj, maxAngleDiff))
					{
						continue;
					}
					
					if (this instanceof L2Attackable && obj instanceof L2PcInstance && getTarget() instanceof L2Attackable)
					{
						continue;
					}
					
					temp = (L2Character) obj;
					
					// Launch a simple attack against the L2Character targeted
					if (!temp.isAlikeDead())
					{
						if (temp == getAI().getTarget() || temp.isAutoAttackable(this))
						{
							hitted |= doAttackHitSimple(attack, temp, attackpercent, sAtk);
							// removed - interlude doesn't have it
							// attackpercent /= 1.15;
							
							attackcount++;
							if (attackcount > attackRandomCountMax)
							{
								break;
							}
						}
					}
				}
			}
		}
		
		return hitted;
	}
	
	private boolean doAttackHitSimple(Attack attack, L2Character target, int sAtk)
	{
		return doAttackHitSimple(attack, target, 100, sAtk);
	}
	
	private boolean doAttackHitSimple(Attack attack, L2Character target, double attackpercent, int sAtk)
	{
		int damage1 = 0;
		boolean shld1 = false;
		boolean crit1 = false;
		
		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			
			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.hasSoulshot());
			
			if (attackpercent != 100)
			{
				damage1 = (int) (damage1 * attackpercent / 100);
			}
		}
		
		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.hasSoulshot(), shld1), sAtk);
		
		// Add this hit to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Return true if hit isn't missed
		return !miss1;
	}
	
	public void doCast(L2Skill skill)
	{
		if (skill == null || isSkillDisabled(skill))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!skill.isPotion() && isAllSkillsDisabled())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (skill.isMagic() && isMuted() && !skill.isPotion())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!skill.isMagic() && isPsychicalMuted() && !skill.isPotion())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if the caster has enough MP
		if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// prevent casting signet to peace zone
		if (skill.getSkillType() == SkillType.SIGNET || skill.getSkillType() == SkillType.SIGNET_CASTTIME)
		{
			L2WorldRegion region = getWorldRegion();
			if (region == null)
			{
				return;
			}
			
			boolean canCast = true;
			if (skill.getTargetType() == SkillTargetType.TARGET_GROUND && this instanceof L2PcInstance)
			{
				Location wp = ((L2PcInstance) this).getCurrentSkillWorldPosition();
				if (!region.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
				{
					canCast = false;
				}
			}
			else if (!region.checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
			{
				canCast = false;
			}
			
			if (!canCast)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addSkillName(skill.getId());
				sendPacket(sm);
				return;
			}
		}
		
		// Re-charge AutoSoulShot
		if (skill.useSoulShot())
		{
			if (this instanceof L2PcInstance)
			{
				((L2PcInstance) this).rechargeAutoSoulShot(true, false, false);
			}
			else if (this instanceof L2Summon)
			{
				((L2Summon) this).getOwner().rechargeAutoSoulShot(true, false, true);
			}
		}
		
		// Get all possible targets of the skill in a table in function of the skill target type
		final L2Object[] targets = skill.getTargetList(this);
		L2Character target = null;
		
		switch (skill.getTargetType())
		{
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_CORPSE_ALLY:
			case TARGET_CORPSE_CLAN:
			case TARGET_GROUND:
			{
				target = this;
				break;
			}
			default:
			{
				if (skill.isPotion())
				{
					target = this;
				}
				else if (targets == null || targets.length == 0)
				{
					getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
					return;
				}
				else if ((skill.getSkillType() == SkillType.BUFF || skill.getSkillType() == SkillType.HEAL || skill.getSkillType() == SkillType.COMBATPOINTHEAL || skill.getSkillType() == SkillType.COMBATPOINTPERCENTHEAL || skill.getSkillType() == SkillType.MANAHEAL
					|| skill.getSkillType() == SkillType.REFLECT || skill.getSkillType() == SkillType.SEED || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_PET || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_PARTY
					|| skill.getTargetType() == L2Skill.SkillTargetType.TARGET_CLAN || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_ALLY) && !skill.isPotion())
				{
					target = (L2Character) targets[0];
				}
				else
				{
					target = (L2Character) getTarget();
				}
			}
		}
		
		if (target == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target instanceof L2MonsterInstance || target instanceof L2RaidBossInstance)
		{
			if (Config.RAID_FLAG_LIST.contains(((L2MonsterInstance) target).getNpcId()))
			{
				if (this instanceof L2PcInstance)
				{
					((L2PcInstance) this).updatePvPStatus();
				}
				else if (this instanceof L2Summon)
				{
					((L2Summon) this).getOwner().updatePvPStatus();
				}
			}
		}
		
		if (Config.GET_PVP_FLAG)
		{
			if (target instanceof L2RaidBossInstance)
			{
				if (this instanceof L2PcInstance)
				{
					((L2PcInstance) this).updatePvPStatus();
				}
				else if (this instanceof L2Summon)
				{
					((L2Summon) this).getOwner().updatePvPStatus();
				}
			}
		}
		
		if (Config.GET_PVP_FLAG_FROM_CHAMP)
		{
			if (target.isChampion())
			{
				if (this instanceof L2PcInstance)
				{
					((L2PcInstance) this).updatePvPStatus();
				}
				else if (this instanceof L2Summon)
				{
					((L2Summon) this).getOwner().updatePvPStatus();
				}
			}
		}
		
		if (this instanceof L2PcInstance && target instanceof L2PcInstance && !charIsGM() && (skill.getSkillType() == SkillType.BUFF))
		{
			if (((L2PcInstance) target).getBlockAllBuffs() && !(((L2PcInstance) target).isInOlympiadMode()) && (target != this))
			{
				((L2PcInstance) this).sendMessage(target.getName() + " is blocking your buffs.");
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		/*
		 * if (Config.BUFF_PET_BLOCK) { if (this instanceof L2PcInstance && target instanceof L2Summon && (skill.getSkillType() == SkillType.BUFF)) { L2Summon pet = (L2Summon) target; if (!pet.getOwner().isInOlympiadMode() && (pet.getOwner().getPet() != this.getPet())) { SystemMessage sm = new
		 * SystemMessage(SystemMessageId.S1_S2); sm.addString("No se permite buffear a los Pet Summons"); sendPacket(sm); sendPacket(ActionFailed.STATIC_PACKET); return; } } }
		 */
		
		if (skill.isPotion())
		{
			setLastPotionCast(skill);
		}
		else
		{
			setLastSkillCast(skill);
		}
		
		// Get the Identifier of the skill
		int magicId = skill.getId();
		int displayId = skill.getDisplayId();
		int level = skill.getLevel();
		
		if (level < 1)
		{
			level = 1;
		}
		
		// Get the casting time of the skill (base)
		int hitTime = skill.getHitTime();
		int coolTime = skill.getCoolTime();
		final boolean effectWhileCasting = skill.hasEffectWhileCasting();
		
		boolean forceBuff = skill.getSkillType() == SkillType.FORCE_BUFF && target instanceof L2PcInstance;
		
		// Calculate the casting time of the skill (base + modifier of MAtkSpd)
		// Don't modify the skill time for FORCE_BUFF skills. The skill time for those skills represent the buff time.
		if (!effectWhileCasting && !forceBuff && !skill.isStaticHitTime())
		{
			hitTime = Formulas.getInstance().calcMAtkSpd(this, skill, hitTime);
			if (coolTime > 0)
			{
				coolTime = Formulas.getInstance().calcMAtkSpd(this, skill, coolTime);
			}
		}
		
		// Calculate altered Cast Speed due to BSpS/SpS only for Magic skills
		if ((checkBss() || checkSps()) && !skill.isStaticHitTime() && !skill.isPotion() && skill.isMagic())
		{
			// Only takes 70% of the time to cast a BSpS/SpS cast
			hitTime = (int) (0.70 * hitTime);
			coolTime = (int) (0.70 * coolTime);
		}
		
		if (skill.isPotion())
		{
			// Set the _castEndTime and _castInterruptTim. +10 ticks for lag situations, will be reseted in onMagicFinalizer
			_castPotionEndTime = 10 + GameTimeController.getInstance().getGameTicks() + (coolTime + hitTime) / GameTimeController.MILLIS_IN_TICK;
			_castPotionInterruptTime = -2 + GameTimeController.getInstance().getGameTicks() + hitTime / GameTimeController.MILLIS_IN_TICK;
			
		}
		else
		{
			// Set the _castEndTime and _castInterruptTim. +10 ticks for lag situations, will be reseted in onMagicFinalizer
			_castEndTime = 10 + GameTimeController.getInstance().getGameTicks() + (coolTime + hitTime) / GameTimeController.MILLIS_IN_TICK;
			_castInterruptTime = -2 + GameTimeController.getInstance().getGameTicks() + hitTime / GameTimeController.MILLIS_IN_TICK;
		}
		
		// Like L2OFF after a skill the player must stop the movement, also with toggle
		if (!skill.isPotion() && this instanceof L2PcInstance)
		{
			((L2PcInstance) this).stopMove(null);
		}
		
		// Start the effect as long as the player is casting.
		if (effectWhileCasting)
		{
			callSkill(skill, targets);
		}
		
		// Send a system message USE_S1 to the L2Character
		if (this instanceof L2PcInstance)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.USE_S1);
			switch (magicId)
			{
				case 1312: // Fishing
				{
					// Done in startFishing()
					break;
				}
				case 2005:
				{
					sm.addItemName(728);
					sendPacket(sm);
					break;
				}
				case 2003:
				{
					sm.addItemName(726);
					sendPacket(sm);
					break;
				}
				case 2046:
				{
					// Done in Summon Items
					break;
				}
				case 2166:
				{
					switch (skill.getLevel())
					{
						case 1:
						{
							sm.addItemName(5591);
							sendPacket(sm);
							break;
						}
						case 2:
						{
							sendPacket(sm);
							sm.addItemName(5592);
							break;
						}
					}
					break;
				}
				default:
				{
					sm.addSkillName(skill);
					sendPacket(sm);
					break;
				}
			}
		}
		
		// Init the reuse time of the skill
		int reuseDelay = skill.getReuseDelay();
		
		if (this instanceof L2PcInstance && Formulas.calcSkillMastery(this))
		{
			reuseDelay = 0;
		}
		else if (!skill.isStaticReuse() && !skill.isPotion())
		{
			if (skill.isMagic())
			{
				reuseDelay *= getStat().getMReuseRate(skill);
			}
			else
			{
				reuseDelay *= getStat().getPReuseRate(skill);
			}
			
			reuseDelay *= 333.0 / (skill.isMagic() ? getMAtkSpd() : getPAtkSpd());
		}
		
		// Skill reuse check
		if (reuseDelay > 0)
		{
			addTimeStamp(skill, reuseDelay);
		}
		
		// Check if this skill consume mp on start casting
		int initmpcons = getStat().getMpInitialConsume(skill);
		if (initmpcons > 0)
		{
			if (skill.isDance())
			{
				getStatus().reduceMp(calcStat(Stats.DANCE_MP_CONSUME_RATE, initmpcons, null, null));
			}
			else if (skill.isMagic())
			{
				getStatus().reduceMp(calcStat(Stats.MAGICAL_MP_CONSUME_RATE, initmpcons, null, null));
			}
			else
			{
				getStatus().reduceMp(calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, initmpcons, null, null));
			}
			
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			sendPacket(su);
		}
		
		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
		if (reuseDelay > 0)
		{
			disableSkill(skill, reuseDelay);
		}
		
		// For force buff skills, start the effect as long as the player is casting.
		if (forceBuff)
		{
			startForceBuff(target, skill);
		}
		
		// To turn local player in target direction
		setHeading(Util.calculateHeadingFrom(this, target));
		
		// Send a Server->Client packet MagicSkillUser with target, displayId, level, skillTime, reuseDelay
		// to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		broadcastPacket(new MagicSkillUser(this, target, displayId, level, hitTime, reuseDelay));
		
		if (this instanceof L2PlayableInstance)
		{
			// Before start AI Cast Broadcast Fly Effect is Need
			if ((this instanceof L2PcInstance) && (skill.getFlyType() != null))
			{
				ThreadPoolManager.getInstance().scheduleEffect(new FlyToLocationTask(this, target, skill), 50);
			}
		}
		
		// Get all possible targets of the skill in a table in function of the skill target type
		MagicUseTask mut = new MagicUseTask(targets, skill, hitTime, coolTime);
		
		// launch the magic in hitTime milliseconds
		if (hitTime > 210)
		{
			// Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
			if (this instanceof L2PcInstance && !forceBuff)
			{
				sendPacket(new SetupGauge(SetupGauge.BLUE, hitTime));
			}
			
			if (effectWhileCasting)
			{
				mut.phase = 2;
			}
			
			// Disable all skills during the casting
			if (!skill.isPotion())
			{
				// for particular potion is the timestamp to disable particular skill
				disableAllSkills();
				
				if (_skillCast != null) // delete previous skill cast
				{
					_skillCast.cancel(true);
					_skillCast = null;
				}
				
				// Let's check some skills
				if (_startSkillCheckTask == null && Config.LIVE_CASTING_CHECK && getTarget() != null && skill.getTargetType() == SkillTargetType.TARGET_ONE && skill.getHitTime() >= 1000)
				{
					_startSkillCheckTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SkillCastCheck(this, skill), 0, Config.LIVE_CASTING_CHECK_TIME);
				}
			}
			
			// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
			// For client animation reasons (party buffs especially) 200 ms before!
			if (getForceBuff() != null || effectWhileCasting)
			{
				if (skill.isPotion())
				{
					_potionCast = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime);
				}
				else
				{
					_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime);
				}
			}
			else
			{
				if (skill.isPotion())
				{
					_potionCast = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime - 200);
				}
				else
				{
					_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime - 200);
				}
			}
		}
		else
		{
			mut.hitTime = 0;
			onMagicLaunchedTimer(mut);
		}
	}
	
	public final Map<Integer, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}
	
	public final void addTimeStamp(L2Skill skill, long reuse)
	{
		addTimeStamp(skill, reuse, -1);
	}
	
	public final void addTimeStamp(L2Skill skill, long reuse, long systime)
	{
		if (_reuseTimeStampsSkills == null)
		{
			synchronized (this)
			{
				if (_reuseTimeStampsSkills == null)
				{
					_reuseTimeStampsSkills = new ConcurrentHashMap<>();
				}
			}
		}
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}
	
	public synchronized final void removeTimeStamp(L2Skill skill)
	{
		if (_reuseTimeStampsSkills != null)
		{
			_reuseTimeStampsSkills.remove(skill.getReuseHashCode());
		}
	}
	
	public synchronized final void resetTimeStamps()
	{
		if (_reuseTimeStampsSkills != null)
		{
			_reuseTimeStampsSkills.clear();
		}
	}
	
	public synchronized final long getSkillRemainingReuseTime(int hashCode)
	{
		final TimeStamp reuseStamp = (_reuseTimeStampsSkills != null) ? _reuseTimeStampsSkills.get(hashCode) : null;
		
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	public synchronized final boolean hasSkillReuse(int hashCode)
	{
		final TimeStamp reuseStamp = (_reuseTimeStampsSkills != null) ? _reuseTimeStampsSkills.get(hashCode) : null;
		return (reuseStamp != null) && reuseStamp.hasNotPassed();
	}
	
	public synchronized final TimeStamp getSkillReuseTimeStamp(int hashCode)
	{
		return _reuseTimeStampsSkills != null ? _reuseTimeStampsSkills.get(hashCode) : null;
	}
	
	public void startForceBuff(L2Character target, L2Skill skill)
	{
		if (skill.getSkillType() != SkillType.FORCE_BUFF)
		{
			return;
		}
		
		if (_forceBuff == null)
		{
			_forceBuff = new ForceBuff(this, target, skill);
		}
	}
	
	public void deleteMe()
	{
		if (hasAI())
		{
			getAI().stopAITask();
		}
	}
	
	public boolean doDie(L2Character killer)
	{
		// just in case
		stopSkillCheck();
		
		// killing is only possible one time
		synchronized (this)
		{
			if (isKilledAlready())
			{
				return false;
			}
			
			setIsKilledAlready(true);
		}
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop fear to avoid possible bug with char position after death
		if (isAfraid())
		{
			stopFear(null);
		}
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();
		
		// Stop all active skills effects in progress on the L2Character,
		// if the Character isn't affected by Soul of The Phoenix or Salvation
		if (this instanceof L2PlayableInstance && ((L2PlayableInstance) this).isPhoenixBlessed())
		{
			if (((L2PlayableInstance) this).isNoblesseBlessed())
			{
				((L2PlayableInstance) this).stopNoblesseBlessing(null);
			}
			if (((L2PlayableInstance) this).getCharmOfLuck())
			{
				((L2PlayableInstance) this).stopCharmOfLuck(null);
			}
		}
		// Same thing if the Character isn't a Noblesse Blessed L2PlayableInstance
		else if (this instanceof L2PlayableInstance && ((L2PlayableInstance) this).isNoblesseBlessed())
		{
			((L2PlayableInstance) this).stopNoblesseBlessing(null);
			
			if (((L2PlayableInstance) this).getCharmOfLuck())
			{
				((L2PlayableInstance) this).stopCharmOfLuck(null);
			}
		}
		else
		{
			// to avoid DM Remove buffs on die
			if ((this instanceof L2PcInstance && ((L2PcInstance) this)._inEventDM && DM.is_started()))
			{
				if (Config.DM_REMOVE_BUFFS_ON_DIE)
				{
					stopAllEffects();
				}
			}
			else if (Config.LEAVE_BUFFS_ON_DIE) // this means that the player is not in event dm or is not player
			{
				stopAllEffects();
			}
		}
		
		// if killer is the same then the most damager/hated
		L2Character mostHated = null;
		if (this instanceof L2Attackable)
		{
			mostHated = ((L2Attackable) this)._mostHated;
		}
		
		if (mostHated != null && isInsideRadius(mostHated, 200, false, false))
		{
			calculateRewards(mostHated);
		}
		else
		{
			calculateRewards(killer);
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();
		
		// Notify L2Character AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		
		if (getWorldRegion() != null)
		{
			getWorldRegion().onDeath(this);
		}
		
		// Notify Quest of character's death
		for (QuestState qs : getNotifyQuestOfDeath())
		{
			qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);
		}
		
		getNotifyQuestOfDeath().clear();
		
		getAttackByList().clear();
		
		// If character is PhoenixBlessed a resurrection popup will show up
		if (this instanceof L2PlayableInstance && ((L2PlayableInstance) this).isPhoenixBlessed())
		{
			((L2PcInstance) this).reviveRequest(((L2PcInstance) this), null, false);
		}
		
		// Update active skills in progress (In Use and Not In Use because stacked) icones on client
		updateEffectIcons();
		
		return true;
	}
	
	protected void calculateRewards(L2Character killer)
	{
	}
	
	public void doRevive()
	{
		if (!isTeleporting())
		{
			setIsPendingRevive(false);
			if (this instanceof L2PlayableInstance && ((L2PlayableInstance) this).isPhoenixBlessed())
			{
				((L2PlayableInstance) this).stopPhoenixBlessing(null);
				
				// Like L2OFF Soul of The Phoenix and Salvation restore all hp,cp,mp.
				_status.setCurrentCp(getMaxCp());
				_status.setCurrentHp(getMaxHp());
				_status.setCurrentMp(getMaxMp());
			}
			else
			{
				_status.setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_CP);
				_status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
			}
		}
		// Start broadcast status
		broadcastPacket(new Revive(this));
		
		if (getWorldRegion() != null)
		{
			getWorldRegion().onRevive(this);
		}
		else
		{
			setIsPendingRevive(true);
		}
		fireEvent(EventType.REVIVE.name, (Object[]) null);
	}
	
	/**
	 * Revives the L2Character using skill.
	 * @param revivePower the revive power
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	// Only for monsters
	protected void useMagic(L2Skill skill)
	{
		if (skill == null || isDead() || isAllSkillsDisabled() || skill.isPassive() || isCastingNow() || isAlikeDead() || skill.isChance())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final L2Object target = skill.getFirstOfTargetList(this);
		if (target != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
		}
	}
	
	public L2CharacterAI getAI()
	{
		if (_ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
				{
					return _ai = initAI();
				}
			}
		}
		return _ai;
	}
	
	protected L2CharacterAI initAI()
	{
		return new L2CharacterAI(this);
	}
	
	public void setAI(L2CharacterAI newAI)
	{
		final L2CharacterAI oldAI = _ai;
		if ((oldAI != null) && (oldAI != newAI) && (oldAI instanceof L2AttackableAI))
		{
			oldAI.stopAITask();
		}
		
		_ai = newAI;
	}
	
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	@Override
	public boolean isNpc()
	{
		return false;
	}
	
	public final List<L2Character> getAttackByList()
	{
		if (_attackByList == null)
		{
			_attackByList = new ArrayList<>();
		}
		
		return _attackByList;
	}
	
	public final L2Skill getLastSkillCast()
	{
		return _lastSkillCast;
	}
	
	public void setLastSkillCast(L2Skill skill)
	{
		_lastSkillCast = skill;
	}
	
	public final L2Skill getLastPotionCast()
	{
		return _lastPotionCast;
	}
	
	public void setLastPotionCast(L2Skill skill)
	{
		_lastPotionCast = skill;
	}
	
	public final boolean isAfraid()
	{
		return _isAfraid;
	}
	
	public final void setIsAfraid(boolean value)
	{
		_isAfraid = value;
	}
	
	public final boolean isAlikeDead()
	{
		return isFakeDeath() || !(getCurrentHp() > 0.01);
	}
	
	public final boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isImmobileUntilAttacked() || isStunned() || isSleeping() || isParalyzed();
	}
	
	public boolean isAttackingDisabled()
	{
		return isImmobileUntilAttacked() || isStunned() || isSleeping() || isFallsdown() || isAttackingNow() || isFakeDeath() || isParalyzed() || isAttackDisabled();
	}
	
	public final Calculator[] getCalculators()
	{
		return _calculators;
	}
	
	public final boolean isConfused()
	{
		return _isConfused;
	}
	
	public final void setIsConfused(boolean value)
	{
		_isConfused = value;
	}
	
	@Override
	public final boolean isDead()
	{
		return !isFakeDeath() && (getCurrentHp() < 0.5);
	}
	
	public final boolean isFakeDeath()
	{
		return _isFakeDeath;
	}
	
	/**
	 * Sets the checks if is fake death.
	 * @param value the new checks if is fake death
	 */
	public final void setIsFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}
	
	/**
	 * Return True if the L2Character is flying.
	 * @return true, if is flying
	 */
	public final boolean isFlying()
	{
		return _isFlying;
	}
	
	/**
	 * Set the L2Character flying mode to True.
	 * @param mode the new checks if is flying
	 */
	public final void setIsFlying(boolean mode)
	{
		_isFlying = mode;
	}
	
	/**
	 * Checks if is fallsdown.
	 * @return true, if is fallsdown
	 */
	public final boolean isFallsdown()
	{
		return _isFallsdown;
	}
	
	/**
	 * Sets the checks if is fallsdown.
	 * @param value the new checks if is fallsdown
	 */
	public final void setIsFallsdown(boolean value)
	{
		_isFallsdown = value;
	}
	
	public boolean isImobilised()
	{
		return _isImmobilized;
	}
	
	public void setIsImobilised(boolean value)
	{
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		_isImmobilized = value;
	}
	
	public boolean isBlockDebuff()
	{
		return _isBlockDebuff;
	}
	
	public void setBlockDebuff(boolean blockDebuff)
	{
		_isBlockDebuff = blockDebuff;
	}
	
	public final boolean isKilledAlready()
	{
		return _isKilledAlready;
	}
	
	public final void setIsKilledAlready(boolean value)
	{
		_isKilledAlready = value;
	}
	
	public final boolean isMuted()
	{
		return _isMuted;
	}
	
	public final void setIsMuted(boolean value)
	{
		_isMuted = value;
	}
	
	public final boolean isPsychicalMuted()
	{
		return _isPsychicalMuted;
	}
	
	public final void setIsPsychicalMuted(boolean value)
	{
		_isPsychicalMuted = value;
	}
	
	public boolean isMovementDisabled()
	{
		return isImmobileUntilAttacked() || isStunned() || isRooted() || isSleeping() || isOverloaded() || isParalyzed() || isImobilised() || isFakeDeath() || isFallsdown();
	}
	
	/**
	 * Return True if the L2Character can be controlled by the player (confused, afraid).
	 * @return true, if is out of control
	 */
	public final boolean isOutOfControl()
	{
		return isConfused() || isAfraid() || isBlocked();
	}
	
	/**
	 * Checks if is overloaded.
	 * @return true, if is overloaded
	 */
	public final boolean isOverloaded()
	{
		return _isOverloaded;
	}
	
	/**
	 * Set the overloaded status of the L2Character is overloaded (if True, the L2PcInstance can't take more item).
	 * @param value the new checks if is overloaded
	 */
	public final void setIsOverloaded(boolean value)
	{
		_isOverloaded = value;
	}
	
	/**
	 * Checks if is paralyzed.
	 * @return true, if is paralyzed
	 */
	public final boolean isParalyzed()
	{
		return _isParalyzed;
	}
	
	/**
	 * Sets the checks if is paralyzed.
	 * @param value the new checks if is paralyzed
	 */
	public final void setIsParalyzed(boolean value)
	{
		if (_petrified)
		{
			return;
		}
		
		_isParalyzed = value;
	}
	
	/**
	 * Checks if is pending revive.
	 * @return true, if is pending revive
	 */
	public final boolean isPendingRevive()
	{
		return isDead() && _isPendingRevive;
	}
	
	/**
	 * Sets the checks if is pending revive.
	 * @param value the new checks if is pending revive
	 */
	public final void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}
	
	public L2Summon getPet()
	{
		return null;
	}
	
	public final boolean isRiding()
	{
		return _isRiding;
	}
	
	public final void setIsRiding(boolean mode)
	{
		_isRiding = mode;
	}
	
	public final boolean isRooted()
	{
		return _isRooted;
	}
	
	public final void setIsRooted(boolean value)
	{
		_isRooted = value;
	}
	
	public final boolean isRunning()
	{
		return _isRunning;
	}
	
	public final void setIsRunning(boolean value)
	{
		_isRunning = value;
		broadcastPacket(new ChangeMoveType(this));
	}
	
	public final void setRunning()
	{
		if (!isRunning())
		{
			setIsRunning(true);
		}
	}
	
	public final boolean isImmobileUntilAttacked()
	{
		return _isImmobileUntilAttacked;
	}
	
	public final void setIsImmobileUntilAttacked(boolean value)
	{
		_isImmobileUntilAttacked = value;
	}
	
	public final boolean isSleeping()
	{
		return _isSleeping;
	}
	
	public final void setIsSleeping(boolean value)
	{
		_isSleeping = value;
	}
	
	public final boolean isStunned()
	{
		return _isStunned;
	}
	
	public final void setIsStunned(boolean value)
	{
		_isStunned = value;
	}
	
	public final boolean isBetrayed()
	{
		return _isBetrayed;
	}
	
	public final void setIsBetrayed(boolean value)
	{
		_isBetrayed = value;
	}
	
	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	public void setIsInvul(boolean b)
	{
		if (_petrified)
		{
			return;
		}
		
		_isInvul = b;
	}
	
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}
	
	public boolean isUndead()
	{
		return _template.isUndead;
	}
	
	@Override
	public CharKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof CharKnownList))
		{
			setKnownList(new CharKnownList(this));
		}
		
		return (CharKnownList) super.getKnownList();
	}
	
	public CharStat getStat()
	{
		if (_stat == null)
		{
			_stat = new CharStat(this);
		}
		
		return _stat;
	}
	
	public final void setStat(CharStat value)
	{
		_stat = value;
	}
	
	public CharStatus getStatus()
	{
		if (_status == null)
		{
			_status = new CharStatus(this);
		}
		
		return _status;
	}
	
	public final void setStatus(CharStatus value)
	{
		_status = value;
	}
	
	public L2CharTemplate getTemplate()
	{
		return _template;
	}
	
	protected synchronized final void setTemplate(L2CharTemplate template)
	{
		_template = template;
	}
	
	public final String getTitle()
	{
		if (_title == null)
		{
			return "";
		}
		
		return _title;
	}
	
	public final void setTitle(String value)
	{
		if (value == null)
		{
			value = "";
		}
		
		_title = value;
	}
	
	public final void setTitlePvpPk(String value)
	{
		if (value == null)
		{
			value = "";
		}
		
		_title = value;
	}
	
	public final void setWalking()
	{
		if (isRunning())
		{
			setIsRunning(false);
		}
	}
	
	public class HitTask implements Runnable
	{
		L2Character _hitTarget;
		int _damage;
		boolean _crit;
		boolean _miss;
		boolean _shld;
		boolean _soulshot;
		
		public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
		}
		
		@Override
		public void run()
		{
			try
			{
				onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("Error in HitTask - _hitTarget:" + _hitTarget + " _damage:" + _damage);
					e.printStackTrace();
				}
			}
		}
	}
	
	public class MagicUseTask implements Runnable
	{
		L2Object[] targets;
		L2Skill skill;
		int hitTime;
		int coolTime;
		int phase;
		
		public MagicUseTask(L2Object[] t, L2Skill s, int h, int c)
		{
			targets = t;
			skill = s;
			hitTime = h;
			coolTime = c;
			phase = 1;
		}
		
		@Override
		public void run()
		{
			try
			{
				switch (phase)
				{
					case 1:
						onMagicLaunchedTimer(this);
						break;
					case 2:
						onMagicHitTimer(this);
						break;
					case 3:
						onMagicFinalizer(this);
						break;
					default:
						break;
				}
			}
			catch (Exception e)
			{
				enableAllSkills();
				
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		public L2Object[] getTargets()
		{
			return targets;
		}
	}
	
	public class QueuedMagicUseTask implements Runnable
	{
		L2PcInstance _currPlayer;
		L2Skill _queuedSkill;
		boolean _isCtrlPressed;
		boolean _isShiftPressed;
		
		public QueuedMagicUseTask(L2PcInstance currPlayer, L2Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed)
		{
			_currPlayer = currPlayer;
			_queuedSkill = queuedSkill;
			_isCtrlPressed = isCtrlPressed;
			_isShiftPressed = isShiftPressed;
		}
		
		@Override
		public void run()
		{
			try
			{
				_currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public class QueuedattackTask implements Runnable
	{
		L2PcInstance _currPlayer;
		L2Character _target;
		
		public QueuedattackTask(L2PcInstance currPlayer, L2Character target)
		{
			_currPlayer = currPlayer;
			_target = target;
		}
		
		@Override
		public void run()
		{
			try
			{
				_currPlayer.doAttack(_target);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public class NotifyAITask implements Runnable
	{
		private final CtrlEvent _evt;
		
		public NotifyAITask(CtrlEvent evt)
		{
			_evt = evt;
		}
		
		@Override
		public void run()
		{
			try
			{
				getAI().notifyEvent(_evt);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("error in notifyEvent task:");
					e.printStackTrace();
				}
			}
		}
	}
	
	public class SkillCastCheck implements Runnable
	{
		protected int _skillCastRange = 0;
		protected L2Character _caster;
		protected L2Skill _skill;
		
		public SkillCastCheck(L2Character caster, L2Skill skill)
		{
			if (skill.getEffectRange() > _skillCastRange)
			{
				_skillCastRange = skill.getEffectRange();
			}
			else if (skill.getCastRange() < 0 && skill.getSkillRadius() > 80)
			{
				_skillCastRange = skill.getSkillRadius();
			}
			
			_caster = caster;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			try
			{
				if (!_caster.isCastingNow())
				{
					_caster.stopSkillCheck();
					return;
				}
				
				if (_caster.getTarget() == null)
				{
					_caster.abortCast();
					_caster.stopSkillCheck();
					return;
				}
				
				// Out of range
				if (!Util.checkIfInRange(_skillCastRange + 200, _caster, _caster.getTarget(), true))
				{
					if (_caster instanceof L2PcInstance)
					{
						_caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
					}
					_caster.abortCast();
					_caster.stopSkillCheck();
				}
				
				// Peaceful zone
				if (_caster instanceof L2PcInstance && _skill.isOffensive() && _skill.getSkillType() != SkillType.BUFF && _caster.getTarget() != null && (((L2Character) _caster.getTarget()).isInsidePeaceZone((L2PcInstance) _caster)))
				{
					_caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
					_caster.abortCast();
					_caster.stopSkillCheck();
				}
				
				// Can not see target
				if (!GeoData.getInstance().canSeeTarget(_caster, _caster.getTarget()))
				{
					if (_caster instanceof L2PcInstance)
					{
						_caster.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
					}
					_caster.abortCast();
					_caster.stopSkillCheck();
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("error in SkillCheck task:");
					e.printStackTrace();
				}
			}
		}
	}
	
	public class PvPFlag implements Runnable
	{
		public PvPFlag()
		{
		}
		
		@Override
		public void run()
		{
			try
			{
				if (System.currentTimeMillis() > getPvpFlagLasts())
				{
					stopPvPFlag();
				}
				else if (System.currentTimeMillis() > getPvpFlagLasts() - 5000)
				{
					updatePvPFlag(2);
				}
				else
				{
					updatePvPFlag(1);
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("error in pvp flag task:");
					e.printStackTrace();
				}
			}
		}
	}
	
	private int _AbnormalEffects;
	private LinkedList<L2Effect> _effects = new LinkedList<>();
	protected Map<String, List<L2Effect>> _stackedEffects = new HashMap<>();
	
	public static final int ABNORMAL_EFFECT_BLEEDING = 0x000001;
	public static final int ABNORMAL_EFFECT_POISON = 0x000002;
	public static final int ABNORMAL_EFFECT_REDCIRCLE = 0x000004;
	public static final int ABNORMAL_EFFECT_ICE = 0x000008;
	public static final int ABNORMAL_EFFECT_WIND = 0x0000010;
	public static final int ABNORMAL_EFFECT_FEAR = 0x0000020;
	public static final int ABNORMAL_EFFECT_STUN = 0x000040;
	public static final int ABNORMAL_EFFECT_SLEEP = 0x000080;
	public static final int ABNORMAL_EFFECT_MUTED = 0x000100;
	public static final int ABNORMAL_EFFECT_ROOT = 0x000200;
	public static final int ABNORMAL_EFFECT_HOLD_1 = 0x000400;
	public static final int ABNORMAL_EFFECT_HOLD_2 = 0x000800;
	public static final int ABNORMAL_EFFECT_UNKNOWN_13 = 0x001000;
	public static final int ABNORMAL_EFFECT_BIG_HEAD = 0x002000;
	public static final int ABNORMAL_EFFECT_FLAME = 0x004000;
	public static final int ABNORMAL_EFFECT_UNKNOWN_16 = 0x008000;
	public static final int ABNORMAL_EFFECT_GROW = 0x010000;
	public static final int ABNORMAL_EFFECT_FLOATING_ROOT = 0x020000;
	public static final int ABNORMAL_EFFECT_DANCE_STUNNED = 0x040000;
	public static final int ABNORMAL_EFFECT_FIREROOT_STUN = 0x080000;
	public static final int ABNORMAL_EFFECT_STEALTH = 0x100000;
	public static final int ABNORMAL_EFFECT_IMPRISIONING_1 = 0x200000;
	public static final int ABNORMAL_EFFECT_IMPRISIONING_2 = 0x400000;
	public static final int ABNORMAL_EFFECT_MAGIC_CIRCLE = 0x800000;
	public static final int ABNORMAL_EFFECT_CONFUSED = 0x0020;
	public static final int ABNORMAL_EFFECT_AFRAID = 0x0010;
	
	public synchronized void addEffect(final L2Effect newEffect)
	{
		if (newEffect == null)
		{
			return;
		}
		
		final L2Effect[] effects = getAllEffects();
		
		// Make sure there's no same effect previously
		for (final L2Effect effect : effects)
		{
			if (effect == null)
			{
				synchronized (_effects)
				{
					_effects.remove(effect);
				}
				continue;
			}
			
			if (effect.getSkill().getId() == newEffect.getSkill().getId() //
				&& effect.getEffectType() == newEffect.getEffectType() //
				&& effect.getStackType() == newEffect.getStackType())
			{
				if (this instanceof L2PcInstance)
				{
					final L2PcInstance player = (L2PcInstance) this;
					if (player.isInDuel())
					{
						DuelManager.getInstance().getDuel(player.getDuelId()).onBuffStop(player, effect);
					}
				}
				
				if ((newEffect.getSkill().getSkillType() == SkillType.BUFF //
					|| newEffect.getEffectType() == L2Effect.EffectType.BUFF //
					|| newEffect.getEffectType() == L2Effect.EffectType.HEAL_OVER_TIME) //
					&& newEffect.getStackOrder() >= effect.getStackOrder())
				{
					effect.exit(false);
				}
				else
				{
					newEffect.stopEffectTask();
					return;
				}
			}
		}
		
		final L2Skill tempskill = newEffect.getSkill();
		
		// Remove first Buff if number of buffs > BUFFS_MAX_AMOUNT
		if (getBuffCount() >= getMaxBuffCount() //
			&& !doesStack(tempskill) //
			// && (Config.NO_SLOT_FOR_SELF_BUFFS && (tempskill.getTargetType() != SkillTargetType.TARGET_SELF || !(tempskill.getId() > 7550 && tempskill.getId() < 7555)))//
			&& !(tempskill.getId() > 7550 && tempskill.getId() < 7555) //
			&& (tempskill.getSkillType() == L2Skill.SkillType.BUFF //
				|| tempskill.getSkillType() == L2Skill.SkillType.REFLECT //
				|| tempskill.getSkillType() == L2Skill.SkillType.HEAL_PERCENT //
				|| tempskill.getSkillType() == L2Skill.SkillType.MANAHEAL_PERCENT)
			&& !(tempskill.getId() > 1415 && tempskill.getId() < 1417) //
			&& !(tempskill.getId() > 4360 && tempskill.getId() < 4367) //
			&& !(tempskill.getId() > 4550 && tempskill.getId() < 4555))
		{
			if (newEffect.isHerbEffect())
			{
				newEffect.exit(false);
				return;
			}
			removeFirstBuff(tempskill.getId());
		}
		
		// Remove first DeBuff if number of debuffs > DEBUFFS_MAX_AMOUNT
		if (getDeBuffCount() >= Config.DEBUFFS_MAX_AMOUNT && !doesStack(tempskill) && tempskill.is_Debuff())
		{
			removeFirstDeBuff(tempskill.getId());
		}
		
		synchronized (_effects)
		{
			// Add the L2Effect to all effect in progress on the L2Character
			if (!newEffect.getSkill().isToggle())
			{
				int pos = 0;
				
				for (int i = 0; i < _effects.size(); i++)
				{
					if (_effects.get(i) == null)
					{
						_effects.remove(i);
						i--;
						continue;
					}
					
					if (_effects.get(i) != null)
					{
						final int skillid = _effects.get(i).getSkill().getId();
						
						if (!_effects.get(i).getSkill().isToggle() //
							&& !(skillid > 1415 && skillid < 1417) //
							&& !(skillid > 4360 && skillid < 4367) //
							&& !(skillid > 7550 && skillid < 7555))
						{
							pos++;
						}
					}
					else
					{
						break;
					}
				}
				_effects.add(pos, newEffect);
			}
			else
			{
				_effects.addLast(newEffect);
			}
		}
		
		// Check if a stack group is defined for this effect
		if (newEffect.getStackType().equals("none"))
		{
			// Set this L2Effect to In Use
			newEffect.setInUse(true);
			
			// Add Funcs of this effect to the Calculator set of the L2Character
			addStatFuncs(newEffect.getStatFuncs(), true);
			
			// Update active skills in progress icons on player client
			updateEffectIcons();
			return;
		}
		
		// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
		List<L2Effect> stackQueue = _stackedEffects.get(newEffect.getStackType());
		
		if (stackQueue == null)
		{
			stackQueue = new LinkedList<>();
		}
		
		if (stackQueue.size() > 0)
		{
			// Get the first stacked effect of the Stack group selected
			if (_effects.contains(stackQueue.get(0)))
			{
				// Remove all Func objects corresponding to this stacked effect from the Calculator set of the L2Character
				removeStatsOwner(stackQueue.get(0));
				
				// Set the L2Effect to Not In Use
				stackQueue.get(0).setInUse(false);
			}
		}
		
		// Add the new effect to the stack group selected at its position
		stackQueue = effectQueueInsert(newEffect, stackQueue);
		
		if (stackQueue == null)
		{
			return;
		}
		
		// Update the Stack Group table _stackedEffects of the L2Character
		_stackedEffects.put(newEffect.getStackType(), stackQueue);
		
		if (stackQueue.size() > 0) // Needs to check it
		{
			// Get the first stacked effect of the Stack group selected
			if (_effects.contains(stackQueue.get(0)))
			{
				// Set this L2Effect to In Use
				stackQueue.get(0).setInUse(true);
				
				// Add all Func objects corresponding to this stacked effect to the Calculator set of the L2Character
				addStatFuncs(stackQueue.get(0).getStatFuncs(), true);
			}
		}
		
		// Update active skills in progress (In Use and Not In Use because stacked) icones on client
		updateEffectIcons();
	}
	
	private List<L2Effect> effectQueueInsert(L2Effect newStackedEffect, List<L2Effect> stackQueue)
	{
		// Create an Iterator to go through the list of stacked effects in progress on the L2Character
		Iterator<L2Effect> queueIterator = stackQueue.iterator();
		
		int i = 0;
		while (queueIterator.hasNext())
		{
			L2Effect cur = queueIterator.next();
			if (newStackedEffect.getStackOrder() < cur.getStackOrder())
			{
				i++;
			}
			else
			{
				break;
			}
		}
		
		// Add the new effect to the Stack list in function of its position in the Stack group
		stackQueue.add(i, newStackedEffect);
		
		// skill.exit() could be used, if the users don't wish to see "effect
		// removed" always when a timer goes off, even if the buff isn't active
		// any more (has been replaced). but then check e.g. npc hold and raid petrify.
		if (Config.EFFECT_CANCELING && !newStackedEffect.isHerbEffect() && stackQueue.size() > 1)
		{
			synchronized (_effects)
			{
				_effects.remove(stackQueue.get(1));
			}
			
			stackQueue.remove(1);
		}
		return stackQueue;
	}
	
	/**
	 * Stop and remove L2Effect (including Stack Group management) from L2Character and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * Several same effect can't be used on a L2Character at the same time. Indeed, effects are not stackable and the last cast will replace the previous in progress. More, some effects belong to the same Stack Group (ex WindWald and Haste Potion). If 2 effects of a same group are used at the same
	 * time on a L2Character, only the more efficient (identified by its priority order) will be preserve.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove Func added by this effect from the L2Character Calculator (Stop L2Effect)</li>
	 * <li>If the L2Effect belongs to a not empty Stack Group, replace theses Funcs by next stacked effect Funcs</li>
	 * <li>Remove the L2Effect from _effects of the L2Character</li>
	 * <li>Update active skills in progress icones on player client</li><BR>
	 * @param effect the effect
	 */
	public final void removeEffect(L2Effect effect)
	{
		if (effect == null)
		{
			return;
		}
		
		if (effect.getStackType() == "none")
		{
			// Remove Func added by this effect from the L2Character Calculator
			removeStatsOwner(effect);
		}
		else
		{
			if (_stackedEffects == null)
			{
				return;
			}
			
			// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
			List<L2Effect> stackQueue = _stackedEffects.get(effect.getStackType());
			
			if (stackQueue == null || stackQueue.size() < 1)
			{
				return;
			}
			
			// Get the Identifier of the first stacked effect of the Stack group selected
			L2Effect frontEffect = stackQueue.get(0);
			
			// Remove the effect from the Stack Group
			boolean removed = stackQueue.remove(effect);
			
			if (removed)
			{
				// Check if the first stacked effect was the effect to remove
				if (frontEffect == effect)
				{
					// Remove all its Func objects from the L2Character calculator set
					removeStatsOwner(effect);
					
					// Check if there's another effect in the Stack Group
					if (stackQueue.size() > 0)
					{
						// Add its list of Funcs to the Calculator set of the L2Character
						if (_effects.contains(stackQueue.get(0)))
						{
							// Add its list of Funcs to the Calculator set of the L2Character
							addStatFuncs(stackQueue.get(0).getStatFuncs(), true);
							// Set the effect to In Use
							stackQueue.get(0).setInUse(true);
						}
					}
				}
				if (stackQueue.isEmpty())
				{
					_stackedEffects.remove(effect.getStackType());
				}
				else
				{
					// Update the Stack Group table _stackedEffects of the L2Character
					_stackedEffects.put(effect.getStackType(), stackQueue);
				}
			}
		}
		
		synchronized (_effects)
		{
			// Remove the active skill L2effect from _effects of the L2Character
			_effects.remove(effect);
			
		}
		
		// Update active skills in progress (In Use and Not In Use because stacked) icones on client
		updateEffectIcons();
	}
	
	/**
	 * Active abnormal effects flags in the binary mask and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 * @param mask the mask
	 */
	public final void startAbnormalEffect(int mask)
	{
		_AbnormalEffects |= mask;
		updateAbnormalEffect();
	}
	
	/**
	 * immobile start.
	 */
	public final void startImmobileUntilAttacked()
	{
		setIsImmobileUntilAttacked(true);
		abortAttack();
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Confused flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startConfused()
	{
		setIsConfused(true);
		getAI().notifyEvent(CtrlEvent.EVT_CONFUSED);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Fake Death flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startFakeDeath()
	{
		setIsFallsdown(true);
		setIsFakeDeath(true);
		/* Aborts any attacks/casts if fake dead */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
	}
	
	/**
	 * Active the abnormal effect Fear flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startFear()
	{
		setIsAfraid(true);
		abortAttack();
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_AFFRAID);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Muted flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startMuted()
	{
		setIsMuted(true);
		/* Aborts any casts if muted */
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Psychical_Muted flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startPsychicalMuted()
	{
		setIsPsychicalMuted(true);
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Root flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startRooted()
	{
		setIsRooted(true);
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_ROOTED);
		updateAbnormalEffect();
	}
	
	/**
	 * Active the abnormal effect Sleep flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public final void startSleeping()
	{
		setIsSleeping(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		updateAbnormalEffect();
	}
	
	/**
	 * Launch a Stun Abnormal Effect on the L2Character.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate the success rate of the Stun Abnormal Effect on this L2Character</li>
	 * <li>If Stun succeed, active the abnormal effect Stun flag, notify the L2Character AI and send Server->Client UserInfo/CharInfo packet</li>
	 * <li>If Stun NOT succeed, send a system message Failed to the L2PcInstance attacker</li><BR>
	 * <BR>
	 */
	public final void startStunning()
	{
		if (isStunned())
		{
			return;
		}
		
		setIsStunned(true);
		abortAttack();
		abortCast();
		getAI().stopFollow(); // Like L2OFF char stop to follow if sticked to another one
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_STUNNED);
		updateAbnormalEffect();
	}
	
	public final void startParalyze()
	{
		setIsParalyzed(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_PARALYZED);
	}
	
	public final void startBetray()
	{
		setIsBetrayed(true);
		getAI().notifyEvent(CtrlEvent.EVT_BETRAYED);
		updateAbnormalEffect();
	}
	
	public final void stopBetray()
	{
		stopEffects(L2Effect.EffectType.BETRAY);
		setIsBetrayed(false);
		updateAbnormalEffect();
	}
	
	public final void stopAbnormalEffect(int mask)
	{
		_AbnormalEffects &= ~mask;
		updateAbnormalEffect();
	}
	
	public final void stopAllEffects()
	{
		final L2Effect[] effects = getAllEffects();
		for (int k = 0; k < effects.length; k++)
		{
			
			if (effects[k] != null)
			{
				effects[k].exit(true);
			}
			else
			{
				synchronized (_effects)
				{
					_effects.remove(effects[k]);
				}
			}
		}
		
		if (this instanceof L2PcInstance)
		{
			((L2PcInstance) this).updateAndBroadcastStatus(2);
		}
	}
	
	/**
	 * Stop immobilization until attacked abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) immobilization until attacked abnormal L2Effect from L2Character and update client magic icon</li>
	 * <li>Set the abnormal effect flag _muted to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 * @param effect the effect
	 */
	public final void stopImmobileUntilAttacked(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.IMMOBILEUNTILATTACKED);
		}
		else
		{
			removeEffect(effect);
			stopSkillEffects(effect.getSkill().getNegateId());
		}
		
		setIsImmobileUntilAttacked(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop a specified/all Confused abnormal L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete a specified/all (if effect=null) Confused abnormal L2Effect from L2Character and update client magic icone</li>
	 * <li>Set the abnormal effect flag _confused to False</li>
	 * <li>Notify the L2Character AI</li>
	 * <li>Send Server->Client UserInfo/CharInfo packet</li><BR>
	 * <BR>
	 * @param effect the effect
	 */
	public final void stopConfused(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.CONFUSION);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsConfused(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	/**
	 * Stop and remove the L2Effects corresponding to the L2Skill Identifier and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * @param skillId the skill id
	 */
	public final void stopSkillEffects(int skillId)
	{
		final L2Effect[] effects = getAllEffects();
		for (int i = 0; i < effects.length; i++)
		{
			if (effects[i] == null || effects[i].getSkill() == null)
			{
				synchronized (_effects)
				{
					_effects.remove(effects[i]);
				}
				continue;
			}
			
			if (effects[i].getSkill().getId() == skillId)
			{
				effects[i].exit(true);
			}
		}
	}
	
	/**
	 * Stop and remove all L2Effect of the selected type (ex : BUFF, DMG_OVER_TIME...) from the L2Character and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove Func added by this effect from the L2Character Calculator (Stop L2Effect)</li>
	 * <li>Remove the L2Effect from _effects of the L2Character</li>
	 * <li>Update active skills in progress icones on player client</li><BR>
	 * <BR>
	 * @param type The type of effect to stop ((ex : BUFF, DMG_OVER_TIME...)
	 */
	public final void stopEffects(L2Effect.EffectType type)
	{
		final L2Effect[] effects = getAllEffects();
		for (int i = 0; i < effects.length; i++)
		{
			if (effects[i] == null)
			{
				synchronized (_effects)
				{
					_effects.remove(effects[i]);
				}
				continue;
			}
			
			if (effects[i].getEffectType() == type)
			{
				effects[i].exit(true);
			}
		}
	}
	
	public final void stopSkillEffects(SkillType skillType, double power)
	{
		final L2Effect[] effects = getAllEffects();
		for (int i = 0; i < effects.length; i++)
		{
			if (effects[i] == null || effects[i].getSkill() == null)
			{
				synchronized (_effects)
				{
					_effects.remove(effects[i]);
				}
				continue;
			}
			
			if (effects[i].getSkill().getSkillType() == skillType && (power == 0 || effects[i].getSkill().getPower() <= power))
			{
				effects[i].exit(true);
			}
		}
	}
	
	public final void stopSkillEffects(SkillType skillType)
	{
		stopSkillEffects(skillType, -1);
	}
	
	public final void stopFakeDeath(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.FAKE_DEATH);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsFakeDeath(false);
		setIsFallsdown(false);
		// if this is a player instance, start the grace period for this character (grace from mobs only)!
		if (this instanceof L2PcInstance)
		{
			((L2PcInstance) this).setRecentFakeDeath(true);
		}
		
		ChangeWaitType revive = new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH);
		broadcastPacket(revive);
		broadcastPacket(new Revive(this));
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
	}
	
	public final void stopFear(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.FEAR);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsAfraid(false);
		updateAbnormalEffect();
	}
	
	public final void stopMuted(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.MUTE);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsMuted(false);
		updateAbnormalEffect();
	}
	
	public final void stopPsychicalMuted(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.PSYCHICAL_MUTE);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsPsychicalMuted(false);
		updateAbnormalEffect();
	}
	
	public final void stopRooting(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.ROOT);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsRooted(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	public final void stopSleeping(L2Effect effect)
	{
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.SLEEP);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsSleeping(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	public final void stopStunning(L2Effect effect)
	{
		if (!isStunned())
		{
			return;
		}
		
		if (effect == null)
		{
			stopEffects(L2Effect.EffectType.STUN);
		}
		else
		{
			removeEffect(effect);
		}
		
		setIsStunned(false);
		
		if (this instanceof L2Summon)
		{
			((L2Summon) this).setFollowStatus(true);
		}
		
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}
	
	public abstract void updateAbnormalEffect();
	
	public final void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	public final void updateEffectIcons(boolean partyOnly)
	{
		// Create a L2PcInstance of this if needed
		L2PcInstance player = null;
		if (this instanceof L2PcInstance)
		{
			player = (L2PcInstance) this;
		}
		
		// Create a L2Summon of this if needed
		L2Summon summon = null;
		if (this instanceof L2Summon)
		{
			summon = (L2Summon) this;
			player = summon.getOwner();
			summon.getOwner().sendPacket(new PetInfo(summon));
		}
		
		// Create the main packet if needed
		MagicEffectIcons mi = null;
		if (!partyOnly)
		{
			mi = new MagicEffectIcons();
		}
		
		// Create the party packet if needed
		PartySpelled ps = null;
		if (summon != null)
		{
			ps = new PartySpelled(summon);
		}
		else if (player != null && player.isInParty())
		{
			ps = new PartySpelled(player);
		}
		
		// Create the olympiad spectator packet if needed
		ExOlympiadSpelledInfo os = null;
		if (player != null && player.isInOlympiadMode())
		{
			os = new ExOlympiadSpelledInfo(player);
		}
		
		// Go through all effects if any
		synchronized (_effects)
		{
			for (int i = 0; i < _effects.size(); i++)
			{
				if (_effects.get(i) == null || _effects.get(i).getSkill() == null)
				{
					_effects.remove(i);
					i--;
					continue;
				}
				
				if (_effects.get(i).getEffectType() == L2Effect.EffectType.CHARGE && player != null)
				{
					continue;
				}
				
				if (_effects.get(i).getInUse())
				{
					if (mi != null)
					{
						_effects.get(i).addIcon(mi);
					}
					
					// Like L2OFF toggle and healing potions must not be showed on party buff list
					if (ps != null && !_effects.get(i).getSkill().isToggle() && !(_effects.get(i).getSkill().getId() == 2031) && !(_effects.get(i).getSkill().getId() == 2037) && !(_effects.get(i).getSkill().getId() == 2032))
					{
						_effects.get(i).addPartySpelledIcon(ps);
					}
					
					if (os != null)
					{
						_effects.get(i).addOlympiadSpelledIcon(os);
					}
				}
			}
		}
		
		// Send the packets if needed
		if (mi != null)
		{
			sendPacket(mi);
		}
		
		if (ps != null && player != null)
		{
			if (player.isInParty() && summon == null)
			{
				player.getParty().broadcastToPartyMembers(player, ps);
			}
			else
			{
				player.sendPacket(ps);
			}
		}
		
		if (os != null)
		{
			if (player != null)
			{
				final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
				if (game != null && game.isBattleStarted())
				{
					game.getZone().broadcastPacketToObservers(os);
				}
			}
		}
	}
	
	public int getAbnormalEffect()
	{
		int ae = _AbnormalEffects;
		
		if (isStunned())
		{
			ae |= ABNORMAL_EFFECT_STUN;
		}
		if (isRooted())
		{
			ae |= ABNORMAL_EFFECT_ROOT;
		}
		if (isSleeping())
		{
			ae |= ABNORMAL_EFFECT_SLEEP;
		}
		if (isConfused())
		{
			ae |= ABNORMAL_EFFECT_CONFUSED;
		}
		if (isMuted())
		{
			ae |= ABNORMAL_EFFECT_MUTED;
		}
		if (isAfraid())
		{
			ae |= ABNORMAL_EFFECT_AFRAID;
		}
		if (isPsychicalMuted())
		{
			ae |= ABNORMAL_EFFECT_MUTED;
		}
		
		return ae;
	}
	
	public final L2Effect[] getAllEffects()
	{
		synchronized (_effects)
		{
			L2Effect[] output = _effects.toArray(new L2Effect[_effects.size()]);
			
			return output;
		}
	}
	
	public final L2Effect getFirstEffect(int index)
	{
		final L2Effect[] effects = getAllEffects();
		
		L2Effect effNotInUse = null;
		
		for (int i = 0; i < effects.length; i++)
		{
			if (effects[i] == null)
			{
				synchronized (_effects)
				{
					_effects.remove(effects[i]);
				}
				continue;
			}
			
			if (effects[i].getSkill().getId() == index)
			{
				if (effects[i].getInUse())
				{
					return effects[i];
				}
				
				if (effNotInUse == null)
				{
					effNotInUse = effects[i];
				}
			}
		}
		
		return effNotInUse;
	}
	
	public final L2Effect getFirstEffect(SkillType type)
	{
		final L2Effect[] effects = getAllEffects();
		
		L2Effect effNotInUse = null;
		
		for (int i = 0; i < effects.length; i++)
		{
			if (effects[i] == null)
			{
				synchronized (_effects)
				{
					_effects.remove(effects[i]);
				}
				continue;
			}
			
			if (effects[i].getSkill().getSkillType() == type)
			{
				if (effects[i].getInUse())
				{
					return effects[i];
				}
				
				if (effNotInUse == null)
				{
					effNotInUse = effects[i];
				}
			}
		}
		
		return effNotInUse;
	}
	
	public final L2Effect getFirstEffect(L2Skill skill)
	{
		final L2Effect[] effects = getAllEffects();
		L2Effect effNotInUse = null;
		
		for (int i = 0; i < effects.length; i++)
		{
			if (effects[i] == null)
			{
				synchronized (_effects)
				{
					_effects.remove(effects[i]);
				}
				continue;
			}
			
			if (effects[i].getSkill() == skill)
			{
				if (effects[i].getInUse())
				{
					return effects[i];
				}
				
				if (effNotInUse == null)
				{
					effNotInUse = effects[i];
				}
			}
			
		}
		return effNotInUse;
	}
	
	public final L2Effect getFirstEffect(L2Effect.EffectType tp)
	{
		final L2Effect[] effects = getAllEffects();
		
		L2Effect effNotInUse = null;
		
		for (int i = 0; i < effects.length; i++)
		{
			if (effects[i] == null)
			{
				synchronized (_effects)
				{
					_effects.remove(effects[i]);
				}
				continue;
			}
			
			if (effects[i].getEffectType() == tp)
			{
				if (effects[i].getInUse())
				{
					return effects[i];
				}
				
				if (effNotInUse == null)
				{
					effNotInUse = effects[i];
				}
			}
			
		}
		
		return effNotInUse;
	}
	
	public EffectCharge getChargeEffect()
	{
		L2Effect effect = getFirstEffect(SkillType.CHARGE);
		if (effect != null)
		{
			return (EffectCharge) effect;
		}
		
		return null;
	}
	
	public void detachAI()
	{
		setAI(null);
	}
	
	public static class MoveData
	{
		public int _moveStartTime;
		public int _moveTimestamp; // last update
		public int _xDestination;
		public int _yDestination;
		public int _zDestination;
		public double _xAccurate; // otherwise there would be rounding errors
		public double _yAccurate;
		public double _zAccurate;
		public int _heading;
		
		public boolean disregardingGeodata;
		public int onGeodataPathIndex;
		public List<AbstractNodeLoc> geoPath;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
	}
	
	private volatile Map<Integer, TimeStamp> _reuseTimeStampsSkills = new ConcurrentHashMap<>();
	
	protected Map<Integer, Long> _disabledSkills;
	private boolean _allSkillsDisabled;
	
	private boolean _isRaid = false;
	
	protected MoveData _move;
	
	private int _heading;
	
	private L2Object _target;
	
	private int _castEndTime;
	private int _castInterruptTime;
	private int _castPotionEndTime;
	
	@SuppressWarnings("unused")
	private int _castPotionInterruptTime;
	
	private int _attacking;
	private int _disableBowAttackEndTime;
	
	private static final Calculator[] NPC_STD_CALCULATOR = Formulas.getInstance().getStdNPCCalculators();
	
	protected volatile L2CharacterAI _ai = null;
	protected Future<?> _skillCast;
	protected Future<?> _potionCast;
	
	private List<QuestState> _NotifyQuestOfDeathList = new ArrayList<>();
	
	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null || _NotifyQuestOfDeathList.contains(qs))
		{
			return;
		}
		
		_NotifyQuestOfDeathList.add(qs);
	}
	
	public final List<QuestState> getNotifyQuestOfDeath()
	{
		if (_NotifyQuestOfDeathList == null)
		{
			_NotifyQuestOfDeathList = new ArrayList<>();
		}
		
		return _NotifyQuestOfDeathList;
	}
	
	public final void addStatFunc(Func f)
	{
		if (f == null)
		{
			return;
		}
		
		// Check if Calculator set is linked to the standard Calculator set of NPC
		if (_calculators == NPC_STD_CALCULATOR)
		{
			// Create a copy of the standard NPC Calculator set
			_calculators = new Calculator[Stats.NUM_STATS];
			
			for (int i = 0; i < Stats.NUM_STATS; i++)
			{
				if (NPC_STD_CALCULATOR[i] != null)
				{
					_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
				}
			}
		}
		
		// Select the Calculator of the affected state in the Calculator set
		int stat = f.stat.ordinal();
		
		if (_calculators[stat] == null)
		{
			_calculators[stat] = new Calculator();
		}
		
		// Add the Func to the calculator corresponding to the state
		_calculators[stat].addFunc(f);
	}
	
	public final void removeStatFunc(Func f)
	{
		if (f == null)
		{
			return;
		}
		
		// Select the Calculator of the affected state in the Calculator set
		int stat = f.stat.ordinal();
		
		if (_calculators[stat] == null)
		{
			return;
		}
		
		// Remove the Func object from the Calculator
		_calculators[stat].removeFunc(f);
		
		if (_calculators[stat].size() == 0)
		{
			_calculators[stat] = null;
		}
		
		// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
		if (this instanceof L2NpcInstance)
		{
			int i = 0;
			
			for (; i < Stats.NUM_STATS; i++)
			{
				if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
				{
					break;
				}
			}
			
			if (i >= Stats.NUM_STATS)
			{
				_calculators = NPC_STD_CALCULATOR;
			}
		}
	}
	
	public final void removeStatFuncs(Func[] funcs)
	{
		final List<Stats> modifiedStats = new ArrayList<>();
		for (Func f : funcs)
		{
			modifiedStats.add(f.stat);
			removeStatFunc(f);
		}
		
		broadcastModifiedStats(modifiedStats);
	}
	
	public final void addStatFuncs(Func[] funcs, boolean update)
	{
		final List<Stats> modifiedStats = new ArrayList<>();
		
		for (Func f : funcs)
		{
			modifiedStats.add(f.stat);
			addStatFunc(f);
		}
		
		if (update)
		{
			broadcastModifiedStats(modifiedStats);
		}
	}
	
	public final void removeStatsOwner(Object owner)
	{
		List<Stats> modifiedStats = null;
		
		int i = 0;
		
		synchronized (this)
		{
			for (Calculator calc : _calculators)
			{
				if (calc != null)
				{
					// Delete all Func objects of the selected owner
					if (modifiedStats != null)
					{
						modifiedStats.addAll(calc.removeOwner(owner));
					}
					else
					{
						modifiedStats = calc.removeOwner(owner);
					}
					
					if (calc.size() == 0)
					{
						_calculators[i] = null;
					}
				}
				i++;
			}
			
			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (this instanceof L2NpcInstance)
			{
				i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stats.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
			
			if (owner instanceof L2Effect && !((L2Effect) owner).preventExitUpdate)
			{
				broadcastModifiedStats(modifiedStats);
			}
		}
	}
	
	public void broadcastModifiedStats(List<Stats> stats)
	{
		if (stats == null || stats.isEmpty())
		{
			return;
		}
		
		boolean broadcastFull = false;
		boolean otherStats = false;
		StatusUpdate su = null;
		
		for (Stats stat : stats)
		{
			if (stat == Stats.POWER_ATTACK_SPEED)
			{
				if (su == null)
				{
					su = new StatusUpdate(getObjectId());
				}
				
				su.addAttribute(StatusUpdate.ATK_SPD, getPAtkSpd());
			}
			else if (stat == Stats.MAGIC_ATTACK_SPEED)
			{
				if (su == null)
				{
					su = new StatusUpdate(getObjectId());
				}
				
				su.addAttribute(StatusUpdate.CAST_SPD, getMAtkSpd());
			}
			else if (stat == Stats.MAX_CP)
			{
				if (this instanceof L2PcInstance)
				{
					if (su == null)
					{
						su = new StatusUpdate(getObjectId());
					}
					
					su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
				}
			}
			else if (stat == Stats.RUN_SPEED)
			{
				broadcastFull = true;
			}
			else
			{
				otherStats = true;
			}
		}
		
		if (this instanceof L2PcInstance)
		{
			if (broadcastFull)
			{
				getActingPlayer().updateAndBroadcastStatus(2);
			}
			else
			{
				if (otherStats)
				{
					getActingPlayer().updateAndBroadcastStatus(1);
					
					if (su != null)
					{
						for (L2PcInstance player : getKnownList().getKnownPlayers().values())
						{
							try
							{
								player.sendPacket(su);
							}
							catch (Exception e)
							{
								if (Config.ENABLE_ALL_EXCEPTIONS)
								{
									e.printStackTrace();
								}
							}
						}
					}
				}
				else if (su != null)
				{
					broadcastPacket(su);
				}
			}
		}
		else if (this instanceof L2NpcInstance)
		{
			if (broadcastFull && getKnownList() != null && getKnownList().getKnownPlayers() != null)
			{
				for (L2PcInstance player : getKnownList().getKnownPlayers().values())
				{
					if (player != null)
					{
						player.sendPacket(new NpcInfo((L2NpcInstance) this, player));
					}
				}
			}
			else if (su != null)
			{
				broadcastPacket(su);
			}
		}
		else if (this instanceof L2Summon)
		{
			if (broadcastFull)
			{
				for (L2PcInstance player : getKnownList().getKnownPlayers().values())
				{
					if (player != null)
					{
						player.sendPacket(new NpcInfo((L2Summon) this, player));
					}
				}
			}
			else if (su != null)
			{
				broadcastPacket(su);
			}
		}
		else if (su != null)
		{
			broadcastPacket(su);
		}
	}
	
	public final int getHeading()
	{
		return _heading;
	}
	
	public final void setHeading(int heading)
	{
		_heading = heading;
	}
	
	public final int getXdestination()
	{
		MoveData m = _move;
		
		if (m != null)
		{
			return m._xDestination;
		}
		
		return getX();
	}
	
	public final int getYdestination()
	{
		MoveData m = _move;
		
		if (m != null)
		{
			return m._yDestination;
		}
		
		return getY();
	}
	
	public final int getZdestination()
	{
		MoveData m = _move;
		
		if (m != null)
		{
			return m._zDestination;
		}
		
		return getZ();
	}
	
	public boolean isInCombat()
	{
		return (/* getAI().getTarget() != null || WTF??? */getAI().isAutoAttacking());
	}
	
	public final boolean isMoving()
	{
		return _move != null;
	}
	
	public final boolean isOnGeodataPath()
	{
		MoveData m = _move;
		
		if (m == null)
		{
			return false;
		}
		
		if (m.onGeodataPathIndex == -1)
		{
			return false;
		}
		
		if (m.onGeodataPathIndex == m.geoPath.size() - 1)
		{
			return false;
		}
		
		return true;
	}
	
	public final boolean isCastingNow()
	{
		L2Effect mog = getFirstEffect(L2Effect.EffectType.SIGNET_GROUND);
		if (mog != null)
		{
			return true;
		}
		
		return _castEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	public final boolean isCastingPotionNow()
	{
		return _castPotionEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	public final boolean canAbortCast()
	{
		return _castInterruptTime > GameTimeController.getInstance().getGameTicks();
	}
	
	public final boolean isAttackingNow()
	{
		return _attackEndTime >= System.nanoTime();
	}
	
	public final boolean isAttackAborted()
	{
		return _attacking <= 0;
	}
	
	public final void abortAttack()
	{
		if (isAttackingNow())
		{
			_attacking = 0;
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public final int getAttackingBodyPart()
	{
		return _attacking;
	}
	
	public final void abortCast()
	{
		abortCast(false);
	}
	
	public final void abortCast(boolean force)
	{
		if (isCastingNow() || force)
		{
			_castEndTime = 0;
			_castInterruptTime = 0;
			
			if (_skillCast != null)
			{
				_skillCast.cancel(true);
				_skillCast = null;
			}
			
			if (getForceBuff() != null)
			{
				getForceBuff().onCastAbort();
			}
			
			L2Effect mog = getFirstEffect(L2Effect.EffectType.SIGNET_GROUND);
			if (mog != null)
			{
				mog.exit(true);
			}
			
			enableAllSkills(); // re-enables the skills
			
			if (this instanceof L2PcInstance)
			{
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
			}
			
			broadcastPacket(new MagicSkillCanceld(getObjectId())); // broadcast packet to stop animations client-side
			
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public boolean updatePosition()
	{
		MoveData m = _move;
		
		if (m == null)
		{
			return true;
		}
		
		if (!isVisible())
		{
			_move = null;
			return true;
		}
		
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}
		
		int gameTicks = GameTimeController.getInstance().getGameTicks();
		
		// Check if the position has already been calculated
		if (m._moveTimestamp == gameTicks)
		{
			return false;
		}
		
		int xPrev = getX();
		int yPrev = getY();
		int zPrev = getZ();
		
		if (this instanceof L2PcInstance && !(this instanceof FakePlayer))
		{
			if (Config.COORD_SYNCHRONIZE == 3 && getAI().getIntention() == AI_INTENTION_MOVE_TO)
			{
				zPrev = getClientZ();
				if (zPrev == 0)
				{
					zPrev = getZ();
				}
			}
		}
		
		double dx, dy, dz;
		
		if (Config.COORD_SYNCHRONIZE == 1)
		{
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else
		{
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}
		
		boolean isFloating = isFlying() || isInsideZone(ZoneId.ZONE_WATER);
		
		if ((Config.COORD_SYNCHRONIZE == 2) && !isFloating && !m.disregardingGeodata && ((GameTimeController.getInstance().getGameTicks() % 10) == 0) && GeoData.getInstance().hasGeo(xPrev, yPrev))
		{
			int geoHeight = GeoData.getInstance().getSpawnHeight(xPrev, yPrev, zPrev);
			
			dz = m._zDestination - geoHeight;
			
			// quite a big difference, compare to validatePosition packet
			if (this instanceof L2PcInstance && (Math.abs(getActingPlayer().getClientZ() - geoHeight) > 200) && (Math.abs(getActingPlayer().getClientZ() - geoHeight) < 1500))
			{
				dz = m._zDestination - zPrev; // allow diff
			}
			else if (isInCombat() && (Math.abs(dz) > 200) && (((dx * dx) + (dy * dy)) < 40000)) // allow mob to climb up to pcinstance
			{
				dz = m._zDestination - zPrev; // climbing
			}
			else
			{
				zPrev = geoHeight;
			}
		}
		else
		{
			dz = m._zDestination - zPrev;
		}
		
		double delta = ((dx * dx) + (dy * dy));
		
		if ((delta < 10000) && ((dz * dz) > 2500) && !isFloating)
		{
			delta = Math.sqrt(delta);
		}
		else
		{
			delta = Math.sqrt(delta + (dz * dz));
		}
		
		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			double distPassed = (getStat().getMoveSpeed() * (gameTicks - m._moveTimestamp)) / GameTimeController.TICKS_PER_SECOND;
			
			distFraction = distPassed / delta;
		}
		
		if (distFraction > 1) // already in position
		{
			super.getPosition().setXYZ(m._xDestination, m._yDestination, m._zDestination);
		}
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;
			
			super.getPosition().setXYZ((int) m._xAccurate, (int) m._yAccurate, zPrev + (int) ((dz * distFraction) + 0.5));
		}
		
		revalidateZone(false);
		
		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;
		
		if (distFraction > 1)
		{
			ThreadPoolManager.getInstance().executeAi(() ->
			{
				try
				{
					if (Config.MOVE_BASED_KNOWNLIST)
					{
						getKnownList().findObjects();
					}
					
					getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
				}
			});
			
			if (!(this instanceof FakePlayer))
			{
				return true; // fix for bot stuck;
			}
		}
		
		return false;
	}
	
	public void stopMove(Location loc)
	{
		stopMove(loc, false);
	}
	
	public void stopMove(Location loc, boolean updateKnownObjects)
	{
		_move = null;
		
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
		
		broadcastPacket(new StopMove(this));
		
		if (updateKnownObjects)
		{
			getKnownList().findObjects();
		}
	}
	
	public void setTarget(L2Object object)
	{
		if (object != null && !object.isVisible())
		{
			object = null;
		}
		
		if (object != null && object != _target)
		{
			getKnownList().addKnownObject(object);
			object.getKnownList().addKnownObject(this);
		}
		
		_target = object;
	}
	
	public final int getTargetId()
	{
		if (_target != null)
		{
			return _target.getObjectId();
		}
		
		return -1;
	}
	
	public final L2Object getTarget()
	{
		return _target;
	}
	
	public void moveToLocation(int x, int y, int z, int offset)
	{
		// Block movement during Event start
		if (this instanceof L2PcInstance)
		{
			if (L2Event.active && ((L2PcInstance) this).eventSitForced)
			{
				((L2PcInstance) this).sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up...");
				((L2PcInstance) this).getClient().sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if ((TvT.is_sitForced() && ((L2PcInstance) this)._inEventTvT) || (CTF.is_sitForced() && ((L2PcInstance) this)._inEventCTF) || (DM.is_sitForced() && ((L2PcInstance) this)._inEventDM))
			{
				((L2PcInstance) this).sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up...");
				((L2PcInstance) this).getClient().sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if (VIP._sitForced && ((L2PcInstance) this)._inEventVIP)
			{
				((L2PcInstance) this).sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up...");
				((L2PcInstance) this).sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// when start to move again, it has to stop sitdown task
		if (this instanceof L2PcInstance)
		{
			((L2PcInstance) this).setPosticipateSit(false);
		}
		
		// Get the Move Speed of the L2Charcater
		double speed = getStat().getMoveSpeed();
		if (speed <= 0 || isMovementDisabled())
		{
			return;
		}
		
		// Get current position of the L2Character
		final int curX = super.getX();
		final int curY = super.getY();
		final int curZ = super.getZ();
		
		// Calculate distance (dx,dy) between current position and destination
		double dx = x - curX;
		double dy = y - curY;
		double dz = z - curZ;
		double distance = Math.sqrt(dx * dx + dy * dy);
		
		final boolean verticalMovementOnly = isFlying() && (distance == 0) && (dz != 0);
		if (verticalMovementOnly)
		{
			distance = Math.abs(dz);
		}
		
		if (isInsideZone(ZoneId.ZONE_WATER) && (distance >= 700))
		{
			double divider = 15000 / distance;
			
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			
			dx = x - curX;
			dy = y - curY;
			dz = z - curZ;
			
			distance = Math.sqrt(dx * dx + dy * dy);
		}
		
		double cos;
		double sin;
		
		// Check if a movement offset is defined or no distance to go through
		if (offset > 0 || distance < 1)
		{
			// approximation for moving closer when z coordinates are different
			offset -= Math.abs(dz);
			if (offset < 5)
			{
				offset = 5;
			}
			
			// If no distance to go through, the movement is canceled
			if ((distance < 1) || ((distance - offset) <= 0))
			{
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				return;
			}
			
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
			
			distance -= (offset - 5); // due to rounding error, we have to move a bit closer to be in range
			
			// Calculate the new destination with offset included
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
		}
		
		MoveData m = new MoveData();
		
		m.onGeodataPathIndex = -1;
		m.disregardingGeodata = false;
		
		if (Config.GEODATA && !isFlying() && !isInsideZone(ZoneId.ZONE_WATER) && !(this instanceof L2NpcWalkerInstance))
		{
			final boolean isInVehicle = (this instanceof L2PcInstance) && (getActingPlayer().getVehicle() != null);
			if (isInVehicle)
			{
				m.disregardingGeodata = true;
			}
			
			double originalDistance = distance;
			int originalX = x;
			int originalY = y;
			int originalZ = z;
			int gtx = (originalX - L2World.MAP_MIN_X) >> 4;
			int gty = (originalY - L2World.MAP_MIN_Y) >> 4;
			
			if (this instanceof L2Attackable || this instanceof L2PlayableInstance && !isInVehicle || this instanceof L2RiftInvaderInstance)
			{
				if (isOnGeodataPath())
				{
					try
					{
						if (gtx == _move.geoPathGtx && gty == _move.geoPathGty)
						{
							return;
						}
						_move.onGeodataPathIndex = -1;
					}
					catch (Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
					}
				}
				
				Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z, getInstanceId());
				
				x = destiny.getX();
				y = destiny.getY();
				z = destiny.getZ();
				
				dx = x - curX;
				dy = y - curY;
				dz = z - curZ;
				
				distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
			}
			
			if (((originalDistance - distance) > 30) && (distance <= 3000))
			{
				if ((this instanceof L2PlayableInstance) || this instanceof L2Attackable)
				{
					m.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, getInstanceId(), this instanceof L2PlayableInstance);
					
					if (m.geoPath == null || m.geoPath.size() < 2)
					{
						if ((this instanceof L2PcInstance && getAI().getIntention() != AI_INTENTION_MOVE_TO) || this instanceof L2Attackable || this instanceof L2Summon && !((L2Summon) this).getFollowStatus())
						{
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}
						
						m.disregardingGeodata = true;
						
						if (this instanceof L2Attackable)
						{
							x = originalX;
							y = originalY;
							z = originalZ;
						}
						
						distance = originalDistance;
					}
					else
					{
						m.onGeodataPathIndex = 0;
						m.geoPathGtx = gtx;
						m.geoPathGty = gty;
						m.geoPathAccurateTx = originalX;
						m.geoPathAccurateTy = originalY;
						
						x = m.geoPath.get(m.onGeodataPathIndex).getX();
						y = m.geoPath.get(m.onGeodataPathIndex).getY();
						z = m.geoPath.get(m.onGeodataPathIndex).getZ();
						
						if (DoorTable.getInstance().checkIfDoorsBetween(curX, curY, curZ, x, y, z))
						{
							m.geoPath = null;
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}
						
						for (int i = 0; i < m.geoPath.size() - 1; i++)
						{
							if (DoorTable.getInstance().checkIfDoorsBetween(m.geoPath.get(i), m.geoPath.get(i + 1)))
							{
								m.geoPath = null;
								getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
								return;
							}
						}
						
						dx = x - curX;
						dy = y - curY;
						dz = z - curZ;
						distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt((dx * dx) + (dy * dy));
						sin = dy / distance;
						cos = dx / distance;
					}
				}
			}
			
			if ((distance < 1) && (this instanceof L2PlayableInstance || this instanceof L2RiftInvaderInstance || isAfraid()))
			{
				if (this instanceof L2Summon)
				{
					((L2Summon) this).setFollowStatus(false);
				}
				
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				return;
			}
		}
		
		if (isFlying() || isInsideZone(ZoneId.ZONE_WATER) && !verticalMovementOnly)
		{
			distance = Math.sqrt((distance * distance) + (dz * dz));
		}
		
		int ticksToMove = 1 + (int) ((GameTimeController.TICKS_PER_SECOND * distance) / speed);
		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z;
		m._heading = 0;
		
		if (!verticalMovementOnly)
		{
			setHeading(Util.calculateHeadingFrom(cos, sin));
		}
		
		m._moveStartTime = GameTimeController.getInstance().getGameTicks();
		
		_move = m;
		
		GameTimeController.getInstance().registerMovingObject(this);
		
		if ((ticksToMove * GameTimeController.MILLIS_IN_TICK) > 3000)
		{
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}
	}
	
	public boolean moveToNextRoutePoint()
	{
		if (!isOnGeodataPath())
		{
			_move = null;
			return false;
		}
		
		double speed = getStat().getMoveSpeed();
		if (speed <= 0 || isMovementDisabled())
		{
			_move = null;
			return false;
		}
		
		MoveData md = _move;
		if (md == null)
		{
			return false;
		}
		
		MoveData m = new MoveData();
		m.onGeodataPathIndex = md.onGeodataPathIndex + 1;
		m.geoPath = md.geoPath;
		m.geoPathGtx = md.geoPathGtx;
		m.geoPathGty = md.geoPathGty;
		m.geoPathAccurateTx = md.geoPathAccurateTx;
		m.geoPathAccurateTy = md.geoPathAccurateTy;
		
		if (md.onGeodataPathIndex == md.geoPath.size() - 2)
		{
			m._xDestination = md.geoPathAccurateTx;
			m._yDestination = md.geoPathAccurateTy;
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		else
		{
			m._xDestination = md.geoPath.get(m.onGeodataPathIndex).getX();
			m._yDestination = md.geoPath.get(m.onGeodataPathIndex).getY();
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		
		double dx = (m._xDestination - super.getX());
		double dy = (m._yDestination - super.getY());
		double distance = Math.sqrt((dx * dx) + (dy * dy));
		
		if (distance != 0)
		{
			setHeading(Util.calculateHeadingFrom(getX(), getY(), m._xDestination, m._yDestination));
		}
		
		m._heading = 0;
		m._moveStartTime = GameTimeController.getInstance().getGameTicks();
		
		_move = m;
		
		GameTimeController.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		int ticksToMove = 1 + (int) ((GameTimeController.TICKS_PER_SECOND * distance) / speed);
		if ((ticksToMove * GameTimeController.MILLIS_IN_TICK) > 3000)
		{
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}
		
		broadcastPacket(new CharMoveToLocation(this));
		return true;
	}
	
	public boolean validateMovementHeading(int heading)
	{
		MoveData m = _move;
		
		if (m == null)
		{
			return true;
		}
		
		boolean result = true;
		if (m._heading != heading)
		{
			result = (m._heading == 0); // initial value or false
			m._heading = heading;
		}
		
		return result;
	}
	
	public final double getDistance(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();
		
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public final double getDistance(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	
	/**
	 * Return the squared distance between the current position of the L2Character and the given object.<BR>
	 * <BR>
	 * @param object L2Object
	 * @return the squared distance
	 */
	public final double getDistanceSq(L2Object object)
	{
		return getDistanceSq(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * Return the squared distance between the current position of the L2Character and the given x, y, z.<BR>
	 * <BR>
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @return the squared distance
	 */
	public final double getDistanceSq(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		
		return dx * dx + dy * dy + dz * dz;
	}
	
	/**
	 * Return the squared plan distance between the current position of the L2Character and the given object.<BR>
	 * (check only x and y, not z)<BR>
	 * <BR>
	 * @param object L2Object
	 * @return the squared plan distance
	 */
	public final double getPlanDistanceSq(L2Object object)
	{
		return getPlanDistanceSq(object.getX(), object.getY());
	}
	
	/**
	 * Return the squared plan distance between the current position of the L2Character and the given x, y, z.<BR>
	 * (check only x and y, not z)<BR>
	 * <BR>
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @return the squared plan distance
	 */
	public final double getPlanDistanceSq(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();
		
		return dx * dx + dy * dy;
	}
	
	/**
	 * Check if this object is inside the given radius around the given object. Warning: doesn't cover collision radius!<BR>
	 * <BR>
	 * @param object the target
	 * @param radius the radius around the target
	 * @param checkZ should we check Z axis also
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(L2Object object, int radius, boolean checkZ, boolean strictCheck)
	{
		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}
	
	/**
	 * Check if this object is inside the given plan radius around the given point. Warning: doesn't cover collision radius!<BR>
	 * <BR>
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param radius the radius around the target
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}
	
	/**
	 * Check if this object is inside the given radius around the given point.<BR>
	 * <BR>
	 * @param x X position of the target
	 * @param y Y position of the target
	 * @param z Z position of the target
	 * @param radius the radius around the target
	 * @param checkZ should we check Z axis also
	 * @param strictCheck true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	@Override
	public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		
		if (strictCheck)
		{
			if (checkZ)
			{
				return ((dx * dx) + (dy * dy) + (dz * dz)) < (radius * radius);
			}
			
			return ((dx * dx) + (dy * dy)) < (radius * radius);
		}
		
		if (checkZ)
		{
			return ((dx * dx) + (dy * dy) + (dz * dz)) <= (radius * radius);
		}
		
		return ((dx * dx) + (dy * dy)) <= (radius * radius);
	}
	
	public float getWeaponExpertisePenalty()
	{
		return 1.f;
	}
	
	public float getArmourExpertisePenalty()
	{
		return 1.f;
	}
	
	public void setAttackingBodypart()
	{
		_attacking = Inventory.PAPERDOLL_CHEST;
	}
	
	public void setInCombat()
	{
		_attacking = Inventory.PAPERDOLL_CHEST;
	}
	
	protected boolean checkAndEquipArrows()
	{
		return true;
	}
	
	public void addExpAndSp(long addToExp, int addToSp)
	{
	}
	
	public abstract L2ItemInstance getActiveWeaponInstance();
	
	public abstract L2Weapon getActiveWeaponItem();
	
	public abstract L2ItemInstance getSecondaryWeaponInstance();
	
	public abstract L2Weapon getSecondaryWeaponItem();
	
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld)
	{
		if (target == null)
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if (isAlikeDead() || this instanceof L2NpcInstance && ((L2NpcInstance) this).isEventMob)
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if (this instanceof L2NpcInstance && target.isAlikeDead() || target.isDead() || !getKnownList().knowsObject(target) && !isDoor())
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (miss)
		{
			if (target instanceof L2PcInstance)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_S1S_ATTACK);
				
				if (this instanceof L2Summon)
				{
					int mobId = ((L2Summon) this).getTemplate().getNpcId();
					sm.addNpcName(mobId);
				}
				else
				{
					sm.addString(getName());
				}
				target.sendPacket(sm);
			}
		}
		
		if (!isAttackAborted())
		{
			if (Config.ALLOW_RAID_BOSS_PETRIFIED && target.isRaid() && this instanceof L2PlayableInstance)
			{
				if (getLevel() > target.getLevel() + 8)
				{
					L2Skill curseSkill = SkillTable.getInstance().getInfo(4515, 1);
					if (curseSkill != null)
					{
						abortAttack();
						abortCast();
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						curseSkill.getEffects(target, this, false, false, false);
						
						if (this instanceof L2Summon)
						{
							L2Summon src = ((L2Summon) this);
							if (src.getOwner() != null)
							{
								src.getOwner().abortAttack();
								src.getOwner().abortCast();
								src.getOwner().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
								curseSkill.getEffects(target, src.getOwner(), false, false, false);
							}
						}
					}
					sendDamageMessage(target, 0, false, false, false);
					return;
				}
			}
			
			sendDamageMessage(target, damage, false, crit, miss);
			
			// If L2Character target is a L2PcInstance, send a system message
			if (target instanceof L2PcInstance)
			{
				L2PcInstance enemy = (L2PcInstance) target;
				
				// Check if shield is efficient
				if (shld)
				{
					enemy.sendPacket(new SystemMessage(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL));
					// else if (!miss && damage < 1)
					// enemy.sendMessage("You hit the target's armor.");
				}
			}
			
			if (target instanceof L2Summon)
			{
				L2Summon activeSummon = (L2Summon) target;
				SystemMessage sm = new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_S1);
				sm.addString(getName());
				sm.addNumber(damage);
				activeSummon.getOwner().sendPacket(sm);
			}
			
			if (!miss && damage > 0)
			{
				L2Weapon weapon = getActiveWeaponItem();
				boolean isBow = weapon != null && weapon.getItemType().toString().equalsIgnoreCase("Bow");
				
				if (!isBow) // Do not reflect or absorb if weapon is of type bow
				{
					// Absorb HP from the damage inflicted
					double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);
					
					if (absorbPercent > 0)
					{
						int maxCanAbsorb = (int) (getMaxHp() - getCurrentHp());
						int absorbDamage = (int) (absorbPercent / 100. * damage);
						
						if (absorbDamage > maxCanAbsorb)
						{
							absorbDamage = maxCanAbsorb; // Can't absord more than max hp
						}
						
						if (absorbDamage > 0)
						{
							setCurrentHp(getCurrentHp() + absorbDamage);
						}
					}
					
					// Reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary
					double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
					
					if (reflectPercent > 0)
					{
						int reflectedDamage = (int) (reflectPercent / 100. * damage);
						damage -= reflectedDamage;
						
						if (reflectedDamage > target.getMaxHp())
						{
							reflectedDamage = target.getMaxHp();
						}
						
						getStatus().reduceHp(reflectedDamage, target, true);
					}
				}
				
				target.reduceCurrentHp(damage, this);
				
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
				
				getAI().clientStartAutoAttack();
				
				if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}
				
				// Maybe launch chance skills on us
				if (_chanceSkills != null)
				{
					
					_chanceSkills.onHit(target, false, crit);
				}
				
				// Maybe launch chance skills on target
				if (target.getChanceSkills() != null)
				{
					target.getChanceSkills().onHit(this, true, crit);
				}
			}
			
			L2Weapon activeWeapon = getActiveWeaponItem();
			if (activeWeapon != null)
			{
				activeWeapon.getSkillEffects(this, target, crit);
			}
			
			// Like L2OFF soulshot re-charge
			L2ItemInstance weaponInst = getActiveWeaponInstance();
			if (this instanceof L2Summon && !(this instanceof L2PetInstance))
			{
				((L2Summon) this).setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
			}
			else if (weaponInst != null)
			{
				weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
			}
			// removeSs();
			// return;
		}
	}
	
	public void breakAttack()
	{
		if (isAttackingNow())
		{
			abortAttack();
			
			if (this instanceof L2PcInstance)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}
		}
	}
	
	public void breakCast()
	{
		if (isCastingNow() && canAbortCast() && getLastSkillCast() != null && getLastSkillCast().isMagic())
		{
			abortCast();
			
			if (this instanceof L2PcInstance)
			{
				sendPacket(new SystemMessage(SystemMessageId.CASTING_INTERRUPTED));
			}
		}
	}
	
	protected void reduceArrowCount()
	{
	}
	
	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		if (player.getTarget() == null || !(player.getTarget() instanceof L2Character))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (isInsidePeaceZone(player))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInOlympiadMode() && player.getTarget() != null && player.getTarget() instanceof L2PlayableInstance)
		{
			L2PcInstance target;
			
			if (player.getTarget() instanceof L2Summon)
			{
				target = ((L2Summon) player.getTarget()).getOwner();
			}
			else
			{
				target = (L2PcInstance) player.getTarget();
			}
			
			if (target.isInOlympiadMode() && !player.isOlympiadStart() && player.getOlympiadGameId() == target.getOlympiadGameId())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		if (player.isConfused() || player.isBlocked())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!GeoData.getInstance().canSeeTarget(player, this))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
	}
	
	public boolean isInsidePeaceZone(L2PcInstance attacker)
	{
		return isInsidePeaceZone(attacker, this);
		
	}
	
	public static boolean isInsidePeaceZone(L2Object attacker, L2Object target)
	{
		if (target == null)
		{
			return false;
		}
		
		if (target instanceof L2NpcInstance && Config.DISABLE_ATTACK_NPC_TYPE)
		{
			String mobtype = ((L2NpcInstance) target).getTemplate().type;
			if (Config.LIST_ALLOWED_NPC_TYPES.contains(mobtype))
			{
				return false;
			}
		}
		
		// Attack Monster on Peace Zone like L2OFF.
		if (target instanceof L2MonsterInstance || attacker instanceof L2MonsterInstance && Config.ALT_MOB_AGRO_IN_PEACEZONE)
		{
			return false;
		}
		
		// Attack Guard on Peace Zone like L2OFF.
		if (target instanceof L2GuardInstance || attacker instanceof L2GuardInstance)
		{
			return false;
		}
		// Attack NPC on Peace Zone like L2OFF.
		if (target instanceof L2NpcInstance || attacker instanceof L2NpcInstance)
		{
			return false;
		}
		
		if (Config.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			// allows red to be attacked and red to attack flagged players
			if (target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
			{
				return false;
			}
			
			if (target instanceof L2Summon && ((L2Summon) target).getOwner().getKarma() > 0)
			{
				return false;
			}
			
			if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getKarma() > 0)
			{
				if (target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() > 0)
				{
					return false;
				}
				
				if (target instanceof L2Summon && ((L2Summon) target).getOwner().getPvpFlag() > 0)
				{
					return false;
				}
			}
			
			if (attacker instanceof L2Summon && ((L2Summon) attacker).getOwner().getKarma() > 0)
			{
				if (target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() > 0)
				{
					return false;
				}
				
				if (target instanceof L2Summon && ((L2Summon) target).getOwner().getPvpFlag() > 0)
				{
					return false;
				}
			}
		}
		
		L2PcInstance src = null;
		L2PcInstance dst = null;
		
		if (attacker instanceof L2PlayableInstance && target instanceof L2PlayableInstance)
		{
			if (attacker instanceof L2PcInstance)
			{
				src = (L2PcInstance) attacker;
			}
			else if (attacker instanceof L2Summon)
			{
				src = ((L2Summon) attacker).getOwner();
			}
			
			if (target instanceof L2PcInstance)
			{
				dst = (L2PcInstance) target;
			}
			else if (target instanceof L2Summon)
			{
				dst = ((L2Summon) target).getOwner();
			}
		}
		
		if (src != null && src.getAccessLevel().allowPeaceAttack())
		{
			return false;
		}
		
		// checks on event status
		if (src != null && dst != null)
		{
			// Attacker and target can fight in olympiad with peace zone
			if (src.isInOlympiadMode() && src.isOlympiadStart() && dst.isInOlympiadMode() && dst.isOlympiadStart())
			{
				return false;
			}
			
			if (src.isinTownWar() && dst.isinTownWar())
			{
				return false;
			}
			
			if (dst.isInFunEvent() && src.isInFunEvent())
			{
				
				if (src.isInStartedTVTEvent() && dst.isInStartedTVTEvent())
				{
					return false;
				}
				else if (src.isInStartedDMEvent() && dst.isInStartedDMEvent())
				{
					return false;
				}
				else if (src.isInStartedCTFEvent() && dst.isInStartedCTFEvent())
				{
					return false;
				}
				else if (src.isInStartedVIPEvent() && dst.isInStartedVIPEvent())
				{
					return false;
				}
				else if (src.isInStartedVIPEvent() && dst.isInStartedVIPEvent())
				{
					return false;
				}
			}
		}
		
		if (attacker instanceof L2Character && ((L2Character) attacker).isInsideZone(ZoneId.ZONE_PEACE))
		{
			return true;
		}
		
		if (target instanceof L2Character && ((L2Character) target).isInsideZone(ZoneId.ZONE_PEACE))
		{
			return true;
		}
		
		return false;
	}
	
	public Boolean isInActiveRegion()
	{
		try
		{
			L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
			return region != null && region.isActive();
		}
		catch (Exception e)
		{
			if (this instanceof L2PcInstance)
			{
				LOG.warn("Player " + getName() + " at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
				
				((L2PcInstance) this).sendMessage("Error with your coordinates! Please reboot your game fully!");
				((L2PcInstance) this).teleToLocation(80753, 145481, -3532, getHeading(), false, false, false); // Near Giran luxury shop
			}
			else
			{
				LOG.warn("Object " + getName() + " at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
				decayMe();
			}
			return false;
		}
	}
	
	public boolean isInParty()
	{
		return false;
	}
	
	public L2Party getParty()
	{
		return null;
	}
	
	public int calculateTimeBetweenAttacks(L2Character target, L2Weapon weapon)
	{
		double atkSpd = 0;
		if (weapon != null)
		{
			switch (weapon.getItemType())
			{
				case BOW:
					atkSpd = getStat().getPAtkSpd();
					return (int) (1500 * 345 / atkSpd);
				case DAGGER:
					atkSpd = getStat().getPAtkSpd();
					break;
				default:
					atkSpd = getStat().getPAtkSpd();
			}
		}
		else
		{
			atkSpd = getPAtkSpd();
		}
		return Formulas.getInstance().calcPAtkSpd(this, target, atkSpd);
		
		/* return 500000 / getPAtkSpd(); */
	}
	
	public int calculateReuseTime(L2Character target, L2Weapon weapon)
	{
		if (weapon == null)
		{
			return 0;
		}
		int reuse = weapon.getAttackReuseDelay();
		if (reuse == 0)
		{
			return 0;
		}
		reuse *= getStat().getReuseModifier(target);
		double atkSpd = getStat().getPAtkSpd();
		switch (weapon.getItemType())
		{
			case BOW:
				return (int) (reuse * 345 / atkSpd);
			default:
				return (int) (reuse * 312 / atkSpd);
		}
		
		/*
		 * if ((weapon == null) || (weapon.getAttackReuseDelay() == 0)) { return 0; } return (weapon.getAttackReuseDelay() * 333) / getPAtkSpd();
		 */
	}
	
	public boolean isUsingDualWeapon()
	{
		return false;
	}
	
	@Override
	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;
		
		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			
			// If an old skill has been replaced, remove all its Func objects
			if (oldSkill != null)
			{
				// if skill came with another one, we should delete the other one too.
				if (oldSkill.triggerAnotherSkill())
				{
					_triggeredSkills.remove(oldSkill.getTriggeredId());
					removeSkill(oldSkill.getTriggeredId(), true);
				}
				
				removeStatsOwner(oldSkill);
			}
			
			// Add Func objects of newSkill to the calculator set of the L2Character
			addStatFuncs(newSkill.getStatFuncs(null, this), true);
			if ((oldSkill != null) && (_chanceSkills != null))
			{
				removeChanceSkill(oldSkill.getId());
			}
			if (newSkill.isChance() || newSkill.isPassive())
			{
				addChanceSkill(newSkill);
			}
			
			if ((newSkill.isChance() || newSkill.isPassive()) && newSkill.triggerAnotherSkill())
			{
				L2Skill triggeredSkill = SkillTable.getInstance().getInfo(newSkill.getTriggeredId(), newSkill.getTriggeredLevel());
				addSkill(triggeredSkill);
			}
			
			if (newSkill.triggerAnotherSkill())
			{
				_triggeredSkills.put(newSkill.getTriggeredId(), SkillTable.getInstance().getInfo(newSkill.getTriggeredId(), newSkill.getTriggeredLevel()));
			}
		}
		
		return oldSkill;
	}
	
	public void addChanceSkill(L2Skill skill)
	{
		synchronized (this)
		{
			if (_chanceSkills == null)
			{
				_chanceSkills = new ChanceSkillList(this);
			}
			
			_chanceSkills.put(skill, skill.getChanceCondition());
		}
	}
	
	public void removeChanceSkill(int id)
	{
		synchronized (this)
		{
			for (L2Skill skill : _chanceSkills.keySet())
			{
				if (skill.getId() == id)
				{
					_chanceSkills.remove(skill);
				}
			}
			
			if (_chanceSkills.isEmpty())
			{
				_chanceSkills = null;
			}
		}
	}
	
	public synchronized L2Skill removeSkill(L2Skill skill)
	{
		if (skill == null)
		{
			return null;
		}
		
		// Remove the skill from the L2Character _skills
		return removeSkill(skill.getId());
	}
	
	/**
	 * Removes the skill.
	 * @param skillId the skill id
	 * @return the l2 skill
	 */
	public L2Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}
	
	public L2Skill removeSkill(int skillId, boolean cancelEffect)
	{
		// Remove the skill from the L2Character _skills
		L2Skill oldSkill = _skills.remove(skillId);
		
		// Remove all its Func objects from the L2Character calculator set
		if (oldSkill != null)
		{
			// this is just a fail-safe againts buggers and gm dummies...
			if (oldSkill.triggerAnotherSkill())
			{
				removeSkill(oldSkill.getTriggeredId(), true);
				_triggeredSkills.remove(oldSkill.getTriggeredId());
			}
			
			// Stop casting if this skill is used right now
			if (getLastSkillCast() != null && isCastingNow())
			{
				if (oldSkill.getId() == getLastSkillCast().getId())
				{
					abortCast();
				}
			}
			
			if (cancelEffect || oldSkill.isToggle())
			{
				L2Effect e = getFirstEffect(oldSkill);
				if (e == null)
				{
					removeStatsOwner(oldSkill);
					stopSkillEffects(oldSkill.getId());
				}
			}
			
			if (oldSkill.isChance() && _chanceSkills != null)
			{
				removeChanceSkill(oldSkill.getId());
			}
			removeStatsOwner(oldSkill);
		}
		return oldSkill;
	}
	
	public final L2Skill[] getAllSkills()
	{
		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}
	
	@Override
	public Map<Integer, L2Skill> getSkills()
	{
		return _skills;
	}
	
	public ChanceSkillList getChanceSkills()
	{
		return _chanceSkills;
	}
	
	@Override
	public int getSkillLevel(int skillId)
	{
		L2Skill skill = _skills.get(skillId);
		
		if (skill == null)
		{
			return -1;
		}
		
		return skill.getLevel();
	}
	
	public L2Skill getSkill(int skillId)
	{
		return getSkills().get(skillId);
	}
	
	@Override
	public final L2Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
	
	public int getBuffCount()
	{
		L2Effect[] effects = getAllEffects();
		
		int numBuffs = 0;
		
		for (L2Effect e : effects)
		{
			if (e == null)
			{
				synchronized (_effects)
				{
					_effects.remove(e);
				}
				continue;
			}
			
			if ((e.getSkill().getSkillType() == L2Skill.SkillType.BUFF || e.getSkill().getSkillType() == L2Skill.SkillType.REFLECT || e.getSkill().getSkillType() == L2Skill.SkillType.HEAL_PERCENT || e.getSkill().getSkillType() == L2Skill.SkillType.MANAHEAL_PERCENT)
				&& !(e.getSkill().getId() > 1415 && e.getSkill().getId() < 1417) && !(e.getSkill().getId() > 4360 && e.getSkill().getId() < 4367)) // 7s
			{
				numBuffs++;
			}
		}
		
		return numBuffs;
	}
	
	public int getDeBuffCount()
	{
		L2Effect[] effects = getAllEffects();
		int numDeBuffs = 0;
		
		for (L2Effect e : effects)
		{
			if (e == null)
			{
				synchronized (_effects)
				{
					_effects.remove(e);
				}
				continue;
			}
			
			// Check for all debuff skills
			if (e.getSkill().is_Debuff())
			{
				numDeBuffs++;
			}
		}
		
		return numDeBuffs;
	}
	
	public int getMaxBuffCount()
	{
		int maxBuffs = Config.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
		return maxBuffs;
	}
	
	public void removeFirstBuff(int preferSkill)
	{
		L2Effect[] effects = getAllEffects();
		
		L2Effect removeMe = null;
		
		for (L2Effect e : effects)
		{
			if (e == null)
			{
				synchronized (_effects)
				{
					_effects.remove(e);
				}
				continue;
			}
			
			if ((e.getSkill().getSkillType() == L2Skill.SkillType.BUFF || e.getSkill().getSkillType() == L2Skill.SkillType.REFLECT || e.getSkill().getSkillType() == L2Skill.SkillType.HEAL_PERCENT || e.getSkill().getSkillType() == L2Skill.SkillType.MANAHEAL_PERCENT)
				&& (!(e.getSkill().getId() > 1415 && e.getSkill().getId() < 1417)) && (!(e.getSkill().getId() > 4360 && e.getSkill().getId() < 4367)))
			{
				if (preferSkill == 0)
				{
					removeMe = e;
					break;
				}
				else if (e.getSkill().getId() == preferSkill)
				{
					removeMe = e;
					break;
				}
				else if (removeMe == null)
				{
					removeMe = e;
				}
			}
		}
		
		if (removeMe != null)
		{
			removeMe.exit(true);
		}
	}
	
	public void removeEffect(int skillId)
	{
		L2Effect[] effects = getAllEffects();
		L2Effect removeMe = null;
		
		for (L2Effect e : effects)
		{
			if (e == null)
			{
				continue;
			}
			
			if (e.getSkill().getId() == skillId)
			{
				removeMe = e;
			}
		}
		
		if (removeMe != null)
		{
			removeMe.exit(true);
		}
	}
	
	public void removeFirstDeBuff(int preferSkill)
	{
		L2Effect[] effects = getAllEffects();
		
		L2Effect removeMe = null;
		
		for (L2Effect e : effects)
		{
			if (e == null)
			{
				synchronized (_effects)
				{
					_effects.remove(e);
				}
				continue;
			}
			
			if (e.getSkill().is_Debuff())
			{
				if (preferSkill == 0)
				{
					removeMe = e;
					break;
				}
				else if (e.getSkill().getId() == preferSkill)
				{
					removeMe = e;
					break;
				}
				else if (removeMe == null)
				{
					removeMe = e;
				}
			}
		}
		
		if (removeMe != null)
		{
			removeMe.exit(true);
		}
	}
	
	public int getDanceCount()
	{
		int danceCount = 0;
		
		L2Effect[] effects = getAllEffects();
		
		for (L2Effect e : effects)
		{
			if (e == null)
			{
				synchronized (_effects)
				{
					_effects.remove(e);
				}
				continue;
			}
			
			if (e.getSkill().isDance() && e.getInUse())
			{
				danceCount++;
			}
		}
		
		return danceCount;
	}
	
	public boolean doesStack(L2Skill checkSkill)
	{
		if (_effects.size() < 1 || checkSkill._effectTemplates == null || checkSkill._effectTemplates.length < 1 || checkSkill._effectTemplates[0].stackType == null)
		{
			return false;
		}
		
		String stackType = checkSkill._effectTemplates[0].stackType;
		
		if (stackType.equals("none"))
		{
			return false;
		}
		
		L2Effect[] effects = getAllEffects();
		
		for (L2Effect e : effects)
		{
			if (e == null)
			{
				synchronized (_effects)
				{
					_effects.remove(e);
				}
				continue;
			}
			
			if (e.getStackType() != null && e.getStackType().equals(stackType))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void onMagicLaunchedTimer(MagicUseTask mut)
	{
		stopSkillCheck();
		
		final L2Skill skill = mut.skill;
		L2Object[] targets = mut.targets;
		
		if (skill == null || (targets == null))
		{
			abortCast();
			_skillCast = null;
			enableAllSkills();
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if (this instanceof L2PcInstance && (skill.getTargetType() == SkillTargetType.TARGET_SELF && !((L2PcInstance) this).isGM() && (skill.getSkillType() == SkillType.BUFF)))
		{
			// like l2off mystic immunity must block self buffs
			if (calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0)
			{
				abortCast();
				return;
			}
			
			// like l2off invul skill must block self buffs
			if (this.isInvul())
			{
				abortCast();
				return;
			}
		}
		
		if (skill.isOffensive())
		{
			getAI().clientStartAutoAttack();
		}
		
		if (targets.length == 0)
		{
			switch (skill.getTargetType())
			{
				// only AURA-type skills can be cast without target
				case TARGET_AURA:
				case TARGET_FRONT_AURA:
				case TARGET_BEHIND_AURA:
					break;
				default:
					abortCast();
					return;
			}
		}
		
		// Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
		int escapeRange = 0;
		if (skill.getEffectRange() > escapeRange)
		{
			escapeRange = skill.getEffectRange();
		}
		else if (skill.getCastRange() < 0 && skill.getSkillRadius() > 80)
		{
			escapeRange = skill.getSkillRadius();
		}
		
		if ((targets.length > 0) && (escapeRange > 0))
		{
			
			int _skiprange = 0;
			int _skipgeo = 0;
			int _skippeace = 0;
			
			final List<L2Character> targetList = new ArrayList<>();
			for (L2Object target : targets)
			{
				if (target instanceof L2Character)
				{
					if (!Util.checkIfInRange(escapeRange, this, target, true))
					{
						_skiprange++;
						continue;
					}
					
					// Check if the target is behind a wall
					if ((skill.getSkillRadius() > 0) && skill.isOffensive() && Config.GEODATA && !GeoData.getInstance().canSeeTarget(this, target))
					{
						_skipgeo++;
						continue;
					}
					
					if (skill.isOffensive())
					{
						if (this instanceof L2PcInstance)
						{
							if (((L2Character) target).isInsidePeaceZone((L2PcInstance) this))
							{
								_skippeace++;
								continue;
							}
						}
						else
						{
							if (L2Character.isInsidePeaceZone(this, target))
							{
								_skippeace++;
								continue;
							}
						}
					}
					targetList.add((L2Character) target);
				}
			}
			
			if (targetList.isEmpty())
			{
				if (this instanceof L2PcInstance)
				{
					if (_skiprange > 0)
					{
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
					}
					else if (_skipgeo > 0)
					{
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
					}
					else if (_skippeace > 0)
					{
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
					}
				}
				abortCast();
				return;
			}
			mut.targets = targetList.toArray(new L2Character[targetList.size()]);
		}
		
		// if the skill is not a potion and player
		// is not casting now
		// Ensure that a cast is in progress
		// Check if player is using fake death.
		// Potions can be used while faking death.
		if (!skill.isPotion() && (!isCastingNow() || isAlikeDead()))
		{
			_skillCast = null;
			enableAllSkills();
			
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			
			_castEndTime = 0;
			_castInterruptTime = 0;
			return;
		}
		
		// Get the display identifier of the skill
		int magicId = skill.getDisplayId();
		
		// Get the level of the skill
		int level = getSkillLevel(skill.getId());
		
		if (level < 1)
		{
			level = 1;
		}
		
		// Send a Server->Client packet MagicSkillLaunched to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		if (!skill.isPotion())
		{
			broadcastPacket(new MagicSkillLaunched(this, magicId, level, targets));
		}
		
		mut.phase = 2;
		if (mut.hitTime == 0)
		{
			onMagicHitTimer(mut);
		}
		else
		{
			if (skill.isPotion())
			{
				_potionCast = ThreadPoolManager.getInstance().scheduleEffect(mut, 200);
			}
			else
			{
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, 200);
			}
		}
	}
	
	public void onMagicHitTimer(MagicUseTask mut)
	{
		final L2Skill skill = mut.skill;
		final L2Object[] targets = mut.targets;
		
		if (skill == null || (targets == null || targets.length <= 0) && skill.getTargetType() != SkillTargetType.TARGET_AURA)
		{
			_skillCast = null;
			enableAllSkills();
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		
		if (getForceBuff() != null)
		{
			_skillCast = null;
			enableAllSkills();
			getForceBuff().onCastAbort();
			return;
		}
		
		L2Effect mog = getFirstEffect(L2Effect.EffectType.SIGNET_GROUND);
		if (mog != null)
		{
			_skillCast = null;
			enableAllSkills();
			
			// close skill if it's not SIGNET_CASTTIME
			if (mog.getSkill().getSkillType() != SkillType.SIGNET_CASTTIME)
			{
				mog.exit(true);
			}
			
			L2Object target = targets == null ? null : targets[0];
			if (target != null)
			{
				notifyQuestEventSkillFinished(skill, target);
			}
			return;
		}
		
		try
		{
			if (targets != null && targets.length != 0)
			{
				// Go through targets table
				for (int i = 0; i < targets.length; i++)
				{
					L2Object target2 = targets[i];
					if (target2 == null)
					{
						continue;
					}
					
					if (target2 instanceof L2PlayableInstance)
					{
						L2Character target = (L2Character) target2;
						
						// If the skill is type STEALTH(ex: Dance of Shadow)
						if (skill.isAbnormalEffectByName(ABNORMAL_EFFECT_STEALTH))
						{
							L2Effect silentMove = target.getFirstEffect(L2Effect.EffectType.SILENT_MOVE);
							if (silentMove != null)
							{
								silentMove.exit(true);
							}
						}
						
						if (skill.getSkillType() == L2Skill.SkillType.BUFF || skill.getSkillType() == L2Skill.SkillType.SEED)
						{
							SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
							smsg.addString(skill.getName());
							target.sendPacket(smsg);
						}
						
						if (this instanceof L2PcInstance && target instanceof L2Summon)
						{
							((L2Summon) target).getOwner().sendPacket(new PetInfo((L2Summon) target));
							sendPacket(new NpcInfo((L2Summon) target, this));
							
							// The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
							((L2Summon) target).updateEffectIcons(true);
						}
					}
				}
				
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		
		try
		{
			StatusUpdate su = new StatusUpdate(getObjectId());
			boolean isSendStatus = false;
			
			// Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
			double mpConsume = getStat().getMpConsume(skill);
			if (mpConsume > 0)
			{
				if (skill.isDance())
				{
					getStatus().reduceMp(calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume, null, null));
				}
				else if (skill.isMagic())
				{
					getStatus().reduceMp(calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume, null, null));
				}
				else
				{
					getStatus().reduceMp(calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume, null, null));
				}
				
				su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
				isSendStatus = true;
			}
			
			// Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
			if (skill.getHpConsume() > 0)
			{
				double consumeHp;
				
				consumeHp = calcStat(Stats.HP_CONSUME_RATE, skill.getHpConsume(), null, null);
				
				if (consumeHp + 1 >= getCurrentHp())
				{
					consumeHp = getCurrentHp() - 1.0;
				}
				
				getStatus().reduceHp(consumeHp, this);
				
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				isSendStatus = true;
			}
			
			// Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance
			if (isSendStatus)
			{
				sendPacket(su);
			}
			
			// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
			if (skill.getItemConsume() > 0)
			{
				if (Config.DONT_DESTROY_CURSED_BONES && skill.getItemConsumeId() == 2508)
				{
					consumeItem(skill.getItemConsumeId(), 0);
				}
				else
				{
					consumeItem(skill.getItemConsumeId(), skill.getItemConsume());
				}
			}
			
			// Launch the magic skill in order to calculate its effects
			callSkill(mut.skill, mut.targets);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		
		mut.phase = 3;
		
		if ((mut.hitTime == 0) || (mut.coolTime == 0))
		{
			onMagicFinalizer(mut);
		}
		else
		{
			if (skill.isPotion())
			{
				_potionCast = ThreadPoolManager.getInstance().scheduleEffect(mut, mut.coolTime);
			}
			else
			{
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, 0); // removed stupid time mut.coolTime
			}
		}
	}
	
	public void onMagicFinalizer(MagicUseTask mut)
	{
		final L2Skill skill = mut.skill;
		final L2Object target = mut.getTargets().length > 0 ? mut.getTargets()[0] : null;
		
		if (skill.isPotion())
		{
			_potionCast = null;
			_castPotionEndTime = 0;
			_castPotionInterruptTime = 0;
		}
		else
		{
			_skillCast = null;
			_castEndTime = 0;
			_castInterruptTime = 0;
			
			enableAllSkills();
			
			/*
			 * if (skill.getId() != 345 && skill.getId() != 346) { if (getAI().getNextIntention() == null && (skill.getSkillType() == SkillType.PDAM && skill.getCastRange() < 400) || skill.getSkillType() == SkillType.BLOW || skill.getSkillType() == SkillType.DRAIN_SOUL || skill.getSkillType() ==
			 * SkillType.SOW || skill.getSkillType() == SkillType.SPOIL) { if (this instanceof L2PcInstance) { L2PcInstance currPlayer = (L2PcInstance) this; SkillDat skilldat = currPlayer.getCurrentSkill(); if (skilldat != null && !skilldat.isCtrlPressed() && skill.nextActionIsAttack() &&
			 * getTarget() != null && getTarget() instanceof L2Character) { getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getTarget()); } } else { if (skill.nextActionIsAttack() && getTarget() != null && getTarget() instanceof L2Character) {
			 * getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getTarget()); } else if ((skill.isOffensive()) && !(skill.getSkillType() == SkillType.UNLOCK) && !(skill.getSkillType() == SkillType.BLOW) && !(skill.getSkillType() == SkillType.DELUXE_KEY_UNLOCK) && skill.getId() != 345 &&
			 * skill.getId() != 346) { getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getTarget()); getAI().clientStartAutoAttack(); } } } if (this instanceof L2PcInstance) { L2PcInstance currPlayer = (L2PcInstance) this; SkillDat skilldat = currPlayer.getCurrentSkill(); if (skilldat !=
			 * null && !skilldat.isCtrlPressed() && (skill.isOffensive()) && !(skill.getSkillType() == SkillType.UNLOCK) && !(skill.getSkillType() == SkillType.BLOW) && !(skill.getSkillType() == SkillType.DELUXE_KEY_UNLOCK) && skill.getId() != 345 && skill.getId() != 346) { if (!skill.isMagic() &&
			 * skill.nextActionIsAttack()) { getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getTarget()); } getAI().clientStartAutoAttack(); } } else { if ((skill.isOffensive()) && !(skill.getSkillType() == SkillType.UNLOCK) && !(skill.getSkillType() == SkillType.BLOW) &&
			 * !(skill.getSkillType() == SkillType.DELUXE_KEY_UNLOCK) && skill.getId() != 345 && skill.getId() != 346) { if (!skill.isMagic()) { getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getTarget()); } getAI().clientStartAutoAttack(); } } } else { getAI().clientStopAutoAttack(); }
			 */
			
			// Attack target after skill use
			if ((skill.nextActionIsAttack()) && (getTarget() instanceof L2NpcInstance) && (target != null) && (getTarget() == target) && getTarget().isAutoAttackable(this))
			{
				if ((getAI().getNextIntention() == null) || (getAI().getNextIntention().getCtrlIntention() != CtrlIntention.AI_INTENTION_MOVE_TO))
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
				}
			}
			
			if (skill.isOffensive() && !(skill.getSkillType() == SkillType.UNLOCK) && !(skill.getSkillType() == SkillType.DELUXE_KEY_UNLOCK))
			{
				getAI().clientStartAutoAttack();
			}
			
			getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
			
			notifyQuestEventSkillFinished(skill, target);
			
			if (this instanceof L2PcInstance)
			{
				L2PcInstance currPlayer = (L2PcInstance) this;
				SkillDat queuedSkill = currPlayer.getQueuedSkill();
				
				currPlayer.setCurrentSkill(null, false, false);
				
				if (queuedSkill != null)
				{
					currPlayer.setQueuedSkill(null, false, false);
					ThreadPoolManager.getInstance().executeTask(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
				}
				
				/*
				 * final L2Weapon activeWeapon = getActiveWeaponItem(); if (activeWeapon != null) { try { if (target != null && mut.getTargets().length > 0) { for (L2Object target2 : mut.getTargets()) { if (target2 != null && target2 instanceof L2Character && !((L2Character) target2).isDead()) {
				 * final L2Character player = (L2Character) target2; if (activeWeapon.getSkillEffects(this, player, skill)) { sendPacket(SystemMessage.sendString("Target affected by weapon special ability.")); } } } } } catch ( Exception e) { if (Config.ENABLE_ALL_EXCEPTIONS) { e.printStackTrace();
				 * } } }
				 */
				
			}
		}
	}
	
	private void notifyQuestEventSkillFinished(L2Skill skill, L2Object target)
	{
		if (this instanceof L2NpcInstance && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			L2PcInstance player = target instanceof L2PcInstance ? (L2PcInstance) target : ((L2Summon) target).getOwner();
			
			for (Quest quest : ((L2NpcTemplate) getTemplate()).getEventQuests(Quest.QuestEventType.ON_SPELL_FINISHED))
			{
				quest.notifySpellFinished(((L2NpcInstance) this), player, skill);
			}
		}
	}
	
	public void consumeItem(int itemConsumeId, int itemCount)
	{
	}
	
	public Map<Integer, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}
	
	public void enableSkill(L2Skill skill)
	{
		if (skill == null || _disabledSkills == null)
		{
			return;
		}
		
		_disabledSkills.remove(skill.getReuseHashCode());
	}
	
	public void disableSkill(L2Skill skill, long delay)
	{
		if (skill == null)
		{
			return;
		}
		
		if (_disabledSkills == null)
		{
			synchronized (this)
			{
				if (_disabledSkills == null)
				{
					_disabledSkills = new ConcurrentHashMap<>();
				}
			}
		}
		
		_disabledSkills.put(skill.getReuseHashCode(), delay > 0 ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
	}
	
	public boolean isSkillDisabled(int hashCode)
	{
		/*
		 * if (isAllSkillsDisabled()) { return true; }
		 */
		
		if (_disabledSkills == null)
		{
			return false;
		}
		
		final Long stamp = _disabledSkills.get(hashCode);
		if (stamp == null)
		{
			return false;
		}
		
		if (stamp < System.currentTimeMillis())
		{
			_disabledSkills.remove(hashCode);
			return false;
		}
		return true;
	}
	
	public boolean isSkillDisabled(L2Skill skill)
	{
		if (skill == null)
		{
			return true;
		}
		
		if (this instanceof L2PcInstance)
		{
			final L2PcInstance activeChar = (L2PcInstance) this;
			
			if ((skill.getSkillType() == SkillType.FISHING || skill.getSkillType() == SkillType.REELING || skill.getSkillType() == SkillType.PUMPING) && !activeChar.isFishing() && (activeChar.getActiveWeaponItem() != null && activeChar.getActiveWeaponItem().getItemType() != L2WeaponType.ROD))
			{
				if (skill.getSkillType() == SkillType.PUMPING)
				{
					// Pumping skill is available only while fishing
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CAN_USE_PUMPING_ONLY_WHILE_FISHING));
				}
				else if (skill.getSkillType() == SkillType.REELING)
				{
					// Reeling skill is available only while fishing
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CAN_USE_REELING_ONLY_WHILE_FISHING));
				}
				else if (skill.getSkillType() == SkillType.FISHING)
				{
					// Player hasn't fishing pole equiped
					activeChar.sendPacket(new SystemMessage(SystemMessageId.FISHING_POLE_NOT_EQUIPPED));
				}
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addString(skill.getName());
				activeChar.sendPacket(sm);
				return true;
			}
			
			if ((skill.getSkillType() == SkillType.FISHING || skill.getSkillType() == SkillType.REELING || skill.getSkillType() == SkillType.PUMPING) && activeChar.getActiveWeaponItem() == null)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addString(skill.getName());
				activeChar.sendPacket(sm);
				return true;
			}
			
			if ((skill.getSkillType() == SkillType.REELING || skill.getSkillType() == SkillType.PUMPING) && !activeChar.isFishing() && (activeChar.getActiveWeaponItem() != null && activeChar.getActiveWeaponItem().getItemType() == L2WeaponType.ROD))
			{
				if (skill.getSkillType() == SkillType.PUMPING)
				{
					// Pumping skill is available only while fishing
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CAN_USE_PUMPING_ONLY_WHILE_FISHING));
				}
				else if (skill.getSkillType() == SkillType.REELING)
				{
					// Reeling skill is available only while fishing
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CAN_USE_REELING_ONLY_WHILE_FISHING));
				}
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addString(skill.getName());
				activeChar.sendPacket(sm);
				return true;
			}
			
			if (activeChar.isHero() && HeroSkillTable.isHeroSkill(skill.getId()) && activeChar.isInOlympiadMode() && activeChar.isOlympiadStart())
			{
				activeChar.sendMessage("You can't use Hero skills during Olympiad match.");
				return true;
			}
		}
		
		return isSkillDisabled(skill.getReuseHashCode());
	}
	
	public synchronized final void resetDisabledSkills()
	{
		if (_disabledSkills != null)
		{
			_disabledSkills.clear();
		}
	}
	
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	/**
	 * Enable all skills (set _allSkillsDisabled to False).<BR>
	 * <BR>
	 */
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	public void callSkill(L2Skill skill, L2Object[] targets)
	{
		try
		{
			if (skill.isToggle() && getFirstEffect(skill.getId()) != null)
			{
				return;
			}
			
			if (targets == null || targets.length == 0)
			{
				getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
				return;
			}
			
			for (L2Object trg : targets)
			{
				// Check if over-hit is possible
				if (skill.isOverhit())
				{
					if (trg instanceof L2Attackable)
					{
						((L2Attackable) trg).overhitEnabled(true);
					}
				}
				
				if (trg instanceof L2Character)
				{
					L2Character target = (L2Character) trg;
					
					if (ChanceSkillList.canTriggerByCast(this, target, skill))
					{
						if (getActiveWeaponItem() != null && !target.isDead())
						{
							if (this instanceof L2PcInstance && getActiveWeaponItem().getSkillEffects(this, target, skill))
							{
								sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED).addSkillName(skill));
							}
						}
						
						// Maybe launch chance skills on us
						if (_chanceSkills != null)
						{
							_chanceSkills.onSkillHit(target, false, skill.isMagic(), skill.isOffensive());
						}
						
						// Maybe launch chance skills on target
						if (target.getChanceSkills() != null)
						{
							target.getChanceSkills().onSkillHit(this, true, skill.isMagic(), skill.isOffensive());
						}
					}
					
					if (Config.ALLOW_RAID_BOSS_PETRIFIED && target.isRaid() && this instanceof L2PlayableInstance)
					{
						if (getLevel() > (target.getLevel() + 8))
						{
							if (skill.isMagic())
							{
								L2Skill curseSkill = SkillTable.getInstance().getInfo(4215, 1);
								if (curseSkill != null)
								{
									abortAttack();
									abortCast();
									getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
									curseSkill.getEffects(target, this, false, false, false);
									
									if (this instanceof L2Summon)
									{
										L2Summon pet = ((L2Summon) this);
										if (pet.getOwner() != null)
										{
											pet.getOwner().abortAttack();
											pet.getOwner().abortCast();
											pet.getOwner().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
											curseSkill.getEffects(target, pet.getOwner(), false, false, false);
										}
									}
								}
							}
							else
							{
								L2Skill curseSkill = SkillTable.getInstance().getInfo(4515, 1);
								if (curseSkill != null)
								{
									curseSkill.getEffects(target, this);
								}
							}
							sendDamageMessage(target, 0, false, false, false);
							return;
						}
					}
				}
			}
			
			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
			if (handler != null)
			{
				handler.useSkill(this, skill, targets);
			}
			else
			{
				skill.useSkill(this, targets);
			}
			
			for (L2Object trg : targets)
			{
				if (trg instanceof L2Character)
				{
					L2Character target = (L2Character) trg;
					L2PcInstance activeChar = null;
					
					if (this instanceof L2PcInstance)
					{
						activeChar = (L2PcInstance) this;
					}
					else if (this instanceof L2Summon)
					{
						activeChar = ((L2Summon) this).getOwner();
					}
					
					if (activeChar != null)
					{
						if (skill.isOffensive())
						{
							if (target instanceof L2PcInstance || target instanceof L2Summon)
							{
								if (skill.getSkillType() != L2Skill.SkillType.SIGNET && skill.getSkillType() != L2Skill.SkillType.SIGNET_CASTTIME)
								{
									target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
									activeChar.updatePvPStatus(target);
								}
							}
							else if (target instanceof L2Attackable)
							{
								switch (skill.getId())
								{
									case 51: // Lure
										break;
									default:
										target.addAttackerToAttackByList(this);
										
										if (target.hasAI())
										{
											switch (skill.getSkillType())
											{
												case AGGREDUCE:
												case AGGREDUCE_CHAR:
												case AGGREMOVE:
													break;
												default:
													target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
											}
										}
								}
							}
						}
						else
						{
							if (target instanceof L2PcInstance)
							{
								// Casting non offensive skill on player with pvp flag set or with karma
								if (!target.equals(this) && (((L2PcInstance) target).getPvpFlag() > 0 || ((L2PcInstance) target).getKarma() > 0))
								{
									activeChar.updatePvPStatus();
								}
							}
							else if (target instanceof L2Attackable && !(skill.getSkillType() == L2Skill.SkillType.SUMMON) && !(skill.getSkillType() == L2Skill.SkillType.BEAST_FEED) && !(skill.getSkillType() == L2Skill.SkillType.UNLOCK)
								&& !(skill.getSkillType() == L2Skill.SkillType.DELUXE_KEY_UNLOCK))
							{
								activeChar.updatePvPStatus();
							}
						}
					}
				}
			}
			
			// if the skill is a potion, must delete the potion item
			if (skill.isPotion() && this instanceof L2PlayableInstance)
			{
				Potions.delete_Potion_Item((L2PlayableInstance) this, skill.getId(), skill.getLevel());
			}
			
			if (this instanceof L2PcInstance || this instanceof L2Summon)
			{
				L2PcInstance caster = this instanceof L2PcInstance ? (L2PcInstance) this : ((L2Summon) this).getOwner();
				boolean isPet = this instanceof L2PcInstance ? false : true;
				for (L2Object target : targets)
				{
					if (target instanceof L2NpcInstance)
					{
						L2NpcInstance npc = (L2NpcInstance) target;
						
						for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_USE))
						{
							quest.notifySkillUse(npc, caster, skill);
						}
						
						for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE))
						{
							quest.notifySkillSee(npc, caster, skill, targets, isPet);
						}
					}
				}
				
				if (skill.getAggroPoints() > 0)
				{
					for (L2Object spMob : caster.getKnownList().getKnownObjects().values())
					{
						if (spMob instanceof L2NpcInstance)
						{
							L2NpcInstance npcMob = (L2NpcInstance) spMob;
							if (npcMob.isInsideRadius(caster, 1000, true, true) && npcMob.hasAI() && npcMob.getAI().getIntention() == AI_INTENTION_ATTACK)
							{
								L2Object npcTarget = npcMob.getTarget();
								for (L2Object target : targets)
								{
									if (npcTarget == target || npcMob == target)
									{
										npcMob.seeSpell(caster, target, skill);
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOG.warn("callSkill:", e);
		}
	}
	
	public void seeSpell(L2PcInstance caster, L2Object target, L2Skill skill)
	{
		if (this instanceof L2Attackable)
		{
			((L2Attackable) this).addDamageHate(caster, 0, -skill.getAggroPoints());
		}
	}
	
	public boolean isFacing(L2Object target, int maxAngle)
	{
		if (target == null)
		{
			return false;
		}
		
		double maxAngleDiff = maxAngle / 2;
		double angleTarget = Util.calculateAngleFrom(this, target);
		double angleChar = Util.convertHeadingToDegree(getHeading());
		double angleDiff = angleChar - angleTarget;
		
		if (angleDiff <= -360 + maxAngleDiff)
		{
			angleDiff += 360;
		}
		
		if (angleDiff >= 360 - maxAngleDiff)
		{
			angleDiff -= 360;
		}
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isBehindTarget()
	{
		L2Object target = getTarget();
		if (target instanceof L2Character)
		{
			return isBehind((L2Character) target);
		}
		
		return false;
	}
	
	public boolean isBehind(L2Character target)
	{
		if (target == null)
		{
			return false;
		}
		
		final double maxAngleDiff = 60;
		
		double angleChar = Util.calculateAngleFrom(this, target);
		double angleTarget = Util.convertHeadingToDegree(target.getHeading());
		double angleDiff = angleChar - angleTarget;
		
		if (angleDiff <= -360 + maxAngleDiff)
		{
			angleDiff += 360;
		}
		
		if (angleDiff >= 360 - maxAngleDiff)
		{
			angleDiff -= 360;
		}
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isFrontTarget()
	{
		L2Object target = getTarget();
		if (target instanceof L2Character)
		{
			return isFront((L2Character) target);
		}
		return false;
	}
	
	public boolean isFront(L2Character target)
	{
		if (target == null)
		{
			return false;
		}
		
		final double maxAngleDiff = 60;
		
		double angleTarget = Util.calculateAngleFrom(target, this);
		double angleChar = Util.convertHeadingToDegree(target.getHeading());
		double angleDiff = angleChar - angleTarget;
		
		if (angleDiff <= -360 + maxAngleDiff)
		{
			angleDiff += 360;
		}
		
		if (angleDiff >= 360 - maxAngleDiff)
		{
			angleDiff -= 360;
		}
		
		return Math.abs(angleDiff) <= maxAngleDiff;
	}
	
	public boolean isSideTarget()
	{
		return isSide(getTarget());
	}
	
	public boolean isSide(L2Object target)
	{
		if (target == null)
		{
			return false;
		}
		
		if (target instanceof L2Character)
		{
			if (isBehindTarget() || isFrontTarget())
			{
				return false;
			}
		}
		return true;
	}
	
	public double getLevelMod()
	{
		return 1;
	}
	
	public final void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}
	
	public final void setSkillCastEndTime(int newSkillCastEndTime)
	{
		_castEndTime = newSkillCastEndTime;
		_castInterruptTime = newSkillCastEndTime - 12;
	}
	
	public final int getSkillCastEndTime()
	{
		return _castEndTime;
	}
	
	private Future<?> _PvPRegTask;
	private Future<?> _startSkillCheckTask;
	private long _pvpFlagLasts;
	
	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}
	
	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}
	
	public void stopSkillCheckTask()
	{
		if (_startSkillCheckTask != null)
		{
			_startSkillCheckTask.cancel(true);
		}
	}
	
	public void stopSkillCheck()
	{
		stopSkillCheckTask();
		
		_startSkillCheckTask = null;
	}
	
	public void startPvPFlag()
	{
		updatePvPFlag(1);
		
		if (_PvPRegTask == null)
		{
			_PvPRegTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PvPFlag(), 1000, 1000);
		}
	}
	
	public void stopPvpRegTask()
	{
		if (_PvPRegTask != null)
		{
			_PvPRegTask.cancel(true);
		}
	}
	
	public void stopPvPFlag()
	{
		stopPvpRegTask();
		updatePvPFlag(0);
		_PvPRegTask = null;
	}
	
	/**
	 * Update pvp flag.
	 * @param value the value
	 */
	public void updatePvPFlag(int value)
	{
	}
	
	/**
	 * Return a Random Damage in function of the weapon.<BR>
	 * <BR>
	 * @param target the target
	 * @return the random damage
	 */
	public final int getRandomDamage(L2Character target)
	{
		L2Weapon weaponItem = getActiveWeaponItem();
		
		if (weaponItem == null)
		{
			return 5 + (int) Math.sqrt(getLevel());
		}
		
		return weaponItem.getRandomDamage();
	}
	
	@Override
	public String toString()
	{
		return "mob " + getObjectId();
	}
	
	/**
	 * Gets the attack end time.
	 * @return the attack end time
	 */
	public final long getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	public abstract int getLevel();
	
	public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill)
	{
		return getStat().calcStat(stat, init, target, skill);
	}
	
	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}
	
	public final float getAttackSpeedMultiplier()
	{
		return getStat().getAttackSpeedMultiplier();
	}
	
	public int getCON()
	{
		return getStat().getCON();
	}
	
	public int getDEX()
	{
		return getStat().getDEX();
	}
	
	public final double getCriticalDmg(L2Character target, double init)
	{
		return getStat().getCriticalDmg(target, init);
	}
	
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}
	
	public int getEvasionRate(L2Character target)
	{
		return getStat().getEvasionRate(target);
	}
	
	public int getINT()
	{
		return getStat().getINT();
	}
	
	public final int getMagicalAttackRange(L2Skill skill)
	{
		return getStat().getMagicalAttackRange(skill);
	}
	
	public final int getMaxCp()
	{
		return getStat().getMaxCp();
	}
	
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}
	
	/**
	 * Gets the m atk spd.
	 * @return the m atk spd
	 */
	public int getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}
	
	/**
	 * Gets the max mp.
	 * @return the max mp
	 */
	public int getMaxMp()
	{
		return getStat().getMaxMp();
	}
	
	/**
	 * Gets the max hp.
	 * @return the max hp
	 */
	public int getMaxHp()
	{
		return getStat().getMaxHp();
	}
	
	/**
	 * Gets the m critical hit.
	 * @param target the target
	 * @param skill the skill
	 * @return the m critical hit
	 */
	public final int getMCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getMCriticalHit(target, skill);
	}
	
	/**
	 * Gets the m def.
	 * @param target the target
	 * @param skill the skill
	 * @return the m def
	 */
	public int getMDef(L2Character target, L2Skill skill)
	{
		return getStat().getMDef(target, skill);
	}
	
	/**
	 * Gets the mEN.
	 * @return the mEN
	 */
	public int getMEN()
	{
		return getStat().getMEN();
	}
	
	/**
	 * Gets the m reuse rate.
	 * @param skill the skill
	 * @return the m reuse rate
	 */
	public double getMReuseRate(L2Skill skill)
	{
		return getStat().getMReuseRate(skill);
	}
	
	/**
	 * Gets the movement speed multiplier.
	 * @return the movement speed multiplier
	 */
	public float getMovementSpeedMultiplier()
	{
		return getStat().getMovementSpeedMultiplier();
	}
	
	/**
	 * Gets the p atk.
	 * @param target the target
	 * @return the p atk
	 */
	public int getPAtk(L2Character target)
	{
		return getStat().getPAtk(target);
	}
	
	/**
	 * Gets the p atk animals.
	 * @param target the target
	 * @return the p atk animals
	 */
	public double getPAtkAnimals(L2Character target)
	{
		return getStat().getPAtkAnimals(target);
	}
	
	/**
	 * Gets the p atk dragons.
	 * @param target the target
	 * @return the p atk dragons
	 */
	public double getPAtkDragons(L2Character target)
	{
		return getStat().getPAtkDragons(target);
	}
	
	/**
	 * Gets the p atk angels.
	 * @param target the target
	 * @return the p atk angels
	 */
	public double getPAtkAngels(L2Character target)
	{
		return getStat().getPAtkAngels(target);
	}
	
	/**
	 * Gets the p atk insects.
	 * @param target the target
	 * @return the p atk insects
	 */
	public double getPAtkInsects(L2Character target)
	{
		return getStat().getPAtkInsects(target);
	}
	
	/**
	 * Gets the p atk monsters.
	 * @param target the target
	 * @return the p atk monsters
	 */
	public double getPAtkMonsters(L2Character target)
	{
		return getStat().getPAtkMonsters(target);
	}
	
	/**
	 * Gets the p atk plants.
	 * @param target the target
	 * @return the p atk plants
	 */
	public double getPAtkPlants(L2Character target)
	{
		return getStat().getPAtkPlants(target);
	}
	
	/**
	 * Gets the p atk spd.
	 * @return the p atk spd
	 */
	public int getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}
	
	/**
	 * Gets the p atk undead.
	 * @param target the target
	 * @return the p atk undead
	 */
	public double getPAtkUndead(L2Character target)
	{
		return getStat().getPAtkUndead(target);
	}
	
	/**
	 * Gets the p def undead.
	 * @param target the target
	 * @return the p def undead
	 */
	public double getPDefUndead(L2Character target)
	{
		return getStat().getPDefUndead(target);
	}
	
	/**
	 * Gets the p def plants.
	 * @param target the target
	 * @return the p def plants
	 */
	public double getPDefPlants(L2Character target)
	{
		return getStat().getPDefPlants(target);
	}
	
	/**
	 * Gets the p def insects.
	 * @param target the target
	 * @return the p def insects
	 */
	public double getPDefInsects(L2Character target)
	{
		return getStat().getPDefInsects(target);
	}
	
	/**
	 * Gets the p def animals.
	 * @param target the target
	 * @return the p def animals
	 */
	public double getPDefAnimals(L2Character target)
	{
		return getStat().getPDefAnimals(target);
	}
	
	/**
	 * Gets the p def monsters.
	 * @param target the target
	 * @return the p def monsters
	 */
	public double getPDefMonsters(L2Character target)
	{
		return getStat().getPDefMonsters(target);
	}
	
	/**
	 * Gets the p def dragons.
	 * @param target the target
	 * @return the p def dragons
	 */
	public double getPDefDragons(L2Character target)
	{
		return getStat().getPDefDragons(target);
	}
	
	/**
	 * Gets the p def angels.
	 * @param target the target
	 * @return the p def angels
	 */
	public double getPDefAngels(L2Character target)
	{
		return getStat().getPDefAngels(target);
	}
	
	/**
	 * Gets the p def.
	 * @param target the target
	 * @return the p def
	 */
	public int getPDef(L2Character target)
	{
		return getStat().getPDef(target);
	}
	
	/**
	 * Gets the p atk giants.
	 * @param target the target
	 * @return the p atk giants
	 */
	public double getPAtkGiants(L2Character target)
	{
		return getStat().getPAtkGiants(target);
	}
	
	/**
	 * Gets the p atk magic creatures.
	 * @param target the target
	 * @return the p atk magic creatures
	 */
	public double getPAtkMagicCreatures(L2Character target)
	{
		return getStat().getPAtkMagicCreatures(target);
	}
	
	/**
	 * Gets the p def giants.
	 * @param target the target
	 * @return the p def giants
	 */
	public double getPDefGiants(L2Character target)
	{
		return getStat().getPDefGiants(target);
	}
	
	/**
	 * Gets the p def magic creatures.
	 * @param target the target
	 * @return the p def magic creatures
	 */
	public double getPDefMagicCreatures(L2Character target)
	{
		return getStat().getPDefMagicCreatures(target);
	}
	
	/**
	 * Gets the physical attack range.
	 * @return the physical attack range
	 */
	public final int getPhysicalAttackRange()
	{
		return getStat().getPhysicalAttackRange();
	}
	
	/**
	 * Gets the run speed.
	 * @return the run speed
	 */
	public int getRunSpeed()
	{
		return getStat().getRunSpeed();
	}
	
	/**
	 * Gets the shld def.
	 * @return the shld def
	 */
	public final int getShldDef()
	{
		return getStat().getShldDef();
	}
	
	/**
	 * Gets the sTR.
	 * @return the sTR
	 */
	public int getSTR()
	{
		return getStat().getSTR();
	}
	
	/**
	 * Gets the walk speed.
	 * @return the walk speed
	 */
	public final int getWalkSpeed()
	{
		return getStat().getWalkSpeed();
	}
	
	/**
	 * Gets the wIT.
	 * @return the wIT
	 */
	public int getWIT()
	{
		return getStat().getWIT();
	}
	
	public void addStatusListener(L2Character object)
	{
		getStatus().addStatusListener(object);
	}
	
	public void reduceCurrentHp(double i, L2Character attacker)
	{
		reduceCurrentHp(i, attacker, true);
	}
	
	public void reduceCurrentHp(double i, L2Character attacker, boolean awake)
	{
		if (this instanceof L2NpcInstance)
		{
			if (Config.INVUL_NPC_LIST.contains(Integer.valueOf(((L2NpcInstance) this).getNpcId())))
			{
				return;
			}
		}
		
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion() && Config.L2JMOD_CHAMPION_HP != 0)
		{
			getStatus().reduceHp(i / Config.L2JMOD_CHAMPION_HP, attacker, awake);
		}
		else if (is_advanceFlag())
		{
			getStatus().reduceHp(i / _advanceMultiplier, attacker, awake);
		}
		else
		{
			getStatus().reduceHp(i, attacker, awake);
		}
	}
	
	private long _nextReducingHPByOverTime = -1;
	
	public void reduceCurrentHpByDamOverTime(double i, L2Character attacker, boolean awake, int period)
	{
		if (_nextReducingHPByOverTime > System.currentTimeMillis())
		{
			return;
		}
		
		_nextReducingHPByOverTime = System.currentTimeMillis() + (period * 1000);
		reduceCurrentHp(i, attacker, awake);
		
	}
	
	private long _nextReducingMPByOverTime = -1;
	
	public void reduceCurrentMpByDamOverTime(double i, int period)
	{
		if (_nextReducingMPByOverTime > System.currentTimeMillis())
		{
			return;
		}
		
		_nextReducingMPByOverTime = System.currentTimeMillis() + (period * 1000);
		reduceCurrentMp(i);
		
	}
	
	public void reduceCurrentMp(double i)
	{
		getStatus().reduceMp(i);
	}
	
	@Override
	public void removeStatusListener(L2Character object)
	{
		getStatus().removeStatusListener(object);
	}
	
	protected void stopHpMpRegeneration()
	{
		getStatus().stopHpMpRegeneration();
	}
	
	public final double getCurrentCp()
	{
		return getStatus().getCurrentCp();
	}
	
	/**
	 * Sets the current cp.
	 * @param newCp the new current cp
	 */
	public final void setCurrentCp(Double newCp)
	{
		setCurrentCp((double) newCp);
	}
	
	/**
	 * Sets the current cp.
	 * @param newCp the new current cp
	 */
	public final void setCurrentCp(double newCp)
	{
		getStatus().setCurrentCp(newCp);
	}
	
	/**
	 * Gets the current hp.
	 * @return the current hp
	 */
	public final double getCurrentHp()
	{
		return getStatus().getCurrentHp();
	}
	
	/**
	 * Sets the current hp.
	 * @param newHp the new current hp
	 */
	public final void setCurrentHp(double newHp)
	{
		getStatus().setCurrentHp(newHp);
	}
	
	/**
	 * Sets the current hp direct.
	 * @param newHp the new current hp direct
	 */
	public final void setCurrentHpDirect(double newHp)
	{
		getStatus().setCurrentHpDirect(newHp);
	}
	
	/**
	 * Sets the current cp direct.
	 * @param newCp the new current cp direct
	 */
	public final void setCurrentCpDirect(double newCp)
	{
		getStatus().setCurrentCpDirect(newCp);
	}
	
	/**
	 * Sets the current mp direct.
	 * @param newMp the new current mp direct
	 */
	public final void setCurrentMpDirect(double newMp)
	{
		getStatus().setCurrentMpDirect(newMp);
	}
	
	/**
	 * Sets the current hp mp.
	 * @param newHp the new hp
	 * @param newMp the new mp
	 */
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		getStatus().setCurrentHpMp(newHp, newMp);
	}
	
	/**
	 * Gets the current mp.
	 * @return the current mp
	 */
	public final double getCurrentMp()
	{
		return getStatus().getCurrentMp();
	}
	
	/**
	 * Sets the current mp.
	 * @param newMp the new current mp
	 */
	public final void setCurrentMp(Double newMp)
	{
		setCurrentMp((double) newMp);
	}
	
	/**
	 * Sets the current mp.
	 * @param newMp the new current mp
	 */
	public final void setCurrentMp(double newMp)
	{
		getStatus().setCurrentMp(newMp);
	}
	
	/**
	 * Sets the ai class.
	 * @param aiClass the new ai class
	 */
	public void setAiClass(String aiClass)
	{
		_aiClass = aiClass;
	}
	
	/**
	 * Gets the ai class.
	 * @return the ai class
	 */
	public String getAiClass()
	{
		return _aiClass;
	}
	
	/**
	 * Sets the champion.
	 * @param champ the new champion
	 */
	public void setChampion(boolean champ)
	{
		_champion = champ;
	}
	
	/**
	 * Checks if is champion.
	 * @return true, if is champion
	 */
	public boolean isChampion()
	{
		return _champion;
	}
	
	/**
	 * Gets the last heal amount.
	 * @return the last heal amount
	 */
	public int getLastHealAmount()
	{
		return _lastHealAmount;
	}
	
	/*
	 * public void setLastBuffer(L2Character buffer) { _lastBuffer = buffer; }
	 */
	
	/**
	 * Sets the last heal amount.
	 * @param hp the new last heal amount
	 */
	public void setLastHealAmount(int hp)
	{
		_lastHealAmount = hp;
	}
	
	/**
	 * @return the _advanceFlag
	 */
	public boolean is_advanceFlag()
	{
		return _advanceFlag;
	}
	
	/**
	 * @param advanceFlag
	 */
	public void set_advanceFlag(boolean advanceFlag)
	{
		_advanceFlag = advanceFlag;
	}
	
	/**
	 * @param advanceMultiplier
	 */
	public void set_advanceMultiplier(int advanceMultiplier)
	{
		_advanceMultiplier = advanceMultiplier;
	}
	
	/**
	 * Check if character reflected skill.
	 * @param skill the skill
	 * @return true, if successful
	 */
	public boolean reflectSkill(L2Skill skill)
	{
		double reflect = calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
		
		if (Rnd.get(100) < reflect)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Vengeance skill.
	 * @param skill the skill
	 * @return true, if successful
	 */
	public boolean vengeanceSkill(L2Skill skill)
	{
		if (!skill.isMagic() && skill.getCastRange() <= 40)
		{
			final double venganceChance = calcStat(Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0, null, skill);
			if (venganceChance > Rnd.get(100))
			{
				return true;
			}
		}
		return false;
	}
	
	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
	}
	
	/**
	 * Gets the force buff.
	 * @return the force buff
	 */
	public ForceBuff getForceBuff()
	{
		return _forceBuff;
	}
	
	/**
	 * Sets the force buff.
	 * @param fb the new force buff
	 */
	public void setForceBuff(ForceBuff fb)
	{
		_forceBuff = fb;
	}
	
	public void setVotedIp(int VS)
	{
		_VotedSystem = VS;
	}
	
	public int getVotedIp()
	{
		return _VotedSystem;
	}
	
	/**
	 * Checks if is fear immune.
	 * @return true, if is fear immune
	 */
	public boolean isFearImmune()
	{
		return false;
	}
	
	/**
	 * Restore hpmp.
	 */
	public void restoreHPMP()
	{
		getStatus().setCurrentHpMp(getMaxHp(), getMaxMp());
	}
	
	/**
	 * Restore cp.
	 */
	public void restoreCP()
	{
		getStatus().setCurrentCp(getMaxCp());
	}
	
	/**
	 * Block.
	 */
	public void block()
	{
		_blocked = true;
	}
	
	/**
	 * Unblock.
	 */
	public void unblock()
	{
		_blocked = false;
	}
	
	/**
	 * Checks if is blocked.
	 * @return true, if is blocked
	 */
	public boolean isBlocked()
	{
		return _blocked;
	}
	
	/**
	 * Checks if is meditated.
	 * @return true, if is meditated
	 */
	public boolean isMeditated()
	{
		return _meditated;
	}
	
	/**
	 * Sets the meditated.
	 * @param meditated the new meditated
	 */
	public void setMeditated(boolean meditated)
	{
		_meditated = meditated;
	}
	
	/**
	 * Update attack stance.
	 */
	public void updateAttackStance()
	{
		attackStance = System.currentTimeMillis();
	}
	
	/**
	 * Gets the attack stance.
	 * @return the attack stance
	 */
	public long getAttackStance()
	{
		return attackStance;
	}
	
	/** The _petrified. */
	private boolean _petrified = false;
	
	/**
	 * Checks if is petrified.
	 * @return the petrified
	 */
	public boolean isPetrified()
	{
		return _petrified;
	}
	
	/**
	 * Sets the petrified.
	 * @param petrified the petrified to set
	 */
	public void setPetrified(boolean petrified)
	{
		if (petrified)
		{
			setIsParalyzed(petrified);
			setIsInvul(petrified);
			_petrified = petrified;
		}
		else
		{
			_petrified = petrified;
			setIsParalyzed(petrified);
			setIsInvul(petrified);
		}
	}
	
	/**
	 * Check bss.
	 * @return true, if successful
	 */
	public boolean checkBss()
	{
		
		boolean bss = false;
		
		L2ItemInstance weaponInst = this.getActiveWeaponInstance();
		
		if (weaponInst != null)
		{
			if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				bss = true;
				// ponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
			
		}
		// If there is no weapon equipped, check for an active summon.
		else if (this instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) this;
			
			if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				bss = true;
				// activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
			}
			
		}
		
		return bss;
	}
	
	/**
	 * Removes the bss.
	 */
	synchronized public void removeBss()
	{
		L2ItemInstance weaponInst = this.getActiveWeaponInstance();
		
		if (weaponInst != null)
		{
			if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
		}
		// If there is no weapon equipped, check for an active summon.
		else if (this instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) this;
			
			if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
			{
				activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
			}
			
		}
		reloadShots(true);
	}
	
	/**
	 * Check sps.
	 * @return true, if successful
	 */
	public boolean checkSps()
	{
		
		boolean ss = false;
		
		L2ItemInstance weaponInst = this.getActiveWeaponInstance();
		
		if (weaponInst != null)
		{
			if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				ss = true;
				// weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
		}
		// If there is no weapon equipped, check for an active summon.
		else if (this instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) this;
			
			if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				ss = true;
				// activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
			}
		}
		
		return ss;
		
	}
	
	/**
	 * Removes the sps.
	 */
	synchronized public void removeSps()
	{
		
		final L2ItemInstance weaponInst = this.getActiveWeaponInstance();
		
		if (weaponInst != null)
		{
			if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
		}
		// If there is no weapon equipped, check for an active summon.
		else if (this instanceof L2Summon)
		{
			final L2Summon activeSummon = (L2Summon) this;
			
			if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
			}
		}
		
		reloadShots(true);
	}
	
	/**
	 * Check ss.
	 * @return true, if successful
	 */
	public boolean checkSs()
	{
		boolean ss = false;
		
		L2ItemInstance weaponInst = this.getActiveWeaponInstance();
		
		if (weaponInst != null)
		{
			if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
			{
				ss = true;
			}
		}
		// If there is no weapon equipped, check for an active summon.
		else if (this instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) this;
			
			if (activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT)
			{
				ss = true;
			}
		}
		
		return ss;
		
	}
	
	/**
	 * Removes the ss.
	 */
	public void removeSs()
	{
		
		L2ItemInstance weaponInst = this.getActiveWeaponInstance();
		
		if (weaponInst != null)
		{
			if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
			{
				weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
			}
		}
		// If there is no weapon equipped, check for an active summon.
		else if (this instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) this;
			
			if (activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT)
			{
				activeSummon.setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
			}
		}
		reloadShots(false);
	}
	
	/**
	 * Return a multiplier based on weapon random damage<BR>
	 * <BR>
	 * .
	 * @return the random damage multiplier
	 */
	public final double getRandomDamageMultiplier()
	{
		L2Weapon activeWeapon = getActiveWeaponItem();
		int random;
		
		if (activeWeapon != null)
		{
			random = activeWeapon.getRandomDamage();
		}
		else
		{
			random = 5 + (int) Math.sqrt(getLevel());
		}
		
		return (1 + ((double) Rnd.get(0 - random, random) / 100));
	}
	
	/**
	 * Sets the checks if is buff protected.
	 * @param value the new checks if is buff protected
	 */
	public final void setIsBuffProtected(boolean value)
	{
		_isBuffProtected = value;
	}
	
	public boolean isBuffProtected()
	{
		return _isBuffProtected;
	}
	
	public Map<Integer, L2Skill> get_triggeredSkills()
	{
		return _triggeredSkills;
	}
	
	public void setTargetTrasformedNpc(L2Attackable trasformedNpc)
	{
		if (trasformedNpc == null)
		{
			return;
		}
		
		this.setTarget(trasformedNpc);
		
		MyTargetSelected my = new MyTargetSelected(trasformedNpc.getObjectId(), this.getLevel() - trasformedNpc.getLevel());
		sendPacket(my);
		
		StatusUpdate su = new StatusUpdate(trasformedNpc.getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) trasformedNpc.getCurrentHp());
		su.addAttribute(StatusUpdate.MAX_HP, trasformedNpc.getMaxHp());
		sendPacket(su);
	}
	
	public boolean isUnkillable()
	{
		return _isUnkillable;
	}
	
	public void setIsUnkillable(boolean value)
	{
		_isUnkillable = value;
	}
	
	public boolean isAttackDisabled()
	{
		return _isAttackDisabled;
	}
	
	public void setIsAttackDisabled(boolean value)
	{
		_isAttackDisabled = value;
	}
	
	/*
	 * AI not. Task
	 */
	static class notifyAiTaskDelayed implements Runnable
	{
		
		CtrlEvent event;
		Object object;
		L2Object tgt;
		
		notifyAiTaskDelayed(CtrlEvent evt, Object obj, L2Object target)
		{
			event = evt;
			object = obj;
			tgt = target;
		}
		
		@Override
		public void run()
		{
			((L2Character) tgt).getAI().notifyEvent(event, object);
		}
		
	}
	
	class FlyToLocationTask implements Runnable
	{
		private final L2Object _tgt;
		private final L2Character _actor;
		private final L2Skill _skill;
		
		public FlyToLocationTask(L2Character actor, L2Object target, L2Skill skill)
		{
			_actor = actor;
			_tgt = target;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			try
			{
				FlyType _flyType;
				
				_flyType = FlyType.valueOf(_skill.getFlyType());
				
				broadcastPacket(new FlyToLocation(_actor, _tgt, _flyType));
				setXYZ(_tgt.getX(), _tgt.getY(), _tgt.getZ());
			}
			catch (Exception e)
			{
				LOG.warn("Failed executing FlyToLocationTask.", e);
			}
		}
	}
	
	synchronized public void reloadShots(final boolean isMagic)
	{
		if (this instanceof L2PcInstance)
		{
			((L2PcInstance) this).rechargeAutoSoulShot(!isMagic, isMagic, false);
		}
		else if (this instanceof L2Summon)
		{
			((L2Summon) this).getOwner().rechargeAutoSoulShot(!isMagic, isMagic, true);
		}
	}
	
	public boolean isRaidMinion()
	{
		return false;
	}
	
	public boolean isBossInstance()
	{
		return false;
	}
	
	public boolean isInArena()
	{
		return false;
	}
	
	public final int getClientX()
	{
		return _clientX;
	}
	
	public final int getClientY()
	{
		return _clientY;
	}
	
	public final int getClientZ()
	{
		return _clientZ;
	}
	
	public final int getClientHeading()
	{
		return _clientHeading;
	}
	
	public final void setClientX(int val)
	{
		_clientX = val;
	}
	
	public final void setClientY(int val)
	{
		_clientY = val;
	}
	
	public final void setClientZ(int val)
	{
		_clientZ = val;
	}
	
	/**
	 * Sets the client heading.
	 * @param val the new client heading
	 */
	public final void setClientHeading(int val)
	{
		_clientHeading = val;
	}
	
	public boolean isClickedArrowButton()
	{
		return _isClickedArrowButton;
	}
	
	public void setClickedArrowButton(boolean value)
	{
		
		_isClickedArrowButton = value;
	}
}