package l2jorion.game.network;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.mmocore.SendablePacket;

public abstract class PacketServer extends SendablePacket<L2GameClient>
{
	public static final Logger LOG = LoggerFactory.getLogger(PacketServer.class);
	
	@Override
	protected void write()
	{
		try
		{
			writeImpl();
			
			// LOG.info("" + getType());
		}
		catch (Throwable t)
		{
			LOG.error("Client: " + getClient().toString() + " - Failed writing: " + getType());
			t.printStackTrace();
		}
	}
	
	public void runImpl()
	{
	}
	
	protected abstract void writeImpl();
	
	public abstract String getType();
}