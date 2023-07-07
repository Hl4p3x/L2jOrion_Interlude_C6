package l2jorion.game.powerpack.other;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.text.TextBuilder;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class WeeklyBoard implements ICustomByPassHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(WeeklyBoard.class);
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		if (parameters.startsWith("get"))
		{
			int pos = 0;
			NpcHtmlMessage htmFile = new NpcHtmlMessage(1);
			TextBuilder htm = new TextBuilder();
			
			Connection con = null;
			
			final int index = Integer.parseInt(parameters.substring(3).trim());
			
			switch (index)
			{
				case 1: // pvp
				{
					htm.append("<html><head><title>TOP 10</title></head><body>");
					htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					htm.append("<table width=335 height=25 bgcolor=000000><tr>");
					htm.append("<td width=45><center><font color =\"0066CC\">#</font></center></td><td width=90><center><font color =\"0066CC\">Name</font></center></td><td width=90><center><font color =\"0066CC\">PvP's</font></center></td>");
					htm.append("</tr></table>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT char_name, pvp_kills FROM weeklyBoard WHERE pvp_kills > 0  order by pvp_kills desc limit 10");
						ResultSet rs = stm.executeQuery();
						while (rs.next())
						{
							pos++;
							
							String char_name = rs.getString("char_name");
							String char_pvp_kills = rs.getString("pvp_kills");
							
							htm.append("<table width=335><tr>");
							htm.append("<td width=45><center>" + pos + ".</center></td><td width=90><center>" + char_name + "</center></td><td width=90><center><font color =\"FF33AA\">" + char_pvp_kills + "</font></center></td>");
							htm.append("</tr></table>");
							htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
						}
						
						htm.append("<center>");
						htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
						htm.append("<br><button value=\"Back\" action=\"bypass -h custom_weeklyboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
						htm.append("</center></body></html>");
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("WeeklyBoard: ", e);
					}
					finally
					{
						CloseUtil.close(con);
					}
					
					htmFile.setHtml(htm.toString());
					player.sendPacket(htmFile);
					break;
				}
				case 2: // pk
				{
					htm.append("<html><head><title>TOP 10</title></head><body>");
					htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					htm.append("<table width=335 height=25 bgcolor=000000><tr>");
					htm.append("<td width=45><center><font color =\"0066CC\">#</font></center></td><td width=90><center><font color =\"0066CC\">Name</font></center></td><td width=90><center><font color =\"0066CC\">PK's</font></center></td>");
					htm.append("</tr></table>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT char_name, pk_kills FROM weeklyBoard WHERE pk_kills > 0 order by pk_kills desc limit 10");
						ResultSet rs = stm.executeQuery();
						while (rs.next())
						{
							pos++;
							
							String char_name = rs.getString("char_name");
							String char_pk_kills = rs.getString("pk_kills");
							
							htm.append("<table width=335><tr>");
							htm.append("<td width=45><center>" + pos + ".</center></td><td width=90><center>" + char_name + "</center></td><td width=90><center><font color =\"FF0000\">" + char_pk_kills + "</font></center></td>");
							htm.append("</tr></table>");
							htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
						}
						htm.append("<center>");
						htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
						htm.append("<br><button value=\"Back\" action=\"bypass -h custom_weeklyboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
						htm.append("</center></body></html>");
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("WeeklyBoard: ", e);
					}
					finally
					{
						CloseUtil.close(con);
					}
					
					htmFile.setHtml(htm.toString());
					player.sendPacket(htmFile);
					break;
				}
				case 3: // mob kills
				{
					htm.append("<html><head><title>TOP 10</title></head><body>");
					htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					htm.append("<table width=335 height=25 bgcolor=000000><tr>");
					htm.append("<td width=45><center><font color =\"0066CC\">#</font></center></td><td width=90><center><font color =\"0066CC\">Name</font></center></td><td width=90><center><font color =\"0066CC\">Kills</font></center></td>");
					htm.append("</tr></table>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT char_name, farm_kills FROM weeklyBoard WHERE farm_kills > 0 order by farm_kills desc limit 10");
						ResultSet rs = stm.executeQuery();
						while (rs.next())
						{
							pos++;
							
							String char_name = rs.getString("char_name");
							String char_farm_kills = rs.getString("farm_kills");
							
							htm.append("<table width=335><tr>");
							htm.append("<td width=45><center>" + pos + ".</center></td><td width=90><center>" + char_name + "</center></td><td width=90><center><font color =\"LEVEL\">" + char_farm_kills + "</font></center></td>");
							htm.append("</tr></table>");
							htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
							
						}
						htm.append("<center>");
						htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
						htm.append("<br><button value=\"Back\" action=\"bypass -h custom_weeklyboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
						htm.append("</center></body></html>");
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("WeeklyBoard: ", e);
					}
					finally
					{
						CloseUtil.close(con);
					}
					
					htmFile.setHtml(htm.toString());
					player.sendPacket(htmFile);
					break;
				}
				case 4: // raid points
				{
					htm.append("<html><head><title>TOP 10</title></head><body>");
					htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					htm.append("<table width=335 height=25 bgcolor=000000><tr>");
					htm.append("<td width=45><center><font color =\"0066CC\">#</font></center></td><td width=90><center><font color =\"0066CC\">Clan Name</font></center></td><td width=90><center><font color =\"0066CC\">Raid Points</font></center></td>");
					htm.append("</tr></table>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT clan_name, SUM(raid_points) AS total FROM weeklyBoard WHERE raid_points > 0 GROUP BY clan_name ORDER BY total DESC LIMIT 10");
						ResultSet rs = stm.executeQuery();
						while (rs.next())
						{
							String name = rs.getString("clan_name");
							
							if (name == null || name.isEmpty())
							{
								continue;
							}
							
							pos++;
							
							String points = rs.getString("total");
							
							htm.append("<table width=335><tr>");
							htm.append("<td width=45><center>" + pos + ".</center></td><td width=90><center>" + name + "</center></td><td width=90><center><font color =\"3399FF\">" + points + "</font></center></td>");
							htm.append("</tr></table>");
							htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
						}
						htm.append("<center>");
						htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
						htm.append("<br><button value=\"Back\" action=\"bypass -h custom_weeklyboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
						htm.append("</center></body></html>");
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("WeeklyBoard: ", e);
					}
					finally
					{
						CloseUtil.close(con);
					}
					
					htmFile.setHtml(htm.toString());
					player.sendPacket(htmFile);
					break;
				}
				case 5: // online time
				{
					htm.append("<html><head><title>TOP 10</title></head><body>");
					htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					htm.append("<table width=335 height=25 bgcolor=000000><tr>");
					htm.append("<td width=45><center><font color =\"0066CC\">#</font></center></td><td width=90><center><font color =\"0066CC\">Name</font></center></td><td width=90><center><font color =\"0066CC\">Online Time</font></center></td>");
					htm.append("</tr></table>");
					htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT char_name, online_time FROM weeklyBoard WHERE online_time > 0 order by online_time desc limit 10");
						ResultSet rs = stm.executeQuery();
						while (rs.next())
						{
							pos++;
							
							String char_name = rs.getString("char_name");
							int online_time = rs.getInt("online_time");
							
							int hours = online_time / 3600;
							int minutes = (online_time % 3600) / 60;
							int seconds = online_time % 60;
							String char_online_time = String.format("%2dh %2dm %2ds", hours, minutes, seconds);
							
							htm.append("<table width=335><tr>");
							htm.append("<td width=45><center>" + pos + ".</center></td><td width=90><center>" + char_name + "</center></td><td width=90><center><font color =\"009900\">" + char_online_time + "</font></center></td>");
							htm.append("</tr></table>");
							htm.append("<img src=\"L2UI.Squaregray\" width=\"300\" height=\"1\">");
						}
						htm.append("<center>");
						htm.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
						htm.append("<br><button value=\"Back\" action=\"bypass -h custom_weeklyboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
						htm.append("</center></body></html>");
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("WeeklyBoard: ", e);
					}
					finally
					{
						CloseUtil.close(con);
					}
					
					htmFile.setHtml(htm.toString());
					player.sendPacket(htmFile);
					break;
				}
				case 6:
				{
					String text = HtmCache.getInstance().getHtm("data/html/default/14.htm");
					htmFile.setHtml(text.toString());
					player.sendPacket(htmFile);
					break;
				}
			}
		}
	}
	
	private static String[] _weeklyboard =
	{
		"weeklyboard"
	};
	
	@Override
	public String[] getByPassCommands()
	{
		return _weeklyboard;
	}
}