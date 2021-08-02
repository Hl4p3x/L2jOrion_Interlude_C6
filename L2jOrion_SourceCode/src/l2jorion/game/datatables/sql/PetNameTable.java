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
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class PetNameTable
{
	private final static Logger LOG = LoggerFactory.getLogger(PetNameTable.class);
	
	private static PetNameTable _instance;
	
	public static PetNameTable getInstance()
	{
		if (_instance == null)
		{
			_instance = new PetNameTable();
		}
		
		return _instance;
	}
	
	public boolean doesPetNameExist(final String name, final int petNpcId)
	{
		boolean result = true;
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT name FROM pets p, items i WHERE p.item_obj_id = i.object_id AND name=? AND i.item_id IN (?)");
			statement.setString(1, name);
			
			String cond = "";
			for (final int it : L2PetDataTable.getPetItemsAsNpc(petNpcId))
			{
				if (cond != "")
				{
					cond += ", ";
				}
				
				cond += it;
			}
			statement.setString(2, cond);
			final ResultSet rset = statement.executeQuery();
			result = rset.next();
			DatabaseUtils.close(rset);
			DatabaseUtils.close(statement);
		}
		catch (final SQLException e)
		{
			LOG.error("Could not check existing petname", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		return result;
	}
	
	public boolean isValidPetName(final String name)
	{
		boolean result = true;
		
		if (!isAlphaNumeric(name))
			return result;
		
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.PET_NAME_TEMPLATE);
		}
		catch (final PatternSyntaxException e) // case of illegal pattern
		{
			LOG.warn("ERROR : Pet name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		
		final Matcher regexp = pattern.matcher(name);
		
		if (!regexp.matches())
		{
			result = false;
		}
		
		return result;
	}
	
	private boolean isAlphaNumeric(final String text)
	{
		boolean result = true;
		final char[] chars = text.toCharArray();
		for (final char aChar : chars)
		{
			if (!Character.isLetterOrDigit(aChar))
			{
				result = false;
				break;
			}
		}
		
		return result;
	}
}
