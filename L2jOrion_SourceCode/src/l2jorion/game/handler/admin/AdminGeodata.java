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
package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.game.geo.GeoData;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class AdminGeodata implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_geo_pos",
		"admin_geo_spawn_pos",
		"admin_geo_can_move",
		"admin_geo_can_see"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "admin_geo_pos":
			{
				final int worldX = activeChar.getX();
				final int worldY = activeChar.getY();
				final int worldZ = activeChar.getZ();
				final int geoX = GeoData.getInstance().getGeoX(worldX);
				final int geoY = GeoData.getInstance().getGeoY(worldY);
				
				if (GeoData.getInstance().hasGeoPos(geoX, geoY))
				{
					activeChar.sendMessage("WorldX: " + worldX + ", WorldY: " + worldY + ", WorldZ: " + worldZ + ", GeoX: " + geoX + ", GeoY: " + geoY + ", GeoZ: " + GeoData.getInstance().getNearestZ(geoX, geoY, worldZ));
				}
				else
				{
					activeChar.sendMessage("There is no geodata at this position.");
				}
				break;
			}
			case "admin_geo_spawn_pos":
			{
				final int worldX = activeChar.getX();
				final int worldY = activeChar.getY();
				final int worldZ = activeChar.getZ();
				final int geoX = GeoData.getInstance().getGeoX(worldX);
				final int geoY = GeoData.getInstance().getGeoY(worldY);
				
				if (GeoData.getInstance().hasGeoPos(geoX, geoY))
				{
					activeChar.sendMessage("WorldX: " + worldX + ", WorldY: " + worldY + ", WorldZ: " + worldZ + ", GeoX: " + geoX + ", GeoY: " + geoY + ", GeoZ: " + GeoData.getInstance().getSpawnHeight(worldX, worldY, worldZ));
				}
				else
				{
					activeChar.sendMessage("There is no geodata at this position.");
				}
				break;
			}
			case "admin_geo_can_move":
			{
				final L2Object target = activeChar.getTarget();
				if (target != null)
				{
					if (GeoData.getInstance().canSeeTarget(activeChar, target))
					{
						activeChar.sendMessage("Can move beeline.");
					}
					else
					{
						activeChar.sendMessage("Can not move beeline.");
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				break;
			}
			case "admin_geo_can_see":
			{
				final L2Object target = activeChar.getTarget();
				if (target != null)
				{
					if (GeoData.getInstance().canSeeTarget(activeChar, target))
					{
						activeChar.sendMessage("Can see target.");
					}
					else
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
					}
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				}
				break;
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}