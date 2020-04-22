
/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.ai.additional;

import javolution.util.FastMap;
import javolution.util.FastSet;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;


public class Frozen extends Quest implements Runnable
{
	private static int HasSpawned;
	private static FastSet<Integer> myTrackingSet = new FastSet<>();
	static final int[] mobs = {22094, 22088};
	private static final FastMap<Integer, Integer[]> MINIONS = new FastMap<>();
	
	static
	{
		MINIONS.put(22094, new Integer[] {22093,22093,22093,22093,22093});
		MINIONS.put(22088, new Integer[] {22087,22087,22087,22087,22087});
	}
	
	public Frozen(int questId, String name, String descr)
	{
		super(questId, name, descr);
		registerMobs(mobs, QuestEventType.ON_SKILL_USE);
	}
	
	@Override
	public String onSkillUse(L2NpcInstance npc, L2PcInstance caster, L2Skill skill)
	{
		int npcId = npc.getNpcId();
		int npcObjId = npc.getObjectId();
		
		if (Util.contains(mobs,npcId))
		{
			if (skill.isMagic())
			{
				return null;
			}

			if (!myTrackingSet.contains(npcObjId))
			{
				myTrackingSet.add(npcObjId);
				HasSpawned = npcObjId;
			}
			
			if (HasSpawned == npcObjId)
			{
				HasSpawned = 0;
				Integer[] minions = MINIONS.get(npcId);
				for (Integer minion : minions)
				{
					L2Attackable newNpc = (L2Attackable) addSpawn(minion, (npc.getX() + Rnd.get(-150, 150)), (npc.getY() + Rnd.get(-150, 150)), npc.getZ(), 0, false, 0);
					newNpc.setRunning();
					newNpc.addDamageHate(caster, 0, 999);
					newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, caster);
				}
			}
		}
		return super.onSkillUse(npc, caster, skill);
	}
	
	@Override
	public void run()
	{}
}