/*
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
package l2jorion.game.datatables.xml;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.actor.instance.L2StaticObjectInstance;
import l2jorion.util.xml.IXmlReader;

public class StaticObjectData implements IXmlReader
{
	L2StaticObjectInstance _objects = null;
	
	protected StaticObjectData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/xml/staticobjects.xml");
		LOG.info(StaticObjectData.class.getSimpleName() + ": Loaded.");
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		Node n = doc.getFirstChild();
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equalsIgnoreCase("staticobject"))
			{
				NamedNodeMap node = d.getAttributes();
				
				_objects = new L2StaticObjectInstance(IdFactory.getInstance().getNextId());
				_objects.setType(Integer.valueOf(node.getNamedItem("type").getNodeValue()));
				_objects.setStaticObjectId(Integer.valueOf(node.getNamedItem("id").getNodeValue()));
				_objects.setXYZ(Integer.valueOf(node.getNamedItem("x").getNodeValue()), Integer.valueOf(node.getNamedItem("y").getNodeValue()), Integer.valueOf(node.getNamedItem("z").getNodeValue()));
				_objects.setMap(node.getNamedItem("texture").getNodeValue(), Integer.valueOf(node.getNamedItem("map_x").getNodeValue()), Integer.valueOf(node.getNamedItem("map_y").getNodeValue()));
				_objects.spawnMe();
			}
		}
	}
	
	public static StaticObjectData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final StaticObjectData INSTANCE = new StaticObjectData();
	}
}