package l2jorion.game.ai.additional.events;

import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2GourdInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.QuestState;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public class SquashEvent extends Quest implements Runnable
{
	private static final int MANAGER = 31860;
	private static final int NECTAR_SKILL = 9000;
	
	private static final int[] CHRONO_LIST =
	{
		4202,
		5133,
		5817,
		7058,
		8350
	};
	
	public static final int[] SQUASH_LIST =
	{
		12774,
		12775,
		12776,
		12777,
		12778,
		12779,
		13016,
		13017
	};
	
	private static final String[] _NOCHRONO_TEXT =
	{
		"You cannot kill me without Chrono",
		"Hehe...keep trying...",
		"Nice try...",
		"Tired ?",
		"Go go ! haha..."
	};
	
	private static final String[] _CHRONO_TEXT =
	{
		"Arghh... Chrono weapon...",
		"My end is coming...",
		"Please leave me !",
		"Heeellpppp...",
		"Somebody help me please..."
	};
	private static final String[] _NECTAR_TEXT =
	{
		"Yummy... Nectar...",
		"Plase give me more...",
		"Hmmm.. More.. I need more...",
		"I will like you more if you give me more...",
		"Hmmmmmmm...",
		"My favourite..."
	};
	
	private static final int[][] DROPLIST =
	{
		// must be sorted by npcId !
		// npcId, itemId, chance
		// Young Squash
		{
			12774,
			1060,
			100
		}, // Lesser Healing potion
		{
			12774,
			1062,
			50
		}, // Haste potion
			// High Quality Squash
		{
			12775,
			1539,
			100
		}, // Greater Healing potion
		{
			12775,
			1375,
			70
		}, // Greater Swift Attack Potion
		{
			12775,
			1459,
			50
		}, // Crystal c-grade
			// Low Quality Squash
		{
			12776,
			1061,
			100
		}, // Healing potion
		{
			12776,
			1062,
			70
		}, // Haste potion
		{
			12776,
			1458,
			50
		}, // Crystal d-grade
		
		// Large Young Squash
		{
			12777,
			1061,
			100
		}, // Healing potion
		{
			12777,
			1374,
			50
		}, // Greater Haste potion
			// High Quality Large
		{
			12778,
			1539,
			100
		}, // Greater Healing potion
		{
			12778,
			6036,
			70
		}, // Greater Magic Haste Potion
		{
			12778,
			1459,
			40
		}, // Crystal c-grade
			// Low Quality Large
		{
			12779,
			6035,
			70
		}, // Magic Haste Potion
		{
			12779,
			1458,
			50
		}, // Crystal d-grade
		
		// King
		{
			13016,
			1540,
			100
		}, // Quick Healing Potion
		{
			13016,
			1460,
			40
		}, // Crystal b-grade
		{
			13016,
			5234,
			20
		}, // Mystery Potion
			// Emperor
		{
			13017,
			1540,
			100
		}, // Quick Healing Potion
		{
			13017,
			20004,
			40
		}, // Energy Ginseng
		{
			13017,
			1461,
			20
		}, // Crystal a-grade
		{
			13017,
			5234,
			10
		} // Mystery Potion
	};
	
	public SquashEvent(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		for (final int mob : SQUASH_LIST)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
			addEventId(mob, Quest.QuestEventType.ON_SKILL_SEE);
			addEventId(mob, Quest.QuestEventType.ON_SPAWN);
		}
		
		addStartNpc(MANAGER);
		addFirstTalkId(MANAGER);
		addTalkId(MANAGER);
		
		// addSpawn(MANAGER, 83077, 147910, -3471, 29412, false, 0);
	}
	
	@Override
	public String onAttack(L2NpcInstance npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (contains(SQUASH_LIST, npc.getNpcId()))
		{
			if (isPet)
			{
				noChronoText(npc);
				npc.setIsInvul(true);
				return null;
			}
			
			if (attacker.getActiveWeaponItem() != null && contains(CHRONO_LIST, attacker.getActiveWeaponItem().getItemId()))
			{
				ChronoText(npc);
				npc.setIsInvul(false);
				npc.getStatus().reduceHp(10, attacker);
				return null;
			}
			
			noChronoText(npc);
			npc.setIsInvul(true);
			return null;
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}
	
	@Override
	public String onSkillSee(L2NpcInstance npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (contains(targets, npc) && contains(SQUASH_LIST, npc.getNpcId()) && (skill.getId() == NECTAR_SKILL))
		{
			switch (npc.getNpcId())
			{
				case 12774:
					randomSpawn(12775, 12776, npc, caster, true);
					break;
				case 12777:
					randomSpawn(12778, 12779, npc, caster, true);
					break;
				case 12775:
					randomSpawn(13016, npc, caster, true);
					break;
				case 12778:
					randomSpawn(13017, npc, caster, true);
					break;
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		dropItem(npc, killer);
		
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public String onSpawn(L2NpcInstance npc)
	{
		npc.setIsImobilised(true);
		npc.setIsAttackDisabled(true);
		return super.onSpawn(npc);
	}
	
	private static final void dropItem(L2NpcInstance mob, L2PcInstance player)
	{
		final int npcId = mob.getNpcId();
		final int chance = Rnd.get(100);
		for (int i = 0; i < DROPLIST.length; i++)
		{
			int[] drop = DROPLIST[i];
			if (npcId == drop[0])
			{
				if (chance < drop[2])
				{
					if (drop[1] > 20000)
					{
						((L2MonsterInstance) mob).DropItem(player, drop[1], 2);
					}
					else
					{
						((L2MonsterInstance) mob).DropItem(player, drop[1], Rnd.get(2, 6));
					}
					continue;
				}
			}
			if (npcId < drop[0])
			{
				return; // not found
			}
		}
	}
	
	private void randomSpawn(int lower, int higher, L2NpcInstance npc, L2PcInstance caster, boolean delete)
	{
		int _random = Rnd.get(100);
		if (_random < 10)
		{
			spawnNext(lower, npc, caster);
		}
		else if (_random < 30)
		{
			spawnNext(higher, npc, caster);
		}
		else
		{
			nectarText(npc);
		}
	}
	
	private void randomSpawn(int npcId, L2NpcInstance npc, L2PcInstance caster, boolean delete)
	{
		if (Rnd.get(100) < 10)
		{
			spawnNext(npcId, npc, caster);
		}
		else
		{
			nectarText(npc);
		}
	}
	
	private void ChronoText(L2NpcInstance npc)
	{
		if (Rnd.get(100) < 20)
		{
			npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(), _CHRONO_TEXT[Rnd.get(_CHRONO_TEXT.length)]));
		}
	}
	
	private void noChronoText(L2NpcInstance npc)
	{
		if (Rnd.get(100) < 20)
		{
			npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(), _NOCHRONO_TEXT[Rnd.get(_NOCHRONO_TEXT.length)]));
		}
	}
	
	private void nectarText(L2NpcInstance npc)
	{
		if (Rnd.get(100) < 30)
		{
			npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(), _NECTAR_TEXT[Rnd.get(_NECTAR_TEXT.length)]));
		}
	}
	
	private void spawnNext(int npcId, L2NpcInstance npc, L2PcInstance caster)
	{
		L2GourdInstance gourd = (L2GourdInstance) npc;
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		
		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setId(IdFactory.getInstance().getNextId());
			
			spawn.setLocx(npc.getX());
			spawn.setLocy(npc.getY());
			spawn.setLocz(npc.getZ());
			
			gourd = (L2GourdInstance) spawn.spawnOne();
			L2World.getInstance().storeObject(gourd);
			gourd.setOwner(caster.getName());
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			sm.addString("Target is not ingame.");
			caster.sendPacket(sm);
		}
		
		// gourd.addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 60000);
		npc.deleteMe();
	}
	
	public static <T> boolean contains(T[] array, T obj)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	public static boolean contains(int[] array, int obj)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String onFirstTalk(L2NpcInstance npc, L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			Quest q = QuestManager.getInstance().getQuest(getName());
			st = q.newQuestState(player);
		}
		htmltext = npc.getNpcId() + ".htm";
		return htmltext;
	}
	
	@Override
	public void run()
	{
	}
}