package l2jorion.game.model.actor.instance;

import java.util.List;
import java.util.StringTokenizer;

import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.enums.AchType;
import l2jorion.game.managers.AchievementManager;
import l2jorion.game.model.AchievementHolder;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Util;

public final class L2AchievementInstance extends L2FolkInstance
{
	private final static int PAGE_LIMIT = Config.PAGE_LIMIT;
	
	public L2AchievementInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		
		if (currentCommand.startsWith("Daily"))
		{
			int val = Integer.parseInt(st.nextToken());
			showDailyMissions(player, val);
		}
		else if (currentCommand.startsWith("Chat"))
		{
			int val = Integer.parseInt(st.nextToken());
			showChatWindow(player, val);
		}
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(player, getNpcId(), 0));
		html.replace("%objectId%", getObjectId());
		html.replace("%list%", getList(player, val));
		player.sendPacket(html);
	}
	
	public void showDailyMissions(L2PcInstance player, int val)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/achievements/daily.htm");
		html.replace("%objectId%", getObjectId());
		html.replace("%list%", getDailyList(player, val));
		player.sendPacket(html);
	}
	
	public static int countPagesNumber(int objectsSize, int pageSize)
	{
		return objectsSize / pageSize + (objectsSize % pageSize == 0 ? 0 : 1);
	}
	
	private String getList(L2PcInstance player, int page)
	{
		// Retrieve the entire types list based on group type.
		List<AchType> list = AchievementManager.getInstance().getTypeList(player);
		
		// Calculate page number.
		final int max = countPagesNumber(list.size(), PAGE_LIMIT);
		page = page > max ? max : page < 1 ? 1 : page;
		
		// Cut skills list up to page number.
		list = list.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, list.size()));
		
		final StringBuilder sb = new StringBuilder();
		
		int row = 0;
		for (AchType type : list)
		{
			
			boolean exist = player.getAchievement().getData().containsKey(type);
			boolean completed = false;
			int getLevel = exist ? player.getAchievement().getData().get(type).getId() - 1 : 0;
			int getCount = exist ? player.getAchievement().getData().get(type).getValue() : 0;
			int allStages = AchievementManager.getInstance().getStages(type).size();
			
			if (AchievementManager.getInstance().getStages(type).size() < (getLevel + 1))
			{
				getLevel = AchievementManager.getInstance().getStages(type).size() - 1;
				completed = true;
			}
			
			AchievementHolder ach = AchievementManager.getInstance().getAchievements().get(type).get(getLevel);
			
			sb.append("<img src=l2ui.squareblack width=296 height=1><table cellspacing=1 cellpadding=1 width=296 bgcolor=000000><tr>");
			sb.append("<td width=40 height=40 align=center><button width=32 height=32 back=" + ach.getIcon() + " fore=" + ach.getIcon() + "></td>");
			sb.append("<td width=256>" + ach.getName().toUpperCase() + " " + (completed ? "<font color=00FF00>Completed</font>" : "") + "<br1>");
			sb.append("Stage:<font color=LEVEL>" + ach.getLevel() + "/" + allStages + "</font> Reward:<font color=LEVEL>" + Util.formatAdena(ach.getRewardCount()) + "</font> " + L2Item.getItemNameById(ach.getRewardId()) + "<br1>");
			sb.append("<font color=B09878>" + (completed ? " " : ach.getDescription().replaceAll("%required%", getCount + "/" + ach.getRequired())) + "</font></td></tr></table>");
			sb.append("<table cellspacing=0 cellpadding=0 width=300 height=15><tr><td>" + generateBar(300, 15, completed ? ach.getRequired() : getCount, ach.getRequired(), false) + "</td></tr></table>");
			sb.append("<img src=l2ui.squareblack width=296 height=1><img height=3>");
			row++;
		}
		
		for (int i = PAGE_LIMIT; i > row; i--)
		{
			sb.append("<img height=47>");
		}
		
		sb.append("<table width=300><tr>");
		sb.append("<td align=left width=100>" + (page > 1 ? "<button value=\"Prev\" action=\"bypass npc_" + getObjectId() + "_Chat " + (page - 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=100>Page " + page + "/" + max + "</td>");
		sb.append("<td align=right width=100>" + (page < max ? "<button value=\"Next\" action=\"bypass npc_" + getObjectId() + "_Chat " + (page + 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		return sb.toString();
	}
	
	private String getDailyList(L2PcInstance player, int page)
	{
		// Retrieve the entire types list based on group type.
		List<AchType> list = AchievementManager.getInstance().getDailyTypeList(player);
		
		// Calculate page number.
		final int max = countPagesNumber(list.size(), PAGE_LIMIT);
		page = page > max ? max : page < 1 ? 1 : page;
		
		// Cut skills list up to page number.
		list = list.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, list.size()));
		
		final StringBuilder sb = new StringBuilder();
		
		int row = 0;
		for (AchType type : list)
		{
			boolean exist = player.getAchievement().getData().containsKey(type);
			boolean completed = false;
			int getLevel = exist ? player.getAchievement().getData().get(type).getId() - 1 : 0;
			int getCount = exist ? player.getAchievement().getData().get(type).getValue() : 0;
			
			if (AchievementManager.getInstance().getStages(type).size() < (getLevel + 1))
			{
				getLevel = AchievementManager.getInstance().getStages(type).size() - 1;
				completed = true;
			}
			
			AchievementHolder ach = AchievementManager.getInstance().getAchievements().get(type).get(getLevel);
			
			L2NpcTemplate npc = null;
			if (ach.getNpcId() != 0)
			{
				npc = NpcTable.getInstance().getTemplate(ach.getNpcId());
			}
			
			// Needed rework for description, because of few values
			String desc = (ach.getDescription().replace("%required%", getCount + "/" + ach.getRequired())//
				.replace("%npcName%", (npc == null ? "" : String.valueOf(npc.getName())))//
				.replace("%level%", (npc == null ? "" : String.valueOf(npc.getLevel()))) //
				.replace("%itemName%", String.valueOf(L2Item.getItemNameById(ach.getItemId()))));
			
			sb.append("<img src=l2ui.squareblack width=296 height=1><table cellspacing=1 cellpadding=1 width=296 bgcolor=000000><tr>");
			sb.append("<td width=40 height=40 align=center><button width=32 height=32 back=" + ach.getIcon() + " fore=" + ach.getIcon() + "></td>");
			sb.append("<td width=256>" + ach.getName().toUpperCase() + " " + (completed ? "<font color=00FF00>Completed</font>" : "") + "<br1>");
			sb.append("Reward:<font color=LEVEL>" + Util.formatAdena(ach.getRewardCount()) + "</font> " + L2Item.getItemNameById(ach.getRewardId()) + "<br1>");
			sb.append("<font color=B09878>" + (completed ? " " : desc) + "</font></td></tr></table>");
			sb.append("<table cellspacing=0 cellpadding=0 width=300 height=15><tr><td>" + generateBar(300, 15, completed ? ach.getRequired() : getCount, ach.getRequired(), true) + "</td></tr></table>");
			sb.append("<img src=l2ui.squareblack width=296 height=1><img height=3>");
			row++;
		}
		
		for (int i = PAGE_LIMIT; i > row; i--)
		{
			sb.append("<img height=47>");
		}
		
		sb.append("<table width=300><tr>");
		sb.append("<td align=left width=100>" + (page > 1 ? "<button value=\"Prev\" action=\"bypass npc_" + getObjectId() + "_Daily " + (page - 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("<td align=center width=100>Page " + page + "/" + max + "</td>");
		sb.append("<td align=right width=100>" + (page < max ? "<button value=\"Next\" action=\"bypass npc_" + getObjectId() + "_Daily " + (page + 1) + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>" : "") + "</td>");
		sb.append("</tr></table>");
		
		return sb.toString();
	}
	
	public String generateBar(int width, int height, int current, int max, boolean daily)
	{
		final StringBuilder sb = new StringBuilder();
		current = current > max ? max : current;
		int bar = Math.max((width * (current * 100 / max) / 100), 0);
		sb.append("<table width=" + width + " cellspacing=0 cellpadding=0><tr><td width=" + bar + " align=center><img src=L2UI_CH3.BR_BAR1_" + (daily ? "MP" : "CP") + " width=" + bar + " height=" + height + "/></td>");
		sb.append("<td width=" + (width - bar) + " align=center><img src=L2UI_CH3.BR_BAR1_" + (daily ? "MP" : "CP") + "1 width=" + (width - bar) + " height=" + height + "/></td></tr></table>");
		return sb.toString();
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		return "data/html/achievements/" + npcId + "" + (val == 0 ? "" : "-" + val) + ".htm";
	}
}