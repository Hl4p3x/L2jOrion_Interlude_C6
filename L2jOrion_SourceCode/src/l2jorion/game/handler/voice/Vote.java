package l2jorion.game.handler.voice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l2jorion.Config;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public class Vote implements IVoicedCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(Vote.class);
	
	private static final String CREATE_VOTEDATA = "INSERT INTO accounts_voted (vote_ip, last_hop_vote, last_top_vote, last_net_vote, last_bra_vote, last_l2topgr, last_l2toponline) values(?,?,?,?,?,?,?)";
	private static final String RESTORE_VOTEDATA = "SELECT last_hop_vote, last_top_vote, last_net_vote, last_bra_vote, last_l2topgr, last_l2toponline FROM accounts_voted WHERE vote_ip=?";
	
	private static String[] _voicedCommands =
	{
		"votereward"
	};
	
	@Override
	public boolean useVoicedCommand(final String command, final L2PcInstance player, final String target)
	{
		if (player == null)
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("votereward"))
		{
			showHtm(player);
		}
		return true;
	}
	
	public static void showHtm(L2PcInstance player)
	{
		restoreVotedData(player, player.getClient().getConnection().getInetAddress().getHostAddress());
		
		final L2Item item = ItemTable.getInstance().getTemplate(Config.VOTE_REWARD_ITEM_ID);
		String itemIcon = L2Item.getItemIcon(Config.VOTE_REWARD_ITEM_ID);
		
		NpcHtmlMessage htm = new NpcHtmlMessage(1);
		htm.setFile("data/html/mods/vote.htm");
		
		htm.replace("%name%", player.getName());
		htm.replace("%color%", player.StringToHexForVote(Integer.toHexString(player.getAppearance().getNameColor()).toUpperCase()));
		
		htm.replace("%item%", item.getName());
		htm.replace("%icon%", "icon." + itemIcon);
		htm.replace("%min%", Config.VOTE_REWARD_ITEM_COUNT_MIN);
		htm.replace("%max%", Config.VOTE_REWARD_ITEM_COUNT_MAX);
		htm.replace("%chance%", Config.VOTE_REWARD_CHANCE + "%");
		
		if (player.eligibleToVoteHop())
		{
			htm.replace("%hop%", "<a action=\"bypass -h vote hopzone\">Get Reward</a>");
		}
		else
		{
			htm.replace("%hop%", String.format("<font color=00ff00>Voted</font> (" + player.getVoteCountdownHop() + ")"));
		}
		
		if (player.eligibleToVoteTop())
		{
			htm.replace("%top%", "<a action=\"bypass -h vote topzone\">Get Reward</a>");
		}
		else
		{
			htm.replace("%top%", String.format("<font color=00ff00>Voted</font> (" + player.getVoteCountdownTop() + ")"));
		}
		
		if (player.eligibleToVoteNet())
		{
			htm.replace("%net%", "<a action=\"bypass -h vote network\">Get Reward</a>");
			
		}
		else
		{
			htm.replace("%net%", String.format("<font color=00ff00>Voted</font> (" + player.getVoteCountdownNet() + ")"));
		}
		
		if (player.eligibleToVoteBra())
		{
			htm.replace("%bra%", "<a action=\"bypass -h vote brasil\">Get Reward</a>");
			
		}
		else
		{
			htm.replace("%bra%", String.format("<font color=00ff00>Voted</font> (" + player.getVoteCountdownBra() + ")"));
		}
		
		if (player.eligibleToVoteL2TopGr())
		{
			htm.replace("%L2TopGr%", "<a action=\"bypass -h vote L2TopGr\">Get Reward</a>");
			
		}
		else
		{
			htm.replace("%L2TopGr%", String.format("<font color=00ff00>Voted</font> (" + player.getVoteCountdownL2TopGr() + ")"));
		}
		
		if (player.eligibleToVoteL2TopOnline())
		{
			htm.replace("%L2TopOnline%", "<a action=\"bypass -h vote L2TopOnline\">Get Reward</a>");
			
		}
		else
		{
			htm.replace("%L2TopOnline%", String.format("<font color=00ff00>Voted</font> (" + player.getVoteCountdownL2TopOnline() + ")"));
		}
		
		player.sendPacket(htm);
		return;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
	private static void createVotedDB(L2PcInstance player)
	{
		String ip = player.getClient().getConnection().getInetAddress().getHostAddress();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(CREATE_VOTEDATA);
			statement.setString(1, ip);
			statement.setLong(2, 0);
			statement.setLong(3, 0);
			statement.setLong(4, 0);
			statement.setLong(5, 0);
			statement.setLong(6, 0);
			statement.setLong(7, 0);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VoteCommand: Could not insert ip data: " + e);
			
			return;
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public static void restoreVotedData(L2PcInstance player, String ip)
	{
		boolean sucess = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_VOTEDATA);
			statement.setString(1, ip);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				sucess = true;
				
				player.setLastHopVote(rset.getLong("last_hop_vote"));
				player.setLastTopVote(rset.getLong("last_top_vote"));
				player.setLastNetVote(rset.getLong("last_net_vote"));
				player.setLastBraVote(rset.getLong("last_bra_vote"));
				player.setLastL2TopGr(rset.getLong("last_l2topgr"));
				player.setLastL2TopOnline(rset.getLong("last_l2toponline"));
				
				if (rset.getLong("last_hop_vote") <= System.currentTimeMillis())
				{
					rset.getLong("last_hop_vote");
				}
				else
				{
					player.setLastHopVote(rset.getLong("last_hop_vote"));
				}
				
				if (rset.getLong("last_top_vote") <= System.currentTimeMillis())
				{
					rset.getLong("last_top_vote");
				}
				else
				{
					player.setLastTopVote(rset.getLong("last_top_vote"));
				}
				
				if (rset.getLong("last_net_vote") <= System.currentTimeMillis())
				{
					rset.getLong("last_net_vote");
				}
				else
				{
					player.setLastNetVote(rset.getLong("last_net_vote"));
				}
				
				if (rset.getLong("last_bra_vote") <= System.currentTimeMillis())
				{
					rset.getLong("last_bra_vote");
				}
				else
				{
					player.setLastBraVote(rset.getLong("last_bra_vote"));
				}
				
				if (rset.getLong("last_l2topgr") <= System.currentTimeMillis())
				{
					rset.getLong("last_l2topgr");
				}
				else
				{
					player.setLastL2TopGr(rset.getLong("last_l2topgr"));
				}
				
				if (rset.getLong("last_l2toponline") <= System.currentTimeMillis())
				{
					rset.getLong("last_l2toponline");
				}
				else
				{
					player.setLastL2TopOnline(rset.getLong("last_l2toponline"));
				}
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("VoteCommand: Could not restore voted data for:" + ip + "." + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		if (sucess == false)
		{
			createVotedDB(player);
		}
	}
	
	public static void giveReward(L2PcInstance player)
	{
		player.sendMessage("Thank you! You've got reward for the vote.");
		player.sendPacket(new ExShowScreenMessage("Thank you! You've got reward for the vote.", 4000, 0x02, false));
		player.sendPacket(new PlaySound("ItemSound.quest_fanfare_1"));
		// player.broadcastUserInfo();
		
		int itemcount = Rnd.get(Config.VOTE_REWARD_ITEM_COUNT_MIN, Config.VOTE_REWARD_ITEM_COUNT_MAX);
		L2ItemInstance newitem = player.getInventory().addItem("VoteItem", Config.VOTE_REWARD_ITEM_ID, itemcount, player, null);
		InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(newitem);
		player.sendPacket(playerIU);
		if (itemcount > 1)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(Config.VOTE_REWARD_ITEM_ID);
			sm.addNumber(itemcount);
			player.sendPacket(sm);
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
			sm.addItemName(Config.VOTE_REWARD_ITEM_ID);
			player.sendPacket(sm);
		}
		
		showHtm(player);
		return;
	}
}