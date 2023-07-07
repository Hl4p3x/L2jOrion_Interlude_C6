package l2jorion.game.handler;

import java.util.Map;

import javolution.util.FastMap;
import l2jorion.Config;
import l2jorion.game.handler.user.ChannelDelete;
import l2jorion.game.handler.user.ChannelLeave;
import l2jorion.game.handler.user.ChannelListUpdate;
import l2jorion.game.handler.user.ClanPenalty;
import l2jorion.game.handler.user.ClanWarsList;
import l2jorion.game.handler.user.DisMount;
import l2jorion.game.handler.user.Escape;
import l2jorion.game.handler.user.Loc;
import l2jorion.game.handler.user.Mount;
import l2jorion.game.handler.user.OfflineShop;
import l2jorion.game.handler.user.OlympiadStat;
import l2jorion.game.handler.user.PartyInfo;
import l2jorion.game.handler.user.SiegeStatus;
import l2jorion.game.handler.user.Time;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class UserCommandHandler
{
	private static final Logger LOG = LoggerFactory.getLogger(UserCommandHandler.class);
	
	private static UserCommandHandler _instance;
	
	private final Map<Integer, IUserCommandHandler> _datatable;
	
	public static UserCommandHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new UserCommandHandler();
		}
		
		return _instance;
	}
	
	private UserCommandHandler()
	{
		_datatable = new FastMap<>();
		registerUserCommandHandler(new Time());
		registerUserCommandHandler(new OlympiadStat());
		registerUserCommandHandler(new ChannelLeave());
		registerUserCommandHandler(new ChannelDelete());
		registerUserCommandHandler(new ChannelListUpdate());
		registerUserCommandHandler(new ClanPenalty());
		registerUserCommandHandler(new ClanWarsList());
		registerUserCommandHandler(new DisMount());
		registerUserCommandHandler(new Escape());
		registerUserCommandHandler(new Loc());
		registerUserCommandHandler(new Mount());
		registerUserCommandHandler(new PartyInfo());
		registerUserCommandHandler(new SiegeStatus());
		
		if (Config.OFFLINE_TRADE_ENABLE && Config.OFFLINE_COMMAND1)
		{
			registerUserCommandHandler(new OfflineShop());
		}
		
		LOG.info("UserCommandHandler: Loaded " + _datatable.size() + " handlers");
	}
	
	public void registerUserCommandHandler(final IUserCommandHandler handler)
	{
		int[] ids = handler.getUserCommandList();
		
		for (final int id : ids)
		{
			_datatable.put(Integer.valueOf(id), handler);
		}
	}
	
	public IUserCommandHandler getUserCommandHandler(final int userCommand)
	{
		return _datatable.get(Integer.valueOf(userCommand));
	}
	
	public int size()
	{
		return _datatable.size();
	}
}