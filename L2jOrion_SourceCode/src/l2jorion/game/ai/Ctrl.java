package l2jorion.game.ai;

import l2jorion.game.model.L2Character;
import l2jorion.game.model.L2Object;

public interface Ctrl
{
	L2Character getActor();
	
	CtrlIntention getIntention();
	
	L2Object getTarget();
	
	void setIntention(CtrlIntention intention);
	
	void setIntention(CtrlIntention intention, Object arg0);
	
	void setIntention(CtrlIntention intention, Object arg0, Object arg1);
	
	void notifyEvent(CtrlEvent evt);
	
	void notifyEvent(CtrlEvent evt, Object arg0);
	
	void notifyEvent(CtrlEvent evt, Object... args);
}