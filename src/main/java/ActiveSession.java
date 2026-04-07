import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ActiveSession extends Application {

    private String slotId;
    private String user = "Unknown User";
    private String protocol = "Standard Protocol";
    private Label lblCar;
    private Label lblPct;
    private Arc progressArc;
    private Label lblDuration;
    private Label lblCost;
    private Label lblEstTime;
    private int estSeconds = 2280; // 38 mins

    // Telemetry labels
    private Label lblCurrentPower = new Label("142.5");
    private Label lblPeakSpeed = new Label("250");
    private Label lblVoltage = new Label("800");
    private Label lblCellTemp = new Label("34.2");
    private Label lblCoolantFlow = new Label("12.5");
    private Label lblGridStability = new Label("99.9");

    public ActiveSession() {
        this.slotId = null;
    }

    public ActiveSession(String slotId) {
        this.slotId = slotId;
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        
        // Gradient Background
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0a0e27")),
            new Stop(0.5, Color.web("#162e47")),
            new Stop(1, Color.web("#0d3d5c"))
        );
        root.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        root.setStyle("-fx-font-family: 'Inter', sans-serif;");

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

        stage.setTitle("Volt Charge - Active Session");
        stage.setScene(scene);
        stage.setMaximized(false);
        stage.setMaximized(true);
        stage.show();

        setupLiveTelemetry();
    }

    // Live telemetry setup – 1-second timeline that pulls SimSession data and updates all labels
    private void setupLiveTelemetry() {
        if (slotId == null)
            return;

        // Fetch rates once
        double[] settingsRates = { 0.44, 0.44, 0.88, 0.88 }; // t1s, t2s, t1u, t2u
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:sessions.db");
                java.sql.Statement stmt = conn.createStatement()) {
            java.sql.ResultSet rsSettings = stmt.executeQuery("SELECT key, value FROM settings");
            while (rsSettings.next()) {
                String k = rsSettings.getString("key");
                String v = rsSettings.getString("value");
                if ("t1s".equals(k))
                    settingsRates[0] = Double.parseDouble(v);
                else if ("t2s".equals(k))
                    settingsRates[1] = Double.parseDouble(v);
                else if ("t1u".equals(k))
                    settingsRates[2] = Double.parseDouble(v);
                else if ("t2u".equals(k))
                    settingsRates[3] = Double.parseDouble(v);
            }
        } catch (Exception ignored) {
        }

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            Dashboard.SimSession sim = Dashboard.getSimSession(slotId);
            if (sim != null) {
                // Update Progress
                lblPct.setText(String.valueOf((int) sim.progress));
                if (progressArc != null)
                    progressArc.setLength(-360 * (sim.progress / 100.0));

                // Details
                lblCar.setText((sim.user != null ? sim.user : "Unknown") + " \u2022 ");

                // Timing & Cost
                int totalSecs = sim.timeElapsed;
                int hrs = totalSecs / 3600;
                int mins = (totalSecs % 3600) / 60;
                int secs = totalSecs % 60;
                if (lblDuration != null) {
                    lblDuration.setText(String.format("%02d:%02d:%02d", hrs, mins, secs));
                }

                // Decrement Estimated Time
                if (estSeconds > 0) {
                    estSeconds--;
                    int estH = estSeconds / 3600;
                    int estM = (estSeconds % 3600) / 60;
                    int estS = estSeconds % 60;
                    if (lblEstTime != null)
                        lblEstTime.setText(String.format("~%02d:%02d:%02d", estH, estM, estS));
                }

                if (lblCost != null) {

                    double r = settingsRates[0];
                    if (sim.protocol.contains("Type 1") && sim.protocol.contains("Standard"))
                        r = settingsRates[0];
                    else if (sim.protocol.contains("Type 2") && sim.protocol.contains("Standard"))
                        r = settingsRates[1];
                    else if (sim.protocol.contains("Type 1") && sim.protocol.contains("Ultra Fast"))
                        r = settingsRates[2];
                    else if (sim.protocol.contains("Type 2") && sim.protocol.contains("Ultra Fast"))
                        r = settingsRates[3];

                    double totalKwh = sim.protocol.contains("Type 1") ? 42.0 : 62.4;
                    double costAt100 = totalKwh * r;
                    lblCost.setText(String.format("$%.2f", (sim.progress / 100.0) * costAt100));
                }

                // Dwindling Telemetry values simulation
                double currPower = 142.0 + (Math.random() * 5 - 2.5);
                lblCurrentPower.setText(String.format("%.1f", currPower));

                double voltage = 800.0 + (Math.random() * 4 - 2);
                lblVoltage.setText(String.format("%.0f", voltage));

                double peakSpeed = 250.0 + (Math.random() * 2 - 1);
                lblPeakSpeed.setText(String.format("%.0f", Math.min(252, Math.max(248, peakSpeed))));

                double cellTemp = 34.0 + (Math.random() * 2 - 1);
                lblCellTemp.setText(String.format("%.1f", cellTemp));

                double coolant = 12.5 + (Math.random() - 0.5);
                lblCoolantFlow.setText(String.format("%.1f", Math.max(12.0, coolant)));

                double grid = 99.5 + (Math.random() * 0.5);
                lblGridStability.setText(String.format("%.1f", Math.min(100.0, grid)));

            } else {
                lblPct.setText("100");
                if (progressArc != null)
                    progressArc.setLength(-360);
                // Zero out live stats if completed
                lblCurrentPower.setText("0.0");
                lblVoltage.setText("0");
                lblCoolantFlow.setText("0.0");
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
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
                createNavItem("Settings", false, stage));

        Button emergency = new Button("EMERGENCY STOP");
        emergency.setMaxWidth(Double.MAX_VALUE);
        emergency.setStyle(
                "-fx-background-color: #9f0519; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 12 0; -fx-cursor: hand;");
        emergency.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Emergency Stop");
            alert.setHeaderText("All charging processes halted!");
            alert.setContentText("Emergency shutdown initiated across all stations.");
            alert.showAndWait();
        });
        VBox emergencyBox = new VBox(emergency);
        emergencyBox.setPadding(new Insets(32, 16, 0, 16));

        bottomMenu.getChildren().add(emergencyBox);

        sidebar.getChildren().addAll(header, navMenu, bottomMenu);
        return sidebar;
    }

    // Nav item – single clickable row that routes to the selected module
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
                    ((BorderPane) stage.getScene().getRoot()).setCenter(placeholder);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return box;
    }

    // Top navbar – search bar, system live badge and user avatar menu
    private HBox createTopNav() {
        HBox topNav = new HBox(16);
        topNav.setPrefHeight(64);
        topNav.setAlignment(Pos.CENTER_LEFT);
        topNav.setPadding(new Insets(0, 32, 0, 32));
        topNav.setStyle("-fx-background-color: rgba(14, 14, 14, 0.8);");

        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle("-fx-background-color: #20201f; -fx-padding: 6 12; -fx-background-radius: 16;");
        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setTextFill(Color.web("#adaaaa"));
        TextField searchField = new TextField();
        searchField.setPromptText("Search stations or sessions...");
        searchField.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        searchField.setPrefWidth(256);
        searchBox.getChildren().addAll(searchIcon, searchField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox sysLiveBox = new HBox(4);
        sysLiveBox.setAlignment(Pos.CENTER);
        sysLiveBox.setStyle(
                "-fx-background-color: rgba(63, 255, 139, 0.1); -fx-border-color: rgba(63, 255, 139, 0.2); -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 4 12;");
        Circle dot = new Circle(4, Color.web("#3fff8b"));
        Label sysTxt = new Label("SYSTEM LIVE");
        sysTxt.setStyle("-fx-text-fill: #3fff8b; -fx-font-size: 10px; -fx-font-weight: bold;");
        sysLiveBox.getChildren().addAll(dot, sysTxt);

        Circle avatar = new Circle(16, Color.web("#81ecff"));
        avatar.setStyle("-fx-cursor: hand;");
        avatar.setOnMouseClicked(e -> {
            ContextMenu profileMenu = new ContextMenu(new MenuItem("Profile"), new MenuItem("Log Out"));
            profileMenu.getItems().get(1).setOnAction(evt -> handleLogout((Stage) avatar.getScene().getWindow()));
            profileMenu.show(avatar, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        HBox profileBox = new HBox();
        profileBox.getChildren().addAll(avatar);

        topNav.getChildren().addAll(searchBox, spacer, profileBox);
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
        // Fetch Current Session Details
        if (slotId != null) {
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:sessions.db")) {
                String query = "SELECT username, protocol FROM sessions WHERE slot = ?";
                java.sql.PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1, slotId);
                java.sql.ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    this.user = rs.getString("username");
                    this.protocol = rs.getString("protocol");
                }
            } catch (java.sql.SQLException ex) {
                ex.printStackTrace();
            }
        }

        VBox content = new VBox(32);
        content.setPadding(new Insets(40));

        // Page header – station ID badge, session title and vehicle connection info
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.BOTTOM_LEFT);

        VBox headerLeft = new VBox(8);
        HBox idBox = new HBox(12);
        idBox.setAlignment(Pos.CENTER_LEFT);
        Label lblStation = new Label("STATION " + (slotId != null ? slotId : "N/A"));
        lblStation.setStyle(
                "-fx-background-color: rgba(63,255,139,0.1); -fx-text-fill: #3fff8b; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 4;");
        Label lblSessionId = new Label("Session For: " + user);
        lblSessionId.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 12px; -fx-font-weight: bold;");
        idBox.getChildren().addAll(lblStation, lblSessionId);

        Label lblTitle = new Label("Active Session");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 48px; -fx-font-weight: bold;");
        headerLeft.getChildren().addAll(idBox, lblTitle);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        VBox headerRight = new VBox(4);
        headerRight.setAlignment(Pos.BOTTOM_RIGHT);
        Label lblConn = new Label("Vehicle Connected (" + protocol + ")");
        lblConn.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblCar = new Label(user + " \u2022 ");
        lblCar.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label lblStatus = new Label("Charging");
        lblStatus.setStyle("-fx-text-fill: #81ecff; -fx-font-size: 18px; -fx-font-weight: bold;");
        HBox carStatusBox = new HBox();
        carStatusBox.setAlignment(Pos.CENTER_RIGHT);
        carStatusBox.getChildren().addAll(lblCar, lblStatus);
        headerRight.getChildren().addAll(lblConn, carStatusBox);

        headerBox.getChildren().addAll(headerLeft, hSpacer, headerRight);

        // Core Interactive Grid
        HBox gridHBox = new HBox(32);

        VBox leftCol = createLeftGaugeColumn();
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        VBox rightCol = createRightMetricsColumn();
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.setPrefWidth(450); // Set roughly 5 columns relative to 7

        gridHBox.getChildren().addAll(leftCol, rightCol);

        // Live Data Feed (Bottom)
        VBox bottomSection = new VBox(24);

        HBox botHeader = new HBox();
        botHeader.setAlignment(Pos.CENTER_LEFT);
        Label botTitle = new Label("Real-time Telemetry");
        botTitle.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);
        Label botUpdate = new Label("UPDATED 0.2S AGO");
        botUpdate.setStyle(
                "-fx-background-color: #20201f; -fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 16;");
        botHeader.getChildren().addAll(botTitle, bSpacer, botUpdate);

        HBox telemetryCards = new HBox(24);
        telemetryCards.getChildren().addAll(
                createTelemetryCard("Cell Temperature", lblCellTemp, "\u00B0C", "#3fff8b"),
                createTelemetryCard("Coolant Flow", lblCoolantFlow, "L/min", "#81ecff"),
                createTelemetryCard("Grid Stability", lblGridStability, "%", "#3fff8b"),
                createTelemetryCard("Cable Tension", new Label("Nominal"), "✓", "#81ecff"));
        for (javafx.scene.Node n : telemetryCards.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
            ((VBox) n).setMaxWidth(Double.MAX_VALUE);
        }

        bottomSection.getChildren().addAll(botHeader, telemetryCards);

        content.getChildren().addAll(headerBox, gridHBox, bottomSection);
        return content;
    }

    // Left gauge column – segmented charge bar, percentage display and power/speed/voltage stats
    private VBox createLeftGaugeColumn() {
        VBox box = new VBox(48);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 12; -fx-padding: 48;");
        // Segmented Gauge representation
        FlowPane segmentedGauge = new FlowPane();
        segmentedGauge.setHgap(8);
        segmentedGauge.setVgap(8);
        segmentedGauge.setPrefWrapLength(300);
        segmentedGauge.setAlignment(Pos.CENTER);

        for (int i = 0; i < 20; i++) {
            Rectangle r = new Rectangle(12, 30, Color.web("#202020"));
            r.setArcWidth(4);
            r.setArcHeight(4);
            segmentedGauge.getChildren().add(r);
        }

        // Initialize lblPct BEFORE attaching listener
        lblPct = new Label("0");
        lblPct.setStyle("-fx-text-fill: white; -fx-font-size: 72px; -fx-font-weight: bold;");

        // Progress Listener for modern gauge
        lblPct.textProperty().addListener((obs, oldV, newV) -> {
            try {
                int p = Integer.parseInt(newV.replace("%", "").trim());
                int blocks = (p * 20) / 100;
                for (int i = 0; i < 20; i++) {
                    Rectangle r = (Rectangle) segmentedGauge.getChildren().get(i);
                    r.setFill(i < blocks ? Color.web("#3fff8b") : Color.web("#202020"));
                }
            } catch (NumberFormatException ignored) {
            }
        });

        VBox gaugeText = new VBox(20);
        gaugeText.setAlignment(Pos.CENTER);

        HBox pctBox = new HBox(4);
        pctBox.setAlignment(Pos.CENTER);
        Label lblSym = new Label("%");
        lblSym.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 24px; -fx-font-weight: bold;");
        pctBox.getChildren().addAll(lblPct, lblSym);

        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER);
        Label lblBolt = new Label("\u26A1");
        lblBolt.setStyle("-fx-text-fill: #3fff8b;");
        Label lblStatus = new Label("ENERGIZING");
        lblStatus.setStyle("-fx-text-fill: #3fff8b; -fx-font-size: 12px; -fx-font-weight: bold;");
        statusBox.getChildren().addAll(lblBolt, lblStatus);

        gaugeText.getChildren().addAll(segmentedGauge, pctBox, statusBox);
        box.getChildren().add(gaugeText);

        // Stats row – Current Power, Peak Speed and Voltage shown below the gauge
        HBox statsBox = new HBox(48);
        statsBox.setAlignment(Pos.CENTER);

        statsBox.getChildren().addAll(
                createGaugeStat("CURRENT POWER", lblCurrentPower, "kW"),
                createGaugeStat("PEAK SPEED", lblPeakSpeed, "kW"),
                createGaugeStat("VOLTAGE", lblVoltage, "V"));

        box.getChildren().addAll(statsBox);

        return box;
    }

    private VBox createGaugeStat(String title, Label v, String unit) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
        HBox valBox = new HBox(4);
        valBox.setAlignment(Pos.BASELINE_CENTER);
        v.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold;");
        Label u = new Label(unit);
        u.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 18px; -fx-font-weight: bold;");
        valBox.getChildren().addAll(v, u);
        box.getChildren().addAll(t, valBox);
        return box;
    }

    // Right metrics column – session health card, location map card and action buttons
    private VBox createRightMetricsColumn() {
        VBox box = new VBox(32);

        // Session health card – time elapsed, estimated total, energy delivered and current cost
        VBox healthCard = new VBox(24);
        healthCard.setStyle(
                "-fx-background-color: rgba(26,26,26,0.7); -fx-border-color: rgba(255,255,255,0.05); -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 32;");

        Label hTitle = new Label("\u23F2 SESSION HEALTH");
        hTitle.setStyle("-fx-text-fill: #81ecff; -fx-font-size: 12px; -fx-font-weight: bold;");

        VBox durationBox = new VBox(4);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        Label dTitle = new Label("TIME ELAPSED");
        dTitle.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
        lblDuration = new Label("00:00:00");
        lblDuration.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 36px; -fx-font-weight: bold;");
        durationBox.getChildren().addAll(dTitle, lblDuration);

        VBox estBox = new VBox(4);
        estBox.setAlignment(Pos.CENTER_RIGHT);
        Label eTitle = new Label("ESTIMATED TOTAL");
        eTitle.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
        lblEstTime = new Label("~00:38:00");
        lblEstTime.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 24px; -fx-font-weight: bold;");
        estBox.getChildren().addAll(eTitle, lblEstTime);

        HBox row1 = new HBox();
        row1.getChildren().addAll(durationBox, createSpacer(), estBox);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: rgba(255,255,255,0.05);");

        VBox energyBox = new VBox(4);
        energyBox.setAlignment(Pos.CENTER_LEFT);
        Label nTitle = new Label("ENERGY DELIVERED");
        nTitle.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label nVal = new Label("24.0 kWh");
        nVal.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 36px; -fx-font-weight: bold;");
        energyBox.getChildren().addAll(nTitle, nVal);

        VBox costBox = new VBox(4);
        costBox.setAlignment(Pos.CENTER_RIGHT);
        Label cTitle = new Label("CURRENT COST");
        cTitle.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
        lblCost = new Label("$0.00");
        lblCost.setStyle("-fx-text-fill: #3fff8b; -fx-font-size: 36px; -fx-font-weight: bold;");
        costBox.getChildren().addAll(cTitle, lblCost);

        HBox row2 = new HBox();
        row2.getChildren().addAll(energyBox, createSpacer(), costBox);

        healthCard.getChildren().addAll(hTitle, row1, divider, row2);

        // Location card – station address overlay and station temperature footer
        VBox mapCard = new VBox();
        mapCard.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 12; -fx-overflow: hidden;");

        Region mapImg = new Region();
        mapImg.setPrefHeight(160);
        mapImg.setStyle("-fx-background-color: #20201f; -fx-background-radius: 12 12 0 0;");
        // Pretending map is here

        HBox mapLabelBox = new HBox(8);
        mapLabelBox.setAlignment(Pos.CENTER_LEFT);
        mapLabelBox.setStyle(
                "-fx-background-color: rgba(38,38,38,0.8); -fx-padding: 6 12; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.1);");
        Label lIcon = new Label("\uD83D\uDCCD");
        lIcon.setStyle("-fx-text-fill: #3fff8b;");
        Label lText = new Label("712 North Congress Ave, Austin");
        lText.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
        mapLabelBox.getChildren().addAll(lIcon, lText);

        // Inside a StackPane to overlay label on map
        StackPane mapStack = new StackPane(mapImg, mapLabelBox);
        StackPane.setAlignment(mapLabelBox, Pos.TOP_LEFT);
        StackPane.setMargin(mapLabelBox, new Insets(16));

        HBox mapFooter = new HBox();
        mapFooter.setPadding(new Insets(24));
        mapFooter.setAlignment(Pos.CENTER_LEFT);

        VBox tempBox = new VBox(4);
        Label t1 = new Label("STATION TEMPERATURE");
        t1.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");
        HBox t2Box = new HBox(8);
        t2Box.setAlignment(Pos.BASELINE_LEFT);
        Label t2 = new Label("42\u00B0C");
        t2.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label t3 = new Label("OPTIMAL");
        t3.setStyle("-fx-text-fill: #3fff8b; -fx-font-size: 10px; -fx-font-weight: bold;");
        t2Box.getChildren().addAll(t2, t3);
        tempBox.getChildren().addAll(t1, t2Box);

        mapFooter.getChildren().addAll(tempBox, createSpacer(), new Label("\uD83D\uDDFA  \uD83D\uDCF6"));
        mapCard.getChildren().addAll(mapStack, mapFooter);

        // Action buttons – Modify Target (boost progress) and Stop (end session early)
        HBox btnArea = new HBox(16);
        // Button – Modify Target: boosts session progress by 15% for demo purposes
        Button btnModify = new Button("MODIFY TARGET \u270E");
        btnModify.setStyle(
                "-fx-background-color: linear-gradient(to right, #81ecff, #00d4ec); -fx-text-fill: #003840; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 20; -fx-background-radius: 8; -fx-cursor: hand;");
        HBox.setHgrow(btnModify, Priority.ALWAYS);
        btnModify.setMaxWidth(Double.MAX_VALUE);
        btnModify.setOnAction(e -> {
            Dashboard.SimSession sim = Dashboard.getSimSession(slotId);
            if (sim != null) {
                sim.progress = Math.min(100, sim.progress + 15.0);
                btnModify.setText("BOOSTED! \u26A1");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> btnModify.setText("MODIFY TARGET \u270E"));
                pause.play();
            }
        });

        // Button – Stop: ends the session early and moves it to pending payments
        Button btnStop = new Button("STOP");
        btnStop.setStyle(
                "-fx-background-color: #262626; -fx-border-color: rgba(255,113,108,0.2); -fx-text-fill: #ff716c; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 20; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
        btnStop.setPrefWidth(120);
        btnStop.setOnAction(e -> {
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:sessions.db")) {
                Dashboard.SimSession sim = Dashboard.getSimSession(slotId);
                double currentProgress = (sim != null) ? sim.progress : 0;

                String username = user;
                String prot = protocol;
                String stTime = "Unknown";

                String query = "SELECT * FROM sessions WHERE slot = ?";
                try (java.sql.PreparedStatement pstmtS = conn.prepareStatement(query)) {
                    pstmtS.setString(1, slotId);
                    java.sql.ResultSet rs = pstmtS.executeQuery();
                    if (rs.next()) {
                        username = rs.getString("username");
                        prot = rs.getString("protocol");
                        stTime = rs.getString("start_time");
                    }
                }

                String insertSql = "INSERT INTO pending_payments (slot, username, protocol, start_time, progress) VALUES (?, ?, ?, ?, ?)";
                try (java.sql.PreparedStatement pstmtI = conn.prepareStatement(insertSql)) {
                    pstmtI.setString(1, slotId);
                    pstmtI.setString(2, username);
                    pstmtI.setString(3, prot);
                    pstmtI.setString(4, stTime);
                    pstmtI.setDouble(5, currentProgress);
                    pstmtI.executeUpdate();
                }

                String deleteSql = "DELETE FROM sessions WHERE slot = ?";
                try (java.sql.PreparedStatement pstmtD = conn.prepareStatement(deleteSql)) {
                    pstmtD.setString(1, slotId);
                    pstmtD.executeUpdate();
                }

                if (sim != null) {
                    sim.status = "Payment Pending";
                    sim.progress = currentProgress;
                }

                new Dashboard().start((Stage) box.getScene().getWindow());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnArea.getChildren().addAll(btnModify, btnStop);

        box.getChildren().addAll(healthCard, mapCard, createSpacer(), btnArea);
        return box;
    }

    private Region createSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    private VBox createHealthMetric(String title, String val, String color, String size) {
        VBox box = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label v = new Label(val);
        v.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "; -fx-font-weight: bold;");
        box.getChildren().addAll(t, v);
        return box;
    }

    private VBox createHealthMetricRight(String title, String val, String color, String size) {
        VBox box = createHealthMetric(title, val, color, size);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // Telemetry card – displays a single sensor reading (temperature, coolant, grid stability, etc.)
    private VBox createTelemetryCard(String title, Label v, String unit, String borderColor) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: #1a1a1a; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: transparent transparent transparent "
                        + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 4;");
        Label t = new Label(title.toUpperCase());
        t.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold;");

        HBox vBox = new HBox(4);
        vBox.setAlignment(Pos.BOTTOM_LEFT);
        v.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label u = new Label(unit);
        u.setStyle("-fx-text-fill: " + (unit.equals("\u2713") ? "#3fff8b" : "#adaaaa") + "; -fx-font-size: 14px;");
        vBox.getChildren().addAll(v, u);

        card.getChildren().addAll(t, vBox);
        return card;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
