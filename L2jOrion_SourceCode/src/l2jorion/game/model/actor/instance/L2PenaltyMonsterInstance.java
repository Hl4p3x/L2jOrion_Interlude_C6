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

import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.util.random.Rnd;

public class L2PenaltyMonsterInstance extends L2MonsterInstance
{
	private L2PcInstance _ptk;
	
	public L2PenaltyMonsterInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public L2Character getMostHated()
	{
		return _ptk;
	}
	
	@Deprecated
	public void notifyPlayerDead()
	{
		// Monster kill player and can by deleted
		deleteMe();
		
		L2Spawn spawn = getSpawn();
		if (spawn != null)
		{
			spawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(spawn, false);
			spawn = null;
		}
	}
	
	public void setPlayerToKill(final L2PcInstance ptk)
	{
		if (Rnd.nextInt(100) <= 80)
		{
			CreatureSay cs = new CreatureSay(getObjectId(), Say2.ALL, getName(), "mmm your bait was delicious");
			this.broadcastPacket(cs);
			cs = null;
		}
		_ptk = ptk;
		addDamageHate(ptk, 10, 10);
		getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, ptk);
		addAttackerToAttackByList(ptk);
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		if (Rnd.nextInt(100) <= 75)
		{
			CreatureSay cs = new CreatureSay(getObjectId(), Say2.ALL, getName(), "I will tell fishes not to take your bait");
			this.broadcastPacket(cs);
			cs = null;
		}
		return true;
	}
	
}
