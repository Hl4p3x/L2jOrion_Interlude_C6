package l2jorion.game.network.serverpackets;

public class ExOlympiadMatchEnd extends L2GameServerPacket
{
	private static final String _S__FE_2C_OLYMPIADMATCHEND = "[S] FE:2C ExOlympiadMatchEnd";
	
	public static final ExOlympiadMatchEnd STATIC_PACKET = new ExOlympiadMatchEnd();
	
	private ExOlympiadMatchEnd()
	{
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x2c);
	}
	
	@Override
	public String getType()
	{
		return _S__FE_2C_OLYMPIADMATCHEND;
	}
}