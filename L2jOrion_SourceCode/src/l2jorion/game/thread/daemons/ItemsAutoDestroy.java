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
package l2jorion.game.thread.daemons;

import java.util.List;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.managers.ItemsOnGroundManager;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ItemsAutoDestroy
{
	protected static final Logger LOG = LoggerFactory.getLogger("ItemsAutoDestroy");
	
	private static ItemsAutoDestroy _instance;
	protected List<L2ItemInstance> _items = null;
	protected static long _sleep;
	
	private ItemsAutoDestroy()
	{
		_items = new FastList<>();
		_sleep = Config.AUTODESTROY_ITEM_AFTER * 1000;
		
		if (_sleep == 0)
		{
			_sleep = 3600000;
		}
		
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckItemsForDestroy(), 5000, 5000);
	}
	
	public static ItemsAutoDestroy getInstance()
	{
		if (_instance == null)
		{
			_instance = new ItemsAutoDestroy();
		}
		return _instance;
	}
	
	public synchronized void addItem(final L2ItemInstance item)
	{
		item.setDropTime(System.currentTimeMillis());
		_items.add(item);
	}
	
	public synchronized void removeItems()
	{
		if (_items.isEmpty())
		{
			return;
		}
		
		final long curtime = System.currentTimeMillis();
		
		for (final L2ItemInstance item : _items)
		{
			if (item == null || item.getDropTime() == 0 || item.getLocation() != L2ItemInstance.ItemLocation.VOID)
			{
				_items.remove(item);
			}
			else
			{
				if (item.getItemType() == L2EtcItemType.HERB)
				{
					if (curtime - item.getDropTime() > Config.HERB_AUTO_DESTROY_TIME)
					{
						L2World.getInstance().removeVisibleObject(item, item.getWorldRegion());
						L2World.getInstance().removeObject(item);
						_items.remove(item);
						
						if (Config.SAVE_DROPPED_ITEM)
						{
							ItemsOnGroundManager.getInstance().removeObject(item);
						}
					}
				}
				else if (curtime - item.getDropTime() > _sleep)
				{
					L2World.getInstance().removeVisibleObject(item, item.getWorldRegion());
					L2World.getInstance().removeObject(item);
					_items.remove(item);
					
					if (Config.SAVE_DROPPED_ITEM)
					{
						ItemsOnGroundManager.getInstance().removeObject(item);
					}
				}
			}
		}
		
		if (Config.DEBUG)
		{
			LOG.info("[ItemsAutoDestroy] : " + _items.size() + " items remaining.");
		}
	}
	
	protected class CheckItemsForDestroy extends Thread
	{
		@Override
		public void run()
		{
			removeItems();
		}
	}
}
