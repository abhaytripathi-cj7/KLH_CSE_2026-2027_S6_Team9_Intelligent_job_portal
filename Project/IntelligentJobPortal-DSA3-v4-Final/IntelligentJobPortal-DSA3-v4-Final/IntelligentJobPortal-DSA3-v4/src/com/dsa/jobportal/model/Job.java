package com.dsa.jobportal.model;

public class Job {
    private final int id;
    private final String title;
    private final String company;
    private final String location;
    private final String skills;
    private final String description;
    private final int salaryLpa;
    private final int experienceYears;
    private final int vacancies;
    private final int applyHours;
    private final String workMode;

    public Job(int id, String title, String company, String location, String skills,
               String description, int salaryLpa, int experienceYears, int vacancies, int applyHours) {
        this(id, title, company, location, skills, description, salaryLpa, experienceYears, vacancies, applyHours, "");
    }

    public Job(int id, String title, String company, String location, String skills,
               String description, int salaryLpa, int experienceYears, int vacancies, int applyHours,
               String workMode) {
        this.id = id;
        this.title = title == null ? "" : title.trim();
        this.company = company == null ? "" : company.trim();
        this.location = location == null ? "" : location.trim();
        this.skills = skills == null ? "" : skills.trim();
        this.description = description == null ? "" : description.trim();
        this.salaryLpa = salaryLpa;
        this.experienceYears = experienceYears;
        this.vacancies = vacancies;
        this.applyHours = applyHours;
        this.workMode = workMode == null ? "" : workMode.trim();
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getSkills() { return skills; }
    public String getDescription() { return description; }
    public int getSalaryLpa() { return salaryLpa; }
    public int getExperienceYears() { return experienceYears; }
    public int getVacancies() { return vacancies; }
    public int getApplyHours() { return applyHours; }
    public String getWorkMode() { return workMode; }

    public String searchableText() {
        return (title + " " + company + " " + location + " " + skills.replace('|', ' ') + " " + description).toLowerCase();
    }

    public String[] skillArray() {
        if (skills == null || skills.isBlank()) return new String[0];
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

    public String shortLine() {
        return String.format("#%d | %-28s | %-14s | %-11s | %2d LPA", id, title, company, location, salaryLpa);
    }

    public String detailedView() {
        return "Job ID       : " + id + "\n" +
               "Title        : " + title + "\n" +
               "Company      : " + company + "\n" +
               "Location     : " + location + "\n" +
               "Work Mode    : " + (workMode.isBlank() ? "Not specified" : workMode) + "\n" +
               "Skills       : " + skills.replace('|', ',') + "\n" +
               "Salary       : " + salaryLpa + " LPA\n" +
               "Experience   : " + experienceYears + " year(s)\n" +
               "Vacancies    : " + vacancies + "\n" +
               "Apply Time   : " + applyHours + " hour(s)\n" +
               "Description  : " + description;
    }
}
