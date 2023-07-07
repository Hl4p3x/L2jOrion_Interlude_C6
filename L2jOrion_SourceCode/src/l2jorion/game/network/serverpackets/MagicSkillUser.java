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

import l2jorion.game.model.L2Character;
import l2jorion.game.network.PacketServer;

public class MagicSkillUser extends PacketServer
{
	private static final String _S__5A_MAGICSKILLUSER = "[S] 5A MagicSkillUser";
	
	private int _targetId;
	private final int _skillId;
	private final int _skillLevel;
	private final int _hitTime;
	private final int _reuseDelay;
	private final int _charObjId, _x, _y, _z;
	
	public MagicSkillUser(final L2Character cha, final L2Character target, final int skillId, final int skillLevel, final int hitTime, final int reuseDelay)
	{
		_charObjId = cha.getObjectId();
		
		if (target != null)
		{
			_targetId = target.getObjectId();
		}
		else
		{
			_targetId = cha.getTargetId();
		}
		
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
	}
	
	public MagicSkillUser(final L2Character cha, final int skillId, final int skillLevel, final int hitTime, final int reuseDelay)
	{
		_charObjId = cha.getObjectId();
		_targetId = cha.getTargetId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x48);
		writeD(_charObjId);
		writeD(_targetId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_hitTime);
		writeD(_reuseDelay);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeH(0x00); // unknown loop but not AoE
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
	}
	
	@Override
	public String getType()
	{
		return _S__5A_MAGICSKILLUSER;
	}
}