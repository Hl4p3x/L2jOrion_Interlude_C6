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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.type.L2BossZone;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class IceFairySirra extends Quest implements Runnable
{
	protected static final Logger LOG = LoggerFactory.getLogger(Antharas.class);
	
	private static final int STEWARD = 32029;
	private static final int ICE_QUEEN = 29056;
	
	private static final int SILVER_HEMOCYTE = 8057;
	private static L2BossZone _freyasZone;
	private static L2PcInstance _player = null;
	protected ScheduledFuture<?> _checkZoneTask = null;
	
	protected FastList<L2NpcInstance> _allMobs = new FastList<>();
	
	public IceFairySirra(final int id, final String name, final String descr)
	{
		super(id, name, descr);
		
		final int[] mobs =
		{
			STEWARD,
			ICE_QUEEN,
			22100,
			22102,
			22104
		};
		
		for (final int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
			addEventId(mob, Quest.QuestEventType.QUEST_START);
			addEventId(mob, Quest.QuestEventType.QUEST_TALK);
			//addEventId(mob, Quest.QuestEventType.NPC_FIRST_TALK);
		}
		
		String bossData = loadGlobalQuestVar("IceFairySirra");
		if (bossData.isEmpty())
		{
			String val = "" + 0;
			saveGlobalQuestVar("IceFairySirra", val);
		}
		
		init();
	}
	
	/*public String onFirstTalk(final L2NpcInstance npc, final L2PcInstance player)
	{
		if (player.getQuestState("IceFairySirra") == null)
		{
			_a.sys("newQuestState");
			newQuestState(player);
		}
		
		player.setLastQuestNpcObject(npc.getObjectId());
		
		String filename = "";
		if (npc.isBusy())
		{
			filename = getHtmlPath(4);
		}
		else
		{
			filename = getHtmlPath(0);
		}
		
		sendHtml(npc, player, filename);
		
		return null;
	}*/
	
	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance player)
	{
		String filename;
		
		if (npc.isBusy())
		{
			filename = getHtmlPath(4);
		}
		else
		{
			String bossData = loadGlobalQuestVar("IceFairySirra");
			long respawnTime = 0;
			long remainingTime = 0;
			
			if (Long.parseLong(bossData) > 0)
			{
				respawnTime = Long.parseLong(loadGlobalQuestVar("IceFairySirra"));
				remainingTime = respawnTime - System.currentTimeMillis();
			}
			
			if (remainingTime <= 0)
			{
					if (player.isInParty() && player.getParty().getPartyLeaderOID() == player.getObjectId())
					{
						if (checkItems(player))
						{
							startQuestTimer("start", 1000, null, player);
							_player = player;
							destroyItems(player);
							player.getInventory().addItem("Scroll", 8379, 3, player, null);
							npc.setBusy(true);
							screenMessage(player, "Steward: Please wait a moment.", 1000);
							filename = getHtmlPath(5);
						}
						else
						{
							filename = getHtmlPath(2);
						}
					}
					else
					{
						filename = getHtmlPath(1);
					}
			}
			else
			{
				filename = getHtmlPath(3);
			}
			
		}
		
		sendHtml(npc, player, filename);
		
		return null;
	}
	
	@Override
	public String onKill(L2NpcInstance npc, L2PcInstance killer, boolean isPet)
	{
		if (npc.getNpcId() == ICE_QUEEN)
		{
			npc.broadcastPacket(new PlaySound(1, "BS01_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			
			int respawnMinDelay = 86400000 * (int) Config.RAID_MIN_RESPAWN_MULTIPLIER;
			int respawnMaxDelay = 90000000 * (int) Config.RAID_MAX_RESPAWN_MULTIPLIER;
			long respawn_delay = Rnd.get(respawnMinDelay,respawnMaxDelay);
			
			long time = System.currentTimeMillis() + respawn_delay;
			
			saveGlobalQuestVar("IceFairySirra", ""+time);
		}
		return super.onKill(npc, killer, isPet);
	}
	
	@Override
	public String onAdvEvent(final String event, final L2NpcInstance npc, final L2PcInstance player)
	{
		if (event.equalsIgnoreCase("start"))
		{
			if (_freyasZone == null)
			{
				LOG.warn("IceFairySirraManager: Failed to load zone");
				cleanUp();
				return super.onAdvEvent(event, npc, player);
			}
			
			_freyasZone.setZoneEnabled(true);
			
			closeGates();
			doSpawns();
			checkZone(npc);
			startQuestTimer("Party_Port", 2000, null, player);
			startQuestTimer("End", 1802000, null, player);
		}
		else if (event.equalsIgnoreCase("Party_Port"))
		{
			teleportInside(player);
			screenMessage(player, "Steward: Please restore the Queen's appearance!", 10000);
			startQuestTimer("30MinutesRemaining", 300000, null, player);
		}
		else if (event.equalsIgnoreCase("30MinutesRemaining"))
		{
			screenMessage(player, "30 minute(s) are remaining.", 10000);
			startQuestTimer("20minutesremaining", 600000, null, player);
		}
		else if (event.equalsIgnoreCase("20MinutesRemaining"))
		{
			screenMessage(player, "20 minute(s) are remaining.", 10000);
			startQuestTimer("10minutesremaining", 600000, null, player);
		}
		else if (event.equalsIgnoreCase("10MinutesRemaining"))
		{
			screenMessage(player, "Steward: Waste no time! Please hurry!", 10000);
		}
		else if (event.equalsIgnoreCase("End"))
		{
			screenMessage(player, "Steward: Was it indeed too much to ask.", 10000);
			cleanUp();
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	public void init()
	{
		_freyasZone = GrandBossManager.getInstance().getZone(105546, -127892, -2768);
		if (_freyasZone == null)
		{
			LOG.warn("IceFairySirraManager: Failed to load zone");
			return;
		}
		_freyasZone.setZoneEnabled(false);
		
		final L2NpcInstance steward = findTemplate(STEWARD);
		if (steward != null)
		{
			steward.setBusy(false);
		}
		
		if (_checkZoneTask != null)
		{
			_checkZoneTask.cancel(true);
		}
		
		openGates();
	}
	
	public void cleanUp()
	{
		init();
		
		cancelQuestTimer("30MinutesRemaining", null, _player);
		cancelQuestTimer("20MinutesRemaining", null, _player);
		cancelQuestTimer("10MinutesRemaining", null, _player);
		cancelQuestTimer("End", null, _player);
		for (final L2NpcInstance mob : _allMobs)
		{
			try
			{
				mob.getSpawn().stopRespawn();
				mob.deleteMe();
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				
				LOG.error("IceFairySirraManager: Failed deleting mob.", e);
			}
		}
		_allMobs.clear();
	}
	
	public L2NpcInstance findTemplate(final int npcId)
	{
		L2NpcInstance npc = null;
		for (final L2Spawn spawn : SpawnTable.getInstance().getSpawnTable().values())
		{
			if (spawn != null && spawn.getNpcid() == npcId)
			{
				npc = spawn.getLastSpawn();
				break;
			}
		}
		return npc;
	}
	
	protected void openGates()
	{
		for (int i = 23140001; i < 23140003; i++)
		{
			try
			{
				final L2DoorInstance door = DoorTable.getInstance().getDoor(i);
				if (door != null)
				{
					door.openMe();
				}
				else
				{
					LOG.warn("IceFairySirraManager: Attempted to open undefined door. doorId: " + i);
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				
				LOG.error("IceFairySirraManager: Failed closing door", e);
			}
		}
	}
	
	protected void closeGates()
	{
		for (int i = 23140001; i < 23140003; i++)
		{
			try
			{
				final L2DoorInstance door = DoorTable.getInstance().getDoor(i);
				if (door != null)
				{
					door.closeMe();
				}
				else
				{
					LOG.warn("IceFairySirraManager: Attempted to close undefined door. doorId: " + i);
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					e.printStackTrace();
				
				LOG.error("IceFairySirraManager: Failed closing door", e);
			}
		}
	}
	
	public boolean checkItems(final L2PcInstance player)
	{
		if (player.getParty() != null)
		{
			for (final L2PcInstance pc : player.getParty().getPartyMembers())
			{
				final L2ItemInstance i = pc.getInventory().getItemByItemId(SILVER_HEMOCYTE);
				if (i == null || i.getCount() < 10)
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}
		
		return true;
	}
	
	public void destroyItems(final L2PcInstance player)
	{
		if (player.getParty() != null)
		{
			for (final L2PcInstance pc : player.getParty().getPartyMembers())
			{
				final L2ItemInstance i = pc.getInventory().getItemByItemId(SILVER_HEMOCYTE);
				pc.destroyItem("Hemocytes", i.getObjectId(), 10, null, false);
			}
		}
		else
		{
			cleanUp();
		}
	}
	
	public void teleportInside(final L2PcInstance player)
	{
		if (player.getParty() != null)
		{
			for (final L2PcInstance pc : player.getParty().getPartyMembers())
			{
				pc.teleToLocation(113533, -126159, -3488, false);
				if (_freyasZone == null)
				{
					LOG.warn("IceFairySirraManager: Failed to load zone");
					cleanUp();
					return;
				}
				_freyasZone.allowPlayerEntry(pc, 2103);
			}
		}
		else
		{
			cleanUp();
		}
	}
	
	public void screenMessage(final L2PcInstance player, final String text, final int time)
	{
		if (player.getParty() != null)
		{
			for (final L2PcInstance pc : player.getParty().getPartyMembers())
			{
				pc.sendPacket(new ExShowScreenMessage(text, time));
			}
		}
		else
		{
			cleanUp();
		}
	}
	
	public void doSpawns()
	{
		final int[][] mobs =
		{
			{
				29060,
				105546,
				-127892,
				-2768
			},
			{
				29056,
				102779,
				-125920,
				-2840
			},
			{
				22100,
				111719,
				-126646,
				-2992
			},
			{
				22102,
				109509,
				-128946,
				-3216
			},
			{
				22104,
				109680,
				-125756,
				-3136
			}
		};
		L2Spawn spawnDat;
		L2NpcTemplate template;
		try
		{
			for (int i = 0; i < 5; i++)
			{
				template = NpcTable.getInstance().getTemplate(mobs[i][0]);
				if (template != null)
				{
					spawnDat = new L2Spawn(template);
					spawnDat.setAmount(1);
					spawnDat.setLocx(mobs[i][1]);
					spawnDat.setLocy(mobs[i][2]);
					spawnDat.setLocz(mobs[i][3]);
					spawnDat.setHeading(0);
					spawnDat.setRespawnDelay(60);
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_allMobs.add(spawnDat.doSpawn());
					spawnDat.stopRespawn();
				}
				else
				{
					LOG.warn("IceFairySirraManager: Data missing in NPC table for ID: " + mobs[i][0]);
				}
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("IceFairySirraManager: Spawns could not be initialized: " + e);
		}
	}
	
	public String getHtmlPath(final int val)
	{
		String pom = "";
		
		pom = "32029-" + val;
		if (val == 0)
		{
			pom = "32029";
		}
		
		final String temp = "data/html/default/" + pom + ".htm";
		
		if (!Config.LAZY_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it doesnt exist
			if (HtmCache.getInstance().contains(temp))
			{
				return temp;
			}
		}
		else
		{
			if (HtmCache.getInstance().isLoadable(temp))
			{
				return temp;
			}
		}
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
	
	public void sendHtml(final L2NpcInstance npc, final L2PcInstance player, final String filename)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		
		if (_checkZoneTask != null)
		{
			html.replace("%time%", _checkZoneTask.getDelay(TimeUnit.MINUTES));
		}
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	protected void checkZone(L2NpcInstance npc)
	{
		if (_checkZoneTask == null)
		{
			_checkZoneTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() ->
			{
				npc.setBusy(false);
				
				if (_checkZoneTask != null)
				{
					_checkZoneTask.cancel(true);
				}
				
			}, 1802000, 100);
		}
	}
	
	@Override
	public void run()
	{
	}
}
