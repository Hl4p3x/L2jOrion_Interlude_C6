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
package l2jorion.game.handler.item;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.handler.IItemHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.ClanHallManager;
import l2jorion.game.managers.FortManager;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PlayableInstance;
import l2jorion.game.model.entity.event.CTF;
import l2jorion.game.model.entity.event.DM;
import l2jorion.game.model.entity.event.TvT;
import l2jorion.game.model.entity.event.VIP;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.SetupGauge;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.ThreadPoolManager;

public class ScrollOfEscape implements IItemHandler
{
	// all the items ids that this handler knowns
	private static final int[] ITEM_IDS =
	{
		736,
		1830,
		1829,
		1538,
		3958,
		5858,
		5859,
		7117,
		7118,
		7119,
		7120,
		7121,
		7122,
		7123,
		7124,
		7125,
		7126,
		7127,
		7128,
		7129,
		7130,
		7131,
		7132,
		7133,
		7134,
		7135,
		7554,
		7555,
		7556,
		7557,
		7558,
		7559,
		7618,
		7619,
		9760,
		10015
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		L2PcInstance activeChar = (L2PcInstance) playable;
		
		if (checkConditions(activeChar))
		{
			return;
		}
		
		// Check to see if player is sitting
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return;
		}
		
		// if(activeChar._inEventTvT && TvT._started)
		if (activeChar._inEventTvT && TvT.is_started())
		{
			activeChar.sendMessage("You can't use Scroll of Escape in TvT.");
			return;
		}
		
		// if(activeChar._inEventDM && DM._started)
		if (activeChar._inEventDM && DM.is_started())
		{
			activeChar.sendMessage("You can't use Scroll of Escape in DM.");
			return;
		}
		
		// if(activeChar._inEventCTF && CTF._started)
		if (activeChar._inEventCTF && CTF.is_started())
		{
			activeChar.sendMessage("You can't use Scroll of Escape in CTF.");
			return;
		}
		
		// if(activeChar._inEventVIP && VIP._started)
		if (activeChar._inEventVIP && VIP._started)
		{
			activeChar.sendMessage("You can't use Scroll of Escape in VIP.");
			return;
		}
		
		// not usefull
		/*
		 * if(GrandBossManager.getInstance().getZone(activeChar) != null && !activeChar.isGM()) { activeChar.sendMessage("You Can't Use SOE In Grand boss zone!"); return; }
		 */
		
