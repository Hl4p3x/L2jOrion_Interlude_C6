/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.util.random.Rnd;

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
		}
	}
	
	@Override
	public String onKill(final L2NpcInstance npc, final L2PcInstance killer, final boolean isPet)
	{
		final int npcId = npc.getNpcId();
		for (final int treeId : trees)
		{
			if (npcId == treeId)
			{
				for (int i = 0; i < 20; i++)
				{
					final L2Attackable newNpc = (L2Attackable) addSpawn(27189, npc.getX(), npc.getY(), npc.getZ(), 0, false, 30000);
					final L2Character originalKiller = isPet ? killer.getPet() : killer;
					newNpc.setRunning();
					newNpc.addDamageHate(originalKiller, 0, 999);
					newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalKiller);
					if (Rnd.nextBoolean())
					{
						if (originalKiller != null)
						{
							final L2Skill skill = SkillTable.getInstance().getInfo(4243, 1);
							if (skill != null)
							{
								skill.getEffects(newNpc, originalKiller, false, false, false);
							}
						}
					}
				}
			}
		}
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public void run()
	{
		
	}
}
