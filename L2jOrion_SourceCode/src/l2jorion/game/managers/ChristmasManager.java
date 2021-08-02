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
package l2jorion.game.managers;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.datatables.sql.ItemTable;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.sql.SpawnTable;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.L2Attackable;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2ItemInstance;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.entity.Announcements;
import l2jorion.game.model.spawn.L2Spawn;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.CreatureSay;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.thread.ThreadPoolManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

/**
 * control for sequence of Christmas.
 * @version 1.00
 * @author Darki699
 */
public class ChristmasManager
{
	private static final Logger LOG = LoggerFactory.getLogger(ChristmasManager.class);
	
	protected List<L2NpcInstance> objectQueue = new FastList<>();
	protected Random rand = new Random();
	
	// X-Mas message list
	protected String[] message =
	{
		"Ho Ho Ho... Merry Christmas!",
		"God is Love...",
		"Christmas is all about love...",
		"Christmas is thus about God and Love...",
		"Love is the key to peace among all Lineage creature kind..",
		"Love is the key to peace and happiness within all creation...",
		"Love needs to be practiced - Love needs to flow - Love needs to make happy...",
		"Love starts with your partner, children and family and expands to all world.",
		"God bless all kind.",
		"God bless Lineage.",
		"Forgive all.",
		"Ask for forgiveness even from all the \"past away\" ones.",
		"Give love in many different ways to your family members, relatives, neighbors and \"foreigners\".",
		"Enhance the feeling within yourself of being a member of a far larger family than your physical family",
		"MOST important - Christmas is a feast of BLISS given to YOU from God and all beloved ones back home in God !!",
		"Open yourself for all divine bliss, forgiveness and divine help that is offered TO YOU by many others AND GOD.",
		"Take it easy. Relax these coming days.",
		"Every day is Christmas day - it is UP TO YOU to create the proper inner attitude and opening for love toward others AND from others within YOUR SELF !",
		"Peace and Silence. Reduced activities. More time for your most direct families. If possible NO other dates or travel may help you most to actually RECEIVE all offered bliss.",
		"What ever is offered to you from God either enters YOUR heart and soul or is LOST for GOOD !!! or at least until another such day - next year Christmas or so !!",
		"Divine bliss and love NEVER can be stored and received later.",
		"There is year round a huge quantity of love and bliss available from God and your Guru and other loving souls, but Christmas days are an extended period FOR ALL PLANET",
		"Please open your heart and accept all love and bliss - For your benefit as well as for the benefit of all your beloved ones.",
		"Beloved children of God",
		"Beyond Christmas days and beyond Christmas season - The Christmas love lives on, the Christmas bliss goes on, the Christmas feeling expands.",
		"The holy spirit of Christmas is the holy spirit of God and God's love for all days.",
		"When the Christmas spirit lives on and on...",
		"When the power of love created during the pre-Christmas days is kept alive and growing.",
		"Peace among all mankind is growing as well =)",
		"The holy gift of love is an eternal gift of love put into your heart like a seed.",
		"Dozens of millions of humans worldwide are changing in their hearts during weeks of pre-Christmas time and find their peak power of love on Christmas nights and Christmas days.",
		"What is special during these days, to give all of you this very special power of love, the power to forgive, the power to make others happy, power to join the loved one on his or her path of loving life.",
		"It only is your now decision that makes the difference !",
		"It only is your now focus in life that makes all the changes. It is your shift from purely worldly matters toward the power of love from God that dwells within all of us that gave you the power to change your own behavior from your normal year long behavior.",
		"The decision of love, peace and happiness is the right one.",
		"Whatever you focus on is filling your mind and subsequently filling your heart.",
		"No one else but you have change your focus these past Christmas days and the days of love you may have experienced in earlier Christmas seasons.",
		"God's love is always present.",
		"God's Love has always been in same power and purity and quantity available to all of you.",
		"Expand the spirit of Christmas love and Christmas joy to span all year of your life...",
		"Do all year long what is creating this special Christmas feeling of love joy and happiness.",
		"Expand the true Christmas feeling, expand the love you have ever given at your maximum power of love days ... ",
		"Expand the power of love over more and more days.",
		"Re-focus on what has brought your love to its peak power and refocus on those objects and actions in your focus of mind and actions.",
		"Remember the people and surrounding you had when feeling most happy, most loved, most magic",
		"People of true loving spirit - who all was present, recall their names, recall the one who may have had the greatest impact in love those hours of magic moments of love...",
		"The decoration of your surrounding - Decoration may help to focus on love - Or lack of decoration may make you drift away into darkness or business - away from love...",
		"Love songs, songs full of living joy - any of the thousands of true touching love songs and happy songs do contribute to the establishment of an inner attitude perceptible of love.",
		"Songs can fine tune and open our heart for love from God and our loved ones.",
		"Your power of will and focus of mind can keep Christmas Love and Christmas joy alive beyond Christmas season for eternity",
		"Enjoy your love for ever!",
		"Christmas can be every day - As soon as you truly love every day =)",
		"Christmas is when you love all and are loved by all.",
		"Christmas is when you are truly happy by creating true happiness in others with love from the bottom of your heart.",
		"Secret in God's creation is that no single person can truly love without ignition of his love.",
		"You need another person to love and to receive love, a person to truly fall in love to ignite your own divine fire of love. ",
		"God created many and all are made of love and all are made to love...",
		"The miracle of love only works if you want to become a fully loving member of the family of divine love.",
		"Once you have started to fall in love with the one God created for you - your entire eternal life will be a permanent fire of miracles of love ... Eternally !",
		"May all have a happy time on Christmas each year. Merry Christmas!",
		"Christmas day is a time for love. It is a time for showing our affection to our loved ones. It is all about love.",
		"Have a wonderful Christmas. May god bless our family. I love you all.",
		"Wish all living creatures a Happy X-mas and a Happy New Year! By the way I would like us to share a warm fellowship in all places.",
		"Just as animals need peace of mind, poeple and also trees need peace of mind. This is why I say, all creatures are waiting upon the Lord for their salvation. May God bless you all creatures in the whole world.",
		"Merry Xmas!",
		"May the grace of Our Mighty Father be with you all during this eve of Christmas. Have a blessed Christmas and a happy New Year.",
		"Merry Christmas my children. May this new year give all of the things you rightly deserve. And may peace finally be yours.",
		"I wish everybody a Merry Christmas! May the Holy Spirit be with you all the time.",
		"May you have the best of Christmas this year and all your dreams come true.",
		"May the miracle of Christmas fill your heart with warmth and love. Merry Christmas!"
	},
	
