package xyz.unpunished.util;

import com.sun.javafx.application.PlatformImpl;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import org.apache.commons.io.FilenameUtils;


public class GinCompiler implements Runnable{
    
    private Button ginButton;
    private AlertWorker alertWorker;
    private String wavPath, grainPath, exportPath;
    private String minIdx, minRPM, maxIdx, maxRPM;
    private boolean decel;
    
    private Path ginPath;
    private Path encoderPath;
    
    public GinCompiler(Button ginButton, String wavPath,
            String grainPath, String exportPath,
            String minIdx, String minRPM, String maxIdx, String maxRPM,
            boolean decel){
        this.ginButton = ginButton;
        this.wavPath = wavPath;
        this.grainPath = grainPath;
        this.exportPath = exportPath;
        this.minIdx = minIdx;
        this.minRPM = minRPM;
        this.maxIdx = maxIdx;
        this.maxRPM = maxRPM;
        this.decel = decel;
    }


    @Override
    public void run() {
        Platform.runLater(() -> ginButton.setDisable(true));
        if(encodeGins()){
            int fileCount = compileTable();
            if(fileCount > 0){
                String cutWav = compileGinFromWav(fileCount);
                if(!cutWav.equals(""))
                    if(insertTableAndMove(cutWav))
                        PlatformImpl.runAndWait(() -> {
                            alertWorker.close();
                            AlertWorker.showAlert(
                                Alert.AlertType.INFORMATION,
                                "gintool",
                                "Success",
                                "GIN has been encoded");
                        });
            }
        }
        Platform.runLater(() -> ginButton.setDisable(false));
    }
    
    private boolean encodeGins(){
        File wav = new File(wavPath);
        String usableGrainPath = (grainPath.equals(""))
                ? Paths.get(FilenameUtils.removeExtension(wavPath))
                        .toString()
                : grainPath;
        encoderPath = Paths.get(System.getProperty("user.dir"),
                "gin_encode.exe");
        String[] grains = new File(usableGrainPath).list();
        grains = Arrays.stream(grains)
                .filter(grain ->FilenameUtils.getExtension(grain)
                .toLowerCase().equals("wav"))
                .toArray(String[]::new);
        if(grains.length == 0){
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                Alert.AlertType.ERROR,
                "gintool",
                "Error",
                "Grains not found"));
            return false;
        }
        try{
            ginPath = Paths.get(System.getProperty("user.dir"), "gin");
            deleteDirectory(ginPath.toFile());
            Files.createDirectories(ginPath);
        }
        catch (IOException ex){
            ex.printStackTrace();
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "File error: failed to create a gin directory. "
                        + "Check if gintool has necessary permissions"
                        + "or move the files somewhere else"));
            return false;
        }
        int threadCount = (int) Math.round(Math.sqrt(grains.length)) * 4;
        threadCount = threadCount > 256 ? 256 : threadCount;
        TableRun[] tableRuns = new TableRun[threadCount];
        for (int i = 0; i < threadCount; i++){
            tableRuns[i] = new TableRun(usableGrainPath, i);
        }
        tableRuns[0].setThreadCount(threadCount);
        tableRuns[0].setEncoderPath(encoderPath.toString());
        tableRuns[0].setGrains(grains);
        Thread[] threads = new Thread[threadCount];
        for(int i = 0; i < threadCount; i++){
            threads[i] = new Thread(tableRuns[i]);
            threads[i].start();
        }
        try{
            for(int i = 0; i < threadCount; i++){
                threads[i].join();
            }
        }
        catch(InterruptedException ex){
            ex.printStackTrace();
            return false;
        }
        alertWorker = tableRuns[0].getAlertWorker();
        if (!tableRuns[0].isRes()){
            PlatformImpl.runLater(() -> {
                alertWorker.close();
                AlertWorker.showAlert(
                        Alert.AlertType.ERROR,
                        "gintool",
                        "Error",
                        "Runtime error: one or more grains failed to encode. "+
                            "Check if gin_encode is installed " +
                            "and supported on your system");
            });
        }
        return tableRuns[0].isRes();
    }
    
    private int compileTable(){
        Platform.runLater(() -> alertWorker.getAlert()
                .setHeaderText("Compiling table..."));
        Platform.runLater(() -> alertWorker.getAlert()
                .setContentText(""));
        File[] gins = ginPath.toFile()
                .listFiles((dir, name) -> FilenameUtils
                        .getExtension(name)
                        .toLowerCase().equals("gin"));
        File table = Paths.get(ginPath.toString(), "table.gin")
                .toFile();
        int fileCount = gins.length + 1;
        int sampleCount = 0;
        try(OutputStream os = new BufferedOutputStream(
                    new FileOutputStream(table))){
            for(File gin: gins){
                try(InputStream is = new BufferedInputStream
                        (new FileInputStream(gin))){
                    byte[] buf = new byte[24];
                    is.read(buf);
                    buf = new byte[4];
                    is.read(buf);
                    ByteBuffer byteBuf = ByteBuffer.wrap(buf);
                    byteBuf.order(ByteOrder.LITTLE_ENDIAN);
                    sampleCount += byteBuf.getInt();
                    byteBuf = ByteBuffer.allocate(4);
                    byteBuf.order(ByteOrder.LITTLE_ENDIAN);
                    byteBuf.putInt(sampleCount);
                    os.write(byteBuf.array());
                    is.close();
                }
                catch(IOException ex){
                    ex.printStackTrace();
                    PlatformImpl.runLater(() -> {
                        alertWorker.close();
                        AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "File error: Couldn't read one of the grains");
                    });
                    return -1;
                }
            }
            os.flush();
            os.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
            PlatformImpl.runLater(() -> {
                        alertWorker.close();
                        AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "File error: Couldn't write into table");
                    });
            return -1;
        }
        return fileCount;
    }
    
    private String compileGinFromWav(int fileCount){
        Platform.runLater(() -> alertWorker.getAlert()
                .setHeaderText("Compiling WAV and GIN..."));
        Platform.runLater(() -> alertWorker.getAlert()
                .setContentText(""));
        File wav = new File(wavPath);
        Path cutWav = Paths.get(FilenameUtils.removeExtension(wavPath)
                + "_cut.wav");
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg.exe",
                "-i", wavPath,
                "-map_metadata", "-1",
                "-fflags", "+bitexact", 
                "-flags:v", "+bitexact", 
                "-flags:a", "+bitexact",
                "-af", 
                "atrim=start_sample=" + minIdx + ":end_sample=" + maxIdx,
                cutWav.toString()    
        );
        builder.inheritIO();
