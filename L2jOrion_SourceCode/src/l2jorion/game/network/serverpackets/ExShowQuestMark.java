package l2jorion.game.network.serverpackets;

import l2jorion.game.network.PacketServer;

public class ExShowQuestMark extends PacketServer
{
	private final int _questId;
	
	public ExShowQuestMark(int questId)
	{
		_questId = questId;
	}
	
	@Override
	public String getType()
	{
		return "[S] FE:LA ExShowQuestMark";
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x1a);
		writeD(_questId);
	}
	
}
