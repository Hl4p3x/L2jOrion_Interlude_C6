package l2jorion.game.ai.additional;

import l2jorion.game.model.quest.Quest;

/*
 * @author m095 (L2EmuRT)
 */

public class Golkonda extends Quest implements Runnable
{
	// Golkonda NpcID
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
