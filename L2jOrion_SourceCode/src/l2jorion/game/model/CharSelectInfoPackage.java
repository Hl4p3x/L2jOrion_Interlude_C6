/*
 * L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.model;

public class CharSelectInfoPackage
{
	private String _name;
	private int _objectId = 0;
	private int _charId = 0x00030b7a;
	private long _exp = 0;
	private int _sp = 0;
	private int _clanId = 0;
	private int _race = 0;
	private int _classId = 0;
	private int _baseClassId = 0;
	private long _deleteTimer = 0L;
	private long _lastAccess = 0L;
	private int _face = 0;
	private int _hairStyle = 0;
	private int _hairColor = 0;
	private int _sex = 0;
	private int _level = 1;
	private int _maxHp = 0;
	private double _currentHp = 0;
	private int _maxMp = 0;
	private double _currentMp = 0;
	private final int[][] _paperdoll;
	private int _karma = 0;
	private int _augmentationId = 0;
	private int _accessLevel;
	
	private int _armorSkinOption = 0;
	private int _weaponSkinOption = 0;
	private int _hairSkinOption = 0;
	private int _faceSkinOption = 0;
	
	public CharSelectInfoPackage(final int objectId, final String name)
	{
		setObjectId(objectId);
		_name = name;
		_paperdoll = PcInventory.restoreVisibleInventory(objectId);
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public void setObjectId(final int objectId)
	{
		_objectId = objectId;
	}
	
	public int getCharId()
	{
		return _charId;
	}
	
	public void setCharId(final int charId)
	{
		_charId = charId;
	}
	
	public int getClanId()
	{
		return _clanId;
	}
	
	public void setClanId(final int clanId)
	{
		_clanId = clanId;
	}
	
	public int getClassId()
	{
		return _classId;
	}
	
	public int getBaseClassId()
	{
		return _baseClassId;
	}
	
	public void setClassId(final int classId)
	{
		_classId = classId;
	}
	
	public void setBaseClassId(final int baseClassId)
	{
		_baseClassId = baseClassId;
	}
	
	public double getCurrentHp()
	{
		return _currentHp;
	}
	
	public void setCurrentHp(final double currentHp)
	{
		_currentHp = currentHp;
	}
	
	public double getCurrentMp()
	{
		return _currentMp;
	}
	
	public void setCurrentMp(final double currentMp)
	{
		_currentMp = currentMp;
	}
	
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	public void setDeleteTimer(final long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	public void setLastAccess(final long lastAccess)
	{
		_lastAccess = lastAccess;
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public void setExp(final long exp)
	{
		_exp = exp;
	}
	
	public int getFace()
	{
		return _face;
	}
	
	public void setFace(final int face)
	{
		_face = face;
	}
	
	public int getHairColor()
	{
		return _hairColor;
	}
	
	public void setHairColor(final int hairColor)
	{
		_hairColor = hairColor;
	}
	
	public int getHairStyle()
	{
		return _hairStyle;
	}
	
	public void setHairStyle(final int hairStyle)
	{
		_hairStyle = hairStyle;
	}
	
	public int getPaperdollObjectId(final int slot)
	{
		return _paperdoll[slot][0];
	}
	
	public int getPaperdollItemId(final int slot)
	{
		return _paperdoll[slot][1];
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public void setLevel(final int level)
	{
		_level = level;
	}
	
	public int getMaxHp()
	{
		return _maxHp;
	}
	
	public void setMaxHp(final int maxHp)
	{
		_maxHp = maxHp;
	}
	
	public int getMaxMp()
	{
		return _maxMp;
	}
	
	public void setMaxMp(final int maxMp)
	{
		_maxMp = maxMp;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(final String name)
	{
		_name = name;
	}
	
	public int getRace()
	{
		return _race;
	}
	
	public void setRace(final int race)
	{
		_race = race;
	}
	
	public int getSex()
	{
		return _sex;
	}
	
	public void setSex(final int sex)
	{
		_sex = sex;
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public void setSp(final int sp)
	{
		_sp = sp;
	}
	
	public int getEnchantEffect()
	{
		if (_paperdoll[Inventory.PAPERDOLL_RHAND][2] > 0)
		{
			return _paperdoll[Inventory.PAPERDOLL_RHAND][2];
		}
		
		return _paperdoll[Inventory.PAPERDOLL_LRHAND][2];
	}
	
	public void setKarma(final int k)
	{
		_karma = k;
	}
	
	public int getKarma()
	{
		return _karma;
	}
	
	public void setAugmentationId(final int augmentationId)
	{
		_augmentationId = augmentationId;
	}
	
	public int getAugmentationId()
	{
		return _augmentationId;
	}
	
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	public void setAccessLevel(final int accessLevel)
	{
		_accessLevel = accessLevel;
	}
	
	public void setArmorSkinOption(int armorSkinOption)
	{
		_armorSkinOption = armorSkinOption;
	}
	
	public int getArmorSkinOption()
	{
		return _armorSkinOption;
	}
	
	public void setWeaponSkinOption(int armorSkinOption)
	{
		_weaponSkinOption = armorSkinOption;
	}
	
	public int getWeaponSkinOption()
	{
		return _weaponSkinOption;
	}
	
	public void setHairSkinOption(int hairSkinOption)
	{
		_hairSkinOption = hairSkinOption;
	}
	
	public int getHairSkinOption()
	{
		return _hairSkinOption;
	}
	
	public void setFaceSkinOption(int faceSkinOption)
	{
		_faceSkinOption = faceSkinOption;
	}
	
	public int getFaceSkinOption()
	{
		return _faceSkinOption;
	}
}
