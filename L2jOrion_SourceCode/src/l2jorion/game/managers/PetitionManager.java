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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.clientpackets.Say2;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.L2GameServerPacket;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public final class PetitionManager
{
	protected static final Logger LOG = LoggerFactory.getLogger(PetitionManager.class);
	private static PetitionManager _instance;
	
	private final Map<Integer, Petition> _pendingPetitions;
	private final Map<Integer, Petition> _completedPetitions;
	
	private static enum PetitionState
	{
		Pending,
		Responder_Cancel,
		Responder_Missing,
		Responder_Reject,
		Responder_Complete,
		Petitioner_Cancel,
		Petitioner_Missing,
		In_Process,
		Completed
	}
	
	private static enum PetitionType
	{
		Immobility,
		Recovery_Related,
		Bug_Report,
		Quest_Related,
		Bad_User,
		Suggestions,
		Game_Tip,
		Operation_Related,
		Other
	}
	
	public static PetitionManager getInstance()
	{
		if (_instance == null)
		{
			//LOG.info("Initializing PetitionManager");
			_instance = new PetitionManager();
		}
		
		return _instance;
	}
	
	private class Petition
	{
		private final long _submitTime = System.currentTimeMillis();
		// private long _endTime = -1;
		
		private final int _id;
		private final PetitionType _type;
		private PetitionState _state = PetitionState.Pending;
		private final String _content;
		
		private final List<CreatureSay> _messageLogger = new FastList<>();
		
		private final L2PcInstance _petitioner;
		private L2PcInstance _responder;
		
		public Petition(final L2PcInstance petitioner, final String petitionText, int petitionType)
		{
			petitionType--;
			_id = IdFactory.getInstance().getNextId();
			if (petitionType >= PetitionType.values().length)
			{
				LOG.warn("PetitionManager:Petition : invalid petition type (received type was +1) : " + petitionType);
			}
			_type = PetitionType.values()[petitionType];
			_content = petitionText;
			
			_petitioner = petitioner;
		}
		
		protected boolean addLogMessage(final CreatureSay cs)
		{
			return _messageLogger.add(cs);
		}
		
		protected List<CreatureSay> getLogMessages()
		{
			return _messageLogger;
		}
		
		public boolean endPetitionConsultation(final PetitionState endState)
		{
			setState(endState);
			// _endTime = System.currentTimeMillis();
			
			if (getResponder() != null && getResponder().isOnline() == 1)
			{
				if (endState == PetitionState.Responder_Reject)
				{
					getPetitioner().sendMessage("Your petition was rejected. Please try again later.");
				}
				else
				{
					// Ending petition consultation with <Player>.
					SystemMessage sm = new SystemMessage(SystemMessageId.PETITION_ENDED_WITH_S1);
					sm.addString(getPetitioner().getName());
					getResponder().sendPacket(sm);
					sm = null;
					
					if (endState == PetitionState.Petitioner_Cancel)
					{
						// Receipt No. <ID> petition cancelled.
						sm = new SystemMessage(SystemMessageId.RECENT_NO_S1_CANCELED);
						sm.addNumber(getId());
						getResponder().sendPacket(sm);
						sm = null;
					}
				}
			}
			
			// End petition consultation and inform them, if they are still online.
			if (getPetitioner() != null && getPetitioner().isOnline() == 1)
			{
				getPetitioner().sendPacket(new SystemMessage(SystemMessageId.THIS_END_THE_PETITION_PLEASE_PROVIDE_FEEDBACK));
			}
			
			getCompletedPetitions().put(getId(), this);
			return getPendingPetitions().remove(getId()) != null;
		}
		
		public String getContent()
		{
			return _content;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public L2PcInstance getPetitioner()
		{
			return _petitioner;
		}
		
		public L2PcInstance getResponder()
		{
			return _responder;
		}
		
		// public long getEndTime()
		// {
		// return _endTime;
		// }
		
		public long getSubmitTime()
		{
			return _submitTime;
		}
		
		public PetitionState getState()
		{
			return _state;
		}
		
		public String getTypeAsString()
		{
			return _type.toString().replace("_", " ");
		}
		
		public void sendPetitionerPacket(final L2GameServerPacket responsePacket)
		{
			if (getPetitioner() == null || getPetitioner().isOnline() == 0)
				// endPetitionConsultation(PetitionState.Petitioner_Missing);
				return;
			
			getPetitioner().sendPacket(responsePacket);
		}
		
		public void sendResponderPacket(final L2GameServerPacket responsePacket)
		{
			if (getResponder() == null || getResponder().isOnline() == 0)
			{
				endPetitionConsultation(PetitionState.Responder_Missing);
				return;
			}
			
			getResponder().sendPacket(responsePacket);
		}
		
		public void setState(final PetitionState state)
		{
			_state = state;
		}
		
		public void setResponder(final L2PcInstance respondingAdmin)
		{
			if (getResponder() != null)
				return;
			
			_responder = respondingAdmin;
		}
	}
	
	private PetitionManager()
	{
		_pendingPetitions = new FastMap<>();
		_completedPetitions = new FastMap<>();
	}
	
	public void clearCompletedPetitions()
	{
		final int numPetitions = getPendingPetitionCount();
		
		getCompletedPetitions().clear();
		LOG.info("PetitionManager: Completed petition data cleared. " + numPetitions + " petition(s) removed.");
	}
	
	public void clearPendingPetitions()
	{
		final int numPetitions = getPendingPetitionCount();
		
		getPendingPetitions().clear();
		LOG.info("PetitionManager: Pending petition queue cleared. " + numPetitions + " petition(s) removed.");
	}
	
	public boolean acceptPetition(final L2PcInstance respondingAdmin, final int petitionId)
	{
		if (!isValidPetition(petitionId))
			return false;
		
		Petition currPetition = getPendingPetitions().get(petitionId);
		
		if (currPetition.getResponder() != null)
			return false;
		
		currPetition.setResponder(respondingAdmin);
		currPetition.setState(PetitionState.In_Process);
		
		// Petition application accepted. (Send to Petitioner)
		currPetition.sendPetitionerPacket(new SystemMessage(SystemMessageId.PETITION_APP_ACCEPTED));
		
		// Petition application accepted. Reciept No. is <ID>
		SystemMessage sm = new SystemMessage(SystemMessageId.PETITION_ACCEPTED_RECENT_NO_S1);
		sm.addNumber(currPetition.getId());
		currPetition.sendResponderPacket(sm);
		
		// Petition consultation with <Player> underway.
		sm = new SystemMessage(SystemMessageId.PETITION_WITH_S1_UNDER_WAY);
		sm.addString(currPetition.getPetitioner().getName());
		currPetition.sendResponderPacket(sm);
		sm = null;
		currPetition = null;
		return true;
	}
	
	public boolean cancelActivePetition(final L2PcInstance player)
	{
		for (final Petition currPetition : getPendingPetitions().values())
		{
			if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId())
				return currPetition.endPetitionConsultation(PetitionState.Petitioner_Cancel);
			
			if (currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId())
				return currPetition.endPetitionConsultation(PetitionState.Responder_Cancel);
		}
		
		return false;
	}
	
	public void checkPetitionMessages(final L2PcInstance petitioner)
	{
		if (petitioner != null)
		{
			for (final Petition currPetition : getPendingPetitions().values())
			{
				if (currPetition == null)
				{
					continue;
				}
				
				if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == petitioner.getObjectId())
				{
					for (final CreatureSay logMessage : currPetition.getLogMessages())
					{
						petitioner.sendPacket(logMessage);
					}
					
					return;
				}
			}
		}
	}
	
	public boolean endActivePetition(final L2PcInstance player)
	{
		if (!player.isGM())
			return false;
		
		for (final Petition currPetition : getPendingPetitions().values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			if (currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId())
				return currPetition.endPetitionConsultation(PetitionState.Completed);
		}
		
		return false;
	}
	
	protected Map<Integer, Petition> getCompletedPetitions()
	{
		return _completedPetitions;
	}
	
	protected Map<Integer, Petition> getPendingPetitions()
	{
		return _pendingPetitions;
	}
	
	public int getPendingPetitionCount()
	{
		return getPendingPetitions().size();
	}
	
	public int getPlayerTotalPetitionCount(final L2PcInstance player)
	{
		if (player == null)
			return 0;
		
		int petitionCount = 0;
		
		for (final Petition currPetition : getPendingPetitions().values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId())
			{
				petitionCount++;
			}
		}
		
		for (final Petition currPetition : getCompletedPetitions().values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId())
			{
				petitionCount++;
			}
		}
		
		return petitionCount;
	}
	
	public boolean isPetitionInProcess()
	{
		for (final Petition currPetition : getPendingPetitions().values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			if (currPetition.getState() == PetitionState.In_Process)
				return true;
		}
		
		return false;
	}
	
	public boolean isPetitionInProcess(final int petitionId)
	{
		if (!isValidPetition(petitionId))
			return false;
		
		final Petition currPetition = getPendingPetitions().get(petitionId);
		return currPetition.getState() == PetitionState.In_Process;
	}
	
	public boolean isPlayerInConsultation(final L2PcInstance player)
	{
		if (player != null)
		{
			for (final Petition currPetition : getPendingPetitions().values())
			{
				if (currPetition == null)
				{
					continue;
				}
				
				if (currPetition.getState() != PetitionState.In_Process)
				{
					continue;
				}
				
				if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId() || currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId())
					return true;
			}
		}
		
		return false;
	}
	
	public boolean isPetitioningAllowed()
	{
		return Config.PETITIONING_ALLOWED;
	}
	
	public boolean isPlayerPetitionPending(final L2PcInstance petitioner)
	{
		if (petitioner != null)
		{
			for (final Petition currPetition : getPendingPetitions().values())
			{
				if (currPetition == null)
				{
					continue;
				}
				
				if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == petitioner.getObjectId())
					return true;
			}
		}
		
		return false;
	}
	
	private boolean isValidPetition(final int petitionId)
	{
		return getPendingPetitions().containsKey(petitionId);
	}
	
	public boolean rejectPetition(final L2PcInstance respondingAdmin, final int petitionId)
	{
		if (!isValidPetition(petitionId))
			return false;
		
		final Petition currPetition = getPendingPetitions().get(petitionId);
		
		if (currPetition.getResponder() != null)
			return false;
		
		currPetition.setResponder(respondingAdmin);
		return currPetition.endPetitionConsultation(PetitionState.Responder_Reject);
	}
	
	public boolean sendActivePetitionMessage(final L2PcInstance player, final String messageText)
	{
		// if (!isPlayerInConsultation(player))
		// return false;
		
		CreatureSay cs;
		
		for (final Petition currPetition : getPendingPetitions().values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			if (currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId())
			{
				cs = new CreatureSay(player.getObjectId(), Say2.PETITION_PLAYER, player.getName(), messageText);
				currPetition.addLogMessage(cs);
				
				currPetition.sendResponderPacket(cs);
				currPetition.sendPetitionerPacket(cs);
				
				cs = null;
				return true;
			}
			
			if (currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId())
			{
				cs = new CreatureSay(player.getObjectId(), Say2.PETITION_GM, player.getName(), messageText);
				currPetition.addLogMessage(cs);
				
				currPetition.sendResponderPacket(cs);
				currPetition.sendPetitionerPacket(cs);
				cs = null;
				return true;
			}
		}
		return false;
	}
	
	public void sendPendingPetitionList(final L2PcInstance activeChar)
	{
		final TextBuilder htmlContent = new TextBuilder("<html><body>" + "<center><font color=\"LEVEL\">Current Petitions</font><br><table width=\"300\">");
		final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM HH:mm z");
		
		if (getPendingPetitionCount() == 0)
		{
			htmlContent.append("<tr><td colspan=\"4\">There are no currently pending petitions.</td></tr>");
		}
		else
		{
			htmlContent.append("<tr><td></td><td><font color=\"999999\">Petitioner</font></td>" + "<td><font color=\"999999\">Petition Type</font></td><td><font color=\"999999\">Submitted</font></td></tr>");
		}
		
		for (final Petition currPetition : getPendingPetitions().values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			htmlContent.append("<tr><td>");
			
			if (currPetition.getState() != PetitionState.In_Process)
			{
				htmlContent.append("<button value=\"View\" action=\"bypass -h admin_view_petition " + currPetition.getId() + "\" " + "width=\"40\" height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			}
			else
			{
				htmlContent.append("<font color=\"999999\">In Process</font>");
			}
			
			htmlContent.append("</td><td>" + currPetition.getPetitioner().getName() + "</td><td>" + currPetition.getTypeAsString() + "</td><td>" + dateFormat.format(new Date(currPetition.getSubmitTime())) + "</td></tr>");
		}
		
		htmlContent.append("</table><br><button value=\"Refresh\" action=\"bypass -h admin_view_petitions\" width=\"50\" " + "height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br><button value=\"Back\" action=\"bypass -h admin_admin\" " + "width=\"40\" height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		
		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
		htmlMsg.setHtml(htmlContent.toString());
		activeChar.sendPacket(htmlMsg);
		htmlMsg = null;
	}
	
	public int submitPetition(final L2PcInstance petitioner, final String petitionText, final int petitionType)
	{
		// Create a new petition instance and add it to the list of pending petitions.
		Petition newPetition = new Petition(petitioner, petitionText, petitionType);
		final int newPetitionId = newPetition.getId();
		getPendingPetitions().put(newPetitionId, newPetition);
		newPetition = null;
		
		// Notify all GMs that a new petition has been submitted.
		String msgContent = petitioner.getName() + " has submitted a new petition."; // (ID: " + newPetitionId + ").";
		GmListTable.broadcastToGMs(new CreatureSay(petitioner.getObjectId(), 17, "Petition System", msgContent));
		msgContent = null;
		
		return newPetitionId;
	}
	
	public void viewPetition(final L2PcInstance activeChar, final int petitionId)
	{
		if (!activeChar.isGM())
			return;
		
		if (!isValidPetition(petitionId))
			return;
		
		Petition currPetition = getPendingPetitions().get(petitionId);
		TextBuilder htmlContent = new TextBuilder("<html><body>");
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd MMM HH:mm z");
		
		htmlContent.append("<center><br><font color=\"LEVEL\">Petition #" + currPetition.getId() + "</font><br1>");
		htmlContent.append("<img src=\"L2UI.SquareGray\" width=\"200\" height=\"1\"></center><br>");
		htmlContent.append("Submit Time: " + dateFormat.format(new Date(currPetition.getSubmitTime())) + "<br1>");
		htmlContent.append("Petitioner: " + currPetition.getPetitioner().getName() + "<br1>");
		htmlContent.append("Petition Type: " + currPetition.getTypeAsString() + "<br>" + currPetition.getContent() + "<br>");
		htmlContent.append("<center><button value=\"Accept\" action=\"bypass -h admin_accept_petition " + currPetition.getId() + "\"" + "width=\"50\" height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br1>");
		htmlContent.append("<button value=\"Reject\" action=\"bypass -h admin_reject_petition " + currPetition.getId() + "\" " + "width=\"50\" height=\"15\" back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>");
		htmlContent.append("<button value=\"Back\" action=\"bypass -h admin_view_petitions\" width=\"40\" height=\"15\" back=\"sek.cbui94\" " + "fore=\"sek.cbui92\"></center>");
		htmlContent.append("</body></html>");
		
		currPetition = null;
		dateFormat = null;
		
		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
		htmlMsg.setHtml(htmlContent.toString());
		activeChar.sendPacket(htmlMsg);
		
		htmlMsg = null;
		htmlContent = null;
	}
}
