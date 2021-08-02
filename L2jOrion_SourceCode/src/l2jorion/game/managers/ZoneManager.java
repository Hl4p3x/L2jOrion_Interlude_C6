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
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.L2WorldRegion;
import l2jorion.game.model.zone.AbstractZoneSettings;
import l2jorion.game.model.zone.L2ZoneForm;
import l2jorion.game.model.zone.L2ZoneRespawn;
import l2jorion.game.model.zone.L2ZoneType;
import l2jorion.game.model.zone.form.ZoneCuboid;
import l2jorion.game.model.zone.form.ZoneCylinder;
import l2jorion.game.model.zone.form.ZoneNPoly;
import l2jorion.game.model.zone.type.L2ArenaZone;
import l2jorion.game.model.zone.type.L2OlympiadStadiumZone;
import l2jorion.game.model.zone.type.L2RespawnZone;
import l2jorion.game.model.zone.type.L2WaterZone;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.xml.IXmlReader;

public class ZoneManager implements IXmlReader
{
	private static final Logger LOG = LoggerFactory.getLogger(ZoneManager.class);
	
	private static final Map<String, AbstractZoneSettings> _settings = new HashMap<>();
	private final Map<Class<? extends L2ZoneType>, Map<Integer, ? extends L2ZoneType>> _classZones = new HashMap<>();
	private int _lastDynamicId = 300000;
	
	protected ZoneManager()
	{
		load();
	}
	
	public void reload()
	{
		int count = 0;
		// Get the world regions
		final L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
		
		// Backup old zone settings
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			for (L2ZoneType zone : map.values())
			{
				if (zone.getSettings() != null)
				{
					_settings.put(zone.getName(), zone.getSettings());
				}
			}
		}
		
		// Clear zones
		for (L2WorldRegion[] worldRegion : worldRegions)
		{
			for (L2WorldRegion element : worldRegion)
			{
				element.getZones().clear();
				count++;
			}
		}
		GrandBossManager.getInstance().getZones().clear();
		LOG.info("{}: Removed zones in " + count + " regions.", getClass().getSimpleName());
		
		// Load the zones
		load();
		
