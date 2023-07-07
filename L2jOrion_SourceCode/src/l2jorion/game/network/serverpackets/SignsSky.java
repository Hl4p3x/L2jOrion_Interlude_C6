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
package l2jorion.game.network.serverpackets;

import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.network.PacketServer;

public class SignsSky extends PacketServer
{
	private static final String _S__F8_SignsSky = "[S] F8 SignsSky";
	
	public static final SignsSky REGULAR_SKY_PACKET = new SignsSky(256);
	public static final SignsSky DUSK_SKY_PACKET = new SignsSky(257);
	public static final SignsSky DAWN_SKY_PACKET = new SignsSky(258);
	public static final SignsSky RED_SKY_PACKET = new SignsSky(259);
	
	private int _state = 0;
	
	public static SignsSky Sky()
	{
		final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
		
		if (SevenSigns.getInstance().isSealValidationPeriod())
		{
			if (compWinner == SevenSigns.CABAL_DAWN)
			{
				return DAWN_SKY_PACKET;
			}
			
			if (compWinner == SevenSigns.CABAL_DUSK)
			{
				return DUSK_SKY_PACKET;
			}
		}
		return REGULAR_SKY_PACKET;
	}
	
	public SignsSky(int state)
	{
		_state = state;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xf8);
		writeH(_state);
	}
	
	@Override
	public String getType()
	{
		return _S__F8_SignsSky;
	}
}
