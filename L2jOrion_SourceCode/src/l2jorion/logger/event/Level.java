package l2jorion.logger.event;

import static l2jorion.logger.event.EventConstants.DEBUG_INT;
import static l2jorion.logger.event.EventConstants.ERROR_INT;
import static l2jorion.logger.event.EventConstants.INFO_INT;
import static l2jorion.logger.event.EventConstants.TRACE_INT;
import static l2jorion.logger.event.EventConstants.WARN_INT;

public enum Level
{
	
	ERROR(ERROR_INT, "ERROR"),
	WARN(WARN_INT, "WARN"),
	INFO(INFO_INT, "INFO"),
	DEBUG(DEBUG_INT, "DEBUG"),
	TRACE(TRACE_INT, "TRACE");
	
	private int levelInt;
	private String levelStr;
	
	Level(int i, String s)
	{
		levelInt = i;
		levelStr = s;
	}
	
	public int toInt()
	{
		return levelInt;
	}
	
	/**
	 * Returns the string representation of this Level.
	 */
	@Override
	public String toString()
	{
		return levelStr;
	}
	
}
