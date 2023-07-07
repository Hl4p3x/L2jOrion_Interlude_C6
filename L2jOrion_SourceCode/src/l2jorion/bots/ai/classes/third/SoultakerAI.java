package l2jorion.bots.ai.classes.third;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2jorion.bots.FakePlayer;
import l2jorion.bots.ai.CombatAI;
import l2jorion.bots.ai.addon.IConsumableSpender;
import l2jorion.bots.helpers.FakeHelpers;
import l2jorion.bots.model.HealingSpell;
import l2jorion.bots.model.OffensiveSpell;
import l2jorion.bots.model.SupportSpell;
import l2jorion.game.enums.ShotType;

public class SoultakerAI extends CombatAI implements IConsumableSpender
{
	private final int boneId = 2508;
	
	public SoultakerAI(FakePlayer character)
	{
		super(character);
	}
	
	@Override
	public void thinkAndAct()
	{
		super.thinkAndAct();
		setBusyThinking(true);
		applyDefaultBuffs();
		handleConsumable(_fakePlayer, boneId);
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
		_offensiveSpells.add(new OffensiveSpell(1234, 1));
		_offensiveSpells.add(new OffensiveSpell(1148, 2));
		_offensiveSpells.add(new OffensiveSpell(1343, 3));
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