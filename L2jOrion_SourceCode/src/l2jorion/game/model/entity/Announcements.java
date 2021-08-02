/*
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
package l2jorion.game.model.entity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.script.DateRange;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Announcements
{
	private static Logger LOG = LoggerFactory.getLogger(Announcements.class.getName());
	
	private static Announcements _instance;
	private List<String> _announcements = new FastList<>();
	private List<List<Object>> _eventAnnouncements = new FastList<>();
	
	public Announcements()
	{
		loadAnnouncements();
	}
	
	public static Announcements getInstance()
	{
		if (_instance == null)
		{
			_instance = new Announcements();
		}
		
		return _instance;
	}
	
	public void loadAnnouncements()
	{
		_announcements.clear();
		File file = new File(Config.DATAPACK_ROOT, "config/announcements.txt");
		
		if (file.exists())
		{
			readFromDisk(file);
		}
		else
		{
			LOG.error("config/announcements.txt doesn't exist");
		}
	}
	
	public void showAnnouncementsNewStyle(L2PcInstance activeChar)
	{
		for (int i = 0; i < _announcements.size(); i++)
		{
			CreatureSay cs = new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, activeChar.getName(), _announcements.get(i).replace("%name%", activeChar.getName()));
			activeChar.sendPacket(cs);
		}
		
		for (int i = 0; i < _eventAnnouncements.size(); i++)
		{
			List<Object> entry = _eventAnnouncements.get(i);
			
			DateRange validDateRange = (DateRange) entry.get(0);
			String[] msg = (String[]) entry.get(1);
			Date currentDate = new Date();
			
			if (!validDateRange.isValid() || validDateRange.isWithinRange(currentDate))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				
				for (String element : msg)
				{
					sm.addString(element);
				}
				activeChar.sendPacket(sm);
			}
		}
	}
	
	public void showAnnouncements(L2PcInstance activeChar)
	{
		for (int i = 0; i < _announcements.size(); i++)
		{
			CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, activeChar.getName(), _announcements.get(i).replace("%name%", activeChar.getName()));
			activeChar.sendPacket(cs);
			cs = null;
		}
		
		for (int i = 0; i < _eventAnnouncements.size(); i++)
		{
			List<Object> entry = _eventAnnouncements.get(i);
			
			DateRange validDateRange = (DateRange) entry.get(0);
			String[] msg = (String[]) entry.get(1);
			Date currentDate = new Date();
			
			if (!validDateRange.isValid() || validDateRange.isWithinRange(currentDate))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				
				for (String element : msg)
				{
					sm.addString(element);
				}
				activeChar.sendPacket(sm);
			}
		}
	}
	
	public void addEventAnnouncement(DateRange validDateRange, String[] msg)
	{
		List<Object> entry = new FastList<>();
		entry.add(validDateRange);
		entry.add(msg);
		_eventAnnouncements.add(entry);
	}
	
	public void listAnnouncements(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtmForce("data/html/admin/announce.htm");
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(content);
		TextBuilder replyMSG = new TextBuilder("<br>");
		
		for (int i = 0; i < _announcements.size(); i++)
		{
			replyMSG.append("<table width=275><tr><td width=255>" + _announcements.get(i) + "</td><td width=20>");
			replyMSG.append("<button value=\"[X]\" action=\"bypass -h admin_del_announcement " + i + "\" width=20 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		}
		
		adminReply.replace("%announces%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public void addAnnouncement(String text)
	{
		_announcements.add(text);
		saveToDisk();
	}
	
	public void delAnnouncement(int line)
	{
		_announcements.remove(line);
		saveToDisk();
	}
	
	private void readFromDisk(File file)
	{
		LineNumberReader lnr = null;
		FileReader reader = null;
		try
		{
			int i = 0;
			
			String line = null;
			reader = new FileReader(file);
			lnr = new LineNumberReader(reader);
			
			while ((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if (st.hasMoreTokens())
				{
					String announcement = st.nextToken();
					_announcements.add(announcement);
					i++;
				}
			}
			LOG.info("Announcements: Loaded " + i + " Announcements");
		}
		catch (IOException e1)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e1.printStackTrace();
			}
			
			LOG.error("Error reading announcements", e1);
		}
		finally
		{
			if (lnr != null)
			{
				try
				{
					lnr.close();
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
			
			if (reader != null)
			{
				try
				{
					reader.close();
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
		}
	}
	
	private void saveToDisk()
	{
		File file = new File("config/announcements.txt");
		FileWriter save = null;
		
		try
		{
			save = new FileWriter(file);
			for (int i = 0; i < _announcements.size(); i++)
			{
				save.write(_announcements.get(i));
				save.write("\r\n");
			}
			save.flush();
		}
		catch (IOException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("saving the announcements file has failed: " + e);
		}
		finally
		{
			if (save != null)
			{
				try
				{
					save.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void announceToAll(String text)
	{
		CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", text);
		
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	public void AutoAnnounceToAll(String text)
	{
		CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", text);
		
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	public void sys(String text)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", "SYS: " + text);
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null && player.isOnline() == 1 && player.isGM())
			{
				player.sendPacket(cs);
			}
		}
	}
	
	// Colored Announcements 8D
	public void gameAnnounceToAll(String text)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", "" + text);
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	public void pvpAnnounceToAll(String text)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", "Announcements: " + text);
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	public void gameAnnounceToAll2(String text)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", "" + text);
		
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	public void adminMsg(String text, L2PcInstance activeChar)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", activeChar.getName() + ": " + text);
		
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	public void announceWithServerName(String text)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", Config.ALT_Server_Menu_Name + ": " + text);
		
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player != null)
			{
				if (player.isOnline() != 0)
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	public void announceToAll(SystemMessage sm)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			player.sendPacket(sm);
		}
	}
	
	public void handleAnnounce(String command, int lengthToTrim)
	{
		try
		{
			String text = command.substring(lengthToTrim);
			Announcements.getInstance().gameAnnounceToAll(text);
		}
		
		catch (StringIndexOutOfBoundsException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void handleAnnounce2(String command, int lengthToTrim)
	{
		try
		{
			String text = command.substring(lengthToTrim);
			Announcements.getInstance().gameAnnounceToAll2(text);
		}
		
		catch (StringIndexOutOfBoundsException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
}
