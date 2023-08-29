package xyz.unpunished.controller;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import xyz.unpunished.util.AlertWorker;
import xyz.unpunished.util.IniWorker;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import xyz.unpunished.util.GinCompiler;

public class mainController implements Initializable {

    private IniWorker iniWorker;
    private boolean wipedToInt = false;
    private boolean wipedToIntTwice = false;
    

    @FXML
    private TextField wavFileField;
    @FXML
    private TextField minIDXField;
    @FXML
    private TextField minRPMField;
    @FXML
    private TextField maxIDXField;
    @FXML
    private TextField maxRPMField;
    @FXML
    private TextField grainPathField;
    @FXML
    private TextField exportPathField;
    @FXML
    private CheckBox grainPathCB;
    @FXML
    private CheckBox exportPathCB;
    @FXML
    private CheckBox decelCB;
    @FXML
    private Button grainPathButton;
    @FXML
    private Button exportPathButton;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Button ginButton;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // value fields contain only numbers
        TextField[] fields = {minIDXField, minRPMField, maxIDXField, maxRPMField};
        for (TextField tf: fields){
            tf.textProperty().addListener((observable, oldValue, newValue) -> {
                try{
                    float x = Float.parseFloat(newValue);
                }
                catch(NumberFormatException ex){
                    if(!newValue.equals(""))
                        tf.setText(oldValue);
                    return;
                }
                String[] newValueSplit = newValue.split("\\.", 2);
                for(int i = 0; i < newValueSplit.length; i ++)
                    newValueSplit[i] = newValueSplit[i].matches("\\d*") 
                        ? newValueSplit[i]
                        : newValueSplit[i].replaceAll("[^\\d]", "");
                if(newValueSplit.length > 1)
                    tf.setText(newValueSplit[0] + "." 
                            + (newValueSplit[1].length() > 2
                            ? newValueSplit[1].substring(0, 2)
                            : newValueSplit[1]));
                else
                    if(!newValueSplit[0].equals(newValue))
                        tf.setText(newValueSplit[0]);
            });
            tf.focusedProperty().addListener((ov, t, t1) -> {
                if(!t1){
                    String[] split = tf.getText().split("\\.", 2);
                    if(split.length > 1 && split[1].equals(""))
                        tf.setText(tf.getText() + "00");
                }
            });
        }
        // ini file
        iniWorker = new IniWorker("gintool.ini");
        setFieldValuesFromIni();
        // checkbox listeners
        grainPathCB.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            grainPathField.setDisable(!t1);
            grainPathButton.setDisable(!t1);
        });
        exportPathCB.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            exportPathField.setDisable(!t1);
            exportPathButton.setDisable(!t1);
        });
        splitPane.lookupAll(".split-pane-divider").stream()
                .forEach(div ->  div.setMouseTransparent(true) );
    }

    private void setFieldValuesFromIni(){
        wavFileField.setText(iniWorker.getWavPath());
        exportPathField.setText(iniWorker.getExportPath());
        grainPathField.setText(iniWorker.getGrainPath());
    }

    @FXML
    private void compileGin(){
        if(!checkFields() || !checkOptionalFields()){
                return;
        }
        Thread thread = new Thread(new GinCompiler(
            ginButton,
            wavFileField.getText(),
            grainPathCB.isSelected() ? grainPathField.getText() : "",
            exportPathCB.isSelected() ? exportPathField.getText() : "",
            minIDXField.getText(),
            minRPMField.getText(),
            maxIDXField.getText(),
            maxRPMField.getText(),
            decelCB.isSelected()));
        thread.start();
    }

    

    private boolean checkFields(){
        if(wavFileField.getText().equals("")
        || minRPMField.getText().equals("")
        || minIDXField.getText().equals("")
        || maxRPMField.getText().equals("")
        || maxIDXField.getText().equals("")){
            PlatformImpl.runAndWait(() 
                    -> AlertWorker.showAlert(Alert.AlertType.ERROR,
                        "gintool", "Error",
                        "None of the main fields should be empty"));
            Platform.runLater(() -> ginButton.setDisable(false));
            return false;
        }
        return true;
    }

    private boolean checkOptionalFields(){
        if((exportPathCB.isSelected() && exportPathField.getText().equals(""))
        || (grainPathCB.isSelected() && grainPathField.getText().equals(""))){
            PlatformImpl.runAndWait(() 
                    -> AlertWorker.showAlert(Alert.AlertType.ERROR,
                        "gintool", "Error",
                        "None of the checked optional fields should be empty"));
            Platform.runLater(() -> ginButton.setDisable(false));
            return false;
        }
        return true;
    }

    @FXML
    private void browseWav(){
        File f = new File(wavFileField.getText());
        FileChooser dc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                "Waveform Audio File",
                "*.wav");
        dc.getExtensionFilters().add(filter);
        if(f.exists())
            dc.setInitialDirectory(new File(f.getParent()));
        else
            dc.setInitialDirectory(new File(System.getProperty("user.dir")));
        try{
            f = dc.showOpenDialog((Stage) wavFileField.getScene().getWindow());
            if(f !=null && f.exists() && f.isFile()){
                String wavPath = f.getAbsolutePath();
                wavFileField.setText(wavPath);
                iniWorker.setWavPath(wavPath);
            }
        }
        catch (Exception e){
            e.printStackTrace();
            AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "Selected file doesn't exist"
            );
        }
    }

    @FXML
    private void browseGrainPath(){
        File f = new File(grainPathField.getText());
        DirectoryChooser dc = new DirectoryChooser();
        if(f.exists())
            dc.setInitialDirectory(new File(f.getParent()));
        else
            dc.setInitialDirectory(new File(System.getProperty("user.dir")));
        try{
            f = dc.showDialog(grainPathField.getScene().getWindow());
            if(f !=null && f.exists() && f.isDirectory()){
                String grainPath = f.getAbsolutePath();
                grainPathField.setText(grainPath);
                iniWorker.setGrainPath(grainPath);
            }
        }
        catch (Exception e){
            e.printStackTrace();
            AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "Selected directory doesn't exist"
            );
        }
    }

    @FXML
    private void browseExportPath(){
        File f = new File(exportPathField.getText());
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                "GIN Audio File",
                "*.gin");
        fc.getExtensionFilters().add(filter);
        if(f.exists())
            fc.setInitialDirectory(new File(f.getParent()));
        else
            fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        try{
            f = fc.showSaveDialog(exportPathField.getScene().getWindow());
            if(f != null){
                String exportPath = f.getAbsolutePath();
                iniWorker.setExportPath(exportPath);
                exportPathField.setText(exportPath);
            }
        }
        catch (Exception e){
            e.printStackTrace();
            AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "Selected directory doesn't exist"
            );
        }
    }
}
