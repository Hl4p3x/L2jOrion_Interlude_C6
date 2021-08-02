/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.CursedWeapon;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2CommanderInstance;
import l2jorion.game.model.actor.instance.L2FestivalMonsterInstance;
import l2jorion.game.model.actor.instance.L2FortSiegeGuardInstance;
import l2jorion.game.model.actor.instance.L2GrandBossInstance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2RiftInvaderInstance;
import l2jorion.game.model.actor.instance.L2SiegeGuardInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class CursedWeaponsManager
{
	private static final Logger LOG = LoggerFactory.getLogger(CursedWeaponsManager.class);
	
	private static final Map<Integer, CursedWeapon> _cursedWeapons = new FastMap<>();
	
	public static final CursedWeaponsManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public CursedWeaponsManager()
	{
		if (!Config.ALLOW_CURSED_WEAPONS)
		{
			return;
		}
		
		load();
		restore();
		controlPlayers();
		
		LOG.info("CursedWeaponsManager: Loaded: " + _cursedWeapons.size() + " cursed weapons");
	}
	
	public final void reload()
	{
		if (!Config.ALLOW_CURSED_WEAPONS)
		{
			return;
		}

		// Drop existing CWs.
		for (CursedWeapon cw : _cursedWeapons.values())
		{
			cw.endOfLife();
		}
		
		_cursedWeapons.clear();
		load();
		//restore();
		//controlPlayers();
		
		LOG.info("Reloaded: " + _cursedWeapons.size() + " cursed weapon(s).");
	}
	
	private final void load()
	{
		//LOG.info("Initializing CursedWeaponsManager");
		if (Config.DEBUG)
		{
			LOG.info("Loading data: ");
		}
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File file = new File(Config.DATAPACK_ROOT + "/data/xml/cursedWeapons.xml");
			if (!file.exists())
			{
				if (Config.DEBUG)
				{
					LOG.info("NO FILE");
				}
				return;
			}
			
			final Document doc = factory.newDocumentBuilder().parse(file);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							final int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
							String name = attrs.getNamedItem("name").getNodeValue();
							
							CursedWeapon cw = new CursedWeapon(id, skillId, name);
							name = null;
							
							int val;
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("dropRate".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDropRate(val);
								}
								else if ("duration".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDuration(val);
								}
								else if ("durationLost".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDurationLost(val);
								}
								else if ("disapearChance".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDisapearChance(val);
								}
								else if ("stageKills".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setStageKills(val);
								}
							}
							
							// Store cursed weapon
							_cursedWeapons.put(id, cw);
							
							attrs = null;
							cw = null;
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			LOG.error("Error parsing cursed weapons file.", e);
			
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
		
	}
	

	private final void restore()
	{
		Connection con = null;
		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT itemId, playerId, playerKarma, playerPkKills, nbKills, endTime FROM cursed_weapons");
			ResultSet rset = statement.executeQuery();
			
			if (rset.next())
			{
				final int itemId = rset.getInt("itemId");
				final int playerId = rset.getInt("playerId");
				final int playerKarma = rset.getInt("playerKarma");
				final int playerPkKills = rset.getInt("playerPkKills");
				final int nbKills = rset.getInt("nbKills");
				final long endTime = rset.getLong("endTime");
				
				CursedWeapon cw = _cursedWeapons.get(itemId);
				cw.setPlayerId(playerId);
				cw.setPlayerKarma(playerKarma);
				cw.setPlayerPkKills(playerPkKills);
				cw.setNbKills(nbKills);
				cw.setEndTime(endTime);
				cw.reActivate();
				
				cw = null;
				
				// clean up the cursed weapons table.
				removeFromDb(itemId);
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
			rset = null;
			statement = null;
			
		}
		catch (final Exception e)
		{
			LOG.warn("Could not restore CursedWeapons data: " + e);
			
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		
	}
	

	private final void controlPlayers()
	{
		
		Connection con = null;
		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			ResultSet rset = null;
			
			// TODO: See comments below...
			// This entire for loop should NOT be necessary, since it is already handled by
			// CursedWeapon.endOfLife(). However, if we indeed *need* to duplicate it for safety,
			// then we'd better make sure that it FULLY cleans up inactive cursed weapons!
			// Undesired effects result otherwise, such as player with no zariche but with karma
			// or a lost-child entry in the cursed weapons table, without a corresponding one in items...
			for (final CursedWeapon cw : _cursedWeapons.values())
			{
				if (cw.isActivated())
					continue;
				
				// Do an item check to be sure that the cursed weapon isn't hold by someone
				final int itemId = cw.getItemId();
				try
				{
					statement = con.prepareStatement("SELECT owner_id FROM items WHERE item_id=?");
					statement.setInt(1, itemId);
					rset = statement.executeQuery();
					
					if (rset.next())
					{
						// A player has the cursed weapon in his inventory ...
						final int playerId = rset.getInt("owner_id");
						LOG.info("PROBLEM : Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");
						
						// Delete the item
						statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
						statement.setInt(1, playerId);
						statement.setInt(2, itemId);
						if (statement.executeUpdate() != 1)
						{
							LOG.warn("Error while deleting cursed weapon " + itemId + " from userId " + playerId);
						}
						DatabaseUtils.close(statement);
						
						// Restore the player's old karma and pk count
						statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE obj_id=?");
						statement.setInt(1, cw.getPlayerKarma());
						statement.setInt(2, cw.getPlayerPkKills());
						statement.setInt(3, playerId);
						if (statement.executeUpdate() != 1)
						{
							LOG.warn("Error while updating karma & pkkills for userId " + cw.getPlayerId());
						}
						
					}
					DatabaseUtils.close(rset);
					DatabaseUtils.close(statement);
					rset = null;
					statement = null;
					
				}
				catch (final SQLException sqlE)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						sqlE.printStackTrace();
				}
			}
		}
		catch (final Exception e)
		{
			LOG.warn("Could not check CursedWeapons data: ");
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		
	}
	
	// =========================================================
	// Properties - Public
	public synchronized void checkDrop(final L2Attackable attackable, final L2PcInstance player)
	{
		if (attackable instanceof L2SiegeGuardInstance || attackable instanceof L2RiftInvaderInstance || attackable instanceof L2FestivalMonsterInstance || attackable instanceof L2GrandBossInstance || attackable instanceof L2FortSiegeGuardInstance || attackable instanceof L2CommanderInstance)
			return;
		
		if (player.isCursedWeaponEquiped())
			return;
		
		for (final CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActive())
			{
				continue;
			}
			
			if (cw.checkDrop(attackable, player))
			{
				break;
			}
		}
	}
	
	public void activate(final L2PcInstance player, final L2ItemInstance item)
	{
		CursedWeapon cw = _cursedWeapons.get(item.getItemId());
		if (player.isCursedWeaponEquiped()) // cannot own 2 cursed swords
		{
			final CursedWeapon cw2 = _cursedWeapons.get(player.getCursedWeaponEquipedId());
			/*
			 * TODO: give the bonus level in a more appropriate manner. The following code adds "_stageKills" levels. This will also show in the char status. I do not have enough info to know if the bonus should be shown in the pk count, or if it should be a full "_stageKills" bonus or just the
			 * remaining from the current count till the of the current stage... This code is a TEMP fix, so that the cursed weapon's bonus level can be observed with as little change in the code as possible, until proper info arises.
			 */
			cw2.setNbKills(cw2.getStageKills() - 1);
			cw2.increaseKills();
			
			// erase the newly obtained cursed weapon
			cw.setPlayer(player); // NECESSARY in order to find which inventory the weapon is in!
			cw.endOfLife(); // expire the weapon and clean up.
			
		}
		else
		{
			cw.activate(player, item);
		}
	}
	
	public void drop(final int itemId, final L2Character killer)
	{
		CursedWeapon cw = _cursedWeapons.get(itemId);
		
		cw.dropIt(killer);
		cw = null;
	}
	
	public void increaseKills(final int itemId)
	{
		CursedWeapon cw = _cursedWeapons.get(itemId);
		
		cw.increaseKills();
		cw = null;
	}
	
	public int getLevel(final int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		
		return cw.getLevel();
	}
	
	public static void announce(final SystemMessage sm)
	{
		for (final L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player == null)
			{
				continue;
			}
			
			player.sendPacket(sm);
		}
		
	}
	
	public void checkPlayer(final L2PcInstance player)
	{
		if (player == null)
			return;
		
		for (final CursedWeapon cw : _cursedWeapons.values())
		{
			if (cw.isActive() && player.getObjectId() == cw.getPlayerId())
			{
				cw.setPlayer(player);
				cw.setItem(player.getInventory().getItemByItemId(cw.getItemId()));
				cw.giveSkill();
				player.setCursedWeaponEquipedId(cw.getItemId());
				
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
				sm.addString(cw.getName());
				// sm.addItemName(cw.getItemId());
				sm.addNumber((int) ((cw.getEndTime() - System.currentTimeMillis()) / 60000));
				player.sendPacket(sm);
				
				sm = new SystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION);
				sm.addZoneName(player.getX(), player.getY(), player.getZ()); // Region Name
				sm.addItemName(cw.getItemId());
				CursedWeaponsManager.announce(sm);
			}
		}
	}
	
	public static void removeFromDb(final int itemId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			// Delete datas
			PreparedStatement statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
			statement.setInt(1, itemId);
			statement.executeUpdate();
			
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final SQLException e)
		{
			LOG.error("CursedWeaponsManager: Failed to remove data", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public void saveData()
	{
		for (final CursedWeapon cw : _cursedWeapons.values())
		{
			cw.saveData();
		}
	}
	
	public boolean isCursed(final int itemId)
	{
		return _cursedWeapons.containsKey(itemId);
	}
	
	public Collection<CursedWeapon> getCursedWeapons()
	{
		return _cursedWeapons.values();
	}
	
	public Set<Integer> getCursedWeaponsIds()
	{
		return _cursedWeapons.keySet();
	}
	
	public CursedWeapon getCursedWeapon(int itemId)
	{
		return _cursedWeapons.get(itemId);
	}
	
	public void givePassive(int itemId)
	{
		try
		{
			_cursedWeapons.get(itemId).giveSkill();
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
		}
	}
	
	private static class SingletonHolder
	{
		protected static final CursedWeaponsManager _instance = new CursedWeaponsManager();
	}
}
