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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.knownlist.NullKnownList;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.MyTargetSelected;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.ShowTownMap;
import l2jorion.game.network.serverpackets.StaticObject;

/**
 * GODSON ROX!.
 */
public class L2StaticObjectInstance extends L2Object
{
	
	/** The LOG. */
	private static Logger LOG = LoggerFactory.getLogger(L2StaticObjectInstance.class);
	
	/** The interaction distance of the L2StaticObjectInstance. */
	public static final int INTERACTION_DISTANCE = 150;
	
	/** The _static object id. */
	private int _staticObjectId;
	
	/** The _type. */
	private int _type = -1; // 0 - map signs, 1 - throne , 2 - arena signs
	
	/** The _x. */
	private int _x;
	
	/** The _y. */
	private int _y;
	
	/** The _texture. */
	private String _texture;
	
	/**
	 * Gets the static object id.
	 * @return Returns the StaticObjectId.
	 */
	public int getStaticObjectId()
	{
		return _staticObjectId;
	}
	
	/**
	 * Sets the static object id.
	 * @param StaticObjectId the new static object id
	 */
	public void setStaticObjectId(final int StaticObjectId)
	{
		_staticObjectId = StaticObjectId;
	}
	
	/**
	 * Instantiates a new l2 static object instance.
	 * @param objectId the object id
	 */
	public L2StaticObjectInstance(final int objectId)
	{
		super(objectId);
		setKnownList(new NullKnownList(this));
	}
	
	/**
	 * Gets the type.
	 * @return the type
	 */
	public int getType()
	{
		return _type;
	}
	
	/**
	 * Sets the type.
	 * @param type the new type
	 */
	public void setType(final int type)
	{
		_type = type;
	}
	
	/**
	 * Sets the map.
	 * @param texture the texture
	 * @param x the x
	 * @param y the y
	 */
	public void setMap(final String texture, final int x, final int y)
	{
		_texture = "town_map." + texture;
		_x = x;
		_y = y;
	}
	
	/**
	 * Gets the map x.
	 * @return the map x
	 */
	private int getMapX()
	{
		return _x;
	}
	
	/**
	 * Gets the map y.
	 * @return the map y
	 */
	private int getMapY()
	{
		return _y;
	}
	
	/**
	 * this is called when a player interacts with this NPC.
	 * @param player the player
	 */
	@Override
	public void onAction(final L2PcInstance player)
	{
		if (_type < 0)
		{
			LOG.info("L2StaticObjectInstance: StaticObject with invalid type! StaticObjectId: " + getStaticObjectId());
		}
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		}
		else
		{
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;
			
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!player.isInsideRadius(this, INTERACTION_DISTANCE, false, false))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				
				// Send a Server->Client packet ActionFailed (target is out of interaction range) to the L2PcInstance player
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
					player.sendPacket(ActionFailed.STATIC_PACKET);
					html = null;
					filename = null;
					content = null;
				}
				else if (_type == 0)
				{
					player.sendPacket(new ShowTownMap(_texture, getMapX(), getMapY()));
				}
				
				// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.L2Object#isAttackable()
	 */
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
