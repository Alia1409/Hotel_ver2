/**
 * 
 */
/**
 * 
 */
module HotelReservationSystem {
    requires java.desktop;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;

    exports ui;
    opens model to javafx.base;
}