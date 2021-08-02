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
package l2jorion.game.model.actor.instance;

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Tower;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.templates.L2NpcTemplate;

public class L2ControlTowerInstance extends L2Tower
{
	
	private List<L2Spawn> _guards;
	
	public L2ControlTowerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (getCastle().getSiege().getIsInProgress())
		{
			getCastle().getSiege().killedCT(this);
			
			if ((_guards != null) && !_guards.isEmpty())
			{
				for (L2Spawn spawn : _guards)
				{
					try
					{
						spawn.stopRespawn();
						// spawn.getLastSpawn().doDie(spawn.getLastSpawn());
					}
					catch (Exception e)
					{
						LOG.warn("Error at L2ControlTowerInstance {}", e);
					}
				}
				_guards.clear();
			}
		}
		return super.doDie(killer);
	}
	
	public void registerGuard(final L2Spawn guard)
	{
		getGuards().add(guard);
	}
	
	public final List<L2Spawn> getGuards()
	{
		if (_guards == null)
		{
			_guards = new FastList<>();
		}
		return _guards;
	}
}
