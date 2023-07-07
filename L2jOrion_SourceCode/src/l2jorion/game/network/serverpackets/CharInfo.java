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

import java.util.Map;
import java.util.Set;

import l2jorion.Config;
import l2jorion.game.datatables.sql.NpcTable;
import l2jorion.game.datatables.xml.DressMeData;
import l2jorion.game.managers.CursedWeaponsManager;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Character;
import l2jorion.game.model.actor.instance.L2CubicInstance;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.SkinPackage;
import l2jorion.game.model.zone.ZoneId;
import l2jorion.game.network.PacketServer;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class CharInfo extends PacketServer
{
	private static final Logger LOG = LoggerFactory.getLogger(CharInfo.class);
	
	private static final String _S__03_CHARINFO = "[S] 03 CharInfo";
	
	private L2PcInstance _activeChar;
	
	private int _objId;
	private int _x, _y, _z, _heading;
	
	private Inventory _inv;
	private int _mAtkSpd, _pAtkSpd;
	private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd;
	
	private int _flRunSpd;
	private int _flWalkSpd;
	private int _flyRunSpd;
	
	private int _flyWalkSpd;
	private float _moveMultiplier, _attackSpeedMultiplier;
	private int _maxCp;
	
	private String _name;
	private String _title;
	private int _nameColor;
	private int _titleColor;
	private int _clanId;
	private int _clanCrestId;
	private int _allyId;
	private int _allyCrestId;
	
	// Other characters info
	public CharInfo(L2PcInstance player)
	{
		_activeChar = player;
		
		_objId = player.getObjectId();
		_x = player.getX();
		_y = player.getY();
		_z = player.getZ();
		_heading = player.getHeading();
		
		_inv = player.getInventory();
		_mAtkSpd = player.getMAtkSpd();
		_pAtkSpd = player.getPAtkSpd();
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_attackSpeedMultiplier = player.getAttackSpeedMultiplier();
		_runSpd = (int) (player.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (player.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
		_maxCp = player.getMaxCp();
		
		if (!hideInfo())
		{
			_name = player.getName();
			
			if (player.getAppearance().getInvisible())
			{
				_title = "Invisible";
			}
			else
			{
				
				_title = _activeChar.getTitle();
			}
			
			_nameColor = player.getAppearance().getNameColor();
			_titleColor = player.getAppearance().getTitleColor();
			_clanId = player.getClanId();
			_clanCrestId = player.getClanCrestId();
			_allyId = player.getAllyId();
			_allyCrestId = player.getAllyCrestId();
		}
		else
		{
			_name = Config.PLAYER_FAKE_NAME;
			_title = "";
			_nameColor = 0xFFFFFF;
			_titleColor = 0xFFFF77;
			_clanId = 0;
			_clanCrestId = 0;
			_allyId = 0;
			_allyCrestId = 0;
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_activeChar.getPoly().isMorphed())
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
			
			if (template != null)
			{
				writeC(0x16);
				writeD(_objId);
				writeD(_activeChar.getPoly().getPolyId() + 1000000); // npctype id
				writeD(_activeChar.getKarma() > 0 ? 1 : 0);
				writeD(_x);
				writeD(_y);
				writeD(_z);
				writeD(_heading);
				writeD(0x00);
				writeD(_mAtkSpd);
				writeD(_pAtkSpd);
				writeD(_runSpd);
				writeD(_walkSpd);
				writeD(_swimRunSpd); // swimspeed
				writeD(_swimWalkSpd); // swimspeed
				writeD(_flRunSpd);
				writeD(_flWalkSpd);
				writeD(_flyRunSpd);
				writeD(_flyWalkSpd);
				writeF(_moveMultiplier);
				writeF(_attackSpeedMultiplier);
				writeF(template.collisionRadius);
				writeF(template.collisionHeight);
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND)); // right hand weapon
				writeD(0);
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND)); // left hand weapon
				writeC(1); // name above char 1 = true
				writeC(_activeChar.isRunning() ? 1 : 0);
				writeC(_activeChar.isInCombat() ? 1 : 0);
				writeC(_activeChar.isAlikeDead() ? 1 : 0);
				writeC(0); // if the charinfo is written means receiver can see the char
				
				writeS(_name);
				writeS(_title);
				
				writeD(0);
				writeD(0);
				writeD(0000);
				
				if (_activeChar.getAppearance().getInvisible())
				{
					writeD((_activeChar.getAbnormalEffect() | L2Character.ABNORMAL_EFFECT_STEALTH));
				}
				else
				{
					writeD(_activeChar.getAbnormalEffect()); // C2
				}
				
				writeD(0); // C2
				writeD(0); // C2
				writeD(0); // C2
				writeD(0); // C2
				writeC(0); // C2
			}
			else
			{
				LOG.warn("Character " + _activeChar.getName() + " (" + _activeChar.getObjectId() + ") morphed in a Npc (" + _activeChar.getPoly().getPolyId() + ") w/o template.");
			}
		}
		else
		{
			writeC(0x03);
			
			writeD(_x);
			writeD(_y);
			writeD(_z);
			
			writeD(0); // it's _vehicleId, but we don't have it yet
			
			writeD(_objId);
			writeS(_name);
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
			
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			
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
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FACE));
			}
			
			// c6 new h's
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
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
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			
			writeD(_activeChar.getPvpFlag());
			writeD(_activeChar.getKarma());
			
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			
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
			writeF(_activeChar.getMovementSpeedMultiplier());
			writeF(_activeChar.getAttackSpeedMultiplier());
			
			writeF(_activeChar.getCollisionRadius());
			writeF(_activeChar.getCollisionHeight());
			
			writeD(_activeChar.getAppearance().getHairStyle());
			writeD(_activeChar.getAppearance().getHairColor());
			writeD(_activeChar.getAppearance().getFace());
			
			writeS(_title);
			
			writeD(_clanId);
			writeD(_clanCrestId);
			writeD(_allyId);
			writeD(_allyCrestId);
			
			writeD(0);
			
			writeC(_activeChar.isSitting() ? 0 : 1); // standing = 1 sitting = 0
			writeC(_activeChar.isRunning() ? 1 : 0); // running = 1 walking = 0
			writeC(_activeChar.isInCombat() ? 1 : 0);
			writeC(_activeChar.isAlikeDead() ? 1 : 0);
			
			writeC(0); // if the charinfo is written means receiver can see the char
			
			writeC(_activeChar.getMountType()); // 1 on strider 2 on wyvern 0 no mount
			writeC(_activeChar.getPrivateStoreType()); // 1 - sellshop
			
			final Map<Integer, L2CubicInstance> cubics = _activeChar.getCubics();
			final Set<Integer> cubicsIds = cubics.keySet();
			
			writeH(cubicsIds.size());
			for (final Integer id : cubicsIds)
			{
				if (id != null)
				{
					writeH(id);
				}
			}
			
			writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);
			
			if (_activeChar.getAppearance().getInvisible())
			{
				writeD((_activeChar.getAbnormalEffect() | L2Character.ABNORMAL_EFFECT_STEALTH));
			}
			else
			{
				writeD(_activeChar.getAbnormalEffect());
			}
			
			writeC(_activeChar.getRecomLeft());
			writeH(_activeChar.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
			writeD(_activeChar.getClassId().getId());
			
			writeD(_maxCp);
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
			writeC(_activeChar.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
			writeC((_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) || _activeChar.getIsPVPHero()) ? 1 : 0); // Hero Aura
			
			writeC(_activeChar.isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
			writeD(_activeChar.getFishx());
			writeD(_activeChar.getFishy());
			writeD(_activeChar.getFishz());
			
			writeD(_nameColor);
			
			writeD(_heading);
			
			writeD(_activeChar.getPledgeClass());
			writeD(_activeChar.getPledgeType());
			
			writeD(_titleColor);
			
			if (_activeChar.isCursedWeaponEquiped())
			{
				writeD(CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquipedId()));
			}
			else
			{
				writeD(0x00);
			}
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
	
	public boolean hideInfo()
	{
		if (Config.HIDE_PLAYER_INFO)
		{
			if (_activeChar.isInsideZone(ZoneId.ZONE_RANDOM))
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String getType()
	{
		return _S__03_CHARINFO;
	}
}
