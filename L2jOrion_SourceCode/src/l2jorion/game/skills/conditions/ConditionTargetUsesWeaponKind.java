package l2jorion.game.skills.conditions;

import l2jorion.game.skills.Env;
import l2jorion.game.templates.L2Weapon;

public class ConditionTargetUsesWeaponKind extends Condition
{
	private final int _weaponMask;
	
	public ConditionTargetUsesWeaponKind(final int weaponMask)
	{
		_weaponMask = weaponMask;
	}
	
	@Override
	public boolean testImpl(final Env env)
	{
		if (env.target == null)
		{
			return false;
		}
		
		final L2Weapon item = env.target.getActiveWeaponItem();
		
		if (item == null)
		{
			return false;
		}
		
		return (item.getItemType().mask() & _weaponMask) != 0;
	}
}