		// Check to see if player is on olympiad
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}
		
		if (!Config.ALLOW_SOE_IN_PVP && activeChar.getPvpFlag() != 0)
		{
			activeChar.sendMessage("You can't use SOE in PvP.");
			return;
		}
		
		// Check to see if the player is in a festival.
		if (activeChar.isFestivalParticipant())
		{
			activeChar.sendPacket(SystemMessage.sendString("You may not use an escape skill in a festival."));
			return;
		}
		
		// Check to see if player is in jail
		if (activeChar.isInJail())
		{
			activeChar.sendPacket(SystemMessage.sendString("You can not escape from jail."));
			return;
		}
		
		// Check to see if player is in a duel
		if (activeChar.isInDuel())
		{
			activeChar.sendPacket(SystemMessage.sendString("You cannot use escape skills during a duel."));
			return;
		}
		
		if (activeChar.isParalyzed())
		{
			activeChar.sendPacket(SystemMessage.sendString("You may not use an escape skill in a paralyzed."));
			return;
		}
		
		if (activeChar.isOutOfControl())
		{
			return;
		}
		
		// activeChar.abortCast();
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		// SoE Animation section
		// Check if this is a blessed scroll, if it is then shorten the cast time.
		final int itemId = item.getItemId();
		
		final SystemMessage sm3 = new SystemMessage(SystemMessageId.USE_S1);
		sm3.addItemName(itemId);
		activeChar.sendPacket(sm3);
		
		final int escapeSkill = itemId == 1538 || itemId == 5858 || itemId == 5859 || itemId == 3958 || itemId == 10130 ? 2036 : 2013;
		
		if (itemId != 9760 && itemId != 10015) // customer's item
		{
			if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
			{
				return;
			}
		}
		
		activeChar.disableAllSkills();
		
		// fix soe
		L2Object oldtarget = activeChar.getTarget();
		activeChar.setTarget(activeChar);
		
		final L2Skill skill = SkillTable.getInstance().getInfo(escapeSkill, 1);
		final MagicSkillUser msu = new MagicSkillUser(activeChar, escapeSkill, 1, skill.getHitTime(), 0);
		activeChar.broadcastPacket(msu);
		activeChar.setTarget(oldtarget);
		SetupGauge sg = new SetupGauge(0, skill.getHitTime());
		activeChar.sendPacket(sg);
		
		// End SoE Animation section
		activeChar.setTarget(null);
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(itemId);
		activeChar.sendPacket(sm);
		
		EscapeFinalizer ef = new EscapeFinalizer(activeChar, itemId);
		// continue execution later
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(ef, skill.getHitTime()));
		activeChar.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + skill.getHitTime() / GameTimeController.MILLIS_IN_TICK);
	}
	
	static class EscapeFinalizer implements Runnable
	{
		private final L2PcInstance _activeChar;
		private final int _itemId;
		
		EscapeFinalizer(final L2PcInstance activeChar, final int itemId)
		{
			_activeChar = activeChar;
			_itemId = itemId;
		}
		
		@Override
		public void run()
		{
			if (_activeChar.isDead())
			{
				return;
			}
			
			_activeChar.enableAllSkills();
			
			_activeChar.setIsIn7sDungeon(false);
			
			try
			{
				if (_itemId == 10015)
				{
					_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				else if (_itemId == 9760)
				{
					_activeChar.teleToLocation(-52307, 141567, -2922, true); // custom place
				}
				// escape to castle if own's one
				else if ((_itemId == 1830 || _itemId == 5859))
				{
					if (CastleManager.getInstance().getCastleByOwner(_activeChar.getClan()) != null)
					{
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Castle);
					}
					else
					{
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
				}
				// escape to fortress if own's one if own's one
				else if ((_itemId == 1830 || _itemId == 5859))
				{
					if (FortManager.getInstance().getFortByOwner(_activeChar.getClan()) != null)
					{
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Fortress);
					}
					else
					{
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
				}
				else if ((_itemId == 1829 || _itemId == 5858) && _activeChar.getClan() != null && ClanHallManager.getInstance().getAbstractHallByOwner(_activeChar.getClan()) != null) // escape to clan hall if own's one
				{
					_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.ClanHall);
				}
				else if (_itemId == 5858) // do nothing
				{
					_activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_HAS_NO_CLAN_HALL));
					return;
				}
				else if (_activeChar.getKarma() > 0 && Config.ALT_KARMA_TELEPORT_TO_FLORAN)
				{
					_activeChar.teleToLocation(17836, 170178, -3507, true); // Floran
					return;
				}
				else
				{
					if (_itemId < 7117)
					{
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
					else
					{
						switch (_itemId)
						{
							case 7117:
								_activeChar.teleToLocation(-84318, 244579, -3730, true); // Talking Island
								break;
							case 7554:
								_activeChar.teleToLocation(-84318, 244579, -3730, true); // Talking Island quest scroll
								break;
							case 7118:
								_activeChar.teleToLocation(46934, 51467, -2977, true); // Elven Village
								break;
							case 7555:
								_activeChar.teleToLocation(46934, 51467, -2977, true); // Elven Village quest scroll
								break;
							case 7119:
								_activeChar.teleToLocation(9745, 15606, -4574, true); // Dark Elven Village
								break;
							case 7556:
								_activeChar.teleToLocation(9745, 15606, -4574, true); // Dark Elven Village quest scroll
								break;
							case 7120:
								_activeChar.teleToLocation(-44836, -112524, -235, true); // Orc Village
								break;
							case 7557:
								_activeChar.teleToLocation(-44836, -112524, -235, true); // Orc Village quest scroll
								break;
							case 7121:
								_activeChar.teleToLocation(115113, -178212, -901, true); // Dwarven Village
								break;
							case 7558:
								_activeChar.teleToLocation(115113, -178212, -901, true); // Dwarven Village quest scroll
								break;
							case 7122:
								_activeChar.teleToLocation(-80826, 149775, -3043, true); // Gludin Village
								break;
							case 7123:
								_activeChar.teleToLocation(-12678, 122776, -3116, true); // Gludio Castle Town
								break;
							case 7124:
								_activeChar.teleToLocation(15670, 142983, -2705, true); // Dion Castle Town
								break;
							case 7125:
								_activeChar.teleToLocation(17836, 170178, -3507, true); // Floran
								break;
							case 7126:
								_activeChar.teleToLocation(83400, 147943, -3404, true); // Giran Castle Town
								break;
							case 7559:
								_activeChar.teleToLocation(83400, 147943, -3404, true); // Giran Castle Town quest scroll
								break;
							case 7127:
								_activeChar.teleToLocation(105918, 109759, -3207, true); // Hardin's Private Academy
								break;
							case 7128:
								_activeChar.teleToLocation(111409, 219364, -3545, true); // Heine
								break;
							case 7129:
								_activeChar.teleToLocation(82956, 53162, -1495, true); // Oren Castle Town
								break;
							case 7130:
								_activeChar.teleToLocation(85348, 16142, -3699, true); // Ivory Tower
								break;
							case 7131:
								_activeChar.teleToLocation(116819, 76994, -2714, true); // Hunters Village
								break;
							case 7132:
								_activeChar.teleToLocation(146331, 25762, -2018, true); // Aden Castle Town
								break;
							case 7133:
								_activeChar.teleToLocation(147928, -55273, -2734, true); // Goddard Castle Town
								break;
							case 7134:
								_activeChar.teleToLocation(43799, -47727, -798, true); // Rune Castle Town
								break;
							case 7135:
								_activeChar.teleToLocation(87331, -142842, -1317, true); // Schuttgart Castle Town
								break;
							case 7618:
								_activeChar.teleToLocation(149864, -81062, -5618, true); // Ketra Orc Village
								break;
							case 7619:
								_activeChar.teleToLocation(108275, -53785, -2524, true); // Varka Silenos Village
								break;
							default:
								_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
								break;
						}
					}
				}
			}
			catch (final Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	private static boolean checkConditions(final L2PcInstance actor)
	{
		return actor.isStunned() || actor.isSleeping() || actor.isParalyzed() || actor.isFakeDeath() || actor.isTeleporting() || actor.isMuted() || actor.isAlikeDead() || actor.isAllSkillsDisabled();
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
