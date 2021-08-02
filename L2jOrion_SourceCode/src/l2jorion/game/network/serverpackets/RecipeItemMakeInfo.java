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
package l2jorion.game.network.serverpackets;

import l2jorion.Config;
import l2jorion.game.datatables.csv.RecipeTable;
import l2jorion.game.model.L2RecipeList;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class RecipeItemMakeInfo extends L2GameServerPacket
{
	private static final String _S__D7_RECIPEITEMMAKEINFO = "[S] D7 RecipeItemMakeInfo";
	
	private static Logger LOG = LoggerFactory.getLogger(RecipeItemMakeInfo.class);
	
	private final int _id;
	private final L2PcInstance _activeChar;
	private final boolean _success;
	
	public RecipeItemMakeInfo(final int id, final L2PcInstance player, final boolean success)
	{
		_id = id;
		_activeChar = player;
		_success = success;
	}
	
	public RecipeItemMakeInfo(final int id, final L2PcInstance player)
	{
		_id = id;
		_activeChar = player;
		_success = true;
	}
	
	@Override
	protected final void writeImpl()
	{
		final L2RecipeList recipe = RecipeTable.getInstance().getRecipeById(_id);
		
		if (recipe != null)
		{
			writeC(0xD7);
			
			writeD(_id);
			writeD(recipe.isDwarvenRecipe() ? 0 : 1); // 0 = Dwarven - 1 = Common
			writeD((int) _activeChar.getCurrentMp());
			writeD(_activeChar.getMaxMp());
			writeD(_success ? 1 : 0); // item creation success/failed
		}
		else if (Config.DEBUG)
		{
			LOG.info("No recipe found with ID = " + _id);
		}
	}
	
	@Override
	public String getType()
	{
		return _S__D7_RECIPEITEMMAKEINFO;
	}
}
