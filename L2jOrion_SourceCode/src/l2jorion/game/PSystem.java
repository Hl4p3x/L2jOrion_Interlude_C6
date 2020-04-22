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
	private static String [] coreIP =
	{
		"localhost",
		"81.99.76.224",
		"91.211.245.77", "185.80.128.233",
		"62.210.109.121",// Akroma
		"l2trebon.ddns.net"//StarMaster
	};
	
	public static boolean check()
	{
		List <String> ipCoreList = Arrays.asList(coreIP);
		
		if (!ipCoreList.contains(Config.EXTERNAL_HOSTNAME))
		{
			String [] systemIP = {getIP()};
			List <String> ipSystemList = Arrays.asList(systemIP);
			if (!ipSystemList.contains(Config.EXTERNAL_HOSTNAME))
			{
				showError();
				return true;
			}
		}
		return false;
	}
	
	private static void showError()
	{
		Util.printSection("Unauthorized server IP address - "+ Config.EXTERNAL_HOSTNAME);
		System.out.println("Your server IP must be assigned to your forum user.");
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
			statement = con.prepareStatement("select field_6 from core_pfields_content WHERE member_id=?");
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
				e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		return data;
	}
}