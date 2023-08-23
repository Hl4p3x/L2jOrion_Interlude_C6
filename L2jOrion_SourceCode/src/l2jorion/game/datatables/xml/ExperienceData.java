package l2jorion.game.datatables.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.xml.IXmlReader;

public class ExperienceData implements IXmlReader
{
	private static Logger LOG = LoggerFactory.getLogger(ExperienceData.class);
	
	private final Map<Integer, Long> _expTable = new HashMap<>();
	
	private byte MAX_LEVEL;
	private byte MAX_PET_LEVEL;
	
	protected ExperienceData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_expTable.clear();
		parseDatapackFile("data/xml/experience.xml");
		LOG.info(getClass().getSimpleName() + ": Loaded " + _expTable.size() + " levels");
		LOG.info(getClass().getSimpleName() + ": Max player level is: " + (MAX_LEVEL - 1));
		LOG.info(getClass().getSimpleName() + ": Max pet level is: " + (MAX_PET_LEVEL - 1));
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		final Node table = doc.getFirstChild();
		final NamedNodeMap tableAttr = table.getAttributes();
		MAX_LEVEL = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxLevel").getNodeValue()) + 1);
		MAX_PET_LEVEL = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxPetLevel").getNodeValue()) + 1);
		if (MAX_LEVEL > Config.MAX_PLAYER_LEVEL)
		{
			MAX_LEVEL = Config.MAX_PLAYER_LEVEL;
		}
		if (MAX_PET_LEVEL > (MAX_LEVEL + 1))
		{
			MAX_PET_LEVEL = (byte) (MAX_LEVEL + 1); // Pet level should not exceed owner level.
		}
		
		int maxLevel = 0;
		for (Node n = table.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("experience".equals(n.getNodeName()))
			{
				final NamedNodeMap attrs = n.getAttributes();
				maxLevel = parseInteger(attrs, "level");
				if (maxLevel > Config.MAX_PLAYER_LEVEL)
				{
					break;
				}
				_expTable.put(maxLevel, parseLong(attrs, "tolevel"));
			}
		}
	}
	
	public long getExpForLevel(int level)
	{
		if (level > Config.MAX_PLAYER_LEVEL)
		{
			return _expTable.get((int) Config.MAX_PLAYER_LEVEL);
		}
		return _expTable.get(level);
	}
	
	public byte getMaxLevel()
	{
		return MAX_LEVEL;
	}
	
	public byte getMaxPetLevel()
	{
		return MAX_PET_LEVEL;
	}
	
	public static ExperienceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ExperienceData INSTANCE = new ExperienceData();
	}
}
