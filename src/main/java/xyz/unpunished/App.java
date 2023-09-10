package xyz.unpunished;

import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import xyz.unpunished.controller.MainController;
import xyz.unpunished.util.I18N;
import xyz.unpunished.util.IniWorker;

public class App extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        IniWorker iniWorker = new IniWorker("gintool.ini");
        ResourceBundle bundle = ResourceBundle.getBundle("i18n/messages", iniWorker.getLocale());
        I18N.setLocale(iniWorker.getLocale());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/static/main.fxml"), bundle);
        Parent root = loader.load();
        MainController controller = loader.getController();
        Scene scene = new Scene(root);
        stage.setResizable(false);
        stage.setOnCloseRequest((t) -> {
            controller.getFileThread().interrupt();
            controller.getIniWorker().rewriteIni(controller.getIniWorker().getDefaultIni());
            System.exit(0);
        });
        stage.setTitle(I18N.get("tool_name"));
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/icon.png")));
        stage.setScene(scene);
        stage.show();
    }
}
