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
package l2jorion.game.handler.item;

import l2jorion.game.datatables.csv.RecipeTable;
import l2jorion.game.enums.AchType;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2RecipeList;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class Recipes implements IItemHandler
{
	private final int[] ITEM_IDS;
	
	public Recipes()
	{
		final RecipeTable rc = RecipeTable.getInstance();
		ITEM_IDS = new int[rc.getRecipesCount()];
		for (int i = 0; i < rc.getRecipesCount(); i++)
		{
			ITEM_IDS[i] = rc.getRecipeList(i).getRecipeId();
		}
	}
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2RecipeList rp = RecipeTable.getInstance().getRecipeByItemId(item.getItemId());
		if (activeChar.hasRecipeList(rp.getId()))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.RECIPE_ALREADY_REGISTERED);
			activeChar.sendPacket(sm);
		}
		else
		{
			if (rp.isDwarvenRecipe())
			{
				if (activeChar.hasDwarvenCraft())
				{
					if (rp.getLevel() > activeChar.getDwarvenCraft())
					{
						// can't add recipe, becouse create item level too low
						SystemMessage sm = new SystemMessage(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
						activeChar.sendPacket(sm);
					}
					else if (activeChar.getDwarvenRecipeBook().length >= activeChar.GetDwarfRecipeLimit())
					{
						// Up to $s1 recipes can be registered.
						SystemMessage sm = new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER);
						sm.addNumber(activeChar.GetDwarfRecipeLimit());
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.registerDwarvenRecipeList(rp, true);
						activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_ADDED);
						sm.addString(item.getItemName());
						activeChar.sendPacket(sm);
						activeChar.getAchievement().increase(AchType.RECIPE_LEARN);
					}
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
					activeChar.sendPacket(sm);
				}
			}
			else
			{
				if (activeChar.hasCommonCraft())
				{
					if (rp.getLevel() > activeChar.getCommonCraft())
					{
						// can't add recipe, becouse create item level too low
						SystemMessage sm = new SystemMessage(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER);
						activeChar.sendPacket(sm);
						sm = null;
					}
					else if (activeChar.getCommonRecipeBook().length >= activeChar.GetCommonRecipeLimit())
					{
						// Up to $s1 recipes can be registered.
						SystemMessage sm = new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER);
						sm.addNumber(activeChar.GetCommonRecipeLimit());
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.registerCommonRecipeList(rp, true);
						activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
						SystemMessage sm = new SystemMessage(SystemMessageId.S1_ADDED);
						sm.addString(item.getItemName());
						activeChar.sendPacket(sm);
						activeChar.getAchievement().increase(AchType.RECIPE_LEARN);
					}
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT);
					activeChar.sendPacket(sm);
				}
			}
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
