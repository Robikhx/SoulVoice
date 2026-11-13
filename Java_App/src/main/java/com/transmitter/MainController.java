/*
Main controller for the App
-handles the connection between backend and main, backend can't directly modify ui elements
-implements functions and handles connection states and edge cases

*/

package com.transmitter;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

// implements initializable to override initialize for startup 
public class MainController implements Initializable{ 
    // instantiate serialBackend obj
    private SerialBackend backend = new SerialBackend(); 
    private TtsService tts = new TtsService();
    private String lastPort = ""; // last opened port name
   
    private Stage mappingStage = null; // popup mapping reference window
    private Stage myMessagesStage = null; // popup sent messages window 
    private Stage logsStage = null; // popup logs window
   
    @FXML private HBox statusBanner;
    @FXML private Circle statusIndicator;
    @FXML private Label statusLabel;
    @FXML private Button connectBtn;
    @FXML private Button disconnectBtn;
    @FXML private TextArea serialInputArea; // input serial area
    @FXML private TextArea serialOutputArea; // output (received) area
    @FXML private TextArea logsArea; // logs area
    @FXML private TextField messageInput; // message input 
    // initialize ComboBox with ports
    @FXML private ComboBox portComboBox; // port combo box
    @FXML private ComboBox voiceComboBox; // voice combo box


    @Override
    public void initialize(URL url, ResourceBundle rb){
        // Load voices immediately
        try {
            voiceComboBox.getItems().addAll(backend.getAvailableVoices());
            voiceComboBox.getSelectionModel().selectFirst();
        } catch (Exception e) {
            System.err.println("Warning: Could not load voices: " + e.getMessage());
        }
        
        // Load serial ports asynchronously after a short delay
        // This prevents the jSerialComm initialization error from crashing the app at startup
        Platform.runLater(() -> {
            try {
                portComboBox.getItems().addAll(backend.getAvailablePorts());
            } catch (Exception e) {
                System.err.println("Warning: Could not load serial ports: " + e.getMessage());
                portComboBox.getItems().add("(Serial port unavailable)");
            }
        });
    }

    
    // refresh ports
    @FXML
    private void onRefresh(){
        // refresh ports
        try {
            portComboBox.getItems().clear();
            portComboBox.getItems().addAll(backend.getAvailablePorts());
        } catch (Exception e) {
            System.err.println("Warning: Could not refresh serial ports: " + e.getMessage());
        }

        // refresh voices
        try {
            voiceComboBox.getItems().clear();
            voiceComboBox.getItems().addAll(backend.getAvailableVoices());
            voiceComboBox.getSelectionModel().selectFirst();
        } catch (Exception e) {
            System.err.println("Warning: Could not refresh voices: " + e.getMessage());
        }
    }
    
    // try connecting
    @FXML
    private void onConnect(){
        onDisconnect(); // disconnect from last port

        String portName = (String)portComboBox.getValue();
        if (portName == null){ // check for ports selected
            logsArea.appendText("No port selected\n");
            return;
        }
        int baudRate = backend.getBaudRate();
        // try to connect
        if(backend.connect(portName, baudRate) && backend.isConnected()){
            logsArea.appendText("Connected to " + portName +"\n");
            lastPort = portName;
            
            connectBtn.setDisable(true);
            disconnectBtn.setDisable(false);
     
            statusLabel.setText("Connected");
            statusBanner.getStyleClass().remove("status-disconnected");
            statusBanner.getStyleClass().add("status-connected");
        
            backend.startListening(message ->{ 
                Platform.runLater(() -> {
                    serialOutputArea.appendText(message);
                    String voice = (String)voiceComboBox.getValue();
                    tts.speak(message,voice);
                });
            });
        }
        else 
            logsArea.appendText("Failed to connected to " + portName + "\n");        
    }
    
    // disconnect
    @FXML
    private void onDisconnect(){
        if(backend.isConnected()){
            backend.stopListening();
            backend.disconnect();
            logsArea.appendText("Disconnected from " + lastPort + "\n");

            connectBtn.setDisable(false);
            disconnectBtn.setDisable(true);
            
            statusLabel.setText("Not Connected");
            statusBanner.getStyleClass().remove("status-connected");
            statusBanner.getStyleClass().add("status-disconnected");
        }
    }

    // speak custom messages
    @FXML
    private void onCustomMessage() {
        String input = messageInput.getText().trim();
        if(input.isEmpty()) {
            logsArea.appendText("[WARN] Please write something");
            return;
        }
        speakQuickMessage(input);
    }
    
