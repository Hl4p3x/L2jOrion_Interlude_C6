package l2jorion.bots.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import l2jorion.Config;
import l2jorion.bots.FakePlayer;
import l2jorion.bots.FakePlayerNameManager;
import l2jorion.bots.ai.FakePlayerAI;
import l2jorion.bots.ai.FallbackAI;
import l2jorion.bots.ai.classes.newbie.DarkElfFighterAI;
import l2jorion.bots.ai.classes.newbie.DarkMageAI;
import l2jorion.bots.ai.classes.newbie.DwarvenFighterAI;
import l2jorion.bots.ai.classes.newbie.ElvenFighterAI;
import l2jorion.bots.ai.classes.newbie.ElvenMageAI;
import l2jorion.bots.ai.classes.newbie.HumanFighterAI;
import l2jorion.bots.ai.classes.newbie.HumanMageAI;
import l2jorion.bots.ai.classes.newbie.OrcFighterAI;
import l2jorion.bots.ai.classes.newbie.OrcMageAI;
import l2jorion.bots.ai.classes.third.AdventurerAI;
import l2jorion.bots.ai.classes.third.ArchmageAI;
import l2jorion.bots.ai.classes.third.CardinalAI;
import l2jorion.bots.ai.classes.third.DominatorAI;
import l2jorion.bots.ai.classes.third.DreadnoughtAI;
import l2jorion.bots.ai.classes.third.DuelistAI;
import l2jorion.bots.ai.classes.third.GhostHunterAI;
import l2jorion.bots.ai.classes.third.GhostSentinelAI;
import l2jorion.bots.ai.classes.third.GrandKhavatariAI;
import l2jorion.bots.ai.classes.third.MoonlightSentinelAI;
import l2jorion.bots.ai.classes.third.MysticMuseAI;
import l2jorion.bots.ai.classes.third.SaggitariusAI;
import l2jorion.bots.ai.classes.third.SoultakerAI;
import l2jorion.bots.ai.classes.third.StormScreamerAI;
import l2jorion.bots.ai.classes.third.TitanAI;
import l2jorion.bots.ai.classes.third.WindRiderAI;
import l2jorion.bots.xml.EquipPackage;
import l2jorion.bots.xml.botEquipment;
import l2jorion.game.datatables.sql.CharTemplateTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.actor.appearance.PcAppearance;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2PcTemplate;
import l2jorion.util.random.Rnd;

public class FakeHelpers
{
	public static int[][] getFighterBuffs()
	{
		return Config.BOTS_FIGHTER_BUFFS;
	}
	
	public static int[][] getMageBuffs()
	{
		return Config.BOTS_MAGE_BUFFS;
	}
	
	public static FakePlayer createRandomFakePlayer(int level, int classNumber, boolean pvpZone, boolean farmer)
	{
		int objectId = IdFactory.getInstance().getNextId();
		String accountName = Config.BOTS_ACCOUNT;
		
		String playerName = FakePlayerNameManager.INSTANCE.getRandomAvailableName();
		
		ClassId classId = null;
		
		switch (classNumber)
		{
			case 1:
			{
				classId = getNewbieClasses().get(Rnd.get(0, getNewbieClasses().size() - 1));
				break;
			}
			case 3:
			{
				classId = getThirdClasses().get(Rnd.get(0, getThirdClasses().size() - 1));
				break;
			}
		}
		
		final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(classId);
		PcAppearance app = getRandomAppearance();
		FakePlayer player = new FakePlayer(objectId, template, accountName, app);
		
		player.setName(playerName);
		if (Config.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE)
		{
			player.setNewbie(true);
		}
		player.setBaseClass(player.getClassId());
		setLevel(player, level);
		player.rewardSkills();
		
		switch (level)
		{
			case 1:
			{
				if (Config.BOTS_CUSTOM_ITEMS)
				{
					giveEquipmentByClass(player, false, 0, 0);
				}
				else
				{
					final L2Item[] items = player.getBaseTemplate().getItems();
					for (final L2Item item2 : items)
					{
						final L2ItemInstance item = player.getInventory().addItem("Init", item2.getItemId(), 1, player, null);
						if (item.isEquipable())
						{
							player.getInventory().equipItemAndRecord(item);
						}
					}
				}
				break;
			}
			default:
				if (pvpZone)
				{
					giveEquipmentByClass(player, Config.BOTS_RANDOM_ENCHANT_PVPZONE, Config.BOTS_RANDOM_ENCHANT_PVPZONE_MIN, Config.BOTS_RANDOM_ENCHANT_PVPZONE_MAX);
				}
				else if (farmer)
				{
					giveEquipmentByClass(player, Config.BOTS_RANDOM_ENCHANT_FARMER, Config.BOTS_RANDOM_ENCHANT_FARMER_MIN, Config.BOTS_RANDOM_ENCHANT_FARMER_MAX);
				}
				
				else
				{
					// Walkers
					if (level > 20)
					{
						giveEquipmentByClass(player, Config.BOTS_RANDOM_ENCHANT_WALKER, Config.BOTS_RANDOM_ENCHANT_WALKER_MIN, Config.BOTS_RANDOM_ENCHANT_WALKER_MAX);
					}
				}
				break;
		}
		
		if (Config.AUTO_LOOT)
		{
			player.setAutoLootEnabled(true);
		}
		else
		{
			player.setAutoLootEnabled(false);
		}
		
		if (Config.AUTO_LOOT_HERBS)
		{
			player.setAutoLootHerbs(true);
		}
		else
		{
			player.setAutoLootHerbs(false);
		}
		
		player.heal();
		
		boolean ok = player.createDb();
		if (!ok)
		{
			return null;
		}
		
		return player;
	}
	
