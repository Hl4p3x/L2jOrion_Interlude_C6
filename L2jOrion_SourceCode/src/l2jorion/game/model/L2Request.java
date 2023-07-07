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
package l2jorion.game.model;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketClient;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.ThreadPoolManager;

public class L2Request
{
	private static final int REQUEST_TIMEOUT = 15; // in secs
	
	protected L2PcInstance _player;
	protected L2PcInstance _partner;
	protected boolean _isRequestor;
	protected boolean _isAnswerer;
	protected PacketClient _requestPacket;
	
	public L2Request(final L2PcInstance player)
	{
		_player = player;
	}
	
	protected void clear()
	{
		_partner = null;
		_requestPacket = null;
		_isRequestor = false;
		_isAnswerer = false;
	}
	
	/**
	 * Set the L2PcInstance member of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR>
	 * <BR>
	 * @param partner
	 */
	private synchronized void setPartner(final L2PcInstance partner)
	{
		_partner = partner;
	}
	
	/**
	 * @return the L2PcInstance member of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public L2PcInstance getPartner()
	{
		return _partner;
	}
	
	/**
	 * Set the packet incomed from requester.
	 * @param packet
	 */
	private synchronized void setRequestPacket(final PacketClient packet)
	{
		_requestPacket = packet;
	}
	
	/**
	 * @return the packet originally incomed from requester.
	 */
	public PacketClient getRequestPacket()
	{
		return _requestPacket;
	}
	
	/**
	 * Checks if request can be made and in success case puts both PC on request state.
	 * @param partner
	 * @param packet
	 * @return
	 */
	public synchronized boolean setRequest(final L2PcInstance partner, final PacketClient packet)
	{
		if (partner == null)
		{
			_player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return false;
		}
		
		if (partner.getRequest().isProcessingRequest())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
			sm.addString(partner.getName());
			_player.sendPacket(sm);
			sm = null;
			
			return false;
		}
		
		if (isProcessingRequest())
		{
			_player.sendPacket(new SystemMessage(SystemMessageId.WAITING_FOR_ANOTHER_REPLY));
			return false;
		}
		
		_partner = partner;
		_requestPacket = packet;
		setOnRequestTimer(true);
		_partner.getRequest().setPartner(_player);
		_partner.getRequest().setRequestPacket(packet);
		_partner.getRequest().setOnRequestTimer(false);
		
		return true;
	}
	
	private void setOnRequestTimer(final boolean isRequestor)
	{
		_isRequestor = isRequestor ? true : false;
		_isAnswerer = isRequestor ? false : true;
		
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				clear();
			}
		}, REQUEST_TIMEOUT * 1000);
		
	}
	
	/**
	 * Clears PC request state. Should be called after answer packet receive.<BR>
	 * <BR>
	 */
	public void onRequestResponse()
	{
		if (_partner != null)
		{
			_partner.getRequest().clear();
		}
		
		clear();
	}
	
	/**
	 * @return true if a transaction is in progress.
	 */
	public boolean isProcessingRequest()
	{
		return _partner != null;
	}
}