    private void speakQuickMessage(String message){
        serialInputArea.appendText(message + "\n");
        serialOutputArea.appendText(message + "\n");
        String voice = (String)voiceComboBox.getValue(); 
        tts.speak(message,voice);
        messageInput.clear();
        
    }
    
    // Helper to create a single mapping button 
    @FXML
private void onShowMapping() {
        if (mappingStage != null && mappingStage.isShowing()) {
            mappingStage.toFront();
            return;
        }
    
        mappingStage = new Stage();
        mappingStage.setTitle("Button Mapping Reference");
        mappingStage.initOwner(messageInput.getScene().getWindow());
        
        HBox root = new HBox(0);
        root.setStyle("-fx-background-color: white;");
        
        VBox categoryPanel = new VBox(5);
        categoryPanel.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 15; -fx-border-color: #dadce0; -fx-border-width: 0 2 0 0;");
        categoryPanel.setPrefWidth(300); 
        
        Label categoryTitle = new Label("Switch Categories");
        categoryTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        categoryPanel.getChildren().add(categoryTitle);
        
        // Category data
        String[][] categories = {
            {"0", "Understanding/Comprehension", "#9C27B0"},
            {"1", "Physical Needs", "#F44336"},
            {"2", "Classroom Requests", "#2196F3"},
            {"3", "Emotional/Mental State", "#FF9800"},
            {"4", "Break/Time Needs", "#4CAF50"},
            {"5", "Social/Peer", "#00BCD4"},
            {"6", "Technical/Materials", "#795548"},
            {"7", "⚠️ EMERGENCY", "#D32F2F"},
            {"8", "General Responses", "#607D88"}
        };
        
        String[][][] messages = {
            // Switch 0 - Understanding
            {
                {"I don't understand"},
                {"Could you please explain?"},
                {"I need help with this"},
                {"Can you show me an example?"},
                {"I'm confused about the instructions"}
            },
            // Switch 1 - Physical Needs
            {
                {"May I please use the bathroom?"},
                {"I need water please"},
                {"I don't feel well"},
                {"I need my medication"},
                {"Can I get some food?"}
            },
            // Switch 2 - Classroom Requests
            {
                {"Could you please repeat that?"},
                {"I can't hear you"},
                {"Could you speak slower please?"},
                {"Can you speak louder please?"},
                {"Can you write it down please?"}
            },
            // Switch 3 - Emotional/Mental
            {
                {"I'm feeling overwhelmed"},
                {"I'm feeling anxious"},
                {"I need a moment to calm down"},
                {"I'm tired"},
                {"I need some quiet time"}
            },
            // Switch 4 - Break/Time Needs
            {
                {"May I please take a break?"},
                {"I need more time please"},
                {"I'm finished"},
                {"I'm ready to continue"},
                {"Can we stop for now?"}
            },
            // Switch 5 - Social/Peer
            {
                {"Can I work with someone?"},
                {"I need some space please"},
                {"Can I join your group?"},
                {"I'd like to work alone"},
                {"Can I share my idea?"}
            },
            // Switch 6 - Technical/Materials
            {
                {"I need a pen or pencil"},
                {"I need paper"},
                {"My equipment isn't working"},
                {"What page are we on?"},
                {"I didn't get the handout"}
            },
            // Switch 7 - EMERGENCY
            {
                {"I NEED HELP NOW"},
                {"I need the nurse"},
                {"Please call my parents"},
                {"I'm having a medical emergency"},
                {"Something is wrong"}
            },
            // Switch 8 - General
            {
                {"Yes"},
                {"No"},
                {"Maybe"},
                {"Thank you"},
                {"I understand"}
            }
        };
        
        // RIGHT SIDE: Button layout display
        VBox displayPanel = new VBox(20);
        displayPanel.setStyle("-fx-background-color: white; -fx-padding: 30;");
        displayPanel.setAlignment(javafx.geometry.Pos.CENTER);
        HBox.setHgrow(displayPanel, javafx.scene.layout.Priority.ALWAYS);
        
        Label displayTitle = new Label("Select a category to view button layout");
        displayTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #5f6368;");
        displayPanel.getChildren().add(displayTitle);
        
        // Create category buttons and set up display
        for (int i = 0; i < categories.length; i++) {
            final int index = i;
            Button categoryBtn = new Button("Switch " + categories[i][0] + ": " + categories[i][1]);
            categoryBtn.setMaxWidth(Double.MAX_VALUE);
            categoryBtn.setWrapText(true); // ADDED - allows text to wrap
            categoryBtn.setPrefHeight(50); // ADDED - taller buttons
            categoryBtn.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: " + categories[i][2] + "; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5; " +
                "-fx-padding: 8; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: " + categories[i][2] + ";"
            );
            
            categoryBtn.setOnMouseEntered(e -> {
                categoryBtn.setStyle(
                    "-fx-background-color: " + categories[index][2] + "; " +
                    "-fx-border-color: " + categories[index][2] + "; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 5; " +
                    "-fx-background-radius: 5; " +
                    "-fx-padding: 8; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-fill: white;"
                );
            });
            
            categoryBtn.setOnMouseExited(e -> {
                categoryBtn.setStyle(
                    "-fx-background-color: white; " +
                    "-fx-border-color: " + categories[index][2] + "; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 5; " +
                    "-fx-background-radius: 5; " +
                    "-fx-padding: 8; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-fill: " + categories[index][2] + ";"
                );
            });
            
            categoryBtn.setOnAction(e -> {
                displayPanel.getChildren().clear();
                
                // Category title
                Label catTitle = new Label("Switch " + categories[index][0] + ": " + categories[index][1]);
                catTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + categories[index][2] + ";");
                displayPanel.getChildren().add(catTitle);
                
                // Button layout visualization
                VBox buttonLayout = createButtonLayout(messages[index], categories[index][2]);
                displayPanel.getChildren().add(buttonLayout);
            });
            
