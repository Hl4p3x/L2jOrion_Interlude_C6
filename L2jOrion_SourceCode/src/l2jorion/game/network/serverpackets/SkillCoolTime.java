package l2jorion.game.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import l2jorion.game.model.TimeStamp;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.PacketServer;

public class SkillCoolTime extends PacketServer
{
	private final List<TimeStamp> _skillReuseTimeStamps = new ArrayList<>();
	
	public SkillCoolTime(L2PcInstance player)
	{
		final Map<Integer, TimeStamp> skillReuseTimeStamps = player.getSkillReuseTimeStamps();
		if (skillReuseTimeStamps != null)
		{
			for (TimeStamp ts : skillReuseTimeStamps.values())
			{
				if (ts.hasNotPassed())
				{
					_skillReuseTimeStamps.add(ts);
				}
			}
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xc1);
		writeD(_skillReuseTimeStamps.size());
		for (TimeStamp ts : _skillReuseTimeStamps)
		{
			writeD(ts.getSkillId());
			writeD(0);
			writeD((int) ts.getReuse() / 1000);
			writeD((int) ts.getRemaining() / 1000);
		}
	}
	
	@Override
	public String getType()
	{
		return "[S] c1 SkillCoolTime";
	}
}