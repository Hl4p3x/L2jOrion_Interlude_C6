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
package l2jorion.game.model;

import java.lang.reflect.Constructor;

import l2jorion.Config;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.managers.ItemsOnGroundManager;
import l2jorion.game.managers.MercTicketManager;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.knownlist.ObjectKnownList;
import l2jorion.game.model.actor.poly.ObjectPoly;
import l2jorion.game.model.actor.position.ObjectPosition;
import l2jorion.game.model.extender.BaseExtender;
import l2jorion.game.model.extender.BaseExtender.EventType;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.GetItem;

public abstract class L2Object
{
	private boolean _isVisible; // Object visibility
	private ObjectKnownList _knownList;
	private String _name;
	private int _objectId; // Object identifier
	private ObjectPoly _poly;
	private ObjectPosition _position;
	private int _instanceId = 0;
	private BaseExtender _extender = null;
	
	public L2Object(final int objectId)
	{
		_objectId = objectId;
		
		if (Config.EXTENDERS.get(this.getClass().getName()) != null)
		{
			for (final String className : Config.EXTENDERS.get(this.getClass().getName()))
			{
				try
				{
					final Class<?> clazz = Class.forName(className);
					if (clazz == null)
					{
						continue;
					}
					
					if (!BaseExtender.class.isAssignableFrom(clazz))
					{
						continue;
					}
					
					if (!(Boolean) clazz.getMethod("canCreateFor", L2Object.class).invoke(null, this))
					{
						continue;
					}
					
					final Constructor<?> construct = clazz.getConstructor(L2Object.class);
					if (construct != null)
					{
						addExtender((BaseExtender) construct.newInstance(this));
					}
				}
				catch (final Exception e)
				{
					continue;
				}
			}
		}
	}
	
	public void addExtender(final BaseExtender newExtender)
	{
		if (_extender == null)
		{
			_extender = newExtender;
		}
		else
		{
			_extender.addExtender(newExtender);
		}
	}
	
	public BaseExtender getExtender(final String simpleName)
	{
		if (_extender == null)
		{
			return null;
		}
		
		return _extender.getExtender(simpleName);
	}
	
	public Object fireEvent(final String event, final Object... params)
	{
		if (_extender == null)
		{
			return null;
		}
		
		return _extender.onEvent(event, params);
	}
	
	public void removeExtender(final BaseExtender ext)
	{
		if (_extender != null)
		{
			if (_extender == ext)
			{
				_extender = _extender.getNextExtender();
			}
			else
			{
				_extender.removeExtender(ext);
			}
		}
	}
	
	public void onAction(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onActionShift(final L2GameClient client)
	{
		onActionShift(client.getActiveChar());
	}
	
	public void onActionShift(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onForcedAttack(final L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onSpawn()
	{
		fireEvent(EventType.SPAWN.name, (Object[]) null);
	}
	
	public final void setXYZ(int x, int y, int z)
	{
		getPosition().setXYZ(x, y, z);
	}
	
	public final void setXYZInvisible(int x, int y, int z)
	{
		getPosition().setXYZInvisible(x, y, z);
	}
	
	public final int getX()
	{
		assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getX();
	}
	
	public final int getY()
	{
		assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getY();
	}
	
	public final int getZ()
	{
		assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getZ();
	}
	
	public final void decayMe()
	{
		if (Config.ASSERT)
		{
			assert getPosition().getWorldRegion() != null;
		}
		
		L2WorldRegion reg = getPosition().getWorldRegion();
		
		synchronized (this)
		{
			_isVisible = false;
			getPosition().setWorldRegion(null);
		}
		
		L2World.getInstance().removeVisibleObject(this, reg);
		L2World.getInstance().removeObject(this);
		
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().removeObject(this);
		}
	}
	
	public final void pickupMe(final L2Character player)
	{
		if (Config.ASSERT)
		{
			assert this instanceof L2ItemInstance;
		}
		
		if (Config.ASSERT)
		{
			assert getPosition().getWorldRegion() != null;
		}
		
		L2WorldRegion oldregion = getPosition().getWorldRegion();
		
		player.broadcastPacket(new GetItem((L2ItemInstance) this, player.getObjectId()));
		
		synchronized (this)
		{
			_isVisible = false;
			getPosition().setWorldRegion(null);
		}
		
		// if this item is a mercenary ticket, remove the spawns!
		if (this instanceof L2ItemInstance)
		{
			final int itemId = ((L2ItemInstance) this).getItemId();
			if (MercTicketManager.getInstance().getTicketCastleId(itemId) > 0)
			{
				MercTicketManager.getInstance().removeTicket((L2ItemInstance) this);
				ItemsOnGroundManager.getInstance().removeObject(this);
			}
		}
		
		L2World.getInstance().removeVisibleObject(this, oldregion);
	}
	
	public void refreshID()
	{
		L2World.getInstance().removeObject(this);
		IdFactory.getInstance().releaseId(getObjectId());
		_objectId = IdFactory.getInstance().getNextId();
	}
	
	public final void spawnMe()
	{
		if (Config.ASSERT)
		{
			assert getPosition().getWorldRegion() == null && getPosition().getWorldPosition().getX() != 0 && getPosition().getWorldPosition().getY() != 0 && getPosition().getWorldPosition().getZ() != 0;
		}
		
		synchronized (this)
		{
			_isVisible = true;
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
			L2World.getInstance().storeObject(this);
			getPosition().getWorldRegion().addVisibleObject(this);
		}
		
		onSpawn();
		
		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());
	}
	
	public final void spawnMe(int x, int y, final int z)
	{
		if (Config.ASSERT)
		{
			assert getPosition().getWorldRegion() == null;
		}
		
		synchronized (this)
		{
			_isVisible = true;
			getPosition().setWorldPosition(x, y, z);
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
			L2World.getInstance().storeObject(this);
			getPosition().getWorldRegion().addVisibleObject(this);
		}
		
		onSpawn(); // moved up because of pvp zone
		
		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());
	}
	
