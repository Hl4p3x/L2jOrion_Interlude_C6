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
package l2jorion.game.model.actor.instance;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import java.text.DateFormat;
import java.util.List;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.MobGroupTable;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.datatables.sql.HelperBuffTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CustomNpcInstanceManager;
import l2jorion.game.managers.DimensionalRiftManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.managers.TownManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2NpcAIData;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.knownlist.NpcKnownList;
import l2jorion.game.model.actor.stat.NpcStat;
import l2jorion.game.model.actor.status.NpcStatus;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.L2Event;
import l2jorion.game.model.entity.event.Lottery;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.entity.olympiad.Olympiad;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.multisell.L2Multisell;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.quest.Quest.QuestEventType;
import l2jorion.game.model.scripts.L2RBManager;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.type.L2TownZone;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExShowVariationCancelWindow;
import l2jorion.game.network.serverpackets.ExShowVariationMakeWindow;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.NpcInfo;
import l2jorion.game.network.serverpackets.NpcSay;
import l2jorion.game.network.serverpackets.RadarControl;
import l2jorion.game.network.serverpackets.ServerObjectInfo;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.powerpack.other.Market;
import l2jorion.game.skills.Stats;
import l2jorion.game.taskmanager.DecayTaskManager;
import l2jorion.game.templates.L2HelperBuff;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.templates.L2NpcTemplate.AIType;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;

import org.omg.PortableServer.POAManagerPackage.State;

public class L2NpcInstance extends L2Character
{
	public static final int INTERACTION_DISTANCE = 150;
	
	private L2CustomNpcInstance _customNpcInstance;
	
	private L2Spawn _spawn;
	
	private boolean _isBusy = false;
	
	private String _busyMessage = "";
	
	volatile boolean _isDecayed = false;
	
	private boolean _isSpoil = false;
	
	private int _castleIndex = -2;
	
	private int _fortIndex = -2;
	
	public boolean isEventMob = false, _isEventMobTvT = false, _isEventVIPNPC = false, _isEventVIPNPCEnd = false, _isEventMobDM = false, _isEventMobCTF = false, _isCTF_throneSpawn = false, _isCTF_Flag = false;
	
	private boolean _isInTown = false;
	
	public String _CTF_FlagTeamName;
	
	private int _isSpoiledBy = 0;
	
	protected RandomAnimationTask _rAniTask = null;
	
	private int _currentLHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentRHandId; // normally this shouldn't change from the template, but there exist exceptions
	
	private int _currentCollisionHeight; // used for npc grow effect skills
	private int _currentCollisionRadius; // used for npc grow effect skills
	
	public boolean _soulshotcharged = false;
	public boolean _spiritshotcharged = false;
	private int _soulshotamount = 0;
	private int _spiritshotamount = 0;
	public boolean _ssrecharged = true;
	public boolean _spsrecharged = true;
	
	private final L2NpcAIData _staticAIData = getTemplate().getAIDataStatic();
	
	// AI Recall
	public final L2NpcAIData getAIData()
	{
		return _staticAIData;
	}
	
	public int getSoulShot()
	{
		return _staticAIData.getSoulShot();
	}
	
	public int getSpiritShot()
	{
		return _staticAIData.getSpiritShot();
	}
	
	public int getSoulShotChance()
	{
		return _staticAIData.getSoulShotChance();
	}
	
	public int getSpiritShotChance()
	{
		return _staticAIData.getSpiritShotChance();
	}
	
	public boolean useSoulShot()
	{
		if (_soulshotcharged)
		{
			return true;
		}
		
		if (_ssrecharged)
		{
			_soulshotamount = getSoulShot();
			_ssrecharged = false;
		}
		else if (_soulshotamount > 0)
		{
			if (Rnd.get(100) <= getSoulShotChance())
			{
				_soulshotamount = _soulshotamount - 1;
				Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUser(this, this, 2154, 1, 0, 0), 360000);
				_soulshotcharged = true;
			}
		}
		else
		{
			return false;
		}
		
