package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.L2Skill;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class SkillSpellbookTable
{
	private final static Logger LOG = LoggerFactory.getLogger(SkillTreeTable.class);
	private static SkillSpellbookTable _instance;
	
	private static Map<Integer, Integer> skillSpellbooks;
	
	public static SkillSpellbookTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new SkillSpellbookTable();
		}
		
		return _instance;
	}
	
	private SkillSpellbookTable()
	{
		skillSpellbooks = new FastMap<>();
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT skill_id, item_id FROM skill_spellbooks");
			final ResultSet spbooks = statement.executeQuery();
			
			while (spbooks.next())
			{
				skillSpellbooks.put(spbooks.getInt("skill_id"), spbooks.getInt("item_id"));
			}
			
			spbooks.close();
			DatabaseUtils.close(statement);
			
			LOG.info("SkillSpellbookTable: Loaded " + skillSpellbooks.size() + " spellbooks");
		}
		catch (final Exception e)
		{
			LOG.error("Error while loading spellbook data", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public int getBookForSkill(final int skillId, final int level)
	{
		if (skillId == L2Skill.SKILL_DIVINE_INSPIRATION && level != -1)
		{
			switch (level)
			{
				case 1:
					return 8618; // Ancient Book - Divine Inspiration (Modern Language Version)
				case 2:
					return 8619; // Ancient Book - Divine Inspiration (Original Language Version)
				case 3:
					return 8620; // Ancient Book - Divine Inspiration (Manuscript)
				case 4:
					return 8621; // Ancient Book - Divine Inspiration (Original Version)
				default:
					return -1;
			}
		}
		
		if (!skillSpellbooks.containsKey(skillId))
		{
			return -1;
		}
		
		return skillSpellbooks.get(skillId);
	}
	
	public int getBookForSkill(final L2Skill skill)
	{
		return getBookForSkill(skill.getId(), -1);
	}
	
	public int getBookForSkill(final L2Skill skill, final int level)
	{
		return getBookForSkill(skill.getId(), level);
	}
}
