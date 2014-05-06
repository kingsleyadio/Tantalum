package org.tantalum.security;

import java.io.UnsupportedEncodingException;

/**
 * Simplified cryptography routines
 *
 * @author phou
 */
public abstract class CryptoUtils {

  /**
   * The length of each digest in bytes
   *
   * A digest is a byte[] of this length
   */
  public static final int DIGEST_LENGTH = 16;
  protected static final int LONG_LENGTH_IN_BYTES = 8;

  /**
   * Convert from String form, which may take a lot of RAM, into a fixed size cryptographic digest.
   *
   * @param key
   * @return 16 byte cryptographic has
   * @throws CryptoException
   * @throws UnsupportedEncodingException
   */
  public synchronized long toDigest(final String key) throws CryptoException, UnsupportedEncodingException {
    if (key == null) {
      throw new NullPointerException("You attempted to convert a null string into a hash digest");
    }

    final byte[] bytes = key.getBytes("UTF-8");

    return toDigest(bytes);
  }

  /**
   * Generate a cryptographic MD5 digest from a byte array
   *
   * @param bytes
   * @return
   * @throws CryptoException
   * @throws UnsupportedEncodingException
   */
  public abstract long toDigest(final byte[] bytes) throws CryptoException;

  /**
   * Encode 8 bytes into one Long
   *
   * @param bytes
   * @param start
   * @return
   */
  public long bytesToLong(final byte[] bytes, final int start) {
    if (bytes == null) {
      throw new NullPointerException("Bad bytesToLong, bytes passed are null: can not convert digest to Long");
    }
    if (bytes.length != 8) {
      throw new IllegalArgumentException("Bad bytesToLong, length != 8: can not convert digest to Long");
    }
    long l = 0;

    for (int i = 0; i < 8; i++) {
      l |= ((long) (bytes[start + i] & 0xFF)) << (8 * i);
    }

    return l;
  }

  /**
   * Encode one Long to 8 bytes
   *
   * @param l
   * @return
   */
  public byte[] longToBytes(final long l) {
    final byte[] bytes = new byte[8];

    longToBytes(l, bytes, 0);

    return bytes;
  }

  /**
   * Encode one Long to 8 bytes inserted into an existing array
   *
   * @param l
   * @param bytes
   * @param start
   */
  public void longToBytes(final long l, final byte[] bytes, final int start) {
    for (int i = 0; i < 8; i++) {
      bytes[start + i] = (byte) (((int) (l >>> (8 * i))) & 0xFF);
    }
  }
}
