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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import l2jorion.Config;
import l2jorion.game.datatables.xml.DressMeData;
import l2jorion.game.model.CharSelectInfoPackage;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.model.base.SkinPackage;
import l2jorion.game.network.L2GameClient;
import l2jorion.game.network.PacketServer;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class CharSelectInfo extends PacketServer
{
	private static final String _S__1F_CHARSELECTINFO = "[S] 1F CharSelectInfo";
	
	private static Logger LOG = LoggerFactory.getLogger(CharSelectInfo.class);
	
	private final String _loginName;
	private final int _sessionId;
	private int _activeId;
	
	private final CharSelectInfoPackage[] _characterPackages;
	
	public CharSelectInfo(final String loginName, final int sessionId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo();
		_activeId = -1;
	}
	
	public CharSelectInfo(final String loginName, final int sessionId, final int activeId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo();
		_activeId = activeId;
	}
	
	public CharSelectInfoPackage[] getCharInfo()
	{
		return _characterPackages;
	}
	
	@Override
	protected final void writeImpl()
	{
		final int size = _characterPackages.length;
		
		writeC(0x13);
		writeD(size);
		
		long lastAccess = 0L;
		
		if (_activeId == -1)
		{
			for (int i = 0; i < size; i++)
			{
				if (lastAccess < _characterPackages[i].getLastAccess())
				{
					lastAccess = _characterPackages[i].getLastAccess();
					_activeId = i;
				}
			}
		}
		
		for (int i = 0; i < size; i++)
		{
			final CharSelectInfoPackage charInfoPackage = _characterPackages[i];
			
			writeS(charInfoPackage.getName());
			writeD(charInfoPackage.getCharId());
			writeS(_loginName);
			writeD(_sessionId);
			writeD(charInfoPackage.getClanId());
			writeD(0x00);
			
			writeD(charInfoPackage.getSex());
			writeD(charInfoPackage.getRace());
			
			if (charInfoPackage.getClassId() == charInfoPackage.getBaseClassId())
			{
				writeD(charInfoPackage.getClassId());
			}
			else
			{
				writeD(charInfoPackage.getBaseClassId());
			}
			
			writeD(0x01);
			
			writeD(0x00); // x
			writeD(0x00); // y
			writeD(0x00); // z
			
			writeF(charInfoPackage.getCurrentHp());
			writeF(charInfoPackage.getCurrentMp());
			
			writeD(charInfoPackage.getSp());
			writeQ(charInfoPackage.getExp());
			writeD(charInfoPackage.getLevel());
			
			writeD(charInfoPackage.getKarma()); // karma
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_DHAIR));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
			
			if (Config.ALLOW_DRESS_ME_SYSTEM)
			{
				if (charInfoPackage.getWeaponSkinOption() > 0 && getWeaponOption(charInfoPackage.getWeaponSkinOption()) != null)
				{
					writeD(getWeaponOption(charInfoPackage.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(charInfoPackage.getWeaponSkinOption()).getWeaponId() : charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
				}
				
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
				
				if (charInfoPackage.getArmorSkinOption() > 0 && getArmorOption(charInfoPackage.getArmorSkinOption()) != null)
				{
					writeD(getArmorOption(charInfoPackage.getArmorSkinOption()).getGlovesId() != 0 ? getArmorOption(charInfoPackage.getArmorSkinOption()).getGlovesId() : charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
					writeD(getArmorOption(charInfoPackage.getArmorSkinOption()).getChestId() != 0 ? getArmorOption(charInfoPackage.getArmorSkinOption()).getChestId() : charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
					writeD(getArmorOption(charInfoPackage.getArmorSkinOption()).getLegsId() != 0 ? getArmorOption(charInfoPackage.getArmorSkinOption()).getLegsId() : charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
					writeD(getArmorOption(charInfoPackage.getArmorSkinOption()).getFeetId() != 0 ? getArmorOption(charInfoPackage.getArmorSkinOption()).getFeetId() : charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
					writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
					writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
					writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
				}
				
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
				
				if (charInfoPackage.getWeaponSkinOption() > 0 && getWeaponOption(charInfoPackage.getWeaponSkinOption()) != null)
				{
					writeD(getWeaponOption(charInfoPackage.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(charInfoPackage.getWeaponSkinOption()).getWeaponId() : charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
				}
				
				if (charInfoPackage.getHairSkinOption() > 0 && getHairOption(charInfoPackage.getHairSkinOption()) != null)
				{
					writeD(getHairOption(charInfoPackage.getHairSkinOption()).getHairId() != 0 ? getHairOption(charInfoPackage.getHairSkinOption()).getHairId() : charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
				}
				
				if (charInfoPackage.getFaceSkinOption() > 0 && getFaceOption(charInfoPackage.getFaceSkinOption()) != null)
				{
					writeD(getFaceOption(charInfoPackage.getFaceSkinOption()).getFaceId() != 0 ? getFaceOption(charInfoPackage.getFaceSkinOption()).getFaceId() : charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
				}
			}
			else
			{
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
				writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
			}
			
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_REAR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_NECK));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			
			if (Config.ALLOW_DRESS_ME_SYSTEM)
			{
				if (charInfoPackage.getWeaponSkinOption() > 0 && getWeaponOption(charInfoPackage.getWeaponSkinOption()) != null)
				{
					writeD(getWeaponOption(charInfoPackage.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(charInfoPackage.getWeaponSkinOption()).getWeaponId() : charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
				}
				
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
				
				if (charInfoPackage.getArmorSkinOption() > 0 && getArmorOption(charInfoPackage.getArmorSkinOption()) != null)
				{
					writeD(getArmorOption(charInfoPackage.getArmorSkinOption()).getGlovesId() != 0 ? getArmorOption(charInfoPackage.getArmorSkinOption()).getGlovesId() : charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
					writeD(getArmorOption(charInfoPackage.getArmorSkinOption()).getChestId() != 0 ? getArmorOption(charInfoPackage.getArmorSkinOption()).getChestId() : charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
					writeD(getArmorOption(charInfoPackage.getArmorSkinOption()).getLegsId() != 0 ? getArmorOption(charInfoPackage.getArmorSkinOption()).getLegsId() : charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
					writeD(getArmorOption(charInfoPackage.getArmorSkinOption()).getFeetId() != 0 ? getArmorOption(charInfoPackage.getArmorSkinOption()).getFeetId() : charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
					writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
					writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
					writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
				}
				
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
				
				if (charInfoPackage.getWeaponSkinOption() > 0 && getWeaponOption(charInfoPackage.getWeaponSkinOption()) != null)
				{
					writeD(getWeaponOption(charInfoPackage.getWeaponSkinOption()).getWeaponId() != 0 ? getWeaponOption(charInfoPackage.getWeaponSkinOption()).getWeaponId() : charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
				}
				
				if (charInfoPackage.getHairSkinOption() > 0 && getHairOption(charInfoPackage.getHairSkinOption()) != null)
				{
					writeD(getHairOption(charInfoPackage.getHairSkinOption()).getHairId() != 0 ? getHairOption(charInfoPackage.getHairSkinOption()).getHairId() : charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
				}
				
				if (charInfoPackage.getFaceSkinOption() > 0 && getFaceOption(charInfoPackage.getFaceSkinOption()) != null)
				{
					writeD(getFaceOption(charInfoPackage.getFaceSkinOption()).getFaceId() != 0 ? getFaceOption(charInfoPackage.getFaceSkinOption()).getFaceId() : charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FACE));
				}
				else
				{
					writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FACE));
				}
			}
			else
			{
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
				writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FACE));
			}
			
			writeD(charInfoPackage.getHairStyle());
			writeD(charInfoPackage.getHairColor());
			writeD(charInfoPackage.getFace());
			
			writeF(charInfoPackage.getMaxHp()); // Hp max
			writeF(charInfoPackage.getMaxMp()); // Mp max
			
			final long deleteTime = charInfoPackage.getDeleteTimer();
			final int accesslevels = charInfoPackage.getAccessLevel();
			int deletedays = 0;
			if (deleteTime > 0)
			{
				deletedays = (int) ((deleteTime - System.currentTimeMillis()) / 1000);
			}
			else if (accesslevels < 0)
			{
				deletedays = -1; // Like L2OFF player looks dead if he is banned.
			}
			writeD(deletedays); // Days left before
			
			writeD(charInfoPackage.getClassId());
			
			if (i == _activeId)
			{
				writeD(0x01);
			}
			else
			{
				writeD(0x00); // Auto-select character
			}
			
			writeC(charInfoPackage.getEnchantEffect() > 127 ? 127 : charInfoPackage.getEnchantEffect());
			writeD(charInfoPackage.getAugmentationId());
		}
	}
	
	private CharSelectInfoPackage[] loadCharacterSelectInfo()
	{
		CharSelectInfoPackage charInfopackage;
		final List<CharSelectInfoPackage> characterList = new LinkedList<>();
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, base_class FROM characters WHERE account_name=?");
			
			statement.setString(1, _loginName);
			final ResultSet charList = statement.executeQuery();
			
			while (charList.next()) // Fills the package
			{
				charInfopackage = restoreChar(charList);
				if (charInfopackage != null)
				{
					characterList.add(charInfopackage);
				}
			}
			
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
		
		return characterList.toArray(new CharSelectInfoPackage[characterList.size()]);
	}
	
	private void loadCharacterSubclassInfo(final CharSelectInfoPackage charInfopackage, final int ObjectId, final int activeClassId)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT exp, sp, level FROM character_subclasses WHERE char_obj_id=? && class_id=? ORDER BY char_obj_id");
			statement.setInt(1, ObjectId);
			statement.setInt(2, activeClassId);
			final ResultSet charList = statement.executeQuery();
			
			if (charList.next())
			{
				charInfopackage.setExp(charList.getLong("exp"));
				charInfopackage.setSp(charList.getInt("sp"));
				charInfopackage.setLevel(charList.getInt("level"));
			}
			
			charList.close();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private void loadCharacterDressMeInfo(final CharSelectInfoPackage charInfopackage, final int ObjectId)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement("SELECT obj_Id, armor_skins, armor_skin_option, weapon_skins, weapon_skin_option, hair_skins, hair_skin_option, face_skins, face_skin_option FROM characters_dressme_data WHERE obj_id=?");
			
			statement.setInt(1, ObjectId);
			final ResultSet chardata = statement.executeQuery();
			
			if (chardata.next())
			{
				charInfopackage.setArmorSkinOption(chardata.getInt("armor_skin_option"));
				charInfopackage.setWeaponSkinOption(chardata.getInt("weapon_skin_option"));
				charInfopackage.setHairSkinOption(chardata.getInt("hair_skin_option"));
				charInfopackage.setFaceSkinOption(chardata.getInt("face_skin_option"));
			}
			
			chardata.close();
			DatabaseUtils.close(statement);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			CloseUtil.close(con);
		}
	}
	
	private CharSelectInfoPackage restoreChar(final ResultSet chardata) throws Exception
	{
		final int objectId = chardata.getInt("obj_id");
		
		final long deletetime = chardata.getLong("deletetime");
		if (deletetime > 0)
		{
			if (System.currentTimeMillis() > deletetime)
			{
				final L2PcInstance cha = L2PcInstance.load(objectId);
				final L2Clan clan = cha.getClan();
				if (clan != null)
				{
					clan.removeClanMember(cha.getName(), 0);
				}
				
				L2GameClient.deleteCharByObjId(objectId);
				return null;
			}
		}
		
		final String name = chardata.getString("char_name");
		
		final CharSelectInfoPackage charInfopackage = new CharSelectInfoPackage(objectId, name);
		charInfopackage.setLevel(chardata.getInt("level"));
		charInfopackage.setMaxHp(chardata.getInt("maxhp"));
		charInfopackage.setCurrentHp(chardata.getDouble("curhp"));
		charInfopackage.setMaxMp(chardata.getInt("maxmp"));
		charInfopackage.setCurrentMp(chardata.getDouble("curmp"));
		charInfopackage.setKarma(chardata.getInt("karma"));
		
		charInfopackage.setFace(chardata.getInt("face"));
		charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
		charInfopackage.setHairColor(chardata.getInt("haircolor"));
		charInfopackage.setSex(chardata.getInt("sex"));
		
		charInfopackage.setExp(chardata.getLong("exp"));
		charInfopackage.setSp(chardata.getInt("sp"));
		charInfopackage.setClanId(chardata.getInt("clanid"));
		
		charInfopackage.setRace(chardata.getInt("race"));
		
		charInfopackage.setAccessLevel(chardata.getInt("accesslevel"));
		
		final int baseClassId = chardata.getInt("base_class");
		final int activeClassId = chardata.getInt("classid");
		
		// if is in subclass, load subclass exp, sp, lvl info
		if (baseClassId != activeClassId)
		{
			loadCharacterSubclassInfo(charInfopackage, objectId, activeClassId);
		}
		
		loadCharacterDressMeInfo(charInfopackage, objectId);
		
		charInfopackage.setClassId(activeClassId);
		
		// Get the augmentation id for equipped weapon
		int weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND);
		if (weaponObjId < 1)
		{
			weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
		}
		
		if (weaponObjId > 0)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				final PreparedStatement statement = con.prepareStatement("SELECT attributes FROM augmentations WHERE item_id=?");
				statement.setInt(1, weaponObjId);
				final ResultSet result = statement.executeQuery();
				
				if (result.next())
				{
					charInfopackage.setAugmentationId(result.getInt("attributes"));
				}
				
				result.close();
				DatabaseUtils.close(statement);
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
				
				LOG.warn("Could not restore augmentation info: " + e);
			}
			finally
			{
				CloseUtil.close(con);
			}
		}
		
		if (baseClassId == 0 && activeClassId > 0)
		{
			charInfopackage.setBaseClassId(activeClassId);
		}
		else
		{
			charInfopackage.setBaseClassId(baseClassId);
		}
		
		charInfopackage.setDeleteTimer(deletetime);
		charInfopackage.setLastAccess(chardata.getLong("lastAccess"));
		
		return charInfopackage;
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
		return _S__1F_CHARSELECTINFO;
	}
}