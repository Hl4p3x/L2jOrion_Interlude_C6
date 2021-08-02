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
import java.util.List;
import java.util.StringTokenizer;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.sql.SkillTreeTable;
import l2jorion.game.model.FishData;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class FishTable
{
	private static Logger LOG = LoggerFactory.getLogger(SkillTreeTable.class);
	private static final FishTable _instance = new FishTable();
	
	private static List<FishData> _fishsNormal;
	private static List<FishData> _fishsEasy;
	private static List<FishData> _fishsHard;
	public static FishData fish;
	
	public static FishTable getInstance()
	{
		return _instance;
	}
	
	private FishTable()
	{
		int count = 0;
		
		FileReader reader = null;
		BufferedReader buff = null;
		LineNumberReader lnr = null;
		
		try
		{
			final File fileData = new File(Config.DATAPACK_ROOT + "/data/csv/fish.csv");
			
			reader = new FileReader(fileData);
			buff = new BufferedReader(reader);
			lnr = new LineNumberReader(buff);
			
			String line = null;
			
			_fishsEasy = new FastList<>();
			_fishsNormal = new FastList<>();
			_fishsHard = new FastList<>();
			FishData fish;
			
			// format:
			// id;level;name;hp;hpregen;fish_type;fish_group;fish_guts;guts_check_time;wait_time;combat_time
			while ((line = lnr.readLine()) != null)
			{
				// ignore comments
				if (line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}
				
				final StringTokenizer st = new StringTokenizer(line, ";");
				
				final int id = Integer.parseInt(st.nextToken());
				final int lvl = Integer.parseInt(st.nextToken());
				final String name = st.nextToken();
				final int hp = Integer.parseInt(st.nextToken());
				final int hpreg = Integer.parseInt(st.nextToken());
				final int type = Integer.parseInt(st.nextToken());
				final int group = Integer.parseInt(st.nextToken());
				final int fish_guts = Integer.parseInt(st.nextToken());
				final int guts_check_time = Integer.parseInt(st.nextToken());
				final int wait_time = Integer.parseInt(st.nextToken());
				final int combat_time = Integer.parseInt(st.nextToken());
				
				fish = new FishData(id, lvl, name, hp, hpreg, type, group, fish_guts, guts_check_time, wait_time, combat_time);
				switch (fish.getGroup())
				{
					case 0:
						_fishsEasy.add(fish);
						break;
					case 1:
						_fishsNormal.add(fish);
						break;
					case 2:
						_fishsHard.add(fish);
				}
			}
			
			count = _fishsEasy.size() + _fishsNormal.size() + _fishsHard.size();
			
		}
		catch (final FileNotFoundException e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				e.printStackTrace();
			
			LOG.warn("fish.csv is missing in data folder");
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
		LOG.info("FishTable: Loaded " + count + " fishes");
		
	}
	
	/**
	 * @param lvl
	 * @param type
	 * @param group
	 * @return List of Fish that can be fished
	 */
	public List<FishData> getfish(final int lvl, final int type, final int group)
	{
		final List<FishData> result = new FastList<>();
		List<FishData> _Fishs = null;
		
		switch (group)
		{
			case 0:
				_Fishs = _fishsEasy;
				break;
			case 1:
				_Fishs = _fishsNormal;
				break;
			case 2:
				_Fishs = _fishsHard;
		}
		if (_Fishs == null)
		{
			// the fish list is empty
			LOG.warn("Fish are not defined !");
			return null;
		}
		for (final FishData f : _Fishs)
		{
			if (f.getLevel() != lvl)
			{
				continue;
			}
			
			if (f.getType() != type)
			{
				continue;
			}
			
			result.add(f);
		}
		if (result.size() == 0)
		{
			LOG.warn("Cant Find Any Fish!? - Lvl: " + lvl + " Type: " + type);
		}
		
		return result;
	}
	
}
