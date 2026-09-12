package com.dsa.jobportal.algorithms.randomized;

import com.dsa.jobportal.model.MatchResult;

public class RandomizedQuickSort {
    private final LCGRandom random;

    public RandomizedQuickSort() {
        random = new LCGRandom(System.nanoTime());
    }

    public void sortDescending(MatchResult[] values) {
        if (values == null || values.length < 2) return;
        quickSort(values, 0, values.length - 1);
    }

    private void quickSort(MatchResult[] a, int left, int right) {
        while (left < right) {
            int pivotIndex = left + random.nextInt(right - left + 1);
            swap(a, pivotIndex, right);
            int p = partition(a, left, right);
            if (p - left < right - p) {
                quickSort(a, left, p - 1);
                left = p + 1;
            } else {
                quickSort(a, p + 1, right);
                right = p - 1;
            }
        }
    }

    private int partition(MatchResult[] a, int left, int right) {
        double pivot = a[right].getScore();
        int store = left;
        for (int i = left; i < right; i++) {
            if (a[i].getScore() > pivot) {
                swap(a, i, store++);
            }
        }
        swap(a, store, right);
        return store;
    }

    private void swap(MatchResult[] a, int i, int j) {
        MatchResult t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
