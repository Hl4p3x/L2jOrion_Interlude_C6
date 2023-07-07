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
package l2jorion.game.skills.funcs;

import l2jorion.Config;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.skills.Env;
import l2jorion.game.skills.Stats;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;

public class FuncEnchant extends Func
{
	public FuncEnchant(final Stats pStat, final int pOrder, final Object owner, final Lambda lambda)
	{
		super(pStat, pOrder, owner);
	}
	
	@Override
	public void calc(final Env env)
	{
		if (cond != null && !cond.test(env))
		{
			return;
		}
		
		final L2ItemInstance item = (L2ItemInstance) funcOwner;
		final int crystal = item.getItem().getCrystalType();
		final Enum<?> itemType = item.getItemType();
		
		if (crystal == L2Item.CRYSTAL_NONE)
		{
			return;
		}
		
		int enchant = item.getEnchantLevel();
		
		int overenchant = 0;
		if (enchant > 3)
		{
			overenchant = enchant - 3;
			enchant = 3;
		}
		
		if (env.player != null && env.player instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) env.player;
			if (player.isInOlympiadMode() && Config.ALT_OLY_ENCHANT_LIMIT >= 0 && enchant + overenchant > Config.ALT_OLY_ENCHANT_LIMIT)
			{
				if (Config.ALT_OLY_ENCHANT_LIMIT > 3)
				{
					overenchant = Config.ALT_OLY_ENCHANT_LIMIT - 3;
				}
				else
				{
					overenchant = 0;
					enchant = Config.ALT_OLY_ENCHANT_LIMIT;
				}
			}
			
			// boots item by olympiad enchant limit
			if (Config.L2LIMIT_CUSTOM)
			{
				if (player.isHero())
				{
					L2Weapon currentWeapon = player.getActiveWeaponItem();
					if (currentWeapon != null && currentWeapon.isHeroItem())
					{
						overenchant = 0;
						enchant = 6;
					}
					
				}
			}
		}
		
		if (stat == Stats.MAGIC_DEFENCE || stat == Stats.POWER_DEFENCE)
		{
			env.value += enchant + 3 * overenchant;
			return;
		}
		
		if (!Config.EXPLLOSIVE_CUSTOM)
		{
			
			if (stat == Stats.MAGIC_ATTACK)
			{
				switch (item.getItem().getCrystalType())
				{
					case L2Item.CRYSTAL_S:
						env.value += 4 * enchant + 8 * overenchant;
						break;
					case L2Item.CRYSTAL_A:
						env.value += 3 * enchant + 6 * overenchant;
						break;
					case L2Item.CRYSTAL_B:
						env.value += 3 * enchant + 6 * overenchant;
						break;
					case L2Item.CRYSTAL_C:
						env.value += 3 * enchant + 6 * overenchant;
						break;
					case L2Item.CRYSTAL_D:
						env.value += 2 * enchant + 4 * overenchant;
						break;
				}
				return;
			}
			
			switch (item.getItem().getCrystalType())
			{
				case L2Item.CRYSTAL_A:
					if (itemType == L2WeaponType.BOW)
					{
						env.value += 8 * enchant + 16 * overenchant;
					}
					else if (itemType == L2WeaponType.DUALFIST || itemType == L2WeaponType.DUAL || itemType == L2WeaponType.SWORD && item.getItem().getBodyPart() == 16384)
					{
						env.value += 5 * enchant + 10 * overenchant;
					}
					else
					{
						env.value += 4 * enchant + 8 * overenchant;
					}
					break;
				case L2Item.CRYSTAL_B:
					if (itemType == L2WeaponType.BOW)
					{
						env.value += 6 * enchant + 12 * overenchant;
					}
					else if (itemType == L2WeaponType.DUALFIST || itemType == L2WeaponType.DUAL || itemType == L2WeaponType.SWORD && item.getItem().getBodyPart() == 16384)
					{
						env.value += 4 * enchant + 8 * overenchant;
					}
					else
					{
						env.value += 3 * enchant + 6 * overenchant;
					}
					break;
				case L2Item.CRYSTAL_C:
					if (itemType == L2WeaponType.BOW)
					{
						env.value += 6 * enchant + 12 * overenchant;
					}
					else if (itemType == L2WeaponType.DUALFIST || itemType == L2WeaponType.DUAL || itemType == L2WeaponType.SWORD && item.getItem().getBodyPart() == 16384)
					{
						env.value += 4 * enchant + 8 * overenchant;
					}
					else
					{
						env.value += 3 * enchant + 6 * overenchant;
					}
					
					break;
				case L2Item.CRYSTAL_D:
					if (itemType == L2WeaponType.BOW)
					{
						env.value += 4 * enchant + 8 * overenchant;
					}
					else
					{
						env.value += 2 * enchant + 4 * overenchant;
					}
					break;
				case L2Item.CRYSTAL_S:
					if (itemType == L2WeaponType.BOW)
					{
						env.value += 10 * enchant + 20 * overenchant;
					}
					else if (itemType == L2WeaponType.DUALFIST || itemType == L2WeaponType.DUAL || itemType == L2WeaponType.SWORD && item.getItem().getBodyPart() == 16384)
					{
						env.value += 4 * enchant + 12 * overenchant;
					}
					else
					{
						env.value += 4 * enchant + 10 * overenchant;
					}
					break;
			}
			return;
		}
		
