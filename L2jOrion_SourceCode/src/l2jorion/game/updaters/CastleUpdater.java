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
package l2jorion.game.updaters;

import l2jorion.Config;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.model.ItemContainer;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.log.Log;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class CastleUpdater implements Runnable
{
	protected static Logger LOG = LoggerFactory.getLogger(CastleUpdater.class);
	private final L2Clan _clan;
	private int _runCount = 0;
	
	public CastleUpdater(final L2Clan clan, final int runCount)
	{
		_clan = clan;
		_runCount = runCount;
	}
	
	@Override
	public void run()
	{
		try
		{
			// Move current castle treasury to clan warehouse every 2 hour
			ItemContainer warehouse = _clan.getWarehouse();
			if (warehouse != null && _clan.getHasCastle() > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleById(_clan.getHasCastle());
				if (!Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					if (_runCount % Config.ALT_MANOR_SAVE_PERIOD_RATE == 0)
					{
						castle.saveSeedData();
						castle.saveCropData();
						final String text = "Manor System: all data for " + castle.getName() + " saved";
						Log.add(text, "Manor_system");
					}
				}
				
				_runCount++;
				final CastleUpdater cu = new CastleUpdater(_clan, _runCount);
				ThreadPoolManager.getInstance().scheduleGeneral(cu, 3600000);
			}
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}
}
