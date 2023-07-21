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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.game.thread.LoginServerThread.SessionKey;
import l2jorion.game.thread.ThreadPoolManager;

public final class AuthLogin extends PacketClient
{
	private String _loginName;
	private int _playKey1;
	private int _playKey2;
	private int _loginKey1;
	private int _loginKey2;
	
	@Override
	protected void readImpl()
	{
		_loginName = readS().toLowerCase();
		_playKey2 = readD();
		_playKey1 = readD();
		_loginKey1 = readD();
		_loginKey2 = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);
		final L2GameClient client = getClient();
		
		if (Config.USE_SUBSCRIPTION)
		{
			client.setLoginName(_loginName);
			client.restoreSubscripionData(client.getLoginName());
			if (client.getSubscription() == 0)
			{
				client.sendPacket(new SystemMessage(SystemMessageId.SUBSCRIPTION_MSG));
				
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						client.closeNow();
					}
				}, Config.SUBSCRIPTION_DC_TIME * 1000);
				return;
			}
		}
		
		if (client.getAccountName() == null)
		{
			if (LoginServerThread.getInstance().addGameServerLogin(_loginName, client))
			{
				client.setAccountName(_loginName);
				LoginServerThread.getInstance().addWaitingClientAndSendRequest(_loginName, client, key);
			}
			else
			{
				client.closeNow();
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 08 AuthLogin";
	}
}