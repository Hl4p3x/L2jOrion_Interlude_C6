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
package l2jorion.game.managers;

import java.awt.Polygon;
import java.awt.Shape;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.DimensionalRift;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Util;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DimensionalRiftManager
{
	
	protected static final Logger LOG = LoggerFactory.getLogger(DimensionalRiftManager.class);
	private static DimensionalRiftManager _instance;
	private final FastMap<Byte, FastMap<Byte, DimensionalRiftRoom>> _rooms = new FastMap<>();
	private final short DIMENSIONAL_FRAGMENT_ITEM_ID = 7079;
	private final static int MAX_PARTY_PER_AREA = 3;
	
	public static DimensionalRiftManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new DimensionalRiftManager();
		}
		
		return _instance;
	}
	
	private DimensionalRiftManager()
	{
		loadRooms();
		loadSpawns();
	}
	
	public DimensionalRiftRoom getRoom(final byte type, final byte room)
	{
		return _rooms.get(type) == null ? null : _rooms.get(type).get(room);
	}
	
	public boolean isAreaAvailable(final byte area)
	{
		final FastMap<Byte, DimensionalRiftRoom> tmap = _rooms.get(area);
		if (tmap == null)
			return false;
		int used = 0;
		for (final DimensionalRiftRoom room : tmap.values())
		{
			if (room.isUsed())
				used++;
		}
		return used <= MAX_PARTY_PER_AREA;
	}
	
	public boolean isRoomAvailable(final byte area, final byte room)
	{
		if (_rooms.get(area) == null || _rooms.get(area).get(room) == null)
			return false;
		return !_rooms.get(area).get(room).isUsed();
	}
	
	private void loadRooms()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement s = con.prepareStatement("SELECT * FROM dimensional_rift");
			ResultSet rs = s.executeQuery();
			
			while (rs.next())
			{
				// 0 waiting room, 1 recruit, 2 soldier, 3 officer, 4 captain , 5 commander, 6 hero
				final byte type = rs.getByte("type");
				final byte room_id = rs.getByte("room_id");
				
				// coords related
				final int xMin = rs.getInt("xMin");
				final int xMax = rs.getInt("xMax");
				final int yMin = rs.getInt("yMin");
				final int yMax = rs.getInt("yMax");
				final int z1 = rs.getInt("zMin");
				final int z2 = rs.getInt("zMax");
				final int xT = rs.getInt("xT");
				final int yT = rs.getInt("yT");
				final int zT = rs.getInt("zT");
				final boolean isBossRoom = rs.getByte("boss") > 0;
				
				if (!_rooms.containsKey(type))
				{
					_rooms.put(type, new FastMap<Byte, DimensionalRiftRoom>());
				}
				
				_rooms.get(type).put(room_id, new DimensionalRiftRoom(type, room_id, xMin, xMax, yMin, yMax, z1, z2, xT, yT, zT, isBossRoom));
			}
			
			s.close();
			rs.close();
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("Can't load Dimension Rift zones. " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		final int typeSize = _rooms.keySet().size();
		int roomSize = 0;
		
		for (final Byte b : _rooms.keySet())
		{
			roomSize += _rooms.get(b).keySet().size();
		}
		
		LOG.info("DimensionalRiftManager: Loaded " + typeSize + " room types with " + roomSize + " rooms.");
	}
	
	public void loadSpawns()
	{
		int countGood = 0, countBad = 0;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File file = new File(Config.DATAPACK_ROOT + "/data/xml/dimensionalRift.xml");
			if (!file.exists())
				throw new IOException();
			
			final Document doc = factory.newDocumentBuilder().parse(file);
			
			NamedNodeMap attrs;
			byte type, roomId;
			int mobId, x, y, z, delay, count;
			L2Spawn spawnDat;
			L2NpcTemplate template;
			
			for (Node rift = doc.getFirstChild(); rift != null; rift = rift.getNextSibling())
			{
				if ("rift".equalsIgnoreCase(rift.getNodeName()))
				{
					for (Node area = rift.getFirstChild(); area != null; area = area.getNextSibling())
					{
						if ("area".equalsIgnoreCase(area.getNodeName()))
						{
							attrs = area.getAttributes();
							type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());
							
							for (Node room = area.getFirstChild(); room != null; room = room.getNextSibling())
							{
								if ("room".equalsIgnoreCase(room.getNodeName()))
								{
									attrs = room.getAttributes();
									roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());
									
									for (Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
									{
										if ("spawn".equalsIgnoreCase(spawn.getNodeName()))
										{
											attrs = spawn.getAttributes();
											mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
											delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
											count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
											
											template = NpcTable.getInstance().getTemplate(mobId);
											if (template == null)
											{
												LOG.warn("Template " + mobId + " not found!");
											}
											if (!_rooms.containsKey(type))
											{
												LOG.warn("Type " + type + " not found!");
											}
											else if (!_rooms.get(type).containsKey(roomId))
											{
												LOG.warn("Room " + roomId + " in Type " + type + " not found!");
											}
											
											for (int i = 0; i < count; i++)
											{
												DimensionalRiftRoom riftRoom = _rooms.get(type).get(roomId);
												x = riftRoom.getRandomX();
												y = riftRoom.getRandomY();
												z = riftRoom.getTeleportCoords()[2];
												
												if(template != null && _rooms.containsKey(type) && _rooms.get(type).containsKey(roomId))
												{
													spawnDat = new L2Spawn(template);
													spawnDat.setAmount(1);
													spawnDat.setLocx(x);
													spawnDat.setLocy(y);
													spawnDat.setLocz(z);
													spawnDat.setHeading(-1);
													spawnDat.setRespawnDelay(delay);
													SpawnTable.getInstance().addNewSpawn(spawnDat, false);
													_rooms.get(type).get(roomId).getSpawns().add(spawnDat);
													countGood++;
												}
												else
												{
													countBad++;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (final Exception e)
		{
			LOG.warn("Error on loading dimensional rift spawns: " + e);
			e.printStackTrace();
		}
		LOG.info("DimensionalRiftManager: Loaded " + countGood + " dimensional rift spawns, " + countBad + " errors.");
	}
	
	@SuppressWarnings("unlikely-arg-type")
	public void reload()
	{
		for (final Byte b : _rooms.keySet())
		{
			for (final int i : _rooms.get(b).keySet())
			{
				_rooms.get(b).get(i).getSpawns().clear();
			}
			_rooms.get(b).clear();
		}
		_rooms.clear();
		loadRooms();
		loadSpawns();
	}
	
	public boolean checkIfInRiftZone(final int x, final int y, final int z, final boolean ignorePeaceZone)
	{
		if (ignorePeaceZone)
			return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z);
		
		return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z) && !_rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
	}
	
	public boolean checkIfInPeaceZone(final int x, final int y, final int z)
	{
		return _rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
	}
	
	public void teleportToWaitingRoom(final L2PcInstance player)
	{
		final int[] coords = getRoom((byte) 0, (byte) 0).getTeleportCoords();
		player.teleToLocation(coords[0], coords[1], coords[2], true);
	}
	
	public void start(final L2PcInstance player, final byte type, final L2NpcInstance npc)
	{
		boolean canPass = true;
		if (!player.isInParty())
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NoParty.htm", npc);
			return;
		}
		
		if (player.getParty().getPartyLeaderOID() != player.getObjectId())
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
			return;
		}
		
		if (player.getParty().isInDimensionalRift())
		{
			handleCheat(player, npc);
			return;
		}
		
		if (!isAreaAvailable(type))
		{
			player.sendMessage("This rift area is full. Try later.");
			return;
		}
		
		if (player.getParty().getMemberCount() < Config.RIFT_MIN_PARTY_SIZE)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/seven_signs/rift/SmallParty.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", String.valueOf(Config.RIFT_MIN_PARTY_SIZE));
			player.sendPacket(html);
			return;
		}
		
		for (final L2PcInstance p : player.getParty().getPartyMembers())
			if (!checkIfInPeaceZone(p.getX(), p.getY(), p.getZ()))
			{
				canPass = false;
			}
		
		if (!canPass)
		{
			showHtmlFile(player, "data/html/seven_signs/rift/NotInWaitingRoom.htm", npc);
			return;
		}
		
		L2ItemInstance i;
		for (final L2PcInstance p : player.getParty().getPartyMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
			
			if (i == null)
			{
				canPass = false;
				break;
			}
			
			if (i.getCount() > 0)
				if (i.getCount() < getNeededItems(type))
				{
					canPass = false;
				}
		}
		
		if (!canPass)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/seven_signs/rift/NoFragments.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", String.valueOf(getNeededItems(type)));
			player.sendPacket(html);
			html = null;
			return;
		}
		
		for (final L2PcInstance p : player.getParty().getPartyMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
			p.destroyItem("RiftEntrance", i.getObjectId(), getNeededItems(type), null, false);
		}
		
		i = null;
		
		byte room;
		do
		{
			room = (byte) Rnd.get(1, 9);
		}
		while (!isRoomAvailable(type, room));
		
		new DimensionalRift(player.getParty(), type, room);
	}
	
	public void killRift(final DimensionalRift d)
	{
		if (d.getTeleportTimerTask() != null)
		{
			d.getTeleportTimerTask().cancel();
		}
		d.setTeleportTimerTask(null);
		
		if (d.getTeleportTimer() != null)
		{
			d.getTeleportTimer().cancel();
		}
		d.setTeleportTimer(null);
		
		if (d.getSpawnTimerTask() != null)
		{
			d.getSpawnTimerTask().cancel();
		}
		d.setSpawnTimerTask(null);
		
		if (d.getSpawnTimer() != null)
		{
			d.getSpawnTimer().cancel();
		}
		d.setSpawnTimer(null);
	}
	
	public class DimensionalRiftRoom
	{
		protected final byte _type;
		protected final byte _room;
		private final int _xMin;
		private final int _xMax;
		private final int _yMin;
		private final int _yMax;
		private final int _zMin;
		private final int _zMax;
		private final int[] _teleportCoords;
		private final Shape _s;
		private final boolean _isBossRoom;
		
		private final FastList<L2Spawn> _roomSpawns;
		
		protected final FastList<L2NpcInstance> _roomMobs;
		
		private boolean _isUsed = false;
		
		public DimensionalRiftRoom(final byte type, final byte room, final int xMin, final int xMax, final int yMin, final int yMax, final int zMin, final int zMax, final int xT, final int yT, final int zT, final boolean isBossRoom)
		{
			_type = type;
			_room = room;
			_xMin = xMin + 128;
			_xMax = xMax - 128;
			_yMin = yMin + 128;
			_yMax = yMax - 128;
			_zMin = zMin;
			_zMax = zMax;
			
			_teleportCoords = new int[]
			{
				xT,
				yT,
				zT
			};
			
			_isBossRoom = isBossRoom;
			_roomSpawns = new FastList<>();
			_roomMobs = new FastList<>();
			_s = new Polygon(new int[]
			{
				xMin,
				xMax,
				xMax,
				xMin
			}, new int[]
			{
				yMin,
				yMin,
				yMax,
				yMax
			}, 4);
		}
		
		public int getRandomX()
		{
			return Rnd.get(_xMin, _xMax);
		}
		
		public int getRandomY()
		{
			return Rnd.get(_yMin, _yMax);
		}
		
		public int[] getTeleportCoords()
		{
			return _teleportCoords;
		}
		
		public boolean checkIfInZone(final int x, final int y, final int z)
		{
			return _s.contains(x, y) && z >= _zMin && z <= _zMax;
		}
		
		public boolean isBossRoom()
		{
			return _isBossRoom;
		}
		
		public FastList<L2Spawn> getSpawns()
		{
			return _roomSpawns;
		}
		
		public void spawn()
		{
			for (L2Spawn spawn : _roomSpawns)
			{
				spawn.doSpawn();
				
				if (spawn.getNpcid() != 25338)
				{
					spawn.startRespawn();
				}
			}
		}
		
		public void unspawn()
		{
			for (L2Spawn spawn : _roomSpawns)
			{
				spawn.stopRespawn();
				
				if (spawn.getLastSpawn() != null)
				{
					spawn.getLastSpawn().deleteMe();
				}
				
				spawn.decreaseCount(null);
			}
			
			_isUsed = false;
		}
		
		public void setUsed()
		{
			_isUsed = true;
		}
		
		public boolean isUsed()
		{
			return _isUsed;
		}
	}
	
	private int getNeededItems(final byte type)
	{
		switch (type)
		{
			case 1:
				return Config.RIFT_ENTER_COST_RECRUIT;
			case 2:
				return Config.RIFT_ENTER_COST_SOLDIER;
			case 3:
				return Config.RIFT_ENTER_COST_OFFICER;
			case 4:
				return Config.RIFT_ENTER_COST_CAPTAIN;
			case 5:
				return Config.RIFT_ENTER_COST_COMMANDER;
			case 6:
				return Config.RIFT_ENTER_COST_HERO;
			default:
				return 999999;
		}
	}
	
	public void showHtmlFile(final L2PcInstance player, final String file, final L2NpcInstance npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(file);
		html.replace("%npc_name%", npc.getName());
		player.sendPacket(html);
		html = null;
	}
	
	public void handleCheat(final L2PcInstance player, final L2NpcInstance npc)
	{
		showHtmlFile(player, "data/html/seven_signs/rift/Cheater.htm", npc);
		if (!player.isGM())
		{
			LOG.warn("Player " + player.getName() + "(" + player.getObjectId() + ") was cheating in dimension rift area!");
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " tried to cheat in dimensional rift.", Config.DEFAULT_PUNISH);
		}
	}
}
