package l2jorion.game.handler.item;

import java.util.ArrayList;

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.powerpack.PowerPackConfig;

public class CustomItemForMage implements IItemHandler
{
	@Override
	public void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("This item can't be used on The Olympiad!");
		}
		
		playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		
		ArrayList<L2Skill> skills_to_buff = new ArrayList<>();
		
		for (int skillId : PowerPackConfig.MAGE_SKILL_LIST.keySet())
		{
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.MAGE_SKILL_LIST.get(skillId));
			if (skill != null)
			{
				skills_to_buff.add(skill);
			}
		}
		
		for (L2Skill sk : skills_to_buff)
		{
			sk.getEffects(activeChar, activeChar, false, false, false);
		}
		
		activeChar.sendMessage("Congratulation! You've got your buffs.");
		activeChar.sendPacket(new ExShowScreenMessage("Congratulation! You've got your buffs.", 4000, 0x02, false));
		
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	private static final int ITEM_IDS[] =
	{
		// Config.NOOBLE_CUSTOM_ITEM_ID
		9991
	};
	
}
