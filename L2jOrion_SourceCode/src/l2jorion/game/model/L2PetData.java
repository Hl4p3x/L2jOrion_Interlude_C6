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

public class L2PetData
{
	public static final String PET_TYPE = "typeID";
	public static final String PET_LEVEL = "level";
	public static final String PET_MAX_EXP = "expMax";
	public static final String PET_MAX_HP = "hpMax";
	public static final String PET_MAX_MP = "mpMax";
	public static final String PET_PATK = "patk";
	public static final String PET_PDEF = "pdef";
	public static final String PET_MATK = "matk";
	public static final String PET_MDEF = "mdef";
	public static final String PET_ACCURACY = "acc";
	public static final String PET_EVASION = "evasion";
	public static final String PET_CRITICAL = "crit";
	public static final String PET_SPEED = "speed";
	public static final String PET_ATK_SPEED = "atk_speed";
	public static final String PET_CAST_SPEED = "cast_speed";
	public static final String PET_MAX_FEED = "feedMax";
	public static final String PET_FEED_BATTLE = "feedbattle";
	public static final String PET_FEED_NORMAL = "feednormal";
	public static final String PET_MAX_LOAD = "loadMax";
	public static final String PET_REGEN_HP = "hpregen";
	public static final String PET_REGEN_MP = "mpregen";
	public static final String OWNER_EXP_TAKEN = "owner_exp_taken";
	
	private int _petId;
	private int _petLevel;
	private float _ownerExpTaken;
	private long _petMaxExp;
	private int _petMaxHP;
	private int _petMaxMP;
	private int _petPAtk;
	private int _petPDef;
	private int _petMAtk;
	private int _petMDef;
	private int _petAccuracy;
	private int _petEvasion;
	private int _petCritical;
	private int _petSpeed;
	private int _petAtkSpeed;
	private int _petCastSpeed;
	private int _petMaxFeed;
	private int _petFeedBattle;
	private int _petFeedNormal;
	private int _petMaxLoad;
	private int _petRegenHP;
	private int _petRegenMP;
	
	public void setStat(final String stat, final int value)
	{
		if (stat.equalsIgnoreCase(PET_MAX_EXP))
		{
			setPetMaxExp(value);
		}
		
		else if (stat.equalsIgnoreCase(PET_MAX_HP))
		{
			setPetMaxHP(value);
		}
		
		else if (stat.equalsIgnoreCase(PET_MAX_MP))
		{
			setPetMaxMP(value);
		}
		else if (stat.equalsIgnoreCase(PET_PATK))
		{
			setPetPAtk(value);
		}
		else if (stat.equalsIgnoreCase(PET_PDEF))
		{
			setPetPDef(value);
		}
		else if (stat.equalsIgnoreCase(PET_MATK))
		{
			setPetMAtk(value);
		}
		else if (stat.equalsIgnoreCase(PET_MDEF))
		{
			setPetMDef(value);
		}
		else if (stat.equalsIgnoreCase(PET_ACCURACY))
		{
			setPetAccuracy(value);
		}
		else if (stat.equalsIgnoreCase(PET_EVASION))
		{
			setPetEvasion(value);
		}
		else if (stat.equalsIgnoreCase(PET_CRITICAL))
		{
			setPetCritical(value);
		}
		else if (stat.equalsIgnoreCase(PET_SPEED))
		{
			setPetSpeed(value);
		}
		else if (stat.equalsIgnoreCase(PET_ATK_SPEED))
		{
			setPetAtkSpeed(value);
		}
		else if (stat.equalsIgnoreCase(PET_CAST_SPEED))
		{
			setPetCastSpeed(value);
		}
		
		else if (stat.equalsIgnoreCase(PET_MAX_FEED))
		{
			setPetMaxFeed(value);
		}
		else if (stat.equalsIgnoreCase(PET_FEED_NORMAL))
		{
			setPetFeedNormal(value);
		}
		else if (stat.equalsIgnoreCase(PET_FEED_BATTLE))
		{
			setPetFeedBattle(value);
		}
		
		else if (stat.equalsIgnoreCase(PET_MAX_LOAD))
		{
			setPetMaxLoad(value);
		}
		else if (stat.equalsIgnoreCase(PET_REGEN_HP))
		{
			setPetRegenHP(value);
		}
		else if (stat.equalsIgnoreCase(PET_REGEN_MP))
		{
			setPetRegenMP(value);
		}
	}
	
	public void setStat(final String stat, final long value)
	{
		if (stat.equalsIgnoreCase(PET_MAX_EXP))
		{
			setPetMaxExp(value);
		}
	}
	
	public void setStat(final String stat, final float value)
	{
		if (stat.equalsIgnoreCase(OWNER_EXP_TAKEN))
		{
			setOwnerExpTaken(value);
		}
	}
	
	// ID
	public int getPetID()
	{
		return _petId;
	}
	
	public void setPetID(final int pPetID)
	{
		_petId = pPetID;
	}
	
	// Level
	public int getPetLevel()
	{
		return _petLevel;
	}
	
	public void setPetLevel(final int pPetLevel)
	{
		_petLevel = pPetLevel;
	}
	
	public long getPetMaxExp()
	{
		return _petMaxExp;
	}
	
	public void setPetMaxExp(final long pPetMaxExp)
	{
		_petMaxExp = pPetMaxExp;
	}
	
	public float getOwnerExpTaken()
	{
		return _ownerExpTaken;
	}
	
	public void setOwnerExpTaken(final float pOwnerExpTaken)
	{
		_ownerExpTaken = pOwnerExpTaken;
	}
	
	public int getPetMaxHP()
	{
		return _petMaxHP;
	}
	
	public void setPetMaxHP(final int pPetMaxHP)
	{
		_petMaxHP = pPetMaxHP;
	}
	
