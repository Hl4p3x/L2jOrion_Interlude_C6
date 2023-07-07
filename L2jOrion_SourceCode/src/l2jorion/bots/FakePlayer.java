package l2jorion.bots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2jorion.Config;
import l2jorion.bots.ai.FakePlayerAI;
import l2jorion.bots.helpers.FakeHelpers;
import l2jorion.bots.model.FarmLocation;
import l2jorion.bots.model.WalkNode;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.autofarm.AutofarmManager;
import l2jorion.game.cache.WarehouseCacheManager;
import l2jorion.game.controllers.RecipeController;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.enums.AchType;
import l2jorion.game.geo.GeoData;
import l2jorion.game.handler.skill.SiegeFlag;
import l2jorion.game.handler.skill.StrSiegeAssault;
import l2jorion.game.handler.skill.TakeCastle;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.FortSiegeManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillTargetType;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.Location;
import l2jorion.game.model.PartyMatchRoom;
import l2jorion.game.model.PartyMatchRoomList;
import l2jorion.game.model.PartyMatchWaitingList;
import l2jorion.game.model.actor.appearance.PcAppearance;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.actor.instance.L2RaidBossInstance;
import l2jorion.game.model.actor.instance.L2SummonInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.effects.EffectCharge;
import l2jorion.game.skills.l2skills.L2SkillSummon;
import l2jorion.game.templates.L2PcTemplate;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;

public class FakePlayer extends L2PcInstance
{
	private FakePlayerAI _fakeAi;
	private boolean _underControl = false;
	
	private Class<? extends L2Character> targetClass = L2PcInstance.class;
	private int targetRange = 2000;
	private int maxTargetRange = 2000;
	private int actionId = 0;
	private int locationId = 0;
	protected int townId = 0;
	protected int botMode = 0;
	protected int zoneId = 0;
	private FarmLocation distance;
	
	private int x;
	private int y;
	
	private WalkNode _currentWalkNode;
	private boolean isWalking = false;
	
	public boolean isUnderControl()
	{
		return _underControl;
	}
	
	public void setUnderControl(boolean underControl)
	{
		_underControl = underControl;
	}
	
	public Class<? extends L2Character> getTargetClass()
	{
		return targetClass;
	}
	
	public void setTargetClass(Class<? extends L2Character> target)
	{
		targetClass = target;
	}
	
	public void setTargetRange(int range)
	{
		targetRange = range;
	}
	
	public int getTargetRange()
	{
		return targetRange;
	}
	
	public void setMaxTargetRange(int range)
	{
		maxTargetRange = range;
	}
	
	public int getMaxTargetRange()
	{
		return maxTargetRange;
	}
	
	protected FakePlayer(int objectId)
	{
		super(objectId);
	}
	
