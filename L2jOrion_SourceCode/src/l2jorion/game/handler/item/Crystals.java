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

import l2jorion.game.datatables.SkillTable;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Crystals implements IItemHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(Crystals.class);
	
	private static final int[] ITEM_IDS =
	{
		7906,
		7907,
		7908,
		7909,
		7910,
		7911,
		7912,
		7913,
		7914,
		7915,
		7916,
		7917
	};
	
	@Override
	public synchronized void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		L2PcInstance activeChar;
		// boolean res = false;
		
		if (playable instanceof L2PcInstance)
		{
			activeChar = (L2PcInstance) playable;
		}
		else if (playable instanceof L2PetInstance)
		{
			activeChar = ((L2PetInstance) playable).getOwner();
		}
		else
			return;
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}
		
		if (activeChar.isAllSkillsDisabled())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int itemId = item.getItemId();
		L2Skill skill = null;
		
		switch (itemId)
		{
			case 7906:
				skill = SkillTable.getInstance().getInfo(2248, 1);
				break;
			case 7907:
				skill = SkillTable.getInstance().getInfo(2249, 1);
				break;
			case 7908:
				skill = SkillTable.getInstance().getInfo(2250, 1);
				break;
			case 7909:
				skill = SkillTable.getInstance().getInfo(2251, 1);
				break;
			case 7910:
				skill = SkillTable.getInstance().getInfo(2252, 1);
				break;
			case 7911:
				skill = SkillTable.getInstance().getInfo(2253, 1);
				break;
			case 7912:
				skill = SkillTable.getInstance().getInfo(2254, 1);
				break;
			case 7913:
				skill = SkillTable.getInstance().getInfo(2255, 1);
				break;
			case 7914:
				skill = SkillTable.getInstance().getInfo(2256, 1);
				break;
			case 7915:
				skill = SkillTable.getInstance().getInfo(2257, 1);
				break;
			case 7916:
				skill = SkillTable.getInstance().getInfo(2258, 1);
				break;
			case 7917:
				skill = SkillTable.getInstance().getInfo(2259, 1);
				break;
			default:
		}
		
		if (skill != null)
		{
			activeChar.doCast(skill);
			// We have the consume on XML skills
			// playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}