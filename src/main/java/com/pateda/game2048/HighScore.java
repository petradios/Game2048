package com.pateda.game2048;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

 //Represents a single high score entry containing player name, score, and date.

public class HighScore implements Serializable, Comparable<HighScore> {
    private String name;
    private long score;
    private String date;

    // Default constructor required for Jackson deserialization
    public HighScore() {}

    // Main constructor initializes fields and sets current date
    public HighScore(String name, long score) {
        this.name = (name == null || name.trim().isEmpty()) ? "Anonymous" : name;
        this.score = score;
        this.date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    // Getters and Setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getScore() { return score; }
    public void setScore(long score) { this.score = score; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // Sorts scores in descending order
    @Override
    public int compareTo(HighScore o) {
        return Long.compare(o.score, this.score);
    }
}