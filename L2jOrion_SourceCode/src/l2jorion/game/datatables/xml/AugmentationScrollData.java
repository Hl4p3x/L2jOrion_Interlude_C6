package l2jorion.game.datatables.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.templates.StatsSet;
import l2jorion.util.xml.IXmlReader;

public class AugmentationScrollData implements IXmlReader
{
	private final Map<Integer, L2AugmentScroll> _scrolls = new HashMap<>();
	
	public AugmentationScrollData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("./data/xml/augmentScroll.xml");
		LOG.info(getClass().getSimpleName() + ": Loaded {} augment scrolls", _scrolls.size());
	}
	
	public void reload()
	{
		_scrolls.clear();
		load();
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		for (Node a = doc.getFirstChild(); a != null; a = a.getNextSibling())
		{
			if ("list".equalsIgnoreCase(a.getNodeName()))
			{
				for (Node b = a.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("scroll".equalsIgnoreCase(b.getNodeName()))
					{
						final NamedNodeMap attrs = b.getAttributes();
						final StatsSet set = new StatsSet();
						
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						_scrolls.put(set.getInteger("id"), new L2AugmentScroll(set));
					}
				}
			}
		}
	}
	
	public L2AugmentScroll getScroll(L2ItemInstance item)
	{
		return _scrolls.get(item.getItemId());
	}
	
	public class L2AugmentScroll
	{
		private final int _id;
		private final int _skillId;
		private final int _skillLv;
		
		public L2AugmentScroll(StatsSet set)
		{
			_id = set.getInteger("id");
			_skillId = set.getInteger("skill");
			_skillLv = set.getInteger("level");
		}
		
		public final int getAugmentScrollId()
		{
			return _id;
		}
		
		public final int getAugmentSkillId()
		{
			return _skillId;
		}
		
		public final int getAugmentSkillLv()
		{
			return _skillLv;
		}
	}
	
	public static AugmentationScrollData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AugmentationScrollData INSTANCE = new AugmentationScrollData();
	}
}