/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.ai.additional;

import java.util.ArrayList;

import javolution.util.FastSet;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

public class Transform extends Quest implements Runnable
{
	private static int HasSpawned;
	private static FastSet<Integer> myTrackingSet = new FastSet<>(); // Used to track instances of npcs
	private final ArrayList<Transformer> _mobs = new ArrayList<>();
	
	private static class Transformer
	{
		private final int _id;
		private final int _idPoly;
		private final int _chance;
		private final int _message;
		private final int _effect;
		
		protected Transformer(final int id, final int idPoly, final int chance, final int message, final int effect)
		{
			_id = id;
			_idPoly = idPoly;
			_chance = chance;
			_message = message;
			_effect = effect;
		}
		
		protected int getNpcId()
		{
			return _id;
		}
		
		protected int getIdPoly()
		{
			return _idPoly;
		}
		
		protected int getChance()
		{
			return _chance;
		}
		
		protected int getMessage()
		{
			return _message;
		}
		
		protected int getEffect()
		{
			return _effect;
		}
	}
	
	private static String[] Message =
	{
		"I cannot despise the fellow! I see his sincerity in the duel.",
		"Nows we truly begin!",
		"Fool! Right now is only practice!",
		"Have a look at my true strength.",
		"This time at the last! The end!"
	};
	
	public Transform(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		_mobs.add(new Transformer(21261, 21262, 1, 5, 1)); // 1st mutation Ol Mahum Transcender
		_mobs.add(new Transformer(21262, 21263, 1, 5, 1)); // 2st mutation Ol Mahum Transcender
		_mobs.add(new Transformer(21263, 21264, 1, 5, 1)); // 3rd mutation Ol Mahum Transcender
		_mobs.add(new Transformer(21258, 21259, 100, 5, 1)); // always mutation on atk Fallen Orc Shaman
		_mobs.add(new Transformer(20835, 21608, 1, 5, 1)); // zaken's seer to zaken's watchman
		_mobs.add(new Transformer(21608, 21609, 1, 5, 1)); // zaken's watchman
		_mobs.add(new Transformer(20832, 21602, 1, 5, 1)); // Zaken's pikeman
		_mobs.add(new Transformer(21602, 21603, 1, 5, 1)); // Zaken's pikeman
		_mobs.add(new Transformer(20833, 21605, 1, 5, 1)); // Zaken's archet
		_mobs.add(new Transformer(21605, 21606, 1, 5, 1)); // Zaken's archet
		_mobs.add(new Transformer(21625, 21623, 1, 5, 1)); // zaken's Elite Guard to zaken's Guard
		_mobs.add(new Transformer(21623, 21624, 1, 5, 1)); // zaken's Guard
		_mobs.add(new Transformer(20842, 21620, 0, 5, 1)); // Musveren
		_mobs.add(new Transformer(21620, 21621, 0, 5, 1)); // Musveren
		_mobs.add(new Transformer(20830, 20859, 100, 0, 1)); //
		_mobs.add(new Transformer(21067, 21068, 100, 0, 1)); //
		_mobs.add(new Transformer(21062, 21063, 100, 0, 1)); // Angels
		_mobs.add(new Transformer(20831, 20860, 100, 0, 1)); //
		_mobs.add(new Transformer(21070, 21071, 100, 0, 1)); //
		_mobs.add(new Transformer(21521, 21522, 5, 0, 0)); // Claw of Splendor
		_mobs.add(new Transformer(21527, 21528, 5, 0, 0)); // Anger of Splendor
		_mobs.add(new Transformer(21537, 21538, 5, 0, 0)); // Fang of Splendor
		_mobs.add(new Transformer(21533, 21534, 5, 0, 0)); // Alliance of Splendor
		
		final int[] mobsKill =
		{
			20830,
			21067,
			21062,
			20831,
			21070
		};
		
		for (final int mob : mobsKill)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
		}
		
		final int[] mobsAttack =
		{
			21620,
			20842,
			21623,
			21625,
			21605,
			20833,
			21602,
			20832,
			21608,
			20835,
			21258,
			21521,
			21527,
			21537,
			21533
		};
		
		for (final int mob : mobsAttack)
		{
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
		}
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		final int npcObjId = npc.getObjectId();
		
		for (Transformer monsterPoly : _mobs)
		{
			if (npc.getNpcId() == monsterPoly.getNpcId())
			{
				if (!myTrackingSet.contains(npcObjId)) // this allows to handle multiple instances of npc
				{
					myTrackingSet.add(npcObjId);
					HasSpawned = npcObjId;
				}
				
				if (HasSpawned == npcObjId)
				{
					if (Rnd.get(100) <= monsterPoly.getChance() * Config.RATE_DROP_QUEST)
					{
						if (monsterPoly.getMessage() != 0)
						{
							npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), Message[Rnd.get(monsterPoly.getMessage())]));
						}
						
						if (monsterPoly.getChance() > 0)
						{
							HasSpawned = 0;
							npc.onDecay();
							
							L2Attackable newNpc = (L2Attackable) addSpawn(monsterPoly.getIdPoly(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
							
							if (monsterPoly.getEffect() > 0)
							{
								ThreadPoolManager.getInstance().executeTask(new NPCSpawnTask(newNpc, 4000, 800000));
							}
							
							L2Character originalAttacker = isPet ? attacker.getPet() : attacker;
							
							if (npc.isChampion())
							{
								npc.setChampion(false);
								newNpc.setChampion(true);
							}
							
							newNpc.setRunning();
							newNpc.addDamageHate(originalAttacker, 0, 999);
							newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalAttacker);
							
							originalAttacker.setTargetTrasformedNpc(newNpc);
						}
					}
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(final L2NpcInstance npc, final L2PcInstance killer, final boolean isPet)
	{
		for (final Transformer monster : _mobs)
		{
			if (npc.getNpcId() == monster.getNpcId())
			{
				if (monster.getMessage() != 0)
				{
					npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), Message[Rnd.get(monster.getMessage())]));
				}
				
				final L2Attackable newNpc = (L2Attackable) addSpawn(monster.getIdPoly(), npc);
				final L2Character originalAttacker = isPet ? killer.getPet() : killer;
				
				if (npc.isChampion())
				{
					npc.setChampion(false);
					newNpc.setChampion(true);
				}
				
				newNpc.setRunning();
				newNpc.addDamageHate(originalAttacker, 0, 999);
				newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalAttacker);
			}
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public void run()
	{
	}
	
	private class NPCSpawnTask implements Runnable
	{
		private final L2NpcInstance _newNpc;
		private final long _spawnEffectTime;
		private final int _spawnAbnormalEffect;
		
		public NPCSpawnTask(L2NpcInstance newNpc, long spawnEffectTime, int spawnAbnormalEffect)
		{
			_newNpc = newNpc;
			_spawnEffectTime = spawnEffectTime;
			_spawnAbnormalEffect = Integer.decode("0x" + spawnAbnormalEffect);
		}
		
		@Override
		public void run()
		{
			_newNpc.startAbnormalEffect(_spawnAbnormalEffect);
			
			try
			{
				Thread.sleep(_spawnEffectTime);
			}
			catch (final InterruptedException e)
			{
			}
			
			_newNpc.stopAbnormalEffect(_spawnAbnormalEffect);
		}
	}
}