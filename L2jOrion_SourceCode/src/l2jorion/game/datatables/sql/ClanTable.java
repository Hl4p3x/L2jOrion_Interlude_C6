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
package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.enums.AchType;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.FortManager;
import l2jorion.game.managers.FortSiegeManager;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2ClanMember;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.entity.siege.FortSiege;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.network.serverpackets.PledgeShowMemberListAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class ClanTable
{
	private static Logger LOG = LoggerFactory.getLogger(ClanTable.class);
	
	private static ClanTable _instance;
	
	private final Map<Integer, L2Clan> _clans;
	
	public static ClanTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new ClanTable();
		}
		
		return _instance;
	}
	
	public static void reload()
	{
		_instance = null;
		getInstance();
	}
	
	public L2Clan[] getClans()
	{
		return _clans.values().toArray(new L2Clan[_clans.size()]);
	}
	
	public int getClansCount()
	{
		if (_clans.size() < 1)
		{
			return 1;
		}
		
		return _clans.size();
	}
	
	public int getTopRate(final int clan_id)
	{
		L2Clan clan = getClan(clan_id);
		if (clan.getLevel() < 3)
		{
			return 0;
		}
		int i = 1;
		for (final L2Clan clans : getClans())
		{
			if (clan != clans)
			{
				if (clan.getLevel() < clans.getLevel())
				{
					i++;
				}
				else if (clan.getLevel() == clans.getLevel())
				{
					if (clan.getReputationScore() <= clans.getReputationScore())
					{
						i++;
					}
				}
			}
		}
		clan = null;
		return i;
	}
	
	private ClanTable()
	{
		_clans = new FastMap<>();
		L2Clan clan;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM clan_data");
			final ResultSet result = statement.executeQuery();
			
			// Count the clans
			int clanCount = 0;
			
			while (result.next())
			{
				_clans.put(Integer.parseInt(result.getString("clan_id")), new L2Clan(Integer.parseInt(result.getString("clan_id"))));
				clan = getClan(Integer.parseInt(result.getString("clan_id")));
				if (clan.getDissolvingExpiryTime() != 0)
				{
					if (clan.getDissolvingExpiryTime() < System.currentTimeMillis())
					{
						destroyClan(clan.getClanId());
					}
					else
					{
						scheduleRemoveClan(clan.getClanId());
					}
				}
				clanCount++;
			}
			result.close();
			DatabaseUtils.close(statement);
			
			LOG.info("ClanTable: Loaded " + clanCount + " clans");
		}
		catch (final Exception e)
		{
			LOG.error("Data error on ClanTable", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		restorewars();
	}
	
	/**
	 * @param clanId
	 * @return
	 */
	public L2Clan getClan(final int clanId)
	{
		
		return _clans.get(clanId);
	}
	
	public L2Clan getClanByName(final String clanName)
	{
		for (final L2Clan clan : getClans())
		{
			if (clan.getName().equalsIgnoreCase(clanName))
			{
				return clan;
			}
		}
		
		return null;
	}
	
	public L2Clan createClan(final L2PcInstance player, final String clanName)
	{
		if (null == player)
		{
			return null;
		}
		
		if (10 > player.getLevel())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN));
			return null;
		}
		
		if (0 != player.getClanId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_CREATE_CLAN));
			return null;
		}
		
		if (System.currentTimeMillis() < player.getClanCreateExpiryTime())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MUST_WAIT_XX_DAYS_BEFORE_CREATING_A_NEW_CLAN));
			return null;
		}
		
		if (!isValidClanName(player, clanName))
		{
			return null;
		}
		
		final L2Clan clan = new L2Clan(IdFactory.getInstance().getNextId(), clanName);
		final L2ClanMember leader = new L2ClanMember(clan, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle());
		
		clan.setLeader(leader);
		leader.setPlayerInstance(player);
		clan.store();
		
		player.setClan(clan);
		player.setPledgeClass(leader.calculatePledgeClass(player));
		player.setClanPrivileges(L2Clan.CP_ALL);
		
		_clans.put(Integer.valueOf(clan.getClanId()), clan);
		
		player.sendPacket(new PledgeShowInfoUpdate(clan));
		player.sendPacket(new PledgeShowMemberListAll(clan, player));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new SystemMessage(SystemMessageId.CLAN_CREATED));
		player.getAchievement().increase(AchType.LEADER);
		
		return clan;
	}
	
	public boolean isValidClanName(final L2PcInstance player, final String clanName)
	{
		if (!Util.isAlphaNumeric(clanName) || clanName.length() < 2)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return false;
		}
		
		if (clanName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return false;
		}
		
		if (getClanByName(clanName) != null)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
			sm.addString(clanName);
			player.sendPacket(sm);
			return false;
		}
		
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.CLAN_NAME_TEMPLATE);
		}
		catch (final PatternSyntaxException e) // case of illegal pattern
		{
			LOG.warn("ERROR: Clan name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		
		final Matcher match = pattern.matcher(clanName);
		
		if (!match.matches())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return false;
		}
		
		return true;
	}
	
	public synchronized void destroyClan(final int clanId)
	{
		final L2Clan clan = getClan(clanId);
		
		if (clan == null)
		{
			return;
		}
		
		L2PcInstance leader = null;
		if (clan.getLeader() != null && (leader = clan.getLeader().getPlayerInstance()) != null)
		{
			
			if (Config.CLAN_LEADER_COLOR_ENABLED && clan.getLevel() >= Config.CLAN_LEADER_COLOR_CLAN_LEVEL)
			{
				
				if (Config.CLAN_LEADER_COLORED == 1)
				{
					leader.getAppearance().setNameColor(0x000000);
				}
				else
				{
					leader.getAppearance().setTitleColor(0xFFFF77);
				}
				
			}
			
			// remove clan leader skills
			leader.addClanLeaderSkills(false);
		}
		
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_HAS_DISPERSED));
		
		final int castleId = clan.getHasCastle();
		
		if (castleId == 0)
		{
			for (final Siege siege : SiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clanId);
			}
		}
		
		final int fortId = clan.getHasFort();
		
		if (fortId == 0)
		{
			for (final FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clanId);
			}
		}
		
		final L2ClanMember leaderMember = clan.getLeader();
		
		if (leaderMember == null)
		{
			clan.getWarehouse().destroyAllItems("ClanRemove", null, null);
		}
		else
		{
			clan.getWarehouse().destroyAllItems("ClanRemove", clan.getLeader().getPlayerInstance(), null);
		}
		
		for (final L2ClanMember member : clan.getMembers())
		{
			clan.removeClanMember(member.getName(), 0);
		}
		
		final int leaderId = clan.getLeaderId();
		final int clanLvl = clan.getLevel();
		
		_clans.remove(clanId);
		IdFactory.getInstance().releaseId(clanId);
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			
			statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			
			statement = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			
			statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?");
			statement.setInt(1, clanId);
			statement.execute();
			
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?");
			statement.setInt(1, clanId);
			statement.setInt(2, clanId);
			statement.execute();
			
			if (leader == null && leaderId != 0 && Config.CLAN_LEADER_COLOR_ENABLED && clanLvl >= Config.CLAN_LEADER_COLOR_CLAN_LEVEL)
			{
				String query;
				if (Config.CLAN_LEADER_COLORED == 1)
				{
					query = "UPDATE characters SET name_color = '000000' WHERE obj_Id = ?";
				}
				else
				{
					query = "UPDATE characters SET title_color = 'FFFF77' WHERE obj_Id = ?";
				}
				statement = con.prepareStatement(query);
				statement.setInt(1, leaderId);
				statement.execute();
			}
			
			if (castleId != 0)
			{
				statement = con.prepareStatement("UPDATE castle SET taxPercent = 0 WHERE id = ?");
				statement.setInt(1, castleId);
				statement.execute();
			}
			
			if (fortId != 0)
			{
				final Fort fort = FortManager.getInstance().getFortById(fortId);
				if (fort != null)
				{
					final L2Clan owner = fort.getOwnerClan();
					if (clan == owner)
					{
						fort.removeOwner(clan);
					}
				}
			}
			
			LOG.debug("Clan removed in db: {}" + " " + clanId);
			
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Error while removing clan in db", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void scheduleRemoveClan(final int clanId)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				if (getClan(clanId) == null)
				{
					return;
				}
				
				if (getClan(clanId).getDissolvingExpiryTime() != 0)
				{
					destroyClan(clanId);
				}
			}
		}, getClan(clanId).getDissolvingExpiryTime() - System.currentTimeMillis());
	}
	
	public boolean isAllyExists(final String allyName)
	{
		for (final L2Clan clan : getClans())
		{
			if (clan.getAllyName() != null && clan.getAllyName().equalsIgnoreCase(allyName))
			{
				return true;
			}
		}
		return false;
	}
	
	public void storeclanswars(final int clanId1, final int clanId2)
	{
		final L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		final L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		
		clan1.setEnemyClan(clan2);
		clan2.setAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2, wantspeace1, wantspeace2) VALUES(?,?,?,?)");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.setInt(3, 0);
			statement.setInt(4, 0);
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Could not store clans wars data", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_WAR_DECLARED_AGAINST_S1_IF_KILLED_LOSE_LOW_EXP);
		msg.addString(clan2.getName());
		clan1.broadcastToOnlineMembers(msg);
		
		msg = new SystemMessage(SystemMessageId.CLAN_S1_DECLARED_WAR);
		msg.addString(clan1.getName());
		clan2.broadcastToOnlineMembers(msg);
	}
	
	public void deleteclanswars(final int clanId1, final int clanId2)
	{
		final L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		final L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);
		
		clan1.deleteEnemyClan(clan2);
		clan2.deleteAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.execute();
			
			// statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? AND clan2=?");
			// statement.setInt(1,clanId2);
			// statement.setInt(2,clanId1);
			// statement.execute();
			
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Could not restore clans wars data", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		SystemMessage msg = new SystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED);
		msg.addString(clan2.getName());
		clan1.broadcastToOnlineMembers(msg);
		
		msg = new SystemMessage(SystemMessageId.CLAN_S1_HAS_DECIDED_TO_STOP);
		msg.addString(clan1.getName());
		clan2.broadcastToOnlineMembers(msg);
	}
	
	public void checkSurrender(final L2Clan clan1, final L2Clan clan2)
	{
		int count = 0;
		
		for (final L2ClanMember player : clan1.getMembers())
		{
			if (player != null && player.getPlayerInstance().getWantsPeace() == 1)
			{
				count++;
			}
		}
		
		if (count == clan1.getMembers().length - 1)
		{
			clan1.deleteEnemyClan(clan2);
			clan2.deleteEnemyClan(clan1);
			deleteclanswars(clan1.getClanId(), clan2.getClanId());
		}
	}
	
	private void restorewars()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT clan1, clan2, wantspeace1, wantspeace2 FROM clan_wars");
			final ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				getClan(rset.getInt("clan1")).setEnemyClan(rset.getInt("clan2"));
				getClan(rset.getInt("clan2")).setAttackerClan(rset.getInt("clan1"));
			}
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Could not restore clan wars data:");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
}
