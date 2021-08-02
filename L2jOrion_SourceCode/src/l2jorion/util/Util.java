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
package l2jorion.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javolution.text.TextBuilder;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Util
{
	protected static final Logger LOG = LoggerFactory.getLogger(Util.class);
	
	public static boolean isInternalIP(final String ipAddress)
	{
		return ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") || ipAddress.startsWith("127.0.0.1");
	}
	
	public static String printData(final byte[] data, final int len)
	{
		final TextBuilder result = new TextBuilder();
		
		int counter = 0;
		
		for (int i = 0; i < len; i++)
		{
			if (counter % 16 == 0)
			{
				result.append(fillHex(i, 4) + ": ");
			}
			
			result.append(fillHex(data[i] & 0xff, 2) + " ");
			counter++;
			if (counter == 16)
			{
				result.append("   ");
				
				int charpoint = i - 15;
				for (int a = 0; a < 16; a++)
				{
					final int t1 = data[charpoint++];
					if (t1 > 0x1f && t1 < 0x80)
					{
						result.append((char) t1);
					}
					else
					{
						result.append('.');
					}
				}
				
				result.append('\n');
				counter = 0;
			}
		}
		
		final int rest = data.length % 16;
		if (rest > 0)
		{
			for (int i = 0; i < 17 - rest; i++)
			{
				result.append("   ");
			}
			
			int charpoint = data.length - rest;
			for (int a = 0; a < rest; a++)
			{
				final int t1 = data[charpoint++];
				if (t1 > 0x1f && t1 < 0x80)
				{
					result.append((char) t1);
				}
				else
				{
					result.append('.');
				}
			}
			
			result.append('\n');
		}
		
		return result.toString();
	}
	
	public static String fillHex(final int data, final int digits)
	{
		String number = Integer.toHexString(data);
		
		for (int i = number.length(); i < digits; i++)
		{
			number = "0" + number;
		}
		
		return number;
	}
	
	public static void printSection(String s)
	{
		final int maxlength = 90;
		s = "( " + s + " ) ";
		
		final int slen = s.length();
		if (slen > maxlength)
		{
			System.out.println(s);
			return;
		}
		
		int i;
		for (i = 0; i < maxlength - slen; i++)
		{
			s = "-" + s;
		}
		System.out.println(s);
	}
	
	public static String printData(final byte[] raw)
	{
		return printData(raw, raw.length);
	}
	
	private static void printCpuInfo()
	{
		LOG.info("Avaible CPU(s): " + Runtime.getRuntime().availableProcessors());
		LOG.info("Processor(s) Identifier: " + System.getenv("PROCESSOR_IDENTIFIER"));
		LOG.info("..................................................");
		LOG.info("..................................................");
	}
	
	/**
	 * returns the operational system server is running on it.
	 */
	private static void printOSInfo()
	{
		LOG.info("OS: " + System.getProperty("os.name") + " Build: " + System.getProperty("os.version"));
		LOG.info("OS Arch: " + System.getProperty("os.arch"));
		LOG.info("..................................................");
		LOG.info("..................................................");
	}
	
	/**
	 * returns JAVA Runtime Enviroment properties
	 */
	private static void printJreInfo()
	{
		LOG.info("Java Platform Information");
		LOG.info("Java Runtime  Name: " + System.getProperty("java.runtime.name"));
		LOG.info("Java Version: " + System.getProperty("java.version"));
		LOG.info("Java Class Version: " + System.getProperty("java.class.version"));
		LOG.info("..................................................");
		LOG.info("..................................................");
	}
	
	/**
	 * returns general infos related to machine
	 */
	private static void printRuntimeInfo()
	{
		LOG.info("Runtime Information");
		LOG.info("Current Free Heap Size: " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + " mb");
		LOG.info("Current Heap Size: " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + " mb");
		LOG.info("Maximum Heap Size: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " mb");
		LOG.info("..................................................");
		LOG.info("..................................................");
		
	}
	
	/**
	 * calls time service to get system time.
	 */
	private static void printSystemTime()
	{
		// instanciates Date Objec
		final Date dateInfo = new Date();
		
		// generates a simple date format
		final SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa");
		
		// generates String that will get the formater info with values
		final String dayInfo = df.format(dateInfo);
		
		LOG.info("..................................................");
		LOG.info("System Time: " + dayInfo);
		LOG.info("..................................................");
	}
	
	/**
	 * gets system JVM properties.
	 */
	private static void printJvmInfo()
	{
		LOG.info("Virtual Machine Information (JVM)");
		LOG.info("JVM Name: " + System.getProperty("java.vm.name"));
		LOG.info("JVM installation directory: " + System.getProperty("java.home"));
		LOG.info("JVM version: " + System.getProperty("java.vm.version"));
		LOG.info("JVM Vendor: " + System.getProperty("java.vm.vendor"));
		LOG.info("JVM Info: " + System.getProperty("java.vm.info"));
		LOG.info("..................................................");
		LOG.info("..................................................");
	}
	
	/**
	 * prints all other methods.
	 */
	public static void printGeneralSystemInfo()
	{
		printSystemTime();
		printOSInfo();
		printCpuInfo();
		printRuntimeInfo();
		printJreInfo();
		printJvmInfo();
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
	
	public static int getAvailableProcessors()
	{
		final Runtime rt = Runtime.getRuntime();
		return rt.availableProcessors();
	}
	
	public static String getOSName()
	{
		return System.getProperty("os.name");
	}
	
	public static String getOSVersion()
	{
		return System.getProperty("os.version");
	}
	
	public static String getOSArch()
	{
		return System.getProperty("os.arch");
	}
	
	/**
	 * Method to get the stack trace of a Throwable into a String
	 * @param t Throwable to get the stacktrace from
	 * @return stack trace from Throwable as String
	 */
	public static String getStackTrace(Throwable t)
	{
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
