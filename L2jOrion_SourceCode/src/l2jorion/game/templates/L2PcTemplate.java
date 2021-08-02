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
package l2jorion.game.templates;

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.base.Race;

public class L2PcTemplate extends L2CharTemplate
{
	public final Race race;
	public final ClassId classId;
	
	public final int _currentCollisionRadius;
	public final int _currentCollisionHeight;
	public final String className;
	
	public final int spawnX;
	public final int spawnY;
	public final int spawnZ;
	
	public final int classBaseLevel;
	public final float lvlHpAdd;
	public final float lvlHpMod;
	public final float lvlCpAdd;
	public final float lvlCpMod;
	public final float lvlMpAdd;
	public final float lvlMpMod;
	
	private final List<L2Item> _items = new FastList<>();
	
	public L2PcTemplate(final StatsSet set)
	{
		super(set);
		
		classId = ClassId.values()[set.getInteger("classId")];
		race = Race.values()[set.getInteger("raceId")];
		className = set.getString("className");
		
		_currentCollisionRadius = set.getInteger("collision_radius");
		_currentCollisionHeight = set.getInteger("collision_height");
		
		spawnX = set.getInteger("spawnX");
		spawnY = set.getInteger("spawnY");
		spawnZ = set.getInteger("spawnZ");
		
		classBaseLevel = set.getInteger("classBaseLevel");
		lvlHpAdd = set.getFloat("lvlHpAdd");
		lvlHpMod = set.getFloat("lvlHpMod");
		lvlCpAdd = set.getFloat("lvlCpAdd");
		lvlCpMod = set.getFloat("lvlCpMod");
		lvlMpAdd = set.getFloat("lvlMpAdd");
		lvlMpMod = set.getFloat("lvlMpMod");
	}
	
	public void addItem(final int itemId)
	{
		final L2Item item = ItemTable.getInstance().getTemplate(itemId);
		if (item != null)
		{
			_items.add(item);
		}
	}
	
	public L2Item[] getItems()
	{
		return _items.toArray(new L2Item[_items.size()]);
	}
	
	@Override
	public int getCollisionRadius()
	{
		return _currentCollisionRadius;
	}
	
	@Override
	public int getCollisionHeight()
	{
		return _currentCollisionHeight;
	}
	
	public int getBaseFallSafeHeight(final boolean female)
	{
		if (classId.getRace() == Race.darkelf || classId.getRace() == Race.elf)
		{
			return classId.isMage() ? (female ? 330 : 300) : female ? 380 : 350;
		}
		else if (classId.getRace() == Race.dwarf)
		{
			return female ? 200 : 180;
		}
		else if (classId.getRace() == Race.human)
		{
			return classId.isMage() ? (female ? 220 : 200) : female ? 270 : 250;
		}
		else if (classId.getRace() == Race.orc)
		{
			return classId.isMage() ? (female ? 280 : 250) : female ? 220 : 200;
		}
		
		return 400;
		
		/*
		 * Dark Elf Fighter F 380 Dark Elf Fighter M 350 Dark Elf Mystic F 330 Dark Elf Mystic M 300 Dwarf Fighter F 200 Dwarf Fighter M 180 Elf Fighter F 380 Elf Fighter M 350 Elf Mystic F 330 Elf Mystic M 300 Human Fighter F 270 Human Fighter M 250 Human Mystic F 220 Human Mystic M 200 Orc Fighter
		 * F 220 Orc Fighter M 200 Orc Mystic F 280 Orc Mystic M 250
		 */
	}
	
	public final int getFallHeight()
	{
		return 333;
	}
}
