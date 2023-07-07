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

package l2jorion.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Log
{
	private static final Logger LOG = LoggerFactory.getLogger(Log.class);
	
	public static final void add(String text, String cat)
	{
		String date = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
		
		new File("log/game").mkdirs();
		File file = new File("log/game/" + cat + ".txt");
		FileWriter save = null;
		try
		{
			save = new FileWriter(file, true);
			String out = "[" + date + "]    >    " + text + "\n";
			save.write(out);
			save.flush();
		}
		catch (IOException e)
		{
			LOG.warn("saving log failed: " + e);
			e.printStackTrace();
		}
		finally
		{
			
			if (save != null)
			{
				try
				{
					save.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public static final void addOlyLog(String text, String cat)
	{
		String date = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
		
		new File("log/olympiad").mkdirs();
		File file = new File("log/olympiad/" + cat + ".txt");
		FileWriter save = null;
		try
		{
			save = new FileWriter(file, true);
			String out = "[" + date + "]    >    " + text + "\n";
			save.write(out);
			save.flush();
		}
		catch (IOException e)
		{
			LOG.warn("saving log failed: " + e);
			e.printStackTrace();
		}
		finally
		{
			
			if (save != null)
			{
				try
				{
					save.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public static final void addLocLog(String text, String cat)
	{
		File file = new File(cat + ".xml");
		FileWriter save = null;
		try
		{
			save = new FileWriter(file, true);
			String out = text + "\n";
			save.write(out);
			save.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (save != null)
			{
				try
				{
					save.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public static final void Assert(boolean exp, String cmt)
	{
		if (exp || !Config.ASSERT)
		{
			return;
		}
		
		LOG.info("Assertion error [" + cmt + "]");
		Thread.dumpStack();
	}
}
