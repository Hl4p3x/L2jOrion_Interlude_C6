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

public class AccessLevelCustomItem implements IItemHandler
{

	public AccessLevelCustomItem()
	{
	//null
	}

	protected static final Logger LOG = LoggerFactory.getLogger(AccessLevelCustomItem.class.getName());
	
	String INSERT_DATA = "UPDATE characters SET accesslevel=? WHERE obj_id=?";

	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if(Config.ACCESS_CUSTOM_ITEMS)
		{
			if(!(playable instanceof L2PcInstance))
				return;

			L2PcInstance activeChar = (L2PcInstance) playable;

			if(activeChar.isInOlympiadMode())
			{
				activeChar.sendMessage("This item cannot be used on Olympiad Games.");
			}
			else
			{
				//updateDatabase(activeChar);
				activeChar.setAccessLevel(10);
				activeChar.sendMessage("Congratulation! You've got access!");
				activeChar.sendPacket(new ExShowScreenMessage("Congratulation! You've got access!", 4000, 0x07, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				activeChar.sendPacket(playSound);
				activeChar.sendPacket(new UserInfo(activeChar));
				activeChar.broadcastUserInfo();
				playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
			}
			activeChar = null;
		}
	}

	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	@SuppressWarnings("unused")
	private void updateDatabase(L2PcInstance player)
	{
		Connection con = null;
		try
		{
			if (player == null)
				return;

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
			stmt.setInt(1, 10);
			stmt.setInt(2, player.getObjectId());
			stmt.execute();
			stmt.close();
			stmt = null;
		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.error("Error: could not update database: ", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}

	private static final int ITEM_IDS[] =
	{
		Config.ACCESS_CUSTOM_ITEM_ID
	};

}
