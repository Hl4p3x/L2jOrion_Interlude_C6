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
package l2jorion.game.skills.l2skills;

import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.StatsSet;
import l2jorion.util.random.Rnd;

public class L2SkillCreateItem extends L2Skill
{
	private final int[] _createItemId;
	private final int _createItemCount;
	private final int _randomCount;
	
	public L2SkillCreateItem(final StatsSet set)
	{
		super(set);
		_createItemId = set.getIntegerArray("create_item_id");
		_createItemCount = set.getInteger("create_item_count", 0);
		_randomCount = set.getInteger("random_count", 1);
	}
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
		{
			return;
		}
		
		if (_createItemId == null || _createItemCount == 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE));
			return;
		}
		
		final L2PcInstance player = (L2PcInstance) activeChar;
		if (activeChar instanceof L2PcInstance)
		{
			final int count = _createItemCount * (Rnd.nextInt(_randomCount) + 1);
			final int rndid = Rnd.nextInt(_createItemId.length);
			giveItems(player, _createItemId[rndid], count);
		}
	}
	
	public void giveItems(final L2PcInstance activeChar, final int itemId, final int count)
	{
		final L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		
		item.setCount(count);
		activeChar.getInventory().addItem("Skill", item, activeChar, activeChar);
		
		if (count > 1)
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			smsg.addItemName(item.getItemId());
			smsg.addNumber(count);
			activeChar.sendPacket(smsg);
		}
		else
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.EARNED_ITEM);
			smsg.addItemName(item.getItemId());
			activeChar.sendPacket(smsg);
		}
		final ItemList il = new ItemList(activeChar, false);
		activeChar.sendPacket(il);
	}
}
