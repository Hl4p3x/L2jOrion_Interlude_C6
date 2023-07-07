package l2jorion.game.thread.daemons;

import l2jorion.Config;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.random.Rnd;

public class PcPoint implements Runnable
{
	private static Logger LOG = LoggerFactory.getLogger(PcPoint.class);
	
	private static PcPoint _instance;
	
	public static PcPoint getInstance()
	{
		if (_instance == null)
		{
			_instance = new PcPoint();
		}
		
		return _instance;
	}
	
	private PcPoint()
	{
		LOG.info("PcBang point event - Enabled");
	}
	
	@Override
	public void run()
	{
		
		int score = 0;
		for (L2PcInstance activeChar : L2World.getInstance().getAllPlayers().values())
		{
			
			if (activeChar.isOnline() == 1 && activeChar.getLevel() >= Config.PCB_MIN_LEVEL && !activeChar.isInOfflineMode())
			{
				score = Rnd.get(Config.PCB_POINT_MIN, Config.PCB_POINT_MAX);
				
				if (Rnd.get(100) <= Config.PCB_CHANCE_DUAL_POINT)
				{
					score *= 2;
					
					activeChar.addPcBangScore(score);
					
					SystemMessage sm = new SystemMessage(SystemMessageId.DOUBLE_POINTS_YOU_GOT_$51_GLASSES_PC);
					sm.addNumber(score);
					activeChar.sendPacket(sm);
					
					activeChar.updatePcBangWnd(score, true, true);
				}
				else
				{
					activeChar.addPcBangScore(score);
					
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_RECEVIED_$51_GLASSES_PC);
					sm.addNumber(score);
					activeChar.sendPacket(sm);
					
					activeChar.updatePcBangWnd(score, true, false);
				}
			}
		}
	}
}