	public FakePlayer(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template, accountName, app);
	}
	
	public FakePlayerAI getFakeAi()
	{
		return _fakeAi;
	}
	
	public void setFakeAi(FakePlayerAI fakeAi)
	{
		_fakeAi = fakeAi;
	}
	
	public void assignDefaultAI()
	{
		try
		{
			setFakeAi(FakeHelpers.getAIbyClassId(getClassId()).getConstructor(FakePlayer.class).newInstance(this));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean checkUseMagicConditions(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (skill == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (isSkillDisabled(skill))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (inObserverMode())
		{
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster is sitting
		if (isSitting() && !skill.isPotion())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough MP
		if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Can't use Hero and resurrect skills during Olympiad
		if (isInOlympiadMode() && (skill.isHeroSkill() || skill.getSkillType() == SkillType.RESURRECT))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		int skill_id = skill.getId();
		
		// Check if the skill type is TOGGLE
		if (skill.isToggle())
		{
			// Like L2OFF you can't use fake death if you are mounted
			if (skill.getId() == 60 && isMounted())
			{
				return false;
			}
			
			L2Effect effect = getFirstEffect(skill);
			
			// Like L2OFF toogle skills have little delay
			if (TOGGLE_USE != 0 && TOGGLE_USE + 400 > System.currentTimeMillis())
			{
				TOGGLE_USE = 0;
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			TOGGLE_USE = System.currentTimeMillis();
			if (effect != null)
			{
				effect.exit(false);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if it's okay to summon siege pets
		// siege golem (13), Wild Hog Cannon (299), Swoop Cannon (448)
		if ((skill_id == 13 || skill_id == 299 || skill_id == 448) && !SiegeManager.getInstance().checkIfOkToSummon(this, false) && !FortSiegeManager.getInstance().checkIfOkToSummon(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// triggered skills cannot be used directly
		if (_triggeredSkills.size() > 0)
		{
			if (_triggeredSkills.get(skill.getId()) != null)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		L2Object target = null;
		
		SkillTargetType skillTargetType = skill.getTargetType();
		SkillType skillType = skill.getSkillType();
		Location worldPosition = getCurrentSkillWorldPosition();
		
		if (skillTargetType == SkillTargetType.TARGET_GROUND && worldPosition == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		switch (skillTargetType)
		{
			case TARGET_AREA_CORPSE_MOB:
				if (skillType == L2Skill.SkillType.SWEEP)
				{
					target = this;
					break;
				}
				target = getTarget();
				break;
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
				// case TARGET_AREA_UNDEAD:
				// case TARGET_AREA_CORPSE_MOB:
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CORPSE_ALLY:
			case TARGET_CLAN:
			case TARGET_CORPSE_CLAN:
			case TARGET_GROUND:
			case TARGET_SELF:
			{
				target = this;
				break;
			}
			case TARGET_PET:
			{
				target = getPet();
				break;
			}
			default:
			{
				target = getTarget();
				break;
			}
		}
		
		// Check the validity of the target
		if (target == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the spell consume an item
		if (skill.getItemConsume() > 0)
		{
			// Get the L2ItemInstance consumed by the spell
			L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Check if the caster owns enough consumed Item to cast
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Player can't heal rb config
		if (!Config.PLAYERS_CAN_HEAL_RB && !isGM() && (target instanceof L2RaidBossInstance || target instanceof L2GrandBossInstance) && (skill.getSkillType() == SkillType.HEAL || skill.getSkillType() == SkillType.HEAL_PERCENT))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Player can't burn mana rb config
		if (!Config.PLAYERS_CAN_BURN_MANA_RB && !isGM() && (target instanceof L2RaidBossInstance || target instanceof L2GrandBossInstance) && (skill.getSkillType() == SkillType.MANADAM))
		{
			this.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (target instanceof L2NpcInstance && Config.DISABLE_ATTACK_NPC_TYPE)
		{
			String mobtype = ((L2NpcInstance) target).getTemplate().type;
			if (!Config.LIST_ALLOWED_NPC_TYPES.contains(mobtype))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Like L2OFF you can't heal random purple people without using CTRL
		SkillDat skilldat = getCurrentSkill();
		if (skilldat != null && skill.getSkillType() == SkillType.HEAL && !skilldat.isCtrlPressed() && target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() == 1 && this != target)
		{
			if ((getClanId() == 0 || ((L2PcInstance) target).getClanId() == 0) || (getClanId() != ((L2PcInstance) target).getClanId()))
			{
				if ((getAllyId() == 0 || ((L2PcInstance) target).getAllyId() == 0) || (getAllyId() != ((L2PcInstance) target).getAllyId()))
				{
					if ((getParty() == null || ((L2PcInstance) target).getParty() == null) || (!getParty().equals(((L2PcInstance) target).getParty())))
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
			}
		}
		
		// Are the target and the player in the same duel?
		if (isInDuel())
		{
			if (!(target instanceof L2PcInstance && ((L2PcInstance) target).getDuelId() == getDuelId()) && !(target instanceof L2SummonInstance && ((L2Summon) target).getOwner().getDuelId() == getDuelId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Pk protection config
		if (skill.isOffensive() && !isGM() && target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() == 0 && ((L2PcInstance) target).getKarma() == 0 && (getLevel() < Config.ALT_PLAYER_PROTECTION_LEVEL || ((L2PcInstance) target).getLevel() < Config.ALT_PLAYER_PROTECTION_LEVEL))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (skill.isOffensive() && target != this && target instanceof L2PcInstance && !isCursedWeaponEquiped() && ((L2PcInstance) target).getSiegeState() == 0 && (!checkAntiFarm((L2PcInstance) target)))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if all skills are disabled
		if (isAllSkillsDisabled() && !getAccessLevel().allowPeaceAttack())
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// prevent casting signets to peace zone
		if (skill.getSkillType() == SkillType.SIGNET || skill.getSkillType() == SkillType.SIGNET_CASTTIME)
		{
			if (isInsidePeaceZone(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Like L2OFF if you are mounted on wyvern you can't use own skills
		if (isFlying())
		{
			if (skill_id != 327 && skill_id != 4289 && !skill.isPotion())
			{
				return false;
			}
		}
		
		// Like L2OFF if you have a summon you can't summon another one (ignore cubics)
		if (skillType == L2Skill.SkillType.SUMMON && skill instanceof L2SkillSummon && !((L2SkillSummon) skill).isCubic())
		{
			if (getPet() != null || isMounted())
			{
				return false;
			}
		}
		
		if (skill.getNumCharges() > 0 && skill.getSkillType() != SkillType.CHARGE && skill.getSkillType() != SkillType.CHARGEDAM && skill.getSkillType() != SkillType.CHARGE_EFFECT && skill.getSkillType() != SkillType.PDAM)
		{
			EffectCharge effect = (EffectCharge) getFirstEffect(L2Effect.EffectType.CHARGE);
			if (effect == null || effect.numCharges < skill.getNumCharges())
			{
				sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE));
				return false;
			}
			effect.numCharges -= skill.getNumCharges();
			sendPacket(new EtcStatusUpdate(this));
			
			if (effect.numCharges == 0)
			{
				effect.exit(false);
			}
		}
		// ************************************* Check Casting Conditions *******************************************
		// Check if the caster own the weapon needed
		if (!skill.getWeaponDependancy(this))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// ************************************* Check Player State *******************************************
		// Check if the player use "Fake Death" skill
		if (isAlikeDead() && !skill.isPotion() && skill.getSkillType() != L2Skill.SkillType.FAKE_DEATH)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Only fishing skills are available
		if (isFishing() && skillType != SkillType.PUMPING && skillType != SkillType.REELING && skillType != SkillType.FISHING)
		{
			return false;
		}
		
		// ************************************* Check Skill Type *******************************************
		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			boolean peace = isInsidePeaceZone(this, target);
			// Like L2OFF you can use cupid bow skills on peace zone
			// Like L2OFF people can use TARGET_AURE skills on peace zone
			if (peace && (skill.getId() != 3261 && skill.getId() != 3260 && skill.getId() != 3262 && skillTargetType != SkillTargetType.TARGET_AURA))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
			if (isInOlympiadMode() && !isOlympiadStart() && skillTargetType != SkillTargetType.TARGET_AURA)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !ctrlPressed)
			{
				switch (skillTargetType)
				{
					case TARGET_AURA:
					case TARGET_FRONT_AURA:
					case TARGET_BEHIND_AURA:
					case TARGET_AREA_UNDEAD:
					case TARGET_AREA_CORPSE_MOB:
					case TARGET_PARTY:
					case TARGET_ALLY:
					case TARGET_CORPSE_ALLY:
					case TARGET_CLAN:
					case TARGET_CORPSE_CLAN:
					case TARGET_GROUND:
					case TARGET_SELF:
					{
						break;
					}
					default:
					{
						if ((_inEventTvT && TvT.is_started()) || (_inEventDM && !DM.is_started()) || (_inEventCTF && !CTF.is_started()) || (_inEventVIP && !VIP._started))
						{
							sendPacket(ActionFailed.STATIC_PACKET);
							return false;
						}
						
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
			}
			// Check if the target is in the skill cast range
			if (shiftPressed)
			{
				// Calculate the distance between the L2PcInstance and the target
				if (skillTargetType == SkillTargetType.TARGET_GROUND)
				{
					if (!isInsideRadius(getCurrentSkillWorldPosition().getX(), getCurrentSkillWorldPosition().getY(), getCurrentSkillWorldPosition().getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
				else if (skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange() + getTemplate().collisionRadius, false, false)) // Calculate the distance between the L2PcInstance and the target
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (skillType == SkillType.SIGNET) // Check range for SIGNET skills
			{
				if (!isInsideRadius(getCurrentSkillWorldPosition().getX(), getCurrentSkillWorldPosition().getY(), getCurrentSkillWorldPosition().getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		// Check if the skill is defensive
		if (!skill.isOffensive())
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
			if (target instanceof L2MonsterInstance && !ctrlPressed && skillTargetType != SkillTargetType.TARGET_PET && skillTargetType != SkillTargetType.TARGET_AURA && skillTargetType != SkillTargetType.TARGET_CLAN && skillTargetType != SkillTargetType.TARGET_SELF
				&& skillTargetType != SkillTargetType.TARGET_PARTY && skillTargetType != SkillTargetType.TARGET_ALLY && skillTargetType != SkillTargetType.TARGET_CORPSE_MOB && skillTargetType != SkillTargetType.TARGET_AREA_CORPSE_MOB && skillTargetType != SkillTargetType.TARGET_GROUND
				&& skillType != SkillType.BEAST_FEED && skillType != SkillType.DELUXE_KEY_UNLOCK && skillType != SkillType.UNLOCK)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (skillType == SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if the skill is Sweep type and if conditions not apply
		if (skillType == SkillType.SWEEP && target instanceof L2Attackable)
		{
			int spoilerId = ((L2Attackable) target).getSpoilerId();
			
			if (((L2Attackable) target).isDead())
			{
				if (spoilerId == 0)
				{
					sendPacket(new SystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				if (!isInLooterParty(spoilerId))
				{
					sendPacket(new SystemMessage(SystemMessageId.SWEEP_NOT_ALLOWED));
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (skillType == SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (skillTargetType)
		{
			case TARGET_PARTY:
			case TARGET_CORPSE_ALLY:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_AURA:
			case TARGET_SELF:
			case TARGET_GROUND:
				break;
			default:
				// if pvp skill is not allowed for given target
				if (!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack() && (skill.getId() != 3261 && skill.getId() != 3260 && skill.getId() != 3262))
				{
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
		}
		
		if (skillTargetType == SkillTargetType.TARGET_HOLY && !TakeCastle.checkIfOkToCastSealOfRule(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return false;
		}
		
		if (skillType == SkillType.SIEGEFLAG && !SiegeFlag.checkIfOkToPlaceFlag(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return false;
		}
		else if (skillType == SkillType.STRSIEGEASSAULT && !StrSiegeAssault.checkIfOkToUseStriderSiegeAssault(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return false;
		}
		
		if ((target instanceof L2GrandBossInstance) && ((L2GrandBossInstance) target).getNpcId() == 29022)
		{
			if (Math.abs(getClientZ() - target.getZ()) > 200)
			{
				sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// GeoData check
		if (skill.getCastRange() > 0 && !GeoData.getInstance().canSeeTarget(this, target))
		{
			sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		return true;
	}
	
	public void forceAutoAttack(L2Character creature)
	{
		if (getTarget() == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (isInsidePeaceZone(this, getTarget()))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (isInOlympiadMode() && getTarget() != null && getTarget() instanceof L2PlayableInstance)
		{
			L2PcInstance target = getTarget().getActingPlayer();
			if (target == null || (target.isInOlympiadMode() && (!isOlympiadStart() || getOlympiadGameId() != target.getOlympiadGameId())))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		if (getTarget() != null && !getTarget().isAttackable() && !getAccessLevel().allowPeaceAttack())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (isConfused())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Notify AI with ATTACK
		getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getTarget());
	}
	
	public void rndWalk(int x, int y, int z)
	{
		int posX = x + Rnd.get(-150, 150);
		int posY = y + Rnd.get(-150, 150);
		int posZ = z;
		
		getAI().moveTo(posX, posY, posZ);
	}
	
	public synchronized void despawnPlayer()
	{
		AutofarmManager.INSTANCE.onPlayerLogout(this);
		
		if (inObserverMode())
		{
			setXYZInvisible(_lastLoc.getX(), _lastLoc.getY(), _lastLoc.getZ());
		}
		
		if (isTeleporting())
		{
			try
			{
				wait(2000);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			onTeleported();
		}
		
		Castle castle = null;
		if (getClan() != null)
		{
			castle = CastleManager.getInstance().getCastleByOwner(getClan());
			if (castle != null)
			{
				castle.destroyClanGate();
			}
		}
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			setOnlineStatus(false);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
			
		}
		
		// Stop crafting, if in progress
		try
		{
			RecipeController.getInstance().requestMakeItemAbort(this);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Cancel Attak or Cast
		try
		{
			abortAttack();
			abortCast();
			setTarget(null);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		PartyMatchWaitingList.getInstance().removePlayer(this);
		if (_partyroom != 0)
		{
			PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
			if (room != null)
			{
				room.deleteMember(this);
			}
		}
		
		// Remove from world regions zones
		if (getWorldRegion() != null)
		{
			getWorldRegion().removeFromZones(this);
		}
		
		try
		{
			if (_forceBuff != null)
			{
				abortCast();
			}
			
			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if (character.getForceBuff() != null && character.getForceBuff().getTarget() == this)
				{
					character.abortCast();
				}
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Remove the L2PcInstance from the world
		if (isVisible())
		{
			try
			{
				decayMe();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		// If a Party is in progress, leave it
		if (isInParty())
		{
			try
			{
				leaveParty();
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try
			{
				getPet().unSummon(this);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		if (getClanId() != 0 && getClan() != null)
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				L2ClanMember clanMember = getClan().getClanMember(getName());
				if (clanMember != null)
				{
					clanMember.setPlayerInstance(null);
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			setActiveRequester(null);
		}
		
		if (OlympiadManager.getInstance().isRegistered(this) || getOlympiadGameId() != -1)
		{
			OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
		}
		
		// If the L2PcInstance is a GM, remove it from the GM List
		if (isGM())
		{
			try
			{
				GmListTable.getInstance().deleteGm(this);
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					LOG.error("deleteMe()", e);
				}
			}
		}
		
		// Update database with items in its inventory and remove them from the world
		try
		{
			getInventory().deleteMe();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Update database with items in its warehouse and remove them from the world
		try
		{
			clearWarehouse();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().remCacheTask(this);
		}
		
		// Update database with items in its freight and remove them from the world
		try
		{
			getFreight().deleteMe();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				LOG.error("deleteMe()", e);
			}
		}
		
		closeNetConnection();
		
		if (getClanId() > 0)
		{
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
		}
		
		for (L2PcInstance player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (L2PcInstance player : _snoopListener)
		{
			player.removeSnooped(this);
		}
		
		if (_chanceSkills != null)
		{
			_chanceSkills.setOwner(null);
			_chanceSkills = null;
		}
		
		notifyFriends(false);
		
		L2World.getInstance().removeObject(this);
		L2World.getInstance().removeFromAllPlayers(this); // force remove in case of crash during teleport
		
	}
	
	public void heal()
	{
		setCurrentCp(getMaxCp());
		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());
	}
	
	@SuppressWarnings("unchecked")
	public final <A> List<A> getKnownTypeInRadius(Class<A> type, int radius)
	{
		final L2WorldRegion region = getWorldRegion();
		if (region == null)
		{
			return Collections.emptyList();
		}
		
		final List<A> result = new ArrayList<>();
		
		for (L2WorldRegion reg : region.getSurroundingRegions())
		{
			for (L2Object target : reg.getVisibleObjects().values())
			{
				if (target == this || !type.isAssignableFrom(target.getClass()) || target.isDead() || !Util.checkIfInRange(radius, target, this, true))
				{
					continue;
				}
				
				if (!target.isMonster() && !target.isPlayer())
				{
					continue;
				}
				
				if (target.isPlayer() && ((L2PcInstance) target).getPvpFlag() == 0)
				{
					continue;
				}
				
				// Remove same clan from attack list
				if (target.isPlayer() && isPlayer() && ((L2PcInstance) target).getClan() != null && getClan() != null && getClanId() == ((L2PcInstance) target).getClanId())
				{
					continue;
				}
				
				if (getDistance() != null && !target.isInsideRadius(getDistance().getX(), getDistance().getY(), getDistance().getZ(), Config.BOTS_FARM_ZONE_MAX_DISTANCE, false, false))
				{
					continue;
				}
				
				result.add((A) target);
			}
		}
		
		return result;
	}
	
	@Override
	public void increasePvpKills()
	{
		if (Config.BOTS_INCREASE_PVP_POINT)
		{
			// Add karma to attacker and increase its PK counter
			setPvpKills(getPvpKills() + 1);
		}
		
		getAchievement().increase(AchType.PVP);
		
		if (Config.PVPEXPSP_SYSTEM)
		{
			addExpAndSp(Config.ADD_EXP, Config.ADD_SP);
			{
				sendMessage("Earned Exp & SP for a pvp kill");
			}
		}
		
		if (getTitleOn())
		{
			updateTitle();
		}
		
		// Update the character's name color if they reached any of the 5 PvP levels.
		updatePvPColor(getPvpKills());
		
		if (Config.ALLOW_QUAKE_SYSTEM)
		{
			QuakeSystem();
		}
		
		broadcastUserInfo();
	}
	
	@Override
	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		// we don't need any item from mobs for bot
	}
	
	@Override
	public synchronized void rewardSkills(final boolean restore)
	{
		if (!Config.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills();
		}
		
		super.rewardSkills(restore);
	}
	
	public void setActionId(int id)
	{
		actionId = id;
	}
	
	public int getActionId()
	{
		return actionId;
	}
	
	public void setLocId(int id)
	{
		locationId = id;
	}
	
	public int getLocId()
	{
		return locationId;
	}
	
	public void setTownId(int id)
	{
		townId = id;
	}
	
	public int getTownId()
	{
		return townId;
	}
	
	public void setBotMode(int mode)
	{
		botMode = mode;
	}
	
	public int getBotMode()
	{
		return botMode;
	}
	
	public void setZoneId(int id)
	{
		zoneId = id;
	}
	
	public int getZoneId()
	{
		return zoneId;
	}
	
	public FarmLocation getDistance()
	{
		return distance;
	}
	
	public void setOldTargetX(int x)
	{
		this.x = x;
	}
	
	public int getOldTargetX()
	{
		return x;
	}
	
	public void setOldTargetY(int y)
	{
		this.y = y;
	}
	
	public int getOldTargetY()
	{
		return y;
	}
	
	public void setDistance(FarmLocation loc)
	{
		distance = loc;
	}
	
	public void seCurrentWalkNode(WalkNode node)
	{
		_currentWalkNode = node;
	}
	
	public WalkNode getCurrentWalkNode()
	{
		return _currentWalkNode;
	}
	
	public boolean isWalking()
	{
		return isWalking;
	}
	
	public void setIsWalking(boolean walking)
	{
		isWalking = walking;
	}
	
	@Override
	public boolean isBot()
	{
		return true;
	}
}
