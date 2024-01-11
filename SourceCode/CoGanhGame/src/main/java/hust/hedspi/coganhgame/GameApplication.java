package hust.hedspi.coganhgame;

import hust.hedspi.coganhgame.Utilities.AdaptiveUtilities;
import hust.hedspi.coganhgame.Utilities.ViewUtilities;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class GameApplication extends Application {
    @Override
    public void start(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenHeight = screenBounds.getHeight();
        AdaptiveUtilities.setProperties(screenHeight);
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(GameApplication.class.getResource("View/menu-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setOnCloseRequest(e -> {
                e.consume();
                boolean confirm = ViewUtilities.showConfirm("Exit", "Are you sure you want to exit?");
                if (confirm) {
                    stage.close();
                }
            });

            // set the scene size to match the size of the root layout.
            stage.setResizable(false);
            stage.setTitle("Co Ganh Game");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e){
            ViewUtilities.showAlert("Error", "Error loading menu view", e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch();
    }
}