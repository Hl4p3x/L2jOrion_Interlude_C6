/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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

package l2jorion.game.model.actor.instance;

import l2jorion.game.model.L2Character;
import l2jorion.game.taskmanager.DecayTaskManager;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Util;

public final class L2GourdInstance extends L2MonsterInstance
{
	private String _name;
	private byte _nectar = 0;
	private byte _good = 0;
	
	private static final int[] YETIS =
	{
		35596,
		35597,
		35598,
		35599
	};
	
	public L2GourdInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		DecayTaskManager.getInstance().addDecayTask(this, 180000);
	}
	
	public void setOwner(final String name)
	{
		_name = name;
	}
	
	public String getOwner()
	{
		return _name;
	}
	
	public void addNectar()
	{
		_nectar++;
	}
	
	public byte getNectar()
	{
		return _nectar;
	}
	
	public void addGood()
	{
		_good++;
	}
	
	public byte getGood()
	{
		return _good;
	}
	
	@Override
	public void reduceCurrentHp(double damage, final L2Character attacker, final boolean awake)
	{
		L2NpcInstance target = (L2NpcInstance) attacker.getTarget();
		if (target != null && Util.contains(YETIS, target.getNpcId()))
		{
			super.reduceCurrentHp(damage, attacker, awake);
			return;
		}
		
		if (!attacker.getName().equalsIgnoreCase(getOwner()))
		{
			damage = 0;
		}
		
		if (getTemplate().getNpcId() == 12778 || getTemplate().getNpcId() == 12779)
		{
			if (attacker.getActiveWeaponInstance().getItemId() == 4202 || attacker.getActiveWeaponInstance().getItemId() == 5133 || attacker.getActiveWeaponInstance().getItemId() == 5817 || attacker.getActiveWeaponInstance().getItemId() == 7058)
			{
				super.reduceCurrentHp(damage, attacker, awake);
			}
			else if (damage > 0)
			{
				damage = 0;
			}
		}
		super.reduceCurrentHp(damage, attacker, awake);
	}
}
