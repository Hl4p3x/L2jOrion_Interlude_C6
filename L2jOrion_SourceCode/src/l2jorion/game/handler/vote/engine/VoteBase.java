package l2jorion.game.handler.vote.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public abstract class VoteBase
{
	public static Logger LOG = LoggerFactory.getLogger(VoteBase.class);
	
	public String getPlayerIp(L2PcInstance player)
	{
		// System.out.println(player.getClient().getConnection().getInetAddress().getHostAddress());
		// System.out.println("");
		return player.getClient().getConnection().getInetAddress().getHostAddress();
	}
	
	public abstract void reward(L2PcInstance player);
	
	public abstract void setVoted(L2PcInstance player);
	
	public void updateDB(L2PcInstance player, String column)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(String.format("UPDATE accounts_voted set %s=? where vote_ip=?", column));
			statement.setLong(1, System.currentTimeMillis());
			statement.setString(2, getPlayerIp(player));
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOG.error("Error in VoteBase.updateDB:");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean hasVoted(L2PcInstance player)
	{
		try
		{
			String endpoint = getApiEndpoint(player);
			if (endpoint.startsWith("error"))
			{
				return false;
			}
			
			String voted = getApiResponse(endpoint);
			
			return tryParseBool(voted);
		}
		catch (Exception e)
		{
			player.sendMessage("Something went wrong. Please try again later.");
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean tryParseBool(String response)
	{
		if (response.startsWith("1"))
		{
			return true;
		}
		// else if (bool.startsWith("{\"apiver\":\"0.1\",\"voted\":true"))
		// {
		// return true;
		// }
		// else if (bool.contains("{\"ok\":true,\"error_code\":0,\"description\":\"\",\"result\":{\"isVoted\":true"))
		// else if (bool.contains("\"isVoted\":true"))
		// {
		// return true;
		// }
		else if (response.toLowerCase().contains("\"is_voted\":true"))
		{
			return true;
		}
		else if (response.toLowerCase().contains("\"voted\":true"))
		{
			return true;
		}
		else if (response.contains("<status>1</status>"))
		
		{
			return true;
		}
		
		return Boolean.parseBoolean(response.trim());
	}
	
	public abstract String getApiEndpoint(L2PcInstance player);
	
	public String getApiResponse(String endpoint)
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		try
		{
			URL url = new URL(endpoint);
			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			connection.addRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(2000);
			connection.setReadTimeout(4000);
			connection.connect();
			
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
			{
				String line = null;
				while ((line = reader.readLine()) != null)
				{
					stringBuilder.append(line + "\n");
				}
			}
			catch (NullPointerException e)
			{
				LOG.error("Votebase: read error");
			}
			
			connection.disconnect();
			
			// System.out.println(stringBuilder.toString());
			
			return stringBuilder.toString();
		}
		catch (SocketTimeoutException e)
		{
			LOG.error(getClass().getSimpleName() + ": Socket timed out");
			return "error";
		}
		catch (Exception e)
		{
			LOG.error(getClass().getSimpleName() + ": Error:");
			e.printStackTrace();
			return "error";
		}
	}
}