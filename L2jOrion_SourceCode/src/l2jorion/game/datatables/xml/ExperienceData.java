package l2jorion.game.datatables.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ExperienceData
{
	private static Logger LOG = LoggerFactory.getLogger(ExperienceData.class);
	
	public static byte MAX_LEVEL;
	private byte MAX_PET_LEVEL;
	
	private final Map<Integer, Long> _expTable = new HashMap<>();
	
	protected ExperienceData()
	{
		loadData();
	}
	
	private void loadData()
	{
		final File xml = new File(Config.DATAPACK_ROOT, "data/xml/experience.xml");
		if (!xml.exists())
		{
			LOG.warn(getClass().getSimpleName() + ": experience.xml not found!");
			return;
		}
		
		Document doc = null;
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		try
		{
			doc = factory.newDocumentBuilder().parse(xml);
		}
		catch (final Exception e)
		{
			LOG.warn("Could not parse experience.xml: " + e.getMessage());
			return;
		}
		
		final Node table = doc.getFirstChild();
		final NamedNodeMap tableAttr = table.getAttributes();
		
		MAX_LEVEL = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxLevel").getNodeValue()) + 1);
		MAX_PET_LEVEL = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxPetLevel").getNodeValue()) + 1);
		
		_expTable.clear();
		
		NamedNodeMap attrs;
		Integer level;
		Long exp;
		for (Node experience = table.getFirstChild(); experience != null; experience = experience.getNextSibling())
		{
			if (experience.getNodeName().equals("experience"))
			{
				attrs = experience.getAttributes();
				level = Integer.valueOf(attrs.getNamedItem("level").getNodeValue());
				exp = Long.valueOf(attrs.getNamedItem("tolevel").getNodeValue());
				_expTable.put(level, exp);
			}
		}
		
		LOG.info(getClass().getSimpleName() + ": Loaded " + _expTable.size() + " levels");
		LOG.info(getClass().getSimpleName() + ": Max player level is: " + (MAX_LEVEL - 1));
		LOG.info(getClass().getSimpleName() + ": Max pet level is: " + (MAX_PET_LEVEL - 1));
	}
	
	public long getExpForLevel(final int level)
	{
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
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ExperienceData _instance = new ExperienceData();
	}
}
