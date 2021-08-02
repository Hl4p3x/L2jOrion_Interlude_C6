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
package l2jorion.game.model.base;

public enum ClassId
{
	/** The fighter. */
	fighter(0x00, false, Race.human, null),
	
	/** The warrior. */
	warrior(0x01, false, Race.human, fighter),
	
	/** The gladiator. */
	gladiator(0x02, false, Race.human, warrior),
	
	/** The warlord. */
	warlord(0x03, false, Race.human, warrior),
	
	/** The knight. */
	knight(0x04, false, Race.human, fighter),
	
	/** The paladin. */
	paladin(0x05, false, Race.human, knight),
	
	/** The dark avenger. */
	darkAvenger(0x06, false, Race.human, knight),
	
	/** The rogue. */
	rogue(0x07, false, Race.human, fighter),
	
	/** The treasure hunter. */
	treasureHunter(0x08, false, Race.human, rogue),
	
	/** The hawkeye. */
	hawkeye(0x09, false, Race.human, rogue),
	
	/** The mage. */
	mage(0x0a, true, Race.human, null),
	
	/** The wizard. */
	wizard(0x0b, true, Race.human, mage),
	
	/** The sorceror. */
	sorceror(0x0c, true, Race.human, wizard),
	
	/** The necromancer. */
	necromancer(0x0d, true, Race.human, wizard),
	
	/** The warlock. */
	warlock(0x0e, true, Race.human, wizard),
	
	/** The cleric. */
	cleric(0x0f, true, Race.human, mage),
	
	/** The bishop. */
	bishop(0x10, true, Race.human, cleric),
	
	/** The prophet. */
	prophet(0x11, true, Race.human, cleric),
	
	/** The elven fighter. */
	elvenFighter(0x12, false, Race.elf, null),
	
	/** The elven knight. */
	elvenKnight(0x13, false, Race.elf, elvenFighter),
	
	/** The temple knight. */
	templeKnight(0x14, false, Race.elf, elvenKnight),
	
	/** The sword singer. */
	swordSinger(0x15, false, Race.elf, elvenKnight),
	
	/** The elven scout. */
	elvenScout(0x16, false, Race.elf, elvenFighter),
	
	/** The plains walker. */
	plainsWalker(0x17, false, Race.elf, elvenScout),
	
	/** The silver ranger. */
	silverRanger(0x18, false, Race.elf, elvenScout),
	
	/** The elven mage. */
	elvenMage(0x19, true, Race.elf, null),
	
	/** The elven wizard. */
	elvenWizard(0x1a, true, Race.elf, elvenMage),
	
	/** The spellsinger. */
	spellsinger(0x1b, true, Race.elf, elvenWizard),
	
	/** The elemental summoner. */
	elementalSummoner(0x1c, true, Race.elf, elvenWizard),
	
	/** The oracle. */
	oracle(0x1d, true, Race.elf, elvenMage),
	
	/** The elder. */
	elder(0x1e, true, Race.elf, oracle),
	
	/** The dark fighter. */
	darkFighter(0x1f, false, Race.darkelf, null),
	
	/** The palus knight. */
	palusKnight(0x20, false, Race.darkelf, darkFighter),
	
	/** The shillien knight. */
	shillienKnight(0x21, false, Race.darkelf, palusKnight),
	
	/** The bladedancer. */
	bladedancer(0x22, false, Race.darkelf, palusKnight),
	
	/** The assassin. */
	assassin(0x23, false, Race.darkelf, darkFighter),
	
	/** The abyss walker. */
	abyssWalker(0x24, false, Race.darkelf, assassin),
	
	/** The phantom ranger. */
	phantomRanger(0x25, false, Race.darkelf, assassin),
	
	/** The dark mage. */
	darkMage(0x26, true, Race.darkelf, null),
	
	/** The dark wizard. */
	darkWizard(0x27, true, Race.darkelf, darkMage),
	
	/** The spellhowler. */
	spellhowler(0x28, true, Race.darkelf, darkWizard),
	
	/** The phantom summoner. */
	phantomSummoner(0x29, true, Race.darkelf, darkWizard),
	
	/** The shillien oracle. */
	shillienOracle(0x2a, true, Race.darkelf, darkMage),
	
	/** The shillen elder. */
	shillenElder(0x2b, true, Race.darkelf, shillienOracle),
	
	/** The orc fighter. */
	orcFighter(0x2c, false, Race.orc, null),
	
	/** The orc raider. */
	orcRaider(0x2d, false, Race.orc, orcFighter),
	
	/** The destroyer. */
	destroyer(0x2e, false, Race.orc, orcRaider),
	
	/** The orc monk. */
	orcMonk(0x2f, false, Race.orc, orcFighter),
	
