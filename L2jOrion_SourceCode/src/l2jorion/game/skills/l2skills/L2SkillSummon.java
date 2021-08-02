/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.skills.l2skills;

import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2CubicInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2SiegeSummonInstance;
import l2jorion.game.model.actor.instance.L2SummonInstance;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.PetInfo;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;
import l2jorion.util.random.Rnd;

public class L2SkillSummon extends L2Skill
{
	public static final int SKILL_CUBIC_MASTERY = 143;
	protected Map<Integer, L2CubicInstance> _cubics = new FastMap<>();
	
	private final int _npcId;
	private final float _expPenalty;
	private final boolean _isCubic;
	
	// cubic AI
	// Activation time for a cubic
	private final int _activationtime;
	// Activation chance for a cubic.
	private final int _activationchance;
	// What is the total lifetime of summons (in millisecs)
	private final int _summonTotalLifeTime;
	
	public L2SkillSummon(final StatsSet set)
	{
		super(set);
		
		_npcId = set.getInteger("npcId", 0); // default for undescribed skills
		_expPenalty = set.getFloat("expPenalty", 0.f);
		_isCubic = set.getBool("isCubic", false);
		
		_activationtime = set.getInteger("activationtime", 8);
		_activationchance = set.getInteger("activationchance", 30);
		
		if (Config.CUSTOM_SUMMON_LIFE)
		{
			_summonTotalLifeTime = Config.CUSTOM_SUMMON_LIFE_TIME;
		}
		else
		{
			_summonTotalLifeTime = set.getInteger("summonTotalLifeTime", 1200000); // 20 minutes default
		}
		_summonTimeLostIdle = set.getInteger("summonTimeLostIdle", 0);
		_summonTimeLostActive = set.getInteger("summonTimeLostActive", 0);
		
		_itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
		_itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
		_itemConsumeTime = set.getInteger("itemConsumeTime", 0);
		_itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
	}
	
	public boolean checkCondition(final L2Character activeChar)
	{
		if (activeChar instanceof L2PcInstance)
		{
			final L2PcInstance player = (L2PcInstance) activeChar;
			
			if (isCubic())
			{
				if (getTargetType() != L2Skill.SkillTargetType.TARGET_SELF)
				{
					return true; // Player is always able to cast mass cubic skill
				}
				int mastery = player.getSkillLevel(SKILL_CUBIC_MASTERY);
				if (mastery < 0)
				{
					mastery = 0;
				}
				
				final int count = player.getCubics().size();
				
				if (count > mastery)
				{
					player.sendMessage("You already have " + count + " cubic(s).");
					return false;
				}
			}
			else
			{
				if (player.inObserverMode())
				{
					return false;
				}
				
				if (!player.isGM() && player.getPet() != null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
					return false;
				}
			}
		}
		return super.checkCondition(activeChar, null, false);
	}
	
