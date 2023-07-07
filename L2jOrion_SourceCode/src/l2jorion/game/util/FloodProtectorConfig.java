/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.util;

public final class FloodProtectorConfig
{
	public String FLOOD_PROTECTOR_TYPE;
	public float FLOOD_PROTECTION_INTERVAL;
	public boolean LOG_FLOODING;
	public int PUNISHMENT_LIMIT;
	public String PUNISHMENT_TYPE;
	public int PUNISHMENT_TIME;
	
	public boolean ALTERNATIVE_METHOD;
	
	public FloodProtectorConfig(final String floodProtectorType)
	{
		super();
		FLOOD_PROTECTOR_TYPE = floodProtectorType;
		ALTERNATIVE_METHOD = false;
	}
	
	public FloodProtectorConfig(final String floodProtectorType, final boolean alt_func)
	{
		super();
		FLOOD_PROTECTOR_TYPE = floodProtectorType;
		ALTERNATIVE_METHOD = alt_func;
	}
}
