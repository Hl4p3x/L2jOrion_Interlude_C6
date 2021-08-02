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

import l2jorion.game.model.L2ArmorSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public final class CustomArmorSetsTable
{
	private static final Logger LOG = LoggerFactory.getLogger(CustomArmorSetsTable.class);
	
	private static CustomArmorSetsTable _instance;
	
	public static CustomArmorSetsTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new CustomArmorSetsTable();
		}
		return _instance;
	}
	
	public CustomArmorSetsTable()
	{
		Connection con = null;
		int count = 0;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill_id, shield, shield_skill_id, enchant6skill FROM custom_armorsets");
			final ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int chest = rset.getInt("chest");
				final int legs = rset.getInt("legs");
				final int head = rset.getInt("head");
				final int gloves = rset.getInt("gloves");
				final int feet = rset.getInt("feet");
				final int skill_id = rset.getInt("skill_id");
				final int shield = rset.getInt("shield");
				final int shield_skill_id = rset.getInt("shield_skill_id");
				final int enchant6skill = rset.getInt("enchant6skill");
				ArmorSetsTable.getInstance().addObj(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skill_id, shield, shield_skill_id, enchant6skill));
				count++;
			}
			if (count > 0)
			{
				LOG.info("ArmorSetsTable: Loaded custom armor sets");
			}
			
			DatabaseUtils.close(statement);
			DatabaseUtils.close(rset);
		}
		catch (final Exception e)
		{
			LOG.error("ArmorSetsTable: Error reading Custom ArmorSets table", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
}
