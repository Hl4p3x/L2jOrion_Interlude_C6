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

import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.network.PacketServer;

public class MagicSkillLaunched extends PacketServer
{
	private static final String _S__8E_MAGICSKILLLAUNCHED = "[S] 8E MagicSkillLaunched";
	
	private final int _charObjId;
	private final int _skillId;
	private final int _skillLevel;
	private int _numberOfTargets;
	private L2Object[] _targets;
	private final int _singleTargetId;
	
	public MagicSkillLaunched(final L2Character cha, final int skillId, final int skillLevel, final L2Object[] targets)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		
		if (targets != null)
		{
			_numberOfTargets = targets.length;
			_targets = targets;
		}
		else
		{
			_numberOfTargets = 1;
			final L2Object[] objs =
			{
				cha
			};
			_targets = objs;
		}
		
		_singleTargetId = 0;
	}
	
	public MagicSkillLaunched(final L2Character cha, final int skillId, final int skillLevel)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_numberOfTargets = 1;
		_singleTargetId = cha.getTargetId();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x76);
		writeD(_charObjId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_numberOfTargets); // also failed or not?
		if (_singleTargetId != 0 || _numberOfTargets == 0)
		{
			writeD(_singleTargetId);
		}
		else
		{
			for (final L2Object target : _targets)
			{
				try
				{
					writeD(target.getObjectId());
				}
				catch (final NullPointerException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					writeD(0); // untested
				}
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__8E_MAGICSKILLLAUNCHED;
	}
	
}
