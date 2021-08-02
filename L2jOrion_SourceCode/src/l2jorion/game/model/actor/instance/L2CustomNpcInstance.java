/*
 * L2jOrion Project - www.l2jorion.com 
 * 
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
package l2jorion.game.model.actor.instance;

import l2jorion.game.managers.CustomNpcInstanceManager;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.base.Race;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.util.random.Rnd;

public final class L2CustomNpcInstance
{
	
	private boolean _allowRandomWeapons = true; // Default value
	private boolean _allowRandomClass = true; // Default value
	private boolean _allowRandomAppearance = true; // Default value
	private String _name;
	private String _title;
	
	private int _int[]; // PcInstance integer stats
	private boolean _boolean[]; // PcInstance booolean stats
	private L2NpcInstance _NpcInstance; // Reference to Npc with this stats
	private ClassId _classId; // ClassId of this (N)Pc
	
	/**
	 * A constructor
	 * @param myNpc - Receives the L2NpcInstance as a reference.
	 */
	public L2CustomNpcInstance(final L2NpcInstance myNpc)
	{
		_NpcInstance = myNpc;
		if (_NpcInstance == null)
		{
			return;
		}
		else if (_NpcInstance.getSpawn() == null)
		{
			return;
		}
		else
		{
			initialize();
		}
	}
	
	/**
	 * Initializes the semi PcInstance stats for this NpcInstance, making it appear as a PcInstance on all clients
	 */
	private final void initialize()
	{
		_int = new int[25];
		// karma=1, clanId=2, allyId=3, clanCrest=4, allyCrest=5, race=6, classId=7
		// EnchantWeapon=8, PledgeClass=9, CursedWeaponLevel=10
		// RightHand=11, LeftHand=12, Gloves=13, Chest=14, Legs=15, Feet=16, Hair1=17, Hair2=18
		// HairStyle=19, HairColor=20, Face=21
		// NameColor=22, TitleColor=23
		
		_boolean = new boolean[4];
		// pvp=0 , noble=1, hero=2, isFemaleSex=3
		
		// load the Pc Morph Data
		CustomNpcInstanceManager.customInfo ci = CustomNpcInstanceManager.getInstance().getCustomData(_NpcInstance.getSpawn().getId(), _NpcInstance.getNpcId());
		
		if (ci == null)
		{
			_NpcInstance.setCustomNpcInstance(null);
			_NpcInstance = null;
			return;
		}
		
		_NpcInstance.setCustomNpcInstance(this);
		
		setPcInstanceData(ci);
		
		if (_allowRandomClass)
		{
			chooseRandomClass();
		}
		if (_allowRandomAppearance)
		{
			chooseRandomAppearance();
		}
		if (_allowRandomWeapons)
		{
			chooseRandomWeapon();
		}
		
		ci = null;
	}
	
	/**
	 * @return the custom npc's name, or the original npc name if no custom name is provided
	 */
	public final String getName()
	{
		return _name == null ? _NpcInstance.getName() : _name;
	}
	
	/**
	 * @return the custom npc's title, or the original npc title if no custom title is provided
	 */
	public final String getTitle()
	{
		return _title == null ? _NpcInstance.getTitle() : _NpcInstance.isChampion() ? "The Champion" + _title : _title;
	}
	
	/**
	 * @return the npc's karma or aggro range if he has any...
	 */
	public final int getKarma()
	{
		return _int[1] > 0 ? _int[1] : _NpcInstance.getAggroRange();
	}
	
	/**
	 * @return the clan Id
	 */
	public final int getClanId()
	{
		return _int[2];
	}
	
	/**
	 * @return the ally Id
	 */
	public final int getAllyId()
	{
		return _int[3];
	}
	
	/**
	 * @return the clan crest Id
	 */
	public final int getClanCrestId()
	{
		return _int[4];
	}
	
	/**
	 * @return the ally crest Id
	 */
	public final int getAllyCrestId()
	{
		return _int[5];
	}
	
	/**
	 * @return the Race ordinal
	 */
	public final int getRace()
	{
		return _int[6];
	}
	
	/**
	 * @return the class id, e.g.: fighter, warrior, mystic muse...
	 */
	public final int getClassId()
	{
		return _int[7];
	}
	
	/**
	 * @return the enchant level of the equipped weapon, if one is equipped (max = 127)
	 */
	public final int getEnchantWeapon()
	{
		return PAPERDOLL_RHAND() == 0 || getCursedWeaponLevel() != 0 ? 0 : _int[8] > 127 ? 127 : _int[8];
	}
	
	/**
	 * @return the pledge class identifier, e.g. vagabond, baron, marquiz
	 * @remark Champion mobs are always Marquiz
	 */
	public final int getPledgeClass()
	{
		return _NpcInstance.isChampion() ? 8 : _int[9];
	}
	
	/**
	 * @return the cursed weapon level, if one is equipped
	 */
	public final int getCursedWeaponLevel()
	{
		return PAPERDOLL_RHAND() == 0 || _int[8] > 0 ? 0 : _int[10];
	}
	
	/**
	 * @return the item id for the item in the right hand, if a custom item is not equipped the value returned is the original npc right-hand weapon id
	 */
	public final int PAPERDOLL_RHAND()
	{
		return _int[11] != 0 ? _int[11] : _NpcInstance.getRightHandItem();
	}
	
	/**
	 * @return the item id for the item in the left hand, if a custom item is not equipped the value returned is the original npc left-hand weapon id. Setting this value _int[12] = -1 will not allow a npc to have anything in the left hand
	 */
	public final int PAPERDOLL_LHAND()
	{
		return _int[12] > 0 ? _int[12] : _int[12] == 0 ? _NpcInstance.getLeftHandItem() : 0;
	}
	
	/**
	 * @return the item id for the gloves
	 */
	public final int PAPERDOLL_GLOVES()
	{
		return _int[13];
	}
	
	/**
	 * @return the item id for the chest armor
	 */
	public final int PAPERDOLL_CHEST()
	{
		return _int[14];
	}
	
	/**
	 * @return the item id for the leg armor, or 0 if wearing a full armor
	 */
	public final int PAPERDOLL_LEGS()
	{
		return _int[15];
	}
	
	/**
	 * @return the item id for feet armor
	 */
	public final int PAPERDOLL_FEET()
	{
		return _int[16];
	}
	
	/**
	 * @return the item id for the 1st hair slot, or all hair
	 */
	public final int PAPERDOLL_HAIR()
	{
		return _int[17];
	}
	
	/**
	 * @return the item id for the 2nd hair slot
	 */
	public final int PAPERDOLL_HAIR2()
	{
		return _int[18];
	}
	
	/**
	 * @return the npc's hair style appearance
	 */
	public final int getHairStyle()
	{
		return _int[19];
	}
	
	/**
	 * @return the npc's hair color appearance
	 */
	public final int getHairColor()
	{
		return _int[20];
	}
	
	/**
	 * @return the npc's face appearance
	 */
	public final int getFace()
	{
		return _int[21];
	}
	
	/**
	 * @return the npc's name color (in hexadecimal), 0xFFFFFF is the default value
	 */
	public final int nameColor()
	{
		return _int[22] == 0 ? 0xFFFFFF : _int[22];
	}
	
	/**
	 * @return the npc's title color (in hexadecimal), 0xFFFF77 is the default value
	 */
	public final int titleColor()
	{
		return _int[23] == 0 ? 0xFFFF77 : _int[23];
	}
	
	/**
	 * @return is npc in pvp mode?
	 */
	public final boolean getPvpFlag()
	{
		return _boolean[0];
	}
	
	/**
	 * @return is npc in pvp mode?
	 */
	public final int getHeading()
	{
		return _NpcInstance.getHeading();
	}
	
	/**
	 * @return true if npc is a noble
	 */
	public final boolean isNoble()
	{
		return _boolean[1];
	}
	
	/**
	 * @return true if hero Glow should show up
	 * @remark A Champion mob will always have hero Glow
	 */
	public final boolean isHero()
	{
		return _NpcInstance.isChampion() ? true : _boolean[2];
	}
	
	/**
	 * @return true if female, false if male
	 * @remark In the DB, if you set
	 * @MALE value=0
	 * @FEMALE value=1
	 * @MAYBE value=2 % chance for the <b>Entire Template</b> to become male or female (it's a maybe value) If female, all template will be female, if Male, all template will be male
	 */
	public final boolean isFemaleSex()
	{
		return _boolean[3];
	}
	
	/**
	 * Choose a random weapon for this L2CustomNpcInstance
	 */
	private final void chooseRandomWeapon()
	{
		L2WeaponType wpnType = null;
		{
			wpnType = Rnd.get(100) > 40 ? L2WeaponType.BOW : L2WeaponType.BOW;
			{
				wpnType = L2WeaponType.BOW;
			}
		}
		{
			while (true) // Choose correct weapon TYPE
			{
				wpnType = L2WeaponType.values()[Rnd.get(L2WeaponType.values().length)];
				if (wpnType == null)
				{
					continue;
				}
				else if (wpnType == L2WeaponType.BOW || wpnType == L2WeaponType.BOW)
				{
					continue;
				}
				else if (_classId.getRace() == Race.human)
				{
				}
				break;
			}
		}
		if (Rnd.get(100) < 10)
		{
		}
	}
	
	/**
	 * Choose a random class & race for this L2CustomNpcInstance
	 */
	private final void chooseRandomClass()
	{
		while (true)
		{
			_classId = ClassId.values()[Rnd.get(ClassId.values().length)];
			if (_classId == null)
			{
				continue;
			}
			else if (_classId.getRace() != null && _classId.getParent() != null)
			{
				break;
			}
		}
		_int[6] = _classId.getRace().ordinal();
		_int[7] = _classId.getId();
	}
	
	/**
	 * Choose random appearance for this L2CustomNpcInstance
	 */
	private final void chooseRandomAppearance()
	{
		// Karma=1, PledgeClass=9
		// HairStyle=19, HairColor=20, Face=21
		// NameColor=22, TitleColor=23
		// noble=1, hero=2, isFemaleSex=3
		_boolean[1] = Rnd.get(100) < 15 ? true : false;
		_boolean[3] = Rnd.get(100) < 50 ? true : false;
		_int[22] = _int[23] = 0;
		if (Rnd.get(100) < 5)
		{
			_int[22] = 0x0000FF;
		}
		else if (Rnd.get(100) < 5)
		{
			_int[22] = 0x00FF00;
		}
		if (Rnd.get(100) < 5)
		{
			_int[23] = 0x0000FF;
		}
		else if (Rnd.get(100) < 5)
		{
			_int[23] = 0x00FF00;
		}
		_int[1] = Rnd.get(100) > 95 ? 0 : Rnd.get(100) > 10 ? 50 : 1000;
		_int[19] = Rnd.get(100) < 34 ? 0 : Rnd.get(100) < 34 ? 1 : 2;
		_int[20] = Rnd.get(100) < 34 ? 0 : Rnd.get(100) < 34 ? 1 : 2;
		_int[21] = Rnd.get(100) < 34 ? 0 : Rnd.get(100) < 34 ? 1 : 2;
		
		final int pledgeLevel = Rnd.get(100);
		// 30% is left for either pledge=0 or default sql data
		// Only Marqiz are Champion mobs
		if (pledgeLevel > 30)
		{
			_int[9] = 1;
		}
		if (pledgeLevel > 50)
		{
			_int[9] = 2;
		}
		if (pledgeLevel > 60)
		{
			_int[9] = 3;
		}
		if (pledgeLevel > 80)
		{
			_int[9] = 4;
		}
		if (pledgeLevel > 90)
		{
			_int[9] = 5;
		}
		if (pledgeLevel > 95)
		{
			_int[9] = 6;
		}
		if (pledgeLevel > 98)
		{
			_int[9] = 7;
		}
	}
	
	/**
	 * Sets the data received from the CustomNpcInstanceManager
	 * @param ci the customInfo data
	 */
	public void setPcInstanceData(final CustomNpcInstanceManager.customInfo ci)
	{
		if (ci == null)
			return;
		
		// load the "massive" data
		for (int i = 0; i < 25; i++)
		{
			_int[i] = ci.integerData[i];
		}
		for (int i = 0; i < 4; i++)
		{
			_boolean[i] = ci.booleanData[i];
		}
		
		// random variables to apply to this L2NpcInstance polymorph
		_allowRandomClass = ci.booleanData[4];
		_allowRandomAppearance = ci.booleanData[5];
		_allowRandomWeapons = ci.booleanData[6];
		
		// name & title override
		_name = ci.stringData[0];
		_title = ci.stringData[1];
		if (_name != null && _name.equals(""))
		{
			_name = null;
		}
		if (_title != null && _title.equals(""))
		{
			_title = null;
		}
		
		// Not really necessary but maybe called upon on wrong random settings:
		// Initiate this PcInstance class id to the correct pcInstance class.
		final ClassId ids[] = ClassId.values();
		if (ids != null)
		{
			for (final ClassId id : ids)
				if (id == null)
				{
					continue;
				}
				else if (id.getId() == _int[7])
				{
					_classId = id;
					_int[6] = id.getRace().ordinal();
					break;
				}
		}
	}
}
