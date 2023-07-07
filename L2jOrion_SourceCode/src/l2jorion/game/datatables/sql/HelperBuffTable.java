package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javolution.util.FastList;
import l2jorion.game.datatables.csv.HennaTable;
import l2jorion.game.templates.L2HelperBuff;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class HelperBuffTable
{
	private final static Logger LOG = LoggerFactory.getLogger(HennaTable.class);
	
	private static HelperBuffTable _instance;
	
	public List<L2HelperBuff> helperBuff;
	
	private final boolean _initialized = true;
	
	private int _magicClassLowestLevel = 100;
	private int _physicClassLowestLevel = 100;
	
	private int _magicClassHighestLevel = 1;
	private int _physicClassHighestLevel = 1;
	
	public static HelperBuffTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new HelperBuffTable();
		}
		
		return _instance;
	}
	
	public static void reload()
	{
		_instance = null;
		getInstance();
	}
	
	private HelperBuffTable()
	{
		helperBuff = new FastList<>();
		restoreHelperBuffData();
	}
	
	private void restoreHelperBuffData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT * FROM helper_buff_list");
			final ResultSet helperbuffdata = statement.executeQuery();
			
			fillHelperBuffTable(helperbuffdata);
			helperbuffdata.close();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Table helper_buff_list not found: Update your database", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
	}
	
	private void fillHelperBuffTable(final ResultSet HelperBuffData) throws Exception
	{
		while (HelperBuffData.next())
		{
			final StatsSet helperBuffDat = new StatsSet();
			final int id = HelperBuffData.getInt("id");
			
			helperBuffDat.set("id", id);
			helperBuffDat.set("skillID", HelperBuffData.getInt("skill_id"));
			helperBuffDat.set("skillLevel", HelperBuffData.getInt("skill_level"));
			helperBuffDat.set("lowerLevel", HelperBuffData.getInt("lower_level"));
			helperBuffDat.set("upperLevel", HelperBuffData.getInt("upper_level"));
			helperBuffDat.set("isMagicClass", HelperBuffData.getString("is_magic_class"));
			
			if ("false".equals(HelperBuffData.getString("is_magic_class")))
			{
				if (HelperBuffData.getInt("lower_level") < _physicClassLowestLevel)
				{
					_physicClassLowestLevel = HelperBuffData.getInt("lower_level");
				}
				
				if (HelperBuffData.getInt("upper_level") > _physicClassHighestLevel)
				{
					_physicClassHighestLevel = HelperBuffData.getInt("upper_level");
				}
			}
			else
			{
				if (HelperBuffData.getInt("lower_level") < _magicClassLowestLevel)
				{
					_magicClassLowestLevel = HelperBuffData.getInt("lower_level");
				}
				
				if (HelperBuffData.getInt("upper_level") > _magicClassHighestLevel)
				{
					_magicClassHighestLevel = HelperBuffData.getInt("upper_level");
				}
			}
			
			// Add this Helper Buff to the Helper Buff List
			final L2HelperBuff template = new L2HelperBuff(helperBuffDat);
			helperBuff.add(template);
		}
		
		LOG.info("HelperBuffTable: Loaded " + helperBuff.size() + " helper buff templates");
		
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	public L2HelperBuff getHelperBuffTableItem(final int id)
	{
		return helperBuff.get(id);
	}
	
	public List<L2HelperBuff> getHelperBuffTable()
	{
		return helperBuff;
	}
	
	public int getMagicClassHighestLevel()
	{
		return _magicClassHighestLevel;
	}
	
	public void setMagicClassHighestLevel(final int magicClassHighestLevel)
	{
		_magicClassHighestLevel = magicClassHighestLevel;
	}
	
	public int getMagicClassLowestLevel()
	{
		return _magicClassLowestLevel;
	}
	
	public void setMagicClassLowestLevel(final int magicClassLowestLevel)
	{
		_magicClassLowestLevel = magicClassLowestLevel;
	}
	
	public int getPhysicClassHighestLevel()
	{
		return _physicClassHighestLevel;
	}
	
	public void setPhysicClassHighestLevel(final int physicClassHighestLevel)
	{
		_physicClassHighestLevel = physicClassHighestLevel;
	}
	
	public int getPhysicClassLowestLevel()
	{
		return _physicClassLowestLevel;
	}
	
	public void setPhysicClassLowestLevel(final int physicClassLowestLevel)
	{
		_physicClassLowestLevel = physicClassLowestLevel;
	}
	
}
