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
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.L2LvlupData;
import l2jorion.game.model.base.ClassId;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class LevelUpData
{
	private static final String SELECT_ALL = "SELECT classid, defaulthpbase, defaulthpadd, defaulthpmod, defaultcpbase, defaultcpadd, defaultcpmod, defaultmpbase, defaultmpadd, defaultmpmod, class_lvl FROM lvlupgain";
	private static final String CLASS_LVL = "class_lvl";
	private static final String MP_MOD = "defaultmpmod";
	private static final String MP_ADD = "defaultmpadd";
	private static final String MP_BASE = "defaultmpbase";
	private static final String HP_MOD = "defaulthpmod";
	private static final String HP_ADD = "defaulthpadd";
	private static final String HP_BASE = "defaulthpbase";
	private static final String CP_MOD = "defaultcpmod";
	private static final String CP_ADD = "defaultcpadd";
	private static final String CP_BASE = "defaultcpbase";
	private static final String CLASS_ID = "classid";
	
	private final static Logger LOG = LoggerFactory.getLogger(LevelUpData.class);
	
	private static LevelUpData _instance;
	
	private final Map<Integer, L2LvlupData> lvlTable;
	
	public static LevelUpData getInstance()
	{
		if (_instance == null)
		{
			_instance = new LevelUpData();
		}
		
		return _instance;
	}
	
	private LevelUpData()
	{
		lvlTable = new FastMap<>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement(SELECT_ALL);
			final ResultSet rset = statement.executeQuery();
			L2LvlupData lvlDat;
			
			while (rset.next())
			{
				lvlDat = new L2LvlupData();
				lvlDat.setClassid(rset.getInt(CLASS_ID));
				lvlDat.setClassLvl(rset.getInt(CLASS_LVL));
				lvlDat.setClassHpBase(rset.getFloat(HP_BASE));
				lvlDat.setClassHpAdd(rset.getFloat(HP_ADD));
				lvlDat.setClassHpModifier(rset.getFloat(HP_MOD));
				lvlDat.setClassCpBase(rset.getFloat(CP_BASE));
				lvlDat.setClassCpAdd(rset.getFloat(CP_ADD));
				lvlDat.setClassCpModifier(rset.getFloat(CP_MOD));
				lvlDat.setClassMpBase(rset.getFloat(MP_BASE));
				lvlDat.setClassMpAdd(rset.getFloat(MP_ADD));
				lvlDat.setClassMpModifier(rset.getFloat(MP_MOD));
				
				lvlTable.put(Integer.valueOf(lvlDat.getClassid()), lvlDat);
			}
			
			DatabaseUtils.close(statement);
			DatabaseUtils.close(rset);
			
			LOG.info("LevelUpData: Loaded " + lvlTable.size() + " character level up templates");
		}
		catch (final Exception e)
		{
			LOG.error("Error while creating Lvl up data table", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * @param classId
	 * @return
	 */
	public L2LvlupData getTemplate(final int classId)
	{
		return lvlTable.get(classId);
	}
	
	public L2LvlupData getTemplate(final ClassId classId)
	{
		return lvlTable.get(classId.getId());
	}
}
