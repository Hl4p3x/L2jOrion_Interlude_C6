/*
 * Copyright (C) 2004-2016 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.community.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.CommunityBoard;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class FavoriteBBSManager extends BaseBBSManager
{
	protected static Logger LOG = Logger.getLogger(FavoriteBBSManager.class.getName());
	
	protected FavoriteBBSManager()
	{
	}
	
	// SQL Queries
	private static final String SELECT_FAVORITES = "SELECT * FROM `bbs_favorites` WHERE `playerId`=? ORDER BY `favAddDate` DESC";
	private static final String DELETE_FAVORITE = "DELETE FROM `bbs_favorites` WHERE `playerId`=? AND `favId`=?";
	private static final String ADD_FAVORITE = "REPLACE INTO `bbs_favorites`(`playerId`, `favTitle`, `favBypass`) VALUES(?, ?, ?)";
	
	@Override
	public void parseCmd(String command, L2PcInstance player)
	{
		if (command.startsWith("_bbsgetfav"))
		{
			String list = HtmCache.getInstance().getHtm("data/html/CommunityBoard/favorite_list.htm");
			StringBuilder sb = new StringBuilder();
			String favTitle = "";
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(SELECT_FAVORITES);
				ps.setInt(1, player.getObjectId());
				
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						String link = list.replaceAll("%fav_bypass%", String.valueOf(rs.getString("favBypass")));
						
						favTitle = rs.getString("favTitle");
						
						link = link.replaceAll("%fav_title%", favTitle);
						player.getAllFavorites().add(favTitle);
						
						final SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						link = link.replaceAll("%fav_add_date%", date.format(rs.getTimestamp("favAddDate")));
						link = link.replaceAll("%fav_id%", String.valueOf(rs.getInt("favId")));
						sb.append(link);
					}
				}
				
				String html = HtmCache.getInstance().getHtm("data/html/CommunityBoard/favorite.htm");
				html = html.replaceAll("%fav_list%", sb.toString());
				separateAndSend(html, player);
			}
			catch (Exception e)
			{
				LOG.warning(FavoriteBBSManager.class.getSimpleName() + ": Couldn't load favorite links for player " + player.getName());
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
		else if (command.startsWith("bbs_add_fav"))
		{
			final String bypass = CommunityBoard.getInstance().removeBypass(player);
			if (bypass != null)
			{
				final String[] parts = bypass.split("&", 2);
				if (parts.length != 2)
				{
					LOG.warning(FavoriteBBSManager.class.getSimpleName() + ": Couldn't add favorite link, " + bypass + " it's not a valid bypass!");
					return;
				}
				
				Connection con = null;
				
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement ps = con.prepareStatement(ADD_FAVORITE);
					
					ps.setInt(1, player.getObjectId());
					ps.setString(2, parts[0].trim());
					ps.setString(3, parts[1].trim());
					ps.execute();
					
					player.getAllFavorites().add(parts[0].trim());
					
					// Callback
					parseCmd("_bbsgetfav", player);
				}
				catch (Exception e)
				{
					LOG.warning(FavoriteBBSManager.class.getSimpleName() + ": Couldn't add favorite link " + bypass + " for player " + player.getName());
				}
				finally
				{
					CloseUtil.close(con);
				}
			}
		}
		else if (command.startsWith("_bbsdelfav_"))
		{
			final String[] parts = command.split(" ");
			if (parts.length > 3)
			{
				parts[2] = ""+parts[2]+" "+parts[3]+"";
			}
			
			Connection con = null;
			
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_FAVORITE);
				
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, Integer.parseInt(parts[1]));
				ps.execute();
				
				player.getAllFavorites().remove(parts[2]);
				
				parseCmd("_bbsgetfav", player);
			}
			catch (Exception e)
			{
				LOG.warning(FavoriteBBSManager.class.getSimpleName() + ": Couldn't delete favorite link ID " + parts[1] + " for player " + player.getName());
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
		else
		{
			super.parseCmd(command, player);
		}
	}
	
	public void loadFavorites(L2PcInstance player)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_FAVORITES);
			ps.setInt(1, player.getObjectId());
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					player.getAllFavorites().add( rs.getString("favTitle"));
				}
			}
		}
		catch (Exception e)
		{
			LOG.warning(FavoriteBBSManager.class.getSimpleName() + ": Couldn't load favorites for player " + player.getName());
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	
	public static FavoriteBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FavoriteBBSManager INSTANCE = new FavoriteBBSManager();
	}
}
