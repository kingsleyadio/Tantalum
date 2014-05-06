/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tantalum.jme;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.tantalum.security.CryptoException;
import org.tantalum.security.CryptoUtils;
import org.tantalum.util.L;

/**
 *
 * @author ADIKSONLINE
 */
public class JMECryptoUtils extends CryptoUtils {

  private MessageDigest messageDigest;

  private static class CryptoUtilsHolder {

    static final CryptoUtils instance = new JMECryptoUtils();
  }

  /**
   * Get the singleton
   *
   * @return
   */
  public static CryptoUtils getInstance() {
    return CryptoUtilsHolder.instance;
  }

  private JMECryptoUtils() {
    try {
      messageDigest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException ex) {
      //#debug
      L.e("Can not init CryptoUtils", "", ex);
    }
  }

  public synchronized long toDigest(final byte[] bytes) throws CryptoException {
    if (bytes == null) {
      throw new NullPointerException("You attempted to convert a null byte[] into a hash digest");
    }

    try {
      final byte[] hashKey = new byte[DIGEST_LENGTH];

      messageDigest.update(bytes, 0, bytes.length);
      messageDigest.digest(hashKey, 0, DIGEST_LENGTH);

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