	sender =
	{
		"Santa Claus",
		"Papai Noel",
		"Shengdan Laoren",
		"Santa",
		"Viejo Pascuero",
		"Sinter Klaas",
		"Father Christmas",
		"Saint Nicholas",
		"Joulupukki",
		"Pere Noel",
		"Saint Nikolaus",
		"Kanakaloka",
		"De Kerstman",
		"Winter grandfather",
		"Babbo Natale",
		"Hoteiosho",
		"Kaledu Senelis",
		"Black Peter",
		"Kerstman",
		"Julenissen",
		"Swiety Mikolaj",
		"Ded Moroz",
		"Julenisse",
		"El Nino Jesus",
		"Jultomten",
		"Reindeer Dasher",
		"Reindeer Dancer",
		"Christmas Spirit",
		"Reindeer Prancer",
		"Reindeer Vixen",
		"Reindeer Comet",
		"Reindeer Cupid",
		"Reindeer Donner",
		"Reindeer Donder",
		"Reindeer Dunder",
		"Reindeer Blitzen",
		"Reindeer Bliksem",
		"Reindeer Blixem",
		"Reindeer Rudolf",
		"Christmas Elf"
	};
	
	// Presents List:
	protected int[] presents =
	{
		5560,
		5560,
		5560,
		5560,
		5560, /* x-mas tree */
		5560,
		5560,
		5560,
		5560,
		5560,
		5561,
		5561,
		5561,
		5561,
		5561, /* special x-mas tree */
		5562,
		5562,
		5562,
		5562, /* 1st Carol */
		5563,
		5563,
		5563,
		5563, /* 2nd Carol */
		5564,
		5564,
		5564,
		5564, /* 3rd Carol */
		5565,
		5565,
		5565,
		5565, /* 4th Carol */
		5566,
		5566,
		5566,
		5566, /* 5th Carol */
		5583,
		5583, /* 6th Carol */
		5584,
		5584, /* 7th Carol */
		5585,
		5585, /* 8th Carol */
		5586,
		5586, /* 9th Carol */
		5587,
		5587, /* 10th Carol */
		6403,
		6403,
		6403,
		6403, /* Star Shard */
		6403,
		6403,
		6403,
		6403,
		6406,
		6406,
		6406,
		6406, /* FireWorks */
		6407,
		6407, /* Large FireWorks */
		5555, /* Token of Love */
		7836, /* Santa Hat #1 */
		9138, /* Santa Hat #2 */
		8936, /* Santa's Antlers Hat */
		6394, /* Red Party Mask */
		5808
	/* Black Party Mask */
	};
	
