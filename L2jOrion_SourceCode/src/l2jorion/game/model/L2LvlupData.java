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
package l2jorion.game.model;

public class L2LvlupData
{
	private int _classid;
	private int _classLvl;
	private float _classHpAdd;
	private float _classHpBase;
	private float _classHpModifier;
	private float _classCpAdd;
	private float _classCpBase;
	private float _classCpModifier;
	private float _classMpAdd;
	private float _classMpBase;
	private float _classMpModifier;
	
	/**
	 * @return Returns the _classHpAdd.
	 */
	@Deprecated
	public float getClassHpAdd()
	{
		return _classHpAdd;
	}
	
	/**
	 * @param hpAdd The _classHpAdd to set.
	 */
	public void setClassHpAdd(final float hpAdd)
	{
		_classHpAdd = hpAdd;
	}
	
	/**
	 * @return Returns the _classHpBase.
	 */
	@Deprecated
	public float getClassHpBase()
	{
		return _classHpBase;
	}
	
	/**
	 * @param hpBase The _classHpBase to set.
	 */
	public void setClassHpBase(final float hpBase)
	{
		_classHpBase = hpBase;
	}
	
	/**
	 * @return Returns the _classHpModifier.
	 */
	@Deprecated
	public float getClassHpModifier()
	{
		return _classHpModifier;
	}
	
	/**
	 * @param hpModifier The _classHpModifier to set.
	 */
	public void setClassHpModifier(final float hpModifier)
	{
		_classHpModifier = hpModifier;
	}
	
	/**
	 * @return Returns the _classCpAdd.
	 */
	@Deprecated
	public float getClassCpAdd()
	{
		return _classCpAdd;
	}
	
	/**
	 * @param cpAdd
	 */
	public void setClassCpAdd(final float cpAdd)
	{
		_classCpAdd = cpAdd;
	}
	
	/**
	 * @return Returns the _classCpBase.
	 */
	@Deprecated
	public float getClassCpBase()
	{
		return _classCpBase;
	}
	
	/**
	 * @param cpBase
	 */
	public void setClassCpBase(final float cpBase)
	{
		_classCpBase = cpBase;
	}
	
	/**
	 * @return Returns the _classCpModifier.
	 */
	@Deprecated
	public float getClassCpModifier()
	{
		return _classCpModifier;
	}
	
	/**
	 * @param cpModifier The _classCpModifier to set.
	 */
	public void setClassCpModifier(final float cpModifier)
	{
		_classCpModifier = cpModifier;
	}
	
	/**
	 * @return Returns the _classid.
	 */
	public int getClassid()
	{
		return _classid;
	}
	
	/**
	 * @param pClassid
	 */
	public void setClassid(final int pClassid)
	{
		_classid = pClassid;
	}
	
	/**
	 * @return Returns the _classLvl.
	 */
	@Deprecated
	public int getClassLvl()
	{
		return _classLvl;
	}
	
	/**
	 * @param lvl The _classLvl to set.
	 */
	public void setClassLvl(final int lvl)
	{
		_classLvl = lvl;
	}
	
	/**
	 * @return Returns the _classMpAdd.
	 */
	@Deprecated
	public float getClassMpAdd()
	{
		return _classMpAdd;
	}
	
	/**
	 * @param mpAdd The _classMpAdd to set.
	 */
	public void setClassMpAdd(final float mpAdd)
	{
		_classMpAdd = mpAdd;
	}
	
	/**
	 * @return Returns the _classMpBase.
	 */
	@Deprecated
	public float getClassMpBase()
	{
		return _classMpBase;
	}
	
	/**
	 * @param mpBase The _classMpBase to set.
	 */
	public void setClassMpBase(final float mpBase)
	{
		_classMpBase = mpBase;
	}
	
	/**
	 * @return Returns the _classMpModifier.
	 */
	@Deprecated
	public float getClassMpModifier()
	{
		return _classMpModifier;
	}
	
	/**
	 * @param mpModifier The _classMpModifier to set.
	 */
	public void setClassMpModifier(final float mpModifier)
	{
		_classMpModifier = mpModifier;
	}
}
