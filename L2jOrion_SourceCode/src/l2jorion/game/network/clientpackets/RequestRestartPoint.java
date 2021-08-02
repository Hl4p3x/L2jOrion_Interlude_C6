package l2jorion.game.network.clientpackets;

import l2jorion.Config;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2SiegeClan;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.ClanHall;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.entity.siege.Fort;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.RestartResponse;
import l2jorion.game.network.serverpackets.Revive;
import l2jorion.game.taskmanager.RandomZoneTaskManager;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.IllegalPlayerAction;
import l2jorion.game.util.Util;

public final class RequestRestartPoint extends L2GameClientPacket
{
	protected int _requestedPointType;
	protected boolean _continuation;
	
	@Override
	protected void readImpl()
	{
		_requestedPointType = readD();
	}
	
	class DeathTask implements Runnable
	{
		L2PcInstance activeChar;
		
		DeathTask(final L2PcInstance _activeChar)
		{
			activeChar = _activeChar;
		}
		
		@Override
		public void run()
		{
			if ((activeChar._inEventTvT && TvT.is_started()) || (activeChar._inEventDM && DM.is_started()) || (activeChar._inEventCTF && CTF.is_started()))
			{
				activeChar.sendMessage("You can't restart in Event!");
				return;
			}
			
			if (activeChar.isInArenaEvent() || activeChar.isArenaProtection())
			{
				activeChar.sendMessage("You cannot restart while in Tournament Event!");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(RestartResponse.valueOf(false));
				return;
			}
			
			try
			{
				Location loc = null;
				Castle castle = null;
				Fort fort = null;
				
				if (activeChar.isInJail())
				{
					_requestedPointType = 27;
				}
				else if (activeChar.isFestivalParticipant())
				{
					_requestedPointType = 4;
				}
				
				if (activeChar.isPhoenixBlessed())
				{
					activeChar.stopPhoenixBlessing(null);
				}
				
				switch (_requestedPointType)
				{
					case 1: // to clanhall
						
						if (activeChar.getClan() != null)
						{
							
							if (activeChar.getClan().getHasHideout() == 0)
							{
								// cheater
								activeChar.sendMessage("You may not use this respawn point!");
								Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
								return;
							}
							loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.ClanHall);
							
							if (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) != null && ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
							{
								activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
							}
							
							break;
						}
						
						loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
						break;
					case 2: // to castle
						Boolean isInDefense = false;
						castle = CastleManager.getInstance().getCastle(activeChar);
						fort = FortManager.getInstance().getFort(activeChar);
						MapRegionTable.TeleportWhereType teleportWhere = MapRegionTable.TeleportWhereType.Town;
						
						if (castle != null && castle.getSiege().getIsInProgress())
						{
							// siege in progress
							if (castle.getSiege().checkIsDefender(activeChar.getClan()))
							{
								isInDefense = true;
							}
						}
						
						if (fort != null && fort.getSiege().getIsInProgress())
						{
							// siege in progress
							if (fort.getSiege().checkIsDefender(activeChar.getClan()))
							{
								isInDefense = true;
							}
						}
						
						if (activeChar.getClan().getHasCastle() == 0 && activeChar.getClan().getHasFort() == 0 && !isInDefense)
						{
							// cheater
							activeChar.sendMessage("You may not use this respawn point!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
							return;
						}
						
						if (CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) != null)
						{
							teleportWhere = MapRegionTable.TeleportWhereType.Castle;
						}
						else if (FortManager.getInstance().getFortByOwner(activeChar.getClan()) != null)
						{
							teleportWhere = MapRegionTable.TeleportWhereType.Fortress;
						}
						
						loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, teleportWhere);
						break;
					
					case 3: // to siege HQ
						L2SiegeClan siegeClan = null;
						castle = CastleManager.getInstance().getCastle(activeChar);
						fort = FortManager.getInstance().getFort(activeChar);
						
						if (castle != null && castle.getSiege().getIsInProgress())
						{
							siegeClan = castle.getSiege().getAttackerClan(activeChar.getClan());
						}
						else if (fort != null && fort.getSiege().getIsInProgress())
						{
							siegeClan = fort.getSiege().getAttackerClan(activeChar.getClan());
						}
						
						if (siegeClan == null || siegeClan.getFlag().size() == 0)
						{
							// cheater
							activeChar.sendMessage("You may not use this respawn point!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
							return;
						}
						
						loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.SiegeFlag);
						break;
					
					case 4: // Fixed or Player is a festival participant
						if (!activeChar.isGM() && !activeChar.isFestivalParticipant() && !activeChar.isInsideZone(ZoneId.ZONE_RANDOM))
						{
							// cheater
							activeChar.sendMessage("You may not use this respawn point.");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
							return;
						}
						
						if (!activeChar.isGM() && activeChar.isInsideZone(ZoneId.ZONE_RANDOM))
						{
							loc = RandomZoneTaskManager.getInstance().getCurrentZone().getLoc();
						}
						else
						{
							loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ());
						}
						break;
					
					case 27: // to jail
						if (!activeChar.isInJail())
						{
							return;
						}
						loc = new Location(-114356, -249645, -2984);
						break;
					
					default:
						if (activeChar.getKarma() > 0 && Config.ALT_KARMA_TELEPORT_TO_FLORAN)
						{
							loc = new Location(17836, 170178, -3507);// Floran Village
							break;
						}
						
						if (Config.CUSTOM_RESPAWN)
						{
							loc = new Location(Config.CSPAWN_X, Config.CSPAWN_Y, Config.CSPAWN_Z);
						}
						else
						{
							loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
						}
						break;
				}
				
				// Stand up and teleport, proof dvp video.
				activeChar.setIsIn7sDungeon(false);
				activeChar.setIsPendingRevive(true);
				activeChar.teleToLocation(loc, true);
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.isFakeDeath())
		{
			activeChar.stopFakeDeath(null);
			activeChar.broadcastPacket(new Revive(activeChar));
			return;
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if (castle != null && castle.getSiege().getIsInProgress())
		{
			if (activeChar.getClan() != null && castle.getSiege().checkIsAttacker(activeChar.getClan()))
			{
				// Schedule respawn delay for attacker
				ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), castle.getSiege().getAttackerRespawnDelay());
				activeChar.sendMessage("You will be re-spawned in " + castle.getSiege().getAttackerRespawnDelay() / 1000 + " seconds");
				return;
			}
		}
		// run immediately (no need to schedule)
		new DeathTask(activeChar).run();
	}
	
	@Override
	public String getType()
	{
		return "[C] 6d RequestRestartPoint";
	}
}