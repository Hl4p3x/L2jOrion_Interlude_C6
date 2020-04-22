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

/**
 * This class ...
 * @version $Revision: 1.3 $ $Date: 2004/10/23 22:12:44 $
 */
public class L2Macro
{
	public final static int CMD_TYPE_SKILL = 1;
	public final static int CMD_TYPE_ACTION = 3;
	public final static int CMD_TYPE_SHORTCUT = 4;
	
	public int id;
	public final int icon;
	public final String name;
	public final String descr;
	public final String acronym;
	public final L2MacroCmd[] commands;
	
	public static class L2MacroCmd
	{
		public final int entry;
		public final int type;
		public final int d1; // skill_id or page for shortcuts
		public final int d2; // shortcut
		public final String cmd;
		
		public L2MacroCmd(final int pEntry, final int pType, final int pD1, final int pD2, final String pCmd)
		{
			entry = pEntry;
			type = pType;
			d1 = pD1;
			d2 = pD2;
			cmd = pCmd;
		}
	}
	
	/**
	 * @param pId
	 * @param pIcon
	 * @param pName
	 * @param pDescr
	 * @param pAcronym
	 * @param pCommands
	 */
	public L2Macro(final int pId, final int pIcon, final String pName, final String pDescr, final String pAcronym, final L2MacroCmd[] pCommands)
	{
		id = pId;
		icon = pIcon;
		name = pName;
		descr = pDescr;
		acronym = pAcronym;
		commands = pCommands;
	}
}
