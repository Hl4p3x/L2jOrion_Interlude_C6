/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model;

import l2jorion.game.templates.L2NpcTemplate.AIType;

/**
 * Model used for NPC AI related attributes.
 * @author ShanSoft from L2JTW.
 */
public class L2NpcAIData
{
	private int _primaryAttack;
	private int _minSkillChance;
	private int _maxSkillChance;
	private int _canMove;
	private int _soulshot;
	private int _spiritshot;
	private int _soulshotChance;
	private int _spiritshotChance;
	private int _isChaos;
	private String _clan = null;
	private int _clanRange;
	private String _enemyClan = null;
	private int _enemyRange;
	private int _longRangeSkill;
	private int _shortRangeSkill;
	private int _longRangeChance;
	private int _shortRangeChance;
	private int _switchRangeChance;
	private AIType _aiType = AIType.FIGHTER;
	private int _aggroRange;
	
	// --------------------------------------------------------------------------------------------------------------
	// Setting....
	// --------------------------------------------------------------------------------------------------------------
	public void setPrimaryAttack(int primaryattack)
	{
		_primaryAttack = primaryattack;
	}
	
	public void setMinSkillChance(int skill_chance)
	{
		_minSkillChance = skill_chance;
	}
	
	public void setMaxSkillChance(int skill_chance)
	{
		_maxSkillChance = skill_chance;
	}
	
	public void setCanMove(int canMove)
	{
		_canMove = canMove;
	}
	
	public void setSoulShot(int soulshot)
	{
		_soulshot = soulshot;
	}
	
	public void setSpiritShot(int spiritshot)
	{
		_spiritshot = spiritshot;
	}
	
	public void setSoulShotChance(int soulshotchance)
	{
		_soulshotChance = soulshotchance;
	}
	
	public void setSpiritShotChance(int spiritshotchance)
	{
		_spiritshotChance = spiritshotchance;
	}
	
	public void setShortRangeSkill(int shortrangeskill)
	{
		_shortRangeSkill = shortrangeskill;
	}
	
	public void setShortRangeChance(int shortrangechance)
	{
		_shortRangeChance = shortrangechance;
	}
	
	public void setLongRangeSkill(int longrangeskill)
	{
		_longRangeSkill = longrangeskill;
	}
	
	public void setLongRangeChance(int longrangechance)
	{
		_shortRangeChance = longrangechance;
	}
	
	public void setSwitchRangeChance(int switchrangechance)
	{
		_switchRangeChance = switchrangechance;
	}
	
	public void setIsChaos(int ischaos)
	{
		_isChaos = ischaos;
	}
	
	public void setClan(String clan)
	{
		if ((clan != null) && !clan.equals("") && !clan.equalsIgnoreCase("null"))
		{
			_clan = clan.intern();
		}
	}
	
	public void setClanRange(int clanRange)
	{
		_clanRange = clanRange;
	}
	
	public void setEnemyClan(String enemyClan)
	{
		if ((enemyClan != null) && !enemyClan.equals("") && !enemyClan.equalsIgnoreCase("null"))
		{
			_enemyClan = enemyClan.intern();
		}
	}
	
	public void setEnemyRange(int enemyRange)
	{
		_enemyRange = enemyRange;
	}
	
	public void setAi(String ai)
	{
		if (ai.equalsIgnoreCase("archer"))
		{
			_aiType = AIType.ARCHER;
		}
		else if (ai.equalsIgnoreCase("balanced"))
		{
			_aiType = AIType.BALANCED;
		}
		else if (ai.equalsIgnoreCase("mage"))
		{
			_aiType = AIType.MAGE;
		}
		else if (ai.equalsIgnoreCase("healer"))
		{
			_aiType = AIType.HEALER;
		}
		else if (ai.equalsIgnoreCase("corpse"))
		{
			_aiType = AIType.CORPSE;
		}
		else
		{
			_aiType = AIType.FIGHTER;
		}
	}
	
	public void setAggro(int val)
	{
		_aggroRange = val;
	}
	
	public int getPrimaryAttack()
	{
		return _primaryAttack;
	}
	
	public int getMinSkillChance()
	{
		return _minSkillChance;
	}
	
	public int getMaxSkillChance()
	{
		return _maxSkillChance;
	}
	
	public int getCanMove()
	{
		return _canMove;
	}
	
	public int getSoulShot()
	{
		return _soulshot;
	}
	
	public int getSpiritShot()
	{
		return _spiritshot;
	}
	
	public int getSoulShotChance()
	{
		return _soulshotChance;
	}
	
	public int getSpiritShotChance()
	{
		return _spiritshotChance;
	}
	
	public int getShortRangeSkill()
	{
		return _shortRangeSkill;
	}
	
	public int getShortRangeChance()
	{
		return _shortRangeChance;
	}
	
	public int getLongRangeSkill()
	{
		return _longRangeSkill;
	}
	
	public int getLongRangeChance()
	{
		return _longRangeChance;
	}
	
	public int getSwitchRangeChance()
	{
		return _switchRangeChance;
	}
	
	public int getIsChaos()
	{
		return _isChaos;
	}
	
	public String getClan()
	{
		return _clan;
	}
	
	public int getClanRange()
	{
		return _clanRange;
	}
	
	public String getEnemyClan()
	{
		return _enemyClan;
	}
	
	public int getEnemyRange()
	{
		return _enemyRange;
	}
	
	public AIType getAiType()
	{
		return _aiType;
	}
	
	public int getAggroRange()
	{
		return _aggroRange;
	}
}