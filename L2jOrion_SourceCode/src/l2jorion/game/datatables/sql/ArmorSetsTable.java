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
package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.util.FastMap;
import l2jorion.game.model.L2ArmorSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class ArmorSetsTable
{
	private final static Logger LOG = LoggerFactory.getLogger(ArmorSetsTable.class);
	private static ArmorSetsTable _instance;
	
	public FastMap<Integer, L2ArmorSet> armorSets;
	private final FastMap<Integer, ArmorDummy> cusArmorSets;
	
	public static ArmorSetsTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new ArmorSetsTable();
		}
		
		return _instance;
	}
	
	private ArmorSetsTable()
	{
		armorSets = new FastMap<>();
		cusArmorSets = new FastMap<>();
		loadData();
	}
	
	private void loadData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT id, chest, legs, head, gloves, feet, skill_id, shield, shield_skill_id, enchant6skill FROM armorsets");
			final ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int id = rset.getInt("id");
				final int chest = rset.getInt("chest");
				final int legs = rset.getInt("legs");
				final int head = rset.getInt("head");
				final int gloves = rset.getInt("gloves");
				final int feet = rset.getInt("feet");
				final int skill_id = rset.getInt("skill_id");
				final int shield = rset.getInt("shield");
				final int shield_skill_id = rset.getInt("shield_skill_id");
				final int enchant6skill = rset.getInt("enchant6skill");
				
				armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skill_id, shield, shield_skill_id, enchant6skill));
				cusArmorSets.put(id, new ArmorDummy(chest, legs, head, gloves, feet, skill_id, shield));
			}
			
			LOG.info("ArmorSetsTable: Loaded: " + armorSets.size() + " armor sets");
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Error while loading armor sets data", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public boolean setExists(final int chestId)
	{
		return armorSets.containsKey(chestId);
	}
	
	public L2ArmorSet getSet(final int chestId)
	{
		return armorSets.get(chestId);
	}
	
	public void addObj(final int v, final L2ArmorSet s)
	{
		armorSets.put(v, s);
	}
	
	public ArmorDummy getCusArmorSets(final int id)
	{
		return cusArmorSets.get(id);
	}
	
	public class ArmorDummy
	{
		private final int _chest;
		private final int _legs;
		private final int _head;
		private final int _gloves;
		private final int _feet;
		private final int _skill_id;
		private final int _shield;
		
		public ArmorDummy(final int chest, final int legs, final int head, final int gloves, final int feet, final int skill_id, final int shield)
		{
			_chest = chest;
			_legs = legs;
			_head = head;
			_gloves = gloves;
			_feet = feet;
			_skill_id = skill_id;
			_shield = shield;
		}
		
		public int getChest()
		{
			return _chest;
		}
		
		public int getLegs()
		{
			return _legs;
		}
		
		public int getHead()
		{
			return _head;
		}
		
		public int getGloves()
		{
			return _gloves;
		}
		
		public int getFeet()
		{
			return _feet;
		}
		
		public int getSkill_id()
		{
			return _skill_id;
		}
		
		public int getShield()
		{
			return _shield;
		}
	}
}
