package l2jorion.game.managers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import l2jorion.game.handler.ICommunityBoardHandler;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class BypassManager
{
	static final Logger LOG = LoggerFactory.getLogger(BypassManager.class);
	
	private static final Pattern a = Pattern.compile("\"(bypass +-h +|bypass+)(.+?)\"");
	private static final Pattern b = Pattern.compile("\"(bypass +-h +)(.+?)\"");
	
	public static enum BypassType
	{
		ENCODED,
		ENCODED_BBS,
		SIMPLE,
		SIMPLE_BBS,
		SIMPLE_DIRECT
	}
	
	public static BypassType getBypassType(String bypass)
	{
		switch (bypass.charAt(0))
		{
			case '0':
				return BypassType.ENCODED;
			case '1':
				return BypassType.ENCODED_BBS;
			default:
				if (matches(bypass, "^(player_help|manor_menu_select|arenachange|answer|_diary).*", Pattern.DOTALL))
				{
					return BypassType.SIMPLE;
				}
				return BypassType.SIMPLE_DIRECT;
		}
	}
	
	public static boolean matches(String str, String regex, int flags)
	{
		return Pattern.compile(regex, flags).matcher(str).matches();
	}
	
	public static String encode(String html, List<String> bypassStorage, boolean bbs)
	{
		Matcher m = a.matcher(html);
		Matcher mb = b.matcher(html);
		
		StringBuffer sb = new StringBuffer();
		
		while (m.find())
		{
			String bypass = m.group(2);
			String code = bypass;
			String params = "";
			int i = bypass.indexOf(" $");
			boolean use_params = i >= 0;
			
			if (use_params)
			{
				code = bypass.substring(0, i);
				params = bypass.substring(i).replace("$", "\\$");
			}
			
			if (bbs)
			{
				m.appendReplacement(sb, "\"bypass 1" + Integer.toHexString(bypassStorage.size()) + params + "\"");
			}
			else
			{
				if (mb.find())
				{
					m.appendReplacement(sb, "\"bypass -h 0" + Integer.toHexString(bypassStorage.size()) + params + "\"");
				}
				else
				{
					
					m.appendReplacement(sb, "\"bypass 0" + Integer.toHexString(bypassStorage.size()) + params + "\"");
				}
			}
			
			bypassStorage.add(code);
		}
		
		m.appendTail(sb);
		
		return sb.toString();
	}
	
	public static DecodedBypass decode(String bypass, List<String> bypassStorage, boolean bbs, L2PcInstance player)
	{
		synchronized (bypassStorage)
		{
			String[] bypass_parsed = bypass.split(" ");
			int idx = Integer.parseInt(bypass_parsed[0].substring(1), 16);
			String bp;
			
			try
			{
				bp = bypassStorage.get(idx);
			}
			catch (RuntimeException e)
			{
				bp = null;
			}
			
			if (bp == null)
			{
				// LOG.warn("Can't decode bypass (bypass not exists): " + (bbs ? "[bbs] " : "") + bypass + " / Player: " + player.getName() + " / Npc: " + (player.getLastFolkNPC() == null ? "null" : player.getLastFolkNPC().getName()), "debug_bypass");
				String error = "Can't decode bypass (bypass not exists): " + (bbs ? "[bbs] " : "") + bypass + " / Player: " + player.getName() + " / Npc: " + (player.getLastFolkNPC() == null ? "null" : player.getLastFolkNPC().getName());
				Log.add(error, "Decode_errors");
				return null;
			}
			
			DecodedBypass result = null;
			result = new DecodedBypass(bp, bbs);
			for (int i = 1; i < bypass_parsed.length; i++)
			{
				result.bypass += " " + bypass_parsed[i];
			}
			result.trim();
			
			return result;
		}
	}
	
	public static class DecodedBypass
	{
		public String bypass;
		public boolean bbs;
		public ICommunityBoardHandler handler;
		
		public DecodedBypass(String _bypass, boolean _bbs)
		{
			bypass = _bypass;
			bbs = _bbs;
		}
		
		public DecodedBypass(String _bypass, ICommunityBoardHandler _handler)
		{
			bypass = _bypass;
			handler = _handler;
		}
		
		public DecodedBypass trim()
		{
			bypass = bypass.trim();
			return this;
		}
	}
}