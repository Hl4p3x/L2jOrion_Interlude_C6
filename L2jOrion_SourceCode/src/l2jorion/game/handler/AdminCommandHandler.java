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
package l2jorion.game.handler;

import java.util.Arrays;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.sql.AdminCommandAccessRights;
import l2jorion.game.handler.admin.AdminAdmin;
import l2jorion.game.handler.admin.AdminAnnouncements;
import l2jorion.game.handler.admin.AdminBan;
import l2jorion.game.handler.admin.AdminBanHwid;
import l2jorion.game.handler.admin.AdminBuffs;
import l2jorion.game.handler.admin.AdminCHSiege;
import l2jorion.game.handler.admin.AdminCTFEngine;
import l2jorion.game.handler.admin.AdminCache;
import l2jorion.game.handler.admin.AdminChangeAccessLevel;
import l2jorion.game.handler.admin.AdminCharSupervision;
import l2jorion.game.handler.admin.AdminChristmas;
import l2jorion.game.handler.admin.AdminCreateItem;
import l2jorion.game.handler.admin.AdminCursedWeapons;
import l2jorion.game.handler.admin.AdminDMEngine;
import l2jorion.game.handler.admin.AdminDelete;
import l2jorion.game.handler.admin.AdminDonator;
import l2jorion.game.handler.admin.AdminDoorControl;
import l2jorion.game.handler.admin.AdminEditChar;
import l2jorion.game.handler.admin.AdminEditNpc;
import l2jorion.game.handler.admin.AdminEffects;
import l2jorion.game.handler.admin.AdminEnchant;
import l2jorion.game.handler.admin.AdminEventEngine;
import l2jorion.game.handler.admin.AdminExpSp;
import l2jorion.game.handler.admin.AdminFakeOnline;
import l2jorion.game.handler.admin.AdminFightCalculator;
import l2jorion.game.handler.admin.AdminFortSiege;
import l2jorion.game.handler.admin.AdminGeodata;
import l2jorion.game.handler.admin.AdminGm;
import l2jorion.game.handler.admin.AdminGmChat;
import l2jorion.game.handler.admin.AdminHeal;
import l2jorion.game.handler.admin.AdminHelpPage;
import l2jorion.game.handler.admin.AdminInvul;
import l2jorion.game.handler.admin.AdminKick;
import l2jorion.game.handler.admin.AdminKill;
import l2jorion.game.handler.admin.AdminLevel;
import l2jorion.game.handler.admin.AdminLogin;
import l2jorion.game.handler.admin.AdminMammon;
import l2jorion.game.handler.admin.AdminManor;
import l2jorion.game.handler.admin.AdminMassControl;
import l2jorion.game.handler.admin.AdminMassRecall;
import l2jorion.game.handler.admin.AdminMenu;
import l2jorion.game.handler.admin.AdminMobGroup;
import l2jorion.game.handler.admin.AdminMonsterRace;
import l2jorion.game.handler.admin.AdminNoble;
import l2jorion.game.handler.admin.AdminPForge;
import l2jorion.game.handler.admin.AdminPetition;
import l2jorion.game.handler.admin.AdminPhantom;
import l2jorion.game.handler.admin.AdminPledge;
import l2jorion.game.handler.admin.AdminPolymorph;
import l2jorion.game.handler.admin.AdminQuest;
import l2jorion.game.handler.admin.AdminReload;
import l2jorion.game.handler.admin.AdminRepairChar;
import l2jorion.game.handler.admin.AdminRes;
import l2jorion.game.handler.admin.AdminRideWyvern;
import l2jorion.game.handler.admin.AdminScript;
import l2jorion.game.handler.admin.AdminShop;
import l2jorion.game.handler.admin.AdminShutdown;
import l2jorion.game.handler.admin.AdminSiege;
import l2jorion.game.handler.admin.AdminSkill;
import l2jorion.game.handler.admin.AdminSpawn;
import l2jorion.game.handler.admin.AdminTarget;
import l2jorion.game.handler.admin.AdminTeleport;
import l2jorion.game.handler.admin.AdminTest;
import l2jorion.game.handler.admin.AdminTownWar;
import l2jorion.game.handler.admin.AdminTvTEngine;
import l2jorion.game.handler.admin.AdminUnblockIp;
import l2jorion.game.handler.admin.AdminVIPEngine;
import l2jorion.game.handler.admin.AdminWho;
import l2jorion.game.handler.admin.AdminZone;
import l2jorion.game.handler.admin.Hero;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class AdminCommandHandler
{
	protected static final Logger LOG = LoggerFactory.getLogger(AdminCommandHandler.class.getName());
	
	private static AdminCommandHandler _instance;
	
	private FastMap<String, IAdminCommandHandler> _datatable;
	
	public static AdminCommandHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new AdminCommandHandler();
		}
		return _instance;
	}
	
	private AdminCommandHandler()
	{
		_datatable = new FastMap<>();
		
		if (Config.L2JGUARD_PROTECTION)
		{
			registerAdminCommandHandler(new AdminBanHwid());
		}
		
		registerAdminCommandHandler(new AdminAdmin());
		registerAdminCommandHandler(new AdminInvul());
		registerAdminCommandHandler(new AdminCHSiege());
		registerAdminCommandHandler(new AdminDelete());
		registerAdminCommandHandler(new AdminKill());
		registerAdminCommandHandler(new AdminFakeOnline());
		registerAdminCommandHandler(new AdminTarget());
		registerAdminCommandHandler(new AdminShop());
		registerAdminCommandHandler(new AdminCTFEngine());
		registerAdminCommandHandler(new AdminVIPEngine());
		registerAdminCommandHandler(new AdminDMEngine());
		registerAdminCommandHandler(new AdminAnnouncements());
		registerAdminCommandHandler(new AdminCreateItem());
		registerAdminCommandHandler(new AdminHeal());
		registerAdminCommandHandler(new AdminHelpPage());
		registerAdminCommandHandler(new AdminShutdown());
		registerAdminCommandHandler(new AdminSpawn());
		registerAdminCommandHandler(new AdminSkill());
		registerAdminCommandHandler(new AdminScript());
		registerAdminCommandHandler(new AdminExpSp());
		registerAdminCommandHandler(new AdminEventEngine());
		registerAdminCommandHandler(new AdminGmChat());
		registerAdminCommandHandler(new AdminEditChar());
		registerAdminCommandHandler(new AdminGm());
		registerAdminCommandHandler(new AdminTeleport());
		registerAdminCommandHandler(new AdminRepairChar());
		registerAdminCommandHandler(new AdminChangeAccessLevel());
		registerAdminCommandHandler(new AdminChristmas());
		registerAdminCommandHandler(new AdminBan());
		registerAdminCommandHandler(new AdminPolymorph());
		// registerAdminCommandHandler(new AdminBanChat());
		registerAdminCommandHandler(new AdminReload());
		registerAdminCommandHandler(new AdminKick());
		registerAdminCommandHandler(new AdminMonsterRace());
		registerAdminCommandHandler(new AdminEditNpc());
		registerAdminCommandHandler(new AdminFightCalculator());
		registerAdminCommandHandler(new AdminSiege());
		registerAdminCommandHandler(new AdminFortSiege());
		registerAdminCommandHandler(new AdminPetition());
		registerAdminCommandHandler(new AdminPForge());
		registerAdminCommandHandler(new AdminEffects());
		registerAdminCommandHandler(new AdminDoorControl());
		registerAdminCommandHandler(new AdminTest());
		registerAdminCommandHandler(new AdminEnchant());
		registerAdminCommandHandler(new AdminMassRecall());
		registerAdminCommandHandler(new AdminMenu());
		registerAdminCommandHandler(new AdminMassControl());
		registerAdminCommandHandler(new AdminMobGroup());
		registerAdminCommandHandler(new AdminRes());
		registerAdminCommandHandler(new AdminMammon());
		registerAdminCommandHandler(new AdminUnblockIp());
		registerAdminCommandHandler(new AdminPledge());
		registerAdminCommandHandler(new AdminRideWyvern());
		registerAdminCommandHandler(new AdminLogin());
		registerAdminCommandHandler(new AdminCache());
		registerAdminCommandHandler(new AdminLevel());
		registerAdminCommandHandler(new AdminQuest());
		registerAdminCommandHandler(new AdminZone());
		registerAdminCommandHandler(new AdminCursedWeapons());
		registerAdminCommandHandler(new AdminGeodata());
		registerAdminCommandHandler(new AdminManor());
		registerAdminCommandHandler(new AdminTownWar());
		registerAdminCommandHandler(new AdminTvTEngine());
		registerAdminCommandHandler(new AdminDonator());
		registerAdminCommandHandler(new AdminNoble());
		registerAdminCommandHandler(new AdminBuffs());
		registerAdminCommandHandler(new AdminCharSupervision());
		registerAdminCommandHandler(new AdminWho());
		registerAdminCommandHandler(new Hero());
		registerAdminCommandHandler(new AdminPhantom());
		
		LOG.info("AdminCommandHandler: Loaded " + _datatable.size() + " handlers");
		
		if (Config.DEBUG)
		{
			String[] commands = new String[_datatable.keySet().size()];
			
			commands = _datatable.keySet().toArray(commands);
			
			Arrays.sort(commands);
			
			for (String command : commands)
			{
				if (AdminCommandAccessRights.getInstance().accessRightForCommand(command) < 0)
				{
					LOG.info("ATTENTION: admin command " + command + " has not an access right");
				}
			}
			
		}
		
	}
	
	public void registerAdminCommandHandler(IAdminCommandHandler handler)
	{
		String[] ids = handler.getAdminCommandList();
		for (String element : ids)
		{
			if (Config.DEBUG)
			{
				LOG.info("Adding handler for command " + element);
			}
			
			if (_datatable.keySet().contains(new String(element)))
			{
				LOG.warn("Duplicated command \"" + element + "\" definition in " + handler.getClass().getName() + ".");
			}
			else
			{
				_datatable.put(element, handler);
			}
		}
	}
	
	public IAdminCommandHandler getAdminCommandHandler(String adminCommand)
	{
		String command = adminCommand;
		
		if (adminCommand.indexOf(" ") != -1)
		{
			command = adminCommand.substring(0, adminCommand.indexOf(" "));
		}
		
		if (Config.DEBUG)
		{
			LOG.info("getting handler for command: " + command + " -> " + (_datatable.get(command) != null));
		}
		
		return _datatable.get(command);
	}
}