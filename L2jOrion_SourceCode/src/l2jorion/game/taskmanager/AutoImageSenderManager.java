package l2jorion.game.taskmanager;

import l2jorion.game.cache.ImagesCache;
import l2jorion.game.model.L2World;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;

public class AutoImageSenderManager
{
	protected static final int[] IMAGES_SENT_ORDER =
	{
		10000,
		10001,
		10002
	};
	
	private static final long DELAY_BETWEEN_PICTURE = 1000L;
	
	private AutoImageSenderManager()
	{
	}
	
	/**
	 * Checking if <code>imageId</code> is sent automatically from this class, or it should be sent in real time If image is sent automatically and player didn't receive it yet, he needs to wait.
	 * @param imageId Id of requested Image
	 * @return should player wait for the Image Thread?
	 */
	public static boolean isImageAutoSendable(int imageId)
	{
		for (int spendableId : IMAGES_SENT_ORDER)
		{
			if (spendableId == imageId)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checking if All Required images to watch Community Board were sent to the Player
	 * @param player that could receive Images
	 * @return were those images sent already?
	 */
	public static boolean wereAllImagesSent(L2PcInstance player)
	{
		return /* !Config.ALLOW_SENDING_IMAGES || */ player.getLoadedImagesSize() >= IMAGES_SENT_ORDER.length;
	}
	
	/**
	 * Starting a Thread which sends Images to every player that didn't receive them yet
	 */
	public static void startSendingImages()
	{
		ThreadPoolManager.getInstance().scheduleAi(new ImageSendThread(), DELAY_BETWEEN_PICTURE);
	}
	
	protected static class ImageSendThread implements Runnable
	{
		@Override
		public void run()
		{
			// if (Config.ALLOW_SENDING_IMAGES && Config.COMMUNITYBOARD_ENABLED)
			{
				final Iterable<L2PcInstance> players = L2World.getInstance().getAllPlayers().values();
				for (L2PcInstance player : players)
				{
					if (player != null && player.isOnline() == 1)// Check in case of No-Carrier System
					{
						int pictureToLoad = getNextPicture(player);
						
						if (pictureToLoad != -1)
						{
							ImagesCache.getInstance().sendImageToPlayer(player, pictureToLoad);
						}
					}
				}
			}
			
			ThreadPoolManager.getInstance().scheduleAi(new ImageSendThread(), DELAY_BETWEEN_PICTURE);
		}
		
		private static int getNextPicture(L2PcInstance player)
		{
			if (wereAllImagesSent(player))
			{
				return -1;
			}
			
			for (int imageId : IMAGES_SENT_ORDER)
			{
				if (!player.wasImageLoaded(imageId))
				{
					return imageId;
				}
			}
			
			// player.addQuickVar("AllImagesLoaded", true);
			return -1;
		}
	}
}
