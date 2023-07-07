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
package l2jorion.game.model;

public class L2ItemMarketModel
{
	private int id;
	private int ownerId;
	private int enchLvl;
	private int itemId;
	private int itemGrade;
	private int priceItem;
	private int price;
	private int count;
	private String ownerName = null;
	private String itemName = null;
	private String itemType = null;
	private String l2Type = null;
	
	private int augmentationId;
	private int augmentationSkill;
	private int augmentationSkillLevel;
	private String augmentationBonus = null;
	
	public void setId(int id)
	{
		this.id = id;
	}
	
	public void setOwnerId(int ownerId)
	{
		this.ownerId = ownerId;
	}
	
	public void setOwnerName(String ownerName)
	{
		this.ownerName = ownerName;
	}
	
	public void setItemId(int itemId)
	{
		this.itemId = itemId;
	}
	
	public void setItemName(String itemName)
	{
		this.itemName = itemName;
	}
	
	public void setEnchLvl(int enchLvl)
	{
		this.enchLvl = enchLvl;
	}
	
	public void setItemGrade(int itemGrade)
	{
		this.itemGrade = itemGrade;
	}
	
	public void setItemType(String itemType)
	{
		this.itemType = itemType;
	}
	
	public void setL2Type(String l2Type)
	{
		this.l2Type = l2Type;
	}
	
	public void setPriceItem(int priceItem)
	{
		this.priceItem = priceItem;
	}
	
	public void setPrice(int price)
	{
		this.price = price;
	}
	
	public void setCount(int count)
	{
		this.count = count;
	}
	
	public void setAugmentationId(int augmentationId)
	{
		this.augmentationId = augmentationId;
	}
	
	public void setAugmentationSkill(int augmentationSkill)
	{
		this.augmentationSkill = augmentationSkill;
	}
	
	public void setAugmentationSkillLevel(int augmentationSkillLevel)
	{
		this.augmentationSkillLevel = augmentationSkillLevel;
	}
	
	public void setAugmentationBonus(String augmentationBonus)
	{
		this.augmentationBonus = augmentationBonus;
	}
	
	public int getId()
	{
		return id;
	}
	
	public int getOwnerId()
	{
		return ownerId;
	}
	
	public String getOwnerName()
	{
		return ownerName;
	}
	
	public int getItemId()
	{
		return itemId;
	}
	
	public String getItemName()
	{
		return itemName;
	}
	
	public int getEnchLvl()
	{
		return enchLvl;
	}
	
	public int getItemGrade()
	{
		return itemGrade;
	}
	
	public String getItemType()
	{
		return itemType;
	}
	
	public String getL2Type()
	{
		return l2Type;
	}
	
	public int getPriceItem()
	{
		return priceItem;
	}
	
	public int getPrice()
	{
		return price;
	}
	
	public int getCount()
	{
		return count;
	}
	
	public int getAugmentationId()
	{
		return augmentationId;
	}
	
	public int getAugmentationSkill()
	{
		return augmentationSkill;
	}
	
	public int getAugmentationSkillLevel()
	{
		return augmentationSkillLevel;
	}
	
	public String getAugmentationBonus()
	{
		return augmentationBonus;
	}
}