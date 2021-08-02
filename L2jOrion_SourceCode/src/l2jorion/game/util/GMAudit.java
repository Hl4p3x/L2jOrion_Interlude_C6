/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class GMAudit
{
	static
	{
		new File("log/GMAudit").mkdirs();
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(Log.class);
	
	public static void auditGMAction(final String gmName, final String action, final String target, final String params)
	{
		final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
		final String today = formatter.format(new Date());
		
		FileWriter save = null;
		try
		{
			final File file = new File("log/GMAudit/" + gmName + ".txt");
			save = new FileWriter(file, true);
			
			final String out = "[" + today + "] --> GM: " + gmName + ", Target: [" + target + "], Action: [" + action + "], Params: [" + params + "] \r\n";
			save.write(out);
		}
		catch (final IOException e)
		{
			LOG.error("GMAudit for GM " + gmName + " could not be saved: ", e);
		}
		finally
		{
			if (save != null)
				try
				{
					save.close();
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
		}
	}
	
	public static void auditGMAction(final String gmName, final String action, final String target)
	{
		auditGMAction(gmName, action, target, "");
	}
}