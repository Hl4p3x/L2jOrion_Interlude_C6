/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

/**
 * @author programmos, scoria dev
 */

public class FortSiegeGuardManager
{
	private static final Logger LOG = LoggerFactory.getLogger(FortSiegeGuardManager.class);
	
	// =========================================================
	// Data Field
	private final Fort _fort;
	private final List<L2Spawn> _siegeGuardSpawn = new FastList<>();
	
	// =========================================================
	// Constructor
	public FortSiegeGuardManager(final Fort fort)
	{
		_fort = fort;
	}
	
	// =========================================================
	// Method - Public
	/**
	 * Add guard.<BR>
	 * <BR>
	 * @param activeChar
	 * @param npcId
	 */
	public void addSiegeGuard(final L2PcInstance activeChar, final int npcId)
	{
		if (activeChar == null)
			return;
		
		addSiegeGuard(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
	}
	
	/**
	 * Add guard.<BR>
	 * <BR>
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param npcId
	 */
	public void addSiegeGuard(final int x, final int y, final int z, final int heading, final int npcId)
	{
		saveSiegeGuard(x, y, z, heading, npcId, 0);
	}
	
	/**
	 * Hire merc.<BR>
	 * <BR>
	 * @param activeChar
	 * @param npcId
	 */
	public void hireMerc(final L2PcInstance activeChar, final int npcId)
	{
		if (activeChar == null)
			return;
		
		hireMerc(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
	}
	
	/**
	 * Hire merc.<BR>
	 * <BR>
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param npcId
	 */
	public void hireMerc(final int x, final int y, final int z, final int heading, final int npcId)
	{
		saveSiegeGuard(x, y, z, heading, npcId, 1);
	}
	
	/**
	 * Remove a single mercenary, identified by the npcId and location. Presumably, this is used when a fort lord picks up a previously dropped ticket
	 * @param npcId
	 * @param x
	 * @param y
	 * @param z
	 */
	public void removeMerc(final int npcId, final int x, final int y, final int z)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Delete From fort_siege_guards Where npcId = ? And x = ? AND y = ? AND z = ? AND isHired = 1");
			statement.setInt(1, npcId);
			statement.setInt(2, x);
			statement.setInt(3, y);
			statement.setInt(4, z);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e1)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e1.printStackTrace();
			
			LOG.warn("Error deleting hired siege guard at " + x + ',' + y + ',' + z + ":" + e1);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Remove mercs.<BR>
	 * <BR>
	 */
	public void removeMercs()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Delete From fort_siege_guards Where fortId = ? And isHired = 1");
			statement.setInt(1, getFort().getFortId());
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e1)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e1.printStackTrace();
			LOG.warn("Error deleting hired siege guard for fort " + getFort().getName() + ":" + e1);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Spawn guards.<BR>
	 * <BR>
	 */
	public void spawnSiegeGuard()
	{
		try
		{
			/*
			 * int hiredCount = 0; hiredMax = MercTicketManager.getInstance().getMaxAllowedMerc(_fort.getFortId()); boolean isHired = getFort().getOwnerId() > 0 ? true : false;
			 */
			loadSiegeGuard();
			for (final L2Spawn spawn : getSiegeGuardSpawn())
			{
				if (spawn != null)
				{
					spawn.init();
					/*
					 * if(isHired) { hiredCount++; //if (hiredCount > hiredMax) // return; }
					 */
				}
			}
		}
		catch (final Throwable t)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				t.printStackTrace();
			
			LOG.warn("Error spawning siege guards for fort " + getFort().getName() + ":" + t.toString());
		}
	}
	
	/**
	 * Unspawn guards.<BR>
	 * <BR>
	 */
	public void unspawnSiegeGuard()
	{
		for (final L2Spawn spawn : getSiegeGuardSpawn())
		{
			if (spawn == null)
			{
				continue;
			}
			
			spawn.stopRespawn();
			spawn.getLastSpawn().doDie(spawn.getLastSpawn());
		}
		
		getSiegeGuardSpawn().clear();
	}
	
	// =========================================================
	// Method - Private
	/**
	 * Load guards.<BR>
	 * <BR>
	 */
	private void loadSiegeGuard()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM fort_siege_guards Where fortId = ? ");
			statement.setInt(1, getFort().getFortId());
			ResultSet rs = statement.executeQuery();
			
			L2Spawn spawn1;
			L2NpcTemplate template1;
			
			while (rs.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rs.getInt("npcId"));
				if (template1 != null)
				{
					spawn1 = new L2Spawn(template1);
					spawn1.setId(rs.getInt("id"));
					spawn1.setAmount(1);
					spawn1.setLocx(rs.getInt("x"));
					spawn1.setLocy(rs.getInt("y"));
					spawn1.setLocz(rs.getInt("z"));
					spawn1.setHeading(rs.getInt("heading"));
					spawn1.setRespawnDelay(rs.getInt("respawnDelay"));
					spawn1.setLocation(0);
					_siegeGuardSpawn.add(spawn1);
					spawn1 = null;
				}
				else
				{
					LOG.warn("Missing npc data in npc table for id: " + rs.getInt("npcId"));
				}
				template1 = null;
			}
			rs.close();
			DatabaseUtils.close(statement);
			statement = null;
			rs = null;
		}
		catch (final Exception e1)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e1.printStackTrace();
			
			LOG.warn("Error loading siege guard for fort " + getFort().getName() + ":" + e1);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Save guards.<BR>
	 * <BR>
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param npcId
	 * @param isHire
	 */
	private void saveSiegeGuard(final int x, final int y, final int z, final int heading, final int npcId, final int isHire)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Insert Into fort_siege_guards (fortId, npcId, x, y, z, heading, respawnDelay, isHired) Values (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, getFort().getFortId());
			statement.setInt(2, npcId);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.setInt(6, heading);
			if (isHire == 1)
			{
				statement.setInt(7, 0);
			}
			else
			{
				statement.setInt(7, 600);
			}
			statement.setInt(8, isHire);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e1)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e1.printStackTrace();
			
			LOG.warn("Error adding siege guard for fort " + getFort().getName() + ":" + e1);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	// =========================================================
	// Proeprty
	
	public final Fort getFort()
	{
		return _fort;
	}
	
	public final List<L2Spawn> getSiegeGuardSpawn()
	{
		return _siegeGuardSpawn;
	}
}
