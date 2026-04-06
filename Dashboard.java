import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;


public class Dashboard extends Application {

    public final List<String> ALL_SLOTS = Arrays.asList("A-01", "A-02", "A-03", "B-01", "B-02", "B-03", "C-01",
            "C-02");


    // In-memory simulation states
    public static class SimSession {
        public String slot, user, protocol, startTime, status;
        public double progress;
        int timeElapsed = 0;

        public SimSession(String slot, String user, String protocol, String startTime, String status) {
            this.slot = slot;
            this.user = user;
            this.protocol = protocol;
            this.startTime = startTime;
            this.status = status;
            this.progress = 0;
        }
    }

    private static final Map<String, SimSession> activeCharges = new ConcurrentHashMap<>();

    public static SimSession getSimSession(String slot) {
        return activeCharges.get(slot);
    }

    static class ActivityLog {
        String icon, color, title, sub;

        public ActivityLog(String icon, String color, String title, String sub) {
            this.icon = icon;
            this.color = color;
            this.title = title;
            this.sub = sub;
        }
    }

    private static final List<ActivityLog> recentActivities = new CopyOnWriteArrayList<>();
    private static boolean threadStarted = false;
    private static Dashboard activeInstance = null;

    private String slotSpeedFilter = "ALL";
    private String slotStatusFilter = "ALL";
    private String searchQuery = "";

    // Reactive UI Components
    private Label lblActiveSessions;
    private Label lblTotalSlots;
    private Label lblRevenueToday;
    private FlowPane slotGrid;
    private VBox activityBox;
    private Label actTitle;

    private final Map<String, SlotUI> uiSlots = new HashMap<>();

    class SlotUI {
        VBox card;
        Label lStatus, pLabel, icon, mTxt, f1, f2;
        HBox header, progBg, footer;
        VBox center;
        Region progFill;
        String currentId;

        public SlotUI(String id) {
            this.currentId = id;
            card = new VBox(12);
            card.setPrefSize(180, 160);

            header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label lId = new Label(id);
            lId.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
            Region space = new Region();
            HBox.setHgrow(space, Priority.ALWAYS);
            lStatus = new Label();
            header.getChildren().addAll(lId, space, lStatus);

            center = new VBox(4);
            center.setAlignment(Pos.CENTER);
            pLabel = new Label();
            icon = new Label("\uD83D\uDD0C"); // Plug Symbol
            icon.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 24px;");
            mTxt = new Label();
            mTxt.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px;");

            progBg = new HBox();
            progBg.setPrefHeight(4);
            progBg.setStyle("-fx-background-color: #262626; -fx-background-radius: 2;");
            progFill = new Region();
            progBg.getChildren().add(progFill);

            footer = new HBox();
            f1 = new Label();
            f1.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px;");
            Region space2 = new Region();
            HBox.setHgrow(space2, Priority.ALWAYS);
            f2 = new Label();
            f2.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px;");
            footer.getChildren().addAll(f1, space2, f2);

            card.getChildren().addAll(header, center);
        }

        public void update(String status, String mainTxt, String color, String percent, String metric1, String metric2,
                boolean clickTarget) {
            boolean isCharging = status.equals("Charging");
            String bg = status.equals("Offline") ? "-fx-background-color: #131313; -fx-opacity: 0.7;"
                    : isCharging ? "-fx-background-color: #1a1a1a;" : "-fx-background-color: #131313;";
            card.setStyle(bg + " -fx-border-color: " + color
                    + " transparent transparent transparent; -fx-border-width: 2 0 0 0; -fx-padding: 16; -fx-background-radius: 4 4 0 0;"
                    + (clickTarget ? " -fx-cursor: hand;" : ""));

            lStatus.setText(status.toUpperCase());
            lStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: bold;");

            center.getChildren().clear();
            if (percent != null) {
                pLabel.setText(percent);
                pLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 24px; -fx-font-weight: bold;");
                center.getChildren().add(pLabel);
                center.setPadding(Insets.EMPTY);
            } else {
                center.getChildren().add(icon);
                center.setPadding(new Insets(12, 0, 12, 0));
            }
            mTxt.setText(mainTxt);
            center.getChildren().add(mTxt);

            card.getChildren().remove(progBg);
            card.getChildren().remove(footer);
            if (isCharging && percent != null) {
                progFill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");
                double w = 1.0;
                try {
                    w = Double.parseDouble(percent.replace("%", "")) / 100.0;
                } catch (Exception e) {
                }
                progFill.prefWidthProperty().bind(progBg.widthProperty().multiply(w));
                card.getChildren().add(progBg);

                f1.setText(metric1);
                f2.setText(metric2);
                card.getChildren().add(footer);
            }
        }
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0e0e0e; -fx-font-family: 'Inter', sans-serif;");

