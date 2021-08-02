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
package l2jorion.game.network.clientpackets;

import java.nio.BufferUnderflowException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.handler.IVoicedCommandHandler;
import l2jorion.game.handler.VoicedCommandHandler;
import l2jorion.game.managers.PetitionManager;
import l2jorion.game.model.BlockList;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.PartyMatchRoom;
import l2jorion.game.model.PartyMatchRoomList;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance.PunishLevel;
import l2jorion.game.network.SystemChatChannelId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.util.Util;

public final class Say2 extends L2GameClientPacket
{
	protected static final Logger LOG = Logger.getLogger(Say2.class.getName());
	private static Logger _logChat = Logger.getLogger("chat");
	
	private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
	
	public final static int ALL = 0;
	public final static int SHOUT = 1; // !
	public final static int TELL = 2;
	public final static int PARTY = 3; // #
	public final static int CLAN = 4; // @
	public final static int GM = 5;
	public final static int PETITION_PLAYER = 6;
	public final static int PETITION_GM = 7;
	public final static int TRADE = 8; // +
	public final static int ALLIANCE = 9; // $
	public final static int ANNOUNCEMENT = 10;
	public final static int BOAT = 11;
	public final static int L2FRIEND = 12;
	public final static int MSNCHAT = 13;
	
	public final static int PARTYMATCH_ROOM = 14;
	public final static int PARTYROOM_COMMANDER = 15; // (Yellow)
	public final static int PARTYROOM_ALL = 16; // (Red)
	
	public final static int HERO_VOICE = 17;
	public static final int CRITICAL_ANNOUNCE = 18;
	public final static int SCREEN_ANNOUNCE = 19;
	public final static int BATTLEFIELD = 20;
	public final static int MPCC_ROOM = 21;
	
	private final static String[] CHAT_NAMES =
	{
		"ALL",
		"SHOUT",
		"TELL",
		"PARTY",
		"CLAN",
		"GM",
		"PETITION_PLAYER",
		"PETITION_GM",
		"TRADE",
		"ALLIANCE",
		"ANNOUNCEMENT", // 10
		"BOAT",
		"WILLCRASHCLIENT",
		"FAKEALL?",
		"PARTYMATCH_ROOM",
		"PARTYROOM_COMMANDER",
		"PARTYROOM_ALL",
		"HERO_VOICE",
		"CRITICAL_ANNOUNCE",
		"SCREEN_ANNOUNCE",
		"BATTLEFIELD",
		"MPCC_ROOM"
	};
	
	private String _text;
	private int _type;
	private SystemChatChannelId _type2Check;
	private String _target;
	
	@Override
	protected void readImpl()
	{
		_text = readS();
		
		try
		{
			_type = readD();
			_type2Check = SystemChatChannelId.getChatType(_type);
			
		}
		catch (BufferUnderflowException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			_type = CHAT_NAMES.length;
			_type2Check = SystemChatChannelId.CHAT_NONE;
		}
		
		_target = _type == TELL ? readS() : null;
	}
	
