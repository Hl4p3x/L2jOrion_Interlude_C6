/*
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
package l2jorion.game.skills.effects;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.geo.GeoData;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.Location;
import l2jorion.game.network.serverpackets.FlyToLocation;
import l2jorion.game.network.serverpackets.FlyToLocation.FlyType;
import l2jorion.game.network.serverpackets.ValidateLocation;
import l2jorion.game.skills.Env;
import l2jorion.game.util.Util;

public class EffectWarp extends L2Effect
{
	private int x, y, z;
	private L2Character _actor;
	
	public EffectWarp(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.WARP;
	}
	
	@Override
	public void onStart()
	{
		_actor = isSelfEffect() ? getEffector() : getEffected();
		
		if (_actor.isMovementDisabled())
		{
			return;
		}
		
		int _radius = getSkill().getFlyRadius();
		
		double angle = Util.convertHeadingToDegree(_actor.getHeading());
		double radian = Math.toRadians(angle);
		double course = Math.toRadians(getSkill().getFlyCourse());
		
		int x1 = (int) (Math.cos(Math.PI + radian + course) * _radius);
		int y1 = (int) (Math.sin(Math.PI + radian + course) * _radius);
		
		x = _actor.getX() + x1;
		y = _actor.getY() + y1;
		z = _actor.getZ();
		
		if (Config.GEODATA)
		{
			Location destiny = GeoData.getInstance().moveCheck(_actor.getX(), _actor.getY(), _actor.getZ(), x, y, z, _actor.getInstanceId());
			x = destiny.getX();
			y = destiny.getY();
			z = destiny.getZ();
		}
		
		_actor.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		_actor.broadcastPacket(new FlyToLocation(_actor, x, y, z, FlyType.DUMMY));
		_actor.abortAttack();
		_actor.abortCast();
		
		_actor.setXYZ(x, y, z);
		_actor.broadcastPacket(new ValidateLocation(_actor));
		
		return;
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}