        // Sidebar
        root.setLeft(createSidebar(stage));

        // Right Main Area
        BorderPane rightArea = new BorderPane();

        // Top Nav
        rightArea.setTop(createTopNav());

        // Main Content
        ScrollPane mainScroll = new ScrollPane();
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-control-inner-background: #0e0e0e;");
        mainScroll.setContent(createMainContent());

        rightArea.setCenter(mainScroll);

        root.setCenter(rightArea);

        Scene scene = new Scene(root, 1280, 800);
        try {
            scene.getStylesheets().add(getClass().getResource("plugin.css").toExternalForm());
        } catch (Exception e) {
            // Ignore if missing
        }

        stage.setTitle("Volt Charge - Dashboard");
        stage.setScene(scene);
        stage.setMaximized(false);
        stage.setMaximized(true);
        stage.show();

        activeInstance = this;

        // Start simulation only once
        if (!threadStarted) {
            threadStarted = true;
            Thread simThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        updateSimulation();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            simThread.setDaemon(true);
            simThread.start();
        }

        // Ensure initial seeds only trigger once if lists are empty
        if (recentActivities.isEmpty()) {
            recentActivities.add(new ActivityLog("\u26A0", "#ff716c", "System Warning D-04",
                    "Network connection unstable \u2022 12m ago"));
        }
    }

