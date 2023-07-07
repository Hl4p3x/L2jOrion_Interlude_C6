package l2jorion.bots.xml;

import l2jorion.game.templates.StatsSet;

public class EquipPackage
{
	private String _type;
	private int _classId;
	private int _weaponId;
	private int _shieldId;
	private int _helmId;
	private int _chestId;
	private int _hairId;
	private int _faceId;
	private int _legsId;
	private int _glovesId;
	private int _feetId;
	private int _neckId;
	private int _learId;
	private int _rearId;
	private int _lfingerId;
	private int _rfingerId;
	
	public EquipPackage(StatsSet set)
	{
		_type = set.getString("type", "default");
		_classId = set.getInteger("classId", 0);
		_weaponId = set.getInteger("weaponId", 0);
		_shieldId = set.getInteger("shieldId", 0);
		_helmId = set.getInteger("helmId", 0);
		_chestId = set.getInteger("chestId", 0);
		_hairId = set.getInteger("hairId", 0);
		_faceId = set.getInteger("faceId", 0);
		_legsId = set.getInteger("legsId", 0);
		_glovesId = set.getInteger("glovesId", 0);
		_feetId = set.getInteger("feetId", 0);
		
		_neckId = set.getInteger("neckId", 0);
		_learId = set.getInteger("learId", 0);
		_rearId = set.getInteger("rearId", 0);
		_lfingerId = set.getInteger("lfingerId", 0);
		_rfingerId = set.getInteger("rfingerId", 0);
	}
	
	public int geClasstId()
	{
		return _classId;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public int getWeaponId()
	{
		return _weaponId;
	}
	
	public int getShieldId()
	{
		return _shieldId;
	}
	
	public int getHelmId()
	{
		return _helmId;
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
	
	public int getNeck()
	{
		return _neckId;
	}
	
	public int getLeftEarId()
	{
		return _learId;
	}
	
	public int getRightEarId()
	{
		return _rearId;
	}
	
	public int getLeftFingerId()
	{
		return _lfingerId;
	}
	
	public int getRightFingerId()
	{
		return _rfingerId;
	}
}