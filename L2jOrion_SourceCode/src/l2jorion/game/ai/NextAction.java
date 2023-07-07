package l2jorion.game.ai;

import java.util.ArrayList;
import java.util.List;

public class NextAction
{
	public interface NextActionCallback
	{
		public void doWork();
	}
	
	private List<CtrlEvent> _events;
	private List<CtrlIntention> _intentions;
	private NextActionCallback _callback;
	
	public NextAction(List<CtrlEvent> events, List<CtrlIntention> intentions, NextActionCallback callback)
	{
		_events = events;
		_intentions = intentions;
		setCallback(callback);
	}
	
	public NextAction(CtrlEvent event, CtrlIntention intention, NextActionCallback callback)
	{
		if (_events == null)
		{
			_events = new ArrayList<>();
		}
		
		if (_intentions == null)
		{
			_intentions = new ArrayList<>();
		}
		
		if (event != null)
		{
			_events.add(event);
		}
		
		if (intention != null)
		{
			_intentions.add(intention);
		}
		
		setCallback(callback);
	}
	
	public void doAction()
	{
		if (_callback != null)
		{
			_callback.doWork();
		}
	}
	
	public List<CtrlEvent> getEvents()
	{
		if (_events == null)
		{
			_events = new ArrayList<>();
		}
		
		return _events;
	}
	
	public void setEvents(ArrayList<CtrlEvent> event)
	{
		_events = event;
	}
	
	public void addEvent(CtrlEvent event)
	{
		if (_events == null)
		{
			_events = new ArrayList<>();
		}
		
		if (event != null)
		{
			_events.add(event);
		}
	}
	
	public void removeEvent(CtrlEvent event)
	{
		if (_events == null)
		{
			return;
		}
		
		_events.remove(event);
	}
	
	public NextActionCallback getCallback()
	{
		return _callback;
	}
	
	public void setCallback(NextActionCallback callback)
	{
		_callback = callback;
	}
	
	public List<CtrlIntention> getIntentions()
	{
		if (_intentions == null)
		{
			_intentions = new ArrayList<>();
		}
		return _intentions;
	}
	
	public void setIntentions(ArrayList<CtrlIntention> intentions)
	{
		_intentions = intentions;
	}
	
	public void addIntention(CtrlIntention intention)
	{
		if (_intentions == null)
		{
			_intentions = new ArrayList<>();
		}
		
		if (intention != null)
		{
			_intentions.add(intention);
		}
	}
	
	public void removeIntention(CtrlIntention intention)
	{
		if (_intentions == null)
		{
			return;
		}
		_intentions.remove(intention);
	}
}