package l2jorion.game.ai.additional.group;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;

public class AbandonedCamp extends Quest implements Runnable
{
	private static int _attacked;
	private static Set<Integer> _tracking = new HashSet<>();
	
	private static final int commander = 20076;
	
	private static final int chance = 10;
	
	private static final int[] mobs =
	{
		20436,
		20439,
		20438,
		20058,
		20061,
		20063,
		20066
	};
	
	private static final String[] MONSTERS_MSG =
	{
		"Retreat!",
		"It's stronger than expected!",
		"Mass fleeing!",
		"You can't get anything by killing me.",
		"Tactical retreat!",
		"I'll kill you next time!",
		"Invader!",
		"Am I the neighborhood drum for beating!"
	};
	
	public AbandonedCamp(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		addAttackId(mobs);
		addKillId(mobs);
	}
	
	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		if (event.equals("SeekForHelp") && (npc != null))
		{
			if ((Util.checkIfInRange(npc.getFactionRange(), npc, npc.getFactionHelp(), false)))
			{
				npc.setIsAttackDisabled(false);
				((L2Attackable) npc.getFactionHelp()).addDamageHate(npc.getFactionEnemy(), 1, 0);
			}
		}
		return null;
	}
	
	@Override
	public String onAttack(final L2NpcInstance npc, final L2PcInstance attacker, int damage, final boolean isPet)
	{
		if (Rnd.get(100) <= chance)
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
					
					List<L2Attackable> targets = npc.getKnownList().getKnownTypeInRadius(L2MonsterInstance.class, 2000).stream().filter(x -> !x.isDead() && (x.isMonster()) && x.getNpcId() == commander).collect(Collectors.toList());
					if (!targets.isEmpty())
					{
						L2Attackable target = targets.stream().min((a1, a2) -> (int) Util.calculateDistance(a1, a2, false)).get();
						
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(target.getX(), target.getY(), target.getZ()));
						npc.setIsAttackDisabled(true);
						npc.setFactionHelp(target);
						npc.setFactionEnemy(attacker);
						npc.broadcastNpcSay(Rnd.get(MONSTERS_MSG));
						startQuestTimer("SeekForHelp", 2000, npc, null, true);
					}
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		if (Util.contains(mobs, npc.getNpcId()))
		{
			cancelQuestTimer("SeekForHelp", npc, null);
		}
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public void run()
	{
	}
}
