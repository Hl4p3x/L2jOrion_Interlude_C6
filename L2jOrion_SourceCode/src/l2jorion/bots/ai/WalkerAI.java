package l2jorion.bots.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2jorion.Config;
import l2jorion.bots.FakePlayer;
import l2jorion.bots.helpers.FakeHelpers;
import l2jorion.bots.model.HealingSpell;
import l2jorion.bots.model.OffensiveSpell;
import l2jorion.bots.model.SpellUsageCondition;
import l2jorion.bots.model.SupportSpell;
import l2jorion.bots.model.WalkNode;
import l2jorion.bots.model.WalkerType;
import l2jorion.bots.xml.botRandomWalk;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.enums.ShotType;
import l2jorion.game.model.Location;
import l2jorion.util.random.Rnd;

public class WalkerAI extends CombatAI
{
	protected WalkNode _currentWalkNode;
	private int currentStayIterations = 0;
	
	public WalkerAI(FakePlayer character)
	{
		super(character);
	}
	
	public List<WalkNode> getWalkNodes()
	{
		return botRandomWalk.getInstance().getWalkNode(_fakePlayer.getTownId());
	}
	
	@Override
	public void setup()
	{
		super.setup();
	}
	
	@Override
	public void thinkAndAct()
	{
		setBusyThinking(true);
		
		int x, y, z;
		handleDeath();
		applyDefaultBuffs();
		handleShots();
		selfSupportBuffs();
		
		if (getWalkNodes().isEmpty())
		{
			
			return;
		}
		
		if (_fakePlayer.isWalking())
		{
			if (!_fakePlayer.isMoving())
			{
				if (currentStayIterations < _fakePlayer.getCurrentWalkNode().getStayIterations())
				{
					currentStayIterations++;
					setBusyThinking(false);
					return;
				}
				
				_fakePlayer.seCurrentWalkNode(null);
				currentStayIterations = 0;
				_fakePlayer.setIsWalking(false);
				
				// Let's change bot's zone to another one
				if (botRandomWalk.getInstance().getLastTownId() > 1)
				{
					int townId = Rnd.get(1, botRandomWalk.getInstance().getLastTownId());
					if (10 > Rnd.get(100) && townId != _fakePlayer.getTownId())
					{
						_currentWalkNode = (WalkNode) botRandomWalk.getInstance().getWalkNode(townId).toArray()[Rnd.get(0, botRandomWalk.getInstance().getWalkNode(townId).size() - 1)];
						x = _currentWalkNode.getX();
						y = _currentWalkNode.getY();
						x += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
						y += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
						
						_fakePlayer.setTownId(townId);
						_fakePlayer.teleToLocation(x, y, _currentWalkNode.getZ(), true);
						setBusyThinking(false);
						return;
					}
				}
			}
		}
		
		if (!_fakePlayer.isWalking() && _fakePlayer.getCurrentWalkNode() == null)
		{
			_fakePlayer.seCurrentWalkNode((WalkNode) getWalkNodes().toArray()[Rnd.get(0, getWalkNodes().size() - 1)]);
			
			x = _fakePlayer.getCurrentWalkNode().getX();
			y = _fakePlayer.getCurrentWalkNode().getY();
			z = _fakePlayer.getCurrentWalkNode().getZ();
			
			x += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
			y += Rnd.get(-Config.BOTS_RANDOM_MAX_OFFSET, Config.BOTS_RANDOM_MAX_OFFSET);
			
			_fakePlayer.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(x, y, z, 0));
			_fakePlayer.setIsWalking(true);
		}
		
		setBusyThinking(false);
	}
	
	@Override
	protected int[][] getBuffs()
	{
		if (_fakePlayer.isMageClass())
		{
			return FakeHelpers.getMageBuffs();
		}
		return FakeHelpers.getFighterBuffs();
	}
	
	protected WalkerType getWalkerType()
	{
		return WalkerType.RANDOM;
	}
	
	@Override
	protected ShotType getShotType()
	{
		return ShotType.SOULSHOT;
	}
	
	@Override
	protected List<OffensiveSpell> getOffensiveSpells()
	{
		return Collections.emptyList();
	}
	
	@Override
	protected List<HealingSpell> getHealingSpells()
	{
		return Collections.emptyList();
	}
	
	@Override
	protected List<SupportSpell> getSelfSupportSpells()
	{
		List<SupportSpell> _selfSupportSpells = new ArrayList<>();
		
		if (_fakePlayer.getClassId().getId() == 88)
		{
			_selfSupportSpells.add(new SupportSpell(8, SpellUsageCondition.SELFBUFFS, 5, 1));
		}
		else if (_fakePlayer.getClassId().getId() == 93)
		{
			_selfSupportSpells.add(new SupportSpell(4, SpellUsageCondition.SELFBUFFS, 5, 1));
			_selfSupportSpells.add(new SupportSpell(357, SpellUsageCondition.SELFBUFFS, 5, 2));
		}
		else if (_fakePlayer.getClassId().getId() == 108)
		{
			_selfSupportSpells.add(new SupportSpell(357, SpellUsageCondition.SELFBUFFS, 5, 1));
		}
		else if (_fakePlayer.getClassId().getId() == 92)
		{
			_selfSupportSpells.add(new SupportSpell(4, SpellUsageCondition.SELFBUFFS, 5, 1));
		}
		else if (_fakePlayer.getClassId().getId() == 115)
		{
			_selfSupportSpells.add(new SupportSpell(1256, SpellUsageCondition.SELFBUFFS, 5, 1));
		}
		else if (_fakePlayer.getClassId().getId() == 114)
		{
			_selfSupportSpells.add(new SupportSpell(50, SpellUsageCondition.SELFBUFFS, 5, 1));
		}
		
		return _selfSupportSpells;
	}
}