/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package l2jorion.logger.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import l2jorion.logger.ILoggerFactory;
import l2jorion.logger.Logger;

public class JDK14LoggerFactory implements ILoggerFactory
{
	ConcurrentMap<String, Logger> loggerMap;
	
	public JDK14LoggerFactory()
	{
		loggerMap = new ConcurrentHashMap<>();
		java.util.logging.Logger.getLogger("");
	}
	
	@Override
	public Logger getLogger(String name)
	{
		if (name.equalsIgnoreCase(Logger.ROOT_LOGGER_NAME))
		{
			name = "";
		}
		
		Logger slf4jLogger = loggerMap.get(name);
		if (slf4jLogger != null)
		{
			return slf4jLogger;
		}
		
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(name);
		Logger newInstance = new JDK14LoggerAdapter(julLogger);
		Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
		return oldInstance == null ? newInstance : oldInstance;
	}
}