    private void updateSimulation() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
                Statement stmt = conn.createStatement()) {

            // Create table if not exists
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS sessions (slot TEXT, username TEXT, protocol TEXT, start_time TEXT)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS pending_payments (slot TEXT, username TEXT, protocol TEXT, start_time TEXT, progress REAL DEFAULT 100.0)");
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS completed_sessions (slot TEXT, username TEXT, protocol TEXT, start_time TEXT, end_time TEXT, revenue REAL)");

            List<String> dbSlots = new ArrayList<>();

            // 1. Fetch Charging Sessions
            ResultSet rs = stmt.executeQuery("SELECT slot, username, protocol, start_time FROM sessions");

            while (rs.next()) {
                String slot = rs.getString("slot");
                String user = rs.getString("username");
                String protocol = rs.getString("protocol");
                String stTime = rs.getString("start_time");
                String status = "CHARGING";
                dbSlots.add(slot);

                // Add to simulation if not running
                activeCharges.putIfAbsent(slot, new SimSession(slot, user, protocol, stTime, status));
                if (activeCharges.containsKey(slot))
                    activeCharges.get(slot).status = status;
            }
            rs.close();

            // 2. Fetch Pending Payments
            ResultSet rsPending = stmt
                    .executeQuery("SELECT slot, username, protocol, start_time, progress FROM pending_payments");
            while (rsPending.next()) {
                String slot = rsPending.getString("slot");
                String user = rsPending.getString("username");
                String protocol = rsPending.getString("protocol");
                String stTime = rsPending.getString("start_time");
                double prog = rsPending.getDouble("progress");
                String status = "Payment Pending";
                dbSlots.add(slot);

                activeCharges.putIfAbsent(slot, new SimSession(slot, user, protocol, stTime, status));
                if (activeCharges.containsKey(slot)) {
                    activeCharges.get(slot).status = status;
                    activeCharges.get(slot).progress = prog;
                }
            }
            rsPending.close();

            // Remove slots that were deleted outside
            activeCharges.keySet().removeIf(k -> !dbSlots.contains(k));

            // Update progress
            Iterator<Map.Entry<String, SimSession>> it = activeCharges.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, SimSession> entry = it.next();
                SimSession sim = entry.getValue();
                if (sim.progress < 100 && !"Payment Pending".equals(sim.status)) {
                    sim.progress += 3.5; // +3.5% per second for fast simulation
                    sim.timeElapsed++;
                }

                if (sim.progress >= 100 && !"Payment Pending".equals(sim.status)) {
                    sim.progress = 100;
                    sim.status = "Payment Pending";
                    // Log completion
                    String energy = sim.protocol.contains("Type 1") ? "42.0 kWh" : "62.4 kWh";
                    double rev = sim.protocol.contains("Type 1") ? 14.50 : 22.80;
                    try {
                        ResultSet rsSettings = stmt.executeQuery("SELECT key, value FROM settings");
                        double type1Std = 0.44;
                        double type2Std = 0.44;
                        double type1Ultra = 0.88;
                        double type2Ultra = 0.88;
                        while(rsSettings.next()) {
                            if("t1s".equals(rsSettings.getString("key"))) type1Std = Double.parseDouble(rsSettings.getString("value"));
                            if("t2s".equals(rsSettings.getString("key"))) type2Std = Double.parseDouble(rsSettings.getString("value"));
                            if("t1u".equals(rsSettings.getString("key"))) type1Ultra = Double.parseDouble(rsSettings.getString("value"));
                            if("t2u".equals(rsSettings.getString("key"))) type2Ultra = Double.parseDouble(rsSettings.getString("value"));
                        }
                        double kwh = sim.protocol.contains("Type 1") ? 42.0 : 62.4;
                        double rate = 0;
                        if (sim.protocol.contains("Type 1") && sim.protocol.contains("Standard")) rate = type1Std;
                        else if (sim.protocol.contains("Type 2") && sim.protocol.contains("Standard")) rate = type2Std;
                        else if (sim.protocol.contains("Type 1") && sim.protocol.contains("Ultra Fast")) rate = type1Ultra;
                        else if (sim.protocol.contains("Type 2") && sim.protocol.contains("Ultra Fast")) rate = type2Ultra;
                        else rate = type1Std; // Default fallback
                        
                        rev = kwh * rate;
                    } catch (Exception ignored) {}
                    
                    recentActivities.add(0, new ActivityLog("\u2713", "#3fff8b", "Slot " + sim.slot + " Completed",
                            energy + " \u2022 100% \u2022 Just now"));
                    if (recentActivities.size() > 4)
                        recentActivities.remove(recentActivities.size() - 1);

                    // Move to completed_sessions handled in Checkout.java confirmation now


                    // Insert to Pending Payments
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO pending_payments (slot, username, protocol, start_time, progress) VALUES (?, ?, ?, ?, ?)")) {
                        pstmt.setString(1, sim.slot);
                        pstmt.setString(2, sim.user);
                        pstmt.setString(3, sim.protocol);
                        pstmt.setString(4, sim.startTime);
                        pstmt.setDouble(5, sim.progress);
                        pstmt.executeUpdate();
                    }

                    // Delete from charging sessions
                    try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM sessions WHERE slot = ?")) {
                        pstmt.setString(1, sim.slot);
                        pstmt.executeUpdate();
                    }
                    // Keep it in active charges so dashboard shows it as payment pending
                }
            }

            // Calculate Revenue ONLY from finalized payments in completed_sessions
            String todayDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            ResultSet revRs = stmt.executeQuery(
                    "SELECT SUM(revenue) FROM completed_sessions WHERE end_time LIKE '" + todayDate + "%'");
            double calRev = 0.0;
            if (revRs.next())
                calRev = revRs.getDouble(1);
            final double currentRev = calRev;


            // Push to UI thread using static instance check
            Platform.runLater(() -> {
                if (activeInstance != null) {
                    if (activeInstance.lblRevenueToday != null) {
                        activeInstance.lblRevenueToday.setText(String.format("$%.2f", currentRev));
                    }
                    activeInstance.refreshUI();
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshUI() {
        lblActiveSessions.setText(String.valueOf(activeCharges.size()));
        lblTotalSlots.setText(String.valueOf(ALL_SLOTS.size()));

        for (String slotName : ALL_SLOTS) {
            SlotUI ui = uiSlots.get(slotName);
            if (ui == null)
                continue;

            boolean visible = true;
            if (!searchQuery.isEmpty() && !slotName.toLowerCase().contains(searchQuery)) {
                visible = false;
            }

            if (!slotSpeedFilter.equals("ALL")) {
                boolean isHigh = slotName.startsWith("A") || slotName.startsWith("B");
                if (slotSpeedFilter.equals("HIGH_SPEED") && !isHigh) visible = false;
                if (slotSpeedFilter.equals("STANDARD") && isHigh) visible = false;
            }

            SimSession sim = activeCharges.get(slotName);
            String currentStatus = (sim == null) ? "AVAILABLE" : ("Payment Pending".equals(sim.status) ? "PAYMENT PENDING" : "CHARGING");
            
            if (!slotStatusFilter.equals("ALL")) {
                if (slotStatusFilter.equals("AVAILABLE") && !currentStatus.equals("AVAILABLE")) visible = false;
                if (slotStatusFilter.equals("CHARGING") && (!currentStatus.equals("CHARGING") && !currentStatus.equals("PAYMENT PENDING"))) visible = false;
                if (slotStatusFilter.equals("MAINTENANCE") && !currentStatus.equals("MAINTENANCE")) visible = false;
            }

            ui.card.setVisible(visible);
            ui.card.setManaged(visible);

            if (activeCharges.containsKey(slotName)) {
                final SimSession finalSim = activeCharges.get(slotName);
                String rate = finalSim.protocol.contains("Ultra") ? "280 kW/h" : "120 kW/h";
                int timeLeft = (int) ((100 - finalSim.progress) / 3.5);
                if ("Payment Pending".equals(finalSim.status)) {
                    ui.update("Payment Pending", finalSim.user != null ? finalSim.user : "Unknown User", "#ffae00", (int)finalSim.progress + "%",
                            "Stopped", "Awaiting checkout", true);
                } else {
                    ui.update("Charging", finalSim.user != null ? finalSim.user : "Unknown User", "#81ecff",
                            (int) finalSim.progress + "%", rate, timeLeft + "s left", true);
                }

                // Set interaction
                final String capturedSlot = finalSim.slot;
                ui.card.setOnMouseClicked(e -> {
                    System.out.println("CLICK: Slot=" + capturedSlot + " Status=" + finalSim.status);
                    try {
                        Stage currentStage = (Stage) ui.card.getScene().getWindow();
                        if ("Payment Pending".equals(finalSim.status)) {
                            System.out.println("  -> Opening Checkout");
                            new Checkout(capturedSlot).start(currentStage);
                        } else {
                            System.out.println("  -> Opening ActiveSession");
                            new ActiveSession(capturedSlot).start(currentStage);
                        }
                    } catch (Exception ex) {
                        System.out.println("  -> ERROR: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });


            } else {
                ui.update("Ready", "Available", "#3fff8b", null, null, null, true);
                ui.card.setOnMouseClicked(e -> {
                    try {
                        new Plugin(slotName).start((Stage) ui.card.getScene().getWindow());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

        }

        // Activity Log update
        activityBox.getChildren().clear();
        activityBox.getChildren().add(actTitle);
        for (ActivityLog act : recentActivities) {
            activityBox.getChildren().add(createActivityRow(act.icon, act.color, act.title, act.sub));
        }
    }

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
                createNavItem("Dashboard", true, stage),
                createNavItem("Plug In", false, stage));
        VBox.setVgrow(navMenu, Priority.ALWAYS);

        VBox bottomMenu = new VBox(8);
        bottomMenu.setPadding(new Insets(24, 0, 24, 0));
        bottomMenu.getChildren().addAll(
                createNavItem("Support", false, stage),
                createNavItem("Settings", false, stage));

        Button emergency = new Button("EMERGENCY STOP");
        emergency.setMaxWidth(Double.MAX_VALUE);
        emergency.setStyle(
                "-fx-background-color: #9f0519; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 12 0; -fx-cursor: hand;");
        emergency.setOnAction(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM sessions");
                stmt.execute("DELETE FROM pending_payments");
                activeCharges.clear();
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
        box.setStyle(active
                ? "-fx-background-color: #1a1a1a; -fx-border-color: transparent #00E5FF transparent transparent; -fx-border-width: 0 4 0 0;"
                : "-fx-background-color: transparent;");

        Label label = new Label(text);
        label.setStyle(active ? "-fx-text-fill: #00E5FF; -fx-font-weight: bold; -fx-font-size: 14px;"
                : "-fx-text-fill: #adaaaa; -fx-font-size: 14px;");

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
        HBox topNav = new HBox(16);
        topNav.setPrefHeight(64);
        topNav.setAlignment(Pos.CENTER_LEFT);
        topNav.setPadding(new Insets(0, 32, 0, 32));
        topNav.setStyle("-fx-background-color: rgba(14, 14, 14, 0.8);");

        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle("-fx-background-color: #20201f; -fx-padding: 6 12; -fx-background-radius: 2;");
        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setTextFill(Color.web("#adaaaa"));
        TextField searchField = new TextField();
        searchField.setPromptText("Search station ID...");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        searchField.setPrefWidth(256);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            this.searchQuery = newV.toLowerCase();
            javafx.application.Platform.runLater(this::refreshUI);
        });
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


    private VBox createMainContent() {
        VBox content = new VBox(32);
        content.setPadding(new Insets(32));

        lblActiveSessions = new Label("0");
        lblTotalSlots = new Label("8");
        slotGrid = new FlowPane();
        slotGrid.setHgap(16);
        slotGrid.setVgap(16);

        // Initialize UI Elements Once
        for (String slotName : ALL_SLOTS) {
            SlotUI ui = new SlotUI(slotName);
            uiSlots.put(slotName, ui);
            slotGrid.getChildren().add(ui.card);
        }

        activityBox = new VBox(16);
        activityBox.setPrefWidth(350);
        activityBox.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 4; -fx-padding: 24;");
        actTitle = new Label("RECENT ACTIVITY");
        actTitle.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");

        // High-Level Summary Bento
        HBox summaryBento = new HBox(24);

        VBox slotsCard = createSummaryCard("Total Slots", lblTotalSlots, "Online", "#81ecff");
        VBox activeCard = createSummaryCard("Active Sessions", lblActiveSessions, "In Progress", "#81ecff");

        VBox revenueCard = new VBox(16);
        revenueCard.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(129,236,255,0.1), #1a1a1a); -fx-background-radius: 4; -fx-padding: 24;");
        HBox revHeader = new HBox();
        revHeader.setAlignment(Pos.CENTER_LEFT);
        Label revTitle = new Label("REVENUE TODAY");
        revTitle.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
        revHeader.getChildren().add(revTitle);

        HBox revContent = new HBox(12);
        revContent.setAlignment(Pos.BASELINE_LEFT);
        lblRevenueToday = new Label("$0.00");
        lblRevenueToday.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold;");
        Label revTarget = new Label("92% of target");
        revTarget.setStyle("-fx-text-fill: #3fff8b; -fx-font-size: 12px; -fx-font-weight: bold;");
        revContent.getChildren().addAll(lblRevenueToday, revTarget);
        revenueCard.getChildren().addAll(revHeader, revContent);

        HBox.setHgrow(revenueCard, Priority.ALWAYS);
        summaryBento.getChildren().addAll(slotsCard, activeCard, revenueCard);

        // Dashboard Grid
        VBox gridSection = new VBox(24);

        HBox gridHeader = new HBox();
        gridHeader.setAlignment(Pos.CENTER_LEFT);
        Label gridTitle = new Label("Live Charging Grid");
        gridTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Region gSpacer = new Region();
        HBox.setHgrow(gSpacer, Priority.ALWAYS);
        HBox filterBtns = new HBox(8);
        filterBtns.getChildren().addAll(
                createBtn("All Slots", true, "ALL"),
                createBtn("High Speed", false, "HIGH_SPEED"),
                createBtn("Standard", false, "STANDARD"));
        gridHeader.getChildren().addAll(gridTitle, gSpacer, filterBtns);

        // Status Filter Tabs
        HBox statusTabs = new HBox(16);
        statusTabs.setStyle("-fx-background-color: #131313; -fx-padding: 4; -fx-background-radius: 4;");
        statusTabs.getChildren().addAll(
                createStatusTab("All", "#ffffff", true),
                createStatusTab("Available", "#3fff8b", false),
                createStatusTab("Charging", "#81ecff", false),
                createStatusTab("Maintenance", "#ff716c", false));
        
        for (javafx.scene.Node n : statusTabs.getChildren()) {
            n.setOnMouseClicked(e -> {
                for (javafx.scene.Node node : statusTabs.getChildren()) {
                    node.setStyle("-fx-padding: 8 16;");
                    ((Label)((HBox)node).getChildren().get(1)).setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
                }
                n.setStyle("-fx-background-color: #262626; -fx-padding: 8 16; -fx-background-radius: 2;");
                ((Label)((HBox)n).getChildren().get(1)).setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
                
                String tabTxt = ((Label)((HBox)n).getChildren().get(1)).getText();
                this.slotStatusFilter = tabTxt.toUpperCase();
                refreshUI();
            });
        }

        FlowPane.setMargin(gridHeader, new Insets(0, 0, 0, 0));

        gridSection.getChildren().addAll(gridHeader, statusTabs, slotGrid);

        // Bottom Layout
        HBox bottomSection = new HBox(32);

        VBox mapBox = new VBox(8);
        mapBox.setStyle("-fx-background-color: #131313; -fx-background-radius: 4; -fx-padding: 24;");
        Label mapTitle = new Label("Station Network");
        mapTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        ImageView mapPlaceholder = new ImageView();
        try {
            mapPlaceholder.setImage(new javafx.scene.image.Image(getClass().getResource("signup-bg.png").toExternalForm()));
            mapPlaceholder.setFitWidth(400);
            mapPlaceholder.setFitHeight(200);
            mapPlaceholder.setPreserveRatio(false);
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(400, 200);
            clip.setArcWidth(8); clip.setArcHeight(8);
            mapPlaceholder.setClip(clip);
        } catch (Exception e) {}
        
        mapBox.getChildren().add(mapPlaceholder);
        HBox.setHgrow(mapBox, Priority.ALWAYS);


        bottomSection.getChildren().addAll(mapBox, activityBox);

        content.getChildren().addAll(summaryBento, gridSection, bottomSection);
        refreshUI();
        return content;
    }

    private VBox createSummaryCard(String title, Label lblVal, String sub, String colorStr) {
        VBox card = new VBox(16);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 4; -fx-padding: 24;");

        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");

        VBox content = new VBox(8);
        lblVal.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        Label lblSub = new Label(sub);
        lblSub.setStyle("-fx-text-fill: " + colorStr + "; -fx-font-size: 10px;");
        content.getChildren().addAll(lblVal, lblSub);

        card.getChildren().addAll(lblTitle, content);
        return card;
    }

    private Button createBtn(String txt, boolean active, String filter) {
        Button btn = new Button(txt.toUpperCase());
        btn.setStyle(active
                ? "-fx-background-color: #262626; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 8 16;"
                : "-fx-background-color: transparent; -fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 8 16;");
        btn.setOnAction(e -> {
            if (btn.getParent() != null) {
                for (javafx.scene.Node sibling : ((HBox)btn.getParent()).getChildren()) {
                    sibling.setStyle("-fx-background-color: transparent; -fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 8 16;");
                }
            }
            btn.setStyle("-fx-background-color: #262626; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 8 16;");
            
            if (txt.equals("All Slots")) this.slotSpeedFilter = "ALL";
            else if (txt.equals("High Speed")) this.slotSpeedFilter = "HIGH_SPEED";
            else if (txt.equals("Standard")) this.slotSpeedFilter = "STANDARD";
            
            refreshUI();
        });
        return btn;
    }

    private HBox createStatusTab(String txt, String color, boolean active) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER);
        box.setStyle(active ? "-fx-background-color: #262626; -fx-padding: 8 16; -fx-background-radius: 2;"
                : "-fx-padding: 8 16;");
        Circle dot = new Circle(4, Color.web(color));
        Label l = new Label(txt);
        l.setStyle(active ? "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;"
                : "-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
        box.getChildren().addAll(dot, l);
        return box;
    }

    private HBox createActivityRow(String iconStr, String color, String title, String sub) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Circle iconBg = new Circle(12, Color.web(color, 0.1));
        Label sym = new Label(iconStr);
        sym.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        StackPane sp = new StackPane(iconBg, sym);

        VBox text = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label s = new Label(sub);
        s.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px;");
        text.getChildren().addAll(t, s);

        row.getChildren().addAll(sp, text);
        return row;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
