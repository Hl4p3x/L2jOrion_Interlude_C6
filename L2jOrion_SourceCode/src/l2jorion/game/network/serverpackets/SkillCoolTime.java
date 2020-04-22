package l2jorion.game.network.serverpackets;

import java.util.Collection;
import java.util.Iterator;

import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance.TimeStamp;

public class SkillCoolTime extends L2GameServerPacket
{
	public Collection<TimeStamp> _reuseTimeStamps;
	
	public SkillCoolTime(L2PcInstance cha)
	{
		_reuseTimeStamps = cha.getReuseTimeStamps();
		Iterator<TimeStamp> iter = _reuseTimeStamps.iterator();
		while (iter.hasNext())
		{
			if (!iter.next().hasNotPassed()) // remove expired timestamps
			{
				iter.remove();
			}
		}
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xc1);
		writeD(_reuseTimeStamps.size()); // list size
		for (TimeStamp ts : _reuseTimeStamps)
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