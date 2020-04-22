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

package l2jorion.game.model.zone.type;

import java.util.Iterator;

import javolution.util.FastList;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.util.random.Rnd;

public class L2CastleTeleportZone extends L2ZoneType
{
	
	private final int _spawnLoc[];
	private int _castleId;
	private Castle _castle;
	
	public L2CastleTeleportZone(final int id)
	{
		super(id);
		_spawnLoc = new int[5];
	}
	
	@Override
	public void setParameter(final String name, final String value)
	{
		switch (name)
		{
			case "castleId":
				_castleId = Integer.parseInt(value);
				_castle = CastleManager.getInstance().getCastleById(_castleId);
				_castle.setTeleZone(this);
				break;
			case "spawnMinX":
				_spawnLoc[0] = Integer.parseInt(value);
				break;
			case "spawnMaxX":
				_spawnLoc[1] = Integer.parseInt(value);
				break;
			case "spawnMinY":
				_spawnLoc[2] = Integer.parseInt(value);
				break;
			case "spawnMaxY":
				_spawnLoc[3] = Integer.parseInt(value);
				break;
			case "spawnZ":
				_spawnLoc[4] = Integer.parseInt(value);
				break;
			default:
				super.setParameter(name, value);
				break;
		}
	}
	
	@Override
	protected void onEnter(final L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
	}
	
	@Override
	protected void onExit(final L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
	}
	
	@Override
	public void onDieInside(final L2Character l2character)
	{
	}
	
	@Override
	public void onReviveInside(final L2Character l2character)
	{
	}
	
	public FastList<L2Character> getAllPlayers()
	{
		final FastList<L2Character> players = new FastList<>();
		Iterator<L2Character> i$ = _characterList.values().iterator();
		
		while (i$.hasNext())
		{
			L2Character temp = i$.next();
			
			if (temp instanceof L2PcInstance)
			{
				players.add(temp);
			}
			
			temp = null;
		}
		
		i$ = null;
		
		return players;
	}
	
	public void oustAllPlayers()
	{
		if (_characterList == null)
			return;
		
		if (_characterList.isEmpty())
			return;
		
		Iterator<L2Character> i$ = _characterList.values().iterator();
		while (i$.hasNext())
		{
			L2Character character = i$.next();
			
			if (character != null && character instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) character;
				
				if (player.isOnline() == 1)
				{
					player.teleToLocation(Rnd.get(_spawnLoc[0], _spawnLoc[1]), Rnd.get(_spawnLoc[2], _spawnLoc[3]), _spawnLoc[4]);
				}
				
				player = null;
			}
			
			character = null;
		}
		
		i$ = null;
	}
	
	public int[] getSpawn()
	{
		return _spawnLoc;
	}
}
