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
package l2jorion.game.model.quest;

import static l2jorion.game.ai.CtrlIntention.AI_INTENTION_ATTACK;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.ai.CtrlEvent;
import l2jorion.game.cache.HtmCache;
import l2jorion.game.datatables.GmListTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.managers.CastleManager;
import l2jorion.game.managers.DimensionalRiftManager;
import l2jorion.game.managers.QuestManager;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2MonsterInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2RiftInvaderInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.entity.siege.Siege;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ConfirmDlg;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.scripting.ManagedScript;
import l2jorion.game.scripting.ScriptManager;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.util.Util;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public class Quest extends ManagedScript
{
	protected static final Logger LOG = LoggerFactory.getLogger(Quest.class);
	
	private static final String HTML_NONE_AVAILABLE = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	
	private static Map<String, Quest> _allEventsS = new FastMap<>();
	private final Map<String, ArrayList<QuestTimer>> _allEventTimers = new FastMap<>();
	
	private final int _questId;
	private final String _name;
	private final String _prefixPath; // used only for admin_quest_reload
	private final String _descr;
	private State _initialState;
	private final Map<String, State> _states;
	private FastList<Integer> _questItemIds;
	
	public static Collection<Quest> findAllEvents()
	{
		return _allEventsS.values();
	}
	
	public Quest(final int questId, final String name, final String descr)
	{
		_questId = questId;
		_name = name;
		_descr = descr;
		_states = new FastMap<>();
		
		StringBuffer temp = new StringBuffer(getClass().getCanonicalName());
		temp.delete(0, temp.indexOf(".scripts.") + 9);
		temp.delete(temp.indexOf(getClass().getSimpleName()), temp.length());
		_prefixPath = temp.toString();
		
		if (questId != 0)
		{
			QuestManager.getInstance().addQuest(Quest.this);
		}
		else
		{
			_allEventsS.put(name, this);
		}
		
		init_LoadGlobalData();
	}
	
	public int getQuestId()
	{
		return _questId;
	}
	
	public boolean isRealQuest()
	{
		return _questId > 0;
	}
	
	protected void init_LoadGlobalData()
	{
	}
	
	public void saveGlobalData()
	{
	}
	
	public static enum QuestEventType
	{
		NPC_FIRST_TALK(false),
		QUEST_START(true),
		QUEST_TALK(true),
		ON_FACTION_CALL(true),
		ON_SKILL_SEE(true),
		ON_SPELL_FINISHED(true),
		ON_AGGRO_RANGE_ENTER(true),
		ON_MOVEMENT(true),
		ON_SPAWN(true),
		ON_SKILL_USE(true),
		ON_KILL(true),
		ON_ATTACK(true),
		ON_ITEM_USE(true);
		
		// control whether this event type is allowed for the same npc template in multiple quests
		// or if the npc must be registered in at most one quest for the specified event
		private boolean _allowMultipleRegistration;
		
		QuestEventType(final boolean allowMultipleRegistration)
		{
			_allowMultipleRegistration = allowMultipleRegistration;
		}
		
		public boolean isMultipleRegistrationAllowed()
		{
			return _allowMultipleRegistration;
		}
	}
	
	public int getQuestIntId()
	{
		return _questId;
	}
	
	public void setInitialState(final State state)
	{
		_initialState = state;
	}
	
	public QuestState newQuestState(final L2PcInstance player)
	{
		final QuestState qs = new QuestState(this, player, getInitialState(), false);
		player.setQuestState(qs);
		Quest.createQuestInDb(qs);
		return qs;
	}
	
	public State getInitialState()
	{
		return _initialState;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getPrefixPath()
	{
		return _prefixPath;
	}
	
	public String getDescr()
	{
		return _descr;
	}
	
	public State addState(final State state)
	{
		_states.put(state.getName(), state);
		return state;
	}
	
	public void startQuestTimer(final String name, final long time, final L2NpcInstance npc, final L2PcInstance player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	public synchronized void startQuestTimer(final String name, final long time, final L2NpcInstance npc, final L2PcInstance player, final boolean repeating)
	{
		if (Config.DEBUG)
		{
			LOG.info("StartingQuestTimer for Quest " + this.getName());
			
			String info = "Event:" + name + " Time:" + time;
			if (npc != null)
			{
				info = info + " Npc:" + npc.getName();
			}
			
			if (player != null)
			{
				info = info + " Player:" + player.getName();
			}
			
			LOG.info(info + " Repeat:" + repeating);
		}
		
		synchronized (_allEventTimers)
		{
			
			ArrayList<QuestTimer> timers = _allEventTimers.get(name);
			
			// no timer exists with the same name, at all
			if (timers == null)
			{
				
				timers = new ArrayList<>();
				timers.add(new QuestTimer(this, name, time, npc, player, repeating));
				
				// a timer with this name exists, but may not be for the same set of npc and player
			}
			else
			{
				
				QuestTimer timer = null;
				for (int i = 0; i < timers.size(); i++)
				{
					
					final QuestTimer tmp = timers.get(i);
					
					if (tmp != null && tmp.isMatch(this, name, npc, player))
					{
						timer = tmp;
						break;
					}
					
				}
				
				// if there exists a timer with this name, allow the timer only if the [npc, player] set is unique
				// nulls act as wildcards
				if (timer == null)
				{
					timers.add(new QuestTimer(this, name, time, npc, player, repeating));
				}
				
			}
			
			_allEventTimers.put(name, timers);
			
		}
		
	}
	
	public QuestTimer getQuestTimer(final String name, final L2NpcInstance npc, final L2PcInstance player)
	{
		if (name == null)
		{
			return null;
		}
		
		synchronized (_allEventTimers)
		{
			final ArrayList<QuestTimer> qt = _allEventTimers.get(name);
			
			if (qt == null || qt.isEmpty())
			{
				return null;
			}
			
			for (int i = 0; i < qt.size(); i++)
			{
				
				final QuestTimer timer = qt.get(i);
				
				if (timer != null && timer.isMatch(this, name, npc, player))
				{
					return timer;
				}
			}
			return null;
		}
	}
	
	public void cancelQuestTimer(final String name, final L2NpcInstance npc, final L2PcInstance player)
	{
		if (name == null)
		{
			return;
		}
		
		synchronized (_allEventTimers)
		{
			final ArrayList<QuestTimer> qt = _allEventTimers.get(name);
			if (qt == null || qt.isEmpty())
			{
				return;
			}
			
			for (int i = 0; i < qt.size(); i++)
			{
				
				final QuestTimer timer = qt.get(i);
				if (timer != null && timer.isMatch(this, name, npc, player))
				{
					timer.cancel(false);
					qt.remove(timer);
					return;
				}
			}
		}
	}
	
	public void cancelQuestTimers(final String name)
	{
		if (name == null)
		{
			return;
		}
		
		synchronized (_allEventTimers)
		{
			final ArrayList<QuestTimer> timers = _allEventTimers.get(name);
			
			if (timers == null)
			{
				return;
			}
			
			for (final QuestTimer timer : timers)
			{
				if (timer != null)
				{
					timer.cancel(false);
					
				}
			}
			
			timers.clear();
			
		}
	}
	
	public void removeQuestTimer(final QuestTimer timer)
	{
		if (timer == null)
		{
			return;
		}
		
		synchronized (_allEventTimers)
		{
			final ArrayList<QuestTimer> timers = _allEventTimers.get(timer.getName());
			
			if (timers == null)
			{
				return;
			}
			
			timers.remove(timer);
		}
	}
	
	public final boolean notifyAttack(final L2NpcInstance npc, final L2PcInstance attacker, final int damage, final boolean isPet)
	{
		String res = null;
		
		try
		{
			res = onAttack(npc, attacker, damage, isPet);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(attacker, e);
		}
		
		return showResult(attacker, res);
	}
	
	public final boolean notifyDeath(final L2Character killer, final L2Character victim, final QuestState qs)
	{
		String res = null;
		
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(qs.getPlayer(), e);
		}
		
		return showResult(qs.getPlayer(), res);
	}
	
	public final boolean notifyEvent(final String event, final L2NpcInstance npc, final L2PcInstance player)
	{
		String res = null;
		
		try
		{
			res = onAdvEvent(event, npc, player);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(player, e);
		}
		
		return showResult(player, res);
	}
	
	public final boolean notifyKill(final L2NpcInstance npc, final L2PcInstance killer, final boolean isPet)
	{
		String res = null;
		
		try
		{
			res = onKill(npc, killer, isPet);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(killer, e);
		}
		
		return showResult(killer, res);
	}
	
	public final boolean notifyTalk(L2NpcInstance npc, QuestState qs)
	{
		String res = null;
		try
		{
			res = onTalk(npc, qs.getPlayer());
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(qs.getPlayer(), e);
		}
		
		qs.getPlayer().setLastQuestNpcObject(npc.getObjectId());
		
		return showResult(qs.getPlayer(), res);
	}
	
	public final void notifyFirstTalk(L2NpcInstance npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			showError(player, e);
			return;
		}
		
		player.setLastQuestNpcObject(npc.getObjectId());
		
		showResult(player, res);
		
		if (res == null && !(npc.getTemplate().getType().contains("L2ClanHallSiege")))
		{
			npc.showChatWindow(player);
		}
	}
	
	public final boolean notifySkillUse(final L2NpcInstance npc, final L2PcInstance caster, final L2Skill skill)
	{
		String res = null;
		try
		{
			res = onSkillUse(npc, caster, skill);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(caster, e);
		}
		
		return showResult(caster, res);
	}
	
	public final boolean notifyItemUse(final L2NpcInstance npc, final L2PcInstance user, final L2ItemInstance item)
	{
		String res = null;
		try
		{
			res = onItemUse(npc, user, item);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(user, e);
		}
		
		return showResult(user, res);
	}
	
	public final boolean notifySpellFinished(final L2NpcInstance npc, final L2PcInstance player, final L2Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(npc, player, skill);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	public final boolean notifyFactionCall(L2NpcInstance npc, L2NpcInstance caller, L2PcInstance attacker, final boolean isPet)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isPet);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(attacker, e);
		}
		return showResult(attacker, res);
	}
	
	public final boolean notifyMovement(final L2NpcInstance npc, final L2Character target, final boolean isPet)
	{
		String res = null;
		
		try
		{
			res = onMovement(npc, target, isPet);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(target, e);
		}
		return showResult(target, res);
	}
	
	public final boolean notifyAggroRangeEnter(final L2NpcInstance npc, final L2PcInstance player, final boolean isPet)
	{
		String res = null;
		
		try
		{
			res = onAggroRangeEnter(npc, player, isPet);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	public final boolean notifySpawn(final L2NpcInstance npc)
	{
		String res = null;
		
		try
		{
			res = onSpawn(npc);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			return showError(npc, e);
		}
		return showResult(npc, res);
	}
	
	// these are methods that java calls to invoke scripts
	public String onAttack(final L2NpcInstance npc, final L2PcInstance attacker, final int damage, final boolean isPet)
	{
		return null;
	}
	
	public String onDeath(final L2Character killer, final L2Character victim, final QuestState qs)
	{
		if (killer instanceof L2NpcInstance)
		{
			return onAdvEvent("", (L2NpcInstance) killer, qs.getPlayer());
		}
		return onAdvEvent("", null, qs.getPlayer());
	}
	
	public String onAdvEvent(String event, L2NpcInstance npc, L2PcInstance player)
	{
		if (player == null)
		{
			return null;
		}
		
		// if not overriden by a subclass, then default to the returned value of the simpler (and older) onEvent override
		// if the player has a state, use it as parameter in the next call, else return null
		final QuestState qs = player.getQuestState(getName());
		
		if (qs != null)
		{
			return onEvent(event, qs);
		}
		
		return null;
	}
	
	public void sendDlgMessage(final String text, final L2PcInstance player)
	{
		player.dialog = this;
		final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1.getId());
		dlg.addString(text);
		player.sendPacket(dlg);
	}
	
	public void onDlgAnswer(final L2PcInstance player)
	{
	}
	
	public String onEvent(final String event, final QuestState qs)
	{
		return null;
	}
	
	public String onKill(final L2NpcInstance npc, final L2PcInstance killer, final boolean isPet)
	{
		return null;
	}
	
	public String onTalk(final L2NpcInstance npc, final L2PcInstance talker)
	{
		return null;
	}
	
	public String onFirstTalk(final L2NpcInstance npc, final L2PcInstance player)
	{
		return null;
	}
	
	public String onSkillUse(final L2NpcInstance npc, final L2PcInstance caster, final L2Skill skill)
	{
		return null;
	}
	
	public String onItemUse(L2NpcInstance npc, L2PcInstance player, L2ItemInstance item)
	{
		return null;
	}
	
	public String onSpellFinished(final L2NpcInstance npc, final L2PcInstance player, final L2Skill skill)
	{
		return null;
	}
	
	public String onSkillSee(L2NpcInstance npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (caster == null)
		{
			return null;
		}
		
		if (!(npc instanceof L2Attackable))
		{
			return null;
		}
		
		L2Attackable attackable = (L2Attackable) npc;
		int skillAggroPoints = skill.getAggroPoints();
		
		if (caster.getPet() != null)
		{
			if ((targets.length == 1) && Util.contains(targets, caster.getPet()))
			{
				skillAggroPoints = 0;
			}
		}
		
		if (skillAggroPoints > 0)
		{
			if (attackable.hasAI() && (attackable.getAI().getIntention() == AI_INTENTION_ATTACK))
			{
				L2Object npcTarget = attackable.getTarget();
				for (L2Object skillTarget : targets)
				{
					if ((npcTarget == skillTarget) || (npc == skillTarget))
					{
						L2Character originalCaster = isPet ? caster.getPet() : caster;
						attackable.addDamageHate(originalCaster, 0, (skillAggroPoints * 150) / (attackable.getLevel() + 7));
					}
				}
			}
		}
		return null;
	}
	
	public String onFactionCall(final L2NpcInstance npc, final L2NpcInstance caller, final L2PcInstance attacker, final boolean isPet)
	{
		if (attacker == null)
		{
			return null;
		}
		
		L2Character originalAttackTarget = (isPet ? attacker.getPet() : attacker);
		
		if (attacker.isInParty() && attacker.getParty().isInDimensionalRift())
		{
			byte riftType = attacker.getParty().getDimensionalRift().getType();
			byte riftRoom = attacker.getParty().getDimensionalRift().getCurrentRoom();
			
			if ((caller instanceof L2RiftInvaderInstance) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
			{
				return null;
			}
		}
		
		npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, originalAttackTarget, 1);
		
		return null;
	}
	
	public String onMovement(L2NpcInstance npc, L2Character player, boolean isPet)
	{
		if (player == null)
		{
			return null;
		}
		
		if (npc.getAggroRange() > 0 && player.isMoving())
		{
			Announcements _a = Announcements.getInstance();
			_a.sys(player.getName() + " is moving NPC:" + npc.getName());
		}
		
		return null;
	}
	
	public String onAggroRangeEnter(L2NpcInstance npc, L2PcInstance player, boolean isPet)
	{
		if (player == null)
		{
			return null;
		}
		
		((L2MonsterInstance) npc).addDamageHate(isPet ? player.getPet() : player, 0, 1);
		
		return null;
	}
	
	public String onSpawn(final L2NpcInstance npc)
	{
		return null;
	}
	
	public boolean showError(L2Character object, Throwable e)
	{
		if (getScriptFile() != null)
		{
			LOG.warn(getScriptFile().getAbsolutePath(), e);
		}
		
		if (object == null)
		{
			return false;
		}
		
		if (object instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) object;
			if (player.getAccessLevel().isGm())
			{
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				pw.close();
				
				String res = "<html><title>Script error</title><body>" + e.getMessage() + "</body></html>";
				
				return showResult(player, res);
			}
		}
		else
		{
			final StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			e.printStackTrace(pw);
			pw.close();
			
			String res = "Script error: " + e.getMessage();
			GmListTable.broadcastMessageToGMs(res);
			
			return showResult(object, res);
		}
		return false;
	}
	
	/**
	 * Show a message to player.<BR>
	 * <BR>
	 * <U><I>Concept : </I></U><BR>
	 * 3 cases are managed according to the value of the parameter "res" :<BR>
	 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
	 * <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI>
	 * <LI><U>otherwise :</U> the message hold in "res" is shown in chat box</LI>
	 * @param object
	 * @param res : String pointing out the message to show at the player
	 * @return boolean
	 */
	private boolean showResult(L2Character object, String res)
	{
		if (res == null)
		{
			return true;
		}
		
		if (object instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) object;
			if (res.endsWith(".htm"))
			{
				showHtmlFile(player, res);
			}
			else if (res.startsWith("<html>"))
			{
				NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
				npcReply.setHtml(res);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
				sm.addString(res);
				player.sendPacket(sm);
			}
		}
		
		return false;
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds
	 * @return L2NpcTemplate : Start NPC
	 */
	public L2NpcTemplate[] addStartNpc(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, Quest.QuestEventType.QUEST_START);
		}
		
		return value;
	}
	
	public L2NpcTemplate addStartNpc(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.QUEST_START);
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog)
	 * @param npcIds
	 * @return L2NpcTemplate : Start NPC
	 */
	public L2NpcTemplate[] addFirstTalkId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, Quest.QuestEventType.NPC_FIRST_TALK);
		}
		return value;
	}
	
	public L2NpcTemplate addFirstTalkId(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.NPC_FIRST_TALK);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for Attack Events.<BR>
	 * <BR>
	 * @param attackId
	 * @return int : attackId
	 */
	public L2NpcTemplate addAttackId(final int attackId)
	{
		return addEventId(attackId, Quest.QuestEventType.ON_ATTACK);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for Kill Events.<BR>
	 * <BR>
	 * @param killId
	 * @return int : killId
	 */
	public L2NpcTemplate addKillId(final int killId)
	{
		return addEventId(killId, Quest.QuestEventType.ON_KILL);
	}
	
	public void addKillId(int... killIds)
	{
		for (int killId : killIds)
		{
			addEventId(killId, Quest.QuestEventType.ON_KILL);
		}
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.<BR>
	 * <BR>
	 * @param talkIds : ID of the NPC
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate[] addTalkId(int... talkIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[talkIds.length];
		int i = 0;
		for (int talkId : talkIds)
		{
			value[i++] = addEventId(talkId, Quest.QuestEventType.QUEST_TALK);
		}
		return value;
	}
	
	public L2NpcTemplate addTalkId(int talkId)
	{
		return addEventId(talkId, Quest.QuestEventType.QUEST_TALK);
	}
	
	public L2NpcTemplate addFactionCallId(final int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_FACTION_CALL);
	}
	
	public L2NpcTemplate addSkillUseId(final int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_SKILL_USE);
	}
	
	public L2NpcTemplate addSpellFinishedId(final int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_SPELL_FINISHED);
	}
	
	public L2NpcTemplate addAggroRangeEnterId(final int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_AGGRO_RANGE_ENTER);
	}
	
	public L2NpcTemplate addSpawnId(final int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_SPAWN);
	}
	
	public L2NpcTemplate addItemUseId(final int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_ITEM_USE);
	}
	
	public L2NpcTemplate addEventId(final int npcId, final QuestEventType eventType)
	{
		try
		{
			final L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
			
			if (t != null)
			{
				t.addQuestEvent(eventType, this);
			}
			
			return t;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Add quests to the L2PCInstance of the player.<BR>
	 * <BR>
	 * <U><I>Action : </U></I><BR>
	 * Add state of quests, drops and variables for quests in the HashMap _quest of L2PcInstance
	 * @param player : Player who is entering the world
	 */
	public final static void playerEnter(final L2PcInstance player)
	{
		if (Config.DEBUG)
		{
			LOG.info("Quest.playerEnter " + player.getName());
			
		}
		
		if (Config.ALT_DEV_NO_QUESTS)
		{
			return;
		}
		
		Connection con = null;
		try
		{
			// Get list of quests owned by the player from database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			final PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? and name=?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("delete FROM character_quests WHERE char_id=? and name=? and var=?");
			
			statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE char_id=? AND var=?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
			{
				
				// Get ID of the quest and ID of its state
				final String questId = rs.getString("name");
				final String stateId = rs.getString("value");
				
				// Search quest associated with the ID
				final Quest q = QuestManager.getInstance().getQuest(questId);
				
				if (q == null)
				{
					if (Config.DEVELOPER)
					{
						LOG.info("Unknown quest " + questId + " for player " + player.getName());
					}
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestData.setInt(1, player.getObjectId());
						invalidQuestData.setString(2, questId);
						invalidQuestData.executeUpdate();
					}
					continue;
				}
				
				// Identify the state of the quest for the player
				boolean completed = false;
				
				if (stateId.equals("Completed"))
				{
					completed = true;
				}
				
				// Create an object State containing the state of the quest
				final State state = q._states.get(stateId);
				if (state == null)
				{
					if (Config.DEVELOPER)
					{
						LOG.info("Unknown state in quest " + questId + " for player " + player.getName());
					}
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestData.setInt(1, player.getObjectId());
						invalidQuestData.setString(2, questId);
						invalidQuestData.executeUpdate();
					}
					continue;
				}
				
				// Create a new QuestState for the player that will be added to the player's list of quests
				final QuestState qs = new QuestState(q, player, state, completed);
				player.setQuestState(qs);
				
			}
			
			rs.close();
			invalidQuestData.close();
			DatabaseUtils.close(statement);
			statement = null;
			rs = null;
			
			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE char_id=?");
			statement.setInt(1, player.getObjectId());
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				String questId = rs.getString("name");
				String var = rs.getString("var");
				String value = rs.getString("value");
				
				// Get the QuestState saved in the loop before
				QuestState qs = player.getQuestState(questId);
				
				if (qs == null)
				{
					if (Config.DEVELOPER)
					{
						LOG.info("Lost variable " + var + " in quest " + questId + " for player " + player.getName());
					}
					
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestDataVar.setInt(1, player.getObjectId());
						invalidQuestDataVar.setString(2, questId);
						invalidQuestDataVar.setString(3, var);
						invalidQuestDataVar.executeUpdate();
					}
					continue;
				}
				// Add parameter to the quest
				qs.setInternal(var, value);
				
				questId = null;
				var = null;
				value = null;
				qs = null;
			}
			
			rs.close();
			invalidQuestDataVar.close();
			DatabaseUtils.close(statement);
			statement = null;
			rs = null;
			invalidQuestDataVar = null;
			
		}
		
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not insert char quest:", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
		
		// events
		for (final String name : _allEventsS.keySet())
		{
			player.processQuestEvent(name, "enter");
		}
	}
	
	/**
	 * Insert (or Update) in the database variables that need to stay persistant for this quest after a reboot. This function is for storage of values that do not related to a specific player but are global for all characters. For example, if we need to disable a quest-gatekeeper until a certain
	 * time (as is done with some grand-boss gatekeepers), we can save that time in the DB.
	 * @param var : String designating the name of the variable for the quest
	 * @param value : String designating the value of the variable for the quest
	 */
	public final void saveGlobalQuestVar(final String var, final String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not insert global quest variable:", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Read from the database a previously saved variable for this quest. Due to performance considerations, this function should best be used only when the quest is first loaded. Subclasses of this class can define structures into which these loaded values can be saved. However, on-demand usage of
	 * this function throughout the script is not prohibited, only not recommended. Values read from this function were entered by calls to "saveGlobalQuestVar"
	 * @param var : String designating the name of the variable for the quest
	 * @return String : String representing the loaded value for the passed var, or an empty string if the var was invalid
	 */
	public final String loadGlobalQuestVar(final String var)
	{
		String result = "";
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			ResultSet rs = statement.executeQuery();
			
			if (rs.first())
			{
				result = rs.getString(1);
			}
			
			rs.close();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not load global quest variable:", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return result;
	}
	
	/**
	 * Permanently delete from the database a global quest variable that was previously saved for this quest.
	 * @param var : String designating the name of the variable for the quest
	 */
	public final void deleteGlobalQuestVar(final String var)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.executeUpdate();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not delete global quest variable:", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Permanently delete from the database all global quest variables that was previously saved for this quest.
	 */
	public final void deleteAllGlobalQuestVars()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ?");
			statement.setString(1, getName());
			statement.executeUpdate();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not delete global quest variables:", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Insert in the database the quest for the player.
	 * @param qs : QuestState pointing out the state of the quest
	 * @param var : String designating the name of the variable for the quest
	 * @param value : String designating the value of the variable for the quest
	 */
	public static void createQuestVarInDb(final QuestState qs, final String var, final String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("INSERT INTO character_quests (char_id,name,var,value) VALUES (?,?,?,?)");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.executeUpdate();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final SQLIntegrityConstraintViolationException e)
		{
			updateQuestVarInDb(qs, var, value);
			
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not insert char quest:", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Update the value of the variable "var" for the quest.<BR>
	 * <BR>
	 * <U><I>Actions :</I></U><BR>
	 * The selection of the right record is made with :
	 * <LI>char_id = qs.getPlayer().getObjectID()</LI>
	 * <LI>name = qs.getQuest().getName()</LI>
	 * <LI>var = var</LI> <BR>
	 * <BR>
	 * The modification made is :
	 * <LI>value = parameter value</LI>
	 * @param qs : Quest State
	 * @param var : String designating the name of the variable for quest
	 * @param value : String designating the value of the variable for quest
	 */
	public static void updateQuestVarInDb(final QuestState qs, final String var, final String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE char_id=? AND name=? AND var = ?");
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuestName());
			statement.setString(4, var);
			statement.executeUpdate();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not update char quest:", e);
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	/**
	 * Delete a variable of player's quest from the database.
	 * @param qs : object QuestState pointing out the player's quest
	 * @param var : String designating the variable characterizing the quest
	 */
	public static void deleteQuestVarInDb(final QuestState qs, final String var)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=? AND var=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.executeUpdate();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not delete char quest:", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	/**
	 * Delete the player's quest from database.
	 * @param qs : QuestState pointing out the player's quest
	 */
	public static void deleteQuestInDb(final QuestState qs)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.executeUpdate();
			DatabaseUtils.close(statement);
			statement = null;
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.warn("could not delete char quest:", e);
		}
		finally
		{
			CloseUtil.close(con);
			con = null;
		}
	}
	
	public static void createQuestInDb(final QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", qs.getStateId());
	}
	
	public static void updateQuestInDb(final QuestState qs)
	{
		String val = qs.getStateId();
		updateQuestVarInDb(qs, "<state>", val);
	}
	
	public L2PcInstance getRandomPartyMember(final L2PcInstance player)
	{
		// NPE prevention. If the player is null, there is nothing to return
		if (player == null)
		{
			return null;
		}
		
		if (player.getParty() == null || player.getParty().getPartyMembers().size() == 0)
		{
			return player;
		}
		
		final L2Party party = player.getParty();
		
		return party.getPartyMembers().get(Rnd.get(party.getPartyMembers().size()));
	}
	
	public L2PcInstance getRandomPartyMember(final L2PcInstance player, final String value)
	{
		return getRandomPartyMember(player, "cond", value);
	}
	
	public L2PcInstance getRandomPartyMember(final L2PcInstance player, final String var, final String value)
	{
		if (player == null)
		{
			return null;
		}
		
		// for null var condition, return any random party member.
		if (var == null)
		{
			return getRandomPartyMember(player);
		}
		
		// normal cases...if the player is not in a party, check the player's state
		QuestState temp = null;
		L2Party party = player.getParty();
		
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if (party == null || party.getPartyMembers().size() == 0)
		{
			temp = player.getQuestState(getName());
			if (temp != null && temp.get(var) != null && ((String) temp.get(var)).equalsIgnoreCase(value))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly
		// including this player)
		final FastList<L2PcInstance> candidates = new FastList<>();
		
		// get the target for enforcing distance limitations.
		L2Object target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (final L2PcInstance partyMember : party.getPartyMembers())
		{
			temp = partyMember.getQuestState(getName());
			if (temp != null && temp.get(var) != null && ((String) temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}
		
		// if there was no match, return null...
		if (candidates.size() == 0)
		{
			return null;
		}
		
		temp = null;
		target = null;
		party = null;
		
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	public L2PcInstance getRandomPartyMemberState(final L2PcInstance player, final State state)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// for null var condition, return any random party member.
		if (state == null)
		{
			return getRandomPartyMember(player);
		}
		
		// normal cases...if the player is not in a partym check the player's state
		QuestState temp = null;
		L2Party party = player.getParty();
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if (party == null || party.getPartyMembers().size() == 0)
		{
			temp = player.getQuestState(getName());
			if (temp != null && temp.getState() == state)
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly
		// including this player)
		final FastList<L2PcInstance> candidates = new FastList<>();
		
		// get the target for enforcing distance limitations.
		L2Object target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (final L2PcInstance partyMember : party.getPartyMembers())
		{
			temp = partyMember.getQuestState(getName());
			
			if (temp != null && temp.getState() == state && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}
		
		// if there was no match, return null...
		if (candidates.size() == 0)
		{
			return null;
		}
		
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	public String showHtmlFile(L2PcInstance player, String fileName)
	{
		String questId = getName();
		
		String directory = getDescr().toLowerCase();
		String content = HtmCache.getInstance().getHtm("data/scripts/" + directory + "/" + questId + "/" + fileName);
		
		if (content == null)
		{
			content = HtmCache.getInstance().getHtmForce("data/scripts/quests/" + questId + "/" + fileName);
		}
		
		if (player != null)
		{
			if (player.getTarget() != null)
			{
				content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));
			}
			
			if (content != null)
			{
				NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
		}
		return content;
	}
	
	public L2NpcInstance addSpawn(int npcId, Location loc, boolean randomOffset, int despawnDelay)
	{
		return QuestSpawn.getInstance().addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay);
	}
	
	public L2NpcInstance addSpawn(final int npcId, final L2Character cha)
	{
		return QuestSpawn.getInstance().addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0);
	}
	
	public L2NpcInstance addSpawn(final int npcId, final int x, final int y, final int z, final int heading, final boolean randomOffset, final int despawnDelay)
	{
		return QuestSpawn.getInstance().addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay);
	}
	
	public void registerItem(final int itemId)
	{
		if (_questItemIds == null)
		{
			_questItemIds = new FastList<>();
		}
		
		_questItemIds.add(itemId);
	}
	
	public FastList<Integer> getRegisteredItemIds()
	{
		return _questItemIds;
	}
	
	@Override
	public ScriptManager<?> getScriptManager()
	{
		return QuestManager.getInstance();
	}
	
	@Override
	public boolean unload()
	{
		saveGlobalData();
		// cancel all pending timers before reloading.
		// if timers ought to be restarted, the quest can take care of it
		// with its code (example: save global data indicating what timer must
		// be restarted).
		
		synchronized (_allEventTimers)
		{
			
			for (final ArrayList<QuestTimer> timers : _allEventTimers.values())
			{
				for (final QuestTimer timer : timers)
				{
					timer.cancel(false);
				}
			}
			_allEventTimers.clear();
			
		}
		
		return QuestManager.getInstance().removeQuest(this);
	}
	
	@Override
	public boolean reload()
	{
		unload();
		return super.reload();
	}
	
	@Override
	public String getScriptName()
	{
		return getName();
	}
	
	public void registerMobs(final int[] mobs)
	{
		for (final int id : mobs)
		{
			addEventId(id, QuestEventType.ON_ATTACK);
			addEventId(id, QuestEventType.ON_KILL);
			addEventId(id, QuestEventType.ON_SPAWN);
			addEventId(id, QuestEventType.ON_SPELL_FINISHED);
			addEventId(id, QuestEventType.ON_FACTION_CALL);
			addEventId(id, QuestEventType.ON_SKILL_SEE);
			addEventId(id, QuestEventType.ON_SKILL_USE);
		}
	}
	
	public static void LoadInit()
	{
		Quest ai = new Quest(-1, "Quest", "Quest");
		
		for (int level = 1; level < 100; level++)
		{
			final L2NpcTemplate[] templates = NpcTable.getInstance().getAllOfLevel(level);
			for (L2NpcTemplate t : templates)
			{
				try
				{
					if (L2Attackable.class.isAssignableFrom(Class.forName("l2jorion.game.model.actor.instance." + t.getType() + "Instance")))
					{
						ai.addEventId(t.getNpcId(), QuestEventType.ON_ATTACK);
						ai.addEventId(t.getNpcId(), QuestEventType.ON_KILL);
						ai.addEventId(t.getNpcId(), QuestEventType.ON_SPAWN);
						ai.addEventId(t.getNpcId(), QuestEventType.ON_SPELL_FINISHED);
						ai.addEventId(t.getNpcId(), QuestEventType.ON_SKILL_SEE);
						ai.addEventId(t.getNpcId(), QuestEventType.ON_SKILL_USE);
						ai.addEventId(t.getNpcId(), QuestEventType.ON_FACTION_CALL);
					}
				}
				catch (ClassNotFoundException ex)
				{
					LOG.info("Class not found: " + t.getType() + "Instance");
				}
			}
		}
	}
	
	public void registerMobs(final int[] mobs, final QuestEventType... types)
	{
		for (final int id : mobs)
		{
			for (final QuestEventType type : types)
			{
				addEventId(id, type);
			}
		}
	}
	
	public final Siege addSiegeNotify(int castleId)
	{
		final Siege siege = CastleManager.getInstance().getCastleById(castleId).getSiege();
		siege.addQuestEvent(this);
		return siege;
	}
	
	public static String getNoQuestMsg()
	{
		return HTML_NONE_AVAILABLE;
	}
	
	public void onSiegeEvent()
	{
	}
}
