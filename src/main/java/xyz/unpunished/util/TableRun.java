package xyz.unpunished.util;

import com.sun.javafx.application.PlatformImpl;
import java.io.IOException;
import java.nio.file.Paths;
import javafx.application.Platform;
import lombok.SneakyThrows;

public class TableRun implements Runnable{

    private final int threadNum;
    private static int threadCount;
    private static String encoderPath;
    private static boolean res = true;
    private static AlertWorker alertWorker;
    private static String[] grains;
    private final String grainPath;

    public TableRun(String grainPath, int threadNum){
        this.grainPath = grainPath;
        this.threadNum = threadNum;
    }

    public AlertWorker getAlertWorker(){
        return alertWorker;
    }

    public void setThreadCount(int threadCount){
        TableRun.threadCount = threadCount;
    }
    
    public void setEncoderPath(String encoderPath){
        TableRun.encoderPath = encoderPath;
    }

    public void setGrains(String[] grains){
        TableRun.grains = grains;
    }

    public boolean isRes() {
        return res;
    }

    @SneakyThrows
    @Override
    public void run() {
        try{
            if(threadNum == 0){
                PlatformImpl.runAndWait(() -> alertWorker = new AlertWorker(grains.length));
                PlatformImpl.runLater(() -> alertWorker.show());
            }
            for(int i = threadNum; i < grains.length; i += threadCount){
                ProcessBuilder builder = new ProcessBuilder(encoderPath,
                    Paths.get(grainPath, grains[i]).toString(),
                    "1", "2", "2" );
                builder.directory(Paths.get(System.getProperty("user.dir"),
                        "gin").toFile());
                builder.redirectErrorStream(true);
                Process pr = builder.start();
                pr.waitFor();
                pr.destroy();
                Platform.runLater(() -> alertWorker.incrementGrain());
            }
        }
        catch (IOException | InterruptedException e){
            e.printStackTrace();
            res = false;
        }
    }
}
