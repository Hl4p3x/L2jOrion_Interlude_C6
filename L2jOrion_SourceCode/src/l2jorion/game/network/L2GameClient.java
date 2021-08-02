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
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.strixplatform.network.cipher.StrixGameCrypt;
import org.strixplatform.utils.StrixClientData;

import javolution.util.FastList;
import l2jguard.Protection;
import l2jorion.Config;
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
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.network.serverpackets.ServerClose;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.game.thread.LoginServerThread.SessionKey;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.EventData;
import l2jorion.game.util.FloodProtectors;
import l2jorion.mmocore.MMOClient;
import l2jorion.mmocore.MMOConnection;
import l2jorion.mmocore.ReceivablePacket;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>> implements Runnable
{
	protected static final Logger LOG = Logger.getLogger(L2GameClient.class.getName());
	
	protected static final Logger LogAccounting = Logger.getLogger("accounting");
	
	private static final String INSERT_SUBSCRIPTION = "INSERT INTO account_subscription (account_name,subscription,enddate) values(?,?,?)";
	private static final String RESTORE_SUBSCRIPTION = "SELECT subscription,enddate FROM account_subscription WHERE account_name=?";
	private static final String UPDATE_SUBSCRIPTION = "UPDATE account_subscription SET subscription=?,enddate=? WHERE account_name=?";
	private int _subsription;
	
	public static enum GameClientState
	{
		CONNECTED,
		AUTHED,
		ENTERING,
		IN_GAME,
		DISCONNECTED;
	}
	
	public GameClientState _state;
	
	// Info
	public String _accountName;
	public String _loginName;
	public SessionKey _sessionId;
	public L2PcInstance _activeChar;
	private ReentrantLock _activeCharLock = new ReentrantLock();
	
	private boolean _isAuthedGG;
	private long _connectionStartTime;
	private List<Integer> _charSlotMapping = new FastList<>();
	
	// flood protectors
	private final FloodProtectors _floodProtectors = new FloodProtectors(this);
	
	// Tasks
	private ScheduledFuture<?> _guardCheckTask = null;
	protected ScheduledFuture<?> _cleanupTask = null;
	
	private ClientStats _stats;
	
	// Crypt
	public GameCrypt _crypt;
	
	// Flood protection
	public long packetsNextSendTick = 0;
	
	protected boolean _closenow = true;
	private boolean _isDetached = false;
	protected boolean _forcedToClose = false;
	
	private final ArrayBlockingQueue<ReceivablePacket<L2GameClient>> _packetQueue;
	private ReentrantLock _queueLock = new ReentrantLock();
	
	private long _last_received_packet_action_time = 0;
	
	public StrixGameCrypt gameCrypt = null;
	private StrixClientData _clientData;
	
	public L2GameClient(MMOConnection<L2GameClient> con)
	{
		super(con);
		
		_state = GameClientState.CONNECTED;
		_connectionStartTime = System.currentTimeMillis();
		
		if (Config.STRIX_PROTECTION)
		{
			gameCrypt = new StrixGameCrypt();
		}
		else
		{
			_crypt = new GameCrypt();
		}
		
		_stats = new ClientStats();
		_packetQueue = new ArrayBlockingQueue<>(Config.CLIENT_PACKET_QUEUE_SIZE);
	}
	
	public byte[] enableCrypt()
	{
		byte[] key = BlowFishKeygen.getRandomKey();
		
		if (Config.L2JGUARD_PROTECTION)
		{
			_crypt.setKey(key);
			if (Protection.isProtectionOn())
			{
				key = Protection.getKey(key);
			}
			
			return key;
		}
		
		if (Config.STRIX_PROTECTION)
		{
			gameCrypt.setKey(key);
			return key;
		}
		
		GameCrypt.setKey(key, _crypt);
		
		return key;
	}
	
	public GameClientState getState()
	{
		return _state;
	}
	
	public void setState(GameClientState pState)
	{
		if (_state != pState)
		{
			_state = pState;
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
		
		if (Config.STRIX_PROTECTION)
		{
			gameCrypt.decrypt(buf.array(), buf.position(), size);
		}
		else
		{
			GameCrypt.decrypt(buf.array(), buf.position(), size, _crypt);
		}
		
		return true;
	}
	
	@Override
	public boolean encrypt(final ByteBuffer buf, final int size)
	{
		if (Config.STRIX_PROTECTION)
		{
			gameCrypt.encrypt(buf.array(), buf.position(), size);
		}
		else
		{
			GameCrypt.encrypt(buf.array(), buf.position(), size, _crypt);
		}
		
		buf.position(buf.position() + size);
		return true;
	}
	
	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}
	
	public void setActiveChar(L2PcInstance pActiveChar)
	{
		_activeChar = pActiveChar;
		if (_activeChar != null)
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
		_accountName = pAccountName;
	}
	
	public String getAccountName()
	{
		return _accountName;
	}
	
	public void setLoginName(String pAccountName)
	{
		_loginName = pAccountName;
	}
	
	public String getLoginName()
	{
		return _loginName;
	}
	
	public void setSessionId(SessionKey sk)
	{
		_sessionId = sk;
	}
	
	public SessionKey getSessionId()
	{
		return _sessionId;
	}
	
	public void sendPacket(L2GameServerPacket gsp)
	{
		if (_isDetached || (gsp == null))
		{
			return;
		}
		
		if (getConnection() != null)
		{
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
	
	public byte markToDeleteChar(int charslot)
	{
		int objid = getObjectIdForSlot(charslot);
		
		if (objid < 0)
		{
			return -1;
		}
		
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
			
			if (clanId != 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(clanId);
				
				if (clan == null)
				{
					answer = 0;
				}
				else if (clan.getLeaderId() == objid)
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
			if (answer == 0)
			{
				if (Config.DELETE_DAYS == 0)
				{
					deleteCharByObjId(objid);
				}
				else
				{
					statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE obj_Id=?");
					statement.setLong(1, System.currentTimeMillis() + Config.DELETE_DAYS * 86400000L);
					statement.setInt(2, objid);
					statement.execute();
					statement.close();
					rs.close();
				}
			}
			else
			{
				statement.close();
				rs.close();
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warning("Data error on update delete time of char: " + e);
			
			answer = -1;
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return answer;
	}
	
	public void markRestoredChar(int charslot)
	{
		int objid = getObjectIdForSlot(charslot);
		
		if (objid < 0)
		{
			return;
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.log(Level.SEVERE, "Error restoring character.", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		final LogRecord record = new LogRecord(Level.WARNING, "Restore");
		record.setParameters(new Object[]
		{
			objid,
			this
		});
		LogAccounting.log(record);
	}
	
	public static void deleteCharByObjId(int objid)
	{
		if (objid < 0)
		{
			return;
		}
		
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
			
			statement = con.prepareStatement("DELETE FROM heroes WHERE char_Id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;
			
			statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE char_Id=?");
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
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.log(Level.SEVERE, "Error deleting character.", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public L2PcInstance loadCharFromDisk(int charslot)
	{
		final int objId = getObjectIdForSlot(charslot);
		if (objId < 0)
		{
			return null;
		}
		
		L2PcInstance character = L2World.getInstance().getPlayer(objId);
		if (character != null)
		{
			LOG.severe("Attempt of double login: " + character.getName() + "(" + objId + ") " + getAccountName());
			
			if (character.getClient() != null)
			{
				character.getClient().closeNow();
			}
			else
			{
				character.deleteMe();
				
				try
				{
					character.store();
				}
				catch (Exception e2)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e2.printStackTrace();
					}
				}
				
			}
		}
		
		character = L2PcInstance.load(objId);
		if (character == null)
		{
			LOG.severe("could not restore in slot: " + charslot);
		}
		
		return character;
	}
	
	public void setCharSelection(CharSelectInfoPackage[] chars)
	{
		_charSlotMapping.clear();
		
		for (CharSelectInfoPackage c : chars)
		{
			int objectId = c.getObjectId();
			
			_charSlotMapping.add(Integer.valueOf(objectId));
		}
	}
	
	public void close(L2GameServerPacket gsp)
	{
		if (getConnection() != null)
		{
			getConnection().close(gsp);
		}
	}
	
	private int getObjectIdForSlot(int charslot)
	{
		if (charslot < 0 || charslot >= _charSlotMapping.size())
		{
			LOG.warning(toString() + " tried to delete Character in slot " + charslot + " but no characters exits at that slot.");
			return -1;
		}
		
		Integer objectId = _charSlotMapping.get(charslot);
		
		return objectId.intValue();
	}
	
	public void stopGuardTask()
	{
		if (_guardCheckTask != null)
		{
			_guardCheckTask.cancel(true);
			_guardCheckTask = null;
		}
		
	}
	
	@Override
	public String toString()
	{
		try
		{
			InetAddress address = getConnection().getInetAddress();
			String ip = "N/A";
			
			if (address == null)
			{
				ip = "Disconnected";
			}
			else
			{
				ip = address.getHostAddress();
			}
			
			switch (getState())
			{
				case CONNECTED:
					return "(IP: " + ip + ")";
				case AUTHED:
					return "(Account:" + getAccountName() + " IP:" + ip + ")";
				case ENTERING:
				case IN_GAME:
					return "(Character:" + (getActiveChar() == null ? "Disconnected" : getActiveChar().getName()) + " Account:" + getAccountName() + " IP:" + ip + ")";
				default:
					throw new IllegalStateException("Missing state on switch");
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return "Character read failed due to disconnect";
		}
	}
	
	@Override
	public void onForcedDisconnection()
	{
		_forcedToClose = true;
		
		setState(L2GameClient.GameClientState.DISCONNECTED);
		
		LogRecord record = new LogRecord(Level.WARNING, "Disconnected abnormally");
		record.setParameters(new Object[]
		{
			this
		});
		LogAccounting.log(record);
	}
	
	@Override
	public void onDisconnection()
	{
		setState(L2GameClient.GameClientState.DISCONNECTED);
		
		try
		{
			ThreadPoolManager.getInstance().executeTask(new DisconnectTask());
		}
		catch (RejectedExecutionException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void closeNow()
	{
		close(0);
	}
	
	public void close(int delay)
	{
		
		close(ServerClose.STATIC_PACKET);
		synchronized (this)
		{
			if (_cleanupTask != null)
			{
				cancelCleanup();
			}
			
			_cleanupTask = ThreadPoolManager.getInstance().scheduleGeneral(new CleanupTask(), delay); // delayed
		}
		
		if (Config.L2JGUARD_PROTECTION)
		{
			stopGuardTask();
			Protection.removePlayer(null);
		}
	}
	
	protected class DisconnectTask implements Runnable
	{
		@Override
		public void run()
		{
			boolean fast = true;
			try
			{
				L2PcInstance player = getActiveChar();
				if (player != null)
				{
					if (!player.isKicked() && !OlympiadManager.getInstance().isRegistered(player) && !player.isInOlympiadMode() && !player.isInFunEvent() && ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE) || (player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE)))
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
						
						final LogRecord record = new LogRecord(Level.INFO, "Entering offline mode (OFFLINE STORE)");
						record.setParameters(new Object[]
						{
							this
						});
						LogAccounting.log(record);
						return;
					}
					
					if (Config.SELLBUFF_SYSTEM_OFFLINE && player.isSellBuff() && player.isSitting() && !player.isKicked() && !OlympiadManager.getInstance().isRegistered(player) && !player.isInOlympiadMode() && !player.isInFunEvent())
					{
						player.setOfflineMode(true);
						player.setOnlineStatus(false);
						player.leaveParty();
						player.store();
						
						if (player.getOfflineStartTime() == 0)
						{
							player.setOfflineStartTime(System.currentTimeMillis());
						}
						
						final LogRecord record = new LogRecord(Level.INFO, "Entering offline mode (SELL BUFFS");
						record.setParameters(new Object[]
						{
							this
						});
						LogAccounting.log(record);
						return;
					}
					
					fast = !player.isInCombat() && !player.isLocked();
				}
				cleanMe(fast);
			}
			catch (Exception e1)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e1.printStackTrace();
				}
				
				LOG.log(Level.WARNING, "Error while cleanup client.", e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(getAccountName());
			}
		}
	}
	
	public void cleanMe(boolean fast)
	{
		try
		{
			synchronized (this)
			{
				if (_cleanupTask == null)
				{
					_cleanupTask = ThreadPoolManager.getInstance().scheduleGeneral(new CleanupTask(), fast ? 5 : 15000L);
				}
			}
		}
		catch (Exception e1)
		{
			LOG.log(Level.WARNING, "Error during cleanup.", e1);
		}
	}
	
	protected class CleanupTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				L2PcInstance player = getActiveChar();
				if (player != null)
				{
					if (player.isInArenaEvent())
					{
						// player.increaseArenaDefeats();
						player.setXYZ(82698, 148638, -3473);
					}
					
					if (player.isLocked())
					{
						LOG.log(Level.WARNING, "Player " + getActiveChar().getName() + " still performing subclass actions during disconnect.");
					}
					
					if (player.atEvent)
					{
						EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventKarma, player.eventPvpKills, player.eventPkKills, player.eventTitle, player.kills, player.eventSitForced);
						
						L2Event.connectionLossData.put(player.getName(), data);
					}
					else
					{
						if (player._inEventCTF)
						{
							CTF.onDisconnect(player);
						}
						else if (player._inEventDM)
						{
							DM.onDisconnect(player);
						}
						else if (player._inEventTvT)
						{
							TvT.onDisconnect(player);
						}
						else if (player._inEventVIP)
						{
							VIP.onDisconnect(player);
						}
						
					}
					
					if (player.isFlying())
					{
						player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
					}
					
					if (OlympiadManager.getInstance().isRegistered(player))
					{
						OlympiadManager.getInstance().unRegisterNoble(player);
					}
					
					try
					{
						player.store(_forcedToClose);
					}
					catch (Exception e2)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e2.printStackTrace();
						}
					}
					
					// Decrease boxes number
					if (player._active_boxes != -1)
					{
						player.decreaseBoxes();
					}
					
					// prevent closing again
					player.setClient(null);
					player.deleteMe();
				}
				
				setActiveChar(null);
				setDetached(true);
			}
			catch (Exception e1)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e1.printStackTrace();
				}
				
				LOG.log(Level.WARNING, "Error while cleanup client.", e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(getAccountName());
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
	
	public boolean dropPacket()
	{
		if (Config.CLIENT_FLOOD_PROTECTION)
		{
			if (_isDetached)
			{
				return true;
			}
			
			// flood protection
			if (getStats().countPacket(_packetQueue.size()))
			{
				LOG.log(Level.WARNING, "Detected flood: Client " + toString());
				sendPacket(ActionFailed.STATIC_PACKET);
				return true;
			}
			
			return getStats().dropPacket();
		}
		
		if (_isDetached)
		{
			return true;
		}
		
		return false;
	}
	
	public void onBufferUnderflow()
	{
		if (getStats().countUnderflowException())
		{
			LOG.log(Level.WARNING, "Client " + toString() + " - Disconnected: Too many buffer underflow exceptions.");
			closeNow();
			return;
		}
		
		if (_state == GameClientState.CONNECTED)
		{
			LOG.log(Level.WARNING, "Client " + toString() + " - Disconnected, too many buffer underflows in non-authed state.");
			closeNow();
		}
	}
	
	public void onUnknownPacket()
	{
		if (_state != GameClientState.ENTERING)
		{
			if (getStats().countUnknownPacket())
			{
				LOG.log(Level.WARNING, "Client " + toString() + " - Disconnected: Too many unknown packets.");
				closeNow();
				return;
			}
		}
		
		if (_state == GameClientState.CONNECTED)
		{
			LOG.log(Level.WARNING, "Client " + toString() + " - Disconnected, too many unknown packets in non-authed state.");
			closeNow();
		}
	}
	
	public void execute(ReceivablePacket<L2GameClient> packet)
	{
		if (getStats().countFloods())
		{
			LOG.log(Level.WARNING, "Client " + toString() + " - Disconnected, too many floods:" + getStats().longFloods + " long and " + getStats().shortFloods + " short.");
			closeNow();
			return;
		}
		
		if (!_packetQueue.offer(packet))
		{
			if (getStats().countQueueOverflow())
			{
				LOG.log(Level.WARNING, "Client " + toString() + " - Disconnected, too many queue overflows.");
				closeNow();
			}
			else
			{
				sendPacket(ActionFailed.STATIC_PACKET);
			}
			
			return;
		}
		
		if (_queueLock.isLocked()) // already processing
		{
			return;
		}
		
		// save last action time
		_last_received_packet_action_time = System.currentTimeMillis();
		
		try
		{
			if (_state == GameClientState.CONNECTED)
			{
				if (getStats().processedPackets > 3)
				{
					LOG.log(Level.WARNING, "Client " + toString() + " - Disconnected, too many packets in non-authed state.");
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
				LOG.log(Level.WARNING, "Failed executing: " + packet.getClass().getSimpleName() + " for Client: " + toString());
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
					
					LOG.log(Level.WARNING, "Exception during execution " + packet.getClass().getSimpleName() + ", client: " + toString() + "," + e.getMessage());
				}
				
				count++;
				
				if (getStats().countBurst(count))
				{
					LOG.log(Level.WARNING, "Client: " + toString() + " - Disconnected: burst is over limited:" + count);
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
	
	public boolean is_forcedToClose()
	{
		return _forcedToClose;
	}
	
	public boolean isConnectionAlive()
	{
		// if last received packet time is higher then Config.CHECK_CONNECTION_INACTIVITY_TIME --> check connection
		if (System.currentTimeMillis() - _last_received_packet_action_time > Config.CHECK_CONNECTION_INACTIVITY_TIME)
		{
			_last_received_packet_action_time = System.currentTimeMillis();
			return !getConnection().isClosed();
		}
		return true;
	}
	
	// HWID
	private String _playerName = "";
	private int _playerId = 0;
	private String _hwid = "";
	private int _revision = 0;
	
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
		_revision = revision;
	}
	
	public int getRevision()
	{
		return _revision;
	}
	
	private void createSubscriptionDB(String account)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_SUBSCRIPTION);
			statement.setString(1, account);
			statement.setInt(2, 0);
			statement.setLong(3, 0);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			LOG.log(Level.WARNING, "Subscription: Could not insert char data: " + e);
			return;
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private static void SubscriptiontimeOver(String account)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_SUBSCRIPTION);
			statement.setInt(1, 0);
			statement.setLong(2, 0);
			statement.setString(3, account);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.log(Level.WARNING, "Subscription: Could not increase data");
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void restoreSubscripionData(String account)
	{
		boolean sucess = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SUBSCRIPTION);
			statement.setString(1, account);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				sucess = true;
				if (Config.USE_SUBSCRIPTION)
				{
					if (rset.getLong("enddate") <= System.currentTimeMillis())
					{
						SubscriptiontimeOver(account);
						setSubscription(0);
					}
					else
					{
						setSubscription(rset.getInt("subscription"));
					}
				}
				else
				{
					setSubscription(0);
				}
			}
			statement.close();
			
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.log(Level.WARNING, "PremiumService: Could not restore PremiumService data for:" + account + "." + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		if (sucess == false)
		{
			createSubscriptionDB(account);
			setSubscription(0);
		}
	}
	
	public void setSubscription(int subsription)
	{
		_subsription = subsription;
	}
	
	public int getSubscription()
	{
		return _subsription;
	}
	
	public void setStrixClientData(final StrixClientData clientData)
	{
		_clientData = clientData;
	}
	
	public StrixClientData getStrixClientData()
	{
		return _clientData;
	}
}
