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

public class botEquipment implements IXmlReader
{
	private static final Logger LOG = LoggerFactory.getLogger(botEquipment.class);
	
	private final static Map<Integer, EquipPackage> _botEquipment = new HashMap<>();
	
	public botEquipment()
	{
		load();
	}
	
	public void reload()
	{
		_botEquipment.clear();
		
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("./config/bots/botEquipment.xml");
		LOG.info(getClass().getSimpleName() + ": Loaded " + _botEquipment.size() + " bot equipments");
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
						
						String type = parseString(attrs, "type");
						
						final StatsSet set = new StatsSet();
						
						for (Node typeN = skin.getFirstChild(); typeN != null; typeN = typeN.getNextSibling())
						{
							if ("type".equalsIgnoreCase(typeN.getNodeName()))
							{
								final NamedNodeMap attrs2 = typeN.getAttributes();
								
								int classId = parseInteger(attrs2, "classId");
								int weaponId = parseInteger(attrs2, "weapon", 0);
								int shieldId = parseInteger(attrs2, "shield", 0);
								int helmId = parseInteger(attrs2, "helm", 0);
								int chestId = parseInteger(attrs2, "chest", 0);
								int hairId = parseInteger(attrs2, "hair", 0);
								int faceId = parseInteger(attrs2, "face", 0);
								int legsId = parseInteger(attrs2, "legs", 0);
								int glovesId = parseInteger(attrs2, "gloves", 0);
								int feetId = parseInteger(attrs2, "feet", 0);
								int neckId = parseInteger(attrs2, "neck", 0);
								int learId = parseInteger(attrs2, "lear", 0);
								int rearId = parseInteger(attrs2, "rear", 0);
								int lfingerId = parseInteger(attrs2, "lfing", 0);
								int rfingerId = parseInteger(attrs2, "rfing", 0);
								
								set.set("type", type);
								
								set.set("classId", classId);
								set.set("weaponId", weaponId);
								set.set("shieldId", shieldId);
								set.set("helmId", helmId);
								set.set("chestId", chestId);
								set.set("hairId", hairId);
								set.set("faceId", faceId);
								set.set("legsId", legsId);
								set.set("glovesId", glovesId);
								set.set("feetId", feetId);
								set.set("neckId", neckId);
								set.set("learId", learId);
								set.set("rearId", rearId);
								set.set("lfingerId", lfingerId);
								set.set("rfingerId", rfingerId);
								
								switch (type.toLowerCase())
								{
									case "equipment":
										_botEquipment.put(classId, new EquipPackage(set));
										break;
									default:
										break;
								}
								
							}
						}
					}
				}
			}
		}
		
	}
	
	public EquipPackage getEquipmentPackage(int classId)
	{
		if (!_botEquipment.containsKey(classId))
		{
			return null;
		}
		
		return _botEquipment.get(classId);
	}
	
	public Map<Integer, EquipPackage> getEquipmentOptions()
	{
		return _botEquipment;
	}
	
	public static botEquipment getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final botEquipment _instance = new botEquipment();
	}
}