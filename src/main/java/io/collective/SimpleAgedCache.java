package io.collective;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public class SimpleAgedCache {
    private static final int INITIAL_CAPACITY = 10;

    private int entryCount = 0;
    private ExpirableEntry[] entries = new ExpirableEntry[INITIAL_CAPACITY];
    private Clock clock;
    private Instant createdInstant;

    public SimpleAgedCache() {
        this(Clock.systemDefaultZone());
    }

    public SimpleAgedCache(Clock clock) {
        this.clock = clock;
        this.createdInstant = clock.instant();
    }

    public void put(Object key, Object value, int retentionInMillis) {
        if (findEntry(key) != null) {
            // element already in array
            throw new IllegalArgumentException("Element key already exists: " + key);
        }

        addEntry(new ExpirableEntry(key, value, retentionInMillis));
    }

    public boolean isEmpty() {
        return entryCount == 0;
    }

    public int size() {
        expireEntries();
        return entryCount;
    }

    public Object get(Object key) {
        expireEntries();
        ExpirableEntry entry = findEntry(key);
        return entry != null ? entry.value : null;
    }

    private ExpirableEntry findEntry(Object key) {
        for (int i = 0; i < entryCount; i++) {
            ExpirableEntry entry = entries[i];
            if (entry.key.equals(key)) {
                return entry;
            }
        }
        return null;
    }

    private void addEntry(ExpirableEntry entry) {
        ensureCapacity();
        entries[entryCount] = entry;
        entryCount++;
    }

    private void ensureCapacity() {
        if (entryCount < entries.length) {
            return;
        }
        // expand array
    }

    private void expireEntries() {
        var newEntries = new ExpirableEntry[entries.length];
        int entryCountSnap = entryCount;
        int copyIdx = 0;
        for (int i = 0; i < entryCountSnap; i++) {
            ExpirableEntry entry = entries[i];
            if (isExpired(entry)) {
                entryCount--;
                continue;
            }
            newEntries[copyIdx] = entry;
            copyIdx++;
        }
        entries = newEntries;
    }

    private boolean isExpired(ExpirableEntry entry) {
        Instant expirationInstant = createdInstant.plusMillis(entry.retentionInMillis);
        return expirationInstant.isBefore(clock.instant());
    }

    private class ExpirableEntry {
        public Object key;
        public Object value;
        public int retentionInMillis;

        public ExpirableEntry(Object key, Object value, int retentionInMillis) {
            this.key = key;
            this.value = value;
            this.retentionInMillis = retentionInMillis;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ExpirableEntry that = (ExpirableEntry) o;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}