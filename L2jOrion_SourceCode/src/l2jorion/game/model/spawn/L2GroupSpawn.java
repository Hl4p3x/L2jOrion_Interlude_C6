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
package l2jorion.game.model.spawn;

import java.lang.reflect.Constructor;

import l2jorion.Config;
import l2jorion.game.datatables.sql.TerritoryTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public class L2GroupSpawn extends L2Spawn
{
	private final Constructor<?> _constructor;
	private final L2NpcTemplate _template;
	
	public L2GroupSpawn(final L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		super(mobTemplate);
		
		_constructor = Class.forName("l2jorion.game.model.actor.instance.L2ControllableMobInstance").getConstructors()[0];
		_template = mobTemplate;
		
		setAmount(1);
	}
	
	public L2NpcInstance doGroupSpawn()
	{
		L2NpcInstance mob = null;
		
		try
		{
			if (_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion"))
			{
				return null;
			}
			
			Object[] parameters =
			{
				IdFactory.getInstance().getNextId(),
				_template
			};
			Object tmp = _constructor.newInstance(parameters);
			
			if (!(tmp instanceof L2NpcInstance))
			{
				return null;
			}
			
			mob = (L2NpcInstance) tmp;
			
			int newlocx = 0;
			int newlocy = 0;
			int newlocz = 0;
			
			if (getLocx() == 0 && getLocy() == 0)
			{
				if (getLocation() == 0)
				{
					return null;
				}
				
				final Location location = TerritoryTable.getInstance().getRandomPoint(getLocation());
				if (location != null)
				{
					newlocx = location.getX();
					newlocy = location.getY();
					newlocz = location.getZ();
				}
			}
			else
			{
				newlocx = getLocx();
				newlocy = getLocy();
				newlocz = getLocz();
			}
			
			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			
			if (getHeading() == -1)
			{
				mob.setHeading(Rnd.nextInt(61794));
			}
			else
			{
				mob.setHeading(getHeading());
			}
			
			mob.setSpawn(this);
			mob.spawnMe(newlocx, newlocy, newlocz);
			mob.onSpawn();
			
			if (Config.DEBUG)
			{
				LOG.debug("spawned Mob ID: " + _template.npcId + " ,at: " + mob.getX() + " x, " + mob.getY() + " y, " + mob.getZ() + " z");
			}
			
			parameters = null;
			tmp = null;
			
			return mob;
			
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("NPC class not found: " + e);
			return null;
		}
	}
}
