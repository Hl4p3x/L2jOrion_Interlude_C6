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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Layane
 */
public class HtmCache
{
	private static Logger LOG = LoggerFactory.getLogger(HtmCache.class);
	private static HtmCache _instance;
	
	private final FastMap<Integer, String> _cache;
	
	private int _loadedFiles;
	private long _bytesBuffLen;
	
	public static HtmCache getInstance()
	{
		if (_instance == null)
		{
			_instance = new HtmCache();
		}
		
		return _instance;
	}
	
	public HtmCache()
	{
		_cache = new FastMap<>();
		reload();
	}
	
	public void reload()
	{
		reload(Config.DATAPACK_ROOT);
	}
	
	public void reload(final File f)
	{
		if (!Config.LAZY_CACHE)
		{
			LOG.info("Html cache start...");
			parseDir(f);
			LOG.info("HTML Cache: " + String.format("%.3f", getMemoryUsage()) + " megabytes on " + getLoadedFiles() + " files loaded");
		}
		else
		{
			_cache.clear();
			_loadedFiles = 0;
			_bytesBuffLen = 0;
			LOG.info("HTML Cache: Running cache.");
		}
	}
	
	public void reloadPath(final File f)
	{
		parseDir(f);
		LOG.info("HTML Cache: Reloaded specified path.");
	}
	
	public double getMemoryUsage()
	{
		return (float) _bytesBuffLen / 1048576;
	}
	
	public int getLoadedFiles()
	{
		return _loadedFiles;
	}
	
	class HtmFilter implements FileFilter
	{
		@Override
		public boolean accept(final File file)
		{
			if (!file.isDirectory())
				return file.getName().endsWith(".htm") || file.getName().endsWith(".html");
			return true;
		}
	}
	
	private void parseDir(final File dir)
	{
		FileFilter filter = new HtmFilter();
		File[] files = dir.listFiles(filter);
		
		for (final File file : files)
		{
			if (!file.isDirectory())
			{
				loadFile(file);
			}
			else
			{
				parseDir(file);
			}
		}
		
		files = null;
		filter = null;
	}
	
	public String loadFile(final File file)
	{
		final HtmFilter filter = new HtmFilter();
		
		String content = null;
		
		if (file.exists() && filter.accept(file) && !file.isDirectory())
		{
			FileInputStream fis = null;
			BufferedInputStream bis = null;
			try
			{
				fis = new FileInputStream(file);
				bis = new BufferedInputStream(fis);
				final int bytes = bis.available();
				final byte[] raw = new byte[bytes];
				
				bis.read(raw);
				
				content = new String(raw, "UTF-8");
				content = content.replaceAll("\r\n", "\n");
				
				final String relpath = Util.getRelativePath(Config.DATAPACK_ROOT, file);
				final int hashcode = relpath.hashCode();
				
				final String oldContent = _cache.get(hashcode);
				
				if (oldContent == null)
				{
					_bytesBuffLen += bytes;
					_loadedFiles++;
				}
				else
				{
					_bytesBuffLen = _bytesBuffLen - oldContent.length() + bytes;
				}
				
				_cache.put(hashcode, content);
				
			}
			catch (final Exception e)
			{
				LOG.warn("problem with htm file " + e);
				e.printStackTrace();
			}
			finally
			{
				if (bis != null)
					try
					{
						bis.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
				
				if (fis != null)
					try
					{
						fis.close();
					}
					catch (final Exception e1)
					{
						e1.printStackTrace();
					}
			}
			
		}
		
		return content;
	}
	
	public String getHtmForce(final String path)
	{
		String content = getHtm(path);
		
		if (content == null)
		{
			content = "<html><body>HTML is missing:<br>" + path + "</body></html>";
			LOG.warn("HTML Cache: Missing HTML page: " + path);
		}
		
		return content;
	}
	
	public String getHtm(final String path)
	{
		String content = _cache.get(path.hashCode());
		
		if (Config.LAZY_CACHE && content == null)
		{
			content = loadFile(new File(Config.DATAPACK_ROOT, path));
		}
		
		return content;
	}
	
	public boolean contains(final String path)
	{
		return _cache.containsKey(path.hashCode());
	}
	
	/**
	 * Check if an HTM exists and can be loaded
	 * @param path The path to the HTM
	 * @return
	 */
	public boolean isLoadable(final String path)
	{
		File file = new File(path);
		HtmFilter filter = new HtmFilter();
		
		if (file.exists() && filter.accept(file) && !file.isDirectory())
		{
			return true;
		}
		
		return false;
	}
}
