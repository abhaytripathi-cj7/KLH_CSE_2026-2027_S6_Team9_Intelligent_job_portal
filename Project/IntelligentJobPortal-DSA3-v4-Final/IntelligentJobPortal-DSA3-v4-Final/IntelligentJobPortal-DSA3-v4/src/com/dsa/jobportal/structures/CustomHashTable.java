package com.dsa.jobportal.structures;

public class CustomHashTable<V> {
    private static class Entry<V> {
        String key;
        V value;
        Entry<V> next;

        Entry(String key, V value, Entry<V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Entry<V>[] buckets;
    private int size;

    @SuppressWarnings("unchecked")
    public CustomHashTable(int capacity) {
        buckets = (Entry<V>[]) new Entry[Math.max(17, capacity)];
    }

    public void put(String key, V value) {
        String normalized = normalize(key);
        int index = index(normalized);
        Entry<V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(normalized)) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        buckets[index] = new Entry<>(normalized, value, buckets[index]);
        size++;
    }

    public V get(String key) {
        String normalized = normalize(key);
        Entry<V> current = buckets[index(normalized)];
        while (current != null) {
            if (current.key.equals(normalized)) return current.value;
            current = current.next;
        }
        return null;
    }

    public boolean containsKey(String key) {
        return get(key) != null;
    }

    public int size() {
        return size;
    }

    private int index(String key) {
        long hash = 1469598103934665603L;
        for (int i = 0; i < key.length(); i++) {
            hash ^= key.charAt(i);
            hash *= 1099511628211L;
        }
        hash &= Long.MAX_VALUE;
        return (int) (hash % buckets.length);
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
