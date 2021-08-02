package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;

/**
 * @author ProGramMoS
 */

public class AdminBuffs implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_getbuffs",
		"admin_stopbuff",
		"admin_stopallbuffs",
		"admin_areacancel"
	};
	
	private enum CommandEnum
	{
		admin_getbuffs,
		admin_stopbuff,
		admin_stopallbuffs,
		admin_areacancel
	}
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		
		CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_getbuffs:
				if (st.hasMoreTokens())
				{
					L2PcInstance player = null;
					String playername = st.nextToken();
					
					player = L2World.getInstance().getPlayer(playername);
					
					if (player != null)
					{
						showBuffs(player, activeChar);
						return true;
					}
					
					activeChar.sendMessage("The player " + playername + " is not online.");
					return false;
				}
				else if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
				{
					showBuffs((L2PcInstance) activeChar.getTarget(), activeChar);
					return true;
				}
				else if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2NpcInstance)
				{
					showBuffs((L2NpcInstance) activeChar.getTarget(), activeChar);
					return true;
				}
				else
				{
					return true;
				}
				
			case admin_stopbuff:
				if (st.hasMoreTokens())
				{
					int targetId = Integer.parseInt(st.nextToken());
					
					if (st.hasMoreTokens())
					{
						int SkillId = 0;
						
						try
						{
							SkillId = Integer.parseInt(st.nextToken());
						}
						catch (NumberFormatException e)
						{
							activeChar.sendMessage("Usage: //stopbuff <playername> [skillId] (skillId must be a number)");
							return false;
						}
						
						if (SkillId > 0)
						{
							removeBuff(activeChar, targetId, SkillId);
						}
						else
						{
							activeChar.sendMessage("Usage: //stopbuff <playername> [skillId] (skillId must be a number > 0)");
							return false;
						}
						return true;
					}
					activeChar.sendMessage("Usage: //stopbuff <playername> [skillId]");
					return false;
				}
				activeChar.sendMessage("Usage: //stopbuff <playername> [skillId]");
				return false;
			
			case admin_stopallbuffs:
				
				if (st.hasMoreTokens())
				{
					int targetId = Integer.parseInt(st.nextToken());
					
					if (targetId != 0)
					{
						removeAllBuffs(activeChar, targetId);
						return true;
					}
					activeChar.sendMessage("Usage: //stopallbuffs <playername>");
					
					st = null;
					return false;
				}
				activeChar.sendMessage("Usage: //stopallbuffs <playername>");
				return false;
			case admin_areacancel:
				
				if (st.hasMoreTokens())
				{
					String val = st.nextToken();
					
					int radius = 0;
					
					try
					{
						radius = Integer.parseInt(val);
						
					}
					catch (NumberFormatException e)
					{
						activeChar.sendMessage("Usage: //areacancel <radius> (integer value > 0)");
						st = null;
						val = null;
						
						return false;
					}
					
					if (radius > 0)
					{
						for (L2Character knownChar : activeChar.getKnownList().getKnownCharactersInRadius(radius))
						{
							if (knownChar instanceof L2PcInstance && !knownChar.equals(activeChar))
							{
								knownChar.stopAllEffects();
							}
						}
						
						activeChar.sendMessage("All effects canceled within raidus " + radius);
						return true;
					}
					activeChar.sendMessage("Usage: //areacancel <radius> (integer value > 0)");
					return false;
					
				}
				activeChar.sendMessage("Usage: //areacancel <radius>");
				return false;
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	public void showBuffs(L2Character target, L2PcInstance activeChar)
	{
		TextBuilder html = new TextBuilder();
		
		html.append("<html><center><font color=\"LEVEL\">Effects of " + target.getName() + "</font><center><br>");
		html.append("<table>");
		html.append("<tr><td width=200>Skill</td><td width=70>Action</td></tr>");
		
		L2Effect[] effects = target.getAllEffects();
		
		for (L2Effect e : effects)
		{
			if (e != null)
			{
				html.append("<tr><td>" + e.getSkill().getName() + "</td><td><button value=\"Remove\" action=\"bypass -h admin_stopbuff " + target.getObjectId() + " " + String.valueOf(e.getSkill().getId()) + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			}
		}
		
		html.append("</table><br>");
		html.append("<button value=\"Remove All\" action=\"bypass -h admin_stopallbuffs " + target.getObjectId() + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		html.append("</html>");
		
		NpcHtmlMessage ms = new NpcHtmlMessage(1);
		ms.setHtml(html.toString());
		
		activeChar.sendPacket(ms);
	}
	
	private void removeBuff(L2PcInstance remover, int targetId, int SkillId)
	{
		L2Character target = null;
		try
		{
			target = (L2Character) L2World.getInstance().findObject(targetId);
		}
		catch (Exception e)
		{
		}
		
		if (target != null && SkillId > 0)
		{
			L2Effect[] effects = target.getAllEffects();
			
			for (L2Effect e : effects)
			{
				if (e != null && e.getSkill().getId() == SkillId)
				{
					e.exit(true);
					remover.sendMessage("Removed " + e.getSkill().getName() + " level " + e.getSkill().getLevel() + " from " + target.getName());
				}
			}
			showBuffs(target, remover);
		}
	}
	
	private void removeAllBuffs(L2PcInstance remover, int targetId)
	{
		L2Character target = null;
		try
		{
			target = (L2Character) L2World.getInstance().findObject(targetId);
		}
		catch (Exception e)
		{
		}
		
		if (target != null)
		{
			target.stopAllEffects();
			remover.sendMessage("Removed all effects from " + target.getName());
			showBuffs(target, remover);
		}
		else
		{
			remover.sendMessage("No target");
		}
		
	}
	
}
