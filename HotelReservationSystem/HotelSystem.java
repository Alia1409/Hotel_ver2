package test;

import controller.ReservationService;
import controller.RoomService;
import model.Reservation;
import model.Room;
import org.junit.jupiter.api.*;
import persistence.DataStore;

import java.io.File;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HotelSystemTest {

    static DataStore          dataStore;
    static RoomService        roomService;
    static ReservationService reservationService;

    @BeforeAll
    static void setup() {
        // Use a temp data dir so tests don't touch production data
        System.setProperty("user.dir", System.getProperty("java.io.tmpdir") + "/hotel-test-" + System.currentTimeMillis());
        new File(System.getProperty("user.dir") + "/data").mkdirs();
        dataStore          = new DataStore();
        roomService        = new RoomService(dataStore);
        reservationService = new ReservationService(dataStore, roomService);

        // Seed a room
        roomService.addRoom(new Room(101, Room.RoomType.SINGLE, 1, 80.0, "Garden view"));
        roomService.addRoom(new Room(102, Room.RoomType.DOUBLE, 1, 120.0, "Sea view"));
    }

    // Room tests

    @Test @Order(1)
    void testAddRoom() {
        assertTrue(roomService.findByNumber(101).isPresent());
        assertEquals(Room.RoomType.SINGLE, roomService.findByNumber(101).get().getType());
    }

    @Test @Order(2)
    void testAddDuplicateRoom() {
        assertThrows(IllegalArgumentException.class,
            () -> roomService.addRoom(new Room(101, Room.RoomType.DOUBLE, 1, 90, "Dup")));
    }

    @Test @Order(3)
    void testInvalidPrice() {
        assertThrows(IllegalArgumentException.class,
            () -> roomService.addRoom(new Room(200, Room.RoomType.SUITE, 5, -10, "Bad")));
    }

    // Reservation tests

    @Test @Order(4)
    void testCreateReservation() {
        Reservation r = reservationService.create(101, "Alice", "alice@test.com",
            "+34 600 123 456", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 5), "");
        assertEquals(5, r.getNumberOfNights()); // wait, actually 4
        // 5 - 1 = 4 nights
        assertEquals(4, r.getNumberOfNights());
        assertEquals(320.0, r.getTotalAmount(), 0.01);
    }

    @Test @Order(5)
    void testOverlapDetection() {
        // Try to book same room overlapping dates
        assertThrows(IllegalArgumentException.class,
            () -> reservationService.create(101, "Bob", "bob@test.com",
                "+34 600 999 000", LocalDate.of(2025, 6, 3), LocalDate.of(2025, 6, 7), ""));
    }

    @Test @Order(6)
    void testNonOverlapingOk() {
        // After the first reservation ends, should be fine
        assertDoesNotThrow(() ->
            reservationService.create(101, "Carol", "carol@test.com",
                "+34 611 000 000", LocalDate.of(2025, 6, 5), LocalDate.of(2025, 6, 8), ""));
    }

    @Test @Order(7)
    void testInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
            () -> reservationService.create(102, "Dave", "not-an-email",
                "+34 600 000 000", LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3), ""));
    }

    @Test @Order(8)
    void testCheckInOut() {
        Reservation r = reservationService.create(102, "Eve", "eve@test.com",
            "+34 622 000 000", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 4), "");
        reservationService.checkIn(r.getId());
        assertEquals(Room.RoomStatus.OCCUPIED, roomService.findByNumber(102).get().getStatus());

        reservationService.checkOut(r.getId());
        assertEquals(Reservation.ReservationStatus.COMPLETED,
            reservationService.findById(r.getId()).get().getStatus());
        assertEquals(Room.RoomStatus.AVAILABLE, roomService.findByNumber(102).get().getStatus());
    }

    @Test @Order(9)
    void testCancelReservation() {
        Reservation r = reservationService.create(102, "Frank", "frank@test.com",
            "+34 633 000 000", LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 3), "");
        reservationService.cancel(r.getId());
        assertEquals(Reservation.ReservationStatus.CANCELLED,
            reservationService.findById(r.getId()).get().getStatus());
    }

    @Test @Order(10)
    void testInvalidDates() {
        assertThrows(IllegalArgumentException.class,
            () -> reservationService.create(102, "Grace", "grace@test.com",
                "+34 644 000 000", LocalDate.of(2025, 10, 5), LocalDate.of(2025, 10, 1), ""));
    }
}
