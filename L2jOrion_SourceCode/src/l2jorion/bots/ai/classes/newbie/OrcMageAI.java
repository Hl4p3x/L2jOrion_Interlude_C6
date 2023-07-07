package l2jorion.bots.ai.classes.newbie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2jorion.bots.FakePlayer;
import l2jorion.bots.ai.CombatAI;
import l2jorion.bots.helpers.FakeHelpers;
import l2jorion.bots.model.HealingSpell;
import l2jorion.bots.model.OffensiveSpell;
import l2jorion.bots.model.SupportSpell;
import l2jorion.game.enums.ShotType;

public class OrcMageAI extends CombatAI
{
	public OrcMageAI(FakePlayer character)
	{
		super(character);
	}
	
	@Override
	public void thinkAndAct()
	{
		super.thinkAndAct();
		setBusyThinking(true);
		
		if (handleLevels())
		{
			setBusyThinking(false);
			return;
		}
		
		applyDefaultBuffs();
		handleShots();
		tryTargetRandomCreatureByTypeInRadius(_fakePlayer.getTargetClass(), _fakePlayer.getTargetRange());
		
		// if (_fakePlayer.getLevel() < 7)
		{
			tryAttackingUsingFighterOffensiveSkill();
		}
		// else
		// {
		// tryAttackingUsingMageOffensiveSkill();
		// }
		setBusyThinking(false);
	}
	
	@Override
	protected ShotType getShotType()
	{
		if (_fakePlayer.getLevel() < 7)
		{
			return ShotType.SOULSHOT;
		}
		return ShotType.BLESSED_SPIRITSHOT;
	}
	
	@Override
	protected List<OffensiveSpell> getOffensiveSpells()
	{
		List<OffensiveSpell> _offensiveSpells = new ArrayList<>();
		
		if (_fakePlayer.getLevel() < 7)
		{
			return Collections.emptyList();
		}
		
		if (_fakePlayer.getLevel() >= 7)
		{
			_offensiveSpells.add(new OffensiveSpell(1100, 1));
		}
		
		if (_fakePlayer.getLevel() >= 8)
		{
			_offensiveSpells.add(new OffensiveSpell(1090, 1));
		}
		
		return _offensiveSpells;
	}
	
	@Override
	protected int[][] getBuffs()
	{
		return FakeHelpers.getMageBuffs();
	}
	
	@Override
	protected List<HealingSpell> getHealingSpells()
	{
		return Collections.emptyList();
	}
	
	@Override
	protected List<SupportSpell> getSelfSupportSpells()
	{
		return Collections.emptyList();
	}
}