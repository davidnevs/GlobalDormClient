package globaldormclient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GlobalDormClient {
    private static final String BASE_URL = "http://localhost:8080/GlobalDorm/Dorm/rooms";
    private static final String USERS_FILE = "users.json";
    private static final Gson gson = new Gson();
    private static String loggedInUser = null;
    private static boolean isAdmin = false;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) { // Main loop to handle login and logout transitions
            // Login loop
            while (loggedInUser == null) {
                System.out.println("\n=== Welcome to GlobalDorm Client ===");
                System.out.println("1. Login");
                System.out.println("2. Create New User");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1 -> login(scanner); // Attempt login
                    case 2 -> createNewUser(scanner); // Create a new user
                    case 3 -> {
                        System.out.println("Exiting...");
                        scanner.close();
                        return; // Exit the program
                    }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            }
            
            if (isAdmin){
                adminMenu(scanner);
            }else{
                roomManagementMenu(scanner);
            }
            // User is logged in, proceed to Room Management Menu unless an Admin
            // After logout, loggedInUser will be null, and the program will return to the login loop            
        }
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
                isAdmin = user.isAdmin(); // Set admin flag
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

            System.out.print("Is this an admin account? (yes/no): ");
            isAdmin = scanner.nextLine().equalsIgnoreCase("yes");

            // Validate username uniqueness
            List<User> users = loadUsers();
            if (users.stream().anyMatch(user -> user.getUsername().equals(username))) {
                System.out.println("Error: Username already exists.");
                return;
            }

            // Hash the password
            String hashedPassword = hashPassword(password);

            // Add the new user to the list and save to file
            users.add(new User(username, hashedPassword, isAdmin));
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
            System.out.println("5. Check Distance to a Room");
            System.out.println("6. Check Weather");
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1 -> viewAllRooms();
                case 2 -> applyForRoom(scanner);
                case 3 -> cancelApplication(scanner);
                case 4 -> viewApplicationHistory(scanner);
                case 5 -> checkDistanceToRoom(scanner);
                case 6 -> checkWeather(scanner); // New menu option
                case 7 -> {
                    System.out.println("Logged out successfully.");
                    loggedInUser = null;
                    return; // Exit the menu and return to login
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void adminMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n=== Admin Menu ===");
            System.out.println("1. Accept Room Offer");
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1 -> acceptRoomOffer(scanner);
                case 2 -> {
                    System.out.println("Logged out successfully.");
                    loggedInUser = null;
                    isAdmin = false;
                    return; // Exit the admin menu
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
        try {
            URL url = new URL(BASE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                System.out.println("\n=== All Rooms ===");
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
                in.close();
            } else {
                System.out.println("Error: Unable to fetch rooms. HTTP Code: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void applyForRoom(Scanner scanner) {
        try {
            System.out.print("Enter Application ID: ");
            long applicationId = scanner.nextLong();

            // Validate applicationId
            if (applicationId <= 0) {
                System.out.println("Error: Application ID must be a positive number.");
                return;
            }

            System.out.print("Enter Room ID: ");
            long roomId = scanner.nextLong();

            // Validate roomId
            if (roomId <= 0) {
                System.out.println("Error: Room ID must be a positive number.");
                return;
            }

            scanner.nextLine(); // Consume newline

            // Use the logged-in user for userId
            String userId = loggedInUser;

            // Create JSON payload
            String jsonInput = String.format(
                "{\"applicationId\":%d,\"roomId\":%d,\"userId\":\"%s\"}",
                applicationId, roomId, userId
            );

            // Make the POST request
            URL url = new URL(BASE_URL + "/apply");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send the JSON payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Handle the server response
            if (connection.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response;
                System.out.println("\n=== Application Response ===");
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }
                in.close();
            } else {
                System.out.println("Error: Unable to apply for room. HTTP Code: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            System.out.println("An error occurred while applying for the room:");
            e.printStackTrace();
        }
    }

    private static void cancelApplication(Scanner scanner) {
        
        viewApplicationHistory(scanner);
        // Prompt user for application ID to cancel
        try {
            System.out.print("\nEnter Application ID to Cancel: ");
            long applicationId = scanner.nextLong();

            // Validate applicationId
            if (applicationId <= 0) {
                System.out.println("Error: Application ID must be a positive number.");
                return;
            }

            // Make the DELETE request
            URL url = new URL(BASE_URL + "/cancel/" + applicationId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");

            // Handle the server response
            switch (connection.getResponseCode()) {
                case 200 -> {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String response;
                        System.out.println("\n=== Cancel Response ===");
                        while ((response = in.readLine()) != null) {
                            System.out.println(response);
                        }
                    }
                }

                case 404 -> System.out.println("Error: Application not found.");
                default -> System.out.println("Error: Unable to cancel application. HTTP Code: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            System.out.println("An error occurred while cancelling the application:");
            e.printStackTrace();
        }
    }

    private static void viewApplicationHistory(Scanner scanner) {
        // Display user's application history
        System.out.println("\n=== Your Applications ===");
        try {
            // Make the GET request for the logged-in user's application history
            URL url = new URL(BASE_URL + "/history/" + loggedInUser);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Handle the server response
            switch (connection.getResponseCode()) {
                case 200 -> {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String response;
                        System.out.println("\n=== Application History ===");
                        while ((response = in.readLine()) != null) {
                            System.out.println(response);
                        }
                    }
                }
                case 404 -> System.out.println("Error: No application history found for the logged-in user.");
                default -> System.out.println("Error: Unable to fetch application history. HTTP Code: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            System.out.println("An error occurred while fetching the application history:");
            e.printStackTrace();
        }
    }
    
    private static void acceptRoomOffer(Scanner scanner) {
        if (!isAdmin) {
            System.out.println("Error: Only admin users can accept room offers.");
            return;
        }

        try {
            // Fetch all pending applications
            System.out.println("\n=== Pending Room Applications ===");
            URL url = new URL(BASE_URL + "/applications/pending");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response;
                boolean hasPending = false;

                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                    hasPending = true;
                }
                in.close();

                if (!hasPending) {
                    System.out.println("No pending room applications found.");
                    return;
                }
            } else {
                System.out.println("Error: Unable to fetch pending applications. HTTP Code: " + connection.getResponseCode());
                return;
            }

            // Prompt admin to accept an application
            System.out.print("\nEnter Application ID to Accept: ");
            long applicationId = scanner.nextLong();

            // Validate applicationId
            if (applicationId <= 0) {
                System.out.println("Error: Application ID must be a positive number.");
                return;
            }

            // Make the PUT request to accept the offer
            URL acceptUrl = new URL(BASE_URL + "/accept/" + applicationId);
            HttpURLConnection acceptConnection = (HttpURLConnection) acceptUrl.openConnection();
            acceptConnection.setRequestMethod("PUT");

            switch (acceptConnection.getResponseCode()) {
                case 200 -> {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(acceptConnection.getInputStream()))) {
                        String response;
                        System.out.println("\n=== Accept Offer Response ===");
                        while ((response = in.readLine()) != null) {
                            System.out.println(response);
                        }
                    }
                }

                case 404 -> System.out.println("Error: Application not found.");
                default -> System.out.println("Error: Unable to accept offer. HTTP Code: " + acceptConnection.getResponseCode());
            }
        } catch (IOException e) {
            System.out.println("An error occurred while accepting the offer:");
            e.printStackTrace();
        }
    }
    
    private static void checkDistanceToRoom(Scanner scanner) {
        if (loggedInUser == null) {
            System.out.println("Error: No user is currently logged in.");
            return;
        }

        try {
            // Prompt for user location (postcode)
            System.out.print("Enter your postcode: ");
            String userPostcode = scanner.nextLine();

            // Prompt for room ID
            System.out.print("Enter the Room ID: ");
            long roomId = scanner.nextLong();

            // Validate room ID
            if (roomId <= 0) {
                System.out.println("Error: Room ID must be a positive number.");
                return;
            }

            // Call the proximity endpoint
            URL url = new URL(BASE_URL + "/proximity?userPostcode=" + URLEncoder.encode(userPostcode, "UTF-8") + "&roomId=" + roomId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Handle the server response
            switch (connection.getResponseCode()) {
                case 200 -> {
                    StringBuilder response;
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                    }
                    // Parse the JSON response and extract distance
                    double distanceMeters = parseDistanceFromResponse(response.toString());
                    if (distanceMeters >= 0) {
                        double distanceKilometers = distanceMeters / 1000.0; // Convert meters to kilometers
                        System.out.printf("\nThe distance to the room is %.2f km.%n", distanceKilometers);
                    } else {
                        System.out.println("Error: Unable to extract distance from response.");
                    }
                }

                case 404 -> System.out.println("Error: Room not found or invalid postcode.");
                default -> System.out.println("Error: Unable to fetch proximity data. HTTP Code: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            System.out.println("An error occurred while checking proximity:");
            e.printStackTrace();
        }
    }
    
    private static double parseDistanceFromResponse(String jsonResponse) {
        try {
            // Parse the JSON response using Gson
            JsonObject jsonObject = new Gson().fromJson(jsonResponse, JsonObject.class);
            JsonArray routes = jsonObject.getAsJsonArray("routes");
            if (routes != null && routes.size() > 0) {
                JsonObject route = routes.get(0).getAsJsonObject();
                return route.get("distance").getAsDouble(); // Extract distance in meters
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if parsing fails
    }
    
    private static void checkWeather(Scanner scanner) {
        try {
            System.out.println("\n=== Check Weather ===");
            System.out.println("1. Check Weather by Room ID");
            System.out.println("2. Check Weather by Postcode");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            String endpoint;
            switch (choice) {
                case 1 -> {
                    // Check weather by Room ID
                    System.out.print("Enter the Room ID: ");
                    long roomId = scanner.nextLong();
                    scanner.nextLine(); // Consume newline
                    // Validate Room ID
                    if (roomId <= 0) {
                        System.out.println("Error: Room ID must be a positive number.");
                        return;
                    }   endpoint = BASE_URL + "/rooms/weather?roomId=" + roomId;
                }
                case 2 -> {
                    // Check weather by Postcode
                    System.out.print("Enter the Postcode: ");
                    String postcode = scanner.nextLine();
                    // Validate Postcode
                    if (postcode.isBlank()) {
                        System.out.println("Error: Postcode cannot be blank.");
                        return;
                    }   endpoint = BASE_URL + "/weather?postcode=" + URLEncoder.encode(postcode, "UTF-8");
                }
                default -> {
                    System.out.println("Invalid option.");
                    return;
                }
            }

            // Call the weather endpoint
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Handle the server response
            if (connection.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String response;
                    System.out.println("\n=== Weather Information ===");
                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                    }
                }
            } else {
                System.out.println("Error: Unable to fetch weather data. HTTP Code: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            System.out.println("An error occurred while checking weather:");
            e.printStackTrace();
        }
    }
}


