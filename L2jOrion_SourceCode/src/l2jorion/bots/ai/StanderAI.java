package l2jorion.bots.ai;

import l2jorion.bots.FakePlayer;

public class StanderAI extends FakePlayerAI
{
	public StanderAI(FakePlayer character)
	{
		super(character);
	}
	
	@Override
	public void setup()
	{
		super.setup();
	}
	
	@Override
	public void thinkAndAct()
	{
		if (isBusyThinking())
		{
			return;
		}
		setBusyThinking(true);
	}
	
	@Override
	protected int[][] getBuffs()
	{
		return new int[0][0];
	}
}