		if (Config.EXPLLOSIVE_CUSTOM)
		{
			if (item.getEnchantLevel() >= 26)
			{
				if (env.player != null && env.player instanceof L2PcInstance)
				{
					if (((L2PcInstance) env.player).isMageClass())
					{
						if (stat == Stats.PVE_MAGICAL_DMG)
						{
							switch (item.getItem().getCrystalType())
							{
								case L2Item.CRYSTAL_S:
									env.value += 0.05 * (item.getEnchantLevel() - 25);
									break;
							}
							return;
						}
					}
					else
					{
						if (stat == Stats.PVE_PHYSICAL_DMG || stat == Stats.PVE_BOW_DMG)
						{
							switch (item.getItem().getCrystalType())
							{
								case L2Item.CRYSTAL_S:
									env.value += 0.05 * (item.getEnchantLevel() - 25);
									break;
							}
							return;
						}
					}
				}
			}
			
			if (stat == Stats.MAGIC_ATTACK)
			{
				switch (item.getItem().getCrystalType())
				{
					case L2Item.CRYSTAL_S:
						if (item.getEnchantLevel() >= 26)
						{
							env.value += 4 * 3 + 8 * 22;
						}
						else
						{
							env.value += 4 * enchant + 8 * overenchant;
						}
						
						break;
					case L2Item.CRYSTAL_A:
						env.value += 3 * enchant + 6 * overenchant;
						break;
					case L2Item.CRYSTAL_B:
						env.value += 3 * enchant + 6 * overenchant;
						break;
					case L2Item.CRYSTAL_C:
						env.value += 3 * enchant + 6 * overenchant;
						break;
					case L2Item.CRYSTAL_D:
						env.value += 2 * enchant + 4 * overenchant;
						break;
				}
				return;
			}
			
			if (stat == Stats.POWER_ATTACK)
			{
				switch (item.getItem().getCrystalType())
				{
					case L2Item.CRYSTAL_A:
						if (itemType == L2WeaponType.BOW)
						{
							env.value += 8 * enchant + 16 * overenchant;
						}
						else if (itemType == L2WeaponType.DUALFIST || itemType == L2WeaponType.DUAL || itemType == L2WeaponType.SWORD && item.getItem().getBodyPart() == 16384)
						{
							env.value += 5 * enchant + 10 * overenchant;
						}
						else
						{
							env.value += 4 * enchant + 8 * overenchant;
						}
						break;
					case L2Item.CRYSTAL_B:
						if (itemType == L2WeaponType.BOW)
						{
							env.value += 6 * enchant + 12 * overenchant;
						}
						else if (itemType == L2WeaponType.DUALFIST || itemType == L2WeaponType.DUAL || itemType == L2WeaponType.SWORD && item.getItem().getBodyPart() == 16384)
						{
							env.value += 4 * enchant + 8 * overenchant;
						}
						else
						{
							env.value += 3 * enchant + 6 * overenchant;
						}
						break;
					case L2Item.CRYSTAL_C:
						if (itemType == L2WeaponType.BOW)
						{
							env.value += 6 * enchant + 12 * overenchant;
						}
						else if (itemType == L2WeaponType.DUALFIST || itemType == L2WeaponType.DUAL || itemType == L2WeaponType.SWORD && item.getItem().getBodyPart() == 16384)
						{
							env.value += 4 * enchant + 8 * overenchant;
						}
						else
						{
							env.value += 3 * enchant + 6 * overenchant;
						}
						
						break;
					case L2Item.CRYSTAL_D:
						if (itemType == L2WeaponType.BOW)
						{
							env.value += 4 * enchant + 8 * overenchant;
						}
						else
						{
							env.value += 2 * enchant + 4 * overenchant;
						}
						break;
					case L2Item.CRYSTAL_S:
						if (itemType == L2WeaponType.BOW)
						{
							if (item.getEnchantLevel() >= 26)
							{
								env.value += 10 * 3 + 20 * 22;
							}
							else
							{
								env.value += 10 * enchant + 20 * overenchant;
							}
						}
						else if (itemType == L2WeaponType.DUALFIST || itemType == L2WeaponType.DUAL || itemType == L2WeaponType.SWORD && item.getItem().getBodyPart() == 16384)
						{
							if (item.getEnchantLevel() >= 26)
							{
								env.value += 4 * 3 + 12 * 22;
							}
							else
							{
								env.value += 4 * enchant + 12 * overenchant;
							}
						}
						else
						{
							if (item.getEnchantLevel() >= 26)
							{
								env.value += 4 * 3 + 10 * 22;
							}
							else
							{
								env.value += 4 * enchant + 10 * overenchant;
							}
						}
						break;
				}
				return;
			}
		}
	}
}