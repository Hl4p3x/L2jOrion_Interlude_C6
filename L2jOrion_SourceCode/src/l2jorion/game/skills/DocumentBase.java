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
package l2jorion.game.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2Skill.SkillType;
import l2jorion.game.model.base.Race;
import l2jorion.game.skills.conditions.Condition;
import l2jorion.game.skills.conditions.ConditionElementSeed;
import l2jorion.game.skills.conditions.ConditionForceBuff;
import l2jorion.game.skills.conditions.ConditionGameChance;
import l2jorion.game.skills.conditions.ConditionGameTime;
import l2jorion.game.skills.conditions.ConditionGameTime.CheckGameTime;
import l2jorion.game.skills.conditions.ConditionLogicAnd;
import l2jorion.game.skills.conditions.ConditionLogicNot;
import l2jorion.game.skills.conditions.ConditionLogicOr;
import l2jorion.game.skills.conditions.ConditionPlayerClassIdRestriction;
import l2jorion.game.skills.conditions.ConditionPlayerHp;
import l2jorion.game.skills.conditions.ConditionPlayerHpPercentage;
import l2jorion.game.skills.conditions.ConditionPlayerLevel;
import l2jorion.game.skills.conditions.ConditionPlayerMp;
import l2jorion.game.skills.conditions.ConditionPlayerRace;
import l2jorion.game.skills.conditions.ConditionPlayerState;
import l2jorion.game.skills.conditions.ConditionPlayerState.CheckPlayerState;
import l2jorion.game.skills.conditions.ConditionSkillStats;
import l2jorion.game.skills.conditions.ConditionSlotItemId;
import l2jorion.game.skills.conditions.ConditionTargetAggro;
import l2jorion.game.skills.conditions.ConditionTargetClassIdRestriction;
import l2jorion.game.skills.conditions.ConditionTargetLevel;
import l2jorion.game.skills.conditions.ConditionTargetRaceId;
import l2jorion.game.skills.conditions.ConditionTargetUsesWeaponKind;
import l2jorion.game.skills.conditions.ConditionUsingItemType;
import l2jorion.game.skills.conditions.ConditionUsingSkill;
import l2jorion.game.skills.conditions.ConditionWithSkill;
import l2jorion.game.skills.effects.EffectTemplate;
import l2jorion.game.skills.funcs.FuncTemplate;
import l2jorion.game.skills.funcs.Lambda;
import l2jorion.game.skills.funcs.LambdaCalc;
import l2jorion.game.skills.funcs.LambdaConst;
import l2jorion.game.skills.funcs.LambdaStats;
import l2jorion.game.templates.L2ArmorType;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.game.templates.L2WeaponType;
import l2jorion.game.templates.StatsSet;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

abstract class DocumentBase
{
	static Logger LOG = LoggerFactory.getLogger(DocumentBase.class);
	
	private final File _file;
	protected Map<String, String[]> _tables;
	
	DocumentBase(final File pFile)
	{
		_file = pFile;
		_tables = new FastMap<>();
	}
	
	Document parse()
	{
		Document doc;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(_file);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Error loading file " + _file, e);
			return null;
		}
		
