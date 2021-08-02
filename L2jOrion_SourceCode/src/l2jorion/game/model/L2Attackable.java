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

import java.util.ArrayList;
import java.util.List;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2AttackableAI;
import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2SiegeGuardAI;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2MinionInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.actor.instance.L2SummonInstance;
import l2jorion.game.model.actor.knownlist.AttackableKnownList;
import l2jorion.game.model.actor.status.AttackableStatus;
import l2jorion.game.model.base.SoulCrystal;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.script.EventDroplist;
import l2jorion.game.script.EventDroplist.DateDrop;
import l2jorion.game.skills.Stats;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.thread.daemons.ItemsAutoDestroy;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class L2Attackable extends L2NpcInstance
{
	public static Logger LOG = LoggerFactory.getLogger(L2Attackable.class);
	
	private boolean _isRaid = false;
	private boolean _isRaidMinion = false;
	
	private boolean _isBossInstance = false;
	
	public final class AggroInfo
	{
		protected L2Character _attacker;
		
		protected int _hate;
		
		protected int _damage;
		
		AggroInfo(L2Character pAttacker)
		{
			_attacker = pAttacker;
		}
		
		public final L2Character getAttacker()
		{
			return _attacker;
		}
		
		public final int checkHate(L2Character owner)
		{
			if (_attacker.isAlikeDead() || !_attacker.isVisible() || !owner.getKnownList().knowsObject(_attacker))
			{
				_hate = 0;
			}
			
			return _hate;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			
			if (obj instanceof AggroInfo)
			{
				return ((AggroInfo) obj)._attacker == _attacker;
			}
			
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}
	}
	
	/**
	 * This class contains all RewardInfo of the L2Attackable against the any attacker L2Character, based on amount of damage done.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B><BR>
	 * <BR>
	 * <li>attacker : The attacker L2Character concerned by this RewardInfo of this L2Attackable</li>
	 * <li>dmg : Total amount of damage done by the attacker to this L2Attackable (summon + own)</li>
	 */
	protected final class RewardInfo
	{
		protected L2Character _attacker;
		protected int _dmg = 0;
		
		public RewardInfo(L2Character pAttacker, int pDmg)
		{
			_attacker = pAttacker;
			_dmg = pDmg;
		}
		
		public void addDamage(int pDmg)
		{
			_dmg += pDmg;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			
			if (obj instanceof RewardInfo)
			{
				return ((RewardInfo) obj)._attacker == _attacker;
			}
			
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}
	}
	
	public final class AbsorberInfo
	{
		protected L2PcInstance _absorber;
		protected int _crystalId;
		protected double _absorbedHP;
		
		AbsorberInfo(L2PcInstance attacker, int pCrystalId, double pAbsorbedHP)
		{
			_absorber = attacker;
			_crystalId = pCrystalId;
			_absorbedHP = pAbsorbedHP;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			
			if (obj instanceof AbsorberInfo)
			{
				return ((AbsorberInfo) obj)._absorber == _absorber;
			}
			
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return _absorber.getObjectId();
		}
	}
	
	public final class RewardItem
	{
		protected int _itemId;
		protected int _count;
		protected int _enchantLevel;
		
		public RewardItem(int itemId, int count, int enchantLevel)
		{
			_itemId = itemId;
			_count = count;
			_enchantLevel = enchantLevel;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public int getCount()
		{
			return _count;
		}
		
		public int getEnchantLevel()
		{
			return _enchantLevel;
		}
	}
	
	private FastMap<L2Character, AggroInfo> _aggroList = new FastMap<L2Character, AggroInfo>().shared();
	
	public final FastMap<L2Character, AggroInfo> getAggroListRP()
	{
		return _aggroList;
	}
	
	public final FastMap<L2Character, AggroInfo> getAggroList()
	{
		return _aggroList;
	}
	
	private boolean _isReturningToSpawnPoint = false;
	
	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}
	
	public final void setisReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}
	
	private RewardItem[] _sweepItems;
	
	private RewardItem[] _harvestItems;
	private boolean _seeded;
	private int _seedType = 0;
	private L2PcInstance _seeder = null;
	
	private boolean _overhit;
	private double _overhitDamage;
	private L2Character _overhitAttacker;
	
	private volatile L2CommandChannel _firstCommandChannelAttacked = null;
	private CommandChannelTimer _commandChannelTimer = null;
	private long _commandChannelLastAttack = 0;
	
	private boolean _absorbed;
	private FastMap<L2PcInstance, AbsorberInfo> _absorbersList = new FastMap<L2PcInstance, AbsorberInfo>().shared();
	
	private boolean _mustGiveExpSp;
	
	public L2Character _mostHated;
	
	public L2Attackable(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
		_mustGiveExpSp = true;
	}
	
	@Override
	public AttackableKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof AttackableKnownList))
		{
			setKnownList(new AttackableKnownList(this));
		}
		
		return (AttackableKnownList) super.getKnownList();
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2AttackableAI(this);
	}
	
	@Override
	public AttackableStatus getStatus()
	{
		if (super.getStatus() == null || !(super.getStatus() instanceof AttackableStatus))
		{
			setStatus(new AttackableStatus(this));
		}
		
		return (AttackableStatus) super.getStatus();
	}
	
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker)
	{
		reduceCurrentHp(damage, attacker, true);
	}
	
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
	{
		if (isRaid() && !isMinion() && attacker != null && attacker.getParty() != null && attacker.getParty().isInCommandChannel() && attacker.getParty().getCommandChannel().meetRaidWarCondition(this))
		{
			if (_firstCommandChannelAttacked == null) // looting right isn't set
			{
				synchronized (this)
				{
					if (_firstCommandChannelAttacked == null)
					{
						_firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
						if (_firstCommandChannelAttacked != null)
						{
							_commandChannelTimer = new CommandChannelTimer(this);
							_commandChannelLastAttack = System.currentTimeMillis();
							ThreadPoolManager.getInstance().scheduleGeneral(_commandChannelTimer, 10000); // check for last attack
							
							_firstCommandChannelAttacked.broadcastToChannelMembers(new CreatureSay(0, Say2.PARTYROOM_ALL, "", "You have looting rights!")); // TODO: retail msg
						}
					}
				}
			}
			else if (attacker.getParty().getCommandChannel().equals(_firstCommandChannelAttacked)) // is in same channel
			{
				_commandChannelLastAttack = System.currentTimeMillis(); // update last attack time
			}
		}
		
		if (isEventMob)
		{
			return;
		}
		
		// Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList
		if (attacker != null)
		{
			addDamage(attacker, (int) damage);
		}
		
		// If this L2Attackable is a L2MonsterInstance and it has spawned minions, call its minions to battle
		if (this instanceof L2MonsterInstance)
		{
			L2MonsterInstance master = (L2MonsterInstance) this;
			
			if (this instanceof L2MinionInstance)
			{
				master = ((L2MinionInstance) this).getLeader();
				
				if (!master.isInCombat() && !master.isDead())
				{
					master.addDamage(attacker, 1);
				}
			}
			
			if (master.hasMinions())
			{
				master.callMinionsToAssist(attacker);
			}
		}
		
		super.reduceCurrentHp(damage, attacker, awake);
	}
	
	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}
	
	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp;
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		L2PcInstance attacker = null;
		if (killer instanceof L2PcInstance || killer instanceof L2Summon)
		{
			attacker = killer instanceof L2Summon ? ((L2Summon) killer).getOwner() : (L2PcInstance) killer;
		}
		
		if (attacker != null)
		{
			// Enhance soul crystals of the attacker if this L2Attackable had its soul absorbed
			levelSoulCrystals(attacker);
			
			for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL))
			{
				quest.notifyKill(this, attacker, killer instanceof L2Summon);
			}
		}
		
		if (Config.L2JMOD_CHAMPION_ENABLE && !isRaid() && !isRaidMinion())
		{
			setChampion(false);
			if (this instanceof L2MonsterInstance && Config.L2JMOD_CHAMPION_FREQUENCY > 0 && getLevel() >= Config.L2JMOD_CHAMP_MIN_LVL && getLevel() <= Config.L2JMOD_CHAMP_MAX_LVL)
			{
				int random = Rnd.get(100);
				if (random < Config.L2JMOD_CHAMPION_FREQUENCY)
				{
					setChampion(true);
				}
			}
		}
		return true;
	}
	
	class OnKillNotifyTask implements Runnable
	{
		private L2Attackable _attackable;
		private Quest _quest;
		private L2PcInstance _killer;
		private boolean _isPet;
		
		public OnKillNotifyTask(L2Attackable attackable, Quest quest, L2PcInstance killer, boolean isPet)
		{
			_attackable = attackable;
			_quest = quest;
			_killer = killer;
			_isPet = isPet;
		}
		
		@Override
		public void run()
		{
			_quest.notifyKill(_attackable, _killer, _isPet);
		}
	}
	
	/**
	 * Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the L2PcInstance owner of the L2SummonInstance (if necessary) and L2Party in progress</li>
	 * <li>Calculate the Experience and SP rewards in function of the level difference</li>
	 * <li>Add Exp and SP rewards to L2PcInstance (including Summon penalty) and to Party members in the known area of the last attacker</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR>
	 * <BR>
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	@Override
	protected void calculateRewards(L2Character lastAttacker)
	{
		// Creates an empty list of rewards
		FastMap<L2Character, RewardInfo> rewards = new FastMap<L2Character, RewardInfo>().shared();
		
		try
		{
			if (getAggroListRP().isEmpty())
			{
				return;
			}
			
			L2PcInstance maxDealer = null;
			int maxDamage = 0;
			
			int damage;
			
			L2Character attacker, ddealer;
			
			// While Interacting over This Map Removing Object is Not Allowed
			synchronized (getAggroList())
			{
				// Go through the _aggroList of the L2Attackable
				for (AggroInfo info : getAggroListRP().values())
				{
					if (info == null)
					{
						continue;
					}
					
					// Get the L2Character corresponding to this attacker
					attacker = info._attacker;
					
					// Get damages done by this attacker
					damage = info._damage;
					
					// Prevent unwanted behavior
					if (damage > 1)
					{
						if (attacker instanceof L2SummonInstance || attacker instanceof L2PetInstance && ((L2PetInstance) attacker).getPetData().getOwnerExpTaken() > 0)
						{
							ddealer = ((L2Summon) attacker).getOwner();
						}
						else
						{
							ddealer = info._attacker;
						}
						
						// Check if ddealer isn't too far from this (killed monster)
						if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, ddealer, true))
						{
							continue;
						}
						
						// Calculate real damages (Summoners should get own damage plus summon's damage)
						RewardInfo reward = rewards.get(ddealer);
						if (reward == null)
						{
							reward = new RewardInfo(ddealer, damage);
						}
						else
						{
							reward.addDamage(damage);
						}
						
						rewards.put(ddealer, reward);
						
						if (ddealer instanceof L2PlayableInstance && ((L2PlayableInstance) ddealer).getActingPlayer() != null && reward._dmg > maxDamage)
						{
							maxDealer = ((L2PlayableInstance) ddealer).getActingPlayer();
							maxDamage = reward._dmg;
						}
						
					}
				}
			}
			
			// Manage Base, Quests and Sweep drops of the L2Attackable
			doItemDrop(maxDealer != null && maxDealer.isOnline() == 1 ? maxDealer : lastAttacker);
			
			// Manage drop of Special Events created by GM for a defined period
			doEventDrop(maxDealer != null && maxDealer.isOnline() == 1 ? maxDealer : lastAttacker);
			
			if (!getMustRewardExpSP())
			{
				return;
			}
			
			if (!rewards.isEmpty())
			{
				L2Party attackerParty;
				long exp;
				int levelDiff, partyDmg, partyLvl, sp;
				float partyMul, penalty;
				RewardInfo reward2;
				int[] tmp;
				
				for (RewardInfo reward : rewards.values())
				{
					if (reward == null)
					{
						continue;
					}
					
					// Penalty applied to the attacker's XP
					penalty = 0;
					
					// Attacker to be rewarded
					attacker = reward._attacker;
					
					// Total amount of damage done
					damage = reward._dmg;
					
					// If the attacker is a Pet, get the party of the owner
					if (attacker instanceof L2PetInstance)
					{
						attackerParty = ((L2PetInstance) attacker).getParty();
					}
					else if (attacker instanceof L2PcInstance)
					{
						attackerParty = ((L2PcInstance) attacker).getParty();
					}
					else
					{
						return;
					}
					
					// If this attacker is a L2PcInstance with a summoned L2SummonInstance, get Exp Penalty applied for the current summoned L2SummonInstance
					if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getPet() instanceof L2SummonInstance)
					{
						penalty = ((L2SummonInstance) ((L2PcInstance) attacker).getPet()).getExpPenalty();
					}
					
					// We must avoid "over damage", if any
					if (damage > getMaxHp())
					{
						damage = getMaxHp();
					}
					
					// If there's NO party in progress
					if (attackerParty == null)
					{
						// Calculate Exp and SP rewards
						if (attacker.getKnownList().knowsObject(this))
						{
							// Calculate the difference of level between this attacker (L2PcInstance or L2SummonInstance owner) and the L2Attackable
							// mob = 24, atk = 10, diff = -14 (full xp)
							// mob = 24, atk = 28, diff = 4 (some xp)
							// mob = 24, atk = 50, diff = 26 (no xp)
							levelDiff = attacker.getLevel() - getLevel();
							
							tmp = calculateExpAndSp(levelDiff, damage);
							
							if (Config.EXPLLOSIVE_CUSTOM && attacker.getLevel() >= 79)
							{
								tmp = calculateExpAndSpCustom(levelDiff, damage);
							}
							
							exp = tmp[0];
							exp *= 1 - penalty;
							sp = tmp[1];
							
							if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
							{
								exp *= Config.L2JMOD_CHAMPION_REWARDS;
								sp *= Config.L2JMOD_CHAMPION_REWARDS;
							}
							
							// Check for an over-hit enabled strike and Donator options
							if (attacker instanceof L2PcInstance)
							{
								L2Character overhitAttacker = getOverhitAttacker();
								if (isOverhit() && (overhitAttacker != null) && (overhitAttacker.getActingPlayer() != null) && (attacker == overhitAttacker.getActingPlayer()))
								{
									attacker.sendPacket(new SystemMessage(SystemMessageId.OVER_HIT));
									exp += calculateOverhitExp(exp);
								}
								
								if (attacker.getPremiumService() == 1)
								{
									exp = (long) (exp * Config.PREMIUM_XPSP_RATE);
									sp = (int) (sp * Config.PREMIUM_XPSP_RATE);
								}
							}
							
							// Distribute the Exp and SP between the L2PcInstance and its L2Summon
							if (!attacker.isDead())
							{
								attacker.addExpAndSp(Math.round(attacker.calcStat(Stats.EXPSP_RATE, exp, null, null)), (int) attacker.calcStat(Stats.EXPSP_RATE, sp, null, null));
							}
						}
					}
					else
					{
						// share with party members
						partyDmg = 0;
						partyMul = 1.f;
						partyLvl = 0;
						
						// Get all L2Character that can be rewarded in the party
						List<L2PlayableInstance> rewardedMembers = new FastList<>();
						
						// Go through all L2PcInstance in the party
						List<L2PcInstance> groupMembers;
						
						if (attackerParty.isInCommandChannel())
						{
							groupMembers = attackerParty.getCommandChannel().getMembers();
						}
						else
						{
							groupMembers = attackerParty.getPartyMembers();
						}
						
						for (L2PcInstance pl : groupMembers)
						{
							if (pl == null || pl.isDead())
							{
								continue;
							}
							
							// Get the RewardInfo of this L2PcInstance from L2Attackable rewards
							reward2 = rewards.get(pl);
							
							// If the L2PcInstance is in the L2Attackable rewards add its damages to party damages
							if (reward2 != null)
							{
								if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, pl, true))
								{
									partyDmg += reward2._dmg; // Add L2PcInstance damages to party damages
									rewardedMembers.add(pl);
									
									if (pl.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = pl.getLevel();
										}
									}
								}
								
								rewards.remove(pl); // Remove the L2PcInstance from the L2Attackable rewards
							}
							else
							{
								// Add L2PcInstance of the party (that have attacked or not) to members that can be rewarded
								// and in range of the monster.
								if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, pl, true))
								{
									rewardedMembers.add(pl);
									
									if (pl.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = pl.getLevel();
										}
									}
								}
							}
							
							L2PlayableInstance summon = pl.getPet();
							if (summon != null && summon instanceof L2PetInstance)
							{
								reward2 = rewards.get(summon);
								
								if (reward2 != null) // Pets are only added if they have done damage
								{
									if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, summon, true))
									{
										partyDmg += reward2._dmg; // Add summon damages to party damages
										rewardedMembers.add(summon);
										
										if (summon.getLevel() > partyLvl)
										{
											partyLvl = summon.getLevel();
										}
									}
									rewards.remove(summon); // Remove the summon from the L2Attackable rewards
								}
							}
						}
						
						// If the party didn't killed this L2Attackable alone
						if (partyDmg < getMaxHp())
						{
							partyMul = (float) partyDmg / (float) getMaxHp();
						}
						
						// Avoid "over damage"
						if (partyDmg > getMaxHp())
						{
							partyDmg = getMaxHp();
						}
						
						// Calculate the level difference between Party and L2Attackable
						levelDiff = partyLvl - getLevel();
						
						// Calculate Exp and SP rewards
						tmp = calculateExpAndSp(levelDiff, partyDmg);
						
						if (Config.EXPLLOSIVE_CUSTOM && attacker.getLevel() >= 79)
						{
							tmp = calculateExpAndSpCustom(levelDiff, partyDmg);
						}
						
						exp = tmp[0];
						sp = tmp[1];
						
						if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
						{
							exp *= Config.L2JMOD_CHAMPION_REWARDS;
							sp *= Config.L2JMOD_CHAMPION_REWARDS;
						}
						
						exp *= partyMul;
						sp *= partyMul;
						
						// Check for an over-hit enabled strike
						// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members)
						if (attacker instanceof L2PcInstance)
						{
							L2Character overhitAttacker = getOverhitAttacker();
							if (isOverhit() && (overhitAttacker != null) && (overhitAttacker.getActingPlayer() != null) && (attacker == overhitAttacker.getActingPlayer()))
							{
								attacker.sendPacket(new SystemMessage(SystemMessageId.OVER_HIT));
								exp += calculateOverhitExp(exp);
							}
						}
						
						// Distribute Experience and SP rewards to L2PcInstance Party members in the known area of the last attacker
						if (partyDmg > 0)
						{
							attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOG.info("", e);
		}
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.<BR>
	 * <BR>
	 * @param attacker The L2Character that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker L2Character
	 */
	public void addDamage(L2Character attacker, int damage)
	{
		addDamageHate(attacker, damage, damage);
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.<BR>
	 * <BR>
	 * @param attacker The L2Character that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker L2Character
	 * @param aggro The hate (=damage) given by the attacker L2Character
	 */
	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Get the AggroInfo of the attacker L2Character from the _aggroList of the L2Attackable
		AggroInfo ai = getAggroListRP().get(attacker);
		
		if (ai == null)
		{
			ai = new AggroInfo(attacker);
			ai._damage = 0;
			ai._hate = 0;
			getAggroListRP().put(attacker, ai);
		}
		
		// If aggro is negative, its comming from SEE_SPELL, buffs use constant 150
		if (aggro < 0)
		{
			ai._hate -= aggro * 150 / (getLevel() + 7);
			aggro = -aggro;
		}
		// if damage == 0 -> this is case of adding only to aggro list, dont apply formula on it
		else if (damage == 0)
		{
			ai._hate += aggro;
			// else its damage that must be added using constant 100
		}
		else
		{
			ai._hate += aggro * 100 / (getLevel() + 7);
		}
		
		// Add new damage and aggro (=damage) to the AggroInfo object
		ai._damage += damage;
		
		if (aggro == 0)
		{
			final L2PcInstance targetPlayer = attacker.getActingPlayer();
			
			if (targetPlayer != null)
			{
				if (getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER) != null)
				{
					for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER))
					{
						quest.notifyAggroRangeEnter(this, targetPlayer, (attacker instanceof L2Summon));
					}
				}
			}
			else
			{
				aggro = 1;
				ai._hate = 1;
			}
		}
		else
		{
			// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
			if (getAI() != null && aggro > 0 && getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			}
		}
		
		// Notify the L2Attackable AI with EVT_ATTACKED
		if (damage > 0)
		{
			if (getAI() != null)
			{
				getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker);
			}
			
			try
			{
				if (attacker instanceof L2PcInstance || attacker instanceof L2Summon)
				{
					L2PcInstance player = attacker instanceof L2PcInstance ? (L2PcInstance) attacker : ((L2Summon) attacker).getOwner();
					
					for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
					{
						quest.notifyAttack(this, player, damage, attacker instanceof L2Summon);
					}
				}
			}
			catch (Exception e)
			{
				LOG.error("", e);
			}
		}
	}
	
	public void reduceHate(L2Character target, int amount)
	{
		if (getAI() instanceof L2SiegeGuardAI)
		{
			stopHating(target);
			setTarget(null);
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
			return;
		}
		
		if (target == null) // whole aggrolist
		{
			L2Character mostHated = getMostHated();
			
			if (mostHated == null) // makes target passive for a moment more
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}
			for (L2Character aggroed : getAggroListRP().keySet())
			{
				AggroInfo ai = getAggroListRP().get(aggroed);
				if (ai == null)
				{
					return;
				}
				
				ai._hate -= amount;
			}
			
			amount = getHating(mostHated);
			if (amount <= 0)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
			return;
		}
		
		AggroInfo ai = getAggroListRP().get(target);
		
		if (ai == null)
		{
			return;
		}
		
		ai._hate -= amount;
		
		if (ai._hate <= 0)
		{
			if (getMostHated() == null)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
		}
	}
	
	/**
	 * Clears _aggroList hate of the L2Character without removing from the list.<BR>
	 * <BR>
	 * @param target
	 */
	public void stopHating(L2Character target)
	{
		if (target == null)
		{
			return;
		}
		
		AggroInfo ai = getAggroListRP().get(target);
		
		if (ai == null)
		{
			return;
		}
		ai._hate = 0;
	}
	
	/**
	 * Return the most hated L2Character of the L2Attackable _aggroList.<BR>
	 * <BR>
	 * @return
	 */
	public L2Character getMostHated()
	{
		if (getAggroListRP().isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		L2Character mostHated = null;
		
		long maxHate = 0;
		
		// While Interating over This Map Removing Object is Not Allowed
		synchronized (getAggroList())
		{
			// Go through the aggroList of the L2Attackable
			for (AggroInfo ai : getAggroListRP().values())
			{
				if (ai == null)
				{
					continue;
				}
				
				if (ai._attacker.isAlikeDead() || !getKnownList().knowsObject(ai._attacker) || !ai._attacker.isVisible() || ai._attacker instanceof L2PcInstance && ((L2PcInstance) ai._attacker).isInOfflineMode())
				{
					ai._hate = 0;
				}
				
				if (ai._hate > maxHate)
				{
					mostHated = ai._attacker;
					maxHate = ai._hate;
				}
			}
		}
		
		return mostHated;
	}
	
	/**
	 * Return the hate level of the L2Attackable against this L2Character contained in _aggroList.<BR>
	 * <BR>
	 * @param target The L2Character whose hate level must be returned
	 * @return
	 */
	public int getHating(L2Character target)
	{
		if (getAggroListRP().isEmpty())
		{
			return 0;
		}
		
		if (target == null)
		{
			return 0;
		}
		
		AggroInfo ai = getAggroListRP().get(target);
		
		if (ai == null)
		{
			return 0;
		}
		
		if (ai._attacker instanceof L2PcInstance && (((L2PcInstance) ai._attacker).getAppearance().getInvisible() || ((L2PcInstance) ai._attacker).isSpawnProtected() || ((L2PcInstance) ai._attacker).isTeleportProtected() || ai._attacker.isInvul()))
		{
			getAggroList().remove(target);
			return 0;
		}
		
		if (!ai._attacker.isVisible())
		{
			getAggroList().remove(target);
			return 0;
		}
		
		if (ai._attacker.isAlikeDead())
		{
			ai._hate = 0;
			return 0;
		}
		return ai._hate;
	}
	
	/**
	 * Calculates quantity of items for specific drop according to current situation <br>
	 * @param drop The L2DropData count is being calculated for
	 * @param lastAttacker The L2PcInstance that has killed the L2Attackable
	 * @param levelModifier level modifier in %'s (will be subtracted from drop chance)
	 * @param isSweep
	 * @return
	 */
	private RewardItem calculateRewardItem(L2PcInstance lastAttacker, L2DropData drop, int levelModifier, boolean isSweep)
	{
		float dropChance = drop.getChance();
		int deepBlueDrop = 1;
		
		if (Config.DEEPBLUE_DROP_RULES)
		{
			if (levelModifier > 0)
			{
				// We should multiply by the server's drop rate, so we always get a low chance of drop for deep blue mobs.
				// NOTE: This is valid only for adena drops! Others drops will still obey server's rate
				deepBlueDrop = 3;
				
				if (drop.getItemId() == 57)
				{
					deepBlueDrop *= isRaid() && !isRaidMinion() ? 1 : Config.RATE_DROP_ITEMS;
				}
			}
		}
		
		if (deepBlueDrop == 0)
		{
			deepBlueDrop = 1;
		}
		
		// Check if we should apply our maths so deep blue mobs will not drop that easy
		if (Config.DEEPBLUE_DROP_RULES)
		{
			dropChance = ((drop.getChance() - ((drop.getChance() * levelModifier) / 100)) / deepBlueDrop);
		}
		
		// Applies Drop rates
		if ((drop.getItemId() == 57 || drop.getItemId() >= 6360 && drop.getItemId() <= 6362))
		{
			// like l2off must be drop chance x1 no matter what
			dropChance *= 1;
		}
		else if (isSweep)
		{
			if (this instanceof L2RaidBossInstance)
			{
				dropChance *= Config.SPOIL_RAID;
			}
			else if (this instanceof L2GrandBossInstance)
			{
				dropChance *= Config.SPOIL_BOSS;
			}
			else if (this instanceof L2MinionInstance)
			{
				dropChance *= Config.SPOIL_MINON;
			}
			else
			{
				dropChance *= Config.RATE_DROP_SPOIL;
				
				if (lastAttacker.getPremiumService() == 1)
				{
					dropChance *= Config.PREMIUM_SPOIL_RATE;
				}
			}
		}
		else
		{
			if (this instanceof L2RaidBossInstance)
			{
				dropChance *= Config.ITEMS_RAID;
			}
			else if (this instanceof L2GrandBossInstance)
			{
				dropChance *= Config.ITEMS_BOSS;
			}
			else if (this instanceof L2MinionInstance)
			{
				dropChance *= Config.ITEMS_MINON;
			}
			else
			{
				dropChance *= Config.RATE_DROP_ITEMS;
				
				if (lastAttacker.getPremiumService() == 1)
				{
					dropChance *= Config.PREMIUM_DROP_RATE;
				}
			}
		}
		
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
		{
			dropChance *= Config.L2JMOD_CHAMPION_REWARDS;
		}
		
		// Round drop chance
		dropChance = Math.round(dropChance);
		
		// Set our limits for chance of drop
		if (dropChance < 1)
		{
			dropChance = 1;
		}
		
		// Get min and max Item quantity that can be dropped in one time
		int minCount = drop.getMinDrop();
		int maxCount = drop.getMaxDrop();
		int itemCount = 0;
		
		int minEnchant = drop.getMinEnchant();
		int maxEnchant = drop.getMaxEnchant();
		int enchantLevel = 0;
		if (minEnchant > 0 && maxEnchant > 0)
		{
			enchantLevel = Rnd.get(minEnchant, maxEnchant);
		}
		
		// Count and chance adjustment for high rate servers
		if (dropChance > L2DropData.MAX_CHANCE && !Config.PRECISE_DROP_CALCULATION)
		{
			int multiplier = (int) dropChance / L2DropData.MAX_CHANCE;
			
			if (minCount < maxCount)
			{
				itemCount += Rnd.get(minCount * multiplier, maxCount * multiplier);
			}
			else if (minCount == maxCount)
			{
				itemCount += minCount * multiplier;
			}
			else
			{
				itemCount += multiplier;
			}
			dropChance = dropChance % L2DropData.MAX_CHANCE;
		}
		
		// Check if the Item must be dropped
		int random = Rnd.get(L2DropData.MAX_CHANCE);
		
		while (random < dropChance)
		{
			// Get the item quantity dropped
			if (minCount < maxCount)
			{
				itemCount += Rnd.get(minCount, maxCount);
			}
			else if (minCount == maxCount)
			{
				itemCount += minCount;
			}
			else
			{
				itemCount++;
			}
			
			// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
			dropChance -= L2DropData.MAX_CHANCE;
		}
		
		if (Config.L2JMOD_CHAMPION_ENABLE)
		{
			if ((drop.getItemId() == 57 || drop.getItemId() >= 6360 && drop.getItemId() <= 6362) && isChampion())
			{
				itemCount *= Config.L2JMOD_CHAMPION_ADENAS_REWARDS;
			}
		}
		
		if (drop.getItemId() >= 6360 && drop.getItemId() <= 6362)
		{
			itemCount *= Config.RATE_DROP_SEAL_STONES;
		}
		
		if (drop.getItemId() == 57)
		{
			if (this instanceof L2RaidBossInstance)
			{
				itemCount *= Config.ADENA_RAID;
			}
			else if (this instanceof L2GrandBossInstance)
			{
				itemCount *= Config.ADENA_BOSS;
			}
			else if (this instanceof L2MinionInstance)
			{
				itemCount *= Config.ADENA_MINON;
			}
			else
			{
				itemCount *= Config.RATE_DROP_ADENA;
				
				if (lastAttacker.getPremiumService() == 1)
				{
					itemCount *= Config.PREMIUM_ADENA_RATE;
				}
			}
		}
		
		if (itemCount > 0)
		{
			return new RewardItem(drop.getItemId(), itemCount, enchantLevel);
		}
		else if (itemCount == 0 && Config.DEBUG)
		{
			LOG.warn("Roll produced 0 items to drop...");
		}
		return null;
	}
	
	/**
	 * Calculates quantity of items for specific drop CATEGORY according to current situation <br>
	 * Only a max of ONE item from a category is allowed to be dropped.
	 * @param lastAttacker The L2PcInstance that has killed the L2Attackable
	 * @param categoryDrops
	 * @param levelModifier level modifier in %'s (will be subtracted from drop chance)
	 * @return
	 */
	private RewardItem calculateCategorizedRewardItem(L2PcInstance lastAttacker, L2DropCategory categoryDrops, int levelModifier)
	{
		if (categoryDrops == null)
		{
			return null;
		}
		
		L2DropData drop = categoryDrops.dropOne(isRaid() && !isRaidMinion());
		
		if (drop == null)
		{
			return null;
		}
		
		int basecategoryDropChance = categoryDrops.getCategoryChance();
		int categoryDropChance = basecategoryDropChance;
		int deepBlueDrop = 1;
		
		if (Config.DEEPBLUE_DROP_RULES)
		{
			if (levelModifier > 0)
			{
				// We should multiply by the server's drop rate, so we always get a low chance of drop for deep blue mobs.
				// NOTE: This is valid only for adena drops! Others drops will still obey server's rate
				deepBlueDrop = 3;
			}
			
			categoryDropChance = ((categoryDropChance - ((categoryDropChance * levelModifier) / 100)) / deepBlueDrop);
		}
		
		if (drop.getItemId() == 57 || drop.getItemId() == 6360 || drop.getItemId() == 6361 || drop.getItemId() == 6362)
		{
			// like l2off must be categoryDropChance chance x1 no matter what
			categoryDropChance *= 1;
			if (lastAttacker.getPremiumService() == 1)
			{
				categoryDropChance *= Config.PREMIUM_SS_RATE;
			}
		}
		else
		{
			if (this instanceof L2RaidBossInstance)
			{
				categoryDropChance *= Config.ITEMS_RAID_CATEGORY_CHANCE;
			}
			else if (this instanceof L2GrandBossInstance)
			{
				categoryDropChance *= Config.ITEMS_BOSS_CATEGORY_CHANCE;
			}
			else if (this instanceof L2MinionInstance)
			{
				categoryDropChance *= Config.ITEMS_MINON_CATEGORY_CHANCE;
			}
			else
			{
				categoryDropChance *= Config.RATE_DROP_ITEMS_CATEGORY_CHANCE;
			}
			
			if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
			{
				categoryDropChance *= Config.L2JMOD_CHAMPION_REWARDS;
			}
		}
		
		// Round drop chance
		categoryDropChance = Math.round(categoryDropChance);
		
		// Set our limits for chance of drop
		if (categoryDropChance < 1)
		{
			categoryDropChance = 1;
		}
		
		// Check if an Item from this category must be dropped
		if (Rnd.get(L2DropData.MAX_CHANCE) < categoryDropChance)
		{
			int dropChance = drop.getChance();
			
			if ((drop.getItemId() == 57 || drop.getItemId() >= 6360 && drop.getItemId() <= 6362))
			{
				// like l2off must be drop chance x1 no matter what
				dropChance *= 1;
			}
			else
			{
				if (this instanceof L2RaidBossInstance)
				{
					dropChance *= Config.ITEMS_RAID;
				}
				else if (this instanceof L2GrandBossInstance)
				{
					dropChance *= Config.ITEMS_BOSS;
				}
				else if (this instanceof L2MinionInstance)
				{
					dropChance *= Config.ITEMS_MINON;
				}
				else
				{
					dropChance *= Config.RATE_DROP_ITEMS;
					
					if (lastAttacker.getPremiumService() == 1)
					{
						dropChance *= Config.PREMIUM_DROP_RATE;
					}
				}
			}
			
			if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
			{
				dropChance *= Config.L2JMOD_CHAMPION_REWARDS;
			}
			
			dropChance = Math.round(dropChance);
			
			if (dropChance < L2DropData.MAX_CHANCE)
			{
				dropChance = L2DropData.MAX_CHANCE;
			}
			
			// Get min and max Item quantity that can be dropped in one time
			int min = drop.getMinDrop();
			int max = drop.getMaxDrop();
			
			// Get the item quantity dropped
			int itemCount = 0;
			
			int enchantMin = drop.getMinEnchant();
			int enchantMax = drop.getMaxEnchant();
			int enchantLevel = 0;
			if (enchantMin > 0 && enchantMax > 0)
			{
				enchantLevel = Rnd.get(enchantMin, enchantMax);
			}
			
			// Count and chance adjustment for high rate servers
			if (dropChance > L2DropData.MAX_CHANCE && !Config.PRECISE_DROP_CALCULATION)
			{
				int multiplier = (dropChance) / L2DropData.MAX_CHANCE;
				
				if (min < max)
				{
					itemCount += Rnd.get(min * multiplier, max * multiplier);
				}
				else if (min == max)
				{
					itemCount += min * multiplier;
				}
				else
				{
					itemCount += multiplier;
				}
				
				dropChance = dropChance % L2DropData.MAX_CHANCE;
			}
			
			// Check if the Item must be dropped
			int random = Rnd.get(L2DropData.MAX_CHANCE);
			
			while (random < dropChance)
			{
				// Get the item quantity dropped
				if (min < max)
				{
					itemCount += Rnd.get(min, max);
				}
				else if (min == max)
				{
					itemCount = min;
				}
				else
				{
					itemCount++;
				}
				
				// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
				dropChance -= L2DropData.MAX_CHANCE;
			}
			
			if (drop.getItemId() >= 6360 && drop.getItemId() <= 6362)
			{
				itemCount *= Config.RATE_DROP_SEAL_STONES;
				if (lastAttacker.getPremiumService() == 1)
				{
					itemCount *= Config.PREMIUM_SS_RATE;
				}
			}
			
			if (drop.getItemId() == 57)
			{
				if (this instanceof L2RaidBossInstance)
				{
					itemCount *= Config.ADENA_RAID;
				}
				else if (this instanceof L2GrandBossInstance)
				{
					itemCount *= Config.ADENA_BOSS;
				}
				else if (this instanceof L2MinionInstance)
				{
					itemCount *= Config.ADENA_MINON;
				}
				else
				{
					itemCount *= Config.RATE_DROP_ADENA;
					
					if (lastAttacker.getPremiumService() == 1)
					{
						itemCount *= Config.PREMIUM_ADENA_RATE;
					}
				}
			}
			
			if (Config.L2JMOD_CHAMPION_ENABLE)
			{
				if (isChampion() && ItemTable.getInstance().getTemplate(drop.getItemId()).isStackable())
				{
					if ((drop.getItemId() == 57 || drop.getItemId() >= 6360 && drop.getItemId() <= 6362))
					{
						itemCount *= Config.L2JMOD_CHAMPION_ADENAS_REWARDS;
					}
					else
					{
						itemCount *= Config.L2JMOD_CHAMPION_REWARDS;
					}
				}
			}
			
			if (!Config.MULTIPLE_ITEM_DROP && !ItemTable.getInstance().getTemplate(drop.getItemId()).isStackable() && itemCount > 1)
			{
				itemCount = 1;
			}
			
			if (itemCount > 0)
			{
				return new RewardItem(drop.getItemId(), itemCount, enchantLevel);
			}
		}
		return null;
	}
	
	/**
	 * Calculates the level modifier for drop<br>
	 * @param lastAttacker The L2PcInstance that has killed the L2Attackable
	 * @return
	 */
	private int calculateLevelModifierForDrop(L2PcInstance lastAttacker)
	{
		if (Config.DEEPBLUE_DROP_RULES)
		{
			int highestLevel = lastAttacker.getLevel();
			
			// Check to prevent very high level player to nearly kill mob and let low level player do the last hit.
			if (getAttackByList() != null && !getAttackByList().isEmpty())
			{
				for (L2Character atkChar : getAttackByList())
				{
					if (atkChar != null && atkChar.getLevel() > highestLevel)
					{
						highestLevel = atkChar.getLevel();
					}
				}
			}
			
			if (highestLevel - 9 >= getLevel())
			{
				return (highestLevel - (getLevel() + 8)) * 9;
			}
		}
		
		return 0;
	}
	
	public void doItemDrop(L2Character lastAttacker)
	{
		doItemDrop(getTemplate(), lastAttacker);
	}
	
	/**
	 * Manage Base, Quests and Special Events drops of L2Attackable (called by calculateRewards).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * During a Special Event all L2Attackable can drop extra Items. Those extra Items are defined in the table <B>allNpcDateDrops</B> of the EventDroplist. Each Special Event has a start and end date to stop to drop extra Items automaticaly. <BR>
	 * <BR>
	 * <B><U> Actions</U> : </B><BR>
	 * <BR>
	 * <li>Manage drop of Special Events created by GM for a defined period</li>
	 * <li>Get all possible drops of this L2Attackable from L2NpcTemplate and add it Quest drops</li>
	 * <li>For each possible drops (base + quests), calculate which one must be dropped (random)</li>
	 * <li>Get each Item quantity dropped (random)</li>
	 * <li>Create this or these L2ItemInstance corresponding to each Item Identifier dropped</li>
	 * <li>If the autoLoot mode is actif and if the L2Character that has killed the L2Attackable is a L2PcInstance, give this or these Item(s) to the L2PcInstance that has killed the L2Attackable</li>
	 * <li>If the autoLoot mode isn't actif or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these Item(s) in the world as a visible object at the position where mob was last</li><BR>
	 * <BR>
	 * @param npcTemplate
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character lastAttacker)
	{
		L2PcInstance player = null;
		
		if (lastAttacker instanceof L2PcInstance)
		{
			player = (L2PcInstance) lastAttacker;
		}
		else if (lastAttacker instanceof L2Summon)
		{
			player = ((L2Summon) lastAttacker).getOwner();
		}
		
		// Don't drop anything if the last attacker or ownere isn't L2PcInstance
		if (player == null)
		{
			return;
		}
		
		int levelModifier = calculateLevelModifierForDrop(player); // level modifier in %'s (will be subtracted from drop chance)
		
		// Check the drop of a cursed weapon
		if (levelModifier == 0 && player.getLevel() > 20)
		{
			CursedWeaponsManager.getInstance().checkDrop(this, player);
		}
		
		// now throw all categorized drops and handle spoil.
		for (L2DropCategory cat : npcTemplate.getDropData())
		{
			RewardItem item = null;
			if (cat.isSweep())
			{
				if (getSpoilerId() != 0)
				{
					FastList<RewardItem> sweepList = new FastList<>();
					
					if (Config.L2JMOD_CHAMPION_ENABLE && isChampion() && Config.L2JMOD_CHAMPION_REWARD_SPOIL && (Config.DEEPBLUE_DROP_RULES && lastAttacker.getLevel() <= getLevel() + 5) && Config.L2JMOD_CHAMPION_REWARD > 0 && Rnd.get(100) < Config.L2JMOD_CHAMPION_REWARD)
					{
						int champ_reward = Config.L2JMOD_CHAMPION_REWARD_ID;
						int champ_qty = Rnd.get(2, Config.L2JMOD_CHAMPION_REWARD_QTY);
						
						item = new RewardItem(champ_reward, champ_qty, 0);
						sweepList.add(item);
					}
					
					for (L2DropData drop : cat.getAllDrops())
					{
						item = calculateRewardItem(player, drop, levelModifier, true);
						
						if (item == null)
						{
							continue;
						}
						
						sweepList.add(item);
					}
					
					// Set the table _sweepItems of this L2Attackable
					if (!sweepList.isEmpty())
					{
						_sweepItems = sweepList.toArray(new RewardItem[sweepList.size()]);
					}
				}
			}
			else
			{
				if (isSeeded())
				{
					L2DropData drop = cat.dropSeedAllowedDropsOnly();
					
					if (drop == null)
					{
						continue;
					}
					
					item = calculateRewardItem(player, drop, levelModifier, false);
				}
				else
				{
					item = calculateCategorizedRewardItem(player, cat, levelModifier);
				}
				
				if (item != null)
				{
					// Check if the autoLoot mode is active
					if (player.getAutoLootEnabled())
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
						if (!player.getInventory().validateCapacity(item_templ) || (!Config.AUTO_LOOT_BOSS && this instanceof L2RaidBossInstance) || (!Config.AUTO_LOOT_BOSS && this instanceof L2GrandBossInstance))
						{
							DropItem(player, item);
						}
						else
						{
							player.doAutoLoot(this, item);
						}
					}
					else
					{
						if ((Config.AUTO_LOOT_BOSS && this instanceof L2RaidBossInstance) || (Config.AUTO_LOOT_BOSS && this instanceof L2GrandBossInstance))
						{
							player.doAutoLoot(this, item);
						}
						else
						{
							DropItem(player, item); // drop the item on the ground
						}
					}
					
					// Broadcast message if RaidBoss was defeated
					if (this instanceof L2RaidBossInstance || this instanceof L2GrandBossInstance)
					{
						SystemMessage sm;
						sm = new SystemMessage(SystemMessageId.S1_DIED_DROPPED_S3_S2);
						sm.addString(getName());
						sm.addItemName(item.getItemId());
						sm.addNumber(item.getCount());
						broadcastPacket(sm);
					}
				}
			}
		}
		
		if (Config.L2JMOD_CHAMPION_ENABLE && isChampion() && (Config.DEEPBLUE_DROP_RULES && player.getLevel() <= getLevel() + 5) && Config.L2JMOD_CHAMPION_REWARD > 0 && Rnd.get(100) < Config.L2JMOD_CHAMPION_REWARD)
		{
			int champqty = Rnd.get(2, Config.L2JMOD_CHAMPION_REWARD_QTY);
			
			RewardItem item = new RewardItem(Config.L2JMOD_CHAMPION_REWARD_ID, champqty, 0);
			
			// Give this or these Item(s) to the L2PcInstance that has killed the L2Attackable
			if (player.getAutoLootEnabled())
			{
				L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
				if (!player.getInventory().validateCapacity(item_templ))
				{
					DropItem(player, item);
				}
				else
				{
					player.doAutoLoot(this, item);
				}
			}
			else
			{
				DropItem(player, item);
			}
		}
		
		double rateHp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		
		if (rateHp < 2 && String.valueOf(npcTemplate.type).contentEquals("L2Monster") || String.valueOf(npcTemplate.DropHerb).contentEquals("1")) // only L2Monster with <= 1x HP can drop herbs
		{
			// no drop if -1
			if (String.valueOf(npcTemplate.DropHerb).contentEquals("-1"))
			{
				return;
			}
			
			boolean _hp = false;
			boolean _mp = false;
			boolean _spec = false;
			
			int deepBlueDrop = 1;
			
			// ptk - patk type enhance
			int random = Rnd.get(1000); // note *10
			
			// Check if we should apply our maths so deep blue mobs will not drop that easy
			float special_herb = Config.RATE_DROP_SPECIAL_HERBS;
			if (Config.DEEPBLUE_DROP_RULES)
			{
				special_herb = ((special_herb - ((special_herb * levelModifier) * 100)) / deepBlueDrop);
			}
			
			if (random < special_herb && !_spec) // && !_spec useless yet
			{
				RewardItem item = new RewardItem(8612, 1, 0); // Herb of Warrior
				if (player.getAutoLootHerbs())
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
					
					if (!player.getInventory().validateCapacity(item_templ))
					{
						DropItem(player, item);
					}
					else
					{
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					}
				}
				else
				{
					DropItem(player, item);
				}
				_spec = true;
			}
			else
			{
				for (int i = 0; i < 3; i++)
				{
					random = Rnd.get(100);
					
					float common_chance = Config.RATE_DROP_COMMON_HERBS;
					if (Config.DEEPBLUE_DROP_RULES)
					{
						common_chance = ((common_chance - ((common_chance * levelModifier) * 100)) / deepBlueDrop);
					}
					
					if (random < common_chance)
					{
						RewardItem item = null;
						if (i == 0)
						{
							item = new RewardItem(8606, 1, 0); // Herb of Power
						}
						if (i == 1)
						{
							item = new RewardItem(8608, 1, 0); // Herb of Atk. Spd.
						}
						if (i == 2)
						{
							item = new RewardItem(8610, 1, 0); // Herb of Critical Attack
						}
						
						if (item == null)
						{
							break;
						}
						
						if (player.getAutoLootHerbs())
						{
							L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
							
							if (!player.getInventory().validateCapacity(item_templ))
							{
								DropItem(player, item);
							}
							else
							{
								player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
							}
						}
						else
						{
							DropItem(player, item);
						}
						break;
					}
				}
			}
			
			// mtk - matk type enhance
			random = Rnd.get(1000); // note *10
			// Check if we should apply our maths so deep blue mobs will not drop that easy
			float special_chance = Config.RATE_DROP_SPECIAL_HERBS;
			if (Config.DEEPBLUE_DROP_RULES)
			{
				special_chance = ((special_chance - ((special_chance * levelModifier) * 100)) / deepBlueDrop);
			}
			
			if (random < special_chance && !_spec)
			{
				RewardItem item = new RewardItem(8613, 1, 0); // Herb of Mystic
				
				if (player.getAutoLootHerbs())
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
					
					if (!player.getInventory().validateCapacity(item_templ))
					{
						DropItem(player, item);
					}
					else
					{
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					}
				}
				else
				{
					DropItem(player, item);
				}
				_spec = true;
			}
			else
			{
				for (int i = 0; i < 2; i++)
				{
					random = Rnd.get(100);
					// Check if we should apply our maths so deep blue mobs will not drop that easy
					float common_chance = Config.RATE_DROP_COMMON_HERBS;
					if (Config.DEEPBLUE_DROP_RULES)
					{
						common_chance = ((common_chance - ((common_chance * levelModifier) * 100)) / deepBlueDrop);
					}
					if (random < common_chance)
					{
						RewardItem item = null;
						if (i == 0)
						{
							item = new RewardItem(8607, 1, 0); // Herb of Magic
						}
						if (i == 1)
						{
							item = new RewardItem(8609, 1, 0); // Herb of Casting Speed
						}
						if (item == null)
						{
							break;
						}
						if (player.getAutoLootHerbs())
						{
							L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
							
							if (!player.getInventory().validateCapacity(item_templ))
							{
								DropItem(player, item);
							}
							else
							{
								player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
							}
						}
						else
						{
							DropItem(player, item);
						}
						break;
					}
				}
			}
			// hp+mp type
			random = Rnd.get(1000); // note *10
			// Check if we should apply our maths so deep blue mobs will not drop that easy
			float special_chance2 = Config.RATE_DROP_SPECIAL_HERBS;
			if (Config.DEEPBLUE_DROP_RULES)
			{
				special_chance2 = ((special_chance2 - ((special_chance2 * levelModifier) * 100)) / deepBlueDrop);
			}
			
			if (random < special_chance2 && !_spec)
			{
				RewardItem item = new RewardItem(8614, 1, 0); // Herb of Recovery
				
				if (player.getAutoLootHerbs())
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
					
					if (!player.getInventory().validateCapacity(item_templ))
					{
						DropItem(player, item);
					}
					else
					{
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					}
				}
				else
				{
					DropItem(player, item);
				}
				_mp = true;
				_hp = true;
				_spec = true;
			}
			
			// hp - restore hp type
			if (!_hp)
			{
				random = Rnd.get(100);
				float mp_hp = Config.RATE_DROP_MP_HP_HERBS;
				if (Config.DEEPBLUE_DROP_RULES)
				{
					mp_hp = ((mp_hp - ((mp_hp * levelModifier) * 100)) / deepBlueDrop);
				}
				if (random < mp_hp)
				{
					RewardItem item = new RewardItem(8600, 1, 0); // Herb of Life
					
					if (player.getAutoLootHerbs())
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
						
						if (!player.getInventory().validateCapacity(item_templ))
						{
							DropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						DropItem(player, item);
					}
					item = null;
					_hp = true;
				}
			}
			if (!_hp)
			{
				random = Rnd.get(100);
				float greater = Config.RATE_DROP_GREATER_HERBS;
				if (Config.DEEPBLUE_DROP_RULES)
				{
					greater = ((greater - ((greater * levelModifier) * 100)) / deepBlueDrop);
				}
				
				if (random < greater)
				{
					RewardItem item = new RewardItem(8601, 1, 0); // Greater Herb of Life
					
					if (player.getAutoLootHerbs())
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
						
						if (!player.getInventory().validateCapacity(item_templ))
						{
							DropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						DropItem(player, item);
					}
					item = null;
					_hp = true;
				}
			}
			if (!_hp)
			{
				random = Rnd.get(1000); // note *10
				float superior = Config.RATE_DROP_SUPERIOR_HERBS;
				if (Config.DEEPBLUE_DROP_RULES)
				{
					superior = ((superior - ((superior * levelModifier) * 100)) / deepBlueDrop);
				}
				if (random < superior)
				{
					RewardItem item = new RewardItem(8602, 1, 0); // Superior Herb of Life
					
					if (player.getAutoLootHerbs())
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
						
						if (!player.getInventory().validateCapacity(item_templ))
						{
							DropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						DropItem(player, item);
					}
					item = null;
				}
			}
			// mp - restore mp type
			if (!_mp)
			{
				random = Rnd.get(100);
				float hp_mp2 = Config.RATE_DROP_MP_HP_HERBS;
				if (Config.DEEPBLUE_DROP_RULES)
				{
					hp_mp2 = ((hp_mp2 - ((hp_mp2 * levelModifier) * 100)) / deepBlueDrop);
				}
				if (random < hp_mp2)
				{
					RewardItem item = new RewardItem(8603, 1, 0); // Herb of Mana
					
					if (player.getAutoLootHerbs())
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
						
						if (!player.getInventory().validateCapacity(item_templ))
						{
							DropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						DropItem(player, item);
					}
					item = null;
					_mp = true;
				}
			}
			
			if (!_mp)
			{
				random = Rnd.get(100);
				float greater2 = Config.RATE_DROP_GREATER_HERBS;
				if (Config.DEEPBLUE_DROP_RULES)
				{
					greater2 = ((greater2 - ((greater2 * levelModifier) * 100)) / deepBlueDrop);
				}
				
				if (random < greater2)
				{
					RewardItem item = new RewardItem(8604, 1, 0); // Greater Herb of Mana
					
					if (player.getAutoLootHerbs())
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
						
						if (!player.getInventory().validateCapacity(item_templ))
						{
							DropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						DropItem(player, item);
					}
					item = null;
					_mp = true;
				}
			}
			if (!_mp)
			{
				random = Rnd.get(1000); // note *10
				float superior3 = Config.RATE_DROP_SUPERIOR_HERBS;
				if (Config.DEEPBLUE_DROP_RULES)
				{
					superior3 = ((superior3 - ((superior3 * levelModifier) * 100)) / deepBlueDrop);
				}
				
				if (random < superior3)
				{
					RewardItem item = new RewardItem(8605, 1, 0); // Superior Herb of Mana
					
					if (player.getAutoLootHerbs())
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
						
						if (!player.getInventory().validateCapacity(item_templ))
						{
							DropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						DropItem(player, item);
					}
					item = null;
				}
			}
			// speed enhance type
			random = Rnd.get(100);
			float common4 = Config.RATE_DROP_COMMON_HERBS;
			if (Config.DEEPBLUE_DROP_RULES)
			{
				common4 = ((common4 - ((common4 * levelModifier) * 100)) / deepBlueDrop);
			}
			
			if (random < common4)
			{
				RewardItem item = new RewardItem(8611, 1, 0); // Herb of Speed
				
				if (player.getAutoLootHerbs())
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
					
					if (!player.getInventory().validateCapacity(item_templ))
					{
						DropItem(player, item);
					}
					else
					{
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					}
				}
				else
				{
					DropItem(player, item);
				}
			}
		}
	}
	
	/**
	 * Manage Special Events drops created by GM for a defined period.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * During a Special Event all L2Attackable can drop extra Items. Those extra Items are defined in the table <B>allNpcDateDrops</B> of the EventDroplist. Each Special Event has a start and end date to stop to drop extra Items automaticaly. <BR>
	 * <BR>
	 * <B><U> Actions</U> : <I>If an extra drop must be generated</I></B><BR>
	 * <BR>
	 * <li>Get an Item Identifier (random) from the DateDrop Item table of this Event</li>
	 * <li>Get the Item quantity dropped (random)</li>
	 * <li>Create this or these L2ItemInstance corresponding to this Item Identifier</li>
	 * <li>If the autoLoot mode is actif and if the L2Character that has killed the L2Attackable is a L2PcInstance, give this or these Item(s) to the L2PcInstance that has killed the L2Attackable</li>
	 * <li>If the autoLoot mode isn't actif or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these Item(s) in the world as a visible object at the position where mob was last</li><BR>
	 * <BR>
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	public void doEventDrop(L2Character lastAttacker)
	{
		L2PcInstance player = null;
		
		if (lastAttacker instanceof L2PcInstance)
		{
			player = (L2PcInstance) lastAttacker;
		}
		else if (lastAttacker instanceof L2Summon)
		{
			player = ((L2Summon) lastAttacker).getOwner();
		}
		
		if (isGuard())
		{
			return;
		}
		
		if (player == null)
		{
			return; // Don't drop anything if the last attacker or ownere isn't L2PcInstance
		}
		
		if (player.getLevel() - getLevel() > 9)
		{
			return;
		}
		
		// Go through DateDrop of EventDroplist allNpcDateDrops within the date range
		for (DateDrop drop : EventDroplist.getInstance().getAllDrops())
		{
			if (Rnd.get(L2DropData.MAX_CHANCE) < drop.chance)
			{
				RewardItem item = new RewardItem(drop.items[Rnd.get(drop.items.length)], Rnd.get(drop.min, drop.max), 0);
				
				if (player.getAutoLootEnabled())
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());
					
					if (!player.getInventory().validateCapacity(item_templ))
					{
						DropItem(player, item);
					}
					else
					{
						player.doAutoLoot(this, item); // Give this or these Item(s) to the L2PcInstance that has killed the L2Attackable
					}
				}
				else
				{
					DropItem(player, item); // drop the item on the ground
				}
			}
		}
	}
	
	/**
	 * Drop reward item.<BR>
	 * <BR>
	 * @param mainDamageDealer
	 * @param item
	 * @return
	 */
	public L2ItemInstance DropItem(L2PcInstance mainDamageDealer, RewardItem item)
	{
		int randDropLim = 70;
		
		L2ItemInstance ditem = null;
		
		for (int i = 0; i < item.getCount(); i++)
		{
			// Randomise drop position
			int newX = getX() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newY = getY() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newZ = Math.max(getZ(), mainDamageDealer.getZ()) + 10;
			
			// Unit the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
			ditem = ItemTable.getInstance().createItem("Loot", item.getItemId(), item.getCount(), mainDamageDealer, this);
			
			if (item.getEnchantLevel() > 0)
			{
				if (ditem.getItem().getType2() == L2Item.TYPE2_WEAPON || ditem.getItem().getType2() == L2Item.TYPE2_SHIELD_ARMOR || ditem.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
				{
					ditem.setEnchantLevel(item.getEnchantLevel());
				}
			}
			
			ditem.getDropProtection().protect(mainDamageDealer);
			ditem.dropMe(this, newX, newY, newZ);
			
			// Add drop to auto destroy item task
			if (!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
			{
				if (Config.AUTODESTROY_ITEM_AFTER > 0 && ditem.getItemType() != L2EtcItemType.HERB || Config.HERB_AUTO_DESTROY_TIME > 0 && ditem.getItemType() == L2EtcItemType.HERB)
				{
					ItemsAutoDestroy.getInstance().addItem(ditem);
				}
			}
			
			ditem.setProtected(false);
			
			// If stackable, end loop as entire count is included in 1 instance of item
			if (ditem.isStackable() || !Config.MULTIPLE_ITEM_DROP)
			{
				break;
			}
		}
		return ditem;
	}
	
	public L2ItemInstance DropItem(L2PcInstance lastAttacker, int itemId, int itemCount)
	{
		return DropItem(lastAttacker, new RewardItem(itemId, itemCount, 0));
	}
	
	/**
	 * Return the active weapon of this L2Attackable (= null).<BR>
	 * <BR>
	 * @return
	 */
	public L2ItemInstance getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * Return True if the _aggroList of this L2Attackable is Empty.<BR>
	 * <BR>
	 * @return
	 */
	public boolean noTarget()
	{
		return getAggroListRP().isEmpty();
	}
	
	/**
	 * Return True if the _aggroList of this L2Attackable contains the L2Character.<BR>
	 * <BR>
	 * @param player The L2Character searched in the _aggroList of the L2Attackable
	 * @return
	 */
	public boolean containsTarget(L2Character player)
	{
		return getAggroListRP().containsKey(player);
	}
	
	/**
	 * Clear the _aggroList of the L2Attackable.<BR>
	 * <BR>
	 */
	public void clearAggroList()
	{
		getAggroList().clear();
		
		// clear overhit values
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}
	
	/**
	 * Return True if a Dwarf use Sweep on the L2Attackable and if item can be spoiled.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isSweepActive()
	{
		return _sweepItems != null;
	}
	
	/**
	 * Return table containing all L2ItemInstance that can be spoiled.<BR>
	 * <BR>
	 * @return
	 */
	public synchronized RewardItem[] takeSweep()
	{
		RewardItem[] sweep = _sweepItems;
		
		_sweepItems = null;
		
		return sweep;
	}
	
	/**
	 * Return table containing all L2ItemInstance that can be harvested.<BR>
	 * <BR>
	 * @return
	 */
	public synchronized RewardItem[] takeHarvest()
	{
		RewardItem[] harvest = _harvestItems;
		
		_harvestItems = null;
		
		return harvest;
	}
	
	/**
	 * Set the over-hit flag on the L2Attackable.<BR>
	 * <BR>
	 * @param status The status of the over-hit flag
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}
	
	/**
	 * Set the over-hit values like the attacker who did the strike and the ammount of damage done by the skill.<BR>
	 * <BR>
	 * @param attacker The L2Character who hit on the L2Attackable using the over-hit enabled skill
	 * @param damage The ammount of damage done by the over-hit enabled skill on the L2Attackable
	 */
	public void setOverhitValues(L2Character attacker, double damage)
	{
		// Calculate the over-hit damage
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40
		double overhitDmg = -(getCurrentHp() - damage);
		
		if (overhitDmg < 0)
		{
			// we didn't killed the mob with the over-hit strike. (it wasn't really an over-hit strike)
			// let's just clear all the over-hit related values
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}
	
	/**
	 * Return the L2Character who hit on the L2Attackable using an over-hit enabled skill.<BR>
	 * <BR>
	 * @return L2Character attacker
	 */
	public L2Character getOverhitAttacker()
	{
		return _overhitAttacker;
	}
	
	/**
	 * Return the ammount of damage done on the L2Attackable using an over-hit enabled skill.<BR>
	 * <BR>
	 * @return double damage
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}
	
	/**
	 * Return True if the L2Attackable was hit by an over-hit enabled skill.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	/**
	 * Activate the absorbed soul condition on the L2Attackable.<BR>
	 * <BR>
	 */
	public void absorbSoul()
	{
		_absorbed = true;
		
	}
	
	/**
	 * Return True if the L2Attackable had his soul absorbed.<BR>
	 * <BR>
	 * @return
	 */
	public boolean isAbsorbed()
	{
		return _absorbed;
	}
	
	/**
	 * Adds an attacker that successfully absorbed the soul of this L2Attackable into the _absorbersList.<BR>
	 * @param attacker - a valid L2PcInstance
	 * @param crystalId
	 */
	public void addAbsorber(L2PcInstance attacker, int crystalId)
	{
		// This just works for targets like L2MonsterInstance
		if (!(this instanceof L2MonsterInstance))
		{
			return;
		}
		
		// The attacker must not be null
		if (attacker == null)
		{
			return;
		}
		
		// This L2Attackable must be of one type in the _absorbingMOBS_levelXX tables.
		// OBS: This is done so to avoid triggering the absorbed conditions for mobs that can't be absorbed.
		if (getAbsorbLevel() == 0)
		{
			return;
		}
		
		// If we have no _absorbersList initiated, do it
		AbsorberInfo ai = _absorbersList.get(attacker);
		
		// If the L2Character attacker isn't already in the _absorbersList of this L2Attackable, add it
		if (ai == null)
		{
			ai = new AbsorberInfo(attacker, crystalId, getCurrentHp());
			_absorbersList.put(attacker, ai);
		}
		else
		{
			ai._absorber = attacker;
			ai._crystalId = crystalId;
			ai._absorbedHP = getCurrentHp();
		}
		
		// Set this L2Attackable as absorbed
		absorbSoul();
		
		ai = null;
	}
	
	/**
	 * Calculate the levelling chance of Soul Crystals based on the attacker that killed this L2Attackable
	 * @param attacker
	 */
	private void levelSoulCrystals(L2Character attacker)
	{
		// Only L2PcInstance can absorb a soul
		if (!(attacker instanceof L2PcInstance) && !(attacker instanceof L2Summon))
		{
			resetAbsorbList();
			return;
		}
		
		int maxAbsorbLevel = getAbsorbLevel();
		int minAbsorbLevel = 0;
		
		// If this is not a valid L2Attackable, clears the _absorbersList and just return
		if (maxAbsorbLevel == 0)
		{
			resetAbsorbList();
			return;
		}
		
		// All boss mobs with maxAbsorbLevel 13 have minAbsorbLevel of 12 else 10
		if (maxAbsorbLevel > 10)
		{
			minAbsorbLevel = maxAbsorbLevel > 12 ? 12 : 10;
		}
		
		// Init some useful vars
		boolean isSuccess = true;
		boolean doLevelup = true;
		
		boolean isBossMob = maxAbsorbLevel > 10 ? true : false;
		
		L2NpcTemplate.AbsorbCrystalType absorbType = getTemplate().absorbType;
		
		L2PcInstance killer = attacker instanceof L2Summon ? ((L2Summon) attacker).getOwner() : (L2PcInstance) attacker;
		
		// If this mob is a boss, then skip some checking
		if (!isBossMob)
		{
			// Fail if this L2Attackable isn't absorbed or there's no one in its _absorbersList
			if (!isAbsorbed())
			{
				resetAbsorbList();
				return;
			}
			
			// Fail if the killer isn't in the _absorbersList of this L2Attackable and mob is not boss
			AbsorberInfo ai = _absorbersList.get(killer);
			if (ai == null || ai._absorber.getObjectId() != killer.getObjectId())
			{
				isSuccess = false;
			}
			
			// Check if the soul crystal was used when HP of this L2Attackable wasn't higher than half of it
			if (ai != null && ai._absorbedHP > getMaxHp() / 2.0)
			{
				isSuccess = false;
			}
			
			if (!isSuccess)
			{
				resetAbsorbList();
				return;
			}
		}
		
		String[] crystalNFO = null;
		String crystalNME = "";
		
		int dice = Rnd.get(100);
		int crystalQTY = 0;
		int crystalLVL = 0;
		int crystalOLD = 0;
		int crystalNEW = 0;
		
		// ********
		// Now we have four choices:
		// 1- The Monster level is too low for the crystal. Nothing happens.
		// 2- Everything is correct, but it failed. Nothing happens. (57.5%)
		// 3- Everything is correct, but it failed. The crystal scatters. A sound event is played. (10%)
		// 4- Everything is correct, the crystal level up. A sound event is played. (32.5%)
		
		List<L2PcInstance> players = new FastList<>();
		
		if (absorbType == L2NpcTemplate.AbsorbCrystalType.FULL_PARTY && killer.isInParty())
		{
			players = killer.getParty().getPartyMembers();
		}
		else if (absorbType == L2NpcTemplate.AbsorbCrystalType.PARTY_ONE_RANDOM && killer.isInParty())
		{
			// This is a naive method for selecting a random member. It gets any random party member and
			// then checks if the member has a valid crystal. It does not select the random party member
			// among those who have crystals, only. However, this might actually be correct (same as retail).
			players.add(killer.getParty().getPartyMembers().get(Rnd.get(killer.getParty().getMemberCount())));
		}
		else
		{
			players.add(killer);
		}
		
		for (L2PcInstance player : players)
		{
			if (player == null)
			{
				continue;
			}
			
			// reset for next member
			isSuccess = true;
			doLevelup = true;
			crystalQTY = 0;
			// Let's check quest
			boolean hasQuest = player.getQuestState("350_EnhanceYourWeapon") != null;
			
			L2ItemInstance[] inv = player.getInventory().getItems();
			
			for (L2ItemInstance item : inv)
			{
				int itemId = item.getItemId();
				for (int id : SoulCrystal.SoulCrystalTable)
				{
					// Find any of the 39 possible crystals.
					if (id == itemId)
					{
						crystalQTY++;
						
						// Validate if the crystal has already levelled
						if (id != SoulCrystal.RED_NEW_CRYSTAL && id != SoulCrystal.GRN_NEW_CYRSTAL && id != SoulCrystal.BLU_NEW_CRYSTAL)
						{
							try
							{
								if (item.getItem().getName().contains("Grade"))
								{
									// Split the name of the crystal into 'name' & 'level'
									crystalNFO = item.getItem().getName().trim().replace(" Grade ", "-").split("-");
									// Set Level to 13
									crystalLVL = 13;
									// Get Name
									crystalNME = crystalNFO[0].toLowerCase();
								}
								else
								{
									// Split the name of the crystal into 'name' & 'level'
									crystalNFO = item.getItem().getName().trim().replace(" Stage ", "").split("-");
									// Get Level
									crystalLVL = Integer.parseInt(crystalNFO[1].trim());
									// Get Name
									crystalNME = crystalNFO[0].toLowerCase();
								}
								
								// Allocate current and level up ids for higher level crystals
								if (crystalLVL > 9)
								{
									for (int[] element : SoulCrystal.HighSoulConvert)
									{
										// Get the next stage above 10 using array.
										if (id == element[0])
										{
											crystalNEW = element[1];
											break;
										}
									}
								}
								else
								{
									crystalNEW = id + 1;
								}
							}
							catch (NumberFormatException nfe)
							{
								LOG.warn("An attempt to identify a soul crystal failed, verify the names have not changed in etcitem table.", nfe);
								player.sendMessage("There has been an error handling your soul crystal. Please notify your server admin.");
								
								isSuccess = false;
								break;
							}
							catch (Exception e)
							{
								e.printStackTrace();
								isSuccess = false;
								break;
							}
						}
						else
						{
							crystalNME = item.getItem().getName().toLowerCase().trim();
							crystalNEW = id + 1;
						}
						
						// Done
						crystalOLD = id;
						break;
					}
				}
				
				if (!isSuccess)
				{
					break;
				}
			}
			
			// If the crystal level is way too high for this mob, say that we can't increase it
			if (crystalLVL < minAbsorbLevel || crystalLVL >= maxAbsorbLevel)
			{
				doLevelup = false;
			}
			
			// The player doesn't have any crystals with him get to the next player.
			if (crystalQTY < 1 || crystalQTY > 1 || !isSuccess || !doLevelup || !hasQuest)
			{
				// Too many crystals in inventory.
				if (crystalQTY > 1)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED_RESONATION));
				}
				// The soul crystal stage of the player is way too high
				// Like L2OFF message must not appear if char hasn't crystal on inventory
				else if (!doLevelup && crystalQTY > 0)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_REFUSED));
				}
				
				continue;
			}
			
			/*
			 * It is known that bosses with FULL_PARTY crystal level ups have 100% success rate, but this is not the case for the other bosses (one-random or last-hit). While not confirmed, it is most reasonable that crystals leveled up at bosses will never break. Also, the chance to level up is
			 * guessed as around 70% if not higher.
			 */
			int chanceLevelUp = isBossMob ? SoulCrystal.LEVEL_CHANCE_FOR_BOSS : SoulCrystal.LEVEL_CHANCE;
			
			// If succeeds or it is a full party absorb, level up the crystal.
			if (absorbType == L2NpcTemplate.AbsorbCrystalType.FULL_PARTY && (doLevelup) || absorbType == L2NpcTemplate.AbsorbCrystalType.PARTY_ONE_RANDOM && (doLevelup) || dice <= chanceLevelUp)
			{
				// Give staged crystal
				exchangeCrystal(player, crystalOLD, crystalNEW, false);
			}
			
			// If true and not a last-hit mob, break the crystal.
			else if (!isBossMob && dice >= 100.0 - SoulCrystal.BREAK_CHANCE)
			{
				// Remove current crystal an give a broken open.
				if (crystalNME.startsWith("red"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.RED_BROKEN_CRYSTAL, true);
				}
				else if (crystalNME.startsWith("gre"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.GRN_BROKEN_CYRSTAL, true);
				}
				else if (crystalNME.startsWith("blu"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.BLU_BROKEN_CRYSTAL, true);
				}
				resetAbsorbList();
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED));
			}
		}
	}
	
	private void exchangeCrystal(L2PcInstance player, int takeid, int giveid, boolean broke)
	{
		L2ItemInstance Item = player.getInventory().destroyItemByItemId("SoulCrystal", takeid, 1, player, this);
		
		if (Item != null)
		{
			// Prepare inventory update packet
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addRemovedItem(Item);
			
			// Add new crystal to the killer's inventory
			Item = player.getInventory().addItem("SoulCrystal", giveid, 1, player, this);
			playerIU.addItem(Item);
			
			// Send a sound event and text message to the player
			if (broke)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_BROKE));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_SUCCEEDED));
			}
			
			// Send system message
			SystemMessage sms = new SystemMessage(SystemMessageId.EARNED_ITEM);
			sms.addItemName(giveid);
			player.sendPacket(sms);
			// Send inventory update packet
			player.sendPacket(playerIU);
		}
	}
	
	private void resetAbsorbList()
	{
		_absorbed = false;
		_absorbersList.clear();
	}
	
	/**
	 * Calculate the Experience and SP to distribute to attacker (L2PcInstance, L2SummonInstance or L2Party) of the L2Attackable.<BR>
	 * <BR>
	 * @param diff The difference of level between attacker (L2PcInstance, L2SummonInstance or L2Party) and the L2Attackable
	 * @param damage The damages given by the attacker (L2PcInstance, L2SummonInstance or L2Party)
	 * @return
	 */
	private int[] calculateExpAndSp(int diff, int damage)
	{
		double xp;
		double sp;
		
		if (diff < -5)
		{
			diff = -5; // makes possible to use ALT_GAME_EXPONENT configuration
		}
		
		xp = (double) getExpReward() * damage / getMaxHp();
		
		if (Config.ALT_GAME_EXPONENT_XP != 0)
		{
			xp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_XP);
		}
		
		sp = (double) getSpReward() * damage / getMaxHp();
		
		if (Config.ALT_GAME_EXPONENT_SP != 0)
		{
			sp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_SP);
		}
		
		if (Config.ALT_GAME_EXPONENT_XP == 0 && Config.ALT_GAME_EXPONENT_SP == 0)
		{
			if (diff > 5) // formula revised May 07
			{
				double pow = Math.pow((double) 5 / 6, diff - 5);
				xp = xp * pow;
				sp = sp * pow;
			}
			
			if (xp <= 0)
			{
				xp = 0;
				sp = 0;
			}
			else if (sp <= 0)
			{
				sp = 0;
			}
		}
		
		int[] tmp =
		{
			(int) xp,
			(int) sp
		};
		
		return tmp;
	}
	
	private int[] calculateExpAndSpCustom(int diff, int damage)
	{
		double xp;
		double sp;
		
		if (diff < -5)
		{
			diff = -5; // makes possible to use ALT_GAME_EXPONENT configuration
		}
		
		xp = (double) getExpRewardCustom() * damage / getMaxHp();
		
		if (Config.ALT_GAME_EXPONENT_XP != 0)
		{
			xp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_XP);
		}
		
		sp = (double) getSpRewardCustom() * damage / getMaxHp();
		
		if (Config.ALT_GAME_EXPONENT_SP != 0)
		{
			sp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_SP);
		}
		
		if (Config.ALT_GAME_EXPONENT_XP == 0 && Config.ALT_GAME_EXPONENT_SP == 0)
		{
			if (diff > 5) // formula revised May 07
			{
				double pow = Math.pow((double) 5 / 6, diff - 5);
				xp = xp * pow;
				sp = sp * pow;
			}
			
			if (xp <= 0)
			{
				xp = 0;
				sp = 0;
			}
			else if (sp <= 0)
			{
				sp = 0;
			}
		}
		
		int[] tmp =
		{
			(int) xp,
			(int) sp
		};
		
		return tmp;
	}
	
	public long calculateOverhitExp(long normalExp)
	{
		// Get the percentage based on the total of extra (over-hit) damage done relative to the total (maximum) ammount of HP on the L2Attackable
		double overhitPercentage = getOverhitDamage() * 100 / getMaxHp();
		
		// Over-hit damage percentages are limited to 25% max
		if (overhitPercentage > 25)
		{
			overhitPercentage = 25;
		}
		
		// Get the overhit exp bonus according to the above over-hit damage percentage
		// (1/1 basis - 13% of over-hit damage, 13% of extra exp is given, and so on...)
		double overhitExp = overhitPercentage / 100 * normalExp;
		
		// Return the rounded ammount of exp points to be added to the player's normal exp reward
		long bonusOverhit = Math.round(overhitExp);
		
		return bonusOverhit;
	}
	
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		// Clear mob spoil,seed
		setSpoilerId(0);
		// Clear all aggro char from list
		clearAggroList();
		// Clear Harvester Rewrard List
		_harvestItems = null;
		// Clear mod Seeded stat
		setSeeded(false);
		
		_sweepItems = null;
		resetAbsorbList();
		
		setWalking();
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		if (!isInActiveRegion())
		{
			if (hasAI())
			{
				getAI().stopAITask();
			}
		}
	}
	
	public void setSeeded()
	{
		if (_seedType != 0 && _seeder != null)
		{
			setSeeded(_seedType, _seeder.getLevel());
		}
	}
	
	public void setSeeded(int id, L2PcInstance seeder)
	{
		if (!_seeded)
		{
			_seedType = id;
			_seeder = seeder;
		}
	}
	
	public void setSeeded(int id, int seederLvl)
	{
		_seeded = true;
		_seedType = id;
		int count = 1;
		
		for (int skillId : getTemplate().getSkills().keySet())
		{
			switch (skillId)
			{
				case 4303: // Strong type x2
					count *= 2;
					break;
				case 4304: // Strong type x3
					count *= 3;
					break;
				case 4305: // Strong type x4
					count *= 4;
					break;
				case 4306: // Strong type x5
					count *= 5;
					break;
				case 4307: // Strong type x6
					count *= 6;
					break;
				case 4308: // Strong type x7
					count *= 7;
					break;
				case 4309: // Strong type x8
					count *= 8;
					break;
				case 4310: // Strong type x9
					count *= 9;
					break;
			}
			
		}
		
		int diff = getLevel() - (L2Manor.getInstance().getSeedLevel(_seedType) - 5);
		
		// hi-lvl mobs bonus
		if (diff > 0)
		{
			count += diff;
		}
		
		FastList<RewardItem> harvested = new FastList<>();
		
		harvested.add(new RewardItem(L2Manor.getInstance().getCropType(_seedType), count * Config.RATE_DROP_MANOR, 0));
		
		_harvestItems = harvested.toArray(new RewardItem[harvested.size()]);
		
		harvested = null;
	}
	
	public void setSeeded(boolean seeded)
	{
		_seeded = seeded;
	}
	
	public L2PcInstance getSeeder()
	{
		return _seeder;
	}
	
	public int getSeedType()
	{
		return _seedType;
	}
	
	public boolean isSeeded()
	{
		return _seeded;
	}
	
	private int getAbsorbLevel()
	{
		return getTemplate().absorbLevel;
	}
	
	// This is located here because L2Monster and L2FriendlyMob both extend this class. The other non-pc instances extend either L2NpcInstance or L2MonsterInstance.
	@Override
	public boolean hasRandomAnimation()
	{
		return Config.MAX_MONSTER_ANIMATION > 0 && !(this instanceof L2GrandBossInstance);
	}
	
	@Override
	public boolean isMob()
	{
		return true;
	}
	
	protected void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}
	
	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}
	
	public L2CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}
	
	public void setFirstCommandChannelAttacked(L2CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}
	
	public long getCommandChannelLastAttack()
	{
		return _commandChannelLastAttack;
	}
	
	public void setCommandChannelLastAttack(long channelLastAttack)
	{
		_commandChannelLastAttack = channelLastAttack;
	}
	
	private static class CommandChannelTimer implements Runnable
	{
		private L2Attackable _monster;
		
		public CommandChannelTimer(L2Attackable monster)
		{
			_monster = monster;
		}
		
		@Override
		public void run()
		{
			if ((System.currentTimeMillis() - _monster.getCommandChannelLastAttack()) > 900000)
			{
				_monster.setCommandChannelTimer(null);
				_monster.setFirstCommandChannelAttacked(null);
				_monster.setCommandChannelLastAttack(0);
			}
			else
			{
				ThreadPoolManager.getInstance().scheduleGeneral(this, 10000); // 10sec
			}
		}
	}
	
	@Override
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	@Override
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	public void setIsRaidMinion(boolean val)
	{
		_isRaid = val;
		_isRaidMinion = val;
	}
	
	@Override
	public boolean isRaidMinion()
	{
		return _isRaidMinion;
	}
	
	public void setIsBossInstance(boolean val)
	{
		_isBossInstance = val;
	}
	
	@Override
	public boolean isBossInstance()
	{
		return _isBossInstance;
	}
	
	@Override
	public boolean isMinion()
	{
		return getLeader() != null;
	}
	
	public L2Attackable getLeader()
	{
		return null;
	}
	
	public void returnHome()
	{
		clearAggroList();
		
		if (hasAI() && getSpawn() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz(), 0));
		}
	}
	
	public List<L2Character> getHateList()
	{
		List<L2Character> result = new ArrayList<>();
		
		if (getAggroList().isEmpty() || isAlikeDead())
		{
			return result;
		}
		
		for (AggroInfo ai : getAggroList().values())
		{
			if (ai == null)
			{
				continue;
			}
			
			ai.checkHate(this);
			result.add(ai.getAttacker());
		}
		
		return result;
	}
}
