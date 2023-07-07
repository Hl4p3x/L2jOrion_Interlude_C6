package l2jorion.game.network.serverpackets;

import l2jorion.game.network.PacketServer;

public class ServerToClientCommunicationPacket extends PacketServer
{
	private static final String _S__7D_SERVERTOCLIENT = "[S] 7D ServerToClientCommunicationPacket";
	
	private static boolean IS_OLD_CLIENT = false; // If you use Interlude or older version, set on "true"
	private ServerRequest serverRequest;
	private String drawText;
	private String url;
	private WarnWindowType warnWindowType;
	
	public ServerToClientCommunicationPacket(final String drawText)
	{
		this.serverRequest = ServerRequest.SC_SERVER_REQUEST_SET_DRAW_TEXT;
		this.drawText = drawText;
	}
	
	public ServerToClientCommunicationPacket(final String strValue, ServerRequest serverRequest)
	{
		this.serverRequest = serverRequest;
		if (serverRequest == ServerRequest.SC_SERVER_REQUEST_OPEN_URL)
		{
			this.url = strValue;
		}
		else if (serverRequest == ServerRequest.SC_SERVER_REQUEST_SET_DRAW_TEXT)
		{
			this.drawText = strValue;
		}
	}
	
	public ServerToClientCommunicationPacket(final WarnWindowType warnWindowType, final String warnMessage)
	{
		this.serverRequest = ServerRequest.SC_SERVER_REQUEST_SHOW_CUSTOM_WARN_MESSAGE;
		this.warnWindowType = warnWindowType;
		this.drawText = warnMessage;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(IS_OLD_CLIENT ? 0x70 : 0x7D); // Dummy packet opcode
		writeC(serverRequest.ordinal()); // Needed server request
		switch (serverRequest)
		{
			case SC_SERVER_REQUEST_SET_DRAW_TEXT:
				writeS(drawText);
				break;
			case SC_SERVER_REQUEST_SHOW_CUSTOM_WARN_MESSAGE:
				writeC(warnWindowType.ordinal());
				writeS(drawText);
				break;
			case SC_SERVER_REQUEST_OPEN_URL:
				writeS(url);
				break;
		}
	}
	
	public static enum ServerRequest
	{
		SC_SERVER_REQUEST_SET_DRAW_TEXT,
		SC_SERVER_REQUEST_SHOW_CUSTOM_WARN_MESSAGE,
		SC_SERVER_REQUEST_OPEN_URL,
	}
	
	public static enum WarnWindowType
	{
		UL2CW_DEFAULT,
		UL2CW_CLOSE_WINDOW,
	}
	
	@Override
	public String getType()
	{
		return _S__7D_SERVERTOCLIENT;
	}
}