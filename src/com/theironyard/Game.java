package com.theironyard;

/**
 * Created by zach on 2/23/16.
 */
public class Game {
    String name;
    String genre;
    String platform;
    int releaseYear;
    int id;


    public Game(String name, String genre, String platform, int releaseYear, int id) {
        this.name = name;
        this.genre = genre;
        this.platform = platform;
        this.releaseYear = releaseYear;
        this.id = id;

    }
}
