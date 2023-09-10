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
    private String wavPath, grainPath, exportPath, exportName;
    private String minIdx, minRPM, maxIdx, maxRPM;
    private boolean grainPathSel, exportPathSel, exportNameSel, decel;
    
    private Path ginPath;
    private Path encoderPath;
    
    public GinCompiler(Button ginButton, String wavPath,
            String grainPath, String exportPath, String exportName,
            String minIdx, String minRPM, String maxIdx, String maxRPM,
            boolean grainPathSel, boolean exportPathSel, boolean exportNameSel,
            boolean decel){
        this.ginButton = ginButton;
        this.wavPath = wavPath;
        this.grainPath = grainPath;
        this.exportPath = exportPath;
        this.exportName = exportName;
        this.minIdx = minIdx;
        this.minRPM = minRPM;
        this.maxIdx = maxIdx;
        this.maxRPM = maxRPM;
        this.grainPathSel = grainPathSel;
        this.exportPathSel = exportPathSel;
        this.exportNameSel = exportNameSel;
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
                                I18N.get("success"),
                                I18N.get("gin_encoded"));
                        });
            }
        }
        Platform.runLater(() -> ginButton.setDisable(false));
    }
    
    private boolean encodeGins(){
        File wav = new File(wavPath);
        String usableGrainPath = grainPathSel
                ? grainPath
                : Paths.get(FilenameUtils.removeExtension(wavPath))
                        .toString();
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
                I18N.get("error"),
                I18N.get("grains_not_found")));
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
                    I18N.get("error"),
                    I18N.get("failed_create_gin_dir")));
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
                        I18N.get("error"),
                        I18N.get("failed_encode_gins"));
            });
        }
        return tableRuns[0].isRes();
    }
    
    private int compileTable(){
        Platform.runLater(() -> alertWorker.getAlert()
                .setHeaderText(I18N.get("compiling_table")));
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
                    HexWorker.skip(is, 24);
                    byte[] buf = new byte[4];
                    is.read(buf);
                    sampleCount += HexWorker.readInt32Val(ByteOrder.LITTLE_ENDIAN, buf);
                    ByteBuffer byteBuf = ByteBuffer.allocate(4);
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
                    I18N.get("error"),
                    I18N.get("couldnt_read_grains"));
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
                    I18N.get("error"),
                    I18N.get("couldnt_write_table"));
                    });
            return -1;
        }
        return fileCount;
    }
    
    private String compileGinFromWav(int fileCount){
        Platform.runLater(() -> alertWorker.getAlert()
                .setHeaderText(I18N.get("compiling_wav_gin")));
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
                    I18N.get("error"),
                    I18N.get("failed_to_delete_cut_wav_gin")));
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
                    I18N.get("error"),
                    I18N.get("ffmpeg_error")));
            return "";
        }
        try {
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
                    I18N.get("error"),
                    I18N.get("gin_encode_error")));
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
                    I18N.get("error"),
                    I18N.get("failed_to_delete_cut_wav")));
            return "";
        }
        return cutWav.toString();
    }

    private boolean insertTableAndMove(String cutName){
        Platform.runLater(() -> alertWorker.getAlert()
                .setHeaderText(I18N.get("inserting_table")));
        Platform.runLater(() -> alertWorker.getAlert()
                .setContentText(""));
        File table = Paths.get(ginPath.toString(), "table.gin")
                .toFile();
        int tableSize = (int) table.length();
        File gin = Paths.get(FilenameUtils.removeExtension(wavPath) 
                + "_cut.gin").toFile();
        File newGin = exportNameSel 
                ? Paths.get(Paths.get(wavPath).getParent().toString(), exportName).toFile()
                : Paths.get(FilenameUtils.removeExtension(wavPath) + ".gin").toFile();
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
            HexWorker.skip(ginIn, tableSize);
            buf = new byte[tableSize];
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
                    I18N.get("error"),
                    I18N.get("negative_size_array")));
            return false;
        }
        catch (Exception ex){
            ex.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    I18N.get("error"),
                    I18N.get("table_operations_error")));
            return false;
        }
        try{
            Files.delete(gin.toPath());
            Files.move(newGin.toPath(), exportPathSel 
                    ? exportNameSel 
                        ? Paths.get(Paths.get(exportPath).getParent().toString(), exportName)
                        : Paths.get(exportPath)
                    : newGin.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        catch (IOException e){
            e.printStackTrace();
            PlatformImpl.runLater(() -> {alertWorker.close();});
            PlatformImpl.runAndWait(() -> AlertWorker.showAlert(
                    Alert.AlertType.ERROR,
                    I18N.get("error"),
                    I18N.get("move_final_gin_error")));
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
