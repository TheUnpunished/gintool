package xyz.unpunished;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import xyz.unpunished.controller.MainController;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/static/main.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        Scene scene = new Scene(root);
        stage.setResizable(false);
        stage.setOnCloseRequest((t) -> {
            controller.getFileThread().interrupt();
            controller.getIniWorker().rewriteIni(controller.getIniWorker().getDefaultIni());
            System.exit(0);
        });
        stage.setTitle("gintool");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon.png")));
        stage.setScene(scene);
        stage.show();
    }
}
