/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2jorion.game.datatables;

/**
 * @author FBIagent<br>
 */
public class AccessLevel
{
	/** The logger<br> */
	// private final static Log LOGGER = LogFactory.getLog(AccessLevel.class);
	/** The access level<br> */
	private int _accessLevel = 0;
	/** The access level name<br> */
	private String _name = null;
	/** The name color for the access level<br> */
	private int _nameColor = 0;
	/** The title color for the access level<br> */
	private int _titleColor = 0;
	/** Flag to determine if the access level has gm access<br> */
	private boolean _isGm = false;
	/** Flag for peace zone attack */
	private boolean _allowPeaceAttack = false;
	/** Flag for fixed res */
	private boolean _allowFixedRes = false;
	/** Flag for transactions */
	private boolean _allowTransaction = false;
	/** Flag for AltG commands */
	private boolean _allowAltG = false;
	/** Flag to give damage */
	private boolean _giveDamage = false;
	/** Flag to take aggro */
	private boolean _takeAggro = false;
	/** Flag to gain exp in party */
	private boolean _gainExp = false;
	
	// L2EMU_ ADD - Rayan
	private boolean _useNameColor = true;
	private boolean _useTitleColor = false;
	private boolean _canDisableGmStatus = false;
	
	// L2EMU_ ADD
	/**
	 * Initializes members<br>
	 * <br>
	 * @param accessLevel as int<br>
	 * @param name as String<br>
	 * @param nameColor as int<br>
	 * @param titleColor as int<br>
	 * @param isGm as boolean<br>
	 * @param allowPeaceAttack as boolean<br>
	 * @param allowFixedRes as boolean<br>
	 * @param allowTransaction as boolean<br>
	 * @param allowAltG as boolean<br>
	 * @param giveDamage as boolean<br>
	 * @param takeAggro as boolean<br>
	 * @param gainExp as boolean<br>
	 * @param useNameColor as boolean<br>
	 * @param useTitleColor as boolean<br>
	 * @param canDisableGmStatus
	 */
	public AccessLevel(final int accessLevel, final String name, final int nameColor, final int titleColor, final boolean isGm, final boolean allowPeaceAttack, final boolean allowFixedRes, final boolean allowTransaction, final boolean allowAltG, final boolean giveDamage, final boolean takeAggro, final boolean gainExp, final boolean useNameColor, final boolean useTitleColor, final boolean canDisableGmStatus)
	{
		_accessLevel = accessLevel;
		_name = name;
		_nameColor = nameColor;
		_titleColor = titleColor;
		_isGm = isGm;
		_allowPeaceAttack = allowPeaceAttack;
		_allowFixedRes = allowFixedRes;
		_allowTransaction = allowTransaction;
		_allowAltG = allowAltG;
		_giveDamage = giveDamage;
		_takeAggro = takeAggro;
		_gainExp = gainExp;
		
		// L2EMU_ ADD - Rayan
		_useNameColor = useNameColor;
		_useTitleColor = useTitleColor;
		_canDisableGmStatus = canDisableGmStatus;
		// L2EMU_ ADD
	}
	
	/**
	 * Returns the access level<br>
	 * <br>
	 * @return int: access level<br>
	 */
	public int getLevel()
	{
		return _accessLevel;
	}
	
	/**
	 * Returns the access level name<br>
	 * <br>
	 * @return String: access level name<br>
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Returns the name color of the access level<br>
	 * <br>
	 * @return int: the name color for the access level<br>
	 */
	public int getNameColor()
	{
		return _nameColor;
	}
	
	/**
	 * Returns the title color color of the access level<br>
	 * <br>
	 * @return int: the title color for the access level<br>
	 */
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	/**
	 * Retuns if the access level has gm access or not<br>
	 * <br>
	 * @return boolean: true if access level have gm access, otherwise false<br>
	 */
	public boolean isGm()
	{
		return _isGm;
	}
	
	/**
	 * Returns if the access level is allowed to attack in peace zone or not<br>
	 * <br>
	 * @return boolean: true if the access level is allowed to attack in peace zone, otherwise false<br>
	 */
	public boolean allowPeaceAttack()
	{
		return _allowPeaceAttack;
	}
	
	/**
	 * @return true if the access level is allowed to use fixed res, otherwise false.
	 */
	public boolean allowFixedRes()
	{
		return _allowFixedRes;
	}
	
	/**
	 * Returns if the access level is allowed to perform transactions or not<br>
	 * <br>
	 * @return boolean: true if access level is allowed to perform transactions, otherwise false<br>
	 */
	public boolean allowTransaction()
	{
		return _allowTransaction;
	}
	
	/**
	 * Returns if the access level is allowed to use AltG commands or not<br>
	 * <br>
	 * @return boolean: true if access level is allowed to use AltG commands, otherwise false<br>
	 */
	public boolean allowAltG()
	{
		return _allowAltG;
	}
	
	/**
	 * Returns if the access level can give damage or not<br>
	 * <br>
	 * @return boolean: true if the access level can give damage, otherwise false<br>
	 */
	public boolean canGiveDamage()
	{
		return _giveDamage;
	}
	
	/**
	 * Returns if the access level can take aggro or not<br>
	 * <br>
	 * @return boolean: true if the access level can take aggro, otherwise false<br>
	 */
	public boolean canTakeAggro()
	{
		return _takeAggro;
	}
	
	/**
	 * Returns if the access level can gain exp or not<br>
	 * <br>
	 * @return boolean: true if the access level can gain exp, otherwise false<br>
	 */
	public boolean canGainExp()
	{
		return _gainExp;
	}
	
	public boolean useNameColor()
	{
		return _useNameColor;
	}
	
	public boolean useTitleColor()
	{
		return _useTitleColor;
	}
	
	/**
	 * Retuns if the access level is a gm that can temp disable gm access<br>
	 * <br>
	 * @return boolean: true if is a gm that can temp disable gm access, otherwise false<br>
	 */
	public boolean canDisableGmStatus()
	{
		return _canDisableGmStatus;
	}
}
