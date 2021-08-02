/*
 * L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.model.actor.instance;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;

import javolution.util.FastList;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2DoorAI;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Territory;
import l2jorion.game.model.actor.knownlist.DoorKnownList;
import l2jorion.game.model.actor.stat.DoorStat;
import l2jorion.game.model.actor.status.DoorStatus;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.entity.siege.hallsiege.SiegableHall;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ConfirmDlg;
import l2jorion.game.network.serverpackets.DoorInfo;
import l2jorion.game.network.serverpackets.DoorStatusUpdate;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2DoorTemplate;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class L2DoorInstance extends L2Character
{
	protected static final Logger LOG = LoggerFactory.getLogger(L2DoorInstance.class);
	
	public static final byte OPEN_BY_CLICK = 1;
	public static final byte OPEN_BY_TIME = 2;
	public static final byte OPEN_BY_ITEM = 4;
	public static final byte OPEN_BY_SKILL = 8;
	public static final byte OPEN_BY_CYCLE = 16;
	
	private int _castleIndex = -2;
	private int _mapRegion = -1;
	private int _fortIndex = -2;
	
	protected final int _doorId;
	protected final String _name;
	private boolean _open;
	private ClanHall _clanHall;
	
	private boolean _isAttackableDoor = false;
	private boolean _isTargetable;
	
	private int _meshindex = 1;
	
	private Future<?> _autoCloseTask;
	
	public final L2Territory pos;
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2DoorAI(this);
	}
	
	@Override
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	public L2DoorInstance(int objectId, L2DoorTemplate template, int doorId, String name)
	{
		super(objectId, template);
		
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		
		_doorId = doorId;
		_name = name;
		
		pos = new L2Territory();
		
		_open = template.isOpenByDefault();
		
		_isAttackableDoor = template.isAttackable();
		
		_isTargetable = template.isTargetable();
		
		if (getGroupName() != null)
		{
			DoorTable.addDoorGroup(getGroupName(), getDoorId());
		}
		
		if (isOpenableByTime())
		{
			startTimerOpen();
		}
		
		int clanhallId = template.getClanHallId();
		if (clanhallId > 0)
		{
			ClanHall hall = ClanHallManager.getAllClanHalls().get(clanhallId);
			if (hall != null)
			{
				setClanHall(hall);
				hall.getDoors().add(this);
			}
		}
	}
	
	private void startTimerOpen()
	{
		int delay = _open ? getTemplate().getOpenTime() : getTemplate().getCloseTime();
		if (getTemplate().getRandomTime() > 0)
		{
			delay += Rnd.get(getTemplate().getRandomTime());
		}
		ThreadPoolManager.getInstance().scheduleGeneral(new TimerOpen(), delay * 1000);
	}
	
	@Override
	public final DoorKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof DoorKnownList))
		{
			setKnownList(new DoorKnownList(this));
		}
		
		return (DoorKnownList) super.getKnownList();
	}
	
	@Override
	public final DoorStat getStat()
	{
		if (super.getStat() == null || !(super.getStat() instanceof DoorStat))
		{
			setStat(new DoorStat(this));
		}
		
		return (DoorStat) super.getStat();
	}
	
	@Override
	public final DoorStatus getStatus()
	{
		if (super.getStatus() == null || !(super.getStatus() instanceof DoorStatus))
		{
			setStatus(new DoorStatus(this));
		}
		
		return (DoorStatus) super.getStatus();
	}
	
	@Override
	public L2DoorTemplate getTemplate()
	{
		return (L2DoorTemplate) super.getTemplate();
	}
	
	public void setMeshIndex(int mesh)
	{
		_meshindex = mesh;
	}
	
	public boolean getIsAttackableDoor()
	{
		return _isAttackableDoor;
	}
	
	public boolean getIsShowHp()
	{
		return getTemplate().isShowHp();
	}
	
	public void setIsAttackableDoor(boolean val)
	{
		_isAttackableDoor = val;
	}
	
	public int getMeshIndex()
	{
		return _meshindex;
	}
	
	public int getEmitter()
	{
		return getTemplate().getEmmiter();
	}
	
	public boolean isWall()
	{
		return getTemplate().isWall();
	}
	
	public String getGroupName()
	{
		return getTemplate().getGroupName();
	}
	
	public int getChildId()
	{
		return getTemplate().getChildDoorId();
	}
	
	public void setTargetable(boolean b)
	{
		_isTargetable = b;
		broadcastStatusUpdate();
	}
	
	public boolean isTargetable()
	{
		return _isTargetable;
	}
	
	public boolean checkCollision()
	{
		return getTemplate().isCheckCollision();
	}
	
	public final boolean isUnlockable()
	{
		return (getTemplate().getOpenType() & OPEN_BY_SKILL) == OPEN_BY_SKILL;
	}
	
	@Override
	public final int getLevel()
	{
		return 1;
	}
	
	@Override
	public final String getLevels()
	{
		return "" + 1;
	}
	
	public int getDoorId()
	{
		return _doorId;
	}
	
	private void notifyChildEvent(boolean open)
	{
		byte openThis = open ? getTemplate().getMasterDoorOpen() : getTemplate().getMasterDoorClose();
		
		if (openThis == 0)
		{
			return;
		}
		else if (openThis == 1)
		{
			openMe();
		}
		else if (openThis == -1)
		{
			closeMe();
		}
	}
	
	private L2DoorInstance getSiblingDoor(int doorId)
	{
		if (getInstanceId() == 0)
		{
			return DoorTable.getInstance().getDoor(doorId);
		}
		
		return null;
	}
	
	public boolean getOpen()
	{
		return _open;
	}
	
	public void setOpen(boolean open)
	{
		_open = open;
		if (getChildId() > 0)
		{
			L2DoorInstance sibling = getSiblingDoor(getChildId());
			if (sibling != null)
			{
				sibling.notifyChildEvent(open);
			}
			else
			{
				LOG.warn("Cannot find child id: {}", getChildId());
			}
		}
	}
	
	public int getDamage()
	{
		final int dmg = 6 - (int) Math.ceil(getCurrentHp() / getMaxHp() * 6);
		if (dmg > 6)
		{
			return 6;
		}
		
		if (dmg < 0)
		{
			return 0;
		}
		return dmg;
	}
	
	public final Castle getCastle()
	{
		if (_castleIndex < 0)
		{
			_castleIndex = CastleManager.getInstance().getCastleIndex(this);
		}
		
		if (_castleIndex < 0)
		{
			return null;
		}
		
		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}
	
	public final Fort getFort()
	{
		if (_fortIndex < 0)
		{
			_fortIndex = FortManager.getInstance().getFortIndex(this);
		}
		
		if (_fortIndex < 0)
		{
			return null;
		}
		
		return FortManager.getInstance().getForts().get(_fortIndex);
	}
	
	public void setClanHall(ClanHall clanhall)
	{
		_clanHall = clanhall;
	}
	
	public ClanHall getClanHall()
	{
		return _clanHall;
	}
	
	public boolean isEnemyOf(final L2Character cha)
	{
		return true;
	}
	
	public boolean isEnemy()
	{
		if ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getZone().isActive() && getIsShowHp())
		{
			return true;
		}
		// if ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getZone().isActive() && getIsShowHp())
		// {
		// return true;
		// }
		if ((getClanHall() != null) && getClanHall().isSiegableHall() && ((SiegableHall) getClanHall()).getSiegeZone().isActive() && getIsShowHp())
		{
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (isUnlockable())
		{
			return true;
		}
		
		if (attacker == null || !(attacker instanceof L2PlayableInstance))
		{
			return false;
		}
		
		if (getIsAttackableDoor())
		{
			return true;
		}
		
		L2PcInstance player = null;
		if (attacker instanceof L2PcInstance)
		{
			player = (L2PcInstance) attacker;
		}
		else if (attacker instanceof L2SummonInstance)
		{
			player = ((L2SummonInstance) attacker).getOwner();
		}
		else if (attacker instanceof L2PetInstance)
		{
			player = ((L2PetInstance) attacker).getOwner();
		}
		
		if (player == null)
		{
			return false;
		}
		
		final L2Clan clan = player.getClan();
		final boolean isCastle = getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress();
		final boolean isFort = getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress();
		boolean isHall = getClanHall() != null && getClanHall().isSiegableHall() && ((SiegableHall) getClanHall()).isInSiege();
		
		if (isFort)
		{
			if (clan != null && clan == getFort().getOwnerClan())
			{
				return false;
			}
		}
		else if (isCastle)
		{
			if (clan != null && clan.getClanId() == getCastle().getOwnerId())
			{
				return false;
			}
		}
		else if (isFort)
		{
			if (clan != null && clan.getClanId() == getClanHall().getOwnerId())
			{
				return false;
			}
		}
		
		return isCastle || isFort || isHall;
	}
	
	public boolean isAttackable(L2Character attacker)
	{
		return isAutoAttackable(attacker);
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (this != player.getTarget())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			player.sendPacket(new DoorInfo(this));
			player.sendPacket(new DoorStatusUpdate(this));
		}
		else
		{
			if (isAutoAttackable(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else if (player.getClan() != null && getCastle() != null && player.getClanId() == getCastle().getOwnerId())
			{
				if (!isInsideRadius(player, 200, false, false))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Like L2OFF Clanhall's doors get request to be closed/opened
					player.gatesRequest(this);
					
					if (!getOpen())
					{
						player.sendPacket(new ConfirmDlg(1140));
					}
					else
					{
						player.sendPacket(new ConfirmDlg(1141));
					}
				}
			}
			else if (player.getClan() != null && getClanHall() != null && player.getClanId() == getClanHall().getOwnerId())
			{
				
				if (!isInsideRadius(player, 200, false, false))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Like L2OFF Clanhall's doors get request to be closed/opened
					player.gatesRequest(this);
					
					if (!getOpen())
					{
						player.sendPacket(new ConfirmDlg(1140));
					}
					else
					{
						player.sendPacket(new ConfirmDlg(1141));
					}
				}
			}
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (player.getAccessLevel().isGm())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));
			
			if (isAutoAttackable(player))
			{
				player.sendPacket(new DoorInfo(this));
				player.sendPacket(new DoorStatusUpdate(this));
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/admin/doorinfo.htm");
			html.replace("%class%", getClass().getSimpleName());
			html.replace("%hp%", String.valueOf((int) getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(getMaxHp()));
			html.replace("%objid%", String.valueOf(getObjectId()));
			html.replace("%doorid%", String.valueOf(_doorId));
			
			html.replace("%minx%", String.valueOf(getX(0)));
			html.replace("%miny%", String.valueOf(getY(0)));
			html.replace("%minz%", String.valueOf(getZMin()));
			
			html.replace("%maxx%", String.valueOf(getX(2)));
			html.replace("%maxy%", String.valueOf(getY(2)));
			html.replace("%maxz%", String.valueOf(getZMax()));
			html.replace("%unlock%", isUnlockable() ? "<font color=00FF00>Yes</font>" : "<font color=FF0000>No</font>");
			player.sendPacket(html);
		}
		else
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));
			
			if (isAutoAttackable(player))
			{
				player.sendPacket(new DoorInfo(this));
				player.sendPacket(new DoorStatusUpdate(this));
			}
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void broadcastStatusUpdate()
	{
		final Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		
		if (knownPlayers == null || knownPlayers.isEmpty())
		{
			return;
		}
		
		for (final L2PcInstance player : knownPlayers)
		{
			sendInfo(player);
		}
	}
	
	public final void openMe()
	{
		if (getGroupName() != null)
		{
			manageGroupOpen(true, getGroupName());
			return;
		}
		
		setOpen(true);
		
		broadcastStatusUpdate();
		startAutoCloseTask();
	}
	
	public final void closeMe()
	{
		Future<?> oldTask = _autoCloseTask;
		if (oldTask != null)
		{
			_autoCloseTask = null;
			oldTask.cancel(false);
		}
		
		if (getGroupName() != null)
		{
			manageGroupOpen(false, getGroupName());
			return;
		}
		
		setOpen(false);
		broadcastStatusUpdate();
	}
	
	private void manageGroupOpen(boolean open, String groupName)
	{
		Set<Integer> set = DoorTable.getDoorsByGroup(groupName);
		L2DoorInstance first = null;
		for (Integer id : set)
		{
			L2DoorInstance door = getSiblingDoor(id);
			if (first == null)
			{
				first = door;
			}
			
			if (door.getOpen() != open)
			{
				door.setOpen(open);
				door.broadcastStatusUpdate();
			}
		}
		
		if ((first != null) && open)
		{
			first.startAutoCloseTask(); // only one from group
		}
	}
	
	public final boolean isOpenableByItem()
	{
		return (getTemplate().getOpenType() & OPEN_BY_ITEM) == OPEN_BY_ITEM;
	}
	
	/**
	 * @return {@code true} if door is open-able by double-click.
	 */
	public final boolean isOpenableByClick()
	{
		return (getTemplate().getOpenType() & OPEN_BY_CLICK) == OPEN_BY_CLICK;
	}
	
	/**
	 * @return {@code true} if door is open-able by time.
	 */
	public final boolean isOpenableByTime()
	{
		return (getTemplate().getOpenType() & OPEN_BY_TIME) == OPEN_BY_TIME;
	}
	
	/**
	 * @return {@code true} if door is open-able by Field Cycle system.
	 */
	public final boolean isOpenableByCycle()
	{
		return (getTemplate().getOpenType() & OPEN_BY_CYCLE) == OPEN_BY_CYCLE;
	}
	
	@Override
	public String toString()
	{
		return "door " + _doorId;
	}
	
	public String getDoorName()
	{
		return _name;
	}
	
	public int getMapRegion()
	{
		return _mapRegion;
	}
	
	public void setMapRegion(final int region)
	{
		_mapRegion = region;
	}
	
	public int getX(int i)
	{
		return getTemplate().getNodeX()[i];
	}
	
	public int getY(int i)
	{
		return getTemplate().getNodeY()[i];
	}
	
	public int getZMin()
	{
		return getTemplate().getNodeZ();
	}
	
	public int getZMax()
	{
		return getTemplate().getNodeZ() + getTemplate().getHeight();
	}
	
	public Collection<L2SiegeGuardInstance> getKnownSiegeGuards()
	{
		final FastList<L2SiegeGuardInstance> result = new FastList<>();
		
		for (final L2Object obj : getKnownList().getKnownObjects().values())
		{
			if (obj instanceof L2SiegeGuardInstance)
			{
				result.add((L2SiegeGuardInstance) obj);
			}
		}
		
		return result;
	}
	
	public Collection<L2FortSiegeGuardInstance> getKnownFortSiegeGuards()
	{
		final FastList<L2FortSiegeGuardInstance> result = new FastList<>();
		
		final Collection<L2Object> objs = getKnownList().getKnownObjects().values();
		{
			for (final L2Object obj : objs)
			{
				if (obj instanceof L2FortSiegeGuardInstance)
				{
					result.add((L2FortSiegeGuardInstance) obj);
				}
			}
		}
		return result;
	}
	
	@Override
	public void reduceCurrentHp(final double damage, final L2Character attacker, final boolean awake)
	{
		if (isAutoAttackable(attacker) || (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isGM()))
		{
			super.reduceCurrentHp(damage, attacker, awake);
		}
		else
		{
			super.reduceCurrentHp(0, attacker, awake);
		}
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		final boolean isFort = (getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress());
		final boolean isCastle = (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress());
		boolean isHall = ((getClanHall() != null) && getClanHall().isSiegableHall() && ((SiegableHall) getClanHall()).isInSiege());
		
		if (isFort || isCastle || isHall)
		{
			broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTLE_GATE_BROKEN_DOWN));
		}
		
		return true;
	}
	
	private void startAutoCloseTask()
	{
		if ((getTemplate().getCloseTime() < 0) || isOpenableByTime())
		{
			return;
		}
		Future<?> oldTask = _autoCloseTask;
		if (oldTask != null)
		{
			_autoCloseTask = null;
			oldTask.cancel(false);
		}
		_autoCloseTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoClose(), getTemplate().getCloseTime() * 1000);
	}
	
	class AutoClose implements Runnable
	{
		@Override
		public void run()
		{
			if (getOpen())
			{
				closeMe();
			}
		}
	}
	
	class TimerOpen implements Runnable
	{
		@Override
		public void run()
		{
			boolean open = getOpen();
			if (open)
			{
				closeMe();
			}
			else
			{
				openMe();
			}
			
			int delay = open ? getTemplate().getCloseTime() : getTemplate().getOpenTime();
			if (getTemplate().getRandomTime() > 0)
			{
				delay += Rnd.get(getTemplate().getRandomTime());
			}
			ThreadPoolManager.getInstance().scheduleGeneral(this, delay * 1000);
		}
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new DoorInfo(this));
		activeChar.sendPacket(new DoorStatusUpdate(this));
	}
	
	@Override
	public boolean isDoor()
	{
		return true;
	}
}
