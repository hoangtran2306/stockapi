package com.finance24h.api.helpers;

import com.google.common.io.Files;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by ait on 10/9/17.
 */
public class LogStashHelper {
    private FileWriter infoFileWriter, errorFileWriter;
    private File accessLog, errorLog;
    private boolean canWrite = true;
    private int maxFileSize;
    private String path, accessLogFile, errorLogFile;

    public LogStashHelper(String path, String accessLogFile, String errorLogFile, int maxFileSize) {
        this.accessLogFile = accessLogFile;
        this.errorLogFile = errorLogFile;
        this.maxFileSize = maxFileSize;
        this.path = path;
        try {
            accessLog = new File(path + "/" + accessLogFile);
            errorLog = new File(path + "/" + errorLogFile);
            infoFileWriter = new FileWriter(accessLog, true);
            errorFileWriter = new FileWriter(errorLog, true);
        } catch (IOException e) {
            canWrite = false;
            e.printStackTrace();
        }
    }

    public synchronized void logAPI(HttpServletRequest request) {
        long startTime = Long.valueOf(request.getSession().getAttribute("start_time").toString());
        int executeTime = (int) (Utilities.getCurrentTimestampMilis() - startTime);
        String platform = request.getSession().getAttribute("platform").toString();
        String appVersion = request.getSession().getAttribute("app_version").toString();
        String deviceId = request.getSession().getAttribute("device_id").toString();
        String uri = request.getRequestURI();
        Map<String, String[]> params = request.getParameterMap();
        JsonObject logObject = new JsonObject();
        logObject.addProperty("timestamp", Utilities.getCurrentDateTime());
        logObject.addProperty("module", "finance-api");
        logObject.addProperty("path", uri);
        logObject.addProperty("method", request.getMethod());
        logObject.addProperty("ip", request.getRemoteAddr());
        logObject.addProperty("platform", platform);
        logObject.addProperty("version", appVersion);
        logObject.addProperty("device", deviceId);
        JsonObject paramObject = new JsonObject();
        for (Map.Entry<String, String[]> oneParam : params.entrySet()) {
            paramObject.addProperty(oneParam.getKey(), oneParam.getValue()[0]);
        }
        logObject.add("params", paramObject);
        logObject.addProperty("execute_time", executeTime);
        if (canWrite) {
            try {
                if (isRotate(accessLog)) {
                    infoFileWriter.close();
                    rotateFile(path, accessLogFile, accessLog);
                    accessLog = new File(path + "/" + accessLogFile);
                    infoFileWriter = new FileWriter(accessLog, true);
                }
                infoFileWriter.write(logObject + "\n");
                infoFileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logInfo(String message) {
        String log = Utilities.getCurrentDateTime() + " : " + message;
        System.out.println(log);
    }

    public void logError(String message) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("timestamp", Utilities.getCurrentDateTime());
        jsonObject.addProperty("level", "error");
        jsonObject.addProperty("message", message);
        errorLogger(jsonObject);
    }

    private synchronized void errorLogger(JsonObject jsonObject) {
        if (canWrite) {
            try {
                if (isRotate(errorLog)) {
                    errorFileWriter.close();
                    rotateFile(path, errorLogFile, errorLog);
                    errorLog = new File(path + "/" + errorLogFile);
                    errorFileWriter = new FileWriter(errorLog, true);
                }
                errorFileWriter.write(jsonObject + "\n");
                errorFileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isRotate(File logFile) {
        if (maxFileSize <= 0) {
            return false;
        }
        long bytes = logFile.length();
        long megabytes = bytes / 1024 / 1024;
        return megabytes >= maxFileSize;
    }

    private void rotateFile(String path, String fileName, File file) {
        try {
            String rawName = fileName.substring(0, fileName.length() - 4);
            String backUpName = rawName + Utilities.getCurrentTimestamp() + ".rotateLog";
            File backUpFile = new File(path + "/" + backUpName);
            FileWriter fileWriter = new FileWriter(backUpFile, true);
            fileWriter.close();
            Files.move(file, backUpFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
