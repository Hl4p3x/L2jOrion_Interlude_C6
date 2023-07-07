package l2jorion.game.handler.admin;

import java.util.StringTokenizer;
import java.util.stream.Collectors;

import l2jorion.bots.FakePlayer;
import l2jorion.bots.FakePlayerManager;
import l2jorion.bots.FakePlayerTaskManager;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

public class AdminFakePlayers implements IAdminCommandHandler
{
	private final String fakesFolder = "data/html/admin/fakeplayers/";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_bots",
		"admin_spawnbot",
		"admin_spawnrandom",
		"admin_deletebot",
		"admin_spawnenchanter",
		"admin_spawnwalker",
		"admin_deleteallbots",
		"admin_killallbots",
		"admin_ressallbots",
		"admin_takecontrol",
		"admin_releasecontrol"
	};
	
	private enum CommandEnum
	{
		admin_bots,
		admin_spawnbot,
		admin_spawnrandom,
		admin_deletebot,
		admin_spawnenchanter,
		admin_spawnwalker,
		admin_deleteallbots,
		admin_killallbots,
		admin_ressallbots,
		admin_takecontrol,
		admin_releasecontrol
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showFakeDashboard(L2PcInstance activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(1);
		
		html.setFile(fakesFolder + "bots.htm");
		html.replace("%fakecount%", FakePlayerManager.INSTANCE.getFakePlayersCount());
		html.replace("%taskcount%", FakePlayerTaskManager.INSTANCE.getTaskCount());
		
		activeChar.sendPacket(html);
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer stt = new StringTokenizer(command);
		
		String comm_s = stt.nextToken();
		
		CommandEnum comm = CommandEnum.valueOf(comm_s);
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_bots:
			{
				// showFakeDashboard(activeChar);
				break;
			}
			case admin_spawnbot:
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				
				Class<? extends L2Character> targetClass = null;
				
				int fake_number = 1;
				String type = null;
				int range = 2000;
				int radius = 0;
				String target = null;
				
				fake_number = Integer.parseInt(st.nextToken());
				type = st.nextToken();
				range = Integer.parseInt(st.nextToken());
				target = st.nextToken();
				radius = Integer.parseInt(st.nextToken());
				
				switch (target.toUpperCase())
				{
					case "PLAYER":
						targetClass = L2PcInstance.class;
						break;
					case "MONSTER":
						targetClass = L2MonsterInstance.class;
						break;
					case "BOTH":
						targetClass = L2Character.class;
						break;
				}
				
				// Corrections
				if (range < 2000)
				{
					range = 2000;
				}
				
				switch (type)
				{
					case "PVP_ZONE":
						for (int i = 0; i < fake_number; i++)
						{
							final int x = (int) (radius * Math.cos(i * 1.777));
							final int y = (int) (radius * Math.sin(i * 1.777));
							
							FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(activeChar.getX() + x, activeChar.getY() + y, activeChar.getZ(), 81, 3, true, false, false);
							fakePlayer.setTargetClass(targetClass);
							fakePlayer.setTargetRange(range);
							fakePlayer.assignDefaultAI();
						}
						break;
					case "COMBAT":
						for (int i = 0; i < fake_number; i++)
						{
							final int x = (int) (radius * Math.cos(i * 1.777));
							final int y = (int) (radius * Math.sin(i * 1.777));
							
							FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(activeChar.getX() + x, activeChar.getY() + y, activeChar.getZ(), 81, 3, false, false, false);
							fakePlayer.setTargetClass(targetClass);
							fakePlayer.setTargetRange(range);
							fakePlayer.assignDefaultAI();
						}
						break;
				}
				
				showFakeDashboard(activeChar);
				break;
			}
			case admin_deletebot:
			{
				if (activeChar.getTarget() != null && activeChar.getTarget() instanceof FakePlayer)
				{
					FakePlayer fakePlayer = (FakePlayer) activeChar.getTarget();
					fakePlayer.despawnPlayer();
				}
				break;
			}
			case admin_spawnwalker:
			{
				/*
				 * if (command.contains(" ")) { String locationName = command.split(" ")[1]; FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(activeChar.getX(), activeChar.getY(), activeChar.getZ()); switch (locationName) { case "giran": fakePlayer.setFakeAi(new WalkerAI(fakePlayer));
				 * break; } return true; }
				 */
				break;
			}
			case admin_spawnenchanter:
			{
				/*
				 * FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(activeChar.getX(), activeChar.getY(), activeChar.getZ()); fakePlayer.setFakeAi(new EnchanterAI(fakePlayer));
				 */
				break;
			}
			case admin_spawnrandom:
			{
				/*
				 * FakePlayer fakePlayer = FakePlayerManager.INSTANCE.spawnPlayer(activeChar.getX(), activeChar.getY(), activeChar.getZ()); fakePlayer.assignDefaultAI(); if (command.contains(" ")) { String arg = command.split(" ")[1]; if (arg.equalsIgnoreCase("htm")) { showFakeDashboard(activeChar);
				 * } }
				 */
				break;
			}
			case admin_takecontrol:
			{
				if (activeChar.getTarget() != null && activeChar.getTarget() instanceof FakePlayer)
				{
					FakePlayer fakePlayer = (FakePlayer) activeChar.getTarget();
					fakePlayer.setUnderControl(true);
					activeChar.setPlayerUnderControl(fakePlayer);
					activeChar.sendMessage("You are now controlling: " + fakePlayer.getName());
					return true;
				}
				activeChar.sendMessage("You can only take control of a Bot Player");
				break;
			}
			case admin_releasecontrol:
			{
				if (activeChar.isControllingFakePlayer())
				{
					FakePlayer fakePlayer = activeChar.getPlayerUnderControl();
					activeChar.sendMessage("You are no longer controlling: " + fakePlayer.getName());
					fakePlayer.setUnderControl(false);
					activeChar.setPlayerUnderControl(null);
					return true;
				}
				activeChar.sendMessage("You are not controlling a Bot Player");
				break;
			}
			case admin_deleteallbots:
			{
				int number = L2World.getInstance().getPlayers().stream().filter(player -> player instanceof FakePlayer).collect(Collectors.toList()).size();
				L2World.getInstance().getPlayers().stream().filter(player -> player instanceof FakePlayer).forEach(player -> ((FakePlayer) player).despawnPlayer());
				activeChar.sendMessage("Kickked: " + number);
				break;
			}
			case admin_killallbots:
			{
				int number = L2World.getInstance().getPlayers().stream().filter(player -> player instanceof FakePlayer).collect(Collectors.toList()).size();
				L2World.getInstance().getPlayers().stream().filter(player -> player instanceof FakePlayer).forEach(player -> player.reduceCurrentHp(player.getMaxHp() + player.getMaxCp() + 1, activeChar));
				activeChar.sendMessage("Killed: " + number);
				break;
			}
			case admin_ressallbots:
			{
				int number = L2World.getInstance().getPlayers().stream().filter(player -> player instanceof FakePlayer).collect(Collectors.toList()).size();
				L2World.getInstance().getPlayers().stream().filter(player -> player instanceof FakePlayer).forEach(player -> player.doRevive());
				activeChar.sendMessage("Resurrected: " + number);
				break;
			}
			default:
				break;
		}
		
		return true;
	}
}