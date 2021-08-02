package l2jorion.logger.event;

import java.util.Queue;

import l2jorion.logger.Logger;
import l2jorion.logger.Marker;
import l2jorion.logger.helpers.SubstituteLogger;

public class EventRecodingLogger implements Logger {

    String name;
    SubstituteLogger logger;
    Queue<SubstituteLoggingEvent> eventQueue;

    public EventRecodingLogger(SubstituteLogger logger, Queue<SubstituteLoggingEvent> eventQueue) {
        this.logger = logger;
        this.name = logger.getName();
        this.eventQueue = eventQueue;
    }

    @Override
	public String getName() {
        return name;
    }

    private void recordEvent(Level level, String msg, Object[] args, Throwable throwable) {
        recordEvent(level, null, msg, args, throwable);
    }

    private void recordEvent(Level level, Marker marker, String msg, Object[] args, Throwable throwable) {
        // System.out.println("recording logger:"+name+", msg:"+msg);
        SubstituteLoggingEvent loggingEvent = new SubstituteLoggingEvent();
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.setLevel(level);
        loggingEvent.setLogger(logger);
        loggingEvent.setLoggerName(name);
        loggingEvent.setMarker(marker);
        loggingEvent.setMessage(msg);
        loggingEvent.setArgumentArray(args);
        loggingEvent.setThrowable(throwable);
        loggingEvent.setThreadName(Thread.currentThread().getName());
        eventQueue.add(loggingEvent);
    }

    @Override
	public boolean isTraceEnabled() {
        return true;
    }

    @Override
	public void trace(String msg) {
        recordEvent(Level.TRACE, msg, null, null);
    }

    @Override
	public void trace(String format, Object arg) {
        recordEvent(Level.TRACE, format, new Object[] { arg }, null);
    }

