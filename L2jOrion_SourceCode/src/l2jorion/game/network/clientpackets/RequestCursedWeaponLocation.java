/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.clientpackets;

import java.util.List;

import javolution.util.FastList;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.model.CursedWeapon;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.Location;
import l2jorion.game.network.serverpackets.ExCursedWeaponLocation;
import l2jorion.game.network.serverpackets.ExCursedWeaponLocation.CursedWeaponInfo;

public final class RequestCursedWeaponLocation extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
		// ignore read packet
	}
	
	@Override
	protected void runImpl()
	{
		final L2Character activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final List<CursedWeaponInfo> list = new FastList<>();
		for (final CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			if (!cw.isActive())
			{
				continue;
			}
			
			final Location pos = cw.getWorldPosition();
			
			if (pos != null)
			{
				list.add(new CursedWeaponInfo(pos, cw.getItemId(), cw.isActivated() ? 1 : 0));
			}
		}
		
		if (!list.isEmpty())
		{
			activeChar.sendPacket(new ExCursedWeaponLocation(list));
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] D0:23 RequestCursedWeaponLocation";
	}
}
