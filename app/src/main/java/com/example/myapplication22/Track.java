package com.example.myapplication22;

public class Track {

    private final long id;
    private final String name;
    private final String start;
    private final String finish;
    private final String elapsedTime;

    public Track(long id, String name, String start, String finish, String elapsedTime) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.finish = finish;
        this.elapsedTime = elapsedTime;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStart() {
        return start;
    }

    public String getFinish() {
        return finish;
    }

    public String getElapsedTime() {
        return elapsedTime;
    }
}
