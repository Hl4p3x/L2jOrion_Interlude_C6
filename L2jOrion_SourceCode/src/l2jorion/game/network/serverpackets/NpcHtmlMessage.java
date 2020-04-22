/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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
package l2jorion.game.network.serverpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.clientpackets.RequestBypassToServer;

public class NpcHtmlMessage extends L2GameServerPacket
{
	/** The Constant _S__1B_NPCHTMLMESSAGE. */
	private static final String _S__1B_NPCHTMLMESSAGE = "[S] 0f NpcHtmlMessage";
	
	/** The LOG. */
	private static Logger LOG = LoggerFactory.getLogger(RequestBypassToServer.class);
	
	/** The _npc obj id. */
	private final int _npcObjId;
	
	/** The _html. */
	private String _html;
	
	/** The _validate. */
	private final boolean _validate = true;
	
	/**
	 * Instantiates a new npc html message.
	 * @param html the text
	 */
	public NpcHtmlMessage(final String html)
	{
		_npcObjId = 0;
		_html = html;
	}
	
	public NpcHtmlMessage(final int npcObjId, final String text)
	{
		_npcObjId = npcObjId;
		setHtml(text);
	}
	
	/**
	 * Instantiates a new npc html message.
	 * @param npcObjId the npc obj id
	 */
	public NpcHtmlMessage(final int npcObjId)
	{
		_npcObjId = npcObjId;
	}
	
	@Override
	public void runImpl()
	{
		if (Config.BYPASS_VALIDATION && _validate)
		{
			buildBypassCache(getClient().getActiveChar());
			buildLinksCache(getClient().getActiveChar());
		}
	}
	
	/**
	 * Sets the html.
	 * @param text the new html
	 */
	public void setHtml(final String text)
	{
		if (text == null)
		{
			//LOG.warn("Html is null! this will crash the client!");
			
			_html = "<html><body>No data! Report it for an admin. Thank you.</body></html>";
			return;
		}
		
		if (text.length() > 8192)
		{
			LOG.warn("Html is too long! this will crash the client!");
			_html = "<html><body>Html was too long</body></html>";
			return;
		}
		
		_html = text; // html code must not exceed 8192 bytes
	}
	
	/**
	 * Sets the file.
	 * @param path the path
	 * @return true, if successful
	 */
	public boolean setFile(final String path)
	{
		final String content = HtmCache.getInstance().getHtm(path);
		
		if (content == null)
		{
			setHtml("<html><body>My Text is missing:<br>" + path + "</body></html>");
			LOG.warn("missing html page " + path);
			return false;
		}
		
		setHtml(content);
		return true;
	}
	
	/**
	 * Replace.
	 * @param pattern the pattern
	 * @param value the value
	 */
	public void replace(final String pattern, final String value)
	{
		_html = _html.replaceAll(pattern, value);
	}
	
	public void replace(final String pattern, final boolean val)
	{
		replace(pattern, String.valueOf(val));
	}
	
	public void replace(final String pattern, final int val)
	{
		replace(pattern, String.valueOf(val));
	}
	
	public void replace(final String pattern, final long val)
	{
		replace(pattern, String.valueOf(val));
	}
	
	public void replace(final String pattern, final double val)
	{
		replace(pattern, String.valueOf(val));
	}
	
	/**
	 * Builds the bypass cache.
	 * @param activeChar the active char
	 */
	private final void buildBypassCache(final L2PcInstance activeChar)
	{
		if (activeChar == null)
		{
			return;
		}
		
		activeChar.clearBypass();
		for (int i = 0; i < _html.length(); i++)
		{
			int start = _html.indexOf("\"bypass ", i);
			int finish = _html.indexOf("\"", start + 1);
			if ((start < 0) || (finish < 0))
			{
				break;
			}
			
			if (_html.substring(start + 8, start + 10).equals("-h"))
			{
				start += 11;
			}
			else
			{
				start += 8;
			}
			
			i = finish;
			int finish2 = _html.indexOf("$", start);
			if ((finish2 < finish) && (finish2 > 0))
			{
				activeChar.addBypass2(_html.substring(start, finish2).trim());
			}
			else
			{
				activeChar.addBypass(_html.substring(start, finish).trim());
			}
		}
	}
	
	/**
	 * Builds the links cache.
	 * @param activeChar the active char
	 */
	private final void buildLinksCache(final L2PcInstance activeChar)
	{
		if (activeChar == null)
			return;
		
		activeChar.clearLinks();
		final int len = _html.length();
		for (int i = 0; i < len; i++)
		{
			final int start = _html.indexOf("link", i);
			final int finish = _html.indexOf("\"", start);
			
			if (start < 0 || finish < 0)
				break;
			
			i = start;
			activeChar.addLink(_html.substring(start + 5, finish).trim());
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x0f);
		
		writeD(_npcObjId);
		writeS(_html);
		writeD(0x00);
	}
	
	@Override
	public String getType()
	{
		return _S__1B_NPCHTMLMESSAGE;
	}
	
	/**
	 * Gets the content.
	 * @return the content
	 */
	public String getContent()
	{
		return _html;
	}
	
}
