package l2jorion.game.ai.additional.invidual;

import l2jorion.game.model.quest.Quest;

public class Hallate extends Quest implements Runnable
{
	private static final int HALLATE = 25220;
	
	public Hallate(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		addEventId(HALLATE, Quest.QuestEventType.ON_ATTACK);
	}
	
	@Override
	public void run()
	{
	}
}
