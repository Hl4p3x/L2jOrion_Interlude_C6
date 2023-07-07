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

import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2SiegeClan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.PacketServer;

public class Die extends PacketServer
{
	private static final String _S__0B_DIE = "[S] 06 Die";
	
	private int _charObjId;
	private boolean _fake;
	private boolean _sweepable;
	private boolean _canTeleport;
	private boolean _allowFixedRes;
	private L2Clan _clan;
	L2Character _activeChar;
	
	public Die(L2Character cha)
	{
		_activeChar = cha;
		if (cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			_allowFixedRes = (player.getAccessLevel().allowFixedRes() || player.isInsideZone(ZoneId.ZONE_RANDOM));
			_clan = player.getClan();
			_canTeleport = !((TvT.is_started() && player._inEventTvT) || (DM.is_started() && player._inEventDM) || (CTF.is_started() && player._inEventCTF) || player.isInFunEvent() || player.isInArenaEvent() || player.isPendingRevive());
		}
		
		_charObjId = cha.getObjectId();
		_fake = !cha.isDead();
		
		if (cha instanceof L2Attackable)
		{
			_sweepable = ((L2Attackable) cha).isSweepActive();
		}
		
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_fake)
		{
			return;
		}
		
		writeC(0x06);
		writeD(_charObjId);
		writeD(_canTeleport ? 0x01 : 0);
		
		if (_canTeleport && _clan != null)
		{
			L2SiegeClan siegeClan = null;
			Boolean isInDefense = false;
			Castle castle = CastleManager.getInstance().getCastle(_activeChar);
			Fort fort = FortManager.getInstance().getFort(_activeChar);
			
			if (castle != null && castle.getSiege().getIsInProgress())
			{
				// siege in progress
				siegeClan = castle.getSiege().getAttackerClan(_clan);
				if (siegeClan == null && castle.getSiege().checkIsDefender(_clan))
				{
					isInDefense = true;
				}
			}
			else if (fort != null && fort.getSiege().getIsInProgress())
			{
				siegeClan = fort.getSiege().getAttackerClan(_clan);
				if (siegeClan == null && fort.getSiege().checkIsDefender(_clan))
				{
					isInDefense = true;
				}
			}
			
			writeD(_clan.getHasHideout() > 0 ? 0x01 : 0x00);
			writeD(_clan.getHasCastle() > 0 || _clan.getHasFort() > 0 || isInDefense ? 0x01 : 0x00);
			writeD(siegeClan != null && !isInDefense && siegeClan.getFlag().size() > 0 ? 0x01 : 0x00);
		}
		else
		{
			writeD(0x00); // 6d 01 00 00 00 - to hide away
			writeD(0x00); // 6d 02 00 00 00 - to castle
			writeD(0x00); // 6d 03 00 00 00 - to siege HQ
		}
		
		writeD(_sweepable ? 0x01 : 0x00); // sweepable (blue Glow)
		writeD(_allowFixedRes ? 0x01 : 0x00);
	}
	
	@Override
	public String getType()
	{
		return _S__0B_DIE;
	}
}