		// Re-validate all characters in zones
		for (L2Object obj : L2World.getInstance().getVisibleObjects())
		{
			if (obj instanceof L2Character)
			{
				((L2Character) obj).revalidateZone(true);
			}
		}
		_settings.clear();
	}
	
	@Override
	public final void load()
	{
		parseDatapackDirectory("data/xml/zones", false);
		LOG.info("{}: Loaded {} zone classes and {} zones", getClass().getSimpleName(), _classZones.size(), getSize());
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		Connection con = null;
		NamedNodeMap attrs;
		Node attribute;
		String zoneName;
		int[][] coords;
		int zoneId, minZ, maxZ;
		String zoneType, zoneShape;
		final List<int[]> rs = new ArrayList<>();
		
		// Get the world regions
		L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
		
		// Load the zone xml
		try
		{
			// Get a sql connection here
			con = L2DatabaseFactory.getInstance().getConnection();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				attrs = n.getAttributes();
				attribute = attrs.getNamedItem("enabled");
				if ((attribute != null) && !Boolean.parseBoolean(attribute.getNodeValue()))
				{
					continue;
				}
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("zone".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						
						attribute = attrs.getNamedItem("type");
						if (attribute != null)
						{
							zoneType = attribute.getNodeValue();
						}
						else
						{
							LOG.warn("ZoneData: Missing type for zone in file {}", f.getName());
							continue;
						}
						
						attribute = attrs.getNamedItem("id");
						if (attribute != null)
						{
							zoneId = Integer.parseInt(attribute.getNodeValue());
						}
						else
						{
							zoneId = _lastDynamicId++;
						}
						
						attribute = attrs.getNamedItem("name");
						if (attribute != null)
						{
							zoneName = attribute.getNodeValue();
						}
						else
						{
							zoneName = null;
						}
						
						if (zoneName == null)
						{
							LOG.warn("ZoneData: Missing name for NpcSpawnTerritory in file: {}, skipping zone!", f.getName());
							continue;
						}
						
						minZ = parseInteger(attrs, "minZ");
						maxZ = parseInteger(attrs, "maxZ");
						
						zoneType = parseString(attrs, "type");
						zoneShape = parseString(attrs, "shape");
						
						// get the zone shape from file if any
						L2ZoneForm zoneForm = null;
						try
						{
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("node".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									int[] point = new int[2];
									point[0] = parseInteger(attrs, "X");
									point[1] = parseInteger(attrs, "Y");
									rs.add(point);
								}
							}
							
							coords = rs.toArray(new int[rs.size()][2]);
							rs.clear();
							
							if ((coords == null) || (coords.length == 0))
							{
								// Get the zone shape from sql or from file if not defined into sql
								try
								{
									PreparedStatement statement = null;
									
									// Set the correct query
									statement = con.prepareStatement("SELECT x,y FROM zone_vertices WHERE id=? ORDER BY 'order' ASC ");
									
									statement.setInt(1, zoneId);
									ResultSet rset = statement.executeQuery();
									
									// Create this zone. Parsing for cuboids is a bit different than for other polygons
									// cuboids need exactly 2 points to be defined. Other polygons need at least 3 (one per vertex)
									if (zoneShape.equals("Cuboid"))
									{
										int[] x =
										{
											0,
											0
										};
										int[] y =
										{
											0,
											0
										};
										
										boolean successfulLoad = true;
										
										for (int i = 0; i < 2; i++)
										{
											if (rset.next())
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
											zoneForm = new ZoneCuboid(x[0], x[1], y[0], y[1], minZ, maxZ);
										}
										else
										{
											continue;
										}
									}
									else if (zoneShape.equals("NPoly"))
									{
										FastList<Integer> fl_x = new FastList<>(), fl_y = new FastList<>();
										
										// Load the rest
										while (rset.next())
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
											zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
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
								catch (Exception e)
								{
									if (Config.ENABLE_ALL_EXCEPTIONS)
									{
										e.printStackTrace();
									}
									
									LOG.warn("ZoneData: Failed to load zone coordinates: " + e);
								}
							}
							else
							{
								// Create this zone. Parsing for cuboids is a bit different than for other polygons cuboids need exactly 2 points to be defined.
								// Other polygons need at least 3 (one per vertex)
								if (zoneShape.equalsIgnoreCase("Cuboid"))
								{
									if (coords.length == 2)
									{
										zoneForm = new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ);
									}
									else
									{
										LOG.warn("{}: ZoneData: Missing cuboid vertex in sql data for zone: {} in file {}!", getClass().getSimpleName(), zoneId, f.getName());
										continue;
									}
								}
								else if (zoneShape.equalsIgnoreCase("NPoly"))
								{
									// nPoly needs to have at least 3 vertices
									if (coords.length > 2)
									{
										final int[] aX = new int[coords.length];
										final int[] aY = new int[coords.length];
										for (int i = 0; i < coords.length; i++)
										{
											aX[i] = coords[i][0];
											aY[i] = coords[i][1];
										}
										zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
									}
									else
									{
										LOG.warn("{}: ZoneData: Bad data for zone: {} in file {}!", getClass().getSimpleName(), zoneId, f.getName());
										continue;
									}
								}
								else if (zoneShape.equalsIgnoreCase("Cylinder"))
								{
									// A Cylinder zone requires a center point
									// at x,y and a radius
									attrs = d.getAttributes();
									final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
									if ((coords.length == 1) && (zoneRad > 0))
									{
										zoneForm = new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad);
									}
									else
									{
										LOG.warn("{}: ZoneData: Bad data for zone: {} in file {}!", getClass().getSimpleName(), zoneId, f.getName());
										continue;
									}
								}
								else
								{
									LOG.warn("{}: ZoneData: Unknown shape: {}  for zone {} in file {}", getClass().getSimpleName(), zoneShape, zoneId, f.getName());
									continue;
								}
							}
						}
						catch (Exception e)
						{
							LOG.warn("{}: ZoneData: Failed to load zone {} coordinates!", getClass().getSimpleName(), zoneId, e);
						}
						
						// Create the zone
						Class<?> newZone = null;
						Constructor<?> zoneConstructor = null;
						L2ZoneType temp;
						try
						{
							newZone = Class.forName("l2jorion.game.model.zone.type.L2" + zoneType);
							zoneConstructor = newZone.getConstructor(int.class);
							temp = (L2ZoneType) zoneConstructor.newInstance(zoneId);
							temp.setZone(zoneForm);
						}
						catch (Exception e)
						{
							LOG.warn("{}: ZoneData: No such zone type: {} in file {}!", getClass().getSimpleName(), zoneType, f.getName());
							continue;
						}
						
						// Check for additional parameters
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								String name = attrs.getNamedItem("name").getNodeValue();
								String val = attrs.getNamedItem("val").getNodeValue();
								
								temp.setParameter(name, val);
							}
							else if ("spawn".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof L2ZoneRespawn))
							{
								attrs = cd.getAttributes();
								int spawnX = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
								int spawnY = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
								int spawnZ = Integer.parseInt(attrs.getNamedItem("Z").getNodeValue());
								Node val = attrs.getNamedItem("type");
								((L2ZoneRespawn) temp).parseLoc(spawnX, spawnY, spawnZ, val == null ? null : val.getNodeValue());
							}
							else if ("race".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof L2RespawnZone))
							{
								attrs = cd.getAttributes();
								String race = attrs.getNamedItem("name").getNodeValue();
								String point = attrs.getNamedItem("point").getNodeValue();
								
								((L2RespawnZone) temp).addRaceRespawnPoint(race, point);
							}
						}
						
						if (checkId(zoneId))
						{
							LOG.info("{}: Caution: Zone ({}) from file {} overrides previous definition.", getClass().getSimpleName(), zoneId, f.getName());
						}
						
						if (!zoneName.isEmpty())
						{
							temp.setName(zoneName);
						}
						
						addZone(zoneId, temp);
						
						int ax, ay, bx, by;
						for (int x = 0; x < worldRegions.length; x++)
						{
							for (int y = 0; y < worldRegions[x].length; y++)
							{
								ax = (x - L2World.OFFSET_X) << L2World.SHIFT_BY;
								bx = ((x + 1) - L2World.OFFSET_X) << L2World.SHIFT_BY;
								ay = (y - L2World.OFFSET_Y) << L2World.SHIFT_BY;
								by = ((y + 1) - L2World.OFFSET_Y) << L2World.SHIFT_BY;
								
								if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
								{
									worldRegions[x][y].addZone(temp);
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Error while loading zones.", e);
			
		}
		finally
		{
			CloseUtil.close(con);
			
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> void addZone(Integer id, T zone)
	{
		Map<Integer, T> map = (Map<Integer, T>) _classZones.get(zone.getClass());
		if (map == null)
		{
			map = new HashMap<>();
			map.put(id, zone);
			_classZones.put(zone.getClass(), map);
		}
		else
		{
			map.put(id, zone);
		}
	}
	
	@Deprecated
	public Collection<L2ZoneType> getAllZones()
	{
		final List<L2ZoneType> zones = new ArrayList<>();
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			zones.addAll(map.values());
		}
		return zones;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> Collection<T> getAllZones(Class<T> zoneType)
	{
		return (Collection<T>) _classZones.get(zoneType).values();
	}
	
	public L2ZoneType getZoneById(int id)
	{
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return map.get(id);
			}
		}
		return null;
	}
	
	public List<L2ZoneType> getZones(L2Object object)
	{
		return getZones(object.getX(), object.getY(), object.getZ());
	}
	
	public <T extends L2ZoneType> T getZone(L2Object object, Class<T> type)
	{
		if (object == null)
		{
			return null;
		}
		return getZone(object.getX(), object.getY(), object.getZ(), type);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> T getZone(int x, int y, int z, Class<T> type)
	{
		final L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y, z) && type.isInstance(zone))
			{
				return (T) zone;
			}
		}
		return null;
	}
	
	public List<L2ZoneType> getZones(int x, int y)
	{
		L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		List<L2ZoneType> temp = new ArrayList<>();
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y))
			{
				temp.add(zone);
			}
		}
		return temp;
	}
	
	public List<L2ZoneType> getZones(int x, int y, int z)
	{
		L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		List<L2ZoneType> temp = new ArrayList<>();
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y, z))
			{
				temp.add(zone);
			}
		}
		return temp;
	}
	
	public final L2ArenaZone getArena(L2Character character)
	{
		if (character == null)
		{
			return null;
		}
		
		for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
		{
			if (temp instanceof L2ArenaZone && temp.isCharacterInZone(character))
			{
				return ((L2ArenaZone) temp);
			}
		}
		
		return null;
	}
	
	public final L2OlympiadStadiumZone getOlympiadStadium(L2Character character)
	{
		if (character == null)
		{
			return null;
		}
		
		for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
		{
			if (temp instanceof L2OlympiadStadiumZone && temp.isCharacterInZone(character))
			{
				return ((L2OlympiadStadiumZone) temp);
			}
		}
		return null;
	}
	
	public final L2WaterZone getWaterZone(L2Character character)
	{
		if (character == null)
		{
			return null;
		}
		
		for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), (character.getZ() - 100)))
		{
			if (temp instanceof L2WaterZone && temp.isCharacterInZone(character))
			{
				return ((L2WaterZone) temp);
			}
		}
		
		return null;
	}
	
	public int getSize()
	{
		int i = 0;
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			i += map.size();
		}
		return i;
	}
	
	public boolean checkId(int id)
	{
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return true;
			}
		}
		return false;
	}
	
	public static AbstractZoneSettings getSettings(String name)
	{
		return _settings.get(name);
	}
	
	public static final ZoneManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ZoneManager _instance = new ZoneManager();
	}
}
