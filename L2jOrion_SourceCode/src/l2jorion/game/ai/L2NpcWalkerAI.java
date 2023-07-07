package l2jorion.game.ai;

import java.util.List;

import l2jorion.Config;
import l2jorion.game.datatables.csv.NpcWalkerRoutesTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2NpcWalkerNode;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2NpcWalkerInstance;
import l2jorion.game.thread.ThreadPoolManager;

public class L2NpcWalkerAI extends L2CharacterAI implements Runnable
{
	private static final int DEFAULT_MOVE_DELAY = 0;
	
	private long _nextMoveTime;
	
	private boolean _walkingToNextPoint = false;
	
	int _homeX, _homeY, _homeZ;
	
	private List<L2NpcWalkerNode> _route;
	
	private int _currentPos;
	
	public L2NpcWalkerAI(L2Character creature)
	{
		super(creature);
		
		if (!Config.ALLOW_NPC_WALKERS)
		{
			return;
		}
		
		_route = NpcWalkerRoutesTable.getInstance().getRouteForNpc(getActor().getNpcId());
		
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public void run()
	{
		onEvtThink();
	}
	
	@Override
	protected void onEvtThink()
	{
		if (!Config.ALLOW_NPC_WALKERS)
		{
			return;
		}
		
		if (isWalkingToNextPoint())
		{
			checkArrived();
			return;
		}
		
		if (_nextMoveTime < System.currentTimeMillis())
		{
			walkToLocation();
		}
	}
	
	@Override
	protected void onEvtArrivedBlocked(final Location blocked_at_pos)
	{
		LOG.warn("NpcWalker ID: " + getActor().getNpcId() + ": Blocked at rote position [" + _currentPos + "], coords: " + blocked_at_pos.getX() + ", " + blocked_at_pos.getY() + ", " + blocked_at_pos.getZ() + ". Teleporting to next point");
		
		if (_route.size() <= _currentPos)
		{
			return;
		}
		
		final int destinationX = _route.get(_currentPos).getMoveX();
		final int destinationY = _route.get(_currentPos).getMoveY();
		final int destinationZ = _route.get(_currentPos).getMoveZ();
		
		getActor().teleToLocation(destinationX, destinationY, destinationZ, false);
		super.onEvtArrivedBlocked(blocked_at_pos);
	}
	
	private void checkArrived()
	{
		if (_route.size() <= _currentPos)
		{
			return;
		}
		
		final int destinationX = _route.get(_currentPos).getMoveX();
		final int destinationY = _route.get(_currentPos).getMoveY();
		final int destinationZ = _route.get(_currentPos).getMoveZ();
		
		if (getActor().getX() == destinationX && getActor().getY() == destinationY && getActor().getZ() == destinationZ)
		{
			String chat = _route.get(_currentPos).getChatText();
			
			if (chat != null && !chat.equals("NULL"))
			{
				try
				{
					getActor().broadcastChat(chat);
				}
				catch (final ArrayIndexOutOfBoundsException e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
					
					LOG.info("L2NpcWalkerInstance: Error, " + e);
				}
			}
			chat = null;
			
			long delay = _route.get(_currentPos).getDelay() * 1000;
			
			if (delay < 0)
			{
				delay = DEFAULT_MOVE_DELAY;
			}
			
			_nextMoveTime = System.currentTimeMillis() + delay;
			setWalkingToNextPoint(false);
		}
	}
	
	private void walkToLocation()
	{
		if (_currentPos < _route.size() - 1)
		{
			_currentPos++;
		}
		else
		{
			_currentPos = 0;
		}
		
		if (_route.size() <= _currentPos)
		{
			return;
		}
		
		final boolean moveType = _route.get(_currentPos).getRunning();
		
		if (moveType)
		{
			getActor().setRunning();
		}
		else
		{
			getActor().setWalking();
		}
		
		final int destinationX = _route.get(_currentPos).getMoveX();
		final int destinationY = _route.get(_currentPos).getMoveY();
		final int destinationZ = _route.get(_currentPos).getMoveZ();
		
		setWalkingToNextPoint(true);
		
		setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(destinationX, destinationY, destinationZ, 0));
	}
	
	@Override
	public L2NpcWalkerInstance getActor()
	{
		return (L2NpcWalkerInstance) super.getActor();
	}
	
	public int getHomeX()
	{
		return _homeX;
	}
	
	public int getHomeY()
	{
		return _homeY;
	}
	
	public int getHomeZ()
	{
		return _homeZ;
	}
	
	public void setHomeX(final int homeX)
	{
		_homeX = homeX;
	}
	
	public void setHomeY(final int homeY)
	{
		_homeY = homeY;
	}
	
	public void setHomeZ(final int homeZ)
	{
		_homeZ = homeZ;
	}
	
	public boolean isWalkingToNextPoint()
	{
		return _walkingToNextPoint;
	}
	
	public void setWalkingToNextPoint(final boolean value)
	{
		_walkingToNextPoint = value;
	}
}
