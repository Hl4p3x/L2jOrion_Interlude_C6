package l2jorion.game.model.olympiad;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import l2jorion.Config;
import l2jorion.game.datatables.HeroSkillTable;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.enums.AchType;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2CubicInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2TamedBeastInstance;
import l2jorion.game.model.zone.type.L2OlympiadStadiumZone;
import l2jorion.game.network.PacketServer;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.ExOlympiadMode;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;

public abstract class AbstractOlympiadGame
{
	protected static final Logger LOG = Logger.getLogger(AbstractOlympiadGame.class.getName());
	
	protected static final String POINTS = "olympiad_points";
	protected static final String COMP_DONE = "competitions_done";
	protected static final String COMP_WON = "competitions_won";
	protected static final String COMP_LOST = "competitions_lost";
	protected static final String COMP_DRAWN = "competitions_drawn";
	
	protected long _startTime = 0;
	protected boolean _aborted = false;
	protected final int _stadiumID;
	
	protected AbstractOlympiadGame(int id)
	{
		_stadiumID = id;
	}
	
	public final boolean isAborted()
	{
		return _aborted;
	}
	
	public final int getStadiumId()
	{
		return _stadiumID;
	}
	
	protected boolean makeCompetitionStart()
	{
		_startTime = System.currentTimeMillis();
		return !_aborted;
	}
	
