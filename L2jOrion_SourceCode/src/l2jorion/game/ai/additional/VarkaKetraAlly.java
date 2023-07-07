package l2jorion.game.ai.additional;

import l2jorion.game.model.PcInventory;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.QuestList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;

public class VarkaKetraAlly extends Quest implements Runnable
{
	private static final int Ketra_Alliance_One = 7211;
	private static final int Ketra_Alliance_Two = 7212;
	private static final int Ketra_Alliance_Three = 7213;
	private static final int Ketra_Alliance_Four = 7214;
	private static final int Ketra_Alliance_Five = 7215;
	
	private static final String[] ketraMissions =
	{
		"605_AllianceWithKetraOrcs"
	};
	
	private static final int Varka_Alliance_One = 7221;
	private static final int Varka_Alliance_Two = 7222;
	private static final int Varka_Alliance_Three = 7223;
	private static final int Varka_Alliance_Four = 7224;
	private static final int Varka_Alliance_Five = 7225;
	
	private static final String[] varkaMissions =
	{
		"611_AllianceWithVarkaSilenos"
	};
	
	public VarkaKetraAlly(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		final int[] mobs =
		{
			// ketra mobs
			21324,
			21325,
			21327,
			21328,
			21329,
			21331,
			21332,
			21334,
			21335,
			21336,
			21338,
			21339,
			21340,
			21342,
			21343,
			21344,
			21345,
			21346,
			21347,
			21348,
			21349,
			// varka mobs
			21350,
			21351,
			21353,
			21354,
			21355,
			21357,
			21358,
			21360,
			21361,
			21362,
			21364,
			21365,
			21366,
			21368,
			21369,
			21370,
			21371,
			21372,
			21373,
			21374,
			21375
		};
		
		for (final int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
		}
	}
	
