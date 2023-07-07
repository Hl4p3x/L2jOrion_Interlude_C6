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

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.managers.SiegeManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2SiegeClan;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.templates.L2NpcTemplate;

public class L2SiegeFlagInstance extends L2NpcInstance
{
	private final L2PcInstance _player;
	private final Siege _siege;
	
	public L2SiegeFlagInstance(final L2PcInstance player, final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		
		_player = player;
		_siege = SiegeManager.getInstance().getSiege(_player.getX(), _player.getY(), _player.getZ());
		if (_player.getClan() == null || _siege == null)
		{
			deleteMe();
		}
		else
		{
			L2SiegeClan sc = _siege.getAttackerClan(_player.getClan());
			if (sc == null)
			{
				deleteMe();
			}
			else
			{
				sc.addFlag(this);
			}
		}
	}
	
	@Override
	public boolean isAttackable()
	{
		// Attackable during siege by attacker only
		return getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress();
	}
	
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		// Attackable during siege by attacker only
		return attacker != null && attacker instanceof L2PcInstance && getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress();
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		L2SiegeClan sc = _siege.getAttackerClan(_player.getClan());
		if (sc != null)
		{
			sc.removeFlag(this);
		}
		
		return true;
	}
	
	@Override
	public void onForcedAttack(final L2PcInstance player)
	{
		onAction(player);
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (player == null || !canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else
		{
			if (isAutoAttackable(player) && Math.abs(player.getZ() - getZ()) < 100)
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
}
