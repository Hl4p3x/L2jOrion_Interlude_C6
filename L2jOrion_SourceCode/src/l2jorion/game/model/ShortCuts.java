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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.ShortCutInit;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class ShortCuts
{
	private static Logger LOG = LoggerFactory.getLogger(ShortCuts.class);
	
	private final L2PcInstance _owner;
	private final Map<Integer, L2ShortCut> _shortCuts = new TreeMap<>();
	
	public ShortCuts(final L2PcInstance owner)
	{
		_owner = owner;
	}
	
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.values().toArray(new L2ShortCut[_shortCuts.values().size()]);
	}
	
	public L2ShortCut getShortCut(final int slot, final int page)
	{
		L2ShortCut sc = _shortCuts.get(slot + page * 12);
		
		// verify shortcut
		if (sc != null && sc.getType() == L2ShortCut.TYPE_ITEM)
		{
			if (_owner.getInventory().getItemByObjectId(sc.getId()) == null)
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
		
		return sc;
	}
	
	public synchronized void registerShortCut(final L2ShortCut shortcut)
	{
		L2ShortCut oldShortCut = _shortCuts.put(shortcut.getSlot() + 12 * shortcut.getPage(), shortcut);
		
		registerShortCutInDb(shortcut, oldShortCut);
	}
	
	private void registerShortCutInDb(final L2ShortCut shortcut, final L2ShortCut oldShortCut)
	{
		if (oldShortCut != null)
		{
			deleteShortCutFromDb(oldShortCut);
		}
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("REPLACE INTO character_shortcuts (char_obj_id,slot,page,type,shortcut_id,level,class_index) values(?,?,?,?,?,?,?)");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.getSlot());
			statement.setInt(3, shortcut.getPage());
			statement.setInt(4, shortcut.getType());
			statement.setInt(5, shortcut.getId());
			statement.setInt(6, shortcut.getLevel());
			statement.setInt(7, _owner.getClassIndex());
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.warn("Could not store character shortcut: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public synchronized void deleteShortCut(final int slot, final int page)
	{
		L2ShortCut old = _shortCuts.remove(slot + page * 12);
		
		if (old == null || _owner == null)
		{
			return;
		}
		
		deleteShortCutFromDb(old);
		
		if (old.getType() == L2ShortCut.TYPE_ITEM)
		{
			L2ItemInstance item = _owner.getInventory().getItemByObjectId(old.getId());
			
			if (item != null && item.getItemType() == L2EtcItemType.SHOT)
			{
				_owner.removeAutoSoulShot(item.getItemId());
				_owner.sendPacket(new ExAutoSoulShot(item.getItemId(), 0));
			}
		}
		
		_owner.sendPacket(new ShortCutInit(_owner));
		
		for (final int shotId : _owner.getAutoSoulShot())
		{
			_owner.sendPacket(new ExAutoSoulShot(shotId, 1));
		}
	}
	
	public synchronized void deleteShortCutByObjectId(final int objectId)
	{
		L2ShortCut toRemove = null;
		
		for (final L2ShortCut shortcut : _shortCuts.values())
		{
			if (shortcut.getType() == L2ShortCut.TYPE_ITEM && shortcut.getId() == objectId)
			{
				toRemove = shortcut;
				break;
			}
		}
		
		if (toRemove != null)
		{
			deleteShortCut(toRemove.getSlot(), toRemove.getPage());
		}
	}
	
	private void deleteShortCutFromDb(final L2ShortCut shortcut)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND slot=? AND page=? AND class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.getSlot());
			statement.setInt(3, shortcut.getPage());
			statement.setInt(4, _owner.getClassIndex());
			statement.execute();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.warn("Could not delete character shortcut: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void restore()
	{
		_shortCuts.clear();
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_obj_id, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, _owner.getClassIndex());
			
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int slot = rset.getInt("slot");
				final int page = rset.getInt("page");
				final int type = rset.getInt("type");
				final int id = rset.getInt("shortcut_id");
				final int level = rset.getInt("level");
				
				L2ShortCut sc = new L2ShortCut(slot, page, type, id, level, 1);
				_shortCuts.put(slot + page * 12, sc);
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.warn("Could not restore character shortcuts: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		// verify shortcuts
		for (final L2ShortCut sc : getAllShortCuts())
		{
			if (sc.getType() == L2ShortCut.TYPE_ITEM)
			{
				L2ItemInstance item = _owner.getInventory().getItemByObjectId(sc.getId());
				if (item == null)
				{
					deleteShortCut(sc.getSlot(), sc.getPage());
				}
			}
		}
	}
}
