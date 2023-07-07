package l2jorion.game.ai.additional.invidual;

import l2jorion.game.model.quest.Quest;

public class Golkonda extends Quest implements Runnable
{
	private static final int GOLKONDA = 25126;
	
	public Golkonda(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		addEventId(GOLKONDA, Quest.QuestEventType.ON_ATTACK);
	}
	
	@Override
	public void run()
	{
	}
}
