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
import java.util.concurrent.ConcurrentHashMap;

import javolution.text.TextBuilder;
import javolution.util.FastMap;
import javolution.util.FastMap.Entry;
import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.community.manager.BaseBBSManager;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.handler.ICommunityBoardHandler;
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
import l2jorion.game.powerpack.buffer.BuffsTable.Buff;
import l2jorion.game.powerpack.buffer.BuffsTable.Scheme;
import l2jorion.game.taskmanager.AttackStanceTaskManager;
import l2jorion.game.templates.L2Item;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Buffer implements IVoicedCommandHandler, ICustomByPassHandler, ICommunityBoardHandler
{
	protected static Logger LOG = LoggerFactory.getLogger(Buffer.class);
	
	private static final String PARENT_DIR = "data/html/buffer/";
	
	public static Map<Integer, String> _visitedPages = new ConcurrentHashMap<>();
	private String lastUsedTarget = "";
	
	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			PowerPackConfig.BUFFER_COMMAND,
		};
	}
	
	private boolean checkAllowed(L2PcInstance activeChar)
	{
		if (activeChar.isGM())
		{
			return true;
		}
		
		String msg = null;
		
		if (PowerPackConfig.BUFFER_EXCLUDE_ON.contains("PREMIUM") && activeChar.getPremiumService() == 0)
		{
			msg = "This feature is only available for The Premium Account.";
		}
		else if (activeChar.isSitting())
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
			msg = "Buffer is not available for player with KARMA.";
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
			activeChar.sendPacket(new ExShowScreenMessage(msg, 2000, 2, false));
			activeChar.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
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
		
		if (command.startsWith(PowerPackConfig.BUFFER_COMMAND))
		{
			String text = HtmCache.getInstance().getHtm(PARENT_DIR + "buffer.htm");
			NpcHtmlMessage htm = new NpcHtmlMessage(1);
			htm.setHtml(text);
			activeChar.sendPacket(htm);
			
			synchronized (_visitedPages)
			{
				_visitedPages.put(activeChar.getObjectId(), String.valueOf(0));
			}
		}
		
		return true;
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
		
		if (player.isDead() || player.isAlikeDead())
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
		
		// _visitedPages.clear();
		
		if (parameters.contains("Pet"))
		{
			if (player.getPet() == null)
			{
				player.sendMessage("You have not a summoned pet.");
				player.sendPacket(new ExShowScreenMessage("You have not a summoned pet.", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				returnHtm(command, "Chat", player);
				return;
			}
		}
		
		StringTokenizer st = new StringTokenizer(parameters, " ");
		String currentCommand = st.nextToken();
		
		if (parameters.equals("RemoveMenu"))
		{
			showBuffs(command, player);
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
				removeBuff(command, player, SkillId);
			}
		}
		else if (parameters.equals("RemoveAll"))
		{
			final L2Effect[] effects = player.getAllEffects();
			
			for (L2Effect e : effects)
			{
				if (e.getEffectType() == L2Effect.EffectType.BUFF)
				{
					player.removeEffect(e);
				}
			}
			
			returnHtm(command, "Chat", player);
		}
		else if (parameters.equals("RemovePetMenu"))
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
		else if (parameters.equals("RemovePetAll"))
		{
			final L2Effect[] effects = player.getPet().getAllEffects();
			
			for (L2Effect e : effects)
			{
				if (e.getEffectType() == L2Effect.EffectType.BUFF)
				{
					player.getPet().removeEffect(e);
				}
			}
			
			returnHtm(command, "Chat", player);
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
				text = text.replaceAll("custom_doyoubuff", "bbs_bbsyoubuff");
				text = text.replaceAll("<title>", "<br><center>");
				text = text.replaceAll("</title>", "</center>");
				BaseBBSManager.separateAndSend(text, player, (Config.LIFEDRAIN_CUSTOM ? true : false));
			}
			else
			{
				NpcHtmlMessage htm = new NpcHtmlMessage(1);
				htm.setHtml(text);
				player.sendPacket(htm);
			}
		}
		else if (parameters.startsWith("RestoreAll"))
		{
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				returnHtm(command, "Chat", player);
				return;
			}
			
			player.getStatus().setCurrentCp(player.getMaxCp());
			player.getStatus().setCurrentMp(player.getMaxMp());
			player.getStatus().setCurrentHp(player.getMaxHp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
			
			returnHtm(command, "Chat", player);
		}
		else if (parameters.startsWith("RestorePetAll"))
		{
			if (player.getPet() == null)
			{
				player.sendMessage("You have not a summoned pet.");
				player.sendPacket(new ExShowScreenMessage("You have not a summoned pet.", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				returnHtm(command, "Chat", player);
				return;
			}
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL && player.getInventory().getAdena() < PowerPackConfig.BUFFER_PRICE * 3)
			{
				player.sendMessage("You don't have enough Adena.");
				player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				returnHtm(command, "Chat", player);
				return;
			}
			
			player.getPet().getStatus().setCurrentMp(player.getPet().getMaxMp());
			player.getPet().getStatus().setCurrentHp(player.getPet().getMaxHp());
			
			if (player.getLevel() > PowerPackConfig.BUFFER_FREE_LVL)
			{
				player.reduceAdena("Buff", PowerPackConfig.BUFFER_PRICE * 3, null, true);
			}
			
			returnHtm(command, "Chat", player);
		}
		else if (parameters.startsWith("MakeBuffs"))
		{
			String buffName = parameters.substring(9).trim();
			
			ArrayList<Buff> buffs = null;
			
			buffs = BuffsTable.getInstance().getBuffsForName(buffName);
			
			if (buffs != null && buffs.size() == 1)
			{
				if (!player.getOwnBuffs(player.getObjectId()).contains(buffs.get(0)))
				{
					if (player.getOwnBuffs(player.getObjectId()).size() < getMaxBuffCount(player)) // PowerPackConfig.NPCBUFFER_MAX_SKILLS)
					{
						player.getOwnBuffs(player.getObjectId()).add(buffs.get(0));
					}
					
					if (player.getOwnBuffs(player.getObjectId()).size() > getMaxBuffCount(player) || player.getOwnBuffs(player.getObjectId()).size() == getMaxBuffCount(player))
					{
						player.sendMessage("This set has reached maximun amount of allowed buffs: " + getMaxBuffCount(player) + ".");
						player.sendPacket(new ExShowScreenMessage("This set has reached maximun amount of allowed buffs: " + getMaxBuffCount(player) + ".", 2000, 2, false));
						player.sendPacket(new PlaySound("ItemSound2.broken_key"));
						showManageSchemeWindow(command, player, "player");
						return;
					}
					
					player.sendPacket(new ExShowScreenMessage("Buffs set: " + player.getOwnBuffs(player.getObjectId()).size(), 2000, 3, false));
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
									player.sendMessage("You don't have enough " + L2Item.getItemNameById(buff._itemId) + ".");
									player.sendPacket(new ExShowScreenMessage("You don't have enough " + L2Item.getItemNameById(buff._itemId) + ".", 2000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
								
								player.destroyItem("Consume", item.getObjectId(), buff._itemCount, null, true);
							}
						}
						
						if (buff._premium)
						{
							if (player.getPremiumService() == 0 && PowerPackConfig.BUFFER_PREMIUM_ITEM_ID == 0)
							{
								player.sendMessage("You're not The Premium account.");
								player.sendPacket(new ExShowScreenMessage("You're not The Premium account.", 2000, 2, false));
								player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								continue;
							}
							else if (PowerPackConfig.BUFFER_PREMIUM_ITEM_ID != 0)
							{
								L2ItemInstance item = player.getInventory().getItemByItemId(PowerPackConfig.BUFFER_PREMIUM_ITEM_ID);
								if (item == null)
								{
									player.sendMessage("You don't have a special item.");
									player.sendPacket(new ExShowScreenMessage("You don't have a special item.", 2000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
							}
						}
						
						if (buff._voter && (player.eligibleToVoteHop() || player.eligibleToVoteTop() || player.eligibleToVoteNet() || player.eligibleToVoteBra()))
						{
							player.sendMessage("You can't get this buff, because you didn't vote yet.");
							player.sendPacket(new ExShowScreenMessage("You can't get this buff, because you didn't vote yet.", 2000, 2, false));
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
				}
			}
			
			returnHtm(command, "Chat", player);
		}
		else if (parameters.startsWith("MakePetBuffs"))
		{
			if (player.getPet() == null)
			{
				player.sendMessage("You have not a summoned pet.");
				player.sendPacket(new ExShowScreenMessage("You have not a summoned pet.", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				returnHtm(command, "Chat", player);
				return;
			}
			
			String buffName = parameters.substring(12).trim();
			
			ArrayList<Buff> buffs = null;
			
			if (parameters.startsWith("RestorePetBuffs"))
			{
				buffs = player.getOwnBuffs(player.getPet().getObjectId());
			}
			else
			{
				buffs = BuffsTable.getInstance().getBuffsForName(buffName);
			}
			
			if (buffs != null && buffs.size() == 1)
			{
				if (!player.getOwnBuffs(player.getPet().getObjectId()).contains(buffs.get(0)))
				{
					if (player.getOwnBuffs(player.getPet().getObjectId()).size() < getMaxBuffCount(player))
					{
						player.getOwnBuffs(player.getPet().getObjectId()).add(buffs.get(0));
					}
					
					if (player.getOwnBuffs(player.getPet().getObjectId()).size() > getMaxBuffCount(player) || player.getOwnBuffs(player.getPet().getObjectId()).size() == getMaxBuffCount(player))
					{
						player.sendMessage("This set has reached maximun amount of allowed buffs: " + getMaxBuffCount(player) + ".");
						player.sendPacket(new ExShowScreenMessage("This set has reached maximun amount of allowed buffs: " + getMaxBuffCount(player) + ".", 2000, 2, false));
						player.sendPacket(new PlaySound("ItemSound2.broken_key"));
						showManageSchemeWindow(command, player, "pet");
						return;
					}
					
					player.sendPacket(new ExShowScreenMessage("Buffs set: " + player.getOwnBuffs(player.getPet().getObjectId()).size(), 2000, 3, false));
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
									player.sendPacket(new ExShowScreenMessage("You don't have enough " + getItemNameById(buff._itemId) + ".", 2000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
								
								player.destroyItem("Consume", item.getObjectId(), buff._itemCount, null, true);
							}
						}
						
						if (buff._premium)
						{
							if (player.getPremiumService() == 0 && PowerPackConfig.BUFFER_PREMIUM_ITEM_ID == 0)
							{
								player.sendMessage("You're not The Premium account.");
								player.sendPacket(new ExShowScreenMessage("You're not The Premium account.", 2000, 2, false));
								player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								continue;
							}
							else if (PowerPackConfig.BUFFER_PREMIUM_ITEM_ID != 0)
							{
								L2ItemInstance item = player.getInventory().getItemByItemId(PowerPackConfig.BUFFER_PREMIUM_ITEM_ID);
								if (item == null)
								{
									player.sendMessage("You don't have a special item.");
									player.sendPacket(new ExShowScreenMessage("You don't have a special item.", 2000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
							}
						}
						
						if (buff._voter && (player.eligibleToVoteHop() || player.eligibleToVoteTop() || player.eligibleToVoteNet() || player.eligibleToVoteBra()))
						{
							player.sendMessage("You can't get this buff, because you didn't vote yet.");
							player.sendPacket(new ExShowScreenMessage("You can't get this buff, because you didn't vote yet.", 2000, 2, false));
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
				}
			}
			
			returnHtm(command, "Chat", player);
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
					for (Scheme sk : BuffsTable.getInstance().getScheme(player.getObjectId(), scheme_key))
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
									player.sendPacket(new ExShowScreenMessage("You don't have enough " + getItemNameById(sk._itemId) + ".", 2000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
								
								player.destroyItem("Consume", item.getObjectId(), sk._itemCount, null, true);
							}
						}
						
						if (sk._premium)
						{
							if (player.getPremiumService() == 0 && PowerPackConfig.BUFFER_PREMIUM_ITEM_ID == 0)
							{
								player.sendMessage("You're not The Premium account.");
								player.sendPacket(new ExShowScreenMessage("You're not The Premium account.", 2000, 2, false));
								player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
								continue;
							}
							else if (PowerPackConfig.BUFFER_PREMIUM_ITEM_ID != 0)
							{
								L2ItemInstance item = player.getInventory().getItemByItemId(PowerPackConfig.BUFFER_PREMIUM_ITEM_ID);
								if (item == null)
								{
									player.sendMessage("You don't have a special item.");
									player.sendPacket(new ExShowScreenMessage("You don't have a special item.", 2000, 2, false));
									player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
									continue;
								}
							}
						}
						
						if (sk._voter && (player.eligibleToVoteHop() || player.eligibleToVoteTop() || player.eligibleToVoteNet() || player.eligibleToVoteBra()))
						{
							player.sendMessage("You can't get this buff, because you didn't vote yet.");
							player.sendPacket(new ExShowScreenMessage("You can't get this buff, because you didn't vote yet.", 2000, 2, false));
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
			
			showManageSchemeWindow(command, player, targettype);
		}
		else if (currentCommand.startsWith("manageschemes"))
		{
			String targetType = st.nextToken();
			lastUsedTarget = targetType;
			showManageSchemeWindow(command, player, targetType);
		}
		else if (currentCommand.startsWith("createscheme"))
		{
			ArrayList<Buff> buffs = player.getOwnBuffs(player.getObjectId());
			
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
				showManageSchemeWindow(command, player, targettype);
				return;
			}
			
			if (name.isEmpty() || name.length() < 2 || name.length() > 14)
			{
				player.sendMessage("Error: Scheme's name must contain 2-14 chars without any space.");
				showManageSchemeWindow(command, player, targettype);
			}
			else if (BuffsTable.getInstance().getAllSchemes(player.getObjectId()) != null && BuffsTable.getInstance().getAllSchemes(player.getObjectId()).size() == PowerPackConfig.NPCBUFFER_MAX_SCHEMES)
			{
				player.sendMessage("Error: Maximun schemes amount reached, please delete one before creating a new one.");
				showManageSchemeWindow(command, player, targettype);
			}
			else if (BuffsTable.getInstance().getAllSchemes(player.getObjectId()) != null && BuffsTable.getInstance().getAllSchemes(player.getObjectId()).containsKey(name))
			{
				player.sendMessage("Error: duplicate entry. Please use another name.");
				showManageSchemeWindow(command, player, targettype);
			}
			else
			{
				if (BuffsTable.getInstance().getAllSchemes(player.getObjectId()) == null)
				{
					BuffsTable.getInstance().getSchemesTable().put(player.getObjectId(), new FastMap<String, ArrayList<Scheme>>(PowerPackConfig.NPCBUFFER_MAX_SCHEMES + 1));
				}
				
				BuffsTable.getInstance().setScheme(player.getObjectId(), name.trim(), new ArrayList<Scheme>(getMaxBuffCount(player) + 1));
				
				for (Buff buff : buffs)
				{
					if (BuffsTable.getInstance() != null && BuffsTable.getInstance().getScheme(player.getObjectId(), name) != null && BuffsTable.getInstance().getScheme(player.getObjectId(), name).size() < getMaxBuffCount(player))
					{
						BuffsTable.getInstance().getScheme(player.getObjectId(), name).add(new Scheme(player.getObjectId(), buff._skillId, buff._skillLevel, buff._premium, buff._voter, buff._useItem, buff._itemId, buff._itemCount, name));
					}
					else
					{
						player.sendMessage("This scheme has reached maximun amount of buffs.");
					}
				}
				
				buffs.clear();
				showManageSchemeWindow(command, player, targettype);
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
				buffs = player.getOwnBuffs(target.getObjectId());
				L2Skill skill = SkillTable.getInstance().getInfo(buffs.get(getBuffId)._skillId, buffs.get(getBuffId)._skillLevel);
				player.sendMessage("Removed buff: " + skill.getName() + " Level: " + buffs.get(getBuffId)._skillLevel);
				player.sendPacket(new ExShowScreenMessage("Removed buff: " + skill.getName() + " Level: " + buffs.get(getBuffId)._skillLevel, 2000, 2, false));
				buffs.remove(Integer.parseInt(BuffId));
			}
			
			showManageSchemeWindow(command, player, targettype);
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
				buffs = player.getOwnBuffs(target.getObjectId());
				buffs.clear();
			}
			
			showManageSchemeWindow(command, player, targettype);
		}
		else if (currentCommand.startsWith("deletescheme"))
		{
			String name = st.nextToken();
			String targetType = st.nextToken();
			
			if (BuffsTable.getInstance().getAllSchemes(player.getObjectId()) != null && BuffsTable.getInstance().getAllSchemes(player.getObjectId()).containsKey(name))
			{
				BuffsTable.getInstance().getAllSchemes(player.getObjectId()).remove(name);
				showManageSchemeWindow(command, player, targetType);
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
					player.sendPacket(new ExShowScreenMessage("You don't have enough Adena.", 2000, 2, false));
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
				player.sendMessage("You have not a summoned pet.");
				player.sendPacket(new ExShowScreenMessage("You have not a summoned pet.", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
			}
			
			returnHtm(command, "Chat", player);
		}
	}
	
	@Override
	public String[] getBypassBbsCommands()
	{
		return new String[]
		{
			"bbsyoubuff"
		};
	}
	
	private void showManageSchemeWindow(String command, L2PcInstance player, String targetType)
	{
		ArrayList<Buff> buffs = player.getOwnBuffs(player.getObjectId());
		int color = 1;
		String cost;
		String available = null;
		int schemes = 0;
		
		if (BuffsTable.getInstance().getAllSchemes(player.getObjectId()) != null)
		{
			schemes = BuffsTable.getInstance().getAllSchemes(player.getObjectId()).size();
		}
		
		TextBuilder tb = new TextBuilder();
		tb.append("<html>");
		tb.append("<body>");
		tb.append("<br><center>Scheme for: <font color=\"009900\">" + targetType + "</font></center><br>");
		
		int countBuffs = 0;
		
		if (buffs.size() > 0)
		{
			tb.append("<br><center>Delete buff (click on icon):</center>");
			
			tb.append("<table with=\"300\"><tr>");
			for (Buff buff : buffs)
			{
				int skillId = buff._skillId;
				String iconId = String.valueOf(buff._skillId);
				
				if (skillId < 1000)
				{
					iconId = "0" + iconId;
				}
				
				if (skillId < 100)
				{
					iconId = "0" + iconId;
				}
				
				if (skillId == 4551 || skillId == 4552 || skillId == 4553 || skillId == 4554)
				{
					iconId = "1164";
				}
				
				if (skillId == 4702 || skillId == 4703)
				{
					iconId = "1332";
				}
				
				if (skillId == 4699 || skillId == 4700)
				{
					iconId = "1331";
				}
				
				countBuffs++;
				
				tb.append("<td><button action=\"bypass -h custom_doyoubuff removebuff " + (countBuffs - 1) + " " + targetType + "\" width=32 height=32 back=\"icon.skill" + iconId + "\" fore=\"icon.skill" + iconId + "\"></td>");
				
				if (countBuffs == 8)
				{
					countBuffs = 0;
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
		
		if (BuffsTable.getInstance().getAllSchemes(player.getObjectId()) != null)
		{
			for (Entry<String, ArrayList<Scheme>> e = BuffsTable.getInstance().getAllSchemes(player.getObjectId()).head(), end = BuffsTable.getInstance().getAllSchemes(player.getObjectId()).tail(); (e = e.getNext()) != end;)
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
					tb.append("<td width=\"140\">Buffs: <font color=LEVEL>" + String.valueOf(BuffsTable.getInstance().getScheme(player.getObjectId(), e.getKey()).size()) + "</font> Prize: <font color=LEVEL>" + cost + "</font></td>");
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
					tb.append("<td width=\"140\">Buffs: <font color=LEVEL>" + String.valueOf(BuffsTable.getInstance().getScheme(player.getObjectId(), e.getKey()).size()) + "</font> Prize: <font color=LEVEL>" + cost + "</font></td>");
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
		
		if (command.startsWith("bbsyoubuff"))
		{
			String text = tb.toString().replaceAll("custom_doyoubuff", "bbs_bbsyoubuff");
			text = text.replaceAll("<title>", "<br><center>");
			text = text.replaceAll("</title>", "</center>");
			BaseBBSManager.separateAndSend(text, player);
		}
		else
		{
			NpcHtmlMessage htm = new NpcHtmlMessage(1);
			htm.setHtml(tb.toString());
			player.sendPacket(htm);
		}
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
	
	// XXX showBuffs
	public void showBuffs(String command, L2PcInstance player)
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
				String iconId = String.valueOf(e.getSkill().getId());
				
				if (skillId < 1000)
				{
					iconId = "0" + iconId;
				}
				
				if (skillId < 100)
				{
					iconId = "0" + iconId;
				}
				if (skillId == 4551 || skillId == 4552 || skillId == 4553 || skillId == 4554)
				{
					iconId = "1164";
				}
				
				if (skillId == 4702 || skillId == 4703)
				{
					iconId = "1332";
				}
				
				if (skillId == 4699 || skillId == 4700)
				{
					iconId = "1331";
				}
				
				t.append("<td><button action=\"bypass -h custom_doyoubuff RemoveOne " + skillId + "\" width=32 height=32 back=\"Icon.skill" + iconId + "\" fore=\"Icon.skill" + iconId + "\"></td>");
				
				if (count == 8)
				{
					count = 0;
					t.append("</tr></table><table><tr>");
				}
			}
		}
		t.append("</tr></table>");
		
		if (command.startsWith("bbsyoubuff"))
		{
			String html = ms.getContent().replace("%buffs%", t.toString());
			html = html.replaceAll("custom_doyoubuff", "bbs_bbsyoubuff");
			html = html.replaceAll("<title>", "<br><center>");
			html = html.replaceAll("</title>", "</center>");
			BaseBBSManager.separateAndSend(html, player);
		}
		else
		{
			ms.replace("%buffs%", t.toString());
			player.sendPacket(ms);
		}
	}
	
	private void removeBuff(String command, L2PcInstance player, int SkillId)
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
			showBuffs(command, player);
		}
	}
	
	public void showBuffsPet(L2PcInstance player)
	{
		if (player.getPet() == null)
		{
			player.sendMessage("You have not a summoned pet.");
			player.sendPacket(new ExShowScreenMessage("You have not a summoned pet.", 2000, 2, false));
			player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
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
				String iconId = String.valueOf(e.getSkill().getId());
				
				if (skillId < 1000)
				{
					iconId = "0" + iconId;
				}
				
				if (skillId < 100)
				{
					iconId = "0" + iconId;
				}
				
				if (skillId == 4551 || skillId == 4552 || skillId == 4553 || skillId == 4554)
				{
					iconId = "1164";
				}
				
				if (skillId == 4702 || skillId == 4703)
				{
					iconId = "1332";
				}
				
				if (skillId == 4699 || skillId == 4700)
				{
					iconId = "1331";
				}
				
				t.append("<td><button action=\"bypass -h custom_doyoubuff RemovePetOne " + skillId + "\" width=32 height=32 back=\"Icon.skill" + iconId + "\" fore=\"Icon.skill" + iconId + "\"></td>");
				
				if (count == 8)
				{
					count = 0;
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
			if (player.getPet() == null)
			{
				player.sendMessage("You have not a summoned pet.");
				player.sendPacket(new ExShowScreenMessage("You have not a summoned pet.", 2000, 2, false));
				player.sendPacket(new PlaySound("ItemSound3.sys_impossible"));
				return;
			}
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
	
	public void returnHtm(String command, String param, L2PcInstance player)
	{
		if (_visitedPages.get(player.getObjectId()) != null)
		{
			handleCommand(command, player, param + " " + _visitedPages.get(player.getObjectId()));
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
	
	public int getMaxBuffCount(L2Character player)
	{
		return Config.BUFFS_MAX_AMOUNT + Math.max(0, player.getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
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
}
