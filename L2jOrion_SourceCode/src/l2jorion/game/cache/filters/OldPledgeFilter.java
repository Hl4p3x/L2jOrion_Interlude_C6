package l2jorion.game.cache.filters;

import java.io.File;
import java.io.FileFilter;

public class OldPledgeFilter implements FileFilter
{
	@Override
	public boolean accept(File file)
	{
		return file.getName().startsWith("Pledge_");
	}
}
