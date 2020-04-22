package l2jorion.game.powerpack.engrave;

import l2jorion.Config;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.model.quest.State;
import l2jorion.game.powerpack.PowerPackConfig;

public class EngraveQuest extends Quest
{
	private static String qn = "8008_Engrave";
	
	public EngraveQuest()
	{
		super(8008, qn, "Engrave");
		setInitialState(new State("Start", this));
		if (PowerPackConfig.SPAWN_ENGRAVER)
		{
			addSpawn(50018, PowerPackConfig.ENGRAVER_X, PowerPackConfig.ENGRAVER_Y, PowerPackConfig.ENGRAVER_Z, 0, false, 0);
			LOG.info("...spawned engraver");
		}
		addStartNpc(50018);
		addTalkId(50018);
	}
	
	private interface CondChecker
	{
		public boolean check(L2ItemInstance item, L2PcInstance player);
	}
	
	private String buildList(final L2PcInstance player, final int startWith, final String baseAction, final String action, final CondChecker checker)
	{
		String htm = "<table width=300>";
		int i = 0;
		int numadded = 0;
		boolean endreached = true;
		for (final L2ItemInstance it : player.getInventory().getItems())
		{
			if (i++ < startWith)
			{
				continue;
			}
			if (numadded == 20)
			{
				endreached = false;
				break;
			}
			if (checker.check(it, player))
			{
				numadded++;
				htm += "<tr><td><a action=\"bypass -h Quest 8008_Engrave " + action + "_" + it.getObjectId() + "\">" + it.getItemName() + "</a></td></tr>";
			}
		}
		
		htm += "</table>";
		if (!endreached)
		{
			htm += "<br1><center><a action=\"bypass -h Quest 8008_Engrave " + baseAction + "_" + i + "\">more...</a><center>";
		}
		return htm;
	}
	
