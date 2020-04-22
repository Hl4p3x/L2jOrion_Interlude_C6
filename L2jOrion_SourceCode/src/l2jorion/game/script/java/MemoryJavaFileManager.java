/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. 
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met: Redistributions of source code 
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of 
 * conditions and the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of 
 * is contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission. 

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package l2jorion.game.script.java;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

import org.eclipse.jdt.internal.compiler.tool.EclipseFileManager;

public final class MemoryJavaFileManager extends EclipseFileManager
{
	private static final String EXT = ".java";
	protected Map<String, byte[]> classBytes;
	
	public MemoryJavaFileManager()
	{
		super(null, null);
		classBytes = new HashMap<>();
	}
	
	public Map<String, byte[]> getClassBytes()
	{
		return classBytes;
	}
	
	@Override
	public void close()
	{
		classBytes = new HashMap<>();
	}
	
	@Override
	public void flush()
	{
	}
	
	/**
	 * A file object used to represent Java source coming from a string.
	 */
	private static class StringInputBuffer extends SimpleJavaFileObject
	{
		final String code;
		
		StringInputBuffer(String name, String code)
		{
			super(toURI(name), Kind.SOURCE);
			this.code = code;
		}
		
		@Override
		public CharBuffer getCharContent(boolean ignoreEncodingErrors)
		{
			return CharBuffer.wrap(code);
		}
	}
	
	/**
	 * A file object that stores Java bytecode into the classBytes map.
	 */
	private class ClassOutputBuffer extends SimpleJavaFileObject
	{
		protected final String name;
		
		ClassOutputBuffer(String name)
		{
			super(toURI(name), Kind.CLASS);
			this.name = name;
		}
		
		@Override
		public OutputStream openOutputStream()
		{
			return new FilterOutputStream(new ByteArrayOutputStream())
			{
				@Override
				public void close() throws IOException
				{
					out.close();
					ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
					classBytes.put(name, bos.toByteArray());
				}
			};
		}
	}
	
	@Override
	public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, Kind kind, FileObject sibling) throws IOException
	{
		if (kind == Kind.CLASS)
		{
			return new ClassOutputBuffer(className.replace('/', '.'));
		}
		return super.getJavaFileForOutput(location, className, kind, sibling);
	}
	
	static JavaFileObject makeStringSource(String name, String code)
	{
		return new StringInputBuffer(name, code);
	}
	
	static URI toURI(String name)
	{
		File file = new File(name);
		if (file.exists())
		{
			return file.toURI();
		}
		
		try
		{
			final StringBuilder newUri = new StringBuilder();
			newUri.append("file:///");
			newUri.append(name.replace('.', '/'));
			if (name.endsWith(EXT))
			{
				newUri.replace(newUri.length() - EXT.length(), newUri.length(), EXT);
			}
			return URI.create(newUri.toString());
		}
		catch (Exception exp)
		{
			return URI.create("file:///com/sun/script/java/java_source");
		}
	}
}
