package service;

import model.Reservation;
import model.Room;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    private List<Reservation> reservations;
    private RoomService roomService;
    private final String FILE_NAME = "reservations.txt";

    public ReservationService(RoomService roomService) {
        this.roomService = roomService;
        this.reservations = new ArrayList<>();
        loadReservationsFromFile(); // Automatically pulls records when app turns on
    }

    public List<Reservation> getAllReservations() {
        return reservations;
    }

    public boolean isRoomAvailable(String roomNumber, LocalDate start, LocalDate end) {
        for (Reservation res : reservations) {
            if (res.getRoomNumber().equals(roomNumber) && !res.getStatus().equalsIgnoreCase("cancelled")) {
                // Algorithmic check: Overlap detection
                if (start.isBefore(res.getCheckOutDate()) && end.isAfter(res.getCheckInDate())) {
                    return false; 
                }
            }
        }
        return true;
    }

    public boolean createReservation(String id, String roomNumber, String guestName, String email, 
                                     String phone, LocalDate checkIn, LocalDate checkOut, String notes) {
        Room room = roomService.findRoomByNumber(roomNumber);
        if (room == null || !room.getStatus().equalsIgnoreCase("available")) return false;
        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) return false;
        if (checkOut.isBefore(checkIn) || checkIn.equals(checkOut)) return false;

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double totalBill = nights * room.getPricePerNight();

        Reservation newRes = new Reservation(id, roomNumber, guestName, email, phone, checkIn, checkOut, totalBill, "confirmed", notes);
        reservations.add(newRes);
        saveReservationsToFile(); // Saves instantly to reservations.txt
        return true;
    }

    public boolean updateReservation(String id, String roomNumber, String guestName, String email, 
                                     String phone, LocalDate checkIn, LocalDate checkOut, String notes, String status) {
        Reservation res = findReservationById(id);
        if (res == null) return false;

        Room room = roomService.findRoomByNumber(roomNumber);
        if (room == null) return false;

        // Temporarily remove to check for timeline overlaps without colliding with oneself
        reservations.remove(res);
        boolean available = isRoomAvailable(roomNumber, checkIn, checkOut);
        if (!available && !status.equalsIgnoreCase("cancelled")) {
            reservations.add(res); // restore if failed
            return false;
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double totalBill = nights * room.getPricePerNight();

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

    public void deleteReservation(String id) {
        Reservation res = findReservationById(id);
        if (res != null) {
            reservations.remove(res);
            saveReservationsToFile();
        }
    }

    public void processCheckIn(String id) {
        Reservation res = findReservationById(id);
        if (res != null && res.getStatus().equalsIgnoreCase("confirmed")) {
            res.setStatus("in-progress");
            Room r = roomService.findRoomByNumber(res.getRoomNumber());
            if (r != null) r.setStatus("occupied");
            roomService.saveRoomsToFile();
            saveReservationsToFile();
        }
    }

    public void processCheckOut(String id) {
        Reservation res = findReservationById(id);
        if (res != null && res.getStatus().equalsIgnoreCase("in-progress")) {
            res.setStatus("completed");
            Room r = roomService.findRoomByNumber(res.getRoomNumber());
            if (r != null) r.setStatus("available");
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
                // Encodes all variables cleanly using comma separation strings
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