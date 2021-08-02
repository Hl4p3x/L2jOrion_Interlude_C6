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
package l2jorion.game.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.game.model.L2PetData;
import l2jorion.game.model.actor.instance.L2PetInstance;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class L2PetDataTable
{
	private final static Logger LOG = LoggerFactory.getLogger(L2PetInstance.class);
	
	private static L2PetDataTable _instance;
	
	private static Map<Integer, Map<Integer, L2PetData>> _petTable;
	
	public static L2PetDataTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new L2PetDataTable();
		}
		
		return _instance;
	}
	
	private L2PetDataTable()
	{
		_petTable = new FastMap<>();
	}
	
	public void loadPetsData()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT typeID, level, expMax, hpMax, mpMax, patk, pdef, matk, mdef, acc, evasion, crit, speed, atk_speed, cast_speed, feedMax, feedbattle, feednormal, loadMax, hpregen, mpregen, owner_exp_taken FROM pets_stats");
			final ResultSet rset = statement.executeQuery();
			
			int petId, petLevel;
			
			while (rset.next())
			{
				petId = rset.getInt("typeID");
				petLevel = rset.getInt("level");
				
				final L2PetData petData = new L2PetData();
				
				petData.setPetID(petId);
				petData.setPetLevel(petLevel);
				petData.setPetMaxExp(rset.getInt("expMax"));
				petData.setPetMaxHP(rset.getInt("hpMax"));
				petData.setPetMaxMP(rset.getInt("mpMax"));
				petData.setPetPAtk(rset.getInt("patk"));
				petData.setPetPDef(rset.getInt("pdef"));
				petData.setPetMAtk(rset.getInt("matk"));
				petData.setPetMDef(rset.getInt("mdef"));
				petData.setPetAccuracy(rset.getInt("acc"));
				petData.setPetEvasion(rset.getInt("evasion"));
				petData.setPetCritical(rset.getInt("crit"));
				petData.setPetSpeed(rset.getInt("speed"));
				petData.setPetAtkSpeed(rset.getInt("atk_speed"));
				petData.setPetCastSpeed(rset.getInt("cast_speed"));
				petData.setPetMaxFeed(rset.getInt("feedMax"));
				petData.setPetFeedNormal(rset.getInt("feednormal"));
				petData.setPetFeedBattle(rset.getInt("feedbattle"));
				petData.setPetMaxLoad(rset.getInt("loadMax"));
				petData.setPetRegenHP(rset.getInt("hpregen"));
				petData.setPetRegenMP(rset.getInt("mpregen"));
				petData.setPetRegenMP(rset.getInt("mpregen"));
				petData.setOwnerExpTaken(rset.getFloat("owner_exp_taken"));
				
				// if its the first data for this petid, we initialize its level FastMap
				if (!_petTable.containsKey(petId))
				{
					_petTable.put(petId, new FastMap<Integer, L2PetData>());
				}
				
				_petTable.get(petId).put(petLevel, petData);
			}
			
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			LOG.error("Could not load pets stats", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public void addPetData(final L2PetData petData)
	{
		final Map<Integer, L2PetData> h = _petTable.get(petData.getPetID());
		
		if (h == null)
		{
			final Map<Integer, L2PetData> statTable = new FastMap<>();
			statTable.put(petData.getPetLevel(), petData);
			_petTable.put(petData.getPetID(), statTable);
			return;
		}
		
		h.put(petData.getPetLevel(), petData);
	}
	
	public void addPetData(final L2PetData[] petLevelsList)
	{
		for (final L2PetData element : petLevelsList)
		{
			addPetData(element);
		}
	}
	
	public L2PetData getPetData(final int petID, final int petLevel)
	{
		return _petTable.get(petID).get(petLevel);
	}
	
	public static boolean isWolf(final int npcId)
	{
		return npcId == 12077;
	}
	
	public static boolean isSinEater(final int npcId)
	{
		return npcId == 12564;
	}
	
	public static boolean isHatchling(final int npcId)
	{
		return npcId > 12310 && npcId < 12314;
	}
	
	public static boolean isStrider(final int npcId)
	{
		return (npcId > 12525 && npcId < 12529) || (npcId >= 100 && npcId <= 299);
	}
	
	public static boolean isWyvern(final int npcId)
	{
		return npcId == 12621;
	}
	
	public static boolean isBaby(final int npcId)
	{
		return npcId > 12779 && npcId < 12783;
	}
	
	public static boolean isPetFood(final int itemId)
	{
		return itemId == 2515 || itemId == 4038 || itemId == 5168 || itemId == 5169 || itemId == 6316 || itemId == 7582;
	}
	
	public static boolean isWolfFood(final int itemId)
	{
		return itemId == 2515;
	}
	
	public static boolean isSinEaterFood(final int itemId)
	{
		return itemId == 2515;
	}
	
	public static boolean isHatchlingFood(final int itemId)
	{
		return itemId == 4038;
	}
	
	public static boolean isStriderFood(final int itemId)
	{
		return itemId == 5168 || itemId == 5169;
	}
	
	public static boolean isWyvernFood(final int itemId)
	{
		return itemId == 6316;
	}
	
	public static boolean isBabyFood(final int itemId)
	{
		return itemId == 7582;
	}
	
	public static int getFoodItemId(final int npcId)
	{
		if (isWolf(npcId))
		{
			return 2515;
		}
		else if (isSinEater(npcId))
		{
			return 2515;
		}
		else if (isHatchling(npcId))
		{
			return 4038;
		}
		else if (isStrider(npcId))
		{
			return 5168;
		}
		else if (isBaby(npcId))
		{
			return 7582;
		}
		else
		{
			return 0;
		}
	}
	
	public static int getPetIdByItemId(final int itemId)
	{
		switch (itemId)
		{
			// wolf pet a
			case 2375:
				return 12077;
			// Sin Eater
			case 4425:
				return 12564;
			// hatchling of wind
			case 3500:
				return 12311;
			// hatchling of star
			case 3501:
				return 12312;
			// hatchling of twilight
			case 3502:
				return 12313;
			// wind strider
			case 4422:
				return 12526;
			// Star strider
			case 4423:
				return 12527;
			// Twilight strider
			case 4424:
				return 12528;
			// Wyvern
			case 8663:
				return 12621;
			// Baby Buffalo
			case 6648:
				return 12780;
			// Baby Cougar
			case 6649:
				return 12782;
			// Baby Kookaburra
			case 6650:
				return 12781;
			// unknown item id.. should never happen
			default:
				return 0;
		}
	}
	
	public static int getHatchlingWindId()
	{
		return 12311;
	}
	
	public static int getHatchlingStarId()
	{
		return 12312;
	}
	
	public static int getHatchlingTwilightId()
	{
		return 12313;
	}
	
	public static int getStriderWindId()
	{
		return 12526;
	}
	
	public static int getStriderStarId()
	{
		return 12527;
	}
	
	public static int getStriderTwilightId()
	{
		return 12528;
	}
	
	public static int getWyvernItemId()
	{
		return 8663;
	}
	
	public static int getStriderWindItemId()
	{
		return 4422;
	}
	
	public static int getStriderStarItemId()
	{
		return 4423;
	}
	
	public static int getStriderTwilightItemId()
	{
		return 4424;
	}
	
	public static int getSinEaterItemId()
	{
		return 4425;
	}
	
	public static boolean isPetItem(final int itemId)
	{
		return itemId == 2375 // wolf
			|| itemId == 4425 // Sin Eater
			|| itemId == 3500 || itemId == 3501 || itemId == 3502 // hatchlings
			|| itemId == 4422 || itemId == 4423 || itemId == 4424 // striders
			|| itemId == 8663 // Wyvern
			|| itemId == 6648 || itemId == 6649 || itemId == 6650; // Babies
	}
	
	public static int[] getPetItemsAsNpc(final int npcId)
	{
		switch (npcId)
		{
			case 12077:// wolf pet a
				return new int[]
				{
					2375
				};
			case 12564:// Sin Eater
				return new int[]
				{
					4425
				};
			
			case 12311:// hatchling of wind
			case 12312:// hatchling of star
			case 12313:// hatchling of twilight
				return new int[]
				{
					3500,
					3501,
					3502
				};
			
			case 12526:// wind strider
			case 12527:// Star strider
			case 12528:// Twilight strider
				return new int[]
				{
					4422,
					4423,
					4424
				};
			
			case 12621:// Wyvern
				return new int[]
				{
					8663
				};
			
			case 12780:// Baby Buffalo
			case 12782:// Baby Cougar
			case 12781:// Baby Kookaburra
				return new int[]
				{
					6648,
					6649,
					6650
				};
			
			// unknown item id.. should never happen
			default:
				return new int[]
				{
					0
				};
		}
	}
	
	public static boolean isMountable(final int npcId)
	{
		return npcId == 12526 // wind strider
			|| npcId == 12527 // star strider
			|| npcId == 12528 // twilight strider
			|| npcId == 12621; // wyvern
	}
}
