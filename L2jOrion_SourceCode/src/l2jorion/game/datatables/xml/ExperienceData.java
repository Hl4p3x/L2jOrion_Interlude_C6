package l2jorion.game.datatables.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.xml.IXmlReader;

public class ExperienceData implements IXmlReader
{
	private static Logger LOG = LoggerFactory.getLogger(ExperienceData.class);
	
	private final Map<Integer, Long> _expTable = new HashMap<>();
	
	private static final byte PLAYER_MAXIMUM_LEVEL = 80;
	public byte _maxPlayerLevel;
	private byte _maxPetLevel;
	
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
		LOG.info(getClass().getSimpleName() + ": Max player level is: " + (_maxPlayerLevel - 1));
		LOG.info(getClass().getSimpleName() + ": Max pet level is: " + (_maxPetLevel - 1));
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		final Node table = doc.getFirstChild();
		final NamedNodeMap tableAttr = table.getAttributes();
		_maxPlayerLevel = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxLevel").getNodeValue()) + 1);
		_maxPetLevel = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxPetLevel").getNodeValue()) + 1);
		if (_maxPlayerLevel > PLAYER_MAXIMUM_LEVEL)
		{
			_maxPlayerLevel = PLAYER_MAXIMUM_LEVEL;
		}
		if (_maxPetLevel > (_maxPlayerLevel + 1))
		{
			_maxPetLevel = (byte) (_maxPlayerLevel + 1); // Pet level should not exceed owner level.
		}
		
		int maxLevel = 0;
		for (Node n = table.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("experience".equals(n.getNodeName()))
			{
				final NamedNodeMap attrs = n.getAttributes();
				maxLevel = parseInteger(attrs, "level");
				if (maxLevel > PLAYER_MAXIMUM_LEVEL)
				{
					break;
				}
				_expTable.put(maxLevel, parseLong(attrs, "tolevel"));
			}
		}
	}
	
	public long getExpForLevel(int level)
	{
		return _expTable.get(level);
	}
	
	public byte getMaxLevel()
	{
		return _maxPlayerLevel;
	}
	
	public byte getMaxPetLevel()
	{
		return _maxPetLevel;
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
