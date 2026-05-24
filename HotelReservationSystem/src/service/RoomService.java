package service;

import model.Room;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for managing the hotel's room inventory.
 * Handles all CRUD operations and maintains data persistence using a local text file.
 */
public class RoomService {
    // In-memory cache of rooms to serve fast UI read operations
    private List<Room> rooms;
    // The local file used as a lightweight database
    private final String FILE_NAME = "rooms.txt";

    public RoomService() {
        // Load existing data from the file on startup
        this.rooms = loadRoomsFromFile();
        
        // Seed the application with default rooms if the file is empty or newly created
        if (rooms.isEmpty()) {
            addRoom(new Room("101", "single", 1, 80.0, "available", "Street View"));
            addRoom(new Room("102", "double", 1, 120.0, "available", "Courtyard View"));
            addRoom(new Room("201", "suite", 2, 250.0, "available", "Ocean View Balcony"));
        }
    }

    // CREATE
    public void addRoom(Room room) {
        rooms.add(room);
        saveRoomsToFile(); // Persist changes immediately
    }

    // READ
    public List<Room> getAllRooms() { return rooms; }

    public Room findRoomByNumber(String roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber().equals(roomNumber)) return r;
        }
        return null; // Return null if the room doesn't exist in the system
    }

    // UPDATE
    public void updateRoom(String oldRoomNum, Room updatedRoom) {
        Room r = findRoomByNumber(oldRoomNum);
        if (r != null) {
            // Map the updated values into the existing managed object
            r.setRoomNumber(updatedRoom.getRoomNumber());
            r.setType(updatedRoom.getType());
            r.setFloor(updatedRoom.getFloor());
            r.setPricePerNight(updatedRoom.getPricePerNight());
            r.setStatus(updatedRoom.getStatus());
            r.setDescription(updatedRoom.getDescription());
            
            saveRoomsToFile(); // Sync updated state to the file
        }
    }

    // Specialized update method just for changing availability status (e.g., during check-in/out)
    public void updateRoomStatus(String roomNumber, String newStatus) {
        Room room = findRoomByNumber(roomNumber);
        if (room != null) {
            room.setStatus(newStatus);
            saveRoomsToFile();
        }
    }

    // DELETE
    public void deleteRoom(String roomNumber) {
        // Remove any room matching the target ID
        rooms.removeIf(r -> r.getRoomNumber().equals(roomNumber));
        saveRoomsToFile(); // Sync updated state to the file
    }

    /**
     * Writes the current in-memory list of rooms to the text file.
     * Uses a simple comma-separated values (CSV) format.
     */
    public void saveRoomsToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Room r : rooms) {
                out.println(r.getRoomNumber() + "," + r.getType() + "," + r.getFloor() + "," + 
                            r.getPricePerNight() + "," + r.getStatus() + "," + r.getDescription());
            }
        } catch (IOException e) {
            System.out.println("Error saving rooms data.");
        }
    }

    /**
     * Reads the text file and parses the comma-separated strings back into Room objects.
     */
    private List<Room> loadRoomsFromFile() {
        List<Room> loadedRooms = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return loadedRooms; // Return an empty list if no previous save exists

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            // Read file line by line, splitting on commas
            while ((line = reader.readLine()) != null) {
                String[] t = line.split(",");
                if (t.length >= 6) {
                    loadedRooms.add(new Room(t[0], t[1], Integer.parseInt(t[2]), 
                                   Double.parseDouble(t[3]), t[4], t[5]));
                }
            }
        } catch (Exception e) {
            // Catches parsing errors (e.g., if a manual edit to the file breaks the format)
            System.out.println("Notice: Started fresh local runtime instance.");
        }
        return loadedRooms;
    }
}package service;

import model.Room;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    private List<Room> rooms;
    private final String FILE_NAME = "rooms.txt";

    public RoomService() {
        this.rooms = loadRoomsFromFile();
        if (rooms.isEmpty()) {
            addRoom(new Room("101", "single", 1, 80.0, "available", "Street View"));
            addRoom(new Room("102", "double", 1, 120.0, "available", "Courtyard View"));
            addRoom(new Room("201", "suite", 2, 250.0, "available", "Ocean View Balcony"));
        }
    }

    // CREATE
    public void addRoom(Room room) {
        rooms.add(room);
        saveRoomsToFile();
    }

    // READ
    public List<Room> getAllRooms() { return rooms; }

    public Room findRoomByNumber(String roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber().equals(roomNumber)) return r;
        }
        return null;
    }

    // UPDATE
    public void updateRoom(String oldRoomNum, Room updatedRoom) {
        Room r = findRoomByNumber(oldRoomNum);
        if (r != null) {
            r.setRoomNumber(updatedRoom.getRoomNumber());
            r.setType(updatedRoom.getType());
            r.setFloor(updatedRoom.getFloor());
            r.setPricePerNight(updatedRoom.getPricePerNight());
            r.setStatus(updatedRoom.getStatus());
            r.setDescription(updatedRoom.getDescription());
            saveRoomsToFile();
        }
    }

    public void updateRoomStatus(String roomNumber, String newStatus) {
        Room room = findRoomByNumber(roomNumber);
        if (room != null) {
            room.setStatus(newStatus);
            saveRoomsToFile();
        }
    }

    // DELETE
    public void deleteRoom(String roomNumber) {
        rooms.removeIf(r -> r.getRoomNumber().equals(roomNumber));
        saveRoomsToFile();
    }

    public void saveRoomsToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Room r : rooms) {
                out.println(r.getRoomNumber() + "," + r.getType() + "," + r.getFloor() + "," + 
                            r.getPricePerNight() + "," + r.getStatus() + "," + r.getDescription());
            }
        } catch (IOException e) {
            System.out.println("Error saving rooms data.");
        }
    }

    private List<Room> loadRoomsFromFile() {
        List<Room> loadedRooms = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return loadedRooms;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] t = line.split(",");
                if (t.length >= 6) {
                    loadedRooms.add(new Room(t[0], t[1], Integer.parseInt(t[2]), 
                                   Double.parseDouble(t[3]), t[4], t[5]));
                }
            }
        } catch (Exception e) {
            System.out.println("Notice: Started fresh local runtime instance.");
        }
        return loadedRooms;
    }
}
