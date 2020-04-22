/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.handler.skill;

import l2jorion.Config;
import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.util.random.Rnd;

public class ZakenSelf implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.ZAKENSELF
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		try
		{
			for (final L2Object target1 : targets)
			{
				if (!(target1 instanceof L2Character))
				{
					continue;
				}
				
				final L2Character target = (L2Character) target1;
				final int ch = (Rnd.get(1,15));
				
				if (ch == 1)
				{
					target.teleToLocation(55299, 219120, -2952, false);
				}
				else if (ch == 2)
				{
					target.teleToLocation(56363, 218043, -2952, false);
				}
				else if (ch == 3)
				{
					target.teleToLocation(54245, 220162, -2952, false);
				}
				else if (ch == 4) // looks like bug zone
				{
					target.teleToLocation(56289, 220126, -2952, false);
				}
				else if (ch == 5) // zone from gm command
				{
					target.teleToLocation(55299, 219120, -3224, false);
				}
				else if (ch == 6)
				{
					target.teleToLocation(56363, 218043, -3224, false);
				}
				else if (ch == 7)
				{
					target.teleToLocation(54245, 220162, -3224, false);
				}
				else if (ch == 8)
				{
					target.teleToLocation(56289, 220126, -3224, false);
				}
				else if (ch == 9)
				{
					target.teleToLocation(55299, 219120, -3496, false);
				}
				else if (ch == 10)
				{
					target.teleToLocation(56363, 218043, -3496, false);
				}
				else if (ch == 11)
				{
					target.teleToLocation(54245, 220162, -3496, false);
				}
				else if (ch == 12)
				{
					target.teleToLocation(56289, 220126, -3496, false);
				}
				else
				{
					target.teleToLocation(54228, 218054, -2952, false);
				}
				
				L2NpcInstance zaken = (L2NpcInstance) target;
				if (zaken.getSpawn() != null)
				{
					zaken.getSpawn().setLocx(zaken.getX());
					zaken.getSpawn().setLocy(zaken.getY());
					zaken.getSpawn().setLocz(zaken.getZ());
				}
			}
		}
		catch (final Throwable e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}