package l2jorion.game.datatables.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.game.model.L2ExtractableItem;
import l2jorion.game.model.L2ExtractableProductItem;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.xml.IXmlReader;

public class ExtractableItemsData implements IXmlReader
{
	protected static final Logger LOG = LoggerFactory.getLogger(ExtractableItemsData.class);
	
	private Map<Integer, L2ExtractableItem> _items = new HashMap<>();
	
	protected ExtractableItemsData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_items.clear();
		parseDatapackFile("data/xml/extractableItems.xml");
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		try
		{
			int id;
			int amount;
			int production;
			float totalChance;
			float chance;
			final StatsSet set = new StatsSet();
			final Node n = doc.getFirstChild();
			for (Node node = n.getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("item".equalsIgnoreCase(node.getNodeName()))
				{
					id = Integer.parseInt(node.getAttributes().getNamedItem("id").getNodeValue());
					final List<L2ExtractableProductItem> extractables = new ArrayList<>();
					for (Node b = node.getFirstChild(); b != null; b = b.getNextSibling())
					{
						if ("extract".equalsIgnoreCase(b.getNodeName()))
						{
							final NamedNodeMap attrs = b.getAttributes();
							for (int i = 0; i < attrs.getLength(); i++)
							{
								final Node attr = attrs.item(i);
								set.set(attr.getNodeName(), attr.getNodeValue());
							}
							
							production = set.getInteger("id");
							amount = set.getInteger("quantity");
							chance = set.getInteger("chance");
							extractables.add(new L2ExtractableProductItem(production, amount, chance));
							totalChance = 0;
							for (L2ExtractableProductItem extractable : extractables)
							{
								totalChance += extractable.getChance();
							}
							if (totalChance > 100)
							{
								LOG.info(getClass().getSimpleName() + ": Extractable with id " + id + " has was more than 100% total chance!");
							}
						}
					}
					_items.put(id, new L2ExtractableItem(id, extractables));
				}
			}
			
			LOG.info(getClass().getSimpleName() + ": Loaded " + _items.size() + " extractable items.");
		}
		catch (Exception e)
		{
			LOG.warn(getClass().getSimpleName() + ": Error while loading extractable items! " + e);
		}
	}
	
	public L2ExtractableItem getExtractableItem(int itemId)
	{
		return _items.get(itemId);
	}
	
	public int[] itemIDs()
	{
		int index = 0;
		int[] result = new int[_items.size()];
		for (L2ExtractableItem ei : _items.values())
		{
			result[index++] = ei.getItemId();
		}
		return result;
	}
	
	public static ExtractableItemsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ExtractableItemsData INSTANCE = new ExtractableItemsData();
	}
}
