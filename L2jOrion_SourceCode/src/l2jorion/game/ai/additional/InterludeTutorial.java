package l2jorion.game.ai.additional;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;

public class InterludeTutorial extends Quest implements Runnable
{
	private static final int SOULSHOT_NOVICE = 5789;
	private static final int SPIRITSHOT_NOVICE = 5790;
	private static final int TOKEN = 8542;
	
	private static final int NEWBIE_GUIDE_1 = 30600;
	private static final int NEWBIE_GUIDE_2 = 30601;
	private static final int NEWBIE_GUIDE_3 = 30602;
	private static final int NEWBIE_GUIDE_4 = 30598;
	private static final int NEWBIE_GUIDE_5 = 30599;
	
	private static final String[] Interlude_Tutorial =
	{
		"999_C3Tutorial"
	};
	
	public InterludeTutorial(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addStartNpc(NEWBIE_GUIDE_1, NEWBIE_GUIDE_2, NEWBIE_GUIDE_3, NEWBIE_GUIDE_4, NEWBIE_GUIDE_5);
		addFirstTalkId(NEWBIE_GUIDE_1, NEWBIE_GUIDE_2, NEWBIE_GUIDE_3, NEWBIE_GUIDE_4, NEWBIE_GUIDE_5);
		addTalkId(NEWBIE_GUIDE_1, NEWBIE_GUIDE_2, NEWBIE_GUIDE_3, NEWBIE_GUIDE_4, NEWBIE_GUIDE_5);
	}
	
	@Override
	public String onFirstTalk(L2NpcInstance npc, L2PcInstance player)
	{
		int npcId = npc.getNpcId();
		QuestState quest;
		for (String mission : Interlude_Tutorial)
		{
			quest = player.getQuestState(mission);
			if (quest != null)
			{
				int onlyone = quest.getInt("onlyone");
				
				if (npcId == NEWBIE_GUIDE_1 || npcId == NEWBIE_GUIDE_2 || npcId == NEWBIE_GUIDE_3 || npcId == NEWBIE_GUIDE_4 || npcId == NEWBIE_GUIDE_5)
				{
					if (onlyone != 2)
					{
						quest.set("onlyone", "2");
						
						if (!Config.RON_CUSTOM)
						{
							if (player.getClassId().isMage())
							{
								quest.giveItems(SPIRITSHOT_NOVICE, 100);
							}
							else
							{
								quest.giveItems(SOULSHOT_NOVICE, 200);
							}
							
							if (!Config.GGAMES_EU_CUSTOM)
							{
								quest.giveItems(TOKEN, 12);
							}
						}
					}
					
				}
			}
		}
		return null;
	}
	
	@Override
	public void run()
	{
	}
}
