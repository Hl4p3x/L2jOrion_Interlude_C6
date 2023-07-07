/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ChestInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.CharInfo;
import l2jorion.game.network.serverpackets.Earthquake;
import l2jorion.game.network.serverpackets.ExRedSky;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SignsSky;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.StopMove;
import l2jorion.game.network.serverpackets.SunRise;
import l2jorion.game.network.serverpackets.SunSet;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.network.serverpackets.UserInfo;

public class AdminEffects implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_invis",
		"admin_invisible",
		"admin_vis",
		"admin_visible",
		"admin_invis_menu",
		"admin_earthquake",
		"admin_earthquake_menu",
		"admin_bighead",
		"admin_shrinkhead",
		"admin_gmspeed",
		"admin_gmspeed_menu",
		"admin_unpara_all",
		"admin_para_all",
		"admin_para_world",
		"admin_unpara_world",
		"admin_unpara",
		"admin_para",
		"admin_unpara_all_menu",
		"admin_para_all_menu",
		"admin_unpara_menu",
		"admin_para_menu",
		"admin_polyself",
		"admin_unpolyself",
		"admin_polyself_menu",
		"admin_unpolyself_menu",
		"admin_clearteams",
		"admin_setteam_close",
		"admin_setteam",
		"admin_social",
		"admin_effect",
		"admin_social_menu",
		"admin_effect_menu",
		"admin_abnormal",
		"admin_abnormal_menu",
		"admin_play_sounds",
		"admin_play_sound",
		"admin_atmosphere",
		"admin_atmosphere_menu",
		"admin_npc_say",
		"admin_debuff"
	};
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (command.equals("admin_invis_menu"))
		{
			if (!activeChar.getAppearance().getInvisible())
			{
				activeChar.getAppearance().setInvisible();
				activeChar.decayMe();
				L2World.getInstance().addPlayerToWorld(activeChar);
				activeChar.broadcastUserInfo();
				activeChar.spawnMe();
			}
			else
			{
				activeChar.getAppearance().setVisible();
				activeChar.broadcastUserInfo();
			}
		}
		
		else if (command.startsWith("admin_invis"))
		{
			activeChar.getAppearance().setInvisible();
			activeChar.decayMe();
			L2World.getInstance().addPlayerToWorld(activeChar);
			activeChar.broadcastUserInfo();
			activeChar.spawnMe();
		}
		
		else if (command.startsWith("admin_vis"))
		{
			activeChar.getAppearance().setVisible();
			L2World.getInstance().addPlayerToWorld(activeChar);
			activeChar.broadcastUserInfo();
		}
		
		else if (command.startsWith("admin_earthquake"))
		{
			try
			{
				String val1 = st.nextToken();
				final int intensity = Integer.parseInt(val1);
				String val2 = st.nextToken();
				final int duration = Integer.parseInt(val2);
				final Earthquake eq = new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), intensity, duration);
				activeChar.broadcastPacket(eq);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Use: //earthquake <intensity> <duration>");
			}
		}
		
		else if (command.startsWith("admin_atmosphere"))
		{
			try
			{
				String type = st.nextToken();
				String state = st.nextToken();
				adminAtmosphere(type, state, activeChar);
				type = null;
				state = null;
			}
			catch (final Exception ex)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					ex.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("admin_npc_say"))
		{
			try
			{
				L2Object target = activeChar.getTarget();
				
				if (activeChar.getSayMode() != null)
				{
					activeChar.setSayMode(null);
					activeChar.sendMessage("NpcSay mode off");
				}
				else
				{
					if (target != null && target instanceof L2NpcInstance)
					{
						activeChar.setSayMode(target);
						activeChar.sendMessage("NpcSay mode on for " + target.getName());
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
						
						return false;
					}
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Target Npc before. Use: //npc_say");
			}
		}
		
		else if (command.equals("admin_play_sounds"))
		{
			AdminHelpPage.showHelpPage(activeChar, "songs/songs.htm");
		}
		else if (command.startsWith("admin_play_sounds"))
		{
			try
			{
				AdminHelpPage.showHelpPage(activeChar, "songs/songs" + command.substring(17) + ".htm");
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		else if (command.startsWith("admin_play_sound"))
		{
			try
			{
				playAdminSound(activeChar, command.substring(17));
			}
			catch (final StringIndexOutOfBoundsException e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.equals("admin_para") || command.equals("admin_para_menu"))
		{
			String type = "1";
			try
			{
				type = st.nextToken();
			}
			catch (final Exception e)
			{
			}
			try
			{
				final L2Object target = activeChar.getTarget();
				L2Character player = null;
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					if (type.equals("1"))
					{
						player.startAbnormalEffect(0x0400);
					}
					else
					{
						player.startAbnormalEffect(0x0800);
					}
					
					player.startParalyze();
					final StopMove sm = new StopMove(player);
					player.sendPacket(sm);
					player.broadcastPacket(sm);
				}
			}
			catch (final Exception e)
			{
			}
		}
		
		else if (command.equals("admin_unpara") || command.equals("admin_unpara_menu"))
		{
			try
			{
				final L2Object target = activeChar.getTarget();
				L2Character player = null;
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					player.stopAbnormalEffect((short) 0x0400);
					player.setIsParalyzed(false);
				}
			}
			catch (final Exception e)
			{
			}
		}
		
		else if (command.startsWith("admin_para_all"))
		{
			try
			{
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
				{
					if (!player.isGM())
					{
						player.startAbnormalEffect(0x0400);
						player.startParalyze();
						final StopMove sm = new StopMove(player);
						player.sendPacket(sm);
						player.broadcastPacket(sm);
					}
				}
			}
			catch (final Exception e)
			{
			}
		}
		
		else if (command.startsWith("admin_unpara_all"))
		{
			try
			{
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
				{
					player.stopAbnormalEffect(0x0400);
					player.setIsParalyzed(false);
				}
			}
			catch (final Exception e)
			{
			}
		}
		
		else if (command.startsWith("admin_para_world"))
		{
			try
			{
				for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					if (!player.isGM())
					{
						player.startAbnormalEffect(0x0400);
						player.startParalyze();
						final StopMove sm = new StopMove(player);
						player.sendPacket(sm);
						player.broadcastPacket(sm);
					}
				}
			}
			catch (final Exception e)
			{
			}
		}
		
		else if (command.startsWith("admin_unpara_world"))
		{
			try
			{
				for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
				{
					player.stopAbnormalEffect(0x0400);
					player.setIsParalyzed(false);
				}
			}
			catch (final Exception e)
			{
			}
		}
		
		else if (command.startsWith("admin_bighead"))
		{
			try
			{
				L2Object target = activeChar.getTarget();
				L2Character player = null;
				
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					player.startAbnormalEffect(0x2000);
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("admin_shrinkhead"))
		{
			try
			{
				L2Object target = activeChar.getTarget();
				L2Character player = null;
				
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					player.stopAbnormalEffect((short) 0x2000);
				}
				
				target = null;
				player = null;
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("admin_gmspeed"))
		{
			try
			{
				final int val = Integer.parseInt(st.nextToken());
				final boolean sendMessage = activeChar.getFirstEffect(7029) != null;
				
				activeChar.stopSkillEffects(7029);
				
				if (val == 0 && sendMessage)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.EFFECT_S1_DISAPPEARED).addSkillName(7029));
				}
				else if (val >= 1 && val <= 4)
				{
					final L2Skill gmSpeedSkill = SkillTable.getInstance().getInfo(7029, val);
					activeChar.doCast(gmSpeedSkill);
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Use //gmspeed value (0=off...4=max).");
			}
			finally
			{
				activeChar.updateEffectIcons();
			}
		}
		
		else if (command.startsWith("admin_polyself"))
		{
			try
			{
				String id = st.nextToken();
				
				activeChar.getPoly().setPolyInfo("npc", id);
				activeChar.teleToLocation(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false);
				
				CharInfo info1 = new CharInfo(activeChar);
				activeChar.broadcastPacket(info1);
				UserInfo info2 = new UserInfo(activeChar);
				activeChar.sendPacket(info2);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("admin_unpolyself"))
		{
			try
			{
				activeChar.getPoly().setPolyInfo(null, "1");
				activeChar.decayMe();
				activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				
				CharInfo info1 = new CharInfo(activeChar);
				activeChar.broadcastPacket(info1);
				UserInfo info2 = new UserInfo(activeChar);
				activeChar.sendPacket(info2);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.equals("admin_clear_teams"))
		{
			try
			{
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
				{
					player.setTeam(0);
					player.broadcastUserInfo();
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("admin_setteam_close"))
		{
			try
			{
				String val = st.nextToken();
				
				final int teamVal = Integer.parseInt(val);
				
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values())
				{
					if (activeChar.isInsideRadius(player, 400, false, true))
					{
						player.setTeam(0);
						
						if (teamVal != 0)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
							sm.addString("You have joined team " + teamVal);
							player.sendPacket(sm);
						}
						
						player.broadcastUserInfo();
					}
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("admin_setteam"))
		{
			String val = command.substring(14);
			
			final int teamVal = Integer.parseInt(val);
			
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			
			if (target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}
			
			player.setTeam(teamVal);
			
			if (teamVal != 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString("You have joined team " + teamVal);
				player.sendPacket(sm);
			}
			
			player.broadcastUserInfo();
		}
		
		else if (command.startsWith("admin_social"))
		{
			try
			{
				String target = null;
				L2Object obj = activeChar.getTarget();
				
				if (st.countTokens() == 2)
				{
					final int social = Integer.parseInt(st.nextToken());
					
					target = st.nextToken();
					
					if (target != null)
					{
						L2PcInstance player = L2World.getInstance().getPlayer(target);
						
						if (player != null)
						{
							if (performSocial(social, player, activeChar))
							{
								activeChar.sendMessage(player.getName() + " was affected by your request.");
							}
						}
						else
						{
							try
							{
								final int radius = Integer.parseInt(target);
								
								for (final L2Object object : activeChar.getKnownList().getKnownObjects().values())
								{
									if (activeChar.isInsideRadius(object, radius, false, false))
									{
										performSocial(social, object, activeChar);
									}
								}
								
								activeChar.sendMessage(radius + " units radius affected by your request.");
							}
							catch (final NumberFormatException nbe)
							{
								if (Config.ENABLE_ALL_EXCEPTIONS)
								{
									nbe.printStackTrace();
								}
								
								activeChar.sendMessage("Incorrect parameter");
							}
						}
					}
				}
				else if (st.countTokens() == 1)
				{
					final int social = Integer.parseInt(st.nextToken());
					
					if (obj == null)
					{
						obj = activeChar;
					}
					
					if (performSocial(social, obj, activeChar))
					{
						activeChar.sendMessage(obj.getName() + " was affected by your request.");
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					}
				}
				else if (!command.contains("menu"))
				{
					activeChar.sendMessage("Usage: //social <social_id> [player_name|radius]");
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("debuff"))
		{
			try
			{
				L2Object target = activeChar.getTarget();
				L2Character player = null;
				
				if (target instanceof L2Character)
				{
					player = (L2Character) target;
					player.stopAllEffects();
					activeChar.sendMessage("Effects has been cleared from " + player + ".");
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("admin_abnormal"))
		{
			try
			{
				String target = null;
				L2Object obj = activeChar.getTarget();
				
				if (st.countTokens() == 2)
				{
					String parm = st.nextToken();
					
					final int abnormal = Integer.decode("0x" + parm);
					
					target = st.nextToken();
					
					if (target != null)
					{
						L2PcInstance player = L2World.getInstance().getPlayer(target);
						
						if (player != null)
						{
							if (performAbnormal(abnormal, player))
							{
								activeChar.sendMessage(player.getName() + "'s abnormal status was affected by your request.");
							}
							else
							{
								activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
							}
						}
						else
						{
							try
							{
								final int radius = Integer.parseInt(target);
								
								for (final L2Object object : activeChar.getKnownList().getKnownObjects().values())
								{
									if (activeChar.isInsideRadius(object, radius, false, false))
									{
										performAbnormal(abnormal, object);
									}
								}
								
								activeChar.sendMessage(radius + " units radius affected by your request.");
							}
							catch (final NumberFormatException nbe)
							{
								if (Config.ENABLE_ALL_EXCEPTIONS)
								{
									nbe.printStackTrace();
								}
								
								activeChar.sendMessage("Usage: //abnormal <hex_abnormal_mask> [player|radius]");
							}
						}
					}
				}
				else if (st.countTokens() == 1)
				{
					final int abnormal = Integer.decode("0x" + st.nextToken());
					
					if (obj == null)
					{
						obj = activeChar;
					}
					
					if (performAbnormal(abnormal, obj))
					{
						activeChar.sendMessage(obj.getName() + "'s abnormal status was affected by your request.");
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					}
				}
				else if (!command.contains("menu"))
				{
					activeChar.sendMessage("Usage: //abnormal <abnormal_mask> [player_name|radius]");
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
		
		else if (command.startsWith("admin_effect"))
		{
			try
			{
				L2Object obj = activeChar.getTarget();
				
				int level = 1, hittime = 1;
				final int skill = Integer.parseInt(st.nextToken());
				
				if (st.hasMoreTokens())
				{
					level = Integer.parseInt(st.nextToken());
				}
				
				if (st.hasMoreTokens())
				{
					hittime = Integer.parseInt(st.nextToken());
				}
				
				if (obj == null)
				{
					obj = activeChar;
				}
				
				if (!(obj instanceof L2Character))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
				}
				else
				{
					final L2Character target = (L2Character) obj;
					
					target.broadcastPacket(new MagicSkillUser(target, activeChar, skill, level, hittime, 0));
					activeChar.sendMessage(obj.getName() + " performs MSU " + skill + "/" + level + " by your request.");
					
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				activeChar.sendMessage("Usage: //effect skill [level | level hittime]");
			}
		}
		
		if (command.contains("menu"))
		{
			showMainPage(activeChar, command);
		}
		
		return true;
	}
	
	private boolean performAbnormal(final int action, final L2Object target)
	{
		if (target instanceof L2Character)
		{
			L2Character character = (L2Character) target;
			
			if ((character.getAbnormalEffect() & action) == action)
			{
				character.stopAbnormalEffect(action);
			}
			else
			{
				character.startAbnormalEffect(action);
			}
			
			return true;
		}
		return false;
	}
	
	private boolean performSocial(final int action, final L2Object target, final L2PcInstance activeChar)
	{
		try
		{
			if (target instanceof L2Character)
			{
				if (target instanceof L2Summon || target instanceof L2ChestInstance)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					
					return false;
				}
				
				if (target instanceof L2NpcInstance && (action < 1 || action > 3))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					
					return false;
				}
				
				if (target instanceof L2PcInstance && (action < 2 || action > 16))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					
					return false;
				}
				
				L2Character character = (L2Character) target;
				character.broadcastPacket(new SocialAction(target.getObjectId(), action));
			}
			else
			{
				return false;
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		return true;
	}
	
	private void adminAtmosphere(final String type, final String state, final L2PcInstance activeChar)
	{
		PacketServer packet = null;
		
		switch (type)
		{
			case "signsky":
				if (state.equals("dawn"))
				{
					packet = new SignsSky(2);
				}
				else if (state.equals("dusk"))
				{
					packet = new SignsSky(1);
				}
				break;
			case "sky":
				if (state.equals("night"))
				{
					packet = new SunSet();
				}
				else if (state.equals("day"))
				{
					packet = new SunRise();
				}
				else if (state.equals("red"))
				{
					packet = new ExRedSky(10);
				}
				break;
			default:
				activeChar.sendMessage("Usage: //atmosphere <signsky dawn|dusk>|<sky day|night|red>");
				break;
		}
		
		if (packet != null)
		{
			for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
			{
				player.sendPacket(packet);
			}
		}
	}
	
	private void playAdminSound(final L2PcInstance activeChar, final String sound)
	{
		PlaySound _snd = new PlaySound(1, sound, 0, 0, 0, 0, 0);
		activeChar.sendPacket(_snd);
		activeChar.broadcastPacket(_snd);
		activeChar.sendMessage("Playing " + sound + ".");
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showMainPage(final L2PcInstance activeChar, final String command)
	{
		String filename = "effects_menu";
		
		if (command.contains("abnormal"))
		{
			filename = "abnormal";
		}
		else if (command.contains("social"))
		{
			filename = "social";
		}
		
		AdminHelpPage.showHelpPage(activeChar, filename + ".htm");
	}
}
