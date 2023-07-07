/*
 * L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.powerpack.engrave;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.extender.BaseExtender;
import l2jorion.game.powerpack.PowerPackConfig;

public class EngraveExtender extends BaseExtender
{
	private final L2ItemInstance _item;
	
	public static boolean canCreateFor(final L2Object object)
	{
		if (EngraveManager.getInstance().isEngraved(object.getObjectId()))
		{
			return true;
		}
		return false;
	}
	
	public EngraveExtender(final L2Object owner)
	{
		super(owner);
		_item = (L2ItemInstance) owner;
	}
	
	@Override
	public Object onEvent(final String event, final Object... params)
	{
		if (event.compareTo(BaseExtender.EventType.SETOWNER.name) == 0)
		{
			final L2Character reference = (L2Character) L2World.getInstance().findObject((Integer) params[1]);
			final L2Character owner = (L2Character) L2World.getInstance().findObject(_item.getOwnerId());
			EngraveManager.getInstance().logAction(_item, reference, owner, params[0].toString());
		}
		else if (event.compareTo("DESTROY") == 0 || event.compareTo("CRYSTALLIZE") == 0 || event.compareTo("MULTISELL") == 0)
		{
			final L2PcInstance owner = (L2PcInstance) L2World.getInstance().findObject(_item.getOwnerId());
			if (EngraveManager.getInstance().getEngraver(_item.getObjectId()) != _item.getOwnerId())
			{
				if (!PowerPackConfig.ENGRAVE_ALLOW_DESTROY)
				{
					if (owner != null)
					{
						owner.sendMessage("You can not destroy the object you are not engraved.");
					}
					return true;
				}
			}
			EngraveManager.getInstance().logAction(_item, owner, null, "Destroy");
		}
		
		return super.onEvent(event, params);
	}
	
}
