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

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.geo.GeoData;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2CommanderInstance;
import l2jorion.game.model.actor.instance.L2FolkInstance;
import l2jorion.game.model.actor.instance.L2FortSiegeGuardInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeFlagInstance;
import l2jorion.game.model.actor.instance.L2SiegeGuardInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.skills.Env;

final class EffectFear extends L2Effect
{
	public static final int FEAR_RANGE = 500;
	
	public EffectFear(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.FEAR;
	}
	
	@Override
	public void onStart()
	{
		if (getEffected().isSleeping())
		{
			getEffected().stopSleeping(null);
		}
		
		if (!getEffected().isAfraid())
		{
			getEffected().startFear();
			onActionTime();
		}
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopFear(this);
	}
	
	@Override
	public boolean onActionTime()
	{
		// Fear skills cannot be used l2pcinstance to l2pcinstance. Heroic Dread, Curse: Fear, Fear and Horror are the exceptions.
		if (getEffected() instanceof L2PcInstance && getEffector() instanceof L2PcInstance && getSkill().getId() != 1376 && getSkill().getId() != 1169 && getSkill().getId() != 65 && getSkill().getId() != 1092)
		{
			return false;
		}
		
		if (getEffected() instanceof L2FolkInstance)
		{
			return false;
		}
		
		if (getEffected() instanceof L2SiegeGuardInstance)
		{
			return false;
		}
		
		// Fear skills cannot be used on Headquarters Flag.
		if (getEffected() instanceof L2SiegeFlagInstance)
		{
			return false;
		}
		
		if (getEffected() instanceof L2SiegeSummonInstance)
		{
			return false;
		}
		
		if (getEffected() instanceof L2FortSiegeGuardInstance || getEffected() instanceof L2CommanderInstance)
		{
			return false;
		}
		
		int posX = getEffected().getX();
		int posY = getEffected().getY();
		int posZ = getEffected().getZ();
		
		int signx = -1;
		int signy = -1;
		if (getEffected().getX() > getEffector().getX())
		{
			signx = 1;
		}
		
		if (getEffected().getY() > getEffector().getY())
		{
			signy = 1;
		}
		
		posX += signx * FEAR_RANGE;
		posY += signy * FEAR_RANGE;
		
		if (Config.GEODATA)
		{
			Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), posX, posY, posZ, getEffected().getInstanceId());
			posX = destiny.getX();
			posY = destiny.getY();
		}
		
		getEffected().setRunning();
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, posZ, 0));
		return true;
	}
}
