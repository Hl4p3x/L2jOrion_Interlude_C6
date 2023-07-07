package l2jorion.game.datatables.csv;

import java.io.File;
import java.util.Scanner;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.L2SummonItem;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class PetItemsData
{
	private static Logger LOG = LoggerFactory.getLogger(PetItemsData.class);
	
	private final FastMap<Integer, L2SummonItem> _summonitems;
	
	private static PetItemsData _instance;
	
	public static PetItemsData getInstance()
	{
		if (_instance == null)
		{
			_instance = new PetItemsData();
		}
		
		return _instance;
	}
	
	public PetItemsData()
	{
		_summonitems = new FastMap<>();
		
		Scanner s = null;
		
		try
		{
			s = new Scanner(new File(Config.DATAPACK_ROOT + "/data/csv/summon_items.csv"));
			
			int lineCount = 0;
			
			while (s.hasNextLine())
			{
				lineCount++;
				
				String line = s.nextLine();
				
				if (line.startsWith("#"))
				{
					continue;
				}
				else if (line.equals(""))
				{
					continue;
				}
				
				final String[] lineSplit = line.split(";");
				line = null;
				
				boolean ok = true;
				int itemID = 0, npcID = 0;
				byte summonType = 0;
				
				try
				{
					itemID = Integer.parseInt(lineSplit[0]);
					npcID = Integer.parseInt(lineSplit[1]);
					summonType = Byte.parseByte(lineSplit[2]);
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.info("PetItemsData: Error in line " + lineCount + " -> incomplete/invalid data or wrong seperator!");
					LOG.info("		" + line);
					ok = false;
				}
				
				if (!ok)
				{
					continue;
				}
				
				L2SummonItem summonitem = new L2SummonItem(itemID, npcID, summonType);
				_summonitems.put(itemID, summonitem);
				summonitem = null;
			}
			
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.info("PetItemsData: Can not find './data/summon_items.csv'");
		}
		finally
		{
			
			if (s != null)
			{
				s.close();
			}
		}
		
		LOG.info("PetItemsData: Loaded " + _summonitems.size() + " pet items");
	}
	
	public L2SummonItem getSummonItem(final int itemId)
	{
		return _summonitems.get(itemId);
	}
	
	public int[] itemIDs()
	{
		final int size = _summonitems.size();
		final int[] result = new int[size];
		int i = 0;
		
		for (final L2SummonItem si : _summonitems.values())
		{
			result[i] = si.getItemId();
			i++;
		}
		return result;
	}
}
