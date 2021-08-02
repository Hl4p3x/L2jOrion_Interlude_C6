/* L2jOrion Project - www.l2jorion.com 
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
package l2jorion.game.model.actor.instance;

import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.ai.L2CharacterAI;
import l2jorion.game.ai.L2ControllableMobAI;
import l2jorion.game.model.L2Character;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class L2ControllableMobInstance extends L2MonsterInstance
{
	private static Logger LOG = LoggerFactory.getLogger(L2ControllableMobInstance.class);
	private boolean _isInvul;
	private L2ControllableMobAI _aiBackup; // to save ai, avoiding beeing detached
	
	@Override
	public boolean isAggressive()
	{
		return true;
	}
	
	@Override
	public int getAggroRange()
	{
		// force mobs to be aggro
		return 500;
	}
	
	public L2ControllableMobInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public L2CharacterAI initAI()
	{
		return new L2ControllableMobAI(this);
	}
	
	@Override
	public boolean isInvul()
	{
		return _isInvul;
	}
	
	public void setInvul(final boolean isInvul)
	{
		_isInvul = isInvul;
	}
	
	@Override
	public void reduceCurrentHp(double i, final L2Character attacker, final boolean awake)
	{
		if (isInvul() || isDead())
			return;
		
		if (awake)
		{
			stopSleeping(null);
		}
		
		i = getCurrentHp() - i;
		
		if (i < 0)
		{
			i = 0;
		}
		
		setCurrentHp(i);
		
		if (isDead())
		{
			// first die (and calculate rewards), if currentHp < 0,
			// then overhit may be calculated
			if (Config.DEBUG)
			{
				LOG.debug("char is dead.");
			}
			
			stopMove(null);
			
			// Start the doDie process
			doDie(attacker);
			
			// now reset currentHp to zero
			setCurrentHp(0);
		}
	}
	
	@Override
	public boolean doDie(final L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		removeAI();
		return true;
	}
	
	@Override
	public void deleteMe()
	{
		removeAI();
		super.deleteMe();
	}
	
	/**
	 * Definitively remove AI
	 */
	protected void removeAI()
	{
		synchronized (this)
		{
			if (_aiBackup != null)
			{
				_aiBackup.setIntention(CtrlIntention.AI_INTENTION_IDLE);
				_aiBackup = null;
				_ai = null;
			}
		}
	}
}
