package l2jorion.game.autofarm;

public class AutofarmSpell
{
	private final Integer _skillId;
	private final AutofarmSpellType _spellType;
	
	public AutofarmSpell(Integer skillId, AutofarmSpellType spellType)
	{
		_skillId = skillId;
		_spellType = spellType;
	}
	
	public Integer getSkillId()
	{
		return _skillId;
	}
	
	public AutofarmSpellType getSpellType()
	{
		return _spellType;
	}
}