//        String cmd = "ffmpeg.exe"
//                + " -i " + "\"" + wavFileField.getText() + "\""
//                + " -map_metadata -1"
//                + " -fflags +bitexact -flags:v +bitexact -flags:a +bitexact"
//                + " -af atrim=start_sample=" + minIDXField.getText()
//                + ":end_sample=" + maxIDXField.getText()
//                + " \"" + cutName + "_cut.wav" + "\"";
        try{
            if(Files.exists(cutWav))
                Files.delete(cutWav);
            Path cutGin = Paths.get(FilenameUtils
                    .removeExtension(cutWav.toString() + ".gin"));
            if(Files.exists(cutGin))
                Files.delete(cutGin);
        }
        catch (IOException e){
            e.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "File error: Couldn't delete cut "
                    + "WAV and GIN before encoding new ones"));
            return "";
        }
        try {
            Process pr = builder.start();
            if(pr.waitFor() != 0)
                throw new RuntimeException();
            pr.destroy();
        }
        catch (IOException | RuntimeException | InterruptedException e) {
            e.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "Runtime error: check if FFMPEG is installed "
                    + "and supported on your system"));
            return "";
        }
        try {
//            cmd = "gin_encode.exe "
//                    + "\"" + cutName + "_cut.wav" + "\""
//                    + " " + ((decelCB.isSelected() ? maxRPMField.getText() : minRPMField.getText()))
//                    + " " + ((decelCB.isSelected() ? minRPMField.getText() : maxRPMField.getText()))
//                    + " " + fileCount
//            ;
            builder = new ProcessBuilder(encoderPath.toString(),
                    cutWav.toString(),
                    decel ? maxRPM : minRPM,
                    decel ? minRPM : maxRPM,
                    Integer.toString(fileCount)
            );
            builder.directory(Paths.get(wavPath).getParent().toFile());
            builder.inheritIO();
            Process pr = builder.start();
            if(pr.waitFor() != 0)
                throw new RuntimeException();
            pr.destroy();
        }
        catch (IOException | InterruptedException | RuntimeException e){
            e.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "Runtime error: check if gin_encode is installed "
                    + "and supported on your system"));
            return "";
        }
        try{
            Files.delete(cutWav);
        }
        catch (IOException e){
            e.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "File error: couldn't delete cut "
                    + "WAV after encoding temporary GIN"));
            return "";
        }
        return cutWav.toString();
    }

    private boolean insertTableAndMove(String cutName){
        Platform.runLater(() -> alertWorker.getAlert()
                .setHeaderText("Inserting table..."));
        Platform.runLater(() -> alertWorker.getAlert()
                .setContentText(""));
        File table = Paths.get(ginPath.toString(), "table.gin")
                .toFile();
        int tableSize = (int) table.length();
        File gin = Paths.get(FilenameUtils.removeExtension(wavPath) 
                + "_cut.gin").toFile();
        File newGin = Paths.get(FilenameUtils.removeExtension(wavPath) 
                + ".gin").toFile();
        try{
            int ginSize = (int) gin.length();
            InputStream tableIn = new BufferedInputStream(
                    new FileInputStream(table));
            InputStream ginIn = new BufferedInputStream(
                    new FileInputStream(gin));
            OutputStream newGinOut = new BufferedOutputStream(
                    new FileOutputStream(newGin));
            byte[] buf = new byte[240];
            ginIn.read(buf);
            newGinOut.write(buf);
            buf = new byte[tableSize];
            ginIn.read(buf);
            tableIn.read(buf);
            newGinOut.write(buf);
            buf = new byte[ginSize - tableSize - 240];
            ginIn.read(buf);
            newGinOut.write(buf);
            newGinOut.flush();
            ginIn.close();
            newGinOut.close();
            tableIn.close();
            deleteDirectory(ginPath.toFile());
        }
        catch (NegativeArraySizeException ex){
            ex.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "Array of negative size detected. "
                    + "Check your Main values"));
            return false;
        }
        catch (Exception ex){
            ex.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "File error: couldn't perform necessary "
                    + "table operations"));
            return false;
        }
        try{
            Files.delete(gin.toPath());
            Files.move(newGin.toPath(), (exportPath.equals("")) 
                    ? newGin.toPath()
                    : Paths.get(exportPath),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        catch (IOException e){
            e.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    "gintool",
                    "Error",
                    "File error: couldn't move output "
                    + "file to final export location"));
            return false;
        }
        return true;
    }
    
    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File f : allContents) {
                deleteDirectory(f);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
