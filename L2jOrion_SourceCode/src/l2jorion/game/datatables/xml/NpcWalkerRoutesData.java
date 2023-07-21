package l2jorion.game.datatables.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.game.model.L2NpcWalkerNode;
import l2jorion.game.templates.StatsSet;
import l2jorion.util.xml.IXmlReader;

public class NpcWalkerRoutesData implements IXmlReader
{
	protected static final Logger LOGGER = Logger.getLogger(NpcWalkerRoutesData.class.getName());
	
	private final Map<Integer, List<L2NpcWalkerNode>> _routes = new HashMap<>();
	
	protected NpcWalkerRoutesData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_routes.clear();
		parseDatapackFile("data/xml/walkerRoutes.xml");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		try
		{
			final Node n = doc.getFirstChild();
			for (Node node = n.getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("route".equalsIgnoreCase(node.getNodeName()))
				{
					final List<L2NpcWalkerNode> points = new ArrayList<>();
					for (Node b = node.getFirstChild(); b != null; b = b.getNextSibling())
					{
						if (!"point".equalsIgnoreCase(b.getNodeName()))
						{
							continue;
						}
						
						final StatsSet set = new StatsSet();
						final NamedNodeMap attrs = b.getAttributes();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node attr = attrs.item(i);
							set.set(attr.getNodeName(), attr.getNodeValue());
						}
						
						final L2NpcWalkerNode route = new L2NpcWalkerNode();
						route.setMoveX(set.getInteger("x"));
						route.setMoveY(set.getInteger("y"));
						route.setMoveZ(set.getInteger("z"));
						route.setDelay(set.getInteger("delay"));
						route.setRunning(set.getBool("run"));
						route.setChatText(set.getString("chat", null));
						points.add(route);
					}
					_routes.put(Integer.parseInt(node.getAttributes().getNamedItem("npcId").getNodeValue()), points);
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _routes.size() + " walker routes.");
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Error while reading walker route data: " + e);
		}
	}
	
	public List<L2NpcWalkerNode> getRouteForNpc(int id)
	{
		return _routes.get(id);
	}
	
	public static NpcWalkerRoutesData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcWalkerRoutesData INSTANCE = new NpcWalkerRoutesData();
	}
}