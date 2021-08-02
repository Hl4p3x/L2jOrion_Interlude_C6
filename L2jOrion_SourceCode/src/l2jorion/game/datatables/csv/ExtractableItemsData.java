/*
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import l2jorion.Config;
import l2jorion.game.model.L2ExtractableItem;
import l2jorion.game.model.L2ExtractableProductItem;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ExtractableItemsData
{
	protected static final Logger LOG = LoggerFactory.getLogger(ExtractableItemsData.class);
	// Map<itemid, L2ExtractableItem>
	private Map<Integer, L2ExtractableItem> _items;
	
	private static ExtractableItemsData _instance = null;
	
	public static ExtractableItemsData getInstance()
	{
		if (_instance == null)
		{
			_instance = new ExtractableItemsData();
		}
		
		return _instance;
	}
	
	public ExtractableItemsData()
	{
		_items = new HashMap<>();
		
		Scanner s = null;
		try
		{
			s = new Scanner(new File(Config.DATAPACK_ROOT + "/data/csv/extractable_items.csv"));
			
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
				
				String[] lineSplit = line.split(";");
				int itemID = 0;
				try
				{
					itemID = Integer.parseInt(lineSplit[0]);
				}
				catch (Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.error("Extractable items data: Error in line " + lineCount + " -> invalid item id or wrong seperator after item id!");
					LOG.error("		" + line);
				}
				
				List<L2ExtractableProductItem> product_temp = new ArrayList<>(lineSplit.length);
				for (int i = 0; i < lineSplit.length - 1; i++)
				{
					String[] lineSplit2 = lineSplit[i + 1].split(",");
					if (lineSplit2.length != 3)
					{
						LOG.error("Extractable items data: Error in line " + lineCount + " -> wrong seperator!");
						LOG.error("		" + line);
						continue;
					}
					
					int production = 0, amount = 0, chance = 0;
					
					try
					{
						production = Integer.parseInt(lineSplit2[0]);
						amount = Integer.parseInt(lineSplit2[1]);
						chance = Integer.parseInt(lineSplit2[2]);
						lineSplit2 = null;
					}
					catch (Exception e)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
						{
							e.printStackTrace();
						}
						
						LOG.error("Extractable items data: Error in line " + lineCount + " -> incomplete/invalid production data or wrong seperator!");
						LOG.error("		" + line);
						continue;
					}
					
					product_temp.add(new L2ExtractableProductItem(production, amount, chance));
				}
				
				int fullChances = 0;
				for (L2ExtractableProductItem Pi : product_temp)
				{
					fullChances += Pi.getChance();
				}
				
				if (fullChances > 100)
				{
					LOG.error("Extractable items data: Error in line " + lineCount + " -> all chances together are more then 100!");
					LOG.error("		" + line);
					continue;
				}
				
				_items.put(itemID, new L2ExtractableItem(itemID, product_temp));
			}
			
			LOG.info("ExtractableItemsData: Loaded " + _items.size() + " extractable items");
		}
		catch (Exception e)
		{
			// if(Config.ENABLE_ALL_EXCEPTIONS)
			e.printStackTrace();
			
			LOG.error("Extractable items data: Can not find './data/extractable_items.csv'");
			
		}
		finally
		{
			
			if (s != null)
			{
				try
				{
					s.close();
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
		}
	}
	
	public L2ExtractableItem getExtractableItem(int itemID)
	{
		return _items.get(itemID);
	}
	
	public int[] itemIDs()
	{
		int size = _items.size();
		int[] result = new int[size];
		int i = 0;
		for (L2ExtractableItem ei : _items.values())
		{
			result[i] = ei.getItemId();
			i++;
		}
		return result;
	}
}
