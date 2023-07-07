package l2jorion.game.model.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

import l2jorion.Config;
import l2jorion.game.enums.AchType;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.zone.type.L2OlympiadStadiumZone;
import l2jorion.game.network.PacketServer;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ExOlympiadUserInfo;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

abstract public class OlympiadGameNormal extends AbstractOlympiadGame
{
	protected int _damageP1 = 0;
	protected int _damageP2 = 0;
	
	protected Participant _playerOne;
	protected Participant _playerTwo;
	
	protected OlympiadGameNormal(int id, Participant[] opponents)
	{
		super(id);
		
		_playerOne = opponents[0];
		_playerTwo = opponents[1];
		
		_playerOne.player.setOlympiadGameId(id);
		_playerTwo.player.setOlympiadGameId(id);
	}
	
	protected static final Participant[] createListOfParticipants(List<Integer> list)
	{
		if (list == null || list.isEmpty() || list.size() < 2)
		{
			return null;
		}
		
		int playerOneObjectId = 0;
		L2PcInstance playerOne = null;
		L2PcInstance playerTwo = null;
		
		while (list.size() > 1)
		{
			playerOneObjectId = list.remove(Rnd.get(list.size()));
			playerOne = L2World.getInstance().getPlayer(playerOneObjectId);
			if (playerOne == null || playerOne.isOnline() == 0)
			{
				continue;
			}
			
			playerTwo = L2World.getInstance().getPlayer(list.remove(Rnd.get(list.size())));
			if (playerTwo == null || playerTwo.isOnline() == 0)
			{
				list.add(playerOneObjectId);
				continue;
			}
			
			Participant[] result = new Participant[2];
			result[0] = new Participant(playerOne, 1);
			result[1] = new Participant(playerTwo, 2);
			
			return result;
		}
		return null;
	}
	
	@Override
	public final boolean containsParticipant(int playerId)
	{
		return _playerOne.objectId == playerId || _playerTwo.objectId == playerId;
	}
	
	/**
	 * Sends olympiad info to the new spectator.
	 */
	@Override
	public final void sendOlympiadInfo(L2Character player)
	{
		player.sendPacket(new ExOlympiadUserInfo(_playerOne.player));
		_playerOne.player.updateEffectIcons();
		
		player.sendPacket(new ExOlympiadUserInfo(_playerTwo.player));
		_playerTwo.player.updateEffectIcons();
	}
	
