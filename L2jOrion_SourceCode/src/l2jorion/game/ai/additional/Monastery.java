package l2jorion.game.ai.additional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.util.Util;
import l2jorion.util.random.Rnd;

public class Monastery extends Quest implements Runnable
{
	private static int HasSpawned;
	private static Set<Integer> myTrackingSet = new HashSet<>(); // Used to track instances of npcs
	public static final int[] mobs1 =
	{
		22124,
		22125,
		22126,
		22127,
		22129
	};
	
	public static final int[] mobs2 =
	{
		22134,
		22135
	};
	
	public static final int[] mobs3 =
	{
		22128
	};
	
	public static final String[] text =
	{
		"You cannot carry a weapon without authorization!",
		"name, why would you choose the path of darkness?!",
		"name! How dare you defy the will of Einhasad!"
	};
	
	public Monastery(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		registerMobs(mobs1, QuestEventType.ON_AGGRO_RANGE_ENTER, QuestEventType.ON_SPAWN, QuestEventType.ON_SPELL_FINISHED);
		registerMobs(mobs2, QuestEventType.ON_SPELL_FINISHED);
		registerMobs(mobs3, QuestEventType.ON_ATTACK);
	}
	
	@Override
	public String onAggroRangeEnter(L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		if (Util.contains(mobs1, npc.getNpcId()) && !npc.isInCombat() && npc.getTarget() == null)
		{
			if (player.getActiveWeaponInstance() != null && !player.isSilentMoving())
			{
				npc.setTarget(player);
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), text[0]));
				
				switch (npc.getNpcId())
				{
					case 22124:
					case 22126:
					{
						L2Skill skill = SkillTable.getInstance().getInfo(4589, 8);
						npc.doCast(skill);
						break;
					}
					default:
					{
						npc.setIsRunning(true);
						((L2Attackable) npc).addDamageHate(player, 0, 999);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
						break;
					}
				}
			}
			else if (((L2Attackable) npc).getMostHated() == null)
			{
				return null;
			}
		}
		
		return null;
	}
	
	@Override
	public String onSpawn(L2NpcInstance npc)
	{
		if (Util.contains(mobs1, npc.getNpcId()))
		{
			final List<L2PlayableInstance> result = new ArrayList<>();
			final Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
			for (L2Object obj : objs)
			{
				if (obj instanceof L2PcInstance || obj instanceof L2PetInstance)
				{
					if (Util.checkIfInRange(npc.getAggroRange(), npc, obj, true) && !((L2Character) obj).isDead())
					{
						result.add((L2PlayableInstance) obj);
					}
				}
			}
			
			if (!result.isEmpty() && result.size() != 0)
			{
				Object[] characters = result.toArray();
				for (Object obj : characters)
				{
					final L2PlayableInstance target = (L2PlayableInstance) (obj instanceof L2PcInstance ? obj : ((L2Summon) obj).getOwner());
					if (target.getActiveWeaponInstance() == null || (target instanceof L2PcInstance && ((L2PcInstance) target).isSilentMoving()) || (target instanceof L2Summon && ((L2Summon) target).getOwner().isSilentMoving()))
					{
						continue;
					}
					
					if (target.getActiveWeaponInstance() != null && !npc.isInCombat() && npc.getTarget() == null)
					{
						npc.setTarget(target);
						npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), text[0]));
						switch (npc.getNpcId())
						{
							case 22124:
							case 22126:
							case 22127:
							{
								L2Skill skill = SkillTable.getInstance().getInfo(4589, 8);
								npc.doCast(skill);
								break;
							}
							default:
							{
								npc.setIsRunning(true);
								((L2Attackable) npc).addDamageHate(target, 0, 999);
								npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
								break;
							}
						}
					}
				}
			}
		}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onSpellFinished(L2NpcInstance npc, L2PcInstance player, L2Skill skill)
	{
		if (Util.contains(mobs1, npc.getNpcId()) && skill.getId() == 4589)
		{
			npc.setIsRunning(true);
			((L2Attackable) npc).addDamageHate(player, 0, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
		}
		
		if (Util.contains(mobs2, npc.getNpcId()))
		{
			if (skill.getSkillType() == SkillType.AGGDAMAGE)
			{
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), text[Rnd.get(2) + 1].replace("name", player.getName())));
				((L2Attackable) npc).addDamageHate(player, 0, 999);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
				
			}
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onAttack(final L2NpcInstance npc, L2PcInstance attacker, final int damage, final boolean isPet)
	{
		final int npcId = npc.getNpcId();
		final int npcObjId = npc.getObjectId();
		
		if (Util.contains(mobs3, npcId))
		{
			if (!myTrackingSet.contains(npcObjId)) // this allows to handle multiple instances of npc
			{
				myTrackingSet.add(npcObjId);
				HasSpawned = npcObjId;
			}
			
			if (HasSpawned == npcObjId)
			{
				HasSpawned = 0;
				npc.setIsRunning(true);
				((L2Attackable) npc).addDamageHate(attacker, 0, 999);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
				npc.doCast(SkillTable.getInstance().getInfo(4671, 1));
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public void run()
	{
	}
	
}