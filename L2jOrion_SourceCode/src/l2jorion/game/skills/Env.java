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
package l2jorion.game.skills;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;

public final class Env
{
	public L2Character player;
	public L2Character target;
	
	public L2ItemInstance item;
	public L2Skill skill;
	
	public double value;
	public double baseValue;
	
	public boolean skillMastery = false;
	
	private L2Character character;
	private L2Character _target;
	
	public L2Character getCharacter()
	{
		return character;
	}
	
	public L2PcInstance getPlayer()
	{
		return character == null ? null : character.getActingPlayer();
	}
	
	public L2Character getTarget()
	{
		return _target;
	}
}
