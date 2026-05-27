package ui;

import model.Reservation;
import model.Room;
import service.ExportService;
import service.ReservationService;
import service.RoomService;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;

/**
 * Main application class for the Boutique Hotel Reservation System.
 * Handles the JavaFX user interface, routing user actions to the appropriate services.
 */
public class MainApp extends Application {
    // Services for handling business logic
    private RoomService roomService = new RoomService();
    private ReservationService resService = new ReservationService(roomService);

    // Observable lists to automatically update the UI when data changes
    private ObservableList<Room> observableRooms;
    private ObservableList<Reservation> observableReservations;
    
    // State variables to track currently selected items in the tables
    private String selectedReservationId = null;
    private String selectedRoomNumber = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Boutique Hotel Reservation System (Full CRUD)");

        // Initialize observable lists with current database/service data
        observableRooms = FXCollections.observableArrayList(roomService.getAllRooms());
        observableReservations = FXCollections.observableArrayList(resService.getAllReservations());

        // Setup the main layout using a TabPane for navigation
        TabPane tabPane = new TabPane();
        Tab tab1 = new Tab("Daily Occupancy Panel", buildOccupancyView());
        Tab tab2 = new Tab("Reservation Manager (CRUD)", buildReservationView());
        Tab tab3 = new Tab("Room Management (CRUD)", buildRoomView());
        Tab tab4 = new Tab("AI Assistant", new AiChatView(roomService, resService).getRoot());

        // Prevent users from accidentally closing the core tabs
        tab1.setClosable(false); tab2.setClosable(false); tab3.setClosable(false); tab4.setClosable(false);
        tabPane.getTabs().addAll(tab1, tab2, tab3, tab4);

        // Configure and display the main window
        Scene scene = new Scene(tabPane, 1100, 680);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Builds the Daily Occupancy Panel.
     * Allows users to check room availability for a specific target date.
     */
    private VBox buildOccupancyView() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        // Top control bar for date selection
        HBox controlBox = new HBox(10);
        controlBox.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label("Target Verification Date:");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        Button checkButton = new Button("Refresh Status Map");
        controlBox.getChildren().addAll(dateLabel, datePicker, checkButton);

        // List view to display the occupancy status of each room
        ListView<String> statusList = new ListView<>();
        
        // Logic to calculate and display room statuses based on the selected date
        Runnable refreshMap = () -> {
            statusList.getItems().clear();
            LocalDate targetDate = datePicker.getValue();
            if (targetDate == null) return;
            
            for (Room r : roomService.getAllRooms()) {
                boolean available = resService.isRoomAvailable(r.getRoomNumber(), targetDate, targetDate.plusDays(1));
                String visualStatus = available ? " [FREE]" : " [BOOKED/OCCUPIED]";
                if (r.getStatus().equals("maintenance")) visualStatus = " [MAINTENANCE]";
                statusList.getItems().add("Room " + r.getRoomNumber() + " (" + r.getType() + ") - Status: " + visualStatus);
            }
        };

        // Bind the refresh logic to the button and run it once on startup
        checkButton.setOnAction(e -> refreshMap.run());
        refreshMap.run();