	/** The tyrant. */
	tyrant(0x30, false, Race.orc, orcMonk),
	
	/** The orc mage. */
	orcMage(0x31, true, Race.orc, null),
	
	/** The orc shaman. */
	orcShaman(0x32, true, Race.orc, orcMage),
	
	/** The overlord. */
	overlord(0x33, true, Race.orc, orcShaman),
	
	/** The warcryer. */
	warcryer(0x34, true, Race.orc, orcShaman),
	
	/** The dwarven fighter. */
	dwarvenFighter(0x35, false, Race.dwarf, null),
	
	/** The scavenger. */
	scavenger(0x36, false, Race.dwarf, dwarvenFighter),
	
	/** The bounty hunter. */
	bountyHunter(0x37, false, Race.dwarf, scavenger),
	
	/** The artisan. */
	artisan(0x38, false, Race.dwarf, dwarvenFighter),
	
	/** The warsmith. */
	warsmith(0x39, false, Race.dwarf, artisan),
	
	/*
	 * Dummy Entries (id's already in decimal format) btw FU NCSoft for the amount of work you put me through to do this!! <START>
	 */
	/** The dummy entry1. */
	dummyEntry1(58, false, null, null),
	
	/** The dummy entry2. */
	dummyEntry2(59, false, null, null),
	
	/** The dummy entry3. */
	dummyEntry3(60, false, null, null),
	
	/** The dummy entry4. */
	dummyEntry4(61, false, null, null),
	
	/** The dummy entry5. */
	dummyEntry5(62, false, null, null),
	
	/** The dummy entry6. */
	dummyEntry6(63, false, null, null),
	
	/** The dummy entry7. */
	dummyEntry7(64, false, null, null),
	
	/** The dummy entry8. */
	dummyEntry8(65, false, null, null),
	
	/** The dummy entry9. */
	dummyEntry9(66, false, null, null),
	
	/** The dummy entry10. */
	dummyEntry10(67, false, null, null),
	
	/** The dummy entry11. */
	dummyEntry11(68, false, null, null),
	
	/** The dummy entry12. */
	dummyEntry12(69, false, null, null),
	
	/** The dummy entry13. */
	dummyEntry13(70, false, null, null),
	
	/** The dummy entry14. */
	dummyEntry14(71, false, null, null),
	
	/** The dummy entry15. */
	dummyEntry15(72, false, null, null),
	
	/** The dummy entry16. */
	dummyEntry16(73, false, null, null),
	
	/** The dummy entry17. */
	dummyEntry17(74, false, null, null),
	
	/** The dummy entry18. */
	dummyEntry18(75, false, null, null),
	
	/** The dummy entry19. */
	dummyEntry19(76, false, null, null),
	
	/** The dummy entry20. */
	dummyEntry20(77, false, null, null),
	
	/** The dummy entry21. */
	dummyEntry21(78, false, null, null),
	
	/** The dummy entry22. */
	dummyEntry22(79, false, null, null),
	
	/** The dummy entry23. */
	dummyEntry23(80, false, null, null),
	
	/** The dummy entry24. */
	dummyEntry24(81, false, null, null),
	
	/** The dummy entry25. */
	dummyEntry25(82, false, null, null),
	
	/** The dummy entry26. */
	dummyEntry26(83, false, null, null),
	
	/** The dummy entry27. */
	dummyEntry27(84, false, null, null),
	
	/** The dummy entry28. */
	dummyEntry28(85, false, null, null),
	
	/** The dummy entry29. */
	dummyEntry29(86, false, null, null),
	
	/** The dummy entry30. */
	dummyEntry30(87, false, null, null),
	/*
	 * <END> Of Dummy entries
	 */
	
	/*
	 * Now the bad boys! new class ids :)) (3rd classes)
	 */
	/** The duelist. */
	duelist(0x58, false, Race.human, gladiator),
	
	/** The dreadnought. */
	dreadnought(0x59, false, Race.human, warlord),
	
	/** The phoenix knight. */
	phoenixKnight(0x5a, false, Race.human, paladin),
	
	/** The hell knight. */
	hellKnight(0x5b, false, Race.human, darkAvenger),
	
	/** The sagittarius. */
	sagittarius(0x5c, false, Race.human, hawkeye),
	
	/** The adventurer. */
	adventurer(0x5d, false, Race.human, treasureHunter),
	
	/** The archmage. */
	archmage(0x5e, true, Race.human, sorceror),
	
	/** The soultaker. */
	soultaker(0x5f, true, Race.human, necromancer),
	
	/** The arcana lord. */
	arcanaLord(0x60, true, Race.human, warlock),
	
	/** The cardinal. */
	cardinal(0x61, true, Race.human, bishop),
	
	/** The hierophant. */
	hierophant(0x62, true, Race.human, prophet),
	
