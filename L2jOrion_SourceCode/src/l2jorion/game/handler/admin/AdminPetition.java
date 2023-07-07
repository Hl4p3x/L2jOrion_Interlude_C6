package l2jorion.game.handler.admin;

import java.util.StringTokenizer;

import l2jorion.game.handler.IAdminCommandHandler;
import l2jorion.game.managers.PetitionManager;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;

public class AdminPetition implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_view_petitions",
		"admin_view_petition",
		"admin_accept_petition",
		"admin_reject_petition",
		"admin_reset_petitions"
	};
	
	private enum CommandEnum
	{
		admin_view_petitions,
		admin_view_petition,
		admin_accept_petition,
		admin_reject_petition,
		admin_reset_petitions
	}
	
	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		
		final CommandEnum comm = CommandEnum.valueOf(st.nextToken());
		
		if (comm == null)
		{
			return false;
		}
		
		switch (comm)
		{
			case admin_view_petitions:
			{
				PetitionManager.getInstance().sendPendingPetitionList(activeChar);
				return true;
			}
			case admin_view_petition:
			{
				int petitionId = -1;
				
				if (st.hasMoreTokens())
				{
					try
					{
						petitionId = Integer.parseInt(st.nextToken());
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Usage: //admin_view_petition petition_id");
						return false;
					}
				}
				
				PetitionManager.getInstance().viewPetition(activeChar, petitionId);
				return true;
				
			}
			case admin_accept_petition:
			{
				if (PetitionManager.getInstance().isPlayerInConsultation(activeChar))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.ONLY_ONE_ACTIVE_PETITION_AT_TIME));
					return true;
				}
				
				int petitionId = -1;
				
				if (st.hasMoreTokens())
				{
					try
					{
						petitionId = Integer.parseInt(st.nextToken());
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Usage: //admin_accept_petition petition_id");
						return false;
					}
				}
				
				if (PetitionManager.getInstance().isPetitionInProcess(petitionId))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_UNDER_PROCESS));
					return true;
				}
				
				if (!PetitionManager.getInstance().acceptPetition(activeChar, petitionId))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_UNDER_PETITION_CONSULTATION));
					return false;
				}
				
				return true;
			}
			case admin_reject_petition:
			{
				int petitionId = -1;
				
				if (st.hasMoreTokens())
				{
					try
					{
						petitionId = Integer.parseInt(st.nextToken());
					}
					catch (final Exception e)
					{
						activeChar.sendMessage("Usage: //admin_reject_petition petition_id");
						return false;
					}
				}
				
				if (!PetitionManager.getInstance().rejectPetition(activeChar, petitionId))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_CANCEL_PETITION_TRY_LATER));
					return false;
				}
				
				return true;
			}
			case admin_reset_petitions:
			{
				if (PetitionManager.getInstance().isPetitionInProcess())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_UNDER_PROCESS));
					return false;
				}
				
				PetitionManager.getInstance().clearPendingPetitions();
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}