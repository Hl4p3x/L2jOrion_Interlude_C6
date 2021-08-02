/*
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
package l2jorion.game.powerpack;

import l2jorion.Config;
import l2jorion.game.community.CommunityBoard;
import l2jorion.game.datatables.sql.ItemMarketTable;
import l2jorion.game.handler.VoicedCommandHandler;
import l2jorion.game.handler.custom.CustomBypassHandler;
import l2jorion.game.handler.voice.Bank;
import l2jorion.game.handler.voice.Menu;
import l2jorion.game.handler.voice.PremiumMenu;
import l2jorion.game.handler.voice.Sub;
import l2jorion.game.handler.voice.Vote;
import l2jorion.game.handler.voice.VoteForEvent;
import l2jorion.game.handler.voice.Watch;
import l2jorion.game.powerpack.bossInfo.RaidInfoHandler;
import l2jorion.game.powerpack.buffer.BuffHandler;
import l2jorion.game.powerpack.buffer.BuffTable;
import l2jorion.game.powerpack.engrave.EngraveManager;
import l2jorion.game.powerpack.gatekeeper.GKHandler;
import l2jorion.game.powerpack.other.Market;
import l2jorion.game.powerpack.shop.Shop;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class PowerPack
{
	private static Logger LOG = LoggerFactory.getLogger(PowerPack.class);
	
	private static PowerPack _instance = null;

	public static PowerPack getInstance()
	{
		if(_instance == null)
		{
			_instance = new PowerPack();
		}
		return _instance;
	}

	private PowerPack()
	{
		if (Config.POWERPAK_ENABLED)
		{
			PowerPackConfig.load();
			
			if (PowerPackConfig.BUFFER_ENABLED)
			{
				if ((PowerPackConfig.BUFFER_COMMAND != null && PowerPackConfig.BUFFER_COMMAND.length() > 0) || PowerPackConfig.BUFFER_USEBBS)
				{	
					BuffHandler handler = new BuffHandler();
					if( PowerPackConfig.BUFFER_USECOMMAND && PowerPackConfig.BUFFER_COMMAND != null && PowerPackConfig.BUFFER_COMMAND.length() > 0)
					{
						VoicedCommandHandler.getInstance().registerVoicedCommandHandler(handler);
					}
					
					if (PowerPackConfig.BUFFER_USEBBS)
					{
						CommunityBoard.getInstance().registerBBSHandler(handler);
					}
					CustomBypassHandler.getInstance().registerCustomBypassHandler(handler);
					
				}
				
				LOG.info("Buffer - Enabled");
				BuffTable.getInstance();
			}
			
			if (PowerPackConfig.GLOBALGK_ENABDLED)
			{
				GKHandler handler = new GKHandler();
				if ( PowerPackConfig.GLOBALGK_USECOMMAND && PowerPackConfig.GLOBALGK_COMMAND != null && PowerPackConfig.GLOBALGK_COMMAND.length() > 0)
				{
					VoicedCommandHandler.getInstance().registerVoicedCommandHandler(handler);
				}
				
				if (PowerPackConfig.GLOBALGK_USEBBS)
				{
					CommunityBoard.getInstance().registerBBSHandler(handler);
				}
				
				CustomBypassHandler.getInstance().registerCustomBypassHandler(handler);
				LOG.info("Gatekeeper - Enabled");
			}
			
			if (PowerPackConfig.GMSHOP_ENABLED)
			{
				Shop gs = new Shop();
				CustomBypassHandler.getInstance().registerCustomBypassHandler(gs);
				
				if( PowerPackConfig.GMSHOP_USECOMMAND && PowerPackConfig.GMSHOP_COMMAND != null && PowerPackConfig.GMSHOP_COMMAND.length() > 0)
				{
					VoicedCommandHandler.getInstance().registerVoicedCommandHandler(gs);
				}
				
				if (PowerPackConfig.GMSHOP_USEBBS)
				{
					CommunityBoard.getInstance().registerBBSHandler(gs);
				}
				LOG.info("Shop - Enabled");
			}
			
			if (PowerPackConfig.MARKET_ENABLED)
			{
				Market gm = new Market();
				CustomBypassHandler.getInstance().registerCustomBypassHandler(gm);
				
				if (PowerPackConfig.MARKET_COMMAND != null && PowerPackConfig.MARKET_COMMAND.length() > 0)
				{
					VoicedCommandHandler.getInstance().registerVoicedCommandHandler(gm);
				}
				
				if (PowerPackConfig.MARKET_USEBBS)
				{
					CommunityBoard.getInstance().registerBBSHandler(gm);
				}
				
				LOG.info("Market - Enabled");
				ItemMarketTable.getInstance().load();
			}
			
			if (PowerPackConfig.ENGRAVER_ENABLED)
			{
				EngraveManager.getInstance();
				LOG.info("Engrave System - Enabled");
			}
			
			CustomBypassHandler.getInstance().registerCustomBypassHandler(new Menu());
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Menu());
			LOG.info("Command: .menu - Enabled");
			
			CustomBypassHandler.getInstance().registerCustomBypassHandler(new PremiumMenu());
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new PremiumMenu());
			
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Vote());
			LOG.info("Command: .votereward - Enabled");
			
			if (Config.CUSTOM_SUB_CLASS_COMMAND)
			{
				Sub handle4 = new Sub();
				CustomBypassHandler.getInstance().registerCustomBypassHandler(handle4);
				VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Sub());
				LOG.info("Command: .sub - Enabled");
			}
			
			if (PowerPackConfig.RESPAWN_BOSS)
			{
				CustomBypassHandler.getInstance().registerCustomBypassHandler(new RaidInfoHandler());
				VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new RaidInfoHandler());
				LOG.info("Command: .boss  - Enabled");
			}
			
			if (PowerPackConfig.BANK_SYSTEM)
			{
				CustomBypassHandler.getInstance().registerCustomBypassHandler(new Bank());
				VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Bank());
				LOG.info("Command: .bank  - Enabled");
			}
			
			if (Config.MENU_NEW_STYLE)
			{
				CustomBypassHandler.getInstance().registerCustomBypassHandler(new Watch());
				VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Watch());
				LOG.info("Command: .watch  - Enabled");
				
				CustomBypassHandler.getInstance().registerCustomBypassHandler(new VoteForEvent());
				VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new VoteForEvent());
				LOG.info("Command: .event  - Enabled");
			}
		}
	}
}