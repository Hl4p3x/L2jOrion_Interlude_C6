package l2jorion.game.ai.additional.group;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.actor.instance.L2MinionInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.util.random.Rnd;

public final class TimakOrcTroopLeader extends Quest implements Runnable
{
	private static int _attacked;
	private static Set<Integer> _tracking = new HashSet<>();
	
	private static final Map<Integer, Integer[]> MINIONS = new ConcurrentHashMap<>();
	
	private static final int monster = 20767;
	
	private static final String[] MONSTER_MSG =
	{
		"Brothers, destroy the enemy!",
		"Come out! Evil fellows!",
		"Servants! Come out!"
	};
	
	static
	{
		MINIONS.put(20767, new Integer[]
		{
			20768,
			20769,
			20770
		}); // Timak Orc Troop
	}
	
	public TimakOrcTroopLeader(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addAttackId(monster);
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
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
			Integer[] minions = MINIONS.get(npc.getNpcId());
			for (final Integer minion : minions)
			{
				final L2MinionInstance newNpc = (L2MinionInstance) addSpawn(minion, npc.getX() + Rnd.get(-150, 150), npc.getY() + Rnd.get(-150, 150), npc.getZ(), 0, false, 0);
				newNpc.setLeader((L2MonsterInstance) npc);
				newNpc.getLeader().notifyMinionSpawned(newNpc);
				newNpc.setIsRaidMinion(newNpc.getLeader().isRaid());
				newNpc.setRunning();
				newNpc.addDamageHate(attacker, 0, 999);
				newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
			}
		}
		
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public void run()
	{
	}
}