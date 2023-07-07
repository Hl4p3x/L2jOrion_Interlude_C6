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

public class TopBoard implements ICustomByPassHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(TopBoard.class);
	
	@Override
	public void handleCommand(String command, L2PcInstance player, String parameters)
	{
		if (parameters.startsWith("get"))
		{
			final int index = Integer.parseInt(parameters.substring(3).trim());
			
			switch (index)
			{
				case 1: // pvp
				{
					NpcHtmlMessage htmFile = new NpcHtmlMessage(1);
					TextBuilder htm = new TextBuilder("");
					htm.append("<html><head><center><title>TOP 10</title></head><body>");
					htm.append("<img src=\"l2font-e.replay_logo-e\" width=255 height=60>");
					htm.append("<br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
					htm.append("<table width=300 bgcolor=000000>");
					htm.append("<tr>");
					htm.append("<td><font color =\"0066CC\"><center>Rank</center></font></td><td><center><font color =\"0066CC\">Name</font></center></td><td><center><font color =\"0066CC\">PvP's</font></center></td></tr>");
					htm.append("</table>");
					
					htm.append("<table width=300>");
					htm.append("<tr>");
					int pos = 0;
					
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT char_name, pvpkills FROM weekly_top_board WHERE pvpkills > 0  order by pvpkills desc limit 10");
						ResultSet rs = stm.executeQuery();
						
						while (rs.next())
						{
							pos++;
							
							String char_name = rs.getString("char_name");
							String char_pkkills = rs.getString("pvpkills");
							
							htm.append("<tr><td><center>" + pos + "</center></td><td><center>" + char_name + "</center></td><td><center><font color =\"FF33AA\">" + char_pkkills + "</font></center></td></tr>");
						}
						
						htm.append("</table><center><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><br><button value=\"BACK\" action=\"bypass -h custom_topboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center></body></html>");
						
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("topBoard: ", e);
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
					NpcHtmlMessage htmFile = new NpcHtmlMessage(1);
					TextBuilder htm = new TextBuilder("");
					htm.append("<html><head><center><title>TOP 10</title></head><body>");
					htm.append("<img src=\"l2font-e.replay_logo-e\" width=255 height=60>");
					htm.append("<br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
					htm.append("<table width=300 bgcolor=000000>");
					htm.append("<tr>");
					htm.append("<td><center><font color =\"0066CC\">Rank</font></center></td><td><center><font color =\"0066CC\">Name</font></center></td><td><center><font color =\"0066CC\">Pk's</font></center></td></tr>");
					htm.append("</table>");
					
					htm.append("<table width=300>");
					htm.append("<tr>");
					int pos = 0;
					
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT char_name, pkkills FROM weekly_top_board WHERE pkkills > 0 order by pkkills desc limit 10");
						ResultSet rs = stm.executeQuery();
						
						while (rs.next())
						{
							pos++;
							
							String char_name = rs.getString("char_name");
							String char_pkkills = rs.getString("pkkills");
							
							htm.append("<tr><td><center>" + pos + "</center></td><td><center>" + char_name + "</center></td><td><center><font color =\"FF0000\">" + char_pkkills + "</font></center></td></tr>");
						}
						
						htm.append("</table><center><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><br><button value=\"BACK\" action=\"bypass -h custom_topboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center></body></html>");
						
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("topBoard: ", e);
					}
					finally
					{
						CloseUtil.close(con);
					}
					
					htmFile.setHtml(htm.toString());
					player.sendPacket(htmFile);
					
					break;
				}
				case 3: // raid points
				{
					NpcHtmlMessage htmFile = new NpcHtmlMessage(1);
					TextBuilder htm = new TextBuilder("");
					htm.append("<html><head><center><title>TOP 10</title></head><body>");
					htm.append("<img src=\"l2font-e.replay_logo-e\" width=255 height=60>");
					htm.append("<br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
					htm.append("<table width=300 bgcolor=000000>");
					htm.append("<tr>");
					htm.append("<td><center><font color =\"0066CC\">Rank</font></center></td><td><center><font color =\"0066CC\">Clan Name</font></center></td><td><center><font color =\"0066CC\">Raid Points</font></center></td></tr>");
					htm.append("</table>");
					
					htm.append("<table width=300>");
					htm.append("<tr>");
					int pos = 0;
					
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT clan_name, SUM(raid_points) AS total FROM weekly_top_board WHERE raid_points > 0 GROUP BY clan_name ORDER BY total DESC LIMIT 10");
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
							
							htm.append("<tr><td><center>" + pos + "</center></td><td><center>" + name + "</center></td><td><center><font color =\"3399FF\">" + points + "</font></center></td></tr>");
						}
						
						htm.append("</table><center><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><br><button value=\"BACK\" action=\"bypass -h custom_topboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center></body></html>");
						
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("topBoard: ", e);
					}
					finally
					{
						CloseUtil.close(con);
					}
					
					htmFile.setHtml(htm.toString());
					player.sendPacket(htmFile);
					
					break;
				}
				case 4: // clan war points
				{
					NpcHtmlMessage htmFile = new NpcHtmlMessage(1);
					TextBuilder htm = new TextBuilder("");
					htm.append("<html><head><center><title>TOP 10</title></head><body>");
					htm.append("<img src=\"l2font-e.replay_logo-e\" width=255 height=60>");
					htm.append("<br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
					htm.append("<table width=300 bgcolor=000000>");
					htm.append("<tr>");
					htm.append("<td><center><font color =\"0066CC\">Rank</font></center></td><td><center><font color =\"0066CC\">Clan Name</font></center></td><td><center><font color =\"0066CC\">CW Kills</font></center></td></tr>");
					htm.append("</table>");
					
					htm.append("<table width=300>");
					htm.append("<tr>");
					int pos = 0;
					
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT clan_name, SUM(war_points) AS total FROM weekly_top_board WHERE war_points > 0 GROUP BY clan_name ORDER BY total DESC LIMIT 10");
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
							
							htm.append("<tr><td><center>" + pos + "</center></td><td><center>" + name + "</center></td><td><center><font color =\"009900\">" + points + "</font></center></td></tr>");
						}
						
						htm.append("</table><center><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><br><button value=\"BACK\" action=\"bypass -h custom_topboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center></body></html>");
						
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("topBoard: ", e);
					}
					finally
					{
						CloseUtil.close(con);
					}
					
					htmFile.setHtml(htm.toString());
					player.sendPacket(htmFile);
					
					break;
				}
				case 5: // fishing points
				{
					NpcHtmlMessage htmFile = new NpcHtmlMessage(1);
					TextBuilder htm = new TextBuilder("");
					htm.append("<html><head><center><title>TOP 10</title></head><body>");
					htm.append("<img src=\"l2font-e.replay_logo-e\" width=255 height=60>");
					htm.append("<br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32>");
					htm.append("<table width=300 bgcolor=000000>");
					htm.append("<tr>");
					htm.append("<td><center><font color =\"0066CC\">Rank</font></center></td><td><center><font color =\"0066CC\">Name</font></center></td><td><center><font color =\"0066CC\">Points</font></center></td></tr>");
					htm.append("</table>");
					
					htm.append("<table width=300>");
					htm.append("<tr>");
					int pos = 0;
					
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection(false);
						PreparedStatement stm = con.prepareStatement("SELECT char_name, fishing_points FROM weekly_top_board WHERE fishing_points > 0 order by fishing_points desc limit 10");
						ResultSet rs = stm.executeQuery();
						
						while (rs.next())
						{
							pos++;
							
							String name = rs.getString("char_name");
							String points = rs.getString("fishing_points");
							
							htm.append("<tr><td><center>" + pos + "</center></td><td><center>" + name + "</center></td><td><center><font color =\"ff9933\">" + points + "</font></center></td></tr>");
						}
						
						htm.append("</table><center><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><br><button value=\"BACK\" action=\"bypass -h custom_topboard get 6\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center></body></html>");
						
						rs.close();
						stm.close();
					}
					catch (Exception e)
					{
						LOG.info("topBoard: ", e);
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
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					String text = HtmCache.getInstance().getHtm("data/html/default/13.htm");
					html.setHtml(text);
					player.sendPacket(html);
					break;
				}
			}
		}
	}
	
	private static String[] _topboard =
	{
		"topboard"
	};
	
	@Override
	public String[] getByPassCommands()
	{
		return _topboard;
	}
}