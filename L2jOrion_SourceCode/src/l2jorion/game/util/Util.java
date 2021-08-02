/*
 * $Header: Util.java, 21/10/2005 23:17:40 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 21/10/2005 23:17:40 $
 * $Revision: 1 $
 * $Log: Util.java,v $
 * Revision 1  21/10/2005 23:17:40  luisantonioa
 * Added copyright notice
 *
 *
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
package l2jorion.game.util;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import l2jorion.game.geo.GeoData;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;
import l2jorion.game.model.actor.instance.L2NpcInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.thread.ThreadPoolManager;

public final class Util
{
	
	public static void handleIllegalPlayerAction(final L2PcInstance actor, final String message, final int punishment)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new IllegalPlayerAction(actor, message, punishment), 5000);
	}
	
	public static String getRelativePath(final File base, final File file)
	{
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}
	
	public static double calculateAngleFrom(final L2Object obj1, final L2Object obj2)
	{
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	public final static double calculateAngleFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
		{
			angleTarget = 360 + angleTarget;
		}
		return angleTarget;
	}
	
	public final static double convertHeadingToDegree(int clientHeading)
	{
		double degree = clientHeading / 182.044444444;
		
		return degree;
	}
	
	public final static int convertDegreeToClientHeading(double degree)
	{
		if (degree < 0)
		{
			degree = 360 + degree;
		}
		return (int) (degree * 182.044444444);
	}
	
	public final static int calculateHeadingFrom(L2Object obj1, L2Object obj2)
	{
		return calculateHeadingFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	public final static int calculateHeadingFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		
		if (angleTarget < 0)
		{
			angleTarget = 360 + angleTarget;
		}
		
		return (int) (angleTarget * 182.044444444);
	}
	
	public final static int calculateHeadingFrom(double dx, double dy)
	{
		double angleTarget = Math.toDegrees(Math.atan2(dy, dx));
		if (angleTarget < 0)
		{
			angleTarget = 360 + angleTarget;
		}
		return (int) (angleTarget * 182.044444444);
	}
	
	public static double calculateDistance(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2, final boolean includeZAxis)
	{
		final double dx = (double) x1 - x2;
		final double dy = (double) y1 - y2;
		
		if (includeZAxis)
		{
			final double dz = z1 - z2;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public static double calculateDistance(final L2Object obj1, final L2Object obj2, final boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
		{
			return 1000000;
		}
		return calculateDistance(obj1.getPosition().getX(), obj1.getPosition().getY(), obj1.getPosition().getZ(), obj2.getPosition().getX(), obj2.getPosition().getY(), obj2.getPosition().getZ(), includeZAxis);
	}
	
	/**
	 * Capitalizes the first letter of a string, and returns the result.<BR>
	 * (Based on ucfirst() function of PHP)
	 * @param str
	 * @return String containing the modified string.
	 */
	public static String capitalizeFirst(String str)
	{
		str = str.trim();
		
		if (str.length() > 0 && Character.isLetter(str.charAt(0)))
		{
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		}
		
		return str;
	}
	
	/**
	 * Capitalizes the first letter of every "word" in a string.<BR>
	 * (Based on ucwords() function of PHP)
	 * @param str
	 * @return String containing the modified string.
	 */
	public static String capitalizeWords(final String str)
	{
		final char[] charArray = str.toCharArray();
		String result = "";
		
		// Capitalize the first letter in the given string!
		charArray[0] = Character.toUpperCase(charArray[0]);
		
		for (int i = 0; i < charArray.length; i++)
		{
			if (Character.isWhitespace(charArray[i]))
			{
				charArray[i + 1] = Character.toUpperCase(charArray[i + 1]);
			}
			
			result += Character.toString(charArray[i]);
		}
		
		return result;
	}
	
	public static boolean checkIfInRange(int range, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
		{
			return false;
		}
		
		if (range == -1)
		{
			return true; // not limited
		}
		
		int rad = 0;
		if (obj1 instanceof L2Character)
		{
			rad += ((L2Character) obj1).getTemplate().getCollisionRadius();
		}
		if (obj2 instanceof L2Character)
		{
			rad += ((L2Character) obj2).getTemplate().getCollisionRadius();
		}
		
		double dx = obj1.getX() - obj2.getX();
		double dy = obj1.getY() - obj2.getY();
		
		if (includeZAxis)
		{
			double dz = obj1.getZ() - obj2.getZ();
			double d = dx * dx + dy * dy + dz * dz;
			
			return d <= range * range + 2 * range * rad + rad * rad;
		}
		
		double d = dx * dx + dy * dy;
		return d <= range * range + 2 * range * rad + rad * rad;
	}
	
	/*
	 * Checks if object is within short (sqrt(int.max_value)) radius, not using collisionRadius. Faster calculation than checkIfInRange if distance is short and collisionRadius isn't needed. Not for long distance checks (potential teleports, far away castles etc)
	 */
	public static boolean checkIfInShortRadius(int radius, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
		{
			return false;
		}
		if (radius == -1)
		{
			return true; // not limited
		}
		
		int dx = obj1.getX() - obj2.getX();
		int dy = obj1.getY() - obj2.getY();
		
		if (includeZAxis)
		{
			int dz = obj1.getZ() - obj2.getZ();
			return dx * dx + dy * dy + dz * dz <= radius * radius;
		}
		return dx * dx + dy * dy <= radius * radius;
	}
	
	/**
	 * Returns the number of "words" in a given string.
	 * @param str
	 * @return int numWords
	 */
	public static int countWords(final String str)
	{
		return str.trim().split(" ").length;
	}
	
	/**
	 * Returns a delimited string for an given array of string elements.<BR>
	 * (Based on implode() in PHP)
	 * @param strArray
	 * @param strDelim
	 * @return String implodedString
	 */
	public static String implodeString(final String[] strArray, final String strDelim)
	{
		String result = "";
		
		for (final String strValue : strArray)
		{
			result += strValue + strDelim;
		}
		
		return result;
	}
	
	/**
	 * Returns a delimited string for an given collection of string elements.<BR>
	 * (Based on implode() in PHP)
	 * @param strCollection
	 * @param strDelim
	 * @return String implodedString
	 */
	public static String implodeString(final Collection<String> strCollection, final String strDelim)
	{
		return implodeString(strCollection.toArray(new String[strCollection.size()]), strDelim);
	}
	
	/**
	 * Returns the rounded value of val to specified number of digits after the decimal point.<BR>
	 * (Based on round() in PHP)
	 * @param val
	 * @param numPlaces
	 * @return float roundedVal
	 */
	public static float roundTo(final float val, final int numPlaces)
	{
		if (numPlaces <= 1)
		{
			return Math.round(val);
		}
		
		final float exponent = (float) Math.pow(10, numPlaces);
		
		return Math.round(val * exponent) / exponent;
	}
	
	public static boolean isAlphaNumeric(final String text)
	{
		boolean result = true;
		final char[] chars = text.toCharArray();
		for (final char aChar : chars)
		{
			if (!Character.isLetterOrDigit(aChar))
			{
				result = false;
				break;
			}
		}
		return result;
	}
	
	/**
	 * Return amount of adena formatted with "," delimiter
	 * @param amount
	 * @return String formatted adena amount
	 */
	public static String formatAdena(int amount)
	{
		String s = "";
		int rem = amount % 1000;
		s = Integer.toString(rem);
		amount = (amount - rem) / 1000;
		while (amount > 0)
		{
			if (rem < 99)
			{
				s = '0' + s;
			}
			if (rem < 9)
			{
				s = '0' + s;
			}
			rem = amount % 1000;
			s = Integer.toString(rem) + "," + s;
			amount = (amount - rem) / 1000;
		}
		return s;
	}
	
	public static String formatItem(int amount)
	{
		String s = "";
		int rem = amount % 1000;
		s = Integer.toString(rem);
		amount = (amount - rem) / 1000;
		while (amount > 0)
		{
			if (rem < 99)
			{
				s = '0' + s;
			}
			if (rem < 9)
			{
				s = '0' + s;
			}
			rem = amount % 1000;
			s = Integer.toString(rem) + "," + s;
			amount = (amount - rem) / 1000;
		}
		return s;
	}
	
	public static String reverseColor(final String color)
	{
		final char[] ch1 = color.toCharArray();
		final char[] ch2 = new char[6];
		ch2[0] = ch1[4];
		ch2[1] = ch1[5];
		ch2[2] = ch1[2];
		ch2[3] = ch1[3];
		ch2[4] = ch1[0];
		ch2[5] = ch1[1];
		return new String(ch2);
	}
	
	/**
	 * converts a given time from minutes -> miliseconds
	 * @param minutesToConvert
	 * @return
	 */
	public static int convertMinutesToMiliseconds(final int minutesToConvert)
	{
		return minutesToConvert * 60000;
	}
	
	public static int calcCameraAngle(final int heading)
	{
		int angle;
		// int angle;
		if (heading == 0)
		{
			angle = 360;
		}
		else
		{
			angle = (int) (heading / 182.03999999999999D);
		}
		if (angle <= 90)
		{
			angle += 90;
		}
		else if ((angle > 90) && (angle <= 180))
		{
			angle -= 90;
		}
		else if ((angle > 180) && (angle <= 270))
		{
			angle += 90;
		}
		else if ((angle > 270) && (angle <= 360))
		{
			angle -= 90;
		}
		return angle;
	}
	
	public static int calcCameraAngle(final L2NpcInstance target)
	{
		return calcCameraAngle(target.getHeading());
	}
	
	public static boolean contains(int number, int number2)
	{
		if (number == number2)
		{
			return true;
		}
		
		return false;
	}
	
	public static boolean contains(final int[] array, final int obj)
	{
		for (final int anArray : array)
		{
			if (anArray == obj)
			{
				return true;
			}
		}
		return false;
	}
	
	public static <T> boolean contains(T[] array, T obj)
	{
		for (T element : array)
		{
			if (element == obj)
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static String formatDate(Date date, String format)
	{
		final DateFormat dateFormat = new SimpleDateFormat(format);
		if (date != null)
		{
			return dateFormat.format(date);
		}
		
		return null;
	}
	
	public static String formatDate(long date, String format)
	{
		final DateFormat dateFormat = new SimpleDateFormat(format);
		if (date > 0)
		{
			return dateFormat.format(date);
		}
		
		return null;
	}
	
	public static int getPlayersCountInRadius(int range, L2Character npc, boolean invisible)
	{
		int count = 0;
		for (L2Object player : npc.getKnownList().getKnownObjects().values())
		{
			if (((L2Character) player).isDead())
			{
				continue;
			}
			
			if (!invisible && !((L2Character) player).isVisible())
			{
				continue;
			}
			
			if (!(GeoData.getInstance().canSeeTarget(npc, player)))
			{
				continue;
			}
			
			if (Util.checkIfInRange(range, npc, player, true))
			{
				count++;
			}
		}
		return count;
	}
	
	public static boolean isDigit(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		for (char c : text.toCharArray())
		{
			if (!Character.isDigit(c))
			{
				return false;
			}
		}
		return true;
	}
}
