package xyz.unpunished.util;

import javafx.scene.control.Alert;
import lombok.Getter;

import java.io.*;

@Getter
public class IniWorker {

    private final File defaultIni;
    private String wavPath;
    private String grainPath;
    private String exportPath;

    public void setWavPath(String wavPath) {
        this.wavPath = wavPath;
        rewriteIni(defaultIni);
    }

    public void setGrainPath(String grainPath){
        this.grainPath = grainPath;
        rewriteIni(defaultIni);
    }

    public void setExportPath(String exportPath){
        this.exportPath = exportPath;
        rewriteIni(defaultIni);
    }

    public IniWorker(String iniName){
        defaultIni = new File(iniName);
        wavPath = "";
        grainPath = "";
        exportPath = "";
        if(defaultIni.exists()){
            readIniFile(defaultIni);
        }
        else {
            createIniFile(defaultIni);
        }
    }

    private void createIniFile(File ini){
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(ini));
            try{
                bw.write("wavPath = "); bw.newLine();
                bw.write("grainPath = "); bw.newLine();
                bw.write("exportPath = "); bw.newLine();
            }
            finally {
                bw.flush();
                bw.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
            AlertWorker.showAlert(Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "gintool couldn't generate .ini file");
        }
    }

    private void readIniFile(File ini){
       try{
           BufferedReader reader = new BufferedReader((new FileReader(ini)));
           try{
               wavPath = reader.readLine().split("\\s+=\\s+", 2)[1];
               grainPath = reader.readLine().split("\\s+=\\s+", 2)[1];
               exportPath = reader.readLine().split("\\s+=\\s+", 2)[1];
           }
           finally {
               reader.close();
           }
       }
       catch (IOException e){
           e.printStackTrace();
           AlertWorker.showAlert(Alert.AlertType.ERROR,
                   "gintool",
                   "Error",
                   "gintool couldn't read .ini file");
           createIniFile(defaultIni);
       }
    }

    private void rewriteIni(File ini){
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(ini));
            try{
                bw.write("wavPath = " + wavPath); bw.newLine();
                bw.write("grainPath = " + grainPath); bw.newLine();
                bw.write("exportPath = " + exportPath); bw.newLine();

            }
            finally {
                bw.flush();
                bw.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
            AlertWorker.showAlert(Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "gintool failed to write to .ini file");
        }
    }


}
