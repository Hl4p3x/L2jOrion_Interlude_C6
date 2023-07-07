package l2jorion.game.handler.item;

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.PledgeSkillList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ClanSkillsCustomItem implements IItemHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(ClanSkillsCustomItem.class);
	
	@Override
	public void useItem(L2PlayableInstance activeChar, L2ItemInstance item)
	{
		if (!(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) activeChar;
		
		if (player.getClan() == null)
		{
			player.sendMessage("You don't have a clan.");
			return;
		}
		
		// L2Clan clan = player.getClan();
		
		// if (clan.getLevel() < 5)
		// {
		// player.sendMessage("Your clan level must be higher than 4.");
		// return;
		// }
		
		player.destroyItem("Consume", item.getObjectId(), 1, null, true);
		addskill(player, 370, 3);
		addskill(player, 371, 3);
		addskill(player, 372, 3);
		addskill(player, 373, 3);
		addskill(player, 374, 3);
		addskill(player, 375, 3);
		addskill(player, 376, 3);
		addskill(player, 377, 3);
		addskill(player, 378, 3);
		addskill(player, 379, 3);
		addskill(player, 380, 3);
		addskill(player, 381, 3);
		addskill(player, 382, 3);
		addskill(player, 383, 3);
		addskill(player, 384, 3);
		addskill(player, 385, 3);
		addskill(player, 386, 3);
		addskill(player, 387, 3);
		addskill(player, 388, 3);
		addskill(player, 389, 3);
		addskill(player, 390, 3);
		addskill(player, 391, 1);
		player.sendMessage("All clan skills have been added.");
	}
	
	private void addskill(final L2PcInstance activeChar, final int id, final int level)
	{
		final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
		if (skill != null)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED);
			sm.addSkillName(id);
			activeChar.sendPacket(sm);
			activeChar.getClan().broadcastToOnlineMembers(sm);
			activeChar.getClan().addNewSkill(skill);
			activeChar.getClan().broadcastToOnlineMembers(new PledgeSkillList(activeChar.getClan()));
			
			for (final L2PcInstance member : activeChar.getClan().getOnlineMembers(""))
			{
				member.sendSkillList();
			}
			return;
		}
		
		activeChar.sendMessage("Error: there is no such skill.");
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private static final int ITEM_IDS[] =
	{
		10012
	};
}
