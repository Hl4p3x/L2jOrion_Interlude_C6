package l2jorion.game.ai.phantom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import l2jorion.Config;
import l2jorion.game.ai.CtrlIntention;
import l2jorion.game.controllers.GameTimeController;
import l2jorion.game.datatables.SkillTable;
import l2jorion.game.datatables.csv.MapRegionTable;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.xml.ExperienceData;
import l2jorion.game.managers.TownManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2Skill;
import l2jorion.game.model.L2World;
import l2jorion.game.model.Location;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2MerchantInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.actor.instance.L2TeleporterInstance;
import l2jorion.game.model.base.ClassId;
import l2jorion.game.model.base.ClassLevel;
import l2jorion.game.model.base.PlayerClass;
import l2jorion.game.network.serverpackets.ExAutoSoulShot;
import l2jorion.game.network.serverpackets.ExShowScreenMessage;
import l2jorion.game.network.serverpackets.InventoryUpdate;
import l2jorion.game.network.serverpackets.MagicSkillUser;
import l2jorion.game.network.serverpackets.MoveToPawn;
import l2jorion.game.powerpack.PowerPackConfig;
import l2jorion.game.templates.L2Item;
import l2jorion.game.templates.L2PcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.game.util.Broadcast;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.L2DatabaseFactory;
import l2jorion.util.random.Rnd;

public class phantomPlayers
{
	protected final Logger LOG = LoggerFactory.getLogger(phantomPlayers.class);
	
	public String _phantomAcc = Config.PHANTOM_PLAYERS_AKK;
	
	public static int _PhantomsLimit = 0;
	
	public static int _setsRobeCount = 0;
	public static int _setsLightCount = 0;
	public static int _setsHeavyCount = 0;
	
	// Sets
	public static int _setsCount = 0;
	private FastList<L2Set> _setsRobeN = new FastList<>();
	private FastList<L2Set> _setsRobeD = new FastList<>();
	private FastList<L2Set> _setsRobeC = new FastList<>();
	private FastList<L2Set> _setsRobeB = new FastList<>();
	private FastList<L2Set> _setsRobeA = new FastList<>();
	private FastList<L2Set> _setsRobeS = new FastList<>();
	
	private FastList<L2Set> _setsLightN = new FastList<>();
	private FastList<L2Set> _setsLightD = new FastList<>();
	private FastList<L2Set> _setsLightC = new FastList<>();
	private FastList<L2Set> _setsLightB = new FastList<>();
	private FastList<L2Set> _setsLightA = new FastList<>();
	private FastList<L2Set> _setsLightS = new FastList<>();
	
	private FastList<L2Set> _setsHeavyN = new FastList<>();
	private FastList<L2Set> _setsHeavyD = new FastList<>();
	private FastList<L2Set> _setsHeavyC = new FastList<>();
	private FastList<L2Set> _setsHeavyB = new FastList<>();
	private FastList<L2Set> _setsHeavyA = new FastList<>();
	private FastList<L2Set> _setsHeavyS = new FastList<>();
	
	private static phantomPlayers _instance;
	
	public static FastList<Location> _PhantomsRandomLoc = new FastList<>();
	
	public static FastMap<Integer, L2phantome> _phantoms = new FastMap<>();
	
	public static FastList<String> _PhantomsRandomPhrases = new FastList<>();
	public static int _PhantomsRandomPhrasesCount = 0;
	public static int _PhantomLastPhrase = Rnd.get(_PhantomsRandomPhrasesCount);
	
	private static FastSet<Integer> AlreadySpawned = new FastSet<>();
	
	public static int LoadedRandomLoc = 0;
	public static int LoadedPhantoms = 0;
	
	public static phantomPlayers getInstance()
	{
		return _instance;
	}
	
	public static void init()
	{
		_instance = new phantomPlayers();
		_instance.load();
	}
	
	private void load()
	{
		if (Config.ALLOW_PHANTOM_PLAYERS)
		{
			parseRandomLocs();
			loadSets();
			cachePhrases();
			cachephantoms();
			
			_PhantomsLimit = Config.PHANTOM_PLAYERS_COUNT_FIRST;
		}
	}
	
