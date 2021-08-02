/*
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
package l2jorion.game.handler.user;

import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.handler.IUserCommandHandler;
import l2jorion.game.managers.ZoneManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.Race;
import l2jorion.game.model.zone.type.L2RespawnZone;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class Loc implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		0
	};
	
	@Override
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		int region;
		L2RespawnZone zone = ZoneManager.getInstance().getZone(activeChar, L2RespawnZone.class);
		if (zone != null)
		{
			region = MapRegionTable.getInstance().getRestartRegion(activeChar, zone.getAllRespawnPoints().get(Race.human)).getLocId();
		}
		else
		{
			region = MapRegionTable.getInstance().getMapRegionLocId(activeChar);
		}
		
		SystemMessageId msg;
		
		switch (region)
		{
			case 910:
				msg = SystemMessageId.LOC_TI_S1_S2_S3;
				break;
			case 911:
				msg = SystemMessageId.LOC_GLUDIN_S1_S2_S3;
				break;
			case 912:
				msg = SystemMessageId.LOC_GLUDIO_S1_S2_S3;
				break;
			case 914:
				msg = SystemMessageId.LOC_ELVEN_S1_S2_S3;
				break;
			case 915:
				msg = SystemMessageId.LOC_DARK_ELVEN_S1_S2_S3;
				break;
			case 916:
				msg = SystemMessageId.LOC_DION_S1_S2_S3;
				break;
			case 917:
				msg = SystemMessageId.LOC_FLORAN_S1_S2_S3;
				break;
			case 918:
				msg = SystemMessageId.LOC_GIRAN_S1_S2_S3;
				break;
			case 919:
				msg = SystemMessageId.LOC_GIRAN_HARBOR_S1_S2_S3;
				break;
			case 920:
				msg = SystemMessageId.LOC_ORC_S1_S2_S3;
				break;
			case 921:
				msg = SystemMessageId.LOC_DWARVEN_S1_S2_S3;
				break;
			case 922:
				msg = SystemMessageId.LOC_OREN_S1_S2_S3;
				break;
			case 923:
				msg = SystemMessageId.LOC_HUNTER_S1_S2_S3;
				break;
			case 924:
				msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
				break;
			case 926:
				msg = SystemMessageId.LOC_HEINE_S1_S2_S3;
				break;
			case 1537:
				msg = SystemMessageId.LOC_RUNE_S1_S2_S3;
				break;
			case 1538:
				msg = SystemMessageId.LOC_GODDARD_S1_S2_S3;
				break;
			case 1714:
				msg = SystemMessageId.LOC_SCHUTTGART_S1_S2_S3;
				break;
			case 1924:
				msg = SystemMessageId.LOC_PRIMEVAL_ISLE_S1_S2_S3;
				break;
			default:
				msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
		}
		
		SystemMessage sm = new SystemMessage(msg);
		sm.addNumber(activeChar.getX());
		sm.addNumber(activeChar.getY());
		sm.addNumber(activeChar.getZ());
		activeChar.sendPacket(sm);
		
		/*
		 * if (activeChar.isGM()) { SystemMessage sm2 = new SystemMessage(msg); sm2.addNumber(activeChar.getClientX()); sm2.addNumber(activeChar.getClientY()); sm2.addNumber(activeChar.getClientZ()); activeChar.sendPacket(sm2); }
		 */
		
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
