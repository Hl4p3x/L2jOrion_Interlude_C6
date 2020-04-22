/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.network;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastList;
import l2jguard.Protection;
import l2jorion.Config;
import l2jorion.crypt.nProtect;
import l2jorion.game.datatables.OfflineTradeTable;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.model.CharSelectInfoPackage;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.L2Event;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.entity.olympiad.Olympiad;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.network.serverpackets.ServerClose;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.thread.LoginServerThread.SessionKey;
import l2jorion.game.util.EventData;
import l2jorion.game.util.FloodProtectors;
import l2jorion.log.Log;
import l2jorion.mmocore.MMOClient;
import l2jorion.mmocore.MMOConnection;
import l2jorion.mmocore.ReceivablePacket;
import l2jorion.util.CloseUtil;
import l2jorion.util.OlympiadLogger;
import l2jorion.util.database.L2DatabaseFactory;

public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>> implements Runnable
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2GameClient.class);
	
	public static enum GameClientState
	{
		CONNECTED,
		AUTHED,
		ENTERING,
		IN_GAME
	}

	public GameClientState state;

	// Info
	public String accountName;
	public SessionKey sessionId;
	public L2PcInstance activeChar;
	private ReentrantLock _activeCharLock = new ReentrantLock();

	private boolean _isAuthedGG;
	private long _connectionStartTime;
	private List<Integer> _charSlotMapping = new FastList<>();
	
	// floodprotectors
	private final FloodProtectors _floodProtectors = new FloodProtectors(this);
	
	// Task
	private ScheduledFuture<?> _guardCheckTask = null;

	protected ScheduledFuture<?> _cleanupTask = null;
	
	private ClientStats _stats;
	
	// Crypt
	public GameCrypt crypt;

	// Flood protection
	public long packetsNextSendTick = 0;
	
	protected boolean _closenow = true;
	private boolean _isDetached = false;

	protected boolean _forcedToClose = false;
	
	private final ArrayBlockingQueue<ReceivablePacket<L2GameClient>> _packetQueue;
	private ReentrantLock _queueLock = new ReentrantLock();
	
	private long _last_received_packet_action_time = 0;
	
	public L2GameClient(MMOConnection<L2GameClient> con)
	{
		super(con);
		state = GameClientState.CONNECTED;
		_connectionStartTime = System.currentTimeMillis();
		crypt = new GameCrypt();
		_stats = new ClientStats();
		_packetQueue = new ArrayBlockingQueue<>(Config.CLIENT_PACKET_QUEUE_SIZE);
	}

	public byte[] enableCrypt()
	{
		byte[] key = BlowFishKeygen.getRandomKey();
		
		if (!Config.L2JGUARD_PROTECTION)
		{
			GameCrypt.setKey(key, crypt);
		}
		else
		{
			crypt.setKey(key);
			if (Protection.isProtectionOn())
			{
				key = Protection.getKey(key);
			}
		}
		
		return key;
	}

	public GameClientState getState()
	{
		return state;
	}

	public void setState(GameClientState pState)
	{
		if (state != pState)
		{
			state = pState;
			_packetQueue.clear();
		}
	}

	public ClientStats getStats()
	{
		return _stats;
	}
	
	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		_closenow = false;
		GameCrypt.decrypt(buf.array(), buf.position(), size, crypt);
		return true;
	}

	@Override
	public boolean encrypt(final ByteBuffer buf, final int size)
	{
		GameCrypt.encrypt(buf.array(), buf.position(), size, crypt);
		buf.position(buf.position() + size);
		return true;
	}

	public L2PcInstance getActiveChar()
	{
		return activeChar;
	}

	public void setActiveChar(L2PcInstance pActiveChar)
	{
		activeChar = pActiveChar;
		if (activeChar != null)
		{
			L2World.getInstance().storeObject(getActiveChar());
		}
	}

	public ReentrantLock getActiveCharLock()
	{
		return _activeCharLock;
	}

	public boolean isAuthedGG()
	{
		return _isAuthedGG;
	}

	public void setGameGuardOk(boolean val)
	{
		_isAuthedGG = val;
	}

	public void setAccountName(String pAccountName)
	{
		accountName = pAccountName;
	}

	public String getAccountName()
	{
		return accountName;
	}

	public void setSessionId(SessionKey sk)
	{
		sessionId = sk;
	}

	public SessionKey getSessionId()
	{
		return sessionId;
	}

	public void sendPacket(L2GameServerPacket gsp)
	{
		if (_isDetached)
		{
			return;
		}
		
		if (getConnection() != null)
		{
			
			if (Config.PACKET_HANDLER_DEBUG)
			{
				Log.add("[ServerPacket] SendingGameServerPacket, Client: "+this.toString()+" Packet:"+gsp.getType(), "GameServerPacketsLog");
			}
			getConnection().sendPacket(gsp);
			gsp.runImpl();
		}
	}

	public boolean isDetached()
	{
		return _isDetached;
	}
		
	public void setDetached(boolean b)
	{
		_isDetached = b;
	}
	
	/**
	 * Method to handle character deletion
	 * @param charslot 
	 * @return a byte: <li>-1: Error: No char was found for such charslot, caught exception, etc... <li>0: character is
	 *         not member of any clan, proceed with deletion <li>1: character is member of a clan, but not clan leader
	 *         <li>2: character is clan leader
	 */
	public byte markToDeleteChar(int charslot)
	{

		int objid = getObjectIdForSlot(charslot);

		if(objid < 0)
			return -1;

		Connection con = null;
		byte answer = -1;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clanId from characters WHERE obj_Id=?");
			statement.setInt(1, objid);
			ResultSet rs = statement.executeQuery();

			rs.next();

			int clanId = rs.getInt(1);

			answer = 0;
			
			if(clanId != 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(clanId);

				if(clan == null)
				{
					answer = 0; // jeezes!
				}
				else if(clan.getLeaderId() == objid)
				{
					answer = 2;
				}
				else
				{
					answer = 1;
				}

				clan = null;
			}

			// Setting delete time
			if(answer == 0)
			{
				if(Config.DELETE_DAYS == 0)
				{
					deleteCharByObjId(objid);
				}
				else
				{
					statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE obj_Id=?");
					statement.setLong(1, System.currentTimeMillis() + Config.DELETE_DAYS * 86400000L); // 24*60*60*1000 = 86400000
					statement.setInt(2, objid);
					statement.execute();
					statement.close();
					rs.close();
					statement = null;
					rs = null;
				}
			}
			else
			{
				statement.close();
				rs.close();
				statement = null;
				rs = null;
			}

			
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("Data error on update delete time of char: " + e);
			
			answer = -1;
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		
		return answer;
	}

	public void markRestoredChar(int charslot)
	{
		int objid = getObjectIdForSlot(charslot);
		
		if(objid < 0)
			return;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("Data error on restoring char: " + e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}

	public static void deleteCharByObjId(int objid)
	{
		if(objid < 0)
			return;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id=? OR friend_id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM heroes WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM seven_signs WHERE char_obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM characters WHERE obj_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("Data error on deleting char: " + e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}

	public L2PcInstance loadCharFromDisk(int charslot)
	{
		//L2PcInstance character = L2PcInstance.load(getObjectIdForSlot(charslot));

		final int objId = getObjectIdForSlot(charslot);
		if (objId < 0)
			return null;

		L2PcInstance character = L2World.getInstance().getPlayer(objId);
		if (character != null)
		{
			// exploit prevention, should not happens in normal way
			if(Config.DEBUG)
				LOG.error("Attempt of double login: " + character.getName()+"("+objId+") "+getAccountName());
			
			if (character.getClient() != null)
				character.getClient().closeNow();
			else{
				character.deleteMe();
				
				try
				{
					character.store();
				}
				catch(Exception e2)
				{
					if(Config.ENABLE_ALL_EXCEPTIONS)
						e2.printStackTrace();
				}
				
			}
		}
		
		character = L2PcInstance.load(objId);
		
		return character;
	}

	/**
	 * @param chars
	 */
	public void setCharSelection(CharSelectInfoPackage[] chars)
	{
		_charSlotMapping.clear();

		for(CharSelectInfoPackage c : chars)
		{
			int objectId = c.getObjectId();

			_charSlotMapping.add(new Integer(objectId));
		}
	}

	public void close(L2GameServerPacket gsp)
	{
		if (getConnection() != null)
		{
			getConnection().close(gsp);
		}
	}

	/**
	 * @param charslot
	 * @return
	 */
	private int getObjectIdForSlot(int charslot)
	{
		if(charslot < 0 || charslot >= _charSlotMapping.size())
		{
			LOG.warn(toString() + " tried to delete Character in slot " + charslot + " but no characters exits at that slot.");
			return -1;
		}

		Integer objectId = _charSlotMapping.get(charslot);

		return objectId.intValue();
	}

	@Override
	public void onForcedDisconnection()
	{
		_forcedToClose = true;
		
		final String text = "Client " + toString() + " disconnected abnormally.";
		Log.add(text, "Chars_disconnection_logs");
		
		closeNow();
	}

	public void stopGuardTask()
	{
		if(_guardCheckTask != null)
		{
			_guardCheckTask.cancel(true);
			_guardCheckTask = null;
		}

	}

	@Override
	public void onDisconnection()
	{
		try
		{
			ThreadPoolManager.getInstance().executeTask(new DisconnectTask());

		}
		catch(RejectedExecutionException e)
		{
			// server is closing
			if(Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Close client connection with {@link ServerClose} packet
	 */
	public void closeNow()
	{
		close(0);
	}
	
	/**
	 * Close client connection with {@link ServerClose} packet
	 * @param delay 
	 */
	public void close(int delay)
	{
		
		close(ServerClose.STATIC_PACKET);
		synchronized (this)
		{
			if (_cleanupTask != null)
				cancelCleanup();
			
			_cleanupTask = ThreadPoolManager.getInstance().scheduleGeneral(new CleanupTask(), delay); //delayed
		}
		
		stopGuardTask();
		nProtect.getInstance().closeSession(this);
		
		if (Config.L2JGUARD_PROTECTION)
		{
			Protection.removePlayer(null);
		}
	}
	
	/**
	 * Produces the best possible string representation of this client.
	 */
	@Override
	public String toString()
	{
		try
		{
			InetAddress address = getConnection().getInetAddress();
			String ip = "N/A";
			
			if (address == null)
			{
				ip = "disconnected";
			}
			else
			{
				ip = address.getHostAddress();
			}
			
			switch (getState())
			{
				case CONNECTED:
					return "[IP: " + ip + "]";
				case AUTHED:
					return "[Account: " + getAccountName() + " - IP: " + ip + "]";
				case ENTERING:
				case IN_GAME:
					address = null;
					return "[Character: " + (getActiveChar() == null ? "disconnected" : getActiveChar().getName()) + " - Account: " + getAccountName() + " - IP: " + ip + "]";
				default:
					address = null;
					throw new IllegalStateException("Missing state on switch");
			}
		}
		catch(NullPointerException e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			return "[Character read failed due to disconnect]";
		}
	}
	
	protected class CleanupTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				L2PcInstance player = L2GameClient.this.getActiveChar();
				if (player != null)
				{
					if (Config.ENABLE_OLYMPIAD_DISCONNECTION_DEBUG) {
						if (player.isInOlympiadMode()
							|| player.inObserverMode()) {
							if (player.isInOlympiadMode()) {
								String text = "Player " + player.getName()
									+ ", Class:" + player.getClassId()
									+ ", Level:" + player.getLevel()
									+ ", Mode: Olympiad, Loc: "
									+ player.getX() + " Y:" + player.getY()
									+ " Z:" + player.getZ()
									+ ", Critical?: " + _forcedToClose;
								OlympiadLogger.add(text, "Olympiad_crash_debug");
							} else if (player.inObserverMode()) {
								String text = "Player " + player.getName()
									+ ", Class:" + player.getClassId()
									+ ", Level:" + player.getLevel()
									+ ", Mode: Observer, Loc: "
									+ player.getX() + " Y:" + player.getY()
									+ " Z:" + player.getZ()
									+ ", Critical?: " + _forcedToClose;
								OlympiadLogger.add(text, "Olympiad_crash_debug");
							} else {
								String text = "Player " + player.getName()
									+ ", Class:" + player.getClassId()
									+ ", Level:" + player.getLevel()
									+ ", Mode: Default, Loc: "
									+ player.getX() + " Y:" + player.getY()
									+ " Z:" + player.getZ()
									+ ", Critical?: " + _forcedToClose;
								OlympiadLogger.add(text, "Olympiad_crash_debug");
							}
						}
					}
					// we store all data from players who are disconnected while in an event in order to restore it in the next login
					if(player.atEvent)
					{
						EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventKarma, player.eventPvpKills, player.eventPkKills, player.eventTitle, player.kills, player.eventSitForced);

						L2Event.connectionLossData.put(player.getName(), data);
						data = null;
					}else{
						
						if(player._inEventCTF){
							CTF.onDisconnect(player);
						}else if(player._inEventDM){
							DM.onDisconnect(player);
						}else if(player._inEventTvT){
							TvT.onDisconnect(player);
						}else if(player._inEventVIP){
							VIP.onDisconnect(player);
						}
						
					}

					if(player.isFlying())
					{
						player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
					}
					
					if(Olympiad.getInstance().isRegistered(player)){
						Olympiad.getInstance().unRegisterNoble(player);
					}

					//Decrease boxes number
					if(player._active_boxes!=-1)
						player.decreaseBoxes();
					
					// prevent closing again
					player.setClient(null);
					
					player.deleteMe();
					
					try
					{
						player.store(_forcedToClose);
					}
					catch(Exception e2)
					{
						if(Config.ENABLE_ALL_EXCEPTIONS)
							e2.printStackTrace();
					}
					
				}
				
				L2GameClient.this.setActiveChar(null);
				L2GameClient.this.setDetached(true);
			}
			catch (Exception e1)
			{
				if(Config.ENABLE_ALL_EXCEPTIONS)
					e1.printStackTrace();
				
				LOG.error("Error while cleanup client.", e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(L2GameClient.this.getAccountName());
			}
		}
	}
	
	protected class DisconnectTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				L2PcInstance player = L2GameClient.this.getActiveChar();
				if (player != null)
				{
					if (player.atEvent)
					{
						EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventKarma, player.eventPvpKills, player.eventPkKills, player.eventTitle, player.kills, player.eventSitForced);
						L2Event.connectionLossData.put(player.getName(), data);
					}
					else
					{
						
						if(player._inEventCTF)
						{
							CTF.onDisconnect(player);
						}
						else if(player._inEventDM)
						{
							DM.onDisconnect(player);
						}
						else if(player._inEventTvT)
						{
							TvT.onDisconnect(player);
						}
						else if(player._inEventVIP)
						{
							VIP.onDisconnect(player);
						}
						
					}
					
					if (player.isFlying())
					{
						player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
					}
					
					if (Olympiad.getInstance().isRegistered(player))
					{
						Olympiad.getInstance().unRegisterNoble(player);
					}
					
					//Decrease boxes number
					if (player._active_boxes != -1)
					{
						player.decreaseBoxes();
					}
					
					if (!player.isKicked() && !Olympiad.getInstance().isRegistered(player) && !player.isInOlympiadMode() && !player.isInFunEvent() && ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE) 
							|| (player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE)))
					{
						player.setOfflineMode(true);
						player.setOnlineStatus(false);
						player.leaveParty();
						
						if (Config.OFFLINE_SET_NAME_COLOR)
						{
							String color = player.StringToHex(Integer.toHexString(player.getAppearance().getNameColor()).toUpperCase());
							player._originalNameColorOffline = color;
							player.getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
							player.broadcastUserInfo();
						}
						
						player.store();
						
						if (player.getOfflineStartTime() == 0)
						{
							player.setOfflineStartTime(System.currentTimeMillis());
						}
						
						OfflineTradeTable.storeOffliner(player);
						
						return;
					}
					
					if (Config.SELLBUFF_SYSTEM_OFFLINE 
							&& player.isSellBuff() 
							&& player.isSitting() 
							&& !player.isKicked() 
							&& !Olympiad.getInstance().isRegistered(player) 
							&& !player.isInOlympiadMode() 
							&& !player.isInFunEvent())
					{
						player.setOfflineMode(true);
						player.setOnlineStatus(false);
						player.leaveParty();
						player.store();
						
						if (player.getOfflineStartTime() == 0)
						{
							player.setOfflineStartTime(System.currentTimeMillis());
						}
						
						return;
					}
					
					// notify the world about our disconnect
					player.deleteMe();
					
					//store operation
					try
					{
						player.store();
					}
					catch(Exception e2)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
							e2.printStackTrace();
					}
					
				}
				
				L2GameClient.this.setActiveChar(null);
				L2GameClient.this.setDetached(true);
			}
			catch(Exception e1)
			{
				if(Config.ENABLE_ALL_EXCEPTIONS)
					e1.printStackTrace();
				
				LOG.error("error while disconnecting client", e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(L2GameClient.this.getAccountName());
			}
		}
	}

	public FloodProtectors getFloodProtectors()
	{
		return _floodProtectors;
	}
	
	private boolean cancelCleanup()
	{
		Future<?> task = _cleanupTask;
		if (task != null)
		{
			_cleanupTask = null;
			return task.cancel(true);
		}
		return false;
	}
	
	/**
	 * Returns false if client can receive packets.
	 * True if detached, or flood detected, or queue overflow detected and queue still not empty.
	 * @return 
	 */
	public boolean dropPacket()
	{
		if (_isDetached)
		{
			return true;
		}
		
		// flood protection
		if (getStats().countPacket(_packetQueue.size()))
		{
			LOG.error("Client " + toString() + " - Detected: long flood count:"+_packetQueue.size());
			//closeNow();
			sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		return getStats().dropPacket();
	}
	
	/**
	 * Counts buffer underflow exceptions.
	 */
	public void onBufferUnderflow()
	{
		if (getStats().countUnderflowException())
		{
			if (Config.PACKET_HANDLER_DEBUG)
			{
				LOG.error("Client " + toString() + " - Disconnected: Too many buffer underflow exceptions.");
			}
			closeNow();
			return;
		}
		if (state == GameClientState.CONNECTED) // in CONNECTED state kick client immediately
		{
			if (Config.PACKET_HANDLER_DEBUG)
			{
				LOG.error("Client " + toString() + " - Disconnected, too many buffer underflows in non-authed state.");
			}
			closeNow();
		}
	}
	

	/**
	 * Counts unknown packets
	 */
	public void onUnknownPacket()
	{
		if (state != GameClientState.ENTERING)
		{
			if (getStats().countUnknownPacket())
			{
				LOG.error("Client " + toString() + " - Disconnected: Too many unknown packets.");
				closeNow();
				return;
			}
		}
		
		if (state == GameClientState.CONNECTED)
		{
			LOG.error("Client " + toString() + " - Disconnected, too many unknown packets in non-authed state.");
			closeNow();
		}
	}
	
	/**
	 * Add packet to the queue and start worker thread if needed
	 * @param packet 
	 */
	public void execute(ReceivablePacket<L2GameClient> packet)
	{
		if (getStats().countFloods())
		{
			LOG.error("Client " + toString() + " - Disconnected, too many floods:"+getStats().longFloods+" long and "+getStats().shortFloods+" short.");
			closeNow();
			return;
		}
		
		if (!_packetQueue.offer(packet))
		{
			if (getStats().countQueueOverflow())
			{
				LOG.error("Client " + toString() + " - Disconnected, too many queue overflows.");
				closeNow();
			}
			else
				sendPacket(ActionFailed.STATIC_PACKET);
			
			return;
		}
		
		if (_queueLock.isLocked()) // already processing
			return;
		
		//save last action time
		_last_received_packet_action_time = System.currentTimeMillis();
		//LOG.error("Client " + toString() + " - updated last action state "+_last_received_packet_action_time);
						
		try
		{
			if (state == GameClientState.CONNECTED)
			{
				if (getStats().processedPackets > 3)
				{
					LOG.error("Client " + toString() + " - Disconnected, too many packets in non-authed state.");
					closeNow();
					return;
				}
				
				ThreadPoolManager.getInstance().executeIOPacket(this);
			}
			else
			{
				ThreadPoolManager.getInstance().executePacket(this);
			}
		}
		catch (RejectedExecutionException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			if (!ThreadPoolManager.getInstance().isShutdown())
			{
				LOG.error("Failed executing: "+packet.getClass().getSimpleName()+" for Client: "+toString());
			}
		}
	}
	
	@Override
	public void run()
	{
		if (!_queueLock.tryLock())
		{
			return;
		}
		
		try
		{
			int count = 0;
			while (true)
			{
				final ReceivablePacket<L2GameClient> packet = _packetQueue.poll();
				if (packet == null) // queue is empty
				{
					return;
				}
				
				if (_isDetached) // clear queue immediately after detach
				{
					_packetQueue.clear();
					return;
				}
				
				try
				{
					packet.run();
				}
				catch (Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.error("Exception during execution "+packet.getClass().getSimpleName()+", client: "+toString()+","+e.getMessage());
				}
				
				count++;
				
				if (getStats().countBurst(count))
				{
					LOG.error("Client: "+toString()+" - Disconnected: burst is over limited:"+count);
					closeNow();
					return;
				}
			}
		}
		finally
		{
			_queueLock.unlock();
		}
	}

	/**
	 * @return the _forcedToClose
	 */
	public boolean is_forcedToClose()
	{
		return _forcedToClose;
	}
	
	public boolean isConnectionAlive()
	{
		//if last received packet time is higher then Config.CHECK_CONNECTION_INACTIVITY_TIME --> check connection
		if (System.currentTimeMillis() - _last_received_packet_action_time > Config.CHECK_CONNECTION_INACTIVITY_TIME)
		{
			_last_received_packet_action_time = System.currentTimeMillis();
			return !getConnection().isClosed();
		}
		return true;
	}
	
	// HWID
	private String _playerName = "";
	private String _loginName = "";
	private int _playerId = 0;
	private String _hwid = "";
	private int revision = 0;

	public final String getPlayerName()
	{
		return _playerName;
	}

	public void setPlayerName(String name)
	{
		_playerName = name;
	}
	
	public void setPlayerId(int plId)
	{
		_playerId = plId;
	}
	
	public int getPlayerId()
	{
		return _playerId;
	}
	
	public final String getHWID()
	{
		return _hwid;
	}
	
	public void setHWID(String hwid)
	{
		_hwid = hwid;
	}
	
	public void setRevision(int revision)
	{
		this.revision = revision;
	}
	
	public int getRevision()
	{
		return this.revision;
	}
	
	public final String getLoginName()
	{
		return _loginName;
	}
	
	public void setLoginName(String name)
	{
		_loginName = name;
	}
}