		return _soulshotcharged;
	}
	
	public boolean useSpiritShot()
	{
		if (_spiritshotcharged)
		{
			return true;
		}
		
		if (_spsrecharged)
		{
			_spiritshotamount = getSpiritShot();
			_spsrecharged = false;
		}
		else if (_spiritshotamount > 0)
		{
			if (Rnd.get(100) <= getSpiritShotChance())
			{
				_spiritshotamount = _spiritshotamount - 1;
				Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUser(this, this, 2061, 1, 0, 0), 360000);
				_spiritshotcharged = true;
			}
		}
		else
		{
			return false;
		}
		
		return _spiritshotcharged;
	}
	
	public int getAggroRange2()
	{
		return _staticAIData.getAggroRange();
	}
	
	public int getEnemyRange()
	{
		return _staticAIData.getEnemyRange();
	}
	
	public String getEnemyClan()
	{
		return _staticAIData.getEnemyClan();
	}
	
	public int getClanRange()
	{
		return _staticAIData.getClanRange();
	}
	
	public String getClan()
	{
		return _staticAIData.getClan();
	}
	
	public int getPrimaryAttack()
	{
		return _staticAIData.getPrimaryAttack();
	}
	
	public int getMinSkillChance()
	{
		return _staticAIData.getMinSkillChance();
	}
	
	public int getMaxSkillChance()
	{
		return _staticAIData.getMaxSkillChance();
	}
	
	public int getCanMove()
	{
		return _staticAIData.getCanMove();
	}
	
	public int getIsChaos()
	{
		return _staticAIData.getIsChaos();
	}
	
	public int getShortRangeSkillChance()
	{
		return _staticAIData.getShortRangeChance();
	}
	
	public int getLongRangeSkillChance()
	{
		return _staticAIData.getLongRangeChance();
	}
	
	public int getSwitchRangeChance()
	{
		return _staticAIData.getSwitchRangeChance();
	}
	
	public boolean hasLongRangeSkill()
	{
		return _staticAIData.getLongRangeSkill() != 0;
	}
	
	public boolean hasShortRangeSkill()
	{
		return _staticAIData.getShortRangeSkill() != 0;
	}
	
	public FastList<L2Skill> getLongRangeSkill()
	{
		final FastList<L2Skill> skilldata = new FastList<>();
		if ((_staticAIData == null) || (_staticAIData.getLongRangeSkill() == 0))
		{
			return skilldata;
		}
		
		switch (_staticAIData.getLongRangeSkill())
		{
			case -1:
			{
				L2Skill[] skills = null;
				skills = getAllSkills();
				if (skills != null)
				{
					for (L2Skill sk : skills)
					{
						if ((sk == null) || sk.isPassive() || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF))
						{
							continue;
						}
						
						if (sk.getCastRange() >= 200)
						{
							skilldata.add(sk);
						}
					}
				}
				break;
			}
			case 1:
			{
				if (getTemplate().getUniversalSkills() != null)
				{
					for (L2Skill sk : getTemplate().getUniversalSkills())
					{
						if (sk.getCastRange() >= 200)
						{
							skilldata.add(sk);
						}
					}
				}
				break;
			}
			default:
			{
				for (L2Skill sk : getAllSkills())
				{
					if (sk.getId() == _staticAIData.getLongRangeSkill())
					{
						skilldata.add(sk);
					}
				}
			}
		}
		return skilldata;
	}
	
	public FastList<L2Skill> getShortRangeSkill()
	{
		final FastList<L2Skill> skilldata = new FastList<>();
		if ((_staticAIData == null) || (_staticAIData.getShortRangeSkill() == 0))
		{
			return skilldata;
		}
		
		switch (_staticAIData.getShortRangeSkill())
		{
			case -1:
			{
				L2Skill[] skills = null;
				skills = getAllSkills();
				if (skills != null)
				{
					for (L2Skill sk : skills)
					{
						if ((sk == null) || sk.isPassive() || (sk.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF))
						{
							continue;
						}
						
						if (sk.getCastRange() <= 200)
						{
							skilldata.add(sk);
						}
					}
				}
				break;
			}
			case 1:
			{
				if (getTemplate().getUniversalSkills() != null)
				{
					for (L2Skill sk : getTemplate().getUniversalSkills())
					{
						if (sk.getCastRange() <= 200)
						{
							skilldata.add(sk);
						}
					}
				}
				break;
			}
			default:
			{
				for (L2Skill sk : getAllSkills())
				{
					if (sk.getId() == _staticAIData.getShortRangeSkill())
					{
						skilldata.add(sk);
					}
				}
			}
		}
		return skilldata;
	}
	/**
	 * Task launching the function onRandomAnimation().
	 */
	protected class RandomAnimationTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if (this != _rAniTask)
					return; // Shouldn't happen, but who knows... just to make sure every active npc has only one timer.
					
				if (isMob())
				{
					// Cancel further animation timers until intention is changed to ACTIVE again.
					if (getAI().getIntention() != AI_INTENTION_ACTIVE)
						return;
				}
				else
				{
					if (!isInActiveRegion()) // NPCs in inactive region don't run this task
						return;
				}
				
				if (!(isDead() || isStunned() || isSleeping() || isParalyzed()))
				{
					onRandomAnimation();
				}
				
				startRandomAnimationTimer();
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					t.printStackTrace();
			}
		}
	}
	
	/**
	 * Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance and create a new RandomAnimation Task.<BR>
	 * <BR>
	 */
	public void onRandomAnimation()
	{
		final int min = _customNpcInstance != null ? 1 : 2;
		final int max = _customNpcInstance != null ? 13 : 3;
		// Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
		SocialAction sa = new SocialAction(getObjectId(), Rnd.get(min, max));
		broadcastPacket(sa);
		sa = null;
	}
	
	/**
	 * Create a RandomAnimation Task that will be launched after the calculated delay.<BR>
	 * <BR>
	 */
	public void startRandomAnimationTimer()
	{
		if (!hasRandomAnimation())
			return;
		
		final int minWait = isMob() ? Config.MIN_MONSTER_ANIMATION : Config.MIN_NPC_ANIMATION;
		final int maxWait = isMob() ? Config.MAX_MONSTER_ANIMATION : Config.MAX_NPC_ANIMATION;
		
		// Calculate the delay before the next animation
		final int interval = Rnd.get(minWait, maxWait) * 1000;
		
		// Create a RandomAnimation Task that will be launched after the calculated delay
		_rAniTask = new RandomAnimationTask();
		ThreadPoolManager.getInstance().scheduleGeneral(_rAniTask, interval);
	}
	
	/**
	 * Check if the server allows Random Animation.<BR>
	 * <BR>
	 * @return true, if successful
	 */
	public boolean hasRandomAnimation()
	{
		return Config.MAX_NPC_ANIMATION > 0;
	}
	
	/**
	 * The Class destroyTemporalNPC.
	 */
	public class destroyTemporalNPC implements Runnable
	{
		
		/** The _old spawn. */
		private final L2Spawn _oldSpawn;
		
		/**
		 * Instantiates a new destroy temporal npc.
		 * @param spawn the spawn
		 */
		public destroyTemporalNPC(final L2Spawn spawn)
		{
			_oldSpawn = spawn;
		}

		@Override
		public void run()
		{
			try
			{
				_oldSpawn.getLastSpawn().deleteMe();
				_oldSpawn.stopRespawn();
				SpawnTable.getInstance().deleteSpawn(_oldSpawn, false);
			}
			catch (final Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
	
	/**
	 * The Class destroyTemporalSummon.
	 */
	public class destroyTemporalSummon implements Runnable
	{
		
		/** The _summon. */
		L2Summon _summon;
		
		/** The _player. */
		L2PcInstance _player;
		
		/**
		 * Instantiates a new destroy temporal summon.
		 * @param summon the summon
		 * @param player the player
		 */
		public destroyTemporalSummon(final L2Summon summon, final L2PcInstance player)
		{
			_summon = summon;
			_player = player;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			_summon.unSummon(_player);
			_summon = null;
			_player = null;
		}
	}
	
	/**
	 * Constructor of L2NpcInstance (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2Character (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR)</li> <li>Set the name of the L2Character</li> <li>Create a RandomAnimation Task that will be launched after the calculated delay if
	 * the server allow it</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2NpcTemplate to apply to the NPC
	 */
	public L2NpcInstance(final int objectId, final L2NpcTemplate template)
	{
		// Call the L2Character constructor to set the _template of the L2Character, copy skills from template to object
		// and link _calculators to NPC_STD_CALCULATOR
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		initCharStatusUpdateValues();
		
		// initialize the "current" equipment
		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		// initialize the "current" collisions
		_currentCollisionHeight = getTemplate().collisionHeight;
		_currentCollisionRadius = getTemplate().collisionRadius;
		
		// Set the name of the L2Character
		setName(template.name);
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.L2Character#getKnownList()
	 */
	@Override
	public NpcKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof NpcKnownList))
		{
			setKnownList(new NpcKnownList(this));
		}
		
		return (NpcKnownList) super.getKnownList();
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.L2Character#getStat()
	 */
	@Override
	public NpcStat getStat()
	{
		if (super.getStat() == null || !(super.getStat() instanceof NpcStat))
		{
			setStat(new NpcStat(this));
		}
		
		return (NpcStat) super.getStat();
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.L2Character#getStatus()
	 */
	@Override
	public NpcStatus getStatus()
	{
		if (super.getStatus() == null || !(super.getStatus() instanceof NpcStatus))
		{
			setStatus(new NpcStatus(this));
		}
		
		return (NpcStatus) super.getStatus();
	}
	
	/**
	 * Return the L2NpcTemplate of the L2NpcInstance.
	 * @return the template
	 */
	@Override
	public final L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}
	
	/**
	 * Return the generic Identifier of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 * @return the npc id
	 */
	public int getNpcId()
	{
		return getTemplate().npcId;
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.L2Object#isAttackable()
	 */
	@Override
	public boolean isAttackable()
	{
		if (Config.NPC_ATTACKABLE || this instanceof L2Attackable)
			return true;
		
		return false;
	}
	
	/**
	 * Return the faction Identifier of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * If a NPC belows to a Faction, other NPC of the faction inside the Faction range will help it if it's attacked<BR>
	 * <BR>
	 * @return the faction id
	 */
	public final String getFactionId()
	{
		return getTemplate().factionId;
	}
	
	/**
	 * Return the Level of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 * @return the level
	 */
	@Override
	public final int getLevel()
	{
		return getTemplate().level;
	}
	
	@Override
	public final String getLevels()
	{
		return ""+getTemplate().level;
	}
	
	/**
	 * Return True if the L2NpcInstance is agressive (ex : L2MonsterInstance in function of aggroRange).<BR>
	 * <BR>
	 * @return true, if is aggressive
	 */
	public boolean isAggressive()
	{
		return false;
	}
	
	/**
	 * Return the Aggro Range of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 * @return the aggro range
	 */
	public int getAggroRange()
	{
		return getTemplate().aggroRange;
	}
	
	/**
	 * Return the Faction Range of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 * @return the faction range
	 */
	public int getFactionRange()
	{
		return getTemplate().factionRange;
	}
	
	/**
	 * Return True if this L2NpcInstance is undead in function of the L2NpcTemplate.<BR>
	 * <BR>
	 * @return true, if is undead
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead;
	}
	
	/**
	 * Send a packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the L2NpcInstance.<BR>
	 * <BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		// Send a Server->Client packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the L2NpcInstance
		for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
			if (player != null)
			{
				player.sendPacket(new NpcInfo(this, player));
			}
	}
	
	/**
	 * Return the distance under which the object must be add to _knownObject in function of the object type.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>object is a L2FolkInstance : 0 (don't remember it)</li> <li>object is a L2Character : 0 (don't remember it)</li> <li>object is a L2PlayableInstance : 1500</li> <li>others : 500</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2Attackable</li><BR>
	 * <BR>
	 * @param object The Object to add to _knownObject
	 * @return the distance to watch object
	 */
	public int getDistanceToWatchObject(final L2Object object)
	{
		if (object instanceof L2FestivalGuideInstance)
			return 10000;
		
		if (object instanceof L2FolkInstance || !(object instanceof L2Character))
			return 0;
		
		if (object instanceof L2PlayableInstance)
			return 1500;
		
		return 500;
	}
	
	/**
	 * Return the distance after which the object must be remove from _knownObject in function of the object type.<BR>
	 * <BR>
	 * <B><U> Values </U> :</B><BR>
	 * <BR>
	 * <li>object is not a L2Character : 0 (don't remember it)</li> <li>object is a L2FolkInstance : 0 (don't remember it)</li> <li>object is a L2PlayableInstance : 3000</li> <li>others : 1000</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2Attackable</li><BR>
	 * <BR>
	 * @param object The Object to remove from _knownObject
	 * @return the distance to forget object
	 */
	public int getDistanceToForgetObject(final L2Object object)
	{
		return 2 * getDistanceToWatchObject(object);
	}
	
	/**
	 * Return False.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2MonsterInstance : Check if the attacker is not another L2MonsterInstance</li> <li>L2PcInstance</li><BR>
	 * <BR>
	 * @param attacker the attacker
	 * @return true, if is auto attackable
	 */
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return false;
	}
	
	/**
	 * Return the Identifier of the item in the left hand of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 * @return the left hand item
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}
	
	/**
	 * Return the Identifier of the item in the right hand of this L2NpcInstance contained in the L2NpcTemplate.<BR>
	 * <BR>
	 * @return the right hand item
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}
	
	/**
	 * Return True if this L2NpcInstance has drops that can be sweeped.<BR>
	 * <BR>
	 * @return true, if is spoil
	 */
	public boolean isSpoil()
	{
		return _isSpoil;
	}
	
	/**
	 * Set the spoil state of this L2NpcInstance.<BR>
	 * <BR>
	 * @param isSpoil the new spoil
	 */
	public void setSpoil(final boolean isSpoil)
	{
		_isSpoil = isSpoil;
	}
	
	/**
	 * Gets the checks if is spoiled by.
	 * @return the checks if is spoiled by
	 */
	public final int getIsSpoiledBy()
	{
		return _isSpoiledBy;
	}
	
	/**
	 * Sets the checks if is spoiled by.
	 * @param value the new checks if is spoiled by
	 */
	public final void setIsSpoiledBy(final int value)
	{
		_isSpoiledBy = value;
	}
	
	/**
	 * Return the busy status of this L2NpcInstance.<BR>
	 * <BR>
	 * @return true, if is busy
	 */
	public final boolean isBusy()
	{
		return _isBusy;
	}
	
	/**
	 * Set the busy status of this L2NpcInstance.<BR>
	 * <BR>
	 * @param isBusy the new busy
	 */
	public void setBusy(final boolean isBusy)
	{
		_isBusy = isBusy;
	}
	
	/**
	 * Return the busy message of this L2NpcInstance.<BR>
	 * <BR>
	 * @return the busy message
	 */
	public final String getBusyMessage()
	{
		return _busyMessage;
	}
	
	/**
	 * Set the busy message of this L2NpcInstance.<BR>
	 * <BR>
	 * @param message the new busy message
	 */
	public void setBusyMessage(final String message)
	{
		_busyMessage = message;
	}
	
	/**
	 * Can target.
	 * @param player the player
	 * @return true, if successful
	 */
	protected boolean canTarget(final L2PcInstance player)
	{
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Can interact.
	 * @param activeChar the player
	 * @return true, if successful
	 */
	protected boolean canInteract(L2PcInstance activeChar)
	{
		// Like L2OFF if char is dead, is sitting, is in trade or is in fakedeath can't interact with npc
		if (activeChar.isSitting() || activeChar.isDead() || activeChar.isFakeDeath() || activeChar.getActiveTradeList() != null)
		{
			return false;
		}
		
		if (!Util.checkIfInRange(L2NpcInstance.INTERACTION_DISTANCE, activeChar, activeChar.getTarget(), true))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Manage actions when a player click on the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions on first click on the L2NpcInstance (Select it)</U> :</B><BR>
	 * <BR>
	 * <li>Set the L2NpcInstance as target of the L2PcInstance player (if necessary)</li> <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li> <li>If L2NpcInstance is autoAttackable, send a Server->Client packet StatusUpdate to the
	 * L2PcInstance in order to update L2NpcInstance HP bar</li> <li>Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client</li><BR>
	 * <BR>
	 * <B><U> Actions on second click on the L2NpcInstance (Attack it/Intercat with it)</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li> <li>If L2NpcInstance is autoAttackable, notify the L2PcInstance AI with AI_INTENTION_ATTACK (after a height verification)</li> <li>If L2NpcInstance is NOT autoAttackable, notify the
	 * L2PcInstance AI with AI_INTENTION_INTERACT (after a distance verification) and show message</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid that client wait an other packet</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action, AttackRequest</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2ArtefactInstance : Manage only fisrt click to select Artefact</li><BR>
	 * <BR>
	 * <li>L2GuardInstance :</li><BR>
	 * <BR>
	 * @param player The L2PcInstance that start an action on the L2NpcInstance
	 */
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
			return;
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Remove player spawn protection
			player.onActionRequest();
			
			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
				// The player.getLevel() - getLevel() permit to display the correct color in the select window
				MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
				player.sendPacket(my);
				
				// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			else
			{
				// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
				MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
				player.sendPacket(my);
			}
			
			player.setTimerToAttack(System.currentTimeMillis());
		}
		else
		{
			// Check if the player is attackable (without a forced attack) and isn't dead
			if (isAutoAttackable(player))
			{
				if ((Config.GEODATA == 0) || GeoData.getInstance().canSeeTarget(player, this))
				{
					if (!isAlikeDead())
					{	
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					}
					else
					{
						// Rotate the player to face the instance
						player.sendPacket(new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE));
						Broadcast.toKnownPlayers(player, new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE));
						
						// Send ActionFailed to the player in order to avoid he stucks
						player.sendPacket(ActionFailed.STATIC_PACKET);
						
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
				}
				// Send ActionFailed to the player in order to avoid he stucks
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else
			{
				// Calculate the distance between the L2PcInstance and the L2NpcInstance
				if (!canInteract(player))
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Like L2OFF player must rotate to the Npc
					MoveToPawn sp = new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE);
					player.sendPacket(sp);
					Broadcast.toKnownPlayers(player, sp);
					
					// Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance to display a social action of the L2NpcInstance on their client
					final SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
					broadcastPacket(sa);
					
					// Open a chat window on client with the text of the L2NpcInstance
					if (isEventMob)
					{
						L2Event.showEventHtml(player, String.valueOf(getObjectId()));
					}
					else if (_isEventMobTvT)
					{
						TvT.showEventHtml(player, String.valueOf(getObjectId()));
					}
					else if (_isEventMobDM)
					{
						DM.showEventHtml(player, String.valueOf(this.getObjectId()));
					}
					else if (_isEventMobCTF)
					{
						CTF.showEventHtml(player, String.valueOf(getObjectId()));
					}
					else if (_isCTF_Flag && player._inEventCTF)
					{
						CTF.showFlagHtml(player, String.valueOf(this.getObjectId()), _CTF_FlagTeamName);
					}
					else if (_isCTF_throneSpawn)
					{
						CTF.checkRestoreFlags();
					}
					else if (this._isEventVIPNPC)
					{
						VIP.showJoinHTML(player, String.valueOf(this.getObjectId()));
					}
					else if (this._isEventVIPNPCEnd)
					{
						VIP.showEndHTML(player, String.valueOf(this.getObjectId()));
					}
					else
					{
						Quest[] qlst = getTemplate().getEventQuests(QuestEventType.NPC_FIRST_TALK);
						if ((qlst.length >= 1))
						{
							qlst[0].notifyFirstTalk(this, player);
						}
						else
						{
							showChatWindow(player);
						}
					}
					
					// Send ActionFailed to the player in order to avoid he stucks
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
		}
	}
	
	/**
	 * Manage and Display the GM console to modify the L2NpcInstance (GM only).<BR>
	 * <BR>
	 * <B><U> Actions (If the L2PcInstance is a GM only)</U> :</B><BR>
	 * <BR>
	 * <li>Set the L2NpcInstance as target of the L2PcInstance player (if necessary)</li> <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li> <li>If L2NpcInstance is autoAttackable, send a Server->Client packet StatusUpdate to the
	 * L2PcInstance in order to update L2NpcInstance HP bar</li> <li>Send a Server->Client NpcHtmlMessage() containing the GM console about this L2NpcInstance</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid that client wait an other packet</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action</li><BR>
	 * <BR>
	 * @param client The thread that manage the player that pessed Shift and click on the L2NpcInstance
	 */
	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final L2Weapon currentWeapon = player.getActiveWeaponItem();
		
		if (player.getAccessLevel().isGm())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			TextBuilder html1 = new TextBuilder("<html><title>"+getName()+"</title><body>");
			
			html1.append("<center>");
			html1.append("<table><tr>");
			html1.append("<td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc " + getTemplate().npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			html1.append("<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			html1.append("</tr>");
			html1.append("<tr>");
			html1.append("<td><button value=\"DropList\" action=\"bypass -h admin_show_droplist " + getTemplate().npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			html1.append("<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			html1.append("</tr>");
			html1.append("<tr>");
			html1.append("<td><button value=\"Skillist\" action=\"bypass -h admin_show_skilllist_npc " + getTemplate().npcId + "\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			
			if (getSpawn() != null)
			{
				html1.append("<td><button value=\"Send Home\" action=\"bypass -h admin_sendHome "+getSpawn().getLocx()+" "+getSpawn().getLocy()+" "+getSpawn().getLocz()+"\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			}
			else
			{
				html1.append("<td></td>");
			}
			
			
			html1.append("</tr></table></center><br>");
			
			html1.append("<center><font color=\"LEVEL\">NPC Information</font></center>");
			String className = getClass().getName();
			int classNameLength = className.length();
			className = className.substring(37,classNameLength - 8);
			html1.append("<br1>");
			
			html1.append("Type: " + className + "<br1>Faction: " + getFactionId() + "<br1>Location ID: " + (getSpawn() != null ? getSpawn().getLocation() : 0) + "<br1>");
			
			if (this instanceof L2ControllableMobInstance)
			{
				html1.append("Mob Group: " + MobGroupTable.getInstance().getGroupForMob((L2ControllableMobInstance) this).getGroupId() + "<br>");
			}
			else
			{
				html1.append("Respawn Time: " + (getSpawn() != null ? getSpawn().getRespawnDelay() / 1000 + "  Seconds<br>" : "?  Seconds<br>"));
			}
			
			html1.append("<table border=\"0\" width=\"100%\">");
			html1.append("<tr><td>OID:</td><td>" + getObjectId() + "</td><td>TID:</td><td>" + getTemplate().npcId + " SID: " +(getSpawn() != null ? getSpawn().getId() : 0)+"</td></tr>");
			html1.append("<tr><td>Castle:</td><td>" + getCastle().getCastleId() + "</td><td>Coords:</td><td>" + getX() + "," + getY() + "," + getZ() + "</td></tr>");
			html1.append("<tr><td>Level:</td><td>" + getLevel() + "</td><td>Aggro:</td><td>" + (this instanceof L2Attackable ? ((L2Attackable) this).getAggroRange() : 0) + "</td></tr>");
			html1.append("</table><br>");
			
			html1.append("<table border=\"0\" width=\"100%\">");
			html1.append("<tr><td>Current HP:</td><td>" + getCurrentHp() + "</td><td>Current MP:</td><td>" + getCurrentMp() + "</td></tr>");
			html1.append("<tr><td>Max.HP:</td><td>" + (int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null)) + "*" + getStat().calcStat(Stats.MAX_HP, 1, this, null) + "</td><td>Max.MP:</td><td>" + getMaxMp() + "</td></tr>");
			html1.append("<tr><td>P.Atk.:</td><td>" + getPAtk(null) + "</td><td>M.Atk.:</td><td>" + getMAtk(null, null) + "</td></tr>");
			html1.append("<tr><td>P.Def.:</td><td>" + getPDef(null) + "</td><td>M.Def.:</td><td>" + getMDef(null, null) + "</td></tr>");
			html1.append("<tr><td>Accuracy:</td><td>" + getAccuracy() + "</td><td>Evasion:</td><td>" + getEvasionRate(null) + "</td></tr>");
			html1.append("<tr><td>Critical:</td><td>" + getCriticalHit(null, null) + "</td><td>Speed:</td><td>" + getRunSpeed() + "</td></tr>");
			html1.append("<tr><td>Atk.Speed:</td><td>" + getPAtkSpd() + "</td><td>Cast.Speed:</td><td>" + getMAtkSpd() + "</td></tr>");
			html1.append("</table><br>");
			
			html1.append("<table border=\"0\" width=\"100%\">");
			html1.append("<tr><td>STR:</td><td>" + getSTR() + "</td><td>DEX:</td><td>" + getDEX() + "</td><td>CON:</td><td>" + getCON() + "</td></tr>");
			html1.append("<tr><td>INT:</td><td>" + getINT() + "</td><td>WIT:</td><td>" + getWIT() + "</td><td>MEN:</td><td>" + getMEN() + "</td></tr>");
			html1.append("</table>");
			
			html1.append("</body></html>");
			
			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else if (Config.ALT_GAME_VIEWNPC)
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			// The player.getLevel() - getLevel() permit to display the correct color in the select window
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			
			// TODO Shift menu
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			TextBuilder html1 = new TextBuilder("<html><body>");
			
			html1.append("<br><center><font color=\"3399ff\">Stats</font></center>");
			html1.append("<table border=0 width=\"100%\">");
			html1.append("<tr><td>Max.HP</td><td>" + (int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null)) + "*" + (int) getStat().calcStat(Stats.MAX_HP, 1, this, null) + "</td><td>Max.MP</td><td>" + getMaxMp() + "</td></tr>");
			html1.append("<tr><td>P.Atk.</td><td>" + getPAtk(null) + "</td><td>M.Atk.</td><td>" + getMAtk(null, null) + "</td></tr>");
			html1.append("<tr><td>P.Def.</td><td>" + getPDef(null) + "</td><td>M.Def.</td><td>" + getMDef(null, null) + "</td></tr>");
			html1.append("<tr><td>Accuracy</td><td>" + getAccuracy() + "</td><td>Evasion</td><td>" + getEvasionRate(null) + "</td></tr>");
			html1.append("<tr><td>Critical</td><td>" + getCriticalHit(null, null) + "</td><td>Speed</td><td>" + getRunSpeed() + "</td></tr>");
			html1.append("<tr><td>Atk.Speed</td><td>" + getPAtkSpd() + "</td><td>Cast.Speed</td><td>" + getMAtkSpd() + "</td></tr>");
			html1.append("<tr><td>Race</td><td>" + getTemplate().race + "</td><td></td><td></td></tr>");
			html1.append("</table>");
			
			html1.append("<br><center><font color=\"3399ff\">Basic Stats</font></center>");
			html1.append("<table border=0 width=\"100%\">");
			html1.append("<tr><td>STR</td><td>" + getSTR() + "</td><td>DEX</td><td>" + getDEX() + "</td><td>CON</td><td>" + getCON() + "</td></tr>");
			html1.append("<tr><td>INT</td><td>" + getINT() + "</td><td>WIT</td><td>" + getWIT() + "</td><td>MEN</td><td>" + getMEN() + "</td></tr>");
			html1.append("</table>");
			
			html1.append("<br><center><font color=\"3399ff\">Drop Info</font></center>");
			html1.append("<center><button value=\"Droplist\" action=\"bypass -h dropInfo\" width=100 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
			
			html1.append("</body></html>");
			
			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else
		// Like L2OFF set the target of the L2PcInstance player
		{
			// Check if the L2PcInstance already target the L2NpcInstance
			if (this != player.getTarget())
			{
				// Set the target of the L2PcInstance player
				player.setTarget(this);
				
				// Check if the player is attackable (without a forced attack)
				if (isAutoAttackable(player))
				{
					// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
					// The player.getLevel() - getLevel() permit to display the correct color in the select window
					MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
					player.sendPacket(my);
					my = null;
					
					// Send a Server->Client packet StatusUpdate of the L2NpcInstance to the L2PcInstance to update its HP bar
					StatusUpdate su = new StatusUpdate(getObjectId());
					su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
					player.sendPacket(su);
				}
				else
				{
					// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
					MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
					player.sendPacket(my);
				}
				
				player.setTimerToAttack(System.currentTimeMillis());
			}
			else
			{
				// Check if the player is attackable (without a forced attack) and isn't dead
				if (isAutoAttackable(player) && !isAlikeDead())
				{
					// Check the height difference
					if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
					{
						// Like L2OFF player must not move with shift pressed
						// Only archer can hit from long
						if (!canInteract(player) && (currentWeapon != null && currentWeapon.getItemType() != L2WeaponType.BOW))
						{
							// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
							player.sendPacket(ActionFailed.STATIC_PACKET);
						}
						else if (!canInteract(player) && currentWeapon == null)
						{
							// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
							player.sendPacket(ActionFailed.STATIC_PACKET);
						}
						else
						{
							// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						}
					}
					else
					{
						// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
				}
				else if (!isAutoAttackable(player))
				{
					// Like L2OFF player must not move with shift pressed
					// Only archer can hit from long
					if (!canInteract(player) && (currentWeapon != null && currentWeapon.getItemType() != L2WeaponType.BOW))
					{
						// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else if (!canInteract(player) && currentWeapon == null)
					{
						// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{
						// Like L2OFF if char is dead, is sitting, is in trade or is in fakedeath can't interact with npc
						if (player.isSitting() || player.isDead() || player.isFakeDeath() || player.getActiveTradeList() != null)
							return;
						
						
						// Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance to display a social action of the L2NpcInstance on their client
						final SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
						broadcastPacket(sa);
						// Open a chat window on client with the text of the L2NpcInstance
						if (isEventMob)
						{
							L2Event.showEventHtml(player, String.valueOf(getObjectId()));
						}
						else if (_isEventMobTvT)
						{
							TvT.showEventHtml(player, String.valueOf(getObjectId()));
						}
						else if (_isEventMobDM)
						{
							DM.showEventHtml(player, String.valueOf(this.getObjectId()));
						}
						else if (_isEventMobCTF)
						{
							CTF.showEventHtml(player, String.valueOf(getObjectId()));
						}
						else if (_isCTF_Flag && player._inEventCTF)
						{
							CTF.showFlagHtml(player, String.valueOf(this.getObjectId()), _CTF_FlagTeamName);
						}
						else if (_isCTF_throneSpawn)
						{
							CTF.checkRestoreFlags();
						}
						else if (this._isEventVIPNPC)
						{
							VIP.showJoinHTML(player, String.valueOf(this.getObjectId()));
						}
						else if (this._isEventVIPNPCEnd)
						{
							VIP.showEndHTML(player, String.valueOf(this.getObjectId()));
						}
						else
						{
							/*final Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.NPC_FIRST_TALK);
							if (qlst.length == 1)
							{
								qlst[0].notifyFirstTalk(this, player);
							}
							else
							{
								showChatWindow(player, 0);
							}*/
							
							player.sendPacket(ActionFailed.STATIC_PACKET);
						}
					}
					
					// to avoid player stuck
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
				else
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			
			// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Return the L2Castle this L2NpcInstance belongs to.
	 * @return the castle
	 */
	public final Castle getCastle()
	{
		// Get castle this NPC belongs to (excluding L2Attackable)
		if (_castleIndex < 0)
		{
			final L2TownZone town = TownManager.getInstance().getTown(getX(), getY(), getZ());
			
			if (town != null)
			{
				_castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());
			}
			
			if (_castleIndex < 0)
			{
				_castleIndex = CastleManager.getInstance().findNearestCastlesIndex(this);
			}
			else
			{
				_isInTown = true; // Npc was spawned in town
			}
		}
		
		if (_castleIndex < 0)
			return null;
		
		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}
	
	/**
	 * Return the L2Fort this L2NpcInstance belongs to.
	 * @return the fort
	 */
	public final Fort getFort()
	{
		// Get Fort this NPC belongs to (excluding L2Attackable)
		if (_fortIndex < 0)
		{
			final Fort fort = FortManager.getInstance().getFort(getX(), getY(), getZ());
			if (fort != null)
			{
				_fortIndex = FortManager.getInstance().getFortIndex(fort.getFortId());
			}
			if (_fortIndex < 0)
			{
				_fortIndex = FortManager.getInstance().findNearestFortIndex(this);
			}
		}
		if (_fortIndex < 0)
			return null;
		return FortManager.getInstance().getForts().get(_fortIndex);
	}
	
	/**
	 * Gets the checks if is in town.
	 * @return the checks if is in town
	 */
	public final boolean getIsInTown()
	{
		if (_castleIndex < 0)
		{
			getCastle();
		}
		return _isInTown;
	}
	
	/**
	 * Open a quest or chat window on client with the text of the L2NpcInstance in function of the command.<BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : RequestBypassToServer</li><BR>
	 * <BR>
	 * @param player the player
	 * @param command The command string received from client
	 */
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (isBusy() && getBusyMessage().length() > 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("/data/html/npcbusy.htm");
			html.replace("%busymessage%", getBusyMessage());
			html.replace("%npcname%", getName());
			html.replace("%playername%", player.getName());
			player.sendPacket(html);
		}
		else if (command.equalsIgnoreCase("TerritoryStatus"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			{
				if (getCastle().getOwnerId() > 0)
				{
					html.setFile("/data/html/territorystatus.htm");
					final L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
					html.replace("%clanname%", clan.getName());
					html.replace("%clanleadername%", clan.getLeaderName());
				}
				else
				{
					html.setFile("/data/html/territorynoclan.htm");
				}
			}
			html.replace("%name%", getName());
			html.replace("%player_name%", player.getName());
			html.replace("%castlename%", getCastle().getName());
			html.replace("%taxpercent%", "" + getCastle().getTaxPercent());
			html.replace("%objectId%", String.valueOf(getObjectId()));
			
			if (getCastle().getCastleId() > 6)
			{
				html.replace("%territory%", "The Kingdom of Elmore");
			}
			else
			{
				html.replace("%territory%", "The Kingdom of Aden");
			}
			
			player.sendPacket(html);
		}
		else if (command.startsWith("Quest"))
		{
			String quest = "";
			try
			{
				quest = command.substring(5).trim();
			}
			catch (final IndexOutOfBoundsException ioobe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					ioobe.printStackTrace();
			}
			if (quest.length() == 0)
			{
				showQuestWindowGeneral(player);
			}
			else
			{
				showQuestWindowSingle(player, quest);
			}
		}
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException | NumberFormatException ioobe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					ioobe.printStackTrace();
			}
			showChatWindow(player, val);
		}
		else if (command.startsWith("Link"))
		{
			final String path = command.substring(5).trim();
			if (path.indexOf("..") != -1)
				return;
			String filename = "/data/html/" + path;
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			filename = null;
			html = null;
		}
		else if (command.startsWith("NobleTeleport"))
		{
			if (!player.isNoble())
			{
				String filename = "/data/html/teleporter/nobleteleporter-no.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				return;
			}
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException | NumberFormatException ioobe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					ioobe.printStackTrace();
			}
			showChatWindow(player, val);
		}
		else if (command.startsWith("Loto"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException | NumberFormatException ioobe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					ioobe.printStackTrace();
			}
			if (val == 0)
			{
				// new loto ticket
				for (int i = 0; i < 5; i++)
				{
					player.setLoto(i, 0);
				}
			}
			showLotoWindow(player, val);
		}
		else if (command.startsWith("CPRecovery"))
		{
			makeCPRecovery(player);
		}
		else if (command.startsWith("SupportMagic"))
		{
			makeSupportMagic(player);
		}
		else if (command.startsWith("GiveBlessing"))
		{
			giveBlessingSupport(player);
		}
		else if (command.startsWith("multisell"))
		{
			player.setTempAccessBuy(false);
			L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(command.substring(9).trim()), player, false, getCastle().getTaxRate());
		}
		else if (command.startsWith("exc_multisell"))
		{
			player.setTempAccessBuy(false);
			L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(command.substring(13).trim()), player, true, getCastle().getTaxRate());
		}
		else if (command.startsWith("Augment"))
		{
			final int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
			switch (cmdChoice)
			{
				case 1:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
					player.sendPacket(new ExShowVariationMakeWindow());
					break;
				case 2:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
					player.sendPacket(new ExShowVariationCancelWindow());
					break;
			}
		}
		else if (command.startsWith("npcfind_byid"))
		{
			try
			{
				L2Spawn spawn = SpawnTable.getInstance().getTemplate(Integer.parseInt(command.substring(12).trim()));
				
				if (spawn != null)
				{
					player.sendPacket(new RadarControl(0, 1, spawn.getLocx(), spawn.getLocy(), spawn.getLocz()));
					spawn = null;
				}
			}
			catch (final NumberFormatException nfe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					nfe.printStackTrace();
				
				player.sendMessage("Wrong command parameters");
			}
		}
		else if (command.startsWith("newbie_give_coupon"))
		{
			try
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (player.getLevel() > 25 || player.getLevel() < 6 || !player.isNewbie())
				{
					html.setFile("data/html/adventurers_guide/31760-3.htm");
					player.sendPacket(html);
				}
				else if (player.getCoupon(0))
				{
					html.setFile("data/html/adventurers_guide/31760-1.htm");
					player.sendPacket(html);
				}
				else
				{
					player.getInventory().addItem("Weapon Coupon", 7832, 1, player, this);
					player.addCoupon(1);
					html.setFile("data/html/adventurers_guide/31760-2.htm");
					player.sendPacket(html);
				}
			}
			catch (final NumberFormatException nfe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					nfe.printStackTrace();
				
				player.sendMessage("Wrong command parameters");
			}
		}
		else if (command.startsWith("newbie_give_weapon"))
		{
			try
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (player.getLevel() > 25 || player.getLevel() < 6 || !player.isNewbie())
				{
					html.setFile("data/html/adventurers_guide/31760-3.htm");
					player.sendPacket(html);
				}
				else
				{
					L2Multisell.getInstance().SeparateAndSend(10010, player, false, getCastle().getTaxRate());
				}
			}
			catch (final NumberFormatException nfe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					nfe.printStackTrace();
				
				player.sendMessage("Wrong command parameters");
			}
		}
		else if (command.startsWith("newbie_return_weapon"))
		{
			try
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (player.getLevel() > 25 || player.getLevel() < 6 || !player.isNewbie())
				{
					html.setFile("data/html/adventurers_guide/31760-3.htm");
					player.sendPacket(html);
				}
				else
				{
					L2Multisell.getInstance().SeparateAndSend(10011, player, false, getCastle().getTaxRate());
				}
			}
			catch (final NumberFormatException nfe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					nfe.printStackTrace();
				
				player.sendMessage("Wrong command parameters");
			}
		}
		else if (command.startsWith("traveller_give_coupon"))
		{
			try
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (player.getLevel() > 25 || player.getClassId().level() != 1 || !player.isNewbie())
				{
					html.setFile("data/html/adventurers_guide/31760-6.htm");
					player.sendPacket(html);
				}
				else if (player.getCoupon(1))
				{
					html.setFile("data/html/adventurers_guide/31760-4.htm");
					player.sendPacket(html);
				}
				else
				{
					player.getInventory().addItem("Weapon Coupon", 7833, 1, player, this);
					player.addCoupon(2);
					html.setFile("data/html/adventurers_guide/31760-5.htm");
					player.sendPacket(html);
				}
			}
			catch (final NumberFormatException nfe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					nfe.printStackTrace();
				
				player.sendMessage("Wrong command parameters");
			}
		}
		else if (command.startsWith("traveller_give_weapon"))
		{
			try
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (player.getLevel() > 25 || player.getClassId().level() != 1 || !player.isNewbie())
				{
					html.setFile("data/html/adventurers_guide/31760-6.htm");
					player.sendPacket(html);
				}
				else
				{
					L2Multisell.getInstance().SeparateAndSend(10012, player, false, getCastle().getTaxRate());
				}
			}
			catch (final NumberFormatException nfe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					nfe.printStackTrace();
				
				player.sendMessage("Wrong command parameters");
			}
		}
		else if (command.startsWith("traveller_return_weapon"))
		{
			try
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (player.getLevel() > 25 || player.getClassId().level() != 1 || !player.isNewbie())
				{
					html.setFile("data/html/adventurers_guide/31760-6.htm");
					player.sendPacket(html);
				}
				else
				{
					L2Multisell.getInstance().SeparateAndSend(10013, player, false, getCastle().getTaxRate());
				}
			}
			catch (final NumberFormatException nfe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					nfe.printStackTrace();
				
				player.sendMessage("Wrong command parameters");
			}
		}
		else if (command.startsWith("EnterRift"))
		{
			try
			{
				Byte b1 = Byte.parseByte(command.substring(10)); // Selected Area: Recruit, Soldier etc
				DimensionalRiftManager.getInstance().start(player, b1, this);
				b1 = null;
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
			}
		}
		else if (command.startsWith("ChangeRiftRoom"))
		{
			if (player.isInParty() && player.getParty().isInDimensionalRift())
			{
				player.getParty().getDimensionalRift().manualTeleport(player, this);
			}
			else
			{
				DimensionalRiftManager.getInstance().handleCheat(player, this);
			}
		}
		else if (command.startsWith("ExitRift"))
		{
			if (player.isInParty() && player.getParty().isInDimensionalRift())
			{
				player.getParty().getDimensionalRift().manualExitRift(player, this);
			}
			else
			{
				DimensionalRiftManager.getInstance().handleCheat(player, this);
			}
		}
		else if (command.startsWith("RaidbossLvl_"))
		{
			final int endOfId = command.indexOf('_', 5);
			if (endOfId > 0)
				command.substring(4, endOfId);
			else
				command.substring(4);
			try
			{
				if (command.substring(endOfId + 1).startsWith("40"))
					L2RBManager.RaidbossLevel40(player);
				else if (command.substring(endOfId + 1).startsWith("45"))
					L2RBManager.RaidbossLevel45(player);
				else if (command.substring(endOfId + 1).startsWith("50"))
					L2RBManager.RaidbossLevel50(player);
				else if (command.substring(endOfId + 1).startsWith("55"))
					L2RBManager.RaidbossLevel55(player);
				else if (command.substring(endOfId + 1).startsWith("60"))
					L2RBManager.RaidbossLevel60(player);
				else if (command.substring(endOfId + 1).startsWith("65"))
					L2RBManager.RaidbossLevel65(player);
				else if (command.substring(endOfId + 1).startsWith("70"))
					L2RBManager.RaidbossLevel70(player);
			}
			catch (final NumberFormatException nfe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					nfe.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR>
	 * <BR>
	 * @return the active weapon instance
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}
	
	/**
	 * Return the weapon item equiped in the right hand of the L2NpcInstance or null.<BR>
	 * <BR>
	 * @return the active weapon item
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		// Get the weapon identifier equiped in the right hand of the L2NpcInstance
		final int weaponId = getTemplate().rhand;
		
		if (weaponId < 1)
			return null;
		
		// Get the weapon item equiped in the right hand of the L2NpcInstance
		final L2Item item = ItemTable.getInstance().getTemplate(getTemplate().rhand);
		
		if (!(item instanceof L2Weapon))
			return null;
		
		return (L2Weapon) item;
	}
	
	/**
	 * Give blessing support.
	 * @param player the player
	 */
	public void giveBlessingSupport(final L2PcInstance player)
	{
		if (player == null)
			return;
		
		// Blessing of protection - author eX1steam.
		// Prevent a cursed weapon weilder of being buffed - I think no need of that becouse karma check > 0
		if (player.isCursedWeaponEquiped())
			return;
		
		final int player_level = player.getLevel();
		// Select the player
		setTarget(player);
		// If the player is too high level, display a message and return
		if (player_level > 39 || player.getClassId().level() >= 2)
		{
			String content = "<html><body>Newbie Guide:<br>I'm sorry, but you are not eligible to receive the protection blessing.<br1>It can only be bestowed on <font color=\"LEVEL\">characters below level 39 who have not made a seccond transfer.</font></body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			content = null;
			return;
		}
		L2Skill skill = SkillTable.getInstance().getInfo(5182, 1);
		doCast(skill);
		skill = null;
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR>
	 * <BR>
	 * @return the secondary weapon instance
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}
	
	/**
	 * Return the weapon item equiped in the left hand of the L2NpcInstance or null.<BR>
	 * <BR>
	 * @return the secondary weapon item
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// Get the weapon identifier equiped in the right hand of the L2NpcInstance
		final int weaponId = getTemplate().lhand;
		
		if (weaponId < 1)
			return null;
		
		// Get the weapon item equiped in the right hand of the L2NpcInstance
		final L2Item item = ItemTable.getInstance().getTemplate(getTemplate().lhand);
		
		if (!(item instanceof L2Weapon))
			return null;
		
		return (L2Weapon) item;
	}
	
	/**
	 * Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance.<BR>
	 * <BR>
	 * @param player The L2PcInstance who talks with the L2NpcInstance
	 * @param content The text of the L2NpcMessage
	 */
	public void insertObjectIdAndShowChatWindow(final L2PcInstance player, String content)
	{
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));
		NpcHtmlMessage npcReply = new NpcHtmlMessage(getObjectId());
		npcReply.setHtml(content);
		player.sendPacket(npcReply);
		npcReply = null;
	}
	
	/**
	 * Return the pathfile of the selected HTML file in function of the npcId and of the page number.<BR>
	 * <BR>
	 * <B><U> Format of the pathfile </U> :</B><BR>
	 * <BR>
	 * <li>if the file exists on the server (page number = 0) : <B>data/html/default/12006.htm</B> (npcId-page number)</li> <li>if the file exists on the server (page number > 0) : <B>data/html/default/12006-1.htm</B> (npcId-page number)</li> <li>if the file doesn't exist on the server :
	 * <B>data/html/npcdefault.htm</B> (message : "I have nothing to say to you")</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2GuardInstance : Set the pathfile to data/html/guard/12006-1.htm (npcId-page number)</li><BR>
	 * <BR>
	 * @param player 
	 * @param npcId The Identifier of the L2NpcInstance whose text must be display
	 * @param val The number of the page to display
	 * @return the html path
	 */
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		String pom = "";
		
		if (npcId == PowerPackConfig.BUFFER_NPC)
		{
			if (val == 0)
			{
				pom = "buffer";
			}
			else
			{
				pom = "buffer" + "-" + val;
			}
			
			if (!PowerPackConfig.BUFFER_ENABLED)
			{
				return "data/html/disabled.htm";
			}
			
			return "data/html/buffer/"+pom+".htm";
		}
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		final String temp = "data/html/default/" + pom + ".htm";
		
		if (!Config.LAZY_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it doesnt exist
			if (HtmCache.getInstance().contains(temp))
				return temp;
		}
		else
		{
			if (HtmCache.getInstance().isLoadable(temp))
				return temp;
		}
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
	
	/**
	 * Open a choose quest window on client with all quests available of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li><BR>
	 * <BR>
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param quests The table containing quests of the L2NpcInstance
	 */
	@SuppressWarnings("unlikely-arg-type")
	private void showQuestChooseWindow(final L2PcInstance player, final Quest[] quests)
	{
		final TextBuilder sb = new TextBuilder();
		sb.append("<html><body><title>Talk about:</title><br>");
		
		String state = "";
		
		for (final Quest q : quests)
		{
			
			if (q == null)
			{
				continue;
			}
			
			sb.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Quest ").append(q.getName()).append("\">[");
			
			final QuestState qs = player.getQuestState(q.getScriptName());
			
			if ((qs == null) || qs.getState().equals(State.ACTIVE))
			{
				state = "";
				
			}
			else if (qs.isStarted() && (qs.getInt("cond") > 0))
			{
				state = " (In Progress)";
				
			}
			else if (qs.isCompleted())
			{
				state = " (Done)";
			}
			
			sb.append(q.getDescr()).append(state).append("]</a><br>");
		}
		
		sb.append("</body></html>");
		
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		insertObjectIdAndShowChatWindow(player, sb.toString());
	}
	
	/**
	 * Open a quest window on client with the text of the L2Npc.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the quest state in the folder data/scripts/quests/questId/stateId.htm</li> <li>Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance</li> <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait
	 * another packet</li><BR>
	 * <BR>
	 * @param player The L2PcInstance that talk with the L2Npc
	 * @param questId The Identifier of the quest to display the message
	 */
	public void showQuestWindowSingle(L2PcInstance player, String questId)
	{
		String content = null;
		Quest q = null;
		
		if (!Config.ALT_DEV_NO_QUESTS)
		{
			q = QuestManager.getInstance().getQuest(questId);
		}
		
		// Get the state of the selected quest
		QuestState qs = player.getQuestState(questId);
		
		if (q == null)
		{
			// No quests found
			content = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
		}
		else
		{
			if (player.getWeightPenalty() >= 3 && q.getQuestIntId() >= 1 && q.getQuestIntId() < 1000)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT));
				return;
			}
			
			if (qs == null)
			{
				if (q.getQuestIntId() >= 1 && q.getQuestIntId() < 20000)
				{
					final Quest[] questList = player.getAllActiveQuests();
					if (questList.length >= 25) // if too many ongoing quests, don't show window and send message
					{
						player.sendMessage("You have too many quests, cannot register");
						return;
					}
				}
				// Check for start point
				for (final Quest temp : getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START))
				{
					if (temp == q)
					{
						qs = q.newQuestState(player);
						break;
					}
				}
			}
		}
		
		if (qs != null)
		{
			// If the quest is already started, no need to show a window
			if (!qs.getQuest().notifyTalk(this, qs))
				return;
			
			questId = qs.getQuest().getName();
			final String stateId = qs.getStateId();
			final String path = Config.DATAPACK_ROOT + "/data/scripts/quests/" + questId + "/" + stateId + ".htm";
			content = HtmCache.getInstance().getHtm(path);
		}
		
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2Npc
		if (content != null)
		{
			insertObjectIdAndShowChatWindow(player, content);
		}
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Collect awaiting quests/start points and display a QuestChooseWindow (if several available) or QuestWindow.<BR>
	 * <BR>
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 */
	public void showQuestWindowGeneral(final L2PcInstance player)
	{
		// collect awaiting quests and start points
		final List<Quest> options = new FastList<>();
		
		final QuestState[] awaits = player.getQuestsForTalk(getTemplate().npcId);
		final Quest[] starts = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
		
		// Quests are limited between 1 and 999 because those are the quests that are supported by the client.
		// By limitting them there, we are allowed to create custom quests at higher IDs without interfering
		if (awaits != null)
		{
			for (final QuestState x : awaits)
			{
				if (!options.contains(x.getQuest()))
					if (x.getQuest().getQuestIntId() > 0 && x.getQuest().getQuestIntId() < 1000)
					{
						options.add(x.getQuest());
					}
			}
		}
		
		for (final Quest x : starts)
		{
			if (!options.contains(x))
				if (x.getQuestIntId() > 0 && x.getQuestIntId() < 1000)
				{
					options.add(x);
				}
		}
		
		// Display a QuestChooseWindow (if several quests are available) or QuestWindow
		if (options.size() > 1)
		{
			showQuestChooseWindow(player, options.toArray(new Quest[options.size()]));
		}
		else if (options.size() == 1)
		{
			showQuestWindowSingle(player, options.get(0).getName());
		}
		else
		{
			showQuestWindowSingle(player, "");
		}
	}
	
	/**
	 * Open a Loto window on client with the text of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li> <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li> <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the
	 * client wait another packet</li><BR>
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param val The number of the page of the L2NpcInstance to display
	 */
	// 0 - first buy lottery ticket window
	// 1-20 - buttons
	// 21 - second buy lottery ticket window
	// 22 - selected ticket with 5 numbers
	// 23 - current lottery jackpot
	// 24 - Previous winning numbers/Prize claim
	// >24 - check lottery ticket by item object id
	public void showLotoWindow(final L2PcInstance player, final int val)
	{
		final int npcId = getTemplate().npcId;
		String filename;
		SystemMessage sm;
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		if (val == 0) // 0 - first buy lottery ticket window
		{
			filename = getHtmlPath(player, npcId, 1);
			html.setFile(filename);
		}
		else if (val >= 1 && val <= 21) // 1-20 - buttons, 21 - second buy lottery ticket window
		{
			if (!Lottery.getInstance().isStarted())
			{
				// tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD));
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				// tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE));
				return;
			}
			
			filename = getHtmlPath(player, npcId, 5);
			html.setFile(filename);
			
			int count = 0;
			int found = 0;
			// counting buttons and unsetting button if found
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == val)
				{
					// unsetting button
					player.setLoto(i, 0);
					found = 1;
				}
				else if (player.getLoto(i) > 0)
				{
					count++;
				}
			}
			
			// if not rearched limit 5 and not unseted value
			if (count < 5 && found == 0 && val <= 20)
			{
				for (int i = 0; i < 5; i++)
					if (player.getLoto(i) == 0)
					{
						player.setLoto(i, val);
						break;
					}
			}
			
			// setting pusshed buttons
			count = 0;
			for (int i = 0; i < 5; i++)
				if (player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if (player.getLoto(i) < 10)
					{
						button = "0" + button;
					}
					final String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					final String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}
			
			if (count == 5)
			{
				final String search = "0\">Return";
				final String replace = "22\">The winner selected the numbers above.";
				html.replace(search, replace);
			}
		}
		else if (val == 22) // 22 - selected ticket with 5 numbers
		{
			if (!Lottery.getInstance().isStarted())
			{
				// tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD));
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				// tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE));
				return;
			}
			
			final int price = Config.ALT_LOTTERY_TICKET_PRICE;
			final int lotonumber = Lottery.getInstance().getId();
			int enchant = 0;
			int type2 = 0;
			
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == 0)
					return;
				
				if (player.getLoto(i) < 17)
				{
					enchant += Math.pow(2, player.getLoto(i) - 1);
				}
				else
				{
					type2 += Math.pow(2, player.getLoto(i) - 17);
				}
			}
			if (player.getAdena() < price)
			{
				sm = new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				player.sendPacket(sm);
				return;
			}
			if (!player.reduceAdena("Loto", price, this, true))
				return;
			Lottery.getInstance().increasePrize(price);
			
			sm = new SystemMessage(SystemMessageId.ACQUIRED);
			sm.addNumber(lotonumber);
			sm.addItemName(4442);
			player.sendPacket(sm);
			sm = null;
			
			L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), 4442);
			item.setCount(1);
			item.setCustomType1(lotonumber);
			item.setEnchantLevel(enchant);
			item.setCustomType2(type2);
			player.getInventory().addItem("Loto", item, player, this);
			item = null;
			
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(item);
			final L2ItemInstance adenaupdate = player.getInventory().getItemByItemId(57);
			iu.addModifiedItem(adenaupdate);
			player.sendPacket(iu);
			iu = null;
			
			filename = getHtmlPath(player, npcId, 3);
			html.setFile(filename);
		}
		else if (val == 23) // 23 - current lottery jackpot
		{
			filename = getHtmlPath(player, npcId, 3);
			html.setFile(filename);
		}
		else if (val == 24) // 24 - Previous winning numbers/Prize claim
		{
			filename = getHtmlPath(player, npcId, 4);
			html.setFile(filename);
			
			final int lotonumber = Lottery.getInstance().getId();
			String message = "";
			for (final L2ItemInstance item : player.getInventory().getItems())
			{
				if (item == null)
				{
					continue;
				}
				if (item.getItemId() == 4442 && item.getCustomType1() < lotonumber)
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ";
					final int[] numbers = Lottery.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for (int i = 0; i < 5; i++)
					{
						message += numbers[i] + " ";
					}
					final int[] check = Lottery.getInstance().checkTicket(item);
					if (check[0] > 0)
					{
						switch (check[0])
						{
							case 1:
								message += "- 1st Prize";
								break;
							case 2:
								message += "- 2nd Prize";
								break;
							case 3:
								message += "- 3th Prize";
								break;
							case 4:
								message += "- 4th Prize";
								break;
						}
						message += " " + check[1] + "a.";
					}
					message += "</a><br>";
				}
			}
			if (message == "")
			{
				message += "There is no winning lottery ticket...<br>";
			}
			html.replace("%result%", message);
			message = null;
		}
		else if (val > 24) // >24 - check lottery ticket by item object id
		{
			final int lotonumber = Lottery.getInstance().getId();
			final L2ItemInstance item = player.getInventory().getItemByObjectId(val);
			if (item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber)
				return;
			final int[] check = Lottery.getInstance().checkTicket(item);
			
			sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
			sm.addItemName(4442);
			player.sendPacket(sm);
			sm = null;
			
			final int adena = check[1];
			if (adena > 0)
			{
				player.addAdena("Loto", adena, this, true);
			}
			player.destroyItem("Loto", item, this, false);
			return;
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%race%", "" + Lottery.getInstance().getId());
		html.replace("%adena%", "" + Lottery.getInstance().getPrize());
		html.replace("%ticket_price%", "" + Config.ALT_LOTTERY_TICKET_PRICE);
		html.replace("%prize5%", "" + Config.ALT_LOTTERY_5_NUMBER_RATE * 100);
		html.replace("%prize4%", "" + Config.ALT_LOTTERY_4_NUMBER_RATE * 100);
		html.replace("%prize3%", "" + Config.ALT_LOTTERY_3_NUMBER_RATE * 100);
		html.replace("%prize2%", "" + Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE);
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(Lottery.getInstance().getEndDate()));
		player.sendPacket(html);
		
		html = null;
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Make cp recovery.
	 * @param player the player
	 */
	public void makeCPRecovery(final L2PcInstance player)
	{
		if (getNpcId() != 31225 && getNpcId() != 31226)
			return;
		if (player.isCursedWeaponEquiped())
		{
			player.sendMessage("Go away, you're not welcome here.");
			return;
		}
		
		final int neededmoney = 100;
		SystemMessage sm;
		if (!player.reduceAdena("RestoreCP", neededmoney, player.getLastFolkNPC(), true))
			return;
		
		// Skill's animation
		final L2Skill skill = SkillTable.getInstance().getInfo(4380, 1);
		if (skill != null)
		{
			setTarget(player);
			doCast(skill);
		}
		
		player.setCurrentCp(player.getMaxCp());
		// cp restored
		sm = new SystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED);
		sm.addString(player.getName());
		player.sendPacket(sm);
		sm = null;
	}
	
	/**
	 * Add Newbie helper buffs to L2Player according to its level.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the range level in wich player must be to obtain buff</li> <li>If player level is out of range, display a message and return</li> <li>According to player level cast buff</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> Newbie Helper Buff list is define in sql table helper_buff_list</B></FONT><BR>
	 * <BR>
	 * @param player The L2PcInstance that talk with the L2NpcInstance if (!FloodProtector.getInstance().tryPerformAction(player.getObjectId(), FloodProtector.PROTECTED_USEITEM)) return;
	 */
	public void makeSupportMagic(final L2PcInstance player)
	{
		if (player == null)
			return;
		
		// Prevent a cursed weapon weilder of being buffed
		if (player.isCursedWeaponEquiped())
			return;
		
		final int player_level = player.getLevel();
		int lowestLevel = 0;
		int higestLevel = 0;
		
		// Select the player
		setTarget(player);
		
		// Calculate the min and max level between wich the player must be to obtain buff
		if (player.isMageClass())
		{
			lowestLevel = HelperBuffTable.getInstance().getMagicClassLowestLevel();
			higestLevel = HelperBuffTable.getInstance().getMagicClassHighestLevel();
		}
		else
		{
			lowestLevel = HelperBuffTable.getInstance().getPhysicClassLowestLevel();
			higestLevel = HelperBuffTable.getInstance().getPhysicClassHighestLevel();
		}
		
		// If the player is too high level, display a message and return
		if (player_level > higestLevel || !player.isNewbie())
		{
			final String content = "<html><body>Newbie Guide:<br>Only a <font color=\"LEVEL\">novice character of level " + higestLevel + " or less</font> can receive my support magic.<br>Your novice character is the first one that you created and " + "raised in this world.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}
		
		// If the player is too low level, display a message and return
		if (player_level < lowestLevel)
		{
			final String content = "<html><body>Come back here when you have reached level " + lowestLevel + ". I will give you support magic then.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}
		
		L2Skill skill = null;
		// Go through the Helper Buff list define in sql table helper_buff_list and cast skill
		for (final L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable())
		{
			if (helperBuffItem.isMagicClassBuff() == player.isMageClass())
			{
				if (player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel())
				{
					skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
					
					if (skill.getSkillType() == SkillType.SUMMON)
					{
						player.doCast(skill);
					}
					else
					{
						doCast(skill);
					}
				}
			}
			
			if (player.getPet() != null)
			{
				if (helperBuffItem.isMagicClassBuff() == player.isMageClass())
				{
					if (player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel())
					{
						skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
						
						skill.getEffects(this, player.getPet(),false,false,false);
					}
				}
			}
		}
	}
	
	/**
	 * Show chat window.
	 * @param player the player
	 */
	public void showChatWindow(final L2PcInstance player)
	{
		showChatWindow(player, 0);
	}
	
	/**
	 * Returns true if html exists.
	 * @param player the player
	 * @param type the type
	 * @return boolean
	 */
	private boolean showPkDenyChatWindow(final L2PcInstance player, final String type)
	{
		String html = HtmCache.getInstance().getHtm("data/html/" + type + "/" + getNpcId() + "-pk.htm");
		
		if (html != null)
		{
			final NpcHtmlMessage pkDenyMsg = new NpcHtmlMessage(getObjectId());
			pkDenyMsg.setHtml(html);
			player.sendPacket(pkDenyMsg);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		return false;
	}
	
	/*
	 * Returns true if html exists
	 * @param player
	 * @param type
	 * @return boolean
	 */
	/*
	 * private boolean showFlagDenyChatWindow(L2PcInstance player, String type) { String html = HtmCache.getInstance().getHtm("data/html/" + type + "/" + getNpcId() + "-f.htm"); if (html != null) { NpcHtmlMessage pkDenyMsg = new NpcHtmlMessage(getObjectId()); pkDenyMsg.setHtml(html);
	 * player.sendPacket(pkDenyMsg); player.sendPacket(ActionFailed.STATIC_PACKET); html = null; return true; } return false; }
	 */
	
	/**
	 * Open a chat window on client with the text of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li> <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li> <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the
	 * client wait another packet</li><BR>
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param val The number of the page of the L2NpcInstance to display
	 */
	public void showChatWindow(L2PcInstance player, int val)
	{
		// Like L2OFF if char is dead, is sitting, is in trade or is in fakedeath can't speak with npcs
		if (player.isSitting() || player.isDead() || player.isFakeDeath() || player.getActiveTradeList() != null)
			return;
		
		if (player.getKarma() > 0)
		{
			if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2MerchantInstance)
			{
				if (showPkDenyChatWindow(player, "merchant"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && this instanceof L2TeleporterInstance)
			{
				if (showPkDenyChatWindow(player, "teleporter"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && this instanceof L2WarehouseInstance)
			{
				if (showPkDenyChatWindow(player, "warehouse"))
					return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && this instanceof L2FishermanInstance)
			{
				if (showPkDenyChatWindow(player, "fisherman"))
					return;
			}
		}
		
		if (getTemplate().type == "L2Auctioneer" && val == 0)
			return;
		
		final int npcId = getTemplate().npcId;
		
		/* For use with Seven Signs implementation */
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
		
		boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		boolean isCompResultsPeriod = SevenSigns.getInstance().isCompResultsPeriod();
		
		int recruitPeriod = SevenSigns.getInstance().getCurrentPeriod();
		int compWinner = SevenSigns.getInstance().getCabalHighestScore();
		
		int marketId = PowerPackConfig.MARKET_NPC;
		if (npcId == marketId)
		{
			if (npcId == PowerPackConfig.MARKET_NPC)
			{
				Market.getInstance().showMsgWindow(player);
				return;
			}
		}
		
		switch (npcId)
		{
			case 31078:
			case 31079:
			case 31080:
			case 31081:
			case 31082: // Dawn Priests
			case 31083:
			case 31084:
			case 31168:
			case 31692:
			case 31694:
			case 31997:
				switch (playerCabal)
				{
					case SevenSigns.CABAL_DAWN:
						if (isCompResultsPeriod)
							filename += "dawn_priest_5.htm";
						else if (recruitPeriod == 0)
							filename += "dawn_priest_6.htm";
						else if (isSealValidationPeriod)
						{
							if (compWinner == SevenSigns.CABAL_DAWN)
							{
								if (compWinner != sealGnosisOwner)
									filename += "dawn_priest_2c.htm";
								else
									filename += "dawn_priest_2a.htm";
							}
							else if (compWinner == SevenSigns.CABAL_NULL)
								filename += "dawn_priest_2d.htm";
							else
								filename += "dawn_priest_2b.htm";
						}
						else
							filename += "dawn_priest_1b.htm";
						break;
					case SevenSigns.CABAL_DUSK:
						if (isSealValidationPeriod)
							filename += "dawn_priest_3a.htm";
						else
							filename += "dawn_priest_3b.htm";
						break;
					default:
						if (isCompResultsPeriod)
							filename += "dawn_priest_5.htm";
						else if (recruitPeriod == 0)
							filename += "dawn_priest_6.htm";
						else if (isSealValidationPeriod)
						{
							if (compWinner == SevenSigns.CABAL_DAWN)
								filename += "dawn_priest_4.htm";
							else if (compWinner == SevenSigns.CABAL_NULL)
								filename += "dawn_priest_2d.htm";
							else
								filename += "dawn_priest_2b.htm";
						}
						else
							filename += "dawn_priest_1a.htm";
						break;
				}
				break;
			case 31085:
			case 31086:
			case 31087:
			case 31088: // Dusk Priest
			case 31089:
			case 31090:
			case 31091:
			case 31169:
			case 31693:
			case 31695:
			case 31998:
				switch (playerCabal)
				{
					case SevenSigns.CABAL_DUSK:
						if (isCompResultsPeriod)
							filename += "dusk_priest_5.htm";
						else if (recruitPeriod == 0)
							filename += "dusk_priest_6.htm";
						else if (isSealValidationPeriod)
						{
							if (compWinner == SevenSigns.CABAL_DUSK)
							{
								if (compWinner != sealGnosisOwner)
									filename += "dusk_priest_2c.htm";
								else
									filename += "dusk_priest_2a.htm";
							}
							else if (compWinner == SevenSigns.CABAL_NULL)
								filename += "dusk_priest_2d.htm";
							else
								filename += "dusk_priest_2b.htm";
						}
						else
							filename += "dusk_priest_1b.htm";
						break;
					case SevenSigns.CABAL_DAWN:
						if (isSealValidationPeriod)
							filename += "dusk_priest_3a.htm";
						else
							filename += "dusk_priest_3b.htm";
						break;
					default:
						if (isCompResultsPeriod)
							filename += "dusk_priest_5.htm";
						else if (recruitPeriod == 0)
							filename += "dusk_priest_6.htm";
						else if (isSealValidationPeriod)
						{
							if (compWinner == SevenSigns.CABAL_DUSK)
								filename += "dusk_priest_4.htm";
							else if (compWinner == SevenSigns.CABAL_NULL)
								filename += "dusk_priest_2d.htm";
							else
								filename += "dusk_priest_2b.htm";
						}
						else
							filename += "dusk_priest_1a.htm";
						break;
				}
				break;
			case 31095: //
			case 31096: //
			case 31097: //
			case 31098: // Enter Necropolises
			case 31099: //
			case 31100: //
			case 31101: //
			case 31102: //
				if (isSealValidationPeriod)
				{
					if (Config.ALT_REQUIRE_WIN_7S)
					{
						if (playerCabal != compWinner || sealAvariceOwner != compWinner)
						{
							switch (compWinner)
							{
								case SevenSigns.CABAL_DAWN:
									player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
									filename += "necro_no.htm";
									break;
								case SevenSigns.CABAL_DUSK:
									player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
									filename += "necro_no.htm";
									break;
								case SevenSigns.CABAL_NULL:
									filename = getHtmlPath(player, npcId, val); // do the default!
									break;
							}
						}
						else
						{
							filename = getHtmlPath(player, npcId, val); // do the default!
						}
					}
					else
					{
						filename = getHtmlPath(player, npcId, val); // do the default!
					}
				}
				else
				{
					if (playerCabal == SevenSigns.CABAL_NULL)
					{
						filename += "necro_no.htm";
					}
					else
					{
						filename = getHtmlPath(player, npcId, val); // do the default!
					}
				}
				break;
			case 31114: //
			case 31115: //
			case 31116: // Enter Catacombs
			case 31117: //
			case 31118: //
			case 31119: //
				if (isSealValidationPeriod)
				{
					if (Config.ALT_REQUIRE_WIN_7S)
					{
						if (playerCabal != compWinner || sealGnosisOwner != compWinner)
						{
							switch (compWinner)
							{
								case SevenSigns.CABAL_DAWN:
									player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
									filename += "cata_no.htm";
									break;
								case SevenSigns.CABAL_DUSK:
									player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
									filename += "cata_no.htm";
									break;
								case SevenSigns.CABAL_NULL:
									filename = getHtmlPath(player, npcId, val); // do the default!
									break;
							}
						}
						else
						{
							filename = getHtmlPath(player, npcId, val); // do the default!
						}
					}
					else
					{
						filename = getHtmlPath(player, npcId, val); // do the default!
					}
				}
				else
				{
					if (playerCabal == SevenSigns.CABAL_NULL)
					{
						filename += "cata_no.htm";
					}
					else
					{
						filename = getHtmlPath(player, npcId, val); // do the default!
					}
				}
				break;
			case 31111: // Gatekeeper Spirit (Disciples)
				if (playerCabal == sealAvariceOwner && playerCabal == compWinner)
				{
					switch (sealAvariceOwner)
					{
						case SevenSigns.CABAL_DAWN:
							filename += "spirit_dawn.htm";
							break;
						case SevenSigns.CABAL_DUSK:
							filename += "spirit_dusk.htm";
							break;
						case SevenSigns.CABAL_NULL:
							filename += "spirit_null.htm";
							break;
					}
				}
				else
				{
					filename += "spirit_null.htm";
				}
				break;
			case 31112: // Gatekeeper Spirit (Disciples)
				filename += "spirit_exit.htm";
				break;
			case 31127: //
			case 31128: //
			case 31129: // Dawn Festival Guides
			case 31130: //
			case 31131: //
				filename += "festival/dawn_guide.htm";
				break;
			case 31137: //
			case 31138: //
			case 31139: // Dusk Festival Guides
			case 31140: //
			case 31141: //
				filename += "festival/dusk_guide.htm";
				break;
			case 31092: // Black Marketeer of Mammon
				filename += "blkmrkt_1.htm";
				break;
			case 31113: // Merchant of Mammon
				if (Config.ALT_REQUIRE_WIN_7S)
				{
					switch (compWinner)
					{
						case SevenSigns.CABAL_DAWN:
							if (playerCabal != compWinner || playerCabal != sealAvariceOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
								return;
							}
							break;
						case SevenSigns.CABAL_DUSK:
							if (playerCabal != compWinner || playerCabal != sealAvariceOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
								return;
							}
							break;
					}
				}
				filename += "mammmerch_1.htm";
				break;
			case 31126: // Blacksmith of Mammon
				if (Config.ALT_REQUIRE_WIN_7S)
				{
					switch (compWinner)
					{
						case SevenSigns.CABAL_DAWN:
							if (playerCabal != compWinner || playerCabal != sealGnosisOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
								return;
							}
							break;
						case SevenSigns.CABAL_DUSK:
							if (playerCabal != compWinner || playerCabal != sealGnosisOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
								return;
							}
							break;
					}
				}
				filename += "mammblack_1.htm";
				break;
			case 31132:
			case 31133:
			case 31134:
			case 31135:
			case 31136: // Festival Witches
			case 31142:
			case 31143:
			case 31144:
			case 31145:
			case 31146:
				filename += "festival/festival_witch.htm";
				break;
			case 31688:
				if (player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
				}
				else
				{
					filename = getHtmlPath(player, npcId, val);
				}
				break;
			// fixed Monument of Heroes HTML for noble characters like L2OFF
			case 31690:
				if (player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				}
				else
				{
					filename = getHtmlPath(player, npcId, val);
				}
				break;
			case 31769:
			case 31770:
			case 31771:
			case 31772:
				if (player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				}
				else
				{
					filename = getHtmlPath(player, npcId, val);
				}
				break;
			default:
				if (npcId >= 31865 && npcId <= 31918)
				{
					filename += "rift/GuardianOfBorder.htm";
					break;
				}
				if (npcId >= 31093 && npcId <= 31094 || npcId >= 31172 && npcId <= 31201 || npcId >= 31239 && npcId <= 31254)
					return;
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = getHtmlPath(player, npcId, val);
				break;
		}
		
		if (this instanceof L2CastleTeleporterInstance)
		{
			((L2CastleTeleporterInstance) this).showChatWindow(player);
			return;
		}
		
		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		
		html.setFile(filename);
		
		if (this instanceof L2MerchantInstance)
		{
			if (Config.LIST_PET_RENT_NPC.contains(npcId))
			{
				html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");
			}
		}
		html.replace("%playername%", player.getName());
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStart());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Open a chat window on client with the text specified by the given file name and path,<BR>
	 * relative to the datapack root. <BR>
	 * <BR>
	 * Added by Tempy
	 * @param player The L2PcInstance that talk with the L2NpcInstance
	 * @param filename The filename that contains the text to send
	 */
	public void showChatWindow(L2PcInstance player, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public int getExpReward()
	{
		final double rateXp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		
		return (int) (getTemplate().rewardExp * rateXp * Config.RATE_XP);
	}
	
	public int getSpReward()
	{
		final double rateSp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		
		return (int) (getTemplate().rewardSp * rateSp * Config.RATE_SP);
	}
	
	public int getExpRewardCustom()
	{
		final double rateXp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		
		return (int) (getTemplate().rewardExp * rateXp * 1);
	}
	
	public int getSpRewardCustom()
	{
		final double rateSp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		
		return (int) (getTemplate().rewardSp * rateSp * 1);
	}
	
	/**
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create a DecayTask to remove the corpse of the L2NpcInstance after 7 seconds</li> <li>Set target to null and cancel Attack or Cast</li> <li>Stop movement</li> <li>Stop HP/MP/CP Regeneration task</li> <li>Stop all active skills effects in progress on the L2Character</li> <li>Send the
	 * Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform</li> <li>Notify L2Character AI</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2Attackable</li><BR>
	 * <BR>
	 * @param killer The L2Character who killed it
	 * @return true, if successful
	 */
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// normally this wouldn't really be needed, but for those few exceptions,
		// we do need to reset the weapons back to the initial templated weapon.
		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		_currentCollisionHeight = getTemplate().collisionHeight;
		_currentCollisionRadius = getTemplate().collisionRadius;
		
		DecayTaskManager.getInstance().addDecayTask(this);
		
		return true;
	}
	
	/**
	 * Set the spawn of the L2NpcInstance.<BR>
	 * <BR>
	 * @param spawn The L2Spawn that manage the L2NpcInstance
	 */
	public void setSpawn(final L2Spawn spawn)
	{
		_spawn = spawn;
		// Does this Npc morph into a PcInstance?
		if (_spawn != null)
		{
			if (CustomNpcInstanceManager.getInstance().isThisL2CustomNpcInstance(_spawn.getId(), getNpcId()))
			{
				new L2CustomNpcInstance(this);
			}
		}
	}
	
	/**
	 * Remove the L2NpcInstance from the world and update its spawn object (for a complete removal use the deleteMe method).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2NpcInstance from the world when the decay task is launched</li> <li>Decrease its spawn counter</li> <li>Manage Siege task (killFlag, killCT)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void onDecay()
	{
		if (isDecayed())
			return;
		
		setDecayed(true);
		
		// Manage Life Control Tower
		if (this instanceof L2ControlTowerInstance)
		{
			((L2ControlTowerInstance) this).onDeath();
		}
		
		// Remove the L2NpcInstance from the world when the decay task is launched
		super.onDecay();
		
		// Decrease its spawn counter
		if (_spawn != null)
		{
			_spawn.decreaseCount(this);
		}
	}
	
	/**
	 * Remove PROPERLY the L2NpcInstance from the world.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2NpcInstance from the world and update its spawn object</li> <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2NpcInstance then cancel Attak or Cast and notify AI</li> <li>Remove L2Object object from _allObjects of L2World</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 */
	
	@Override
	public void deleteMe()
	{
		try
		{
			decayMe();
		}
		catch (Throwable t)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				t.printStackTrace();
			
			LOG.error("deletedMe(): " + t);
		}
		
		final L2WorldRegion region = getWorldRegion();
		if (region != null)
		{
			region.removeFromZones(this);
		}
		
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Throwable t)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				t.printStackTrace();
			
			LOG.error("deletedMe(): " + t);
		}
		
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
		
		super.deleteMe();
	}
	
	public void Delete()
	{
		try
		{
			onDecay();
		}
		catch (Throwable t)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				t.printStackTrace();
			
			LOG.error("deletedMe(): " + t);
		}
		
		final L2WorldRegion region = getWorldRegion();
		
		if (region != null)
		{
			region.removeFromZones(this);
		}
		
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Throwable t)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				t.printStackTrace();
			
			LOG.error("deletedMe(): " + t);
		}
		
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
	}
	
	/**
	 * Return the L2Spawn object that manage this L2NpcInstance.<BR>
	 * <BR>
	 * @return the spawn
	 */
	public L2Spawn getSpawn()
	{
		return _spawn;
	}
	
	@Override
	public String toString()
	{
		return getTemplate().name;
	}
	
	/**
	 * Checks if is decayed.
	 * @return true, if is decayed
	 */
	public boolean isDecayed()
	{
		return _isDecayed;
	}
	
	/**
	 * Sets the decayed.
	 * @param decayed the new decayed
	 */
	public void setDecayed(final boolean decayed)
	{
		_isDecayed = decayed;
	}
	
	/**
	 * End decay task.
	 */
	public void endDecayTask()
	{
		if (!isDecayed())
		{
			DecayTaskManager.getInstance().cancelDecayTask(this);
			onDecay();
		}
	}
	
	/**
	 * Checks if is mob.
	 * @return true, if is mob
	 */
	public boolean isMob() // rather delete this check
	{
		return false; // This means we use MAX_NPC_ANIMATION instead of MAX_MONSTER_ANIMATION
	}
	
	// Two functions to change the appearance of the equipped weapons on the NPC
	// This is only useful for a few NPCs and is most likely going to be called from AI
	/**
	 * Sets the l hand id.
	 * @param newWeaponId the new l hand id
	 */
	public void setLHandId(final int newWeaponId)
	{
		_currentLHandId = newWeaponId;
	}
	
	/**
	 * Sets the r hand id.
	 * @param newWeaponId the new r hand id
	 */
	public void setRHandId(final int newWeaponId)
	{
		_currentRHandId = newWeaponId;
	}
	
	/**
	 * Sets the collision height.
	 * @param height the new collision height
	 */
	public void setCollisionHeight(final int height)
	{
		_currentCollisionHeight = height;
	}
	
	/**
	 * Sets the collision radius.
	 * @param radius the new collision radius
	 */
	public void setCollisionRadius(final int radius)
	{
		_currentCollisionRadius = radius;
	}
	
	/**
	 * Gets the collision height.
	 * @return the collision height
	 */
	public int getCollisionHeight()
	{
		return _currentCollisionHeight;
	}
	
	/**
	 * Gets the collision radius.
	 * @return the collision radius
	 */
	public int getCollisionRadius()
	{
		return _currentCollisionRadius;
	}
	
	/**
	 * Gets the custom npc instance.
	 * @return the custom npc instance
	 */
	public L2CustomNpcInstance getCustomNpcInstance()
	{
		return _customNpcInstance;
	}
	
	/**
	 * Sets the custom npc instance.
	 * @param arg the new custom npc instance
	 */
	public void setCustomNpcInstance(final L2CustomNpcInstance arg)
	{
		_customNpcInstance = arg;
	}
	
	public AIType getAiType()
	{
		return _staticAIData.getAiType();
	}
	
	public void broadcastNpcSay(String message)
	{
		broadcastPacket(new NpcSay(getObjectId(), Say2.ALL, getNpcId(), message));
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (getRunSpeed() == 0)
		{
			activeChar.sendPacket(new ServerObjectInfo(this, activeChar));
		}
		else
		{
			activeChar.sendPacket(new NpcInfo(this, activeChar));
		}
	}
}
