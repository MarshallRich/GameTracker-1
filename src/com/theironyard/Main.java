package com.theironyard;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    static HashMap<String, User> users = new HashMap<>();

    public static void main(String[] args) throws SQLException {

        Connection conn = DriverManager.getConnection("jdbc:h2:./main");

        Spark.externalStaticFileLocation("public");
        Spark.init();
        Spark.get(
                "/",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());

                    HashMap m = new HashMap<>();
                    ArrayList<Game> games = selectGames(conn);
                    if (user == null) {
                        return new ModelAndView(m, "login.html");
                    }
                    else {
                        m.put("games", games);
                        return new ModelAndView(m, "home.html");
                    }
                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/create-user",
                ((request, response) -> {
                    String name = request.queryParams("loginName");
                    if (name == null) {
                        throw new Exception("Login name is null.");
                    }

                    User user = users.get(name);
                    if (user == null) {
                        user = new User(name);
                        users.put(name, user);
                    }

                    Session session = request.session();
                    session.attribute("userName", name);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/create-game",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    if (user == null) {
                        //throw new Exception("User is not logged in");
                        Spark.halt(403);
                    }

                    String gameName = request.queryParams("gameName");
                    String gameGenre = request.queryParams("gameGenre");
                    String gamePlatform = request.queryParams("gamePlatform");
                    int gameYear = Integer.valueOf(request.queryParams("gameYear"));
                    if (gameName == null || gameGenre == null || gamePlatform == null) {
                        throw new Exception("Didn't receive all query parameters.");
                    }
                    Game game = new Game(gameName, gameGenre, gamePlatform, gameYear, 0);

                    insertGame(game, conn);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/delete-game",
                ((request, response) -> {
                int id = Integer.valueOf(request.queryParams("deleteNumber"));
                    deleteGame(id, conn);

                    response.redirect("/");
                return "";
                })
        );

        Spark.post(
                "/edit-game",
                ((request, response) -> {
                int id = Integer.valueOf(request.queryParams("editNumber"));
                    String editName = request.queryParams("editName");
                    String editGenre = request.queryParams("editGenre");
                    String editPlatform = request.queryParams("editPlatform");
                    String editYearStr = request.queryParams("editYear");

                    if (editYearStr != null || !editYearStr.isEmpty()){

                    int editYear = Integer.valueOf(editYearStr);

                        updateGame(id, editName, editGenre, editPlatform, editYear, conn);
                    }


                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );


    }

    static User getUserFromSession(Session session) {
        String name = session.attribute("userName");
        return users.get(name);
    }

    static void insertGame(Game game, Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        stmt.execute("CREATE TABLE IF NOT EXISTS games (id IDENTITY, game_name VARCHAR, genre VARCHAR, platform VARCHAR, release_year INT)");

        PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO games VALUES (NULL, ?, ?, ?, ?)");


        stmt2.setString(1, game.name);
        stmt2.setString(2, game.genre);
        stmt2.setString(3, game.platform);
        stmt2.setInt(4, game.releaseYear);
        stmt2.execute();
    }

    static void deleteGame(Integer id, Connection conn) throws SQLException {


        PreparedStatement stmt = conn.prepareStatement("DELETE FROM games WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();

    }

    static ArrayList<Game> selectGames(Connection conn) throws SQLException {
        ArrayList<Game> games= new ArrayList<>();
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * FROM games");
        while(results.next()) {
            String name = results.getString("game_name");
            String genre = results.getString("genre");
            String platform = results.getString("platform");
            int releaseYear = results.getInt("release_year");
            int id = results.getInt("id");
            Game game = new Game(name, genre, platform, releaseYear, id);
            games.add(game);
        }
        return games;
    }

    static void updateGame(int id, String name, String genre, String platform, int year, Connection conn) throws SQLException {

        if ( !name.isEmpty() || !genre.isEmpty() || !platform.isEmpty() || name!=null || genre!=null || platform!=null) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE games SET game_name = ?, genre = ?, platform = ?, release_year = ? WHERE id = ?");
            stmt.setString(1, name);
            stmt.setString(2, genre);
            stmt.setString(3, platform);
            stmt.setInt(4, year);
            stmt.setInt(5, id);

            stmt.execute();
        }

    }
}
