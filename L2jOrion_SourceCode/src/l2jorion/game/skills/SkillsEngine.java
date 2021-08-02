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
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.model.Item;
import l2jorion.game.model.L2Skill;
import l2jorion.game.templates.L2Armor;
import l2jorion.game.templates.L2EtcItem;
import l2jorion.game.templates.L2EtcItemType;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2Weapon;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class SkillsEngine
{
	protected static final Logger LOG = LoggerFactory.getLogger(SkillsEngine.class);
	
	private static final SkillsEngine _instance = new SkillsEngine();
	
	private final List<File> _armorFiles = new FastList<>();
	private final List<File> _weaponFiles = new FastList<>();
	private final List<File> _etcitemFiles = new FastList<>();
	private final List<File> _skillFiles = new FastList<>();
	
	public static SkillsEngine getInstance()
	{
		return _instance;
	}
	
	private SkillsEngine()
	{
		hashFiles("data/xml/armor", _armorFiles);
		hashFiles("data/xml/weapon", _weaponFiles);
		hashFiles("data/xml/skills", _skillFiles);
	}
	
	private void hashFiles(final String dirname, final List<File> hash)
	{
		final File dir = new File(Config.DATAPACK_ROOT, dirname);
		if (!dir.exists())
		{
			LOG.info("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		final File[] files = dir.listFiles();
		for (final File f : files)
		{
			if (f.getName().endsWith(".xml"))
			{
				if (!f.getName().startsWith("custom"))
				{
					hash.add(f);
				}
			}
		}
		
		final File customfile = new File(Config.DATAPACK_ROOT, dirname + "/custom.xml");
		if (customfile.exists())
		{
			hash.add(customfile);
		}
	}
	
	public List<L2Skill> loadSkills(final File file)
	{
		if (file == null)
		{
			LOG.warn("Skill file not found.");
			return null;
		}
		
		final DocumentSkill doc = new DocumentSkill(file);
		doc.parse();
		return doc.getSkills();
	}
	
	public void loadAllSkills(final Map<Integer, L2Skill> allSkills)
	{
		int count = 0;
		for (final File file : _skillFiles)
		{
			final List<L2Skill> s = loadSkills(file);
			if (s == null)
			{
				continue;
			}
			
			for (final L2Skill skill : s)
			{
				allSkills.put(SkillTable.getSkillHashCode(skill), skill);
				count++;
			}
		}
		LOG.info("SkillsEngine: Loaded " + count + " skill templates from XML files");
	}
	
	public List<L2Armor> loadArmors(final Map<Integer, Item> armorData)
	{
		final List<L2Armor> list = new FastList<>();
		for (final L2Item item : loadData(armorData, _armorFiles))
		{
			list.add((L2Armor) item);
		}
		return list;
	}
	
	public List<L2Weapon> loadWeapons(final Map<Integer, Item> weaponData)
	{
		final List<L2Weapon> list = new FastList<>();
		for (final L2Item item : loadData(weaponData, _weaponFiles))
		{
			list.add((L2Weapon) item);
		}
		return list;
	}
	
	public List<L2EtcItem> loadItems(final Map<Integer, Item> itemData)
	{
		final List<L2EtcItem> list = new FastList<>();
		for (final L2Item item : loadData(itemData, _etcitemFiles))
		{
			list.add((L2EtcItem) item);
		}
		if (list.size() == 0)
		{
			for (final Item item : itemData.values())
			{
				list.add(new L2EtcItem((L2EtcItemType) item.type, item.set));
			}
		}
		return list;
	}
	
	public List<L2Item> loadData(final Map<Integer, Item> itemData, final List<File> files)
	{
		final List<L2Item> list = new FastList<>();
		for (final File f : files)
		{
			final DocumentItem document = new DocumentItem(itemData, f);
			document.parse();
			list.addAll(document.getItemList());
		}
		return list;
	}
}
