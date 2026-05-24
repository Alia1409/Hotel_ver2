package service;

import model.Reservation;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.temporal.ChronoUnit; // Added import for calculation
import java.util.List;

/**
 * Utility service dedicated to generating external reports.
 * Allows hotel staff to export system data into a spreadsheet-compatible format (CSV).
 */
public class ExportService {
    // Hardcoded target file name for the export report
    private static final String CSV_FILE = "monthly_reservation_report.csv";

    /**
     * Takes the current list of reservations and writes them to a comma-separated values file.
     */
    public static void exportReservationsToCSV(List<Reservation> reservations) {
        // Using try-with-resources to ensure the file writer stream closes automatically when finished
        try (PrintWriter out = new PrintWriter(new FileWriter(CSV_FILE))) {
            
            // 1. Write the column headers for the top row of the spreadsheet
            out.println("Reservation ID,Room Number,Guest Name,Email,Phone,Check-In Date,Check-Out Date,Nights,Total Bill,Status");
            
            // 2. Loop through each booking and write its data as a new row
            for (Reservation res : reservations) {
                // Calculate nights dynamically from the check-in and check-out dates
                long nights = ChronoUnit.DAYS.between(res.getCheckInDate(), res.getCheckOutDate());

                // Concatenate all reservation fields separated by commas to match the headers
                out.println(res.getId() + "," + res.getRoomNumber() + "," + res.getGuestName() + "," +
                            res.getEmail() + "," + res.getPhone() + "," + res.getCheckInDate() + "," +
                            res.getCheckOutDate() + "," + nights + "," + // Used the calculated value here
                            res.getTotalAmount() + "," + res.getStatus());
            }
        } catch (IOException e) {
            // Failsafe in case the file is locked, open in another program, or lacks write permissions
            System.out.println("Failure exporting spreadsheet logs.");
        }
    }
}
