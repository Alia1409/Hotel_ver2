package service;

import model.Reservation;
import model.Room;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for managing hotel bookings.
 * Handles availability checking, calculating bills, updating room states during check-in/out,
 * and persisting reservation records to a local file.
 */
public class ReservationService {
    private List<Reservation> reservations;
    private RoomService roomService; // Dependency to interact with room data
    private final String FILE_NAME = "reservations.txt";

    public ReservationService(RoomService roomService) {
        this.roomService = roomService;
        this.reservations = new ArrayList<>();
        loadReservationsFromFile(); // Automatically pulls records when app turns on
    }

    public List<Reservation> getAllReservations() {
        return reservations;
    }

    /**
     * Checks if a room is free for a requested date range.
     * Compares the requested check-in/out dates against all existing active reservations for that room.
     */
    public boolean isRoomAvailable(String roomNumber, LocalDate start, LocalDate end) {
        for (Reservation res : reservations) {
            // Only check against reservations that are active (not cancelled)
            if (res.getRoomNumber().equals(roomNumber) && !res.getStatus().equalsIgnoreCase("cancelled")) {
                // Algorithmic check: Overlap detection
                // If the requested start is before an existing check-out AND the requested end is after an existing check-in, they overlap.
                if (start.isBefore(res.getCheckOutDate()) && end.isAfter(res.getCheckInDate())) {
                    return false; 
                }
            }
        }
        return true;
    }

    // CREATE
    public boolean createReservation(String id, String roomNumber, String guestName, String email, 
                                     String phone, LocalDate checkIn, LocalDate checkOut, String notes) {
        
        // Validation: Check if room exists and is globally available for booking
        Room room = roomService.findRoomByNumber(roomNumber);
        if (room == null || !room.getStatus().equalsIgnoreCase("available")) return false;
        
        // Validation: Check for date overlaps with other bookings
        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) return false;
        
        // Validation: Ensure check-out is strictly after check-in
        if (checkOut.isBefore(checkIn) || checkIn.equals(checkOut)) return false;

        // Calculate the total cost based on the number of nights stayed
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double totalBill = nights * room.getPricePerNight();

        Reservation newRes = new Reservation(id, roomNumber, guestName, email, phone, checkIn, checkOut, totalBill, "confirmed", notes);
        reservations.add(newRes);
        saveReservationsToFile(); // Saves instantly to reservations.txt
        return true;
    }

    // UPDATE
    public boolean updateReservation(String id, String roomNumber, String guestName, String email, 
                                     String phone, LocalDate checkIn, LocalDate checkOut, String notes, String status) {
        Reservation res = findReservationById(id);
        if (res == null) return false;

        Room room = roomService.findRoomByNumber(roomNumber);
        if (room == null) return false;

        // Temporarily remove this exact reservation to check for timeline overlaps 
        // This prevents the availability check from colliding with the reservation's own old dates
        reservations.remove(res);
        boolean available = isRoomAvailable(roomNumber, checkIn, checkOut);
        
        // If dates overlap with someone else (and we aren't just cancelling the booking), abort the update
        if (!available && !status.equalsIgnoreCase("cancelled")) {
            reservations.add(res); // restore the original record if the update failed
            return false;
        }

        // Recalculate the bill in case the dates or room type changed
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double totalBill = nights * room.getPricePerNight();

        // Apply new values
        res.setRoomNumber(roomNumber);
        res.setGuestName(guestName);
        res.setEmail(email);
        res.setPhone(phone);
        res.setCheckInDate(checkIn);
        res.setCheckOutDate(checkOut);
        res.setTotalAmount(totalBill);
        res.setStatus(status);
        res.setSpecialNotes(notes);

        reservations.add(res);
        saveReservationsToFile(); // Commit changes to flat file
        return true;
    }

    // DELETE
    public void deleteReservation(String id) {
        Reservation res = findReservationById(id);
        if (res != null) {
            reservations.remove(res);
            saveReservationsToFile();
        }
    }

    /**
     * Handles the guest arrival process.
     * Updates the reservation status and physically marks the room as occupied.
     */
    public void processCheckIn(String id) {
        Reservation res = findReservationById(id);
        if (res != null && res.getStatus().equalsIgnoreCase("confirmed")) {
            res.setStatus("in-progress");
            Room r = roomService.findRoomByNumber(res.getRoomNumber());
            if (r != null) r.setStatus("occupied");
            
            // Sync both databases to reflect the real-world state
            roomService.saveRoomsToFile();
            saveReservationsToFile();
        }
    }

    /**
     * Handles the guest departure process.
     * Completes the reservation and physically marks the room as available again.
     */
    public void processCheckOut(String id) {
        Reservation res = findReservationById(id);
        if (res != null && res.getStatus().equalsIgnoreCase("in-progress")) {
            res.setStatus("completed");
            Room r = roomService.findRoomByNumber(res.getRoomNumber());
            if (r != null) r.setStatus("available");
            
            // Sync both databases to reflect the real-world state
            roomService.saveRoomsToFile();
            saveReservationsToFile();
        }
    }

    private Reservation findReservationById(String id) {
        for (Reservation r : reservations) {
            if (r.getId().equals(id)) return r;
        }
        return null;
    }

    // PERSISTENCE SAVE: Writes ALL guest details down to disk
    private void saveReservationsToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Reservation r : reservations) {
                // Encodes all variables cleanly using comma separation strings.
                // Replaces commas in the notes field with spaces so it doesn't break the CSV columns during loading.
                String notes = (r.getSpecialNotes() == null || r.getSpecialNotes().trim().isEmpty()) ? "NONE" : r.getSpecialNotes().replace(",", " ");
                out.println(r.getId() + "," +
                            r.getRoomNumber() + "," +
                            r.getGuestName() + "," +
                            r.getEmail() + "," +
                            r.getPhone() + "," +
                            r.getCheckInDate() + "," +
                            r.getCheckOutDate() + "," +
                            r.getTotalAmount() + "," +
                            r.getStatus() + "," +
                            notes);
            }
        } catch (IOException e) {
            System.err.println("Critical Error writing to records ledger: " + e.getMessage());
        }
    }

    // PERSISTENCE LOAD: Restores guest data fields into memory on application startup
    private void loadReservationsFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = line.split(",");
                if (tokens.length >= 10) {
                    // Reconstruct variables from the CSV line
                    String id = tokens[0];
                    String roomNum = tokens[1];
                    String name = tokens[2];
                    String email = tokens[3];
                    String phone = tokens[4];
                    LocalDate inDate = LocalDate.parse(tokens[5]);
                    LocalDate outDate = LocalDate.parse(tokens[6]);
                    double bill = Double.parseDouble(tokens[7]);
                    String status = tokens[8];
                    String notes = tokens[9];

                    Reservation res = new Reservation(id, roomNum, name, email, phone, inDate, outDate, bill, status, notes);
                    reservations.add(res);
                }
            }
        } catch (Exception e) {
            System.err.println("Critical Error reading database ledger: " + e.getMessage());
        }
    }
}
