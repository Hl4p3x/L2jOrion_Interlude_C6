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
package l2jorion.game.network.serverpackets;

import java.util.List;

import l2jorion.util.StringUtil;

public class ShowBoard extends L2GameServerPacket
{
	private static final String _S__6E_SHOWBOARD = "[S] 6e ShowBoard";

	public static final ShowBoard STATIC_SHOWBOARD_102 = new ShowBoard(null, "102");
	public static final ShowBoard STATIC_SHOWBOARD_103 = new ShowBoard(null, "103");
	
	private final static String TOP = "bypass _bbshome";
	private final static String FAV = "bypass _bbsgetfav";
	private final static String REGION = "bypass _bbsloc";
	private final static String CLAN = "bypass _bbsclan";
	private final static String MEMO = "bypass _bbsmemo";
	private final static String MAIL = "bypass _maillist_0_1_0_";
	private final static String FRIENDS = "bypass _friendlist_0_";
	private final static String ADDFAV = "bypass bbs_add_fav";
	
	private final StringBuilder _htmlCode;
	private boolean _showBoard = true;
	
	public ShowBoard(String htmlCode, String id, boolean showBoard)
	{
		_htmlCode = StringUtil.startAppend(500, id, "\u0008", htmlCode);
		_showBoard = showBoard;
	}
	
	public ShowBoard(String htmlCode, String id)
	{
		_htmlCode = StringUtil.startAppend(500, id, "\u0008", htmlCode);
	}
	
	public ShowBoard(List<String> arg)
	{
		_htmlCode = StringUtil.startAppend(500, "1002\u0008");
		for (String str : arg)
		{
			StringUtil.append(_htmlCode, str, " \u0008");
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x6e);
		writeC(_showBoard ? 0x01 : 0x00); // 1 to show, 0 to hide
		writeS(TOP);
		writeS(FAV);
		writeS(REGION);
		writeS(CLAN);
		writeS(MEMO);
		writeS(MAIL);
		writeS(FRIENDS);
		writeS(ADDFAV);
		writeS(_htmlCode.toString());
	}
	
	@Override
	public String getType()
	{
		return _S__6E_SHOWBOARD;
	}
}
