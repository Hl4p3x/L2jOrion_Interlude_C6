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
package l2jorion.game.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public final class CustomNpcInstanceManager
{
	private final static Logger LOG = LoggerFactory.getLogger(CustomNpcInstanceManager.class);
	
	private static CustomNpcInstanceManager _instance;
	
	private FastMap<Integer, customInfo> spawns;
	private FastMap<Integer, customInfo> templates;
	
	public final class customInfo
	{
		public String stringData[] = new String[2];
		public int integerData[] = new int[27];
		public boolean booleanData[] = new boolean[8];
	}
	
	public CustomNpcInstanceManager()
	{
		load();
	}
	
	public final static CustomNpcInstanceManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new CustomNpcInstanceManager();
		}
		
		return _instance;
	}
	
	/**
	 * Flush the old data, and load new data
	 */
	public final void reload()
	{
		if (spawns != null)
		{
			spawns.clear();
		}
		if (templates != null)
		{
			templates.clear();
		}
		
		load();
	}
	
	/**
	 * Just load the data for mysql...
	 */
	private final void load()
	{
		if (spawns == null || templates == null)
		{
			spawns = new FastMap<>();
			templates = new FastMap<>();
		}
		
		final String[] SQL_ITEM_SELECTS =
		{
			"SELECT" + " spawn,template,name,title,class_id,female,hair_style,hair_color,face,name_color,title_color," + " noble,hero,pvp,karma,wpn_enchant,right_hand,left_hand,gloves,chest,legs,feet,hair,hair2," + " pledge,cw_level,clan_id,ally_id,clan_crest,ally_crest,rnd_class,rnd_appearance,rnd_weapon,rnd_armor,max_rnd_enchant" + " FROM npc_to_pc_polymorph",
		};
		
		Connection con = null;
		try
		{
			int count = 0;
			con = L2DatabaseFactory.getInstance().getConnection();
			for (final String selectQuery : SQL_ITEM_SELECTS)
			{
				PreparedStatement statement = con.prepareStatement(selectQuery);
				ResultSet rset = statement.executeQuery();
				
				while (rset.next())
				{
					count++;
					customInfo ci = new customInfo();
					ci.integerData[26] = rset.getInt("spawn");
					ci.integerData[25] = rset.getInt("template");
					try
					{
						ci.stringData[0] = rset.getString("name");
						ci.stringData[1] = rset.getString("title");
						ci.integerData[7] = rset.getInt("class_id");
						
						final int PcSex = rset.getInt("female");
						switch (PcSex)
						{
							case 0:
								ci.booleanData[3] = false;
								break;
							case 1:
								ci.booleanData[3] = true;
								break;
							default:
								ci.booleanData[3] = Rnd.get(100) > 50 ? true : false;
								break;
						}
						
						ci.integerData[19] = rset.getInt("hair_style");
						ci.integerData[20] = rset.getInt("hair_color");
						ci.integerData[21] = rset.getInt("face");
						ci.integerData[22] = rset.getInt("name_color");
						ci.integerData[23] = rset.getInt("title_color");
						ci.booleanData[1] = rset.getInt("noble") > 0 ? true : false;
						ci.booleanData[2] = rset.getInt("hero") > 0 ? true : false;
						ci.booleanData[0] = rset.getInt("pvp") > 0 ? true : false;
						ci.integerData[1] = rset.getInt("karma");
						ci.integerData[8] = rset.getInt("wpn_enchant");
						ci.integerData[11] = rset.getInt("right_hand");
						ci.integerData[12] = rset.getInt("left_hand");
						ci.integerData[13] = rset.getInt("gloves");
						ci.integerData[14] = rset.getInt("chest");
						ci.integerData[15] = rset.getInt("legs");
						ci.integerData[16] = rset.getInt("feet");
						ci.integerData[17] = rset.getInt("hair");
						ci.integerData[18] = rset.getInt("hair2");
						ci.integerData[9] = rset.getInt("pledge");
						ci.integerData[10] = rset.getInt("cw_level");
						ci.integerData[2] = rset.getInt("clan_id");
						ci.integerData[3] = rset.getInt("ally_id");
						ci.integerData[4] = rset.getInt("clan_crest");
						ci.integerData[5] = rset.getInt("ally_crest");
						ci.booleanData[4] = rset.getInt("rnd_class") > 0 ? true : false;
						ci.booleanData[5] = rset.getInt("rnd_appearance") > 0 ? true : false;
						ci.booleanData[6] = rset.getInt("rnd_weapon") > 0 ? true : false;
						ci.booleanData[7] = rset.getInt("rnd_armor") > 0 ? true : false;
						ci.integerData[24] = rset.getInt("max_rnd_enchant");
						// Same object goes in twice:
						if (ci.integerData[25] != 0 && !templates.containsKey(ci.integerData[25]))
						{
							templates.put(ci.integerData[25], ci);
						}
						if (ci.integerData[25] == 0 && !spawns.containsKey(ci.integerData[26]))
						{
							spawns.put(ci.integerData[26], ci);
						}
					}
					catch (final Throwable t)
					{
						if (Config.ENABLE_ALL_EXCEPTIONS)
							t.printStackTrace();
						
						LOG.warn("Failed to load Npc Morph data for Object Id: " + ci.integerData[26] + " template: " + ci.integerData[25]);
					}
					ci = null;
				}
				DatabaseUtils.close(statement);
				statement = null;
				DatabaseUtils.close(rset);
				rset = null;
			}
			LOG.info("CustomNpcInstanceManager: Loaded " + count + " NPC to PC polymorphs");
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Checks if the L2NpcInstance calling this function has polymorphing data
	 * @param spwnId - L2NpcInstance's unique Object id
	 * @param npcId - L2NpcInstance's npc template id
	 * @return
	 */
	public final boolean isThisL2CustomNpcInstance(final int spwnId, final int npcId)
	{
		if (spwnId == 0 || npcId == 0)
			return false;
		else if (spawns.containsKey(spwnId))
			return true;
		else if (templates.containsKey(npcId))
			return true;
		else
			return false;
	}
	
	/**
	 * Return the polymorphing data for this L2NpcInstance if the data exists
	 * @param spwnId - NpcInstance's unique Object Id
	 * @param npcId - NpcInstance's npc template Id
	 * @return customInfo type data pack, or null if no such data exists.
	 */
	public final customInfo getCustomData(final int spwnId, final int npcId)
	{
		if (spwnId == 0 || npcId == 0)
			return null;
		
		// First check individual spawn objects - incase they have different values than their template
		for (final customInfo ci : spawns.values())
			if (ci != null && ci.integerData[26] == spwnId)
				return ci;
		
		// Now check if templates contains the morph npc template
		for (final customInfo ci : templates.values())
			if (ci != null && ci.integerData[25] == npcId)
				return ci;
		
		return null;
	}
	
	/**
	 * @return all template morphing queue
	 */
	public final FastMap<Integer, customInfo> getAllTemplates()
	{
		return templates;
	}
	
	/**
	 * @return all spawns morphing queue
	 */
	public final FastMap<Integer, customInfo> getAllSpawns()
	{
		return spawns;
	}
	
	/**
	 * Already removed customInfo - Change is saved in the DB <b>NOT IMPLEMENTED YET!</b>
	 * @param ciToRemove
	 */
	public final void updateRemoveInDB(final customInfo ciToRemove)
	{
		//
	}
	
	public final void AddInDB(final customInfo ciToAdd)
	{
		String Query = "REPLACE INTO npc_to_pc_polymorph VALUES" + " spawn,template,name,title,class_id,female,hair_style,hair_color,face,name_color,title_color," + " noble,hero,pvp,karma,wpn_enchant,right_hand,left_hand,gloves,chest,legs,feet,hair,hair2," + " pledge,cw_level,clan_id,ally_id,clan_crest,ally_crest,rnd_class,rnd_appearance,rnd_weapon,rnd_armor,max_rnd_enchant" + " FROM npc_to_pc_polymorph";
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(Query);
			ResultSet rset = statement.executeQuery();
			
			DatabaseUtils.close(statement);
			statement = null;
			
			while (rset.next())
			{
				final customInfo ci = new customInfo();
				ci.integerData[26] = rset.getInt("spawn");
				ci.integerData[25] = rset.getInt("template");
			}
			
			DatabaseUtils.close(rset);
		}
		catch (final Throwable t)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				t.printStackTrace();
			}
			
			LOG.warn("Could not add Npc Morph info into the DB: ");
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
}
