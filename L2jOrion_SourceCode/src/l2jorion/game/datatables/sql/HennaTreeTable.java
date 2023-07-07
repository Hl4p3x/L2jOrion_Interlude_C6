package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.csv.HennaTable;
import l2jorion.game.model.actor.instance.L2HennaInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.templates.L2Henna;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class HennaTreeTable
{
	private static Logger LOG = LoggerFactory.getLogger(HennaTreeTable.class);
	
	private static final HennaTreeTable _instance = new HennaTreeTable();
	private final Map<ClassId, List<L2HennaInstance>> _hennaTrees;
	private final boolean _initialized = true;
	
	public static HennaTreeTable getInstance()
	{
		return _instance;
	}
	
	private HennaTreeTable()
	{
		_hennaTrees = new FastMap<>();
		int classId = 0;
		int count = 0;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT class_name, id, parent_id FROM class_list ORDER BY id");
			final ResultSet classlist = statement.executeQuery();
			List<L2HennaInstance> list;
			
			classlist:
			while (classlist.next())
			{
				list = new FastList<>();
				classId = classlist.getInt("id");
				final PreparedStatement statement2 = con.prepareStatement("SELECT class_id, symbol_id FROM henna_trees where class_id=? ORDER BY symbol_id");
				statement2.setInt(1, classId);
				final ResultSet hennatree = statement2.executeQuery();
				
				while (hennatree.next())
				{
					final int id = hennatree.getInt("symbol_id");
					// String name = hennatree.getString("name");
					final L2Henna template = HennaTable.getInstance().getTemplate(id);
					
					if (template == null)
					{
						hennatree.close();
						statement2.close();
						classlist.close();
						DatabaseUtils.close(statement);
						continue classlist;
					}
					
					final L2HennaInstance temp = new L2HennaInstance(template);
					temp.setSymbolId(id);
					temp.setItemIdDye(template.getDyeId());
					temp.setAmountDyeRequire(template.getAmountDyeRequire());
					temp.setPrice(template.getPrice());
					temp.setStatINT(template.getStatINT());
					temp.setStatSTR(template.getStatSTR());
					temp.setStatCON(template.getStatCON());
					temp.setStatMEM(template.getStatMEM());
					temp.setStatDEX(template.getStatDEX());
					temp.setStatWIT(template.getStatWIT());
					
					list.add(temp);
				}
				_hennaTrees.put(ClassId.values()[classId], list);
				
				hennatree.close();
				statement2.close();
				
				count += list.size();
				if (Config.DEBUG)
				{
					LOG.info("Henna Tree for Class: " + classId + " has " + list.size() + " henna templates");
				}
			}
			
			classlist.close();
			DatabaseUtils.close(statement);
			
		}
		catch (final Exception e)
		{
			LOG.error("Error while creating henna tree for classId {}" + " " + classId, e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		LOG.info("HennaTreeTable: Loaded " + count + " henna tree templates");
		
	}
	
	public L2HennaInstance[] getAvailableHenna(final ClassId classId)
	{
		final List<L2HennaInstance> henna = _hennaTrees.get(classId);
		if (henna == null)
		{
			LOG.warn("Hennatree for class {} is not defined !" + " " + classId);
			return new L2HennaInstance[0];
		}
		
		return henna.toArray(new L2HennaInstance[henna.size()]);
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
}
