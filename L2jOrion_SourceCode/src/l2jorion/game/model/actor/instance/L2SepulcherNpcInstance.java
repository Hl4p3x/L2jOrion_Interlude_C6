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
package l2jorion.game.model.actor.instance;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.managers.FourSepulchersManager;
import l2jorion.game.model.L2World;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

/**
 * @author sandman
 */
public class L2SepulcherNpcInstance extends L2NpcInstance
{
	private static Logger LOG = LoggerFactory.getLogger(L2SepulcherNpcInstance.class);
	protected static Map<Integer, Integer> _hallGateKeepers = new FastMap<>();
	
	protected Future<?> _closeTask = null;
	protected Future<?> _spawnNextMysteriousBoxTask = null;
	protected Future<?> _spawnMonsterTask = null;
	
	private final static String HTML_FILE_PATH = "data/html/SepulcherNpc/";
	private final static int HALLS_KEY = 7260;
	
	public L2SepulcherNpcInstance(final int objectID, final L2NpcTemplate template)
	{
		super(objectID, template);
		// setShowSummonAnimation(true);
		
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
		}
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
		}
		if (_spawnMonsterTask != null)
		{
			_spawnMonsterTask.cancel(true);
		}
		_closeTask = null;
		_spawnNextMysteriousBoxTask = null;
		_spawnMonsterTask = null;
	}
	
	@Override
	public void deleteMe()
	{
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
			_closeTask = null;
		}
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
			_spawnNextMysteriousBoxTask = null;
		}
		if (_spawnMonsterTask != null)
		{
			_spawnMonsterTask.cancel(true);
			_spawnMonsterTask = null;
		}
		super.deleteMe();
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			if (Config.DEBUG)
			{
				LOG.info("new target selected:" + getObjectId());
			}
			
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				// Send a Server->Client packet MyTargetSelected to the
				// L2PcInstance player
				// The player.getLevel() - getLevel() permit to display the
				// correct color in the select window
				final MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
				player.sendPacket(my);
				
				// Send a Server->Client packet StatusUpdate of the
				// L2NpcInstance to the L2PcInstance to update its HP bar
				final StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			else
			{
				// Send a Server->Client packet MyTargetSelected to the
				// L2PcInstance player
				final MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
				player.sendPacket(my);
			}
			
			// Send a Server->Client packet ValidateLocation to correct the
			// L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Check if the player is attackable (without a forced attack) and
			// isn't dead
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				// Check the height difference
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth
				// difference might
				// need some tweaking
				{
					// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
				else
				{
					// Send a Server->Client packet ActionFailed (target is out
					// of attack range) to the L2PcInstance player
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			
			if (!isAutoAttackable(player))
			{
				// Calculate the distance between the L2PcInstance and the
				// L2NpcInstance
				if (!canInteract(player))
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Like L2OFF player must rotate to the Npc
					MoveToPawn sp = new MoveToPawn(player, this, L2NpcInstance.INTERACTION_DISTANCE);
					player.sendPacket(sp);
					Broadcast.toKnownPlayers(player, sp);
					
					// Send a Server->Client packet SocialAction to the all
					// L2PcInstance on the _knownPlayer of the L2NpcInstance
					// to display a social action of the L2NpcInstance on their
					// client
					final SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
					broadcastPacket(sa);
					
					doAction(player);
				}
			}
			// Send a Server->Client ActionFailed to the L2PcInstance in order
			// to avoid that the client wait another packet
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	private void doAction(L2PcInstance player)
	{
		if (isDead())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		switch (getNpcId())
		{
			case 31468:// 4s first room first box
			case 31469:// 4s second room first box
			case 31470:
			case 31471:
			case 31472:
			case 31473:
			case 31474:
			case 31475:
			case 31476:
			case 31477:
			case 31478:
			case 31479:
			case 31480:
			case 31481:
			case 31482:
			case 31483:
			case 31484:
			case 31485:
			case 31486:
			case 31487:
				setIsInvul(false);
				reduceCurrentHp(getMaxHp() + 1, player);
				if (_spawnMonsterTask != null)
				{
					_spawnMonsterTask.cancel(true);
				}
				_spawnMonsterTask = ThreadPoolManager.getInstance().scheduleEffect(new SpawnMonster(getNpcId()), 3500);
				break;
			
			case 31455:// first room key box
			case 31456:
			case 31457:
			case 31458:// second room key box
			case 31459:
			case 31460:
			case 31461:
			case 31462:
			case 31463:
			case 31464:
			case 31465:
			case 31466:
			case 31467:
				setIsInvul(false);
				reduceCurrentHp(getMaxHp() + 1, player);
				if (player.getParty() != null && !player.getParty().isLeader(player))
				{
					player = player.getParty().getLeader();
				}
				player.addItem("Quest", HALLS_KEY, 1, player, true);
				break;
			
			default:
			{
				if (getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START).length > 0)
				{
					player.setLastQuestNpcObject(getObjectId());
				}
				final Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_TALK);
				if (qlst.length == 1)
				{
					qlst[0].notifyFirstTalk(this, player);
				}
				else
				{
					showChatWindow(player, 0);
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return HTML_FILE_PATH + pom + ".htm";
	}
	
	@Override
	public void showChatWindow(final L2PcInstance player, final int val)
	{
		final String filename = getHtmlPath(player, getNpcId(), val);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (isBusy())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/npcbusy.htm");
			html.replace("%busymessage%", getBusyMessage());
			html.replace("%npcname%", getName());
			html.replace("%playername%", player.getName());
			player.sendPacket(html);
		}
		else if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException | NumberFormatException ioobe)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					ioobe.printStackTrace();
				}
			}
			showChatWindow(player, val);
		}
		else if (command.startsWith("open_gate"))
		{
			final L2ItemInstance hallsKey = player.getInventory().getItemByItemId(HALLS_KEY);
			if (hallsKey == null)
			{
				showHtmlFile(player, "Gatekeeper-no.htm");
			}
			else if (FourSepulchersManager.getInstance().isAttackTime())
			{
				switch (getNpcId())
				{
					case 31929:
					case 31934:
					case 31939:
					case 31944:
						FourSepulchersManager.getInstance().spawnShadow(getNpcId());
					default:
					{
						openNextDoor(getNpcId());
						if (player.getParty() != null)
						{
							for (final L2PcInstance mem : player.getParty().getPartyMembers())
							{
								if (mem.getInventory().getItemByItemId(HALLS_KEY) != null)
								{
									mem.destroyItemByItemId("Quest", HALLS_KEY, mem.getInventory().getItemByItemId(HALLS_KEY).getCount(), mem, true);
								}
							}
						}
						else
						{
							player.destroyItemByItemId("Quest", HALLS_KEY, hallsKey.getCount(), player, true);
						}
					}
				}
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public void openNextDoor(final int npcId)
	{
		final int doorId = FourSepulchersManager.getInstance().getHallGateKeepers().get(npcId).intValue();
		final DoorTable _doorTable = DoorTable.getInstance();
		_doorTable.getDoor(doorId).openMe();
		
		if (_closeTask != null)
		{
			_closeTask.cancel(true);
		}
		_closeTask = ThreadPoolManager.getInstance().scheduleEffect(new CloseNextDoor(doorId), 10000);
		if (_spawnNextMysteriousBoxTask != null)
		{
			_spawnNextMysteriousBoxTask.cancel(true);
		}
		_spawnNextMysteriousBoxTask = ThreadPoolManager.getInstance().scheduleEffect(new SpawnNextMysteriousBox(npcId), 0);
	}
	
	private class CloseNextDoor implements Runnable
	{
		final DoorTable _DoorTable = DoorTable.getInstance();
		
		private final int _DoorId;
		
		public CloseNextDoor(final int doorId)
		{
			_DoorId = doorId;
		}
		
		@Override
		@SuppressWarnings("synthetic-access")
		public void run()
		{
			try
			{
				_DoorTable.getDoor(_DoorId).closeMe();
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn(e.getMessage());
			}
		}
	}
	
	private class SpawnNextMysteriousBox implements Runnable
	{
		private final int _NpcId;
		
		public SpawnNextMysteriousBox(final int npcId)
		{
			_NpcId = npcId;
		}
		
		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnMysteriousBox(_NpcId);
		}
	}
	
	private class SpawnMonster implements Runnable
	{
		private final int _NpcId;
		
		public SpawnMonster(final int npcId)
		{
			_NpcId = npcId;
		}
		
		@Override
		public void run()
		{
			FourSepulchersManager.getInstance().spawnMonster(_NpcId);
		}
	}
	
	public void sayInShout(final String msg)
	{
		if (msg == null || msg.isEmpty())
		{
			return;// wrong usage
		}
		
		final Collection<L2PcInstance> knownPlayers = L2World.getInstance().getAllPlayers().values();
		if (knownPlayers == null || knownPlayers.isEmpty())
		{
			return;
		}
		
		final CreatureSay sm = new CreatureSay(0, 1, getName(), msg);
		for (final L2PcInstance player : knownPlayers)
		{
			if (player == null)
			{
				continue;
			}
			
			if (Util.checkIfInRange(15000, player, this, true))
			{
				player.sendPacket(sm);
			}
		}
	}
	
	public void showHtmlFile(final L2PcInstance player, final String file)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/SepulcherNpc/" + file);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
