package l2jorion.game.model.actor.instance;

import java.util.StringTokenizer;

import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class L2CastleTeleporterInstance extends L2NpcInstance
{
	public static final Logger LOG = LoggerFactory.getLogger(L2CastleTeleporterInstance.class);
	
	private boolean _currentTask = false;
	
	public L2CastleTeleporterInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		if (actualCommand.equalsIgnoreCase("tele"))
		{
			int delay;
			if (!getTask())
			{
				if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
				{
					delay = 480000;
				}
				else
				{
					delay = 30000;
				}
				
				setTask(true);
				ThreadPoolManager.getInstance().scheduleGeneral(new oustAllPlayers(), delay);
			}
			
			final String filename = "data/html/castleteleporter/MassGK-1.htm";
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			player.sendPacket(html);
			return;
		}
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(final L2PcInstance player)
	{
		String filename;
		if (!getTask())
		{
			if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
			{
				filename = "data/html/castleteleporter/MassGK-2.htm";
			}
			else
			{
				filename = "data/html/castleteleporter/MassGK.htm";
			}
		}
		else
		{
			filename = "data/html/castleteleporter/MassGK-1.htm";
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	void oustAllPlayers()
	{
		getCastle().oustAllPlayers();
	}
	
	class oustAllPlayers implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				oustAllPlayers();
				setTask(false);
			}
			catch (final NullPointerException e)
			{
				LOG.warn("" + e.getMessage(), e);
			}
		}
	}
	
	public boolean getTask()
	{
		return _currentTask;
	}
	
	public void setTask(final boolean state)
	{
		_currentTask = state;
	}
}