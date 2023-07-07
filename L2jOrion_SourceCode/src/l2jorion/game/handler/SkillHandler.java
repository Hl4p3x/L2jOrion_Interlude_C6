package l2jorion.game.handler;

import java.util.Map;
import java.util.TreeMap;

import l2jorion.game.GameServer;
import l2jorion.game.handler.skill.BalanceLife;
import l2jorion.game.handler.skill.BeastFeed;
import l2jorion.game.handler.skill.Blow;
import l2jorion.game.handler.skill.Charge;
import l2jorion.game.handler.skill.ClanGate;
import l2jorion.game.handler.skill.CombatPointHeal;
import l2jorion.game.handler.skill.Continuous;
import l2jorion.game.handler.skill.CpDam;
import l2jorion.game.handler.skill.Craft;
import l2jorion.game.handler.skill.DOD;
import l2jorion.game.handler.skill.DeluxeKey;
import l2jorion.game.handler.skill.Disablers;
import l2jorion.game.handler.skill.DrainSoul;
import l2jorion.game.handler.skill.Fishing;
import l2jorion.game.handler.skill.FishingSkill;
import l2jorion.game.handler.skill.GetPlayer;
import l2jorion.game.handler.skill.Harvest;
import l2jorion.game.handler.skill.Heal;
import l2jorion.game.handler.skill.InstantJump;
import l2jorion.game.handler.skill.ManaHeal;
import l2jorion.game.handler.skill.Manadam;
import l2jorion.game.handler.skill.Mdam;
import l2jorion.game.handler.skill.Pdam;
import l2jorion.game.handler.skill.Recall;
import l2jorion.game.handler.skill.Resurrect;
import l2jorion.game.handler.skill.SiegeFlag;
import l2jorion.game.handler.skill.Sow;
import l2jorion.game.handler.skill.Spoil;
import l2jorion.game.handler.skill.StrSiegeAssault;
import l2jorion.game.handler.skill.SummonFriend;
import l2jorion.game.handler.skill.SummonTreasureKey;
import l2jorion.game.handler.skill.Sweep;
import l2jorion.game.handler.skill.TakeCastle;
import l2jorion.game.handler.skill.Unlock;
import l2jorion.game.handler.skill.ZakenPlayer;
import l2jorion.game.handler.skill.ZakenSelf;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class SkillHandler
{
	private static final Logger LOG = LoggerFactory.getLogger(GameServer.class);
	
	private static SkillHandler _instance;
	
	private final Map<L2Skill.SkillType, ISkillHandler> _datatable = new TreeMap<>();
	
	public static SkillHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new SkillHandler();
		}
		
		return _instance;
	}
	
	private SkillHandler()
	{
		registerSkillHandler(new Blow());
		registerSkillHandler(new Pdam());
		registerSkillHandler(new Mdam());
		registerSkillHandler(new CpDam());
		registerSkillHandler(new Manadam());
		registerSkillHandler(new Heal());
		registerSkillHandler(new InstantJump());
		registerSkillHandler(new CombatPointHeal());
		registerSkillHandler(new ManaHeal());
		registerSkillHandler(new BalanceLife());
		registerSkillHandler(new Charge());
		registerSkillHandler(new ClanGate());
		registerSkillHandler(new Continuous());
		registerSkillHandler(new Resurrect());
		registerSkillHandler(new Spoil());
		registerSkillHandler(new Sweep());
		registerSkillHandler(new StrSiegeAssault());
		registerSkillHandler(new SummonFriend());
		registerSkillHandler(new SummonTreasureKey());
		registerSkillHandler(new Disablers());
		registerSkillHandler(new Recall());
		registerSkillHandler(new SiegeFlag());
		registerSkillHandler(new TakeCastle());
		registerSkillHandler(new Unlock());
		registerSkillHandler(new DOD());
		registerSkillHandler(new DrainSoul());
		registerSkillHandler(new Craft());
		registerSkillHandler(new Fishing());
		registerSkillHandler(new FishingSkill());
		registerSkillHandler(new BeastFeed());
		registerSkillHandler(new DeluxeKey());
		registerSkillHandler(new Sow());
		registerSkillHandler(new Harvest());
		registerSkillHandler(new GetPlayer());
		registerSkillHandler(new ZakenPlayer());
		registerSkillHandler(new ZakenSelf());
		LOG.info("SkillHandler: Loaded " + _datatable.size() + " handlers");
		
	}
	
	public void registerSkillHandler(final ISkillHandler handler)
	{
		SkillType[] types = handler.getSkillIds();
		
		for (final SkillType t : types)
		{
			_datatable.put(t, handler);
		}
		types = null;
	}
	
	public ISkillHandler getSkillHandler(final SkillType skillType)
	{
		return _datatable.get(skillType);
	}
	
	/**
	 * @return
	 */
	public int size()
	{
		return _datatable.size();
	}
}