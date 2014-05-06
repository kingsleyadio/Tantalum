package org.tantalum.blackberry;

import java.util.Hashtable;

import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;

import org.tantalum.security.CryptoException;
import org.tantalum.storage.FlashDatabaseException;
import org.tantalum.storage.FlashFullException;

public class PersistenceUtils {
	
	private final PersistentObject persistence;
	private static final Object mutex = new Object();
	
	private static PersistenceUtils instance;
	
	private PersistenceUtils() {
		persistence = PersistentStore.getPersistentObject(0x987654311L);
		synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			if (cache == null) {
				cache = new Hashtable();
				persistence.setContents(cache);
				persistence.commit();
			}
		}
	}
	
	public static PersistenceUtils getInstance() {
		if (instance == null) {
			instance = new PersistenceUtils();
		}
		return instance;
	}

	public void clear() {
		synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			cache.clear();
			persistence.commit();
		}
	}

	public void write(String key, byte[] bytes) throws CryptoException,
			FlashFullException, FlashDatabaseException {
        synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			cache.put(key, bytes);
			persistence.commit();
		}
	}

	public void delete(String key) throws FlashDatabaseException {
		synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			cache.remove(key);
			persistence.commit();
		}
	}

	public byte[] read(String key) throws CryptoException,
			FlashDatabaseException {
		synchronized (mutex) {
			Hashtable cache = (Hashtable) persistence.getContents();
			return (byte[]) cache.get(key);
		}
	}
}
