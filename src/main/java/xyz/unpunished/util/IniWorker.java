package xyz.unpunished.util;

import javafx.scene.control.Alert;
import lombok.Getter;

import java.io.*;
import lombok.Setter;

@Getter
@Setter
public class IniWorker {

    private final File defaultIni;
    private String wavPath;
    private String grainPath;
    private String exportPath;
    private int minIDX;
    private float minRPM;
    private int maxIDX;
    private float maxRPM;
    private boolean grainPathSel;
    private boolean exportPathSel;
    private boolean decel;
    private boolean carbonPattern;
    private int carNumber;
    private int enEx;
    private int aclDcl;
    private String javaPath;

    public IniWorker(String iniName){
        defaultIni = new File(iniName);
        initIni();
        if(defaultIni.exists()){
            readIniFile(defaultIni);
        }
        else {
            rewriteIni(defaultIni);
        }
    }

    private void initIni(){
        wavPath = "";
        grainPath = "";
        exportPath = "";
        minIDX = 0;
        minRPM = (float) 0.0;
        maxIDX = 0;
        maxRPM = (float) 0.0;
        grainPathSel = false;
        exportPathSel = false;
        decel = false;
        carbonPattern = false;
        carNumber = 0;
        enEx = 0;
        aclDcl = 0;
        javaPath = "";
    }

    private void readIniFile(File ini){
        try{
            BufferedReader reader = new BufferedReader((new FileReader(ini)));
            wavPath = readLine(reader);
            grainPath = readLine(reader);
            exportPath = readLine(reader);
            minIDX = Integer.parseInt(readLine(reader));
            minIDX = minIDX < 0 ? 0 : minIDX;
            minRPM = Float.parseFloat(readLine(reader));
            minRPM = minRPM < 0 ? 0 : minRPM;
            maxIDX = Integer.parseInt(readLine(reader));
            maxIDX = maxIDX < 0 ? 0 : maxIDX;
            maxRPM = Float.parseFloat(readLine(reader));
            maxRPM = maxRPM < 0 ? 0 : maxRPM;
            grainPathSel = Boolean.parseBoolean(readLine(reader));
            exportPathSel = Boolean.parseBoolean(readLine(reader));
            decel = Boolean.parseBoolean(readLine(reader));
            carbonPattern = Boolean.parseBoolean(readLine(reader));
            carNumber = Integer.parseInt(readLine(reader));
            carNumber = carNumber < 0 ? 0 : carNumber;
            carNumber %= 1000;
            enEx = Integer.parseInt(readLine(reader));
            enEx = enEx < 0 || enEx > 1 ? 0 : enEx;
            aclDcl = Integer.parseInt(readLine(reader));
            aclDcl = aclDcl < 0 || aclDcl > 1 ? 0 : aclDcl;
            javaPath = readLine(reader);
            reader.close();
        }
        catch (IOException | NullPointerException | NumberFormatException e){
            e.printStackTrace();
            initIni();
            AlertWorker.showAlert(Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "gintool couldn't read .ini file");
            rewriteIni(defaultIni);
        }
    }
    
    private String readLine(BufferedReader reader) throws IOException{
        return reader.readLine().split("\\s+=\\s+", 2)[1];
    }

    public void rewriteIni(File ini){
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(ini));
            bw.write("wavPath = " + wavPath); bw.newLine();
            bw.write("grainPath = " + grainPath); bw.newLine();
            bw.write("exportPath = " + exportPath); bw.newLine();
            bw.write("minIDX = " + minIDX); bw.newLine();
            bw.write("minRPM = " + String.format("%.2f", minRPM)); bw.newLine();
            bw.write("maxIDX = " + maxIDX); bw.newLine();
            bw.write("maxRPM = " + String.format("%.2f", maxRPM)); bw.newLine();
            bw.write("grainPathSel = " + grainPathSel); bw.newLine();
            bw.write("exportPathSel = " + exportPathSel); bw.newLine();
            bw.write("decel = " + decel); bw.newLine();
            bw.write("carbonPattern = " + carbonPattern); bw.newLine();
            bw.write("carNumber = " + carNumber); bw.newLine();
            bw.write("enEx = " + enEx); bw.newLine();
            bw.write("aclDcl = " + aclDcl); bw.newLine();
            bw.write("javaPath = " + javaPath); bw.newLine();
            bw.flush();
            bw.close();
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
