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

/**
 * sample
 * <p>
 * 4c 01 00 00 00
 * <p>
 * format cd.
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public final class JoinParty extends L2GameServerPacket
{
	
	/** The Constant _S__4C_JOINPARTY. */
	private static final String _S__4C_JOINPARTY = "[S] 3a JoinParty";
	// private static Logger LOG = LoggerFactory.getLogger(JoinParty.class);
	
	/** The _response. */
	private final int _response;
	
	/**
	 * Instantiates a new join party.
	 * @param response the response
	 */
	public JoinParty(final int response)
	{
		_response = response;
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected final void writeImpl()
	{
		writeC(0x3a);
		
		writeD(_response);
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__4C_JOINPARTY;
	}
	
}
