import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import java.sql.*;

public class Settings extends Application {
    
    private TextField t1sField;
    private TextField t2sField;
    private TextField t1uField;
    private TextField t2uField;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #000000; -fx-font-family: 'Inter', sans-serif;");

        root.setLeft(createSidebar(stage));

        BorderPane rightArea = new BorderPane();
        rightArea.setTop(createTopNav());

        ScrollPane mainScroll = new ScrollPane();
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-control-inner-background: #0e0e0e;");

        BorderPane contentWrap = new BorderPane();
        contentWrap.setPadding(new Insets(48));
        contentWrap.setCenter(createMainContent());

        mainScroll.setContent(contentWrap);
        rightArea.setCenter(mainScroll);

        root.setCenter(rightArea);

        Scene scene = new Scene(root, 1280, 800);
        try {
            scene.getStylesheets().add(getClass().getResource("plugin.css").toExternalForm());
        } catch (Exception e) {}

        stage.setTitle("Volt Charge - Settings");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createMainContent() {
        VBox content = new VBox(32);
        content.setMaxWidth(1100);

        Label title = new Label("System Settings");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 48px; -fx-font-weight: bold;");

        // Pricing configuration card – rate fields for all four protocol/speed combinations
        VBox pricingCard = new VBox(24);
        pricingCard.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 32; -fx-background-radius: 12;");
        
        Label pricingLabel = new Label("PRICING CONFIGURATION");
        pricingLabel.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");

        // Input fields – one per protocol/speed rate (Type 1 Standard, Type 2 Standard, etc.)
        t1sField = createFieldRow(pricingCard, "Type 1 (Standard) Rate ($/kWh):");
        t2sField = createFieldRow(pricingCard, "Type 2 (Standard) Rate ($/kWh):");
        t1uField = createFieldRow(pricingCard, "Type 1 (Ultra Fast) Rate ($/kWh):");
        t2uField = createFieldRow(pricingCard, "Type 2 (Ultra Fast) Rate ($/kWh):");

        // Button – Save Changes: persists updated rates to the settings DB table
        Button saveBtn = new Button("SAVE CHANGES");
        saveBtn.setStyle("-fx-background-color: #3fff8b; -fx-text-fill: #000000; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-radius: 4; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> saveSettings());

        // Button – Reset Earnings: wipes all records from completed_sessions table
        Button resetBtn = new Button("RESET EARNINGS");
        resetBtn.setStyle("-fx-background-color: #9f0519; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-radius: 4; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM completed_sessions");
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setHeaderText("Database Reset");
                a.setContentText("All earnings and session history have been cleared.");
                a.show();
                new Dashboard().start((Stage)resetBtn.getScene().getWindow());
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        loadSettings();

        HBox btnBox = new HBox(16, saveBtn, resetBtn);
        pricingCard.getChildren().addAll(pricingLabel, ((HBox)t1sField.getParent()), ((HBox)t2sField.getParent()), ((HBox)t1uField.getParent()), ((HBox)t2uField.getParent()), btnBox);
        content.getChildren().addAll(title, pricingCard);

        
        return content;
    }

    private TextField createFieldRow(VBox parent, String text) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-pref-width: 300px;");
        TextField f = new TextField();
        f.setStyle("-fx-background-color: #262626; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 4;");
        row.getChildren().addAll(lbl, f);
        return f;
    }

