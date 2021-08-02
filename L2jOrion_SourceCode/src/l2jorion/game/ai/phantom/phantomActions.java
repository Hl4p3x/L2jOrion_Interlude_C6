package l2jorion.game.ai.phantom;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_CAST;

import java.util.ArrayList;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.geo.GeoData;
import l2jorion.game.managers.TownManager;
import l2jorion.game.model.BlockList;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2TeleporterInstance;
import l2jorion.game.model.base.Race;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.util.random.Rnd;

public class phantomActions
{
	public void startAction(L2PcInstance _phantom)
	{
		// TODO Do revive
		if (_phantom.isDead() && Rnd.nextInt(30) == 0)
		{
			_phantom.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			_phantom.doRevive();
			
			if (Config.AUTOBUFFS_ON_CREATE)
			{
				ArrayList<L2Skill> skills_to_buff = new ArrayList<>();
				if (_phantom.isMageClass())
				{
					for (int skillId : PowerPackConfig.MAGE_SKILL_LIST.keySet())
					{
						L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.MAGE_SKILL_LIST.get(skillId));
						if (skill != null)
						{
							skills_to_buff.add(skill);
						}
					}
				}
				else
				{
					for (int skillId : PowerPackConfig.FIGHTER_SKILL_LIST.keySet())
					{
						L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.FIGHTER_SKILL_LIST.get(skillId));
						if (skill != null)
						{
							skills_to_buff.add(skill);
						}
					}
				}
				for (L2Skill sk : skills_to_buff)
				{
					sk.getEffects(_phantom, _phantom, false, false, false);
				}
			}
			
