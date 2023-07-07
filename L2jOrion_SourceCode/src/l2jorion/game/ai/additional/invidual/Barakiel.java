package l2jorion.game.ai.additional.invidual;

import l2jorion.game.model.quest.Quest;

public class Barakiel extends Quest implements Runnable
{
	// Barakiel NpcID
	private static final int BARAKIEL = 25325;
	
	public Barakiel(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		addEventId(BARAKIEL, Quest.QuestEventType.ON_ATTACK);
	}
	
	@Override
	public void run()
	{
	}
}
