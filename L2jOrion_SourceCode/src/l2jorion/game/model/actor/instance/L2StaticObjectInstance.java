/*
 * $Header: /cvsroot/l2j/L2_Gameserver/java/net/sf/l2j/gameserver/model/L2StaticObjectInstance.java,v 1.3.2.2.2.2 2005/02/04 13:05:27 maximas Exp $
 *
 *
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
package l2jorion.game.model.actor.instance;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.knownlist.NullKnownList;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.ShowTownMap;
import l2jorion.game.network.serverpackets.StaticObject;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2StaticObjectInstance extends L2Object
{
	private static Logger LOG = LoggerFactory.getLogger(L2StaticObjectInstance.class);
	
	public static final int INTERACTION_DISTANCE = 150;
	
	private int _staticObjectId;
	private int _type = -1; // 0 - map signs, 1 - throne , 2 - arena signs
	private int _x;
	private int _y;
	
	private String _texture;
	
	public int getStaticObjectId()
	{
		return _staticObjectId;
	}
	
	public void setStaticObjectId(final int StaticObjectId)
	{
		_staticObjectId = StaticObjectId;
	}
	
	public L2StaticObjectInstance(final int objectId)
	{
		super(objectId);
		setKnownList(new NullKnownList(this));
	}
	
	public int getType()
	{
		return _type;
	}
	
	public void setType(final int type)
	{
		_type = type;
	}
	
	public void setMap(final String texture, final int x, final int y)
	{
		_texture = "town_map." + texture;
		_x = x;
		_y = y;
	}
	
	private int getMapX()
	{
		return _x;
	}
	
	private int getMapY()
	{
		return _y;
	}
	
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (_type < 0)
		{
			LOG.info("L2StaticObjectInstance: StaticObject with invalid type! StaticObjectId: " + getStaticObjectId());
		}
		
		if (this != player.getTarget())
		{
			player.setTarget(this);
		}
		else
		{
			if (!player.isInsideRadius(this, INTERACTION_DISTANCE, false, false))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else
			{
				if (_type == 2)
				{
					String filename = "data/html/signboard.htm";
					String content = HtmCache.getInstance().getHtm(filename);
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					
					if (content == null)
					{
						html.setHtml("<html><body>Signboard is missing:<br>" + filename + "</body></html>");
					}
					else
					{
						html.setHtml(content);
					}
					
					player.sendPacket(html);
				}
				else if (_type == 0)
				{
					player.sendPacket(new ShowTownMap(_texture, getMapX(), getMapY()));
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public boolean isAutoAttackable(final L2Character attacker)
	{
		return false;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new StaticObject(this));
	}
}
