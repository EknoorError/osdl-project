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

public class Checkout extends Application {
    private String slot;
    private String user = "Unknown User";
    private String protocol = "Standard Protocol";
    private double revenue = 31.91;
    private String energy = "54.2 kWh";
    private String duration = "01:42:05";

    public Checkout() {
        this.slot = "N/A";
    }

    public Checkout(String slot) {
        this.slot = slot;
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #000000; -fx-font-family: 'Inter', sans-serif;"); // Darker root background

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

        BorderPane contentWrap = new BorderPane();
        contentWrap.setPadding(new Insets(48));
        contentWrap.setCenter(createMainContent(stage));
        contentWrap.setBottom(createFooter());

        mainScroll.setContent(contentWrap);

        rightArea.setCenter(mainScroll);

        root.setCenter(rightArea);

        Scene scene = new Scene(root, 1280, 800);
        try {
            scene.getStylesheets().add(getClass().getResource("plugin.css").toExternalForm());
        } catch (Exception e) {
            // Ignore if missing
        }

        stage.setTitle("Volt Charge - Checkout");
        stage.setScene(scene);
        stage.setMaximized(false);
        stage.setMaximized(true);
        stage.show();
    }

    // Sidebar – left navigation panel with app title, nav links and emergency stop
    private VBox createSidebar(Stage stage) {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(256);
        sidebar.setStyle(
                "-fx-background-color: #0e0e0e; -fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 0 1 0 0;");
        sidebar.setPadding(new Insets(32, 0, 0, 0));

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 32, 32, 32));
        Label title = new Label("VOLT CHARGE");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitle = new Label("Power Admin v1.0");
        subtitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #adaaaa; -fx-opacity: 0.6;");
        header.getChildren().addAll(title, subtitle);

        // Nav menu – Dashboard and Plug In navigation links
        VBox navMenu = new VBox(8);
        navMenu.getChildren().addAll(
                createNavItem("Dashboard", false, stage),
                createNavItem("Plug In", false, stage));
        VBox.setVgrow(navMenu, Priority.ALWAYS);

        // Bottom nav items – Support, Settings and Emergency Stop button
        VBox bottomMenu = new VBox(8);
        bottomMenu.setPadding(new Insets(24, 0, 24, 0));
        bottomMenu.getChildren().addAll(
                createNavItem("Support", false, stage),
                createNavItem("Settings", false, stage));

        Button emergency = new Button("\u26A0 EMERGENCY STOP");
        emergency.setMaxWidth(Double.MAX_VALUE);
        emergency.setStyle(
                "-fx-background-color: #9f0519; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 12 0; -fx-cursor: hand; -fx-background-radius: 4;");
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
        searchField.setPromptText("Search sessions...");
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

    private VBox createMainContent(Stage stage) {
        // Query Database
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db")) {
            // First check pending_payments to get session details
            String query = "SELECT * FROM pending_payments WHERE slot = ? ORDER BY start_time DESC LIMIT 1";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, slot);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                this.user = rs.getString("username");
                this.protocol = rs.getString("protocol");
                double progress = rs.getDouble("progress");

                // Calculate revenue based on protocol
                double totalKwh = protocol.contains("Type 1") ? 42.0 : 62.4;
                double currentKwh = totalKwh * (progress / 100.0);
                double rate = 0.44; // Default
                try (Statement stmtS = conn.createStatement();
                        ResultSet rsS = stmtS.executeQuery("SELECT key, value FROM settings")) {
                    while (rsS.next()) {
                        String k = rsS.getString("key");
                        String v = rsS.getString("value");
                        if (protocol.contains("Type 1") && protocol.contains("Standard") && "t1s".equals(k))
                            rate = Double.parseDouble(v);
                        else if (protocol.contains("Type 2") && protocol.contains("Standard") && "t2s".equals(k))
                            rate = Double.parseDouble(v);
                        else if (protocol.contains("Type 1") && protocol.contains("Ultra Fast") && "t1u".equals(k))
                            rate = Double.parseDouble(v);
                        else if (protocol.contains("Type 2") && protocol.contains("Ultra Fast") && "t2u".equals(k))
                            rate = Double.parseDouble(v);
                    }
                } catch (Exception ignored) {
                }

                this.revenue = currentKwh * rate;
                this.energy = String.format("%.1f kWh", currentKwh);
                this.duration = (progress >= 100) ? "Session completed"
                        : String.format("Stopped at %d%%", (int) progress);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        VBox content = new VBox(32);
        content.setMaxWidth(1100);

        // Header
        VBox headerContent = new VBox(8);
        Label preHeader = new Label("SESSION COMPLETE");
        preHeader.setStyle(
                "-fx-text-fill: #3fff8b; -fx-font-weight: bold; -fx-letter-spacing: 2px; -fx-font-size: 12px;");
        Label title = new Label("Session Summary - " + (slot != null ? slot : "N/A"));
        title.setStyle("-fx-text-fill: white; -fx-font-size: 64px; -fx-font-weight: bold;");
        headerContent.getChildren().addAll(preHeader, title);

        // Columns
        HBox columns = new HBox(32);

        // Left Column (Billing & Payment)
        VBox leftCol = new VBox(24);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // Itemized billing breakdown card – energy, protocol and network fee rows
        VBox breakdownCard = new VBox(24);
        breakdownCard.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 32; -fx-background-radius: 12;");
        Label bdTitle = new Label("ITEMIZED BREAKDOWN (" + user + ")");
        bdTitle.setStyle(
                "-fx-text-fill: #adaaaa; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        VBox itemsBox = new VBox(0);
        itemsBox.getChildren().addAll(
                createBillingRow("Energy Delivered", energy, String.format("$%.2f", revenue * 0.75), true),
                createBillingRow("Protocol", protocol, String.format("$%.2f", revenue * 0.15), true),
                createBillingRow("Network & Handling", "Standard Tier", String.format("$%.2f", revenue * 0.10), false));
        breakdownCard.getChildren().addAll(bdTitle, itemsBox);

        // Payment method card – Cash option and Add New Method option
        VBox paymentCard = new VBox(24);
        paymentCard.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 32; -fx-background-radius: 12;");
        Label payTitle = new Label("PAYMENT METHOD");
        payTitle.setStyle(
                "-fx-text-fill: #adaaaa; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        HBox paymentOptions = new HBox(16);
        Button btnPay = new Button("CONFIRM & PAY \u203A");

        // Option 1
        VBox opt1 = new VBox(8);
        HBox.setHgrow(opt1, Priority.ALWAYS);
        opt1.setStyle(
                "-fx-background-color: #20201f; -fx-border-color: #81ecff; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 24;");
        HBox opt1Head = new HBox();
        Label cardIcon = new Label("\uD83D\uDCB5");
        cardIcon.setStyle("-fx-text-fill: #3fff8b; -fx-font-size: 18px;");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Circle radioDot = new Circle(6, Color.web("#3fff8b"));
        opt1Head.getChildren().addAll(cardIcon, sp1, radioDot);
        Label optSub = new Label("Pay at Counter");
        optSub.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 12px;");
        Label optMain = new Label("CASH");
        optMain.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        opt1.getChildren().addAll(opt1Head, optSub, optMain);

        // Option 2 (Add New)
        VBox opt2 = new VBox(8);
        HBox.setHgrow(opt2, Priority.ALWAYS);
        opt2.setAlignment(Pos.CENTER);
        opt2.setStyle(
                "-fx-background-color: rgba(26,26,26,0.5); -fx-border-color: rgba(72,72,71,0.5); -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 24; -fx-cursor: hand;");
        Label addIcon = new Label("\u2795");
        addIcon.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 18px;");
        Label addTxt = new Label("NEW METHOD");
        addTxt.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        opt2.getChildren().addAll(addIcon, addTxt);
        opt2.setOnMouseClicked(e -> {
            opt1.setStyle(
                    "-fx-background-color: #20201f; -fx-border-color: rgba(72,72,71,0.5); -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 24;");
            opt2.setStyle(
                    "-fx-background-color: #20201f; -fx-border-color: #81ecff; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 24;");
            btnPay.setText("ADD CARD & PAY \u203A");
            radioDot.setFill(Color.TRANSPARENT);
        });
        opt1.setOnMouseClicked(e -> {
            opt2.setStyle(
                    "-fx-background-color: rgba(26,26,26,0.5); -fx-border-color: rgba(72,72,71,0.5); -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 24; -fx-cursor: hand;");
            opt1.setStyle(
                    "-fx-background-color: #20201f; -fx-border-color: #81ecff; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 24;");
            btnPay.setText("CONFIRM & PAY \u203A");
            radioDot.setFill(Color.web("#81ecff"));
        });

        paymentOptions.getChildren().addAll(opt1, opt2);
        paymentCard.getChildren().addAll(payTitle, paymentOptions);

        leftCol.getChildren().addAll(breakdownCard, paymentCard);

        // Right Column (Totals and Actions)
        VBox rightCol = new VBox(24);
        rightCol.setPrefWidth(400);

        // Total amount card – shows the grand total, Confirm & Pay and Print Receipt buttons
        VBox totalCard = new VBox(24);
        totalCard.setStyle(
                "-fx-background-color: #20201f; -fx-border-color: transparent transparent transparent #81ecff; -fx-border-width: 0 0 0 4; -fx-border-radius: 4; -fx-background-radius: 12; -fx-padding: 32;");
        Label totTitle = new Label("TOTAL AMOUNT DUE");
        totTitle.setStyle(
                "-fx-text-fill: #adaaaa; -fx-font-size: 12px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");

        HBox totBox = new HBox(8);
        totBox.setAlignment(Pos.BASELINE_LEFT);
        Label dollar = new Label("$");
        dollar.setStyle("-fx-text-fill: #81ecff; -fx-font-size: 24px;");
        Label amount = new Label(String.format("%.2f", revenue));
        amount.setStyle("-fx-text-fill: white; -fx-font-size: 64px; -fx-font-weight: bold;");
        totBox.getChildren().addAll(dollar, amount);

        VBox btnBox = new VBox(12);
        btnPay.setMaxWidth(Double.MAX_VALUE);
        btnPay.setStyle(
                "-fx-background-color: linear-gradient(to right, #81ecff, #00d4ec); -fx-text-fill: #003840; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-letter-spacing: 2px;");
        btnPay.setOnAction(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db")) {
                String cleanSlot = slot != null ? slot.trim() : "";

                // 1. Move to completed_sessions (Mark as paid/finalized)
                String endTimeStr = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String sqlComplete = "INSERT INTO completed_sessions (slot, username, protocol, start_time, end_time, revenue) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlComplete)) {
                    pstmt.setString(1, cleanSlot);
                    pstmt.setString(2, user);
                    pstmt.setString(3, protocol);
                    pstmt.setString(4, "Session Start");
                    pstmt.setString(5, endTimeStr);
                    pstmt.setDouble(6, revenue);
                    pstmt.executeUpdate();
                }

                // 2. Clear from pending_payments
                String query = "DELETE FROM pending_payments WHERE slot = ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1, cleanSlot);
                int deletedRows = pstmt.executeUpdate();
                System.out.println("Payment Confirmed. Deleted " + deletedRows
                        + " rows from pending_payments for slot: " + cleanSlot);

                new Dashboard().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Button – Print Receipt: writes a text receipt file to disk
        Button btnPrint = new Button("\uD83D\uDDA8 PRINT RECEIPT");
        btnPrint.setMaxWidth(Double.MAX_VALUE);
        btnPrint.setStyle(
                "-fx-background-color: #262626; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-letter-spacing: 2px;");
        btnPrint.setOnAction(e -> {
            try {
                String safeSlot = (slot != null && !slot.isEmpty()) ? slot.replace("-", "").trim() : "unknown";
                String receiptContent = "========================================\n" +
                        "          VOLT CHARGE RECEIPT           \n" +
                        "========================================\n" +
                        "Session ID: " + safeSlot + "\n" +
                        "User:       " + user + "\n" +
                        "Protocol:   " + protocol + "\n" +
                        "Energy:     " + energy + "\n" +
                        "Duration:   " + duration + "\n" +
                        "----------------------------------------\n" +
                        "TOTAL DUE:  $" + String.format("%.2f", revenue) + "\n" +
                        "========================================\n" +
                        "Thank you for choosing Volt Charge.";
                java.nio.file.Files.writeString(java.nio.file.Paths.get("receipt_" + safeSlot + ".txt"),
                        receiptContent);
                btnPrint.setText("RECEIPT PRINTED \u2713");
                btnPrint.setStyle(
                        "-fx-background-color: #3fff8b; -fx-text-fill: #000000; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 16; -fx-background-radius: 6; -fx-cursor: default; -fx-letter-spacing: 2px;");
                btnPrint.setDisable(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnBox.getChildren().addAll(btnPay, btnPrint);
        totalCard.getChildren().addAll(totTitle, totBox, btnBox);

        // Station location card – static map placeholder with station address
        VBox mapCard = new VBox();
        mapCard.setPrefHeight(200);
        mapCard.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 12; -fx-overflow: hidden;");

        Region mapImg = new Region();
        mapImg.setPrefHeight(140);
        mapImg.setStyle("-fx-background-color: #262626; -fx-background-radius: 12 12 0 0;");

        VBox mapFooter = new VBox(4);
        mapFooter.setStyle("-fx-padding: 16 24;");
        Label locTitle = new Label("\uD83D\uDCCD STATION ID: SF-NOMA-04");
        locTitle.setStyle(
                "-fx-text-fill: #81ecff; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        Label locSub = new Label("201 Mission St, San Francisco, CA");
        locSub.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 12px;");
        mapFooter.getChildren().addAll(locTitle, locSub);

        mapCard.getChildren().addAll(mapImg, mapFooter);

        // Energy performance card – shows charging efficiency progress bar
        VBox energyCard = new VBox(16);
        energyCard.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 12; -fx-padding: 24;");
        HBox enHeader = new HBox();
        Label enTitle = new Label("ENERGY PERFORMANCE");
        enTitle.setStyle(
                "-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        Region enSp = new Region();
        HBox.setHgrow(enSp, Priority.ALWAYS);
        Label enEff = new Label("98.2% Efficiency");
        enEff.setStyle("-fx-text-fill: #3fff8b; -fx-font-size: 10px; -fx-font-weight: bold;");
        enHeader.getChildren().addAll(enTitle, enSp, enEff);

        HBox progBg = new HBox();
        progBg.setPrefHeight(6);
        progBg.setStyle("-fx-background-color: rgba(63,255,139,0.2); -fx-background-radius: 4;");
        Region progFill = new Region();
        progFill.setStyle("-fx-background-color: #3fff8b; -fx-background-radius: 4;");
        progFill.prefWidthProperty().bind(progBg.widthProperty().multiply(0.982));
        progBg.getChildren().add(progFill);

        energyCard.getChildren().addAll(enHeader, progBg);

        rightCol.getChildren().addAll(totalCard, mapCard, energyCard);

        columns.getChildren().addAll(leftCol, rightCol);
        content.getChildren().addAll(headerContent, columns);
        return content;
    }

    // Billing row – a single line item showing label, value and cost
    private VBox createBillingRow(String topTxt, String botTxt, String costStr, boolean border) {
        VBox row = new VBox();
        if (border) {
            row.setStyle(
                    "-fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 16 0; -fx-margin: 0 0 16 0;");
        } else {
            row.setStyle("-fx-padding: 16 0 0 0;");
        }

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.BOTTOM_LEFT);

        VBox lBox = new VBox(4);
        Label t1 = new Label(topTxt.toUpperCase());
        t1.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
        Label t2 = new Label(botTxt);
        t2.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        lBox.getChildren().addAll(t1, t2);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label cost = new Label(costStr);
        cost.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        hBox.getChildren().addAll(lBox, sp, cost);
        row.getChildren().add(hBox);
        return row;
    }

    // Footer – charger and connector info with copyright notice
    private HBox createFooter() {
        HBox footer = new HBox(32);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle(
                "-fx-border-color: rgba(255,255,255,0.05) transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-padding: 32 48 32 48; -fx-margin: 48 0 0 0;");

        HBox infoBox = new HBox(48);

        VBox f1 = new VBox(4);
        Label t1 = new Label("CHARGER");
        t1.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
        Label v1 = new Label("Tesla Supercharger v3");
        v1.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        f1.getChildren().addAll(t1, v1);

        VBox f2 = new VBox(4);
        Label t2 = new Label("CONNECTOR");
        t2.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
        Label v2 = new Label("CCS Type 2");
        v2.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        f2.getChildren().addAll(t2, v2);

        infoBox.getChildren().addAll(f1, f2);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label copy = new Label("\u00A9 2024 Volt Charge Network. All transactions are encrypted.");
        copy.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px;");

        footer.getChildren().addAll(infoBox, sp, copy);
        return footer;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
