/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.model.actor.instance;

import java.util.concurrent.ScheduledFuture;

import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Summon;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;

public class L2ProtectorInstance extends L2NpcInstance
{
	private ScheduledFuture<?> _aiTask;
	
	private class ProtectorAI implements Runnable
	{
		private final L2ProtectorInstance _caster;
		
		protected ProtectorAI(final L2ProtectorInstance caster)
		{
			_caster = caster;
		}
		
		@Override
		public void run()
		{
			/**
			 * For each known player in range, cast sleep if pvpFlag != 0 or Karma >0 Skill use is just for buff animation
			 */
			for (final L2PcInstance player : getKnownList().getKnownPlayers().values())
			{
				if (player.getKarma() > 0 && Config.PROTECTOR_PLAYER_PK || player.getPvpFlag() != 0 && Config.PROTECTOR_PLAYER_PVP)
				{
					handleCast(player, Config.PROTECTOR_SKILLID, Config.PROTECTOR_SKILLLEVEL);
				}
				final L2Summon activePet = player.getPet();
				
				if (activePet == null)
				{
					continue;
				}
				
				if (activePet.getKarma() > 0 && Config.PROTECTOR_PLAYER_PK || activePet.getPvpFlag() != 0 && Config.PROTECTOR_PLAYER_PVP)
				{
					handleCastonPet(activePet, Config.PROTECTOR_SKILLID, Config.PROTECTOR_SKILLLEVEL);
				}
			}
		}
		
		// Cast for Player
		private boolean handleCast(final L2PcInstance player, final int skillId, final int skillLevel)
		{
			if (player.isGM() || player.isDead() || !player.isVisible() || !isInsideRadius(player, Config.PROTECTOR_RADIUS_ACTION, false, false))
			{
				return false;
			}
			
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
			
			if (player.getFirstEffect(skill) == null)
			{
				final int objId = _caster.getObjectId();
				skill.getEffects(_caster, player, false, false, false);
				broadcastPacket(new MagicSkillUser(_caster, player, skillId, skillLevel, Config.PROTECTOR_SKILLTIME, 0));
				broadcastPacket(new CreatureSay(objId, 0, String.valueOf(getName()), Config.PROTECTOR_MESSAGE));
				
				skill = null;
				return true;
			}
			
			return false;
		}
		
		// Cast for pet
		private boolean handleCastonPet(final L2Summon player, final int skillId, final int skillLevel)
		{
			if (player.isDead() || !player.isVisible() || !isInsideRadius(player, Config.PROTECTOR_RADIUS_ACTION, false, false))
			{
				return false;
			}
			
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
			if (player.getFirstEffect(skill) == null)
			{
				final int objId = _caster.getObjectId();
				skill.getEffects(_caster, player, false, false, false);
				broadcastPacket(new MagicSkillUser(_caster, player, skillId, skillLevel, Config.PROTECTOR_SKILLTIME, 0));
				broadcastPacket(new CreatureSay(objId, 0, String.valueOf(getName()), Config.PROTECTOR_MESSAGE));
				
				skill = null;
				return true;
			}
			
			return false;
		}
	}
	
	public L2ProtectorInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		
		if (_aiTask != null)
		{
			_aiTask.cancel(true);
		}
		
		_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ProtectorAI(this), 3000, 3000);
	}
	
	@Override
	public void deleteMe()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(true);
			_aiTask = null;
		}
		
		super.deleteMe();
	}
	
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return false;
	}
}
