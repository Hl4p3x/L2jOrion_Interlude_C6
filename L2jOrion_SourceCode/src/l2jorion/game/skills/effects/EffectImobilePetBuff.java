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
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Env;

final class EffectImobilePetBuff extends L2Effect
{
	private L2Summon _pet;
	
	public EffectImobilePetBuff(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}
	
	@Override
	public void onStart()
	{
		_pet = null;
		
		if (getEffected() instanceof L2Summon && getEffector() instanceof L2PcInstance && ((L2Summon) getEffected()).getOwner() == getEffector())
		{
			_pet = (L2Summon) getEffected();
			_pet.setIsImobilised(true);
		}
	}
	
	@Override
	public void onExit()
	{
		if (_pet != null)
		{
			_pet.setIsImobilised(false);
		}
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