	// The message task sent at fixed rate
	protected Future<?> _XMasMessageTask = null, _XMasPresentsTask = null;
	
	// Manager should only be Initialized once:
	protected int isManagerInit = 0;
	
	// Interval of Christmas actions:
	protected long _IntervalOfChristmas = 600000; // 10 minutes
	
	private final int first = 25000, last = 73099;
	
	/************************************** Initial Functions **************************************/
	
	/**
	 * Empty constructor Does nothing
	 */
	public ChristmasManager()
	{
		//
	}
	
	/**
	 * @return an instance of <b>this</b> InstanceManager.
	 */
	public static ChristmasManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * initialize <b>this</b> ChristmasManager
	 * @param activeChar
	 */
	public void init(final L2PcInstance activeChar)
	{
		
		if (isManagerInit > 0)
		{
			activeChar.sendMessage("Christmas Manager has already begun or is processing. Please be patient....");
			return;
		}
		
		activeChar.sendMessage("Started!!!! This will take a 2-3 hours (in order to reduce system lags to a minimum), please be patient... The process is working in the background and will spawn npcs, give presents and messages at a fixed rate.");
		
		// Tasks:
		
		spawnTrees();
		
		startFestiveMessagesAtFixedRate();
		isManagerInit++;
		
		givePresentsAtFixedRate();
		isManagerInit++;
		
		checkIfOkToAnnounce();
		
	}
	
	/**
	 * ends <b>this</b> ChristmasManager
	 * @param activeChar
	 */
	public void end(final L2PcInstance activeChar)
	{
		
		if (isManagerInit < 4)
		{
			if (activeChar != null)
			{
				activeChar.sendMessage("Christmas Manager is not activated yet. Already Ended or is now processing....");
			}
			
			return;
		}
		
		if (activeChar != null)
		{
			activeChar.sendMessage("Terminating! This may take a while, please be patient...");
		}
		
		// Tasks:
		
		ThreadPoolManager.getInstance().executeTask(new DeleteSpawns());
		
		endFestiveMessagesAtFixedRate();
		isManagerInit--;
		
		endPresentGivingAtFixedRate();
		isManagerInit--;
		
		checkIfOkToAnnounce();
		
	}
	
	/************************************ - Tree management - *************************************/
	
	/**
	 * Main function - spawns all trees.
	 */
	public void spawnTrees()
	{
		GetTreePos gtp = new GetTreePos(first);
		ThreadPoolManager.getInstance().executeTask(gtp);
		gtp = null;
	}
	
	/**
	 * returns a random X-Mas tree Npc Id.
	 * @return int tree Npc Id.
	 */
	protected int getTreeId()
	{
		final int[] ids =
		{
			13006,
			13007
		};
		return ids[rand.nextInt(ids.length)];
	}
	
	/**
	 * gets random world positions according to spawned world objects and spawns x-mas trees around them...
	 */
	public class GetTreePos implements Runnable
	{
		private int _iterator;
		private Future<?> _task;
		
		public GetTreePos(final int iter)
		{
			_iterator = iter;
		}
		
		public void setTask(final Future<?> task)
		{
			_task = task;
		}
		
		@Override
		public void run()
		{
			if (_task != null)
			{
				_task.cancel(true);
				_task = null;
			}
			try
			{
				L2Object obj = null;
				
				while (obj == null)
				{
					obj = SpawnTable.getInstance().getTemplate(_iterator).getLastSpawn();
					_iterator++;
					
					if (obj != null && obj instanceof L2Attackable)
						if (rand.nextInt(100) > 10)
						{
							obj = null;
						}
				}
				
				if (rand.nextInt(100) > 50)
				{
					spawnOneTree(getTreeId(), obj.getX() + rand.nextInt(200) - 100, obj.getY() + rand.nextInt(200) - 100, obj.getZ());
				}
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					t.printStackTrace();
			}
			
			if (_iterator >= last)
			{
				isManagerInit++;
				
				SpawnSantaNPCs ssNPCs = new SpawnSantaNPCs(first);
				
				_task = ThreadPoolManager.getInstance().scheduleGeneral(ssNPCs, 300);
				ssNPCs.setTask(_task);
				
				ssNPCs = null;
				
				return;
			}
			
			_iterator++;
			GetTreePos gtp = new GetTreePos(_iterator);
			
			_task = ThreadPoolManager.getInstance().scheduleGeneral(gtp, 300);
			gtp.setTask(_task);
			
			gtp = null;
		}
	}
	
