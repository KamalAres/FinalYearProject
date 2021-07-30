package finalyear.project.cse.comments;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.common.eventbus.EventBus;

import finalyear.project.cse.auth.OAuth2Manager;
import finalyear.project.cse.database.CommentDatabase;
import finalyear.project.cse.util.IpApiProvider;
import finalyear.project.cse.util.Location;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Scanner;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.*;

/**
 * Application Window
 *
 */
class LoginFrame extends JFrame implements ActionListener {

    Container container = getContentPane();
    JLabel userLabel = new JLabel("USERNAME");
    JLabel passwordLabel = new JLabel("PASSWORD");
    JTextField userTextField = new JTextField();
    JPasswordField passwordField = new JPasswordField();
    JButton loginButton = new JButton("LOGIN");
    JButton resetButton = new JButton("RESET");
    JCheckBox showPassword = new JCheckBox("Show Password");
    
    boolean flag = false;


    LoginFrame() {
        setLayoutManager();
        setLocationAndSize();
        addComponentsToContainer();
        addActionEvent();

    }

    public void setLayoutManager() {
        container.setLayout(null);
    }

    public void setLocationAndSize() {
        userLabel.setBounds(50, 150, 100, 30);
        passwordLabel.setBounds(50, 220, 100, 30);
        userTextField.setBounds(150, 150, 150, 30);
        passwordField.setBounds(150, 220, 150, 30);
        showPassword.setBounds(150, 250, 150, 30);
        loginButton.setBounds(50, 300, 100, 30);
        resetButton.setBounds(200, 300, 100, 30);


    }

    public void addComponentsToContainer() {
        container.add(userLabel);
        container.add(passwordLabel);
        container.add(userTextField);
        container.add(passwordField);
        container.add(showPassword);
        container.add(loginButton);
        container.add(resetButton);
    }

    public void addActionEvent() {
        loginButton.addActionListener(this);
        resetButton.addActionListener(this);
        showPassword.addActionListener(this);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        //Coding Part of LOGIN button
        if (e.getSource() == loginButton) {
            String userText;
            String pwdText;
            userText = userTextField.getText();
            pwdText = passwordField.getText();
            if (userText.equalsIgnoreCase("kamal")||userText.equalsIgnoreCase("bharath")||userText.equalsIgnoreCase("jayaram") && pwdText.equalsIgnoreCase("admin")) {
            	flag = true;
                JOptionPane.showMessageDialog(this, "Login Successful");
                
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password");
            }

        }
        //Coding Part of RESET button
        if (e.getSource() == resetButton) {
            userTextField.setText("");
            passwordField.setText("");
        }
       //Coding Part of showPassword JCheckBox
        if (e.getSource() == showPassword) {
            if (showPassword.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('*');
            }


        }
    }

}
public class CommentSuite extends Application{

    private static final Logger logger = LogManager.getLogger();

    private static final ConfigFile<ConfigData> config = new ConfigFile<>("commentsuite.json", new ConfigData());
    private static final EventBus eventBus = new EventBus();
    private static final Location<IpApiProvider, IpApiProvider.Location> location = new Location<>(new IpApiProvider(), IpApiProvider.Location.class);

    private static CommentDatabase database;
    private static YouTube youTube;
    private static String youTubeApiKey;
    private static OAuth2Manager oauth2Manager;
    
    
    public static boolean login() {
    	LoginFrame frame = new LoginFrame();
        frame.setTitle("Login Form");
        frame.setVisible(true);
        frame.setBounds(10, 10, 370, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        while(true) {
        	System.out.print(frame.flag);
        	if(frame.flag==true) {
        	frame.dispose();
        	break;
        }
        }
        return frame.flag;
    }

    public static void main(String[] args) {
    	
    	
    	
	    	if(login()) {
	        logger.debug("Starting Application");
	
	        /*
	         * Setting this system property is a fix for the JavaFX Webview behaving improperly.
	         * The 'Tap Yes' authentication when signing in from {@link mattw.youtube.commentsuite.fxml.Settings)
	         * would do nothing and the icon would flicker when not set, requiring the user to use SMS
	         * authentication instead.
	         */
	        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	
	        /*
	         * https://stackoverflow.com/a/24419312/2650847
	         */
	        System.setProperty("prism.lcdtext", "false");
	
	        launch(args);
	    	}
    }

    public void start(final Stage stage) {
        try {
            youTube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
                    .setApplicationName("Final Year Project")
                    .build();
            youTubeApiKey = config.getDataObject().getYoutubeApiKey();
            database = new CommentDatabase("commentsuite.sqlite3");
            oauth2Manager = new OAuth2Manager();

            final Parent parent = FXMLLoader.load(getClass().getResource("/io/mattw/youtube/commentsuite/fxml/Main.fxml"));

            final Scene scene = new Scene(parent);
            scene.getStylesheets().add("SuiteStyles.css");
            stage.setTitle("ED-COUNSELOR");
            stage.setScene(scene);
            stage.getIcons().add(ImageLoader.YCS_ICON.getImage());
            stage.setOnCloseRequest(we -> {
                try {
                    database.commit();

                    logger.debug("Closing - Closing DB Connection");
                    database.close();
                } catch (SQLException | IOException e) {
                    logger.error(e);
                }
                logger.debug("Closing - Exiting Application");
                Platform.exit();
                System.exit(0);
            });
            stage.show();
        } catch (IOException | SQLException | GeneralSecurityException e) {
            e.printStackTrace();
            logger.error(e);
            Platform.exit();
            System.exit(0);
        }
    }

    public static void postEvent(Object event) {
        eventBus.post(event);
    }

    public static ConfigFile<ConfigData> getConfig() {
        return config;
    }

    public static EventBus getEventBus() {
        return eventBus;
    }

    public static Location<IpApiProvider, IpApiProvider.Location> getLocation() {
        return location;
    }

    public static OAuth2Manager getOauth2Manager() {
        return oauth2Manager;
    }

    public static CommentDatabase getDatabase() {
        return database;
    }

    public static YouTube getYouTube() {
        return youTube;
    }

    public static String getYouTubeApiKey() {
        return getConfig().getDataObject().getApiKeyOrDefault();
    }

}
