package org.tantalum.blackberry;

import net.rim.device.api.crypto.Digest;
import net.rim.device.api.crypto.MD5Digest;

import org.tantalum.security.CryptoException;
import org.tantalum.security.CryptoUtils;

public class BBCryptoUtils extends CryptoUtils {
	private final Digest digest;

	private static class CryptoUtilsHolder {

		static final CryptoUtils instance = new BBCryptoUtils();
	}

	/**
	 * Get the singleton
	 * 
	 * @return
	 */
	public static CryptoUtils getInstance() {
		return CryptoUtilsHolder.instance;
	}

	private BBCryptoUtils() {
		this.digest = new MD5Digest();
	}

	public synchronized long toDigest(byte[] bytes) throws CryptoException {
		if (bytes == null) {
			throw new NullPointerException(
					"You attempted to convert a null byte[] into a hash digest");
		}

		try {

			digest.update(bytes, 0, bytes.length);
			final byte[] hashKey = digest.getDigest();

			final byte[] l = new byte[LONG_LENGTH_IN_BYTES];
			for (int i = 0; i < l.length; i++) {
				l[i] = (byte) ((hashKey[2 * i] & 0xFF) ^ (hashKey[(2 * i) + 1] & 0xFF));
			}

			return bytesToLong(l, 0);
		} catch (Exception e) {
			throw new CryptoException(e.getMessage());
		}
	}

}
