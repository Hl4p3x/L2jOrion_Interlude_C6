package l2jorion.crypt;

import l2jorion.Config;
import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class Base64
{
	public final static Logger LOG = LoggerFactory.getLogger(Base64.class);
	
	public final static int NO_OPTIONS = 0;
	public final static int ENCODE = 1;
	public final static int DECODE = 0;
	public final static int GZIP = 2;
	public final static int DONT_BREAK_LINES = 8;
	private final static int MAX_LINE_LENGTH = 76;
	private final static byte EQUALS_SIGN = (byte) '=';
	private final static byte NEW_LINE = (byte) '\n';
	
	private final static String PREFERRED_ENCODING = "UTF-8";
	
	private final static byte[] ALPHABET;
	private final static byte[] _NATIVE_ALPHABET =
	{
		(byte) 'A',
		(byte) 'B',
		(byte) 'C',
		(byte) 'D',
		(byte) 'E',
		(byte) 'F',
		(byte) 'G',
		(byte) 'H',
		(byte) 'I',
		(byte) 'J',
		(byte) 'K',
		(byte) 'L',
		(byte) 'M',
		(byte) 'N',
		(byte) 'O',
		(byte) 'P',
		(byte) 'Q',
		(byte) 'R',
		(byte) 'S',
		(byte) 'T',
		(byte) 'U',
		(byte) 'V',
		(byte) 'W',
		(byte) 'X',
		(byte) 'Y',
		(byte) 'Z',
		(byte) 'a',
		(byte) 'b',
		(byte) 'c',
		(byte) 'd',
		(byte) 'e',
		(byte) 'f',
		(byte) 'g',
		(byte) 'h',
		(byte) 'i',
		(byte) 'j',
		(byte) 'k',
		(byte) 'l',
		(byte) 'm',
		(byte) 'n',
		(byte) 'o',
		(byte) 'p',
		(byte) 'q',
		(byte) 'r',
		(byte) 's',
		(byte) 't',
		(byte) 'u',
		(byte) 'v',
		(byte) 'w',
		(byte) 'x',
		(byte) 'y',
		(byte) 'z',
		(byte) '0',
		(byte) '1',
		(byte) '2',
		(byte) '3',
		(byte) '4',
		(byte) '5',
		(byte) '6',
		(byte) '7',
		(byte) '8',
		(byte) '9',
		(byte) '+',
		(byte) '/'
	};
	
	static
	{
		byte[] __bytes;
		try
		{
			__bytes = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(PREFERRED_ENCODING);
		}
		catch (final java.io.UnsupportedEncodingException use)
		{
			__bytes = _NATIVE_ALPHABET;
		}
		ALPHABET = __bytes;
	}
	
	final static byte[] DECODABET =
	{
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 0 - 8
		-5,
		-5, // Whitespace: Tab and Linefeed
		-9,
		-9, // Decimal 11 - 12
		-5, // Whitespace: Carriage Return
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 14 - 26
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 27 - 31
		-5, // Whitespace: Space
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 33 - 42
		62, // Plus sign at decimal 43
		-9,
		-9,
		-9, // Decimal 44 - 46
		63, // Slash at decimal 47
		52,
		53,
		54,
		55,
		56,
		57,
		58,
		59,
		60,
		61, // Numbers zero through nine
		-9,
		-9,
		-9, // Decimal 58 - 60
		-1, // Equals sign at decimal 61
		-9,
		-9,
		-9, // Decimal 62 - 64
		0,
		1,
		2,
		3,
		4,
		5,
		6,
		7,
		8,
		9,
		10,
		11,
		12,
		13, // Letters 'A' through 'N'
		14,
		15,
		16,
		17,
		18,
		19,
		20,
		21,
		22,
		23,
		24,
		25, // Letters 'O' through 'Z'
		-9,
		-9,
		-9,
		-9,
		-9,
		-9, // Decimal 91 - 96
		26,
		27,
		28,
		29,
		30,
		31,
		32,
		33,
		34,
		35,
		36,
		37,
		38, // Letters 'a' through 'm'
		39,
		40,
		41,
		42,
		43,
		44,
		45,
		46,
		47,
		48,
		49,
		50,
		51,
		-9,
		-9,
		-9,
		-9
	};
	
	private final static byte WHITE_SPACE_ENC = -5; // Indicates white space in encoding
	private final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign in encoding
	
	/** Defeats instantiation. */
	private Base64()
	{
	}
	
	static byte[] encode3to4(final byte[] b4, final byte[] threeBytes, final int numSigBytes)
	{
		encode3to4(threeBytes, 0, numSigBytes, b4, 0);
		return b4;
	}
	
	static byte[] encode3to4(final byte[] source, final int srcOffset, final int numSigBytes, final byte[] destination, final int destOffset)
	{
		final int inBuff = (numSigBytes > 0 ? source[srcOffset] << 24 >>> 8 : 0) | (numSigBytes > 1 ? source[srcOffset + 1] << 24 >>> 16 : 0) | (numSigBytes > 2 ? source[srcOffset + 2] << 24 >>> 24 : 0);
		
		switch (numSigBytes)
		{
			case 3:
				destination[destOffset] = ALPHABET[(inBuff >>> 18)];
				destination[destOffset + 1] = ALPHABET[inBuff >>> 12 & 0x3f];
				destination[destOffset + 2] = ALPHABET[inBuff >>> 6 & 0x3f];
				destination[destOffset + 3] = ALPHABET[inBuff & 0x3f];
				return destination;
			
			case 2:
				destination[destOffset] = ALPHABET[(inBuff >>> 18)];
				destination[destOffset + 1] = ALPHABET[inBuff >>> 12 & 0x3f];
				destination[destOffset + 2] = ALPHABET[inBuff >>> 6 & 0x3f];
				destination[destOffset + 3] = EQUALS_SIGN;
				return destination;
			
			case 1:
				destination[destOffset] = ALPHABET[(inBuff >>> 18)];
				destination[destOffset + 1] = ALPHABET[inBuff >>> 12 & 0x3f];
				destination[destOffset + 2] = EQUALS_SIGN;
				destination[destOffset + 3] = EQUALS_SIGN;
				return destination;
			
			default:
				return destination;
		}
	}
	
	public static String encodeObject(final java.io.Serializable serializableObject)
	{
		return encodeObject(serializableObject, NO_OPTIONS);
	}
	
	public static String encodeObject(final java.io.Serializable serializableObject, final int options)
	{
		java.io.ByteArrayOutputStream baos = null;
		java.io.OutputStream b64os = null;
		java.io.ObjectOutputStream oos = null;
		java.util.zip.GZIPOutputStream gzos = null;
		
		// Isolate options
		final int gzip = options & GZIP;
		final int dontBreakLines = options & DONT_BREAK_LINES;
		
		try
		{
			// ObjectOutputStream -> (GZIP) -> Base64 -> ByteArrayOutputStream
			baos = new java.io.ByteArrayOutputStream();
			b64os = new Base64.OutputStream(baos, ENCODE | dontBreakLines);
			
			// GZip?
			if (gzip == GZIP)
			{
				gzos = new java.util.zip.GZIPOutputStream(b64os);
				oos = new java.io.ObjectOutputStream(gzos);
			} // end if: gzip
			else
			{
				oos = new java.io.ObjectOutputStream(b64os);
			}
			
			oos.writeObject(serializableObject);
		} // end try
		catch (final java.io.IOException e)
		{
			e.printStackTrace();
			return null;
		} // end catch
		finally
		{
			try
			{
				if (oos != null)
				{
					oos.close();
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			try
			{
				if (gzos != null)
				{
					gzos.close();
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			try
			{
				if (b64os != null)
				{
					b64os.close();
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
			try
			{
				if (baos != null)
				{
					baos.close();
				}
			}
			catch (final Exception e)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					e.printStackTrace();
				}
			}
		} // end finally
		
		// Return value according to relevant encoding.
		if (baos != null)
		{
			try
			{
				return new String(baos.toByteArray(), PREFERRED_ENCODING);
			} // end try
			catch (final java.io.UnsupportedEncodingException uue)
			{
				if (Config.ENABLE_ALL_EXCEPTIONS)
				{
					uue.printStackTrace();
				}
				
				return new String(baos.toByteArray());
			} // end catch
		}
		return null;
	} // end encode
	
	public static String encodeBytes(final byte[] source)
	{
		return encodeBytes(source, 0, source.length, NO_OPTIONS);
	}
	
	public static String encodeBytes(final byte[] source, final int options)
	{
		return encodeBytes(source, 0, source.length, options);
	}
	
	public static String encodeBytes(final byte[] source, final int off, final int len)
	{
		return encodeBytes(source, off, len, NO_OPTIONS);
	}
	
	public static String encodeBytes(final byte[] source, final int off, final int len, final int options)
	{
		// Isolate options
		final int dontBreakLines = options & DONT_BREAK_LINES;
		final int gzip = options & GZIP;
		
		// Compress?
		if (gzip == GZIP)
		{
			java.io.ByteArrayOutputStream baos = null;
			java.util.zip.GZIPOutputStream gzos = null;
			Base64.OutputStream b64os = null;
			
			try
			{
				// GZip -> Base64 -> ByteArray
				baos = new java.io.ByteArrayOutputStream();
				b64os = new Base64.OutputStream(baos, ENCODE | dontBreakLines);
				gzos = new java.util.zip.GZIPOutputStream(b64os);
				
				gzos.write(source, off, len);
				gzos.close();
			} // end try
			catch (final java.io.IOException e)
			{
				e.printStackTrace();
				return null;
			} // end catch
			finally
			{
				try
				{
					if (gzos != null)
					{
						gzos.close();
					}
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
				}
				try
				{
					if (b64os != null)
					{
						b64os.close();
					}
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
				}
				try
				{
					if (baos != null)
					{
						baos.close();
					}
				}
				catch (final Exception e)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
				}
			}
			
			if (baos != null)
			{
				try
				{
					return new String(baos.toByteArray(), PREFERRED_ENCODING);
				} // end try
				catch (final java.io.UnsupportedEncodingException uue)
				{
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						uue.printStackTrace();
					}
					
					return new String(baos.toByteArray());
				}
			}
			return null;
		}
		
		// Convert option to boolean in way that code likes it.
		final boolean breakLines = dontBreakLines == 0;
		
		final int len43 = len * 4 / 3;
		final byte[] outBuff = new byte[len43 + (len % 3 > 0 ? 4 : 0) // Account for padding
			+ (breakLines ? len43 / MAX_LINE_LENGTH : 0)]; // New lines
		int d = 0;
		int e = 0;
		final int len2 = len - 2;
		int lineLength = 0;
		for (; d < len2; d += 3, e += 4)
		{
			encode3to4(source, d + off, 3, outBuff, e);
			
			lineLength += 4;
			if (breakLines && lineLength == MAX_LINE_LENGTH)
			{
				outBuff[e + 4] = NEW_LINE;
				e++;
				lineLength = 0;
			} // end if: end of line
		} // en dfor: each piece of array
		
		if (d < len)
		{
			encode3to4(source, d + off, len - d, outBuff, e);
			e += 4;
		} // end if: some padding needed
		
		// Return value according to relevant encoding.
		try
		{
			return new String(outBuff, 0, e, PREFERRED_ENCODING);
		} // end try
		catch (final java.io.UnsupportedEncodingException uue)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				uue.printStackTrace();
			}
			return new String(outBuff, 0, e);
		}
		
	}
	
	static int decode4to3(final byte[] source, final int srcOffset, final byte[] destination, final int destOffset)
	{
		if (source[srcOffset + 2] == EQUALS_SIGN)
		{
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
			// | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
			final int outBuff = (DECODABET[source[srcOffset]] & 0xFF) << 18 | (DECODABET[source[srcOffset + 1]] & 0xFF) << 12;
			
			destination[destOffset] = (byte) (outBuff >>> 16);
			return 1;
		}
		
		// Example: DkL=
		else if (source[srcOffset + 3] == EQUALS_SIGN)
		{
			// Two ways to do the same thing. Don't know which way I like best.
			// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
			// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
			// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
			final int outBuff = (DECODABET[source[srcOffset]] & 0xFF) << 18 | (DECODABET[source[srcOffset + 1]] & 0xFF) << 12 | (DECODABET[source[srcOffset + 2]] & 0xFF) << 6;
			
			destination[destOffset] = (byte) (outBuff >>> 16);
			destination[destOffset + 1] = (byte) (outBuff >>> 8);
			return 2;
		}
		
		// Example: DkLE
		else
		{
			try
			{
				// Two ways to do the same thing. Don't know which way I like best.
				// int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6 )
				// | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
				// | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
				// | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
				final int outBuff = (DECODABET[source[srcOffset]] & 0xFF) << 18 | (DECODABET[source[srcOffset + 1]] & 0xFF) << 12 | (DECODABET[source[srcOffset + 2]] & 0xFF) << 6 | DECODABET[source[srcOffset + 3]] & 0xFF;
				
				destination[destOffset] = (byte) (outBuff >> 16);
				destination[destOffset + 1] = (byte) (outBuff >> 8);
				destination[destOffset + 2] = (byte) outBuff;
				
				return 3;
			}
			catch (final Exception e)
			{
				LOG.error("", e);
				
				LOG.error("" + source[srcOffset] + ": " + DECODABET[source[srcOffset]]);
				LOG.error("" + source[srcOffset + 1] + ": " + DECODABET[source[srcOffset + 1]]);
				LOG.error("" + source[srcOffset + 2] + ": " + DECODABET[source[srcOffset + 2]]);
				LOG.error("" + source[srcOffset + 3] + ": " + DECODABET[source[srcOffset + 3]]);
				return -1;
			} // e nd catch
		}
	} // end decodeToBytes
	
	public static byte[] decode(final byte[] source, final int off, final int len)
	{
		final int len34 = len * 3 / 4;
		final byte[] outBuff = new byte[len34]; // Upper limit on size of output
		int outBuffPosn = 0;
		
		final byte[] b4 = new byte[4];
		int b4Posn = 0;
		int i = 0;
		byte sbiCrop = 0;
		byte sbiDecode = 0;
		for (i = off; i < off + len; i++)
		{
			sbiCrop = (byte) (source[i] & 0x7f); // Only the low seven bits
			sbiDecode = DECODABET[sbiCrop];
			
			if (sbiDecode >= WHITE_SPACE_ENC) // White space, Equals sign or better
			{
				if (sbiDecode >= EQUALS_SIGN_ENC)
				{
					b4[b4Posn++] = sbiCrop;
					if (b4Posn > 3)
					{
						outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn);
						b4Posn = 0;
						
						// If that was the equals sign, break out of 'for' loop
						if (sbiCrop == EQUALS_SIGN)
						{
							break;
						}
					} // end if: quartet built
					
				} // end if: equals sign or better
				
			} // end if: white space, equals sign or better
			else
			{
				LOG.error("Bad Base64 input character at " + i + ": " + source[i] + "(decimal)");
				return null;
			} // end else:
		} // each input character
		
		final byte[] out = new byte[outBuffPosn];
		System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
		return out;
	} // end decode
	
	public static byte[] decode(final String s)
	{
		byte[] bytes;
		try
		{
			bytes = s.getBytes(PREFERRED_ENCODING);
		} // end try
		catch (final java.io.UnsupportedEncodingException uee)
		{
			if (Config.ENABLE_ALL_EXCEPTIONS)
			{
				uee.printStackTrace();
			}
			
			bytes = s.getBytes();
		}
		
		bytes = decode(bytes, 0, bytes.length);
		
		// Check to see if it's gzip-compressed
		// GZIP Magic Two-Byte Number: 0x8b1f (35615)
		if ((bytes != null) && (bytes.length >= 2))
		{
			
			final int head = bytes[0] & 0xff | bytes[1] << 8 & 0xff00;
			if (bytes.length >= 4 && // Don't want to get ArrayIndexOutOfBounds exception
				java.util.zip.GZIPInputStream.GZIP_MAGIC == head)
			{
				java.io.ByteArrayInputStream bais = null;
				java.util.zip.GZIPInputStream gzis = null;
				java.io.ByteArrayOutputStream baos = null;
				final byte[] buffer = new byte[2048];
				int length = 0;
				
				try
				{
					baos = new java.io.ByteArrayOutputStream();
					bais = new java.io.ByteArrayInputStream(bytes);
					gzis = new java.util.zip.GZIPInputStream(bais);
					
					while ((length = gzis.read(buffer)) >= 0)
					{
						baos.write(buffer, 0, length);
					} // end while: reading input
					
					// No error? Get new bytes.
					bytes = baos.toByteArray();
					
				} // end try
				catch (final java.io.IOException e)
				{
					// Just return originally-decoded bytes
					if (Config.ENABLE_ALL_EXCEPTIONS)
					{
						e.printStackTrace();
					}
				} // end catch
				finally
				{
					if (baos != null)
					{
						try
						{
							baos.close();
						}
						catch (final Exception e)
						{
							e.printStackTrace();
						}
					}
					if (gzis != null)
					{
						try
						{
							gzis.close();
						}
						catch (final Exception e)
						{
							if (Config.ENABLE_ALL_EXCEPTIONS)
							{
								e.printStackTrace();
							}
						}
					}
					if (bais != null)
					{
						try
						{
							bais.close();
						}
						catch (final Exception e)
						{
							if (Config.ENABLE_ALL_EXCEPTIONS)
							{
								e.printStackTrace();
							}
						}
					}
				} // end finally
				
			} // end if: gzipped
		} // end if: bytes.length >= 2
		
		return bytes;
	} // end decode
	
	public static Object decodeToObject(final String encodedObject)
	{
		// Decode and gunzip if necessary
		final byte[] objBytes = decode(encodedObject);
		
		java.io.ByteArrayInputStream bais = null;
		java.io.ObjectInputStream ois = null;
		Object obj = null;
		try
		{
			bais = new java.io.ByteArrayInputStream(objBytes);
			ois = new java.io.ObjectInputStream(bais);
			
			obj = ois.readObject();
		} // end try
		catch (java.io.IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
		} // end catch
			// end catch
		finally
		{
			if (bais != null)
			{
				try
				{
					bais.close();
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
			if (ois != null)
			{
				try
				{
					ois.close();
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		} // end finally
		
		return obj;
	} // end decodeObject
	
	public static class InputStream extends java.io.FilterInputStream
	{
		// private int options; // Options specified
		private final boolean encode; // Encoding or decoding
		private int position; // Current position in the buffer
		private final byte[] buffer; // Small buffer holding converted data
		private final int bufferLength; // Length of buffer (3 or 4)
		private int numSigBytes; // Number of meaningful bytes in the buffer
		private int lineLength;
		private final boolean breakLines; // Break lines at less than 80 characters
		
		public InputStream(final java.io.InputStream pIn)
		{
			this(pIn, DECODE);
		} // end constructor
		
		public InputStream(final java.io.InputStream pIn, final int options)
		{
			super(pIn);
			// this.options = options;
			breakLines = (options & DONT_BREAK_LINES) != DONT_BREAK_LINES;
			encode = (options & ENCODE) == ENCODE;
			bufferLength = encode ? 4 : 3;
			buffer = new byte[bufferLength];
			position = -1;
			lineLength = 0;
		} // end constructor
		
		@Override
		public int read() throws java.io.IOException
		{
			// Do we need to get data?
			if (position < 0)
			{
				if (encode)
				{
					final byte[] b3 = new byte[3];
					int numBinaryBytes = 0;
					for (int i = 0; i < 3; i++)
					{
						try
						{
							final int b = in.read();
							
							// If end of stream, b is -1.
							if (b >= 0)
							{
								b3[i] = (byte) b;
								numBinaryBytes++;
							} // end if: not end of stream
							
						} // end try: read
						catch (final java.io.IOException e)
						{
							if (Config.ENABLE_ALL_EXCEPTIONS)
							{
								e.printStackTrace();
							}
							
							// Only a problem if we got no data at all.
							if (i == 0)
							{
								throw e;
							}
							
						} // end catch
					} // end for: each needed input byte
					
					if (numBinaryBytes > 0)
					{
						encode3to4(b3, 0, numBinaryBytes, buffer, 0);
						position = 0;
						numSigBytes = 4;
					} // end if: got data
					else
					{
						return -1;
					}
				} // end if: encoding
				
				// Else decoding
				else
				{
					final byte[] b4 = new byte[4];
					int i = 0;
					for (i = 0; i < 4; i++)
					{
						// Read four "meaningful" bytes:
						int b = 0;
						do
						{
							b = in.read();
						}
						while (b >= 0 && DECODABET[b & 0x7f] <= WHITE_SPACE_ENC);
						
						if (b < 0)
						{
							break; // Reads a -1 if end of stream
						}
						
						b4[i] = (byte) b;
					} // end for: each needed input byte
					
					if (i == 4)
					{
						numSigBytes = decode4to3(b4, 0, buffer, 0);
						position = 0;
					} // end if: got four characters
					else if (i == 0)
					{
						return -1;
					}
					else
					{
						// Must have broken out from above.
						throw new java.io.IOException("Improperly padded Base64 input.");
					}
					
				} // end else: decode
			} // end else: get data
			
			// Got data?
			if (position >= 0)
			{
				// End of relevant data?
				if ( /* !encode && */position >= numSigBytes)
				{
					return -1;
				}
				
				if (encode && breakLines && lineLength >= MAX_LINE_LENGTH)
				{
					lineLength = 0;
					return '\n';
				} // end if
				lineLength++; // This isn't important when decoding
				// but throwing an extra "if" seems
				// just as wasteful.
				
				final int b = buffer[position++];
				
				if (position >= bufferLength)
				{
					position = -1;
				}
				
				return b & 0xFF; // This is how you "cast" a byte that's
				// intended to be unsigned.
				// end else
			} // end if: position >= 0
			
			// When JDK1.4 is more accepted, use an assertion here.
			throw new java.io.IOException("Error in Base64 code reading stream.");
			// end else
		} // end read
		
		@Override
		public int read(final byte[] dest, final int off, final int len) throws java.io.IOException
		{
			int i;
			int b;
			for (i = 0; i < len; i++)
			{
				b = read();
				
				// if( b < 0 && i == 0 )
				// return -1;
				
				if (b >= 0)
				{
					dest[off + i] = (byte) b;
				}
				else if (i == 0)
				{
					return -1;
				}
				else
				{
					break; // Out of 'for' loop
				}
			} // end for: each byte read
			return i;
		} // end read
		
	} // end inner class InputStream
	
	public static class OutputStream extends java.io.FilterOutputStream
	{
		// private int options;
		private final boolean encode;
		private int position;
		private byte[] buffer;
		private final int bufferLength;
		private int lineLength;
		private final boolean breakLines;
		private final byte[] b4; // Scratch used in a few places
		private boolean suspendEncoding;
		
		public OutputStream(final java.io.OutputStream pOut)
		{
			this(pOut, ENCODE);
		} // end constructor
		
		public OutputStream(final java.io.OutputStream pOut, final int options)
		{
			super(pOut);
			// this.options = options;
			breakLines = (options & DONT_BREAK_LINES) != DONT_BREAK_LINES;
			encode = (options & ENCODE) == ENCODE;
			bufferLength = encode ? 3 : 4;
			buffer = new byte[bufferLength];
			position = 0;
			lineLength = 0;
			suspendEncoding = false;
			b4 = new byte[4];
		} // end constructor
		
		@Override
		public void write(final int theByte) throws java.io.IOException
		{
			// Encoding suspended?
			if (suspendEncoding)
			{
				super.out.write(theByte);
				return;
			} // end if: supsended
			
			// Encode?
			if (encode)
			{
				buffer[position++] = (byte) theByte;
				if (position >= bufferLength) // Enough to encode.
				{
					out.write(encode3to4(b4, buffer, bufferLength));
					
					lineLength += 4;
					if (breakLines && lineLength >= MAX_LINE_LENGTH)
					{
						out.write(NEW_LINE);
						lineLength = 0;
					} // end if: end of line
					
					position = 0;
				} // end if: enough to output
			} // end if: encoding
			
			// Else, Decoding
			else
			{
				// Meaningful Base64 character?
				if (DECODABET[theByte & 0x7f] > WHITE_SPACE_ENC)
				{
					buffer[position++] = (byte) theByte;
					if (position >= bufferLength) // Enough to output.
					{
						final int len = Base64.decode4to3(buffer, 0, b4, 0);
						out.write(b4, 0, len);
						// out.write( Base64.decode4to3( buffer ) );
						position = 0;
					} // end if: enough to output
				} // end if: meaningful base64 character
				else if (DECODABET[theByte & 0x7f] != WHITE_SPACE_ENC)
				{
					throw new java.io.IOException("Invalid character in Base64 data.");
				}
			} // end else: decoding
		} // end write
		
		/**
		 * Calls {@link #write} repeatedly until <var>len</var> bytes are written.
		 * @param theBytes array from which to read bytes
		 * @param off offset for array
		 * @param len max number of bytes to read into array
		 * @since 1.3
		 */
		@Override
		public void write(final byte[] theBytes, final int off, final int len) throws java.io.IOException
		{
			// Encoding suspended?
			if (suspendEncoding)
			{
				super.out.write(theBytes, off, len);
				return;
			} // end if: supsended
			
			for (int i = 0; i < len; i++)
			{
				write(theBytes[off + i]);
			} // end for: each byte written
			
		} // end write
		
		/**
		 * Method added by PHIL. [Thanks, PHIL. -Rob] This pads the buffer without closing the stream.
		 * @throws java.io.IOException
		 */
		public void flushBase64() throws java.io.IOException
		{
			if (position > 0)
			{
				if (encode)
				{
					out.write(encode3to4(b4, buffer, position));
					position = 0;
				} // end if: encoding
				else
				{
					throw new java.io.IOException("Base64 input not properly padded.");
				}
			} // end if: buffer partially full
			
		} // end flush
		
		/**
		 * Flushes and closes (I think, in the superclass) the stream.
		 * @since 1.3
		 */
		@Override
		public void close() throws java.io.IOException
		{
			// 1. Ensure that pending characters are written
			flushBase64();
			
			// 2. Actually close the stream
			// Base class both flushes and closes.
			super.close();
			
			buffer = null;
			out = null;
		} // end close
		
		/**
		 * Suspends encoding of the stream. May be helpful if you need to embed a piece of base640-encoded data in a stream.
		 * @throws java.io.IOException
		 * @since 1.5.1
		 */
		public void suspendEncoding() throws java.io.IOException
		{
			flushBase64();
			suspendEncoding = true;
		} // end suspendEncoding
		
		/**
		 * Resumes encoding of the stream. May be helpful if you need to embed a piece of base640-encoded data in a stream.
		 * @since 1.5.1
		 */
		public void resumeEncoding()
		{
			suspendEncoding = false;
		} // end resumeEncoding
		
	} // end inner class OutputStream
	
} // end class Base64
