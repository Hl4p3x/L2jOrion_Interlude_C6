package l2jorion.game.ai.additional.group;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.util.random.Rnd;

public class RetreatOnAttack extends Quest implements Runnable
{
	private static final int EPLY = 20432;
	
	public RetreatOnAttack(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		addAttackId(EPLY);
	}
	
	@Override
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		if (event.equals("Retreat") && (npc != null) && (player != null))
		{
			npc.setIsAfraid(false);
			((L2Attackable) npc).addDamageHate(player, 0, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
		}
		return null;
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		final int npcId = npc.getNpcId();
		if ((npcId == EPLY) || ((npc.getStatus().getCurrentHp() <= ((npc.getMaxHp() * 50) / 100)) && (Rnd.get(100) < 10)))
		{
			int posX = npc.getX();
			int posY = npc.getY();
			int posZ = npc.getZ();
			
			int signX = -500;
			int signY = -500;
			
			if (npc.getX() > attacker.getX())
			{
				signX = 500;
			}
			
			if (npc.getY() > attacker.getY())
			{
				signY = 500;
			}
			
			posX = posX + signX;
			posY = posY + signY;
			
			npc.setIsAfraid(true);
			npc.setRunning();
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, posZ));
			startQuestTimer("Retreat", 10000, npc, attacker);
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public void run()
	{
	}
}