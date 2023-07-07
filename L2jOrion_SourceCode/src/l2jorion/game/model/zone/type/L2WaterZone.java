/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model.zone.type;

import java.util.Collection;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.NpcInfo;

public class L2WaterZone extends L2ZoneType
{
	public L2WaterZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance && ((L2PcInstance) character).isInBoat())
		{
			return;
		}
		
		character.setInsideZone(ZoneId.ZONE_WATER, true);
		
		if (character instanceof L2PcInstance)
		{
			if (((L2PcInstance) character).isGM())
			{
				((L2PcInstance) character).sendMessage("You entered water name: " + getName());
			}
			((L2PcInstance) character).broadcastUserInfo();
		}
		else if (character instanceof L2NpcInstance)
		{
			Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
			{
				for (L2PcInstance player : plrs)
				{
					player.sendPacket(new NpcInfo((L2NpcInstance) character, player));
				}
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(ZoneId.ZONE_WATER, false);
		
		if (character instanceof L2PcInstance)
		{
			if (((L2PcInstance) character).isGM())
			{
				((L2PcInstance) character).sendMessage("You exited water name:" + getName());
			}
			
			((L2PcInstance) character).broadcastUserInfo();
		}
		else if (character instanceof L2NpcInstance)
		{
			Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
			for (final L2PcInstance player : plrs)
			{
				player.sendPacket(new NpcInfo((L2NpcInstance) character, player));
			}
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
	public int getWaterZ()
	{
		return getZone().getHighZ();
	}
}
