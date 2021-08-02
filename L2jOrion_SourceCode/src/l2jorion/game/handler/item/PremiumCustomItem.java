// Hero Custom Item , Created By Stefoulis15
// Added From Stefoulis15 Into The Core.
// Visit www.MaxCheaters.com For Support 
// Source File Name:   HeroCustomItem.java
// Modded by programmos, sword dev

package l2jorion.game.handler.item;

import java.sql.Connection;
import java.sql.PreparedStatement;

import l2jorion.Config;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.UserInfo;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

public class PremiumCustomItem implements IItemHandler
{
	
	public PremiumCustomItem()
	{
		// null
	}
	
	protected static final Logger LOG = LoggerFactory.getLogger(PremiumCustomItem.class.getName());
	
	String INSERT_DATA = "REPLACE INTO account_premium (account_name, premium_service, enddate) VALUES (?,?,?)";
	
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (Config.PREMIUM_CUSTOM_ITEMS)
		{
			if (!(playable instanceof L2PcInstance))
			{
				return;
			}
			
			L2PcInstance activeChar = (L2PcInstance) playable;
			
			if (activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage("This item cannot be used on Olympiad Games.");
			}
			
			if (activeChar.getPremiumService() == 1)
			{
				activeChar.sendMessage("You're already The Premium account!");
			}
			else
			{
				activeChar.setPremiumService(1);
				updateDatabase(activeChar, Config.PREMIUM_CUSTOM_DAY * 24L * 60L * 60L * 1000L);
				activeChar.sendMessage("Congratulations! You're The Premium account now.");
				activeChar.sendPacket(new ExShowScreenMessage("Congratulations! You're The Premium account now.", 4000, 0x02, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				activeChar.sendPacket(playSound);
				if (Config.PREMIUM_NAME_COLOR_ENABLED && activeChar.getPremiumService() == 1)
				{
					activeChar.getAppearance().setTitleColor(Config.PREMIUM_TITLE_COLOR);
				}
				activeChar.sendPacket(new UserInfo(activeChar));
				activeChar.broadcastUserInfo();
				playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
			}
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private void updateDatabase(L2PcInstance player, long premiumTime)
	{
		Connection con = null;
		try
		{
			if (player == null)
			{
				return;
			}
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
			
			stmt.setString(1, player.getAccountName());
			stmt.setInt(2, 1);
			stmt.setLong(3, premiumTime == 0 ? 0 : System.currentTimeMillis() + premiumTime);
			stmt.execute();
			stmt.close();
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("Error: could not update database: ", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	private static final int ITEM_IDS[] =
	{
		Config.PREMIUM_CUSTOM_ITEM_ID
	};
	
}