	public boolean isAttackable()
	{
		return false;
	}
	
	public abstract boolean isAutoAttackable(L2Character attacker);
	
	public boolean isMarker()
	{
		return false;
	}
	
	public final boolean isVisible()
	{
		return getPosition().getWorldRegion() != null;
	}
	
	public final void setIsVisible(final boolean value)
	{
		_isVisible = value;
		
		if (!_isVisible)
		{
			getPosition().setWorldRegion(null);
		}
	}
	
	public ObjectKnownList getKnownList()
	{
		if (_knownList == null)
		{
			_knownList = new ObjectKnownList(this);
		}
		
		return _knownList;
	}
	
	public final void setKnownList(ObjectKnownList value)
	{
		_knownList = value;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final void setName(final String value)
	{
		_name = value;
	}
	
	public final int getObjectId()
	{
		return _objectId;
	}
	
	public final ObjectPoly getPoly()
	{
		if (_poly == null)
		{
			_poly = new ObjectPoly(this);
		}
		
		return _poly;
	}
	
	public void removeStatusListener(L2Character object)
	{
	}
	
	public L2WorldRegion getWorldRegion()
	{
		return getPosition().getWorldRegion();
	}
	
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	public void setInstanceId(final int instanceId)
	{
		_instanceId = instanceId;
		
		// If we change it for visible objects, me must clear & revalidates knownlists
		if (_isVisible && _knownList != null)
		{
			if (!(this instanceof L2PcInstance))
			{
				// We don't want some ugly looking disappear/appear effects, so don't update
				// the knownlist here, but players usually enter instancezones through teleporting
				// and the teleport will do the revalidation for us.
				decayMe();
				spawnMe();
			}
		}
	}
	
	public final ObjectPosition getPosition()
	{
		if (_position == null)
		{
			_position = new ObjectPosition(this);
		}
		
		return _position;
	}
	
	public L2PcInstance getActingPlayer()
	{
		return null;
	}
	
	@Override
	public String toString()
	{
		return "" + getObjectId();
	}
	
	public boolean isCharacter()
	{
		return false;
	}
	
	public boolean isPlayable()
	{
		return false;
	}
	
	public boolean isPet()
	{
		return false;
	}
	
	public boolean isSummon()
	{
		return false;
	}
	
	public boolean isNpc()
	{
		return false;
	}
	
	public boolean isMonster()
	{
		return false;
	}
	
	public boolean isPlayer()
	{
		return false;
	}
	
	public boolean isItem()
	{
		return false;
	}
	
	public boolean isGuard()
	{
		return false;
	}
	
	public boolean isBoss()
	{
		return false;
	}
	
	public boolean isTrap()
	{
		return false;
	}
	
	public boolean isDoor()
	{
		return false;
	}
	
	public boolean isArtefact()
	{
		return false;
	}
	
	public boolean isSiegeGuard()
	{
		return false;
	}
	
	public boolean isMinion()
	{
		return false;
	}
	
	public final double calculateDistance(int x, int y, int z, boolean includeZAxis, boolean squared)
	{
		final double distance = Math.pow(x - getX(), 2) + Math.pow(y - getY(), 2) + (includeZAxis ? Math.pow(z - getZ(), 2) : 0);
		return (squared) ? distance : Math.sqrt(distance);
	}
	
	public boolean isDead()
	{
		return false;
	}
	
	public boolean isBot()
	{
		return false;
	}
	
	public boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();
		
		if (strictCheck)
		{
			if (checkZ)
			{
				return ((dx * dx) + (dy * dy) + (dz * dz)) < (radius * radius);
			}
			
			return ((dx * dx) + (dy * dy)) < (radius * radius);
		}
		
		if (checkZ)
		{
			return ((dx * dx) + (dy * dy) + (dz * dz)) <= (radius * radius);
		}
		
		return ((dx * dx) + (dy * dy)) <= (radius * radius);
	}
	
	public void sendInfo(L2PcInstance activeChar)
	{
	}
}