/* L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.model.multisell;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.MultiSellList;
import l2jorion.game.templates.L2Armor;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2Multisell
{
	private static Logger LOG = LoggerFactory.getLogger(L2Multisell.class);
	private final List<MultiSellListContainer> _entries = new FastList<>();
	private static L2Multisell _instance;
	
	public MultiSellListContainer getList(final int id)
	{
		synchronized (_entries)
		{
			for (final MultiSellListContainer list : _entries)
			{
				if (list.getListId() == id)
				{
					return list;
				}
			}
		}
		
		LOG.warn("L2Multisell: can't find list with id: " + id);
		return null;
	}
	
	private L2Multisell()
	{
		parseData();
	}
	
	public void reload()
	{
		parseData();
	}
	
	public static L2Multisell getInstance()
	{
		if (_instance == null)
		{
			_instance = new L2Multisell();
		}
		return _instance;
	}
	
	private void parseData()
	{
		_entries.clear();
		parse();
	}
	
	/**
	 * This will generate the multisell list for the items. There exist various parameters in multisells that affect the way they will appear: 1) inventory only: * if true, only show items of the multisell for which the "primary" ingredients are already in the player's inventory. By "primary"
	 * ingredients we mean weapon and armor. * if false, show the entire list. 2) maintain enchantment: presumably, only lists with "inventory only" set to true should sometimes have this as true. This makes no sense otherwise... * If true, then the product will match the enchantment level of the
	 * ingredient. if the player has multiple items that match the ingredient list but the enchantment levels differ, then the entries need to be duplicated to show the products and ingredients for each enchantment level. For example: If the player has a crystal staff +1 and a crystal staff +3 and
	 * goes to exchange it at the mammon, the list should have all exchange possibilities for the +1 staff, followed by all possibilities for the +3 staff. * If false, then any level ingredient will be considered equal and product will always be at +0 3) apply taxes: Uses the "taxIngredient" entry
	 * in order to add a certain amount of adena to the ingredients
	 * @param listId
	 * @param inventoryOnly
	 * @param player
	 * @param taxRate
	 * @return
	 */
	private MultiSellListContainer generateMultiSell(final int listId, final boolean inventoryOnly, final L2PcInstance player, final double taxRate)
	{
		MultiSellListContainer listTemplate = L2Multisell.getInstance().getList(listId);
		MultiSellListContainer list = new MultiSellListContainer();
		
		if (listTemplate == null)
		{
			return list;
		}
		
		list = new MultiSellListContainer();
		list.setListId(listId);
		
		if (inventoryOnly)
		{
			if (player == null)
			{
				return list;
			}
			
			L2ItemInstance[] items;
			
			if (listTemplate.getMaintainEnchantment())
			{
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false, true);
			}
			else
			{
				items = player.getInventory().getUniqueItems(false, false, false, true);
			}
			
			int enchantLevel;
			for (final L2ItemInstance item : items)
			{
				// only do the matchup on equipable items that are not currently equipped
				// so for each appropriate item, produce a set of entries for the multisell list.
				if (!item.isWear() && (item.getItem() instanceof L2Armor || item.getItem() instanceof L2Weapon))
				{
					enchantLevel = listTemplate.getMaintainEnchantment() ? item.getEnchantLevel() : 0;
					// loop through the entries to see which ones we wish to include
					for (final MultiSellEntry ent : listTemplate.getEntries())
					{
						boolean doInclude = false;
						
						// check ingredients of this entry to see if it's an entry we'd like to include.
						for (final MultiSellIngredient ing : ent.getIngredients())
						{
							if (item.getItemId() == ing.getItemId())
							{
								doInclude = true;
								break;
							}
						}
						
						// manipulate the ingredients of the template entry for this particular instance shown
						// i.e: Assign enchant levels and/or apply taxes as needed.
						if (doInclude)
						{
							list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), listTemplate.getMaintainEnchantment(), enchantLevel, taxRate));
						}
					}
				}
			} // end for each inventory item.
			
			items = null;
		} // end if "inventory-only"
		else
		// this is a list-all type
		{
			// if no taxes are applied, no modifications are needed
			for (final MultiSellEntry ent : listTemplate.getEntries())
			{
				list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), false, 0, taxRate));
			}
		}
		
		listTemplate = null;
		
		return list;
	}
	
	// Regarding taxation, the following is the case:
	// a) The taxes come out purely from the adena TaxIngredient
	// b) If the entry has no adena ingredients other than the taxIngredient, the resulting
	// amount of adena is appended to the entry
	// c) If the entry already has adena as an entry, the taxIngredient is used in order to increase
	// the count for the existing adena ingredient
	private MultiSellEntry prepareEntry(final MultiSellEntry templateEntry, final boolean applyTaxes, final boolean maintainEnchantment, final int enchantLevel, final double taxRate)
	{
		final MultiSellEntry newEntry = new MultiSellEntry();
		newEntry.setEntryId(templateEntry.getEntryId() * 100000 + enchantLevel);
		
		int adenaAmount = 0;
		
		for (final MultiSellIngredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
			
			// if taxes are to be applied, modify/add the adena count based on the template adena/ancient adena count
			if (ing.getItemId() == 57 && ing.isTaxIngredient())
			{
				if (applyTaxes)
				{
					adenaAmount += (int) Math.round(ing.getItemCount() * taxRate);
				}
				continue; // do not adena yet, as non-taxIngredient adena entries might occur next (order not guaranteed)
			}
			else if (ing.getItemId() == 57) // && !ing.isTaxIngredient()
			{
				adenaAmount += ing.getItemCount();
				continue; // do not adena yet, as taxIngredient adena entries might occur next (order not guaranteed)
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			else if (maintainEnchantment)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if (tempItem instanceof L2Armor || tempItem instanceof L2Weapon)
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
				
				tempItem = null;
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
			newIngredient = null;
		}
		
		// now add the adena, if any.
		if (adenaAmount > 0)
		{
			newEntry.addIngredient(new MultiSellIngredient(57, adenaAmount, 0, false, false));
		}
		
		// Now modify the enchantment level of products, if necessary
		for (final MultiSellIngredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
			
			if (maintainEnchantment)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				final L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				
				if (tempItem instanceof L2Armor || tempItem instanceof L2Weapon)
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
			}
			
			newEntry.addProduct(newIngredient);
			newIngredient = null;
		}
		
		return newEntry;
	}
	
	public void SeparateAndSend(final int listId, final L2PcInstance player, final boolean inventoryOnly, final double taxRate)
	{
		MultiSellListContainer list = generateMultiSell(listId, inventoryOnly, player, taxRate);
		MultiSellListContainer temp = new MultiSellListContainer();
		
		int page = 1;
		
		temp.setListId(list.getListId());
		
		for (final MultiSellEntry e : list.getEntries())
		{
			if (temp.getEntries().size() == 40)
			{
				player.sendPacket(new MultiSellList(temp, page, 0));
				page++;
				temp = new MultiSellListContainer();
				temp.setListId(list.getListId());
			}
			
			temp.addEntry(e);
		}
		
		player.setMultiSellId(listId);
		
		player.sendPacket(new MultiSellList(temp, page, 1));
	}
	
	private void hashFiles(final String dirname, final List<File> hash)
	{
		final File dir = new File(Config.DATAPACK_ROOT, "data/xml/" + dirname);
		
		if (!dir.exists())
		{
			LOG.warn("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		
		final File[] files = dir.listFiles();
		
		for (final File f : files)
		{
			if (f.getName().endsWith(".xml"))
			{
				hash.add(f);
			}
		}
		
	}
	
	private void parse()
	{
		Document doc = null;
		
		int id = 0;
		
		List<File> files = new FastList<>();
		hashFiles("multisell", files);
		
		for (final File f : files)
		{
			id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
			try
			{
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				doc = factory.newDocumentBuilder().parse(f);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.error("Error loading file " + f, e);
			}
			try
			{
				MultiSellListContainer list = parseDocument(doc);
				list.setListId(id);
				
				updateReferencePrice(list);
				
				_entries.add(list);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.error("Error in file " + f, e);
			}
		}
	}
	
	protected MultiSellListContainer parseDocument(final Document doc)
	{
		final MultiSellListContainer list = new MultiSellListContainer();
		
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				attribute = n.getAttributes().getNamedItem("applyTaxes");
				
				if (attribute == null)
				{
					list.setApplyTaxes(false);
				}
				else
				{
					list.setApplyTaxes(Boolean.parseBoolean(attribute.getNodeValue()));
				}
				
				attribute = n.getAttributes().getNamedItem("npcId");
				if (attribute != null)
				{
					list.setNpcId(attribute.getNodeValue());
				}
				
				attribute = n.getAttributes().getNamedItem("maintainEnchantment");
				if (attribute == null)
				{
					list.setMaintainEnchantment(false);
				}
				else
				{
					list.setMaintainEnchantment(Boolean.parseBoolean(attribute.getNodeValue()));
				}
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						final MultiSellEntry e = parseEntry(d);
						list.addEntry(e);
					}
				}
				
				attribute = null;
			}
			else if ("item".equalsIgnoreCase(n.getNodeName()))
			{
				final MultiSellEntry e = parseEntry(n);
				list.addEntry(e);
			}
		}
		
		return list;
	}
	
	protected MultiSellEntry parseEntry(Node n)
	{
		final int entryId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		
		Node first = n.getFirstChild();
		final MultiSellEntry entry = new MultiSellEntry();
		
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				
				final int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				final int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
				
				boolean isTaxIngredient = false, mantainIngredient = false;
				
				attribute = n.getAttributes().getNamedItem("isTaxIngredient");
				
				if (attribute != null)
				{
					isTaxIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				}
				
				attribute = n.getAttributes().getNamedItem("mantainIngredient");
				
				if (attribute != null)
				{
					mantainIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				}
				
				MultiSellIngredient e = new MultiSellIngredient(id, count, isTaxIngredient, mantainIngredient);
				entry.addIngredient(e);
			}
			else if ("production".equalsIgnoreCase(n.getNodeName()))
			{
				final int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				final int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
				int enchant = 0;
				
				if (n.getAttributes().getNamedItem("enchant") != null)
				{
					enchant = Integer.parseInt(n.getAttributes().getNamedItem("enchant").getNodeValue());
				}
				MultiSellIngredient e = new MultiSellIngredient(id, count, enchant, false, false);
				entry.addProduct(e);
			}
		}
		
		entry.setEntryId(entryId);
		
		return entry;
	}
	
	private void updateReferencePrice(final MultiSellListContainer container)
	{
		for (final MultiSellEntry entry : container.getEntries())
		{
			// if ingredient is just 1 and is adena
			if (entry.getIngredients().size() == 1 && entry.getIngredients().get(0).getItemId() == 57)
			{
				// the buy price must necessarily higher then total reference item price / 2 that is the default sell price
				int totalProductReferencePrice = 0;
				for (final MultiSellIngredient product : entry.getProducts())
				{
					totalProductReferencePrice += (ItemTable.getInstance().getTemplate(product.getItemId()).getReferencePrice() * product.getItemCount());
				}
				
				if (entry.getIngredients().get(0).getItemCount() < (totalProductReferencePrice / 2))
				{
					LOG.warn("Multisell:" + container.getListId() + " entryId:" + entry.getEntryId() + " has an ADENA price lower than total products reference price. Automatically updating it...");
					entry.getIngredients().get(0).setItemCount(totalProductReferencePrice);
				}
			}
		}
	}
}
