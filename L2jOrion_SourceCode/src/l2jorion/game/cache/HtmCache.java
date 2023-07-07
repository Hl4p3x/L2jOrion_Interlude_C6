package l2jorion.game.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2jorion.Config;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.HTMLFilter;

public class HtmCache
{
	private static Logger LOG = LoggerFactory.getLogger(HtmCache.class);
	
	private static HtmCache _instance;
	
	private static final HTMLFilter HTML_FILTER = new HTMLFilter();
	
	private static final Map<String, String> _cache = Config.LAZY_CACHE ? new ConcurrentHashMap<>() : new HashMap<>();
	
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
		_cache.clear();
		_loadedFiles = 0;
		_bytesBuffLen = 0;
	}
	
	public void reload()
	{
		reload(Config.DATAPACK_ROOT);
	}
	
	public void reload(File f)
	{
		if (!Config.LAZY_CACHE)
		{
			LOG.info("Html cache start...");
			parseDir(f);
			LOG.info("HtmCache: " + String.format("%.3f", getMemoryUsage()) + " megabytes on " + getLoadedFiles() + " files loaded");
		}
		else
		{
			_cache.clear();
			_loadedFiles = 0;
			_bytesBuffLen = 0;
			LOG.info("HtmCache: Running lazy cache");
		}
	}
	
	public void reloadPath(final File f)
	{
		parseDir(f);
		LOG.info("HTML Cache: Reloaded specified path");
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
			{
				return file.getName().endsWith(".htm") || file.getName().endsWith(".html");
			}
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
	
	public String loadFile(File file)
	{
		if (!HTML_FILTER.accept(file))
		{
			return null;
		}
		
		final String relpath = Util.getRelativePath(Config.DATAPACK_ROOT, file);
		String content = null;
		try (FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis))
		{
			final int bytes = bis.available();
			byte[] raw = new byte[bytes];
			
			bis.read(raw);
			content = new String(raw, "UTF-8");
			content = content.replaceAll("(?s)<!--.*?-->", ""); // Remove html comments
			
			String oldContent = _cache.put(relpath, content);
			if (oldContent == null)
			{
				_bytesBuffLen += bytes;
				_loadedFiles++;
			}
			else
			{
				_bytesBuffLen = (_bytesBuffLen - oldContent.length()) + bytes;
			}
		}
		catch (Exception e)
		{
			LOG.warn("Problem with htm file {}!", file, e);
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
	
	public String getHtm(String prefix, String path)
	{
		String newPath = null;
		String content;
		
		if ((prefix != null) && !prefix.isEmpty())
		{
			newPath = prefix + path;
			content = getHtm(newPath);
			if (content != null)
			{
				return content;
			}
		}
		
		content = getHtm(path);
		if ((content != null) && (newPath != null))
		{
			_cache.put(newPath, content);
		}
		
		return content;
	}
	
	public String getHtm(String path)
	{
		if ((path == null) || path.isEmpty())
		{
			return ""; // avoid possible NPE
		}
		
		String content = _cache.get(path);
		if (Config.LAZY_CACHE && (content == null))
		{
			content = loadFile(new File(Config.DATAPACK_ROOT, path));
		}
		return content;
	}
	
	public boolean contains(String path)
	{
		return _cache.containsKey(path);
	}
	
	public boolean isLoadable(String path)
	{
		return HTML_FILTER.accept(new File(Config.DATAPACK_ROOT, path));
	}
}
