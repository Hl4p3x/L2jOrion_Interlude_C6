package l2jorion.game.datatables.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.game.model.base.SkinPackage;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.xml.IXmlReader;

public class DressMeData implements IXmlReader
{
	private static final Logger LOG = LoggerFactory.getLogger(DressMeData.class);
	
	private final static Map<Integer, SkinPackage> _armorSkins = new HashMap<>();
	private final static Map<Integer, SkinPackage> _weaponSkins = new HashMap<>();
	private final static Map<Integer, SkinPackage> _hairSkins = new HashMap<>();
	private final static Map<Integer, SkinPackage> _faceSkins = new HashMap<>();
	private final static Map<Integer, SkinPackage> _shieldSkins = new HashMap<>();
	
	public DressMeData()
	{
		load();
	}
	
	public void reload()
	{
		_armorSkins.clear();
		_weaponSkins.clear();
		_hairSkins.clear();
		_faceSkins.clear();
		// _shieldSkins.clear();
		
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("./data/xml/dressme.xml");
		LOG.info(getClass().getSimpleName() + ": Loaded " + _armorSkins.size() + " armor skins");
		LOG.info(getClass().getSimpleName() + ": Loaded " + _weaponSkins.size() + " weapon skins");
		LOG.info(getClass().getSimpleName() + ": Loaded " + _hairSkins.size() + " hair skins");
		LOG.info(getClass().getSimpleName() + ": Loaded " + _faceSkins.size() + " face skins");
		// LOG.info(getClass().getSimpleName() + ": Loaded " + _shieldSkins.size() + " shield skins");
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
					if ("skin".equalsIgnoreCase(skin.getNodeName()))
					{
						final NamedNodeMap attrs = skin.getAttributes();
						
						String type = parseString(attrs, "type");
						
						final StatsSet set = new StatsSet();
						
						for (Node typeN = skin.getFirstChild(); typeN != null; typeN = typeN.getNextSibling())
						{
							if ("type".equalsIgnoreCase(typeN.getNodeName()))
							{
								final NamedNodeMap attrs2 = typeN.getAttributes();
								
								int id = parseInteger(attrs2, "id");
								String name = parseString(attrs2, "name");
								int weaponId = parseInteger(attrs2, "weaponId", 0);
								int shieldId = parseInteger(attrs2, "shieldId", 0);
								int chestId = parseInteger(attrs2, "chestId", 0);
								int hairId = parseInteger(attrs2, "hairId", 0);
								int faceId = parseInteger(attrs2, "faceId", 0);
								int legsId = parseInteger(attrs2, "legsId", 0);
								int glovesId = parseInteger(attrs2, "glovesId", 0);
								int feetId = parseInteger(attrs2, "feetId", 0);
								int priceId = parseInteger(attrs2, "priceId", 0);
								int priceCount = parseInteger(attrs2, "priceCount", 0);
								
								set.set("type", type);
								
								set.set("id", id);
								set.set("name", name);
								set.set("weaponId", weaponId);
								set.set("shieldId", shieldId);
								set.set("chestId", chestId);
								set.set("hairId", hairId);
								set.set("faceId", faceId);
								set.set("legsId", legsId);
								set.set("glovesId", glovesId);
								set.set("feetId", feetId);
								set.set("priceId", priceId);
								set.set("priceCount", priceCount);
								
								switch (type.toLowerCase())
								{
									case "armor":
										_armorSkins.put(id, new SkinPackage(set));
										break;
									case "weapon":
										_weaponSkins.put(id, new SkinPackage(set));
										break;
									case "hair":
										_hairSkins.put(id, new SkinPackage(set));
										break;
									case "face":
										_faceSkins.put(id, new SkinPackage(set));
										break;
									case "shield":
										_shieldSkins.put(id, new SkinPackage(set));
										break;
								}
							}
						}
					}
				}
			}
		}
		
	}
	
	public SkinPackage getArmorSkinsPackage(int id)
	{
		if (!_armorSkins.containsKey(id))
		{
			return null;
		}
		
		return _armorSkins.get(id);
	}
	
	public Map<Integer, SkinPackage> getArmorSkinOptions()
	{
		return _armorSkins;
	}
	
	public SkinPackage getWeaponSkinsPackage(int id)
	{
		if (!_weaponSkins.containsKey(id))
		{
			return null;
		}
		
		return _weaponSkins.get(id);
	}
	
	public Map<Integer, SkinPackage> getWeaponSkinOptions()
	{
		return _weaponSkins;
	}
	
	public SkinPackage getHairSkinsPackage(int id)
	{
		if (!_hairSkins.containsKey(id))
		{
			return null;
		}
		
		return _hairSkins.get(id);
	}
	
	public Map<Integer, SkinPackage> getHairSkinOptions()
	{
		return _hairSkins;
	}
	
	public SkinPackage getFaceSkinsPackage(int id)
	{
		if (!_faceSkins.containsKey(id))
		{
			return null;
		}
		
		return _faceSkins.get(id);
	}
	
	public Map<Integer, SkinPackage> getFaceSkinOptions()
	{
		return _faceSkins;
	}
	
	public SkinPackage getShieldSkinsPackage(int id)
	{
		if (!_shieldSkins.containsKey(id))
		{
			return null;
		}
		
		return _shieldSkins.get(id);
	}
	
	public Map<Integer, SkinPackage> getShieldSkinOptions()
	{
		return _shieldSkins;
	}
	
	public static DressMeData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DressMeData _instance = new DressMeData();
	}
}