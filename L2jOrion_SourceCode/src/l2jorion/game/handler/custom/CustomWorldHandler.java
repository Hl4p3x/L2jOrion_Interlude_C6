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
package l2jorion.game.handler.custom;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Rebirth;

/**
 * This will simply manage any custom 'Enter World callers' needed.<br>
 * Rather then having to add them to the core's. (yuck!)
 * @author JStar
 */
public class CustomWorldHandler
{
	
	private static CustomWorldHandler _instance = null;
	
	private CustomWorldHandler()
	{
		// Do Nothing ^_-
	}
	
	/**
	 * Receives the non-static instance of the RebirthManager.
	 * @return
	 */
	public static CustomWorldHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new CustomWorldHandler();
		}
		
		return _instance;
	}
	
	/**
	 * Requests entry into the world - manages appropriately.
	 * @param player
	 */
	public void enterWorld(final L2PcInstance player)
	{
		// L2Rebirth's skills must be actived only on main class
		if (!player.isSubClassActive())
		{
			Rebirth.getInstance().grantRebirthSkills(player);// Rebirth Caller - if player has any skills, they will be granted them.
		}
	}
	
	/**
	 * Requests removal from the world - manages appropriately.
	 * @param player
	 */
	public void exitWorld(final L2PcInstance player)
	{
		// TODO: Remove the rebirth engine's bonus skills from player?
	}
}
