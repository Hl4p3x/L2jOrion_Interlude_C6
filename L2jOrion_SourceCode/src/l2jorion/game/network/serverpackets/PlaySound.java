package l2jorion.game.network.serverpackets;

import l2jorion.game.network.PacketServer;

public class PlaySound extends PacketServer
{
	private static final String _S__98_PlaySound = "[S] 98 PlaySound";
	
	private final int _unknown1;
	private final String _soundFile;
	private final int _unknown3;
	private final int _unknown4;
	private final int _unknown5;
	private final int _unknown6;
	private final int _unknown7;
	
	public PlaySound(final String soundFile)
	{
		_unknown1 = 0;
		_soundFile = soundFile;
		_unknown3 = 0;
		_unknown4 = 0;
		_unknown5 = 0;
		_unknown6 = 0;
		_unknown7 = 0;
	}
	
	public PlaySound(final int unknown1, final String soundFile, final int unknown3, final int unknown4, final int unknown5, final int unknown6, final int unknown7)
	{
		_unknown1 = unknown1;
		_soundFile = soundFile;
		_unknown3 = unknown3;
		_unknown4 = unknown4;
		_unknown5 = unknown5;
		_unknown6 = unknown6;
		_unknown7 = unknown7;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x98);
		writeD(_unknown1); // unknown 0 for quest and ship;
		writeS(_soundFile);
		writeD(_unknown3); // unknown 0 for quest; 1 for ship;
		writeD(_unknown4); // 0 for quest; objectId of ship
		writeD(_unknown5); // x
		writeD(_unknown6); // y
		writeD(_unknown7); // z
	}
	
	@Override
	public String getType()
	{
		return _S__98_PlaySound;
	}
}