	@Override
	public String onAdvEvent(final String event, final L2NpcInstance npc, final L2PcInstance player)
	{
		String htm = HtmCache.getInstance().getHtm("data/html/default/50018-3.htm");
		if (event.startsWith("mark") || event.startsWith("clear"))
		{
			final int iPos = event.lastIndexOf("_");
			int objectId = 0;
			if (iPos > 0)
			{
				try
				{
					objectId = Integer.parseInt(event.substring(iPos + 1));
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					return htm;
				}
			}
			else
				return htm;
			final L2ItemInstance it = player.getInventory().getItemByObjectId(objectId);
			if (it != null)
			{
				if (PowerPackConfig.ENGRAVE_PRICE > 0 && PowerPackConfig.ENGRAVE_PRICE_ITEM > 0 && event.startsWith("mark"))
				{
					final L2ItemInstance pit = player.getInventory().getItemByItemId(PowerPackConfig.ENGRAVE_PRICE_ITEM);
					if (pit == null || pit.getCount() < PowerPackConfig.ENGRAVE_PRICE)
					{
						htm = HtmCache.getInstance().getHtm("data/html/default/50018-6.htm");
						htm = htm.replace("%itemname%", ItemTable.getInstance().getTemplate(PowerPackConfig.ENGRAVE_PRICE_ITEM).getName());
						htm = htm.replace("%count%", String.valueOf(PowerPackConfig.ENGRAVE_PRICE));
						return htm;
					}
					player.destroyItemByItemId("use", PowerPackConfig.ENGRAVE_PRICE_ITEM, PowerPackConfig.ENGRAVE_PRICE, npc, true);
				}
				
				if (event.startsWith("mark"))
				{
					EngraveManager.getInstance().engraveItem(it, player);
					EngraveManager.getInstance().logAction(it, player, null, "Engrave");
					htm = HtmCache.getInstance().getHtm("data/html/default/50018-5.htm");
				}
				else
				{
					if (EngraveManager.getInstance().getEngraver(it) == player.getObjectId())
					{
						EngraveManager.getInstance().removeEngravement(it);
						htm = HtmCache.getInstance().getHtm("data/html/default/50018-8.htm");
					}
					
				}
				htm = htm.replace("%item%", it.getItemName());
			}
			
		}
		else if (event.startsWith("cleanup"))
		{
			npc.setBusy(true);
			EngraveManager.getInstance().cleanup(player.getObjectId());
			htm = HtmCache.getInstance().getHtm("data/html/default/50018-9.htm");
			npc.setBusy(false);
		}
		else if (event.startsWith("engrave"))
		{
			if (PowerPackConfig.MAX_ENGRAVED_ITEMS_PER_CHAR != 0)
			{
				if (EngraveManager.getInstance().getMyEngravement(player).size() >= PowerPackConfig.MAX_ENGRAVED_ITEMS_PER_CHAR)
				{
					htm = HtmCache.getInstance().getHtm("data/html/default/50018-7.htm");
					htm = htm.replace("%cnt%", String.valueOf(PowerPackConfig.MAX_ENGRAVED_ITEMS_PER_CHAR));
					return htm;
				}
			}
			final int iPos = event.lastIndexOf("_");
			int startwith = 0;
			if (iPos > 0)
			{
				try
				{
					startwith = Integer.parseInt(event.substring(iPos + 1));
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					return htm;
				}
			}
			htm = HtmCache.getInstance().getHtm("data/html/default/50018-4.htm");
			String caption = "Select a subject for drawing engraving";
			if (PowerPackConfig.ENGRAVE_PRICE > 0 && PowerPackConfig.ENGRAVE_PRICE_ITEM > 0)
			{
				caption += "<br1>It will cost you <font color=\"LEVEL\">" + PowerPackConfig.ENGRAVE_PRICE + " " + ItemTable.getInstance().getTemplate(PowerPackConfig.ENGRAVE_PRICE_ITEM).getName() + "</font>";
				
				final L2ItemInstance it = player.getInventory().getItemByItemId(PowerPackConfig.ENGRAVE_PRICE_ITEM);
				if (it == null || it.getCount() < PowerPackConfig.ENGRAVE_PRICE)
				{
					htm = HtmCache.getInstance().getHtm("data/html/default/50018-6.htm");
					htm = htm.replace("%itemname%", ItemTable.getInstance().getTemplate(PowerPackConfig.ENGRAVE_PRICE_ITEM).getName());
					htm = htm.replace("%count%", String.valueOf(PowerPackConfig.ENGRAVE_PRICE));
					return htm;
				}
			}
			htm = htm.replace("%caption%", caption);
			htm = htm.replace("%list%", buildList(player, startwith, "engrave", "mark", new CondChecker()
			{
				@Override
				public boolean check(final L2ItemInstance item, final L2PcInstance player)
				{
					synchronized (PowerPackConfig.ENGRAVE_EXCLUDED_ITEMS)
					{
						
						return !item.isEquipped() && item.isEquipable() && !item.isShadowItem() && !EngraveManager.getInstance().isEngraved(item.getObjectId()) && !PowerPackConfig.ENGRAVE_EXCLUDED_ITEMS.contains(item.getItemId()) && PowerPackConfig.ENGRAVE_ALLOW_GRADE.contains(item.getItem().getCrystalType());
					}
				}
			}));
		}
		else if (event.startsWith("remove"))
		{
			final int iPos = event.lastIndexOf("_");
			int startwith = 0;
			if (iPos > 0)
			{
				try
				{
					startwith = Integer.parseInt(event.substring(iPos + 1));
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					return htm;
				}
			}
			htm = HtmCache.getInstance().getHtm("data/html/default/50018-4.htm");
			htm = htm.replace("%caption%", "Select the item to remove the engraving:");
			htm = htm.replace("%list%", buildList(player, startwith, "remove", "clear", new CondChecker()
			{
				@Override
				public boolean check(final L2ItemInstance item, final L2PcInstance player)
				{
					return !item.isEquipped() && EngraveManager.getInstance().getEngraver(item) == player.getObjectId();
				}
			}));
			
		}
		else if (event.startsWith("look"))
		{
			final int iPos = event.lastIndexOf("_");
			int objectId = 0;
			if (iPos > 0)
			{
				try
				{
					objectId = Integer.parseInt(event.substring(iPos + 1));
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					return htm;
				}
			}
			else
				return htm;
			
			final int[] iinfo = EngraveManager.getInstance().getItemInfo(objectId);
			if (iinfo == null)
				return htm;
			htm = HtmCache.getInstance().getHtm("data/html/default/50018-4.htm");
			htm = htm.replace("%caption%", "History <font color=\"LEVEL\">" + ItemTable.getInstance().getTemplate(iinfo[1]).getName() + "</font>");
			String list = "<table width=300><tr><td>Date</td><td>Action</td><td>From</td><td>Who</td></tr>";
			for (final String s : EngraveManager.getInstance().getLog(objectId))
			{
				list += s;
			}
			list += "</table>";
			htm = htm.replace("%list%", list);
		}
		else if (event.startsWith("trace"))
		{
			final int iPos = event.lastIndexOf("_");
			int startwith = 0;
			int numadded = 0;
			if (iPos > 0)
			{
				try
				{
					startwith = Integer.parseInt(event.substring(iPos + 1));
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						e.printStackTrace();
					
					return htm;
				}
			}
			htm = HtmCache.getInstance().getHtm("data/html/default/50018-4.htm");
			int i = 0;
			boolean endreaced = true;
			String list = "<table width=300>";
			for (final int[] item : EngraveManager.getInstance().getMyEngravement(player))
			{
				if (i++ < startwith)
				{
					continue;
				}
				list += "<tr><td><a action=\"bypass -h Quest 8008_Engrave look_" + item[0] + "\">" + ItemTable.getInstance().getTemplate(item[1]).getName() + "</a></td></tr>";
				numadded++;
				if (numadded == 20)
				{
					endreaced = false;
					break;
				}
			}
			list += "</table>";
			if (!endreaced)
			{
				list += "<br><center><a action=\"bypass -h Quest 8008_Engrave trace_" + i + "\">More</a></center>";
			}
			htm = htm.replace("%caption%", "Objects, engraved by you<br1>");
			htm = htm.replace("%list%", list);
		}
		return htm;
	}
	
	@Override
	public String onTalk(final L2NpcInstance npc, final L2PcInstance player)
	{
		if (player.getQuestState(qn) == null)
		{
			newQuestState(player);
		}
		return HtmCache.getInstance().getHtm("data/html/default/50018-2.htm");
		
	}
	
}
