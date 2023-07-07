package l2jorion.bots.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.bots.model.WalkNode;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;
import l2jorion.util.xml.IXmlReader;

public class botRandomWalk implements IXmlReader
{
	private static final Logger LOG = LoggerFactory.getLogger(botRandomWalk.class);
	
	protected int lastTownId = 1;
	
	private final static Map<Integer, List<WalkNode>> _botRandomWalkNodes = new HashMap<>();
	
	public botRandomWalk()
	{
		load();
	}
	
	public void reload()
	{
		_botRandomWalkNodes.clear();
		
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("./config/bots/botRandomWalk.xml");
		
		LOG.info(getClass().getSimpleName() + ": Loaded " + _botRandomWalkNodes.size() + " bot random walk set(s) ");
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		for (Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
		{
			if ("list".equalsIgnoreCase(list.getNodeName()))
			{
				for (Node skin = list.getFirstChild(); skin != null; skin = skin.getNextSibling())
				{
					if ("bot".equalsIgnoreCase(skin.getNodeName()))
					{
						final NamedNodeMap attrs = skin.getAttributes();
						
						int townId = parseInteger(attrs, "townId");
						
						List<WalkNode> data = new ArrayList<>();
						
						for (Node typeN = skin.getFirstChild(); typeN != null; typeN = typeN.getNextSibling())
						{
							if ("node".equalsIgnoreCase(typeN.getNodeName()))
							{
								final NamedNodeMap attrs2 = typeN.getAttributes();
								
								int x = parseInteger(attrs2, "X");
								int y = parseInteger(attrs2, "Y");
								int z = parseInteger(attrs2, "Z");
								int iterations = parseInteger(attrs2, "iterations");
								
								data.add(new WalkNode(x, y, z, Rnd.get(1, iterations)));
							}
						}
						
						lastTownId = townId;
						_botRandomWalkNodes.put(townId, data);
					}
				}
			}
		}
		
	}
	
	public List<WalkNode> getWalkNode(int townId)
	{
		if (!_botRandomWalkNodes.containsKey(townId))
		{
			return null;
		}
		
		return _botRandomWalkNodes.get(townId);
	}
	
	public Map<Integer, List<WalkNode>> getWalkNodeOptions()
	{
		return _botRandomWalkNodes;
	}
	
	public int getLastTownId()
	{
		return lastTownId;
	}
	
	public static botRandomWalk getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final botRandomWalk _instance = new botRandomWalk();
	}
}