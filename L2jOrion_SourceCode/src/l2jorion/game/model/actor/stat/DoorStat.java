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
package l2jorion.game.model.actor.stat;

import l2jorion.game.model.actor.instance.L2DoorInstance;

public class DoorStat extends CharStat
{
	private int _upgradeHpRatio;
	
	public DoorStat(final L2DoorInstance activeChar)
	{
		super(activeChar);
		
		setLevel((byte) 1);
		
		_upgradeHpRatio = 1;
	}
	
	@Override
	public L2DoorInstance getActiveChar()
	{
		return (L2DoorInstance) super.getActiveChar();
	}
	
	@Override
	public final int getLevel()
	{
		return 1;
	}
	
	public final void setUpgradeHpRatio(int hpRatio)
	{
		_upgradeHpRatio = hpRatio;
	}
	
	public final int getUpgradeHpRatio()
	{
		return _upgradeHpRatio;
	}
}
