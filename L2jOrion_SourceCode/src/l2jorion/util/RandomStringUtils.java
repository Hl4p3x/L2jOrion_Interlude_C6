/*
 * Copyright 2002-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l2jorion.util;

import java.util.Random;

public class RandomStringUtils
{
	private static final Random RANDOM = new Random();
	
	public RandomStringUtils()
	{
	}
	
	public static String random(int count)
	{
		return random(count, false, false);
	}
	
	public static String randomAscii(int count)
	{
		return random(count, 32, 127, false, false);
	}
	
	public static String randomAlphabetic(int count)
	{
		return random(count, true, false);
	}
	
	public static String randomAlphanumeric(int count)
	{
		return random(count, true, true);
	}
	
	public static String randomNumeric(int count)
	{
		return random(count, false, true);
	}
	
	public static String random(int count, boolean letters, boolean numbers)
	{
		return random(count, 0, 0, letters, numbers);
	}
	
	public static String random(int count, int start, int end, boolean letters, boolean numbers)
	{
		return random(count, start, end, letters, numbers, null, RANDOM);
	}
	
	public static String random(int count, int start, int end, boolean letters, boolean numbers, char[] chars)
	{
		return random(count, start, end, letters, numbers, chars, RANDOM);
	}
	
	public static String random(int count, int start, int end, boolean letters, boolean numbers, char[] chars, Random random)
	{
		if (count == 0)
		{
			return "";
		}
		else if (count < 0)
		{
			throw new IllegalArgumentException("Requested random string length " + count + " is less than 0.");
		}
		if ((start == 0) && (end == 0))
		{
			end = 'z' + 1;
			start = ' ';
			if (!letters && !numbers)
			{
				start = 0;
				end = Integer.MAX_VALUE;
			}
		}
		
		StringBuffer buffer = new StringBuffer();
		int gap = end - start;
		
		while (count-- != 0)
		{
			char ch;
			if (chars == null)
			{
				ch = (char) (random.nextInt(gap) + start);
			}
			else
			{
				ch = chars[random.nextInt(gap) + start];
			}
			if ((letters && numbers && Character.isLetterOrDigit(ch)) || (letters && Character.isLetter(ch)) || (numbers && Character.isDigit(ch)) || (!letters && !numbers))
			{
				buffer.append(ch);
			}
			else
			{
				count++;
			}
		}
		return buffer.toString();
	}
	
	public static String random(int count, String chars)
	{
		if (chars == null)
		{
			return random(count, 0, 0, false, false, null, RANDOM);
		}
		return random(count, chars.toCharArray());
	}
	
	public static String random(int count, char[] chars)
	{
		if (chars == null)
		{
			return random(count, 0, 0, false, false, null, RANDOM);
		}
		return random(count, 0, chars.length, false, false, chars, RANDOM);
	}
	
}
