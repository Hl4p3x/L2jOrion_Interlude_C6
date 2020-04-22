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
package l2jorion.game.ai.additional.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2jorion.Config;
import l2jorion.game.ai.additional.Antharas;
import l2jorion.game.ai.additional.Baium;
import l2jorion.game.ai.additional.Barakiel;
import l2jorion.game.ai.additional.Benom;
import l2jorion.game.ai.additional.Core;
import l2jorion.game.ai.additional.FairyTrees;
import l2jorion.game.ai.additional.Frintezza;
import l2jorion.game.ai.additional.Frozen;
import l2jorion.game.ai.additional.Golkonda;
import l2jorion.game.ai.additional.Gordon;
import l2jorion.game.ai.additional.Hallate;
import l2jorion.game.ai.additional.IceFairySirra;
import l2jorion.game.ai.additional.InterludeTutorial;
import l2jorion.game.ai.additional.Kernon;
import l2jorion.game.ai.additional.Monastery;
import l2jorion.game.ai.additional.Orfen;
import l2jorion.game.ai.additional.QueenAnt;
import l2jorion.game.ai.additional.SummonMinions;
import l2jorion.game.ai.additional.Transform;
import l2jorion.game.ai.additional.Valakas;
import l2jorion.game.ai.additional.VanHalter;
import l2jorion.game.ai.additional.VarkaKetraAlly;
import l2jorion.game.ai.additional.Zaken;
import l2jorion.game.ai.additional.ZombieGatekeepers;
import l2jorion.game.thread.ThreadPoolManager;

public class AILoader
{
	private static final Logger LOG = LoggerFactory.getLogger(AILoader.class);
	
	public static void init()
	{
		ThreadPoolManager.getInstance().scheduleAi(new Antharas(-1, "antharas", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Baium(-1, "baium", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Core(-1, "core", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new QueenAnt(-1, "queen_ant", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new VanHalter(-1, "vanhalter", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Gordon(-1, "Gordon", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Monastery(-1, "monastery", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Transform(-1, "transform", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new FairyTrees(-1, "FairyTrees", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new SummonMinions(-1, "SummonMinions", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new ZombieGatekeepers(-1, "ZombieGatekeepers", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new IceFairySirra(-1, "IceFairySirra", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Golkonda(-1, "Golkonda", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Hallate(-1, "Hallate", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Kernon(-1, "Kernon", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new VarkaKetraAlly(-1, "Varka Ketra Ally", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Barakiel(-1, "Barakiel", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Orfen(-1, "Orfen", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Zaken(-1, "Zaken", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Frintezza(-1, "Frintezza", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Valakas(-1, "valakas", "ai"), 0);
		ThreadPoolManager.getInstance().scheduleAi(new Benom(-1, "Benom", "ai"), 0);
		if (Config.LOAD_TUTORIAL)
		{
			ThreadPoolManager.getInstance().scheduleAi(new InterludeTutorial(-1, "Tutorial", "ai"), 0);
		}
		ThreadPoolManager.getInstance().scheduleAi(new Frozen(-1, "Frozen", "ai"), 0);
		
		LOG.info("AI: loaded.");
	}
}
