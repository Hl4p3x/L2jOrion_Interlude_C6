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
package l2jorion.game.network.serverpackets;

import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.xml.DressMeData;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Summon;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.SkinPackage;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.PacketServer;
import l2jorion.game.templates.L2NpcTemplate;

public class UserInfo extends PacketServer
{
	private static final String _S__04_USERINFO = "[S] 04 UserInfo";
	
	private final L2PcInstance _activeChar;
	
	private int _runSpd;
	private int _walkSpd;
	private int _swimRunSpd;
	private int _swimWalkSpd;
	private int _flRunSpd;
	private int _flWalkSpd;
	private int _flyRunSpd;
	private int _flyWalkSpd;
	private int _relation;
	private float _moveMultiplier;
	
	// Your own character info
	public UserInfo(L2PcInstance character)
	{
		_activeChar = character;
		
		_relation = _activeChar.isClanLeader() ? 0x40 : 0;
		
		if (_activeChar.getSiegeState() == 1)
		{
			_relation |= 0x180;
		}
		
		if (_activeChar.getSiegeState() == 2)
		{
			_relation |= 0x80;
		}
		
		_moveMultiplier = _activeChar.getMovementSpeedMultiplier();
		_runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (_activeChar.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x04);
		
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		writeD(_activeChar.getHeading());
		
		writeD(_activeChar.getObjectId());
		writeS(_activeChar.getName());
		writeD(_activeChar.getRace().ordinal());
		writeD(_activeChar.getAppearance().getSex() ? 1 : 0);
		
		if (_activeChar.getClassIndex() == 0)
		{
			writeD(_activeChar.getClassId().getId());
		}
		else
		{
			writeD(_activeChar.getBaseClass());
		}
		
		writeD(_activeChar.getLevel());
		writeQ(_activeChar.getExp());
		writeD(_activeChar.getSTR());
		writeD(_activeChar.getDEX());
		writeD(_activeChar.getCON());
		writeD(_activeChar.getINT());
		writeD(_activeChar.getWIT());
		writeD(_activeChar.getMEN());
		writeD(_activeChar.getMaxHp());
		writeD((int) _activeChar.getCurrentHp());
		writeD(_activeChar.getMaxMp());
		writeD((int) _activeChar.getCurrentMp());
		writeD(_activeChar.getSp());
		writeD(_activeChar.getCurrentLoad());
		writeD(_activeChar.getMaxLoad());
		
		writeD(_activeChar.getActiveWeaponItem() != null ? 40 : 20); // 20 no weapon, 40 weapon equipped
		
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DHAIR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
		
		if (Config.ALLOW_DRESS_ME_SYSTEM)
		{
			if (_activeChar.getWeaponSkinOption() > 0 && getWeaponOption(_activeChar.getWeaponSkinOption()) != null)
			{
				writeD(getWeaponOption(_activeChar.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(_activeChar.getWeaponSkinOption()).getWeaponId() : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			}
			
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
			
			if (_activeChar.getArmorSkinOption() > 0 && getArmorOption(_activeChar.getArmorSkinOption()) != null)
			{
				writeD(getArmorOption(_activeChar.getArmorSkinOption()).getGlovesId() != 0 ? getArmorOption(_activeChar.getArmorSkinOption()).getGlovesId() : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
				writeD(getArmorOption(_activeChar.getArmorSkinOption()).getChestId() != 0 ? getArmorOption(_activeChar.getArmorSkinOption()).getChestId() : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
				writeD(getArmorOption(_activeChar.getArmorSkinOption()).getLegsId() != 0 ? getArmorOption(_activeChar.getArmorSkinOption()).getLegsId() : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
				writeD(getArmorOption(_activeChar.getArmorSkinOption()).getFeetId() != 0 ? getArmorOption(_activeChar.getArmorSkinOption()).getFeetId() : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
				writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
				writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
				writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			}
			
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
			
			if (_activeChar.getWeaponSkinOption() > 0 && getWeaponOption(_activeChar.getWeaponSkinOption()) != null)
			{
				writeD(getWeaponOption(_activeChar.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(_activeChar.getWeaponSkinOption()).getWeaponId() : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
			}
			
			if (_activeChar.getHairSkinOption() > 0 && getHairOption(_activeChar.getHairSkinOption()) != null)
			{
				writeD(getHairOption(_activeChar.getHairSkinOption()).getHairId() != 0 ? getHairOption(_activeChar.getHairSkinOption()).getHairId() : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
			}
			
			if (_activeChar.getFaceSkinOption() > 0 && getFaceOption(_activeChar.getFaceSkinOption()) != null)
			{
				writeD(getFaceOption(_activeChar.getFaceSkinOption()).getFaceId() != 0 ? getFaceOption(_activeChar.getFaceSkinOption()).getFaceId() : _activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
			}
		}
		else
		{
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
			writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
		}
		
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
		
		if (Config.ALLOW_DRESS_ME_SYSTEM)
		{
			if (_activeChar.getWeaponSkinOption() > 0 && getWeaponOption(_activeChar.getWeaponSkinOption()) != null)
			{
				writeD(getWeaponOption(_activeChar.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(_activeChar.getWeaponSkinOption()).getWeaponId() : _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			}
			
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			
			if (_activeChar.getArmorSkinOption() > 0 && getArmorOption(_activeChar.getArmorSkinOption()) != null)
			{
				writeD(getArmorOption(_activeChar.getArmorSkinOption()).getGlovesId() != 0 ? getArmorOption(_activeChar.getArmorSkinOption()).getGlovesId() : _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
				writeD(getArmorOption(_activeChar.getArmorSkinOption()).getChestId() != 0 ? getArmorOption(_activeChar.getArmorSkinOption()).getChestId() : _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
				writeD(getArmorOption(_activeChar.getArmorSkinOption()).getLegsId() != 0 ? getArmorOption(_activeChar.getArmorSkinOption()).getLegsId() : _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
				writeD(getArmorOption(_activeChar.getArmorSkinOption()).getFeetId() != 0 ? getArmorOption(_activeChar.getArmorSkinOption()).getFeetId() : _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
				writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
				writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
				writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			}
			
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			
			if (_activeChar.getWeaponSkinOption() > 0 && getWeaponOption(_activeChar.getWeaponSkinOption()) != null)
			{
				writeD(getWeaponOption(_activeChar.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(_activeChar.getWeaponSkinOption()).getWeaponId() : _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			}
			
			if (_activeChar.getHairSkinOption() > 0 && getHairOption(_activeChar.getHairSkinOption()) != null)
			{
				writeD(getHairOption(_activeChar.getHairSkinOption()).getHairId() != 0 ? getHairOption(_activeChar.getHairSkinOption()).getHairId() : _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			}
			
			if (_activeChar.getFaceSkinOption() > 0 && getFaceOption(_activeChar.getFaceSkinOption()) != null)
			{
				writeD(getFaceOption(_activeChar.getFaceSkinOption()).getFaceId() != 0 ? getFaceOption(_activeChar.getFaceSkinOption()).getFaceId() : _activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FACE));
			}
			else
			{
				writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FACE));
			}
		}
		else
		{
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FACE));
		}
		
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		
		writeD(_activeChar.getPAtk(null));
		writeD(_activeChar.getPAtkSpd());
		writeD(_activeChar.getPDef(null));
		writeD(_activeChar.getEvasionRate(null));
		writeD(_activeChar.getAccuracy());
		writeD(_activeChar.getCriticalHit(null, null));
		writeD(_activeChar.getMAtk(null, null));
		writeD(_activeChar.getMAtkSpd());
		writeD(_activeChar.getPAtkSpd());
		writeD(_activeChar.getMDef(null, null));
		writeD(_activeChar.getPvpFlag());
		writeD(_activeChar.getKarma());
		
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd);
		writeD(_swimWalkSpd);
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(_moveMultiplier);
		writeF(_activeChar.getAttackSpeedMultiplier());
		
		final L2Summon pet = _activeChar.getPet();
		if (_activeChar.getMountType() != 0 && pet != null)
		{
			writeF(pet.getTemplate().getCollisionRadius());
			writeF(pet.getTemplate().getCollisionHeight());
		}
		else
		{
			writeF(_activeChar.getCollisionRadius());
			writeF(_activeChar.getCollisionHeight());
		}
		
		writeD(_activeChar.getAppearance().getHairStyle());
		writeD(_activeChar.getAppearance().getHairColor());
		writeD(_activeChar.getAppearance().getFace());
		writeD(_activeChar.isGM() ? 1 : 0); // builder level
		
		String title = _activeChar.getTitle();
		if (_activeChar.getAppearance().getInvisible() && _activeChar.isGM())
		{
			title = "Invisible";
		}
		if (_activeChar.getPoly().isMorphed())
		{
			final L2NpcTemplate polyObj = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
			if (polyObj != null)
			{
				title += " - " + polyObj.name;
			}
		}
		writeS(title);
		
		writeD(_activeChar.getClanId());
		writeD(_activeChar.getClanCrestId());
		writeD(_activeChar.getAllyId());
		writeD(_activeChar.getAllyCrestId()); // ally crest id
		// 0x40 leader rights
		// siege flags: attacker - 0x180 sword over name, defender - 0x80 shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
		writeD(_relation);
		writeC(_activeChar.getMountType()); // mount type
		writeC(_activeChar.getPrivateStoreType());
		writeC(_activeChar.hasDwarvenCraft() ? 1 : 0);
		writeD(_activeChar.getPkKills());
		writeD(_activeChar.getPvpKills());
		
		writeH(_activeChar.getCubics().size());
		for (Integer id : _activeChar.getCubics().keySet())
		{
			writeH(id);
		}
		
		writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);
		
		writeD(_activeChar.getAbnormalEffect());
		
		writeC(_activeChar.isInsideZone(ZoneId.ZONE_WATER) ? 1 : _activeChar.getMountType() == 2 ? 2 : 0);
		writeD(_activeChar.getClanPrivileges());
		
		writeH(_activeChar.getRecomLeft());
		writeH(_activeChar.getRecomHave());
		writeD(0x00); // Mount npc id
		writeH(_activeChar.getInventoryLimit());
		
		writeD(_activeChar.getClassId().getId());
		writeD(0x00); // special effects? circles around player.
		writeD(_activeChar.getMaxCp());
		writeD((int) _activeChar.getCurrentCp());
		
		writeC(_activeChar.isMounted() ? 0 : _activeChar.getEnchantEffect());
		
		if (_activeChar.getTeam() == 1)
		{
			writeC(0x01); // team circle around feet 1= Blue, 2 = red
		}
		else if (_activeChar.getTeam() == 2)
		{
			writeC(0x02); // team circle around feet 1= Blue, 2 = red
		}
		else
		{
			writeC(0x00); // team circle around feet 1= Blue, 2 = red
		}
		
		writeD(_activeChar.getClanCrestLargeId());
		writeC(_activeChar.isNoble() ? 1 : 0); // 0x01: symbol on char menu ctrl+I
		writeC((_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) || _activeChar.getIsPVPHero()) ? 1 : 0); // 0x01: Hero Aura
		
		writeC(_activeChar.isFishing() ? 1 : 0); // Fishing Mode
		writeD(_activeChar.getFishx());
		writeD(_activeChar.getFishy());
		writeD(_activeChar.getFishz());
		
		writeD(_activeChar.getAppearance().getNameColor());
		
		writeC(_activeChar.isRunning() ? 0x01 : 0x00); // changes the Speed display on Status Window
		
		writeD(_activeChar.getPledgeClass()); // changes the text above CP on Status Window
		writeD(_activeChar.getPledgeType());
		
		writeD(_activeChar.getAppearance().getTitleColor());
		
		if (_activeChar.isCursedWeaponEquiped())
		{
			writeD(CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquipedId()));
		}
		else
		{
			writeD(0x00);
		}
	}
	
	public SkinPackage getArmorOption(int option)
	{
		return (DressMeData.getInstance().getArmorSkinsPackage(option));
	}
	
	public SkinPackage getWeaponOption(int option)
	{
		return DressMeData.getInstance().getWeaponSkinsPackage(option);
	}
	
	public SkinPackage getHairOption(int option)
	{
		return DressMeData.getInstance().getHairSkinsPackage(option);
	}
	
	public SkinPackage getFaceOption(int option)
	{
		return DressMeData.getInstance().getFaceSkinsPackage(option);
	}
	
	@Override
	public String getType()
	{
		return _S__04_USERINFO;
	}
}
