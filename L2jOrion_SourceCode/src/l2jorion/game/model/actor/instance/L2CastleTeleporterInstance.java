package l2jorion.game.model.actor.instance;

/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.StringTokenizer;

import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class L2CastleTeleporterInstance extends L2NpcInstance
{
	
	/** The Constant LOG. */
	public static final Logger LOG = LoggerFactory.getLogger(L2CastleTeleporterInstance.class);
	
	/** The _current task. */
	private boolean _currentTask = false;
	
	/**
	 * Instantiates a new l2 castle teleporter instance.
	 * @param objectId the object id
	 * @param template the template
	 */
	public L2CastleTeleporterInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.actor.instance.L2NpcInstance#onBypassFeedback(l2jorion.game.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		
		if (actualCommand.equalsIgnoreCase("tele"))
		{
			int delay;
			if (!getTask())
			{
				if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
					delay = 480000;
				else
					delay = 30000;
				
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
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.actor.instance.L2NpcInstance#showChatWindow(l2jorion.game.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void showChatWindow(final L2PcInstance player)
	{
		String filename;
		if (!getTask())
		{
			if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
				filename = "data/html/castleteleporter/MassGK-2.htm";
			else
				filename = "data/html/castleteleporter/MassGK.htm";
		}
		else
			filename = "data/html/castleteleporter/MassGK-1.htm";
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	/**
	 * Oust all players.
	 */
	void oustAllPlayers()
	{
		getCastle().oustAllPlayers();
	}
	
	/**
	 * The Class oustAllPlayers.
	 */
	class oustAllPlayers implements Runnable
	{
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				/*
				 * CreatureSay cs = new CreatureSay(getObjectId(), 1, getName(), 1000443); // The defenders of $s1 castle will be teleported to the inner castle. cs.addStringParameter(getCastle().getName()); int region = MapRegionTable.getInstance().getMapRegion(getX(), getY());
				 * Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values(); //synchronized (L2World.getInstance().getAllPlayers().values()) { for (L2PcInstance player : pls) { if (region == MapRegionTable.getInstance().getMapRegion(player.getX(),player.getY())) player.sendPacket(cs); } }
				 */
				oustAllPlayers();
				setTask(false);
			}
			catch (final NullPointerException e)
			{
				LOG.warn("" + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Gets the task.
	 * @return the task
	 */
	public boolean getTask()
	{
		return _currentTask;
	}
	
	/**
	 * Sets the task.
	 * @param state the new task
	 */
	public void setTask(final boolean state)
	{
		_currentTask = state;
	}
	
}