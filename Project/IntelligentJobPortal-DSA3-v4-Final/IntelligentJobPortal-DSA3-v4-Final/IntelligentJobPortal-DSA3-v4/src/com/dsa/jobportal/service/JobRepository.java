package com.dsa.jobportal.service;

import com.dsa.jobportal.model.Job;
import com.dsa.jobportal.structures.DynamicArray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class JobRepository {
    private final DynamicArray<Job> jobs = new DynamicArray<>();

    public void load(String path) throws IOException {
        jobs.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 10) continue;
                Job job = new Job(
                    parseInt(parts[0]), parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim(),
                    parts[5].trim(), parseInt(parts[6]), parseInt(parts[7]), parseInt(parts[8]), parseInt(parts[9])
                );
                jobs.add(job);
            }
        }
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    public DynamicArray<Job> all() { return jobs; }

    public void add(Job job) {
        if (job != null) jobs.add(job);
    }

    public int nextId() {
        int max = 0;
        for (int i = 0; i < jobs.size(); i++) if (jobs.get(i).getId() > max) max = jobs.get(i).getId();
        return max + 1;
    }

    public Job findById(int id) {
        for (int i = 0; i < jobs.size(); i++) if (jobs.get(i).getId() == id) return jobs.get(i);
        return null;
    }
}
