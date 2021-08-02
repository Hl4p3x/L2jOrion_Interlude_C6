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
package l2jorion.game.datatables.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.actor.instance.L2StaticObjectInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class StaticObjects
{
	private static Logger LOG = LoggerFactory.getLogger(StaticObjects.class);
	
	private static StaticObjects _instance;
	private final Map<Integer, L2StaticObjectInstance> _staticObjects;
	
	public static StaticObjects getInstance()
	{
		if (_instance == null)
		{
			_instance = new StaticObjects();
		}
		
		return _instance;
	}
	
	public StaticObjects()
	{
		_staticObjects = new FastMap<>();
		parseData();
		LOG.info("StaticObject: Loaded " + _staticObjects.size() + " StaticObject Templates");
	}
	
	private void parseData()
	{
		FileReader reader = null;
		BufferedReader buff = null;
		LineNumberReader lnr = null;
		
		try
		{
			final File doorData = new File(Config.DATAPACK_ROOT, "data/csv/staticobjects.csv");
			
			reader = new FileReader(doorData);
			buff = new BufferedReader(reader);
			lnr = new LineNumberReader(buff);
			
			String line = null;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}
				
				L2StaticObjectInstance obj = parse(line);
				_staticObjects.put(obj.getStaticObjectId(), obj);
				obj = null;
			}
		}
		catch (final FileNotFoundException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("staticobjects.csv is missing in data folder");
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("error while creating StaticObjects table " + e);
		}
		finally
		{
			if (lnr != null)
				try
				{
					lnr.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
			if (buff != null)
				try
				{
					buff.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
			if (reader != null)
				try
				{
					reader.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			
		}
	}
	
	public static L2StaticObjectInstance parse(final String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");
		
		st.nextToken(); // Pass over static object name (not used in server)
		
		final int id = Integer.parseInt(st.nextToken());
		final int x = Integer.parseInt(st.nextToken());
		final int y = Integer.parseInt(st.nextToken());
		final int z = Integer.parseInt(st.nextToken());
		final int type = Integer.parseInt(st.nextToken());
		final String texture = st.nextToken();
		final int map_x = Integer.parseInt(st.nextToken());
		final int map_y = Integer.parseInt(st.nextToken());
		
		final L2StaticObjectInstance obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId());
		obj.setType(type);
		obj.setStaticObjectId(id);
		obj.setXYZ(x, y, z);
		obj.setMap(texture, map_x, map_y);
		obj.spawnMe();
		
		return obj;
	}
}