            categoryPanel.getChildren().add(categoryBtn);
        }
        
        root.getChildren().addAll(categoryPanel, displayPanel);
        
        Scene scene = new Scene(root, 1000, 650); // INCREASED width from 900 to 1000
        mappingStage.setScene(scene);
        mappingStage.show();
        
        mappingStage.setOnCloseRequest(e -> mappingStage = null);
    }
 
private VBox createButtonLayout(String[][] messages, String color) {
        VBox layout = new VBox(15);
        layout.setAlignment(javafx.geometry.Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");
        
        // Instructions
        Label instruction = new Label("Button Layout on FPGA Board:");
        instruction.setStyle("-fx-font-size: 14px; -fx-text-fill: #5f6368; -fx-padding: 0 0 10 0;");
        layout.getChildren().add(instruction);
        
        // Button cross layout
        VBox crossLayout = new VBox(10);
        crossLayout.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Top (UP - message[1]) - NO ARROW
        HBox topRow = new HBox();
        topRow.setAlignment(javafx.geometry.Pos.CENTER);
        if (messages.length > 1) {
            topRow.getChildren().add(createMappingButton(messages[1][0], color));
        }
        
        // Middle (LEFT - message[2], CENTER - message[0], RIGHT - message[3]) - NO ARROWS/DOTS
        HBox middleRow = new HBox(10);
        middleRow.setAlignment(javafx.geometry.Pos.CENTER);
        if (messages.length > 2) {
            middleRow.getChildren().add(createMappingButton(messages[2][0], color));
        }
        if (messages.length > 0) {
            middleRow.getChildren().add(createMappingButton(messages[0][0], color));
        }
        if (messages.length > 3) {
            middleRow.getChildren().add(createMappingButton(messages[3][0], color));
        }
        
        // Bottom (DOWN - message[4]) - NO ARROW
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(javafx.geometry.Pos.CENTER);
        if (messages.length > 4) {
            bottomRow.getChildren().add(createMappingButton(messages[4][0], color));
        }
        
        crossLayout.getChildren().addAll(topRow, middleRow, bottomRow);
        layout.getChildren().add(crossLayout);
        
        return layout;
}
 
    private Button createMappingButton(String text, String color) {
    Button btn = new Button(text);
    btn.setPrefWidth(240); 
    btn.setPrefHeight(85);  
    btn.setWrapText(true);
    btn.setStyle(
        "-fx-background-color: white; " +
        "-fx-border-color: " + color + "; " +
        "-fx-border-width: 3; " +
        "-fx-border-radius: 8; " +
        "-fx-background-radius: 8; " +
        "-fx-font-size: 16px; " + 
        "-fx-text-fill: #1a1a1a; " +
        "-fx-cursor: hand; " +
        "-fx-font-weight: bold;"
    );
    btn.setOnAction(e -> {
        speakQuickMessage(text);
    });

    btn.setOnMouseEntered(e -> {
        btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-border-color: " + color + "; " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-font-size: 16px; " + 
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand;"
        );
    });
    
    btn.setOnMouseExited(e -> {
        btn.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: " + color + "; " +
            "-fx-border-width: 3; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-font-size: 16px; " + 
            "-fx-text-fill: #1a1a1a; " +
            "-fx-cursor: hand; " +
            "-fx-font-weight: bold;"
        );
    });
    
    return btn;
}
 
    // cleanup for soft close
    public void cleanup(){
        onDisconnect();
        if (myMessagesStage != null) myMessagesStage.close();
        if (logsStage != null) logsStage.close();
        if (mappingStage != null) mappingStage.close();

    }

    @FXML
    private void onQuickYes() {
        speakQuickMessage("Yes");
    }

    @FXML
    private void onQuickNo() {
        speakQuickMessage("No");
    }

    @FXML
    private void onQuickThanks() {
        speakQuickMessage("Thank you");
    }

    @FXML
    private void onQuickUnderstand() {
        speakQuickMessage("I understand");
    }

    @FXML
    private void onQuickHelp() {
        speakQuickMessage("I need help");
    }

    // Clear button handlers (NEW)
    @FXML
    private void onClearSerial() {
        serialOutputArea.clear();
    }

    @FXML
    private void onClearCustom() {
        serialInputArea.clear();
    }

    @FXML
    private void onClearLogs() {
        logsArea.clear();
    }

    // Test voice handler 
    @FXML
    private void onTestVoice() {
        String voice = (String)voiceComboBox.getValue();
        if (voice != null) {
            tts.speak("Voice test successful", voice);
            logsArea.appendText("Testing voice: " + voice + "\n");
        } else {
            logsArea.appendText("[WARN] No voice selected\n");
        }
    }
    
    @FXML
    private void onShowMyMessages() {
        if (myMessagesStage != null && myMessagesStage.isShowing()) {
            myMessagesStage.toFront();
            return;
        }

        myMessagesStage = new Stage();
        myMessagesStage.setTitle("My Messages History");
        myMessagesStage.initOwner(messageInput.getScene().getWindow());        

        VBox root = new VBox(10);
        root.setPadding(new javafx.geometry.Insets(15));
        root.setStyle("-fx-background-color: white;");
        
        // Header
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label("📤 My Messages History");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> onClearCustom());
        clearBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        header.getChildren().addAll(title, spacer, clearBtn);
        
        // TextArea
        TextArea displayArea = new TextArea();
        displayArea.setEditable(false);
        displayArea.setWrapText(true);
        displayArea.setStyle("-fx-font-size: 14px; -fx-background-color: #e3f2fd;");
        displayArea.textProperty().bind(serialInputArea.textProperty());
        VBox.setVgrow(displayArea, javafx.scene.layout.Priority.ALWAYS);
        
        root.getChildren().addAll(header, displayArea);
        
        Scene scene = new Scene(root, 500, 400);
        myMessagesStage.setScene(scene);
        myMessagesStage.show();
        
        myMessagesStage.setOnCloseRequest(e -> myMessagesStage = null);
    }

    // Show System Logs popup window
    @FXML
    private void onShowLogs() {
        if (logsStage != null && logsStage.isShowing()) {
            logsStage.toFront();
            return;
        }

        logsStage = new Stage();
        logsStage.setTitle("System Logs");
        logsStage.initOwner(messageInput.getScene().getWindow());

        
        VBox root = new VBox(10);
        root.setPadding(new javafx.geometry.Insets(15));
        root.setStyle("-fx-background-color: white;");
        
        // Header
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label("📋 System Logs");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> onClearLogs());
        clearBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
        header.getChildren().addAll(title, spacer, clearBtn);
        
        // TextArea
        TextArea displayArea = new TextArea();
        displayArea.setEditable(false);
        displayArea.setWrapText(true);
        displayArea.setStyle("-fx-font-size: 12px; -fx-font-family: 'Consolas', monospace; -fx-background-color: #fafafa;");
        displayArea.textProperty().bind(logsArea.textProperty());
        VBox.setVgrow(displayArea, javafx.scene.layout.Priority.ALWAYS);
        
        root.getChildren().addAll(header, displayArea);
        
        Scene scene = new Scene(root, 600, 400);
        logsStage.setScene(scene);
        logsStage.show();
        
        logsStage.setOnCloseRequest(e -> logsStage = null);
    }
}   