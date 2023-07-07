package l2jorion.game.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2jorion.Config;
import l2jorion.game.autofarm.AutofarmCommandHandler;
import l2jorion.game.handler.custom.CustomBypassHandler;
import l2jorion.game.handler.voice.DressMe;
import l2jorion.game.handler.voice.Event_CTF;
import l2jorion.game.handler.voice.Event_DM;
import l2jorion.game.handler.voice.Event_TVT;
import l2jorion.game.handler.voice.ExpireItems;
import l2jorion.game.handler.voice.OfflineShop;
import l2jorion.game.handler.voice.Online;
import l2jorion.game.handler.voice.PartyTeleport;
import l2jorion.game.handler.voice.SellBuffs;
import l2jorion.game.handler.voice.Wedding;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class VoicedCommandHandler
{
	private static Logger LOG = LoggerFactory.getLogger(VoicedCommandHandler.class);
	
	private static VoicedCommandHandler _instance;
	
	private Map<String, IVoicedCommandHandler> _datatable = new ConcurrentHashMap<>();
	
	public static VoicedCommandHandler getInstance()
	{
		if (_instance == null)
		{
			_instance = new VoicedCommandHandler();
		}
		
		return _instance;
	}
	
	private VoicedCommandHandler()
	{
		if (Config.CTF_COMMAND)
		{
			registerVoicedCommandHandler(new Event_CTF());
		}
		
		if (Config.TVT_COMMAND)
		{
			registerVoicedCommandHandler(new Event_TVT());
		}
		
		if (Config.DM_COMMAND)
		{
			registerVoicedCommandHandler(new Event_DM());
		}
		
		if (Config.L2JMOD_ALLOW_WEDDING)
		{
			registerVoicedCommandHandler(new Wedding());
		}
		
		if (Config.ALLOW_ONLINE_VIEW)
		{
			registerVoicedCommandHandler(new Online());
		}
		
		if (Config.OFFLINE_TRADE_ENABLE && Config.OFFLINE_COMMAND2)
		{
			registerVoicedCommandHandler(new OfflineShop());
		}
		
		if (Config.SELLBUFF_SYSTEM)
		{
			registerVoicedCommandHandler(new SellBuffs());
		}
		
		if (Config.ALLOW_DRESS_ME_SYSTEM)
		{
			registerVoicedCommandHandler(new DressMe());
		}
		
		if (Config.AUTOFARM_ENABLED)
		{
			registerVoicedCommandHandler(new AutofarmCommandHandler());
		}
		
		if (Config.L2LIMIT_CUSTOM)
		{
			registerVoicedCommandHandler(new PartyTeleport());
		}
		
		registerVoicedCommandHandler(new ExpireItems());
		CustomBypassHandler.getInstance().registerCustomBypassHandler(new ExpireItems());
		
		LOG.info("VoicedCommandHandler: Loaded " + _datatable.size() + " handlers");
		
	}
	
	public void registerVoicedCommandHandler(IVoicedCommandHandler handler)
	{
		String[] ids = handler.getVoicedCommandList();
		
		for (String id : ids)
		{
			if (Config.DEBUG)
			{
				LOG.info("Adding handler for command " + id);
			}
			
			_datatable.put(id, handler);
		}
		
		ids = null;
	}
	
	public IVoicedCommandHandler getVoicedCommandHandler(String voicedCommand)
	{
		String command = voicedCommand;
		
		if (voicedCommand.indexOf(" ") != -1)
		{
			command = voicedCommand.substring(0, voicedCommand.indexOf(" "));
		}
		
		if (Config.DEBUG)
		{
			LOG.info("getting handler for command: " + command + " -> " + (_datatable.get(command) != null));
		}
		
		return _datatable.get(command);
	}
	
	public int size()
	{
		return _datatable.size();
	}
}