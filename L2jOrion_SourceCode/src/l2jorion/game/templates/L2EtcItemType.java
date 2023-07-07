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
package l2jorion.game.templates;

public enum L2EtcItemType
{
	ARROW(0, "Arrow"),
	MATERIAL(1, "Material"),
	PET_COLLAR(2, "PetCollar"),
	POTION(3, "Potion"),
	RECEIPE(4, "Receipe"),
	SCROLL(5, "Scroll"),
	QUEST(6, "Quest"),
	MONEY(7, "Money"),
	OTHER(8, "Other"),
	SPELLBOOK(9, "Spellbook"),
	SEED(10, "Seed"),
	SHOT(11, "Shot"),
	HERB(12, "Herb");
	
	final int _id;
	final String _name;
	
	L2EtcItemType(final int id, final String name)
	{
		_id = id;
		_name = name;
	}
	
	public int mask()
	{
		return 1 << _id + 21;
	}
	
	@Override
	public String toString()
	{
		return _name;
	}
}
