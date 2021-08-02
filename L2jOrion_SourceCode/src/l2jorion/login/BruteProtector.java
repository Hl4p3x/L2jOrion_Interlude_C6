package l2jorion.login;

import java.util.ArrayList;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class BruteProtector
{
	private static final Logger LOG = LoggerFactory.getLogger(BruteProtector.class);
	private static final FastMap<String, ArrayList<Integer>> _clients = new FastMap<>();
	
	public static boolean canLogin(final String ip)
	{
		if (!_clients.containsKey(ip))
		{
			_clients.put(ip, new ArrayList<Integer>());
			_clients.get(ip).add((int) (System.currentTimeMillis() / 1000));
			return true;
		}
		
		_clients.get(ip).add((int) (System.currentTimeMillis() / 1000));
		
		/*
		 * I am not quite sure because we can have a number of NATed clients with single IP if (currentAttemptTime - lastAttemptTime <= 2) // Time between last login attempt and current less or equal than 2 seconds return false;
		 */
		if (_clients.get(ip).size() < Config.BRUT_LOGON_ATTEMPTS) // Performing checks only after BRUT_LOGON_ATTEMPTS logon attempts
			return true;
		
		// Calculating average time difference between attempts
		int lastTime = 0;
		int avg = 0;
		for (final int i : _clients.get(ip))
		{
			if (lastTime == 0)
			{
				lastTime = i;
				continue;
			}
			avg += i - lastTime;
			lastTime = i;
		}
		avg = avg / (_clients.get(ip).size() - 1);
		
		// Minimum average time difference (in seconds) between attempts
		if (avg < Config.BRUT_AVG_TIME)
		{
			LOG.warn("IP " + ip + " has " + avg + " seconds between login attempts. Possible BruteForce.");
			// Deleting 2 first elements because if ban will disappear user should have a possibility to logon
			synchronized (_clients.get(ip))
			{
				_clients.get(ip).remove(0);
				_clients.get(ip).remove(0);
			}
			
			return false; // IP have to be banned
		}
		
		synchronized (_clients.get(ip))
		{
			_clients.get(ip).remove(0);
		}
		
		return true;
	}
}
