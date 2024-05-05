import java.sql.*;

/**
 * This class provides access to a local MySQL server.
 * Specifically, it can be used to Create a User and Validate a User.
 */
public class DataAccessObject {
    Connection connection;
    PreparedStatement psGetUser;
    PreparedStatement psInsert;

    public DataAccessObject() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/messengerdatabase",
                    "root", "toor");
            psGetUser = connection.prepareStatement(
                    "SELECT * FROM users WHERE username = ?");
            psInsert = connection.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?,?)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public EventFlag createUser(String user, String pass) {
        ResultSet results;
        try {
            psGetUser.setString(1, user);
            results = psGetUser.executeQuery();
            if (results.isBeforeFirst()) { //User already exists
                System.out.println("User already exists");
                return EventFlag.USER_ALREADY_EXISTS;
            }
            if (!validatePassword(pass)) { // Password is invalid
                System.out.println("Invalid Password");
                return EventFlag.INVALID_PASSWORD;
            }
            psInsert.setString(1,user);
            psInsert.setString(2,pass);
            psInsert.executeUpdate();
            System.out.println("User Inserted");
            return EventFlag.VALID;

        } catch (SQLException e) {
            e.printStackTrace();
            return EventFlag.INVALID;
        }
    }

    public EventFlag validateUser(String user,String pass) {
        ResultSet results;
        String password;
        try {
            psGetUser.setString(1,user);
            results = psGetUser.executeQuery();

            if(!results.isBeforeFirst()) { // Username doesn't exist
                System.out.println("This username (" + user + ") does not exist");
                return EventFlag.USER_DOES_NOT_EXIST ;
            }
            results.next(); // Get Password
            password = results.getString("password");
            if (!password.equals(pass)) { //Incorrect Password
                System.out.println("Incorrect Password");
                return EventFlag.INVALID_PASSWORD;
            }
            return EventFlag.VALID;
        } catch (SQLException e) {
            e.printStackTrace();
            return EventFlag.INVALID;
        }
    }
    private boolean validatePassword(String pass) {
        return ((pass != null) && (pass.length() >= 4));
    }
}
