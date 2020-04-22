/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.util;

import l2jorion.Config;
import l2jorion.game.network.L2GameClient;

public final class FloodProtectors
{
	/**
	 * Use-AugItem flood protector.
	 */
	private final FloodProtectorAction _useAugItem;
	/**
	 * Use-item flood protector.
	 */
	private final FloodProtectorAction _useItem;
	/**
	 * Roll-dice flood protector.
	 */
	private final FloodProtectorAction _rollDice;
	/**
	 * Firework flood protector.
	 */
	private final FloodProtectorAction _firework;
	/**
	 * Item-pet-summon flood protector.
	 */
	private final FloodProtectorAction _itemPetSummon;
	/**
	 * Hero-voice flood protector.
	 */
	private final FloodProtectorAction _heroVoice;
	/**
	 * Global-chat flood protector.
	 */
	private final FloodProtectorAction _globalChat;
	
	private final FloodProtectorAction _tradeChat;
	/**
	 * Subclass flood protector.
	 */
	private final FloodProtectorAction _subclass;
	/**
	 * Drop-item flood protector.
	 */
	private final FloodProtectorAction _dropItem;
	/**
	 * Server-bypass flood protector.
	 */
	private final FloodProtectorAction _serverBypass;
	/**
	 * Multisell flood protector.
	 */
	private final FloodProtectorAction _multiSell;
	/**
	 * Transaction flood protector.
	 */
	private final FloodProtectorAction _transaction;
	/**
	 * Manufacture flood protector.
	 */
	private final FloodProtectorAction _manufacture;
	/**
	 * Manor flood protector.
	 */
	private final FloodProtectorAction _manor;
	/**
	 * Character Select protector
	 */
	private final FloodProtectorAction _characterSelect;
	/**
	 * Unknown Packets protector
	 */
	private final FloodProtectorAction _unknownPackets;
	/**
	 * Party Invitation flood protector.
	 */
	private final FloodProtectorAction _partyInvitation;
	/**
	 * Say Action protector
	 */
	private final FloodProtectorAction _sayAction;
	/**
	 * Move Action protector
	 */
	private final FloodProtectorAction _moveAction;
	/**
	 * Generic Action protector
	 */
	private final FloodProtectorAction _genericAction;
	/**
	 * Macro protector
	 */
	private final FloodProtectorAction _macro;
	/**
	 * Potion protector
	 */
	private final FloodProtectorAction _potion;
	
