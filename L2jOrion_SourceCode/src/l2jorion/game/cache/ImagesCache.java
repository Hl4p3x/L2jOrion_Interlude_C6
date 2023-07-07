package l2jorion.game.cache;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

import l2jorion.Config;
import l2jorion.game.idfactory.IdFactory;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.serverpackets.PledgeCrest;
import l2jorion.game.taskmanager.AutoImageSenderManager;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.DDSConverter;
import l2jorion.util.Util;

public class ImagesCache
{
	protected static Logger LOG = LoggerFactory.getLogger(ImagesCache.class);
	
	private static final int[] SIZES =
	{
		1,
		2,
		4,
		8,
		16,
		32,
		64,
		128,
		256,
		512,
		1024
	};
	private static final int MAX_SIZE = SIZES[(SIZES.length - 1)];
	
	private static final String CREST_IMAGE_KEY_WORD = "Crest.crest_";
	public static final Pattern HTML_PATTERN = Pattern.compile("%image:(.*?)%", 32); // 32
	
	private final Map<Integer, byte[]> _images = new HashMap<>();
	private final Map<String, Integer> _imagesId = new HashMap<>();
	
	public ImagesCache()
	{
		loadImages();
	}
	
	private void loadImages()
	{
		Map<Integer, File> imagesToLoad = getImagesToLoad();
		
		for (Map.Entry<Integer, File> image : imagesToLoad.entrySet())
		{
			File file = image.getValue();
			byte[] data = DDSConverter.convertToDDS(file).array();
			_images.put(image.getKey(), data);
			_imagesId.put(file.getName().toLowerCase(), image.getKey());
		}
		
		LOG.info("ImagesCache: Loaded " + imagesToLoad.size() + " Images");
	}
	
	private static Map<Integer, File> getImagesToLoad()
	{
		Map<Integer, File> files = new HashMap<>();
		
		File folder = new File(Config.DATAPACK_ROOT + "/data/images");
		
		if (!folder.exists())
		{
			LOG.error("Path \"./data/images\" doesn't exist!", new FileNotFoundException());
			return files;
		}
		
		for (File file : folder.listFiles())
		{
			for (File newFile : (file.isDirectory() ? file.listFiles() : new File[]
			{
				file
			}))
			{
				if (newFile.getName().endsWith(".png"))
				{
					
					newFile = resizeImage(newFile);
					
					int id = -1;
					try
					{
						String name = FilenameUtils.getBaseName(newFile.getName());
						id = Integer.parseInt(name);
					}
					catch (Exception e)
					{
						id = IdFactory.getInstance().getNextId();
					}
					
					if (id != -1)
					{
						files.put(id, newFile);
					}
				}
			}
		}
		
		return files;
	}
	
	private static File resizeImage(File file)
	{
		BufferedImage image;
		try
		{
			image = ImageIO.read(file);
		}
		catch (IOException e)
		{
			LOG.error("ImagesChache: Error while resizing " + file.getName() + " image.", e);
			return null;
		}
		
		if (image == null)
		{
			return null;
		}
		
		int width = image.getWidth();
		int height = image.getHeight();
		
		boolean resizeWidth = true;
		if (width > MAX_SIZE)
		{
			image = image.getSubimage(0, 0, MAX_SIZE, height);
			resizeWidth = false;
		}
		
		boolean resizeHeight = true;
		if (height > MAX_SIZE)
		{
			image = image.getSubimage(0, 0, width, MAX_SIZE);
			resizeHeight = false;
		}
		
		int resizedWidth = width;
		if (resizeWidth)
		{
			for (int size : SIZES)
			{
				if (size >= width)
				{
					resizedWidth = size;
					break;
				}
			}
		}
		
		int resizedHeight = height;
		if (resizeHeight)
		{
			for (int size : SIZES)
			{
				if (size >= height)
				{
					resizedHeight = size;
					break;
				}
			}
		}
		
		if ((resizedWidth != width) || (resizedHeight != height))
		{
			for (int x = 0; x < resizedWidth; x++)
			{
				for (int y = 0; y < resizedHeight; y++)
				{
					try
					{
						image.setRGB(x, y, 24); // Color.BLACK.getRGB()
					}
					catch (ArrayIndexOutOfBoundsException e)
					{
						LOG.error("ImagesChache: Error while resizing " + file.getName() + ". Image out of bounds.");
						return null;
					}
				}
			}
			String filename = file.getName();
			String format = filename.substring(filename.lastIndexOf("."));
			
			try
			{
				ImageIO.write(image, format, file);
			}
			catch (IOException e)
			{
				LOG.error("ImagesChache: Error while resizing " + file.getName() + " image.", e);
				return null;
			}
		}
		
		return file;
	}
	
	public String sendUsedImages(String html, L2PcInstance player)
	{
		// if (!Config.ALLOW_SENDING_IMAGES)
		// {
		// return html;
		// }
		
		// We must also replace all the crests_1 on the html to fit the current player serverid, or he wont be able to see the images
		// html = html.replaceAll("Crest.crest_1_", "Crest.crest_" + +player.getClient().getServerId() + "_");
		
		// We must first replace all the images to crests format, things like %image:serverImage% to Crest.crest_1_32423
		Matcher m = HTML_PATTERN.matcher(html);
		while (m.find())
		{
			final String imageName = m.group(1);
			final int imageId = _imagesId.get(imageName);
			// html = html.replaceAll("%image:" + imageName + "%", "Crest.crest_" + player.getClient().getServerId() + "_" + imageId);
			html = html.replaceAll("%image:" + imageName + "%", "Crest.crest_1_" + imageId);
		}
		
		char[] charArray = html.toCharArray();
		int lastIndex = 0;
		boolean hasSentImages = false;
		
		// Then we look for crests in the html and send them
		while (lastIndex != -1)
		{
			lastIndex = html.indexOf(CREST_IMAGE_KEY_WORD, lastIndex);
			
			if (lastIndex != -1)
			{
				int start = lastIndex + CREST_IMAGE_KEY_WORD.length() + 2;
				int end = getFileNameEnd(charArray, start);
				lastIndex = end;
				int imageId = Integer.parseInt(html.substring(start, end));
				
				// Checking if images are sent automatically(then player needs to wait for sending Thread) or in real time
				if (!AutoImageSenderManager.isImageAutoSendable(imageId))
				{
					sendImageToPlayer(player, imageId);
					hasSentImages = true;
				}
			}
		}
		
		// Synerge - To differenciate sent crests we add a CREST in the beggining of the html
		if (hasSentImages)
		{
			html = "CREST" + html;
		}
		
		return html;
	}
	
	private static int getFileNameEnd(char[] charArray, int start)
	{
		int stop = start;
		for (; stop < charArray.length; stop++)
		{
			if (!Util.isInteger(charArray[stop]))
			{
				return stop;
			}
		}
		return stop;
	}
	
	public void sendImageToPlayer(L2PcInstance player, int imageId)
	{
		// if (!Config.ALLOW_SENDING_IMAGES)
		// {
		// return;
		// }
		
		if (player.wasImageLoaded(imageId))
		{
			return;
		}
		
		player.addLoadedImage(imageId);
		
		if (_images.containsKey(imageId))
		{
			player.sendPacket(new PledgeCrest(imageId, _images.get(imageId)));
		}
		// else
		// {
		// LOG.warn("Trying to send image that doesn't exist, id:" + imageId);
		// }
	}
	
	public static ImagesCache getInstance()
	{
		return ImagesCacheHolder.instance;
	}
	
	private static class ImagesCacheHolder
	{
		protected static final ImagesCache instance = new ImagesCache();
	}
}
