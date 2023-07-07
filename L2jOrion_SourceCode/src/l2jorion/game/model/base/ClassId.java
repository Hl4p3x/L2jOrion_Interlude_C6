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
	fighter(0x00, false, Race.human, null),
	warrior(0x01, false, Race.human, fighter),
	gladiator(0x02, false, Race.human, warrior),
	warlord(0x03, false, Race.human, warrior),
	knight(0x04, false, Race.human, fighter),
	paladin(0x05, false, Race.human, knight),
	darkAvenger(0x06, false, Race.human, knight),
	rogue(0x07, false, Race.human, fighter),
	treasureHunter(0x08, false, Race.human, rogue),
	hawkeye(0x09, false, Race.human, rogue),
	mage(0x0a, true, Race.human, null),
	wizard(0x0b, true, Race.human, mage),
	sorceror(0x0c, true, Race.human, wizard),
	necromancer(0x0d, true, Race.human, wizard),
	warlock(0x0e, true, Race.human, wizard),
	cleric(0x0f, true, Race.human, mage),
	bishop(0x10, true, Race.human, cleric),
	prophet(0x11, true, Race.human, cleric),
	elvenFighter(0x12, false, Race.elf, null),
	elvenKnight(0x13, false, Race.elf, elvenFighter),
	templeKnight(0x14, false, Race.elf, elvenKnight),
	swordSinger(0x15, false, Race.elf, elvenKnight),
	elvenScout(0x16, false, Race.elf, elvenFighter),
	plainsWalker(0x17, false, Race.elf, elvenScout),
	silverRanger(0x18, false, Race.elf, elvenScout),
	elvenMage(0x19, true, Race.elf, null),
	elvenWizard(0x1a, true, Race.elf, elvenMage),
	spellsinger(0x1b, true, Race.elf, elvenWizard),
	elementalSummoner(0x1c, true, Race.elf, elvenWizard),
	oracle(0x1d, true, Race.elf, elvenMage),
	elder(0x1e, true, Race.elf, oracle),
	darkFighter(0x1f, false, Race.darkelf, null),
	palusKnight(0x20, false, Race.darkelf, darkFighter),
	shillienKnight(0x21, false, Race.darkelf, palusKnight),
	bladedancer(0x22, false, Race.darkelf, palusKnight),
	assassin(0x23, false, Race.darkelf, darkFighter),
	abyssWalker(0x24, false, Race.darkelf, assassin),
	phantomRanger(0x25, false, Race.darkelf, assassin),
	darkMage(0x26, true, Race.darkelf, null),
	darkWizard(0x27, true, Race.darkelf, darkMage),
	spellhowler(0x28, true, Race.darkelf, darkWizard),
	phantomSummoner(0x29, true, Race.darkelf, darkWizard),
	shillienOracle(0x2a, true, Race.darkelf, darkMage),
	shillenElder(0x2b, true, Race.darkelf, shillienOracle),
	orcFighter(0x2c, false, Race.orc, null),
	orcRaider(0x2d, false, Race.orc, orcFighter),
	destroyer(0x2e, false, Race.orc, orcRaider),
	orcMonk(0x2f, false, Race.orc, orcFighter),
	tyrant(0x30, false, Race.orc, orcMonk),
	orcMage(0x31, true, Race.orc, null),
	orcShaman(0x32, true, Race.orc, orcMage),
	overlord(0x33, true, Race.orc, orcShaman),
	warcryer(0x34, true, Race.orc, orcShaman),
	dwarvenFighter(0x35, false, Race.dwarf, null),
	scavenger(0x36, false, Race.dwarf, dwarvenFighter),
	bountyHunter(0x37, false, Race.dwarf, scavenger),
	artisan(0x38, false, Race.dwarf, dwarvenFighter),
	warsmith(0x39, false, Race.dwarf, artisan),
	
	dummyEntry1(58, false, null, null),
	dummyEntry2(59, false, null, null),
	dummyEntry3(60, false, null, null),
	dummyEntry4(61, false, null, null),
	dummyEntry5(62, false, null, null),
	dummyEntry6(63, false, null, null),
	dummyEntry7(64, false, null, null),
	dummyEntry8(65, false, null, null),
	dummyEntry9(66, false, null, null),
	dummyEntry10(67, false, null, null),
	dummyEntry11(68, false, null, null),
	dummyEntry12(69, false, null, null),
	dummyEntry13(70, false, null, null),
	dummyEntry14(71, false, null, null),
	dummyEntry15(72, false, null, null),
	dummyEntry16(73, false, null, null),
	dummyEntry17(74, false, null, null),
	dummyEntry18(75, false, null, null),
	dummyEntry19(76, false, null, null),
	dummyEntry20(77, false, null, null),
	dummyEntry21(78, false, null, null),
	dummyEntry22(79, false, null, null),
	dummyEntry23(80, false, null, null),
	dummyEntry24(81, false, null, null),
	dummyEntry25(82, false, null, null),
	dummyEntry26(83, false, null, null),
	dummyEntry27(84, false, null, null),
	dummyEntry28(85, false, null, null),
	dummyEntry29(86, false, null, null),
	dummyEntry30(87, false, null, null),
	
	duelist(0x58, false, Race.human, gladiator),
	dreadnought(0x59, false, Race.human, warlord),
	phoenixKnight(0x5a, false, Race.human, paladin),
	hellKnight(0x5b, false, Race.human, darkAvenger),
	sagittarius(0x5c, false, Race.human, hawkeye),
	adventurer(0x5d, false, Race.human, treasureHunter),
	archmage(0x5e, true, Race.human, sorceror),
	soultaker(0x5f, true, Race.human, necromancer),
	arcanaLord(0x60, true, Race.human, warlock),
	cardinal(0x61, true, Race.human, bishop),
	hierophant(0x62, true, Race.human, prophet),
	evaTemplar(0x63, false, Race.elf, templeKnight),
	swordMuse(0x64, false, Race.elf, swordSinger),
	windRider(0x65, false, Race.elf, plainsWalker),
	moonlightSentinel(0x66, false, Race.elf, silverRanger),
	mysticMuse(0x67, true, Race.elf, spellsinger),
	elementalMaster(0x68, true, Race.elf, elementalSummoner),
	evaSaint(0x69, true, Race.elf, elder),
	shillienTemplar(0x6a, false, Race.darkelf, shillienKnight),
	spectralDancer(0x6b, false, Race.darkelf, bladedancer),
	ghostHunter(0x6c, false, Race.darkelf, abyssWalker),
	ghostSentinel(0x6d, false, Race.darkelf, phantomRanger),
	stormScreamer(0x6e, true, Race.darkelf, spellhowler),
	spectralMaster(0x6f, true, Race.darkelf, phantomSummoner),
	shillienSaint(0x70, true, Race.darkelf, shillenElder),
	titan(0x71, false, Race.orc, destroyer),
	grandKhauatari(0x72, false, Race.orc, tyrant),
	dominator(0x73, true, Race.orc, overlord),
	doomcryer(0x74, true, Race.orc, warcryer),
	fortuneSeeker(0x75, false, Race.dwarf, bountyHunter),
	maestro(0x76, false, Race.dwarf, warsmith);
	
	private final int _id;
	private final boolean _isMage;
	private final Race _race;
	private final ClassId _parent;
	
	public static final ClassId[] VALUES = values();
	
	private ClassId(final int pId, final boolean pIsMage, final Race pRace, final ClassId pParent)
	{
		_id = pId;
		_isMage = pIsMage;
		_race = pRace;
		_parent = pParent;
	}
	
	public final int getId()
	{
		return _id;
	}
	
	public final boolean isMage()
	{
		return _isMage;
	}
	
	public final Race getRace()
	{
		return _race;
	}
	
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
	
	public final boolean equalsOrChildOf(final ClassId cid)
	{
		return this == cid || childOf(cid);
	}
	
	public final int level()
	{
		if (_parent == null)
		{
			return 0;
		}
		
		return 1 + _parent.level();
	}
	
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
