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
package l2jorion.game.managers;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.model.quest.Quest;
import l2jorion.game.scripting.L2ScriptEngineManager;
import l2jorion.game.scripting.ScriptManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class QuestManager extends ScriptManager<Quest>
{
	protected static final Logger LOG = LoggerFactory.getLogger(QuestManager.class);
	private Map<String, Quest> _quests = new FastMap<>();
	private static QuestManager _instance;
	
	public static QuestManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new QuestManager();
		}
		return _instance;
	}
	
	public QuestManager()
	{
	}
	
	public final boolean reload(final String questFolder)
	{
		final Quest q = getQuest(questFolder);
		if (q == null)
			return false;
		return q.reload();
	}
	
	public final boolean reload(final int questId)
	{
		final Quest q = getQuest(questId);
		if (q == null)
		{
			return false;
		}
		
		return q.reload();
	}
	
	public final void reloadAllQuests() throws IOException
	{
		LOG.info("Reloading Server Scripts");
		// unload all scripts
		for (final Quest quest : _quests.values())
		{
			if (quest != null)
			{
				quest.unload();
			}
		}
		// now load all scripts
		final File scripts = new File(Config.DATAPACK_ROOT, "config/scripts.cfg");
		L2ScriptEngineManager.getInstance().executeScriptList(scripts);
		QuestManager.getInstance().report();
	}
	
	public final void report()
	{
		LOG.info("QuestManager: Loaded: " + _quests.size() + " quests");
	}
	
	public final void save()
	{
		for (final Quest q : getQuests().values())
		{
			q.saveGlobalData();
		}
	}
	
	// =========================================================
	// Property - Public
	public final Quest getQuest(String name)
	{
		return getQuests().get(name);
	}
	
	public final Quest getQuest(final int questId)
	{
		for (final Quest q : getQuests().values())
		{
			if (q.getQuestIntId() == questId)
				return q;
		}
		return null;
	}
	
	public final void addQuest(Quest newQuest)
	{
		if (newQuest == null)
		{
			throw new IllegalArgumentException("Quest argument cannot be null");
		}
		Quest old = _quests.get(newQuest.getName());
		
		// FIXME: unloading the old quest at this point is a tad too late.
		// the new quest has already initialized itself and read the data, starting
		// an unpredictable number of tasks with that data.  The old quest will now
		// save data which will never be read.
		// However, requesting the newQuest to re-read the data is not necessarily a 
		// good option, since the newQuest may have already started timers, spawned NPCs
		// or taken any other action which it might re-take by re-reading the data. 
		// the current solution properly closes the running tasks of the old quest but
		// ignores the data; perhaps the least of all evils...
		if (old != null && old.isRealQuest())
		{
			old.unload();
			LOG.info("Replaced: (" + old.getName() + ") with a new version (" + newQuest.getName() + ")");
			
		}
		_quests.put(newQuest.getName(), newQuest);
	}
	
	public final FastMap<String, Quest> getQuests()
	{
		if (_quests == null)
		{
			_quests = new FastMap<>();
		}
		
		return (FastMap<String, Quest>) _quests;
	}
	
	/**
	 * This will reload quests
	 */
	public static void reload()
	{
		_instance = new QuestManager();
	}
	
	@Override
	public Iterable<Quest> getAllManagedScripts()
	{
		return _quests.values();
	}
	
	@Override
	public boolean unload(final Quest ms)
	{
		ms.saveGlobalData();
		return removeQuest(ms);
	}
	
	@Override
	public String getScriptManagerName()
	{
		return "QuestManager";
	}
	
	public final boolean removeQuest(final Quest q)
	{
		return _quests.remove(q.getName()) != null;
	}
	
	public final void unloadAllQuests()
	{
		LOG.info("Unloading Server Quests");
		// unload all scripts
		for (final Quest quest : _quests.values())
		{
			if (quest != null)
			{
				quest.unload();
			}
		}
		QuestManager.getInstance().report();
	}
}