	@Override
	public String onKill(final L2NpcInstance npc, final L2PcInstance killer, final boolean isPet)
	{
		if (killer.getParty() != null)
		{
			for (final L2PcInstance member : killer.getParty().getPartyMembers())
			{
				if (Util.checkIfInRange(6000, killer, member, true))
				{
					decreaseAlly(npc, member);
				}
			}
		}
		else
		{
			decreaseAlly(npc, killer);
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	private void decreaseAlly(final L2NpcInstance npc, final L2PcInstance player)
	{
		if (player.getAllianceWithVarkaKetra() != 0)
		{
			if (player.isAlliedWithKetra() && npc.getFactionId() == "ketra")
			{
				final L2ItemInstance varkasBadgeSoldier = player.getInventory().getItemByItemId(7216);
				final L2ItemInstance varkasBadgeOfficer = player.getInventory().getItemByItemId(7217);
				final L2ItemInstance varkasBadgeCaptain = player.getInventory().getItemByItemId(7218);
				
				final int varkasBadgeSoldierCount = varkasBadgeSoldier == null ? 0 : varkasBadgeSoldier.getCount();
				final int varkasBadgeOfficerCount = varkasBadgeOfficer == null ? 0 : varkasBadgeOfficer.getCount();
				final int varkasBadgeCaptainCount = varkasBadgeCaptain == null ? 0 : varkasBadgeCaptain.getCount();
				
				if (varkasBadgeSoldierCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeSoldier", 7216, varkasBadgeSoldierCount, player, player.getTarget());
				}
				if (varkasBadgeOfficerCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeOfficer", 7217, varkasBadgeOfficerCount, player, player.getTarget());
				}
				if (varkasBadgeCaptainCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeCaptain", 7218, varkasBadgeCaptainCount, player, player.getTarget());
				}
				
				final PcInventory inventory = player.getInventory();
				L2ItemInstance item;
				
				player.setAllianceWithVarkaKetra(player.getAllianceWithVarkaKetra() - 1);
				
				// Drop down by 1 level of that alliance.
				item = inventory.getItemByItemId(Ketra_Alliance_One);
				if (item != null)
				{
					player.destroyItemByItemId("Quest", Ketra_Alliance_One, item.getCount(), player, true);
				}
				else
				{
					item = inventory.getItemByItemId(Ketra_Alliance_Two);
					if (item != null)
					{
						player.destroyItemByItemId("Quest", Ketra_Alliance_Two, item.getCount(), player, true);
						player.addItem("Quest", Ketra_Alliance_One, 1, player.getTarget(), true);
					}
					else
					{
						item = inventory.getItemByItemId(Ketra_Alliance_Three);
						if (item != null)
						{
							player.destroyItemByItemId("Quest", Ketra_Alliance_Three, item.getCount(), player, true);
							player.addItem("Quest", Ketra_Alliance_Two, 1, player.getTarget(), true);
						}
						else
						{
							item = inventory.getItemByItemId(Ketra_Alliance_Four);
							if (item != null)
							{
								player.destroyItemByItemId("Quest", Ketra_Alliance_Four, item.getCount(), player, true);
								player.addItem("Quest", Ketra_Alliance_Three, 1, player.getTarget(), true);
							}
							else
							{
								item = inventory.getItemByItemId(Ketra_Alliance_Five);
								if (item != null)
								{
									player.destroyItemByItemId("Quest", Ketra_Alliance_Five, item.getCount(), player, true);
									player.addItem("Quest", Ketra_Alliance_Four, 1, player.getTarget(), true);
								}
							}
						}
					}
				}
				
				QuestState pst1;
				for (String mission : ketraMissions)
				{
					pst1 = player.getQuestState(mission);
					if (pst1 != null)
					{
						pst1.exitQuest(true);
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
						sm.addString("Quest aborted.");
						player.sendPacket(sm);
						final QuestList ql1 = new QuestList();
						player.sendPacket(ql1);
					}
				}
			}
			
			if (player.isAlliedWithVarka() && npc.getFactionId() == "varka")
			{
				final L2ItemInstance varkasBadgeSoldier = player.getInventory().getItemByItemId(7216);
				final L2ItemInstance varkasBadgeOfficer = player.getInventory().getItemByItemId(7217);
				final L2ItemInstance varkasBadgeCaptain = player.getInventory().getItemByItemId(7218);
				
				final int varkasBadgeSoldierCount = varkasBadgeSoldier == null ? 0 : varkasBadgeSoldier.getCount();
				final int varkasBadgeOfficerCount = varkasBadgeOfficer == null ? 0 : varkasBadgeOfficer.getCount();
				final int varkasBadgeCaptainCount = varkasBadgeCaptain == null ? 0 : varkasBadgeCaptain.getCount();
				
				if (varkasBadgeSoldierCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeSoldier", 7216, varkasBadgeSoldierCount, player, player.getTarget());
				}
				if (varkasBadgeOfficerCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeOfficer", 7217, varkasBadgeOfficerCount, player, player.getTarget());
				}
				if (varkasBadgeCaptainCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeCaptain", 7218, varkasBadgeCaptainCount, player, player.getTarget());
				}
				
				final PcInventory inventory = player.getInventory();
				L2ItemInstance item;
				
				player.setAllianceWithVarkaKetra(player.getAllianceWithVarkaKetra() + 1);
				
				// Drop down by 1 level of that alliance.
				item = inventory.getItemByItemId(Varka_Alliance_One);
				if (item != null)
				{
					player.destroyItemByItemId("Quest", Varka_Alliance_One, item.getCount(), player, true);
				}
				else
				{
					item = inventory.getItemByItemId(Varka_Alliance_Two);
					if (item != null)
					{
						player.destroyItemByItemId("Quest", Varka_Alliance_Two, item.getCount(), player, true);
						player.addItem("Quest", Varka_Alliance_One, 1, player.getTarget(), true);
					}
					else
					{
						item = inventory.getItemByItemId(Varka_Alliance_Three);
						if (item != null)
						{
							player.destroyItemByItemId("Quest", Varka_Alliance_Three, item.getCount(), player, true);
							player.addItem("Quest", Varka_Alliance_Two, 1, player.getTarget(), true);
						}
						else
						{
							item = inventory.getItemByItemId(Varka_Alliance_Four);
							if (item != null)
							{
								player.destroyItemByItemId("Quest", Varka_Alliance_Four, item.getCount(), player, true);
								player.addItem("Quest", Varka_Alliance_Three, 1, player.getTarget(), true);
							}
							else
							{
								item = inventory.getItemByItemId(Varka_Alliance_Five);
								if (item != null)
								{
									player.destroyItemByItemId("Quest", Varka_Alliance_Five, item.getCount(), player, true);
									player.addItem("Quest", Varka_Alliance_Four, 1, player.getTarget(), true);
								}
							}
						}
					}
				}
				
				QuestState pst;
				for (String mission : varkaMissions)
				{
					pst = player.getQuestState(mission);
					if (pst != null)
					{
						pst.exitQuest(true);
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
						sm.addString("Quest aborted.");
						player.sendPacket(sm);
						final QuestList ql = new QuestList();
						player.sendPacket(ql);
					}
				}
			}
		}
	}
	
	@Override
	public void run()
	{
	}
}
