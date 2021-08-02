/*
 * Copyright (C) 2004-2016 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.model;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.geo.GeoData;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.templates.L2NpcTemplate;

public abstract class L2Tower extends L2NpcInstance
{
	public L2Tower(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		setIsInvul(false);
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
		return attacker != null && attacker instanceof L2PcInstance && getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress() && getCastle().getSiege().checkIsAttacker(((L2PcInstance) attacker).getClan());
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			player.setTarget(this);
			
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
		}
		else
		{
			if (isAutoAttackable(player) && Math.abs(player.getZ() - getZ()) < 100 && GeoData.getInstance().canSeeTarget(player, this))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
	
	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		onAction(player);
	}
}
