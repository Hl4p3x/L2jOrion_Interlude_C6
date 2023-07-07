package l2jorion.game.handler;

import java.util.HashMap;
import java.util.Map;

import l2jorion.Config;
import l2jorion.game.GameServer;
import l2jorion.game.handler.item.AccessLevelCustomItem;
import l2jorion.game.handler.item.AugmentItem;
import l2jorion.game.handler.item.BeastSoulShot;
import l2jorion.game.handler.item.BeastSpice;
import l2jorion.game.handler.item.BeastSpiritShot;
import l2jorion.game.handler.item.BlessedSpiritShot;
import l2jorion.game.handler.item.Book;
import l2jorion.game.handler.item.BreakingArrow;
import l2jorion.game.handler.item.CharChangePotions;
import l2jorion.game.handler.item.ChestKey;
import l2jorion.game.handler.item.ChristmasTree;
import l2jorion.game.handler.item.ClanLevel8CustomItem;
import l2jorion.game.handler.item.ClanPointCustomItem;
import l2jorion.game.handler.item.ClanSkillsCustomItem;
import l2jorion.game.handler.item.CrystalCarol;
import l2jorion.game.handler.item.Crystals;
import l2jorion.game.handler.item.CustomAugmentationSystem;
import l2jorion.game.handler.item.CustomItemForFighter;
import l2jorion.game.handler.item.CustomItemForMage;
import l2jorion.game.handler.item.CustomPotions;
import l2jorion.game.handler.item.EnchantScrolls;
import l2jorion.game.handler.item.EnergyStone;
import l2jorion.game.handler.item.Expire_Ip_access;
import l2jorion.game.handler.item.Expire_buff_slots;
import l2jorion.game.handler.item.Expire_hour_buffs;
import l2jorion.game.handler.item.ExtractableItems;
import l2jorion.game.handler.item.Firework;
import l2jorion.game.handler.item.FishShots;
import l2jorion.game.handler.item.Harvester;
import l2jorion.game.handler.item.HeroCustomItem;
import l2jorion.game.handler.item.HsItems;
import l2jorion.game.handler.item.JackpotSeed;
import l2jorion.game.handler.item.MOSKey;
import l2jorion.game.handler.item.MapForestOfTheDead;
import l2jorion.game.handler.item.Maps;
import l2jorion.game.handler.item.MercTicket;
import l2jorion.game.handler.item.MysteryPotion;
import l2jorion.game.handler.item.Nectar;
import l2jorion.game.handler.item.NobleCustomItem;
import l2jorion.game.handler.item.PaganKeys;
import l2jorion.game.handler.item.Potions;
import l2jorion.game.handler.item.PremiumCustomItem;
import l2jorion.game.handler.item.Recipes;
import l2jorion.game.handler.item.Remedy;
import l2jorion.game.handler.item.RollingDice;
import l2jorion.game.handler.item.ScrollOfEscape;
import l2jorion.game.handler.item.ScrollOfResurrection;
import l2jorion.game.handler.item.Scrolls;
import l2jorion.game.handler.item.Seed;
import l2jorion.game.handler.item.SevenSignsRecord;
import l2jorion.game.handler.item.SoulCrystals;
import l2jorion.game.handler.item.SoulShots;
import l2jorion.game.handler.item.SpecialXMas;
import l2jorion.game.handler.item.SpiritShot;
import l2jorion.game.handler.item.SummonItems;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ItemHandler
{
	private static final Logger LOG = LoggerFactory.getLogger(GameServer.class.getName());
	
	private final Map<Integer, IItemHandler> _datatable = new HashMap<>();
	
	public static ItemHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected ItemHandler()
	{
		registerItemHandler(new AugmentItem());
		registerItemHandler(new AccessLevelCustomItem());
		registerItemHandler(new ScrollOfEscape());
		registerItemHandler(new ScrollOfResurrection());
		registerItemHandler(new SoulShots());
		registerItemHandler(new SpiritShot());
		registerItemHandler(new BlessedSpiritShot());
		registerItemHandler(new BeastSoulShot());
		registerItemHandler(new BeastSpiritShot());
		registerItemHandler(new ChestKey());
		registerItemHandler(new CustomPotions());
		registerItemHandler(new CustomItemForMage());
		registerItemHandler(new CustomItemForFighter());
		registerItemHandler(new PaganKeys());
		registerItemHandler(new Maps());
		registerItemHandler(new MapForestOfTheDead());
		registerItemHandler(new Potions());
		registerItemHandler(new Recipes());
		registerItemHandler(new RollingDice());
		registerItemHandler(new MysteryPotion());
		registerItemHandler(new EnchantScrolls());
		registerItemHandler(new EnergyStone());
		registerItemHandler(new Book());
		registerItemHandler(new Remedy());
		registerItemHandler(new Scrolls());
		registerItemHandler(new CrystalCarol());
		registerItemHandler(new SoulCrystals());
		registerItemHandler(new SevenSignsRecord());
		registerItemHandler(new CharChangePotions());
		registerItemHandler(new Firework());
		registerItemHandler(new Seed());
		registerItemHandler(new Harvester());
		registerItemHandler(new MercTicket());
		registerItemHandler(new Nectar());
		registerItemHandler(new FishShots());
		registerItemHandler(new ExtractableItems());
		registerItemHandler(new SpecialXMas());
		registerItemHandler(new SummonItems());
		registerItemHandler(new BeastSpice());
		registerItemHandler(new JackpotSeed());
		registerItemHandler(new NobleCustomItem());
		registerItemHandler(new HeroCustomItem());
		registerItemHandler(new PremiumCustomItem());
		registerItemHandler(new MOSKey());
		registerItemHandler(new BreakingArrow());
		registerItemHandler(new ChristmasTree());
		registerItemHandler(new Crystals());
		registerItemHandler(new HsItems());
		registerItemHandler(new ClanPointCustomItem());
		registerItemHandler(new ClanSkillsCustomItem());
		registerItemHandler(new ClanLevel8CustomItem());
		
		if (Config.L2UNLIMITED_CUSTOM)
		{
			registerItemHandler(new CustomAugmentationSystem());
		}
		
		if (Config.RON_CUSTOM)
		{
			registerItemHandler(new Expire_Ip_access());
			registerItemHandler(new Expire_hour_buffs());
			registerItemHandler(new Expire_buff_slots());
		}
		
		LOG.info("ItemHandler: Loaded " + _datatable.size() + " handlers");
	}
	
	public void registerItemHandler(IItemHandler handler)
	{
		int[] ids = handler.getItemIds();
		
		for (int id : ids)
		{
			_datatable.put(Integer.valueOf(id), handler);
		}
	}
	
	public IItemHandler getItemHandler(int itemId)
	{
		return _datatable.get(Integer.valueOf(itemId));
	}
	
	public int size()
	{
		return _datatable.size();
	}
	
	private static class SingletonHolder
	{
		protected static final ItemHandler _instance = new ItemHandler();
	}
}