	protected final void addPointsToParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, points);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
		sm.addString(par.name);
		sm.addNumber(points);
		broadcastPacket(sm);
	}
	
	protected final void removePointsFromParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, -points);
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);
		sm.addString(par.name);
		sm.addNumber(points);
		broadcastPacket(sm);
	}
	
	protected static SystemMessage checkDefaulted(L2PcInstance player)
	{
		if (player == null || player.isOnline() == 0)
		{
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_ENDS_THE_GAME);
		}
		
		if (player.getClient() == null || player.getClient().isDetached())
		{
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_ENDS_THE_GAME);
		}
		
		if (player.inObserverMode())
		{
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.CANNOT_PARTICIPATE_OLYMPIAD_WHILE_DEAD);
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		
		if (player.isSubClassActive())
		{
			player.sendPacket(SystemMessageId.SINCE_YOU_HAVE_CHANGED_YOUR_CLASS_INTO_A_SUB_JOB_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		
		if (player.isCursedWeaponEquipped())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_JOIN_OLYMPIAD_POSSESSING_S1).addItemName(player.getCursedWeaponEquipedId()));
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		
		if (player.getInventoryLimit() * 0.8 <= player.getInventory().getSize())
		{
			player.sendPacket(SystemMessageId.SINCE_80_PERCENT_OR_MORE_OF_YOUR_INVENTORY_SLOTS_ARE_FULL_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			return SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_DOES_NOT_MEET_THE_REQUIREMENTS_FOR_JOINING_THE_GAME);
		}
		
		return null;
	}
	
	protected static final boolean portPlayerToArena(Participant par, Location loc, int id)
	{
		final L2PcInstance player = par.player;
		if (player == null || player.isOnline() == 0)
		{
			return false;
		}
		
		try
		{
			player.setLastLocation();
			
			if (player.isSitting())
			{
				player.standUp();
			}
			
			player.setTarget(null);
			
			player.setOlympiadGameId(id);
			player.setIsInOlympiadMode(true);
			player.setIsOlympiadStart(false);
			player.setOlympiadSide(par.side);
			player.teleToLocation(loc, 0);
			player.sendPacket(new ExOlympiadMode(par.side));
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	protected static final void removals(L2PcInstance player, boolean removeParty)
	{
		try
		{
			if (player == null)
			{
				return;
			}
			
			// Remove Buffs
			player.stopAllEffects();
			
			// Remove Summon's Buffs
			if (player.getPet() != null)
			{
				L2Summon summon = player.getPet();
				summon.stopAllEffects();
				
				if (summon instanceof L2PetInstance)
				{
					summon.unSummon(player);
				}
			}
			
			// Remove Tamed Beast
			if (player.getTrainedBeast() != null)
			{
				L2TamedBeastInstance traindebeast = player.getTrainedBeast();
				traindebeast.stopAllEffects();
				
				traindebeast.doDespawn();
			}
			
			if (Config.REMOVE_CUBIC_OLYMPIAD)
			{
				if (player.getCubics() != null)
				{
					for (L2CubicInstance cubic : player.getCubics().values())
					{
						cubic.stopAction();
						player.delCubic(cubic.getId());
					}
					player.getCubics().clear();
				}
			}
			else if (player.getCubics() != null)
			{
				boolean removed = false;
				for (L2CubicInstance cubic : player.getCubics().values())
				{
					if (cubic.givenByOther())
					{
						cubic.stopAction();
						player.delCubic(cubic.getId());
						removed = true;
					}
				}
				
				if (removed)
				{
					player.broadcastUserInfo();
				}
			}
			
			if (Config.ALLOW_DRESS_ME_SYSTEM && !Config.ALLOW_DRESS_ME_IN_OLY)
			{
				player.setArmorSkinOption(0);
				player.setWeaponSkinOption(0);
				player.setHairSkinOption(0);
				player.setFaceSkinOption(0);
				
				player.broadcastUserInfo();
			}
			
			// Remove Clan Skills
			if (player.getClan() != null)
			{
				for (L2Skill skill : player.getClan().getAllSkills())
				{
					player.removeSkill(skill, false);
				}
			}
			
			// Abort casting if player casting
			player.abortAttack();
			player.abortCast();
			
			// Force the character to be visible
			player.getAppearance().setVisible();
			
			// Remove Hero Skills
			if (player.isHero())
			{
				for (L2Skill skill : HeroSkillTable.getHeroSkills())
				{
					player.removeSkill(skill, false);
				}
			}
			
			// Remove Restricted skills
			for (L2Skill skill : player.getAllSkills())
			{
				if (Config.LIST_OLY_RESTRICTED_SKILLS.contains(skill.getId()))
				{
					player.removeSkill(skill, false);
				}
			}
			
			// Heal Player fully
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			
			// Remove player from his party
			if (removeParty)
			{
				final L2Party party = player.getParty();
				if (party != null)
				{
					party.removePartyMember(player, true);
				}
			}
			
			player.checkItemRestriction();
			
			// Remove shot automation
			Set<Integer> activeSoulShots = player.getAutoSoulShot();
			for (int itemId : activeSoulShots)
			{
				player.removeAutoSoulShot(itemId);
				ExAutoSoulShot atk = new ExAutoSoulShot(itemId, 0);
				player.sendPacket(atk);
			}
			
			// Discharge any active shots
			if (player.getActiveWeaponInstance() != null)
			{
				player.getActiveWeaponInstance().setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
				player.getActiveWeaponInstance().setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			}
			
			// Skill recharge
			if (Config.ALT_OLY_RECHARGE_SKILLS)
			{
				for (L2Skill skill : player.getAllSkills())
				{
					player.enableSkill(skill);
				}
				
				player.updateEffectIcons();
			}
			
			player.sendSkillList();
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	protected static final void buffPlayer(L2PcInstance player)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(1204, 2); // Windwalk 2
		if (skill != null)
		{
			skill.getEffects(player, player);
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(1204));
		}
		
		if (!player.isMageClass())
		{
			skill = SkillTable.getInstance().getInfo(1086, 1); // Haste 1
			if (skill != null)
			{
				skill.getEffects(player, player);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(1086));
			}
		}
		else
		{
			skill = SkillTable.getInstance().getInfo(1085, 1); // Acumen 1
			if (skill != null)
			{
				skill.getEffects(player, player);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(1086));
			}
		}
	}
	
	protected static final void healPlayer(L2PcInstance player)
	{
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
	}
	
	protected static final void cleanEffects(L2PcInstance player)
	{
		try
		{
			player.setIsOlympiadStart(false);
			player.setTarget(null);
			player.abortAttack();
			player.abortCast();
			player.getAI().setIntention(AI_INTENTION_IDLE);
			
			final L2Summon summon = player.getPet();
			if (summon != null && !summon.isDead())
			{
				summon.setTarget(null);
				summon.abortAttack();
				summon.abortCast();
				summon.getAI().setIntention(AI_INTENTION_IDLE);
			}
			
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			
			player.getStatus().startHpMpRegeneration();
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	protected static final void playerStatusBack(L2PcInstance player)
	{
		try
		{
			player.setIsInOlympiadMode(false);
			player.setIsOlympiadStart(false);
			player.setOlympiadSide(-1);
			player.setOlympiadGameId(-1);
			player.sendPacket(new ExOlympiadMode(0));
			
			// Remove Buffs
			player.stopAllEffects();
			
			player.getStatus().startHpMpRegeneration();
			
			final L2Summon summon = player.getPet();
			if (summon != null && !summon.isDead())
			{
				summon.stopAllEffects();
			}
			
			// Add Clan Skills
			if (player.getClan() != null)
			{
				for (L2Skill skill : player.getClan().getAllSkills())
				{
					if (skill.getMinPledgeClass() <= player.getPledgeClass())
					{
						player.addSkill(skill, false);
					}
				}
			}
			
			// Add Hero Skills
			if (player.isHero())
			{
				for (L2Skill skill : HeroSkillTable.getHeroSkills())
				{
					player.addSkill(skill, false);
				}
			}
			
			// Return Restricted Skills
			for (L2Skill skill : player.getAllSkills())
			{
				player.addSkill(skill, false);
			}
			
			player.sendSkillList();
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	protected static final void portPlayerBack(L2PcInstance player)
	{
		if (player == null)
		{
			return;
		}
		
		final Location loc = player.getLastLocation();
		if ((loc.getX() == 0) && (loc.getY() == 0))
		{
			return;
		}
		player.setIsPendingRevive(false);
		player.setInstanceId(0);
		player.teleToLocation(loc);
		player.unsetLastLocation();
		player.getAchievement().increase(AchType.OLYMPIAD_FIGHT);
	}
	
	public static final void rewardParticipant(L2PcInstance player, int[][] reward)
	{
		if (player == null || player.isOnline() == 0 || reward == null)
		{
			return;
		}
		
		try
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (int[] it : reward)
			{
				if (it == null || it.length != 2)
				{
					continue;
				}
				
				final L2ItemInstance item = player.getInventory().addItem("Olympiad", it[0], it[1], player, null);
				if (item == null)
				{
					continue;
				}
				
				iu.addModifiedItem(item);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(it[0]).addNumber(it[1]));
			}
			player.sendPacket(iu);
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	public abstract CompetitionType getType();
	
	public abstract String[] getPlayerNames();
	
	public abstract boolean containsParticipant(int playerId);
	
	public abstract void sendOlympiadInfo(L2Character player);
	
	public abstract void broadcastOlympiadInfo(L2OlympiadStadiumZone stadium);
	
	protected abstract void broadcastPacket(PacketServer packet);
	
	protected abstract boolean checkDefaulted();
	
	protected abstract void removals();
	
	protected abstract void buffPlayers();
	
	protected abstract void healPlayers();
	
	protected abstract boolean portPlayersToArena(List<Location> spawns);
	
	protected abstract void cleanEffects();
	
	protected abstract void portPlayersBack();
	
	protected abstract void playersStatusBack();
	
	protected abstract void clearPlayers();
	
	protected abstract void handleDisconnect(L2PcInstance player);
	
	protected abstract void resetDamage();
	
	protected abstract void addDamage(L2PcInstance player, int damage);
	
	protected abstract boolean checkBattleStatus();
	
	protected abstract boolean haveWinner();
	
	protected abstract void validateWinner(L2OlympiadStadiumZone stadium);
	
	protected abstract int getDivider();
	
	protected abstract int[][] getReward();
}