	/**
	 * Creates new instance of FloodProtectors.
	 * @param client for which the collection of flood protectors is being created.
	 */
	public FloodProtectors(final L2GameClient client)
	{
		super();
		_useAugItem = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_USE_AUG_ITEM);
		_useItem = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_USE_ITEM);
		_rollDice = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_ROLL_DICE);
		_firework = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_FIREWORK);
		_itemPetSummon = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_ITEM_PET_SUMMON);
		_heroVoice = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_HERO_VOICE);
		_globalChat = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_GLOBAL_CHAT);
		_tradeChat = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_TRADE_CHAT);
		_subclass = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_SUBCLASS);
		_dropItem = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_DROP_ITEM);
		_serverBypass = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_SERVER_BYPASS);
		_multiSell = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_MULTISELL);
		_transaction = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_TRANSACTION);
		_manufacture = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_MANUFACTURE);
		_manor = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_MANOR);
		_characterSelect = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_CHARACTER_SELECT);
		_unknownPackets = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_UNKNOWN_PACKETS);
		_partyInvitation = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_PARTY_INVITATION);
		_sayAction = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_SAY_ACTION);
		_moveAction = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_MOVE_ACTION);
		_genericAction = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_GENERIC_ACTION);
		_macro = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_MACRO);
		_potion = new FloodProtectorAction(client, Config.FLOOD_PROTECTOR_POTION);
	}
	
	/**
	 * Returns {@link #_useAugItem}.
	 * @return {@link #_useAugItem}
	 */
	public FloodProtectorAction getUseAugItem()
	{
		return _useAugItem;
	}
	/**
	 * Returns {@link #_useItem}.
	 * @return {@link #_useItem}
	 */
	public FloodProtectorAction getUseItem()
	{
		return _useItem;
	}
	
	/**
	 * Returns {@link #_rollDice}.
	 * @return {@link #_rollDice}
	 */
	public FloodProtectorAction getRollDice()
	{
		return _rollDice;
	}
	
	/**
	 * Returns {@link #_firework}.
	 * @return {@link #_firework}
	 */
	public FloodProtectorAction getFirework()
	{
		return _firework;
	}
	
	/**
	 * Returns {@link #_itemPetSummon}.
	 * @return {@link #_itemPetSummon}
	 */
	public FloodProtectorAction getItemPetSummon()
	{
		return _itemPetSummon;
	}
	
	/**
	 * Returns {@link #_heroVoice}.
	 * @return {@link #_heroVoice}
	 */
	public FloodProtectorAction getHeroVoice()
	{
		return _heroVoice;
	}
	
	/**
	 * Returns {@link #_globalChat}.
	 * @return {@link #_globalChat}
	 */
	public FloodProtectorAction getGlobalChat()
	{
		return _globalChat;
	}
	
	public FloodProtectorAction getTradeChat()
	{
		return _tradeChat;
	}
	
	/**
	 * Returns {@link #_subclass}.
	 * @return {@link #_subclass}
	 */
	public FloodProtectorAction getSubclass()
	{
		return _subclass;
	}
	
	/**
	 * Returns {@link #_dropItem}.
	 * @return {@link #_dropItem}
	 */
	public FloodProtectorAction getDropItem()
	{
		return _dropItem;
	}
	
	/**
	 * Returns {@link #_serverBypass}.
	 * @return {@link #_serverBypass}
	 */
	public FloodProtectorAction getServerBypass()
	{
		return _serverBypass;
	}
	
	public FloodProtectorAction getMultiSell()
	{
		return _multiSell;
	}
	
	/**
	 * Returns {@link #_transaction}.
	 * @return {@link #_transaction}
	 */
	public FloodProtectorAction getTransaction()
	{
		return _transaction;
	}
	
	/**
	 * Returns {@link #_manufacture}.
	 * @return {@link #_manufacture}
	 */
	public FloodProtectorAction getManufacture()
	{
		return _manufacture;
	}
	
	/**
	 * Returns {@link #_manor}.
	 * @return {@link #_manor}
	 */
	public FloodProtectorAction getManor()
	{
		return _manor;
	}
	
	/**
	 * Returns {@link #_characterSelect}.
	 * @return {@link #_characterSelect}
	 */
	public FloodProtectorAction getCharacterSelect()
	{
		return _characterSelect;
	}
	
	/**
	 * Returns {@link #_unknownPackets}.
	 * @return {@link #_unknownPackets}
	 */
	public FloodProtectorAction getUnknownPackets()
	{
		return _unknownPackets;
	}
	
	/**
	 * Returns {@link #_partyInvitation}.
	 * @return {@link #_partyInvitation}
	 */
	public FloodProtectorAction getPartyInvitation()
	{
		return _partyInvitation;
	}
	
	/**
	 * Returns {@link #_sayAction}.
	 * @return {@link #_sayAction}
	 */
	public FloodProtectorAction getSayAction()
	{
		return _sayAction;
	}
	
	/**
	 * Returns {@link #_moveAction}.
	 * @return {@link #_moveAction}
	 */
	public FloodProtectorAction getMoveAction()
	{
		return _moveAction;
	}
	
	/**
	 * Returns {@link #_genericAction}.
	 * @return {@link #_genericAction}
	 */
	public FloodProtectorAction getGenericAction()
	{
		return _genericAction;
	}
	
	/**
	 * Returns {@link #_macro}.
	 * @return {@link #_macro}
	 */
	public FloodProtectorAction getMacro()
	{
		return _macro;
	}
	
	/**
	 * Returns {@link #_potion}.
	 * @return {@link #_potion}
	 */
	public FloodProtectorAction getUsePotion()
	{
		return _potion;
	}
}