package l2jorion.game.model.base;

import l2jorion.game.templates.StatsSet;

public class SkinPackage
{
	private String _type;
	private String _name;
	private int _id;
	private int _weaponId;
	private int _shieldId;
	private int _chestId;
	private int _hairId;
	private int _faceId;
	private int _legsId;
	private int _glovesId;
	private int _feetId;
	private int _priceId;
	private int _priceCount;
	
	public SkinPackage(StatsSet set)
	{
		_type = set.getString("type", "default");
		_name = set.getString("name", "NoName");
		_id = set.getInteger("id", 0);
		_weaponId = set.getInteger("weaponId", 0);
		_shieldId = set.getInteger("shieldId", 0);
		_chestId = set.getInteger("chestId", 0);
		_hairId = set.getInteger("hairId", 0);
		_faceId = set.getInteger("faceId", 0);
		_legsId = set.getInteger("legsId", 0);
		_glovesId = set.getInteger("glovesId", 0);
		_feetId = set.getInteger("feetId", 0);
		_priceId = set.getInteger("priceId", 0);
		_priceCount = set.getInteger("priceCount", 0);
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getWeaponId()
	{
		return _weaponId;
	}
	
	public int getShieldId()
	{
		return _shieldId;
	}
	
	public int getChestId()
	{
		return _chestId;
	}
	
	public int getHairId()
	{
		return _hairId;
	}
	
	public int getFaceId()
	{
		return _faceId;
	}
	
	public int getLegsId()
	{
		return _legsId;
	}
	
	public int getGlovesId()
	{
		return _glovesId;
	}
	
	public int getFeetId()
	{
		return _feetId;
	}
	
	public int getPriceId()
	{
		return _priceId;
	}
	
	public int getPriceCount()
	{
		return _priceCount;
	}
}