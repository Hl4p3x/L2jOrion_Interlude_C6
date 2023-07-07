package l2jorion.bots.ai;

import l2jorion.Config;
import l2jorion.bots.FakePlayer;
import l2jorion.bots.helpers.FakeHelpers;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.util.random.Rnd;

public class EnchanterAI extends FakePlayerAI
{
	
	private int _enchantIterations = 0;
	private int _maxEnchant = Config.ENCHANT_WEAPON_MAX;
	private final int iterationsForAction = Rnd.get(3, 5);
	
	public EnchanterAI(FakePlayer character)
	{
		super(character);
	}
	
	@Override
	public void setup()
	{
		super.setup();
		L2ItemInstance weapon = _fakePlayer.getActiveWeaponInstance();
		weapon = checkIfWeaponIsExistsEquipped(weapon);
		weapon.setEnchantLevel(0);
		_fakePlayer.broadcastUserInfo();
		
	}
	
	@Override
	public void thinkAndAct()
	{
		
		handleDeath();
		setBusyThinking(true);
		if (_enchantIterations % iterationsForAction == 0)
		{
			L2ItemInstance weapon = _fakePlayer.getActiveWeaponInstance();
			weapon = checkIfWeaponIsExistsEquipped(weapon);
			double chance = getSuccessChance(weapon);
			
			int currentEnchantLevel = weapon.getEnchantLevel();
			if (currentEnchantLevel < _maxEnchant || serverHasUnlimitedMax())
			{
				if (Rnd.nextDouble() < chance || weapon.getEnchantLevel() < 4)
				{
					weapon.setEnchantLevel(currentEnchantLevel + 1);
					_fakePlayer.broadcastUserInfo();
				}
				else
				{
					destroyFailedItem(weapon);
				}
			}
		}
		_enchantIterations++;
		setBusyThinking(false);
	}
	
	private void destroyFailedItem(L2ItemInstance weapon)
	{
		_fakePlayer.getInventory().destroyItem("Enchant", weapon, _fakePlayer, null);
		_fakePlayer.broadcastUserInfo();
		_fakePlayer.setActiveEnchantItem(null);
	}
	
	private double getSuccessChance(L2ItemInstance weapon)
	{
		double chance = 0d;
		/*
		 * if (((L2Weapon) weapon.getItem()).isMagical()) { chance = (weapon.getEnchantLevel() > 14) ? Config.ENCHANT_CHANCE_WEAPON_MAGIC_15PLUS : Config.ENCHANT_CHANCE_WEAPON_MAGIC; } else { chance = (weapon.getEnchantLevel() > 14) ? Config.ENCHANT_CHANCE_WEAPON_NONMAGIC_15PLUS :
		 * Config.ENCHANT_CHANCE_WEAPON_NONMAGIC; }
		 */
		return chance;
	}
	
	private boolean serverHasUnlimitedMax()
	{
		return _maxEnchant == 0;
	}
	
	private L2ItemInstance checkIfWeaponIsExistsEquipped(L2ItemInstance weapon)
	{
		if (weapon == null)
		{
			FakeHelpers.giveEquipmentByClass(_fakePlayer, false, 0, 0);
			weapon = _fakePlayer.getActiveWeaponInstance();
		}
		return weapon;
	}
	
	@Override
	protected int[][] getBuffs()
	{
		return new int[0][0];
	}
}
