package l2jorion.crypt;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import l2jorion.logger.Logger;
import l2jorion.logger.LoggerFactory;

public class ScrambledKeyPair
{
	private static Logger LOG = LoggerFactory.getLogger(ScrambledKeyPair.class);
	
	public KeyPair _pair;
	public byte[] _scrambledModulus;
	
	public ScrambledKeyPair(final KeyPair pPair)
	{
		_pair = pPair;
		_scrambledModulus = scrambleModulus(((RSAPublicKey) _pair.getPublic()).getModulus());
	}
	
	private byte[] scrambleModulus(final BigInteger modulus)
	{
		byte[] scrambledMod = modulus.toByteArray();
		
		if (scrambledMod.length == 0x81 && scrambledMod[0] == 0x00)
		{
			byte[] temp = new byte[0x80];
			System.arraycopy(scrambledMod, 1, temp, 0, 0x80);
			scrambledMod = temp;
			temp = null;
		}
		
		// step 1 : 0x4d-0x50 <-> 0x00-0x04
		for (int i = 0; i < 4; i++)
		{
			final byte temp = scrambledMod[0x00 + i];
			scrambledMod[0x00 + i] = scrambledMod[0x4d + i];
			scrambledMod[0x4d + i] = temp;
		}
		
		// step 2 : xor first 0x40 bytes with last 0x40 bytes
		for (int i = 0; i < 0x40; i++)
		{
			scrambledMod[i] = (byte) (scrambledMod[i] ^ scrambledMod[0x40 + i]);
		}
		
		// step 3 : xor bytes 0x0d-0x10 with bytes 0x34-0x38
		for (int i = 0; i < 4; i++)
		{
			scrambledMod[0x0d + i] = (byte) (scrambledMod[0x0d + i] ^ scrambledMod[0x34 + i]);
		}
		
		// step 4 : xor last 0x40 bytes with first 0x40 bytes
		for (int i = 0; i < 0x40; i++)
		{
			scrambledMod[0x40 + i] = (byte) (scrambledMod[0x40 + i] ^ scrambledMod[i]);
		}
		
		LOG.debug("Modulus was scrambled");
		
		return scrambledMod;
	}
}