	/** The eva templar. */
	evaTemplar(0x63, false, Race.elf, templeKnight),
	
	/** The sword muse. */
	swordMuse(0x64, false, Race.elf, swordSinger),
	
	/** The wind rider. */
	windRider(0x65, false, Race.elf, plainsWalker),
	
	/** The moonlight sentinel. */
	moonlightSentinel(0x66, false, Race.elf, silverRanger),
	
	/** The mystic muse. */
	mysticMuse(0x67, true, Race.elf, spellsinger),
	
	/** The elemental master. */
	elementalMaster(0x68, true, Race.elf, elementalSummoner),
	
	/** The eva saint. */
	evaSaint(0x69, true, Race.elf, elder),
	
	/** The shillien templar. */
	shillienTemplar(0x6a, false, Race.darkelf, shillienKnight),
	
	/** The spectral dancer. */
	spectralDancer(0x6b, false, Race.darkelf, bladedancer),
	
	/** The ghost hunter. */
	ghostHunter(0x6c, false, Race.darkelf, abyssWalker),
	
	/** The ghost sentinel. */
	ghostSentinel(0x6d, false, Race.darkelf, phantomRanger),
	
	/** The storm screamer. */
	stormScreamer(0x6e, true, Race.darkelf, spellhowler),
	
	/** The spectral master. */
	spectralMaster(0x6f, true, Race.darkelf, phantomSummoner),
	
	/** The shillien saint. */
	shillienSaint(0x70, true, Race.darkelf, shillenElder),
	
	/** The titan. */
	titan(0x71, false, Race.orc, destroyer),
	
	/** The grand khauatari. */
	grandKhauatari(0x72, false, Race.orc, tyrant),
	
	/** The dominator. */
	dominator(0x73, true, Race.orc, overlord),
	
	/** The doomcryer. */
	doomcryer(0x74, true, Race.orc, warcryer),
	
	/** The fortune seeker. */
	fortuneSeeker(0x75, false, Race.dwarf, bountyHunter),
	
	/** The maestro. */
	maestro(0x76, false, Race.dwarf, warsmith);
	
	/** The Identifier of the Class. */
	private final int _id;
	
	/** True if the class is a mage class. */
	private final boolean _isMage;
	
	/** The Race object of the class. */
	private final Race _race;
	
	/** The parent ClassId or null if this class is a root. */
	private final ClassId _parent;
	
	public static final ClassId[] VALUES = values();
	
	/**
	 * Constructor of ClassId.<BR>
	 * <BR>
	 * @param pId the id
	 * @param pIsMage the is mage
	 * @param pRace the race
	 * @param pParent the parent
	 */
	private ClassId(final int pId, final boolean pIsMage, final Race pRace, final ClassId pParent)
	{
		_id = pId;
		_isMage = pIsMage;
		_race = pRace;
		_parent = pParent;
	}
	
	/**
	 * Return the Identifier of the Class.<BR>
	 * <BR>
	 * @return the id
	 */
	public final int getId()
	{
		return _id;
	}
	
	/**
	 * Return True if the class is a mage class.<BR>
	 * <BR>
	 * @return true, if is mage
	 */
	public final boolean isMage()
	{
		return _isMage;
	}
	
	/**
	 * Return the Race object of the class.<BR>
	 * <BR>
	 * @return the race
	 */
	public final Race getRace()
	{
		return _race;
	}
	
	/**
	 * Return True if this Class is a child of the selected ClassId.<BR>
	 * <BR>
	 * @param cid The parent ClassId to check
	 * @return true, if successful
	 */
	public final boolean childOf(final ClassId cid)
	{
		if (_parent == null)
		{
			return false;
		}
		
		if (_parent == cid)
		{
			return true;
		}
		
		return _parent.childOf(cid);
		
	}
	
	/**
	 * Return True if this Class is equal to the selected ClassId or a child of the selected ClassId.<BR>
	 * <BR>
	 * @param cid The parent ClassId to check
	 * @return true, if successful
	 */
	public final boolean equalsOrChildOf(final ClassId cid)
	{
		return this == cid || childOf(cid);
	}
	
	/**
	 * Return the child level of this Class (0=root, 1=child leve 1...).<BR>
	 * <BR>
	 * @return the int
	 */
	public final int level()
	{
		if (_parent == null)
		{
			return 0;
		}
		
		return 1 + _parent.level();
	}
	
	/**
	 * Return its parent ClassId<BR>
	 * <BR>
	 * .
	 * @return the parent
	 */
	public final ClassId getParent()
	{
		return _parent;
	}
	
	public static ClassId getClassIdByOrdinal(final int id)
	{
		
		for (final ClassId current : values())
		{
			
			if (current._id == id)
			{
				return current;
			}
			
		}
		
		return null;
		
	}
}
