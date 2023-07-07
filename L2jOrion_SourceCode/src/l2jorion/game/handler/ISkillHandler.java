package l2jorion.game.handler;

import java.io.IOException;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;

public interface ISkillHandler
{
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets) throws IOException;
	
	public SkillType[] getSkillIds();
}
