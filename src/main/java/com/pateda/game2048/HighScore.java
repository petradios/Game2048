package com.pateda.game2048;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HighScore implements Serializable, Comparable<HighScore> {
    private String name; // NEW field
    private long score;
    private String date;

    // No-arg constructor for Jackson
    public HighScore() {}

    // Updated constructor to accept name
    public HighScore(String name, long score) {
        this.name = (name == null || name.trim().isEmpty()) ? "Anonymous" : name;
        this.score = score;
        this.date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getScore() { return score; }
    public void setScore(long score) { this.score = score; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    @Override
    public int compareTo(HighScore o) {
        // Sort descending (higher score first)
        return Long.compare(o.score, this.score);
    }
}