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
package l2jorion.game.model.zone.type;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.Race;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class L2MotherTreeZone extends L2ZoneType
{
	public L2MotherTreeZone(final int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) character;
			
			if (player.isInParty())
			{
				for (final L2PcInstance member : player.getParty().getPartyMembers())
				{
					if (member.getRace() != Race.elf)
					{
						return;
					}
				}
			}
			
			player.setInsideZone(ZoneId.ZONE_MOTHERTREE, true);
			player.sendPacket(new SystemMessage(SystemMessageId.ENTER_SHADOW_MOTHER_TREE));
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character instanceof L2PcInstance && character.isInsideZone(ZoneId.ZONE_MOTHERTREE))
		{
			character.setInsideZone(ZoneId.ZONE_MOTHERTREE, false);
			((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.EXIT_SHADOW_MOTHER_TREE));
		}
	}
	
	@Override
	public void onDieInside(final L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(final L2Character character)
	{
	}
	
}
