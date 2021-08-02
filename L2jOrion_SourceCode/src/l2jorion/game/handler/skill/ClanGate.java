package l2jorion.game.handler.skill;

import l2jorion.game.handler.ISkillHandler;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.GrandBossManager;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.L2Effect;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.siege.Castle;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.thread.ThreadPoolManager;

public class ClanGate implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.CLAN_GATE
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		L2PcInstance player = null;
		if (activeChar instanceof L2PcInstance)
		{
			player = (L2PcInstance) activeChar;
		}
		else
		{
			return;
		}
		// need more checking...
		if (player.isInFunEvent() || player.isInsideZone(ZoneId.ZONE_NOLANDING) || player.isInOlympiadMode() || player.isInsideZone(ZoneId.ZONE_PVP) || GrandBossManager.getInstance().getZone(player) != null)
		{
			player.sendMessage("Cannot open the portal here.");
			return;
		}
		
		L2Clan clan = player.getClan();
		if (clan != null)
		{
			if (CastleManager.getInstance().getCastleByOwner(clan) != null)
			{
				Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
				if (player.isCastleLord(castle.getCastleId()))
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new RemoveClanGate(castle.getCastleId(), player), skill.getTotalLifeTime());
					castle.createClanGate(player.getX(), player.getY(), player.getZ() + 20);
					player.getClan().broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_PORTAL_HAS_BEEN_CREATED));
					player.setIsParalyzed(true);
				}
			}
		}
		
		final L2Effect effect = player.getFirstEffect(skill.getId());
		if (effect != null && effect.isSelfEffect())
		{
			effect.exit(false);
		}
		
		skill.getEffectsSelf(player);
	}
	
	private class RemoveClanGate implements Runnable
	{
		private final int castle;
		private final L2PcInstance player;
		
		protected RemoveClanGate(final int castle, final L2PcInstance player)
		{
			this.castle = castle;
			this.player = player;
		}
		
		@Override
		public void run()
		{
			if (player != null)
			{
				player.setIsParalyzed(false);
			}
			CastleManager.getInstance().getCastleById(castle).destroyClanGate();
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
