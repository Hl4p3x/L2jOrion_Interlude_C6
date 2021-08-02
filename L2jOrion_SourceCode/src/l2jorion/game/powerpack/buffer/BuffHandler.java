/*
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
package l2jorion.game.powerpack.buffer;

import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import javolution.util.FastMap.Entry;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.manager.BaseBBSManager;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.IBBSHandler;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.powerpack.buffer.BuffTable.Buff;
import l2jorion.game.powerpack.buffer.BuffTable.Scheme;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.templates.L2Item;
import l2jorion.game.util.Util;

public class BuffHandler implements IVoicedCommandHandler, ICustomByPassHandler, IBBSHandler
{
	private static final String PARENT_DIR = "data/html/buffer/";
	private Map<Integer, ArrayList<Buff>> _savedAllBuffs;
	private Map<Integer, String> _visitedPages;
	private String lastUsedTarget = "";
	
	private ArrayList<Buff> getOwnBuffs(int objectId)
	{
		if (_savedAllBuffs.get(objectId) == null)
		{
			synchronized (_savedAllBuffs)
			{
				_savedAllBuffs.put(objectId, new ArrayList<Buff>());
			}
		}
		
		return _savedAllBuffs.get(objectId);
	}
	
	public BuffHandler()
	{
		_savedAllBuffs = new FastMap<>();
		_visitedPages = new FastMap<>();
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			PowerPackConfig.BUFFER_COMMAND,
			PowerPackConfig.BUFFER_COMMAND2
		};
	}
	
	private boolean checkAllowed(L2PcInstance activeChar)
	{
		if (activeChar.isGM())
		{
			return true;
		}
		
		String msg = null;
		if (activeChar.isSitting())
		{
			msg = "Can't use buffer when sitting.";
		}
		else if (activeChar.isCastingNow() || activeChar.isCastingPotionNow())
		{
			msg = "Can't use buffer when casting.";
		}
		else if (activeChar.isAlikeDead())
		{
			msg = "Can't use buffer while dead.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("ALL"))
		{
			msg = "Buffer is not available in this area.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("CURSED") && activeChar.isCursedWeaponEquiped())
		{
			msg = "Can't use Buffer with Cursed Weapon.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("ATTACK") && AttackStanceTaskManager.getInstance().getAttackStanceTask(activeChar))
		{
			msg = "Buffer is not available during the battle.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("COMBAT") && activeChar.isInCombat())
		{
			msg = "Buffer is not available during combat.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("PVPFLAG") && activeChar.getPvpFlag() > 0)
		{
			msg = "Buffer is not available during PvP Flag.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("KARMA") && activeChar.getKarma() > 0)
		{
			msg = "Buffer is not available during PvP Flag.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("DUNGEON") && activeChar.isIn7sDungeon())
		{
			msg = "Buffer is not available in the catacombs and necropolis.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("RB") && activeChar.isInsideZone(ZoneId.ZONE_BOSS))
		{
			msg = "Buffer is not available in this area.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("PVP") && activeChar.isInsideZone(ZoneId.ZONE_PVP))
		{
			msg = "Buffer is not available in this area.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("PEACE") && activeChar.isInsideZone(ZoneId.ZONE_PEACE))
		{
			msg = "Buffer is not available in this area.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("SIEGE") && activeChar.isInsideZone(ZoneId.ZONE_SIEGE))
		{
			msg = "Buffer is not available in this area.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("OLYMPIAD") && (activeChar.isInOlympiadMode() || activeChar.isInsideZone(ZoneId.ZONE_OLY) || OlympiadManager.getInstance().isRegistered(activeChar) || OlympiadManager.getInstance().isRegisteredInComp(activeChar)))
		{
			msg = "Buffer is not available in Olympiad.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("EVENT") && (activeChar.isInFunEvent()))
		{
			msg = "Buffer is not available in this event.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("TVT") && activeChar._inEventTvT && TvT.is_started())
		{
			msg = "Buffer is not available in TVT.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("CTF") && activeChar._inEventCTF && CTF.is_started())
		{
			msg = "Buffer is not available in CTF.";
		}
		else if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("DM") && activeChar._inEventDM && DM.is_started())
		{
			msg = "Buffer is not available in DM.";
		}
		
		if (msg != null)
		{
			activeChar.sendMessage(msg);
		}
		
		return msg == null;
	}
	
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		if (!checkAllowed(activeChar))
		{
			return false;
		}
		
		if (command.compareTo(PowerPackConfig.BUFFER_COMMAND) == 0 || command.compareTo(PowerPackConfig.BUFFER_COMMAND2) == 0)
		{
			String index = "";
			if (target != null && target.length() != 0)
			{
				if (!target.equals("0"))
				{
					index = "-" + target;
				}
			}
			
			String text = HtmCache.getInstance().getHtm(PARENT_DIR + "buffer" + index + ".htm");
			NpcHtmlMessage htm = new NpcHtmlMessage(activeChar.getLastQuestNpcObject());
			htm.setHtml(text);
			activeChar.sendPacket(htm);
		}
		return false;
	}
	
	private static final String[] _BYPASSCMD =
	{
		"doyoubuff"
	};
	
	@Override
	public String[] getByPassCommands()
	{
		return _BYPASSCMD;
	}
	
	@Override
	public void handleCommand(String command, final L2PcInstance player, String parameters)
	{
		if (player == null)
		{
			return;
		}
		
		if (!checkAllowed(player))
		{
			return;
		}
		
		L2NpcInstance buffer = null;
		
		if ((!PowerPackConfig.BUFFER_USEBBS) && (!PowerPackConfig.BUFFER_USECOMMAND))
		{
			if (player.getTarget() != null)
			{
				if (player.getTarget() instanceof L2NpcInstance)
				{
					buffer = (L2NpcInstance) player.getTarget();
					if (buffer.getTemplate().getNpcId() != PowerPackConfig.BUFFER_NPC)
					{
						buffer = null;
					}
				}
			}
			
			if (buffer == null)
			{
				return;
			}
			
			if (!player.isInsideRadius(buffer, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			{
				return;
			}
		}
		
		if (parameters.contains("Pet"))
		{
			if (player.getPet() == null)
			{
				return;
			}
		}
		
		StringTokenizer st = new StringTokenizer(parameters, " ");
		String currentCommand = st.nextToken();
		
		if (parameters.compareTo("ClearBuffs") == 0)
		{
			getOwnBuffs(player.getObjectId()).clear();
			player.sendMessage("Buff set cleared.");
		}
		else if (parameters.compareTo("ClearPetBuffs") == 0)
		{
			getOwnBuffs(player.getPet().getObjectId()).clear();
			player.sendMessage("Pet Buff set cleared.");
		}
		else if (parameters.compareTo("RemoveMenu") == 0)
		{
			showBuffs(player);
		}
		else if (parameters.startsWith("RemoveOne"))
		{
			if (st.hasMoreTokens())
			{
				int SkillId = 0;
				
				try
				{
					SkillId = Integer.parseInt(st.nextToken());
				}
				catch (NumberFormatException e)
				{
					return;
				}
				removeBuff(player, SkillId);
			}
		}
		else if (parameters.compareTo("RemoveAll") == 0)
		{
			final L2Effect[] effects = player.getAllEffects();
			
			for (L2Effect e : effects)
			{
				if (e.getEffectType() == L2Effect.EffectType.BUFF)
				{
					player.removeEffect(e);
				}
			}
		}
		else if (parameters.compareTo("RemovePetMenu") == 0)
		{
			showBuffsPet(player);
		}
		else if (parameters.startsWith("RemovePetOne"))
		{
			if (st.hasMoreTokens())
			{
				int SkillId = 0;
				
				try
				{
					SkillId = Integer.parseInt(st.nextToken());
				}
				catch (NumberFormatException e)
				{
					return;
				}
				removeBuffPet(player, SkillId);
			}
		}
		else if (parameters.compareTo("RemovePetAll") == 0)
		{
			final L2Effect[] effects = player.getPet().getAllEffects();
			
			for (L2Effect e : effects)
			{
				if (e.getEffectType() == L2Effect.EffectType.BUFF)
				{
					player.getPet().removeEffect(e);
				}
			}
		}
		else if (parameters.startsWith("Chat"))
		{
			String chatIndex = parameters.substring(4).trim();
			
			synchronized (_visitedPages)
			{
				_visitedPages.put(player.getObjectId(), chatIndex);
			}
			
			chatIndex = "-" + chatIndex;
			
			if (chatIndex.equals("-0"))
			{
				chatIndex = "";
			}
			
			String text = HtmCache.getInstance().getHtm("data/html/buffer/buffer" + chatIndex + ".htm");
			
			if (command.startsWith("bbsyoubuff"))
			{
				text = text.replace("-h custom_do", "bbs_bbs");
				BaseBBSManager.separateAndSend(text, player);
			}
			else
			{
				NpcHtmlMessage htm = new NpcHtmlMessage(player.getLastQuestNpcObject());
				htm.setHtml(text);
				player.sendPacket(htm);
			}
		}
		else if (parameters.startsWith("RestoreAll"))
		{
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			player.getStatus().setCurrentCp(player.getMaxCp());
			player.getStatus().setCurrentMp(player.getMaxMp());
			player.getStatus().setCurrentHp(player.getMaxHp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
		}
		else if (parameters.startsWith("RestorePetAll"))
		{
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.getPet().getStatus().setCurrentMp(player.getPet().getMaxMp());
			player.getPet().getStatus().setCurrentHp(player.getPet().getMaxHp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
		}
		else if (parameters.startsWith("RestoreCP"))
		{
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.getStatus().setCurrentCp(player.getMaxCp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
		}
		else if (parameters.startsWith("RestoreMP"))
		{
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.getStatus().setCurrentMp(player.getMaxMp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
		}
		else if (parameters.startsWith("RestorePetMP"))
		{
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.getPet().getStatus().setCurrentMp(player.getPet().getMaxMp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
		}
		else if (parameters.startsWith("RestoreHP"))
		{
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.getStatus().setCurrentHp(player.getMaxHp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
		}
		else if (parameters.startsWith("RestorePetHP"))
		{
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
			
			player.getPet().getStatus().setCurrentHp(player.getPet().getMaxHp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
		}
		else if (parameters.startsWith("MakeBuffs") || parameters.startsWith("RestoreBuffs"))
		{
			String buffName = parameters.substring(9).trim();
			
			ArrayList<Buff> buffs = null;
			
			if (parameters.startsWith("RestoreBuffs"))
			{
				buffs = getOwnBuffs(player.getObjectId());
			}
			else
			{
				buffs = BuffTable.getInstance().getBuffsForName(buffName);
			}
			
			if (buffs != null && buffs.size() == 1)
			{
				if (!getOwnBuffs(player.getObjectId()).contains(buffs.get(0)))
				{
					if (getOwnBuffs(player.getObjectId()).size() < PowerPackConfig.NPCBUFFER_MAX_SKILLS)
					{
						getOwnBuffs(player.getObjectId()).add(buffs.get(0));
					}
					
					if (getOwnBuffs(player.getObjectId()).size() > PowerPackConfig.NPCBUFFER_MAX_SKILLS || getOwnBuffs(player.getObjectId()).size() == PowerPackConfig.NPCBUFFER_MAX_SKILLS)
					{
						player.sendMessage("This set has reached maximun amount of allowed buffs: " + PowerPackConfig.NPCBUFFER_MAX_SKILLS + ".");
						player.sendPacket(new ExShowScreenMessage("This set has reached maximun amount of allowed buffs: " + PowerPackConfig.NPCBUFFER_MAX_SKILLS + ".", 1000, 2, false));
						player.sendPacket(new PlaySound("ItemSound2.broken_key"));
						showManageSchemeWindow(player, "player");
						return;
					}
					
					player.sendPacket(new ExShowScreenMessage("Buffs set: " + getOwnBuffs(player.getObjectId()).size(), 1000, 3, false));
				}
			}
			
			if (buffs == null || buffs.size() == 0)
			{
				player.sendMessage("Your buffs set is missing.");
				return;
			}
			
			for (Buff buff : buffs)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(buff._skillId, buff._skillLevel);
				if (skill != null)
				{
					if (player.getLevel() >= buff._minLevel && player.getLevel() <= buff._maxLevel)
					{
						if (buff._useItem)
						{
							if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
							{
								L2ItemInstance item = player.getInventory().getItemByItemId(buff._itemId);
								if (item == null || item.getCount() < buff._itemCount)
								{
									player.sendMessage("You don't have enough " + getItemNameById(buff._itemId) + ".");
									player.sendPacket(new ExShowScreenMessage("You don't have enough " + getItemNameById(buff._itemId) + ".", 1000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
								
								player.destroyItem("Consume", item.getObjectId(), buff._itemCount, null, true);
							}
						}
						
						if (buff._premium && player.getPremiumService() == 0)
						{
							player.sendMessage("You're not The Premium account.");
							player.sendPacket(new ExShowScreenMessage("You're not The Premium account.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							continue;
						}
						
						if (buff._voter && (player.eligibleToVoteHop() || player.eligibleToVoteTop() || player.eligibleToVoteNet() || player.eligibleToVoteBra()))
						{
							player.sendMessage("You can't get this buff, because you didn't vote yet.");
							player.sendPacket(new ExShowScreenMessage("You can't get this buff, because you didn't vote yet.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							continue;
						}
						
						if (!buff._force && buffer != null)
						{
							buffer.setBusy(true);
							buffer.setCurrentMp(buffer.getMaxMp());
							buffer.setTarget(player);
							skill.getEffects(buffer, player, false, false, false);
							buffer.setBusy(false);
						}
						else
						{
							skill.getEffects(player, player, false, false, false);
						}
					}
					try
					{
						Thread.sleep(100); // Delay for the packet...
					}
					catch (InterruptedException e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
					}
				}
			}
			
			if (_visitedPages.get(player.getObjectId()) != null)
			{
				handleCommand(command, player, "Chat " + _visitedPages.get(player.getObjectId()));
			}
			else
			{
				useVoicedCommand(PowerPackConfig.BUFFER_COMMAND, player, "");
			}
		}
		else if (parameters.startsWith("MakePetBuffs") || parameters.startsWith("RestorePetBuffs"))
		{
			if (player.getPet() == null)
			{
				player.sendMessage("You have not a summoned pet");
				return;
			}
			
			String buffName = parameters.substring(12).trim();
			
			ArrayList<Buff> buffs = null;
			
			if (parameters.startsWith("RestorePetBuffs"))
			{
				buffs = getOwnBuffs(player.getPet().getObjectId());
			}
			else
			{
				buffs = BuffTable.getInstance().getBuffsForName(buffName);
			}
			
			if (buffs != null && buffs.size() == 1)
			{
				if (!getOwnBuffs(player.getPet().getObjectId()).contains(buffs.get(0)))
				{
					if (getOwnBuffs(player.getPet().getObjectId()).size() < PowerPackConfig.NPCBUFFER_MAX_SKILLS)
					{
						getOwnBuffs(player.getPet().getObjectId()).add(buffs.get(0));
					}
					
					if (getOwnBuffs(player.getPet().getObjectId()).size() > PowerPackConfig.NPCBUFFER_MAX_SKILLS || getOwnBuffs(player.getPet().getObjectId()).size() == PowerPackConfig.NPCBUFFER_MAX_SKILLS)
					{
						player.sendMessage("This set has reached maximun amount of allowed buffs: " + PowerPackConfig.NPCBUFFER_MAX_SKILLS + ".");
						player.sendPacket(new ExShowScreenMessage("This set has reached maximun amount of allowed buffs: " + PowerPackConfig.NPCBUFFER_MAX_SKILLS + ".", 1000, 2, false));
						player.sendPacket(new PlaySound("ItemSound2.broken_key"));
						showManageSchemeWindow(player, "pet");
						return;
					}
					
					player.sendPacket(new ExShowScreenMessage("Buffs set: " + getOwnBuffs(player.getPet().getObjectId()).size(), 1000, 3, false));
				}
			}
			
			if (buffs == null || buffs.size() == 0)
			{
				player.sendMessage("Your pet buffs set is missing.");
				return;
			}
			
			for (Buff buff : buffs)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(buff._skillId, buff._skillLevel);
				if (skill != null)
				{
					if (player.getLevel() >= buff._minLevel && player.getLevel() <= buff._maxLevel)
					{
						if (buff._useItem)
						{
							if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
							{
								L2ItemInstance item = player.getInventory().getItemByItemId(buff._itemId);
								if (item == null || item.getCount() < buff._itemCount)
								{
									player.sendMessage("You don't have enough " + getItemNameById(buff._itemId) + ".");
									player.sendPacket(new ExShowScreenMessage("You don't have enough " + getItemNameById(buff._itemId) + ".", 1000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
								
								player.destroyItem("Consume", item.getObjectId(), buff._itemCount, null, true);
							}
						}
						
						if (buff._premium && player.getPremiumService() == 0)
						{
							player.sendMessage("You're not The Premium account.");
							player.sendPacket(new ExShowScreenMessage("You're not The Premium account.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							continue;
						}
						
						if (buff._voter && (player.eligibleToVoteHop() || player.eligibleToVoteTop() || player.eligibleToVoteNet() || player.eligibleToVoteBra()))
						{
							player.sendMessage("You can't get this buff, because you didn't vote yet.");
							player.sendPacket(new ExShowScreenMessage("You can't get this buff, because you didn't vote yet.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							continue;
						}
						
						if (!buff._force && buffer != null)
						{
							buffer.setBusy(true);
							buffer.setCurrentMp(buffer.getMaxMp());
							buffer.setTarget(player.getPet());
							skill.getEffects(buffer, player.getPet(), false, false, false);
							buffer.setBusy(false);
						}
						else
						{
							skill.getEffects(player, player.getPet(), false, false, false);
						}
					}
					try
					{
						Thread.sleep(100); // Delay for the packet...
					}
					catch (InterruptedException e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
					}
				}
			}
			
			if (_visitedPages.get(player.getObjectId()) != null)
			{
				handleCommand(command, player, "Chat " + _visitedPages.get(player.getObjectId()));
			}
			else
			{
				useVoicedCommand(PowerPackConfig.BUFFER_COMMAND, player, "");
			}
		}
		else if (currentCommand.startsWith("givebuffs"))
		{
			String targettype = st.nextToken();
			String scheme_key = st.nextToken();
			int cost = 0;
			
			if (cost == 0 || cost <= player.getInventory().getAdena())
			{
				L2Character target = player;
				if (targettype.equalsIgnoreCase("pet"))
				{
					target = player.getPet();
				}
				
				if (target != null)
				{
					for (Scheme sk : BuffTable.getInstance().getScheme(player.getObjectId(), scheme_key))
					{
						L2Skill skill = SkillTable.getInstance().getInfo(sk._skillId, sk._skillLevel);
						if (skill == null)
						{
							continue;
						}
						
						if (sk._useItem)
						{
							if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
							{
								L2ItemInstance item = player.getInventory().getItemByItemId(sk._itemId);
								if (item == null || item.getCount() < sk._itemCount)
								{
									player.sendMessage("You don't have enough " + getItemNameById(sk._itemId) + ".");
									player.sendPacket(new ExShowScreenMessage("You don't have enough " + getItemNameById(sk._itemId) + ".", 1000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
								
								player.destroyItem("Consume", item.getObjectId(), sk._itemCount, null, true);
							}
						}
						
						if (sk._premium && player.getPremiumService() == 0)
						{
							player.sendMessage("You're not The Premium account.");
							player.sendPacket(new ExShowScreenMessage("You're not The Premium account.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							continue;
						}
						
						if (sk._voter && (player.eligibleToVoteHop() || player.eligibleToVoteTop() || player.eligibleToVoteNet() || player.eligibleToVoteBra()))
						{
							player.sendMessage("You can't get this buff, because you didn't vote yet.");
							player.sendPacket(new ExShowScreenMessage("You can't get this buff, because you didn't vote yet.", 1000, 2, false));
							player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
							continue;
						}
						
						if (buffer != null)
						{
							buffer.setBusy(true);
							buffer.setCurrentMp(buffer.getMaxMp());
							buffer.setTarget(target);
							skill.getEffects(buffer, target, false, false, false);
							buffer.setBusy(false);
						}
						else
						{
							skill.getEffects(target, target, false, false, false);
						}
					}
				}
				else
				{
					player.sendMessage("Incorrect Target.");
				}
			}
			else
			{
				player.sendMessage("Not enough adena.");
			}
			
			showManageSchemeWindow(player, targettype);
		}
		else if (currentCommand.startsWith("manageschemes"))
		{
			String targetType = st.nextToken();
			lastUsedTarget = targetType;
			showManageSchemeWindow(player, targetType);
		}
		else if (currentCommand.startsWith("createscheme"))
		{
			ArrayList<Buff> buffs = getOwnBuffs(player.getObjectId());
			
			String name = "";
			String targettype = "";
			
			if (st.countTokens() > 1)
			{
				name = st.nextToken();
				targettype = st.nextToken();
			}
			
			if (targettype.isEmpty())
			{
				targettype = lastUsedTarget;
			}
			
			if (buffs == null || buffs.size() == 0)
			{
				player.sendMessage("Your buffs set is empty.");
				showManageSchemeWindow(player, targettype);
				return;
			}
			
			if (name.isEmpty() || name.length() < 2 || name.length() > 14)
			{
				player.sendMessage("Error: Scheme's name must contain 2-14 chars without any space.");
				showManageSchemeWindow(player, targettype);
			}
			else if (BuffTable.getInstance().getAllSchemes(player.getObjectId()) != null && BuffTable.getInstance().getAllSchemes(player.getObjectId()).size() == PowerPackConfig.NPCBUFFER_MAX_SCHEMES)
			{
				player.sendMessage("Error: Maximun schemes amount reached, please delete one before creating a new one.");
				showManageSchemeWindow(player, targettype);
			}
			else if (BuffTable.getInstance().getAllSchemes(player.getObjectId()) != null && BuffTable.getInstance().getAllSchemes(player.getObjectId()).containsKey(name))
			{
				player.sendMessage("Error: duplicate entry. Please use another name.");
				showManageSchemeWindow(player, targettype);
			}
			else
			{
				if (BuffTable.getInstance().getAllSchemes(player.getObjectId()) == null)
				{
					BuffTable.getInstance().getSchemesTable().put(player.getObjectId(), new FastMap<String, ArrayList<Scheme>>(PowerPackConfig.NPCBUFFER_MAX_SCHEMES + 1));
				}
				
				BuffTable.getInstance().setScheme(player.getObjectId(), name.trim(), new ArrayList<Scheme>(PowerPackConfig.NPCBUFFER_MAX_SKILLS + 1));
				
				for (Buff buff : buffs)
				{
					if (BuffTable.getInstance() != null && BuffTable.getInstance().getScheme(player.getObjectId(), name) != null && BuffTable.getInstance().getScheme(player.getObjectId(), name).size() < PowerPackConfig.NPCBUFFER_MAX_SKILLS)
					{
						BuffTable.getInstance().getScheme(player.getObjectId(), name).add(new Scheme(player.getObjectId(), buff._skillId, buff._skillLevel, buff._premium, buff._voter, buff._useItem, buff._itemId, buff._itemCount, name));
					}
					else
					{
						player.sendMessage("This scheme has reached maximun amount of buffs.");
					}
				}
				
				buffs.clear();
				showManageSchemeWindow(player, targettype);
			}
		}
		else if (currentCommand.startsWith("removebuff"))
		{
			ArrayList<Buff> buffs;
			
			String BuffId = st.nextToken();
			int getBuffId = Integer.parseInt(BuffId);
			String targettype = st.nextToken();
			
			L2Character target = player;
			if (targettype.equalsIgnoreCase("pet"))
			{
				target = player.getPet();
			}
			
			if (target != null)
			{
				buffs = getOwnBuffs(target.getObjectId());
				L2Skill skill = SkillTable.getInstance().getInfo(buffs.get(getBuffId)._skillId, buffs.get(getBuffId)._skillLevel);
				player.sendMessage("Removed buff: " + skill.getName() + " Level: " + buffs.get(getBuffId)._skillLevel);
				player.sendPacket(new ExShowScreenMessage("Removed buff: " + skill.getName() + " Level: " + buffs.get(getBuffId)._skillLevel, 2000, 2, false));
				buffs.remove(Integer.parseInt(BuffId));
			}
			
			showManageSchemeWindow(player, targettype);
		}
		else if (currentCommand.startsWith("cleanbuffs"))
		{
			ArrayList<Buff> buffs;
			
			String targettype = st.nextToken();
			
			L2Character target = player;
			if (targettype.equalsIgnoreCase("pet"))
			{
				target = player.getPet();
			}
			
			if (target != null)
			{
				buffs = getOwnBuffs(target.getObjectId());
				buffs.clear();
			}
			
			showManageSchemeWindow(player, targettype);
		}
		else if (currentCommand.startsWith("deletescheme"))
		{
			String name = st.nextToken();
			String targetType = st.nextToken();
			
			if (BuffTable.getInstance().getAllSchemes(player.getObjectId()) != null && BuffTable.getInstance().getAllSchemes(player.getObjectId()).containsKey(name))
			{
				BuffTable.getInstance().getAllSchemes(player.getObjectId()).remove(name);
				showManageSchemeWindow(player, targetType);
			}
		}
		else if (currentCommand.startsWith("fighterbuff") || currentCommand.startsWith("magebuff"))
		{
			ArrayList<L2Skill> skills_to_buff = new ArrayList<>();
			
			if (currentCommand.startsWith("magebuff"))
			{
				for (int skillId : PowerPackConfig.MAGE_SKILL_LIST.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.MAGE_SKILL_LIST.get(skillId));
					if (skill != null)
					{
						skills_to_buff.add(skill);
					}
				}
			}
			else
			{
				for (int skillId : PowerPackConfig.FIGHTER_SKILL_LIST.keySet())
				{
					L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.FIGHTER_SKILL_LIST.get(skillId));
					if (skill != null)
					{
						skills_to_buff.add(skill);
					}
				}
			}
			
			String targettype = "";
			if (st.hasMoreTokens())
			{
				targettype = st.nextToken();
			}
			
			int cost = 0;
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				
				if (PowerPackConfig.BUFFER_PRICE > 0)
				{
					cost = PowerPackConfig.BUFFER_PRICE * skills_to_buff.size();
				}
				
				if (player.getInventory().getAdena() < cost)
				{
					player.sendMessage("You don't have enough Adena.");
					player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 1000, 2, false));
					player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
					return;
				}
			}
			
			L2Character target = player;
			if (targettype.equalsIgnoreCase("pet"))
			{
				target = player.getPet();
			}
			
			if (target != null)
			{
				for (L2Skill sk : skills_to_buff)
				{
					sk.getEffects(target, target, false, false, false);
				}
				
				if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
				{
					player.reduceAdena("NPC Buffer", cost, null, true);
				}
			}
			else
			{
				player.sendMessage("Incorrect Pet");
			}
		}
	}
	
	private static String[] _BBSCommand =
	{
		"bbsyoubuff"
	};
	
	@Override
	public String[] getBBSCommands()
	{
		return _BBSCommand;
	}
	
	private void showManageSchemeWindow(L2PcInstance player, String targetType)
	{
		ArrayList<Buff> buffs = getOwnBuffs(player.getObjectId());
		int color = 1;
		String cost;
		String available = null;
		int schemes = 0;
		
		if (BuffTable.getInstance().getAllSchemes(player.getObjectId()) != null)
		{
			schemes = BuffTable.getInstance().getAllSchemes(player.getObjectId()).size();
		}
		
		TextBuilder tb = new TextBuilder();
		tb.append("<html><title>Schemes for " + targetType + "</title>");
		tb.append("<body>");
		
		int countBuffs = 0;
		
		if (buffs.size() > 0)
		{
			tb.append("<br><center>Edit your buffs set (click on icon):</center>");
			
			tb.append("<table with=\"300\"><tr>");
			for (Buff buff : buffs)
			{
				int skillId = buff._skillId;
				String BuffId = String.valueOf(buff._skillId);
				
				if (skillId < 1000)
				{
					BuffId = "0" + BuffId;
				}
				
				if (skillId < 100)
				{
					BuffId = "0" + BuffId;
				}
				
				if (skillId == 4551 || skillId == 4552 || skillId == 4553 || skillId == 4554)
				{
					BuffId = "1164";
				}
				
				if (skillId == 4702 || skillId == 4703)
				{
					BuffId = "1332";
				}
				
				if (skillId == 4699 || skillId == 4700)
				{
					BuffId = "1331";
				}
				
				countBuffs++;
				
				tb.append("<td><button action=\"bypass -h custom_doyoubuff removebuff " + (countBuffs - 1) + " " + targetType + "\" width=32 height=32 back=\"icon.skill" + BuffId + "\" fore=\"icon.skill" + BuffId + "\"></td>");
				
				if (countBuffs == 8 || countBuffs == 16 || countBuffs == 24 || countBuffs == 32 || countBuffs == 40 || countBuffs == 48 || countBuffs == 56 || countBuffs == 64)
				{
					tb.append("</tr></table><table><tr>");
				}
			}
			tb.append("</tr></table>");
		}
		
		tb.append("<center>");
		
		if ((PowerPackConfig.NPCBUFFER_MAX_SCHEMES - schemes) > 0)
		{
			available = "(Available: <font color=\"009900\">" + (PowerPackConfig.NPCBUFFER_MAX_SCHEMES - schemes) + "</font>)";
		}
		else
		{
			available = "(Available: <font color=\"ff0000\">0</font>)";
		}
		
		if ((PowerPackConfig.NPCBUFFER_MAX_SCHEMES - schemes) > 0)
		{
			tb.append("<br>Save your buffs set (<font color=LEVEL>" + buffs.size() + "</font>) as scheme:");
			tb.append("<table><tr>");
			tb.append("<td><button value=\"Clean\" action=\"bypass -h custom_doyoubuff cleanbuffs " + targetType + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
			tb.append("<td><edit var=\"name\" width=150 height=15 length=14 type=text></td>");
			tb.append("<td><button value=\"Save\" action=\"bypass -h custom_doyoubuff createscheme $name " + targetType + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
			tb.append("</tr>");
			tb.append("</table>");
			tb.append("<br><br>");
		}
		
		tb.append("<center>Your schemes " + available + ":</center>");
		
		if (BuffTable.getInstance().getAllSchemes(player.getObjectId()) != null)
		{
			for (Entry<String, ArrayList<Scheme>> e = BuffTable.getInstance().getAllSchemes(player.getObjectId()).head(), end = BuffTable.getInstance().getAllSchemes(player.getObjectId()).tail(); (e = e.getNext()) != end;)
			{
				String schemeName = e.getKey();
				String cap = schemeName.substring(0, 1).toUpperCase() + schemeName.substring(1);
				if (color == 1)
				{
					cost = getFee(e.getValue());
					tb.append("<table width=300 border=0 bgcolor=000000><tr>");
					tb.append("<tr>");
					tb.append("<td width=\"140\"><font color=009900>" + cap + "</font></td>");
					tb.append("<td></td>");
					tb.append("<td></td>");
					tb.append("</tr>");
					tb.append("<tr>");
					tb.append("<td width=\"140\">Buffs: <font color=LEVEL>" + String.valueOf(BuffTable.getInstance().getScheme(player.getObjectId(), e.getKey()).size()) + "</font> Prize: <font color=LEVEL>" + cost + "</font></td>");
					tb.append("<td><button value=\"Load\" action=\"bypass -h custom_doyoubuff givebuffs " + targetType + " " + e.getKey() + " " + String.valueOf(cost) + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
					tb.append("<td><button value=\"Delete\" action=\"bypass -h custom_doyoubuff deletescheme " + e.getKey() + " " + targetType + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
					tb.append("</tr>");
					tb.append("</table>");
					color = 2;
				}
				else
				{
					cost = getFee(e.getValue());
					tb.append("<table width=300 border=0><tr>");
					tb.append("<tr>");
					tb.append("<td width=\"140\"><font color=009900>" + cap + "</font></td>");
					tb.append("<td></td>");
					tb.append("<td></td>");
					tb.append("</tr>");
					tb.append("<tr>");
					tb.append("<td width=\"140\">Buffs: <font color=LEVEL>" + String.valueOf(BuffTable.getInstance().getScheme(player.getObjectId(), e.getKey()).size()) + "</font> Prize: <font color=LEVEL>" + cost + "</font></td>");
					tb.append("<td><button value=\"Load\" action=\"bypass -h custom_doyoubuff givebuffs " + targetType + " " + e.getKey() + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
					tb.append("<td><button value=\"Delete\" action=\"bypass -h custom_doyoubuff deletescheme " + e.getKey() + " " + targetType + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td>");
					tb.append("</tr>");
					tb.append("</table>");
					color = 1;
				}
			}
		}
		
		if (schemes == 0)
		{
			tb.append("<br><font color=\"ff0000\">Empty</font><br>");
		}
		
		tb.append("<br><br>");
		
		tb.append("<button value=\"Back\" action=\"bypass custom_doyoubuff Chat 0\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		tb.append("</center>");
		tb.append("</body></html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(tb.toString());
		player.sendPacket(html);
	}
	
	private String getFee(ArrayList<Scheme> list)
	{
		int fee = 0;
		
		for (Scheme sk : list)
		{
			fee += sk._itemCount;
		}
		
		return Util.formatAdena(fee);
	}
	
	public void showBuffs(L2PcInstance player)
	{
		int count = 0;
		NpcHtmlMessage ms = new NpcHtmlMessage(1);
		ms.setFile(PARENT_DIR + "buffer-player-remove.htm");
		
		ms.replace("%name%", player.getName());
		
		TextBuilder t = new TextBuilder();
		
		L2Effect[] effects = player.getAllEffects();
		
		t.append("<table with=\"300\"><tr>");
		
		for (L2Effect e : effects)
		{
			if (e != null && e.getEffectType() == L2Effect.EffectType.BUFF)
			{
				count++;
				
				int skillId = e.getSkill().getId();
				String BuffId = String.valueOf(e.getSkill().getId());
				
				if (skillId < 1000)
				{
					BuffId = "0" + BuffId;
				}
				
				if (skillId < 100)
				{
					BuffId = "0" + BuffId;
				}
				if (skillId == 4551 || skillId == 4552 || skillId == 4553 || skillId == 4554)
				{
					BuffId = "1164";
				}
				
				if (skillId == 4702 || skillId == 4703)
				{
					BuffId = "1332";
				}
				
				if (skillId == 4699 || skillId == 4700)
				{
					BuffId = "1331";
				}
				
				t.append("<td><button action=\"bypass -h custom_doyoubuff RemoveOne " + skillId + "\" width=32 height=32 back=\"Icon.skill" + BuffId + "\" fore=\"Icon.skill" + BuffId + "\"></td>");
				
				if (count == 8 || count == 16 || count == 24 || count == 32 || count == 40 || count == 48 || count == 56 || count == 64)
				{
					t.append("</tr></table><table><tr>");
				}
			}
		}
		t.append("</tr></table>");
		
		ms.replace("%buffs%", t.toString());
		player.sendPacket(ms);
	}
	
	private void removeBuff(L2PcInstance player, int SkillId)
	{
		if (player != null && SkillId > 0)
		{
			L2Effect[] effects = player.getAllEffects();
			for (L2Effect e : effects)
			{
				if (e != null && e.getSkill().getId() == SkillId)
				{
					e.exit(true);
					player.sendMessage("Removed buff: " + e.getSkill().getName() + " Level: " + e.getSkill().getLevel());
					player.sendPacket(new ExShowScreenMessage("Removed buff: " + e.getSkill().getName() + " Level: " + e.getSkill().getLevel(), 2000, 2, false));
				}
			}
			showBuffs(player);
		}
	}
	
	public void showBuffsPet(L2PcInstance player)
	{
		if (player.getPet() == null)
		{
			return;
		}
		
		int count = 0;
		NpcHtmlMessage ms = new NpcHtmlMessage(1);
		ms.setFile(PARENT_DIR + "buffer-pet-remove.htm");
		
		ms.replace("%name%", player.getPet().getName());
		
		TextBuilder t = new TextBuilder();
		
		L2Effect[] effects = player.getPet().getAllEffects();
		
		t.append("<table with=\"300\"><tr>");
		
		for (L2Effect e : effects)
		{
			if (e != null && e.getEffectType() == L2Effect.EffectType.BUFF)
			{
				count++;
				
				int skillId = e.getSkill().getId();
				String BuffId = String.valueOf(e.getSkill().getId());
				
				if (skillId < 1000)
				{
					BuffId = "0" + BuffId;
				}
				
				if (skillId < 100)
				{
					BuffId = "0" + BuffId;
				}
				
				if (skillId == 4551 || skillId == 4552 || skillId == 4553 || skillId == 4554)
				{
					BuffId = "1164";
				}
				
				if (skillId == 4702 || skillId == 4703)
				{
					BuffId = "1332";
				}
				
				if (skillId == 4699 || skillId == 4700)
				{
					BuffId = "1331";
				}
				
				t.append("<td><button action=\"bypass -h custom_doyoubuff RemovePetOne " + skillId + "\" width=32 height=32 back=\"Icon.skill" + BuffId + "\" fore=\"Icon.skill" + BuffId + "\"></td>");
				
				if (count == 8 || count == 16 || count == 24 || count == 32 || count == 40 || count == 48 || count == 56 || count == 64)
				{
					t.append("</tr></table><table><tr>");
				}
			}
		}
		
		ms.replace("%buffs%", t.toString());
		player.sendPacket(ms);
	}
	
	private void removeBuffPet(L2PcInstance player, int SkillId)
	{
		if (player.getPet() == null)
		{
			return;
		}
		
		if (SkillId > 0)
		{
			L2Effect[] effects = player.getPet().getAllEffects();
			for (L2Effect e : effects)
			{
				if (e != null && e.getSkill().getId() == SkillId)
				{
					e.exit(true);
					player.sendMessage("Removed buff: " + e.getSkill().getName() + " Level: " + e.getSkill().getLevel());
					player.sendPacket(new ExShowScreenMessage("Removed buff: " + e.getSkill().getName() + " Level: " + e.getSkill().getLevel(), 2000, 2, false));
				}
			}
			showBuffsPet(player);
		}
	}
	
	public String getItemNameById(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		
		String itemName = "NoName";
		
		if (itemId != 0)
		{
			itemName = item.getName();
		}
		
		return itemName;
	}
}
