package l2jorion.game.ai.additional.group;

import javolution.util.FastSet;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.util.random.Rnd;

public final class PlainsOfDion extends Quest implements Runnable
{
	private static int _attacked;
	private static FastSet<Integer> _tracking = new FastSet<>();
	
	private static final int chance = 5; // 5%
	private static final int range = 700;
	
	private static final int MONSTERS[] =
	{
		21104, // Delu Lizardman Supplier
		21105, // Delu Lizardman Special Agent
		21107, // Delu Lizardman Commander
	};
	
	private static final String[] MONSTERS_MSG =
	{
		"$s1! How dare you interrupt our fight! Hey guys, help!",
		"$s1! Hey! We're having a duel here!",
		"The duel is over! Attack!",
		"Foul! Kill the coward!",
		"How dare you interrupt a sacred duel! You must be taught a lesson!"
	};
	
	private static final String[] MONSTERS_ASSIST_MSG =
	{
		"Die, you coward!",
		"Kill the coward!",
		"What are you looking at?"
	};
	
	public PlainsOfDion(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addAttackId(MONSTERS);
		addSpawnId(MONSTERS);
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (Rnd.get(100) <= chance)
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
				npc.broadcastNpcSay(Rnd.get(MONSTERS_MSG).replace("$s1", attacker.getName()));
				
				for (L2MonsterInstance obj : npc.getKnownList().getKnownTypeInRadius(L2MonsterInstance.class, range))
				{
					if (!obj.isAttackingNow() && !obj.isDead() && obj.isMonster())
					{
						obj.setRunning();
						obj.addDamageHate(attacker, 0, 999);
						obj.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
						obj.broadcastNpcSay(Rnd.get(MONSTERS_ASSIST_MSG));
					}
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public void run()
	{
	}
}