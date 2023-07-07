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
package l2jorion.game.network.serverpackets;

import l2jorion.game.network.PacketServer;

public class TutorialCloseHtml extends PacketServer
{
	private static final String _S__A3_TUTORIALCLOSEHTML = "[S] a3 TutorialCloseHtml";
	
	@Override
	protected void writeImpl()
	{
		writeC(0xa3);
	}
	
	@Override
	public String getType()
	{
		return _S__A3_TUTORIALCLOSEHTML;
	}
}
