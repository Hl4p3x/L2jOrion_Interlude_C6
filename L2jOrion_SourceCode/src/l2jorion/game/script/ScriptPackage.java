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
package l2jorion.game.script;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javolution.util.FastList;

/**
 * @author Luis Arias
 */
public class ScriptPackage
{
	private final List<ScriptDocument> _scriptFiles;
	private final List<String> _otherFiles;
	private final String _name;
	
	public ScriptPackage(final ZipFile pack)
	{
		_scriptFiles = new FastList<>();
		_otherFiles = new FastList<>();
		_name = pack.getName();
		addFiles(pack);
	}
	
	/**
	 * @return Returns the otherFiles.
	 */
	public List<String> getOtherFiles()
	{
		return _otherFiles;
	}
	
	/**
	 * @return Returns the scriptFiles.
	 */
	public List<ScriptDocument> getScriptFiles()
	{
		return _scriptFiles;
	}
	
	/**
	 * @param pack
	 */
	private void addFiles(final ZipFile pack)
	{
		for (final Enumeration<? extends ZipEntry> e = pack.entries(); e.hasMoreElements();)
		{
			final ZipEntry entry = e.nextElement();
			if (entry.getName().endsWith(".xml"))
			{
				try
				{
					final ScriptDocument newScript = new ScriptDocument(entry.getName(), pack.getInputStream(entry));
					_scriptFiles.add(newScript);
				}
				catch (final IOException e1)
				{
					e1.printStackTrace();
				}
			}
			else if (!entry.isDirectory())
			{
				_otherFiles.add(entry.getName());
			}
		}
	}
	
	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return _name;
	}
	
	@Override
	public String toString()
	{
		if (getScriptFiles().isEmpty() && getOtherFiles().isEmpty())
			return "Empty Package.";
		
		String out = "Package Name: " + getName() + "\n";
		
		if (!getScriptFiles().isEmpty())
		{
			out += "Xml Script Files...\n";
			for (final ScriptDocument script : getScriptFiles())
			{
				out += script.getName() + "\n";
			}
		}
		
		if (!getOtherFiles().isEmpty())
		{
			out += "Other Files...\n";
			for (final String fileName : getOtherFiles())
			{
				out += fileName + "\n";
			}
		}
		return out;
	}
}