        box.getChildren().addAll(controlBox, statusList);
        return box;
    }

    /**
     * Builds the Reservation Manager tab.
     * Contains a form for creating/updating reservations and a table for viewing/managing them.
     */
    private HBox buildReservationView() {
        HBox mainBox = new HBox(15);
        mainBox.setPadding(new Insets(15));

        // Setup the left-side form for reservation details
        VBox formBox = new VBox(10);
        formBox.setPrefWidth(280);

        Label formTitle = new Label("Reservation Record Form");
        formTitle.setStyle("-font-weight: bold; -fx-font-size: 14px;");

        // Form input fields
        TextField txtId = new TextField(); txtId.setPromptText("Booking ID (e.g., RES-001)");
        TextField txtRoomNum = new TextField(); txtRoomNum.setPromptText("Room Number");
        TextField txtName = new TextField(); txtName.setPromptText("Guest Name");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Email Address");
        TextField txtPhone = new TextField(); txtPhone.setPromptText("Phone Number (digits only)");
        DatePicker pickerIn = new DatePicker(LocalDate.now());
        DatePicker pickerOut = new DatePicker(LocalDate.now().plusDays(1));
        TextField txtStatus = new TextField(); txtStatus.setPromptText("Status (confirmed/cancelled)");
        txtStatus.setText("confirmed");
        TextArea txtNotes = new TextArea(); txtNotes.setPromptText("Special Notes");
        txtNotes.setPrefHeight(60);

        // Action buttons for the form
        Button btnCreate = new Button("Add New (Create)");
        Button btnUpdate = new Button("Save Changes (Update)");
        Button btnDelete = new Button("Remove Entry (Delete)");
        btnCreate.setMaxWidth(Double.MAX_VALUE);
        btnUpdate.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setStyle("-fx-base: #e74c3c;"); // Red styling for delete button

        formBox.getChildren().addAll(formTitle, txtId, txtRoomNum, txtName, txtEmail, txtPhone, 
                                    new Label("Check-In:"), pickerIn, new Label("Check-Out:"), pickerOut, 
                                    new Label("Status:"), txtStatus, txtNotes, btnCreate, btnUpdate, btnDelete);

        // Setup the right-side table to display reservations
        VBox tableBox = new VBox(10);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        TableView<Reservation> table = new TableView<>(observableReservations);

        // Map table columns to Reservation object properties
        TableColumn<Reservation, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Reservation, String> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<Reservation, String> colName = new TableColumn<>("Guest");
        colName.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        TableColumn<Reservation, LocalDate> colIn = new TableColumn<>("Check-In");
        colIn.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        TableColumn<Reservation, LocalDate> colOut = new TableColumn<>("Check-Out");
        colOut.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        TableColumn<Reservation, Double> colCash = new TableColumn<>("Bill");
        colCash.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        TableColumn<Reservation, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colId, colRoom, colName, colIn, colOut, colCash, colStatus);

        // Extra action buttons located under the table
        HBox actionRow = new HBox(10);
        Button btnCheckIn = new Button("Log Check-In");
        Button btnCheckOut = new Button("Log Check-Out");
        Button btnExport = new Button("Export to CSV");
        btnExport.setStyle("-fx-base: #2ecc71;"); // Green styling for export
        actionRow.getChildren().addAll(btnCheckIn, btnCheckOut, btnExport);

        tableBox.getChildren().addAll(table, actionRow);
        mainBox.getChildren().addAll(formBox, tableBox);

        // UI Reset Routine to clear reservation fields cleanly after mutations
        Runnable clearFormFields = () -> {
            txtId.clear();
            txtRoomNum.clear();
            txtName.clear();
            txtEmail.clear();
            txtPhone.clear();
            pickerIn.setValue(LocalDate.now());
            pickerOut.setValue(LocalDate.now().plusDays(1));
            txtStatus.setText("confirmed");
            txtNotes.clear();
            selectedReservationId = null;
        };

        // CRUD READ: Selecting an item loads values back into inputs for editing
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedReservationId = newSel.getId();
                txtId.setText(newSel.getId());
                txtRoomNum.setText(newSel.getRoomNumber());
                txtName.setText(newSel.getGuestName());
                txtEmail.setText(newSel.getEmail());
                txtPhone.setText(newSel.getPhone());
                pickerIn.setValue(newSel.getCheckInDate());
                pickerOut.setValue(newSel.getCheckOutDate());
                txtStatus.setText(newSel.getStatus());
                txtNotes.setText(newSel.getSpecialNotes());
            }
        });

        // CRUD CREATE (Enforces RES-001 ID format and Phone digits constraints)
        btnCreate.setOnAction(e -> {
            String inputId = txtId.getText().trim();
            String inputPhone = txtPhone.getText().trim();
            
            // 1. Validation for Booking ID format
            if (!inputId.matches("RES-\\d{3}")) {
                new Alert(Alert.AlertType.ERROR, "Invalid Booking ID format! Please use the 'RES-001' style (RES- followed by 3 digits).").showAndWait();
                return;
            }

            // 2. Validation for Phone Number (7 to 15 numeric digits only)
            if (!inputPhone.matches("\\d{7,15}")) {
                new Alert(Alert.AlertType.ERROR, "Invalid Phone Number! Please enter between 7 and 15 digits only, with no letters or spaces.").showAndWait();
                return;
            }

            // Attempt to create reservation and update UI if successful
            boolean ok = resService.createReservation(inputId, txtRoomNum.getText(), txtName.getText(),
                    txtEmail.getText(), inputPhone, pickerIn.getValue(), pickerOut.getValue(), txtNotes.getText());
            if (ok) {
                observableReservations.setAll(resService.getAllReservations());
                observableRooms.setAll(roomService.getAllRooms());
                clearFormFields.run();
            } else {
                new Alert(Alert.AlertType.ERROR, "Room collision check or date logic failure.").showAndWait();
            }
        });

        // CRUD UPDATE (Enforces validation on edited updates)
        btnUpdate.setOnAction(e -> {
            if (selectedReservationId != null) {
                String inputId = txtId.getText().trim();
                String inputPhone = txtPhone.getText().trim();
                
                // 1. Validation for Booking ID format
                if (!inputId.matches("RES-\\d{3}")) {
                    new Alert(Alert.AlertType.ERROR, "Invalid Booking ID format! Please use the 'RES-001' style.").showAndWait();
                    return;
                }

                // 2. Validation for Phone Number
                if (!inputPhone.matches("\\d{7,15}")) {
                    new Alert(Alert.AlertType.ERROR, "Invalid Phone Number! Please enter between 7 and 15 digits only.").showAndWait();
                    return;
                }

                // Attempt to update and refresh UI
                boolean ok = resService.updateReservation(selectedReservationId, txtRoomNum.getText(), txtName.getText(),
                        txtEmail.getText(), inputPhone, pickerIn.getValue(), pickerOut.getValue(), txtNotes.getText(), txtStatus.getText());
                if (ok) {
                    observableReservations.setAll(resService.getAllReservations());
                    observableRooms.setAll(roomService.getAllRooms());
                    clearFormFields.run();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Room is unavailable for updated target dates.").showAndWait();
                }
            }
        });

        // CRUD DELETE
        btnDelete.setOnAction(e -> {
            if (selectedReservationId != null) {
                resService.deleteReservation(selectedReservationId);
                observableReservations.setAll(resService.getAllReservations());
                clearFormFields.run();
            }
        });

        // Process guest check-in logic
        btnCheckIn.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                resService.processCheckIn(sel.getId());
                observableReservations.setAll(resService.getAllReservations());
                observableRooms.setAll(roomService.getAllRooms());
                clearFormFields.run();
            }
        });

        // Process guest check-out logic
        btnCheckOut.setOnAction(e -> {
            Reservation sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                resService.processCheckOut(sel.getId());
                observableReservations.setAll(resService.getAllReservations());
                observableRooms.setAll(roomService.getAllRooms());
                clearFormFields.run();
            }
        });

        // Trigger CSV export
        btnExport.setOnAction(e -> ExportService.exportReservationsToCSV(resService.getAllReservations()));

        return mainBox;
    }

    /**
     * Builds the Room Management tab.
     * Contains a form and table for tracking the hotel's physical room inventory.
     */
    private HBox buildRoomView() {
        HBox mainBox = new HBox(15);
        mainBox.setPadding(new Insets(15));

        // Setup left-side form for room details
        VBox formBox = new VBox(10);
        formBox.setPrefWidth(250);

        Label title = new Label("Room Inventory Form");
        title.setStyle("-font-weight: bold;");
        
        // Form input fields
        TextField txtNum = new TextField(); txtNum.setPromptText("Room Number");
        TextField txtType = new TextField(); txtType.setPromptText("Type (single/double/suite)");
        TextField txtFloor = new TextField(); txtFloor.setPromptText("Floor");
        TextField txtPrice = new TextField(); txtPrice.setPromptText("Price Per Night");
        TextField txtStatus = new TextField(); txtStatus.setPromptText("Status (available/maintenance)");
        txtStatus.setText("available");
        TextField txtDesc = new TextField(); txtDesc.setPromptText("Room View Specs");
        
        // Action buttons
        Button btnAdd = new Button("Add Room (Create)");
        Button btnUpdateRoom = new Button("Save Changes (Update)");
        Button btnDeleteRoom = new Button("Remove Room (Delete)");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnUpdateRoom.setMaxWidth(Double.MAX_VALUE);
        btnDeleteRoom.setMaxWidth(Double.MAX_VALUE);
        btnDeleteRoom.setStyle("-fx-base: #e74c3c;");

        formBox.getChildren().addAll(title, txtNum, txtType, txtFloor, txtPrice, txtStatus, txtDesc, btnAdd, btnUpdateRoom, btnDeleteRoom);

        // Setup right-side table to display rooms
        TableView<Room> table = new TableView<>(observableRooms);
        HBox.setHgrow(table, Priority.ALWAYS);

        // Map columns to Room object properties
        TableColumn<Room, String> colNum = new TableColumn<>("Room #");
        colNum.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        TableColumn<Room, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Room, Integer> colFloor = new TableColumn<>("Floor");
        colFloor.setCellValueFactory(new PropertyValueFactory<>("floor"));
        TableColumn<Room, Double> colPrice = new TableColumn<>("Price / Night");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerNight"));
        TableColumn<Room, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<Room, String> colDesc = new TableColumn<>("Specs");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.getColumns().addAll(colNum, colType, colFloor, colPrice, colStatus, colDesc);
        mainBox.getChildren().addAll(formBox, table);

        // Helper to reset Room input fields cleanly after an operation
        Runnable clearRoomFields = () -> {
            txtNum.clear();
            txtType.clear();
            txtFloor.clear();
            txtPrice.clear();
            txtStatus.setText("available");
            txtDesc.clear();
            selectedRoomNumber = null;
        };

        // CRUD READ: Clicking a row loads data back into fields for editing
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedRoomNumber = newSel.getRoomNumber();
                txtNum.setText(newSel.getRoomNumber());
                txtType.setText(newSel.getType());
                txtFloor.setText(String.valueOf(newSel.getFloor()));
                txtPrice.setText(String.valueOf(newSel.getPricePerNight()));
                txtStatus.setText(newSel.getStatus());
                txtDesc.setText(newSel.getDescription());
            }
        });

        // CRUD CREATE
        btnAdd.setOnAction(e -> {
            try {
                Room r = new Room(txtNum.getText(), txtType.getText(), Integer.parseInt(txtFloor.getText()),
                        Double.parseDouble(txtPrice.getText()), txtStatus.getText(), txtDesc.getText());
                roomService.addRoom(r);
                observableRooms.setAll(roomService.getAllRooms());
                clearRoomFields.run();
            } catch (Exception ex) {
                // Catches formatting errors (like typing letters in the price/floor fields)
                new Alert(Alert.AlertType.ERROR, "Check numerical parameters fields.").showAndWait();
            }
        });

        // CRUD UPDATE (Includes price rate changes)
        btnUpdateRoom.setOnAction(e -> {
            if (selectedRoomNumber != null) {
                try {
                    Room updated = new Room(txtNum.getText(), txtType.getText(), Integer.parseInt(txtFloor.getText()),
                            Double.parseDouble(txtPrice.getText()), txtStatus.getText(), txtDesc.getText());
                    roomService.updateRoom(selectedRoomNumber, updated);
                    observableRooms.setAll(roomService.getAllRooms());
                    clearRoomFields.run();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Please enter a valid decimal number for the price.").showAndWait();
                }
            }
        });

        // CRUD DELETE
        btnDeleteRoom.setOnAction(e -> {
            if (selectedRoomNumber != null) {
                roomService.deleteRoom(selectedRoomNumber);
                observableRooms.setAll(roomService.getAllRooms());
                clearRoomFields.run();
            }
        });

        return mainBox;
    }
}