	public static void giveEquipmentByClass(FakePlayer player, boolean randomEnchant, int minEnchat, int maxEnchant)
	{
		List<Integer> itemIds = new ArrayList<>();
		
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getWeaponId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getShieldId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getHairId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getFaceId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getHelmId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getChestId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getLegsId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getGlovesId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getFeetId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getNeck());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getLeftEarId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getRightEarId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getLeftFingerId());
		itemIds.add(getEquipmentOption(player.getClassId().getId()).getRightFingerId());
		
		for (int id : itemIds)
		{
			if (id == 0)
			{
				continue;
			}
			
			player.getInventory().addItem("Equipment", id, 1, player, null);
			L2ItemInstance item = player.getInventory().getItemByItemId(id);
			
			if (randomEnchant)
			{
				if (item.getItem().getCrystalType() != L2Item.CRYSTAL_NONE)
				{
					item.setEnchantLevel(Rnd.get(minEnchat, maxEnchant));
				}
			}
			
			player.getInventory().equipItemAndRecord(item);
		}
		player.getInventory().reloadEquippedItems();
		player.broadcastUserInfo();
	}
	
	public static List<ClassId> getThirdClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		// classes.add(ClassId.evaSaint);
		// classes.add(ClassId.shillienTemplar);
		// classes.add(ClassId.spectralDancer);
		// classes.add(ClassId.ghostHunter);
		// classes.add(ClassId.phoenixKnight);
		// classes.add(ClassId.hellKnight);
		// classes.add(ClassId.hierophant);
		// classes.add(ClassId.evaTemplar);
		// classes.add(ClassId.swordMuse);
		// classes.add(ClassId.doomcryer);
		// classes.add(ClassId.fortuneSeeker);
		// classes.add(ClassId.maestro);
		
		// classes.add(ClassId.arcanaLord);
		// classes.add(ClassId.elementalMaster);
		// classes.add(ClassId.spectralMaster);
		// classes.add(ClassId.shillienSaint);
		
		classes.add(ClassId.sagittarius);
		classes.add(ClassId.archmage);
		classes.add(ClassId.soultaker);
		classes.add(ClassId.mysticMuse);
		classes.add(ClassId.stormScreamer);
		classes.add(ClassId.moonlightSentinel);
		classes.add(ClassId.ghostSentinel);
		classes.add(ClassId.adventurer);
		classes.add(ClassId.windRider);
		classes.add(ClassId.dominator);
		classes.add(ClassId.titan);
		// classes.add(ClassId.cardinal);
		classes.add(ClassId.duelist);
		classes.add(ClassId.grandKhauatari);
		if (!Config.L2UNLIMITED_CUSTOM)
		{
			classes.add(ClassId.dreadnought);
		}
		
		return classes;
	}
	
	public static List<ClassId> getNewbieClasses()
	{
		List<ClassId> classes = new ArrayList<>();
		
		classes.add(ClassId.fighter);
		classes.add(ClassId.mage);
		classes.add(ClassId.elvenFighter);
		classes.add(ClassId.elvenMage);
		classes.add(ClassId.darkFighter);
		classes.add(ClassId.darkMage);
		classes.add(ClassId.orcFighter);
		classes.add(ClassId.orcMage);
		classes.add(ClassId.dwarvenFighter);
		
		return classes;
	}
	
	public static Map<ClassId, Class<? extends FakePlayerAI>> getAllAIs()
	{
		Map<ClassId, Class<? extends FakePlayerAI>> ais = new HashMap<>();
		// newbie class
		ais.put(ClassId.fighter, HumanFighterAI.class);
		ais.put(ClassId.mage, HumanMageAI.class);
		ais.put(ClassId.elvenFighter, ElvenFighterAI.class);
		ais.put(ClassId.elvenMage, ElvenMageAI.class);
		ais.put(ClassId.darkFighter, DarkElfFighterAI.class);
		ais.put(ClassId.darkMage, DarkMageAI.class);
		ais.put(ClassId.orcFighter, OrcFighterAI.class);
		ais.put(ClassId.orcMage, OrcMageAI.class);
		ais.put(ClassId.dwarvenFighter, DwarvenFighterAI.class);
		
		// third class
		ais.put(ClassId.stormScreamer, StormScreamerAI.class);
		ais.put(ClassId.mysticMuse, MysticMuseAI.class);
		ais.put(ClassId.archmage, ArchmageAI.class);
		ais.put(ClassId.soultaker, SoultakerAI.class);
		ais.put(ClassId.sagittarius, SaggitariusAI.class);
		ais.put(ClassId.moonlightSentinel, MoonlightSentinelAI.class);
		ais.put(ClassId.ghostSentinel, GhostSentinelAI.class);
		ais.put(ClassId.adventurer, AdventurerAI.class);
		ais.put(ClassId.windRider, WindRiderAI.class);
		ais.put(ClassId.ghostHunter, GhostHunterAI.class);
		ais.put(ClassId.dominator, DominatorAI.class);
		ais.put(ClassId.titan, TitanAI.class);
		ais.put(ClassId.cardinal, CardinalAI.class);
		ais.put(ClassId.duelist, DuelistAI.class);
		ais.put(ClassId.grandKhauatari, GrandKhavatariAI.class);
		ais.put(ClassId.dreadnought, DreadnoughtAI.class);
		return ais;
	}
	
	public enum Sex
	{
		MALE,
		FEMALE,
		ETC;
	}
	
	public static PcAppearance getRandomAppearance()
	{
		
		int randomSex = Rnd.get(1, 2);
		boolean sex = false;
		if (randomSex == 1)
		{
			sex = true; // female
		}
		
		int hairStyle = Rnd.get(0, sex == false ? 4 : 6);
		int hairColor = Rnd.get(0, 3);
		int faceId = Rnd.get(0, 2);
		
		return new PcAppearance((byte) faceId, (byte) hairColor, (byte) hairStyle, sex);
	}
	
	public static void setLevel(FakePlayer player, int level)
	{
		final byte lvl = (byte) level;
		
		final long pXp = player.getStat().getExp();
		final long tXp = ExperienceData.getInstance().getExpForLevel(lvl);
		
		if (pXp > tXp)
		{
			player.getStat().removeExpAndSp(pXp - tXp, 0);
		}
		else if (pXp < tXp)
		{
			player.getStat().addExpAndSp(tXp - pXp, 0);
		}
		
		if (Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
		{
			player.checkAllowedSkills();
		}
		
		player.refreshOverloaded();
		player.refreshExpertisePenalty();
		player.refreshMasteryPenality();
		player.refreshMasteryWeapPenality();
		player.broadcastUserInfo();
	}
	
	public static Class<? extends FakePlayerAI> getAIbyClassId(ClassId classId)
	{
		Class<? extends FakePlayerAI> ai = getAllAIs().get(classId);
		if (ai == null)
		{
			return FallbackAI.class;
		}
		
		return ai;
	}
	
	public static EquipPackage getEquipmentOption(int option)
	{
		return botEquipment.getInstance().getEquipmentPackage(option);
	}
}
