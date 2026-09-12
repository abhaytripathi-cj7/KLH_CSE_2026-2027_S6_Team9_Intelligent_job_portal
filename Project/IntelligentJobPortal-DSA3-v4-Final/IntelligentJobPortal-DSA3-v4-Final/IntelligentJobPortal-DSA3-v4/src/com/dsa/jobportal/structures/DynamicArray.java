package com.dsa.jobportal.structures;

public class DynamicArray<T> {
    private Object[] data;
    private int size;

    public DynamicArray() {
        data = new Object[8];
        size = 0;
    }

    public void add(T value) {
        ensureCapacity(size + 1);
        data[size++] = value;
    }

    public T get(int index) {
        checkIndex(index);
        @SuppressWarnings("unchecked")
        T value = (T) data[index];
        return value;
    }

    public void set(int index, T value) {
        checkIndex(index);
        data[index] = value;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) data[i] = null;
        size = 0;
    }

    private void ensureCapacity(int needed) {
        if (needed <= data.length) return;
        int newCapacity = data.length * 2;
        while (newCapacity < needed) newCapacity *= 2;
        Object[] next = new Object[newCapacity];
        for (int i = 0; i < size; i++) next[i] = data[i];
        data = next;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}
