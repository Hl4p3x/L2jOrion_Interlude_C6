/*
 * $Header$
 * 
 * $Author: fordfrog $ $Date$ $Revision$ $Log$
 * 
 * 
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
package l2jorion.util;

import javolution.text.TextBuilder;

public final class StringUtil
{
	
	private StringUtil()
	{
	}
	
	public static String concat(final String... strings)
	{
		final TextBuilder sbString = TextBuilder.newInstance();
		
		for (final String string : strings)
		{
			sbString.append(string);
		}
		
		final String result = sbString.toString();
		TextBuilder.recycle(sbString);
		return result;
	}
	
	public static StringBuilder startAppend(final int sizeHint, final String... strings)
	{
		final int length = getLength(strings);
		final StringBuilder sbString = new StringBuilder(sizeHint > length ? sizeHint : length);
		
		for (final String string : strings)
		{
			sbString.append(string);
		}
		
		return sbString;
	}
	
	public static void append(final StringBuilder sbString, final String... strings)
	{
		sbString.ensureCapacity(sbString.length() + getLength(strings));
		
		for (final String string : strings)
		{
			sbString.append(string);
		}
	}
	
	public static void append(StringBuilder sb, Object... content)
	{
		for (Object obj : content)
		{
			sb.append((obj == null) ? null : obj.toString());
		}
	}
	
	private static int getLength(final String[] strings)
	{
		int length = 0;
		
		for (final String string : strings)
		{
			if (string == null)
			{
				length += 4;
			}
			else
			{
				length += string.length();
			}
		}
		
		return length;
	}
	
	public static String getTraceString(final StackTraceElement[] trace)
	{
		final TextBuilder sbString = TextBuilder.newInstance();
		for (final StackTraceElement element : trace)
		{
			sbString.append(element.toString()).append('\n');
		}
		
		final String result = sbString.toString();
		TextBuilder.recycle(sbString);
		return result;
	}
	
	public static String substringBetween(String str, String open, String close)
	{
		final int INDEX_NOT_FOUND = -1;
		if (str == null || open == null || close == null)
		{
			return null;
		}
		int start = str.indexOf(open);
		if (start != INDEX_NOT_FOUND)
		{
			int end = str.indexOf(close, start + open.length());
			if (end != INDEX_NOT_FOUND)
			{
				return str.substring(start + open.length(), end);
			}
		}
		return null;
	}
}
