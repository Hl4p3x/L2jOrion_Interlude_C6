/* This program is free software; you can redistribute it and/or modify
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
package l2jorion.game.model.actor.stat;

import l2jorion.Config;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.model.actor.instance.L2ClassMasterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.base.ClassLevel;
import l2jorion.game.model.base.PlayerClass;
import l2jorion.game.model.base.SubClass;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class PcStat extends PlayableStat
{
	private static Logger LOG = LoggerFactory.getLogger(PcStat.class.getName());
	
	private int _oldMaxHp; // stats watch
	private int _oldMaxMp; // stats watch
	private int _oldMaxCp; // stats watch
	
	public PcStat(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addExp(long value)
	{
		L2PcInstance activeChar = getActiveChar();
		
		// Player is Gm and access level is below or equal to canGainExp and is in party, don't give Xp
		if (!getActiveChar().getAccessLevel().canGainExp() && getActiveChar().isInParty())
		{
			return false;
		}
		
		if (!super.addExp(value))
		{
			return false;
		}
		
		// Set new karma
		if (!activeChar.isCursedWeaponEquiped() && activeChar.getKarma() > 0 && (activeChar.isGM() || !activeChar.isInsideZone(ZoneId.ZONE_PVP)))
		{
			int karmaLost = activeChar.calculateKarmaLost(value);
			
			if (karmaLost > 0)
			{
				activeChar.setKarma(activeChar.getKarma() - karmaLost);
			}
		}
		
		activeChar.sendPacket(new UserInfo(activeChar));
		
		return true;
	}
	
	/**
	 * Add Experience and SP rewards to the L2PcInstance, remove its Karma (if necessary) and Launch increase level task.<BR>
	 * <BR>
	 * <B><U> Actions </U> :</B><BR>
	 * <BR>
	 * <li>Remove Karma when the player kills L2MonsterInstance</li>
	 * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance</li>
	 * <li>Send a Server->Client System Message to the L2PcInstance</li>
	 * <li>If the L2PcInstance increases it's level, send a Server->Client packet SocialAction (broadcast)</li>
	 * <li>If the L2PcInstance increases it's level, manage the increase level task (Max MP, Max MP, Recommendation, Expertise and beginner skills...)</li>
	 * <li>If the L2PcInstance increases it's level, send a Server->Client packet UserInfo to the L2PcInstance</li><BR>
	 * <BR>
	 * @param addToExp The Experience value to add
	 * @param addToSp The SP value to add
	 */
	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		float ratioTakenByPlayer = 0;
		
		// Player is Gm and access level is below or equal to GM_DONT_TAKE_EXPSP and is in party, don't give Xp/Sp
		L2PcInstance activeChar = getActiveChar();
		if (!activeChar.getAccessLevel().canGainExp() && activeChar.isInParty())
		{
			return false;
		}
		
		// if this player has a pet that takes from the owner's Exp, give the pet Exp now
		
		if (activeChar.getPet() instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) activeChar.getPet();
			ratioTakenByPlayer = pet.getPetData().getOwnerExpTaken() / 100f;
			
			// only give exp/sp to the pet by taking from the owner if the pet has a non-zero, positive ratio
			// allow possible customizations that would have the pet earning more than 100% of the owner's exp/sp
			
			if (ratioTakenByPlayer > 1)
			{
				ratioTakenByPlayer = 1;
			}
			
			if (!pet.isDead())
			{
				pet.addExpAndSp((long) (addToExp * (1 - ratioTakenByPlayer)), (int) (addToSp * (1 - ratioTakenByPlayer)));
			}
			
			// now adjust the max ratio to avoid the owner earning negative exp/sp
			addToExp = (long) (addToExp * ratioTakenByPlayer);
			addToSp = (int) (addToSp * ratioTakenByPlayer);
		}
		
		if (!super.addExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		// Send a Server->Client System Message to the L2PcInstance
		SystemMessage sm = new SystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP);
		sm.addNumber((int) addToExp);
		sm.addNumber(addToSp);
		getActiveChar().sendPacket(sm);
		
		return true;
	}
	
	@Override
	public boolean removeExpAndSp(long addToExp, int addToSp)
	{
		if (!super.removeExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		// Send a Server->Client System Message to the L2PcInstance
		SystemMessage sm = new SystemMessage(SystemMessageId.EXP_DECREASED_BY_S1);
		sm.addNumber((int) addToExp);
		getActiveChar().sendPacket(sm);
		sm = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
		sm.addNumber(addToSp);
		getActiveChar().sendPacket(sm);
		
		return true;
	}
	
	@Override
	public final boolean addLevel(byte value)
	{
		if (getLevel() + value > ExperienceData.getInstance().getMaxLevel() - 1)
		{
			return false;
		}
		
		boolean levelIncreased = super.addLevel(value);
		
		if (Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS)
		{
			final L2ClassMasterInstance master_instance = L2ClassMasterInstance.getInstance();
			if (master_instance != null)
			{
				ClassLevel lvlnow = PlayerClass.values()[getActiveChar().getClassId().getId()].getLevel();
				if (getLevel() >= 20 && lvlnow == ClassLevel.First)
				{
					L2ClassMasterInstance.getInstance().onTable(getActiveChar());
				}
				else if (getLevel() >= 40 && lvlnow == ClassLevel.Second)
				{
					L2ClassMasterInstance.getInstance().onTable(getActiveChar());
				}
				else if (getLevel() >= 76 && lvlnow == ClassLevel.Third)
				{
					L2ClassMasterInstance.getInstance().onTable(getActiveChar());
				}
			}
		}
		
		if (levelIncreased)
		{
			if (getActiveChar().getLevel() >= Config.MAX_LEVEL_NEWBIE_STATUS && getActiveChar().isNewbie())
			{
				getActiveChar().setNewbie(false);
				
				if (Config.DEBUG)
				{
					LOG.info("Newbie character ended: " + getActiveChar().getCharId());
				}
			}
			
			QuestState qs = getActiveChar().getQuestState("255_Tutorial");
			if (qs != null && qs.getQuest() != null)
			{
				qs.getQuest().notifyEvent("CE40", null, getActiveChar());
			}
			
			getActiveChar().setCurrentCp(getMaxCp());
			getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), 15));
			getActiveChar().sendPacket(new SystemMessage(SystemMessageId.YOU_INCREASED_YOUR_LEVEL));
		}
		
		if (getActiveChar().isInFunEvent())
		{
			if (getActiveChar()._inEventTvT && TvT.get_maxlvl() == getLevel() && !TvT.is_started())
			{
				TvT.removePlayer(getActiveChar());
			}
			getActiveChar().sendMessage("Your event sign up was canceled.");
		}
		
		getActiveChar().rewardSkills(); // Give Expertise skill of this level
		
		if (getActiveChar().getClan() != null)
		{
			getActiveChar().getClan().updateClanMember(getActiveChar());
			getActiveChar().getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(getActiveChar()));
		}
		
		if (getActiveChar().isInParty())
		{
			getActiveChar().getParty().recalculatePartyLevel(); // Recalculate the party level
		}
		
		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		getActiveChar().sendPacket(su);
		
		getActiveChar().refreshOverloaded();
		getActiveChar().refreshExpertisePenalty();
		getActiveChar().sendPacket(new UserInfo(getActiveChar()));
		
		return levelIncreased;
	}
	
	@Override
	public boolean addSp(int value)
	{
		if (!super.addSp(value))
		{
			return false;
		}
		
		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.SP, getSp());
		getActiveChar().sendPacket(su);
		
		return true;
	}
	
	@Override
	public final long getExpForLevel(int level)
	{
		return ExperienceData.getInstance().getExpForLevel(level);
	}
	
	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}
	
	@Override
	public final long getExp()
	{
		final L2PcInstance player = getActiveChar();
		if (player != null && player.isSubClassActive())
		{
			
			int class_index = player.getClassIndex();
			
			SubClass player_subclass = null;
			if ((player_subclass = player.getSubClasses().get(class_index)) != null)
			{
				return player_subclass.getExp();
			}
		}
		
		return super.getExp();
	}
	
	@Override
	public final void setExp(long value)
	{
		final L2PcInstance player = getActiveChar();
		
		if (player.isSubClassActive())
		{
			int class_index = player.getClassIndex();
			
			SubClass player_subclass = null;
			if ((player_subclass = player.getSubClasses().get(class_index)) != null)
			{
				player_subclass.setExp(value);
			}
		}
		else
		{
			super.setExp(value);
		}
	}
	
	@Override
	public final int getLevel()
	{
		try
		{
			final L2PcInstance player = getActiveChar();
			
			if (player.isSubClassActive())
			{
				int class_index = player.getClassIndex();
				
				SubClass player_subclass = null;
				if ((player_subclass = player.getSubClasses().get(class_index)) != null)
				{
					return player_subclass.getLevel();
				}
			}
			return super.getLevel();
		}
		catch (NullPointerException e)
		{
			return -1;
		}
	}
	
	@Override
	public final void setLevel(int value)
	{
		if (value > ExperienceData.getInstance().getMaxLevel() - 1)
		{
			value = ExperienceData.getInstance().getMaxLevel() - 1;
		}
		
		final L2PcInstance player = getActiveChar();
		
		if (player.isSubClassActive())
		{
			int class_index = player.getClassIndex();
			
			SubClass player_subclass = null;
			if ((player_subclass = player.getSubClasses().get(class_index)) != null)
			{
				player_subclass.setLevel(value);
			}
		}
		else
		{
			super.setLevel(value);
		}
	}
	
	@Override
	public final int getMaxCp()
	{
		int val = super.getMaxCp();
		
		if (val != _oldMaxCp)
		{
			_oldMaxCp = val;
			
			final L2PcInstance player = getActiveChar();
			
			if (player.getStatus().getCurrentCp() != val)
			{
				player.getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp());
			}
		}
		return val;
	}
	
	@Override
	public final int getMaxHp()
	{
		// Get the Max HP (base+modifier) of the L2PcInstance
		int val = super.getMaxHp();
		
		if (val != _oldMaxHp)
		{
			_oldMaxHp = val;
			
			final L2PcInstance player = getActiveChar();
			
			// Launch a regen task if the new Max HP is higher than the old one
			if (player.getStatus().getCurrentHp() != val)
			{
				player.getStatus().setCurrentHp(player.getStatus().getCurrentHp()); // trigger start of regeneration
			}
		}
		
		return val;
	}
	
	@Override
	public final int getMaxMp()
	{
		// Get the Max MP (base+modifier) of the L2PcInstance
		int val = super.getMaxMp();
		
		if (val != _oldMaxMp)
		{
			_oldMaxMp = val;
			
			final L2PcInstance player = getActiveChar();
			
			// Launch a regen task if the new Max MP is higher than the old one
			if (player.getStatus().getCurrentMp() != val)
			{
				player.getStatus().setCurrentMp(player.getStatus().getCurrentMp()); // trigger start of regeneration
			}
		}
		
		return val;
	}
	
	@Override
	public final int getSp()
	{
		final L2PcInstance player = getActiveChar();
		
		if (player.isSubClassActive())
		{
			int class_index = player.getClassIndex();
			
			SubClass player_subclass = null;
			if ((player_subclass = player.getSubClasses().get(class_index)) != null)
			{
				return player_subclass.getSp();
			}
		}
		
		return super.getSp();
	}
	
	@Override
	public final void setSp(int value)
	{
		final L2PcInstance player = getActiveChar();
		
		if (player.isSubClassActive())
		{
			int class_index = player.getClassIndex();
			
			SubClass player_subclass = null;
			if ((player_subclass = player.getSubClasses().get(class_index)) != null)
			{
				player_subclass.setSp(value);
			}
		}
		else
		{
			super.setSp(value);
		}
	}
}