	public void cachephantoms()
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				String name = "";
				int obj_id = 0;
				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					con.setTransactionIsolation(1);
					PreparedStatement st = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name = ?");
					st.setString(1, _phantomAcc);
					ResultSet rs = st.executeQuery();
					rs.setFetchSize(250);
					while (rs.next())
					{
						obj_id = Integer.valueOf(rs.getInt("obj_Id"));
						name = rs.getString("char_name");
						_phantoms.put(obj_id, new L2phantome(obj_id, name));
					}
					st.close();
					rs.close();
					con.close();
					
					LOG.info("Phantom system: Cached " + _phantoms.size() + " players.");
				}
				catch (Exception e)
				{
					LOG.warn("Phantom system: could not load chars from DB: " + e);
				}
				finally
				{
					CloseUtil.close(con);
				}
				
				if (!_phantoms.isEmpty())
				{
					ThreadPoolManager.getInstance().scheduleAi(new phantomTask(), Config.PHANTOM_PLAYERS_DELAY_FIRST * 1000);
				}
				
			}
		}).start();
	}
	
	private void parseRandomLocs()
	{
		_PhantomsRandomLoc.clear();
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/random_locations.ini");
			if (!Data.exists())
			{
				return;
			}
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}
				String[] items = line.split(",");
				_PhantomsRandomLoc.add(new Location(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2])));
			}
			LoadedRandomLoc = _PhantomsRandomLoc.size() - 1;
			LOG.info("Loaded: " + LoadedRandomLoc + " random locations");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (fr != null)
				{
					fr.close();
				}
				if (br != null)
				{
					br.close();
				}
				if (lnr != null)
				{
					lnr.close();
				}
			}
			catch (Exception e1)
			{
			}
		}
	}
	
	private void cachePhrases()
	{
		_PhantomsRandomPhrases.clear();
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/chats.ini");
			if (!Data.exists())
			{
				return;
			}
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}
				_PhantomsRandomPhrases.add(line);
			}
			_PhantomsRandomPhrasesCount = _PhantomsRandomPhrases.size() - 1;
			LOG.info("Loaded: " + _PhantomsRandomPhrasesCount + " chat messages");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (fr != null)
				{
					fr.close();
				}
				if (br != null)
				{
					br.close();
				}
				if (lnr != null)
				{
					lnr.close();
				}
			}
			catch (Exception e1)
			{
			}
		}
	}
	
	private L2Set getRandomSet(String armorType, String gradeType)
	{
		if (armorType.contains("robe"))
		{
			if (gradeType.contains("N"))
			{
				return _setsRobeN.get(Rnd.get(_setsRobeN.size()));
			}
			else if (gradeType.contains("D"))
			{
				return _setsRobeD.get(Rnd.get(_setsRobeD.size()));
			}
			else if (gradeType.contains("C"))
			{
				return _setsRobeC.get(Rnd.get(_setsRobeB.size()));
			}
			else if (gradeType.contains("B"))
			{
				return _setsRobeB.get(Rnd.get(_setsRobeB.size()));
			}
			else if (gradeType.contains("A"))
			{
				return _setsRobeA.get(Rnd.get(_setsRobeA.size()));
			}
			else if (gradeType.contains("S"))
			{
				return _setsRobeS.get(Rnd.get(_setsRobeS.size()));
			}
		}
		else if (armorType.contains("light"))
		{
			if (gradeType.contains("N"))
			{
				return _setsLightN.get(Rnd.get(_setsLightN.size()));
			}
			else if (gradeType.contains("D"))
			{
				return _setsLightD.get(Rnd.get(_setsLightD.size()));
			}
			else if (gradeType.contains("C"))
			{
				return _setsLightC.get(Rnd.get(_setsLightC.size()));
			}
			else if (gradeType.contains("B"))
			{
				return _setsLightB.get(Rnd.get(_setsLightB.size()));
			}
			else if (gradeType.contains("A"))
			{
				return _setsLightA.get(Rnd.get(_setsLightA.size()));
			}
			else if (gradeType.contains("S"))
			{
				return _setsLightS.get(Rnd.get(_setsLightS.size()));
			}
		}
		else if (armorType.contains("heavy"))
		{
			if (gradeType.contains("N"))
			{
				return _setsHeavyN.get(Rnd.get(_setsHeavyN.size()));
			}
			else if (gradeType.contains("D"))
			{
				return _setsHeavyD.get(Rnd.get(_setsHeavyD.size()));
			}
			else if (gradeType.contains("C"))
			{
				return _setsHeavyC.get(Rnd.get(_setsHeavyC.size()));
			}
			else if (gradeType.contains("B"))
			{
				return _setsHeavyB.get(Rnd.get(_setsHeavyB.size()));
			}
			else if (gradeType.contains("A"))
			{
				return _setsHeavyA.get(Rnd.get(_setsHeavyA.size()));
			}
			else if (gradeType.contains("S"))
			{
				return _setsHeavyS.get(Rnd.get(_setsHeavyS.size()));
			}
		}
		
		return null;
	}
	
	public int getRandomPhantomNext()
	{
		int obj = 0;
		for (int i = 1; i > 0;)
		{
			obj = Rnd.get(600000001, 600004000);
			if (!AlreadySpawned.contains(obj))
			{
				return obj;
			}
		}
		return getRandomPhantomNext();
	}
	
	private Location getRandomLoc()
	{
		Location loc = _PhantomsRandomLoc.get(Rnd.get(0, LoadedRandomLoc));
		
		return loc;
	}
	
	static class L2Set
	{
		public int _body;
		public int _gaiters;
		public int _gloves;
		public int _boots;
		public int _weapon;
		
		L2Set(int bod, int gaiter, int glove, int boot, int weapon)
		{
			_body = bod;
			_gaiters = gaiter;
			_gloves = glove;
			_boots = boot;
			_weapon = weapon;
		}
	}
	
	public static class L2phantome
	{
		public int ObjId;
		public String name;
		
		L2phantome(int ObjId, String name)
		{
			this.ObjId = ObjId;
			this.name = name;
		}
	}
	
	// TODO phantomTask
	private class phantomTask implements Runnable
	{
		public phantomTask()
		{
		}
		
		@Override
		public void run()
		{
			int PhantomObjId = 0;
			
			LOG.info("Phantom system: spawning...");
			
			while (LoadedPhantoms < Config.PHANTOM_PLAYERS_COUNT_FIRST)
			{
				L2PcInstance phantom = null;
				
				PhantomObjId = getRandomPhantomNext();
				phantom = loadPhantom(PhantomObjId);
				
				if (phantom == null)
				{
					continue;
				}
				
				try
				{
					Thread.sleep(Config.PHANTOM_PLAYERS_DELAY_SPAWN_FIRST * 1000);
				}
				catch (InterruptedException e)
				{
				}
				
				LoadedPhantoms++;
			}
			
			LOG.info("Phantom system: spawned " + LoadedPhantoms + " phantoms.");
		}
	}
	
	// TODO loadPhantom
	public L2PcInstance loadPhantom(int objId)
	{
		int all_players = L2World.getInstance().getAllPlayersCount();
		if (!AlreadySpawned.contains(objId))
		{
			if (all_players < Config.MAXIMUM_ONLINE_USERS)
			{
				L2PcInstance phantom = L2PcInstance.restorePhantom(objId);
				AlreadySpawned.add(objId);
				
				if (phantom == null)
				{
					return null;
				}
				
				L2World.getInstance().addPlayerToWorld(phantom);
				
				phantom.setRunning();
				phantom.setOnlineStatus(true);
				phantom.setAutoLootEnabled(1);
				phantom.setAutoLootHerbs(0);
				phantom.setExpOn(1);
				phantom.setTeleport(1);
				
				if (Config.CHAR_TITLE)
				{
					phantom.setTitle(Config.ADD_CHAR_TITLE);
				}
				else
				{
					phantom.setTitle("");
				}
				
				// default colours
				phantom.getAppearance().setNameColor(Integer.decode(new StringBuilder().append("0x").append("FFFFFF").toString()).intValue());
				phantom.getAppearance().setTitleColor(Integer.decode(new StringBuilder().append("0x").append("FFFF77").toString()).intValue());
				
				L2PcTemplate template = phantom.getTemplate();
				L2ItemInstance rhand = phantom.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (rhand == null)
				{
					final L2Item[] items = template.getItems();
					for (final L2Item item2 : items)
					{
						final L2ItemInstance item = phantom.getInventory().addItem("Init", item2.getItemId(), 1, phantom, null);
						if (item.isEquipable())
						{
							if (phantom.getActiveWeaponItem() == null || !(item.getItem().getType2() != L2Item.TYPE2_WEAPON))
							{
								phantom.getInventory().equipItemAndRecord(item);
							}
							else
							{
								phantom.getInventory().equipItemAndRecord(item);
							}
						}
					}
					
					if (Config.CUSTOM_STARTER_ITEMS_ENABLED)
					{
						if (phantom.isMageClass())
						{
							for (final int[] reward : Config.STARTING_CUSTOM_ITEMS_M)
							{
								if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
								{
									phantom.getInventory().addItem("Starter Items Mage", reward[0], reward[1], phantom, null);
								}
								else
								{
									for (int i = 0; i < reward[1]; ++i)
									{
										phantom.getInventory().addItem("Starter Items Mage", reward[0], 1, phantom, null);
									}
								}
							}
						}
						else
						{
							for (final int[] reward : Config.STARTING_CUSTOM_ITEMS_F)
							{
								if (ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
								{
									phantom.getInventory().addItem("Starter Items Fighter", reward[0], reward[1], phantom, null);
								}
								else
								{
									for (int i = 0; i < reward[1]; ++i)
									{
										phantom.getInventory().addItem("Starter Items Fighter", reward[0], 1, phantom, null);
									}
								}
							}
						}
					}
				}
				
				if (Rnd.get(100) <= Config.CHANCE_FOR_NEUTRAL_PHANTOM && (!_PhantomsRandomLoc.isEmpty()))
				{
					Location loc = getRandomLoc();
					phantom.spawnMe(loc.getX() + Rnd.get(300), loc.getY() + Rnd.get(300), loc.getZ());
					phantom.setIsPhantomNeutral(true);
					
					if (Rnd.get(100) <= 50)
					{
						phantom.rndWalk();
					}
					else
					{
						if (Rnd.get(100) <= 50)
						{
							if ((TownManager.getInstance().getTown(phantom.getX(), phantom.getY(), phantom.getZ()) != null))
							{
								for (L2Object target : L2World.getInstance().getVisibleObjects(phantom, 500))
								{
									if (target != null && target instanceof L2TeleporterInstance && phantom.isInsideRadius(target, 500, false, true))
									{
										phantom.setTarget(target);
										phantom.getAI().moveToPawn(target, 140);
										if (phantom.isInsideRadius(target, 150, false, true))
										{
											MoveToPawn sp = new MoveToPawn(phantom, (L2TeleporterInstance) target, L2NpcInstance.INTERACTION_DISTANCE);
											phantom.sendPacket(sp);
											Broadcast.toKnownPlayers(phantom, sp);
										}
									}
								}
							}
						}
						else
						{
							if ((TownManager.getInstance().getTown(phantom.getX(), phantom.getY(), phantom.getZ()) != null))
							{
								for (L2Object target : L2World.getInstance().getVisibleObjects(phantom, 500))
								{
									if (target != null && target instanceof L2MerchantInstance && phantom.isInsideRadius(target, 500, false, true))
									{
										phantom.setTarget(target);
										phantom.getAI().moveToPawn(target, 140);
										if (phantom.isInsideRadius(target, 150, false, true))
										{
											MoveToPawn sp = new MoveToPawn(phantom, (L2MerchantInstance) target, L2NpcInstance.INTERACTION_DISTANCE);
											phantom.sendPacket(sp);
											Broadcast.toKnownPlayers(phantom, sp);
										}
									}
								}
							}
						}
					}
				}
				else
				{
					// load noob zones
					phantom.spawnMe(template.spawnX, template.spawnY, template.spawnZ);
				}
				
				if (!phantom.isPhantomNeutral())
				{
					if (Config.AUTOBUFFS_ON_CREATE)
					{
						ArrayList<L2Skill> skills_to_buff = new ArrayList<>();
						if (phantom.isMageClass())
						{
							for (int skillId : PowerPackConfig.MAGE_SKILL_LIST.keySet())
							{
								L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.MAGE_SKILL_LIST.get(skillId));
								if (skill != null)
								{
									skills_to_buff.add(skill);
								}
							}
						}
						else
						{
							for (int skillId : PowerPackConfig.FIGHTER_SKILL_LIST.keySet())
							{
								L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.FIGHTER_SKILL_LIST.get(skillId));
								if (skill != null)
								{
									skills_to_buff.add(skill);
								}
							}
						}
						for (L2Skill sk : skills_to_buff)
						{
							sk.getEffects(phantom, phantom, false, false, false);
						}
					}
					
					// Auto shots
					if (phantom.getClassId().isMage())
					{
						if (phantom.getInventory().getItemByItemId(3947) != null && phantom.getInventory().getItemByItemId(3947).getCount() >= 1)
						{
							phantom.addAutoSoulShot(3947);
							phantom.rechargeAutoSoulShot(true, true, false);
							phantom.sendPacket(new ExAutoSoulShot(3947, 1));
						}
					}
					else
					{
						if (phantom.getInventory().getItemByItemId(1835) != null && phantom.getInventory().getItemByItemId(1835).getCount() >= 1)
						{
							phantom.addAutoSoulShot(1835);
							phantom.rechargeAutoSoulShot(true, true, false);
							phantom.sendPacket(new ExAutoSoulShot(1835, 1));
						}
					}
				}
				
				if (!phantom.isPhantomNeutral())
				{
					// Random walk once on start up
					if (Rnd.get(100) <= 50)
					{
						phantom.rndWalk();
					}
					
					if (Rnd.get(100) <= 10)
					{
						GoToTown(phantom);
					}
					
					if (phantom.getPhantomAI() == null)
					{
						L2Skill[] skills = phantom.getAllSkills();
						for (L2Skill skill : skills)
						{
							phantom.addPhantomSkill(skill);
						}
						
						phantom.startPhantomAI();
					}
				}
				
				phantom.getInventory().reloadEquippedItems();
				phantom.broadcastUserInfo();
				return phantom;
			}
		}
		return null;
		// TODO Load Phantom END
	}
	
	public static String getRandomChatPhrase()
	{
		_PhantomLastPhrase = Rnd.get(_PhantomsRandomPhrasesCount);
		
		return _PhantomsRandomPhrases.get(_PhantomLastPhrase);
	}
	
	public void loadPhantomSystem(L2PcInstance gm, boolean reload, int count, int grade)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if (!_phantoms.isEmpty())
				{
					if (reload)
					{
						ThreadPoolManager.getInstance().scheduleAi(new phantomTaskForAdmin(gm), 0);
					}
					else
					{
						ThreadPoolManager.getInstance().scheduleAi(new phantomTaskForAdmin2(gm, count, grade), 0);
					}
				}
				
			}
		}).start();
	}
	
	private class phantomTaskForAdmin2 implements Runnable
	{
		L2PcInstance _gm;
		int _count;
		int _grade;
		
		public phantomTaskForAdmin2(L2PcInstance gm, int count, int grade)
		{
			_gm = gm;
			_count = count;
			_grade = grade;
		}
		
		@Override
		public void run()
		{
			int PhantomObjId = 0;
			int LoadedMorePhantoms = 0;
			
			_gm.sendMessage("Phantoms loading...");
			_gm.sendPacket(new ExShowScreenMessage("Phantoms loading...", 3000, 2, true));
			
			while (LoadedMorePhantoms < _count)
			{
				L2PcInstance phantom = null;
				
				PhantomObjId = getRandomPhantomNext();
				
				phantom = loadPhantomForAdmin(PhantomObjId, _gm, _grade);
				
				if (phantom == null)
				{
					continue;
				}
				
				LoadedMorePhantoms++;
				LoadedPhantoms++;
			}
			
			_gm.sendMessage("Phantoms loaded: +" + _count);
			_gm.sendPacket(new ExShowScreenMessage("Phantoms loaded: +" + _count, 3000, 2, true));
		}
	}
	
	private class phantomTaskForAdmin implements Runnable
	{
		L2PcInstance _gm;
		
		public phantomTaskForAdmin(L2PcInstance gm)
		{
			_gm = gm;
		}
		
		@Override
		public void run()
		{
			int PhantomObjId = 0;
			
			_gm.sendMessage("Phantoms loading...");
			_gm.sendPacket(new ExShowScreenMessage("Phantoms loading...", 3000, 2, true));
			
			while (LoadedPhantoms < Config.PHANTOM_PLAYERS_COUNT_FIRST)
			{
				L2PcInstance phantom = null;
				
				PhantomObjId = getRandomPhantomNext();
				
				phantom = loadPhantom(PhantomObjId);
				
				if (phantom == null)
				{
					continue;
				}
				
				LoadedPhantoms++;
			}
			
			_gm.sendMessage("Phantoms loaded: " + LoadedPhantoms);
			_gm.sendPacket(new ExShowScreenMessage("Phantoms loaded: " + LoadedPhantoms, 3000, 2, true));
			
		}
	}
	
	// TODO loadPhantomForAdmin
	public L2PcInstance loadPhantomForAdmin(int objId, L2PcInstance gm, int grade)
	{
		int all_players = L2World.getInstance().getAllPlayersCount();
		if (!AlreadySpawned.contains(objId))
		{
			if (all_players < Config.MAXIMUM_ONLINE_USERS)
			{
				L2PcInstance phantom = L2PcInstance.restorePhantom(objId);
				AlreadySpawned.add(objId);
				
				if (phantom == null)
				{
					return null;
				}
				
				L2World.getInstance().addPlayerToWorld(phantom);
				
				phantom.setRunning();
				phantom.setOnlineStatus(true);
				phantom.setAutoLootEnabled(1);
				phantom.setAutoLootHerbs(0);
				phantom.setExpOn(1);
				phantom.setTeleport(1);
				
				if (Config.CHAR_TITLE)
				{
					phantom.setTitle(Config.ADD_CHAR_TITLE);
				}
				else
				{
					phantom.setTitle("");
				}
				
				// Reset lvl
				String setLevel = "1";
				
				L2Set set = null;
				
				L2ItemInstance body = null;
				L2ItemInstance gaiters = null;
				L2ItemInstance gloves = null;
				L2ItemInstance boots = null;
				L2ItemInstance weapon = null;
				
				if (phantom.isMageClass())
				{
					set = getRandomSet("robe", "S");
				}
				else
				{
					set = getRandomSet("heavy", "S");
				}
				
				if (set._body != 0)
				{
					body = ItemTable.getInstance().createDummyItem(set._body);
				}
				if (set._gaiters != 0)
				{
					gaiters = ItemTable.getInstance().createDummyItem(set._gaiters);
				}
				if (set._gloves != 0)
				{
					gloves = ItemTable.getInstance().createDummyItem(set._gloves);
				}
				if (set._boots != 0)
				{
					boots = ItemTable.getInstance().createDummyItem(set._boots);
				}
				if (set._weapon != 0)
				{
					weapon = ItemTable.getInstance().createDummyItem(set._weapon);
				}
				
				if (body != null)
				{
					phantom.getInventory().addItem(body);
					phantom.getInventory().equipItem(body);
				}
				if (gaiters != null)
				{
					phantom.getInventory().addItem(gaiters);
					phantom.getInventory().equipItem(gaiters);
				}
				if (gloves != null)
				{
					phantom.getInventory().addItem(gloves);
					phantom.getInventory().equipItem(gloves);
				}
				if (boots != null)
				{
					phantom.getInventory().addItem(boots);
					phantom.getInventory().equipItem(boots);
				}
				if (weapon != null)
				{
					phantom.getInventory().addItem(weapon);
					phantom.getInventory().equipItem(weapon);
				}
				
				// Set lvl
				if (grade == 0)
				{
					setLevel = "" + Rnd.get(1, 19);
				}
				if (grade == 1)
				{
					setLevel = "" + Rnd.get(20, 39);
				}
				if (grade == 2)
				{
					setLevel = "" + Rnd.get(40, 51);
				}
				if (grade == 3)
				{
					setLevel = "" + Rnd.get(52, 60);
				}
				if (grade == 4)
				{
					setLevel = "" + Rnd.get(61, 75);
				}
				if (grade == 5)
				{
					setLevel = "" + Rnd.get(76, 80);
				}
				
				final byte lvl = Byte.parseByte(setLevel);
				final long pXp = phantom.getStat().getExp();
				final long tXp = ExperienceData.getInstance().getExpForLevel(lvl);
				if (pXp > tXp)
				{
					phantom.getStat().removeExpAndSp(pXp - tXp, 0);
				}
				else if (pXp < tXp)
				{
					phantom.getStat().addExpAndSp(tXp - pXp, 0);
				}
				
				// Change class
				ClassId classId = phantom.getClassId();
				int jobLevel = 0;
				int level = phantom.getLevel();
				ClassLevel phantomlvl = PlayerClass.values()[classId.getId()].getLevel();
				
				switch (phantomlvl)
				{
					case First:
						jobLevel = 1;
						break;
					case Second:
						jobLevel = 2;
						break;
					case Third:
						jobLevel = 3;
						break;
				}
				
				List<Integer> classes_list = new ArrayList<>();
				if (level >= 20 && jobLevel == 1)
				{
					classes_list.clear();
					for (ClassId child : ClassId.values())
					{
						if (child.childOf(classId) && child.level() == jobLevel)
						{
							classes_list.add(child.getId());
							
						}
					}
					changeClass(phantom, classes_list.get(Rnd.get(classes_list.size())));
					jobLevel = 2;
				}
				
				if (level >= 40 && jobLevel == 2)
				{
					classes_list.clear();
					for (ClassId child : ClassId.values())
					{
						if (child.childOf(classId) && child.level() == jobLevel)
						{
							classes_list.add(child.getId());
						}
					}
					changeClass(phantom, classes_list.get(Rnd.get(classes_list.size())));
					jobLevel = 3;
				}
				
				if (level >= 76 && jobLevel == 3)
				{
					classes_list.clear();
					for (ClassId child : ClassId.values())
					{
						if (child.childOf(classId) && child.level() == jobLevel)
						{
							classes_list.add(child.getId());
						}
					}
					
					int randomClass = Rnd.get(classes_list.size());
					changeClass(phantom, classes_list.get(randomClass));
				}
				
				// default colours
				phantom.getAppearance().setNameColor(Integer.decode(new StringBuilder().append("0x").append("FFFFFF").toString()).intValue());
				phantom.getAppearance().setTitleColor(Integer.decode(new StringBuilder().append("0x").append("FFFF77").toString()).intValue());
				
				// Buffs
				if (Config.AUTOBUFFS_ON_CREATE)
				{
					ArrayList<L2Skill> skills_to_buff = new ArrayList<>();
					if (phantom.isMageClass())
					{
						for (int skillId : PowerPackConfig.MAGE_SKILL_LIST.keySet())
						{
							L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.MAGE_SKILL_LIST.get(skillId));
							if (skill != null)
							{
								skills_to_buff.add(skill);
							}
						}
					}
					else
					{
						for (int skillId : PowerPackConfig.FIGHTER_SKILL_LIST.keySet())
						{
							L2Skill skill = SkillTable.getInstance().getInfo(skillId, PowerPackConfig.FIGHTER_SKILL_LIST.get(skillId));
							if (skill != null)
							{
								skills_to_buff.add(skill);
							}
						}
					}
					for (L2Skill sk : skills_to_buff)
					{
						sk.getEffects(phantom, phantom, false, false, false);
					}
				}
				L2ItemInstance item = null;
				
				// Auto shots
				if (phantom.getClassId().isMage())
				{
					item = phantom.getInventory().addItem("Admin", 3952, 5000, null, null);
					InventoryUpdate iu = new InventoryUpdate();
					iu.addItem(item);
					phantom.sendPacket(iu);
					
					if (phantom.getInventory().getItemByItemId(3952) != null && phantom.getInventory().getItemByItemId(3952).getCount() >= 1)
					{
						phantom.addAutoSoulShot(3952);
						phantom.rechargeAutoSoulShot(true, true, false);
						phantom.sendPacket(new ExAutoSoulShot(3952, 1));
					}
				}
				else
				{
					item = phantom.getInventory().addItem("Admin", 1467, 5000, null, null);
					InventoryUpdate iu = new InventoryUpdate();
					iu.addItem(item);
					phantom.sendPacket(iu);
					
					if (phantom.getInventory().getItemByItemId(1467) != null && phantom.getInventory().getItemByItemId(1467).getCount() >= 1)
					{
						phantom.addAutoSoulShot(1467);
						phantom.rechargeAutoSoulShot(true, true, false);
						phantom.sendPacket(new ExAutoSoulShot(1835, 1));
					}
				}
				
				phantom.spawnMe(gm.getX() + Rnd.get(300), gm.getY() + Rnd.get(300), gm.getZ());
				
				if (Rnd.get(100) <= 10)
				{
					phantom.rndWalk();
				}
				
				if (phantom.getPhantomAI() == null)
				{
					L2Skill[] skills = phantom.getAllSkills();
					for (L2Skill skill : skills)
					{
						phantom.addPhantomSkill(skill);
					}
					
					phantom.startPhantomAI();
				}
				
				phantom.getInventory().reloadEquippedItems();
				phantom.broadcastUserInfo();
				return phantom;
			}
		}
		return null;
	}
	
	private final void loadSets()
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			File file = new File(Config.DATAPACK_ROOT + "/config/phantom/sets.xml");
			if (!file.exists())
			{
				if (Config.DEBUG)
				{
					LOG.info("The sets.xml file is missing.");
				}
			}
			else
			{
				Document doc = factory.newDocumentBuilder().parse(file);
				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("armor".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								
								String armorType = attrs.getNamedItem("type").getNodeValue();
								
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("grade".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String gradeType = attrs.getNamedItem("type").getNodeValue();
										
										for (Node cdd = cd.getFirstChild(); cdd != null; cdd = cdd.getNextSibling())
										{
											if ("item".equalsIgnoreCase(cdd.getNodeName()))
											{
												attrs = cdd.getAttributes();
												
												int[] item;
												item = new int[5];
												item[0] = Integer.parseInt(attrs.getNamedItem("top").getNodeValue());
												item[1] = Integer.parseInt(attrs.getNamedItem("bottom").getNodeValue());
												item[2] = Integer.parseInt(attrs.getNamedItem("gloves").getNodeValue());
												item[3] = Integer.parseInt(attrs.getNamedItem("boots").getNodeValue());
												item[4] = Integer.parseInt(attrs.getNamedItem("weapon").getNodeValue());
												
												if (armorType.contains("robe"))
												{
													if (gradeType.contains("N"))
													{
														_setsRobeN.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("D"))
													{
														_setsRobeD.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("C"))
													{
														_setsRobeC.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("B"))
													{
														_setsRobeB.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("A"))
													{
														_setsRobeA.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("S"))
													{
														_setsRobeS.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
												}
												else if (armorType.contains("light"))
												{
													if (gradeType.contains("N"))
													{
														_setsLightN.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("D"))
													{
														_setsLightD.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("C"))
													{
														_setsLightC.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("B"))
													{
														_setsLightB.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("A"))
													{
														_setsLightA.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("S"))
													{
														_setsLightS.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
												}
												else if (armorType.contains("heavy"))
												{
													if (gradeType.contains("N"))
													{
														_setsHeavyN.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("D"))
													{
														_setsHeavyD.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("C"))
													{
														_setsHeavyC.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("B"))
													{
														_setsHeavyB.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("A"))
													{
														_setsHeavyA.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
													else if (gradeType.contains("S"))
													{
														_setsHeavyS.add(new L2Set(item[0], item[1], item[2], item[3], item[4]));
													}
												}
												_setsCount++;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				e.printStackTrace();
			}
			
			LOG.error("Error while loading sets.", e);
			
		}
		LOG.info("Loaded " + _setsCount + " sets.");
	}
	
	private void changeClass(L2PcInstance phantom, int val)
	{
		phantom.setTarget(phantom);
		phantom.setClassId(val);
		
		if (phantom.isSubClassActive())
		{
			phantom.getSubClasses().get(phantom.getClassIndex()).setClassId(phantom.getActiveClass());
		}
		else
		{
			ClassId classId = ClassId.getClassIdByOrdinal(phantom.getActiveClass());
			
			if (classId.getParent() != null)
			{
				while (classId.level() == 0)
				{
					classId = classId.getParent();
				}
			}
			
			phantom.setBaseClass(classId);
		}
		
		phantom.setTarget(null);
		phantom.broadcastUserInfo();
		phantom.broadcastClassIcon();
		phantom.rewardSkills();
	}
	
	public void GoToTown(L2PcInstance _phantom)
	{
		int unstuckTimer = Config.UNSTUCK_INTERVAL * 1000;
		
		_phantom.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		_phantom.disableAllSkills();
		
		final MagicSkillUser msu = new MagicSkillUser(_phantom, 1050, 1, unstuckTimer, 0);
		_phantom.broadcastPacket(msu);
		
		// End SoE Animation section
		_phantom.setTarget(null);
		
		EscapeFinalizer ef = new EscapeFinalizer(_phantom);
		// continue execution later
		_phantom.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));
		_phantom.setSkillCastEndTime(10 + GameTimeController.getInstance().getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
	}
	
	private class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _phantom;
		
		EscapeFinalizer(L2PcInstance phantom)
		{
			_phantom = phantom;
		}
		
		@Override
		public void run()
		{
			if (_phantom.isDead())
			{
				return;
			}
			
			_phantom.setIsIn7sDungeon(false);
			_phantom.enableAllSkills();
			_phantom.abortCast();
			_phantom.stopMove(null);
			
			try
			{
				_phantom.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			catch (Throwable e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		}
	}
}