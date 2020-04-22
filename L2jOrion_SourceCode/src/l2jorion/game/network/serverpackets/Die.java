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
package l2jorion.game.network.serverpackets;

import l2jorion.game.datatables.AccessLevel;
import l2jorion.game.datatables.sql.AccessLevels;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2SiegeClan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;

/**
 * sample 0b 952a1048 objectId 00000000 00000000 00000000 00000000 00000000 00000000 format dddddd rev 377 format
 * ddddddd rev 417
 * 
 * @version $Revision: 1.3.3 $ $Date: 2009/04/29 00:46:18 $
 */
public class Die extends L2GameServerPacket
{
	private static final String _S__0B_DIE = "[S] 06 Die";
	private int _charObjId;
	private boolean _fake;
	private boolean _sweepable;
	private boolean _canTeleport;
	private AccessLevel _access = AccessLevels.getInstance()._userAccessLevel;
	private l2jorion.game.model.L2Clan _clan;
	L2Character _activeChar;

	/**
	 * @param cha 
	 */
	public Die(L2Character cha)
	{
		_activeChar = cha;
		if(cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			_access = player.getAccessLevel();
			_clan = player.getClan();
			_canTeleport = !((TvT.is_started() && player._inEventTvT)
							|| (DM.is_started() && player._inEventDM)
							|| (CTF.is_started() && player._inEventCTF)
							|| player.isInFunEvent()
							|| player.isPendingRevive());
		}
		_charObjId = cha.getObjectId();
		_fake = !cha.isDead();
		if(cha instanceof L2Attackable)
		{
			_sweepable = ((L2Attackable) cha).isSweepActive();
		}

	}

	@Override
	protected final void writeImpl()
	{
		if(_fake)
			return;

		writeC(0x06);

		writeD(_charObjId);
		// NOTE:
		// 6d 00 00 00 00 - to nearest village	
		// 6d 01 00 00 00 - to hide away
		// 6d 02 00 00 00 - to castle
		// 6d 03 00 00 00 - to siege HQ
		// sweepable
		// 6d 04 00 00 00 - FIXED

		writeD(_canTeleport ? 0x01 : 0);   // 6d 00 00 00 00 - to nearest village

		if(_canTeleport && _clan != null)
		{
			L2SiegeClan siegeClan = null;
			Boolean isInDefense = false;
			Castle castle = CastleManager.getInstance().getCastle(_activeChar);
			Fort fort = FortManager.getInstance().getFort(_activeChar);

			if(castle != null && castle.getSiege().getIsInProgress())
			{
				//siege in progress
				siegeClan = castle.getSiege().getAttackerClan(_clan);
				if(siegeClan == null && castle.getSiege().checkIsDefender(_clan))
				{
					isInDefense = true;
				}
			}
			else if(fort != null && fort.getSiege().getIsInProgress())
			{
				//siege in progress
				siegeClan = fort.getSiege().getAttackerClan(_clan);
				if(siegeClan == null && fort.getSiege().checkIsDefender(_clan))
				{
					isInDefense = true;
				}
			}
			
			writeD(_clan.getHasHideout() > 0 ? 0x01 : 0x00); // 6d 01 00 00 00 - to hide away
			writeD(_clan.getHasCastle() > 0 || _clan.getHasFort() > 0 || isInDefense ? 0x01 : 0x00); // 6d 02 00 00 00 - to castle
			writeD(siegeClan != null && !isInDefense && siegeClan.getFlag().size() > 0 ? 0x01 : 0x00); // 6d 03 00 00 00 - to siege HQ
		}
		else
		{
			writeD(0x00); // 6d 01 00 00 00 - to hide away
			writeD(0x00); // 6d 02 00 00 00 - to castle
			writeD(0x00); // 6d 03 00 00 00 - to siege HQ
		}

		writeD(_sweepable ? 0x01 : 0x00); // sweepable  (blue Glow)
		writeD(_access.allowFixedRes() ? 0x01 : 0x00); // 6d 04 00 00 00 - to FIXED
	}
	
	@Override
	public String getType()
	{
		return _S__0B_DIE;
	}
}
