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
 *  v 2.1
 */

package l2jorion.game.model.entity.siege.clanhalls;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FortressOfResistance
{
	private static final Logger LOG = LoggerFactory.getLogger(FortressOfResistance.class);
	private static FortressOfResistance _instance;
	private final FastMap<Integer, DamageInfo> _clansDamageInfo;
	
	private static int START_DAY = Config.PARTISAN_DAY;
	private static int HOUR = Config.PARTISAN_HOUR;
	private static int MINUTES = Config.PARTISAN_MINUTES;
	
	private static final int BOSS_ID = 35368;
	private static final int MESSENGER_ID = 35382;
	
	private ScheduledFuture<?> _nurka;
	private ScheduledFuture<?> _announce;
	
	private final Calendar _capturetime = Calendar.getInstance();
	
	public static FortressOfResistance getInstance()
	{
		if (_instance == null)
		{
			_instance = new FortressOfResistance();
		}
		return _instance;
	}
	
	protected class DamageInfo
	{
		public L2Clan _clan;
		public long _damage;
	}
	
	private FortressOfResistance()
	{
		if (Config.PARTISAN_DAY == 1)
		{
			START_DAY = Calendar.MONDAY;
		}
		else if (Config.PARTISAN_DAY == 2)
		{
			START_DAY = Calendar.TUESDAY;
		}
		else if (Config.PARTISAN_DAY == 3)
		{
			START_DAY = Calendar.WEDNESDAY;
		}
		else if (Config.PARTISAN_DAY == 4)
		{
			START_DAY = Calendar.THURSDAY;
		}
		else if (Config.PARTISAN_DAY == 5)
		{
			START_DAY = Calendar.FRIDAY;
		}
		else if (Config.PARTISAN_DAY == 6)
		{
			START_DAY = Calendar.SATURDAY;
		}
		else if (Config.PARTISAN_DAY == 7)
		{
			START_DAY = Calendar.SUNDAY;
		}
		else
		{
			START_DAY = Calendar.FRIDAY;
		}
		
		if (HOUR < 0 || HOUR > 23)
		{
			HOUR = 21;
		}
		if (MINUTES < 0 || MINUTES > 59)
		{
			MINUTES = 0;
		}
		
		_clansDamageInfo = new FastMap<>();
		
		/*
		 * synchronized (this) { setCalendarForNextCaprture(); long milliToCapture = getMilliToCapture(); RunMessengerSpawn rms = new RunMessengerSpawn(); ThreadPoolManager.getInstance().scheduleGeneral(rms, milliToCapture); LOG.info("Fortress of Resistanse: " + milliToCapture / 1000 +
		 * " sec. to capture"); }
		 */
		synchronized (this)
		{
			setCalendarForNextCaprture();
			final long milliToCapture = getMilliToCapture();
			
			RunMessengerSpawn rms = new RunMessengerSpawn();
			ThreadPoolManager.getInstance().scheduleGeneral(rms, milliToCapture);
			
			final long total_millis = System.currentTimeMillis() + milliToCapture;
			
			final GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
			cal.setTimeInMillis(total_millis);
			final String next_ch_siege_date = DateFormat.getInstance().format(cal.getTime());
			
			LOG.info("Fortress of Resistanse: siege will start the " + next_ch_siege_date);
			rms = null;
		}
	}
	
	private void setCalendarForNextCaprture()
	{
		int daysToChange = getDaysToCapture();
		
		if (daysToChange == 7)
			if (_capturetime.get(Calendar.HOUR_OF_DAY) < HOUR)
			{
				daysToChange = 0;
			}
			else if (_capturetime.get(Calendar.HOUR_OF_DAY) == HOUR && _capturetime.get(Calendar.MINUTE) < MINUTES)
			{
				daysToChange = 0;
			}
		
		if (daysToChange > 0)
		{
			_capturetime.add(Calendar.DATE, daysToChange);
		}
		
		_capturetime.set(Calendar.HOUR_OF_DAY, HOUR);
		_capturetime.set(Calendar.MINUTE, MINUTES);
	}
	
	private int getDaysToCapture()
	{
		final int numDays = _capturetime.get(Calendar.DAY_OF_WEEK) - START_DAY;
		
		if (numDays < 0)
			return 0 - numDays;
		
		return 7 - numDays;
	}
	
	private long getMilliToCapture()
	{
		final long currTimeMillis = System.currentTimeMillis();
		final long captureTimeMillis = _capturetime.getTimeInMillis();
		
		return captureTimeMillis - currTimeMillis;
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
		if (!ClanHallManager.getInstance().isFree(21))
		{
			ClanHallManager.getInstance().setFree(21);
		}
		
		Announce("Capture registration of Partisan Hideout has begun!");
		Announce("Now its open for 1 hours!");
		
		L2NpcInstance result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(MESSENGER_ID);
			
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLocx(50335);
			spawn.setLocy(111275);
			spawn.setLocz(-1970);
			spawn.stopRespawn();
			result = spawn.spawnOne();
			template = null;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		final RunBossSpawn rbs = new RunBossSpawn();
		ThreadPoolManager.getInstance().scheduleGeneral(rbs, 3600000); // 60 * 60 * 1000
		LOG.info("Fortress of Resistanse: Messenger spawned!");
		ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(result), 3600000); // 60 * 60 * 1000
	}
	
	protected class RunBossSpawn implements Runnable
	{
		@Override
		public void run()
		{
			BossSpawn();
		}
	}
	
	public void BossSpawn()
	{
		if (!_clansDamageInfo.isEmpty())
		{
			_clansDamageInfo.clear();
		}
		
		L2NpcInstance result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(BOSS_ID);
			
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLocx(44525);
			spawn.setLocy(108867);
			spawn.setLocz(-2020);
			spawn.stopRespawn();
			result = spawn.spawnOne();
			template = null;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		
		LOG.info("Fortress of Resistanse: Boss spawned!");
		Announce("Capture of Partisan Hideout has begun!");
		Announce("You have one hour to kill Nurka!");
		
		_nurka = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(result), 3600000); // 60 * 60 * 1000
		_announce = ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceInfo("No one can`t kill Nurka! Partisan Hideout set free until next week!"), 3600000);
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
			_npc.onDecay();
		}
	}
	
	public final boolean Conditions(final L2PcInstance player)
	{
		if (player != null && player.getClan() != null && player.isClanLeader() && player.getClan().getAuctionBiddedAt() <= 0 && ClanHallManager.getInstance().getClanHallByOwner(player.getClan()) == null && player.getClan().getLevel() > 2)
			return true;
		return false;
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
	
	public void CaptureFinish()
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
			ClanHallManager.getInstance().setOwner(21, clanIdMaxDamage);
			clanIdMaxDamage.setReputationScore(clanIdMaxDamage.getReputationScore() + 600, true);
			update();
			
			Announce("Capture of Partisan Hideout is over.");
			Announce("Now its belong to: '" + clanIdMaxDamage.getName() + "' until next capture.");
		}
		else
		{
			Announce("Capture of Partisan Hideout is over.");
			Announce("No one can`t capture Partisan Hideout.");
		}
		
		_nurka.cancel(true);
		_announce.cancel(true);
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
			statement.setInt(3, 21);
			statement.execute();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
}
