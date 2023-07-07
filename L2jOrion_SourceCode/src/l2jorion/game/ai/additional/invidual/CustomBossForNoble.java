package l2jorion.game.ai.additional.invidual;

import l2jorion.Config;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;

public class CustomBossForNoble extends Quest implements Runnable
{
	private static final int BOSS = 96966;
	
	public CustomBossForNoble(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		addEventId(BOSS, Quest.QuestEventType.ON_KILL);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		L2Party party = killer.getParty();
		
		if (party != null)
		{
			for (L2PcInstance member : party.getPartyMembers())
			{
				if (member == null || member.isNoble())
				{
					continue;
				}
				
				if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE, npc, member, true))
				{
					continue;
				}
				
				member.setNoble(true);
				member.sendMessage("Congratulations! You've got The Nobless status");
				member.sendPacket(new ExShowScreenMessage("Congratulations! You've got The Nobless status", 3000, 0x02, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				member.sendPacket(playSound);
				member.broadcastUserInfo();
				
				L2ItemInstance newitem = member.getInventory().addItem("Tiara", 7694, 1, member, null);
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				member.sendPacket(playerIU);
				SystemMessage sm;
				sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				sm.addItemName(7694);
				member.sendPacket(sm);
			}
		}
		else
		{
			if (!killer.isNoble())
			{
				killer.setNoble(true);
				killer.sendMessage("Congratulations! You've got The Nobless status");
				killer.sendPacket(new ExShowScreenMessage("Congratulations! You've got The Nobless status", 3000, 0x02, false));
				PlaySound playSound = new PlaySound("ItemSound.quest_fanfare_1");
				killer.sendPacket(playSound);
				killer.broadcastUserInfo();
				
				L2ItemInstance newitem = killer.getInventory().addItem("Tiara", 7694, 1, killer, null);
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				killer.sendPacket(playerIU);
				SystemMessage sm;
				sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				sm.addItemName(7694);
				killer.sendPacket(sm);
			}
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public void run()
	{
	}
}
