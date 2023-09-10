package xyz.unpunished.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertWorker {

    private int grainCount;
    private int currentGrain;
    private static Alert alert;

    public AlertWorker(int grainCount){
        this.grainCount = grainCount;
        this.currentGrain = 0;
        alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(I18N.get("tool_name"));
        alert.setHeaderText(I18N.get("encoding_grains"));
        alert.setResult(ButtonType.OK);
    }

    public void show(){
        alert.show();
    }

    public void close(){
        alert.close();
    }

    public void incrementGrain(){
        alert.setContentText(Integer.toString(1 + currentGrain++)
                + "/"
                + Integer.toString(grainCount));
    }
    
    public Alert getAlert(){
        return AlertWorker.alert;
    }
    
    public static void showAlert(Alert.AlertType type, String headerText, String mainText){
        Alert alertToShow = new Alert(type);
        alertToShow.setTitle(I18N.get("tool_name"));
        alertToShow.setHeaderText(headerText);
        alertToShow.setContentText(mainText);
        alertToShow.showAndWait();
    }
    
}
