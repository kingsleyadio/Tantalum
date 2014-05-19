package org.tantalum.blackberry;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import net.rim.device.api.system.Memory;

import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;

import org.tantalum.security.CryptoException;
import org.tantalum.storage.FlashCache;
import org.tantalum.storage.FlashDatabaseException;
import org.tantalum.storage.FlashFullException;
import org.tantalum.util.L;

public class BBCache extends FlashCache {
	
	private final PersistentObject persistence;
	private static final long KEY_PREFIX = 0xff123499034511L;
	
	private final long KEY;
	
	private static final Object mutex = new Object();

	public BBCache(char priority, StartupTask startupTask) {
		super(priority);
		KEY = getPersistenceKey(priority);
		persistence = PersistentStore.getPersistentObject(KEY);

		synchronized (mutex) {
			Object cache = persistence.getContents();
			if (cache == null) {
				cache = new Hashtable();
				persistence.setContents(cache);
				persistence.commit();
			}
		}
	}
	
	private static long getPersistenceKey(long priority) {
		return KEY_PREFIX + priority;
	}

	public void clear() {
		synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			cache.clear();
			persistence.commit();
		}
	}

	public byte[] get(long digest, boolean markAsLeastRecentlyUsed) throws CryptoException,
			FlashDatabaseException {
		synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			return (byte[]) cache.get(new Long(digest));
		}
	}

	public Enumeration getDigests() {
		synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			return cache.keys();
		}
	}

	public long getFreespace() throws FlashDatabaseException {
        return Memory.getPersistentStats().getFree();
	}

	public String getKey(long digest) throws FlashDatabaseException {
		// TODO Auto-generated method stub
		return Long.toString(digest);
	}

	public long getSize() throws FlashDatabaseException {
        return Memory.getPersistentStats().getObjectSize();
	}

	public void maintainDatabase() {
		// TODO Auto-generated method stub

	}

	public void markLeastRecentlyUsed(Long digest) {
		// TODO Auto-generated method stub

	}

	public void put(String key, byte[] bytes) throws CryptoException,
			FlashFullException, FlashDatabaseException {
		// TODO Auto-generated method stub
        final long digest;
        try {
            digest = cryptoUtils.toDigest(key);
        } catch (UnsupportedEncodingException ex) {
            //#debug
            L.e(this, "get() can not decode", "key = " + key, ex);
            throw new FlashDatabaseException("get() can not decode key: " + key + " - " + ex);
        }
        synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			cache.put(new Long(digest), bytes);
			persistence.commit();
		}
	}

	public void removeData(long digest) throws FlashDatabaseException {
		synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			cache.remove(new Long(digest));
			persistence.commit();
		}
	}

}
