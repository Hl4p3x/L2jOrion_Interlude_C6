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

public class AccessLevels
{
	private static final Logger LOG = LoggerFactory.getLogger(AccessLevels.class);
	
	private static AccessLevels _instance = null;
	
	public AccessLevel _masterAccessLevel;
	public AccessLevel _userAccessLevel;
	private final FastMap<Integer, AccessLevel> _accessLevels = new FastMap<>();
	
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
			boolean useNameColor = true;
			boolean useTitleColor = false;
			boolean canDisableGmStatus = true;
			
			while (rset.next())
			{
				accessLevel = rset.getInt("accessLevel");
				name = rset.getString("name");
				
				if (accessLevel == Config.USERACCESS_LEVEL)
				{
					continue;
				}
				else if (accessLevel == Config.MASTERACCESS_LEVEL)
				{
					continue;
				}
				else if (accessLevel < 0)
				{
					continue;
				}
				
				try
				{
					nameColor = Integer.decode("0x" + rset.getString("nameColor"));
				}
				catch (final NumberFormatException nfe)
				{
					LOG.error("", nfe);
					
					try
					{
						nameColor = Integer.decode("0xFFFFFF");
					}
					catch (final NumberFormatException nfe2)
					{
						LOG.error("", nfe);
					}
				}
				
				try
				{
					titleColor = Integer.decode("0x" + rset.getString("titleColor"));
					
				}
				catch (final NumberFormatException nfe)
				{
					LOG.error("", nfe);
					
					try
					{
						titleColor = Integer.decode("0x77FFFF");
					}
					catch (final NumberFormatException nfe2)
					{
						LOG.error("", nfe);
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
				useNameColor = rset.getBoolean("useNameColor");
				useTitleColor = rset.getBoolean("useTitleColor");
				canDisableGmStatus = rset.getBoolean("canDisableGmStatus");
				
				_accessLevels.put(accessLevel, new AccessLevel(accessLevel, name, nameColor, titleColor, isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp, useNameColor, useTitleColor, canDisableGmStatus));
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
		
		LOG.info("AccessLevels: Master Access Level is " + Config.MASTERACCESS_LEVEL);
		LOG.info("AccessLevels: User Access Level is " + Config.USERACCESS_LEVEL);
	}
	
	public static AccessLevels getInstance()
	{
		return _instance == null ? (_instance = new AccessLevels()) : _instance;
	}
	
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
			{
				return;
			}
			
			_accessLevels.put(accessLevel, new AccessLevel(accessLevel, "Banned", Integer.decode("0x000000"), Integer.decode("0x000000"), false, false, false, false, false, false, false, false, false, false, false));
		}
	}
	
	public static void reload()
	{
		_instance = null;
		
		getInstance();
	}
	
}
