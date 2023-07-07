/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.skills.effects;

import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2ArtefactInstance;
import l2jorion.game.model.actor.instance.L2ControlTowerInstance;
import l2jorion.game.model.actor.instance.L2EffectPointInstance;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2SiegeFlagInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.network.serverpackets.BeginRotation;
import l2jorion.game.network.serverpackets.StopRotation;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.skills.Env;

public class EffectBluff extends L2Effect
{
	
	public EffectBluff(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BLUFF;
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
	
	/*
	 * @Override public void onExit() { super.onExit(); }
	 */
	
	@Override
	public void onStart()
	{
		if (getEffected().isDead() || getEffected().isAfraid())
		{
			return;
		}
		
		if (getEffected() instanceof L2FolkInstance || getEffected() instanceof L2ControlTowerInstance || getEffected() instanceof L2ArtefactInstance || getEffected() instanceof L2EffectPointInstance || getEffected() instanceof L2SiegeFlagInstance || getEffected() instanceof L2SiegeSummonInstance)
		{
			return;
		}
		
		super.onStart();
		
		// break target
		getEffected().setTarget(null);
		// stop cast
		getEffected().breakCast();
		// stop attacking
		getEffected().breakAttack();
		// stop follow
		getEffected().getAI().stopFollow();
		// stop auto attack
		getEffected().getAI().clientStopAutoAttack();
		
		getEffected().broadcastPacket(new BeginRotation(getEffected(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new StopRotation(getEffected(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
		
		getEffected().sendPacket(new ValidateLocation(getEffector()));
		getEffector().sendPacket(new ValidateLocation(getEffected()));
		onActionTime();
	}
}