	/**
	 * Delete all x-mas spawned trees from the world. Delete all x-mas trees spawns, and clears the L2NpcInstance tree queue.
	 */
	
	public class DeleteSpawns implements Runnable
	{
		@Override
		public void run()
		{
			if (objectQueue == null || objectQueue.isEmpty())
				return;
			
			for (final L2NpcInstance deleted : objectQueue)
			{
				if (deleted == null)
				{
					continue;
				}
				
				try
				{
					L2World.getInstance().removeObject(deleted);
					
					deleted.decayMe();
					deleted.deleteMe();
				}
				catch (final Throwable t)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
						t.printStackTrace();
					continue;
				}
			}
			
			objectQueue.clear();
			objectQueue = null;
			
			isManagerInit = isManagerInit - 2;
			checkIfOkToAnnounce();
		}
	}
	
	/**
	 * Spawns one x-mas tree at a given location x,y,z
	 * @param id - int tree npc id.
	 * @param x - int loc x
	 * @param y - int loc y
	 * @param z - int loc z
	 */
	protected void spawnOneTree(final int id, final int x, final int y, final int z)
	{
		try
		{
			L2NpcTemplate template1 = NpcTable.getInstance().getTemplate(id);
			L2Spawn spawn = new L2Spawn(template1);
			
			template1 = null;
			
			spawn.setId(IdFactory.getInstance().getNextId());
			
			spawn.setLocx(x);
			spawn.setLocy(y);
			spawn.setLocz(z);
			
			L2NpcInstance tree = spawn.spawnOne();
			L2World.getInstance().storeObject(tree);
			objectQueue.add(tree);
			
			spawn = null;
			tree = null;
		}
		catch (final Throwable t)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
				t.printStackTrace();
		}
	}
	
	/**************************** - send players festive messages - *****************************/
	
	/**
	 * Ends X-Mas messages sent to players, and terminates the thread.
	 */
	
	private void endFestiveMessagesAtFixedRate()
	{
		if (_XMasMessageTask != null)
		{
			_XMasMessageTask.cancel(true);
			_XMasMessageTask = null;
		}
	}
	
	/**
	 * Starts X-Mas messages sent to all players, and initialize the thread.
	 */
	
	private void startFestiveMessagesAtFixedRate()
	{
		SendXMasMessage XMasMessage = new SendXMasMessage();
		_XMasMessageTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(XMasMessage, 60000, _IntervalOfChristmas);
		XMasMessage = null;
	}
	
	/**
	 * Sends X-Mas messages to all world players.
	 * @author Darki699
	 */
	
	class SendXMasMessage implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				for (final L2PcInstance pc : L2World.getInstance().getAllPlayers().values())
				{
					if (pc == null)
					{
						continue;
					}
					else if (pc.isOnline() == 0)
					{
						continue;
					}
					
					pc.sendPacket(getXMasMessage());
				}
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					t.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns a random X-Mas message.
	 * @return CreatureSay message
	 */
	protected CreatureSay getXMasMessage()
	{
		final CreatureSay cs = new CreatureSay(0, 17, getRandomSender(), getRandomXMasMessage());
		return cs;
	}
	
	/**
	 * Returns a random name of the X-Mas message sender, sent to players
	 * @return String of the message sender's name
	 */
	
	private String getRandomSender()
	{
		return sender[rand.nextInt(sender.length)];
	}
	
	/**
	 * Returns a random X-Mas message String
	 * @return String containing the random message.
	 */
	
	private String getRandomXMasMessage()
	{
		return message[rand.nextInt(message.length)];
	}
	
	/******************************* - give special items trees - ********************************/
	// Trees , Carols , Tokens of love, Fireworks, Santa Hats.
	
	/**
	 * Starts X-Mas Santa presents sent to all players, and initialize the thread.
	 */
	
	private void givePresentsAtFixedRate()
	{
		final XMasPresentGivingTask XMasPresents = new XMasPresentGivingTask();
		_XMasPresentsTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(XMasPresents, _IntervalOfChristmas, _IntervalOfChristmas * 3);
	}
	
	class XMasPresentGivingTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				for (final L2PcInstance pc : L2World.getInstance().getAllPlayers().values())
				{
					if (pc == null)
					{
						continue;
					}
					else if (pc.isOnline() == 0)
					{
						continue;
					}
					else if (pc.getInventoryLimit() <= pc.getInventory().getSize())
					{
						pc.sendMessage("Santa wanted to give you a Present but your inventory was full :(");
						continue;
					}
					
					else if (rand.nextInt(100) < 50)
					{
						
						final int itemId = getSantaRandomPresent();
						
						L2ItemInstance item = ItemTable.getInstance().createItem("Christmas Event", itemId, 1, pc);
						pc.getInventory().addItem("Christmas Event", item.getItemId(), 1, pc, pc);
						String itemName = ItemTable.getInstance().getTemplate(itemId).getName();
						
						item = null;
						
						SystemMessage sm;
						sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addString(itemName + " from Santa's Present Bag...");
						pc.broadcastPacket(sm);
						
						itemName = null;
						sm = null;
					}
				}
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					t.printStackTrace();
			}
		}
	}
	
	protected int getSantaRandomPresent()
	{
		return presents[rand.nextInt(presents.length)];
	}
	
	/**
	 * Ends X-Mas present giving to players, and terminates the thread.
	 */
	private void endPresentGivingAtFixedRate()
	{
		if (_XMasPresentsTask != null)
		{
			_XMasPresentsTask.cancel(true);
			_XMasPresentsTask = null;
		}
	}
	
	/************************************ - spawn NPCs in towns - ***************************************/
	
	// NPC Ids: 31863 , 31864
	public class SpawnSantaNPCs implements Runnable
	{
		
		private int _iterator;
		
		private Future<?> _task;
		
		public SpawnSantaNPCs(final int iter)
		{
			_iterator = iter;
		}
		
		public void setTask(final Future<?> task)
		{
			_task = task;
		}
		
		@Override
		public void run()
		{
			if (_task != null)
			{
				_task.cancel(true);
				_task = null;
			}
			
			try
			{
				L2Object obj = null;
				while (obj == null)
				{
					obj = SpawnTable.getInstance().getTemplate(_iterator).getLastSpawn();
					_iterator++;
					if (obj != null && obj instanceof L2Attackable)
					{
						obj = null;
					}
				}
				
				if (rand.nextInt(100) < 80 && obj instanceof L2NpcInstance)
				{
					spawnOneTree(getSantaId(), obj.getX() + rand.nextInt(500) - 250, obj.getY() + rand.nextInt(500) - 250, obj.getZ());
				}
			}
			catch (final Throwable t)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
					t.printStackTrace();
			}
			
			if (_iterator >= last)
			{
				isManagerInit++;
				checkIfOkToAnnounce();
				
				return;
			}
			
			_iterator++;
			SpawnSantaNPCs ssNPCs = new SpawnSantaNPCs(_iterator);
			
			_task = ThreadPoolManager.getInstance().scheduleGeneral(ssNPCs, 300);
			ssNPCs.setTask(_task);
			
			ssNPCs = null;
		}
	}
	
	protected int getSantaId()
	{
		return rand.nextInt(100) < 50 ? 31863 : 31864;
	}
	
	protected void checkIfOkToAnnounce()
	{
		if (isManagerInit == 4)
		{
			Announcements.getInstance().announceToAll("Christmas Event has begun, have a Merry Christmas and a Happy New Year.");
			Announcements.getInstance().announceToAll("Christmas Event will end in 24 hours.");
			LOG.info("ChristmasManager:Init ChristmasManager was started successfully, have a festive holiday.");
			
			final EndEvent ee = new EndEvent();
			Future<?> task = ThreadPoolManager.getInstance().scheduleGeneral(ee, 86400000);
			ee.setTask(task);
			
			task = null;
			isManagerInit = 5;
		}
		
		if (isManagerInit == 0)
		{
			Announcements.getInstance().announceToAll("Christmas Event has ended... Hope you enjoyed the festivities.");
			LOG.info("ChristmasManager:Terminated ChristmasManager.");
			
			isManagerInit = -1;
		}
	}
	
	public class EndEvent implements Runnable
	{
		
		private Future<?> _task;
		
		public void setTask(final Future<?> task)
		{
			_task = task;
		}
		
		@Override
		public void run()
		{
			if (_task != null)
			{
				_task.cancel(true);
				_task = null;
			}
			
			end(null);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final ChristmasManager _instance = new ChristmasManager();
	}
}
