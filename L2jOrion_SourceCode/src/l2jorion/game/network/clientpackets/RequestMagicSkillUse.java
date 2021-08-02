/*
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
package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class RequestMagicSkillUse extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(RequestMagicSkillUse.class.getName());
	
	private int _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_magicId = readD(); // Identifier of the used skill
		_ctrlPressed = readD() != 0; // True if it's a ForceAttack : Ctrl pressed
		_shiftPressed = readC() != 0; // True if Shift pressed
	}
	
	@Override
	protected void runImpl()
	{
		// Get the current L2PcInstance of the player
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		// Get the level of the used skill
		int level = activeChar.getSkillLevel(_magicId);
		if (level <= 0)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.isOutOfControl())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Get the L2Skill template corresponding to the skillID received from the client
		L2Skill skill = SkillTable.getInstance().getInfo(_magicId, level);
		
		// Check the validity of the skill
		if (skill != null)
		{
			// If Alternate rule Karma punishment is set to true, forbid skill Return to player with Karma
			if (skill.getSkillType() == SkillType.RECALL && !Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// players mounted on pets cannot use any toggle skills
			if (skill.isToggle() && activeChar.isMounted())
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			activeChar.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
		else
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			LOG.error("No skill found with id " + _magicId + " and level " + level);
		}
	}
	
	@Override
	public String getType()
	{
		return "[C] 2F RequestMagicSkillUse";
	}
}
