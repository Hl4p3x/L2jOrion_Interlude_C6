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

package l2jorion.game.model;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.xml.AugmentationData;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Stats;
import l2jorion.game.skills.funcs.FuncAdd;
import l2jorion.game.skills.funcs.LambdaConst;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public final class L2Augmentation
{
	private static final Logger LOG = LoggerFactory.getLogger(L2Augmentation.class);
	
	private final L2ItemInstance _item;
	private int _effectsId = 0;
	private augmentationStatBoni _boni = null;
	private L2Skill _skill = null;
	
	public L2Augmentation(final L2ItemInstance item, final int effects, final L2Skill skill, final boolean save)
	{
		_item = item;
		_effectsId = effects;
		_boni = new augmentationStatBoni(_effectsId);
		_skill = skill;
		
		if (save)
		{
			saveAugmentationData();
		}
	}
	
	public L2Augmentation(final L2ItemInstance item, final int effects, final int skill, final int skillLevel, final boolean save)
	{
		this(item, effects, SkillTable.getInstance().getInfo(skill, skillLevel), save);
	}
	
	public class augmentationStatBoni
	{
		public final Stats _stats[];
		public final float _values[];
		private boolean _active;
		
		public augmentationStatBoni(final int augmentationId)
		{
			_active = false;
			FastList<AugmentationData.AugStat> as = AugmentationData.getInstance().getAugStatsById(augmentationId);
			
			_stats = new Stats[as.size()];
			_values = new float[as.size()];
			
			int i = 0;
			for (final AugmentationData.AugStat aStat : as)
			{
				_stats[i] = aStat.getStat();
				_values[i] = aStat.getValue();
				i++;
			}
		}
		
		public void applyBoni(final L2PcInstance player)
		{
			// make sure the boni are not applyed twice
			if (_active)
			{
				return;
			}
			
			for (int i = 0; i < _stats.length; i++)
			{
				player.addStatFunc(new FuncAdd(_stats[i], 0x40, this, new LambdaConst(_values[i])));
			}
			
			_active = true;
		}
		
		public void removeBoni(final L2PcInstance player)
		{
			// make sure the boni is not removed twice
			if (!_active)
			{
				return;
			}
			
			player.removeStatsOwner(this);
			
			_active = false;
		}
	}
	
	private void saveAugmentationData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("INSERT INTO augmentations (item_id,attributes,skill,level) VALUES (?,?,?,?)");
			statement.setInt(1, _item.getObjectId());
			statement.setInt(2, _effectsId);
			
			if (_skill != null)
			{
				statement.setInt(3, _skill.getId());
				statement.setInt(4, _skill.getLevel());
			}
			else
			{
				statement.setInt(3, 0);
				statement.setInt(4, 0);
			}
			
			statement.executeUpdate();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Could not save augmentation for item: " + _item.getObjectId() + " to DB:", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void deleteAugmentationData()
	{
		if (!_item.isAugmented())
		{
			return;
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id=?");
			statement.setInt(1, _item.getObjectId());
			statement.executeUpdate();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Could not delete augmentation for item: " + _item.getObjectId() + " from DB:", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public int getAugmentationId()
	{
		return _effectsId;
	}
	
	public L2Skill getSkill()
	{
		return _skill;
	}
	
	public augmentationStatBoni getBonus()
	{
		return _boni;
	}
	
	public void applyBoni(final L2PcInstance player)
	{
		_boni.applyBoni(player);
		
		if (_skill != null)
		{
			player.addSkill(_skill);
			
			if (_skill.isActive() && Config.ACTIVE_AUGMENTS_START_REUSE_TIME > 0)
			{
				player.disableSkill(_skill, Config.ACTIVE_AUGMENTS_START_REUSE_TIME);
				player.addTimeStamp(_skill, Config.ACTIVE_AUGMENTS_START_REUSE_TIME);
			}
			player.sendSkillList();
		}
	}
	
	public void removeBoni(final L2PcInstance player)
	{
		_boni.removeBoni(player);
		
		// remove the skill if any
		if (_skill != null)
		{
			if (_skill.isPassive())
			{
				player.removeSkill(_skill);
			}
			else
			{
				player.removeSkill(_skill, false);
			}
			
			if ((_skill.isPassive() && Config.DELETE_AUGM_PASSIVE_ON_CHANGE) || (_skill.isActive() && Config.DELETE_AUGM_ACTIVE_ON_CHANGE))
			{
				final L2Effect[] effects = player.getAllEffects();
				
				for (final L2Effect currenteffect : effects)
				{
					final L2Skill effectSkill = currenteffect.getSkill();
					
					if (effectSkill.getId() == _skill.getId())
					{
						player.sendMessage("You feel the power of " + effectSkill.getName() + " leaving yourself.");
						currenteffect.exit(false);
					}
				}
				
			}
			
			player.sendSkillList();
		}
	}
}
