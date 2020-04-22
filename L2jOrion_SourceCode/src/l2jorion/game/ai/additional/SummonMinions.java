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

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.util.random.Rnd;

public class SummonMinions extends Quest implements Runnable
{
	private static int HasSpawned;
	private static FastSet<Integer> myTrackingSet = new FastSet<>(); // Used to track instances of npcs
	private final FastMap<Integer, FastList<L2PcInstance>> _attackersList = new FastMap<Integer, FastList<L2PcInstance>>().shared();
	private static final FastMap<Integer, Integer[]> MINIONS = new FastMap<>();
	
	static
	{
		MINIONS.put(20767, new Integer[]
		{
			20768,
			20769,
			20770
		}); // Timak Orc Troop
		
		MINIONS.put(21524, new Integer[]
		{
			21525
		}); // Blade of Splendor
		MINIONS.put(21531, new Integer[]
		{
			21658
		}); // Punishment of Splendor
		MINIONS.put(21539, new Integer[]
		{
			21540
		}); // Wailing of Splendor
		MINIONS.put(22257, new Integer[]
		{
			18364,
			18364
		}); // Island Guardian
		MINIONS.put(22258, new Integer[]
		{
			18364,
			18364
		}); // White Sand Mirage
		MINIONS.put(22259, new Integer[]
		{
			18364,
			18364
		}); // Muddy Coral
		MINIONS.put(22260, new Integer[]
		{
			18364,
			18364
		}); // Kleopora
		MINIONS.put(22261, new Integer[]
		{
			18365,
			18365
		}); // Seychelles
		MINIONS.put(22262, new Integer[]
		{
			18365,
			18365
		}); // Naiad
		MINIONS.put(22263, new Integer[]
		{
			18365,
			18365
		}); // Sonneratia
		MINIONS.put(22264, new Integer[]
		{
			18366,
			18366
		}); // Castalia
		MINIONS.put(22265, new Integer[]
		{
			18366,
			18366
		}); // Chrysocolla
		MINIONS.put(22266, new Integer[]
		{
			18366,
			18366
		}); // Pythia
	}
	
	public SummonMinions(final int questId, final String name, final String descr)
	{
		super(questId, name, descr);
		final int[] mobs =
		{
			20767,
			21524,
			21531,
			21539,
			22257,
			22258,
			22259,
			22260,
			22261,
			22262,
			22263,
			22264,
			22265,
			22266
		};
		
		for (final int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
		}
	}
	
	@Override
	public String onAttack(final L2NpcInstance npc, L2PcInstance attacker, final int damage, final boolean isPet)
	{
		final int npcId = npc.getNpcId();
		final int npcObjId = npc.getObjectId();
		
		if (MINIONS.containsKey(npcId))
		{
			if (!myTrackingSet.contains(npcObjId)) // this allows to handle multiple instances of npc
			{
				myTrackingSet.add(npcObjId);
				HasSpawned = npcObjId;
			}
			
			if (HasSpawned == npcObjId)
			{
				if (npcId == 22030 || npcId == 22032 || npcId == 22038) // mobs that summon minions only on certain hp
				{
					if (npc.getStatus().getCurrentHp() < npc.getMaxHp() / 2)
					{
						HasSpawned = 0;
						if (Rnd.get(100) < 33) // mobs that summon minions only on certain chance
						{
							Integer[] minions = MINIONS.get(npcId);
							for (final Integer minion : minions)
							{
								final L2Attackable newNpc = (L2Attackable) addSpawn(minion, (npc.getX() + Rnd.get(-150, 150)), (npc.getY() + Rnd.get(-150, 150)), npc.getZ(), 0, false, 0);
								newNpc.setRunning();
								newNpc.addDamageHate(attacker, 0, 999);
								newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
							}
						}
					}
				}
				else if (npcId == 22257 || npcId == 22258 || npcId == 22259 || npcId == 22260 || npcId == 22261 || npcId == 22262 || npcId == 22263 || npcId == 22264 || npcId == 22265 || npcId == 22266)
				{
					if (isPet)
					{
						attacker = attacker.getPet().getOwner();
					}
					if (attacker.getParty() != null)
					{
						for (final L2PcInstance member : attacker.getParty().getPartyMembers())
						{
							if (_attackersList.get(npcObjId) == null)
							{
								final FastList<L2PcInstance> player = new FastList<>();
								player.add(member);
								_attackersList.put(npcObjId, player);
							}
							else if (!_attackersList.get(npcObjId).contains(member))
							{
								_attackersList.get(npcObjId).add(member);
							}
						}
					}
					else
					{
						if (_attackersList.get(npcObjId) == null)
						{
							final FastList<L2PcInstance> player = new FastList<>();
							player.add(attacker);
							_attackersList.put(npcObjId, player);
						}
						else if (!_attackersList.get(npcObjId).contains(attacker))
						{
							_attackersList.get(npcObjId).add(attacker);
						}
					}
					if ((attacker.getParty() != null) && attacker.getParty().getMemberCount() > 2 || _attackersList.get(npcObjId).size() > 2) // Just to make sure..
					{
						HasSpawned = 0;
						Integer[] minions = MINIONS.get(npcId);
						for (final Integer minion : minions)
						{
							final L2Attackable newNpc = (L2Attackable) addSpawn(minion, npc.getX() + Rnd.get(-150, 150), npc.getY() + Rnd.get(-150, 150), npc.getZ(), 0, false, 0);
							newNpc.setRunning();
							newNpc.addDamageHate(attacker, 0, 999);
							newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
						}
					}
				}
				else
				// mobs without special conditions
				{
					HasSpawned = 0;
					Integer[] minions = MINIONS.get(npcId);
					if (npcId != 20767)
					{
						for (final Integer minion : minions)
						{
							final L2Attackable newNpc = (L2Attackable) addSpawn(minion, npc.getX() + Rnd.get(-150, 150), npc.getY() + Rnd.get(-150, 150), npc.getZ(), 0, false, 0);
							newNpc.setRunning();
							newNpc.addDamageHate(attacker, 0, 999);
							newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
							newNpc.doCast(SkillTable.getInstance().getInfo(4671, 1));
						}
					}
					else
					{
						for (final Integer minion : minions)
						{
							this.addSpawn(minion, (npc.getX() + Rnd.get(-100, 100)), (npc.getY() + Rnd.get(-100, 100)), npc.getZ(), 0, false, 0);
						}
					}
					
					if (npcId == 20767)
					{
						npc.broadcastPacket(new CreatureSay(npcObjId, 0, npc.getName(), "Come out, you children of darkness!"));
					}
				}
			}
		}
		if (_attackersList.get(npcObjId) != null)
		{
			_attackersList.get(npcObjId).clear();
		}
		
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onKill(final L2NpcInstance npc, final L2PcInstance killer, final boolean isPet)
	{
		final int npcId = npc.getNpcId();
		final int npcObjId = npc.getObjectId();
		if (MINIONS.containsKey(npcId))
		{
			myTrackingSet.remove(npcObjId);
		}
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public void run()
	{
	}
}
