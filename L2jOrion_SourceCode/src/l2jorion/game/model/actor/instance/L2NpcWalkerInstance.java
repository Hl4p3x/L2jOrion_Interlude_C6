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
package l2jorion.game.model.actor.instance;

import java.util.Collection;

import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2NpcWalkerAI;
import l2jorion.game.model.L2Character;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.templates.L2NpcTemplate;

public class L2NpcWalkerInstance extends L2NpcInstance
{
	public L2NpcWalkerInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		setAI(new L2NpcWalkerAI(this));
	}
	
	@Override
	public void setAI(final L2CharacterAI newAI)
	{
		if (_ai == null)
		{
			super.setAI(newAI);
		}
	}
	
	@Override
	public void onSpawn()
	{
		((L2NpcWalkerAI) getAI()).setHomeX(getX());
		((L2NpcWalkerAI) getAI()).setHomeY(getY());
		((L2NpcWalkerAI) getAI()).setHomeZ(getZ());
	}
	
	public void broadcastChat(final String chat)
	{
		Collection<L2PcInstance> _knownPlayers = getKnownList().getKnownPlayersInRadius(500);
		
		if (_knownPlayers == null)
		{
			return;
		}
		
		if (_knownPlayers.size() > 0)
		{
			CreatureSay cs = new CreatureSay(getObjectId(), 0, getName(), chat);
			
			for (final L2PcInstance players : _knownPlayers)
			{
				players.sendPacket(cs);
			}
		}
	}
	
	@Override
	public void reduceCurrentHp(final double i, final L2Character attacker, final boolean awake)
	{
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		return false;
	}
	
	@Override
	public L2CharacterAI getAI()
	{
		return super.getAI();
	}
}
