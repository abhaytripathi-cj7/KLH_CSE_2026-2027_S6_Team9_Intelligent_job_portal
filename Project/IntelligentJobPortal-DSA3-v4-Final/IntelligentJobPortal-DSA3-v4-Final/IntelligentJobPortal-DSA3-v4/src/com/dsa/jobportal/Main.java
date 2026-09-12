package com.dsa.jobportal;

import com.dsa.jobportal.service.JobRepository;
import com.dsa.jobportal.ui.ConsoleUI;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        String path = locateDataFile();
        try {
            JobRepository repository = new JobRepository();
            repository.load(path);
            System.out.println("Loaded " + repository.all().size() + " jobs from " + path);
            new ConsoleUI(repository).start();
        } catch (Exception e) {
            System.err.println("Unable to start the project: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Expected dataset at data/jobs.csv relative to the project folder.");
        }
    }

    private static String locateDataFile() {
        String[] candidates = {"data/jobs.csv", "../data/jobs.csv", "../../data/jobs.csv"};
        for (String p : candidates) if (new File(p).exists()) return p;
        return "data/jobs.csv";
    }
}
