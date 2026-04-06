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
import java.sql.Statement;

public class Signup extends Application {
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0e0e0e;");
        
        try {
            java.net.URL imgUrl = getClass().getResource("signup-bg.png");
            if (imgUrl == null) {
                imgUrl = new java.io.File(System.getProperty("user.dir") + "/signup-bg.png").toURI().toURL();
            }
            Image bgImg = new Image(imgUrl.toExternalForm());
            BackgroundImage bImg = new BackgroundImage(bgImg,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, false, false));
            root.setBackground(new Background(bImg));
        } catch (Exception e) { e.printStackTrace(); }
        
        Region overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        HBox panel = new HBox();
        panel.setMaxSize(900, 500);
        panel.setStyle("-fx-background-color: rgba(30, 30, 30, 0.85); -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12;");
        
        // Left Side
        VBox leftSide = new VBox(20);
        leftSide.setPrefWidth(350);
        leftSide.setPadding(new Insets(40));
        leftSide.setStyle("-fx-background-color: rgba(20, 20, 20, 0.5); -fx-background-radius: 12 0 0 12; -fx-border-color: transparent rgba(255,255,255,0.05) transparent transparent; -fx-border-width: 1;");
        leftSide.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label("\u26A1");
        icon.setStyle("-fx-background-color: rgba(63, 255, 139, 0.1); -fx-text-fill: #3fff8b; -fx-font-size: 32px; -fx-padding: 10 16; -fx-background-radius: 8;");
        
        Label title = new Label("JOIN THE\nPRECISION NETWORK");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label desc = new Label("Register as a new station\nadministrator to manage the\nfuture of urban electric mobility.");
        desc.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 14px; -fx-line-spacing: 4px;");
        
        Region lSpacer = new Region();
        VBox.setVgrow(lSpacer, Priority.ALWAYS);
        
        Hyperlink createAcc = new Hyperlink("\u2190 Return to Login");
        createAcc.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        createAcc.setOnAction(e -> {
            try { new Login().start(stage); } catch(Exception ex){ ex.printStackTrace(); }
        });
        
        leftSide.getChildren().addAll(icon, title, desc, lSpacer, createAcc);
        
        // Right Form Side
        VBox rightSide = new VBox(24);
        rightSide.setPrefWidth(550);
        rightSide.setPadding(new Insets(40, 40, 40, 40));
        rightSide.setAlignment(Pos.CENTER_LEFT);
        
        VBox headerBox = new VBox(4);
        headerBox.setAlignment(Pos.CENTER_RIGHT);
        headerBox.setMaxWidth(Double.MAX_VALUE);
        Label rTitle = new Label("VoltCharge");
        rTitle.setStyle("-fx-text-fill: #00e5ff; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label rSub = new Label("PRECISION LAB V2.1");
        rSub.setStyle("-fx-text-fill: #adaaaa; -fx-font-size: 10px; -fx-font-family: monospace;");
        headerBox.getChildren().addAll(rTitle, rSub);
        
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        
        TextField txtName = createField("Full Name", "John Doe");
        TextField txtEmpId = createField("Employee ID", "V-88290");
        grid.add(createLabeledField("Full Name", txtName), 0, 0);
        grid.add(createLabeledField("Employee ID", txtEmpId), 1, 0);
        
        TextField txtLocation = createField("Station Location", "Select Managed Hub");
        txtLocation.setPrefWidth(400); 
        grid.add(createLabeledField("Station Location", txtLocation), 0, 1, 2, 1);
        
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("••••••••••••");
        txtPass.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.3); -fx-padding: 12; -fx-background-radius: 6; -fx-border-color: transparent; -fx-font-size: 14px;");
        txtPass.setPrefWidth(400);
        grid.add(createLabeledField("Set Password", txtPass), 0, 2, 2, 1);
        
        Button btnSignup = new Button("REGISTER ACCOUNT \u2192");
        btnSignup.setStyle("-fx-background-color: linear-gradient(to right, #00e5ff, #3b82f6); -fx-text-fill: #002b30; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSignup.setMaxWidth(Double.MAX_VALUE);
        btnSignup.setOnAction(e -> handleSignup(txtName.getText(), txtEmpId.getText(), txtLocation.getText(), txtPass.getText()));
        
        Label lblNotice = new Label("By registering, you agree to the VoltCharge Precision\nSystems Protocol and Secure Network Access Policy.");
        lblNotice.setStyle("-fx-text-fill: rgba(173, 170, 170, 0.4); -fx-font-size: 10px; -fx-alignment: center;");
        lblNotice.setAlignment(Pos.CENTER);
        lblNotice.setMaxWidth(Double.MAX_VALUE);
        
        rightSide.getChildren().addAll(headerBox, grid, btnSignup, lblNotice);
        
        panel.getChildren().addAll(leftSide, rightSide);
        root.getChildren().addAll(overlay, panel);
        
        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("Volt Charge - Registration");
        stage.setScene(scene);
        stage.show();
    }
    
    private TextField createField(String placeholder, String defaultVal) {
        TextField f = new TextField();
        f.setPromptText(defaultVal);
        f.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.3); -fx-padding: 12; -fx-background-radius: 6; -fx-border-color: transparent; -fx-font-size: 14px;");
        return f;
    }
    
    private VBox createLabeledField(String label, TextField field) {
        VBox b = new VBox(6);
        Label l = new Label(label.toUpperCase());
        l.setStyle("-fx-text-fill: #849396; -fx-font-size: 10px; -fx-font-weight: bold; -fx-font-family: monospace;");
        b.getChildren().addAll(l, field);
        return b;
    }
    
    private void handleSignup(String name, String empId, String loc, String pass) {
        if(empId.isEmpty() || pass.isEmpty()) return;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:sessions.db");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, fullname TEXT, employeeid TEXT, location TEXT)");
            
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password, fullname, employeeid, location) VALUES (?, ?, ?, ?, ?)")) {
                pstmt.setString(1, empId);
                pstmt.setString(2, pass);
                pstmt.setString(3, name);
                pstmt.setString(4, empId);
                pstmt.setString(5, loc);
                pstmt.executeUpdate();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Account Registered");
                alert.setContentText("You can now login with your Employee ID.");
                alert.showAndWait();
                
                try {
                    new Login().start(stage);
                } catch(Exception e){}
            }
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Registration Failed");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }
}
