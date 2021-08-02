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
package l2jorion.game.skills.effects;

import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.EtcStatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.skills.Env;

public class EffectCharge extends L2Effect
{
	public int numCharges = 1;
	
	public EffectCharge(final Env env, final EffectTemplate template)
	{
		super(env, template);
		
		if (env.target instanceof L2PcInstance)
		{
			env.target.sendPacket(new EtcStatusUpdate((L2PcInstance) env.target));
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
			sm.addNumber(numCharges);
			getEffected().sendPacket(sm);
		}
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.CHARGE;
	}
	
	@Override
	public boolean onActionTime()
	{
		// ignore
		return true;
	}
	
	@Override
	public int getLevel()
	{
		return numCharges;
	}
	
	public void setNumCharges(final int i)
	{
		numCharges = i;
	}
	
	public void addNumCharges(final int i)
	{
		numCharges = numCharges + i;
	}
}
