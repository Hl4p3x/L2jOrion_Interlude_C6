package l2jorion.game.ai.additional;

import l2jorion.game.model.quest.Quest;

/*
 * @author m095 (L2EmuRT)
 */

public class Kernon extends Quest implements Runnable
{
	// Kernon NpcID
	private static final int KERNON = 25054;
	
	public Kernon(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		addEventId(KERNON, Quest.QuestEventType.ON_ATTACK);
	}
	
	@Override
	public void run()
	{
	}
}
