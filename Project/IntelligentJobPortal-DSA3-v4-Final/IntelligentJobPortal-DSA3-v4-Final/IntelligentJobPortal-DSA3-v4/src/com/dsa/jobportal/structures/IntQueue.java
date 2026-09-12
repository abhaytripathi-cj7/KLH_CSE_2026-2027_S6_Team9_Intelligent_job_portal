package com.dsa.jobportal.structures;

public class IntQueue {
    private int[] data;
    private int head;
    private int tail;
    private int size;

    public IntQueue(int capacity) {
        data = new int[Math.max(4, capacity)];
    }

    public void offer(int value) {
        if (size == data.length) grow();
        data[tail] = value;
        tail = (tail + 1) % data.length;
        size++;
    }

    public int poll() {
        if (size == 0) throw new IllegalStateException("Queue is empty");
        int value = data[head];
        head = (head + 1) % data.length;
        size--;
        return value;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void grow() {
        int[] next = new int[data.length * 2];
        for (int i = 0; i < size; i++) next[i] = data[(head + i) % data.length];
        data = next;
        head = 0;
        tail = size;
    }
}
