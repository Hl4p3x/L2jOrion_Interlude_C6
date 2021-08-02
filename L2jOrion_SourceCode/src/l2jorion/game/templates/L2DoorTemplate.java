/*
 * Copyright (C) 2004-2016 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.templates;

import l2jorion.game.model.actor.instance.L2DoorInstance;

public class L2DoorTemplate extends L2CharTemplate
{
	private final int _doorId;
	private final int _nodeX[];
	private final int _nodeY[];
	private final int _nodeZ;
	private final int _height;
	private final int _posX;
	private final int _posY;
	private final int _posZ;
	private final int _emmiter;
	private final int _childDoorId;
	private final String _name;
	private final String _groupName;
	private final boolean _showHp;
	private final boolean _isWall;
	// -1 close, 0 nothing, 1 open
	private final byte _masterDoorClose;
	private final byte _masterDoorOpen;
	
	private final boolean _isTargetable;
	private final boolean _default_status;
	
	private int _openTime;
	private int _randomTime;
	private final int _closeTime;
	private final int _level;
	private final int _openType;
	private final boolean _checkCollision;
	private final boolean _isAttackableDoor;
	private final int _clanhallId;
	private final boolean _stealth;
	
	public L2DoorTemplate(StatsSet set)
	{
		super(set);
		_doorId = set.getInteger("id");
		_name = set.getString("name");
		
		// position
		String[] pos = set.getString("pos").split(";");
		_posX = Integer.parseInt(pos[0]);
		_posY = Integer.parseInt(pos[1]);
		_posZ = Integer.parseInt(pos[2]);
		_height = set.getInteger("height");
		_nodeZ = set.getInteger("nodeZ");
		_nodeX = new int[4]; // 4 * x
		_nodeY = new int[4]; // 4 * y
		for (int i = 0; i < 4; i++)
		{
			String split[] = set.getString("node" + (i + 1)).split(",");
			_nodeX[i] = Integer.parseInt(split[0]);
			_nodeY[i] = Integer.parseInt(split[1]);
		}
		
		// optional
		_emmiter = set.getInteger("emitter_id", 0);
		_showHp = set.getBool("hp_showable", true);
		_isWall = set.getBool("is_wall", false);
		_groupName = set.getString("group", null);
		
		_childDoorId = set.getInteger("child_id_event", -1);
		
		// true if door is opening
		String masterevent = set.getString("master_close_event", "act_nothing");
		_masterDoorClose = (byte) (masterevent.equals("act_open") ? 1 : masterevent.equals("act_close") ? -1 : 0);
		
		masterevent = set.getString("master_open_event", "act_nothing");
		_masterDoorOpen = (byte) (masterevent.equals("act_open") ? 1 : masterevent.equals("act_close") ? -1 : 0);
		
		_isTargetable = set.getBool("targetable", true);
		_default_status = set.getString("default_status", "close").equals("open");
		_closeTime = set.getInteger("close_time", -1);
		_level = set.getInteger("level", 0);
		_openType = set.getInteger("open_method", 0);
		_checkCollision = set.getBool("check_collision", true);
		
		if ((_openType & L2DoorInstance.OPEN_BY_TIME) == L2DoorInstance.OPEN_BY_TIME)
		{
			_openTime = set.getInteger("open_time");
			_randomTime = set.getInteger("random_time", -1);
		}
		
		_isAttackableDoor = set.getBool("is_attackable", false);
		_clanhallId = set.getInteger("clanhall_id", 0);
		_stealth = set.getBool("stealth", false);
	}
	
	/**
	 * Gets the door ID.
	 * @return the door ID
	 */
	public int getId()
	{
		return _doorId;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int[] getNodeX()
	{
		return _nodeX;
	}
	
	public int[] getNodeY()
	{
		return _nodeY;
	}
	
	public int getNodeZ()
	{
		return _nodeZ;
	}
	
	public int getHeight()
	{
		return _height;
	}
	
	public int getX()
	{
		return _posX;
	}
	
	public int getY()
	{
		return _posY;
	}
	
	public int getZ()
	{
		return _posZ;
	}
	
	public int getEmmiter()
	{
		return _emmiter;
	}
	
	public int getChildDoorId()
	{
		return _childDoorId;
	}
	
	public String getGroupName()
	{
		return _groupName;
	}
	
	public boolean isShowHp()
	{
		return _showHp;
	}
	
	public boolean isWall()
	{
		return _isWall;
	}
	
	public byte getMasterDoorOpen()
	{
		return _masterDoorOpen;
	}
	
	public byte getMasterDoorClose()
	{
		return _masterDoorClose;
	}
	
	public boolean isTargetable()
	{
		return _isTargetable;
	}
	
	public boolean isOpenByDefault()
	{
		return _default_status;
	}
	
	public int getOpenTime()
	{
		return _openTime;
	}
	
	public int getRandomTime()
	{
		return _randomTime;
	}
	
	public int getCloseTime()
	{
		return _closeTime;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getOpenType()
	{
		return _openType;
	}
	
	public boolean isCheckCollision()
	{
		return _checkCollision;
	}
	
	public boolean isAttackable()
	{
		return _isAttackableDoor;
	}
	
	public int getClanHallId()
	{
		return _clanhallId;
	}
	
	public boolean isStealth()
	{
		return _stealth;
	}
}
