/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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
package l2jorion.game.model.entity.olympiad;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.olympiad.Olympiad.COMP_TYPE;
import l2jorion.util.random.Rnd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OlympiadManager implements Runnable
{
	protected static final Logger LOG = LoggerFactory.getLogger(OlympiadManager.class);
	private final Map<Integer, OlympiadGame> _olympiadInstances;
	
	protected static final OlympiadStadium[] STADIUMS =
	{
		new OlympiadStadium(-20814, -21189, -3030),
		new OlympiadStadium(-120324, -225077, -3331),
		new OlympiadStadium(-102495, -209023, -3331),
		new OlympiadStadium(-120156, -207378, -3331),
		new OlympiadStadium(-87628, -225021, -3331),
		new OlympiadStadium(-81705, -213209, -3331),
		new OlympiadStadium(-87593, -207339, -3331),
		new OlympiadStadium(-93709, -218304, -3331),
		new OlympiadStadium(-77157, -218608, -3331),
		new OlympiadStadium(-69682, -209027, -3331),
		new OlympiadStadium(-76887, -201256, -3331),
		new OlympiadStadium(-109985, -218701, -3331),
		new OlympiadStadium(-126367, -218228, -3331),
		new OlympiadStadium(-109629, -201292, -3331),
		new OlympiadStadium(-87523, -240169, -3331),
		new OlympiadStadium(-81748, -245950, -3331),
		new OlympiadStadium(-77123, -251473, -3331),
		new OlympiadStadium(-69778, -241801, -3331),
		new OlympiadStadium(-76754, -234014, -3331),
		new OlympiadStadium(-93742, -251032, -3331),
		new OlympiadStadium(-87466, -257752, -3331),
		new OlympiadStadium(-114413, -213241, -3331)
	};
	
	protected OlympiadManager()
	{
		_olympiadInstances = new FastMap<>();
	}
	
	public static OlympiadManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@Override
	public synchronized void run()
	{
		if (Olympiad.getInstance().isOlympiadEnd())
			return;
		
		final Map<Integer, OlympiadGameTask> _gamesQueue = new FastMap<>();
		while (Olympiad.getInstance().inCompPeriod())
		{
			if (Olympiad.getNobleCount() == 0)
			{
				try
				{
					wait(60000);
				}
				catch (final InterruptedException ex)
				{
					// return;
				}
				continue;
			}
			
			int _gamesQueueSize = 0;
			
			// _compStarted = true;
			final CopyOnWriteArrayList<Integer> readyClasses = Olympiad.hasEnoughRegisteredClassed();
			final boolean readyNonClassed = Olympiad.hasEnoughRegisteredNonClassed();
			if (readyClasses != null || readyNonClassed)
			{
				// set up the games queue
				for (int i = 0; i < STADIUMS.length; i++)
				{
					if (!existNextOpponents(Olympiad.getRegisteredNonClassBased()) && !existNextOpponents(getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses)))
					{
						break;
					}
					if (STADIUMS[i].isFreeToUse())
					{
						if (i < STADIUMS.length / 2)
						{
							if (readyNonClassed && existNextOpponents(Olympiad.getRegisteredNonClassBased()))
							{
								try
								{
									_olympiadInstances.put(i, new OlympiadGame(i, COMP_TYPE.NON_CLASSED, nextOpponents(Olympiad.getRegisteredNonClassBased())));
									_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
									STADIUMS[i].setStadiaBusy();
								}
								catch (final Exception ex)
								{
									if (Config.DEBUG)
										LOG.warn("Olympiad Manager: Stadium - " + i + " assignment, an error has been occurred:", ex);
									
									if (_olympiadInstances.get(i) != null)
									{
										for (final L2PcInstance player : _olympiadInstances.get(i).getPlayers())
										{
											player.sendMessage("Your olympiad registration was canceled due to an error");
											player.setIsInOlympiadMode(false);
											player.setIsOlympiadStart(false);
											player.setOlympiadSide(-1);
											player.setOlympiadGameId(-1);
										}
										_olympiadInstances.remove(i);
									}
									if (_gamesQueue.get(i) != null)
										_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();
									
									// try to reuse this stadia next time
									i--;
								}
							}
							
							else if (readyClasses != null && existNextOpponents(getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses)))
							{
								try
								{
									_olympiadInstances.put(i, new OlympiadGame(i, COMP_TYPE.CLASSED, nextOpponents(getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses))));
									_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
									STADIUMS[i].setStadiaBusy();
								}
								catch (final Exception ex)
								{
									if (Config.DEBUG)
										LOG.warn("Olympiad Manager: Stadium - " + i + " assignment, an error has been occurred:", ex);
									
									if (_olympiadInstances.get(i) != null)
									{
										for (final L2PcInstance player : _olympiadInstances.get(i).getPlayers())
										{
											player.sendMessage("Your olympiad registration was canceled due to an error");
											player.setIsInOlympiadMode(false);
											player.setIsOlympiadStart(false);
											player.setOlympiadSide(-1);
											player.setOlympiadGameId(-1);
										}
										_olympiadInstances.remove(i);
									}
									if (_gamesQueue.get(i) != null)
										_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();
									
									// try to reuse this stadia next time
									i--;
								}
							}
						}
						else
						{
							if (readyClasses != null && existNextOpponents(getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses)))
							{
								try
								{
									_olympiadInstances.put(i, new OlympiadGame(i, COMP_TYPE.CLASSED, nextOpponents(getRandomClassList(Olympiad.getRegisteredClassBased(), readyClasses))));
									_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
									STADIUMS[i].setStadiaBusy();
								}
								catch (final Exception ex)
								{
									if (Config.DEBUG)
										LOG.warn("Olympiad Manager: Stadium - " + i + " assignment, an error has been occurred:", ex);
									
									if (_olympiadInstances.get(i) != null)
									{
										for (final L2PcInstance player : _olympiadInstances.get(i).getPlayers())
										{
											player.sendMessage("Your olympiad registration was canceled due to an error");
											player.setIsInOlympiadMode(false);
											player.setIsOlympiadStart(false);
											player.setOlympiadSide(-1);
											player.setOlympiadGameId(-1);
										}
										_olympiadInstances.remove(i);
									}
									if (_gamesQueue.get(i) != null)
										_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();
									
									// try to reuse this stadia next time
									i--;
								}
							}
							else if (readyNonClassed && existNextOpponents(Olympiad.getRegisteredNonClassBased()))
							{
								try
								{
									_olympiadInstances.put(i, new OlympiadGame(i, COMP_TYPE.NON_CLASSED, nextOpponents(Olympiad.getRegisteredNonClassBased())));
									_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
									STADIUMS[i].setStadiaBusy();
								}
								catch (final Exception ex)
								{
									if (Config.DEBUG)
										LOG.warn("Olympiad Manager: Stadium - " + i + " assignment, an error has been occurred:", ex);
									
									if (_olympiadInstances.get(i) != null)
									{
										for (final L2PcInstance player : _olympiadInstances.get(i).getPlayers())
										{
											player.sendMessage("Your olympiad registration was canceled due to an error");
											player.setIsInOlympiadMode(false);
											player.setIsOlympiadStart(false);
											player.setOlympiadSide(-1);
											player.setOlympiadGameId(-1);
										}
										_olympiadInstances.remove(i);
									}
									if (_gamesQueue.get(i) != null)
										_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();
									
									// try to reuse this stadia next time
									i--;
								}
							}
						}
					}
					else
					{
						if (_gamesQueue.get(i) == null || _gamesQueue.get(i).isTerminated() || _gamesQueue.get(i)._game == null)
						{
							try
							{
								_olympiadInstances.remove(i);
								_gamesQueue.remove(i);
								STADIUMS[i].setStadiaFree();
								i--;
							}
							catch (final Exception e)
							{
								LOG.warn("Exception on OlympiadManager.run() ", e);
							}
						}
					}
				}
				
				/*
				 * try { wait(30000); } catch (InterruptedException e) { }
				 */
				
				// Start games
				_gamesQueueSize = _gamesQueue.size();
				for (int i = 0; i < _gamesQueueSize; i++)
				{
					if (_gamesQueue.get(i) != null && !_gamesQueue.get(i).isTerminated() && !_gamesQueue.get(i).isStarted())
					{
						// start new games
						final Thread T = new Thread(_gamesQueue.get(i));
						T.start();
					}
					
					// Pause one second between games starting to reduce OlympiadManager shout spam.
					try
					{
						wait(1000);
					}
					catch (final InterruptedException e)
					{
						// return;
					}
				}
			}
			
			// wait 30 sec for !stress the server
			try
			{
				wait(30000);
			}
			catch (final InterruptedException e)
			{
				// return;
			}
		}
		
		// when comp time finish wait for all games terminated before execute
		// the cleanup code
		boolean allGamesTerminated = false;
		// wait for all games terminated
		while (!allGamesTerminated)
		{
			try
			{
				wait(30000);
			}
			catch (final InterruptedException e)
			{
			}
			
			if (_gamesQueue.isEmpty())
			{
				allGamesTerminated = true;
			}
			else
			{
				for (final OlympiadGameTask game : _gamesQueue.values())
				{
					allGamesTerminated = allGamesTerminated || game.isTerminated();
				}
			}
		}
		// when all games terminated clear all
		_gamesQueue.clear();
		_olympiadInstances.clear();
		Olympiad.clearRegistered();
		
		OlympiadGame._battleStarted = false;
	}
	
	protected OlympiadGame getOlympiadGame(final int index)
	{
		if (_olympiadInstances != null && !_olympiadInstances.isEmpty())
		{
			return _olympiadInstances.get(index);
		}
		return null;
	}
	
	protected void removeGame(final OlympiadGame game)
	{
		if (_olympiadInstances != null && !_olympiadInstances.isEmpty())
		{
			for (int i = 0; i < _olympiadInstances.size(); i++)
			{
				if (_olympiadInstances.get(i) == game)
				{
					_olympiadInstances.remove(i);
				}
			}
		}
	}
	
	protected Map<Integer, OlympiadGame> getOlympiadGames()
	{
		return _olympiadInstances;
	}
	
	protected CopyOnWriteArrayList<L2PcInstance> getRandomClassList(final Map<Integer, CopyOnWriteArrayList<L2PcInstance>> list, final CopyOnWriteArrayList<Integer> classList)
	{
		if (list == null || classList == null || list.isEmpty() || classList.isEmpty())
			return null;
		
		return list.get(classList.get(Rnd.nextInt(classList.size())));
	}
	
	protected CopyOnWriteArrayList<L2PcInstance> nextOpponents(final CopyOnWriteArrayList<L2PcInstance> list)
	{
		final CopyOnWriteArrayList<L2PcInstance> opponents = new CopyOnWriteArrayList<>();
		if (list.isEmpty())
			return opponents;
		final int loopCount = (list.size() / 2);
		
		int first;
		int second;
		
		if (loopCount < 1)
			return opponents;
		
		first = Rnd.nextInt(list.size());
		opponents.add(list.get(first));
		list.remove(first);
		
		second = Rnd.nextInt(list.size());
		opponents.add(list.get(second));
		list.remove(second);
		
		return opponents;
		
	}
	
	protected boolean existNextOpponents(final CopyOnWriteArrayList<L2PcInstance> list)
	{
		if (list == null)
			return false;
		if (list.isEmpty())
			return false;
		final int loopCount = list.size() >> 1;
		if (loopCount < 1)
			return false;
		
		return true;
	}
	
	protected FastMap<Integer, String> getAllTitles()
	{
		final FastMap<Integer, String> titles = new FastMap<>();
		
		for (final OlympiadGame instance : _olympiadInstances.values())
		{
			if (instance._gamestarted != true)
				continue;
			
			titles.put(instance._stadiumID, instance.getTitle());
		}
		
		return titles;
	}

	private static class SingletonHolder
	{
		protected static final OlympiadManager _instance = new OlympiadManager();
	}
}
