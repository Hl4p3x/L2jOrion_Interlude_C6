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

/*
 *  Author: Qwerty, Scoria dev.
 *  v 2.2
 */

package l2jorion.game.model.entity.siege.clanhalls;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.csv.DoorTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.model.zone.type.L2ClanHallZone;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevastatedCastle
{
	private static final Logger LOG = LoggerFactory.getLogger(DevastatedCastle.class);
	private static DevastatedCastle _instance;
	private final FastMap<Integer, DamageInfo> _clansDamageInfo;
	public ClanHall clanhall = ClanHallManager.getInstance().getClanHallById(34);
	private L2ClanHallZone _zone;
	private List<L2DoorInstance> _doors = new FastList<>();
	private List<String> _doorDefault = new FastList<>();
	
	private static int START_DAY = Config.DEVASTATED_DAY;
	private static int HOUR = Config.DEVASTATED_HOUR;
	private static int MINUTES = Config.DEVASTATED_MINUTES;
	
	private static final int BOSS_ID = 35410; // Gustav @Boss@
	private static final int BOSS1_ID = 35408; // Dietrich @Minion@
	private static final int BOSS2_ID = 35409; // Mikhail @Minion@
	private static final int MESSENGER_ID = 35420;
	
	private ScheduledFuture<?> _gustav;
	private ScheduledFuture<?> _dietrich;
	private ScheduledFuture<?> _mikhail;
	private ScheduledFuture<?> _monsterdespawn;
	
	private L2NpcInstance _minion1 = null;
	private L2NpcInstance _minion2 = null;
	
	private final ArrayList<MonsterLocation> _monsters = new ArrayList<>();
	private ArrayList<L2Spawn> _spawns = new ArrayList<>();
	
	private final Calendar _siegetime = Calendar.getInstance();
	
	private boolean _progress = false;
	
	public static DevastatedCastle getInstance()
	{
		if (_instance == null)
		{
			_instance = new DevastatedCastle();
		}
		return _instance;
	}
	
	protected class DamageInfo
	{
		public L2Clan _clan;
		public long _damage;
	}
	
	private DevastatedCastle()
	{
		if (Config.DEVASTATED_DAY == 1)
		{
			START_DAY = Calendar.MONDAY;
		}
		else if (Config.DEVASTATED_DAY == 2)
		{
			START_DAY = Calendar.TUESDAY;
		}
		else if (Config.DEVASTATED_DAY == 3)
		{
			START_DAY = Calendar.WEDNESDAY;
		}
		else if (Config.DEVASTATED_DAY == 4)
		{
			START_DAY = Calendar.THURSDAY;
		}
		else if (Config.DEVASTATED_DAY == 5)
		{
			START_DAY = Calendar.FRIDAY;
		}
		else if (Config.DEVASTATED_DAY == 6)
		{
			START_DAY = Calendar.SATURDAY;
		}
		else if (Config.DEVASTATED_DAY == 7)
		{
			START_DAY = Calendar.SUNDAY;
		}
		else
		{
			START_DAY = Calendar.MONDAY;
		}
		
		if (HOUR < 0 || HOUR > 23)
		{
			HOUR = 18;
		}
		if (MINUTES < 0 || MINUTES > 59)
		{
			MINUTES = 0;
		}
		
		_clansDamageInfo = new FastMap<>();
		
		synchronized (this)
		{
			setCalendarForNextSiege();
			final long milliToSiege = getMilliToSiege();
			
			RunMessengerSpawn rms = new RunMessengerSpawn();
			ThreadPoolManager.getInstance().scheduleGeneral(rms, milliToSiege);
			
			final long total_millis = System.currentTimeMillis() + milliToSiege;
			
			final GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
			cal.setTimeInMillis(total_millis);
			final String next_ch_siege_date = DateFormat.getInstance().format(cal.getTime());
			
			LOG.info("Devastated Castle: siege will start the " + next_ch_siege_date);
		}
	}
	
	private void setCalendarForNextSiege()
	{
		int daysToChange = getDaysToSiege();
		
		if (daysToChange == 7)
			if (_siegetime.get(Calendar.HOUR_OF_DAY) < HOUR)
			{
				daysToChange = 0;
			}
			else if (_siegetime.get(Calendar.HOUR_OF_DAY) == HOUR && _siegetime.get(Calendar.MINUTE) < MINUTES)
			{
				daysToChange = 0;
			}
		
		if (daysToChange > 0)
		{
			_siegetime.add(Calendar.DATE, daysToChange);
		}
		
		_siegetime.set(Calendar.HOUR_OF_DAY, HOUR);
		_siegetime.set(Calendar.MINUTE, MINUTES);
	}
	
	private int getDaysToSiege()
	{
		final int numDays = _siegetime.get(Calendar.DAY_OF_WEEK) - START_DAY;
		
		if (numDays < 0)
			return 0 - numDays;
		
		return 7 - numDays;
	}
	
	private long getMilliToSiege()
	{
		final long currTimeMillis = System.currentTimeMillis();
		final long siegeTimeMillis = _siegetime.getTimeInMillis();
		
		return siegeTimeMillis - currTimeMillis;
	}
	
	protected class RunMessengerSpawn implements Runnable
	{
		@Override
		public void run()
		{
			MessengerSpawn();
		}
	}
	
	public void MessengerSpawn()
	{
		if (!ClanHallManager.getInstance().isFree(34))
		{
			ClanHallManager.getInstance().setFree(34);
		}
		
		Announce("Siege registration of Devastated castle has begun!");
		Announce("Now its open for 2 hours!");
		
		L2NpcInstance result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(MESSENGER_ID);
			
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLocx(179040);
			spawn.setLocy(-13717);
			spawn.setLocz(-2263);
			spawn.stopRespawn();
			result = spawn.spawnOne();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		
		RunSiege rs = new RunSiege();
		ThreadPoolManager.getInstance().scheduleGeneral(rs, 14400000); // 14400000 [4 * 60 * 60 * 1000]
		
		ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(result), 7200000); // 2 * 60 * 60 * 1000
		ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceInfo("Siege registration of Devastated castle is over!"), 7200000);
		ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceInfo("2 hours until siege begin."), 7200000);
	}
	
	protected class AnnounceInfo implements Runnable
	{
		String _message;
		
		public AnnounceInfo(final String message)
		{
			_message = message;
		}
		
		@Override
		public void run()
		{
			Announce(_message);
		}
	}
	
	public void Announce(final String message)
	{
		Announcements.getInstance().announceToAll(message);
	}
	
	protected class RunSiege implements Runnable
	{
		@Override
		public void run()
		{
			Siege();
		}
	}
	
	public void Siege()
	{
		// Load doors data
		loadDoor();
		
		_progress = true;
		
		L2NpcInstance result = null;
		L2NpcTemplate template = null;
		L2Spawn spawn = null;
		
		//clanhall.spawnDoor();
		_zone = clanhall.getZone();
		_zone.updateZoneStatusForCharactersInside();
		
		try
		{
			fillMonsters();
			
			template = NpcTable.getInstance().getTemplate(BOSS_ID);
			spawn = new L2Spawn(template);
			spawn.setLocx(178298);
			spawn.setLocy(-17624);
			spawn.setLocz(-2194);
			spawn.stopRespawn();
			result = spawn.spawnOne();
			_gustav = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(result), 3600000); // 60 * 60 * 1000
			
			template = NpcTable.getInstance().getTemplate(BOSS1_ID);
			spawn = new L2Spawn(template);
			spawn.setLocx(178306);
			spawn.setLocy(-17535);
			spawn.setLocz(-2195);
			spawn.stopRespawn();
			_minion1 = spawn.spawnOne();
			_dietrich = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(_minion1), 3600000); // 60 * 60 * 1000
			
			template = NpcTable.getInstance().getTemplate(BOSS2_ID);
			spawn = new L2Spawn(template);
			spawn.setLocx(178304);
			spawn.setLocy(-17712);
			spawn.setLocz(-2194);
			spawn.stopRespawn();
			_minion2 = spawn.spawnOne();
			_mikhail = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(_minion2), 3600000); // 60 * 60 * 1000
			
			spawnMonsters();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		Announce("Siege of Devastated castle has begun!");
	}
	
	private static class MonsterLocation
	{
		private final int _id;
		private final int _x;
		private final int _y;
		private final int _z;
		private final int _heading;
		
		protected MonsterLocation(final int id, final int x, final int y, final int z, final int heading)
		{
			_id = id;
			_x = x;
			_y = y;
			_z = z;
			_heading = heading;
		}
		
		protected int getId()
		{
			return _id;
		}
		
		protected int getX()
		{
			return _x;
		}
		
		protected int getY()
		{
			return _y;
		}
		
		protected int getZ()
		{
			return _z;
		}
		
		protected int getHeading()
		{
			return _heading;
		}
		
	}
	
	private void addMonster(final int id, final int x, final int y, final int z, final int heading)
	{
		_monsters.add(new MonsterLocation(id, x, y, z, heading));
	}
	
	private void fillMonsters()
	{
		addMonster(35413, 178288, -14924, -2200, 6320);
		addMonster(35412, 178255, -14884, -2200, 6320);
		addMonster(35413, 178222, -14924, -2200, 6320);
		addMonster(35412, 178222, -14884, -2200, 6320);
		addMonster(35412, 178420, -14904, -2200, 6320);
		addMonster(35412, 178387, -14904, -2200, 6320);
		addMonster(35412, 178288, -14884, -2200, 6320);
		addMonster(35412, 178387, -14884, -2200, 6320);
		addMonster(35413, 178354, -14944, -2200, 6320);
		addMonster(35412, 178321, -14884, -2200, 6320);
		addMonster(35413, 178222, -14944, -2200, 6320);
		addMonster(35412, 178354, -14904, -2200, 6320);
		addMonster(35413, 178255, -14924, -2200, 6320);
		addMonster(35413, 178387, -14924, -2200, 6320);
		addMonster(35413, 178354, -14924, -2200, 6320);
		addMonster(35413, 178420, -14924, -2200, 6320);
		addMonster(35412, 178354, -14884, -2200, 6320);
		addMonster(35412, 178420, -14884, -2200, 6320);
		addMonster(35413, 178454, -14944, -2200, 6320);
		addMonster(35413, 178454, -14924, -2200, 6320);
		addMonster(35413, 178420, -14944, -2200, 6320);
		addMonster(35412, 178222, -14904, -2200, 6320);
		addMonster(35413, 178321, -14944, -2200, 6320);
		addMonster(35413, 178321, -14924, -2200, 6320);
		addMonster(35412, 178288, -14904, -2200, 6320);
		addMonster(35412, 178321, -14904, -2200, 6320);
		addMonster(35413, 178255, -14944, -2200, 6320);
		addMonster(35412, 178255, -14904, -2200, 6320);
		addMonster(35413, 178288, -14944, -2200, 6320);
		addMonster(35412, 178454, -14884, -2200, 6320);
		addMonster(35413, 178387, -14944, -2200, 6320);
		addMonster(35412, 178454, -14904, -2200, 6320);
		addMonster(35413, 179052, -15226, -2221, 6320);
		addMonster(35413, 179260, -15341, -2221, 6320);
		addMonster(35413, 179101, -15253, -2221, 6320);
		addMonster(35413, 179073, -15203, -2221, 6320);
		addMonster(35413, 179144, -15271, -2221, 6320);
		addMonster(35413, 179246, -15285, -2221, 6320);
		addMonster(35413, 179164, -15247, -2221, 6320);
		addMonster(35413, 179226, -15309, -2221, 6320);
		addMonster(35413, 179322, -15349, -2221, 6320);
		addMonster(35413, 179302, -15372, -2221, 6320);
		addMonster(35413, 179189, -15286, -2221, 6320);
		addMonster(35413, 179391, -15439, -2221, 6320);
		addMonster(35413, 179341, -15406, -2221, 6320);
		addMonster(35415, 179503, -15925, -2256, 6320);
		addMonster(35415, 179562, -15984, -2256, 6320);
		addMonster(35415, 179491, -15981, -2256, 6320);
		addMonster(35415, 179370, -16196, -2256, 6320);
		addMonster(35411, 179426, -16009, -2253, 6320);
		addMonster(35415, 179544, -15882, -2256, 6320);
		addMonster(35415, 179599, -15943, -2256, 6320);
		addMonster(35415, 179570, -15901, -2256, 6320);
		addMonster(35415, 179408, -16158, -2256, 6320);
		addMonster(35415, 179279, -16219, -2256, 6320);
		addMonster(35411, 179327, -16101, -2253, 6320);
		addMonster(35411, 179540, -16876, -2246, 6320);
		addMonster(35413, 179009, -15201, -2221, 6320);
		addMonster(35413, 178951, -14699, -2080, 6320);
		addMonster(35413, 178801, -14975, -2080, 6320);
		addMonster(35413, 178865, -14857, -2080, 6320);
		addMonster(35413, 178822, -14936, -2080, 6320);
		addMonster(35413, 178843, -14897, -2080, 6320);
		addMonster(35413, 178929, -14739, -2080, 6320);
		addMonster(35413, 178908, -14778, -2080, 6320);
		addMonster(35413, 178886, -14818, -2080, 6320);
		addMonster(35411, 177719, -15951, -2253, 6320);
		addMonster(35413, 177838, -15664, -2226, 6320);
		addMonster(35411, 177627, -15953, -2250, 6320);
		addMonster(35411, 177387, -15955, -2250, 6320);
		addMonster(35411, 177667, -15921, -2253, 6320);
		addMonster(35413, 177859, -15812, -2226, 6320);
		addMonster(35411, 177661, -16014, -2253, 6320);
		addMonster(35411, 177608, -15985, -2250, 6320);
		addMonster(35415, 177663, -16154, -2250, 6320);
		addMonster(35415, 177530, -16079, -2250, 6320);
		addMonster(35411, 177709, -16043, -2253, 6320);
		addMonster(35411, 177703, -15999, -2250, 6320);
		addMonster(35411, 177810, -16145, -2253, 6320);
		addMonster(35415, 177571, -16105, -2250, 6320);
		addMonster(35415, 177473, -16011, -2250, 6320);
		addMonster(35415, 177612, -16090, -2250, 6320);
		addMonster(35415, 177657, -16113, -2250, 6320);
		addMonster(35415, 177387, -15996, -2250, 6320);
		addMonster(35411, 177564, -15963, -2250, 6320);
		addMonster(35411, 177606, -16035, -2253, 6320);
		addMonster(35411, 177470, -15856, -2250, 6320);
		addMonster(35415, 177428, -15981, -2250, 6320);
		addMonster(35411, 177506, -15887, -2250, 6320);
		addMonster(35411, 177517, -15930, -2250, 6320);
		addMonster(35411, 177308, -15861, -2253, 6320);
		addMonster(35411, 177861, -16164, -2253, 6320);
		addMonster(35413, 177906, -15791, -2226, 6320);
		addMonster(35413, 177765, -15643, -2226, 6320);
		addMonster(35413, 177880, -15744, -2226, 6320);
		addMonster(35413, 177788, -15578, -2226, 6320);
		addMonster(35413, 177811, -15622, -2226, 6320);
		addMonster(35413, 177859, -15704, -2226, 6320);
		addMonster(35413, 177769, -15540, -2226, 6320);
		addMonster(35413, 177813, -15726, -2226, 6320);
		addMonster(35413, 177707, -15427, -2226, 6320);
		addMonster(35413, 177680, -15485, -2226, 6320);
		addMonster(35413, 177722, -15561, -2226, 6320);
		addMonster(35413, 177745, -15498, -2226, 6320);
		addMonster(35413, 177803, -14971, -2210, 6320);
		addMonster(35413, 177727, -15464, -2226, 6320);
		addMonster(35415, 177433, -16026, -2250, 6320);
		addMonster(35413, 177649, -14750, -2210, 6320);
		addMonster(35413, 177619, -14705, -2210, 6320);
		addMonster(35413, 177711, -14838, -2210, 6320);
		addMonster(35413, 177834, -15015, -2210, 6320);
		addMonster(35413, 177741, -14883, -2210, 6320);
		addMonster(35413, 177772, -14927, -2210, 6320);
		addMonster(35413, 177680, -14794, -2210, 6320);
		addMonster(35411, 177400, -15854, -2250, 6320);
		addMonster(35415, 179697, -17781, -2256, 6320);
		addMonster(35411, 179479, -17133, -2256, 6320);
		addMonster(35411, 179485, -17213, -2246, 6320);
		addMonster(35411, 179593, -16876, -2246, 6320);
		addMonster(35411, 179468, -17280, -2256, 6320);
		addMonster(35411, 179433, -16991, -2246, 6320);
		addMonster(35411, 179514, -17281, -2256, 6320);
		addMonster(35411, 179525, -17135, -2256, 6320);
		addMonster(35411, 179444, -16937, -2256, 6320);
		addMonster(35411, 179438, -16875, -2246, 6320);
		addMonster(35415, 179633, -17137, -2256, 6320);
		addMonster(35411, 179537, -17214, -2246, 6320);
		addMonster(35411, 179594, -17453, -2246, 6320);
		addMonster(35415, 179576, -17137, -2256, 6320);
		addMonster(35415, 179508, -17341, -2252, 6320);
		addMonster(35415, 179446, -17391, -2252, 6320);
		addMonster(35415, 179437, -17522, -2252, 6320);
		addMonster(35415, 179536, -17842, -2252, 6320);
		addMonster(35415, 179432, -17719, -2252, 6320);
		addMonster(35415, 179436, -17841, -2252, 6320);
		addMonster(35411, 179542, -17453, -2246, 6320);
		addMonster(35415, 179436, -17776, -2256, 6320);
		addMonster(35415, 179534, -17892, -2252, 6320);
		addMonster(35415, 179482, -17841, -2252, 6320);
		addMonster(35415, 179696, -17844, -2252, 6320);
		addMonster(35415, 179604, -17525, -2252, 6320);
		addMonster(35415, 179707, -17722, -2252, 6320);
		addMonster(35411, 179715, -17454, -2246, 6320);
		addMonster(35411, 179641, -17215, -2246, 6320);
		addMonster(35415, 179665, -17527, -2252, 6320);
		addMonster(35415, 179557, -17524, -2252, 6320);
		addMonster(35415, 179636, -17780, -2256, 6320);
		addMonster(35415, 179694, -17897, -2252, 6320);
		addMonster(35414, 178682, -18200, -2200, 6320);
		addMonster(35413, 178577, -18422, -2250, 6320);
		addMonster(35412, 178745, -18186, -2200, 6320);
		addMonster(35413, 178528, -18499, -2250, 6320);
		addMonster(35414, 178640, -18196, -2200, 6320);
		addMonster(35413, 178766, -18228, -2200, 6320);
		addMonster(35412, 178724, -18184, -2200, 6320);
		addMonster(35412, 178703, -18182, -2200, 6320);
		addMonster(35413, 178575, -18500, -2250, 6320);
		addMonster(35413, 178703, -18222, -2200, 6320);
		addMonster(35413, 178530, -18421, -2250, 6320);
		addMonster(35413, 178523, -18696, -2250, 6320);
		addMonster(35412, 178661, -18178, -2200, 6320);
		addMonster(35414, 178661, -18198, -2200, 6320);
		addMonster(35413, 178788, -18229, -2200, 6320);
		addMonster(35414, 178724, -18204, -2200, 6320);
		addMonster(35413, 178574, -18539, -2250, 6320);
		addMonster(35413, 178578, -18383, -2250, 6320);
		addMonster(35413, 178573, -18577, -2250, 6320);
		addMonster(35413, 178530, -18382, -2250, 6320);
		addMonster(35413, 178528, -18461, -2250, 6320);
		addMonster(35413, 178526, -18538, -2250, 6320);
		addMonster(35413, 178526, -18576, -2250, 6320);
		addMonster(35413, 178570, -18736, -2250, 6320);
		addMonster(35413, 178524, -18618, -2250, 6320);
		addMonster(35413, 178571, -18657, -2250, 6320);
		addMonster(35413, 178523, -18656, -2250, 6320);
		addMonster(35413, 178523, -18735, -2250, 6320);
		addMonster(35413, 178571, -18618, -2250, 6320);
		addMonster(35413, 178571, -18697, -2250, 6320);
		addMonster(35413, 178576, -18461, -2250, 6320);
		addMonster(35413, 178682, -18220, -2200, 6320);
		addMonster(35413, 178661, -18218, -2200, 6320);
		addMonster(35414, 178745, -18206, -2200, 6320);
		addMonster(35412, 178682, -18180, -2200, 6320);
		addMonster(35414, 178703, -18202, -2200, 6320);
		addMonster(35412, 178640, -18176, -2200, 6320);
		addMonster(35414, 178788, -18209, -2200, 6320);
		addMonster(35413, 178640, -18216, -2200, 6320);
		addMonster(35412, 178788, -18189, -2200, 6320);
		addMonster(35413, 178745, -18226, -2200, 6320);
		addMonster(35414, 178766, -18208, -2200, 6320);
		addMonster(35412, 178766, -18188, -2200, 6320);
		addMonster(35413, 178724, -18224, -2200, 6320);
		addMonster(35413, 178430, -16901, -2217, 6320);
		addMonster(35415, 178285, -16832, -2217, 6320);
		addMonster(35413, 178153, -16914, -2217, 6320);
		addMonster(35411, 178398, -16781, -2218, 6320);
		addMonster(35415, 178363, -16768, -2217, 6320);
		addMonster(35413, 178437, -16587, -2217, 6320);
		addMonster(35413, 178431, -16784, -2217, 6320);
		addMonster(35411, 178120, -16714, -2218, 6320);
		addMonster(35415, 178366, -16721, -2217, 6320);
		addMonster(35413, 178433, -16742, -2217, 6320);
		addMonster(35413, 178430, -16862, -2217, 6320);
		addMonster(35411, 178395, -16865, -2218, 6320);
		addMonster(35415, 178288, -16783, -2217, 6320);
		addMonster(35411, 178397, -16824, -2218, 6320);
		addMonster(35415, 178090, -16878, -2217, 6320);
		addMonster(35413, 178154, -16797, -2217, 6320);
		addMonster(35415, 178027, -16773, -2217, 6320);
		addMonster(35413, 178435, -16665, -2217, 6320);
		addMonster(35413, 178433, -16704, -2217, 6320);
		addMonster(35411, 178060, -16868, -2218, 6320);
		addMonster(35413, 178156, -16718, -2217, 6320);
		addMonster(35411, 178324, -16762, -2218, 6320);
		addMonster(35413, 177869, -16832, -2217, 6320);
		addMonster(35411, 178117, -16885, -2218, 6320);
		addMonster(35415, 178366, -16674, -2217, 6320);
		addMonster(35413, 178435, -16627, -2217, 6320);
		addMonster(35415, 178366, -16582, -2217, 6320);
		addMonster(35411, 178403, -16566, -2218, 6320);
		addMonster(35413, 178437, -16548, -2217, 6320);
		addMonster(35415, 178290, -16550, -2217, 6320);
		addMonster(35413, 178160, -16600, -2217, 6320);
		addMonster(35411, 178122, -16759, -2218, 6320);
		addMonster(35411, 178122, -16672, -2218, 6320);
		addMonster(35415, 178095, -16736, -2217, 6320);
		addMonster(35411, 178324, -16804, -2218, 6320);
		addMonster(35415, 178029, -16587, -2217, 6320);
		addMonster(35415, 178290, -16690, -2217, 6320);
		addMonster(35415, 178027, -16820, -2217, 6320);
		addMonster(35413, 178431, -16822, -2217, 6320);
		addMonster(35411, 178326, -16720, -2218, 6320);
		addMonster(35413, 178158, -16679, -2217, 6320);
		addMonster(35413, 178161, -16562, -2217, 6320);
		addMonster(35413, 178158, -16640, -2217, 6320);
		addMonster(35413, 177872, -16714, -2217, 6320);
		addMonster(35411, 178061, -16738, -2218, 6320);
		addMonster(35413, 177871, -16752, -2217, 6320);
		addMonster(35413, 177876, -16597, -2217, 6320);
		addMonster(35413, 177873, -16675, -2217, 6320);
		addMonster(35413, 177869, -16873, -2217, 6320);
		addMonster(35413, 177874, -16637, -2217, 6320);
		addMonster(35411, 178060, -16826, -2218, 6320);
		addMonster(35411, 178063, -16784, -2218, 6320);
		addMonster(35415, 178029, -16727, -2217, 6320);
		addMonster(35413, 177876, -16558, -2217, 6320);
		addMonster(35413, 177870, -16794, -2217, 6320);
		addMonster(35413, 177868, -16911, -2217, 6320);
		addMonster(35413, 178156, -16756, -2217, 6320);
		addMonster(35415, 178092, -16782, -2217, 6320);
		addMonster(35413, 178153, -16876, -2217, 6320);
		addMonster(35413, 178154, -16836, -2217, 6320);
		addMonster(35412, 177217, -17168, -2200, 6320);
		addMonster(35413, 177187, -17128, -2200, 6320);
		addMonster(35413, 177276, -17128, -2200, 6320);
		addMonster(35413, 177335, -17128, -2200, 6320);
		addMonster(35414, 177246, -17148, -2200, 6320);
		addMonster(35412, 177246, -17168, -2200, 6320);
		addMonster(35412, 177129, -17168, -2200, 6320);
		addMonster(35412, 177158, -17168, -2200, 6320);
		addMonster(35413, 177217, -17128, -2200, 6320);
		addMonster(35413, 177158, -17128, -2200, 6320);
		addMonster(35413, 177129, -17128, -2200, 6320);
		addMonster(35414, 177129, -17148, -2200, 6320);
		addMonster(35412, 177276, -17168, -2200, 6320);
		addMonster(35414, 177158, -17148, -2200, 6320);
		addMonster(35413, 177305, -17128, -2200, 6320);
		addMonster(35412, 177187, -17168, -2200, 6320);
		addMonster(35414, 177305, -17148, -2200, 6320);
		addMonster(35413, 177396, -17452, -2207, 6320);
		addMonster(35413, 177397, -17634, -2207, 6320);
		addMonster(35416, 177664, -17599, -2219, 6320);
		addMonster(35416, 177721, -17813, -2219, 6320);
		addMonster(35416, 177501, -17424, -2219, 6320);
		addMonster(35415, 177632, -17788, -2215, 6320);
		addMonster(35413, 177466, -17445, -2207, 6320);
		addMonster(35414, 177217, -17148, -2200, 6320);
		addMonster(35413, 177396, -17545, -2207, 6320);
		addMonster(35414, 177187, -17148, -2200, 6320);
		addMonster(35414, 177335, -17148, -2200, 6320);
		addMonster(35412, 177335, -17168, -2200, 6320);
		addMonster(35412, 177305, -17168, -2200, 6320);
		addMonster(35415, 177439, -17426, -2215, 6320);
		addMonster(35415, 177438, -17472, -2215, 6320);
		addMonster(35416, 177662, -17685, -2219, 6320);
		addMonster(35415, 177527, -17578, -2215, 6320);
		addMonster(35415, 177531, -17399, -2215, 6320);
		addMonster(35415, 177561, -17563, -2215, 6320);
		addMonster(35413, 177604, -17718, -2207, 6320);
		addMonster(35413, 177466, -17537, -2207, 6320);
		addMonster(35416, 177500, -17469, -2219, 6320);
		addMonster(35415, 177527, -17537, -2215, 6320);
		addMonster(35413, 177467, -17809, -2207, 6320);
		addMonster(35413, 177466, -17716, -2207, 6320);
		addMonster(35416, 177496, -17645, -2219, 6320);
		addMonster(35413, 177398, -17817, -2207, 6320);
		addMonster(35416, 177560, -17849, -2219, 6320);
		addMonster(35415, 177524, -17763, -2215, 6320);
		addMonster(35415, 177432, -17791, -2215, 6320);
		addMonster(35413, 177397, -17723, -2207, 6320);
		addMonster(35415, 177523, -17810, -2215, 6320);
		addMonster(35416, 177501, -17384, -2219, 6320);
		addMonster(35413, 177603, -17539, -2207, 6320);
		addMonster(35416, 177725, -17503, -2219, 6320);
		addMonster(35416, 177666, -17508, -2219, 6320);
		addMonster(35415, 177564, -17425, -2215, 6320);
		addMonster(35413, 177749, -17538, -2207, 6320);
		addMonster(35413, 177603, -17446, -2207, 6320);
		addMonster(35413, 177749, -17446, -2207, 6320);
		addMonster(35416, 177728, -17377, -2219, 6320);
		addMonster(35415, 177440, -17380, -2215, 6320);
		addMonster(35416, 177567, -17376, -2219, 6320);
		addMonster(35414, 177276, -17148, -2200, 6320);
		addMonster(35416, 177724, -17594, -2219, 6320);
		addMonster(35415, 177436, -17605, -2215, 6320);
		addMonster(35415, 177636, -17516, -2215, 6320);
		addMonster(35413, 177601, -17811, -2207, 6320);
		addMonster(35413, 177693, -17718, -2207, 6320);
		addMonster(35416, 177722, -17728, -2219, 6320);
		addMonster(35415, 177559, -17708, -2215, 6320);
		addMonster(35416, 177662, -17644, -2219, 6320);
		addMonster(35415, 177635, -17603, -2215, 6320);
		addMonster(35413, 177694, -17811, -2207, 6320);
		addMonster(35415, 177558, -17755, -2215, 6320);
		addMonster(35413, 177693, -17447, -2207, 6320);
		addMonster(35413, 177693, -17629, -2207, 6320);
		addMonster(35416, 177664, -17559, -2219, 6320);
		addMonster(35413, 177466, -17397, -2207, 6320);
		addMonster(35413, 177466, -17627, -2207, 6320);
		addMonster(35416, 177724, -17553, -2219, 6320);
		addMonster(35413, 177750, -17628, -2207, 6320);
		addMonster(35413, 177751, -17810, -2207, 6320);
		addMonster(35416, 177661, -17818, -2219, 6320);
		addMonster(35416, 177496, -17686, -2219, 6320);
		addMonster(35413, 177750, -17717, -2207, 6320);
		addMonster(35415, 177633, -17740, -2215, 6320);
		addMonster(35413, 177693, -17539, -2207, 6320);
		addMonster(35413, 177604, -17628, -2207, 6320);
		addMonster(35415, 177431, -17836, -2215, 6320);
		addMonster(35415, 177631, -17835, -2215, 6320);
		addMonster(35416, 177722, -17768, -2219, 6320);
		addMonster(35413, 177246, -17128, -2200, 6320);
	}
	
	public void spawnMonsters()
	{
		for (final MonsterLocation ml : _monsters)
		{
			try
			{
				final L2NpcTemplate template = NpcTable.getInstance().getTemplate(ml.getId());
				final L2Spawn sp = new L2Spawn(template);
				sp.setAmount(1);
				sp.setLocx(ml.getX());
				sp.setLocy(ml.getY());
				sp.setLocz(ml.getZ());
				sp.setHeading(ml.getHeading());
				sp.setRespawnDelay(300); // 3 * 60
				sp.setLocation(0);
				sp.init();
				_spawns.add(sp);
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		_monsterdespawn = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnMonsters(), 3600000); // 60 * 60 * 1000
	}
	
	protected class DeSpawnMonsters implements Runnable
	{
		@Override
		public void run()
		{
			DeSpawn();
		}
	}
	
	public void DeSpawn()
	{
		synchronized (_spawns)
		{
			if (_spawns.size() > 0)
			{
				for (final L2Spawn sp : _spawns)
				{
					sp.stopRespawn();
					sp.getLastSpawn().doDie(sp.getLastSpawn());
				}
				_spawns.clear();
				_progress = false;
			}
		}
	}
	
	protected class DeSpawnTimer implements Runnable
	{
		L2NpcInstance _npc = null;
		
		public DeSpawnTimer(final L2NpcInstance npc)
		{
			_npc = npc;
		}
		
		@Override
		public void run()
		{
			if (_npc.getNpcId() == 35410)
			{
				Announce("Siege of Devastated castle is over.");
				Announce("Nobody won! ClanHall belong to NPC until next siege.");
				
				FixDoors();
			}
			_npc.onDecay();
		}
	}
	
	public final boolean Conditions(final L2PcInstance player)
	{
		if (player != null && player.getClan() != null && player.isClanLeader() && player.getClan().getAuctionBiddedAt() <= 0 && ClanHallManager.getInstance().getClanHallByOwner(player.getClan()) == null && player.getClan().getLevel() > 3)
			return true;
		
		return false;
	}
	
	public boolean getIsInProgress()
	{
		return _progress;
	}
	
	public void SiegeFinish()
	{
		L2Clan clanIdMaxDamage = null;
		long tempMaxDamage = 0;
		for (final DamageInfo damageInfo : _clansDamageInfo.values())
		{
			if (damageInfo != null)
			{
				if (damageInfo._damage > tempMaxDamage)
				{
					tempMaxDamage = damageInfo._damage;
					clanIdMaxDamage = damageInfo._clan;
				}
			}
		}
		if (clanIdMaxDamage != null)
		{
			ClanHallManager.getInstance().setOwner(34, clanIdMaxDamage);
			clanIdMaxDamage.setReputationScore(clanIdMaxDamage.getReputationScore() + 600, true);
			update();
			
			Announce("Siege of Devastated castle is over.");
			Announce("Now its belong to: '" + clanIdMaxDamage.getName() + "' until next siege.");
		}
		else
		{
			Announce("Siege of Devastated castle is over..");
			Announce("Nobody won! ClanHall belong to NPC until next siege.");
		}
		
		DeSpawn();
		_minion1.onDecay();
		_minion2.onDecay();
		_gustav.cancel(true);
		_dietrich.cancel(true);
		_mikhail.cancel(true);
		_monsterdespawn.cancel(true);
		
		FixDoors();
		_zone = clanhall.getZone();
		_zone.updateZoneStatusForCharactersInside();
	}
	
	public void addSiegeDamage(final L2Clan clan, final long damage)
	{
		DamageInfo clanDamage = _clansDamageInfo.get(clan.getClanId());
		if (clanDamage != null)
		{
			clanDamage._damage += damage;
		}
		else
		{
			clanDamage = new DamageInfo();
			clanDamage._clan = clan;
			clanDamage._damage += damage;
			_clansDamageInfo.put(clan.getClanId(), clanDamage);
		}
	}
	
	private void update()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement("UPDATE clanhall SET paidUntil=?, paid=? WHERE id=?");
			statement.setLong(1, System.currentTimeMillis() + 59760000);
			statement.setInt(2, 1);
			statement.setInt(3, 34);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			LOG.info("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void loadDoor()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Select * from castle_door where castleId = ?");
			statement.setInt(1, 34);
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				// Create list of the door default for use when respawning dead doors
				_doorDefault.add(rs.getString("name") + ";" + rs.getInt("id") + ";" + rs.getInt("x") + ";" + rs.getInt("y") + ";" + rs.getInt("z") + ";" + rs.getInt("range_xmin") + ";" + rs.getInt("range_ymin") + ";" + rs.getInt("range_zmin") + ";" + rs.getInt("range_xmax") + ";" + rs.getInt("range_ymax") + ";" + rs.getInt("range_zmax") + ";" + rs.getInt("hp") + ";" + rs.getInt("pDef") + ";" + rs.getInt("mDef"));
				
				L2DoorInstance door = DoorTable.parseList(_doorDefault.get(_doorDefault.size() - 1));
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				_doors.add(door);
				DoorTable.getInstance().putDoor(door);
				
				ClanHall ch = ClanHallManager.getInstance().getNearbyClanHall(door.getX(), door.getY(), 3000);
				if (ch != null)
				{
					ch.getDoors().add(door);
					door.setClanHall(ch);
				}
			}
			
			rs.close();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void FixDoors()
	{
		for (int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			if (door.getCurrentHp() <= 0)
			{
				door.decayMe(); // Kill current if not killed already
				door = DoorTable.parseList(_doorDefault.get(i));
				
				door.setCurrentHpDirect(door.getMaxHp());
				
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				getDoors().set(i, door);
				
				ClanHall ch = ClanHallManager.getInstance().getNearbyClanHall(door.getX(), door.getY(), 3000);
				if (ch != null)
				{
					ch.getDoors().add(door);
					door.setClanHall(ch);
				}
			}
			else if (door.getOpen())
			{
				door.closeMe();
			}
		}
	}
	
	public final List<L2DoorInstance> getDoors()
	{
		return _doors;
	}
}
