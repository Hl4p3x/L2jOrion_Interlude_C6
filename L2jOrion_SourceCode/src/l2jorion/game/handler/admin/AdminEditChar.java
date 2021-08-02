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
package l2jorion.game.handler.admin;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.sql.ClanTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.BlockList;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.PartySmallWindowAll;
import l2jorion.game.network.serverpackets.PartySmallWindowDeleteAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListAll;
import l2jorion.game.network.serverpackets.PledgeShowMemberListUpdate;
import l2jorion.game.network.serverpackets.SetSummonRemainTime;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminEditChar implements IAdminCommandHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(AdminEditChar.class);
	private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
	
	private static String[] ADMIN_COMMANDS =
	{
		"admin_changename", // changes char name
		"admin_setname", // changes char name
		"admin_edit_character",
		"admin_current_player",
		"admin_nokarma",
		"admin_setkarma",
		"admin_character_list", // same as character_info, kept for compatibility purposes
		"admin_character_info", // given a player name, displays an information window
		"admin_show_characters",
		"admin_find_character",
		"admin_find_dualbox",
		"admin_find_ip", // find all the player connections from a given IPv4 number
		"admin_find_account", // list all the characters from an account (useful for GMs w/o DB access)
		"admin_save_modifications", // consider it deprecated...
		"admin_rec",
		"admin_setclass",
		"admin_settitle",
		"admin_setsex",
		"admin_setcolor",
		"admin_fullfood",
		"admin_remclanwait",
		"admin_setcp",
		"admin_sethp",
		"admin_setmp",
		"admin_setchar_cp",
		"admin_setchar_hp",
		"admin_setchar_mp",
		"admin_test_phantom",
		"admin_alive",
		"admin_send_message",
		"admin_send_message2",
		"admin_send_message3",
		"admin_rndWalk_start",
		"admin_rndWalk_stop",
		"admin_phantom_come",
		"admin_phantom_stop",
		"admin_phantom_give_cgrade_karmian",
		"admin_phantom_give_cgrade_drake",
		"admin_phantom_give_cgrade_composite",
		"admin_phantom_give_item",
		"admin_ss",
		"admin_reload_st"
	};
	
	private enum CommandEnum
	{
		admin_changename, // changes char name
		admin_setname, // changes char name
		admin_edit_character,
		admin_current_player,
		admin_nokarma,
		admin_setkarma,
		admin_character_list, // same as character_info, kept for compatibility purposes
		admin_character_info, // given a player name, displays an information window
		admin_show_characters,
		admin_find_character,
		admin_find_dualbox,
		admin_find_ip, // find all the player connections from a given IPv4 number
		admin_find_account, // list all the characters from an account (useful for GMs w/o DB access)
		admin_save_modifications, // consider it deprecated...
		admin_rec,
		admin_setclass,
		admin_settitle,
		admin_setsex,
		admin_setcolor,
		admin_fullfood,
		admin_remclanwait,
		admin_setcp,
		admin_sethp,
		admin_setmp,
		admin_setchar_cp,
		admin_setchar_hp,
		admin_setchar_mp,
		admin_test_phantom,
		admin_alive,
		admin_send_message,
		admin_send_message2,
		admin_send_message3,
		admin_rndWalk_start,
		admin_rndWalk_stop,
		admin_phantom_come,
		admin_phantom_stop,
		admin_phantom_give_cgrade_karmian,
		admin_phantom_give_cgrade_drake,
		admin_phantom_give_cgrade_composite,
		admin_phantom_give_item,
		admin_ss,
		admin_reload_st
	}
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		
		final CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_changename:
			case admin_setname:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //changename|setname <new_name_for_target>");
					return false;
				}
				
				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				
				String oldName = null;
				
				if (target instanceof L2PcInstance)
				{
					player = (L2PcInstance) target;
					oldName = player.getName();
					
					L2World.getInstance().removeFromAllPlayers(player);
					player.setName(val);
					player.store();
					L2World.getInstance().addPlayerToWorld(player);
					
					player.sendMessage("Your name has been changed by a GM.");
					player.broadcastUserInfo();
					
					if (player.isInParty())
					{
						// Delete party window for other party members
						player.getParty().broadcastToPartyMembers(player, new PartySmallWindowDeleteAll());
						for (final L2PcInstance member : player.getParty().getPartyMembers())
						{
							// And re-add
							if (member != player)
							{
								member.sendPacket(new PartySmallWindowAll(player, player.getParty()));
							}
						}
					}
					
					if (player.getClan() != null)
					{
						player.getClan().updateClanMember(player);
						player.getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(player));
						player.sendPacket(new PledgeShowMemberListAll(player.getClan(), player));
					}
				}
				else if (target instanceof L2NpcInstance)
				{
					final L2NpcInstance npc = (L2NpcInstance) target;
					oldName = npc.getName();
					npc.setName(val);
					npc.updateAbnormalEffect();
				}
				
				if (oldName == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
					return false;
				}
				activeChar.sendMessage("Name changed from " + oldName + " to " + val);
				return true;
			} // changes char name
			case admin_edit_character:
			{
				editCharacter(activeChar);
				return true;
			}
			case admin_current_player:
			{
				showCharacterInfo(activeChar, null);
				return true;
			}
			case admin_nokarma:
			{
				setTargetKarma(activeChar, 0);
				return true;
			}
			case admin_setkarma:
			{
				
				int karma = 0;
				
				if (st.hasMoreTokens())
				{
					String val = st.nextToken();
					
					try
					{
						karma = Integer.parseInt(val);
						val = null;
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //setkarma new_karma_for_target(number)");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //setkarma new_karma_for_target");
					return false;
				}
				
				setTargetKarma(activeChar, karma);
				return true;
				
			}
			case admin_character_list:
			case admin_character_info:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //character_info <player_name>");
					return false;
				}
				
				final L2PcInstance target = L2World.getInstance().getPlayer(val);
				
				if (target != null)
				{
					showCharacterInfo(activeChar, target);
					val = null;
					return true;
				}
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
				val = null;
				return false;
			} // given a player name:{} displays an information window
			case admin_show_characters:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
					
					try
					{
						final int page = Integer.parseInt(val);
						listCharacters(activeChar, page);
						val = null;
						return true;
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //show_characters <page_number>");
						val = null;
						listCharacters(activeChar, 0);
						return false;
					}
				}
				activeChar.sendMessage("Usage: //show_characters <page_number>");
				listCharacters(activeChar, 0);
				return false;
			}
			case admin_find_character:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //find_character <player_name>");
					listCharacters(activeChar, 0);
					return false;
				}
				
				findCharacter(activeChar, val);
				val = null;
				return true;
			}
			case admin_find_dualbox:
			{
				String val = "";
				int boxes = 2;
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
					
					try
					{
						boxes = Integer.parseInt(val);
						val = null;
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //find_dualbox <boxes_number>(default 1)");
						val = null;
						listCharacters(activeChar, 0);
						return false;
					}
				}
				
				findMultibox(activeChar, boxes);
				return true;
			}
			case admin_find_ip:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
				}
				else
				{
					activeChar.sendMessage("Usage: //find_ip <ip>");
					listCharacters(activeChar, 0);
					return false;
				}
				
				try
				{
					findCharactersPerIp(activeChar, val);
					
				}
				catch (final IllegalArgumentException e)
				{
					activeChar.sendMessage("Usage: //find_ip <ip>");
					listCharacters(activeChar, 0);
					return false;
				}
				return true;
			} // find all the player connections from a given IPv4 number
			case admin_find_account:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
				}
				else
				{
					activeChar.sendMessage("Usage: //find_account <account_name>");
					listCharacters(activeChar, 0);
					return false;
				}
				
				findCharactersPerAccount(activeChar, val);
				return true;
				
			} // list all the characters from an account (useful for GMs w/o DB access)
			case admin_save_modifications:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					while (st.hasMoreTokens())
					{
						
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val = val + " " + st.nextToken();
						}
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //save_modifications <mods>");
					listCharacters(activeChar, 0);
					return false;
				}
				
				adminModifyCharacter(activeChar, val);
				val = null;
				return true;
				
			} // consider it deprecated...
			case admin_rec:
			{
				String val = "";
				int value = 1;
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
					
					try
					{
						value = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //rec <value>(default 1)");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //rec <value>(default 1)");
					return false;
				}
				
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				
				if (target instanceof L2PcInstance)
				{
					player = (L2PcInstance) target;
				}
				else
				{
					activeChar.sendMessage("Select player before. Usage: //rec <value>(default 1)");
					listCharacters(activeChar, 0);
					return false;
				}
				target = null;
				
				player.setRecomHave(player.getRecomHave() + value);
				SystemMessage sm = new SystemMessage(SystemMessageId.GM_S1);
				sm.addString("You have been recommended by a GM");
				player.sendPacket(sm);
				player.broadcastUserInfo();
				player = null;
				sm = null;
				val = null;
				
				return true;
			}
			case admin_setclass:
			{
				String val = "";
				int classidval = 0;
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
					
					try
					{
						classidval = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //setclass <value>(default 1)");
						return false;
					}
				}
				else
				{
					AdminHelpPage.showSubMenuPage(activeChar, "charclasses.htm");
					return false;
				}
				
				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				
				if (target instanceof L2PcInstance)
				{
					player = (L2PcInstance) target;
				}
				else
				{
					return false;
				}
				
				boolean valid = false;
				
				for (final ClassId classid : ClassId.values())
				{
					if (classidval == classid.getId())
					{
						valid = true;
					}
				}
				
				if (valid && player.getClassId().getId() != classidval)
				{
					player.setClassId(classidval);
					
					final ClassId classId = ClassId.getClassIdByOrdinal(classidval);
					
					if (!player.isSubClassActive())
					{
						// while(classId.level() != 0){ //go to root
						// classId = classId.getParent();
						// }
						
						player.setBaseClass(classId);
					}
					
					String newclass = player.getTemplate().className;
					player.store();
					
					if (player != activeChar)
					{
						player.sendMessage("A GM changed your class to " + newclass);
					}
					
					player.broadcastUserInfo();
					activeChar.sendMessage(player.getName() + " changed to " + newclass);
					
					newclass = null;
					return true;
				}
				activeChar.sendMessage("Usage: //setclass <valid_new_classid>");
				return false;
			}
			case admin_settitle:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //settitle <new_title>");
					return false;
				}
				
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				L2NpcInstance npc = null;
				
				if (target == null)
				{
					player = activeChar;
				}
				else if (target instanceof L2PcInstance)
				{
					player = (L2PcInstance) target;
				}
				else if (target instanceof L2NpcInstance)
				{
					npc = (L2NpcInstance) target;
				}
				else
				{
					activeChar.sendMessage("Select your target before the command");
					return false;
				}
				
				target = null;
				st = null;
				
				if (player != null)
				{
					player.setTitle(val);
					if (player != activeChar)
					{
						player.sendMessage("Your title has been changed by a GM.");
					}
					player.broadcastTitleInfo();
				}
				else if (npc != null)
				{
					npc.setTitle(val);
					npc.updateAbnormalEffect();
				}
				
				val = null;
				
				return true;
			}
			case admin_setsex:
			{
				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				
				if (target instanceof L2PcInstance)
				{
					player = (L2PcInstance) target;
				}
				else
				{
					activeChar.sendMessage("Select player before command");
					return false;
				}
				player.getAppearance().setSex(player.getAppearance().getSex() ? false : true);
				L2PcInstance.setSexDB(player, 1);
				player.sendMessage("Your gender has been changed by a GM");
				player.decayMe();
				player.spawnMe(player.getX(), player.getY(), player.getZ());
				player.broadcastUserInfo();
				
				return true;
			}
			case admin_setcolor:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					
					val = st.nextToken();
				}
				else
				{
					activeChar.sendMessage("Usage: //setcolor <new_color>");
					return false;
				}
				
				L2Object target = activeChar.getTarget();
				
				if (target == null)
				{
					activeChar.sendMessage("You have to select a player!");
					return false;
				}
				
				if (!(target instanceof L2PcInstance))
				{
					activeChar.sendMessage("Your target is not a player!");
					return false;
				}
				
				L2PcInstance player = (L2PcInstance) target;
				player.getAppearance().setNameColor(Integer.decode("0x" + val));
				player.sendMessage("Your name color has been changed by a GM");
				player.broadcastUserInfo();
				st = null;
				player = null;
				target = null;
				return true;
			}
			case admin_fullfood:
			{
				L2Object target = activeChar.getTarget();
				
				if (target instanceof L2PetInstance)
				{
					L2PetInstance targetPet = (L2PetInstance) target;
					targetPet.setCurrentFed(targetPet.getMaxFed());
					targetPet.getOwner().sendPacket(new SetSummonRemainTime(targetPet.getMaxFed(), targetPet.getCurrentFed()));
					targetPet = null;
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
					return false;
				}
				
				target = null;
				return true;
			}
			case admin_remclanwait:
			{
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				
				if (target instanceof L2PcInstance)
				{
					player = (L2PcInstance) target;
				}
				else
				{
					activeChar.sendMessage("You have to select a player!");
					return false;
				}
				target = null;
				
				if (player.getClan() == null)
				{
					player.setClanJoinExpiryTime(0);
					player.sendMessage("A GM Has reset your clan wait time, You may now join another clan.");
					activeChar.sendMessage("You have reset " + player.getName() + "'s wait time to join another clan.");
				}
				else
				{
					activeChar.sendMessage("Sorry, but " + player.getName() + " must not be in a clan. Player must leave clan before the wait limit can be reset.");
					return false;
				}
				
				player = null;
				return true;
				
			}
			case admin_setcp:
			{
				String val = "";
				int value = 0;
				
				if (st.hasMoreTokens())
				{
					
					val = st.nextToken();
					
					try
					{
						value = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Value must be an integer");
						activeChar.sendMessage("Usage: //setcp <new_value>");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //setcp <new_value>");
					return false;
				}
				
				activeChar.getStatus().setCurrentCp(value);
				
				return true;
				
			}
			case admin_sethp:
			{
				String val = "";
				int value = 0;
				
				if (st.hasMoreTokens())
				{
					
					val = st.nextToken();
					
					try
					{
						value = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Value must be an integer");
						activeChar.sendMessage("Usage: //sethp <new_value>");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //sethp <new_value>");
					return false;
				}
				
				activeChar.getStatus().setCurrentHp(value);
				
				return true;
			}
			case admin_setmp:
			{
				String val = "";
				int value = 0;
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
					
					try
					{
						value = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Value must be an integer");
						activeChar.sendMessage("Usage: //setmp <new_value>");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //setmp <new_value>");
					return false;
				}
				
				activeChar.getStatus().setCurrentMp(value);
				
				return true;
			}
			case admin_setchar_cp:
			{
				String val = "";
				int value = 0;
				
				if (st.hasMoreTokens())
				{
					
					val = st.nextToken();
					
					try
					{
						value = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Value must be an integer");
						activeChar.sendMessage("Usage: //setchar_cp <new_value>");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //setchar_cp <new_value>");
					return false;
				}
				
				if (activeChar.getTarget() instanceof L2PcInstance)
				{
					L2PcInstance pc = (L2PcInstance) activeChar.getTarget();
					pc.getStatus().setCurrentCp(value);
					pc = null;
				}
				else if (activeChar.getTarget() instanceof L2PetInstance)
				{
					L2PetInstance pet = (L2PetInstance) activeChar.getTarget();
					pet.getStatus().setCurrentCp(value);
					pet = null;
				}
				else
				{
					activeChar.getStatus().setCurrentCp(value);
				}
				
				return true;
			}
			case admin_setchar_hp:
			{
				String val = "";
				int value = 0;
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
					
					try
					{
						value = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Value must be an integer");
						activeChar.sendMessage("Usage: //setchar_hp <new_value>");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //setchar_hp <new_value>");
					return false;
				}
				
				if (activeChar.getTarget() instanceof L2PcInstance)
				{
					L2PcInstance pc = (L2PcInstance) activeChar.getTarget();
					pc.getStatus().setCurrentHp(value);
					pc = null;
				}
				else if (activeChar.getTarget() instanceof L2PetInstance)
				{
					L2PetInstance pet = (L2PetInstance) activeChar.getTarget();
					pet.getStatus().setCurrentHp(value);
					pet = null;
				}
				else
				{
					activeChar.getStatus().setCurrentHp(value);
				}
				
				return true;
			}
			case admin_setchar_mp:
			{
				String val = "";
				int value = 0;
				
				if (st.hasMoreTokens())
				{
					val = st.nextToken();
					
					try
					{
						value = Integer.parseInt(val);
					}
					catch (final NumberFormatException e)
					{
						activeChar.sendMessage("Value must be an integer");
						activeChar.sendMessage("Usage: //setchar_mp <new_value>");
						return false;
					}
				}
				else
				{
					activeChar.sendMessage("Usage: //setchar_mp <new_value>");
					return false;
				}
				
				if (activeChar.getTarget() instanceof L2PcInstance)
				{
					L2PcInstance pc = (L2PcInstance) activeChar.getTarget();
					pc.getStatus().setCurrentMp(value);
					pc = null;
				}
				else if (activeChar.getTarget() instanceof L2PetInstance)
				{
					L2PetInstance pet = (L2PetInstance) activeChar.getTarget();
					pet.getStatus().setCurrentMp(value);
					pet = null;
				}
				else
				{
					activeChar.getStatus().setCurrentMp(value);
				}
				
				return true;
			}
			case admin_test_phantom:
			{
				// phantomPlayers.getInstance().startWalk(activeChar);
				showCharacterInfo(activeChar, null);
				break;
			}
			// TODO ALIVE
			case admin_alive:
			{
				// phantomPlayers.getInstance().startWalk((L2PcInstance) activeChar.getTarget());
				showCharacterInfo(activeChar, null);
				
				break;
			}
			// TODO SEND MESSAGE
			case admin_send_message:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
				}
				else
				{
					return false;
				}
				
				L2Character target = (L2Character) activeChar.getTarget();
				
				if (target == activeChar)
				{
					activeChar.sendMessage("Wrong target!");
					return false;
				}
				
				if (target != null)
				{
					CreatureSay cs = new CreatureSay(target.getObjectId(), Say2.ALL, "" + (Config.SHOW_TIME_IN_CHAT ? "[" + fmt.format(new Date(System.currentTimeMillis())) + "]" : "") + " " + target.getName(), val);
					for (L2PcInstance player : target.getKnownList().getKnownPlayers().values())
					{
						if (player != null && activeChar.isInsideRadius(player, 1250, false, true))
						{
							if (!BlockList.isBlocked(player, (L2PcInstance) target))
							{
								player.sendPacket(cs);
							}
						}
					}
					target.sendPacket(cs);
				}
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_send_message2:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
				}
				else
				{
					return false;
				}
				
				L2Character target = (L2Character) activeChar.getTarget();
				
				if (target == activeChar)
				{
					activeChar.sendMessage("Wrong target!");
					return false;
				}
				
				if (target != null)
				{
					CreatureSay cs = new CreatureSay(target.getObjectId(), Say2.SHOUT, "" + (Config.SHOW_TIME_IN_CHAT ? "[" + fmt.format(new Date(System.currentTimeMillis())) + "]" : "") + " " + target.getName(), val);
					int region = MapRegionTable.getInstance().getMapRegionLocId(target.getX(), target.getY());
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (region == MapRegionTable.getInstance().getMapRegionLocId(player.getX(), player.getY()))
						{
							if (!BlockList.isBlocked(player, (L2PcInstance) target))
							{
								player.sendPacket(cs);
							}
						}
					}
				}
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_send_message3:
			{
				String val = "";
				
				if (st.hasMoreTokens())
				{
					while (st.hasMoreTokens())
					{
						if (val.equals(""))
						{
							val = st.nextToken();
						}
						else
						{
							val += " " + st.nextToken();
						}
					}
				}
				else
				{
					return false;
				}
				
				L2Character target = (L2Character) activeChar.getTarget();
				
				if (target == activeChar)
				{
					activeChar.sendMessage("Wrong target!");
					return false;
				}
				
				if (target != null)
				{
					CreatureSay cs = new CreatureSay(target.getObjectId(), Say2.SHOUT, "" + (Config.SHOW_TIME_IN_CHAT ? "[" + fmt.format(new Date(System.currentTimeMillis())) + "]" : "") + " " + target.getName(), val);
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (!BlockList.isBlocked(player, (L2PcInstance) target))
						{
							player.sendPacket(cs);
						}
					}
				}
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_rndWalk_start:
			{
				((L2PcInstance) activeChar.getTarget()).setIsPhantomRndWalk(true);
				((L2PcInstance) activeChar.getTarget()).startPhantomAI();
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_rndWalk_stop:
			{
				((L2PcInstance) activeChar.getTarget()).setIsPhantomRndWalk(false);
				((L2Character) activeChar.getTarget()).getAI().setIntention(AI_INTENTION_IDLE);
				((L2PcInstance) activeChar.getTarget()).stopPhantomAI();
				break;
			}
			case admin_phantom_come:
			{
				((L2PcInstance) activeChar.getTarget()).getAI().moveTo(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				((L2PcInstance) activeChar.getTarget()).broadcastPacket(new MoveToPawn(((L2PcInstance) activeChar.getTarget()), activeChar, 40));
				
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_phantom_stop:
			{
				// ((L2Character) activeChar.getTarget()).stopMove(null);
				// ((L2Character) activeChar.getTarget()).getAI().stopFollow();
				((L2Character) activeChar.getTarget()).getAI().setIntention(AI_INTENTION_IDLE);
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_phantom_give_cgrade_karmian:
			{
				L2ItemInstance body = null;
				L2ItemInstance gaiters = null;
				L2ItemInstance gloves = null;
				L2ItemInstance boots = null;
				L2ItemInstance weapon = null;
				
				body = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 439, 1, ((L2PcInstance) activeChar.getTarget()), null);
				gaiters = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 471, 1, ((L2PcInstance) activeChar.getTarget()), null);
				gloves = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 2454, 1, ((L2PcInstance) activeChar.getTarget()), null);
				boots = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 2430, 1, ((L2PcInstance) activeChar.getTarget()), null);
				weapon = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 6313, 1, ((L2PcInstance) activeChar.getTarget()), null);
				
				if (body != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(body);
				}
				if (gaiters != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(gaiters);
				}
				if (gloves != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(gloves);
				}
				if (boots != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(boots);
				}
				if (weapon != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(weapon);
				}
				showCharacterInfo(activeChar, null);
				((L2PcInstance) activeChar.getTarget()).broadcastUserInfo();
				break;
			}
			case admin_phantom_give_cgrade_drake:
			{
				L2ItemInstance body = null;
				L2ItemInstance gloves = null;
				L2ItemInstance boots = null;
				L2ItemInstance weapon = null;
				
				body = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 401, 1, ((L2PcInstance) activeChar.getTarget()), null);
				gloves = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 2461, 1, ((L2PcInstance) activeChar.getTarget()), null);
				boots = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 2437, 1, ((L2PcInstance) activeChar.getTarget()), null);
				weapon = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 4824, 1, ((L2PcInstance) activeChar.getTarget()), null);
				
				if (body != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(body);
				}
				if (gloves != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(gloves);
				}
				if (boots != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(boots);
				}
				if (weapon != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(weapon);
				}
				((L2PcInstance) activeChar.getTarget()).broadcastUserInfo();
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_phantom_give_cgrade_composite:
			{
				L2ItemInstance body = null;
				L2ItemInstance boots = null;
				
				body = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 60, 1, ((L2PcInstance) activeChar.getTarget()), null);
				boots = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", 64, 1, ((L2PcInstance) activeChar.getTarget()), null);
				
				if (body != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(body);
				}
				if (boots != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(boots);
				}
				((L2PcInstance) activeChar.getTarget()).broadcastUserInfo();
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_phantom_give_item:
			{
				String itemId = "";
				
				if (st.hasMoreTokens())
				{
					while (st.hasMoreTokens())
					{
						if (itemId.equals(""))
						{
							itemId = st.nextToken();
						}
						else
						{
							itemId += " " + st.nextToken();
						}
					}
				}
				else
				{
					activeChar.sendMessage("Usage: just enter item ID");
					return false;
				}
				// int itemId = Integer.parseInt(parameters.trim());
				L2ItemInstance item = null;
				item = ((L2PcInstance) activeChar.getTarget()).getInventory().addItem("PhantomItem", Integer.parseInt(itemId), 1, ((L2PcInstance) activeChar.getTarget()), null);
				
				if (item != null)
				{
					((L2PcInstance) activeChar.getTarget()).getInventory().equipItemAndRecord(item);
				}
				((L2PcInstance) activeChar.getTarget()).broadcastUserInfo();
				showCharacterInfo(activeChar, null);
				break;
			}
			case admin_reload_st:
			{
				for (L2Skill skill : ((L2PcInstance) activeChar.getTarget()).getAllSkills())
				{
					((L2PcInstance) activeChar.getTarget()).enableSkill(skill);
				}
				
				((L2PcInstance) activeChar.getTarget()).updateEffectIcons();
				((L2PcInstance) activeChar.getTarget()).sendSkillList();
				break;
			}
			case admin_ss:
			
		}
		
		return false;
	}
	
	private void listCharacters(final L2PcInstance activeChar, int page)
	{
		final Collection<L2PcInstance> allPlayers_with_offlines = L2World.getInstance().getAllPlayers().values();
		
		List<L2PcInstance> online_players_list = new ArrayList<>();
		
		for (final L2PcInstance actual_player : allPlayers_with_offlines)
		{
			if (actual_player != null && actual_player.isOnline() == 1 && !actual_player.isInOfflineMode())
			{
				online_players_list.add(actual_player);
			}
			else if (actual_player == null)
			{
				LOG.warn("listCharacters: found player null into L2World Instance..");
			}
			else if (actual_player.isOnline() == 0 && Config.DEBUG)
			{
				LOG.warn("listCharacters: player " + actual_player.getName() + " not online into L2World Instance..");
			}
			else if (actual_player.isInOfflineMode() && Config.DEBUG)
			{
				LOG.warn("listCharacters: player " + actual_player.getName() + " offline into L2World Instance..");
			}
		}
		
		L2PcInstance[] players = online_players_list.toArray(new L2PcInstance[online_players_list.size()]);
		online_players_list = null;
		
		final int MaxCharactersPerPage = 30;
		int MaxPages = players.length / MaxCharactersPerPage;
		
		if (players.length > MaxCharactersPerPage * MaxPages)
		{
			MaxPages++;
		}
		
		// Check if number of users changed
		if (page > MaxPages)
		{
			page = MaxPages;
		}
		
		final int CharactersStart = MaxCharactersPerPage * page;
		int CharactersEnd = players.length;
		
		if (CharactersEnd - CharactersStart > MaxCharactersPerPage)
		{
			CharactersEnd = CharactersStart + MaxCharactersPerPage;
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/charlist.htm");
		TextBuilder replyMSG = new TextBuilder();
		
		replyMSG.append("<table width=300>");
		replyMSG.append("<tr>");
		for (int x = 0; x < MaxPages; x++)
		{
			int pagenr = x + 1;
			
			if (pagenr == 12 || pagenr == 24 || pagenr == 36)
			{
				replyMSG.append("</tr></table><table width=300><tr>");
			}
			
			replyMSG.append("<td width=15><center><a action=\"bypass -h admin_show_characters " + x + "\">[" + pagenr + "]</a></center></td>");
			
		}
		replyMSG.append("</tr>");
		replyMSG.append("</table>");
		adminReply.replace("%pages%", replyMSG.toString());
		
		replyMSG.clear();
		
		int count = CharactersStart;
		for (int i = CharactersStart; i < CharactersEnd; i++)
		{
			count++;
			
			replyMSG.append("<tr><td width=120>" + count + ". <a action=\"bypass -h admin_character_info " + players[i].getName() + "\">" + players[i].getName() + "</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
			
		}
		
		adminReply.replace("%players%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * @param activeChar
	 * @param player
	 * @param filename
	 */
	public static void gatherCharacterInfo(final L2PcInstance activeChar, final L2PcInstance player, final String filename)
	{
		String ip = "Disconnected";
		
		try
		{
			if (player.getClient() != null)
			{
				StringTokenizer clientinfo = new StringTokenizer(player.getClient().toString(), " ):-(");
				clientinfo.nextToken();
				clientinfo.nextToken();
				clientinfo.nextToken();
				clientinfo.nextToken();
				clientinfo.nextToken();
				ip = clientinfo.nextToken();
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/" + filename);
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		
		final L2Clan playerClan = ClanTable.getInstance().getClan(player.getClanId());
		if (playerClan != null)
		{
			adminReply.replace("%clan%", playerClan.getName());
		}
		else
		{
			adminReply.replace("%clan%", "-");
		}
		
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		adminReply.replace("%class%", player.getTemplate().className);
		adminReply.replace("%ordinal%", String.valueOf(player.getClassId().ordinal()));
		adminReply.replace("%classid%", String.valueOf(player.getClassId()));
		adminReply.replace("%x%", String.valueOf(player.getX()));
		adminReply.replace("%y%", String.valueOf(player.getY()));
		adminReply.replace("%z%", String.valueOf(player.getZ()));
		adminReply.replace("%currenthp%", String.valueOf((int) player.getCurrentHp()));
		adminReply.replace("%maxhp%", String.valueOf(player.getMaxHp()));
		adminReply.replace("%karma%", String.valueOf(player.getKarma()));
		adminReply.replace("%currentmp%", String.valueOf((int) player.getCurrentMp()));
		adminReply.replace("%maxmp%", String.valueOf(player.getMaxMp()));
		adminReply.replace("%pvpflag%", String.valueOf(player.getPvpFlag()));
		adminReply.replace("%currentcp%", String.valueOf((int) player.getCurrentCp()));
		adminReply.replace("%maxcp%", String.valueOf(player.getMaxCp()));
		adminReply.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
		adminReply.replace("%pkkills%", String.valueOf(player.getPkKills()));
		adminReply.replace("%currentload%", String.valueOf(player.getCurrentLoad()));
		adminReply.replace("%maxload%", String.valueOf(player.getMaxLoad()));
		adminReply.replace("%percent%", String.valueOf(Util.roundTo((float) player.getCurrentLoad() / (float) player.getMaxLoad() * 100, 2)));
		adminReply.replace("%patk%", String.valueOf(player.getPAtk(null)));
		adminReply.replace("%matk%", String.valueOf(player.getMAtk(null, null)));
		adminReply.replace("%pdef%", String.valueOf(player.getPDef(null)));
		adminReply.replace("%mdef%", String.valueOf(player.getMDef(null, null)));
		adminReply.replace("%accuracy%", String.valueOf(player.getAccuracy()));
		adminReply.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
		adminReply.replace("%critical%", String.valueOf(player.getCriticalHit(null, null)));
		adminReply.replace("%runspeed%", String.valueOf(player.getRunSpeed()));
		adminReply.replace("%patkspd%", String.valueOf(player.getPAtkSpd()));
		adminReply.replace("%matkspd%", String.valueOf(player.getMAtkSpd()));
		adminReply.replace("%access%", String.valueOf(player.getAccessLevel().getLevel()));
		adminReply.replace("%account%", String.valueOf(player.getAccountName()));
		adminReply.replace("%ip%", ip);
		adminReply.replace("%heading%", String.valueOf(player.getHeading()));
		activeChar.sendPacket(adminReply);
	}
	
	private void setTargetKarma(final L2PcInstance activeChar, final int newKarma)
	{
		// function to change karma of selected char
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			return;
		}
		
		target = null;
		
		if (newKarma >= 0)
		{
			// for display
			final int oldKarma = player.getKarma();
			
			// update karma
			player.setKarma(newKarma);
			
			StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.KARMA, newKarma);
			player.sendPacket(su);
			su = null;
			
			// Common character information
			SystemMessage sm = new SystemMessage(SystemMessageId.GM_S1);
			sm.addString("Admin has changed your karma from " + oldKarma + " to " + newKarma + ".");
			player.sendPacket(sm);
			sm = null;
			
			// Admin information
			if (player != activeChar)
			{
				activeChar.sendMessage("Successfully Changed karma for " + player.getName() + " from (" + oldKarma + ") to (" + newKarma + ").");
			}
		}
		else
		{
			// tell admin of mistake
			activeChar.sendMessage("You must enter a value for karma greater than or equal to 0.");
		}
		
		player = null;
	}
	
	private void adminModifyCharacter(final L2PcInstance activeChar, final String modifications)
	{
		L2Object target = activeChar.getTarget();
		
		if (!(target instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) target;
		StringTokenizer st = new StringTokenizer(modifications);
		
		target = null;
		
		if (st.countTokens() != 6)
		{
			editCharacter(player);
			return;
		}
		
		final String hp = st.nextToken();
		final String mp = st.nextToken();
		final String cp = st.nextToken();
		final String pvpflag = st.nextToken();
		final String pvpkills = st.nextToken();
		final String pkkills = st.nextToken();
		
		st = null;
		
		final int hpval = Integer.parseInt(hp);
		final int mpval = Integer.parseInt(mp);
		final int cpval = Integer.parseInt(cp);
		final int pvpflagval = Integer.parseInt(pvpflag);
		final int pvpkillsval = Integer.parseInt(pvpkills);
		final int pkkillsval = Integer.parseInt(pkkills);
		
		// Common character information
		player.sendMessage("Admin has changed your stats." + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + "  PvP Flag: " + pvpflagval + " PvP/PK " + pvpkillsval + "/" + pkkillsval);
		player.getStatus().setCurrentHp(hpval);
		player.getStatus().setCurrentMp(mpval);
		player.getStatus().setCurrentCp(cpval);
		player.setPvpFlag(pvpflagval);
		player.setPvpKills(pvpkillsval);
		player.setPkKills(pkkillsval);
		
		// Save the changed parameters to the database.
		player.store();
		
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, hpval);
		su.addAttribute(StatusUpdate.MAX_HP, player.getMaxHp());
		su.addAttribute(StatusUpdate.CUR_MP, mpval);
		su.addAttribute(StatusUpdate.MAX_MP, player.getMaxMp());
		su.addAttribute(StatusUpdate.CUR_CP, cpval);
		su.addAttribute(StatusUpdate.MAX_CP, player.getMaxCp());
		player.sendPacket(su);
		su = null;
		
		// Admin information
		player.sendMessage("Changed stats of " + player.getName() + "." + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + "  PvP: " + pvpflagval + " / " + pvpkillsval);
		
		if (Config.DEBUG)
		{
			LOG.warn("[GM]" + activeChar.getName() + " changed stats of " + player.getName() + ". " + " HP: " + hpval + " MP: " + mpval + " CP: " + cpval + " PvP: " + pvpflagval + " / " + pvpkillsval);
		}
		
		showCharacterInfo(activeChar, null); // Back to start
		
		player.broadcastUserInfo();
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		player.decayMe();
		player.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		
		player = null;
	}
	
	private void editCharacter(final L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		
		if (!(target instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance player = (L2PcInstance) target;
		gatherCharacterInfo(activeChar, player, "charedit.htm");
		target = null;
		player = null;
	}
	
	private void findCharacter(final L2PcInstance activeChar, final String CharacterToFind)
	{
		int CharactersFound = 0;
		
		String name;
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		
		allPlayers = null;
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/charfind.htm");
		TextBuilder replyMSG = new TextBuilder();
		
		for (final L2PcInstance player : players)
		{ // Add player info into new Table row
			name = player.getName();
			
			if (name.toLowerCase().contains(CharacterToFind.toLowerCase()))
			{
				CharactersFound = CharactersFound + 1;
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list " + name + "\">" + name + "</a></td><td width=110>" + player.getTemplate().className + "</td><td width=40>" + player.getLevel() + "</td></tr>");
			}
			
			if (CharactersFound > 20)
			{
				break;
			}
		}
		
		name = null;
		players = null;
		
		adminReply.replace("%results%", replyMSG.toString());
		replyMSG.clear();
		
		if (CharactersFound == 0)
		{
			replyMSG.append("s. Please try again.");
		}
		else if (CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than 20");
			replyMSG.append("s.<br>Please refine your search to see all of the results.");
		}
		else if (CharactersFound == 1)
		{
			replyMSG.append('.');
		}
		else
		{
			replyMSG.append("s.");
		}
		
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void findMultibox(final L2PcInstance activeChar, final int multibox) throws IllegalArgumentException
	{
		final Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		final L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		final Map<String, List<L2PcInstance>> ipMap = new HashMap<>();
		
		String ip = "0.0.0.0";
		
		final Map<String, Integer> dualboxIPs = new HashMap<>();
		
		for (final L2PcInstance player : players)
		{
			if (player.getClient() == null || player.getClient().getConnection() == null || player.getClient().getConnection().getInetAddress() == null || player.getClient().getConnection().getInetAddress().getHostAddress() == null)
			{
				
				continue;
				
			}
			
			ip = player.getClient().getConnection().getInetAddress().getHostAddress();
			
			if (ipMap.get(ip) == null)
			{
				ipMap.put(ip, new ArrayList<L2PcInstance>());
			}
			ipMap.get(ip).add(player);
			
			if (ipMap.get(ip).size() >= multibox)
			{
				final Integer count = dualboxIPs.get(ip);
				if (count == null)
				{
					dualboxIPs.put(ip, 0);
				}
				else
				{
					dualboxIPs.put(ip, count + 1);
				}
			}
		}
		
		List<String> keys = new ArrayList<>(dualboxIPs.keySet());
		Collections.sort(keys, new Comparator<String>()
		{
			@Override
			public int compare(final String left, final String right)
			{
				return dualboxIPs.get(left).compareTo(dualboxIPs.get(right));
			}
		});
		Collections.reverse(keys);
		
		final StringBuilder results = new StringBuilder();
		
		results.append("<table width=140>");
		for (final String dualboxIP : keys)
		{
			results.append("<tr><td width=140><a action=\"bypass -h admin_find_ip " + dualboxIP + "\">" + dualboxIP + "</a> +" + dualboxIPs.get(dualboxIP) + "</td></tr>");
		}
		results.append("</table>");
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/dualbox.htm");
		adminReply.replace("%multibox%", String.valueOf(multibox));
		adminReply.replace("%results%", results.toString());
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * @param activeChar
	 * @param IpAdress
	 * @throws IllegalArgumentException
	 */
	private void findCharactersPerIp(final L2PcInstance activeChar, final String IpAdress) throws IllegalArgumentException
	{
		if (!IpAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
		{
			throw new IllegalArgumentException("Malformed IPv4 number");
		}
		
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers().values();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		allPlayers = null;
		
		int CharactersFound = 0;
		
		String name, ip = "0.0.0.0";
		
		TextBuilder replyMSG = new TextBuilder();
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/ipfind.htm");
		
		for (final L2PcInstance player : players)
		{
			if (player.getClient() == null || player.getClient().getConnection() == null || player.getClient().getConnection().getInetAddress() == null || player.getClient().getConnection().getInetAddress().getHostAddress() == null)
			{
				continue;
			}
			
			ip = player.getClient().getConnection().getInetAddress().getHostAddress();
			
			if (ip.equals(IpAdress))
			{
				name = player.getName();
				CharactersFound = CharactersFound + 1;
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list " + name + "\">" + name + "</a></td><td width=110>" + player.getTemplate().className + "</td><td width=40>" + player.getLevel() + "</td></tr>");
			}
			
			if (CharactersFound == 1)
			{
				adminReply.replace("%player%", player.getName());
			}
			
			if (CharactersFound > 20)
			{
				break;
			}
		}
		
		adminReply.replace("%results%", replyMSG.toString());
		replyMSG.clear();
		
		if (CharactersFound == 0)
		{
			replyMSG.append("s. Maybe they got d/c?");
		}
		else if (CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than " + String.valueOf(CharactersFound));
			replyMSG.append("s.<br>In order to avoid you a client crash I won't <br1>display results beyond the 20th character.");
		}
		else if (CharactersFound == 1)
		{
			replyMSG.append('.');
		}
		else
		{
			replyMSG.append("s.");
		}
		
		adminReply.replace("%ip%", ip);
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void findCharactersPerAccount(final L2PcInstance activeChar, final String characterName) throws IllegalArgumentException
	{
		if (characterName.matches(Config.CNAME_TEMPLATE))
		{
			String account = null;
			Map<Integer, String> chars;
			L2PcInstance player = L2World.getInstance().getPlayer(characterName);
			
			if (player == null)
			{
				throw new IllegalArgumentException("Player doesn't exist");
			}
			
			chars = player.getAccountChars();
			account = player.getAccountName();
			
			TextBuilder replyMSG = new TextBuilder();
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			adminReply.setFile("data/html/admin/accountinfo.htm");
			
			for (final String charname : chars.values())
			{
				replyMSG.append(charname + "<br1>");
			}
			
			adminReply.replace("%characters%", replyMSG.toString());
			adminReply.replace("%account%", account);
			adminReply.replace("%player%", characterName);
			activeChar.sendPacket(adminReply);
		}
		else
		{
			throw new IllegalArgumentException("Malformed character name");
		}
	}
	
	private void showCharacterInfo(final L2PcInstance activeChar, L2PcInstance player)
	{
		if (player == null)
		{
			L2Object target = activeChar.getTarget();
			
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return;
			}
			
			target = null;
		}
		else
		{
			activeChar.setTarget(player);
		}
		
		gatherCharacterInfo(activeChar, player, "charinfo.htm");
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}