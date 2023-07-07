package l2jorion.bots.ai.addon;

import java.util.List;
import java.util.stream.Collectors;

import l2jorion.bots.FakePlayer;
import l2jorion.bots.ai.CombatAI;
import l2jorion.bots.model.HealingSpell;
import l2jorion.game.model.L2Character;

public interface IHealer
{
	default void tryTargetingLowestHpTargetInRadius(FakePlayer player, Class<? extends L2Character> creatureClass, int radius)
	{
		if (player.getTarget() == null)
		{
			List<L2Character> targets = player.getKnownTypeInRadius(creatureClass, radius).stream().filter(x -> !x.isDead()).collect(Collectors.toList());
			
			if (!player.isDead())
			{
				targets.add(player);
			}
			
			List<L2Character> sortedTargets = targets.stream().sorted((x1, x2) -> Double.compare(x1.getCurrentHp(), x2.getCurrentHp())).collect(Collectors.toList());
			
			if (!sortedTargets.isEmpty())
			{
				L2Character target = sortedTargets.get(0);
				player.setTarget(target);
			}
		}
		else
		{
			if (((L2Character) player.getTarget()).isDead())
			{
				player.setTarget(null);
			}
		}
	}
	
	default void tryHealingTarget(FakePlayer player)
	{
		if (player.getTarget() != null && player.getTarget() instanceof L2Character)
		{
			L2Character target = (L2Character) player.getTarget();
			if (player.getFakeAi() instanceof CombatAI)
			{
				HealingSpell skill = ((CombatAI) player.getFakeAi()).getRandomAvaiableHealingSpellForTarget();
				if (skill != null)
				{
					switch (skill.getCondition())
					{
						case LESSHPPERCENT:
							double currentHpPercentage = Math.round(100.0 / target.getMaxHp() * target.getCurrentHp());
							if (currentHpPercentage <= skill.getConditionValue())
							{
								player.getFakeAi().castSpell(player.getSkill(skill.getSkillId()));
							}
							break;
						default:
							break;
					}
				}
			}
		}
	}
}
