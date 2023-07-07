package l2jorion.bots.ai.classes.third;

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

public class MysticMuseAI extends CombatAI
{
	public MysticMuseAI(FakePlayer character)
	{
		super(character);
	}
	
	@Override
	public void thinkAndAct()
	{
		super.thinkAndAct();
		setBusyThinking(true);
		applyDefaultBuffs();
		handleShots();
		tryTargetRandomCreatureByTypeInRadius(_fakePlayer.getTargetClass(), _fakePlayer.getTargetRange());
		tryAttackingUsingMageOffensiveSkill();
		setBusyThinking(false);
	}
	
	@Override
	protected ShotType getShotType()
	{
		return ShotType.BLESSED_SPIRITSHOT;
	}
	
	@Override
	protected List<OffensiveSpell> getOffensiveSpells()
	{
		List<OffensiveSpell> _offensiveSpells = new ArrayList<>();
		_offensiveSpells.add(new OffensiveSpell(1235, 4));
		_offensiveSpells.add(new OffensiveSpell(1340, 3));
		_offensiveSpells.add(new OffensiveSpell(1342, 2));
		_offensiveSpells.add(new OffensiveSpell(1265, 1));
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