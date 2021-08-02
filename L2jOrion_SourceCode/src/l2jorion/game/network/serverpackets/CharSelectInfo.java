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
import java.util.List;

import javolution.util.FastList;
import l2jorion.Config;
import l2jorion.game.model.CharSelectInfoPackage;
import l2jorion.game.model.Inventory;
import l2jorion.game.model.L2Clan;
import l2jorion.game.model.actor.instance.L2PcInstance;
import l2jorion.game.network.L2GameClient;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;
import l2jorion.util.CloseUtil;
import l2jorion.util.database.DatabaseUtils;
import l2jorion.util.database.L2DatabaseFactory;

public class CharSelectInfo extends L2GameServerPacket
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
			
			final int fakeArmorObjectId = charInfoPackage.getFakeArmorObjectId();
			final int fakeArmorItemId = charInfoPackage.getFakeArmorItemId();
			
			writeS(charInfoPackage.getName());
			writeD(charInfoPackage.getCharId());
			writeS(_loginName);
			writeD(_sessionId);
			writeD(charInfoPackage.getClanId());
			writeD(0x00); // ??
			
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
			
			writeD(0x01); // active ??
			
			writeD(0x00); // x
			writeD(0x00); // y
			writeD(0x00); // z
			
			writeF(charInfoPackage.getCurrentHp()); // hp cur
			writeF(charInfoPackage.getCurrentMp()); // mp cur
			
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
			
			// writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
			writeD(fakeArmorObjectId == 0 ? charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HEAD) : 0);
			
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
			
			// writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
			// writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
			// writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
			// writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			writeD(fakeArmorObjectId == 0 ? charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES) : 0);
			writeD(fakeArmorObjectId == 0 ? charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST) : fakeArmorObjectId);
			writeD(fakeArmorObjectId == 0 ? charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS) : 0);
			writeD(fakeArmorObjectId == 0 ? charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FEET) : 0);
			
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FACE));
			
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_REAR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_NECK));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
			
			// writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			writeD(fakeArmorItemId == 0 ? charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HEAD) : 0);
			
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			
			// writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			// writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			// writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			// writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(fakeArmorItemId == 0 ? charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES) : 0);
			writeD(fakeArmorItemId == 0 ? charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST) : fakeArmorItemId);
			writeD(fakeArmorItemId == 0 ? charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS) : 0);
			writeD(fakeArmorItemId == 0 ? charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET) : 0);
			
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FACE));
			
			writeD(charInfoPackage.getHairStyle());
			writeD(charInfoPackage.getHairColor());
			writeD(charInfoPackage.getFace());
			
			writeF(charInfoPackage.getMaxHp()); // hp max
			writeF(charInfoPackage.getMaxMp()); // mp max
			
			final long deleteTime = charInfoPackage.getDeleteTimer();
			final int accesslevels = charInfoPackage.getAccessLevel();
			int deletedays = 0;
			
			if (deleteTime > 0)
			{
				deletedays = (int) ((deleteTime - System.currentTimeMillis()) / 1000);
			}
			else if (accesslevels < 0)
			{
				deletedays = -1; // like L2OFF player looks dead if he is banned.
			}
			
			writeD(deletedays); // days left before
			// delete .. if != 0
			// then char is inactive
			writeD(charInfoPackage.getClassId());
			
			if (i == _activeId)
			{
				writeD(0x01);
			}
			else
			{
				writeD(0x00); // c3 auto-select char
			}
			
			writeC(charInfoPackage.getEnchantEffect() > 127 ? 127 : charInfoPackage.getEnchantEffect());
			
			writeD(charInfoPackage.getAugmentationId());
		}
	}
	
	private CharSelectInfoPackage[] loadCharacterSelectInfo()
	{
		CharSelectInfoPackage charInfopackage;
		final List<CharSelectInfoPackage> characterList = new FastList<>();
		
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, base_class FROM characters WHERE account_name=?");
			
			if (Config.FAKE_ARMORS)
			{
				statement = con.prepareStatement("SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, base_class, fakeArmorObjectId, fakeArmorItemId, fakeWeaponObjectId, fakeWeaponItemId FROM characters WHERE account_name=?");
			}
			
			statement.setString(1, _loginName);
			final ResultSet charList = statement.executeQuery();
			
			while (charList.next())// fills the package
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
			con = null;
		}
	}
	
	private CharSelectInfoPackage restoreChar(final ResultSet chardata) throws Exception
	{
		final int objectId = chardata.getInt("obj_id");
		
		// See if the char must be deleted
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
		
		if (Config.FAKE_ARMORS)
		{
			charInfopackage.setFakeArmorObjectId(chardata.getInt("fakeArmorObjectId"));
			charInfopackage.setFakeArmorItemId(chardata.getInt("fakeArmorItemId"));
		}
		
		final int baseClassId = chardata.getInt("base_class");
		final int activeClassId = chardata.getInt("classid");
		
		// if is in subclass, load subclass exp, sp, lvl info
		if (baseClassId != activeClassId)
		{
			loadCharacterSubclassInfo(charInfopackage, objectId, activeClassId);
		}
		
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
	
	@Override
	public String getType()
	{
		return _S__1F_CHARSELECTINFO;
	}
}