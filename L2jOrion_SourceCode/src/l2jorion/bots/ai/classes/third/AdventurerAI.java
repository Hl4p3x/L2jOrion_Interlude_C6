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

public class AdventurerAI extends CombatAI
{
	public AdventurerAI(FakePlayer character)
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
		tryAttackingUsingFighterOffensiveSkill();
		setBusyThinking(false);
	}
	
	@Override
	protected ShotType getShotType()
	{
		return ShotType.SOULSHOT;
	}
	
	@Override
	protected List<OffensiveSpell> getOffensiveSpells()
	{
		List<OffensiveSpell> _offensiveSpells = new ArrayList<>();
		_offensiveSpells.add(new OffensiveSpell(263, 4));
		_offensiveSpells.add(new OffensiveSpell(12, 1));
		_offensiveSpells.add(new OffensiveSpell(11, 2));
		_offensiveSpells.add(new OffensiveSpell(4, 3));
		_offensiveSpells.add(new OffensiveSpell(409, 5));
		_offensiveSpells.add(new OffensiveSpell(344, 6));
		_offensiveSpells.add(new OffensiveSpell(358, 7));
		return _offensiveSpells;
	}
	
	@Override
	protected int[][] getBuffs()
	{
		return FakeHelpers.getFighterBuffs();
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