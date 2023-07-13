package l2jorion.game.ai.additional.group;

import java.util.HashSet;
import java.util.Set;

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.util.random.Rnd;

public final class TimakOrcOverlord extends Quest implements Runnable
{
	private static int _attacked;
	private static Set<Integer> _tracking = new HashSet<>();
	
	private static final int monster = 20588;
	
	private static final String[] MONSTER_MSG =
	{
		"Humph, wanted to win me to be also in tender!",
		"Here starts the true fight!",
		"Extreme strength! ! ! !",
		"Haven't thought to use this unique skill for this small thing!"
	};
	
	public TimakOrcOverlord(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addAttackId(monster);
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if ((npc.getMaxHp() / 2) > npc.getCurrentHp())
		{
			int npcObjId = npc.getObjectId();
			
			if (!_tracking.contains(npcObjId))
			{
				_tracking.add(npcObjId);
				_attacked = npcObjId;
			}
			
			if (_attacked == npcObjId)
			{
				_attacked = 0;
				npc.broadcastNpcSay(Rnd.get(MONSTER_MSG));
				L2Skill skill = SkillTable.getInstance().getInfo(4318, 1);
				npc.doCast(skill);
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public void run()
	{
	}
}