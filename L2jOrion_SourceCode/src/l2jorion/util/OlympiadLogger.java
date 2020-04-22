/* L2jOrion Project - www.l2jorion.com 
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

/**
 @author ProGramMoS, scoria dev
 version 0.1.1, 2009-04-08
 */

package l2jorion.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.Config;

public class OlympiadLogger
{
	private static final Logger LOG = LoggerFactory.getLogger(OlympiadLogger.class);
	
	public static final void add(final String text, final String cat)
	{
		String date = new SimpleDateFormat("yy.MM.dd H:mm:ss").format(new Date());
		
		new File("log/game").mkdirs();
		final File file = new File("log/game/" + (cat != null ? cat : "_all") + ".txt");
		FileWriter save = null;
		try
		{
			save = new FileWriter(file, true);
			final String out = "[" + date + "] '---': " + text + "\n"; // "+char_name()+"
			save.write(out);
			save.flush();
		}
		catch (final IOException e)
		{
			LOG.warn("saving chat LOGGER failed: " + e);
			e.printStackTrace();
		}
		finally
		{
			
			if (save != null)
				try
				{
					save.close();
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
		}
		
		if (cat != null)
		{
			add(text, null);
		}
		
		date = null;
	}
	
	public static final void Assert(final boolean exp, final String cmt)
	{
		if (exp || !Config.ASSERT)
			return;
		
		LOG.info("Assertion error [" + cmt + "]");
		Thread.dumpStack();
	}
}