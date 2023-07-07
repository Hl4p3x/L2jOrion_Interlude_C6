package l2jorion.bots.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.xml.IXmlReader;

public class botClanList implements IXmlReader
{
	private static final Logger LOG = LoggerFactory.getLogger(botClanList.class);
	
	private final static Map<Integer, ClanSettings> _botClanList = new HashMap<>();
	
	public botClanList()
	{
		load();
	}
	
	public void reload()
	{
		_botClanList.clear();
		
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("./config/bots/botClanList.xml");
		
		LOG.info(getClass().getSimpleName() + ": Loaded " + _botClanList.size() + " bot clans ");
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
					if ("clan".equalsIgnoreCase(skin.getNodeName()))
					{
						final StatsSet set = new StatsSet();
						
						final NamedNodeMap attrs = skin.getAttributes();
						
						int clanId = parseInteger(attrs, "clanId", 0);
						String name = parseString(attrs, "name", "");
						int crestId = parseInteger(attrs, "crestId", 0);
						
						set.set("clanId", clanId);
						set.set("name", name);
						set.set("crestId", crestId);
						
						_botClanList.put(clanId, new ClanSettings(set));
					}
				}
			}
		}
		
	}
	
	public ClanSettings getBotClan(int clanId)
	{
		if (!_botClanList.containsKey(clanId))
		{
			return null;
		}
		
		return _botClanList.get(clanId);
	}
	
	public Map<Integer, ClanSettings> getBotClanOptions()
	{
		return _botClanList;
	}
	
	public static botClanList getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final botClanList _instance = new botClanList();
	}
}