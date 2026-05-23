package service;

import model.Reservation;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.temporal.ChronoUnit; // Added import for calculation
import java.util.List;

public class ExportService {
    private static final String CSV_FILE = "monthly_reservation_report.csv";

    public static void exportReservationsToCSV(List<Reservation> reservations) {
        try (PrintWriter out = new PrintWriter(new FileWriter(CSV_FILE))) {
            out.println("Reservation ID,Room Number,Guest Name,Email,Phone,Check-In Date,Check-Out Date,Nights,Total Bill,Status");
            for (Reservation res : reservations) {
                // Calculate nights dynamically from the check-in and check-out dates
                long nights = ChronoUnit.DAYS.between(res.getCheckInDate(), res.getCheckOutDate());

                out.println(res.getId() + "," + res.getRoomNumber() + "," + res.getGuestName() + "," +
                            res.getEmail() + "," + res.getPhone() + "," + res.getCheckInDate() + "," +
                            res.getCheckOutDate() + "," + nights + "," + // Used the calculated value here
                            res.getTotalAmount() + "," + res.getStatus());
            }
        } catch (IOException e) {
            System.out.println("Failure exporting spreadsheet logs.");
        }
    }
}