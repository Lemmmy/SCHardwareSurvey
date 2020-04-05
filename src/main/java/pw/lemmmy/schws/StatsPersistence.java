package pw.lemmmy.schws;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class StatsPersistence {
    private Properties config = new Properties();
    private File configFile = null;
    
    // Must be done at runtime
    private File getConfigFile() {
        if (configFile != null) return configFile;
        File configDir = SCHardwareSurvey.INSTANCE.configDir;
        return configFile = new File(configDir, "schardwaresurvey.properties");
    }
    
    public void loadProperties() {
        final File configFile = getConfigFile();
        SCHardwareSurvey.LOG.info("Using properties file: {}", configFile.getAbsolutePath());
        
        if (configFile.exists()) {
            try (BufferedReader br = Files.newBufferedReader(configFile.toPath())) {
                config.load(br);
            } catch (IOException e) {
                SCHardwareSurvey.LOG.error("Error reading schardwaresurvey.properties file", e);
            }
        } else {
            config.setProperty("done", "false");
            saveProperties();
        }
    }
    
    private void saveProperties() {
        try (BufferedWriter bw = Files.newBufferedWriter(configFile.toPath())) {
            config.store(bw, "Persistence information for the SwitchCraft Hardware Survey");
        } catch (IOException e) {
            SCHardwareSurvey.LOG.error("Error writing schardwaresurvey.properties file", e);
        }
    }
    
    public void submitted(String token) {
        config.setProperty("done", "true");
        config.setProperty("token", token);
        saveProperties();
    }
    
    public void dontShow() {
        config.setProperty("done", "true");
        saveProperties();
    }
    
    public boolean isDone() {
        return Boolean.parseBoolean(config.getProperty("done", "false"));
    }
}
