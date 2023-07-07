package l2jorion.bots.ai;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import l2jorion.Config;
import l2jorion.bots.FakePlayer;
import l2jorion.bots.model.FarmLocation;
import l2jorion.bots.xml.botFarm;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.csv.MapRegionTable.TeleportWhereType;
import l2jorion.game.geo.GeoData;
import l2jorion.game.managers.TownManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Effect.EffectType;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillTargetType;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2DoorInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.CharMoveToLocation;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.network.serverpackets.StopMove;
import l2jorion.game.network.serverpackets.StopRotation;
import l2jorion.game.taskmanager.RandomZoneTaskManager;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public abstract class FakePlayerAI
{
	protected final Logger LOG = LoggerFactory.getLogger(FakePlayerAI.class);
	
	protected final FakePlayer _fakePlayer;
	protected volatile boolean _clientMoving;
	protected volatile boolean _clientAutoAttacking;
	private long _moveToPawnTimeout;
	protected int _clientMovingToPawnOffset;
	protected boolean _isBusyThinking = false;
	protected int iterationsOnDeath = 0;
	private final int toVillageIterationsOnDeath = Rnd.get(1, 20);
	protected int _stuck = 0;
	protected FarmLocation _currentFarmLoc;
	protected int iterationsInTown = 0;
	private final int toFarmZoneIterationsInTown = Rnd.get(10, 25);
	protected int iterationsNearGk = 0;
	private final int standNearGk = Rnd.get(10, 25);
	
	public FakePlayerAI(FakePlayer character)
	{
		_fakePlayer = character;
		setup();
		applyDefaultBuffs();
	}
	
	public void setup()
	{
		_fakePlayer.setIsRunning(true);
	}
	
	protected void applyDefaultBuffs()
	{
		for (int[] buff : getBuffs())
		{
			try
			{
				Map<Integer, L2Effect> activeEffects = Arrays.stream(_fakePlayer.getAllEffects()).filter(x -> x.getEffectType() == EffectType.BUFF).collect(Collectors.toMap(x -> x.getSkill().getId(), x -> x));
				
				if (!activeEffects.containsKey(buff[0]))
				{
					SkillTable.getInstance().getInfo(buff[0], buff[1]).getEffects(_fakePlayer, _fakePlayer);
				}
				else
				{
					if ((activeEffects.get(buff[0]).getPeriod() - activeEffects.get(buff[0]).getTime()) <= 20)
					{
						SkillTable.getInstance().getInfo(buff[0], buff[1]).getEffects(_fakePlayer, _fakePlayer);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	protected void handleDeath()
	{
		if (_fakePlayer.isDead())
		{
			if (iterationsOnDeath >= toVillageIterationsOnDeath)
			{
				if (_fakePlayer.getBotMode() == 1)
				{
					_fakePlayer.setActionId(3); // reset
				}
				
				toVillageOnDeath();
			}
			iterationsOnDeath++;
			return;
		}
		
		iterationsOnDeath = 0;
	}
	
	public void setBusyThinking(boolean thinking)
	{
		_isBusyThinking = thinking;
	}
	
	public boolean isBusyThinking()
	{
		return _isBusyThinking;
	}
	
	protected void tryTargetRandomCreatureByTypeInRadius(Class<? extends L2Character> creatureClass, int radius)
	{
		if (_fakePlayer.getTarget() == null)
		{
			if (Config.L2UNLIMITED_CUSTOM)
			{
				if (_fakePlayer.getBotMode() == 4 || _fakePlayer.getBotMode() == 5)
				{
					if ((TownManager.getInstance().getTown(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ()) == null) && 1 > Rnd.get(100))
					{
						doUnstuck(_fakePlayer);
						return;
					}
				}
			}
			
			List<L2Character> targets = _fakePlayer.getKnownTypeInRadius(creatureClass, radius).stream().filter(x -> !x.isDead() && !x.charIsGM() && (x.isMonster() && !(Config.BOTS_EXCLUDE_TARGETS_LIST.contains(((L2MonsterInstance) x).getNpcId())) || x.isPlayer())).collect(Collectors.toList());
			
			if (targets.isEmpty())
			{
				if (_fakePlayer.getBotMode() != 3) // pvp zone mode
				{
					if (_fakePlayer.getTargetRange() > _fakePlayer.getMaxTargetRange())
					{
						_fakePlayer.setTargetRange(300);
					}
					_fakePlayer.setTargetRange(_fakePlayer.getTargetRange() + 300);
				}
			}
			else
			{
				L2Character target = targets.get(Rnd.get(0, targets.size() - 1));
				_fakePlayer.setTarget(target);
			}
		}
		else
		{
			if (/* _fakePlayer.getBotMode() == 4 && */_fakePlayer.getTarget() instanceof L2PcInstance) // no attack if pvp flag is gone
			{
				if (((L2PcInstance) _fakePlayer.getTarget()).getPvpFlag() == 0)
				{
					_fakePlayer.setTarget(null);
				}
			}
			
			if (_fakePlayer.getBotMode() != 3)
			{
				if (_fakePlayer.getTarget() != null && !_fakePlayer.isMoving() && !_fakePlayer.isTeleporting()
					&& !GeoData.getInstance().canMove(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ(), ((L2Character) _fakePlayer.getTarget()).getX(), ((L2Character) _fakePlayer.getTarget()).getY(), ((L2Character) _fakePlayer.getTarget()).getZ(), _fakePlayer.getInstanceId()))
				{
					_fakePlayer.setTarget(null);
					return;
				}
			}
			
			if (_fakePlayer.getTarget() != null && ((L2Character) _fakePlayer.getTarget()).isDead())
			{
				_fakePlayer.setTarget(null);
				if (_fakePlayer.getBotMode() != 3)
				{
					_fakePlayer.setTargetRange(300);
				}
			}
		}
	}
	
	public void castSpell(L2Skill skill)
	{
		if (!_fakePlayer.isCastingNow())
		{
			if (skill.getTargetType() == SkillTargetType.TARGET_GROUND)
			{
				if (maybeMoveToPosition((_fakePlayer).getCurrentSkillWorldPosition(), skill.getCastRange()))
				{
					return;
				}
			}
			else
			{
				if (checkTargetLost(_fakePlayer.getTarget()))
				{
					if (skill.isOffensive() && _fakePlayer.getTarget() != null)
					{
						_fakePlayer.setTarget(null);
					}
					return;
				}
				
				if (_fakePlayer.getTarget() != null)
				{
					if (maybeMoveToPawn(_fakePlayer.getTarget(), skill.getCastRange()))
					{
						return;
					}
				}
				
				if (_fakePlayer.isSkillDisabled(skill))
				{
					return;
				}
			}
			
			if (skill.getHitTime() > 50 && !skill.isSimultaneousCast())
			{
				clientStopMoving(null);
			}
			
			_fakePlayer.doCast(skill);
		}
		else
		{
			_fakePlayer.forceAutoAttack((L2Character) _fakePlayer.getTarget());
		}
	}
	
	protected void castSelfSpell(L2Skill skill)
	{
		if (!_fakePlayer.isCastingNow() && !_fakePlayer.isSkillDisabled(skill))
		{
			
			if (skill.getHitTime() > 50 && !skill.isSimultaneousCast())
			{
				clientStopMoving(null);
			}
			
			_fakePlayer.doCast(skill);
		}
	}
	
	protected void actionsInTown()
	{
		if (Config.L2UNLIMITED_CUSTOM)
		{
			if (_fakePlayer.getBotMode() == 4 || _fakePlayer.getBotMode() == 5)
			{
				_currentFarmLoc = (FarmLocation) botFarm.getInstance().getFarmNode(_fakePlayer.getZoneId()).toArray()[Rnd.get(0, botFarm.getInstance().getFarmNode(_fakePlayer.getZoneId()).size() - 1)];
				
				if ((TownManager.getInstance().getTown(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ()) != null))
				{
					if ((TownManager.getInstance().getTown(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ()).getTownId() == 9))
					{
						if (_fakePlayer.getActionId() == 0)
						{
							if (iterationsNearGk >= standNearGk)
							{
								int x, y, z;
								Location loc = null;
								
								int rndChoice = Rnd.get(1, 2);
								switch (rndChoice)
								{
									case 1:
										loc = new Location(83352, 147980, -3405);
										break;
									case 2:
										loc = new Location(81528, 148563, -3467);
										break;
								}
								
								if (loc != null)
								{
									x = loc.getX();
									y = loc.getY();
									z = loc.getZ();
									
									x += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
									y += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
									
									_fakePlayer.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(x, y, z, 0));
									_fakePlayer.setActionId(1);
									iterationsNearGk = 0;
								}
							}
							iterationsNearGk++;
							return;
						}
					}
					else if ((TownManager.getInstance().getTown(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ()).getTownId() == 5))
					{
						if (_fakePlayer.getActionId() == 0)
						{
							if (iterationsNearGk >= standNearGk)
							{
								int x, y, z;
								Location loc = null;
								
								int rndChoice = Rnd.get(1, 2);
								switch (rndChoice)
								{
									case 1:
										loc = new Location(-82947, 150947, -3128);
										break;
									case 2:
										loc = new Location(-83111, 152694, -3177);
										break;
								}
								
								if (loc != null)
								{
									x = loc.getX();
									y = loc.getY();
									z = loc.getZ();
									
									x += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
									y += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
									
									_fakePlayer.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(x, y, z, 0));
									_fakePlayer.setActionId(1);
									iterationsNearGk = 0;
								}
							}
							iterationsNearGk++;
							return;
						}
					}
					
					if (!_fakePlayer.isMoving())
					{
						if (_fakePlayer.getActionId() == 1)
						{
							if (iterationsInTown >= toFarmZoneIterationsInTown)
							{
								_fakePlayer.setBotMode(4);
								_fakePlayer.setDistance(_currentFarmLoc);
								_fakePlayer.setActionId(0);
								iterationsInTown = 0;
								_fakePlayer.teleToLocation(_currentFarmLoc.getX(), _currentFarmLoc.getY(), _currentFarmLoc.getZ(), true);
							}
							
							iterationsInTown++;
							return;
						}
						
						if (iterationsInTown >= toFarmZoneIterationsInTown)
						{
							_fakePlayer.setBotMode(4);
							_fakePlayer.setDistance(_currentFarmLoc);
							_fakePlayer.teleToLocation(_currentFarmLoc.getX(), _currentFarmLoc.getY(), _currentFarmLoc.getZ(), true);
						}
						iterationsInTown++;
						return;
					}
				}
			}
			
			return;
		}
		
		if (_fakePlayer.getBotMode() == 4 || _fakePlayer.getBotMode() == 5)
		{
			_currentFarmLoc = (FarmLocation) botFarm.getInstance().getFarmNode(_fakePlayer.getZoneId()).toArray()[Rnd.get(0, botFarm.getInstance().getFarmNode(_fakePlayer.getZoneId()).size() - 1)];
			
			if ((TownManager.getInstance().getTown(_fakePlayer.getX(), _fakePlayer.getY(), _fakePlayer.getZ()) != null))
			{
				if (iterationsInTown >= toFarmZoneIterationsInTown)
				{
					_fakePlayer.setBotMode(4);
					_fakePlayer.setDistance(_currentFarmLoc);
					_fakePlayer.teleToLocation(_currentFarmLoc.getX(), _currentFarmLoc.getY(), _currentFarmLoc.getZ(), true);
				}
				iterationsInTown++;
				return;
			}
			
			iterationsInTown = 0;
		}
	}
	
	protected void toVillageOnDeath()
	{
		Location location = null;
		boolean pvpZone = false;
		
		if (_fakePlayer.isInsideZone(ZoneId.ZONE_RANDOM))
		{
			pvpZone = true;
			location = RandomZoneTaskManager.getInstance().getCurrentZone().getLoc();
		}
		else
		{
			if (!Config.CUSTOM_RESPAWN)
			{
				location = MapRegionTable.getInstance().getTeleToLocation(_fakePlayer, TeleportWhereType.Town);
			}
			else
			{
				location = new Location(Config.CSPAWN_X, Config.CSPAWN_Y, Config.CSPAWN_Z);
			}
		}
		
		if (_fakePlayer.isDead())
		{
			_fakePlayer.doRevive();
		}
		
		_fakePlayer.teleToLocation(location.getX(), location.getY(), location.getZ(), location.getHeading(), true, false, pvpZone);
	}
	
	protected void clientStopMoving(Location pos)
	{
		if (_fakePlayer.isMoving())
		{
			_fakePlayer.stopMove(pos);
		}
		
		_clientMovingToPawnOffset = 0;
		
		if (_clientMoving || pos != null)
		{
			_clientMoving = false;
			
			_fakePlayer.broadcastPacket(new StopMove(_fakePlayer));
			
			if (pos != null)
			{
				StopRotation sr = new StopRotation(_fakePlayer, pos.getHeading(), 0);
				_fakePlayer.sendPacket(sr);
				_fakePlayer.broadcastPacket(sr);
			}
		}
	}
	
	protected boolean checkTargetLost(L2Object target)
	{
		if (target instanceof L2PcInstance)
		{
			final L2PcInstance victim = (L2PcInstance) target;
			if (victim.isFakeDeath())
			{
				victim.setIsFakeDeath(false);
				return false;
			}
		}
		
		if (target == null)
		{
			_fakePlayer.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			return true;
		}
		return false;
	}
	
	protected boolean maybeMoveToPosition(Location worldPosition, int offset)
	{
		if (worldPosition == null)
		{
			return false;
		}
		
		if (offset < 0)
		{
			return false;
		}
		
		if (!_fakePlayer.isInsideRadius(worldPosition.getX(), worldPosition.getY(), (int) (offset + _fakePlayer.getCollisionRadius()), false))
		{
			if (_fakePlayer.isMovementDisabled())
			{
				return true;
			}
			
			int x = _fakePlayer.getX();
			int y = _fakePlayer.getY();
			
			double dx = worldPosition.getX() - x;
			double dy = worldPosition.getY() - y;
			
			double dist = Math.sqrt(dx * dx + dy * dy);
			
			double sin = dy / dist;
			double cos = dx / dist;
			
			dist -= offset - 5;
			
			x += (int) (dist * cos);
			y += (int) (dist * sin);
			
			moveTo(x, y, worldPosition.getZ());
			return true;
		}
		
		return false;
	}
	
	protected void moveToPawn(L2Object pawn, int offset)
	{
		if (!_fakePlayer.isMovementDisabled())
		{
			if (offset < 10)
			{
				offset = 10;
			}
			
			if (_clientMoving && _fakePlayer.getTarget() == pawn)
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (GameTimeController.getInstance().getGameTicks() < _moveToPawnTimeout)
					{
						return;
					}
				}
				else if (_fakePlayer.isOnGeodataPath())
				{
					if (GameTimeController.getInstance().getGameTicks() < (_moveToPawnTimeout + 10))
					{
						return;
					}
				}
			}
			
			_clientMoving = true;
			_clientMovingToPawnOffset = offset;
			_moveToPawnTimeout = GameTimeController.getInstance().getGameTicks();
			_moveToPawnTimeout += 1000 / GameTimeController.MILLIS_IN_TICK;
			
			if (pawn == null)
			{
				clientActionFailed();
				return;
			}
			
			if (!GeoData.getInstance().canSeeTarget(_fakePlayer, pawn))
			{
				offset = 0;
			}
			
			_fakePlayer.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			
			if (!_fakePlayer.isMoving())
			{
				clientActionFailed();
				return;
			}
			
			if (pawn instanceof L2Character)
			{
				if (_fakePlayer.isOnGeodataPath())
				{
					_fakePlayer.broadcastPacket(new CharMoveToLocation(_fakePlayer));
					_clientMovingToPawnOffset = 0;
				}
				else
				{
					_fakePlayer.broadcastPacket(new MoveToPawn(_fakePlayer, (L2Character) pawn, offset));
				}
			}
			else
			{
				_fakePlayer.broadcastPacket(new CharMoveToLocation(_fakePlayer));
			}
		}
		else
		{
			clientActionFailed();
		}
	}
	
	public void moveTo(int x, int y, int z)
	{
		if (!_fakePlayer.isMovementDisabled())
		{
			_clientMoving = true;
			_clientMovingToPawnOffset = 0;
			_fakePlayer.moveToLocation(x, y, z, 0);
			_fakePlayer.broadcastPacket(new CharMoveToLocation(_fakePlayer));
		}
	}
	
	protected boolean maybeMoveToPawn(L2Object target, int offset)
	{
		if (target == null || offset < 0)
		{
			return false;
		}
		
		offset += _fakePlayer.getCollisionRadius();
		if (target instanceof L2Character)
		{
			offset += ((L2Character) target).getTemplate().getCollisionRadius();
		}
		
		if (!_fakePlayer.isInsideRadius(target, offset, false, false))
		{
			if (_fakePlayer.isMovementDisabled())
			{
				if (_fakePlayer.getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK)
				{
					_fakePlayer.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				}
				return true;
			}
			
			if (target instanceof L2Character && !(target instanceof L2DoorInstance))
			{
				if (((L2Character) target).isMoving())
				{
					if (((L2Character) target).isMoving())
					{
						offset -= 30;
					}
					
					if (offset < 5)
					{
						offset = 5;
					}
				}
			}
			moveToPawn(target, offset);
			return true;
		}
		
		if (!GeoData.getInstance().canSeeTarget(_fakePlayer, _fakePlayer.getTarget()))
		{
			moveToPawn(target, 30); // 50
			return true;
		}
		
		return false;
	}
	
	protected void clientActionFailed()
	{
		if (_fakePlayer != null)
		{
			_fakePlayer.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	protected boolean doUnstuck(L2PcInstance activeChar)
	{
		int unstuckTimer = activeChar.getAccessLevel().isGm() ? 1000 : Config.UNSTUCK_INTERVAL * 1000;
		
		if (activeChar.isFestivalParticipant())
		{
			return false;
		}
		
		if (activeChar._inEventTvT && TvT.is_started())
		{
			return false;
		}
		
		if (activeChar._inEventCTF && CTF.is_started())
		{
			return false;
		}
		
		if (activeChar._inEventDM && DM.is_started())
		{
			return false;
		}
		
		if (activeChar._inEventVIP && VIP._started)
		{
			return false;
		}
		
		if (activeChar.isInJail())
		{
			return false;
		}
		
		if (activeChar.isInFunEvent())
		{
			return false;
		}
		
		if (activeChar.inObserverMode())
		{
			return false;
		}
		
		if (activeChar.isSitting())
		{
			return false;
		}
		
		if (activeChar.isCastingNow() || activeChar.isOutOfControl() || activeChar.isMovementDisabled() || activeChar.isMuted() || activeChar.isAlikeDead() || activeChar.isInOlympiadMode())
		{
			return false;
		}
		
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		activeChar.setTarget(activeChar);
		activeChar.broadcastPacket(new MagicSkillUser(activeChar, 1050, 1, unstuckTimer, 0));
		activeChar.sendPacket(new SetupGauge(0, unstuckTimer));
		activeChar.setTarget(null);
		
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(new EscapeFinalizer(activeChar), unstuckTimer));
		activeChar.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
		
		return true;
	}
	
	static class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;
		
		EscapeFinalizer(L2PcInstance activeChar)
		{
			_activeChar = activeChar;
		}
		
		@Override
		public void run()
		{
			if (_activeChar.isDead())
			{
				return;
			}
			
			_activeChar.setIsIn7sDungeon(false);
			
			try
			{
				if (_activeChar.getKarma() > 0 && Config.ALT_KARMA_TELEPORT_TO_FLORAN)
				{
					_activeChar.teleToLocation(17836, 170178, -3507, true); // Floran
					return;
				}
				
				_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			catch (Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public abstract void thinkAndAct();
	
	protected abstract int[][] getBuffs();
}
