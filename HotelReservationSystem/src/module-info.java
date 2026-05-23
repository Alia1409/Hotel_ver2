/**
 * 
 */
/**
 * 
 */
module HotelReservationSystem {
    requires java.desktop; 
    requires javafx.controls;
    requires javafx.fxml;
    
    exports ui;
    opens model to javafx.base;
}