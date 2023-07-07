package l2jorion.game;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import l2jorion.Config;
import l2jorion.util.CloseUtil;
import l2jorion.util.Util;
import l2jorion.util.database.GlobalDB;

public class PSystem
{
	private static String[] coreIP =
	{
		"87.247.67.217",
		"177.54.146.42",
		"185.80.130.51",
		"82.21.73.7",
		"91.211.245.77",
		"185.80.128.233",
		"91.121.121.217",
		"151.80.47.220",
		"84.15.182.238",
		"87.98.188.140",
		"91.225.104.223"
	};
	
	public static boolean check()
	{
		List<String> ipCoreList = Arrays.asList(coreIP);
		if (!ipCoreList.contains(Config.EXTERNAL_HOSTNAME))
		{
			if (GlobalDB.checkConnection())
			{
				return true;
			}
			
			System.out.println("Checking your data...");
			String[] systemIP =
			{
				getIP()
			};
			
			List<String> ipSystemList = Arrays.asList(systemIP);
			if (!ipSystemList.contains(Config.EXTERNAL_HOSTNAME))
			{
				showError();
				return true;
			}
			
			System.out.println("Your data has been verified successfully!");
		}
		return false;
	}
	
	private static void showError()
	{
		Util.printSection("Unauthorized server IP address - " + Config.EXTERNAL_HOSTNAME);
		System.out.println("Your server IP must be assigned to your forum user:");
		System.out.println("https://www.l2jorion.com/index.php?/topic/23-tutorial-unauthorized-server-ip-address-check-this-out/");
		System.out.println("");
		System.out.println("Website - www.L2jOrion.com");
		System.out.println("Skype - live:l2jorionproject");
		System.out.println("E-mail - support@l2jorion.com");
		Util.printSection("");
	}
	
	private static String getIP()
	{
		Connection con = null;
		String data = null;
		try
		{
			con = GlobalDB.getInstance().getConnection();
			
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT field_6 from core_pfields_content WHERE member_id = ?");
			statement.setInt(1, Config.FORUM_USER_ID);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				data = rset.getString("field_6");
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
		return data;
	}
}