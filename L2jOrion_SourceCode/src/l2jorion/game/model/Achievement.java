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
package l2jorion.game.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import l2jorion.Config;
import l2jorion.game.enums.AchType;
import l2jorion.game.managers.AchievementManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.skills.holders.IntIntHolder;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.database.L2DatabaseFactory;

public class Achievement
{
	private static Logger LOG = LoggerFactory.getLogger(Achievement.class);
	
	private static final String INSERT_OR_UPDATE = "INSERT INTO character_achievements (object_id,type,level,count) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE level=VALUES(level), count=VALUES(count)";
	private final static String LOAD_ACHIEVEMENT = "SELECT * FROM character_achievements WHERE object_id=?";
	
	private final L2PcInstance _player;
	private Map<AchType, IntIntHolder> _data;
	
	public Achievement(L2PcInstance player)
	{
		_player = player;
		_data = new HashMap<>();
	}
	
	public void load()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_ACHIEVEMENT))
		{
			ps.setInt(1, _player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_data.put(AchType.valueOf(rs.getString("type")), new IntIntHolder(rs.getInt("level"), rs.getInt("count")));
				}
			}
		}
		catch (Exception e)
		{
			LOG.warn("Couldn't loadAchivementsData() for {} player", _player.getName());
		}
	}
	
	public void cleanUp()
	{
		_data.clear();
	}
	
	public void store(AchType type, int level, int count)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_OR_UPDATE))
		{
			ps.setInt(1, _player.getObjectId());
			ps.setString(2, String.valueOf(type));
			ps.setInt(3, level);
			ps.setInt(4, count);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOG.error("There was problem while AchievementManager#store({}, {}, {}, {})", _player.getName(), type.toString(), level, count);
		}
	}
	
	public void increase(AchType type)
	{
		increase(type, 1, true, true, false, 0); // Increase count by current count + 1 and reset count to 0 on level up.
	}
	
	public void increase(AchType type, boolean daily, int id)
	{
		increase(type, 1, false, false, daily, id); // Increase count by current count + 1 and reset count to 0 on level up.
	}
	
	public void increase(AchType type, int count, boolean increase, boolean reset, boolean daily, int id)
	{
		if (!Config.ACHIEVEMENT_ENABLE)
		{
			return;
		}
		
		if (type == null)
		{
			return;
		}
		
		if (AchievementManager.getInstance().getAchievements().get(type) == null)
		{
			return;
		}
		
		if (_data.containsKey(type) && AchievementManager.getInstance().getStages(type).size() < _data.get(type).getId())
		{
			return;
		}
		
		// It needs for daily missions only to get the correct data
		AchievementHolder achDaily = AchievementManager.getInstance().getAchievements().get(type).get(0);
		if (daily)
		{
			if (achDaily.getNpcId() != 0 && !(String.valueOf(achDaily.getNpcId()).contains(String.valueOf(id))))
			{
				return;
			}
			
			if (achDaily.getItemId() != 0 && !(String.valueOf(achDaily.getItemId()).contains(String.valueOf(id))))
			{
				return;
			}
			
			if (type == AchType.DAILY_ONLINE)
			{
				if (achDaily.getRequired() != getCount(AchType.DAILY_ONLINE))
				{
					return;
				}
			}
		}
		
		_data.put(type, !_data.containsKey(type) ? new IntIntHolder(1, count) : new IntIntHolder(_data.get(type).getId(), increase ? _data.get(type).getValue() + count : count));
		
		final AchievementHolder ach = AchievementManager.getInstance().getAchievements().get(type).get(_data.get(type).getId() - 1);
		
		if ((daily || ach.getLevel() == _data.get(type).getId()) && ach.getRequired() <= _data.get(type).getValue())
		{
			_player.broadcastPacket(new MagicSkillUser(_player, _player, 5103, 1, 1000, 0));
			_player.sendMessage((daily ? "" : "Lv " + ach.getLevel()) + " " + ach.getName() + " achievement completed.");
			_player.addItem("Reward", ach.getRewardId(), ach.getRewardCount(), _player, true);
			_data.put(type, new IntIntHolder(_data.get(type).getId() + 1, reset ? 0 : _data.get(type).getValue()));
		}
		store(type, _data.get(type).getId(), _data.get(type).getValue());
	}
	
	public final int getLevel(AchType type)
	{
		return _data.containsKey(type) ? _data.get(type).getId() : 1;
	}
	
	public final int getCount(AchType type)
	{
		return _data.containsKey(type) ? _data.get(type).getValue() : 0;
	}
	
	public final Map<AchType, IntIntHolder> getData()
	{
		return _data;
	}
	
	public final L2PcInstance getPlayer()
	{
		return _player;
	}
}