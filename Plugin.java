import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.Region;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.stage.Stage;

public class Plugin extends Application {
    private String preSelectedSlot;

    public Plugin() {
        this.preSelectedSlot = null;
    }

    public Plugin(String slotId) {
        this.preSelectedSlot = slotId;
    }


    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Sidebar
        VBox sidebar = createSidebar(stage);
        root.setLeft(sidebar);

        // Right Main Area
        BorderPane rightArea = new BorderPane();

        // Top Nav
        HBox topNav = createTopNav();
        rightArea.setTop(topNav);

        // Main Content
        ScrollPane mainScroll = new ScrollPane();
        mainScroll.setFitToWidth(true);
        mainScroll.getStyleClass().add("scroll-pane");
        mainScroll.setContent(createMainContent());

        rightArea.setCenter(mainScroll);

        root.setCenter(rightArea);

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("plugin.css").toExternalForm());

        stage.setTitle("Volt Charge - Plug In");
        stage.setScene(scene);
        stage.setMaximized(false);
        stage.setMaximized(true);
        stage.show();
    }

    private VBox createSidebar(Stage stage) {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(256);
        sidebar.setStyle("-fx-background-color: #0e0e0e;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(32));
        Label title = new Label("VOLT CHARGE");
        title.getStyleClass().add("sidebar-title");
        Label subtitle = new Label("Power Admin v1.0");
        subtitle.getStyleClass().add("sidebar-subtitle");
        header.getChildren().addAll(title, subtitle);

        VBox navMenu = new VBox(8);
        navMenu.getChildren().addAll(
                createNavItem("Dashboard", false, stage),
                createNavItem("Plug In", true, stage));
        VBox.setVgrow(navMenu, Priority.ALWAYS);

        VBox bottomMenu = new VBox(8);
        bottomMenu.setPadding(new Insets(24, 0, 24, 0));
        bottomMenu.getChildren().addAll(
                createNavItem("Support", false, stage),
                createNavItem("Settings", false, stage));

        Button emergency = new Button("EMERGENCY STOP");
        emergency.setMaxWidth(Double.MAX_VALUE);
        emergency.getStyleClass().add("emergency-btn");
        emergency.setOnAction(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM sessions");
                stmt.execute("DELETE FROM pending_payments");
                new Dashboard().start(stage);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        VBox emergencyBox = new VBox(emergency);
        emergencyBox.setPadding(new Insets(32, 16, 0, 16));

        bottomMenu.getChildren().add(emergencyBox);

        sidebar.getChildren().addAll(header, navMenu, bottomMenu);
        return sidebar;
    }

    private HBox createNavItem(String text, boolean active, Stage stage) {
        HBox box = new HBox(16);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("nav-item");
        if (active)
            box.getStyleClass().add("nav-item-active");

        Label label = new Label(text);
        label.getStyleClass().add("nav-item-text");
        box.getChildren().add(label);

        box.setOnMouseClicked(e -> {
            try {
                if (text.equals("Dashboard"))
                    new Dashboard().start(stage);
                else if (text.equals("Plug In"))
                    new Plugin().start(stage);
                else if (text.equals("Active Session"))
                    new ActiveSession().start(stage);
                else if (text.equals("Checkout"))
                    new Checkout().start(stage);
                else if (text.equals("Settings"))
                    new Settings().start(stage);
                else if (text.equals("Support")) {
                    VBox placeholder = new VBox(16);
                    placeholder.setAlignment(Pos.CENTER);
                    Label m_title = new Label(text + " Module");
                    m_title.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold;");
                    Label subtitle = new Label("This module is currently detached and pending completion in v1.1.");
                    subtitle.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 14px;");
                    placeholder.getChildren().addAll(m_title, subtitle);
                    ((BorderPane)stage.getScene().getRoot()).setCenter(placeholder);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return box;
    }

    private HBox createTopNav() {
        HBox topNav = new HBox();
        topNav.setPrefHeight(64);
        topNav.setAlignment(Pos.CENTER_LEFT);
        topNav.setPadding(new Insets(0, 32, 0, 32));
        topNav.setStyle("-fx-background-color: rgba(14, 14, 14, 0.8);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Circle avatar = new Circle(16, Color.web("#81ecff"));
        avatar.setStyle("-fx-cursor: hand;");
        avatar.setOnMouseClicked(e -> {
            ContextMenu profileMenu = new ContextMenu(new MenuItem("Profile"), new MenuItem("Log Out"));
            profileMenu.getItems().get(1).setOnAction(evt -> handleLogout((Stage) avatar.getScene().getWindow()));
            profileMenu.show(avatar, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        topNav.getChildren().addAll(spacer, avatar);
        return topNav;
    }

    private void handleLogout(Stage stage) {
        try {
            new Login().start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private VBox createMainContent() {
        VBox content = new VBox(48);
        content.setPadding(new Insets(40));

        VBox headerBox = new VBox(8);
        Label h1 = new Label("Initiate Charge");
        h1.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label p1 = new Label("Configure new energy conduit for incoming vessel.");
        p1.getStyleClass().add("text-on-surface-variant");
        headerBox.getChildren().addAll(h1, p1);

        HBox columns = new HBox(32);

        VBox leftCol = createLeftColumn();
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        leftCol.setPrefWidth(500);

        VBox rightCol = createRightColumn();
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.setPrefWidth(400);

        columns.getChildren().addAll(leftCol, rightCol);
        content.getChildren().addAll(headerBox, columns);
        return content;
    }

    private VBox createLeftColumn() {
        VBox container = new VBox(32);
        container.getStyleClass().addAll("bg-surface-container", "card");

        // Assign Button
        Button assignBtn = new Button("ASSIGN SLOT");
        assignBtn.setMaxWidth(Double.MAX_VALUE);
        assignBtn.getStyleClass().add("assign-btn");

        // Slot Selection
        VBox slotSection = new VBox(16);
        Label slotLabel = new Label("SELECT GRID SLOT");
        slotLabel.getStyleClass().add("card-title");

        GridPane slotsGrid = new GridPane();
        slotsGrid.setHgap(12);
        slotsGrid.setVgap(12);
        List<Button> slotButtons = new ArrayList<>();
        ObjectProperty<Button> selectedSlot = new SimpleObjectProperty<>();
        List<String> activeDbSlots = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery("SELECT slot FROM sessions")) {
            while (rs.next()) {
                activeDbSlots.add(rs.getString("slot"));
            }
        } catch (SQLException ex) {
            // ignore
        }

        String[] slotNames = { "A-01", "A-02", "A-03", "B-01", "B-02", "B-03", "C-01", "C-02" };
        for (int i = 0; i < 8; i++) {
            final String finalSlotName = slotNames[i];
            Button btn = new Button(finalSlotName);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("slot-btn");
            GridPane.setHgrow(btn, Priority.ALWAYS);

            if (activeDbSlots.contains(finalSlotName)) {
                btn.getStyleClass().add("slot-btn-disabled");
                btn.getStyleClass().remove("slot-btn-active");
                btn.setDisable(true);
            } else {
                btn.setOnAction(e -> {
                    for (Button b : slotButtons) {
                        b.getStyleClass().remove("slot-btn-active");
                    }
                    btn.getStyleClass().add("slot-btn-active");
                    assignBtn.setText("ASSIGN SLOT " + btn.getText());
                    selectedSlot.set(btn);
                });
                
                // Pre-selection logic
                if (preSelectedSlot != null && preSelectedSlot.equals(finalSlotName)) {
                    Platform.runLater(() -> {
                        btn.fire();
                    });
                }
            }

            slotButtons.add(btn);
            slotsGrid.add(btn, i % 4, i / 4);
        }


        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            slotsGrid.getColumnConstraints().add(cc);
        }
        slotSection.getChildren().addAll(slotLabel, slotsGrid);

        // User Identification
        VBox userSection = new VBox(16);
        HBox userHeaderBox = new HBox(8);
        userHeaderBox.setAlignment(Pos.CENTER_LEFT);
        SVGPath userIcon = new SVGPath();
        userIcon.setContent(
                "M 12 2 C 9.238 2 7 4.238 7 7 C 7 9.762 9.238 12 12 12 C 14.762 12 17 9.762 17 7 C 17 4.238 14.762 2 12 2 z M 12 14 C 8.666 14 2 15.666 2 19 L 2 22 L 22 22 L 22 19 C 22 15.666 15.334 14 12 14 z");
        userIcon.setFill(Color.web("#adaaaa"));
        userIcon.setScaleX(0.7);
        userIcon.setScaleY(0.7);
        Label userLabel = new Label("USER IDENTIFICATION");
        userLabel.getStyleClass().add("card-title");
        userHeaderBox.getChildren().addAll(userIcon, userLabel);

        TextField userField = new TextField();
        userField.setPromptText("Enter full name or ID");
        userField.getStyleClass().add("input-field");
        userSection.getChildren().addAll(userHeaderBox, userField);

        // Protocol
        VBox protocolSection = new VBox(16);
        Label protocolLabel = new Label("CONNECTOR PROTOCOL");
        protocolLabel.getStyleClass().add("card-title");

        GridPane protocolGrid = new GridPane();
        protocolGrid.setHgap(16);
        protocolGrid.setVgap(16);

        ToggleGroup group = new ToggleGroup();

        HBox box1 = createProtocolBox("Type 1", "SAE J1772", group, false);
        HBox box2 = createProtocolBox("Type 2", "Mennekes", group, true);
        HBox box3 = createProtocolBox("CCS", "Combined System", group, false);
        HBox box4 = createProtocolBox("CHAdeMO", "DC Fast Charge", group, false);

        protocolGrid.add(box1, 0, 0);
        protocolGrid.add(box2, 1, 0);
        protocolGrid.add(box3, 0, 1);
        protocolGrid.add(box4, 1, 1);

        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            protocolGrid.getColumnConstraints().add(cc);
        }

        protocolSection.getChildren().addAll(protocolLabel, protocolGrid);

        container.getChildren().addAll(slotSection, userSection, protocolSection, assignBtn);

        assignBtn.setOnAction(e -> {
            String slot = assignBtn.getText().replace("ASSIGN SLOT ", "");
            String user = userField.getText();

            Toggle selected = group.getSelectedToggle();
            if (user.isEmpty() || selected == null) {
                return;
            }
            String protocol = selected != null ? selected.getUserData().toString() : "Unknown";
            Button selectedBtn = selectedSlot.get();
            selectedBtn.setDisable(true);
            selectedBtn.getStyleClass().remove("slot-btn-active");
            selectedBtn.getStyleClass().add("slot-btn-disabled");
            userField.setText("");
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            ChargingSession session = new ChargingSession(slot, user, protocol, time);
            String url = "jdbc:sqlite:sessions.db";
            String sql = "INSERT INTO sessions (slot, username, protocol, start_time) VALUES (?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(url);
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, session.getSlot());
                pstmt.setString(2, session.getUser());
                pstmt.setString(3, session.getProtocol());
                pstmt.setString(4, session.getStartTime());
                pstmt.executeUpdate();

                System.out.println("Saved to Database");
                printAllSessions();
            } catch (SQLException ex) {
                System.out.println("Error saving to database: " + ex.getMessage());
            }
            group.selectToggle(null);
        });

        return container;

    }

    private HBox createProtocolBox(String title, String sub, ToggleGroup group, boolean active) {
        HBox container = new HBox(16);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("protocol-box");

        RadioButton radio = new RadioButton();
        radio.setToggleGroup(group);
        radio.setSelected(active);
        radio.setUserData(title);
        container.setOnMouseClicked(e -> {
            radio.setSelected(true);
        });
        radio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                container.getStyleClass().add("protocol-box-active");
            } else {
                container.getStyleClass().remove("protocol-box-active");
            }
        });
        VBox text = new VBox(2);
        Label t1 = new Label(title);
        t1.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label t2 = new Label(sub);
        t2.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px;");
        text.getChildren().addAll(t1, t2);

        container.getChildren().addAll(radio, text);
        return container;
    }

    private VBox createRightColumn() {
        VBox rightColumn = new VBox(24);

        // Infrastructure Health
        VBox healthCard = new VBox(24);
        healthCard.getStyleClass().addAll("bg-surface-container-low", "card");

        HBox healthHeader = new HBox();
        healthHeader.setAlignment(Pos.CENTER_LEFT);
        Label hTitle = new Label("INFRASTRUCTURE HEALTH");
        hTitle.getStyleClass().add("card-title");
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Label hStatus = new Label("OPTIMAL");
        hStatus.setStyle("-fx-text-fill: #3fff8b; -fx-font-weight: bold; -fx-font-size: 10px;");
        healthHeader.getChildren().addAll(hTitle, hSpacer, hStatus);

        HBox tempMetric = new HBox(12);
        tempMetric.setAlignment(Pos.CENTER_LEFT);
        SVGPath tempSvg = new SVGPath();
        tempSvg.setContent(
                "M 10 2 C 8.895 2 8 2.895 8 4 L 8 13.203 C 6.782 14.072 6 15.454 6 17 C 6 19.209 7.791 21 10 21 C 12.209 21 14 19.209 14 17 C 14 15.454 13.217 14.072 12 13.203 L 12 4 C 12 2.895 11.105 2 10 2 z M 10 4 C 10.552 4 11 4.448 11 5 L 11 10 L 9 10 L 9 5 C 9 4.448 9.448 4 10 4 z");
        tempSvg.setFill(Color.web("#81ecff"));
        HBox tempIcon = new HBox(tempSvg);
        tempIcon.setAlignment(Pos.CENTER);
        tempIcon.setMinSize(40, 40);
        tempIcon.getStyleClass().add("icon-box");

        VBox tempText = new VBox(4);
        Label lblT1 = new Label("Core Temp");
        lblT1.getStyleClass().add("text-on-surface-variant");
        Label lblT2 = new Label("32.4°C");
        lblT2.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        tempText.getChildren().addAll(lblT1, lblT2);

        Region tSpacer = new Region();
        HBox.setHgrow(tSpacer, Priority.ALWAYS);

        HBox progressBarBase = new HBox();
        progressBarBase.setAlignment(Pos.CENTER_LEFT);
        progressBarBase.setPrefSize(64, 1);
        progressBarBase.setMinHeight(2);
        progressBarBase.setMaxHeight(2);
        progressBarBase.setStyle("-fx-background-color: #262626; -fx-background-radius: 1;");

        Region progressValue = new Region();
        progressValue.setPrefSize(32, 1);
        progressValue.setMinHeight(2);
        progressValue.setMaxHeight(2);
        progressValue.setStyle("-fx-background-color: #3fff8b; -fx-background-radius: 1;");
        progressBarBase.getChildren().add(progressValue);

        tempMetric.getChildren().addAll(tempIcon, tempText, tSpacer, progressBarBase);

        HBox loadMetric = new HBox(12);
        loadMetric.setAlignment(Pos.CENTER_LEFT);
        SVGPath loadSvg = new SVGPath();
        loadSvg.setContent(
                "M 2 5 L 2 9 L 22 9 L 22 5 L 2 5 z M 4 6 L 6 6 L 6 8 L 4 8 L 4 6 z M 8 6 L 10 6 L 10 8 L 8 8 L 8 6 z M 2 11 L 2 15 L 22 15 L 22 11 L 2 11 z M 4 12 L 6 12 L 6 14 L 4 14 L 4 12 z M 8 12 L 10 12 L 10 14 L 8 14 L 8 12 z M 2 17 L 2 21 L 22 21 L 22 17 L 2 17 z M 4 18 L 6 18 L 6 20 L 4 20 L 4 18 z M 8 18 L 10 18 L 10 20 L 8 20 L 8 18 z");
        loadSvg.setFill(Color.web("#81ecff"));
        HBox loadIcon = new HBox(loadSvg);
        loadIcon.setAlignment(Pos.CENTER);
        loadIcon.setMinSize(40, 40);
        loadIcon.getStyleClass().add("icon-box");

        VBox loadText = new VBox(4);
        Label lblL1 = new Label("Load Balancing");
        lblL1.getStyleClass().add("text-on-surface-variant");
        Label lblL2 = new Label("Active");
        lblL2.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        loadText.getChildren().addAll(lblL1, lblL2);

        Region lSpacer = new Region();
        HBox.setHgrow(lSpacer, Priority.ALWAYS);
        Label checkIcon = new Label("✓");
        checkIcon.setStyle("-fx-text-fill: #3fff8b; -fx-font-weight: bold; -fx-font-size: 16px;");

        loadMetric.getChildren().addAll(loadIcon, loadText, lSpacer, checkIcon);

        healthCard.getChildren().addAll(healthHeader, tempMetric, loadMetric);

        // Station Image Card
        StackPane stationCard = new StackPane();
        stationCard.getStyleClass().add("image-card");
        stationCard.setPrefHeight(100);

        try {
            ImageView imgView = new ImageView(new Image(
                    "https://pub-6031dcc5770e42cd932516d5e92a1c15.r2.dev/Generated%20Image%20April%2003%2C%202026%20-%205_51PM.png",
                    800, 900, true, true));
            imgView.setOpacity(0.8);
            stationCard.getChildren().add(imgView);
        } catch (Exception e) {
        }

        VBox stationText = new VBox(4);
        stationText.setAlignment(Pos.BOTTOM_LEFT);
        stationText.setPadding(new Insets(24));
        Label sTitle = new Label("Station Gamma-7");
        sTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label sSub = new Label("Downtown Hub • Level 3 DC Fast");
        sSub.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 12px;");
        stationText.getChildren().addAll(sTitle, sSub);

        stationCard.getChildren().add(stationText);

        // Efficiency Card
        HBox effCard = new HBox(16);
        effCard.setAlignment(Pos.CENTER_LEFT);
        effCard.getStyleClass().addAll("bg-surface-container-high", "card");
        effCard.setPadding(new Insets(24));

        SVGPath effSvg = new SVGPath();
        effSvg.setContent(
                "M 11.0625 2 L 10.125 3.375 L 4.125 12.375 L 3.59375 13.1875 L 4.5625 13.1875 L 11 13.1875 L 9.0625 21.09375 L 9.9375 21 L 10.6875 20 L 19.6875 8 L 20.125 7.4375 L 19.4375 7.4375 L 12.34375 7.4375 L 11.53125 7.4375 L 11.75 6.625 L 12.65625 2.8125 L 11.875 2 L 11.0625 2 z");
        effSvg.setFill(Color.web("#3fff8b"));
        HBox boltIcon = new HBox(effSvg);
        boltIcon.setAlignment(Pos.CENTER);
        boltIcon.setStyle("-fx-background-color: rgba(63, 255, 139, 0.1); -fx-background-radius: 20; -fx-padding: 8;");

        VBox effText = new VBox(4);
        Label e1 = new Label("System Efficiency: 98.2%");
        e1.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label e2 = new Label("Loss minimized via superconductive cables");
        e2.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px;");
        effText.getChildren().addAll(e1, e2);

        effCard.getChildren().addAll(boltIcon, effText);

        rightColumn.getChildren().addAll(healthCard, stationCard, effCard);

        return rightColumn;
    }

    public static void printAllSessions() {
        String url = "jdbc:sqlite:sessions.db";
        String sql = "SELECT * FROM sessions";

        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(sql);
                java.sql.ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n--- All Charging Sessions ---");
            while (rs.next()) {
                System.out.printf("ID: %d | Slot: %s | User: %s | Protocol: %s | Time: %s%n",
                        rs.getInt("id"),
                        rs.getString("slot"),
                        rs.getString("username"),
                        rs.getString("protocol"),
                        rs.getString("start_time"));
            }
            System.out.println("-----------------------------\n");

        } catch (SQLException e) {
            System.out.println("Error reading from database: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String url = "jdbc:sqlite:sessions.db";
        String createTableSql = "CREATE TABLE IF NOT EXISTS sessions ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " slot TEXT NOT NULL,"
                + " username TEXT NOT NULL,"
                + " protocol TEXT NOT NULL,"
                + " start_time TEXT NOT NULL"
                + ");";
        try (Connection conn = DriverManager.getConnection(url);
                PreparedStatement pstmt = conn.prepareStatement(createTableSql)) {
            pstmt.execute();
        } catch (SQLException e) {
            System.out.println("Could not create database table: " + e.getMessage());
        }
        launch(args);
    }
}