	@Override
	protected void runImpl()
	{
		if (Config.DEBUG)
		{
			LOG.info("Say2: Msg Type = '" + _type + "' Text = '" + _text + "'.");
		}
		
		if (_type < 0 || _type >= CHAT_NAMES.length)
		{
			LOG.warning("Say2: Invalid type: " + _type);
			return;
		}
		
		L2PcInstance activeChar = getClient().getActiveChar();
		
		// Anti-PHX Announce
		if (_type2Check == SystemChatChannelId.CHAT_NONE || _type2Check == SystemChatChannelId.CHAT_ANNOUNCE || _type2Check == SystemChatChannelId.CHAT_CRITICAL_ANNOUNCE || _type2Check == SystemChatChannelId.CHAT_SYSTEM || _type2Check == SystemChatChannelId.CHAT_CUSTOM
			|| (_type2Check == SystemChatChannelId.CHAT_GM_PET && !activeChar.isGM()))
		{
			LOG.warning("[Anti-PHX Announce] Illegal Chat channel was used by character: [" + activeChar.getName() + "]");
			return;
		}
		
		if (activeChar == null)
		{
			LOG.warning("Say2: Active Character is null.");
			return;
		}
		
		if (activeChar.isChatBanned() && !activeChar.isGM() && _type != CLAN && _type != ALLIANCE && _type != PARTY)
		{
			activeChar.sendMessage("You may not chat while a chat ban is in effect.");
			return;
		}
		
		if (activeChar.isInJail() && Config.JAIL_DISABLE_CHAT)
		{
			if (_type == TELL || _type == SHOUT || _type == TRADE || _type == HERO_VOICE)
			{
				activeChar.sendMessage("You can not chat with players outside of the jail.");
				return;
			}
		}
		
		if (!getClient().getFloodProtectors().getSayAction().tryPerformAction("Say2"))
		{
			activeChar.sendMessage("You can not speak too fast.");
			return;
		}
		
		if (_type == PETITION_PLAYER && activeChar.isGM())
		{
			_type = PETITION_GM;
		}
		
		if (_text.length() > Config.MAX_CHAT_LENGTH)
		{
			if (Config.DEBUG)
			{
				LOG.info("Say2: Msg Type = '" + _type + "' Text length more than " + Config.MAX_CHAT_LENGTH + " truncate them.");
			}
			_text = _text.substring(0, Config.MAX_CHAT_LENGTH);
		}
		
		if (Config.LOG_CHAT)
		{
			LogRecord record = new LogRecord(Level.INFO, _text);
			record.setLoggerName("chat");
			
			if (_type == TELL)
			{
				record.setParameters(new Object[]
				{
					CHAT_NAMES[_type],
					"[" + activeChar.getName() + " to " + _target + "]"
				});
			}
			else
			{
				record.setParameters(new Object[]
				{
					CHAT_NAMES[_type],
					"[" + activeChar.getName() + "]"
				});
			}
			
			_logChat.log(record);
		}
		
		if (Config.L2WALKER_PROTEC && _type == TELL && checkBot(_text))
		{
			Util.handleIllegalPlayerAction(activeChar, "Client Emulator Detect: Player " + activeChar.getName() + " using l2walker.", Config.DEFAULT_PUNISH);
			return;
		}
		_text = _text.replaceAll("\\\\n", "");
		
		if (Config.USE_SAY_FILTER)
		{
			checkText(activeChar);
		}
		
		L2Object saymode = activeChar.getSayMode();
		if (saymode != null)
		{
			String name = saymode.getName();
			int actor = saymode.getObjectId();
			_type = 0;
			
			Collection<L2Object> list = saymode.getKnownList().getKnownObjects().values();
			
			CreatureSay cs = new CreatureSay(actor, _type, name, _text);
			for (L2Object obj : list)
			{
				if (obj == null || !(obj instanceof L2Character))
				{
					continue;
				}
				
				L2Character chara = (L2Character) obj;
				chara.sendPacket(cs);
			}
			return;
		}
		
		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), _type, "" + (Config.SHOW_TIME_IN_CHAT ? "[" + fmt.format(new Date(System.currentTimeMillis())) + "]" : "") + " " + activeChar.getName(), _text);
		
		switch (_type)
		{
			case TELL:
				L2PcInstance receiver = L2World.getInstance().getPlayer(_target);
				
				if (receiver == null)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_ONLINE);
					sm.addString(_target);
					activeChar.sendPacket(sm);
					return;
				}
				
				if (!BlockList.isBlocked(receiver, activeChar))
				{
					if (activeChar.equals(receiver))
					{
						activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
						return;
					}
					
					if (Config.JAIL_DISABLE_CHAT && receiver.isInJail())
					{
						activeChar.sendMessage("Player is in jail.");
						return;
					}
					
					if (receiver.isChatBanned() && !activeChar.isGM())
					{
						activeChar.sendPacket(SystemMessageId.TARGET_IS_CHAT_BANNED);
						return;
					}
					
					if (receiver.isInOfflineMode())
					{
						activeChar.sendMessage("Player is in offline mode.");
						return;
					}
					
					if (receiver.getMessageRefusal() && !activeChar.isGM())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE));
						return;
					}
					
					receiver.sendPacket(new CreatureSay(activeChar.getObjectId(), _type, activeChar.getName(), (Config.SHOW_TIME_IN_CHAT ? "[" + fmt.format(new Date(System.currentTimeMillis())) + "]" : "") + " " + _text));
					activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), _type, "->" + receiver.getName(), (Config.SHOW_TIME_IN_CHAT ? "[" + fmt.format(new Date(System.currentTimeMillis())) + "]" : "") + " " + _text));
					
				}
				else if (BlockList.isBlocked(receiver, activeChar))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
					sm.addString(_target);
					activeChar.sendPacket(sm);
				}
				break;
			case SHOUT:
				
				if (activeChar.getLevel() <= (Config.MIN_LEVEL_FOR_CHAT - 1))
				{
					activeChar.sendMessage("Your level is too low for SHOUT chat. Min.: " + Config.MIN_LEVEL_FOR_CHAT);
					return;
				}
				
				if (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("on"))
				{
					int region = MapRegionTable.getInstance().getMapRegionLocId(activeChar.getX(), activeChar.getY());
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (region == MapRegionTable.getInstance().getMapRegionLocId(player.getX(), player.getY()))
						{
							if (!BlockList.isBlocked(player, activeChar))
							{
								player.sendPacket(cs);
							}
						}
					}
				}
				else if (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("GLOBAL"))
				{
					// Flood protect Say
					if (!getClient().getFloodProtectors().getGlobalChat().tryPerformAction("global chat"))
					{
						activeChar.sendMessage("You can send message every " + Config.FLOOD_PROTECTOR_GLOBAL_CHAT.FLOOD_PROTECTION_INTERVAL / 10 + " sec");
						return;
					}
					
					if (Config.GLOBAL_CHAT_WITH_PVP)
					{
						if ((activeChar.getPvpKills() < Config.GLOBAL_PVP_AMOUNT) && !activeChar.isGM())
						{
							activeChar.sendMessage("You must have at least " + Config.GLOBAL_PVP_AMOUNT + " pvp kills in order to speak in global chat");
							return;
						}
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (!BlockList.isBlocked(player, activeChar))
							{
								player.sendPacket(cs);
							}
						}
					}
					else
					{
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (!BlockList.isBlocked(player, activeChar))
							{
								player.sendPacket(cs);
							}
						}
					}
				}
				break;
			case TRADE:
				if (Config.DEFAULT_TRADE_CHAT.equalsIgnoreCase("ON"))
				{
					if (!getClient().getFloodProtectors().getTradeChat().tryPerformAction("trade chat"))
					{
						activeChar.sendMessage("You can send message every " + Config.FLOOD_PROTECTOR_TRADE_CHAT.FLOOD_PROTECTION_INTERVAL / 10 + " sec");
						return;
					}
					
					if (activeChar.getLevel() <= (Config.MIN_LEVEL_FOR_CHAT - 1))
					{
						activeChar.sendMessage("Your level is too low for TRADE chat. Min.: " + Config.MIN_LEVEL_FOR_CHAT);
						return;
					}
					
					if (Config.TRADE_CHAT_WITH_PVP)
					{
						if ((activeChar.getPvpKills() <= Config.TRADE_PVP_AMOUNT) && !activeChar.isGM())
						{
							activeChar.sendMessage("You must have at least " + Config.TRADE_PVP_AMOUNT + "  pvp kills in order to speak in trade chat");
							return;
						}
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (!BlockList.isBlocked(player, activeChar))
							{
								player.sendPacket(cs);
							}
						}
					}
					else
					{
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (!BlockList.isBlocked(player, activeChar))
							{
								player.sendPacket(cs);
							}
						}
					}
				}
				else if (Config.DEFAULT_TRADE_CHAT.equalsIgnoreCase("limited"))
				{
					if (activeChar.getLevel() <= (Config.MIN_LEVEL_FOR_CHAT - 1))
					{
						activeChar.sendMessage("Your level is too low for TRADE chat. Min.: " + Config.MIN_LEVEL_FOR_CHAT);
						return;
					}
					
					if (Config.TRADE_CHAT_IS_NOBLE)
					{
						if (!activeChar.isNoble() && !activeChar.isGM())
						{
							activeChar.sendMessage("Only nobles players can use this chat");
							return;
						}
						
						int region = MapRegionTable.getInstance().getMapRegionLocId(activeChar.getX(), activeChar.getY());
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (region == MapRegionTable.getInstance().getMapRegionLocId(player.getX(), player.getY()))
							{
								if (!BlockList.isBlocked(player, activeChar))
								{
									player.sendPacket(cs);
								}
							}
						}
						
					}
					else
					{
						int region = MapRegionTable.getInstance().getMapRegionLocId(activeChar.getX(), activeChar.getY());
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (region == MapRegionTable.getInstance().getMapRegionLocId(player.getX(), player.getY()))
							{
								if (!BlockList.isBlocked(player, activeChar))
								{
									player.sendPacket(cs);
								}
							}
						}
						
					}
					
				}
				break;
			case ALL:
				if (_text.startsWith(">") && activeChar.getPremiumService() == 1)
				{
					String text = _text;
					String reformatedText = (text).substring(1);
					
					CreatureSay pcs = new CreatureSay(activeChar.getObjectId(), SHOUT, "" + (Config.SHOW_TIME_IN_CHAT ? "[" + fmt.format(new Date(System.currentTimeMillis())) + "]" : "") + " " + activeChar.getName(), reformatedText);
					
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (!BlockList.isBlocked(player, activeChar))
						{
							player.sendPacket(pcs);
						}
					}
					break;
				}
				
				if (_text.startsWith("."))
				{
					StringTokenizer st = new StringTokenizer(_text);
					IVoicedCommandHandler vch;
					String command = "";
					String target = "";
					
					if (st.countTokens() > 1)
					{
						command = st.nextToken().substring(1);
						target = _text.substring(command.length() + 2);
						vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);
					}
					else
					{
						command = _text.substring(1);
						vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);
					}
					
					if (vch != null)
					{
						vch.useVoicedCommand(command, activeChar, target);
						break;
					}
				}
				
				for (L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
				{
					if (player != null && activeChar.isInsideRadius(player, 1250, false, true))
					{
						if (!BlockList.isBlocked(player, activeChar))
						{
							player.sendPacket(cs);
						}
					}
				}
				activeChar.sendPacket(cs);
				
				break;
			case CLAN:
				if (activeChar.getClan() != null)
				{
					activeChar.getClan().broadcastToOnlineMembers(cs);
				}
				break;
			case ALLIANCE:
				if (activeChar.getClan() != null)
				{
					activeChar.getClan().broadcastToOnlineAllyMembers(cs);
				}
				break;
			case PARTY:
				if (activeChar.isInParty())
				{
					activeChar.getParty().broadcastToPartyMembers(cs);
				}
				break;
			case PETITION_PLAYER:
			case PETITION_GM:
				if (!PetitionManager.getInstance().isPlayerInConsultation(activeChar))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_IN_PETITION_CHAT));
					break;
				}
				
				PetitionManager.getInstance().sendActivePetitionMessage(activeChar, _text);
				break;
			case PARTYMATCH_ROOM: // 14
				if (activeChar.isInPartyMatchRoom())
				{
					PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(activeChar);
					if (_room != null)
					{
						for (L2PcInstance _member : _room.getPartyMembers())
						{
							_member.sendPacket(cs);
						}
					}
				}
				break;
			case PARTYROOM_COMMANDER: // 15
				if (activeChar.isInParty())
				{
					if (activeChar.getParty().isInCommandChannel() && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar))
					{
						activeChar.getParty().getCommandChannel().broadcastCSToChannelMembers(cs, activeChar);
					}
				}
				break;
			case PARTYROOM_ALL: // 16
				if (activeChar.isInParty())
				{
					if (activeChar.getParty().isInCommandChannel() && activeChar.getParty().isLeader(activeChar))
					{
						activeChar.getParty().getCommandChannel().broadcastCSToChannelMembers(cs, activeChar);
					}
				}
				break;
			case HERO_VOICE:
				if (activeChar.isGM())
				{
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (player == null)
						{
							continue;
						}
						
						player.sendPacket(cs);
					}
				}
				else if (activeChar.isHero())
				{
					if (!getClient().getFloodProtectors().getHeroVoice().tryPerformAction("hero voice"))
					{
						activeChar.sendMessage("You can send message every " + Config.FLOOD_PROTECTOR_HERO_VOICE.FLOOD_PROTECTION_INTERVAL / 10 + " sec.");
						return;
					}
					
					for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					{
						if (player == null)
						{
							continue;
						}
						
						if (!BlockList.isBlocked(player, activeChar))
						{
							player.sendPacket(cs);
						}
					}
				}
				break;
		}
	}
	
	private static final String[] WALKER_COMMAND_LIST =
	{
		"USESKILL",
		"USEITEM",
		"BUYITEM",
		"SELLITEM",
		"SAVEITEM",
		"LOADITEM",
		"MSG",
		"SET",
		"DELAY",
		"LABEL",
		"JMP",
		"CALL",
		"RETURN",
		"MOVETO",
		"NPCSEL",
		"NPCDLG",
		"DLGSEL",
		"CHARSTATUS",
		"POSOUTRANGE",
		"POSINRANGE",
		"GOHOME",
		"SAY",
		"EXIT",
		"PAUSE",
		"STRINDLG",
		"STRNOTINDLG",
		"CHANGEWAITTYPE",
		"FORCEATTACK",
		"ISMEMBER",
		"REQUESTJOINPARTY",
		"REQUESTOUTPARTY",
		"QUITPARTY",
		"MEMBERSTATUS",
		"CHARBUFFS",
		"ITEMCOUNT",
		"FOLLOWTELEPORT"
	};
	
	private boolean checkBot(String text)
	{
		for (String botCommand : WALKER_COMMAND_LIST)
		{
			if (text.startsWith(botCommand))
			{
				return true;
			}
		}
		return false;
	}
	
	private void checkText(L2PcInstance activeChar)
	{
		if (Config.USE_SAY_FILTER)
		{
			String filteredText = _text.toLowerCase();
			
			for (String pattern : Config.FILTER_LIST)
			{
				filteredText = filteredText.replaceAll("(?i)" + pattern, Config.CHAT_FILTER_CHARS);
			}
			
			if (!filteredText.equalsIgnoreCase(_text))
			{
				if (Config.CHAT_FILTER_PUNISHMENT.equalsIgnoreCase("chat"))
				{
					activeChar.setPunishLevel(PunishLevel.CHAT, Config.CHAT_FILTER_PUNISHMENT_PARAM1);
					activeChar.sendMessage("The chat has been automatically banned for " + Config.CHAT_FILTER_PUNISHMENT_PARAM1 + " min.");
				}
				else if (Config.CHAT_FILTER_PUNISHMENT.equalsIgnoreCase("karma"))
				{
					activeChar.setKarma(Config.CHAT_FILTER_PUNISHMENT_PARAM2);
					activeChar.sendMessage("You've got " + Config.CHAT_FILTER_PUNISHMENT_PARAM2 + " karma for bad words.");
				}
				else if (Config.CHAT_FILTER_PUNISHMENT.equalsIgnoreCase("jail"))
				{
					activeChar.setPunishLevel(PunishLevel.JAIL, Config.CHAT_FILTER_PUNISHMENT_PARAM1);
				}
				
				activeChar.sendMessage("The word: " + _text + " is not allowed here.");
				_text = filteredText;
			}
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 38 Say2";
	}
}