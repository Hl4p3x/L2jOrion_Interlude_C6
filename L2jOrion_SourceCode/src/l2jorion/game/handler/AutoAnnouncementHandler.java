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
package l2jorion.game.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class AutoAnnouncementHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(AutoAnnouncementHandler.class);
	private static AutoAnnouncementHandler _instance;
	private static final long DEFAULT_ANNOUNCEMENT_DELAY = 180000; // 3 mins by default
	protected Map<Integer, AutoAnnouncementInstance> _registeredAnnouncements;
	
	protected AutoAnnouncementHandler()
	{
		_registeredAnnouncements = new FastMap<>();
		restoreAnnouncementData();
	}
	
	private void restoreAnnouncementData()
	{
		int numLoaded = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("SELECT * FROM auto_announcements ORDER BY id");
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				numLoaded++;
				
				registerGlobalAnnouncement(rs.getInt("id"), rs.getString("announcement"), rs.getLong("delay"));
				
			}
			
			DatabaseUtils.close(statement);
			rs.close();
			statement = null;
			rs = null;
			
			LOG.info("Announcements: Loaded " + numLoaded + " Auto Announcements");
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			// ignore
		}
		finally
		{
			CloseUtil.close(con);
			
		}
	}
	
	/**
	 * @param activeChar
	 */
	public void listAutoAnnouncements(final L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/auto_announce.htm");
		TextBuilder replyMSG = new TextBuilder();
		
		for (final AutoAnnouncementInstance announcementInst : AutoAnnouncementHandler.getInstance().values())
		{
			replyMSG.append("<table width=300><tr><td width=280>[" + announcementInst.getDefaultDelay() / 1000 + "s] " + announcementInst.getDefaultTexts().toString() + "</td><td width=40>");
			replyMSG.append("<button value=\"[X]\" action=\"bypass -h admin_del_autoannouncement " + announcementInst.getDefaultId() + "\" width=20 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		}
		
		adminReply.replace("%autoann%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public static AutoAnnouncementHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new AutoAnnouncementHandler();
		}
		
		return _instance;
	}
	
	public int size()
	{
		return _registeredAnnouncements.size();
	}
	
	/**
	 * Registers a globally active autoannouncement.<BR>
	 * Returns the associated auto announcement instance.
	 * @param id
	 * @param announcementTexts
	 * @param announcementDelay announcementDelay (-1 = default delay)
	 * @return AutoAnnouncementInstance announcementInst
	 */
	public AutoAnnouncementInstance registerGlobalAnnouncement(final int id, final String announcementTexts, final long announcementDelay)
	{
		return registerAnnouncement(id, announcementTexts, announcementDelay);
	}
	
	/**
	 * Registers a NON globally-active auto announcement <BR>
	 * Returns the associated auto chat instance.
	 * @param id
	 * @param announcementTexts
	 * @param announcementDelay announcementDelay (-1 = default delay)
	 * @return AutoAnnouncementInstance announcementInst
	 */
	public AutoAnnouncementInstance registerAnnouncment(final int id, final String announcementTexts, final long announcementDelay)
	{
		return registerAnnouncement(id, announcementTexts, announcementDelay);
	}
	
	public AutoAnnouncementInstance registerAnnouncment(final String announcementTexts, final long announcementDelay)
	{
		final int nextId = nextAutoAnnouncmentId();
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO auto_announcements (id,announcement,delay) " + "VALUES (?,?,?)");
			statement.setInt(1, nextId);
			statement.setString(2, announcementTexts);
			statement.setLong(3, announcementDelay);
			
			statement.executeUpdate();
			
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("System: Could Not Insert Auto Announcment into DataBase: Reason: " + "Duplicate Id");
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
			
		}
		return registerAnnouncement(nextId, announcementTexts, announcementDelay);
	}
	
	public int nextAutoAnnouncmentId()
	{
		
		int nextId = 0;
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("SELECT id FROM auto_announcements ORDER BY id");
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				if (rs.getInt("id") > nextId)
				{
					nextId = rs.getInt("id");
				}
			}
			
			DatabaseUtils.close(statement);
			rs.close();
			statement = null;
			rs = null;
			
			nextId++;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
			
		}
		return nextId;
	}
	
	/**
	 * @param id
	 * @param announcementTexts
	 * @param chatDelay
	 * @return
	 */
	private final AutoAnnouncementInstance registerAnnouncement(final int id, final String announcementTexts, long chatDelay)
	{
		AutoAnnouncementInstance announcementInst = null;
		
		if (chatDelay < 0)
		{
			chatDelay = DEFAULT_ANNOUNCEMENT_DELAY;
		}
		
		if (_registeredAnnouncements.containsKey(id))
		{
			announcementInst = _registeredAnnouncements.get(id);
		}
		else
		{
			announcementInst = new AutoAnnouncementInstance(id, announcementTexts, chatDelay);
		}
		
		_registeredAnnouncements.put(id, announcementInst);
		
		return announcementInst;
	}
	
	/**
	 * @return
	 */
	public Collection<AutoAnnouncementInstance> values()
	{
		return _registeredAnnouncements.values();
	}
	
	/**
	 * Removes and cancels ALL auto announcement for the given announcement id.
	 * @param id
	 * @return boolean removedSuccessfully
	 */
	public boolean removeAnnouncement(final int id)
	{
		final AutoAnnouncementInstance announcementInst = _registeredAnnouncements.get(id);
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM auto_announcements WHERE id=?");
			statement.setInt(1, announcementInst.getDefaultId());
			statement.executeUpdate();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Could not Delete Auto Announcement in Database, Reason:", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
			
		}
		
		return removeAnnouncement(announcementInst);
	}
	
	/**
	 * Removes and cancels ALL auto announcement for the given announcement instance.
	 * @param announcementInst
	 * @return boolean removedSuccessfully
	 */
	public boolean removeAnnouncement(final AutoAnnouncementInstance announcementInst)
	{
		if (announcementInst == null)
		{
			return false;
		}
		
		_registeredAnnouncements.remove(announcementInst.getDefaultId());
		announcementInst.setActive(false);
		
		return true;
	}
	
	/**
	 * Returns the associated auto announcement instance either by the given announcement ID or object ID.
	 * @param id
	 * @return AutoAnnouncementInstance announcementInst
	 */
	public AutoAnnouncementInstance getAutoAnnouncementInstance(final int id)
	{
		return _registeredAnnouncements.get(id);
	}
	
	/**
	 * Sets the active state of all auto announcement instances to that specified, and cancels the scheduled chat task if necessary.
	 * @param isActive
	 */
	public void setAutoAnnouncementActive(final boolean isActive)
	{
		for (final AutoAnnouncementInstance announcementInst : _registeredAnnouncements.values())
		{
			announcementInst.setActive(isActive);
		}
	}
	
	/**
	 * Auto Announcement Instance
	 */
	public class AutoAnnouncementInstance
	{
		private long _defaultDelay = DEFAULT_ANNOUNCEMENT_DELAY;
		private String _defaultTexts;
		private boolean _defaultRandom = false;
		private final Integer _defaultId;
		
		private boolean _isActive;
		
		public ScheduledFuture<?> _chatTask;
		
		/**
		 * @param id
		 * @param announcementTexts
		 * @param announcementDelay
		 */
		protected AutoAnnouncementInstance(final int id, final String announcementTexts, final long announcementDelay)
		{
			_defaultId = id;
			_defaultTexts = announcementTexts;
			_defaultDelay = announcementDelay * 1000;
			
			setActive(true);
		}
		
		/**
		 * @return
		 */
		public boolean isActive()
		{
			return _isActive;
		}
		
		/**
		 * @return
		 */
		public boolean isDefaultRandom()
		{
			return _defaultRandom;
		}
		
		/**
		 * @return
		 */
		public long getDefaultDelay()
		{
			return _defaultDelay;
		}
		
		/**
		 * @return
		 */
		public String getDefaultTexts()
		{
			return _defaultTexts;
		}
		
		/**
		 * @return
		 */
		public Integer getDefaultId()
		{
			return _defaultId;
		}
		
		/**
		 * @param delayValue
		 */
		public void setDefaultChatDelay(final long delayValue)
		{
			_defaultDelay = delayValue;
		}
		
		/**
		 * @param textsValue
		 */
		public void setDefaultChatTexts(final String textsValue)
		{
			_defaultTexts = textsValue;
		}
		
		/**
		 * @param randValue
		 */
		public void setDefaultRandom(final boolean randValue)
		{
			_defaultRandom = randValue;
		}
		
		/**
		 * @param activeValue
		 */
		public void setActive(final boolean activeValue)
		{
			if (_isActive == activeValue)
			{
				return;
			}
			
			_isActive = activeValue;
			
			if (isActive())
			{
				AutoAnnouncementRunner acr = new AutoAnnouncementRunner(_defaultId);
				_chatTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(acr, _defaultDelay, _defaultDelay);
				acr = null;
			}
			else
			{
				_chatTask.cancel(false);
			}
		}
		
		/**
		 * Auto Announcement Runner <BR>
		 * <BR>
		 * Represents the auto announcement scheduled task for each announcement instance.
		 * @author chief
		 */
		private class AutoAnnouncementRunner implements Runnable
		{
			protected int id;
			
			protected AutoAnnouncementRunner(final int pId)
			{
				id = pId;
			}
			
			@Override
			public synchronized void run()
			{
				AutoAnnouncementInstance announcementInst = _registeredAnnouncements.get(id);
				
				String text;
				
				text = announcementInst.getDefaultTexts();
				
				if (text == null)
				{
					return;
				}
				
				Announcements.getInstance().AutoAnnounceToAll(text);
			}
		}
	}
}
