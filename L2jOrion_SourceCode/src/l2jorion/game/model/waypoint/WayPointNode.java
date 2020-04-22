/*
 * $Header: WayPointNode.java, 20/07/2005 19:49:29 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 20/07/2005 19:49:29 $
 * $Revision: 1 $
 * $Log: WayPointNode.java,v $
 * Revision 1  20/07/2005 19:49:29  luisantonioa
 * Added copyright notice
 *
 *
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
package l2jorion.game.model.waypoint;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.util.Point3D;

/**
 * This class ...
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */

public class WayPointNode extends L2Object
{
	private int _id;
	private String _title, _type;
	private static final String NORMAL = "Node", SELECTED = "Selected", LINKED = "Linked";
	private static int _lineId = 5560;
	private static final String LINE_TYPE = "item";
	private final Map<WayPointNode, List<WayPointNode>> _linkLists;
	
	/**
	 * @param objectId
	 */
	public WayPointNode(final int objectId)
	{
		super(objectId);
		_linkLists = Collections.synchronizedMap(new WeakHashMap<WayPointNode, List<WayPointNode>>());
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.L2Object#isAutoAttackable(l2jorion.game.model.L2Character)
	 */
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return false;
	}
	
	public static WayPointNode spawn(final String type, final int id, final int x, final int y, final int z)
	{
		final WayPointNode newNode = new WayPointNode(IdFactory.getInstance().getNextId());
		newNode.getPoly().setPolyInfo(type, id + "");
		newNode.spawnMe(x, y, z);
		
		return newNode;
	}
	
	public static WayPointNode spawn(final boolean isItemId, final int id, final L2PcInstance player)
	{
		return spawn(isItemId ? "item" : "npc", id, player.getX(), player.getY(), player.getZ());
	}
	
	public static WayPointNode spawn(final boolean isItemId, final int id, final Point3D point)
	{
		return spawn(isItemId ? "item" : "npc", id, point.getX(), point.getY(), point.getZ());
	}
	
	public static WayPointNode spawn(final Point3D point)
	{
		return spawn(Config.NEW_NODE_TYPE, Config.NEW_NODE_ID, point.getX(), point.getY(), point.getZ());
	}
	
	public static WayPointNode spawn(final L2PcInstance player)
	{
		return spawn(Config.NEW_NODE_TYPE, Config.NEW_NODE_ID, player.getX(), player.getY(), player.getZ());
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (player.getTarget() != this)
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;
		}
	}
	
	public void setNormalInfo(final String type, final int id, final String title)
	{
		_type = type;
		changeID(id, title);
	}
	
	public void setNormalInfo(final String type, final int id)
	{
		_type = type;
		changeID(id);
	}
	
	private void changeID(final int id)
	{
		_id = id;
		toggleVisible();
		toggleVisible();
	}
	
	private void changeID(final int id, final String title)
	{
		setName(title);
		setTitle(title);
		changeID(id);
	}
	
	public void setLinked()
	{
		changeID(Config.LINKED_NODE_ID, LINKED);
	}
	
	public void setNormal()
	{
		changeID(Config.NEW_NODE_ID, NORMAL);
	}
	
	public void setSelected()
	{
		changeID(Config.SELECTED_NODE_ID, SELECTED);
	}
	
	@Override
	public boolean isMarker()
	{
		return true;
	}
	
	public final String getTitle()
	{
		return _title;
	}
	
	public final void setTitle(final String title)
	{
		_title = title;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public void setType(final String type)
	{
		_type = type;
	}
	
	/**
	 * @param nodeA
	 * @param nodeB
	 */
	public static void drawLine(final WayPointNode nodeA, final WayPointNode nodeB)
	{
		int x1 = nodeA.getX(), y1 = nodeA.getY(), z1 = nodeA.getZ();
		final int x2 = nodeB.getX(), y2 = nodeB.getY(), z2 = nodeB.getZ();
		final int modX = x1 - x2 > 0 ? -1 : 1;
		final int modY = y1 - y2 > 0 ? -1 : 1;
		final int modZ = z1 - z2 > 0 ? -1 : 1;
		
		final int diffX = Math.abs(x1 - x2);
		final int diffY = Math.abs(y1 - y2);
		final int diffZ = Math.abs(z1 - z2);
		
		final int distance = (int) Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
		
		final int steps = distance / 40;
		
		List<WayPointNode> lineNodes = new FastList<>();
		
		for (int i = 0; i < steps; i++)
		{
			x1 = x1 + modX * diffX / steps;
			y1 = y1 + modY * diffY / steps;
			z1 = z1 + modZ * diffZ / steps;
			
			lineNodes.add(WayPointNode.spawn(LINE_TYPE, _lineId, x1, y1, z1));
		}
		
		nodeA.addLineInfo(nodeB, lineNodes);
		nodeB.addLineInfo(nodeA, lineNodes);
		
		lineNodes = null;
	}
	
	public void addLineInfo(final WayPointNode node, final List<WayPointNode> line)
	{
		_linkLists.put(node, line);
	}
	
	/**
	 * @param target
	 * @param selectedNode
	 */
	public static void eraseLine(final WayPointNode target, final WayPointNode selectedNode)
	{
		List<WayPointNode> lineNodes = target.getLineInfo(selectedNode);
		
		if (lineNodes == null)
			return;
		
		for (final WayPointNode node : lineNodes)
		{
			node.decayMe();
		}
		
		target.eraseLine(selectedNode);
		selectedNode.eraseLine(target);
		lineNodes = null;
	}
	
	/**
	 * @param target
	 */
	public void eraseLine(final WayPointNode target)
	{
		_linkLists.remove(target);
	}
	
	/**
	 * @param selectedNode
	 * @return
	 */
	private List<WayPointNode> getLineInfo(final WayPointNode selectedNode)
	{
		return _linkLists.get(selectedNode);
	}
	
	public static void setLineId(final int line_id)
	{
		_lineId = line_id;
	}
	
	public List<WayPointNode> getLineNodes()
	{
		final List<WayPointNode> list = new FastList<>();
		
		for (final List<WayPointNode> points : _linkLists.values())
		{
			list.addAll(points);
		}
		
		return list;
	}
}
