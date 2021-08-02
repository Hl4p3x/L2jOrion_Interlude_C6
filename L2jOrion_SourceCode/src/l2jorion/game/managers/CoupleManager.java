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
package l2jorion.game.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Wedding;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class CoupleManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(CoupleManager.class);
	
	private final FastList<Wedding> _couples = new FastList<>();
	
	public static final CoupleManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public CoupleManager()
	{
		LOG.info("Wedding Manager - Enabled");
		_couples.clear();
		load();
	}
	
	public void reload()
	{
		_couples.clear();
		load();
	}
	
	private final void load()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			
			statement = con.prepareStatement("Select id from mods_wedding order by id");
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				getCouples().add(new Wedding(rs.getInt("id")));
			}
			
			DatabaseUtils.close(statement);
			rs.close();
			
			if (getCouples().size() > 0)
			{
				LOG.info("Wedding Manager: Loaded " + getCouples().size() + " couples");
			}
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Exception: CoupleManager.load(): " + e.getMessage(), e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	public final Wedding getCouple(final int coupleId)
	{
		final int index = getCoupleIndex(coupleId);
		if (index >= 0)
		{
			return getCouples().get(index);
		}
		return null;
	}
	
	public void createCouple(final L2PcInstance player1, final L2PcInstance player2)
	{
		if (player1 != null && player2 != null)
		{
			if (player1.getPartnerId() == 0 && player2.getPartnerId() == 0)
			{
				final int _player1id = player1.getObjectId();
				final int _player2id = player2.getObjectId();
				
				Wedding _new = new Wedding(player1, player2);
				getCouples().add(_new);
				player1.setPartnerId(_player2id);
				player2.setPartnerId(_player1id);
				player1.setCoupleId(_new.getId());
				player2.setCoupleId(_new.getId());
			}
		}
	}
	
	public void deleteCouple(final int coupleId)
	{
		final int index = getCoupleIndex(coupleId);
		Wedding wedding = getCouples().get(index);
		
		if (wedding != null)
		{
			L2PcInstance player1 = (L2PcInstance) L2World.getInstance().findObject(wedding.getPlayer1Id());
			L2PcInstance player2 = (L2PcInstance) L2World.getInstance().findObject(wedding.getPlayer2Id());
			if (player1 != null)
			{
				player1.setPartnerId(0);
				player1.setMarried(false);
				player1.setCoupleId(0);
				
			}
			if (player2 != null)
			{
				player2.setPartnerId(0);
				player2.setMarried(false);
				player2.setCoupleId(0);
				
			}
			wedding.divorce();
			getCouples().remove(index);
		}
	}
	
	public final int getCoupleIndex(final int coupleId)
	{
		int i = 0;
		for (Wedding temp : getCouples())
		{
			if (temp != null && temp.getId() == coupleId)
			{
				temp = null;
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public final FastList<Wedding> getCouples()
	{
		return _couples;
	}
	
	private static class SingletonHolder
	{
		protected static final CoupleManager _instance = new CoupleManager();
	}
}
