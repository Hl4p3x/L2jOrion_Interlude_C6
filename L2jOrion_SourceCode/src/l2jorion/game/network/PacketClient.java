package l2jorion.game.network;

import java.nio.BufferUnderflowException;
import java.util.logging.Level;
import java.util.logging.Logger;

import l2jorion.game.network.clientpackets.AttackRequest;
import l2jorion.game.network.clientpackets.EnterWorld;
import l2jorion.game.network.clientpackets.MoveBackwardToLocation;
import l2jorion.game.network.clientpackets.RequestMagicSkillUse;
import l2jorion.mmocore.ReceivablePacket;

public abstract class PacketClient extends ReceivablePacket<L2GameClient>
{
	public static Logger LOG = Logger.getLogger(PacketClient.class.getName());
	
	@Override
	protected boolean read()
	{
		try
		{
			readImpl();
			return true;
		}
		catch (Exception e)
		{
			LOG.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed reading: " + getType() + " : " + e.getMessage(), e);
			
			if (e instanceof BufferUnderflowException)
			{
				getClient().onBufferUnderflow();
			}
		}
		return false;
	}
	
	protected abstract void readImpl();
	
	@Override
	public void run()
	{
		try
		{
			runImpl();
			
			// if (!(this instanceof Say2))
			// {
			// Announcements _a = Announcements.getInstance();
			// _a.sys("" + getType());
			// }
			
			if (this instanceof MoveBackwardToLocation || this instanceof AttackRequest || this instanceof RequestMagicSkillUse)
			{
				if (getClient().getActiveChar() != null)
				{
					getClient().getActiveChar().onActionRequest(); // Removes onSpawn Protection
				}
			}
		}
		catch (final Throwable t)
		{
			LOG.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed running: " + getType() + " ; " + t.getMessage(), t);
			
			if (this instanceof EnterWorld)
			{
				getClient().closeNow();
			}
		}
	}
	
	protected abstract void runImpl();
	
	public final void sendPacket(final PacketServer gsp)
	{
		getClient().sendPacket(gsp);
	}
	
	public abstract String getType();
}