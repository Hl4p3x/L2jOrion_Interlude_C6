package l2jorion.logger.event;

import l2jorion.logger.Marker;

public interface LoggingEvent
{
	Level getLevel();
	
	Marker getMarker();
	
	String getLoggerName();
	
	String getMessage();
	
	String getThreadName();
	
	Object[] getArgumentArray();
	
	long getTimeStamp();
	
	Throwable getThrowable();
}
