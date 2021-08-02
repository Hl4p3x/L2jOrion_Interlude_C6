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

import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.ZoneId;

public class L2SwampZone extends L2ZoneType
{
	private double _move_bonus;
	
	private int _castleId;
	private Castle _castle;
	
	public L2SwampZone(int id)
	{
		super(id);
		
		_move_bonus = 0.5;
		
		_castleId = 0;
		_castle = null;
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		if (name.equals("move_bonus"))
		{
			_move_bonus = Double.parseDouble(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	private Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}
		
		return _castle;
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		if (getCastle() != null)
		{
			// castle zones active only during siege
			if (!getCastle().getSiege().getIsInProgress() || !isEnabled())
			{
				return;
			}
			
			// defenders not affected
			final L2PcInstance player = character.getActingPlayer();
			if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
			{
				return;
			}
		}
		
		character.setInsideZone(ZoneId.ZONE_SWAMP, true);
		if (character instanceof L2PcInstance)
		{
			((L2PcInstance) character).broadcastUserInfo();
		}
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		if (character.isInsideZone(ZoneId.ZONE_SWAMP))
		{
			character.setInsideZone(ZoneId.ZONE_SWAMP, false);
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).broadcastUserInfo();
			}
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
	
	public double getMoveBonus()
	{
		return _move_bonus;
	}
}
