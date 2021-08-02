package l2jorion.game.model.custom;

public class DressMeData
{
	private int chestId, legsId, glovesId, feetId, weapId;
	
	public DressMeData()
	{
		chestId = 0;
		legsId = 0;
		glovesId = 0;
		feetId = 0;
		weapId = 0;
	}
	
	public int getChestId()
	{
		return chestId;
	}
	
	public int getLegsId()
	{
		return legsId;
	}
	
	public int getGlovesId()
	{
		return glovesId;
	}
	
	public int getBootsId()
	{
		return feetId;
	}
	
	public int getWeapId()
	{
		return weapId;
	}
	
	public void setChestId(int val)
	{
		chestId = val;
	}
	
	public void setLegsId(int val)
	{
		legsId = val;
	}
	
	public void setGlovesId(int val)
	{
		glovesId = val;
	}
	
	public void setBootsId(int val)
	{
		feetId = val;
	}
	
	public void setWeapId(int val)
	{
		weapId = val;
	}
}