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
package l2jorion.game.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;
import l2jorion.Config;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class L2BoxInstance extends L2NpcInstance
{
	private static Logger LOG = LoggerFactory.getLogger(L2BoxInstance.class);
	
	private class L2BoxItem implements Comparable<Object>
	{
		public int itemid;
		public int id;
		public int count;
		public String name;
		
		public L2BoxItem(final int _itemid, final int _count, final String _name, final int _id/* , int _enchant */)
		{
			itemid = _itemid;
			count = _count;
			name = _name;
			id = _id;
		}
		
		@Override
		public int compareTo(final Object o)
		{
			final int r = name.compareToIgnoreCase(((L2BoxItem) o).name);
			if (r != 0)
			{
				return r;
			}
			
			if (id < ((L2BoxItem) o).id)
			{
				return -1;
			}
			
			return 1;
		}
	}
	
	private static final int MAX_ITEMS_PER_PAGE = 25;
	private static final String INSERT_GRANT = "INSERT INTO boxaccess (charname,spawn) VALUES(?,?)";
	private static final String DELETE_GRANT = "DELETE FROM boxaccess WHERE charname=? AND spawn=?";
	private static final String LIST_GRANT = "SELECT charname FROM boxaccess WHERE spawn=?";
	private static final String VARIABLE_PREFIX = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	
	public L2BoxInstance(final int objectId, final L2NpcTemplate _template)
	{
		super(objectId, _template);
	}
	
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		String playerName = player.getName();
		final boolean access = hasAccess(playerName);
		playerName = null;
		
		if (command.startsWith("Withdraw"))
		{
			if (access)
			{
				showWithdrawWindow(player, command.substring(9));
			}
		}
		else if (command.startsWith("Deposit"))
		{
			if (access)
			{
				showDepositWindow(player, command.substring(8));
			}
		}
		else if (command.startsWith("InBox"))
		{
			if (access)
			{
				putInBox(player, command.substring(6));
			}
		}
		else if (command.startsWith("OutBox"))
		{
			if (access)
			{
				takeOutBox(player, command.substring(7));
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	@Override
	public String getHtmlPath(L2PcInstance player, final int npcId, final int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		return "data/html/custom/" + pom + ".htm";
	}
	
	public boolean hasAccess(final String player)
	{
		Connection con = null;
		boolean result = false;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement("SELECT spawn, charname FROM boxaccess WHERE charname=? AND spawn=?");
			st.setString(1, player);
			st.setInt(2, getSpawn().getId());
			ResultSet rs = st.executeQuery();
			
			if (rs.next())
			{
				result = true;
			}
			
			rs.close();
			st.close();
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.info("hasAccess failed: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		return result;
	}
	
	public List<String> getAccess()
	{
		Connection con = null;
		final List<String> acl = new FastList<>();
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement(LIST_GRANT);
			st.setInt(1, getSpawn().getId());
			ResultSet rs = st.executeQuery();
			
			while (rs.next())
			{
				acl.add(rs.getString("charname"));
			}
			rs.close();
			st.close();
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.info("getAccess failed: " + e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		return acl;
	}
	
	public boolean grantAccess(final String player, final boolean what)
	{
		Connection con = null;
		boolean result = false;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			String _query;
			if (what)
			{
				_query = INSERT_GRANT;
			}
			else
			{
				_query = DELETE_GRANT;
			}
			
			PreparedStatement st = con.prepareStatement(_query);
			st.setString(1, player);
			st.setInt(2, getSpawn().getId());
			st.execute();
			st.close();
			st = null;
			_query = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			result = false;
		}
		finally
		{
			CloseUtil.close(con);
		}
		return result;
	}
	
	private void showWithdrawWindow(final L2PcInstance player, final String command)
	{
		String drawername = "trash";
		
		if (command == null)
		{
			return;
		}
		
		String[] cmd = command.split(" ");
		int startPos = 0;
		
		if (cmd != null)
		{
			drawername = cmd[0];
			if (cmd.length > 1)
			{
				startPos = Integer.parseInt(cmd[1]);
			}
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		int nitems = 0;
		Set<L2BoxItem> _items = getItems(drawername);
		
		if (startPos >= _items.size())
		{
			startPos = 0;
		}
		
		String button = "<button value=\"Withdraw\" width=80 height=15 action=\"bypass -h npc_" + getObjectId() + "_OutBox " + drawername;
		String next = "<button value=\"next\" width=50 height=15 action=\"bypass -h npc_" + getObjectId() + "_Withdraw " + drawername + " " + (startPos + MAX_ITEMS_PER_PAGE) + "\">";
		String back = "<button value=\"back\" width=50 height=15 action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">";
		String content = "<html><body>Drawer " + drawername + ":<br>" + next + " " + back + "<table width=\"100%\">";
		content += "<tr><td>Item</td><td>Count</td><td>Withdraw</td></tr>";
		for (final L2BoxItem i : _items)
		{
			nitems++;
			if (nitems < startPos)
			{
				continue;
			}
			
			final String varname = VARIABLE_PREFIX.charAt(nitems - startPos) + String.valueOf(i.itemid);
			content += "<tr><td>" + i.name + "</td><td align=\"right\">" + i.count + "</td>";
			content += "<td><edit var=\"" + varname + "\" width=30></td></tr>";
			button += " ," + varname + " $" + varname;
			
			if (nitems - startPos >= MAX_ITEMS_PER_PAGE)
			{
				break;
			}
		}
		button += "\">";
		content += "</table><br>" + button + "</body></html>";
		LOG.info("setHtml(" + content + "); items=" + nitems);
		html.setHtml(content);
		player.sendPacket(html);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void showDepositWindow(final L2PcInstance player, final String command)
	{
		String drawername = "trash";
		if (command == null)
		{
			return;
		}
		
		String[] cmd = command.split(" ");
		int startPos = 0;
		
		if (cmd != null)
		{
			drawername = cmd[0];
			if (cmd.length > 1)
			{
				startPos = Integer.parseInt(cmd[1]);
			}
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		int nitems = 0;
		Set<L2BoxItem> _items = new FastSet<>();
		for (final L2ItemInstance i : player.getInventory().getItems())
		{
			if (i.getItemId() == 57 || i.isEquipped())
			{
				continue;
			}
			
			final L2BoxItem bi = new L2BoxItem(i.getItemId(), i.getCount(), i.getItem().getName(), i.getObjectId()/* , i.getEnchantLevel() */);
			_items.add(bi);
		}
		
		if (startPos >= _items.size())
		{
			startPos = 0;
		}
		
		String button = "<button value=\"Deposit\" width=80 height=15 action=\"bypass -h npc_" + getObjectId() + "_InBox " + drawername;
		String next = "<button value=\"next\" width=50 height=15 action=\"bypass -h npc_" + getObjectId() + "_Deposit " + drawername + " " + (startPos + MAX_ITEMS_PER_PAGE) + "\">";
		String back = "<button value=\"back\" width=50 height=15 action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">";
		String content = "<html><body>Drawer " + drawername + ":<br>" + next + " " + back + "<table width=\"100%\">";
		content += "<tr><td>Item</td><td>Count</td><td>Deposit</td></tr>";
		
		for (final L2BoxItem i : _items)
		{
			nitems++;
			if (nitems < startPos)
			{
				continue;
			}
			
			final String varname = VARIABLE_PREFIX.charAt(nitems - startPos) + String.valueOf(i.itemid);
			content += "<tr><td>" + i.name + "</td><td align=\"right\">" + i.count + "</td>";
			content += "<td><edit var=\"" + varname + "\" width=30></td></tr>";
			button += " ," + varname + " $" + varname;
			if (nitems - startPos >= MAX_ITEMS_PER_PAGE)
			{
				break;
			}
		}
		
		button += "\">";
		content += "</table><br>" + button + "</body></html>";
		LOG.info("setHtml(" + content + "); items=" + nitems);
		html.setHtml(content);
		player.sendPacket(html);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private Set<L2BoxItem> getItems(final String drawer)
	{
		final Set<L2BoxItem> it = new FastSet<>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, spawn, npcid, drawer, itemid, name, count, enchant FROM boxes where spawn=? and npcid=? and drawer=?");
			statement.setInt(1, getSpawn().getId());
			statement.setInt(2, getNpcId());
			statement.setString(3, drawer);
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				LOG.info("found: itemid=" + rs.getInt("itemid") + ", count=" + rs.getInt("count"));
				it.add(new L2BoxItem(rs.getInt("itemid"), rs.getInt("count"), rs.getString("name"), rs.getInt("id")/* , rs.getInt("enchant") */));
			}
			rs.close();
			DatabaseUtils.close(statement);
			statement = null;
			rs = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.info("getItems failed: " + e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		return it;
	}
	
	private void putInBox(final L2PcInstance player, final String command)
	{/*
		 * NOTE: Item storing in box is currently not implemented String[] cmd = command.split(","); if (cmd.length<=1) return; String drawername = cmd[0]; for (int i = 1; i < cmd.length; i++) { String[] part = cmd[i].split(" "); if (part == null || part.length < 2) continue; try { int id =
		 * Integer.parseInt(part[0].substring(1)); int count = Integer.parseInt(part[1]); if (count <= 0) continue; int realCount = player.getInventory().getItemByItemId(id).getCount(); if (count < realCount) realCount = count; L2ItemInstance item = player.getInventory().destroyItemByItemId("Box",
		 * id, realCount, player, this); // other than previous l2j, destroyItemByItemId does not return the count destroyed // and we cannot just use the returned item as we cannot change the count L2ItemInstance newItem = ItemTable.getInstance().createItem(id); newItem.setCount(realCount);
		 * newItem.setEnchantLevel(item.getEnchantLevel()); putItemInBox(player, drawername, newItem); } catch (Exception e) { LOG.fine("putInBox "+command+" failed: "+e); } } } private void putItemInBox(L2PcInstance player, String drawer, L2ItemInstance item) { String charname = player.getName();
		 * java.sql.Connection con = null; int foundId = 0; int foundCount = 0; try { con = L2DatabaseFactory.getInstance().getConnection(); if (item.isStackable()) { PreparedStatement st2 = con.prepareStatement("SELECT id,count FROM boxes where spawn=? and npcid=? and drawer=? and itemid=?");
		 * st2.setInt(1, getSpawn().getId()); st2.setInt(2, getNpcId()); st2.setString(3, drawer); st2.setInt(4, item.getItemId()); ResultSet rs = st2.executeQuery(); if (rs.next()) { foundId = rs.getInt("id"); foundCount = rs.getInt("count"); } rs.close(); st2.close(); } if (foundCount == 0) {
		 * PreparedStatement statement = con.prepareStatement("INSERT INTO boxes (spawn,npcid,drawer,itemid,name,count,enchant) VALUES(?,?,?,?,?,?,?)"); statement.setInt(1, getSpawn().getId()); statement.setInt(2, getNpcId()); statement.setString(3, drawer); statement.setInt(4, item.getItemId());
		 * statement.setString(5, item.getItem().getName()); statement.setInt(6, item.getCount()); statement.setInt(7, item.getEnchantLevel()); statement.execute(); DatabaseUtils.close(statement); } else { PreparedStatement statement = con.prepareStatement("UPDATE boxes SET count=? WHERE id=?");
		 * statement.setInt(1, foundCount + item.getCount()); statement.setInt(2, foundId); statement.execute(); DatabaseUtils.close(statement); } } catch (Exception e) { LOG.info("could not store item to box "+getSpawn().getId()+"-"+drawer+" for char "+charname); } finally { try { try {
		 * con.close(); } catch(Exception e) { } } catch (Exception e) { //null } }
		 */
	}
	
	private void takeOutBox(final L2PcInstance player, final String command)
	{/*
		 * NOTE: Item storing in box is currently not implemented String[] cmd = command.split(","); if (cmd.length<=1) return; String drawername = cmd[0]; L2BoxItem bi = null; for (int i = 1; i < cmd.length; i++) { String[] part = cmd[i].split(" "); if (part == null || part.length < 2) continue;
		 * try { int id = Integer.parseInt(part[0].substring(1)); int count = Integer.parseInt(part[1]); if (count <= 0) continue; L2ItemInstance item = ItemTable.getInstance().createItem(id); item.setCount(count); bi = takeItemOutBox(player, drawername, item); if (bi.count > 0) {
		 * item.setCount(bi.count); item.setEnchantLevel(bi.enchant); player.getInventory().addItem("Box", item, player, this); } } catch (Exception e) { LOG.fine("takeOutBox "+command+" failed: "+e); } } } private L2BoxItem takeItemOutBox(L2PcInstance player, String drawer, L2ItemInstance item) {
		 * String charname = player.getName(); java.sql.Connection con = null; L2BoxItem bi = new L2BoxItem(); bi.count = 0; try { con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement statement =
		 * con.prepareStatement("SELECT id,count,enchant FROM boxes WHERE spawn=? AND npcid=? AND drawer=? AND itemid=? AND count>=?"); statement.setInt(1, getSpawn().getId()); statement.setInt(2, getNpcId()); statement.setString(3, drawer); statement.setInt(4, item.getItemId()); statement.setInt(5,
		 * item.getCount()); ResultSet rs = statement.executeQuery(); while (rs.next()) { if (rs.getInt("count") == item.getCount()) { bi.count = item.getCount(); bi.itemid = item.getItemId(); bi.enchant = rs.getInt("enchant"); PreparedStatement st2 =
		 * con.prepareStatement("DELETE FROM boxes WHERE id=?"); st2.setInt(1, rs.getInt("id")); st2.execute(); st2.close(); break; } if (rs.getInt("count") > item.getCount()) { bi.count = item.getCount(); bi.itemid = item.getItemId(); bi.enchant = rs.getInt("enchant"); PreparedStatement st2 =
		 * con.prepareStatement("UPDATE boxes SET count=? WHERE id=?"); st2.setInt(1, rs.getInt("count") - bi.count); st2.setInt(2, rs.getInt("id")); st2.execute(); st2.close(); break; } } rs.close(); DatabaseUtils.close(statement); } catch (Exception e) {
		 * LOG.info("could not delete/update item, box "+getSpawn().getId()+"-"+drawer+" for char "+charname+": "+e); } finally { try { try { con.close(); } catch(Exception e) { } } catch (Exception e) { //null } } return bi;
		 */
	}
}