			_phantom.getStatus().setCurrentHp(_phantom.getMaxHp());
			_phantom.getStatus().setCurrentMp(_phantom.getMaxMp());
			_phantom.getStatus().setCurrentCp(_phantom.getMaxCp());
		}
		
		if (actionDisabled(_phantom))
		{
			return;
		}
		
		if (_phantom.isInWater())
		{
			GoToTown(_phantom);
			return;
		}
		
		if (_phantom.isPhantomRndWalk())
		{
			if (Rnd.nextInt(5) == 0)
			{
				_phantom.rndWalk();
			}
			return;
		}
		
		// TODO Chats
		if (Config.ALLOW_PHANTOM_CHAT)
		{
			if (Rnd.get(1000) <= Config.PHANTOM_CHAT_CHANSE)
			{
				switch (Rnd.get(1, 3))
				{
					case 1:
						CreatureSay cs = new CreatureSay(0, Say2.SHOUT, _phantom.getName(), phantomPlayers.getRandomChatPhrase());
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (!BlockList.isBlocked(player, _phantom))
							{
								player.sendPacket(cs);
							}
						}
						phantomPlayers._PhantomsRandomPhrases.remove(phantomPlayers._PhantomLastPhrase);
						break;
					case 2:
						CreatureSay cs2 = new CreatureSay(0, Say2.TRADE, _phantom.getName(), phantomPlayers.getRandomChatPhrase());
						for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
						{
							if (!BlockList.isBlocked(player, _phantom))
							{
								player.sendPacket(cs2);
							}
						}
						phantomPlayers._PhantomsRandomPhrases.remove(phantomPlayers._PhantomLastPhrase);
						break;
					case 3:
						CreatureSay cs3 = new CreatureSay(0, Say2.ALL, _phantom.getName(), phantomPlayers.getRandomChatPhrase());
						for (L2PcInstance player : _phantom.getKnownList().getKnownPlayers().values())
						{
							if (player != null && _phantom.isInsideRadius(player, 1250, false, true))
							{
								if (!BlockList.isBlocked(player, _phantom))
								{
									player.sendPacket(cs3);
								}
							}
						}
						phantomPlayers._PhantomsRandomPhrases.remove(phantomPlayers._PhantomLastPhrase);
						break;
				}
			}
		}
		
		// TODO Killing mobs
		if (_phantom.getPvpFlag() == 0 && (TownManager.getInstance().getTown(_phantom.getX(), _phantom.getY(), _phantom.getZ()) == null))
		{
			// Mages
			if (_phantom.getClassId().isMage())
			{
				_phantom.getPhantomTargetList().clear();
				
				for (L2Object npc : L2World.getInstance().getObjectsAround(_phantom, 6000))
				{
					if (!_phantom.getPhantomTargetList().contains(npc) && npc instanceof L2MonsterInstance)
					{
						_phantom.getPhantomTargetList().add((L2MonsterInstance) npc);
					}
				}
				
				L2Object[] target = _phantom.getPhantomTargetList().toArray(new L2MonsterInstance[_phantom.getPhantomTargetList().size()]);
				L2Object trg = null;
				
				if (target != null)
				{
					if (target.length > 0)
					{
						trg = target[Rnd.get(_phantom.getPhantomTargetList().size())];
					}
				}
				
				if (trg == null && !_phantom.isMoving() && Rnd.nextInt(30) == 0)
				{
					_phantom.rndWalk();
				}
				
				if (trg != null && _phantom.getTarget() == null)
				{
					_phantom.setTarget(trg);
				}
				
				if (_phantom.getTarget() != null && ((L2Character) _phantom.getTarget()).isDead())
				{
					_phantom.setTarget(trg);
				}
				
				if (_phantom.getTarget() != null)
				{
					if (GeoData.getInstance().canSeeTarget(_phantom, _phantom.getTarget()))
					{
						if (_phantom.getTarget() instanceof L2MonsterInstance && _phantom.getLevel() > ((L2MonsterInstance) _phantom.getTarget()).getLevel() + 8)
						{
							GoToTown(_phantom);
							return;
						}
						
						if (_phantom.getClassId().getRace() == Race.orc && _phantom.getLevel() < 7)
						{
							if (_phantom.isInsideRadius(_phantom.getTarget(), _phantom.getPhysicalAttackRange(), true, false))
							{
								_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _phantom.getTarget());
							}
							else
							{
								_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
							}
						}
						
						for (L2Skill sk : _phantom.getAtkSkills())
						{
							if (sk != null && sk.isOffensive() && !_phantom.isSkillDisabled(sk) && (sk.getWeaponDependancy(_phantom)))
							{
								if (!_phantom.isInsideZone(ZoneId.ZONE_PEACE))
								{
									if (_phantom.isInsideRadius(_phantom.getTarget(), sk.getCastRange(), true, false))
									{
										_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, sk, _phantom.getTarget());
									}
									else
									{
										_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
									}
								}
								else
								{
									if (Rnd.nextInt(30) == 0)
									{
										if (_phantom.isInsideRadius(_phantom.getTarget(), sk.getCastRange(), true, false))
										{
											_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, sk, _phantom.getTarget());
										}
										else
										{
											_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
										}
									}
								}
							}
						}
					}
					else
					{
						if (!_phantom.isMoving())
						{
							_phantom.rndWalk();
						}
					}
				}
			}
			
			// Fighters
			else
			{
				_phantom.getPhantomTargetList().clear();
				
				for (L2Object npc : L2World.getInstance().getObjectsAround(_phantom, 6000))
				{
					if (!_phantom.getPhantomTargetList().contains(npc) && npc instanceof L2MonsterInstance)
					{
						_phantom.getPhantomTargetList().add((L2MonsterInstance) npc);
					}
				}
				
				L2Object[] target = _phantom.getPhantomTargetList().toArray(new L2MonsterInstance[_phantom.getPhantomTargetList().size()]);
				L2Object trg = null;
				
				if (target != null)
				{
					if (target.length > 0)
					{
						trg = target[Rnd.get(_phantom.getPhantomTargetList().size())];
					}
				}
				
				if (trg == null && !_phantom.isMoving() && Rnd.nextInt(30) == 0)
				{
					_phantom.rndWalk();
				}
				
				if (trg != null && _phantom.getTarget() == null)
				{
					_phantom.setTarget(trg);
				}
				
				if (_phantom.getTarget() != null && ((L2Character) _phantom.getTarget()).isDead())
				{
					_phantom.setTarget(trg);
				}
				
				if (_phantom.getTarget() != null)
				{
					if (GeoData.getInstance().canSeeTarget(_phantom, _phantom.getTarget()))
					{
						if (_phantom.getTarget() instanceof L2MonsterInstance && _phantom.getLevel() > ((L2MonsterInstance) _phantom.getTarget()).getLevel() + 8)
						{
							GoToTown(_phantom);
							return;
						}
						
						if (Rnd.nextInt(30) == 0)
						{
							for (L2Skill sk : _phantom.getAtkSkills())
							{
								if (sk != null && sk.isOffensive() && !_phantom.isSkillDisabled(sk) && (sk.getWeaponDependancy(_phantom)))
								{
									if (_phantom.isInsideRadius(_phantom.getTarget(), sk.getCastRange(), true, false))
									{
										_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, sk, _phantom.getTarget());
									}
									else
									{
										_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
									}
								}
							}
						}
						else
						{
							if (!_phantom.isInsideZone(ZoneId.ZONE_PEACE))
							{
								if (_phantom.isInsideRadius(_phantom.getTarget(), _phantom.getPhysicalAttackRange(), true, false))
								{
									_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _phantom.getTarget());
								}
								else
								{
									_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
								}
							}
							else
							{
								if (Rnd.nextInt(30) == 0)
								{
									if (_phantom.isInsideRadius(_phantom.getTarget(), _phantom.getPhysicalAttackRange(), true, false))
									{
										_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _phantom.getTarget());
									}
									else
									{
										_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
									}
								}
							}
						}
					}
					else
					{
						if (!_phantom.isMoving())
						{
							_phantom.rndWalk();
						}
					}
				}
			}
		}
		
		// TODO Killing players
		if ((TownManager.getInstance().getTown(_phantom.getX(), _phantom.getY(), _phantom.getZ()) == null))
		{
			// Mages
			if (_phantom.getClassId().isMage())
			{
				_phantom.getPhantomTargetList().clear();
				
				for (L2Object player : L2World.getInstance().getPlayersAround(_phantom, 6000))
				{
					if (!_phantom.getPhantomTargetList().contains(player) && player instanceof L2PcInstance && ((L2PcInstance) player).getPvpFlag() != 0)
					{
						_phantom.getPhantomTargetList().add((L2PcInstance) player);
					}
				}
				
				L2Object[] target = _phantom.getPhantomTargetList().toArray(new L2PcInstance[_phantom.getPhantomTargetList().size()]);
				L2Object trg = null;
				
				if (target != null)
				{
					if (target.length > 0)
					{
						trg = target[Rnd.get(_phantom.getPhantomTargetList().size())];
					}
				}
				
				if (trg != null && _phantom.getTarget() == null)
				{
					_phantom.setTarget(trg);
				}
				
				if (_phantom.getTarget() != null && ((L2Character) _phantom.getTarget()).isDead())
				{
					_phantom.setTarget(trg);
				}
				
				if (_phantom.getTarget() != null)
				{
					if (GeoData.getInstance().canSeeTarget(_phantom, _phantom.getTarget()))
					{
						if (_phantom.getClassId().getRace() == Race.orc && _phantom.getLevel() < 7)
						{
							if (_phantom.isInsideRadius(_phantom.getTarget(), _phantom.getPhysicalAttackRange(), true, false))
							{
								_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _phantom.getTarget());
							}
							else
							{
								_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
							}
						}
						
						for (L2Skill sk : _phantom.getAtkSkills())
						{
							if (sk != null && sk.isOffensive() && !_phantom.isSkillDisabled(sk) && (sk.getWeaponDependancy(_phantom)))
							{
								if (_phantom.isInsideRadius(_phantom.getTarget(), sk.getCastRange(), true, false))
								{
									_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, sk, _phantom.getTarget());
								}
								else
								{
									_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
								}
							}
						}
					}
				}
			}
			
			// Fighters
			else
			{
				_phantom.getPhantomTargetList().clear();
				
				for (L2Object player : L2World.getInstance().getPlayersAround(_phantom, 6000))
				{
					if (!_phantom.getPhantomTargetList().contains(player) && player instanceof L2PcInstance && ((L2PcInstance) player).getPvpFlag() != 0)
					{
						_phantom.getPhantomTargetList().add((L2PcInstance) player);
					}
				}
				
				L2Object[] target = _phantom.getPhantomTargetList().toArray(new L2PcInstance[_phantom.getPhantomTargetList().size()]);
				L2Object trg = null;
				
				if (target != null)
				{
					if (target.length > 0)
					{
						trg = target[Rnd.get(_phantom.getPhantomTargetList().size())];
					}
				}
				
				if (trg != null && _phantom.getTarget() == null)
				{
					_phantom.setTarget(trg);
				}
				
				if (_phantom.getTarget() != null && ((L2Character) _phantom.getTarget()).isDead())
				{
					_phantom.setTarget(trg);
				}
				
				if (_phantom.getTarget() != null)
				{
					if (GeoData.getInstance().canSeeTarget(_phantom, _phantom.getTarget()))
					{
						if (Rnd.nextInt(5) == 0)
						{
							for (L2Skill sk : _phantom.getAtkSkills())
							{
								if (sk != null && sk.isOffensive() && !_phantom.isSkillDisabled(sk) && (sk.getWeaponDependancy(_phantom)))
								{
									if (_phantom.isInsideRadius(_phantom.getTarget(), sk.getCastRange(), true, false))
									{
										_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, sk, _phantom.getTarget());
									}
									else
									{
										_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
									}
								}
							}
						}
						else
						{
							if (_phantom.isInsideRadius(_phantom.getTarget(), _phantom.getPhysicalAttackRange(), true, false))
							{
								_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _phantom.getTarget());
							}
							else
							{
								_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(_phantom.getTarget().getX(), _phantom.getTarget().getY(), _phantom.getTarget().getZ(), 0));
							}
						}
					}
				}
			}
		}
		
		// TODO go to GK at Town
		if ((TownManager.getInstance().getTown(_phantom.getX(), _phantom.getY(), _phantom.getZ()) != null))
		{
			for (L2Object gk : L2World.getInstance().getVisibleObjects(_phantom, 3000))
			{
				if (gk != null && gk instanceof L2TeleporterInstance && _phantom.isInsideRadius(gk, 3000, false, true))
				{
					if (_phantom.getTarget() == null)
					{
						_phantom.setTarget(gk);
					}
					
					if (_phantom.getMoveToPawn())
					{
						_phantom.getAI().moveToPawn(gk, 140);
						_phantom.setMoveToPawn(false);
					}
					
					if (_phantom.isInsideRadius(gk, 150, false, true))
					{
						_phantom.broadcastPacket(new MoveToPawn(_phantom, (L2TeleporterInstance) gk, L2NpcInstance.INTERACTION_DISTANCE));
						
						if (Rnd.nextInt(50) == 0)
						{
							DoTeleportToZone(_phantom);
						}
					}
				}
			}
			
			if (_phantom.getTarget() == null && Rnd.nextInt(5) == 0)
			{
				_phantom.rndWalk();
			}
		}
	}
	
	public boolean actionDisabled(L2PcInstance phantom)
	{
		return phantom.isTeleporting() || phantom.isAttackingNow() || phantom.isDead() || phantom.isCastingNow() /* || phantom.isMoving() */ || (phantom.getAI().getIntention() == AI_INTENTION_ATTACK) || (phantom.getAI().getIntention() == AI_INTENTION_CAST);
	}
	
	public void DoTeleportToZone(L2PcInstance _phantom)
	{
		int random = Rnd.get(1, 2);
		
		if (_phantom.getClassId().getRace() == Race.human)
		{
			
			switch (random)
			{
				case 1:
					_phantom.teleToLocation(48714, 248462, -6165, true);
					break;
				case 2:
					_phantom.teleToLocation(-99572, 237595, -3578, true);
					break;
			}
			
		}
		
		if (_phantom.getClassId().getRace() == Race.elf)
		{
			_phantom.teleToLocation(26291, 74966, -4096, true);
		}
		
		if (_phantom.getClassId().getRace() == Race.darkelf)
		{
			_phantom.teleToLocation(-30704, 49675, -3573, true);
		}
		
		if (_phantom.getClassId().getRace() == Race.orc)
		{
			_phantom.teleToLocation(7571, -138897, -929, true);
		}
		if (_phantom.getClassId().getRace() == Race.dwarf)
		{
			_phantom.teleToLocation(171961, -173373, -3445, true);
		}
	}
	
	public void GoToTown(L2PcInstance _phantom)
	{
		int unstuckTimer = Config.UNSTUCK_INTERVAL * 1000;
		
		_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		_phantom.disableAllSkills();
		
		final MagicSkillUser msu = new MagicSkillUser(_phantom, 1050, 1, unstuckTimer, 0);
		_phantom.broadcastPacket(msu);
		
		// End SoE Animation section
		_phantom.setTarget(null);
		
		EscapeFinalizer ef = new EscapeFinalizer(_phantom);
		// continue execution later
		_phantom.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));
		_phantom.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
	}
	
	private class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _phantom;
		
		EscapeFinalizer(L2PcInstance phantom)
		{
			_phantom = phantom;
		}
		
		@Override
		public void run()
		{
			if (_phantom.isDead())
			{
				return;
			}
			
			_phantom.setIsIn7sDungeon(false);
			_phantom.enableAllSkills();
			_phantom.abortCast();
			_phantom.stopMove(null);
			
			try
			{
				_phantom.teleToLocation(MapRegionTable.TeleportWhereType.Town);
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
	
	private static phantomActions _instance = null;
	
	public static phantomActions getInstance()
	{
		if (_instance == null)
		{
			_instance = new phantomActions();
		}
		
		return _instance;
	}
}