	@Override
	public void useSkill(final L2Character caster, final L2Object[] targets)
	{
		if (caster.isAlikeDead() || !(caster instanceof L2PcInstance))
		{
			return;
		}
		
		final L2PcInstance activeChar = (L2PcInstance) caster;
		
		// Skill 2046 only used for animation
		if (getId() == 2046)
		{
			return;
		}
		
		if (_npcId == 0)
		{
			activeChar.sendMessage("Summon skill " + getId() + " not described yet");
			return;
		}
		
		if (_isCubic)
		{
			// Gnacik :
			// If skill is enchanted calculate cubic skill level based on enchant
			// 8 at 101 (+1 Power)
			// 12 at 130 (+30 Power)
			// Because 12 is max 5115-5117 skills
			// TODO: make better method of calculation, dunno how its calculated on offi
			int _cubicSkillLevel = getLevel();
			if (_cubicSkillLevel > 100)
			{
				_cubicSkillLevel = ((getLevel() - 100) / 7) + 8;
			}
			
			if (targets.length > 1) // Mass cubic skill
			{
				for (final L2Object obj : targets)
				{
					if (!(obj instanceof L2PcInstance))
					{
						continue;
					}
					final L2PcInstance player = ((L2PcInstance) obj);
					int mastery = player.getSkillLevel(SKILL_CUBIC_MASTERY);
					if (mastery < 0)
					{
						mastery = 0;
					}
					if (mastery == 0 && !player.getCubics().isEmpty())
					{
						// Player can have only 1 cubic - we shuld replace old cubic with new one
						for (L2CubicInstance c : player.getCubics().values())
						{
							c.stopAction();
							c = null;
						}
						player.getCubics().clear();
					}
					// XXX: Should remove first cubic summoned and replace with new cubic
					if (player.getCubics().containsKey(_npcId))
					{
						final L2CubicInstance cubic = player.getCubic(_npcId);
						cubic.stopAction();
						cubic.cancelDisappear();
						player.delCubic(_npcId);
					}
					
					if (player.getCubics().size() > mastery)
					{
						continue;
					}
					
					if (player == activeChar)
					{
						player.addCubic(_npcId, _cubicSkillLevel, getPower(), _activationtime, _activationchance, _summonTotalLifeTime, false);
					}
					else
					{
						// given by other player
						player.addCubic(_npcId, _cubicSkillLevel, getPower(), _activationtime, _activationchance, _summonTotalLifeTime, true);
					}
					
					player.broadcastUserInfo();
				}
				return;
			}
			
			int mastery = activeChar.getSkillLevel(SKILL_CUBIC_MASTERY);
			if (mastery < 0)
			{
				mastery = 0;
			}
			if (activeChar.getCubics().containsKey(_npcId))
			{
				final L2CubicInstance cubic = activeChar.getCubic(_npcId);
				cubic.stopAction();
				cubic.cancelDisappear();
				activeChar.delCubic(_npcId);
			}
			if (activeChar.getCubics().size() > mastery)
			{
				// If maximum amount is reached, first cubic is removed.
				// Players with no mastery can have only one cubic.
				final int removedCubicId = (int) activeChar.getCubics().keySet().toArray()[0];
				final L2CubicInstance removedCubic = activeChar.getCubic(removedCubicId);
				removedCubic.stopAction();
				removedCubic.cancelDisappear();
				activeChar.delCubic(removedCubic.getId());
			}
			activeChar.addCubic(_npcId, _cubicSkillLevel, getPower(), _activationtime, _activationchance, _summonTotalLifeTime, false);
			activeChar.broadcastUserInfo();
			return;
		}
		
		if (activeChar.getPet() != null || activeChar.isMounted())
		{
			if (Config.DEBUG)
			{
				LOG.debug("player has a pet already. Ignore summon skill.");
			}
			return;
		}
		
		L2SummonInstance summon;
		final L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(_npcId);
		if (summonTemplate == null)
		{
			LOG.warn("Summon attempt for nonexisting NPC ID:" + _npcId + ", skill ID:" + this.getId());
			return;
		}
		
		if (summonTemplate.type.equalsIgnoreCase("L2SiegeSummon"))
		{
			summon = new L2SiegeSummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
		}
		else
		{
			summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
		}
		
		summon.setName(summonTemplate.name);
		summon.setTitle(activeChar.getName());
		
		summon.setExpPenalty(_expPenalty);
		if (summon.getLevel() >= ExperienceData.getInstance().getMaxLevel())
		{
			summon.getStat().setExp(ExperienceData.getInstance().getExpForLevel(ExperienceData.getInstance().getMaxPetLevel() - 1));
			LOG.warn("Summon (" + summon.getName() + ") NpcID: " + summon.getNpcId() + " has a level above 75. Please rectify.");
		}
		else
		{
			summon.getStat().setExp(ExperienceData.getInstance().getExpForLevel(summon.getLevel() % ExperienceData.getInstance().getMaxPetLevel()));
		}
		
		summon.setCurrentHp(summon.getMaxHp());
		summon.setCurrentMp(summon.getMaxMp());
		summon.setHeading(activeChar.getHeading());
		summon.setRunning();
		
		activeChar.setPet(summon);
		
		L2World.getInstance().storeObject(summon);
		
		if (getTargetType() == SkillTargetType.TARGET_CORPSE_MOB)
		{
			final L2Character target = (L2Character) targets[0];
			if (target.isDead() && target instanceof L2NpcInstance)
			{
				summon.spawnMe(target.getX(), target.getY(), target.getZ() + 5);
				((L2NpcInstance) target).endDecayTask();
			}
		}
		else
		{
			summon.spawnMe(activeChar.getX() + Rnd.get(40) - 20, activeChar.getY() + Rnd.get(40) - 20, activeChar.getZ());
		}
		
		summon.setFollowStatus(true);
		summon.setShowSummonAnimation(false);
		
		activeChar.sendPacket(new PetInfo(summon));
	}
	
	public final boolean isCubic()
	{
		return _isCubic;
	}
}