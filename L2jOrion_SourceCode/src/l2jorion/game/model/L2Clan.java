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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.cache.CrestCache;
import l2jorion.game.cache.CrestCache.CrestType;
import l2jorion.game.community.bb.Forum;
import l2jorion.game.community.manager.ForumsBBSManager;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.CrownManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.network.serverpackets.PledgeReceiveSubPledgeCreated;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.network.serverpackets.PledgeShowMemberListAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListDeleteAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.PledgeSkillListAdd;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class L2Clan
{
	private static final Logger LOG = LoggerFactory.getLogger(L2Clan.class);
	
	private String _name;
	private int _clanId;
	private L2ClanMember _leader;
	private final Map<String, L2ClanMember> _members = new FastMap<>();
	
	private String _allyName;
	private int _allyId = 0;
	private int _level;
	
	private int _hasCastle;
	private int _hasFort;
	private int _hasHideout;
	
	private boolean _hasCrest;
	private int _hiredGuards;
	private int _crestId;
	private int _crestLargeId;
	private int _allyCrestId;
	private int _auctionBiddedAt = 0;
	private long _allyPenaltyExpiryTime;
	private int _allyPenaltyType;
	private long _charPenaltyExpiryTime;
	private long _dissolvingExpiryTime;
	
	private static final String UPDATE_NOTICE = "UPDATE clan_data SET enabled=?,notice=? WHERE clan_id=?";
	private static final String UPDATE_INTRODUCTION = "UPDATE clan_data SET introduction=? WHERE clan_id=?";
	
	private static final int MAX_NOTICE_LENGTH = 8192;
	private static final int MAX_INTRODUCTION_LENGTH = 300;
	
	public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
	public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
	public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
	public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;
	
	private final ItemContainer _warehouse = new ClanWarehouse(this);
	private final List<Integer> _atWarWith = new FastList<>();
	private final List<Integer> _atWarAttackers = new FastList<>();
	
	private boolean _hasCrestLarge;
	
	private Forum _forum;
	
	private final List<L2Skill> _skillList = new FastList<>();
	
	private String _notice;
	private boolean _noticeEnabled = false;
	
	// Clan Privileges
	public static final int CP_NOTHING = 0;
	public static final int CP_CL_JOIN_CLAN = 2;
	public static final int CP_CL_GIVE_TITLE = 4;
	public static final int CP_CL_VIEW_WAREHOUSE = 8;
	public static final int CP_CL_MANAGE_RANKS = 16;
	public static final int CP_CL_PLEDGE_WAR = 32;
	public static final int CP_CL_DISMISS = 64;
	public static final int CP_CL_REGISTER_CREST = 128;
	public static final int CP_CL_MASTER_RIGHTS = 256;
	public static final int CP_CL_MANAGE_LEVELS = 512;
	public static final int CP_CH_OPEN_DOOR = 1024;
	public static final int CP_CH_OTHER_RIGHTS = 2048;
	public static final int CP_CH_AUCTION = 4096;
	public static final int CP_CH_DISMISS = 8192;
	public static final int CP_CH_SET_FUNCTIONS = 16384;
	public static final int CP_CS_OPEN_DOOR = 32768;
	public static final int CP_CS_MANOR_ADMIN = 65536;
	public static final int CP_CS_MANAGE_SIEGE = 131072;
	public static final int CP_CS_USE_FUNCTIONS = 262144;
	public static final int CP_CS_DISMISS = 524288;
	public static final int CP_CS_TAXES = 1048576;
	public static final int CP_CS_MERCENARIES = 2097152;
	public static final int CP_CS_SET_FUNCTIONS = 4194304;
	public static final int CP_ALL = 8388606;
	
	// Sub-unit types
	/** Clan subunit type of Academy */
	public static final int SUBUNIT_ACADEMY = -1;
	/** Clan subunit type of Royal Guard A */
	public static final int SUBUNIT_ROYAL1 = 100;
	/** Clan subunit type of Royal Guard B */
	public static final int SUBUNIT_ROYAL2 = 200;
	/** Clan subunit type of Order of Knights A-1 */
	public static final int SUBUNIT_KNIGHT1 = 1001;
	/** Clan subunit type of Order of Knights A-2 */
	public static final int SUBUNIT_KNIGHT2 = 1002;
	/** Clan subunit type of Order of Knights B-1 */
	public static final int SUBUNIT_KNIGHT3 = 2001;
	/** Clan subunit type of Order of Knights B-2 */
	public static final int SUBUNIT_KNIGHT4 = 2002;
	
	/** FastMap(Integer, L2Skill) containing all skills of the L2Clan */
	protected final Map<Integer, L2Skill> _skills = new FastMap<>();
	protected final Map<Integer, RankPrivs> _privs = new FastMap<>();
	protected final Map<Integer, SubPledge> _subPledges = new FastMap<>();
	
	private String _introduction;
	
	private int _reputationScore = 0;
	private int _rank = 0;
	
	/**
	 * Called if a clan is referenced only by id. In this case all other data needs to be fetched from db
	 * @param clanId A valid clan Id to create and restore
	 */
	public L2Clan(final int clanId)
	{
		_clanId = clanId;
		initializePrivs();
		
		try
		{
			restore();
			getWarehouse().restore();
		}
		catch (final Exception e)
		{
			LOG.error("Error restoring clan \n\t" + this, e);
		}
	}
	
	/**
	 * Called only if a new clan is created
	 * @param clanId A valid clan Id to create
	 * @param clanName A valid clan name
	 */
	public L2Clan(final int clanId, final String clanName)
	{
		_clanId = clanId;
		_name = clanName;
		initializePrivs();
	}
	
	/**
	 * @return Returns the clanId.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * @param clanId The clanId to set.
	 */
	public void setClanId(final int clanId)
	{
		_clanId = clanId;
	}
	
	/**
	 * @return Returns the leaderId.
	 */
	public int getLeaderId()
	{
		return _leader != null ? _leader.getObjectId() : 0;
	}
	
	/**
	 * @return L2ClanMember of clan leader.
	 */
	public L2ClanMember getLeader()
	{
		return _leader;
	}
	
	/**
	 * @param member
	 * @return
	 */
	public boolean setLeader(final L2ClanMember member)
	{
		if (member == null)
		{
			return false;
		}
		
		final L2ClanMember old_leader = _leader;
		_leader = member;
		_members.put(member.getName(), member);
		
		// refresh oldleader and new leader info
		if (old_leader != null)
		{
			
			final L2PcInstance exLeader = old_leader.getPlayerInstance();
			exLeader.setClan(this);
			exLeader.setPledgeClass(exLeader.getClan().getClanMember(exLeader.getObjectId()).calculatePledgeClass(exLeader));
			exLeader.setClanPrivileges(L2Clan.CP_NOTHING);
			
			exLeader.broadcastUserInfo();
			
			CrownManager.getInstance().checkCrowns(exLeader);
			
		}
		
		updateClanInDB();
		
		if (member.getPlayerInstance() != null)
		{
			
			final L2PcInstance newLeader = member.getPlayerInstance();
			newLeader.setClan(this);
			newLeader.setPledgeClass(member.calculatePledgeClass(newLeader));
			newLeader.setClanPrivileges(L2Clan.CP_ALL);
			
			newLeader.broadcastUserInfo();
			
		}
		
		broadcastClanStatus();
		
		CrownManager.getInstance().checkCrowns(member.getPlayerInstance());
		
		return true;
	}
	
	// public void setNewLeader(L2ClanMember member)
	public void setNewLeader(final L2ClanMember member, final L2PcInstance activeChar)
	{
		if (activeChar.isRiding() || activeChar.isFlying())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!getLeader().isOnline())
		{
			return;
		}
		
		if (member == null)
		{
			return;
		}
		
		if (!member.isOnline())
		{
			return;
		}
		
		if (setLeader(member))
		{
			
			SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_S1);
			sm.addString(member.getName());
			broadcastToOnlineMembers(sm);
		}
	}
	
	/**
	 * @return Returns the leaderName.
	 */
	public String getLeaderName()
	{
		return _leader != null ? _leader.getName() : "";
	}
	
	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @param name The name to set.
	 */
	public void setName(final String name)
	{
		_name = name;
	}
	
	private void addClanMember(final L2ClanMember member)
	{
		_members.put(member.getName(), member);
	}
	
	public void addClanMember(final L2PcInstance player)
	{
		L2ClanMember member = new L2ClanMember(this, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle());
		
		// store in memory
		addClanMember(member);
		member.setPlayerInstance(player);
		player.setClan(this);
		player.setPledgeClass(member.calculatePledgeClass(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new UserInfo(player));
		player.rewardSkills();
	}
	
	public void updateClanMember(final L2PcInstance player)
	{
		L2ClanMember member = new L2ClanMember(player);
		addClanMember(member);
	}
	
	public L2ClanMember getClanMember(final String name)
	{
		return _members.get(name);
	}
	
	public L2ClanMember getClanMember(final int objectID)
	{
		for (final L2ClanMember temp : _members.values())
		{
			if (temp.getObjectId() == objectID)
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public void removeClanMember(final String name, final long clanJoinExpiryTime)
	{
		L2ClanMember exMember = _members.remove(name);
		
		if (exMember == null)
		{
			LOG.warn("Member " + name + " not found in clan while trying to remove");
			return;
		}
		
		final int leadssubpledge = getLeaderSubPledge(name);
		
		if (leadssubpledge != 0)
		{
			// Sub-unit leader withdraws, position becomes vacant and leader
			// should appoint new via NPC
			getSubPledge(leadssubpledge).setLeaderName("");
			updateSubPledgeInDB(leadssubpledge);
		}
		
		if (exMember.getApprentice() != 0)
		{
			L2ClanMember apprentice = getClanMember(exMember.getApprentice());
			
			if (apprentice != null)
			{
				if (apprentice.getPlayerInstance() != null)
				{
					apprentice.getPlayerInstance().setSponsor(0);
				}
				else
				{
					apprentice.initApprenticeAndSponsor(0, 0);
				}
				
				apprentice.saveApprenticeAndSponsor(0, 0);
			}
		}
		
		if (exMember.getSponsor() != 0)
		{
			L2ClanMember sponsor = getClanMember(exMember.getSponsor());
			
			if (sponsor != null)
			{
				if (sponsor.getPlayerInstance() != null)
				{
					sponsor.getPlayerInstance().setApprentice(0);
				}
				else
				{
					sponsor.initApprenticeAndSponsor(0, 0);
				}
				
				sponsor.saveApprenticeAndSponsor(0, 0);
			}
			
			sponsor = null;
		}
		
		exMember.saveApprenticeAndSponsor(0, 0);
		
		if (Config.REMOVE_CASTLE_CIRCLETS)
		{
			CastleManager.getInstance().removeCirclet(exMember, getHasCastle());
		}
		
		if (exMember.isOnline())
		{
			L2PcInstance player = exMember.getPlayerInstance();
			
			player.setTitle("");
			player.setApprentice(0);
			player.setSponsor(0);
			
			if (player.isClanLeader())
			{
				SiegeManager.getInstance().removeSiegeSkills(player);
				player.setClanCreateExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L); // 24*60*60*1000 = 86400000
			}
			
			// remove Clan skills from Player
			for (final L2Skill skill : player.getClan().getAllSkills())
			{
				player.removeSkill(skill, false);
			}
			
			player.setClan(null);
			player.setClanJoinExpiryTime(clanJoinExpiryTime);
			player.setPledgeClass(exMember.calculatePledgeClass(player));
			player.broadcastUserInfo();
			// disable clan tab
			player.sendPacket(new PledgeShowMemberListDeleteAll());
		}
		else
		{
			removeMemberInDatabase(exMember, clanJoinExpiryTime, getLeaderName().equalsIgnoreCase(name) ? System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L : 0);
		}
	}
	
	public L2ClanMember[] getMembers()
	{
		return _members.values().toArray(new L2ClanMember[_members.size()]);
	}
	
	public int getMembersCount()
	{
		return _members.size();
	}
	
	public int getSubPledgeMembersCount(final int subpl)
	{
		int result = 0;
		
		for (final L2ClanMember temp : _members.values())
		{
			if (temp.getPledgeType() == subpl)
			{
				result++;
			}
		}
		
		return result;
	}
	
	public int getMaxNrOfMembers(final int pledgetype)
	{
		int limit = 0;
		
		switch (pledgetype)
		{
			case 0:
				switch (getLevel())
				{
					case 4:
						limit = 40;
						break;
					case 3:
						limit = 30;
						break;
					case 2:
						limit = 20;
						break;
					case 1:
						limit = 15;
						break;
					case 0:
						limit = 10;
						break;
					default:
						limit = 40;
						break;
				}
				break;
			case -1:
			case 100:
			case 200:
				limit = 20;
				break;
			case 1001:
			case 1002:
			case 2001:
			case 2002:
				limit = 10;
				break;
			default:
				break;
		}
		
		return limit;
	}
	
	public L2PcInstance[] getOnlineMembers(final String exclude)
	{
		final List<L2PcInstance> result = new FastList<>();
		
		for (final L2ClanMember temp : _members.values())
		{
			try
			{
				if (temp.isOnline() && !temp.getName().equals(exclude))
				{
					result.add(temp.getPlayerInstance());
				}
			}
			catch (final NullPointerException e)
			{
				e.printStackTrace();
			}
		}
		
		return result.toArray(new L2PcInstance[result.size()]);
		
	}
	
	/**
	 * @return
	 */
	public int getAllyId()
	{
		return _allyId;
	}
	
	/**
	 * @return
	 */
	public String getAllyName()
	{
		return _allyName;
	}
	
	public void setAllyCrestId(final int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}
	
	/**
	 * @return
	 */
	public int getAllyCrestId()
	{
		return _allyCrestId;
	}
	
	/**
	 * @return
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return
	 */
	public int getHasCastle()
	{
		return _hasCastle;
	}
	
	/**
	 * @return hasFort
	 */
	public int getHasFort()
	{
		return _hasFort;
	}
	
	/**
	 * @return
	 */
	public int getHasHideout()
	{
		return _hasHideout;
	}
	
	/**
	 * @param crestId The id of pledge crest.
	 */
	public void setCrestId(int crestId)
	{
		_crestId = crestId;
	}
	
	/**
	 * @return Returns the clanCrestId.
	 */
	public int getCrestId()
	{
		return _crestId;
	}
	
	/**
	 * @param crestLargeId The id of pledge LargeCrest.
	 */
	public void setCrestLargeId(final int crestLargeId)
	{
		_crestLargeId = crestLargeId;
	}
	
	/**
	 * @return Returns the clan CrestLargeId
	 */
	public int getCrestLargeId()
	{
		return _crestLargeId;
	}
	
	/**
	 * @param allyId The allyId to set.
	 */
	public void setAllyId(final int allyId)
	{
		_allyId = allyId;
	}
	
	/**
	 * @param allyName The allyName to set.
	 */
	public void setAllyName(final String allyName)
	{
		_allyName = allyName;
	}
	
	/**
	 * @param hasCastle The hasCastle to set.
	 */
	public void setHasCastle(final int hasCastle)
	{
		_hasCastle = hasCastle;
	}
	
	/**
	 * @param hasFort
	 */
	public void setHasFort(final int hasFort)
	{
		_hasFort = hasFort;
	}
	
	/**
	 * @param hasHideout The hasHideout to set.
	 */
	public void setHasHideout(final int hasHideout)
	{
		_hasHideout = hasHideout;
	}
	
	/**
	 * @param level The level to set.
	 */
	public void setLevel(final int level)
	{
		_level = level;
		
		if (_forum == null)
		{
			if (_level >= 2)
			{
				final ForumsBBSManager fbbsm = ForumsBBSManager.getInstance();
				final Forum clanRootForum = fbbsm.getForumByName("ClanRoot");
				if (clanRootForum != null)
				{
					_forum = clanRootForum.getChildByName(_name);
					if (_forum == null)
					{
						_forum = fbbsm.createNewForum(_name, clanRootForum, Forum.CLAN, Forum.CLANMEMBERONLY, getClanId());
					}
				}
			}
		}
	}
	
	private void storeNotice(String notice, boolean enabled)
	{
		if (notice == null)
		{
			notice = "";
		}
		
		if (notice.length() > MAX_NOTICE_LENGTH)
		{
			notice = notice.substring(0, MAX_NOTICE_LENGTH - 1);
		}
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_NOTICE);
			
			ps.setBoolean(1, enabled);
			ps.setString(2, notice);
			ps.setInt(3, _clanId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOG.error("Error while storing notice.", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		_notice = notice;
		_noticeEnabled = enabled;
	}
	
	public void setNoticeEnabledAndStore(boolean enabled)
	{
		storeNotice(_notice, enabled);
	}
	
	public void setNoticeAndStore(String notice)
	{
		storeNotice(notice, _noticeEnabled);
	}
	
	public boolean isNoticeEnabled()
	{
		return _noticeEnabled;
	}
	
	public void setNoticeEnabled(boolean enabled)
	{
		_noticeEnabled = enabled;
	}
	
	public String getNotice()
	{
		return (_notice == null) ? "" : _notice;
	}
	
	public void setNotice(String notice)
	{
		_notice = notice;
	}
	
	public String getIntroduction()
	{
		return (_introduction == null) ? "" : _introduction;
	}
	
	public void setIntroduction(String intro, boolean saveOnDb)
	{
		if (saveOnDb)
		{
			if (intro == null)
			{
				intro = "";
			}
			
			if (intro.length() > MAX_INTRODUCTION_LENGTH)
			{
				intro = intro.substring(0, MAX_INTRODUCTION_LENGTH - 1);
			}
			
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_INTRODUCTION);
				
				ps.setString(1, intro);
				ps.setInt(2, _clanId);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOG.error("Error while storing introduction.", e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
		
		_introduction = intro;
	}
	
	public boolean hasCastle()
	{
		return _hasCastle > 0;
	}
	
	public boolean hasHideout()
	{
		return _hasHideout > 0;
	}
	
	public boolean isMember(final String name)
	{
		return name == null ? false : _members.containsKey(name);
	}
	
	public void updateClanInDB()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET leader_id=?,ally_id=?,ally_name=?,reputation_score=?,ally_penalty_expiry_time=?,ally_penalty_type=?,char_penalty_expiry_time=?,dissolving_expiry_time=? WHERE clan_id=?");
			statement.setInt(1, getLeaderId());
			statement.setInt(2, getAllyId());
			statement.setString(3, getAllyName());
			statement.setInt(4, getReputationScore());
			statement.setLong(5, getAllyPenaltyExpiryTime());
			statement.setInt(6, getAllyPenaltyType());
			statement.setLong(7, getCharPenaltyExpiryTime());
			statement.setLong(8, getDissolvingExpiryTime());
			statement.setInt(9, getClanId());
			statement.execute();
			DatabaseUtils.close(statement);
			
			if (Config.DEBUG)
			{
				LOG.debug("New clan leader saved in db: " + getClanId());
			}
		}
		catch (final Exception e)
		{
			LOG.warn("error while saving new clan leader to db " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void store()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_data (clan_id,clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id) values (?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getClanId());
			statement.setString(2, getName());
			statement.setInt(3, getLevel());
			statement.setInt(4, getHasCastle());
			statement.setInt(5, getAllyId());
			statement.setString(6, getAllyName());
			statement.setInt(7, getLeaderId());
			statement.setInt(8, getCrestId());
			statement.setInt(9, getCrestLargeId());
			statement.setInt(10, getAllyCrestId());
			statement.execute();
			DatabaseUtils.close(statement);
			
			if (Config.DEBUG)
			{
				LOG.debug("New clan saved in DB: " + getClanId());
			}
		}
		catch (final Exception e)
		{
			LOG.warn("error while saving new clan to DB: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void removeMemberInDatabase(final L2ClanMember member, final long clanJoinExpiryTime, final long clanCreateExpiryTime)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET clanid=0, title=?, clan_join_expiry_time=?, clan_create_expiry_time=?, clan_privs=0, wantspeace=0, subpledge=0, lvl_joined_academy=0, apprentice=0, sponsor=0 WHERE obj_Id=?");
			statement.setString(1, "");
			statement.setLong(2, clanJoinExpiryTime);
			statement.setLong(3, clanCreateExpiryTime);
			statement.setInt(4, member.getObjectId());
			statement.execute();
			DatabaseUtils.close(statement);
			
			if (Config.DEBUG)
			{
				LOG.debug("clan member removed in db: " + getClanId());
			}
			
			statement = con.prepareStatement("UPDATE characters SET apprentice=0 WHERE apprentice=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			DatabaseUtils.close(statement);
			
			statement = con.prepareStatement("UPDATE characters SET sponsor=0 WHERE sponsor=?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			LOG.warn("error while removing clan member in db " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void restore()
	{
		Connection con = null;
		try
		{
			L2ClanMember member;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_name,clan_level,hasCastle,ally_id,ally_name,leader_id,crest_id,crest_large_id,ally_crest_id,reputation_score,auction_bid_at,ally_penalty_expiry_time,ally_penalty_type,char_penalty_expiry_time,dissolving_expiry_time,enabled,notice,introduction FROM clan_data where clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet clanData = statement.executeQuery();
			
			if (clanData.next())
			{
				setName(clanData.getString("clan_name"));
				setLevel(clanData.getInt("clan_level"));
				setHasCastle(clanData.getInt("hasCastle"));
				setAllyId(clanData.getInt("ally_id"));
				setAllyName(clanData.getString("ally_name"));
				setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));
				
				if (getAllyPenaltyExpiryTime() < System.currentTimeMillis())
				{
					setAllyPenaltyExpiryTime(0, 0);
				}
				
				setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));
				
				if (getCharPenaltyExpiryTime() + Config.ALT_CLAN_JOIN_DAYS * 86400000L < System.currentTimeMillis()) // 24*60*60*1000 = 86400000
				{
					setCharPenaltyExpiryTime(0);
				}
				
				setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));
				
				setCrestId(clanData.getInt("crest_id"));
				
				if (getCrestId() != 0)
				{
					setHasCrest(true);
				}
				
				setCrestLargeId(clanData.getInt("crest_large_id"));
				
				if (getCrestLargeId() != 0)
				{
					setHasCrestLarge(true);
				}
				
				setAllyCrestId(clanData.getInt("ally_crest_id"));
				setReputationScore(clanData.getInt("reputation_score"), false);
				setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);
				
				setNoticeEnabled(clanData.getBoolean("enabled"));
				setNotice(clanData.getString("notice"));
				setIntroduction(clanData.getString("introduction"), false);
				
				final int leaderId = clanData.getInt("leader_id");
				
				PreparedStatement statement2 = con.prepareStatement("SELECT char_name,level,classid,obj_Id,title,power_grade,subpledge,apprentice,sponsor FROM characters WHERE clanid=?");
				statement2.setInt(1, getClanId());
				ResultSet clanMembers = statement2.executeQuery();
				
				while (clanMembers.next())
				{
					member = new L2ClanMember(this, clanMembers.getString("char_name"), clanMembers.getInt("level"), clanMembers.getInt("classid"), clanMembers.getInt("obj_id"), clanMembers.getInt("subpledge"), clanMembers.getInt("power_grade"), clanMembers.getString("title"));
					
					if (member.getObjectId() == leaderId)
					{
						setLeader(member);
					}
					else
					{
						addClanMember(member);
					}
					member.initApprenticeAndSponsor(clanMembers.getInt("apprentice"), clanMembers.getInt("sponsor"));
				}
				clanMembers.close();
				statement2.close();
			}
			
			clanData.close();
			DatabaseUtils.close(statement);
			
			if (Config.DEBUG && getName() != null)
			{
				LOG.debug("Restored clan data for \"" + getName() + "\" from database.");
			}
			
			restoreSubPledges();
			restoreRankPrivs();
			restoreSkills();
			checkCrests();
		}
		catch (final Exception e)
		{
			LOG.warn("error while restoring clan ");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public int getOnlineMembersCount()
	{
		int count = 0;
		for (L2ClanMember temp : _members.values())
		{
			if ((temp == null) || !temp.isOnline())
			{
				continue;
			}
			
			count++;
		}
		return count;
	}
	
	private void restoreSkills()
	{
		Connection con = null;
		
		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT skill_id,skill_level FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, getClanId());
			
			ResultSet rset = statement.executeQuery();
			
			// Go though the recordset of this SQL query
			while (rset.next())
			{
				final int id = rset.getInt("skill_id");
				final int level = rset.getInt("skill_level");
				
				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				
				// Add the L2Skill object to the L2Clan _skills
				_skills.put(skill.getId(), skill);
				
				skill = null;
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
			statement = null;
			rset = null;
		}
		catch (final Exception e)
		{
			LOG.warn("Could not restore clan skills: " + e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * used to retrieve all skills
	 * @return
	 */
	public final L2Skill[] getAllSkills()
	{
		if (_skills == null)
		{
			return new L2Skill[0];
		}
		
		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}
	
	/**
	 * used to add a skill to skill list of this L2Clan
	 * @param newSkill
	 * @return
	 */
	public L2Skill addSkill(final L2Skill newSkill)
	{
		L2Skill oldSkill = null;
		
		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
		}
		
		return oldSkill;
	}
	
	/**
	 * used to add a new skill to the list, send a packet to all online clan members, update their stats and store it in db
	 * @param newSkill
	 * @return
	 */
	public L2Skill addNewSkill(final L2Skill newSkill)
	{
		L2Skill oldSkill = null;
		Connection con = null;
		
		if (newSkill != null)
		{
			
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			
			PreparedStatement statement;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				
				if (oldSkill != null)
				{
					statement = con.prepareStatement("UPDATE clan_skills SET skill_level=? WHERE skill_id=? AND clan_id=?");
					statement.setInt(1, newSkill.getLevel());
					statement.setInt(2, oldSkill.getId());
					statement.setInt(3, getClanId());
					statement.execute();
					DatabaseUtils.close(statement);
					statement = null;
				}
				else
				{
					statement = con.prepareStatement("INSERT INTO clan_skills (clan_id,skill_id,skill_level,skill_name) VALUES (?,?,?,?)");
					statement.setInt(1, getClanId());
					statement.setInt(2, newSkill.getId());
					statement.setInt(3, newSkill.getLevel());
					statement.setString(4, newSkill.getName());
					statement.execute();
					DatabaseUtils.close(statement);
					statement = null;
				}
			}
			catch (final Exception e2)
			{
				LOG.warn("Error could not store char skills: ");
				e2.printStackTrace();
			}
			finally
			{
				CloseUtil.close(con);
			}
			
			for (final L2ClanMember temp : _members.values())
			{
				try
				{
					if (temp.isOnline())
					{
						if (newSkill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
						{
							temp.getPlayerInstance().addSkill(newSkill, false); // Skill is not saved to player DB
							temp.getPlayerInstance().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
						}
					}
				}
				catch (final NullPointerException e)
				{
					e.printStackTrace();
					// null
				}
			}
		}
		
		return oldSkill;
	}
	
	public void addSkillEffects()
	{
		for (final L2Skill skill : _skills.values())
		{
			for (final L2ClanMember temp : _members.values())
			{
				try
				{
					if (temp.isOnline())
					{
						if (skill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
						{
							temp.getPlayerInstance().addSkill(skill, false); // Skill is not saved to player DB
						}
					}
				}
				catch (final NullPointerException e)
				{
					// null
					e.printStackTrace();
				}
			}
		}
	}
	
	public void addSkillEffects(final L2PcInstance cm)
	{
		if (cm == null)
		{
			return;
		}
		
		for (final L2Skill skill : _skills.values())
		{
			//
			if (skill.getMinPledgeClass() <= cm.getPledgeClass())
			{
				cm.addSkill(skill, false); // Skill is not saved to player DB
			}
		}
	}
	
	public void broadcastToOnlineAllyMembers(final L2GameServerPacket packet)
	{
		if (getAllyId() == 0)
		{
			return;
		}
		
		for (final L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId())
			{
				clan.broadcastToOnlineMembers(packet);
			}
		}
	}
	
	public void broadcastToOnlineMembers(L2GameServerPacket... packets)
	{
		for (final L2ClanMember member : _members.values())
		{
			if (member != null && member.isOnline())
			{
				for (L2GameServerPacket packet : packets)
				{
					member.getPlayerInstance().sendPacket(packet);
				}
			}
		}
	}
	
	public void broadcastToOnlineMembers(final L2GameServerPacket packet)
	{
		for (final L2ClanMember member : _members.values())
		{
			try
			{
				if (member.isOnline())
				{
					member.getPlayerInstance().sendPacket(packet);
				}
			}
			catch (final NullPointerException e)
			{
				// null
				e.printStackTrace();
			}
		}
	}
	
	public void broadcastToOtherOnlineMembers(final L2GameServerPacket packet, final L2PcInstance player)
	{
		for (final L2ClanMember member : _members.values())
		{
			try
			{
				if (member.isOnline() && member.getPlayerInstance() != player)
				{
					member.getPlayerInstance().sendPacket(packet);
				}
			}
			catch (final NullPointerException e)
			{
				// null
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @return
	 */
	public boolean hasCrest()
	{
		return _hasCrest;
	}
	
	public boolean hasCrestLarge()
	{
		return _hasCrestLarge;
	}
	
	public void setHasCrest(final boolean flag)
	{
		_hasCrest = flag;
	}
	
	public void setHasCrestLarge(final boolean flag)
	{
		_hasCrestLarge = flag;
	}
	
	public ItemContainer getWarehouse()
	{
		return _warehouse;
	}
	
	public boolean isAtWarWith(final Integer id)
	{
		if (_atWarWith != null && _atWarWith.size() > 0)
		{
			if (_atWarWith.contains(id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isAtWarAttacker(final Integer id)
	{
		if (_atWarAttackers != null && _atWarAttackers.size() > 0)
		{
			if (_atWarAttackers.contains(id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void setEnemyClan(final L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.add(id);
		
		id = null;
	}
	
	public void setEnemyClan(final Integer clan)
	{
		_atWarWith.add(clan);
	}
	
	public void setAttackerClan(final L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.add(id);
		
		id = null;
	}
	
	public void setAttackerClan(final Integer clan)
	{
		_atWarAttackers.add(clan);
	}
	
	public void deleteEnemyClan(final L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.remove(id);
		
		id = null;
	}
	
	public void deleteAttackerClan(final L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.remove(id);
		
		id = null;
	}
	
	public int getHiredGuards()
	{
		return _hiredGuards;
	}
	
	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}
	
	public int isAtWar()
	{
		if (_atWarWith != null && _atWarWith.size() > 0)
		{
			return 1;
		}
		
		return 0;
	}
	
	public List<Integer> getWarList()
	{
		return _atWarWith;
	}
	
	public List<Integer> getAttackerList()
	{
		return _atWarAttackers;
	}
	
	public void broadcastClanStatus()
	{
		for (final L2PcInstance member : getOnlineMembers(""))
		{
			member.sendPacket(new PledgeShowMemberListDeleteAll());
			member.sendPacket(new PledgeShowMemberListAll(this, member));
		}
	}
	
	public void removeSkill(final int id)
	{
		L2Skill deleteSkill = null;
		for (final L2Skill sk : _skillList)
		{
			if (sk.getId() == id)
			{
				deleteSkill = sk;
				return;
			}
		}
		_skillList.remove(deleteSkill);
	}
	
	public void removeSkill(final L2Skill deleteSkill)
	{
		_skillList.remove(deleteSkill);
	}
	
	/**
	 * @return
	 */
	public List<L2Skill> getSkills()
	{
		return _skillList;
	}
	
	public class SubPledge
	{
		private final int _id;
		private String _subPledgeName;
		private String _leaderName;
		
		public SubPledge(final int id, final String name, final String leaderName)
		{
			_id = id;
			_subPledgeName = name;
			_leaderName = leaderName;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getName()
		{
			return _subPledgeName;
		}
		
		public String getLeaderName()
		{
			return _leaderName;
		}
		
		public void setLeaderName(final String leaderName)
		{
			_leaderName = leaderName;
		}
		
		/**
		 * @param pledgeName
		 */
		public void setName(final String pledgeName)
		{
			_subPledgeName = pledgeName;
		}
	}
	
	public class RankPrivs
	{
		private final int _rankId;
		private final int _party;
		private int _rankPrivs;
		
		public RankPrivs(final int rank, final int party, final int privs)
		{
			_rankId = rank;
			_party = party;
			_rankPrivs = privs;
		}
		
		public int getRank()
		{
			return _rankId;
		}
		
		public int getParty()
		{
			return _party;
		}
		
		public int getPrivs()
		{
			return _rankPrivs;
		}
		
		public void setPrivs(final int privs)
		{
			_rankPrivs = privs;
		}
	}
	
	private void restoreSubPledges()
	{
		Connection con = null;
		
		try
		{
			// Retrieve all subpledges of this clan from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT sub_pledge_id,name,leader_name FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int id = rset.getInt("sub_pledge_id");
				
				String name = rset.getString("name");
				String leaderName = rset.getString("leader_name");
				// Create a SubPledge object for each record
				SubPledge pledge = new SubPledge(id, name, leaderName);
				_subPledges.put(id, pledge);
				
				name = null;
				leaderName = null;
				pledge = null;
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
			statement = null;
			rset = null;
		}
		catch (final Exception e)
		{
			LOG.warn("Could not restore clan sub-units: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * used to retrieve subPledge by type
	 * @param pledgeType
	 * @return
	 */
	public final SubPledge getSubPledge(final int pledgeType)
	{
		if (_subPledges == null)
		{
			return null;
		}
		
		return _subPledges.get(pledgeType);
	}
	
	/**
	 * used to retrieve subPledge by type
	 * @param pledgeName
	 * @return
	 */
	public final SubPledge getSubPledge(final String pledgeName)
	{
		if (_subPledges == null)
		{
			return null;
		}
		
		for (final SubPledge sp : _subPledges.values())
		{
			if (sp.getName().equalsIgnoreCase(pledgeName))
			{
				return sp;
			}
		}
		return null;
	}
	
	/**
	 * used to retrieve all subPledges
	 * @return
	 */
	public final SubPledge[] getAllSubPledges()
	{
		if (_subPledges == null)
		{
			return new SubPledge[0];
		}
		
		return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
	}
	
	public SubPledge createSubPledge(final L2PcInstance player, int pledgeType, final String leaderName, final String subPledgeName)
	{
		SubPledge subPledge = null;
		pledgeType = getAvailablePledgeTypes(pledgeType);
		
		if (pledgeType == 0)
		{
			if (pledgeType == L2Clan.SUBUNIT_ACADEMY)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY));
			}
			else
			{
				player.sendMessage("You can't create any more sub-units of this type");
			}
			return null;
		}
		
		if (_leader.getName().equals(leaderName))
		{
			player.sendMessage("Leader is not correct");
			return null;
		}
		
		// Royal Guard 5000 points per each
		// Order of Knights 10000 points per each
		if (pledgeType != -1 && (getReputationScore() < 5000 && pledgeType < L2Clan.SUBUNIT_KNIGHT1 || getReputationScore() < 10000 && pledgeType > L2Clan.SUBUNIT_ROYAL2))
		{
			SystemMessage sp = new SystemMessage(SystemMessageId.CLAN_REPUTATION_SCORE_IS_TOO_LOW);
			player.sendPacket(sp);
			sp = null;
			
			return null;
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_subpledges (clan_id,sub_pledge_id,name,leader_name) values (?,?,?,?)");
			statement.setInt(1, getClanId());
			statement.setInt(2, pledgeType);
			statement.setString(3, subPledgeName);
			
			if (pledgeType != -1)
			{
				statement.setString(4, leaderName);
			}
			else
			{
				statement.setString(4, "");
			}
			
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
			
			subPledge = new SubPledge(pledgeType, subPledgeName, leaderName);
			_subPledges.put(pledgeType, subPledge);
			
			if (pledgeType != -1)
			{
				setReputationScore(getReputationScore() - 2500, true);
			}
			
			if (Config.DEBUG)
			{
				LOG.debug("New sub_clan saved in db: " + getClanId() + "; " + pledgeType);
			}
		}
		catch (final Exception e)
		{
			LOG.warn("error while saving new sub_clan to db " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()));
		broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge));
		
		return subPledge;
	}
	
	public int getAvailablePledgeTypes(int pledgeType)
	{
		if (_subPledges.get(pledgeType) != null)
		{
			// LOG.warn("found sub-unit with id: "+pledgeType);
			switch (pledgeType)
			{
				case SUBUNIT_ACADEMY:
					return 0;
				case SUBUNIT_ROYAL1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
					break;
				case SUBUNIT_ROYAL2:
					return 0;
				case SUBUNIT_KNIGHT1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
					break;
				case SUBUNIT_KNIGHT2:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
					break;
				case SUBUNIT_KNIGHT3:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
					break;
				case SUBUNIT_KNIGHT4:
					return 0;
			}
		}
		return pledgeType;
	}
	
	public void updateSubPledgeInDB(final int pledgeType)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE clan_subpledges SET leader_name=?, name=? WHERE clan_id=? AND sub_pledge_id=?");
			statement.setString(1, getSubPledge(pledgeType).getLeaderName());
			statement.setString(2, getSubPledge(pledgeType).getName());
			statement.setInt(3, getClanId());
			statement.setInt(4, pledgeType);
			
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
			
			if (Config.DEBUG)
			{
				LOG.debug("New subpledge leader saved in db: " + getClanId());
			}
		}
		catch (final Exception e)
		{
			LOG.warn("error while saving new clan leader to db " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void restoreRankPrivs()
	{
		Connection con = null;
		
		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT privs,rank,party FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, getClanId());
			// LOG.warn("clanPrivs restore for ClanId : "+getClanId());
			ResultSet rset = statement.executeQuery();
			
			// Go though the recordset of this SQL query
			while (rset.next())
			{
				final int rank = rset.getInt("rank");
				// int party = rset.getInt("party");
				
				final int privileges = rset.getInt("privs");
				// Create a SubPledge object for each record
				// RankPrivs privs = new RankPrivs(rank, party, privileges);
				// _Privs.put(rank, privs);
				
				_privs.get(rank).setPrivs(privileges);
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.warn("Could not restore clan privs by rank: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void initializePrivs()
	{
		RankPrivs privs;
		
		for (int i = 1; i < 10; i++)
		{
			privs = new RankPrivs(i, 0, CP_NOTHING);
			_privs.put(i, privs);
		}
	}
	
	public int getRankPrivs(final int rank)
	{
		if (_privs.get(rank) != null)
		{
			return _privs.get(rank).getPrivs();
		}
		return CP_NOTHING;
	}
	
	public void setRankPrivs(final int rank, final int privs)
	{
		if (_privs.get(rank) != null)
		{
			_privs.get(rank).setPrivs(privs);
			
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privs) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE privs = ?");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.setInt(5, privs);
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				LOG.warn("Could not store clan privs for rank: " + e);
			}
			finally
			{
				CloseUtil.close(con);
			}
			
			for (final L2ClanMember cm : getMembers())
			{
				if (cm.isOnline())
				{
					if (cm.getPowerGrade() == rank)
					{
						if (cm.getPlayerInstance() != null)
						{
							cm.getPlayerInstance().setClanPrivileges(privs);
							cm.getPlayerInstance().sendPacket(new UserInfo(cm.getPlayerInstance()));
						}
					}
				}
			}
			
			broadcastClanStatus();
		}
		else
		{
			_privs.put(rank, new RankPrivs(rank, 0, privs));
			
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_privs (clan_id,rank,party,privs) VALUES (?,?,?,?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.execute();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				LOG.warn("Could not create new rank and store clan privs for rank: " + e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	/**
	 * used to retrieve all RankPrivs
	 * @return
	 */
	public final RankPrivs[] getAllRankPrivs()
	{
		if (_privs == null)
		{
			return new RankPrivs[0];
		}
		
		return _privs.values().toArray(new RankPrivs[_privs.values().size()]);
	}
	
	public int getLeaderSubPledge(final String name)
	{
		int id = 0;
		
		for (final SubPledge sp : _subPledges.values())
		{
			if (sp.getLeaderName() == null)
			{
				continue;
			}
			
			if (sp.getLeaderName().equals(name))
			{
				id = sp.getId();
			}
		}
		return id;
	}
	
	public void setReputationScore(final int value, final boolean save)
	{
		if (_reputationScore >= 0 && value < 0)
		{
			broadcastToOnlineMembers(new SystemMessage(SystemMessageId.REPUTATION_POINTS_0_OR_LOWER_CLAN_SKILLS_DEACTIVATED));
			L2Skill[] skills = getAllSkills();
			
			for (final L2ClanMember member : _members.values())
			{
				if (member.isOnline() && member.getPlayerInstance() != null)
				{
					for (final L2Skill sk : skills)
					{
						member.getPlayerInstance().removeSkill(sk, false);
					}
				}
			}
		}
		else if (_reputationScore < 0 && value >= 0)
		{
			broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_SKILLS_WILL_BE_ACTIVATED_SINCE_REPUTATION_IS_0_OR_HIGHER));
			L2Skill[] skills = getAllSkills();
			
			for (final L2ClanMember member : _members.values())
			{
				if (member.isOnline() && member.getPlayerInstance() != null)
				{
					for (final L2Skill sk : skills)
					{
						if (sk.getMinPledgeClass() <= member.getPlayerInstance().getPledgeClass())
						{
							member.getPlayerInstance().addSkill(sk, false);
						}
					}
				}
			}
			
			skills = null;
		}
		
		_reputationScore = value;
		
		if (_reputationScore > 100000000)
		{
			_reputationScore = 100000000;
		}
		if (_reputationScore < -100000000)
		{
			_reputationScore = -100000000;
		}
		
		if (save)
		{
			updateClanInDB();
		}
	}
	
	public int getReputationScore()
	{
		return _reputationScore;
	}
	
	public synchronized void addReputationScore(int value)
	{
		setReputationScore(_reputationScore + value, true);
	}
	
	public void setRank(final int rank)
	{
		_rank = rank;
	}
	
	public int getRank()
	{
		return _rank;
	}
	
	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}
	
	public void setAuctionBiddedAt(final int id, final boolean storeInDb)
	{
		_auctionBiddedAt = id;
		
		if (storeInDb)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at=? WHERE clan_id=?");
				statement.setInt(1, id);
				statement.setInt(2, getClanId());
				statement.execute();
				DatabaseUtils.close(statement);
				statement = null;
			}
			catch (final Exception e)
			{
				LOG.warn("Could not store auction for clan: " + e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
	}
	
	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 * @param activeChar
	 * @param target
	 * @param pledgeType
	 * @return
	 */
	public boolean checkClanJoinCondition(final L2PcInstance activeChar, final L2PcInstance target, final int pledgeType)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_JOIN_CLAN) != L2Clan.CP_CL_JOIN_CLAN)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return false;
		}
		
		if (target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return false;
		}
		
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_INVITE_YOURSELF));
			return false;
		}
		
		if (getCharPenaltyExpiryTime() > System.currentTimeMillis())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		
		if (target.getClanId() != 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		
		if (target.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		
		if ((target.getLevel() > 40 || target.getClassId().level() >= 2) && pledgeType == -1)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOESNOT_MEET_REQUIREMENTS_TO_JOIN_ACADEMY);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			activeChar.sendPacket(new SystemMessage(SystemMessageId.ACADEMY_REQUIREMENTS));
			return false;
		}
		
		if (getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType))
		{
			if (pledgeType == 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_IS_FULL);
				sm.addString(getName());
				activeChar.sendPacket(sm);
				sm = null;
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SUBCLAN_IS_FULL));
			}
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if activeChar and target meet various conditions to join a clan
	 * @param activeChar
	 * @param target
	 * @return
	 */
	public boolean checkAllyJoinCondition(final L2PcInstance activeChar, final L2PcInstance target)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (activeChar.getAllyId() == 0 || !activeChar.isClanLeader() || activeChar.getClanId() != activeChar.getAllyId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
			return false;
		}
		
		L2Clan leaderClan = activeChar.getClan();
		
		if (leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_INVITE_CLAN_WITHIN_1_DAY));
				return false;
			}
		}
		
		if (target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return false;
		}
		
		if (activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_INVITE_YOURSELF));
			return false;
		}
		
		if (target.getClan() == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_MUST_BE_IN_CLAN));
			return false;
		}
		
		if (!target.isClanLeader())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
			sm.addString(target.getName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		
		L2Clan targetClan = target.getClan();
		
		if (target.getAllyId() != 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE);
			sm.addString(targetClan.getName());
			sm.addString(targetClan.getAllyName());
			activeChar.sendPacket(sm);
			sm = null;
			return false;
		}
		
		if (targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_LEAVED)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANT_ENTER_ALLIANCE_WITHIN_1_DAY);
				sm.addString(target.getClan().getName());
				sm.addString(target.getClan().getAllyName());
				activeChar.sendPacket(sm);
				sm = null;
				return false;
			}
			if (targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_DISMISSED)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_ENTER_ALLIANCE_WITHIN_1_DAY));
				return false;
			}
		}
		
		if (activeChar.isInsideZone(ZoneId.ZONE_SIEGE) && target.isInsideZone(ZoneId.ZONE_SIEGE))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.OPPOSING_CLAN_IS_PARTICIPATING_IN_SIEGE));
			return false;
		}
		
		if (leaderClan.isAtWarWith(targetClan.getClanId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MAY_NOT_ALLY_CLAN_BATTLE));
			return false;
		}
		
		int numOfClansInAlly = 0;
		
		for (final L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == activeChar.getAllyId())
			{
				++numOfClansInAlly;
			}
		}
		
		if (numOfClansInAlly >= Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT));
			return false;
		}
		
		targetClan = null;
		leaderClan = null;
		
		return true;
	}
	
	public long getAllyPenaltyExpiryTime()
	{
		return _allyPenaltyExpiryTime;
	}
	
	public int getAllyPenaltyType()
	{
		return _allyPenaltyType;
	}
	
	public void setAllyPenaltyExpiryTime(final long expiryTime, final int penaltyType)
	{
		_allyPenaltyExpiryTime = expiryTime;
		_allyPenaltyType = penaltyType;
	}
	
	public long getCharPenaltyExpiryTime()
	{
		return _charPenaltyExpiryTime;
	}
	
	public void setCharPenaltyExpiryTime(final long time)
	{
		_charPenaltyExpiryTime = time;
	}
	
	public long getDissolvingExpiryTime()
	{
		return _dissolvingExpiryTime;
	}
	
	public void setDissolvingExpiryTime(final long time)
	{
		_dissolvingExpiryTime = time;
	}
	
	public void createAlly(final L2PcInstance player, final String allyName)
	{
		if (null == player)
		{
			return;
		}
		
		if (Config.DEBUG)
		{
			LOG.debug(player.getObjectId() + "(" + player.getName() + ") requested ally creation from ");
		}
		
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE));
			return;
		}
		
		if (getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_JOINED_ALLIANCE));
			return;
		}
		
		if (getLevel() < 5)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TO_CREATE_AN_ALLY_YOU_CLAN_MUST_BE_LEVEL_5_OR_HIGHER));
			return;
		}
		
		if (getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if (getAllyPenaltyType() == L2Clan.PENALTY_TYPE_DISSOLVE_ALLY)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION));
				return;
			}
		}
		
		if (getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_CREATE_ALLY_WHILE_DISSOLVING));
			return;
		}
		
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.ALLY_NAME_TEMPLATE);
		}
		catch (final PatternSyntaxException e) // case of illegal pattern
		{
			LOG.info("ERROR: Ally name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		
		final Matcher match = pattern.matcher(allyName);
		
		if (!match.matches())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_ALLIANCE_NAME));
			return;
		}
		
		if (allyName.length() > 16 || allyName.length() < 2)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_ALLIANCE_NAME_LENGTH));
			return;
		}
		
		if (ClanTable.getInstance().isAllyExists(allyName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALLIANCE_ALREADY_EXISTS));
			return;
		}
		
		setAllyId(getClanId());
		setAllyName(allyName.trim());
		setAllyPenaltyExpiryTime(0, 0);
		updateClanInDB();
		
		player.sendPacket(new UserInfo(player));
		
		//
		player.sendMessage("Alliance " + allyName + " has been created.");
	}
	
	public void dissolveAlly(final L2PcInstance player)
	{
		if (getAllyId() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_CURRENT_ALLIANCES));
			return;
		}
		
		if (!player.isClanLeader() || getClanId() != getAllyId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
			return;
		}
		
		if (player.isInsideZone(ZoneId.ZONE_SIEGE))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_ALLY_WHILE_IN_SIEGE));
			return;
		}
		
		broadcastToOnlineAllyMembers(new SystemMessage(SystemMessageId.ALLIANCE_DISOLVED));
		
		final long currentTime = System.currentTimeMillis();
		
		for (final L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() == getAllyId() && clan.getClanId() != getClanId())
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.setAllyPenaltyExpiryTime(0, 0);
				clan.updateClanInDB();
			}
		}
		
		setAllyId(0);
		setAllyName(null);
		// 24*60*60*1000 = 86400000
		setAllyPenaltyExpiryTime(currentTime + Config.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000L, L2Clan.PENALTY_TYPE_DISSOLVE_ALLY);
		updateClanInDB();
		
		// The clan leader should take the XP penalty of a full death.
		player.deathPenalty(false);
	}
	
	public void levelUpClan(final L2PcInstance player)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		
		if (System.currentTimeMillis() < getDissolvingExpiryTime())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS));
			return;
		}
		
		boolean increaseClanLevel = false;
		
		switch (getLevel())
		{
			case 0:
			{
				// upgrade to 1
				if (player.getSp() >= 20000 && player.getAdena() >= 650000)
				{
					if (player.reduceAdena("ClanLvl", 650000, player.getTarget(), true))
					{
						player.setSp(player.getSp() - 20000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(20000);
						player.sendPacket(sp);
						sp = null;
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 1:
			{
				// upgrade to 2
				if (player.getSp() >= 100000 && player.getAdena() >= 2500000)
				{
					if (player.reduceAdena("ClanLvl", 2500000, player.getTarget(), true))
					{
						player.setSp(player.getSp() - 100000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(100000);
						player.sendPacket(sp);
						sp = null;
						
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 2:
			{
				// upgrade to 3
				if (player.getSp() >= 350000 && player.getInventory().getItemByItemId(1419) != null)
				{
					// itemid 1419 == proof of blood
					if (player.destroyItemByItemId("ClanLvl", 1419, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 350000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(350000);
						player.sendPacket(sp);
						sp = null;
						
						SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
						sm.addItemName(1419);
						sm.addNumber(1);
						player.sendPacket(sm);
						sm = null;
						
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 3:
			{
				// upgrade to 4
				if (player.getSp() >= 1000000 && player.getInventory().getItemByItemId(3874) != null)
				{
					// itemid 3874 == proof of alliance
					if (player.destroyItemByItemId("ClanLvl", 3874, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 1000000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(1000000);
						player.sendPacket(sp);
						sp = null;
						
						SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
						sm.addItemName(3874);
						sm.addNumber(1);
						player.sendPacket(sm);
						sm = null;
						
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 4:
			{
				// upgrade to 5
				if (player.getSp() >= 2500000 && player.getInventory().getItemByItemId(3870) != null)
				{
					// itemid 3870 == proof of aspiration
					if (player.destroyItemByItemId("ClanLvl", 3870, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 2500000);
						SystemMessage sp = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
						sp.addNumber(2500000);
						player.sendPacket(sp);
						sp = null;
						
						SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
						sm.addItemName(3870);
						sm.addNumber(1);
						player.sendPacket(sm);
						sm = null;
						
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 5:
				if (getReputationScore() >= 10000)// && getMembersCount() >= 30)
				{
					setReputationScore(getReputationScore() - 10000, true);
					SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(10000);
					player.sendPacket(cr);
					cr = null;
					
					increaseClanLevel = true;
				}
				break;
			
			case 6:
				if (getReputationScore() >= 20000)// && getMembersCount() >= 80)
				{
					setReputationScore(getReputationScore() - 20000, true);
					SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(20000);
					player.sendPacket(cr);
					cr = null;
					
					increaseClanLevel = true;
				}
				break;
			case 7:
				if (getReputationScore() >= 40000)// && getMembersCount() >= 120)
				{
					setReputationScore(getReputationScore() - 40000, true);
					SystemMessage cr = new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(40000);
					player.sendPacket(cr);
					cr = null;
					
					increaseClanLevel = true;
				}
				break;
			default:
				return;
		}
		
		if (!increaseClanLevel)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.FAILED_TO_INCREASE_CLAN_LEVEL);
			player.sendPacket(sm);
			sm = null;
			return;
		}
		
		// the player should know that he has less sp now :p
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.SP, player.getSp());
		player.sendPacket(su);
		
		ItemList il = new ItemList(player, false);
		player.sendPacket(il);
		
		changeLevel(getLevel() + 1);
	}
	
	public void changeLevel(final int level)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
			statement.setInt(1, level);
			statement.setInt(2, getClanId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			LOG.warn("could not increase clan level:" + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		setLevel(level);
		
		if (getLeader().isOnline())
		{
			L2PcInstance leader = getLeader().getPlayerInstance();
			
			if (3 < level)
			{
				SiegeManager.getInstance().addSiegeSkills(leader);
			}
			else if (4 > level)
			{
				SiegeManager.getInstance().removeSiegeSkills(leader);
			}
			
			if (4 < level)
			{
				leader.sendPacket(new SystemMessage(SystemMessageId.CLAN_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS));
			}
			
			leader = null;
		}
		
		broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_LEVEL_INCREASED));
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
	}
	
	@Override
	public String toString()
	{
		return "L2Clan [_name=" + _name + ", _clanId=" + _clanId + ", _leader=" + _leader + ", _members=" + _members + ", _allyName=" + _allyName + ", _allyId=" + _allyId + ", _level=" + _level + ", _hasCastle=" + _hasCastle + ", _hasFort=" + _hasFort + ", _hasHideout=" + _hasHideout
			+ ", _hasCrest=" + _hasCrest + ", _hiredGuards=" + _hiredGuards + ", _crestId=" + _crestId + ", _crestLargeId=" + _crestLargeId + ", _allyCrestId=" + _allyCrestId + ", _auctionBiddedAt=" + _auctionBiddedAt + ", _allyPenaltyExpiryTime=" + _allyPenaltyExpiryTime + ", _allyPenaltyType="
			+ _allyPenaltyType + ", _charPenaltyExpiryTime=" + _charPenaltyExpiryTime + ", _dissolvingExpiryTime=" + _dissolvingExpiryTime + ", _warehouse=" + _warehouse + ", _atWarWith=" + _atWarWith + ", _atWarAttackers=" + _atWarAttackers + ", _hasCrestLarge=" + _hasCrestLarge + ", _forum="
			+ _forum + ", _skillList=" + _skillList + ", _notice=" + _notice + ", _noticeEnabled=" + _noticeEnabled + ", _skills=" + _skills + ", _privs=" + _privs + ", _subPledges=" + _subPledges + ", _reputationScore=" + _reputationScore + ", _rank=" + _rank + "]";
	}
	
	/**
	 * Change the clan crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * @param crestId if 0, crest is removed, else new crest id is set and saved to database
	 */
	public void changeClanCrest(int crestId)
	{
		Connection con = null;
		
		if (crestId == 0)
		{
			CrestCache.getInstance().removeCrest(CrestType.PLEDGE, _crestId);
		}
		
		_crestId = crestId;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, _clanId);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.warn("Could not update crest for clan " + _name + " [" + _clanId + "] : " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		
		setCrestId(crestId);
		setHasCrest(true);
		
		for (L2PcInstance member : getOnlineMembers(""))
		{
			member.broadcastUserInfo();
		}
	}
	
	/**
	 * Change the ally crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * @param crestId if 0, crest is removed, else new crest id is set and saved to database
	 * @param onlyThisClan Do it for the ally aswell.
	 */
	public void changeAllyCrest(int crestId, boolean onlyThisClan)
	{
		Connection con = null;
		
		String sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE clan_id = ?";
		int allyId = _clanId;
		if (!onlyThisClan)
		{
			if (crestId == 0)
			{
				CrestCache.getInstance().removeCrest(CrestType.ALLY, _allyCrestId);
			}
			
			sqlStatement = "UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?";
			allyId = _allyId;
		}
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(sqlStatement);
			statement.setInt(1, crestId);
			statement.setInt(2, allyId);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.warn("Could not update ally crest for ally/clan id " + allyId + " : " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (onlyThisClan)
		{
			_allyCrestId = crestId;
			setAllyCrestId(crestId);
			for (L2PcInstance member : getOnlineMembers(""))
			{
				member.broadcastUserInfo();
			}
		}
		else
		{
			for (L2Clan clan : ClanTable.getInstance().getClans())
			{
				if (clan.getAllyId() == _allyId)
				{
					clan.setAllyCrestId(crestId);
					for (L2PcInstance member : clan.getOnlineMembers(""))
					{
						member.broadcastUserInfo();
					}
				}
			}
		}
	}
	
	/**
	 * Change the large crest. If crest id is 0, crest is removed. New crest id is saved to database.
	 * @param crestId if 0, crest is removed, else new crest id is set and saved to database
	 */
	public void changeLargeCrest(int crestId)
	{
		Connection con = null;
		
		if (crestId == 0)
		{
			CrestCache.getInstance().removeCrest(CrestType.PLEDGE_LARGE, _crestLargeId);
		}
		
		_crestLargeId = crestId;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, _clanId);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			LOG.warn("Could not update large crest for clan " + _name + " [" + _clanId + "] : " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		setCrestLargeId(crestId);
		setHasCrestLarge(true);
		
		for (L2PcInstance member : getOnlineMembers(""))
		{
			member.broadcastUserInfo();
		}
	}
	
	private void checkCrests()
	{
		if (_crestId != 0)
		{
			if (CrestCache.getInstance().getCrest(CrestType.PLEDGE, _crestId) == null)
			{
				LOG.warn("Removing non-existent crest for clan " + _name + " [" + _clanId + "], crestId:" + _crestId);
				changeClanCrest(0);
			}
		}
		
		if (_crestLargeId != 0)
		{
			if (CrestCache.getInstance().getCrest(CrestType.PLEDGE_LARGE, _crestLargeId) == null)
			{
				LOG.warn("Removing non-existent large crest for clan " + _name + " [" + _clanId + "], crestLargeId:" + _crestLargeId);
				changeLargeCrest(0);
			}
		}
		
		if (_allyCrestId != 0)
		{
			if (CrestCache.getInstance().getCrest(CrestType.ALLY, _allyCrestId) == null)
			{
				LOG.warn("Removing non-existent ally crest for clan " + _name + " [" + _clanId + "], allyCrestId:" + _allyCrestId);
				changeAllyCrest(0, true);
			}
		}
	}
}