		try
		{
			parseDocument(doc);
		}
		catch (final Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Error in file " + _file, e);
			return null;
		}
		return doc;
	}
	
	protected abstract void parseDocument(Document doc);
	
	protected abstract StatsSet getStatsSet();
	
	protected abstract String getTableValue(String name);
	
	protected abstract String getTableValue(String name, int idx);
	
	protected void resetTable()
	{
		_tables = new FastMap<>();
	}
	
	protected void setTable(final String name, final String[] table)
	{
		_tables.put(name, table);
	}
	
	protected void parseTemplate(Node n, final Object template)
	{
		Condition condition = null;
		n = n.getFirstChild();
		
		if (n == null)
		{
			return;
		}
		
		if ("cond".equalsIgnoreCase(n.getNodeName()))
		{
			condition = parseCondition(n.getFirstChild(), template);
			
			final Node msg = n.getAttributes().getNamedItem("msg");
			if (condition != null && msg != null)
			{
				condition.setMessage(msg.getNodeValue());
			}
			n = n.getNextSibling();
		}
		
		for (; n != null; n = n.getNextSibling())
		{
			if ("add".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Add", condition);
			}
			else if ("sub".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Sub", condition);
			}
			else if ("mul".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Mul", condition);
			}
			else if ("basemul".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "BaseMul", condition);
			}
			else if ("div".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Div", condition);
			}
			else if ("set".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Set", condition);
			}
			else if ("enchant".equalsIgnoreCase(n.getNodeName()))
			{
				attachFunc(n, template, "Enchant", condition);
			}
			else if ("skill".equalsIgnoreCase(n.getNodeName()))
			{
				attachSkill(n, template, condition);
			}
			else if ("effect".equalsIgnoreCase(n.getNodeName()))
			{
				if (template instanceof EffectTemplate)
				{
					throw new RuntimeException("Nested effects");
				}
				
				attachEffect(n, template, condition);
			}
		}
	}
	
	protected void attachFunc(final Node n, final Object template, final String name, final Condition attachCond)
	{
		final Stats stat = Stats.valueOfXml(n.getAttributes().getNamedItem("stat").getNodeValue());
		final String order = n.getAttributes().getNamedItem("order").getNodeValue();
		final Lambda lambda = getLambda(n, template);
		final int ord = Integer.decode(getValue(order, template));
		
		final Condition applayCond = parseCondition(n.getFirstChild(), template);
		
		final FuncTemplate ft = new FuncTemplate(attachCond, applayCond, name, stat, ord, lambda);
		
		if (template instanceof L2Item)
		{
			((L2Item) template).attach(ft);
		}
		else if (template instanceof L2Skill)
		{
			((L2Skill) template).attach(ft);
		}
		else if (template instanceof EffectTemplate)
		{
			((EffectTemplate) template).attach(ft);
		}
	}
	
	protected void attachLambdaFunc(final Node n, final Object template, final LambdaCalc calc)
	{
		String name = n.getNodeName();
		final TextBuilder sb = new TextBuilder(name);
		sb.setCharAt(0, Character.toUpperCase(name.charAt(0)));
		name = sb.toString();
		final Lambda lambda = getLambda(n, template);
		final FuncTemplate ft = new FuncTemplate(null, null, name, null, calc.funcs.length, lambda);
		calc.addFunc(ft.getFunc(new Env(), calc));
	}
	
	protected void attachEffect(final Node n, final Object template, final Condition attachCond)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final String name = attrs.getNamedItem("name").getNodeValue();
		int time, count = 1;
		int showIcon = 0;
		
		if (attrs.getNamedItem("noicon") != null)
		{
			showIcon = Integer.decode(getValue(attrs.getNamedItem("noicon").getNodeValue(), template));
		}
		if (attrs.getNamedItem("count") != null)
		{
			count = Integer.decode(getValue(attrs.getNamedItem("count").getNodeValue(), template));
		}
		
		if (attrs.getNamedItem("time") != null)
		{
			time = Integer.decode(getValue(attrs.getNamedItem("time").getNodeValue(), template));
			
			if (Config.ENABLE_MODIFY_SKILL_DURATION)
			{
				if (Config.SKILL_DURATION_LIST.containsKey(((L2Skill) template).getId()))
				{
					if (((L2Skill) template).getLevel() < 100)
					{
						time = Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
					}
					else if (((L2Skill) template).getLevel() >= 100 && ((L2Skill) template).getLevel() < 140)
					{
						time += Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
					}
					else if (((L2Skill) template).getLevel() > 140)
					{
						time = Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
					}
				}
			}
		}
		else
		{
			time = ((L2Skill) template).getBuffDuration() / 1000 / count;
		}
		
		boolean self = false;
		if (attrs.getNamedItem("self") != null)
		{
			if (Integer.decode(getValue(attrs.getNamedItem("self").getNodeValue(), template)) == 1)
			{
				self = true;
			}
		}
		
		final Lambda lambda = getLambda(n, template);
		final Condition applayCond = parseCondition(n.getFirstChild(), template);
		
		int abnormal = 0;
		
		if (attrs.getNamedItem("abnormal") != null)
		{
			final String abn = attrs.getNamedItem("abnormal").getNodeValue();
			
			if (abn.equals("poison"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_POISON;
			}
			else if (abn.equals("bleeding"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_BLEEDING;
			}
			else if (abn.equals("flame"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_FLAME;
			}
			else if (abn.equals("bighead"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_BIG_HEAD;
			}
			else if (abn.equals("stealth"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_STEALTH;
			}
			else if (abn.equals("float"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_FLOATING_ROOT;
			}
			else if (abn.equals("dancestun"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_DANCE_STUNNED;
			}
			else if (abn.equals("ghoststun"))
			{
				abnormal = L2Character.ABNORMAL_EFFECT_FIREROOT_STUN;
			}
		}
		
		float stackOrder = 0;
		
		String stackType = "none";
		if (attrs.getNamedItem("stackType") != null)
		{
			stackType = attrs.getNamedItem("stackType").getNodeValue();
		}
		
		if (attrs.getNamedItem("stackOrder") != null)
		{
			stackOrder = Float.parseFloat(getValue(attrs.getNamedItem("stackOrder").getNodeValue(), template));
		}
		
		double effectPower = -1;
		if (attrs.getNamedItem("effectPower") != null)
		{
			effectPower = Double.parseDouble(getValue(attrs.getNamedItem("effectPower").getNodeValue(), template));
		}
		
		SkillType type = null;
		if (attrs.getNamedItem("effectType") != null)
		{
			final String typeName = getValue(attrs.getNamedItem("effectType").getNodeValue(), template);
			
			try
			{
				type = Enum.valueOf(SkillType.class, typeName);
			}
			catch (final Exception e)
			{
				throw new IllegalArgumentException("Not skilltype found for: " + typeName);
			}
		}
		
		final EffectTemplate lt = new EffectTemplate(attachCond, applayCond, name, lambda, count, time, abnormal, stackType, stackOrder, showIcon, type, effectPower);
		parseTemplate(n, lt);
		
		if (template instanceof L2Item)
		{
			((L2Item) template).attach(lt);
		}
		else if (template instanceof L2Skill)
		{
			if (self)
			{
				((L2Skill) template).attachSelf(lt);
			}
			else
			{
				((L2Skill) template).attach(lt);
			}
		}
	}
	
	protected void attachSkill(final Node n, final Object template, final Condition attachCond)
	{
		final NamedNodeMap attrs = n.getAttributes();
		
		int id = 0, lvl = 1;
		
		if (attrs.getNamedItem("id") != null)
		{
			id = Integer.decode(getValue(attrs.getNamedItem("id").getNodeValue(), template));
		}
		
		if (attrs.getNamedItem("lvl") != null)
		{
			lvl = Integer.decode(getValue(attrs.getNamedItem("lvl").getNodeValue(), template));
		}
		
		final L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
		
		if (attrs.getNamedItem("chance") != null)
		{
			if (template instanceof L2Weapon || template instanceof L2Item)
			{
				skill.attach(new ConditionGameChance(Integer.decode(getValue(attrs.getNamedItem("chance").getNodeValue(), template))), true);
			}
			else
			{
				skill.attach(new ConditionGameChance(Integer.decode(getValue(attrs.getNamedItem("chance").getNodeValue(), template))), false);
			}
		}
		
		if (template instanceof L2Weapon)
		{
			if (attrs.getNamedItem("onUse") != null || attrs.getNamedItem("onCrit") == null && attrs.getNamedItem("onCast") == null)
			{
				((L2Weapon) template).attach(skill); // Attach as skill triggered on use
			}
			
			if (attrs.getNamedItem("onCrit") != null)
			{
				((L2Weapon) template).attachOnCrit(skill); // Attach as skill triggered on critical hit
			}
			
			if (attrs.getNamedItem("onCast") != null)
			{
				((L2Weapon) template).attachOnCast(skill); // Attach as skill triggered on cast
			}
		}
		else if (template instanceof L2Item)
		{
			((L2Item) template).attach(skill); // Attach as skill triggered on use
		}
	}
	
	protected Condition parseCondition(Node n, final Object template)
	{
		while (n != null && n.getNodeType() != Node.ELEMENT_NODE)
		{
			n = n.getNextSibling();
		}
		if (n == null)
		{
			return null;
		}
		
		if ("and".equalsIgnoreCase(n.getNodeName()))
		{
			return parseLogicAnd(n, template);
		}
		
		if ("or".equalsIgnoreCase(n.getNodeName()))
		{
			return parseLogicOr(n, template);
		}
		
		if ("not".equalsIgnoreCase(n.getNodeName()))
		{
			return parseLogicNot(n, template);
		}
		
		if ("player".equalsIgnoreCase(n.getNodeName()))
		{
			return parsePlayerCondition(n);
		}
		
		if ("target".equalsIgnoreCase(n.getNodeName()))
		{
			return parseTargetCondition(n, template);
		}
		
		if ("skill".equalsIgnoreCase(n.getNodeName()))
		{
			return parseSkillCondition(n);
		}
		
		if ("using".equalsIgnoreCase(n.getNodeName()))
		{
			return parseUsingCondition(n);
		}
		
		if ("game".equalsIgnoreCase(n.getNodeName()))
		{
			return parseGameCondition(n);
		}
		
		return null;
	}
	
	protected Condition parseLogicAnd(Node n, final Object template)
	{
		final ConditionLogicAnd cond = new ConditionLogicAnd();
		
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				cond.add(parseCondition(n, template));
			}
		}
		
		if (cond.conditions == null || cond.conditions.length == 0)
		{
			LOG.warn("Empty <and> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseLogicOr(Node n, final Object template)
	{
		final ConditionLogicOr cond = new ConditionLogicOr();
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				cond.add(parseCondition(n, template));
			}
		}
		
		if (cond.conditions == null || cond.conditions.length == 0)
		{
			LOG.warn("Empty <or> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseLogicNot(Node n, final Object template)
	{
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				return new ConditionLogicNot(parseCondition(n, template));
			}
		}
		
		LOG.warn("Empty <not> condition in " + _file);
		return null;
	}
	
	protected Condition parsePlayerCondition(final Node n)
	{
		Condition cond = null;
		final int[] ElementSeeds = new int[5];
		final int[] forces = new int[2];
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("race".equalsIgnoreCase(a.getNodeName()))
			{
				final Race race = Race.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerRace(race));
			}
			else if ("level".equalsIgnoreCase(a.getNodeName()))
			{
				final int lvl = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerLevel(lvl));
			}
			else if ("resting".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.RESTING, val));
			}
			else if ("flying".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.FLYING, val));
			}
			else if ("moving".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.MOVING, val));
			}
			else if ("running".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.RUNNING, val));
			}
			else if ("behind".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.BEHIND, val));
			}
			else if ("front".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.FRONT, val));
			}
			else if ("side".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.SIDE, val));
			}
			else if ("hp".equalsIgnoreCase(a.getNodeName()))
			{
				final int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHp(hp));
			}
			else if ("hprate".equalsIgnoreCase(a.getNodeName()))
			{
				final double rate = Double.parseDouble(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerHpPercentage(rate));
			}
			else if ("mp".equalsIgnoreCase(a.getNodeName()))
			{
				final int hp = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionPlayerMp(hp));
			}
			else if ("seed_fire".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[0] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("seed_water".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[1] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("seed_wind".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[2] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("seed_various".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[3] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("seed_any".equalsIgnoreCase(a.getNodeName()))
			{
				ElementSeeds[4] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("battle_force".equalsIgnoreCase(a.getNodeName()))
			{
				forces[0] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("spell_force".equalsIgnoreCase(a.getNodeName()))
			{
				forces[1] = Integer.decode(getValue(a.getNodeValue(), null));
			}
			else if ("class_id_restriction".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				final ArrayList<Integer> array = new ArrayList<>(st.countTokens());
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionPlayerClassIdRestriction(array));
			}
		}
		
		// Elemental seed condition processing
		for (final int elementSeed : ElementSeeds)
		{
			if (elementSeed > 0)
			{
				cond = joinAnd(cond, new ConditionElementSeed(ElementSeeds));
				break;
			}
		}
		
		if (forces[0] + forces[1] > 0)
		{
			cond = joinAnd(cond, new ConditionForceBuff(forces));
		}
		
		if (cond == null)
		{
			LOG.warn("Unrecognized <player> condition in " + _file);
		}
		return cond;
	}
	
	protected Condition parseTargetCondition(final Node n, final Object template)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("aggro".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionTargetAggro(val));
			}
			else if ("level".equalsIgnoreCase(a.getNodeName()))
			{
				final int lvl = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetLevel(lvl));
			}
			else if ("class_id_restriction".equalsIgnoreCase(a.getNodeName()))
			{
				final FastList<Integer> array = new FastList<>();
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetClassIdRestriction(array));
			}
			else if ("race_id".equalsIgnoreCase(a.getNodeName()))
			{
				final FastList<Integer> array = new FastList<>();
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetRaceId(array));
			}
			else if ("pvp".equalsIgnoreCase(a.getNodeName()))
			{
				final FastList<Integer> array = new FastList<>();
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					array.add(Integer.decode(getValue(item, null)));
				}
				cond = joinAnd(cond, new ConditionTargetRaceId(array));
			}
			else if ("using".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					for (final L2WeaponType wt : L2WeaponType.values())
					{
						if (wt.toString().equals(item))
						{
							mask |= wt.mask();
							break;
						}
					}
					
					for (final L2ArmorType at : L2ArmorType.values())
					{
						if (at.toString().equals(item))
						{
							mask |= at.mask();
							break;
						}
					}
				}
				cond = joinAnd(cond, new ConditionTargetUsesWeaponKind(mask));
			}
		}
		if (cond == null)
		{
			LOG.warn("Unrecognized <target> condition in " + _file);
		}
		return cond;
	}
	
	protected Condition parseSkillCondition(final Node n)
	{
		final NamedNodeMap attrs = n.getAttributes();
		
		final Stats stat = Stats.valueOfXml(attrs.getNamedItem("stat").getNodeValue());
		
		return new ConditionSkillStats(stat);
	}
	
	protected Condition parseUsingCondition(final Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("kind".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while (st.hasMoreTokens())
				{
					final String item = st.nextToken().trim();
					for (final L2WeaponType wt : L2WeaponType.values())
					{
						if (wt.toString().equals(item))
						{
							mask |= wt.mask();
							break;
						}
					}
					
					for (final L2ArmorType at : L2ArmorType.values())
					{
						if (at.toString().equals(item))
						{
							mask |= at.mask();
							break;
						}
					}
				}
				cond = joinAnd(cond, new ConditionUsingItemType(mask));
			}
			else if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				final int id = Integer.parseInt(a.getNodeValue());
				
				cond = joinAnd(cond, new ConditionUsingSkill(id));
			}
			else if ("slotitem".equalsIgnoreCase(a.getNodeName()))
			{
				final StringTokenizer st = new StringTokenizer(a.getNodeValue(), ";");
				
				final int id = Integer.parseInt(st.nextToken().trim());
				
				final int slot = Integer.parseInt(st.nextToken().trim());
				
				int enchant = 0;
				
				if (st.hasMoreTokens())
				{
					enchant = Integer.parseInt(st.nextToken().trim());
				}
				
				cond = joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
			}
		}
		
		if (cond == null)
		{
			LOG.warn("Unrecognized <using> condition in " + _file);
		}
		
		return cond;
	}
	
	protected Condition parseGameCondition(final Node n)
	{
		Condition cond = null;
		final NamedNodeMap attrs = n.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++)
		{
			final Node a = attrs.item(i);
			if ("skill".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionWithSkill(val));
			}
			
			if ("night".equalsIgnoreCase(a.getNodeName()))
			{
				final boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionGameTime(CheckGameTime.NIGHT, val));
			}
			
			if ("chance".equalsIgnoreCase(a.getNodeName()))
			{
				final int val = Integer.decode(getValue(a.getNodeValue(), null));
				cond = joinAnd(cond, new ConditionGameChance(val));
			}
		}
		
		if (cond == null)
		{
			LOG.warn("Unrecognized <game> condition in " + _file);
		}
		return cond;
	}
	
	protected void parseTable(final Node n)
	{
		final NamedNodeMap attrs = n.getAttributes();
		final String name = attrs.getNamedItem("name").getNodeValue();
		if (name.charAt(0) != '#')
		{
			throw new IllegalArgumentException("Table name must start with #");
		}
		
		final StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
		final List<String> array = new FastList<>();
		while (data.hasMoreTokens())
		{
			array.add(data.nextToken());
		}
		final String[] res = new String[array.size()];
		int i = 0;
		for (final String str : array)
		{
			res[i++] = str;
		}
		setTable(name, res);
	}
	
	protected void parseBeanSet(final Node n, final StatsSet set, final Integer level)
	{
		final String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		final String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		final char ch = value.length() == 0 ? ' ' : value.charAt(0);
		if (ch == '#' || ch == '-' || Character.isDigit(ch))
		{
			set.set(name, String.valueOf(getValue(value, level)));
		}
		else
		{
			set.set(name, value);
		}
	}
	
	protected Lambda getLambda(Node n, final Object template)
	{
		final Node nval = n.getAttributes().getNamedItem("val");
		if (nval != null)
		{
			final String val = nval.getNodeValue();
			if (val.charAt(0) == '#')
			{
				return new LambdaConst(Double.parseDouble(getTableValue(val)));
			}
			else if (val.charAt(0) == '$')
			{
				if (val.equalsIgnoreCase("$player_level"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_LEVEL);
				}
				
				if (val.equalsIgnoreCase("$target_level"))
				{
					return new LambdaStats(LambdaStats.StatsType.TARGET_LEVEL);
				}
				
				if (val.equalsIgnoreCase("$player_max_hp"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_HP);
				}
				
				if (val.equalsIgnoreCase("$player_max_mp"))
				{
					return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_MP);
				}
				
				// try to find value out of item fields
				final StatsSet set = getStatsSet();
				final String field = set.getString(val.substring(1));
				if (field != null)
				{
					return new LambdaConst(Double.parseDouble(getValue(field, template)));
				}
				// failed
				throw new IllegalArgumentException("Unknown value " + val);
			}
			else
			{
				return new LambdaConst(Double.parseDouble(val));
			}
		}
		final LambdaCalc calc = new LambdaCalc();
		n = n.getFirstChild();
		while (n != null && n.getNodeType() != Node.ELEMENT_NODE)
		{
			n = n.getNextSibling();
		}
		
		if (n == null || !"val".equals(n.getNodeName()))
		{
			throw new IllegalArgumentException("Value not specified");
		}
		
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			attachLambdaFunc(n, template, calc);
		}
		return calc;
	}
	
	protected String getValue(final String value, final Object template)
	{
		// is it a table?
		if (value.charAt(0) == '#')
		{
			if (template instanceof L2Skill)
			{
				return getTableValue(value);
			}
			else if (template instanceof Integer)
			{
				return getTableValue(value, ((Integer) template).intValue());
			}
			else
			{
				throw new IllegalStateException();
			}
		}
		return value;
	}
	
	protected Condition joinAnd(final Condition cond, final Condition c)
	{
		if (cond == null)
		{
			return c;
		}
		
		if (cond instanceof ConditionLogicAnd)
		{
			((ConditionLogicAnd) cond).add(c);
			return cond;
		}
		
		final ConditionLogicAnd and = new ConditionLogicAnd();
		and.add(cond);
		and.add(c);
		return and;
	}
}
