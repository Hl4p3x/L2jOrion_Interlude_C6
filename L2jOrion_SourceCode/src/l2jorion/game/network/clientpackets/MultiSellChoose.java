/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.network.clientpackets;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.model.L2Augmentation;
import l2jorion.game.model.PcInventory;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.multisell.L2Multisell;
import l2jorion.game.model.multisell.MultiSellEntry;
import l2jorion.game.model.multisell.MultiSellIngredient;
import l2jorion.game.model.multisell.MultiSellListContainer;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.ItemList;
import l2jorion.game.network.serverpackets.PledgeShowInfoUpdate;
import l2jorion.game.network.serverpackets.StatusUpdate;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2Armor;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class MultiSellChoose extends L2GameClientPacket
{
	private static Logger LOG = LoggerFactory.getLogger(MultiSellChoose.class.getName());
	
	private int _listId;
	private int _entryId;
	private int _amount;
	private int _enchantment;
	private int _transactionTax; // local handling of taxation
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		_entryId = readD();
		_amount = readD();
		_enchantment = _entryId % 100000;
		_entryId = _entryId / 100000;
		_transactionTax = 0; // Initialize tax amount to 0...
	}
	
	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getMultiSell().tryPerformAction("multisell choose"))
		{
			player.setMultiSellId(-1);
			return;
		}
		
		if (_amount < 1 || _amount > 10000)
		{
			player.setMultiSellId(-1);
			return;
		}
		
		L2NpcInstance merchant = player.getTarget() instanceof L2NpcInstance ? (L2NpcInstance) player.getTarget() : null;
		
		if (!player.hasTempAccessBuy())
		{
			if (merchant == null || !player.isInsideRadius(merchant, L2NpcInstance.INTERACTION_DISTANCE, false, false))
			{
				player.setMultiSellId(-1);
				return;
			}
		}
		
		MultiSellListContainer list = L2Multisell.getInstance().getList(_listId);
		int selectedList = player.getMultiSellId();
		if (list == null || list.getListId() != _listId || selectedList != _listId)
		{
			player.setMultiSellId(-1);
			return;
		}
		
		if (player.isCastingNow() || player.isCastingPotionNow())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.setMultiSellId(-1);
			return;
		}
		
		for (MultiSellEntry entry : list.getEntries())
		{
			if (entry.getEntryId() == _entryId)
			{
				doExchange(player, entry, list.getApplyTaxes(), list.getMaintainEnchantment(), _enchantment);
				return;
			}
		}
	}
	
	private void doExchange(L2PcInstance player, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantment)
	{
		PcInventory inv = player.getInventory();
		boolean maintainItemFound = false;
		
		// given the template entry and information about maintaining enchantment and applying taxes re-create the instance of
		// the entry that will be used for this exchange i.e. change the enchantment level of select ingredient/products and adena amount appropriately.
		L2NpcInstance merchant = player.getTarget() instanceof L2NpcInstance ? (L2NpcInstance) player.getTarget() : null;
		
		MultiSellEntry entry = prepareEntry(merchant, templateEntry, applyTaxes, maintainEnchantment, enchantment);
		
		// Generate a list of distinct ingredients and counts in order to check if the correct item-counts
		// are possessed by the player
		FastList<MultiSellIngredient> _ingredientsList = new FastList<>();
		boolean newIng = true;
		
		for (MultiSellIngredient e : entry.getIngredients())
		{
			newIng = true;
			
			// at this point, the template has already been modified so that enchantments are properly included
			// whenever they need to be applied. Uniqueness of items is thus judged by item id AND enchantment level
			for (MultiSellIngredient ex : _ingredientsList)
			{
				// if the item was already added in the list, merely increment the count
				// this happens if 1 list entry has the same ingredient twice (example 2 swords = 1 dual)
				if (ex.getItemId() == e.getItemId() && ex.getEnchantmentLevel() == e.getEnchantmentLevel())
				{
					if ((double) ex.getItemCount() + e.getItemCount() > Integer.MAX_VALUE)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}
					ex.setItemCount(ex.getItemCount() + e.getItemCount());
					newIng = false;
				}
			}
			if (newIng)
			{
				// If there is a maintainIngredient, then we do not need to check the enchantment parameter as the enchant level will be checked elsewhere
				if (maintainEnchantment)
				{
					maintainItemFound = true;
				}
				
				// if it's a new ingredient, just store its info directly (item id, count, enchantment)
				_ingredientsList.add(new MultiSellIngredient(e));
			}
		}
		
		// If there is no maintainIngredient, then we must make sure that the enchantment is not kept from the client packet, as it may have been forged
		if (!maintainItemFound)
		{
			for (MultiSellIngredient product : entry.getProducts())
			{
				product.setEnchantmentLevel(0);
			}
		}
		
		// now check if the player has sufficient items in the inventory to cover the ingredients' expences
		for (MultiSellIngredient e : _ingredientsList)
		{
			if ((double) e.getItemCount() * _amount > Integer.MAX_VALUE)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				_ingredientsList.clear();
				_ingredientsList = null;
				return;
			}
			
			if (e.getItemId() != 65336 && e.getItemId() != 65436)
			{
				// if this is not a list that maintains enchantment, check the count of all items that have the given id.
				// otherwise, check only the count of items with exactly the needed enchantment level
				if (inv.getInventoryItemCount(e.getItemId(), maintainEnchantment ? e.getEnchantmentLevel() : -1) < (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient() ? e.getItemCount() * _amount : e.getItemCount()))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					_ingredientsList.clear();
					_ingredientsList = null;
					return;
				}
			}
			else
			{
				if (e.getItemId() == 65336)
				{
					if (player.getClan() == null)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER));
						return;
					}
					
					if (!player.isClanLeader())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED));
						return;
					}
					
					if (player.getClan().getReputationScore() < e.getItemCount() * _amount)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.CLAN_REPUTATION_SCORE_IS_TOO_LOW));
						return;
					}
				}
				
				if (e.getItemId() == 65436 && e.getItemCount() * _amount > player.getPcBangScore())
				{
					player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
			}
		}
		
		_ingredientsList.clear();
		_ingredientsList = null;
		FastList<L2Augmentation> augmentation = new FastList<>();
		/** All ok, remove items and add final product */
		
		for (MultiSellIngredient e : entry.getIngredients())
		{
			if (e.getItemId() != 65336 && e.getItemId() != 65436)
			{
				for (MultiSellIngredient a : entry.getProducts())
				{
					if (player.getInventoryLimit() < inv.getSize() + _amount && !ItemTable.getInstance().createDummyItem(a.getItemId()).isStackable())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
						return;
					}
					if (player.getInventoryLimit() < inv.getSize() && ItemTable.getInstance().createDummyItem(a.getItemId()).isStackable())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
						return;
					}
				}
				L2ItemInstance itemToTake = inv.getItemByItemId(e.getItemId()); // initialize and initial guess for the item to take.
				
				// this is a cheat, transaction will be aborted and if any items already tanken will not be returned back to inventory!
				if (itemToTake == null)
				{
					LOG.warn("Character: " + player.getName() + " is trying to cheat in multisell, merchatnt id:" + (merchant != null ? merchant.getNpcId() : 0));
					return;
				}
				if (itemToTake.fireEvent("MULTISELL", (Object[]) null) != null)
				{
					return;
				}
				
				if (itemToTake.isWear())
				{
					LOG.warn("Character: " + player.getName() + " is trying to cheat in multisell with weared item");
					return;
				}
				
				if (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient())
				{
					// if it's a stackable item, just reduce the amount from the first (only) instance that is found in the inventory
					if (itemToTake.isStackable())
					{
						if (!player.destroyItem("Multisell", itemToTake.getObjectId(), (e.getItemCount() * _amount), player.getTarget(), true))
						{
							return;
						}
					}
					else
					{
						// for non-stackable items, one of two scenaria are possible:
						// a) list maintains enchantment: get the instances that exactly match the requested enchantment level
						// b) list does not maintain enchantment: get the instances with the LOWEST enchantment level
						
						// a) if enchantment is maintained, then get a list of items that exactly match this enchantment
						if (maintainEnchantment)
						{
							// loop through this list and remove (one by one) each item until the required amount is taken.
							L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantmentLevel());
							for (int i = 0; i < e.getItemCount() * _amount; i++)
							{
								if (inventoryContents[i].isAugmented())
								{
									augmentation.add(inventoryContents[i].getAugmentation());
								}
								
								if (inventoryContents[i].isEquipped())
								{
									if (inventoryContents[i].isAugmented())
									{
										inventoryContents[i].getAugmentation().removeBoni(player);
									}
								}
								
								if (!player.destroyItem("Multisell", inventoryContents[i].getObjectId(), 1, player.getTarget(), true))
								{
									return;
								}
							}
						}
						else
						// b) enchantment is not maintained. Get the instances with the LOWEST enchantment level
						{
							/*
							 * NOTE: There are 2 ways to achieve the above goal. 1) Get all items that have the correct itemId, loop through them until the lowest enchantment level is found. Repeat all this for the next item until proper count of items is reached. 2) Get all items that have the
							 * correct itemId, sort them once based on enchantment level, and get the range of items that is necessary. Method 1 is faster for a small number of items to be exchanged. Method 2 is faster for large amounts. EXPLANATION: Worst case scenario for algorithm 1 will make it
							 * run in a number of cycles given by: m*(2n-m+1)/2 where m is the number of items to be exchanged and n is the total number of inventory items that have a matching id. With algorithm 2 (sort), sorting takes n*log(n) time and the choice is done in a single cycle for case
							 * b (just grab the m first items) or in linear time for case a (find the beginning of items with correct enchantment, index x, and take all items from x to x+m). Basically, whenever m > log(n) we have: m*(2n-m+1)/2 = (2nm-m*m+m)/2 > (2nlogn-logn*logn+logn)/2 = nlog(n) -
							 * log(n*n) + log(n) = nlog(n) + log(n/n*n) = nlog(n) + log(1/n) = nlog(n) - log(n) = (n-1)log(n) So for m < log(n) then m*(2n-m+1)/2 > (n-1)log(n) and m*(2n-m+1)/2 > nlog(n) IDEALLY: In order to best optimize the performance, choose which algorithm to run, based on
							 * whether 2^m > n if ( (2<<(e.getItemCount() * _amount)) < inventoryContents.length ) // do Algorithm 1, no sorting else // do Algorithm 2, sorting CURRENT IMPLEMENTATION: In general, it is going to be very rare for a person to do a massive exchange of non-stackable
							 * items For this reason, we assume that algorithm 1 will always suffice and we keep things simple. If, in the future, it becomes necessary that we optimize, the above discussion should make it clear what optimization exactly is necessary (based on the comments under
							 * "IDEALLY").
							 */
							
							// choice 1. Small number of items exchanged. No sorting.
							for (int i = 1; i <= e.getItemCount() * _amount; i++)
							{
								L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId());
								
								itemToTake = inventoryContents[0];
								// get item with the LOWEST enchantment level from the inventory...
								// +0 is lowest by default...
								if (itemToTake.getEnchantLevel() > 0)
								{
									for (L2ItemInstance inventoryContent : inventoryContents)
									{
										if (inventoryContent.getEnchantLevel() < itemToTake.getEnchantLevel())
										{
											itemToTake = inventoryContent;
											// nothing will have enchantment less than 0. If a zero-enchanted
											// item is found, just take it
											if (itemToTake.getEnchantLevel() == 0)
											{
												break;
											}
										}
									}
								}
								
								if (itemToTake.isEquipped())
								{
									if (itemToTake.isAugmented())
									{
										itemToTake.getAugmentation().removeBoni(player);
									}
								}
								
								if (!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player.getTarget(), true))
								{
									return;
								}
								
							}
						}
					}
				}
			}
			else
			{
				if (e.getItemId() == 65336)
				{
					int repCost = player.getClan().getReputationScore() - e.getItemCount();
					player.getClan().setReputationScore(repCost, true);
					player.sendPacket(new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(e.getItemCount()));
					player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
				}
				else
				{
					player.reducePcBangScore(e.getItemCount() * _amount);
					player.sendPacket(new SystemMessage(SystemMessageId.USING_S1_PCPOINT).addNumber(e.getItemCount()));
				}
			}
		}
		// Generate the appropriate items
		for (MultiSellIngredient e : entry.getProducts())
		{
			if (ItemTable.getInstance().createDummyItem(e.getItemId()).isStackable())
			{
				inv.addItem("Multisell[" + _listId + "]", e.getItemId(), (e.getItemCount() * _amount), player, player.getTarget());
			}
			else
			{
				L2ItemInstance product = null;
				for (int i = 0; i < e.getItemCount() * _amount; i++)
				{
					product = inv.addItem("Multisell[" + _listId + "]", e.getItemId(), 1, player, player.getTarget());
					if (maintainEnchantment && (product != null))
					{
						if (i < augmentation.size())
						{
							product.setAugmentation(new L2Augmentation(product, augmentation.get(i).getAugmentationId(), augmentation.get(i).getSkill(), true));
						}
						product.setEnchantLevel(e.getEnchantmentLevel());
					}
				}
			}
			// Msg part
			SystemMessage sm;
			
			if (e.getItemCount() * _amount > 1)
			{
				sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(e.getItemId());
				sm.addNumber(e.getItemCount() * _amount);
				player.sendPacket(sm);
			}
			else
			{
				if (maintainEnchantment && e.getEnchantmentLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.ACQUIRED);
					sm.addNumber(e.getEnchantmentLevel());
					sm.addItemName(e.getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
					sm.addItemName(e.getItemId());
				}
				player.sendPacket(sm);
			}
		}
		player.sendPacket(new ItemList(player, false));
		
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		player.broadcastUserInfo();
		
		// Finally, give the tax to the castle...
		if (merchant != null && merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0)
		{
			merchant.getCastle().addToTreasury(_transactionTax * _amount);
		}
	}
	
	// Regarding taxation, the following appears to be the case:
	// a) The count of aa remains unchanged (taxes do not affect aa directly).
	// b) 5/6 of the amount of aa is taxed by the normal tax rate.
	// c) the resulting taxes are added as normal adena value.
	// d) normal adena are taxed fully.
	// e) Items other than adena and ancient adena are not taxed even when the list is taxable.
	// example: If the template has an item worth 120aa, and the tax is 10%,
	// then from 120aa, take 5/6 so that is 100aa, apply the 10% tax in adena (10a)
	// so the final price will be 120aa and 10a!
	private MultiSellEntry prepareEntry(L2NpcInstance merchant, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel)
	{
		MultiSellEntry newEntry = new MultiSellEntry();
		newEntry.setEntryId(templateEntry.getEntryId());
		int totalAdenaCount = 0;
		boolean hasIngredient = false;
		
		for (MultiSellIngredient ing : templateEntry.getIngredients())
		{
			// Load the ingredient from the template
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
			
			if (newIngredient.getItemId() == 57 && newIngredient.isTaxIngredient())
			{
				double taxRate = 0.0;
				
				if (applyTaxes)
				{
					if (merchant != null && merchant.getIsInTown())
					{
						taxRate = merchant.getCastle().getTaxRate();
					}
				}
				
				_transactionTax = (int) Math.round(newIngredient.getItemCount() * taxRate);
				totalAdenaCount += _transactionTax;
				continue; // Do not yet add this adena amount to the list as non-taxIngredient adena might be entered later (order not guaranteed)
			}
			else if (ing.getItemId() == 57) // && !ing.isTaxIngredient()
			{
				totalAdenaCount += newIngredient.getItemCount();
				continue; // Do not yet add this adena amount to the list as taxIngredient adena might be entered later (order not guaranteed)
			}
			// If it is an armor/weapon, modify the enchantment level appropriately, if necessary
			else if (maintainEnchantment)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				
				if (tempItem != null && tempItem instanceof L2Armor || tempItem != null && tempItem instanceof L2Weapon)
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					hasIngredient = true;
				}
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// Next add the adena amount, if any
		if (totalAdenaCount > 0)
		{
			newEntry.addIngredient(new MultiSellIngredient(57, totalAdenaCount, false, false));
		}
		
		// Now modify the enchantment level of products, if necessary
		for (MultiSellIngredient ing : templateEntry.getProducts())
		{
			// Load the ingredient from the template
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
			
			if (maintainEnchantment && hasIngredient)
			{
				// If it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				
				if (tempItem instanceof L2Armor || tempItem instanceof L2Weapon)
				{
					if (enchantLevel == 0 && maintainEnchantment)
					{
						enchantLevel = ing.getEnchantmentLevel();
					}
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	@Override
	public String getType()
	{
		return "[C] A7 MultiSellChoose";
	}
}