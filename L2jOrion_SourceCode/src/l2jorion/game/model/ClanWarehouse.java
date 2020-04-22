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
package l2jorion.game.model;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance.ItemLocation;

public final class ClanWarehouse extends Warehouse
{
	// private static final Logger LOG = LoggerFactory.getLogger(PcWarehouse.class);
	
	private final L2Clan _clan;
	
	public ClanWarehouse(final L2Clan clan)
	{
		_clan = clan;
	}
	
	@Override
	public int getOwnerId()
	{
		//return _clan.getLeader().getObjectId();
		return _clan.getClanId();
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _clan.getLeader().getPlayerInstance();
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.CLANWH;
	}
	
	public String getLocationId()
	{
		return "0";
	}
	
	public int getLocationId(final boolean dummy)
	{
		return 0;
	}
	
	public void setLocationId(final L2PcInstance dummy)
	{
	}
	
	@Override
	public boolean validateCapacity(final int slots)
	{
		return _items.size() + slots <= Config.WAREHOUSE_SLOTS_CLAN;
	}
}
