package com.dsa.jobportal.algorithms.randomized;

import com.dsa.jobportal.model.Job;
import com.dsa.jobportal.structures.DynamicArray;

public class ReservoirSampler {
    public Job[] sample(DynamicArray<Job> jobs, int k) {
        if (k <= 0 || jobs.isEmpty()) return new Job[0];
        k = Math.min(k, jobs.size());
        Job[] result = new Job[k];
        for (int i = 0; i < k; i++) result[i] = jobs.get(i);

        LCGRandom rng = new LCGRandom(System.nanoTime());
        for (int i = k; i < jobs.size(); i++) {
            int j = rng.nextInt(i + 1);
            if (j < k) result[j] = jobs.get(i);
        }
        return result;
    }
}
