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
package l2jorion.game.cache;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class CrestCache
{
	private static final Logger LOG = LoggerFactory.getLogger(CrestCache.class);
	
	private static final String CRESTS_DIR = "./data/crests/";
	
	private final Map<Integer, byte[]> _crests;
	private final FileFilter _ddsFilter;
	
	public static enum CrestType
	{
		PLEDGE("Crest_", 256),
		PLEDGE_LARGE("LargeCrest_", 2176),
		ALLY("AllyCrest_", 192);
		
		final String _prefix;
		final int _size;
		
		private CrestType(String prefix, int size)
		{
			_prefix = prefix;
			_size = size;
		}
	}
	
	public static CrestCache getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public CrestCache()
	{
		_crests = new HashMap<>();
		_ddsFilter = new DdsFilter();
		
		load();
	}
	
	public final void reload()
	{
		_crests.clear();
		
		load();
	}
	
	private final void load()
	{
		final File directory = new File(CRESTS_DIR);
		directory.mkdirs();
		
		for (File file : directory.listFiles(_ddsFilter))
		{
			byte[] data;
			try (RandomAccessFile f = new RandomAccessFile(file, "r"))
			{
				data = new byte[(int) f.length()];
				f.readFully(data);
			}
			catch (Exception e)
			{
				LOG.warn("CrestCache: Error loading crest file: " + file.getName());
				continue;
			}
			
			final String fileName = file.getName();
			
			for (CrestType type : CrestType.values())
			{
				if (!fileName.startsWith(type._prefix))
				{
					continue;
				}
				
				_crests.put(Integer.valueOf(fileName.substring(type._prefix.length(), fileName.length() - 4)), data);
			}
		}
		
		LOG.info("CrestCache: Loaded " + _crests.size() + " crest files");
	}
	
	public final byte[] getCrest(CrestType type, int id)
	{
		// get crest data
		byte[] data = _crests.get(id);
		
		// crest data is not required type, return
		if (data == null || data.length != type._size)
			return null;
		
		return data;
	}
	
	public final void removeCrest(CrestType type, int id)
	{
		// get crest data
		byte[] data = _crests.get(id);
		
		// crest data is not required type, return
		if (data == null || data.length != type._size)
			return;
		
		// remove from cache
		_crests.remove(id);
		
		// delete file
		final File file = new File(CRESTS_DIR + type._prefix + id + ".bmp");
		if (!file.delete())
			LOG.warn("Crest Cache: Error deleting crest file: " + file.getName());
	}
	
	public final boolean saveCrest(CrestType type, int id, byte[] data)
	{
		// create file
		File file = new File(CRESTS_DIR + type._prefix + id + ".bmp");
		
		try (FileOutputStream out = new FileOutputStream(file))
		{
			// save crest
			out.write(data);
			
			// put crest to cache
			_crests.put(id, data);
			
			return true;
		}
		catch (IOException e)
		{
			LOG.warn("Crest Cache: Error saving crest file: " + file.getName());
			return false;
		}
	}
	
	private static class SingletonHolder
	{
		protected static final CrestCache _instance = new CrestCache();
	}
	
	protected class DdsFilter implements FileFilter
	{
		@Override
		public boolean accept(File file)
		{
			// client<>server crest transfer is using images in DDS file format (DXT1)
			return file.getName().endsWith(".bmp");
		}
	}
}
