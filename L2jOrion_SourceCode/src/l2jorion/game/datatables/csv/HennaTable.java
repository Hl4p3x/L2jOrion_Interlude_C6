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
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.templates.L2Henna;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class HennaTable
{
	private static Logger LOG = LoggerFactory.getLogger(HennaTable.class);
	
	private static HennaTable _instance;
	
	private final Map<Integer, L2Henna> _henna;
	private final boolean _initialized = true;
	
	public static HennaTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new HennaTable();
		}
		
		return _instance;
	}
	
	private HennaTable()
	{
		_henna = new FastMap<>();
		restoreHennaData();
	}
	
	private void restoreHennaData()
	{
		FileReader reader = null;
		BufferedReader buff = null;
		LineNumberReader lnr = null;
		
		try
		{
			final File fileData = new File(Config.DATAPACK_ROOT + "/data/csv/henna.csv");
			
			reader = new FileReader(fileData);
			buff = new BufferedReader(reader);
			lnr = new LineNumberReader(buff);
			
			String line = null;
			
			while ((line = lnr.readLine()) != null)
			{
				// ignore comments
				if (line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}
				
				final StringTokenizer st = new StringTokenizer(line, ";");
				
				StatsSet hennaDat = new StatsSet();
				final int id = Integer.parseInt(st.nextToken());
				hennaDat.set("symbol_id", id);
				st.nextToken(); // next token...ignore name
				hennaDat.set("dye", Integer.parseInt(st.nextToken()));
				hennaDat.set("amount", Integer.parseInt(st.nextToken()));
				hennaDat.set("price", Integer.parseInt(st.nextToken()));
				hennaDat.set("stat_INT", Integer.parseInt(st.nextToken()));
				hennaDat.set("stat_STR", Integer.parseInt(st.nextToken()));
				hennaDat.set("stat_CON", Integer.parseInt(st.nextToken()));
				hennaDat.set("stat_MEM", Integer.parseInt(st.nextToken()));
				hennaDat.set("stat_DEX", Integer.parseInt(st.nextToken()));
				hennaDat.set("stat_WIT", Integer.parseInt(st.nextToken()));
				
				L2Henna template = new L2Henna(hennaDat);
				_henna.put(id, template);
				hennaDat = null;
				template = null;
			}
			
			LOG.info("HennaTable: Loaded " + _henna.size() + " henna templates");
		}
		catch (final FileNotFoundException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn(Config.DATAPACK_ROOT + "/data/csv/henna.csv is missing in data folder");
		}
		catch (final IOException e0)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e0.printStackTrace();
			
			LOG.warn("Error while creating table: " + e0.getMessage() + "\n" + e0);
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
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	public L2Henna getTemplate(final int id)
	{
		return _henna.get(id);
	}
	
}
