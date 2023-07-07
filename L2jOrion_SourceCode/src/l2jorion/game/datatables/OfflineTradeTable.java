package l2jorion.game.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2ManufactureItem;
import l2jorion.game.model.L2ManufactureList;
import l2jorion.game.model.L2World;
import l2jorion.game.model.TradeList.TradeItem;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.L2GameClient.GameClientState;
import l2jorion.game.thread.LoginServerThread;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class OfflineTradeTable
{
	private static Logger LOG = LoggerFactory.getLogger(OfflineTradeTable.class);
	
	private static final String SAVE_OFFLINE_STATUS = "INSERT INTO character_offline_trade (`charId`,`name`,`time`,`type`,`title`) VALUES (?,?,?,?,?)";
	private static final String SAVE_ITEMS = "INSERT INTO character_offline_trade_items (`charId`,`item`,`count`,`price`,`enchant`) VALUES (?,?,?,?,?)";
	private static final String DELETE_OFFLINE_TABLE_ALL_ITEMS = "delete from character_offline_trade_items where charId=?";
	private static final String DELETE_OFFLINE_TRADER = "DELETE FROM character_offline_trade where charId=?";
	private static final String CLEAR_OFFLINE_TABLE = "DELETE FROM character_offline_trade";
	private static final String CLEAR_OFFLINE_TABLE_ITEMS = "DELETE FROM character_offline_trade_items";
	private static final String LOAD_OFFLINE_STATUS = "SELECT * FROM character_offline_trade";
	private static final String LOAD_OFFLINE_ITEMS = "SELECT * FROM character_offline_trade_items WHERE charId = ?";
	
	// Save on server shutdown/restart
	public static void storeOffliners()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement stm = con.prepareStatement(CLEAR_OFFLINE_TABLE);
			stm.execute();
			stm.close();
			
			stm = con.prepareStatement(CLEAR_OFFLINE_TABLE_ITEMS);
			stm.execute();
			stm.close();
			
			con.setAutoCommit(false);
			
			stm = con.prepareStatement(SAVE_OFFLINE_STATUS);
			PreparedStatement stm_items = con.prepareStatement(SAVE_ITEMS);
			
			for (L2PcInstance pc : L2World.getInstance().getAllPlayers().values())
			{
				try
				{
					if ((pc.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE))
					{
						stm.setInt(1, pc.getObjectId());
						stm.setString(2, pc.getName());
						stm.setLong(3, pc.getOfflineStartTime());
						stm.setInt(4, pc.getPrivateStoreType());
						String title = null;
						
						switch (pc.getPrivateStoreType())
						{
							case L2PcInstance.STORE_PRIVATE_BUY:
								if (!Config.OFFLINE_TRADE_ENABLE)
								{
									continue;
								}
								
								title = pc.getBuyList().getTitle();
								for (TradeItem i : pc.getBuyList().getItems())
								{
									stm_items.setInt(1, pc.getObjectId());
									stm_items.setInt(2, i.getItem().getItemId());
									stm_items.setLong(3, i.getCount());
									stm_items.setLong(4, i.getPrice());
									stm_items.setLong(5, i.getEnchant());
									stm_items.executeUpdate();
									stm_items.clearParameters();
								}
								break;
							case L2PcInstance.STORE_PRIVATE_SELL:
							case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
								if (!Config.OFFLINE_TRADE_ENABLE)
								{
									continue;
								}
								
								title = pc.getSellList().getTitle();
								pc.getSellList().updateItems();
								for (TradeItem i : pc.getSellList().getItems())
								{
									stm_items.setInt(1, pc.getObjectId());
									stm_items.setInt(2, i.getObjectId());
									stm_items.setLong(3, i.getCount());
									stm_items.setLong(4, i.getPrice());
									stm_items.setLong(5, i.getEnchant());
									stm_items.executeUpdate();
									stm_items.clearParameters();
								}
								break;
							case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
								if (!Config.OFFLINE_CRAFT_ENABLE)
								{
									continue;
								}
								
								title = pc.getCreateList().getStoreName();
								for (L2ManufactureItem i : pc.getCreateList().getList())
								{
									stm_items.setInt(1, pc.getObjectId());
									stm_items.setInt(2, i.getRecipeId());
									stm_items.setLong(3, 0);
									stm_items.setLong(4, i.getCost());
									stm_items.setLong(5, 0);
									stm_items.executeUpdate();
									stm_items.clearParameters();
								}
								break;
							default:
								continue;
						}
						
						stm.setString(5, title);
						stm.executeUpdate();
						stm.clearParameters();
						con.commit(); // flush
					}
				}
				catch (SQLException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.warn("OfflineTradersTable[storeTradeItems()]: Error while saving offline trader: " + pc.getObjectId() + " " + e, e);
				}
			}
			
			stm.close();
			stm_items.close();
		}
		catch (SQLException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("OfflineTradersTable[storeTradeItems()]: Error while saving offline traders: " + e, e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public static void restoreOfflineTraders()
	{
		Connection con = null;
		int nTraders = 0;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement(LOAD_OFFLINE_STATUS);
			ResultSet rs = stm.executeQuery();
			while (rs.next())
			{
				long time = rs.getLong("time");
				
				if (Config.OFFLINE_MAX_DAYS > 0)
				{
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(time);
					cal.add(Calendar.DAY_OF_YEAR, Config.OFFLINE_MAX_DAYS);
					
					if (cal.getTimeInMillis() <= System.currentTimeMillis())
					{
						LOG.info("Offline trader: " + rs.getString("name") + " (" + rs.getInt("charId") + ") reached limit of offline time, kicked");
						continue;
					}
				}
				
				int type = rs.getInt("type");
				if (type == L2PcInstance.STORE_PRIVATE_NONE)
				{
					continue;
				}
				
				L2PcInstance player = null;
				
				try
				{
					L2GameClient client = new L2GameClient(null);
					player = L2PcInstance.load(rs.getInt("charId"));
					L2World.getInstance().addPlayerToWorld(player);
					
					client.setActiveChar(player);
					client.setAccountName(player.getAccountName());
					
					client.setState(GameClientState.IN_GAME);
					
					player.setOfflineMode(true);
					player.setOnlineStatus(false);
					player.setOfflineStartTime(time);
					
					player.spawnMe(player.getX(), player.getY(), player.getZ());
					
					LoginServerThread.getInstance().addGameServerLogin(player.getAccountName(), client);
					PreparedStatement stm_items = con.prepareStatement(LOAD_OFFLINE_ITEMS);
					stm_items.setInt(1, player.getObjectId());
					ResultSet items = stm_items.executeQuery();
					
					switch (type)
					{
						case L2PcInstance.STORE_PRIVATE_BUY:
							while (items.next())
							{
								player.getBuyList().addItemByItemId(items.getInt(2), items.getInt(3), items.getInt(4), items.getInt(5));
							}
							
							player.getBuyList().setTitle(rs.getString("title"));
							break;
						case L2PcInstance.STORE_PRIVATE_SELL:
						case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
							while (items.next())
							{
								player.getSellList().addItem(items.getInt(2), items.getInt(3), items.getInt(4));
							}
							
							player.getSellList().setTitle(rs.getString("title"));
							player.getSellList().setPackaged(type == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL);
							break;
						case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
							L2ManufactureList createList = new L2ManufactureList();
							while (items.next())
							{
								createList.add(new L2ManufactureItem(items.getInt(2), items.getInt(4)));
							}
							
							player.setCreateList(createList);
							player.getCreateList().setStoreName(rs.getString("title"));
							break;
						default:
							LOG.info("Offline trader " + player.getName() + " finished to sell his items");
					}
					
					items.close();
					stm_items.close();
					
					player.sitDown();
					
					if (Config.OFFLINE_SET_NAME_COLOR)
					{
						String color = player.StringToHex(Integer.toHexString(player.getAppearance().getNameColor()).toUpperCase());
						player._originalNameColorOffline = color;
						player.getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
					}
					
					player.setPrivateStoreType(type);
					player.setOnlineStatus(true);
					player.restoreEffects();
					
					if (Config.OFFLINE_SLEEP_EFFECT)
					{
						player.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_SLEEP);
					}
					
					player.broadcastUserInfo();
					nTraders++;
				}
				catch (Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.warn("OfflineTradersTable[loadOffliners()]: Error loading trader: ", e);
					
					if (player != null)
					{
						player.logout();
					}
				}
			}
			
			rs.close();
			stm.close();
			
			LOG.info("OfflineTradeTable: Loaded " + nTraders + " offline traders");
			
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("OfflineTradersTable[loadOffliners()]: Error while loading offline traders: ", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	// Store on exit
	public static void storeOffliner(L2PcInstance pc)
	{
		if ((pc.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE) || (!pc.isInOfflineMode()))
		{
			return;
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement stm = con.prepareStatement(DELETE_OFFLINE_TABLE_ALL_ITEMS);
			stm.setInt(1, pc.getObjectId());
			stm.execute();
			stm.clearParameters();
			stm.close();
			
			stm = con.prepareStatement(DELETE_OFFLINE_TRADER);
			stm.setInt(1, pc.getObjectId());
			stm.execute();
			stm.clearParameters();
			stm.close();
			
			con.setAutoCommit(false);
			
			stm = con.prepareStatement(SAVE_OFFLINE_STATUS);
			PreparedStatement stm_items = con.prepareStatement(SAVE_ITEMS);
			
			boolean save = true;
			
			try
			{
				stm.setInt(1, pc.getObjectId()); // Char Id
				stm.setString(2, pc.getName()); // char name
				stm.setLong(3, pc.getOfflineStartTime());
				stm.setInt(4, pc.getPrivateStoreType()); // store type
				String title = null;
				
				switch (pc.getPrivateStoreType())
				{
					case L2PcInstance.STORE_PRIVATE_BUY:
						if (!Config.OFFLINE_TRADE_ENABLE)
						{
							break;
						}
						
						title = pc.getBuyList().getTitle();
						for (TradeItem i : pc.getBuyList().getItems())
						{
							stm_items.setInt(1, pc.getObjectId());
							stm_items.setInt(2, i.getItem().getItemId());
							stm_items.setLong(3, i.getCount());
							stm_items.setLong(4, i.getPrice());
							stm_items.setLong(5, i.getEnchant());
							stm_items.executeUpdate();
							stm_items.clearParameters();
						}
						break;
					case L2PcInstance.STORE_PRIVATE_SELL:
					case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
						if (!Config.OFFLINE_TRADE_ENABLE)
						{
							break;
						}
						
						title = pc.getSellList().getTitle();
						pc.getSellList().updateItems();
						for (TradeItem i : pc.getSellList().getItems())
						{
							stm_items.setInt(1, pc.getObjectId());
							stm_items.setInt(2, i.getObjectId());
							stm_items.setLong(3, i.getCount());
							stm_items.setLong(4, i.getPrice());
							stm_items.setLong(5, i.getEnchant());
							stm_items.executeUpdate();
							stm_items.clearParameters();
						}
						break;
					case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
						if (!Config.OFFLINE_CRAFT_ENABLE)
						{
							break;
						}
						
						title = pc.getCreateList().getStoreName();
						for (L2ManufactureItem i : pc.getCreateList().getList())
						{
							stm_items.setInt(1, pc.getObjectId());
							stm_items.setInt(2, i.getRecipeId());
							stm_items.setLong(3, 0);
							stm_items.setLong(4, i.getCost());
							stm_items.setLong(5, 0);
							stm_items.executeUpdate();
							stm_items.clearParameters();
						}
						break;
					default:
						save = false;
				}
				
				if (save)
				{
					stm.setString(5, title);
					stm.executeUpdate();
					stm.clearParameters();
					con.commit(); // flush
				}
			}
			catch (Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("OfflineTradersTable[storeOffliner()]: Error while saving offline trader: " + pc.getObjectId() + " " + e, e);
			}
			
			stm.close();
			stm_items.close();
			
			String text = "Offline trader " + pc.getName() + " stored.";
			Log.add(text, "Offline_trader");
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("OfflineTradersTable[storeOffliner()]: Error while saving offline traders: " + e, e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
}