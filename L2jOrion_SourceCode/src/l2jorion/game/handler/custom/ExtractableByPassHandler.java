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
package l2jorion.game.handler.custom;

import l2jorion.Config;
import l2jorion.game.handler.ICustomByPassHandler;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.handler.ItemHandler;
import l2jorion.game.handler.item.ExtractableItems;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ExtractableByPassHandler implements ICustomByPassHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(ExtractableByPassHandler.class);
	
	private static String[] _IDS =
	{
		"extractOne",
		"extractAll"
	};
	
	@Override
	public String[] getByPassCommands()
	{
		return _IDS;
	}
	
	@Override
	public void handleCommand(final String command, final L2PcInstance player, final String parameters)
	{
		try
		{
			final int objId = Integer.parseInt(parameters);
			final L2ItemInstance item = player.getInventory().getItemByObjectId(objId);
			if (item == null)
			{
				return;
			}
			final IItemHandler ih = ItemHandler.getInstance().getItemHandler(item.getItemId());
			if (ih == null || !(ih instanceof ExtractableItems))
			{
				return;
			}
			if (command.compareTo(_IDS[0]) == 0)
			{
				((ExtractableItems) ih).doExtract(player, item, 1);
			}
			else if (command.compareTo(_IDS[1]) == 0)
			{
				((ExtractableItems) ih).doExtract(player, item, item.getCount());
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("ExtractableByPassHandler: Error while running ", e);
		}
		
	}
	
}
