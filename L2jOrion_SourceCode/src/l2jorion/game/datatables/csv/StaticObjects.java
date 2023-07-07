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
			}
		}
		catch (final FileNotFoundException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("staticobjects.csv is missing in data folder");
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("error while creating StaticObjects table " + e);
		}
		finally
		{
			if (lnr != null)
			{
				try
				{
					lnr.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
			
			if (buff != null)
			{
				try
				{
					buff.close();
				}
				catch (final Exception e1)
				{
					e1.printStackTrace();
				}
			}
			
			if (reader != null)
			{
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
	}
	
	public static L2StaticObjectInstance parse(final String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");
		st.nextToken();
		
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
