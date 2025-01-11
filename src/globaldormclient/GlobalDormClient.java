package globaldormClient;

import globaldormclient.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GlobalDormClient {
    private static final String BASE_URL = "http://localhost:8080/GlobalDorm/Dorm/rooms";
    private static final String USERS_FILE = "users.json";
    private static final Gson gson = new Gson();
    private static String loggedInUser;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (loggedInUser == null) {
            System.out.println("\n=== Welcome to GlobalDorm Client ===");
            System.out.println("1. Login");
            System.out.println("2. Create New User");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1 -> login(scanner);
                case 2 -> createNewUser(scanner);
                case 3 -> {
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }

        // User is logged in, proceed to room management menu
        roomManagementMenu(scanner);
    }

    private static void login(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        List<User> users = loadUsers();
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(hashPassword(password))) {
                loggedInUser = username;
                System.out.println("Login successful! Welcome, " + loggedInUser);
                return;
            }
        }

        System.out.println("Invalid username or password. Please try again.");
    }

    private static void createNewUser(Scanner scanner) {
        try {
            System.out.print("Enter a username: ");
            String username = scanner.nextLine();

            System.out.print("Enter a password: ");
            String password = scanner.nextLine();

            // Validate username is unique
            List<User> users = loadUsers();
            if (users.stream().anyMatch(user -> user.getUsername().equals(username))) {
                System.out.println("Error: Username already exists.");
                return;
            }

            // Hash the password
            String hashedPassword = hashPassword(password);

            // Add the new user to the list and save to file
            users.add(new User(username, hashedPassword));
            saveUsers(users);

            System.out.println("User created successfully! Please log in.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void roomManagementMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n=== Room Management Menu ===");
            System.out.println("1. View All Rooms");
            System.out.println("2. Apply for a Room");
            System.out.println("3. Cancel an Application");
            System.out.println("4. View Application History");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1 -> viewAllRooms();
                case 2 -> applyForRoom(scanner);
                case 3 -> cancelApplication(scanner);
                case 4 -> viewApplicationHistory(scanner);
                case 5 -> {
                    System.out.println("Logged out successfully.");
                    loggedInUser = null;
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static List<User> loadUsers() {
    File file = new File(USERS_FILE);
    if (!file.exists()) {
        try {
            file.createNewFile(); // Create the file if it doesn't exist
            try (Writer writer = new FileWriter(file)) {
                writer.write("[]"); // Initialize with an empty JSON array
            }
            System.out.println("Created new users.json file.");
        } catch (IOException e) {
            System.out.println("Error creating users.json file.");
            e.printStackTrace();
        }
    }

    try (Reader reader = new FileReader(file)) {
        Type listType = new TypeToken<List<User>>() {}.getType();
        return gson.fromJson(reader, listType);
    } catch (IOException e) {
        e.printStackTrace();
        return new ArrayList<>(); // Return an empty list if there's an error
    }
}

    private static void saveUsers(List<User> users) {
        try (Writer writer = new FileWriter(USERS_FILE)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private static void viewAllRooms() {
        // Implement HTTP GET for /rooms (existing functionality)
    }

    private static void applyForRoom(Scanner scanner) {
        // Implement HTTP POST for /apply (existing functionality)
    }

    private static void cancelApplication(Scanner scanner) {
        // Implement HTTP DELETE for /cancel/{applicationId} (existing functionality)
    }

    private static void viewApplicationHistory(Scanner scanner) {
        // Implement HTTP GET for /history/{userId} (existing functionality)
    }
}


