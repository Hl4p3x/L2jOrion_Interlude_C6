package l2jorion.game.network.serverpackets;

import java.util.List;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class ShowBoard extends PacketServer
{
	private static final String _S__6E_SHOWBOARD = "[S] 6e ShowBoard";
	
	private final static String TOP = "bypass _bbshome";
	private final static String FAV = "bypass _bbsgetfav";
	private final static String REGION = "bypass _bbsloc";
	private final static String CLAN = "bypass _bbsclan";
	private final static String MEMO = "bypass _bbsmemo";
	private final static String MAIL = "bypass _maillist_0_1_0_";
	private final static String FRIENDS = "bypass _friendlist_0_";
	private final static String ADDFAV = "bypass bbs_add_fav";
	
	private final String _htmlCode;
	private String _id;
	private List<String> _arg;
	private boolean _showBoard = true;
	
	public ShowBoard(String htmlCode, String id, L2PcInstance player)
	{
		_id = id;
		
		if (htmlCode != null)
		{
			if (id.equalsIgnoreCase("101"))
			{
				player.cleanBypasses(true);
			}
			
			_htmlCode = player.encodeBypasses(htmlCode, true);
		}
		else
		{
			_htmlCode = null;
		}
	}
	
	public ShowBoard(List<String> arg)
	{
		_id = "1002";
		_htmlCode = null;
		_arg = arg;
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
		String str = _id + "\u0008";
		
		if (!_id.equals("1002"))
		{
			if (_htmlCode != null)
			{
				str += _htmlCode;
			}
		}
		else
		{
			for (String arg : _arg)
			{
				str += arg + " \u0008";
			}
		}
		writeS(str);
	}
	
	@Override
	public String getType()
	{
		return _S__6E_SHOWBOARD;
	}
}