	public int getPetMaxMP()
	{
		return _petMaxMP;
	}
	
	public void setPetMaxMP(final int pPetMaxMP)
	{
		_petMaxMP = pPetMaxMP;
	}
	
	// PAtk
	public int getPetPAtk()
	{
		return _petPAtk;
	}
	
	public void setPetPAtk(final int pPetPAtk)
	{
		_petPAtk = pPetPAtk;
	}
	
	// PDef
	public int getPetPDef()
	{
		return _petPDef;
	}
	
	public void setPetPDef(final int pPetPDef)
	{
		_petPDef = pPetPDef;
	}
	
	// MAtk
	public int getPetMAtk()
	{
		return _petMAtk;
	}
	
	public void setPetMAtk(final int pPetMAtk)
	{
		_petMAtk = pPetMAtk;
	}
	
	// MDef
	public int getPetMDef()
	{
		return _petMDef;
	}
	
	public void setPetMDef(final int pPetMDef)
	{
		_petMDef = pPetMDef;
	}
	
	// Accuracy
	public int getPetAccuracy()
	{
		return _petAccuracy;
	}
	
	public void setPetAccuracy(final int pPetAccuracy)
	{
		_petAccuracy = pPetAccuracy;
	}
	
	// Evasion
	public int getPetEvasion()
	{
		return _petEvasion;
	}
	
	public void setPetEvasion(final int pPetEvasion)
	{
		_petEvasion = pPetEvasion;
	}
	
	// Critical
	public int getPetCritical()
	{
		return _petCritical;
	}
	
	public void setPetCritical(final int pPetCritical)
	{
		_petCritical = pPetCritical;
	}
	
	// Speed
	public int getPetSpeed()
	{
		return _petSpeed;
	}
	
	public void setPetSpeed(final int pPetSpeed)
	{
		_petSpeed = pPetSpeed;
	}
	
	// Atk Speed
	public int getPetAtkSpeed()
	{
		return _petAtkSpeed;
	}
	
	public void setPetAtkSpeed(final int pPetAtkSpeed)
	{
		_petAtkSpeed = pPetAtkSpeed;
	}
	
	// Cast Speed
	public int getPetCastSpeed()
	{
		return _petCastSpeed;
	}
	
	public void setPetCastSpeed(final int pPetCastSpeed)
	{
		_petCastSpeed = pPetCastSpeed;
	}
	
	// MaxFeed
	public int getPetMaxFeed()
	{
		return _petMaxFeed;
	}
	
	public void setPetMaxFeed(final int pPetMaxFeed)
	{
		_petMaxFeed = pPetMaxFeed;
	}
	
	// Normal Feed
	public int getPetFeedNormal()
	{
		return _petFeedNormal;
	}
	
	public void setPetFeedNormal(final int pPetFeedNormal)
	{
		_petFeedNormal = pPetFeedNormal;
	}
	
	// Battle Feed
	public int getPetFeedBattle()
	{
		return _petFeedBattle;
	}
	
	public void setPetFeedBattle(final int pPetFeedBattle)
	{
		_petFeedBattle = pPetFeedBattle;
	}
	
	// Max Load
	public int getPetMaxLoad()
	{
		return _petMaxLoad;
	}
	
	public void setPetMaxLoad(final int pPetMaxLoad)
	{
		_petMaxLoad = pPetMaxLoad;
	}
	
	// Regen HP
	public int getPetRegenHP()
	{
		return _petRegenHP;
	}
	
	public void setPetRegenHP(final int pPetRegenHP)
	{
		_petRegenHP = pPetRegenHP;
	}
	
	// Regen MP
	public int getPetRegenMP()
	{
		return _petRegenMP;
	}
	
	public void setPetRegenMP(final int pPetRegenMP)
	{
		_petRegenMP = pPetRegenMP;
	}
	
	@Override
	public String toString()
	{
		return "PetID: " + getPetID() + " \t" + "PetLevel: " + getPetLevel() + " \t" +
		// PET_EXP + ": " + getPetExp() + " \t" +
			PET_MAX_EXP + ": " + getPetMaxExp() + " \t" +
			// PET_HP + ": " + getPetHP() + " \t" +
			PET_MAX_HP + ": " + getPetMaxHP() + " \t" +
			// PET_MP + ": " + getPetMP() + " \t" +
			PET_MAX_MP + ": " + getPetMaxMP() + " \t" + PET_PATK + ": " + getPetPAtk() + " \t" + PET_PDEF + ": " + getPetPDef() + " \t" + PET_MATK + ": " + getPetMAtk() + " \t" + PET_MDEF + ": " + getPetMDef() + " \t" + PET_ACCURACY + ": " + getPetAccuracy() + " \t" + PET_EVASION + ": "
			+ getPetEvasion() + " \t" + PET_CRITICAL + ": " + getPetCritical() + " \t" + PET_SPEED + ": " + getPetSpeed() + " \t" + PET_ATK_SPEED + ": " + getPetAtkSpeed() + " \t" + PET_CAST_SPEED + ": " + getPetCastSpeed() + " \t" +
			// PET_FEED + ": " + getPetFeed() + " \t" +
			PET_MAX_FEED + ": " + getPetMaxFeed() + " \t" + PET_FEED_BATTLE + ": " + getPetFeedBattle() + " \t" + PET_FEED_NORMAL + ": " + getPetFeedNormal() + " \t" +
			// PET_LOAD + ": " + getPetLoad() + " \t" +
			PET_MAX_LOAD + ": " + getPetMaxLoad() + " \t" + PET_REGEN_HP + ": " + getPetRegenHP() + " \t" + PET_REGEN_MP + ": " + getPetRegenMP();
	}
	
}
