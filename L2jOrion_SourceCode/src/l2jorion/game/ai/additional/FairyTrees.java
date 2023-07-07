package l2jorion.game.ai.additional;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;

public class FairyTrees extends Quest implements Runnable
{
	private static final int[] trees =
	{
		27185,
		27186,
		27187,
		27188
	};
	
	public FairyTrees(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		
		for (final int mob : trees)
		{
			addEventId(mob, QuestEventType.ON_KILL);
			addEventId(mob, QuestEventType.ON_ATTACK);
		}
	}
	
	@Override
	public String onAttack(final L2NpcInstance npc, final L2PcInstance attacker, int damage, final boolean isPet)
	{
		final int npcId = npc.getNpcId();
		final L2Character originalAttacker = isPet ? attacker.getPet() : attacker;
		switch (npcId)
		{
			case 27185:
			case 27186:
			case 27187:
			case 27188:
				for (L2Character obj : npc.getKnownList().getKnownTypeInRadius(L2MonsterInstance.class, npc.getFactionRange()))
				{
					if (!(npc.getFactionId().contains(((L2NpcInstance) obj).getFactionId())))
					{
						continue;
					}
					
					((L2Attackable) obj).addDamageHate(originalAttacker, 0, 999);
					obj.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalAttacker);
				}
				break;
			default:
				break;
		}
		
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(final L2NpcInstance npc, final L2PcInstance killer, final boolean isPet)
	{
		final int npcId = npc.getNpcId();
		
		if (!isPet)
		{
			switch (npcId)
			{
				case 27185:
				case 27186:
				case 27187:
				case 27188:
					for (int i = 0; i < 20; i++)
					{
						final L2Attackable newNpc = (L2Attackable) addSpawn(27189, npc.getX(), npc.getY(), npc.getZ(), 0, false, 30000);
						newNpc.setRunning();
						newNpc.addDamageHate(killer, 0, 999);
						newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, killer);
					}
					break;
			}
		}
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public void run()
	{
	}
}
