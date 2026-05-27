package service.ai;

import model.Room;
import service.RoomService;

import java.util.List;

public class HotelContextBuilder {
    private final RoomService roomService;

    public HotelContextBuilder(RoomService roomService) {
        this.roomService = roomService;
    }

    public String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful receptionist for Boutique Hotel Ver2.\n\n");
        sb.append("Hotel policies:\n");
        sb.append("- Check-in: 3:00 PM. Check-out: 11:00 AM.\n");
        sb.append("- Payment is collected at the front desk at check-in.\n");
        sb.append("- Cancellations: free up to 48 hours before check-in; otherwise one night is charged.\n");
        sb.append("- Contact: frontdesk@boutiquehotelver2.com | +1 (555) 010-2000\n");
        sb.append("- Amenities: complimentary Wi-Fi, courtyard lounge, no pool on site.\n\n");
        sb.append("Current room inventory (live data):\n");
        List<Room> rooms = roomService.getAllRooms();
        if (rooms.isEmpty()) {
            sb.append("- No rooms in inventory.\n");
        } else {
            for (Room r : rooms) {
                sb.append(String.format(
                        "- Room %s: %s, floor %d, $%.2f/night, status: %s — %s%n",
                        r.getRoomNumber(),
                        r.getType(),
                        r.getFloor(),
                        r.getPricePerNight(),
                        r.getStatus(),
                        r.getDescription()));
            }
        }
        sb.append("\nBooking help:\n");
        sb.append("- To create or modify a reservation, direct staff to the Reservation Manager tab in this app.\n");
        sb.append("- Do not invent guest names, emails, phones, or reservation details.\n");
        sb.append("- If asked about a specific booking, ask for the reservation ID only; do not disclose other guests' information.\n");
        sb.append("- Answer general questions about rooms, rates, policies, and check-in/out using the inventory above.\n");
        sb.append("- Do not provide medical, legal, or financial advice.\n");
        return sb.toString();
    }
}
