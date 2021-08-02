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
package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.AccessLevel;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

/**
 * @author FBIagent<br>
 */
public class AccessLevels
{
	private static final Logger LOG = LoggerFactory.getLogger(AccessLevels.class);
	/** The one and only instance of this class, retriveable by getInstance()<br> */
	private static AccessLevels _instance = null;
	/** Reserved master access level<br> */
	// public static final int _masterAccessLevelNum = Config.MASTERACCESS_LEVEL;
	/** The master access level which can use everything<br> */
	
	// L2EMU_EDIT - Rayan -
	public AccessLevel _masterAccessLevel;/*
										 * = new AccessLevel(_masterAccessLevelNum, "Master Access", Config.MASTERACCESS_NAME_COLOR, Config.MASTERACCESS_TITLE_COLOR, true, true, true, true, true, true, true, true, true, true, true); //L2EMU_EDIT /** Reserved user access level<br>
										 */
	// public static final int _userAccessLevelNum = 0;
	/** The user access level which can do no administrative tasks<br> */
	
	// L2EMU_EDIT - Rayan -
	public AccessLevel _userAccessLevel;/*
										 * = new AccessLevel(_userAccessLevelNum, "User", Integer.decode("0xFFFFFF"), Integer.decode("0xFFFFFF"), false, false, false, true, false, true, true, true, true, true, false); //L2EMU_EDIT /** FastMap of access levels defined in database<br>
										 */
	private final FastMap<Integer, AccessLevel> _accessLevels = new FastMap<>();
	
	/**
	 * Loads the access levels from database<br>
	 */
	private AccessLevels()
	{
		_masterAccessLevel = new AccessLevel(Config.MASTERACCESS_LEVEL, "Master Access", Config.MASTERACCESS_NAME_COLOR, Config.MASTERACCESS_TITLE_COLOR, true, true, true, true, true, true, true, true, true, true, true);
		_userAccessLevel = new AccessLevel(Config.USERACCESS_LEVEL, "User", Integer.decode("0xFFFFFF"), Integer.decode("0xFFFFFF"), false, false, false, true, false, true, true, true, true, true, false);
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement stmt = con.prepareStatement("SELECT * FROM `access_levels` ORDER BY `accessLevel` DESC");
			final ResultSet rset = stmt.executeQuery();
			int accessLevel = 0;
			String name = null;
			int nameColor = 0;
			int titleColor = 0;
			boolean isGm = false;
			boolean allowPeaceAttack = false;
			boolean allowFixedRes = false;
			boolean allowTransaction = false;
			boolean allowAltG = false;
			boolean giveDamage = false;
			boolean takeAggro = false;
			boolean gainExp = false;
			
			// L2EMU_ADD
			boolean useNameColor = true;
			boolean useTitleColor = false;
			boolean canDisableGmStatus = true;
			// L2EMU_ADD
			
			while (rset.next())
			{
				accessLevel = rset.getInt("accessLevel");
				name = rset.getString("name");
				
				if (accessLevel == Config.USERACCESS_LEVEL)
				{
					//LOG.info("AccessLevels: Access level with name {} is using reserved user access level {}. Ignoring it.. " + name + " " + Config.USERACCESS_LEVEL);
					continue;
				}
				else if (accessLevel == Config.MASTERACCESS_LEVEL)
				{
					//LOG.info("AccessLevels: Access level with name {} is using reserved master access level {}. Ignoring it.. " + name + " " + Config.MASTERACCESS_LEVEL);
					continue;
				}
				else if (accessLevel < 0)
				{
					//LOG.info("AccessLevels: Access level with name {} is using banned access level state(below 0). Ignoring it.. " + name);
					continue;
				}
				
				try
				{
					nameColor = Integer.decode("0x" + rset.getString("nameColor"));
				}
				catch (final NumberFormatException nfe)
				{
					LOG.error("",nfe);
					
					try
					{
						nameColor = Integer.decode("0xFFFFFF");
					}
					catch (final NumberFormatException nfe2)
					{
						LOG.error("",nfe);
					}
				}
				
				try
				{
					titleColor = Integer.decode("0x" + rset.getString("titleColor"));
					
				}
				catch (final NumberFormatException nfe)
				{
					LOG.error("",nfe);
					
					try
					{
						titleColor = Integer.decode("0x77FFFF");
					}
					catch (final NumberFormatException nfe2)
					{
						LOG.error("",nfe);
					}
				}
				
				isGm = rset.getBoolean("isGm");
				allowPeaceAttack = rset.getBoolean("allowPeaceAttack");
				allowFixedRes = rset.getBoolean("allowFixedRes");
				allowTransaction = rset.getBoolean("allowTransaction");
				allowAltG = rset.getBoolean("allowAltg");
				giveDamage = rset.getBoolean("giveDamage");
				takeAggro = rset.getBoolean("takeAggro");
				gainExp = rset.getBoolean("gainExp");
				
				// L2EMU_ADD - Rayan for temp access
				useNameColor = rset.getBoolean("useNameColor");
				useTitleColor = rset.getBoolean("useTitleColor");
				canDisableGmStatus = rset.getBoolean("canDisableGmStatus");
				
				// L2EMU_EDIT - Rayan for temp access
				_accessLevels.put(accessLevel, new AccessLevel(accessLevel, name, nameColor, titleColor, isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp, useNameColor, useTitleColor, canDisableGmStatus));
				// L2EMU_EDIT
			}
			
			DatabaseUtils.close(rset);
			stmt.close();
		}
		catch (final SQLException e)
		{
			LOG.error("AccessLevels: Error loading from database ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		// LOG.info("AccessLevels: Loaded " + _accessLevels.size() + " Access Levels from database.");
		LOG.info("AccessLevels: Master Access Level is " + Config.MASTERACCESS_LEVEL);
		LOG.info("AccessLevels: User Access Level is " + Config.USERACCESS_LEVEL);
		if (Config.DEBUG)
			for (final int actual : _accessLevels.keySet())
			{
				final AccessLevel actual_access = _accessLevels.get(actual);
				LOG.debug("AccessLevels: {} Access Level is {} " + actual_access.getName() + " " + actual_access.getLevel());
			}
	}
	
	/**
	 * Returns the one and only instance of this class<br>
	 * <br>
	 * @return AccessLevels: the one and only instance of this class<br>
	 */
	public static AccessLevels getInstance()
	{
		return _instance == null ? (_instance = new AccessLevels()) : _instance;
	}
	
	/**
	 * Returns the access level by characterAccessLevel<br>
	 * <br>
	 * @param accessLevelNum as int<br>
	 * <br>
	 * @return AccessLevel: AccessLevel instance by char access level<br>
	 */
	public AccessLevel getAccessLevel(final int accessLevelNum)
	{
		AccessLevel accessLevel = null;
		
		synchronized (_accessLevels)
		{
			accessLevel = _accessLevels.get(accessLevelNum);
		}
		return accessLevel;
	}
	
	public void addBanAccessLevel(final int accessLevel)
	{
		synchronized (_accessLevels)
		{
			if (accessLevel > -1)
				return;
			
			// L2EMU_ADD - Rayan -
			_accessLevels.put(accessLevel, new AccessLevel(accessLevel, "Banned", Integer.decode("0x000000"), Integer.decode("0x000000"), false, false, false, false, false, false, false, false, false, false, false));
			// L2EMU_ADD
		}
	}
	
	public static void reload()
	{
		_instance = null;
		getInstance();
	}
	
}
