package l2jorion.game.datatables;

import l2jorion.game.model.L2Skill;

public class HeroSkillTable
{
	private static HeroSkillTable _instance;
	private static L2Skill[] _heroSkills;
	
	private HeroSkillTable()
	{
		_heroSkills = new L2Skill[5];
		_heroSkills[0] = SkillTable.getInstance().getInfo(395, 1);
		_heroSkills[1] = SkillTable.getInstance().getInfo(396, 1);
		_heroSkills[2] = SkillTable.getInstance().getInfo(1374, 1);
		_heroSkills[3] = SkillTable.getInstance().getInfo(1375, 1);
		_heroSkills[4] = SkillTable.getInstance().getInfo(1376, 1);
	}
	
	public static HeroSkillTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new HeroSkillTable();
		}
		
		return _instance;
	}
	
	public static L2Skill[] getHeroSkills()
	{
		return _heroSkills;
	}
	
	public static boolean isHeroSkill(final int skillid)
	{
		Integer[] _HeroSkillsId = new Integer[]
		{
			395,
			396,
			1374,
			1375,
			1376
		};
		
		for (final int id : _HeroSkillsId)
		{
			if (id == skillid)
			{
				return true;
			}
		}
		_HeroSkillsId = null;
		
		return false;
	}
}