    // loadSettings – reads rate values from DB and populates the input fields
    private void loadSettings() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)");
            
            t1sField.setText("0.44");
            t2sField.setText("0.44");
            t1uField.setText("0.88");
            t2uField.setText("0.88");

            ResultSet rs = stmt.executeQuery("SELECT key, value FROM settings");
            while(rs.next()) {
                String k = rs.getString("key");
                String v = rs.getString("value");
                if ("t1s".equals(k)) t1sField.setText(v);
                else if ("t2s".equals(k)) t2sField.setText(v);
                else if ("t1u".equals(k)) t1uField.setText(v);
                else if ("t2u".equals(k)) t2uField.setText(v);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // saveSettings – writes all four rate fields back to the settings table
    private void saveSettings() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
             PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)")) {
            
            saveOne(pstmt, "t1s", t1sField.getText());
            saveOne(pstmt, "t2s", t2sField.getText());
            saveOne(pstmt, "t1u", t1uField.getText());
            saveOne(pstmt, "t2u", t2uField.getText());
            
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("Settings Saved Successfully");
            a.show();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void saveOne(PreparedStatement pstmt, String k, String v) throws SQLException {
        pstmt.setString(1, k);
        pstmt.setString(2, v);
        pstmt.executeUpdate();
    }

    // Sidebar – left navigation panel with app branding, nav links and emergency stop
    private VBox createSidebar(Stage stage) {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(256);
        sidebar.setStyle("-fx-background-color: #0e0e0e;");
        sidebar.setPadding(new Insets(32, 0, 0, 0));

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 32, 32, 32));
        Label title = new Label("VOLT CHARGE");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitle = new Label("Power Admin v1.0");
        subtitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #adaaaa; -fx-opacity: 0.6;");
        header.getChildren().addAll(title, subtitle);

        VBox navMenu = new VBox(8);
        navMenu.getChildren().addAll(
                createNavItem("Dashboard", false, stage),
                createNavItem("Plug In", false, stage));
        VBox.setVgrow(navMenu, Priority.ALWAYS);

        VBox bottomMenu = new VBox(8);
        bottomMenu.setPadding(new Insets(24, 0, 24, 0));
        bottomMenu.getChildren().addAll(
                createNavItem("Support", false, stage),
                createNavItem("Settings", true, stage));

        Button emergency = new Button("\u26A0 EMERGENCY STOP");
        emergency.setMaxWidth(Double.MAX_VALUE);
        emergency.setStyle("-fx-background-color: #9f0519; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 12 0; -fx-cursor: hand; -fx-background-radius: 4;");
        emergency.setOnAction(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
                 Statement stmt = conn.createStatement()) {
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
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setStyle(active ? "-fx-background-color: #1a1a1a; -fx-border-color: transparent #00E5FF transparent transparent; -fx-border-width: 0 4 0 0;" : "-fx-background-color: transparent;");

        Label label = new Label(text);
        label.setStyle(active ? "-fx-text-fill: #00E5FF; -fx-font-weight: bold; -fx-font-size: 14px;" : "-fx-text-fill: #adaaaa; -fx-font-size: 14px;");
        box.getChildren().add(label);

        box.setOnMouseClicked(e -> {
            try {
                if (text.equals("Dashboard")) new Dashboard().start(stage);
                else if (text.equals("Plug In")) new Plugin().start(stage);
                else if (text.equals("Settings")) new Settings().start(stage);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        return box;
    }

    // Top navbar – search bar and user avatar with profile/logout menu
    private HBox createTopNav() {
        HBox topNav = new HBox(16);
        topNav.setPrefHeight(64);
        topNav.setAlignment(Pos.CENTER_LEFT);
        topNav.setPadding(new Insets(0, 32, 0, 32));
        topNav.setStyle("-fx-background-color: rgba(14, 14, 14, 0.8);");

        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 6 16; -fx-background-radius: 16;");
        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setTextFill(Color.web("#adaaaa"));
        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        searchField.setPrefWidth(256);
        searchBox.getChildren().addAll(searchIcon, searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Circle avatar = new Circle(16, Color.web("#81ecff"));
        avatar.setStyle("-fx-cursor: hand;");
        avatar.setOnMouseClicked(e -> {
            ContextMenu profileMenu = new ContextMenu(new MenuItem("Profile"), new MenuItem("Log Out"));
            profileMenu.getItems().get(1).setOnAction(evt -> handleLogout((Stage) avatar.getScene().getWindow()));
            profileMenu.show(avatar, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        topNav.getChildren().addAll(searchBox, spacer, avatar);
        return topNav;
    }

    private void handleLogout(Stage stage) {
        try {
            new Login().start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
