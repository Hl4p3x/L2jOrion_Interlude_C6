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
package l2jorion.game.model.entity;

import java.util.Calendar;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.enums.AchType;
import l2jorion.game.managers.DuelManager;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.olympiad.OlympiadManager;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ExDuelEnd;
import l2jorion.game.network.serverpackets.ExDuelReady;
import l2jorion.game.network.serverpackets.ExDuelStart;
import l2jorion.game.network.serverpackets.ExDuelUpdateUserInfo;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.network.serverpackets.PlaySound;
import l2jorion.game.network.serverpackets.SocialAction;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Duel
{
	protected static final Logger LOG = LoggerFactory.getLogger(Duel.class);
	
	public static final int DUELSTATE_NODUEL = 0;
	public static final int DUELSTATE_DUELLING = 1;
	public static final int DUELSTATE_DEAD = 2;
	public static final int DUELSTATE_WINNER = 3;
	public static final int DUELSTATE_INTERRUPTED = 4;
	
	private final int _duelId;
	private L2PcInstance _playerA;
	private L2PcInstance _playerB;
	protected boolean _partyDuel;
	private final Calendar _duelEndTime;
	private int _surrenderRequest = 0;
	private int _countdown = 4;
	private boolean _finished = false;
	private FastMap<Integer, PlayerCondition> _playerConditions;
	
	public static enum DuelResultEnum
	{
		
		/** The Continue. */
		Continue,
		
		/** The Team1 win. */
		Team1Win,
		
		/** The Team2 win. */
		Team2Win,
		
		/** The Team1 surrender. */
		Team1Surrender,
		
		/** The Team2 surrender. */
		Team2Surrender,
		
		/** The Canceled. */
		Canceled,
		
		/** The Timeout. */
		Timeout
	}
	
	// =========================================================
	// Constructor
	/**
	 * Instantiates a new duel.
	 * @param playerA the player a
	 * @param playerB the player b
	 * @param partyDuel the party duel
	 * @param duelId the duel id
	 */
	public Duel(final L2PcInstance playerA, final L2PcInstance playerB, final int partyDuel, final int duelId)
	{
		_duelId = duelId;
		_playerA = playerA;
		_playerB = playerB;
		_partyDuel = partyDuel == 1 ? true : false;
		
		_duelEndTime = Calendar.getInstance();
		
		if (_partyDuel)
		{
			_duelEndTime.add(Calendar.SECOND, 300);
		}
		else
		{
			_duelEndTime.add(Calendar.SECOND, 120);
		}
		
		_playerConditions = new FastMap<>();
		
		setFinished(false);
		
		if (_partyDuel)
		{
			// increase countdown so that start task can teleport players
			_countdown++;
			// inform players that they will be portet shortly
			SystemMessage sm = new SystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
			sm = null;
		}
		// Schedule duel start
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartDuelTask(this), 3000);
	}
	
	// ===============================================================
	// Nested Class
	
	/**
	 * The Class PlayerCondition.
	 */
	public class PlayerCondition
	{
		
		/** The _player. */
		private L2PcInstance _player;
		
		/** The _hp. */
		private double _hp;
		
		/** The _mp. */
		private double _mp;
		
		/** The _cp. */
		private double _cp;
		
		/** The _pa duel. */
		private boolean _paDuel;
		
		/** The _z. */
		private int _x, _y, _z;
		
		/** The _debuffs. */
		private FastList<L2Effect> _debuffs;
		
		/**
		 * Instantiates a new player condition.
		 * @param player the player
		 * @param partyDuel the party duel
		 */
		public PlayerCondition(final L2PcInstance player, final boolean partyDuel)
		{
			if (player == null)
			{
				return;
			}
			
			_player = player;
			_hp = _player.getCurrentHp();
			_mp = _player.getCurrentMp();
			_cp = _player.getCurrentCp();
			_paDuel = partyDuel;
			
			if (_paDuel)
			{
				_x = _player.getX();
				_y = _player.getY();
				_z = _player.getZ();
			}
		}
		
		/**
		 * Restore condition.
		 */
		public synchronized void restoreCondition()
		{
			if (_player == null)
			{
				return;
			}
			
			_player.setCurrentHp(_hp);
			_player.setCurrentMp(_mp);
			_player.setCurrentCp(_cp);
			
			if (_paDuel)
			{
				teleportBack();
			}
			
			if (_debuffs != null) // Debuff removal
			{
				for (final L2Effect temp : _debuffs)
				{
					if (temp != null)
					{
						temp.exit(false);
					}
				}
			}
		}
		
		/**
		 * Register debuff.
		 * @param debuff the debuff
		 */
		public void registerDebuff(final L2Effect debuff)
		{
			if (_debuffs == null)
			{
				_debuffs = new FastList<>();
			}
			
			_debuffs.add(debuff);
		}
		
		/**
		 * Remove debuff.
		 * @param debuff the debuff
		 */
		public void removeDebuff(final L2Effect debuff)
		{
			if (_debuffs == null)
			{
				return;
			}
			
			_debuffs.remove(debuff);
		}
		
		/**
		 * Teleport back.
		 */
		public void teleportBack()
		{
			if (_paDuel)
			{
				_player.teleToLocation(_x, _y, _z);
			}
		}
		
		/**
		 * Gets the player.
		 * @return the player
		 */
		public L2PcInstance getPlayer()
		{
			return _player;
		}
	}
	
	// ===============================================================
	// Schedule task
	/**
	 * The Class ScheduleDuelTask.
	 */
	public class ScheduleDuelTask implements Runnable
	{
		
		/** The _duel. */
		private final Duel _duel;
		
		/**
		 * Instantiates a new schedule duel task.
		 * @param duel the duel
		 */
		public ScheduleDuelTask(final Duel duel)
		{
			_duel = duel;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				DuelResultEnum status = _duel.checkEndDuelCondition();
				
				if (status == DuelResultEnum.Canceled)
				{
					// do not schedule duel end if it was interrupted
					setFinished(true);
					_duel.endDuel(status);
				}
				else if (status != DuelResultEnum.Continue)
				{
					setFinished(true);
					playKneelAnimation();
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndDuelTask(_duel, status), 5000);
				}
				else
				{
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
				}
				
				status = null;
			}
			catch (final Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
	
	public class ScheduleStartDuelTask implements Runnable
	{
		private final Duel _duel;
		
		public ScheduleStartDuelTask(final Duel duel)
		{
			_duel = duel;
		}
		
		@Override
		public void run()
		{
			try
			{
				// start/continue countdown
				final int count = _duel.countdown();
				
				if (!_partyDuel || count == 4)
				{
					// Save player Conditions
					savePlayerConditions();
				}
				
				if (count == 4)
				{
					// players need to be teleportet first
					// TODO: stadia manager needs a function to return an unused stadium for duels
					
					// currently if oly in competition period
					// and defined location is into a stadium
					// just use Gludin Arena as location
					// if (OlympiadManager.getInstance().inCompPeriod())
					// {
					// _duel.teleportPlayers(-87912, 142221, -3645);
					// }
					// else
					// {
					_duel.teleportPlayers(Config.DUEL_SPAWN_X, Config.DUEL_SPAWN_Y, Config.DUEL_SPAWN_Z);
					// }
					
					// give players 20 seconds to complete teleport and get ready (its ought to be 30 on offical..)
					ThreadPoolManager.getInstance().scheduleGeneral(this, 20000);
				}
				else if (count > 0) // duel not started yet - continue countdown
				{
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
				}
				else
				{
					_duel.startDuel();
				}
			}
			catch (final Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
	
	/**
	 * The Class ScheduleEndDuelTask.
	 */
	public static class ScheduleEndDuelTask implements Runnable
	{
		
		/** The _duel. */
		private final Duel _duel;
		
		/** The _result. */
		private final DuelResultEnum _result;
		
		/**
		 * Instantiates a new schedule end duel task.
		 * @param duel the duel
		 * @param result the result
		 */
		public ScheduleEndDuelTask(final Duel duel, final DuelResultEnum result)
		{
			_duel = duel;
			_result = result;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				_duel.endDuel(_result);
			}
			catch (final Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
	
	// ========================================================
	// Method - Private
	
	/**
	 * Stops all players from attacking. Used for duel timeout / interrupt.
	 */
	private void stopFighting()
	{
		ActionFailed af = ActionFailed.STATIC_PACKET;
		if (_partyDuel)
		{
			for (final L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
			}
			
			for (final L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
			}
		}
		else
		{
			_playerA.abortCast();
			_playerB.abortCast();
			_playerA.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			_playerA.setTarget(null);
			_playerB.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			_playerB.setTarget(null);
			_playerA.sendPacket(af);
			_playerB.sendPacket(af);
		}
		
		af = null;
	}
	
	// ========================================================
	// Method - Public
	
	/**
	 * Check if a player engaged in pvp combat (only for 1on1 duels).
	 * @param sendMessage the send message
	 * @return returns true if a duelist is engaged in Pvp combat
	 */
	public boolean isDuelistInPvp(final boolean sendMessage)
	{
		if (_partyDuel)
		{
			// Party duels take place in arenas - should be no other players there
			return false;
		}
		else if (_playerA.getPvpFlag() != 0 || _playerB.getPvpFlag() != 0)
		{
			if (sendMessage)
			{
				final String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
				_playerA.sendMessage(engagedInPvP);
				_playerB.sendMessage(engagedInPvP);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Starts the duel.
	 */
	public void startDuel()
	{
		// Save player Conditions
		// savePlayerConditions();
		
		if (_playerA == null || _playerB == null || _playerA.isInDuel() || _playerB.isInDuel() || OlympiadManager.getInstance().isRegisteredInComp(_playerA) || OlympiadManager.getInstance().isRegisteredInComp(_playerB) || OlympiadManager.getInstance().isRegistered(_playerA)
			|| OlympiadManager.getInstance().isRegistered(_playerB))
		{
			// clean up
			_playerConditions.clear();
			_playerConditions = null;
			DuelManager.getInstance().removeDuel(this);
			return;
		}
		
		if (_partyDuel)
		{
			// set isInDuel() state
			// cancel all active trades, just in case? xD
			for (final L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(1);
				// temp.broadcastStatusUpdate();
				temp.broadcastUserInfo();
				broadcastToTeam2(new ExDuelUpdateUserInfo(temp));
			}
			for (final L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(2);
				// temp.broadcastStatusUpdate();
				temp.broadcastUserInfo();
				broadcastToTeam1(new ExDuelUpdateUserInfo(temp));
			}
			
			// Send duel Start packets
			ExDuelReady ready = new ExDuelReady(1);
			ExDuelStart start = new ExDuelStart(1);
			
			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
			
			ready = null;
			start = null;
		}
		else
		{
			// set isInDuel() state
			_playerA.setIsInDuel(_duelId);
			_playerA.setTeam(1);
			_playerB.setIsInDuel(_duelId);
			_playerB.setTeam(2);
			
			// Send duel Start packets
			ExDuelReady ready = new ExDuelReady(0);
			ExDuelStart start = new ExDuelStart(0);
			
			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
			
			broadcastToTeam1(new ExDuelUpdateUserInfo(_playerB));
			broadcastToTeam2(new ExDuelUpdateUserInfo(_playerA));
			// _playerA.broadcastStatusUpdate();
			// _playerB.broadcastStatusUpdate();
			_playerA.broadcastUserInfo();
			_playerB.broadcastUserInfo();
			
			ready = null;
			start = null;
		}
		
		// play sound
		PlaySound ps = new PlaySound(1, "B04_S01", 0, 0, 0, 0, 0);
		broadcastToTeam1(ps);
		broadcastToTeam2(ps);
		
		ps = null;
		
		// start duelling task
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleDuelTask(this), 1000);
	}
	
	/**
	 * Save the current player condition: hp, mp, cp, location.
	 */
	public void savePlayerConditions()
	{
		
		if (_partyDuel)
		{
			for (final L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				_playerConditions.put(temp.getObjectId(), new PlayerCondition(temp, _partyDuel));
			}
			
			for (final L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				_playerConditions.put(temp.getObjectId(), new PlayerCondition(temp, _partyDuel));
			}
		}
		else
		{
			_playerConditions.put(_playerA.getObjectId(), new PlayerCondition(_playerA, _partyDuel));
			_playerConditions.put(_playerB.getObjectId(), new PlayerCondition(_playerB, _partyDuel));
		}
		
	}
	
	/**
	 * Restore player conditions.
	 * @param abnormalDuelEnd the abnormal duel end
	 */
	private synchronized void restorePlayerConditions(final boolean abnormalDuelEnd)
	{
		
		// update isInDuel() state for all players
		if (_partyDuel)
		{
			for (final L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(0);
				temp.broadcastUserInfo();
			}
			
			for (final L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(0);
				temp.broadcastUserInfo();
			}
		}
		else
		{
			_playerA.setIsInDuel(0);
			_playerA.setTeam(0);
			_playerA.broadcastUserInfo();
			_playerB.setIsInDuel(0);
			_playerB.setTeam(0);
			_playerB.broadcastUserInfo();
		}
		
		// if it is an abnormal DuelEnd do not restore hp, mp, cp
		if (abnormalDuelEnd)
		{
			return;
		}
		
		// restore player conditions
		// for (FastList.Node<PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;)
		for (final Integer playerObjId : _playerConditions.keySet())
		{
			final PlayerCondition e = _playerConditions.get(playerObjId);
			e.restoreCondition();
		}
	}
	
	/**
	 * Get the duel id.
	 * @return id
	 */
	public int getId()
	{
		return _duelId;
	}
	
	/**
	 * Returns the remaining time.
	 * @return remaining time
	 */
	public int getRemainingTime()
	{
		return (int) (_duelEndTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
	}
	
	/**
	 * Get the player that requestet the duel.
	 * @return duel requester
	 */
	public L2PcInstance getPlayerA()
	{
		return _playerA;
	}
	
	/**
	 * Get the player that was challenged.
	 * @return challenged player
	 */
	public L2PcInstance getPlayerB()
	{
		return _playerB;
	}
	
	/**
	 * Returns whether this is a party duel or not.
	 * @return is party duel
	 */
	public boolean isPartyDuel()
	{
		return _partyDuel;
	}
	
	/**
	 * Sets the finished.
	 * @param mode the new finished
	 */
	public void setFinished(final boolean mode)
	{
		_finished = mode;
	}
	
	/**
	 * Gets the finished.
	 * @return the finished
	 */
	public boolean getFinished()
	{
		return _finished;
	}
	
	/**
	 * teleport all players to the given coordinates.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 */
	public void teleportPlayers(final int x, final int y, final int z)
	{
		// TODO: adjust the values if needed... or implement something better (especially using more then 1 arena)
		if (!_partyDuel)
		{
			return;
		}
		
		int offset = 0;
		
		for (final L2PcInstance temp : _playerA.getParty().getPartyMembers())
		{
			temp.teleToLocation(x + offset - 180, y - 150, z);
			offset += 40;
		}
		
		offset = 0;
		
		for (final L2PcInstance temp : _playerB.getParty().getPartyMembers())
		{
			temp.teleToLocation(x + offset - 180, y + 150, z);
			offset += 40;
		}
	}
	
	/**
	 * Broadcast a packet to the challanger team.
	 * @param packet the packet
	 */
	public void broadcastToTeam1(final L2GameServerPacket packet)
	{
		if (_playerA == null)
		{
			return;
		}
		
		if (_partyDuel && _playerA.getParty() != null)
		{
			for (final L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerA.sendPacket(packet);
		}
	}
	
	/**
	 * Broadcast a packet to the challenged team.
	 * @param packet the packet
	 */
	public void broadcastToTeam2(final L2GameServerPacket packet)
	{
		if (_playerB == null)
		{
			return;
		}
		
		if (_partyDuel && _playerB.getParty() != null)
		{
			for (final L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerB.sendPacket(packet);
		}
	}
	
	/**
	 * Get the duel winner.
	 * @return winner
	 */
	public L2PcInstance getWinner()
	{
		if (!getFinished() || _playerA == null || _playerB == null)
		{
			return null;
		}
		
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		
		if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		
		return null;
	}
	
	/**
	 * Get the duel looser.
	 * @return looser
	 */
	public L2PcInstance getLooser()
	{
		if (!getFinished() || _playerA == null || _playerB == null)
		{
			return null;
		}
		
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		
		return null;
	}
	
	/**
	 * Playback the bow animation for all loosers.
	 */
	public void playKneelAnimation()
	{
		L2PcInstance looser = getLooser();
		
		if (looser == null)
		{
			return;
		}
		
		if (_partyDuel && looser.getParty() != null)
		{
			for (final L2PcInstance temp : looser.getParty().getPartyMembers())
			{
				temp.broadcastPacket(new SocialAction(temp.getObjectId(), 7));
			}
		}
		else
		{
			looser.broadcastPacket(new SocialAction(looser.getObjectId(), 7));
		}
		
		looser = null;
	}
	
	/**
	 * Do the countdown and send message to players if necessary.
	 * @return current count
	 */
	public int countdown()
	{
		_countdown--;
		
		if (_countdown > 3)
		{
			return _countdown;
		}
		
		// Broadcast countdown to duelists
		SystemMessage sm = null;
		if (_countdown > 0)
		{
			sm = new SystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS);
			sm.addNumber(_countdown);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN);
		}
		
		broadcastToTeam1(sm);
		broadcastToTeam2(sm);
		sm = null;
		
		return _countdown;
	}
	
	/**
	 * The duel has reached a state in which it can no longer continue.
	 * @param result the result
	 */
	public void endDuel(final DuelResultEnum result)
	{
		if (_playerA == null || _playerB == null)
		{
			// clean up
			_playerConditions.clear();
			_playerConditions = null;
			DuelManager.getInstance().removeDuel(this);
			return;
		}
		
		// inform players of the result
		SystemMessage sm = null;
		switch (result)
		{
			case Team2Surrender:
			case Team1Win:
				restorePlayerConditions(false);
				
				// send SystemMessage
				if (_partyDuel)
				{
					sm = new SystemMessage(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL);
				}
				
				sm.addString(_playerA.getName());
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Team1Surrender:
			case Team2Win:
				restorePlayerConditions(false);
				// send SystemMessage
				if (_partyDuel)
				{
					sm = new SystemMessage(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL);
				}
				sm.addString(_playerB.getName());
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Canceled:
				stopFighting();
				
				// dont restore hp, mp, cp
				restorePlayerConditions(true);
				
				// TODO: is there no other message for a canceled duel?
				// send SystemMessage
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			case Timeout:
				stopFighting();
				// hp,mp,cp seem to be restored in a timeout too...
				restorePlayerConditions(false);
				// send SystemMessage
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
		}
		
		// Send end duel packet
		ExDuelEnd duelEnd = null;
		if (_partyDuel)
		{
			duelEnd = new ExDuelEnd(1);
		}
		else
		{
			duelEnd = new ExDuelEnd(0);
		}
		
		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);
		
		// clean up
		_playerConditions.clear();
		_playerConditions = null;
		DuelManager.getInstance().removeDuel(this);
		
		sm = null;
		duelEnd = null;
	}
	
	/**
	 * Did a situation occur in which the duel has to be ended?.
	 * @return DuelResultEnum duel status
	 */
	public DuelResultEnum checkEndDuelCondition()
	{
		// one of the players might leave during duel
		if (_playerA == null || _playerB == null)
		{
			return DuelResultEnum.Canceled;
		}
		
		// got a duel surrender request?
		if (_surrenderRequest != 0)
		{
			if (_surrenderRequest == 1)
			{
				return DuelResultEnum.Team1Surrender;
			}
			return DuelResultEnum.Team2Surrender;
		}
		// duel timed out
		else if (getRemainingTime() <= 0)
		{
			return DuelResultEnum.Timeout;
		}
		else if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResultEnum.Team1Win;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResultEnum.Team2Win;
		}
		
		// More end duel conditions for 1on1 duels
		else if (!_partyDuel)
		{
			// Duel was interrupted e.g.: player was attacked by mobs / other players
			if (_playerA.getDuelState() == DUELSTATE_INTERRUPTED || _playerB.getDuelState() == DUELSTATE_INTERRUPTED)
			{
				return DuelResultEnum.Canceled;
			}
			
			// Are the players too far apart?
			if (!_playerA.isInsideRadius(_playerB, 1600, false, false))
			{
				return DuelResultEnum.Canceled;
			}
			
			// Did one of the players engage in PvP combat?
			if (isDuelistInPvp(true))
			{
				return DuelResultEnum.Canceled;
			}
			
			// is one of the players in a Siege, Peace or PvP zone?
			if (_playerA.isInsideZone(ZoneId.ZONE_PEACE) || _playerB.isInsideZone(ZoneId.ZONE_PEACE) || _playerA.isInsideZone(ZoneId.ZONE_SIEGE) || _playerB.isInsideZone(ZoneId.ZONE_SIEGE) || _playerA.isInsideZone(ZoneId.ZONE_PVP) || _playerB.isInsideZone(ZoneId.ZONE_PVP))
			{
				return DuelResultEnum.Canceled;
			}
		}
		
		return DuelResultEnum.Continue;
	}
	
	/**
	 * Register a surrender request.
	 * @param player the player
	 */
	public void doSurrender(final L2PcInstance player)
	{
		// already recived a surrender request
		if (_surrenderRequest != 0)
		{
			return;
		}
		
		// stop the fight
		stopFighting();
		
		// TODO: Can every party member cancel a party duel? or only the party leaders?
		if (_partyDuel)
		{
			if (_playerA.getParty().getPartyMembers().contains(player))
			{
				_surrenderRequest = 1;
				
				for (final L2PcInstance temp : _playerA.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				
				for (final L2PcInstance temp : _playerB.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
					temp.getAchievement().increase(AchType.DUAL_WON);
				}
			}
			else if (_playerB.getParty().getPartyMembers().contains(player))
			{
				_surrenderRequest = 2;
				
				for (final L2PcInstance temp : _playerB.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				
				for (final L2PcInstance temp : _playerA.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
					temp.getAchievement().increase(AchType.DUAL_WON);
				}
				
			}
		}
		else
		{
			if (player == _playerA)
			{
				_surrenderRequest = 1;
				_playerA.setDuelState(DUELSTATE_DEAD);
				_playerB.setDuelState(DUELSTATE_WINNER);
				_playerB.getAchievement().increase(AchType.DUAL_WON);
			}
			else if (player == _playerB)
			{
				_surrenderRequest = 2;
				_playerB.setDuelState(DUELSTATE_DEAD);
				_playerA.setDuelState(DUELSTATE_WINNER);
				_playerA.getAchievement().increase(AchType.DUAL_WON);
			}
		}
	}
	
	/**
	 * This function is called whenever a player was defeated in a duel.
	 * @param player the player
	 */
	public void onPlayerDefeat(final L2PcInstance player)
	{
		// Set player as defeated
		player.setDuelState(DUELSTATE_DEAD);
		
		if (_partyDuel)
		{
			boolean teamdefeated = true;
			
			for (final L2PcInstance temp : player.getParty().getPartyMembers())
			{
				if (temp.getDuelState() == DUELSTATE_DUELLING)
				{
					teamdefeated = false;
					break;
				}
			}
			
			if (teamdefeated)
			{
				L2PcInstance winner = _playerA;
				
				if (_playerA.getParty().getPartyMembers().contains(player))
				{
					winner = _playerB;
				}
				
				for (final L2PcInstance temp : winner.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
					temp.getAchievement().increase(AchType.DUAL_WON);
				}
			}
		}
		else
		{
			if (player != _playerA && player != _playerB)
			{
				LOG.warn("Error in onPlayerDefeat(): player is not part of this 1vs1 duel");
			}
			
			if (_playerA == player)
			{
				_playerB.setDuelState(DUELSTATE_WINNER);
				_playerB.getAchievement().increase(AchType.DUAL_WON);
			}
			else
			{
				_playerA.setDuelState(DUELSTATE_WINNER);
				_playerA.getAchievement().increase(AchType.DUAL_WON);
			}
		}
	}
	
	/**
	 * This function is called whenever a player leaves a party.
	 * @param player the player
	 */
	public void onRemoveFromParty(final L2PcInstance player)
	{
		// if it isnt a party duel ignore this
		if (!_partyDuel)
		{
			return;
		}
		
		// this player is leaving his party during party duel
		// if hes either playerA or playerB cancel the duel and port the players back
		if (player == _playerA || player == _playerB)
		{
			
			final PlayerCondition e = _playerConditions.remove(player.getObjectId());
			
			if (e != null)
			{
				e.teleportBack();
				e.getPlayer().setIsInDuel(0);
			}
			
			if (player == _playerA)
			{
				_playerA = null;
			}
			else
			{
				_playerB = null;
			}
		}
		else
		// teleport the player back & delete his PlayerCondition record
		{
			final PlayerCondition e = _playerConditions.remove(player.getObjectId());
			
			if (e != null)
			{
				e.teleportBack();
			}
			player.setIsInDuel(0);
		}
	}
	
	/**
	 * On buff.
	 * @param player the player
	 * @param debuff the debuff
	 */
	public void onBuff(final L2PcInstance player, final L2Effect debuff)
	{
		final PlayerCondition e = _playerConditions.get(player.getObjectId());
		if (e != null)
		{
			e.registerDebuff(debuff);
		}
	}
	
	/**
	 * On buff stop.
	 * @param player the player
	 * @param debuff the debuff
	 */
	public void onBuffStop(final L2PcInstance player, final L2Effect debuff)
	{
		final PlayerCondition e = _playerConditions.get(player.getObjectId());
		if (e != null)
		{
			e.removeDebuff(debuff);
		}
		
	}
}
