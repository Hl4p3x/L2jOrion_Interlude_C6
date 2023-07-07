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
package l2jorion.game.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import l2jorion.game.network.PacketServer;

public class MagicEffectIcons extends PacketServer
{
	private static String _S__97_MAGICEFFECTICONS = "[S] 7f MagicEffectIcons";
	
	private List<Effect> _effects;
	
	private class Effect
	{
		protected int _skillId;
		protected int _level;
		protected int _duration;
		
		public Effect(final int pSkillId, final int pLevel, final int pDuration)
		{
			_skillId = pSkillId;
			_level = pLevel;
			_duration = pDuration;
		}
	}
	
	public MagicEffectIcons()
	{
		_effects = new ArrayList<>();
	}
	
	public void addEffect(final int skillId, final int level, final int duration)
	{
		if (skillId == 2031 || skillId == 2032 || skillId == 2037)
		{
			return;
		}
		
		_effects.add(new Effect(skillId, level, duration));
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x7f);
		
		writeH(_effects.size());
		
		for (Effect temp : _effects)
		{
			writeD(temp._skillId);
			writeH(temp._level);
			
			if (temp._duration == -1)
			{
				writeD(-1);
			}
			else
			{
				writeD(temp._duration / 1000);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__97_MAGICEFFECTICONS;
	}
}