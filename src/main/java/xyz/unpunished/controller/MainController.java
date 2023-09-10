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
import xyz.unpunished.util.I18N;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import xyz.unpunished.util.GinCompiler;

@Getter
public class MainController implements Initializable {

    private IniWorker iniWorker;
    private Thread fileThread;
    private Locale previousLocale = Locale.ENGLISH;
    
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
    private HBox grainPathHBox;
    @FXML
    private HBox exportPathHBox;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Button ginButton;
    @FXML
    private CheckBox carbonPattern;
    @FXML
    private HBox carbonPatternHBox;
    @FXML
    private ComboBox<String> enExBox;
    @FXML
    private ComboBox<String> aclDclBox; 
    @FXML
    private TextField carNumberField;
    @FXML
    private ComboBox <Locale> languageBox;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String[] enEx = new String[]{"en", "ex"};
        String[] aclDcl = new String[]{"acl", "dcl"};
        enExBox.setItems(FXCollections.observableArrayList(enEx));
        aclDclBox.setItems(FXCollections.observableArrayList(aclDcl));
        Callback<ListView<Locale>, ListCell<Locale>> factory = lv -> new ListCell<Locale>() {
            @Override
            protected void updateItem(Locale locale, boolean empty) {
                super.updateItem(locale, empty);
                setText(empty ? "" : locale.getDisplayName());
            }
        };
        languageBox.setCellFactory(factory);
        languageBox.setItems(FXCollections.observableArrayList(I18N.getSupportedLocales()));
        languageBox.setButtonCell(factory.call(null));
        iniWorker = new IniWorker("gintool.ini");
        setFieldValuesFromIni();
        languageBox.valueProperty().addListener((ov, t, t1) -> {
            if(!t1.equals(previousLocale)){
                previousLocale = t1;
                iniWorker.setLocale(t1);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setHeaderText(I18N.get("confirmation"));
                alert.setContentText(I18N.get("restart_now"));
                alert.setTitle(I18N.get("tool_name"));
                Optional<ButtonType> bt = alert.showAndWait();
                if(bt.get().equals(ButtonType.OK)){
                    fileThread.interrupt();
                    iniWorker.rewriteIni(iniWorker.getDefaultIni());
                    ProcessBuilder pb = new ProcessBuilder("launchGin.bat");
                    pb = pb.inheritIO();
                    try {
                        pb.start();
                    } catch (IOException ex) {
                        
                    }
                    System.exit(0);
                }
            }
            
        });
        // value fields contain only numbers
        minRPMField.textProperty().addListener((observable, oldValue, newValue) -> {
            try{
                float x = Float.parseFloat(newValue);
            }
            catch(NumberFormatException ex){
                if(!newValue.equals(""))
                    minRPMField.setText(oldValue);
                return;
            }
            String[] newValueSplit = newValue.split("\\.", 2);
            for(int j = 0; j < newValueSplit.length; j ++)
                newValueSplit[j] = newValueSplit[j].matches("\\d*") 
                    ? newValueSplit[j]
                    : newValueSplit[j].replaceAll("[^\\d]", "");
            if(newValueSplit.length > 1)
                minRPMField.setText(newValueSplit[0] + "." 
                        + (newValueSplit[1].length() > 2
                        ? newValueSplit[1].substring(0, 2)
                        : newValueSplit[1]));
            else
                if(!newValueSplit[0].equals(newValue))
                    minRPMField.setText(newValueSplit[0]);
            iniWorker.setMinRPM(Float.parseFloat(minRPMField.getText()));
        });
        minRPMField.focusedProperty().addListener((ov, t, t1) -> {
            if(!t1){
                String[] split = minRPMField.getText().split("\\.", 2);
                if(split.length > 1 && split[1].equals(""))
                    minRPMField.setText(minRPMField.getText() + "00");
            }
            iniWorker.setMinRPM(Float.parseFloat(minRPMField.getText()));
        });
        maxRPMField.textProperty().addListener((observable, oldValue, newValue) -> {
            try{
                float x = Float.parseFloat(newValue);
            }
            catch(NumberFormatException ex){
                if(!newValue.equals(""))
                    maxRPMField.setText(oldValue);
                return;
            }
            String[] newValueSplit = newValue.split("\\.", 2);
            for(int j = 0; j < newValueSplit.length; j ++)
                newValueSplit[j] = newValueSplit[j].matches("\\d*") 
                    ? newValueSplit[j]
                    : newValueSplit[j].replaceAll("[^\\d]", "");
            if(newValueSplit.length > 1)
                maxRPMField.setText(newValueSplit[0] + "." 
                        + (newValueSplit[1].length() > 2
                        ? newValueSplit[1].substring(0, 2)
                        : newValueSplit[1]));
            else
                if(!newValueSplit[0].equals(newValue))
                    maxRPMField.setText(newValueSplit[0]);
            iniWorker.setMaxRPM(Float.parseFloat(maxRPMField.getText()));
        });
        maxRPMField.focusedProperty().addListener((ov, t, t1) -> {
            if(!t1){
                String[] split = maxRPMField.getText().split("\\.", 2);
                if(split.length > 1 && split[1].equals(""))
                    maxRPMField.setText(maxRPMField.getText() + "00");
            }
            iniWorker.setMaxRPM(Float.parseFloat(maxRPMField.getText()));
        });
        minIDXField.textProperty().addListener((ov, t, t1) -> {
            if(!t1.matches("\\d*"))
                minIDXField.setText(t1.replaceAll("[^\\d]", ""));
            iniWorker.setMinIDX(Integer.parseInt(minIDXField.getText()));
        });
        maxIDXField.textProperty().addListener((ov, t, t1) -> {
            if(!t1.matches("\\d*"))
                maxIDXField.setText(t1.replaceAll("[^\\d]", ""));
            iniWorker.setMaxIDX(Integer.parseInt(maxIDXField.getText()));
        });
        carNumberField.textProperty().addListener((ov, t, t1) -> {
            if(!t1.matches("\\d*"))
                carNumberField.setText(t1.replaceAll("[^\\d]", ""));
            if(t1.length() > 3)
                carNumberField.setText(t1.substring(1, 4));
            if(t1.length() < 2)
                carNumberField.setText("0" + t1);
            iniWorker.setCarNumber(Integer.parseInt(carNumberField.getText()));
        });
        grainPathCB.selectedProperty().addListener((observableValue, t, t1) -> {
            grainPathHBox.setDisable(t);
            iniWorker.setGrainPathSel(t1);
        });
        exportPathCB.selectedProperty().addListener((observableValue, t, t1) -> {
            exportPathHBox.setDisable(t);
            iniWorker.setExportPathSel(t1);
        });
        splitPane.lookupAll(".split-pane-divider").stream()
                .forEach(div ->  div.setMouseTransparent(true) );
        carbonPattern.selectedProperty().addListener((ov, t, t1) -> {
            carbonPatternHBox.setDisable(t);
            iniWorker.setCarbonPattern(t1);
        });
        enExBox.getSelectionModel().selectedIndexProperty().addListener((ov, t, t1) -> {
            iniWorker.setEnEx(t1.intValue());
        });
        aclDclBox.getSelectionModel().selectedIndexProperty().addListener((ov, t, t1) -> {
            iniWorker.setAclDcl(t1.intValue());
        });
        decelCB.selectedProperty().addListener((ov, t, t1) -> {
            iniWorker.setDecel(t1);
        });
        wavFileField.textProperty().addListener((ov, t, t1) -> {
            iniWorker.setWavPath(t1);
        });
        grainPathField.textProperty().addListener((ov, t, t1) -> {
            iniWorker.setGrainPath(t1);
        });
        exportPathField.textProperty().addListener((ov, t, t1) -> {
            iniWorker.setExportPath(t1);
        });
        fileThread = new Thread(() -> {
            try {
                while (true){
                    Thread.sleep(10000);
                    iniWorker.rewriteIni(iniWorker.getDefaultIni());
                }
            } catch (InterruptedException ex) {
                
            }
        });
        fileThread.start();
    }

    private void setFieldValuesFromIni(){
        wavFileField.setText(iniWorker.getWavPath());
        minRPMField.setText(String.format(Locale.ENGLISH, "%.2f", iniWorker.getMinRPM()));
        minIDXField.setText(iniWorker.getMinIDX() + "");
        maxRPMField.setText(String.format(Locale.ENGLISH, "%.2f", iniWorker.getMaxRPM()));
        maxIDXField.setText(iniWorker.getMaxIDX() + "");
        exportPathCB.setSelected(iniWorker.isExportPathSel());
        exportPathHBox.setDisable(!iniWorker.isExportPathSel());
        exportPathField.setText(iniWorker.getExportPath());
        grainPathCB.setSelected(iniWorker.isGrainPathSel());
        grainPathHBox.setDisable(!iniWorker.isGrainPathSel());
        grainPathField.setText(iniWorker.getGrainPath());
        carbonPattern.setSelected(iniWorker.isCarbonPattern());
        carbonPatternHBox.setDisable(!iniWorker.isCarbonPattern());
        enExBox.getSelectionModel().select(iniWorker.getEnEx());
        aclDclBox.getSelectionModel().select(iniWorker.getAclDcl());
        carNumberField.setText(String.format("%02d", iniWorker.getCarNumber()));
        decelCB.setSelected(iniWorker.isDecel());
        previousLocale = iniWorker.getLocale();
        languageBox.setValue(previousLocale);
    }

    @FXML
    private void compileGin(){
        if(!checkFields() || !checkOptionalFields()){
                return;
        }
        Thread thread = new Thread(new GinCompiler(
            ginButton,
            wavFileField.getText(),
            grainPathField.getText(),
            exportPathField.getText(),
            createExportName(),
            minIDXField.getText(),
            minRPMField.getText(),
            maxIDXField.getText(),
            maxRPMField.getText(),
            grainPathCB.isSelected(),
            exportPathCB.isSelected(),
            carbonPattern.isSelected(),
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
                        I18N.get("error"),
                        I18N.get("no_fields_empty")));
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
                        I18N.get("error"),
                        I18N.get("no_optional_fields_empty")));
            Platform.runLater(() -> ginButton.setDisable(false));
            return false;
        }
        if(carbonPattern.isSelected() && carNumberField.getText().equals("")
                && aclDclBox.getSelectionModel().getSelectedIndex() < 0
                && enExBox.getSelectionModel().getSelectedIndex() < 0){
            PlatformImpl.runAndWait(() 
                    -> AlertWorker.showAlert(Alert.AlertType.ERROR,
                        I18N.get("error"),
                        I18N.get("no_optional_fields_empty")));
            Platform.runLater(() -> ginButton.setDisable(false));
            return false;
        }
        return true;
    }
    
    private String createExportName(){
        return enExBox.getSelectionModel().getSelectedItem() + "_"
                + aclDclBox.getSelectionModel().getSelectedItem() + "_"
                + carNumberField.getText() + ".gin";
    }

    @FXML
    private void browseWav(){
        wavFileField.setText(browseFileOrDirectory(wavFileField.getText(),
                new String[]{"*.wav"},
                new String[]{I18N.get("waveform")},
                false,
                false));
    }

    @FXML
    private void browseGrainPath(){
        grainPathField.setText(browseFileOrDirectory(grainPathField.getText(),
                new String[]{"*.wav"},
                new String[]{I18N.get("waveform")},
                false,
                true));
    }

    @FXML
    private void browseExportPath(){
        exportPathField.setText(browseFileOrDirectory(exportPathField.getText(),
                new String[]{"*.gin"},
                new String[]{I18N.get("ginfile")},
                true,
                false));
    }
    
    private String browseFileOrDirectory(String initialDirectory, String[] fileTypes, String[] fileTypeNames,
            boolean save, boolean directory){
        File f = new File(initialDirectory);
        if(directory){
            DirectoryChooser dc = new DirectoryChooser();
            if(f.exists()){
                Path p = Paths.get(f.getAbsolutePath());
                dc.setInitialDirectory(new File(p.getParent().toString()));
            }
            else{
                dc.setInitialDirectory(new File(System.getProperty("user.dir")));
            }
            try{
                f = dc.showDialog((Stage) minIDXField.getScene().getWindow());
                if((f.exists() && f.isDirectory()))
                    return f.getAbsolutePath();
                else
                    return initialDirectory;
            }
            catch (Exception e){
                return initialDirectory;
            }
        }
        else{
            FileChooser fc = new FileChooser();
            for(int i = 0; i < fileTypes.length; i ++){
                FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                    fileTypeNames[i],
                    fileTypes[i]);
                fc.getExtensionFilters().add(filter);
            }
            if(fileTypes.length > 1){
                FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                        "All Supported Audio Files",
                        fileTypes
                );
                fc.getExtensionFilters().add(filter);
            }
            if(f.exists()){
                Path p = Paths.get(f.getAbsolutePath());
                fc.setInitialDirectory(new File(p.getParent().toString()));
            }
            else{
                fc.setInitialDirectory(new File(System.getProperty("user.dir")));
            }
            try{
                if(save)
                    f = fc.showSaveDialog((Stage) minIDXField.getScene().getWindow());
                else
                    f = fc.showOpenDialog((Stage) minIDXField.getScene().getWindow());
                if((f.exists() && f.isFile()) || save)
                    return f.getAbsolutePath();
                else
                    return initialDirectory;
            }
            catch (Exception e){
                return initialDirectory;
            }
        }
    }
    
    @FXML
    private void launchTmxTool(){
        if (!iniWorker.isFirstLaunch()){
            ProcessBuilder builder = new ProcessBuilder("launchTMX.bat");
            builder = builder.inheritIO();
            builder = builder.directory(new File(System.getProperty("user.dir")));
            try{
                builder.start();
            }
            catch(IOException ex){
                ex.printStackTrace();
                AlertWorker.showAlert(
                        Alert.AlertType.ERROR,
                        I18N.get("error"),
                        I18N.get("tmxtool_not_found"));
                iniWorker.setFirstLaunch(true);
            }
        }
        else{
            AlertWorker.showAlert(
                        Alert.AlertType.INFORMATION,
                        I18N.get("information"),
                        I18N.get("tmxtool_warning"));
            iniWorker.setFirstLaunch(false);
        }
    }
}
