package l2jorion.game.ai.additional.invidual;

import l2jorion.game.model.quest.Quest;

public class Kernon extends Quest implements Runnable
{
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