    @Override
	public void trace(String format, Object arg1, Object arg2) {
        recordEvent(Level.TRACE, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
	public void trace(String format, Object... arguments) {
        recordEvent(Level.TRACE, format, arguments, null);
    }

    @Override
	public void trace(String msg, Throwable t) {
        recordEvent(Level.TRACE, msg, null, t);
    }

    @Override
	public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
	public void trace(Marker marker, String msg) {
        recordEvent(Level.TRACE, marker, msg, null, null);

    }

    @Override
	public void trace(Marker marker, String format, Object arg) {
        recordEvent(Level.TRACE, marker, format, new Object[] { arg }, null);
    }

    @Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
        recordEvent(Level.TRACE, marker, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
	public void trace(Marker marker, String format, Object... argArray) {
        recordEvent(Level.TRACE, marker, format, argArray, null);

    }

    @Override
	public void trace(Marker marker, String msg, Throwable t) {
        recordEvent(Level.TRACE, marker, msg, null, t);
    }

    @Override
	public boolean isDebugEnabled() {
        return true;
    }

    @Override
	public void debug(String msg) {
        recordEvent(Level.DEBUG, msg, null, null);
    }

    @Override
	public void debug(String format, Object arg) {
        recordEvent(Level.DEBUG, format, new Object[] { arg }, null);

    }

    @Override
	public void debug(String format, Object arg1, Object arg2) {
        recordEvent(Level.DEBUG, format, new Object[] { arg1, arg2 }, null);

    }

    @Override
	public void debug(String format, Object... arguments) {
        recordEvent(Level.DEBUG, format, arguments, null);
    }

    @Override
	public void debug(String msg, Throwable t) {
        recordEvent(Level.DEBUG, msg, null, t);
    }

    @Override
	public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
	public void debug(Marker marker, String msg) {
        recordEvent(Level.DEBUG, marker, msg, null, null);
    }

    @Override
	public void debug(Marker marker, String format, Object arg) {
        recordEvent(Level.DEBUG, marker, format, new Object[] { arg }, null);
    }

    @Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
        recordEvent(Level.DEBUG, marker, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
	public void debug(Marker marker, String format, Object... arguments) {
        recordEvent(Level.DEBUG, marker, format, arguments, null);
    }

    @Override
	public void debug(Marker marker, String msg, Throwable t) {
        recordEvent(Level.DEBUG, marker, msg, null, t);
    }

    @Override
	public boolean isInfoEnabled() {
        return true;
    }

    @Override
	public void info(String msg) {
        recordEvent(Level.INFO, msg, null, null);
    }

    @Override
	public void info(String format, Object arg) {
        recordEvent(Level.INFO, format, new Object[] { arg }, null);
    }

    @Override
	public void info(String format, Object arg1, Object arg2) {
        recordEvent(Level.INFO, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
	public void info(String format, Object... arguments) {
        recordEvent(Level.INFO, format, arguments, null);
    }

    @Override
	public void info(String msg, Throwable t) {
        recordEvent(Level.INFO, msg, null, t);
    }

    @Override
	public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
	public void info(Marker marker, String msg) {
        recordEvent(Level.INFO, marker, msg, null, null);
    }

    @Override
	public void info(Marker marker, String format, Object arg) {
        recordEvent(Level.INFO, marker, format, new Object[] { arg }, null);
    }

    @Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
        recordEvent(Level.INFO, marker, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
	public void info(Marker marker, String format, Object... arguments) {
        recordEvent(Level.INFO, marker, format, arguments, null);
    }

    @Override
	public void info(Marker marker, String msg, Throwable t) {
        recordEvent(Level.INFO, marker, msg, null, t);

    }

    @Override
	public boolean isWarnEnabled() {
        return true;
    }

    @Override
	public void warn(String msg) {
        recordEvent(Level.WARN, msg, null, null);
    }

    @Override
	public void warn(String format, Object arg) {
        recordEvent(Level.WARN, format, new Object[] { arg }, null);

    }

    @Override
	public void warn(String format, Object arg1, Object arg2) {
        recordEvent(Level.WARN, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
	public void warn(String format, Object... arguments) {
        recordEvent(Level.WARN, format, arguments, null);
    }

    @Override
	public void warn(String msg, Throwable t) {
        recordEvent(Level.WARN, msg, null, t);
    }

    @Override
	public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
	public void warn(Marker marker, String msg) {
        recordEvent(Level.WARN, msg, null, null);
    }

    @Override
	public void warn(Marker marker, String format, Object arg) {
        recordEvent(Level.WARN, format, new Object[] { arg }, null);
    }

    @Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
        recordEvent(Level.WARN, marker, format, new Object[] { arg1, arg2 }, null);

    }

    @Override
	public void warn(Marker marker, String format, Object... arguments) {
        recordEvent(Level.WARN, marker, format, arguments, null);
    }

    @Override
	public void warn(Marker marker, String msg, Throwable t) {
        recordEvent(Level.WARN, marker, msg, null, t);
    }

    @Override
	public boolean isErrorEnabled() {
        return true;
    }

    @Override
	public void error(String msg) {
        recordEvent(Level.ERROR, msg, null, null);
    }

    @Override
	public void error(String format, Object arg) {
        recordEvent(Level.ERROR, format, new Object[] { arg }, null);

    }

    @Override
	public void error(String format, Object arg1, Object arg2) {
        recordEvent(Level.ERROR, format, new Object[] { arg1, arg2 }, null);

    }

    @Override
	public void error(String format, Object... arguments) {
        recordEvent(Level.ERROR, format, arguments, null);

    }

    @Override
	public void error(String msg, Throwable t) {
        recordEvent(Level.ERROR, msg, null, t);
    }

    @Override
	public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
	public void error(Marker marker, String msg) {
        recordEvent(Level.ERROR, marker, msg, null, null);

    }

    @Override
	public void error(Marker marker, String format, Object arg) {
        recordEvent(Level.ERROR, marker, format, new Object[] { arg }, null);

    }

    @Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
        recordEvent(Level.ERROR, marker, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
	public void error(Marker marker, String format, Object... arguments) {
        recordEvent(Level.ERROR, marker, format, arguments, null);
    }

    @Override
	public void error(Marker marker, String msg, Throwable t) {
        recordEvent(Level.ERROR, marker, msg, null, t);
    }

}
