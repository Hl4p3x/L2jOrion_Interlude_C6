/* This program is free software; you can redistribute it and/or modify
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.form.ZoneCuboid;
import l2jorion.game.model.zone.form.ZoneCylinder;
import l2jorion.game.model.zone.form.ZoneNPoly;
import l2jorion.game.model.zone.type.L2ArenaZone;
import l2jorion.game.model.zone.type.L2BigheadZone;
import l2jorion.game.model.zone.type.L2BossZone;
import l2jorion.game.model.zone.type.L2CastleTeleportZone;
import l2jorion.game.model.zone.type.L2CastleZone;
import l2jorion.game.model.zone.type.L2ClanHallZone;
import l2jorion.game.model.zone.type.L2CustomZone;
import l2jorion.game.model.zone.type.L2DamageZone;
import l2jorion.game.model.zone.type.L2DerbyTrackZone;
import l2jorion.game.model.zone.type.L2EffectZone;
import l2jorion.game.model.zone.type.L2FishingZone;
import l2jorion.game.model.zone.type.L2FortZone;
import l2jorion.game.model.zone.type.L2JailZone;
import l2jorion.game.model.zone.type.L2MotherTreeZone;
import l2jorion.game.model.zone.type.L2NoHqZone;
import l2jorion.game.model.zone.type.L2NoLandingZone;
import l2jorion.game.model.zone.type.L2NoStoreZone;
import l2jorion.game.model.zone.type.L2OlympiadStadiumZone;
import l2jorion.game.model.zone.type.L2PeaceZone;
import l2jorion.game.model.zone.type.L2PoisonZone;
import l2jorion.game.model.zone.type.L2SwampZone;
import l2jorion.game.model.zone.type.L2TownZone;
import l2jorion.game.model.zone.type.L2WaterZone;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ZoneManager
{
	private static final Logger LOG = LoggerFactory.getLogger(ZoneManager.class);
	private final FastMap<Integer, L2ZoneType> _zones = new FastMap<>();
	
	public static final ZoneManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private ZoneManager()
	{
		load();
	}
	
	public void reload()
	{
		// int zoneCount = 0;
		
		// Get the world regions
		int count = 0;
		L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
		for (int x = 0; x < worldRegions.length; x++)
		{
			for (int y = 0; y < worldRegions[x].length; y++)
			{
				worldRegions[x][y].getZones().clear();
				count++;
			}
		}
		LOG.info("Removed zones in " + count + " regions.");
		// Load the zones
		load();
	}
	
	private final void load()
	{
		Connection con = null;
		int zoneCount = 0;
		
		// Get the world regions
		L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
		
		boolean done = false;
		
		// Load the zone xml
		try
		{
			// Get a sql connection here
			con = L2DatabaseFactory.getInstance().getConnection();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			File file = new File(Config.DATAPACK_ROOT + "/data/xml/zones/zone.xml");
			if(!file.exists())
			{
				if(Config.DEBUG)
				{
					LOG.info("The zone.xml file is missing.");
				}
				
			}
			else
			{
				Document doc = factory.newDocumentBuilder().parse(file);
				factory = null;
				file = null;
				
				int effect_zone_id = 150000;
				for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if("list".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("zone".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();

								int zoneId = -1;
								if(attrs.getNamedItem("id")!=null){
									zoneId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
								}
								
								int minZ = Integer.parseInt(attrs.getNamedItem("minZ").getNodeValue());
								int maxZ = Integer.parseInt(attrs.getNamedItem("maxZ").getNodeValue());
								
								String zoneType = attrs.getNamedItem("type").getNodeValue();
								String zoneShape = attrs.getNamedItem("shape").getNodeValue();
								
								// Create the zone
								L2ZoneType temp = null;
								
								if(zoneType.equals("FishingZone"))
								{
									temp = new L2FishingZone(zoneId);
								}
								else if(zoneType.equals("ClanHallZone"))
								{
									temp = new L2ClanHallZone(zoneId);
								}
								else if(zoneType.equals("PeaceZone"))
								{
									temp = new L2PeaceZone(zoneId);
								}
								else if(zoneType.equals("Town"))
								{
									temp = new L2TownZone(zoneId);
								}
								else if(zoneType.equals("OlympiadStadium"))
								{
									temp = new L2OlympiadStadiumZone(zoneId);
								}
								else if(zoneType.equals("CastleZone"))
								{
									temp = new L2CastleZone(zoneId);
								}
								else if(zoneType.equals("FortZone"))
								{
									temp = new L2FortZone(zoneId);
								}
								else if(zoneType.equals("DamageZone"))
								{
									temp = new L2DamageZone(zoneId);
								}
								else if(zoneType.equals("Arena"))
								{
									temp = new L2ArenaZone(zoneId);
								}
								else if(zoneType.equals("MotherTree"))
								{
									temp = new L2MotherTreeZone(zoneId);
								}
								else if(zoneType.equals("BigheadZone"))
								{
									temp = new L2BigheadZone(zoneId);
								}
								else if(zoneType.equals("NoLandingZone"))
								{
									temp = new L2NoLandingZone(zoneId);
								}
								else if (zoneType.equals("NoStoreZone"))
								{						
									temp = new L2NoStoreZone(zoneId);
								}
								else if(zoneType.equals("JailZone"))
								{
									temp = new L2JailZone(zoneId);
								}
								else if(zoneType.equals("DerbyTrackZone"))
								{
									temp = new L2DerbyTrackZone(zoneId);
								}
								else if(zoneType.equals("WaterZone"))
								{
									temp = new L2WaterZone(zoneId);
								}
								else if(zoneType.equals("NoHqZone"))
								{
									temp = new L2NoHqZone(zoneId);
								}
								else if(zoneType.equals("BossZone"))
								{
									int boss_id = -1;
									
									try{
										boss_id = Integer.parseInt(attrs.getNamedItem("bossId").getNodeValue());
									}catch(IllegalArgumentException e){
										e.printStackTrace();
									}
									
									temp = new L2BossZone(zoneId, boss_id);
								}

								else if(zoneType.equals("EffectZone"))
								{
									zoneId = effect_zone_id;
									effect_zone_id++;
									temp = new L2EffectZone(zoneId);
								}
								else if(zoneType.equals("PoisonZone"))
								{
									temp = new L2PoisonZone(zoneId);
								}
								else if(zoneType.equals("CastleTeleportZone"))
								{
									temp = new L2CastleTeleportZone(zoneId);
								}
								else if(zoneType.equals("CustomZone"))
								{
									temp = new L2CustomZone(zoneId);
								}
								else if(zoneType.equals("SwampZone"))
								{
									temp = new L2SwampZone(zoneId);
								}
								
								// Check for unknown type
								if(temp == null)
								{
									LOG.warn("ZoneData: No such zone type: " + zoneType);
									continue;
								}
								
								//get the zone shape from file if any
								
								int[][] coords = null;
								int[] point;
								FastList<int[]> rs = FastList.newInstance();
								try
								{
									// loading from XML first
									for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
									{
										if ("node".equalsIgnoreCase(cd.getNodeName()))
										{
											attrs = cd.getAttributes();
											point = new int[2];
											point[0] = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
											point[1] = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
											rs.add(point);
										}
									}
									
									coords = rs.toArray(new int[rs.size()][]);
								}
								finally
								{
									FastList.recycle(rs);
								}
								
								if (coords == null || coords.length == 0) //check on database
								{
									
								
									// Get the zone shape from sql or from file if not defined into sql
									try
									{
										PreparedStatement statement = null;
										
										// Set the correct query
										statement = con.prepareStatement("SELECT x,y FROM zone_vertices WHERE id=? ORDER BY 'order' ASC ");
										
										statement.setInt(1, zoneId);
										ResultSet rset = statement.executeQuery();
										
										// Create this zone.  Parsing for cuboids is a bit different than for other polygons
										// cuboids need exactly 2 points to be defined.  Other polygons need at least 3 (one per vertex)
										if (zoneShape.equals("Cuboid"))
										{
											int[] x =
											{
													0, 0
											};
											int[] y =
											{
													0, 0
											};
											
											boolean successfulLoad = true;
											
											for (int i = 0; i < 2; i++)
											{
												if(rset.next())
												{
													x[i] = rset.getInt("x");
													y[i] = rset.getInt("y");
												}
												else
												{
													LOG.warn("ZoneData: Missing cuboid vertex in sql data for zone: " + zoneId);
													statement.close();
													rset.close();
													successfulLoad = false;
													break;
												}
											}
											
											if (successfulLoad)
											{
												temp.setZone(new ZoneCuboid(x[0], x[1], y[0], y[1], minZ, maxZ));
											}
											else
											{
												continue;
											}
										}
										else if(zoneShape.equals("NPoly"))
										{
											FastList<Integer> fl_x = new FastList<>(), fl_y = new FastList<>();
											
											// Load the rest
											while(rset.next())
											{
												fl_x.add(rset.getInt("x"));
												fl_y.add(rset.getInt("y"));
											}
											
											// An nPoly needs to have at least 3 vertices
											if (fl_x.size() == fl_y.size() && fl_x.size() > 2)
											{
												// Create arrays
												int[] aX = new int[fl_x.size()];
												int[] aY = new int[fl_y.size()];
												
												// This runs only at server startup so dont complain :>
												for (int i = 0; i < fl_x.size(); i++)
												{
													aX[i] = fl_x.get(i);
													aY[i] = fl_y.get(i);
												}
												
												// Create the zone
												temp.setZone(new ZoneNPoly(aX, aY, minZ, maxZ));
											}
											else
											{
												LOG.warn("ZoneData: Bad sql data for zone: " + zoneId);
												statement.close();
												rset.close();
												continue;
											}
										}
										else
										{
											LOG.warn("ZoneData: Unknown shape: " + zoneShape);
											statement.close();
											rset.close();
											continue;
										}
										
										statement.close();
										rset.close();
									}
									catch(Exception e)
									{
										if (Config.ENABLE_ALL_EXCEPTIONS)
											e.printStackTrace();
										
										
										LOG.warn("ZoneData: Failed to load zone coordinates: " + e);
									}
									
								}
								else
								{ //use file one
									
									// Create this zone. Parsing for cuboids is a
									// bit different than for other polygons
									// cuboids need exactly 2 points to be defined.
									// Other polygons need at least 3 (one per
									// vertex)
									if (zoneShape.equalsIgnoreCase("Cuboid"))
									{
										if (coords.length == 2)
											temp.setZone(new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ));
										else
										{
											LOG.warn("ZoneData: Missing cuboid vertex in sql data for zone: " + zoneId);
											continue;
										}
									}
									else if (zoneShape.equalsIgnoreCase("NPoly"))
									{
										// nPoly needs to have at least 3 vertices
										if (coords.length > 2)
										{
											int[] aX = new int[coords.length];
											int[] aY = new int[coords.length];
											for (int i = 0; i < coords.length; i++)
											{
												aX[i] = coords[i][0];
												aY[i] = coords[i][1];
											}
											temp.setZone(new ZoneNPoly(aX, aY, minZ, maxZ));
										}
										else
										{
											LOG.warn("ZoneData: Bad data for zone: " + zoneId);
											continue;
										}
									}
									else if (zoneShape.equalsIgnoreCase("Cylinder"))
									{
										// A Cylinder zone requires a center point
										// at x,y and a radius
										attrs = d.getAttributes();
										final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
										if (coords.length == 1 && zoneRad > 0)
											temp.setZone(new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad));
										else
										{
											LOG.warn("ZoneData: Bad data for zone: " + zoneId);
											continue;
										}
									}
									else
									{
										LOG.warn("ZoneData: Unknown shape: " + zoneShape);
										continue;
									}
									
								}
								// Check for aditional parameters
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("stat".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String name = attrs.getNamedItem("name").getNodeValue();
										String val = attrs.getNamedItem("val").getNodeValue();
										
										temp.setParameter(name, val);
									}
									
									if ("spawn".equalsIgnoreCase(cd.getNodeName()))
									{
										temp.setSpawnLocs(cd);
									}
								}
								
								// Register the zone into any world region it intersects with...
								// currently 11136 test for each zone :>
								int ax, ay, bx, by;
								
								for (int x = 0; x < worldRegions.length; x++)
								{
									for (int y = 0; y < worldRegions[x].length; y++)
									{
										ax = x - L2World.OFFSET_X << L2World.SHIFT_BY;
										bx = x + 1 - L2World.OFFSET_X << L2World.SHIFT_BY;
										ay = y - L2World.OFFSET_Y << L2World.SHIFT_BY;
										by = y + 1 - L2World.OFFSET_Y << L2World.SHIFT_BY;
										
										if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
										{
											if(Config.DEBUG)
											{
												LOG.info("Zone (" + zoneId + ") added to: " + x + " " + y);
											}
											worldRegions[x][y].addZone(temp);
										}
									}
								}
								
								// Special managers for arenas, towns...
								if(temp instanceof L2TownZone)
								{
									TownManager.getInstance().addTown((L2TownZone) temp);
								}
								else if(temp instanceof L2BossZone)
								{
									GrandBossManager.getInstance().addZone((L2BossZone) temp);
								}
								
								// Increase the counter
								zoneCount++;
							}
						}
					}
				}
				
				done = true;
			}

		}
		catch(Exception e)
		{
			if(Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			
			LOG.error("Error while loading zones.", e);
			
		}
		finally
		{
			CloseUtil.close(con);
			
		}
		
		if (done)
		{
			GrandBossManager.getInstance().initZones();
		}

		LOG.info("Done: loaded " + zoneCount + " zones.");
	}
	
	/**
	 * Add new zone
	 * @param id 
	 *
	 * @param zone
	 */
	public void addZone(Integer id, L2ZoneType zone)
	{
		_zones.put(id, zone);
	}
	
	/**
	 * Returns all zones registered with the ZoneManager.
	 * To minimise iteration processing retrieve zones from L2WorldRegion for a specific location instead.
	 * @return zones
	 */
	public Collection<L2ZoneType> getAllZones()
	{
		return _zones.values();
	}
	
	public L2ZoneType getZoneById(int id)
	{
		return _zones.get(id);
	}
	
	/**
	 * Returns all zones from where the object is located
	 *
	 * @param object
	 * @return zones
	 */
	public List<L2ZoneType> getZones(L2Object object)
	{
		return getZones(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * Returns zone from where the object is located by type
	 * @param <T> 
	 *
	 * @param object
	 * @param type
	 * @return zone
	 */
	public <T extends L2ZoneType> T getZone(L2Object object, Class<T> type)
	{
		if (object == null)
			return null;
		return getZone(object.getX(), object.getY(), object.getZ(), type);
	}
	
	/**
	 * Returns all zones from given coordinates (plane)
	 * 
	 * @param x
	 * @param y
	 * @return zones
	 */
	public List<L2ZoneType> getZones(int x, int y)
	{
		L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		List<L2ZoneType> temp = new ArrayList<>();
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y))
				temp.add(zone);
		}
		return temp;
	}
	
	/**
	 * Returns all zones from given coordinates 
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @return zones
	 */
	public List<L2ZoneType> getZones(int x, int y, int z)
	{
		L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		List<L2ZoneType> temp = new ArrayList<>();
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y, z))
				temp.add(zone);
		}
		return temp;
	}
	
	/**
	 * Returns zone from given coordinates 
	 * @param <T> 
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param type
	 * @return zone
	 */
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> T getZone(int x, int y, int z, Class<T> type)
	{
		L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y, z) && zone.getClass().equals(type))
				return (T) zone;
		}
		return null;
	}
	
	public final L2ArenaZone getArena(L2Character character)
	{
		if (character == null)
			return null;

		for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
		{
			if (temp instanceof L2ArenaZone && temp.isCharacterInZone(character))
				return ((L2ArenaZone) temp);
		}
		
		return null;
	}
	
	public final L2OlympiadStadiumZone getOlympiadStadium(L2Character character)
	{
		if (character == null)
			return null;

		for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
		{
			if (temp instanceof L2OlympiadStadiumZone && temp.isCharacterInZone(character))
				return ((L2OlympiadStadiumZone) temp);
		}
		return null;
	}
	
	public final L2WaterZone getWaterZone(L2Character character)
	{
		if (character == null)
			return null;
		
		for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), (character.getZ() - 100)))
		{
			if (temp instanceof L2WaterZone && temp.isCharacterInZone(character))
				return ((L2WaterZone) temp);
		}
		
		return null;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ZoneManager _instance = new ZoneManager();
	}
}
