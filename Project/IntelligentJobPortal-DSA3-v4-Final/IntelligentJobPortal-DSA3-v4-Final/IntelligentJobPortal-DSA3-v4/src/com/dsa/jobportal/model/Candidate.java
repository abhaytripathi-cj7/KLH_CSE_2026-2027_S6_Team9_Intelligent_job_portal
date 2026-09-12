package com.dsa.jobportal.model;

public class Candidate {
    private final int id;
    private final String name;
    private final String skills;
    private final String preferredLocation;

    public Candidate(int id, String name, String skills, String preferredLocation) {
        this.id = id;
        this.name = name == null ? "" : name.trim();
        this.skills = skills == null ? "" : skills;
        this.preferredLocation = preferredLocation == null ? "" : preferredLocation.trim();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSkills() { return skills; }
    public String getPreferredLocation() { return preferredLocation; }

    /**
     * Returns non-empty, trimmed skill tokens without relying on java.util collections.
     */
    public String[] skillArray() {
        if (skills.isBlank()) return new String[0];
        String[] raw = skills.split("\\|");
        int count = 0;
        for (int i = 0; i < raw.length; i++) if (!raw[i].trim().isEmpty()) count++;
        String[] clean = new String[count];
        int p = 0;
        for (int i = 0; i < raw.length; i++) {
            String value = raw[i].trim();
            if (!value.isEmpty()) clean[p++] = value;
        }
        return clean;
    }
}