	/**
	 * Broadcasts olympiad info to participants and spectators on battle start.
	 */
	@Override
	public final void broadcastOlympiadInfo(L2OlympiadStadiumZone stadium)
	{
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerOne.player));
		_playerOne.player.updateEffectIcons();
		
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerTwo.player));
		_playerTwo.player.updateEffectIcons();
	}
	
	@Override
	protected final void broadcastPacket(PacketServer packet)
	{
		_playerOne.updatePlayer();
		if (_playerOne.player != null)
		{
			_playerOne.player.sendPacket(packet);
		}
		
		_playerTwo.updatePlayer();
		if (_playerTwo.player != null)
		{
			_playerTwo.player.sendPacket(packet);
		}
	}
	
	@Override
	protected final boolean portPlayersToArena(List<Location> spawns)
	{
		boolean result = true;
		try
		{
			result &= portPlayerToArena(_playerOne, spawns.get(0), _stadiumID);
			result &= portPlayerToArena(_playerTwo, spawns.get(1), _stadiumID);
		}
		catch (Exception e)
		{
			return false;
		}
		return result;
	}
	
	@Override
	protected final void removals()
	{
		if (_aborted)
		{
			return;
		}
		
		removals(_playerOne.player, true);
		removals(_playerTwo.player, true);
	}
	
	@Override
	protected final void buffPlayers()
	{
		if (_aborted)
		{
			return;
		}
		
		buffPlayer(_playerOne.player);
		buffPlayer(_playerTwo.player);
	}
	
	@Override
	protected final void healPlayers()
	{
		if (_aborted)
		{
			return;
		}
		
		healPlayer(_playerOne.player);
		healPlayer(_playerTwo.player);
	}
	
	@Override
	protected final boolean makeCompetitionStart()
	{
		if (!super.makeCompetitionStart())
		{
			return false;
		}
		
		if (_playerOne.player == null || _playerTwo.player == null)
		{
			return false;
		}
		
		_playerOne.player.setIsOlympiadStart(true);
		_playerTwo.player.setIsOlympiadStart(true);
		return true;
	}
	
	@Override
	protected final void cleanEffects()
	{
		if (_playerOne.player != null && !_playerOne.defaulted && !_playerOne.disconnected && _playerOne.player.getOlympiadGameId() == _stadiumID)
		{
			cleanEffects(_playerOne.player);
		}
		
		if (_playerTwo.player != null && !_playerTwo.defaulted && !_playerTwo.disconnected && _playerTwo.player.getOlympiadGameId() == _stadiumID)
		{
			cleanEffects(_playerTwo.player);
		}
	}
	
	@Override
	protected final void portPlayersBack()
	{
		if (_playerOne.player != null && !_playerOne.defaulted && !_playerOne.disconnected)
		{
			portPlayerBack(_playerOne.player);
		}
		if (_playerTwo.player != null && !_playerTwo.defaulted && !_playerTwo.disconnected)
		{
			portPlayerBack(_playerTwo.player);
		}
	}
	
	@Override
	protected final void playersStatusBack()
	{
		if (_playerOne.player != null && !_playerOne.defaulted && !_playerOne.disconnected && _playerOne.player.getOlympiadGameId() == _stadiumID)
		{
			playerStatusBack(_playerOne.player);
		}
		
		if (_playerTwo.player != null && !_playerTwo.defaulted && !_playerTwo.disconnected && _playerTwo.player.getOlympiadGameId() == _stadiumID)
		{
			playerStatusBack(_playerTwo.player);
		}
	}
	
	@Override
	protected final void clearPlayers()
	{
		_playerOne.player = null;
		_playerOne = null;
		_playerTwo.player = null;
		_playerTwo = null;
	}
	
	@Override
	protected final void handleDisconnect(L2PcInstance player)
	{
		if (player.getObjectId() == _playerOne.objectId)
		{
			_playerOne.disconnected = true;
		}
		else if (player.getObjectId() == _playerTwo.objectId)
		{
			_playerTwo.disconnected = true;
		}
	}
	
	@Override
	protected final boolean checkBattleStatus()
	{
		if (_aborted)
		{
			return false;
		}
		
		if (_playerOne.player == null || _playerOne.disconnected)
		{
			return false;
		}
		
		if (_playerTwo.player == null || _playerTwo.disconnected)
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	protected final boolean haveWinner()
	{
		if (!checkBattleStatus())
		{
			return true;
		}
		
		boolean playerOneLost = true;
		try
		{
			if (_playerOne.player.getOlympiadGameId() == _stadiumID)
			{
				playerOneLost = _playerOne.player.isDead();
			}
		}
		catch (Exception e)
		{
			playerOneLost = true;
		}
		
		boolean playerTwoLost = true;
		try
		{
			if (_playerTwo.player.getOlympiadGameId() == _stadiumID)
			{
				playerTwoLost = _playerTwo.player.isDead();
			}
		}
		catch (Exception e)
		{
			playerTwoLost = true;
		}
		
		return playerOneLost || playerTwoLost;
	}
	
	@Override
	protected void validateWinner(L2OlympiadStadiumZone stadium)
	{
		if (_aborted)
		{
			return;
		}
		
		final boolean _pOneCrash = (_playerOne.player == null || _playerOne.disconnected);
		final boolean _pTwoCrash = (_playerTwo.player == null || _playerTwo.disconnected);
		
		final int playerOnePoints = _playerOne.stats.getInteger(POINTS);
		final int playerTwoPoints = _playerTwo.stats.getInteger(POINTS);
		
		int pointDiff = Math.min(playerOnePoints, playerTwoPoints) / getDivider();
		if (pointDiff <= 0)
		{
			pointDiff = 1;
		}
		else if (pointDiff > Config.ALT_OLY_MAX_POINTS)
		{
			pointDiff = Config.ALT_OLY_MAX_POINTS;
		}
		
		int points;
		
		// Check for if a player defaulted before battle started
		if (_playerOne.defaulted || _playerTwo.defaulted)
		{
			try
			{
				if (_playerOne.defaulted)
				{
					try
					{
						points = Math.min(playerOnePoints / 3, Config.ALT_OLY_MAX_POINTS);
						removePointsFromParticipant(_playerOne, points);
					}
					catch (Exception e)
					{
						LOG.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				
				if (_playerTwo.defaulted)
				{
					try
					{
						points = Math.min(playerTwoPoints / 3, Config.ALT_OLY_MAX_POINTS);
						removePointsFromParticipant(_playerTwo, points);
					}
					catch (Exception e)
					{
						LOG.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				return;
			}
			catch (Exception e)
			{
				LOG.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}
		
		// Create results for players if a player crashed
		if (_pOneCrash || _pTwoCrash)
		{
			try
			{
				if (_pTwoCrash && !_pOneCrash)
				{
					stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME).addString(_playerOne.name));
					
					_playerOne.updateStat(COMP_WON, 1);
					addPointsToParticipant(_playerOne, pointDiff);
					
					_playerTwo.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerTwo, pointDiff);
					
					rewardParticipant(_playerOne.player, getReward());
					
					_playerOne.getPlayer().getAchievement().increase(AchType.OLYMPIAD_FIGHT_WON);
					_playerTwo.getPlayer().getAchievement().increase(AchType.OLYMPIAD_FIGHT_LOST);
					
					_playerOne.player = null;
				}
				else if (_pOneCrash && !_pTwoCrash)
				{
					stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME).addString(_playerTwo.name));
					
					_playerTwo.updateStat(COMP_WON, 1);
					addPointsToParticipant(_playerTwo, pointDiff);
					
					_playerOne.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerOne, pointDiff);
					
					rewardParticipant(_playerTwo.player, getReward());
					
					_playerTwo.getPlayer().getAchievement().increase(AchType.OLYMPIAD_FIGHT_WON);
					_playerOne.getPlayer().getAchievement().increase(AchType.OLYMPIAD_FIGHT_LOST);
				}
				else if (_pOneCrash && _pTwoCrash)
				{
					stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE));
					
					_playerOne.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerOne, pointDiff);
					
					_playerTwo.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerTwo, pointDiff);
				}
				
				_playerOne.updateStat(COMP_DONE, 1);
				_playerTwo.updateStat(COMP_DONE, 1);
				
				return;
			}
			catch (Exception e)
			{
				LOG.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}
		
		try
		{
			// Calculate Fight time
			long _fightTime = (System.currentTimeMillis() - _startTime);
			
			double playerOneHp = 0;
			if (_playerOne.player != null && !_playerOne.player.isDead())
			{
				playerOneHp = _playerOne.player.getCurrentHp() + _playerOne.player.getCurrentCp();
				if (playerOneHp < 0.5)
				{
					playerOneHp = 0;
				}
			}
			
			double playerTwoHp = 0;
			if (_playerTwo.player != null && !_playerTwo.player.isDead())
			{
				playerTwoHp = _playerTwo.player.getCurrentHp() + _playerTwo.player.getCurrentCp();
				if (playerTwoHp < 0.5)
				{
					playerTwoHp = 0;
				}
			}
			
			// if players crashed, search if they've relogged
			_playerOne.updatePlayer();
			_playerTwo.updatePlayer();
			
			if ((_playerOne.player == null || _playerOne.player.isOnline() == 0) && (_playerTwo.player == null || _playerTwo.player.isOnline() == 0))
			{
				_playerOne.updateStat(COMP_DRAWN, 1);
				_playerTwo.updateStat(COMP_DRAWN, 1);
				stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE));
			}
			else if (_playerTwo.player == null || _playerTwo.player.isOnline() == 0 || (playerTwoHp == 0 && playerOneHp != 0) || (_damageP1 > _damageP2 && playerTwoHp != 0 && playerOneHp != 0))
			{
				stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME).addString(_playerOne.name));
				
				_playerOne.updateStat(COMP_WON, 1);
				_playerTwo.updateStat(COMP_LOST, 1);
				
				addPointsToParticipant(_playerOne, pointDiff);
				removePointsFromParticipant(_playerTwo, pointDiff);
				
				// Save Fight Result
				saveResults(_playerOne, _playerTwo, 1, _startTime, _fightTime, getType());
				rewardParticipant(_playerOne.player, getReward());
				
				_playerOne.getPlayer().getAchievement().increase(AchType.OLYMPIAD_FIGHT_WON);
				_playerTwo.getPlayer().getAchievement().increase(AchType.OLYMPIAD_FIGHT_LOST);
			}
			else if (_playerOne.player == null || _playerOne.player.isOnline() == 0 || (playerOneHp == 0 && playerTwoHp != 0) || (_damageP2 > _damageP1 && playerOneHp != 0 && playerTwoHp != 0))
			{
				stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME).addString(_playerTwo.name));
				
				_playerTwo.updateStat(COMP_WON, 1);
				_playerOne.updateStat(COMP_LOST, 1);
				
				addPointsToParticipant(_playerTwo, pointDiff);
				removePointsFromParticipant(_playerOne, pointDiff);
				
				// Save Fight Result
				saveResults(_playerOne, _playerTwo, 2, _startTime, _fightTime, getType());
				rewardParticipant(_playerTwo.player, getReward());
				
				_playerTwo.getPlayer().getAchievement().increase(AchType.OLYMPIAD_FIGHT_WON);
				_playerOne.getPlayer().getAchievement().increase(AchType.OLYMPIAD_FIGHT_LOST);
			}
			else
			{
				// Save Fight Result
				saveResults(_playerOne, _playerTwo, 0, _startTime, _fightTime, getType());
				
				stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE));
				
				removePointsFromParticipant(_playerOne, Math.min(playerOnePoints / getDivider(), Config.ALT_OLY_MAX_POINTS));
				removePointsFromParticipant(_playerTwo, Math.min(playerTwoPoints / getDivider(), Config.ALT_OLY_MAX_POINTS));
			}
			
			_playerOne.updateStat(COMP_DONE, 1);
			_playerTwo.updateStat(COMP_DONE, 1);
		}
		catch (Exception e)
		{
			LOG.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
		}
	}
	
	@Override
	protected final void addDamage(L2PcInstance player, int damage)
	{
		if (_playerOne.player == null || _playerTwo.player == null)
		{
			return;
		}
		if (player == _playerOne.player)
		{
			_damageP1 += damage;
		}
		else if (player == _playerTwo.player)
		{
			_damageP2 += damage;
		}
	}
	
	@Override
	public final String[] getPlayerNames()
	{
		return new String[]
		{
			_playerOne.name,
			_playerTwo.name
		};
	}
	
	@Override
	public boolean checkDefaulted()
	{
		SystemMessage reason;
		_playerOne.updatePlayer();
		_playerTwo.updatePlayer();
		
		reason = checkDefaulted(_playerOne.player);
		if (reason != null)
		{
			_playerOne.defaulted = true;
			if (_playerTwo.player != null)
			{
				_playerTwo.player.sendPacket(reason);
			}
		}
		
		reason = checkDefaulted(_playerTwo.player);
		if (reason != null)
		{
			_playerTwo.defaulted = true;
			if (_playerOne.player != null)
			{
				_playerOne.player.sendPacket(reason);
			}
		}
		
		return _playerOne.defaulted || _playerTwo.defaulted;
	}
	
	@Override
	public final void resetDamage()
	{
		_damageP1 = 0;
		_damageP2 = 0;
	}
	
	protected static final void saveResults(Participant one, Participant two, int _winner, long _startTime, long _fightTime, CompetitionType type)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, winner, start, time, classed) values(?,?,?,?,?,?,?,?)");
			statement.setInt(1, one.objectId);
			statement.setInt(2, two.objectId);
			statement.setInt(3, one.baseClass);
			statement.setInt(4, two.baseClass);
			statement.setInt(5, _winner);
			statement.setLong(6, _startTime);
			statement.setLong(7, _fightTime);
			statement.setInt(8, (type == CompetitionType.CLASSED ? 1 : 0));
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if (LOG.isLoggable(Level.SEVERE))
			{
				LOG.log(Level.SEVERE, "SQL exception while saving olympiad fight.", e);
			}
		}
	}
}