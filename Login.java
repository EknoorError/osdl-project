import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.layout.BackgroundSize;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Login extends Application {
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0e0e0e;");
        
        // Background Image
        try {
            java.net.URL imgUrl = getClass().getResource("login-bg.png");
            if (imgUrl == null) {
                imgUrl = new java.io.File(System.getProperty("user.dir") + "/login-bg.png").toURI().toURL();
            }
            Image bgImg = new Image(imgUrl.toExternalForm());
            BackgroundImage bImg = new BackgroundImage(bgImg,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, false, false));
            root.setBackground(new Background(bImg));
        } catch (Exception e) { e.printStackTrace(); }
        
        // Overlay to darken background
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // Main Panel Container
        HBox panel = new HBox();
        panel.setMaxSize(900, 500);
        panel.setStyle("-fx-background-color: rgba(30, 30, 30, 0.85); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");
        
        // Left branding panel – app tagline and link to create a new admin account
        VBox leftSide = new VBox(20);
        leftSide.setPrefWidth(400);
        leftSide.setPadding(new Insets(40));
        leftSide.setStyle("-fx-background-color: rgba(20, 20, 20, 0.5); -fx-background-radius: 12 0 0 12; -fx-border-color: transparent rgba(255,255,255,0.05) transparent transparent; -fx-border-width: 1;");
        leftSide.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label("\u26A1");
        icon.setStyle("-fx-background-color: rgba(0, 229, 255, 0.1); -fx-text-fill: #00e5ff; -fx-font-size: 32px; -fx-padding: 10 16; -fx-background-radius: 8;");
        
        Label title = new Label("EMPOWERING A\nCLEANER JOURNEY");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        
        Label desc = new Label("A state-of-the-art Smart EV Charging\nManagement System designed for\nefficiency and control.");
        desc.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 14px; -fx-line-spacing: 4px;");
        
        Region lSpacer = new Region();
        VBox.setVgrow(lSpacer, Priority.ALWAYS);
        
        // Signup link – navigates to the registration screen
        HBox footerBox = new HBox(5);
        Label newOp = new Label("New operator?");
        newOp.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 12px;");
        Hyperlink createAcc = new Hyperlink("Create Admin Account");
        createAcc.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        createAcc.setOnAction(e -> {
            try { new Signup().start(stage); } catch(Exception ex){ ex.printStackTrace(); }
        });
        footerBox.getChildren().addAll(newOp, createAcc);
        
        leftSide.getChildren().addAll(icon, title, desc, lSpacer, footerBox);
        
        // Right form panel – username, password inputs, Login button and security notice
        VBox rightSide = new VBox(24);
        rightSide.setPrefWidth(500);
        rightSide.setPadding(new Insets(60, 60, 60, 60));
        rightSide.setAlignment(Pos.CENTER_LEFT);
        
        VBox headerBox = new VBox(4);
        Label rTitle = new Label("VoltCharge");
        rTitle.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label rSub = new Label("PRECISION LAB V2.1");
        rSub.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-family: monospace;");
        headerBox.getChildren().addAll(rTitle, rSub);
        
        // Username field
        TextField txtUser = new TextField();
        txtUser.setPromptText("Username (admin)");
        txtUser.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.3); -fx-padding: 12; -fx-background-radius: 6; -fx-border-color: transparent; -fx-font-size: 14px;");
        
        // Password field
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Password");
        txtPass.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.3); -fx-padding: 12; -fx-background-radius: 6; -fx-border-color: transparent; -fx-font-size: 14px;");
        
        // Button – Login: validates credentials against the DB and opens the Dashboard
        Button btnLogin = new Button("LOGIN \u2192");
        btnLogin.setStyle("-fx-background-color: linear-gradient(to right, #00e5ff, #008b9c); -fx-text-fill: #002b30; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setOnAction(e -> handleLogin(txtUser.getText(), txtPass.getText()));
        
        Label lblNotice = new Label("\u2713 Authorized access only. All connection attempts\nare logged via V-Node protocol for security auditing.");
        lblNotice.setStyle("-fx-text-fill: rgba(173, 170, 170, 0.4); -fx-font-size: 10px; -fx-font-family: monospace;");
        
        rightSide.getChildren().addAll(headerBox, txtUser, txtPass, btnLogin, lblNotice);
        
        panel.getChildren().addAll(leftSide, rightSide);
        
        root.getChildren().addAll(overlay, panel);
        
        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("Volt Charge - Secure Access");
        stage.setScene(scene);
        stage.show();
    }
    
    // handleLogin – checks username/password in DB; on success opens Dashboard, else shows error
    private void handleLogin(String username, String password) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, fullname TEXT, employeeid TEXT, location TEXT)");
            
            ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) AS total FROM users");
            if (rsCount.next() && rsCount.getInt("total") == 0) {
                stmt.execute("INSERT INTO users (username, password, fullname, employeeid, location) VALUES ('admin', 'admin', 'System Admin', 'V-88', 'Global')");
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username=? AND password=?")) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    new Dashboard().start(stage);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Access Denied");
                    alert.setHeaderText("Invalid Credentials");
                    alert.setContentText("The username or password provided is incorrect.");
                    alert.showAndWait();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
