package pw.lemmmy.schws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StatsSubmitter {
    private static final String SUBMIT_URL = "https://hardware.switchcraft.pw/submit";
    private static final Gson GSON = new GsonBuilder().create();
    
    private static String submitInternal(StatsPersistence persistence, Map<String, String> stats) throws MalformedURLException {
        UUID token = UUID.randomUUID();
        URL url = new URL(SUBMIT_URL + "/" + token.toString());
        
        String mcVersion = "1.12.2";
        String modVersion = SCHardwareSurvey.VERSION;
        
        SerialisedStats serialisedStats = new SerialisedStats(stats);
        String requestBody = GSON.toJson(serialisedStats);
        
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", String.format("SCHWS/%s/%s", mcVersion, modVersion));
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            
            try (OutputStream os = con.getOutputStream()) {
                IOUtils.write(requestBody, os, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Could not write data to the server", e);
            }
    
            StatsResponse response;
            try (
                InputStream is = con.getErrorStream() != null ? con.getErrorStream() : con.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
            ) {
                response = GSON.fromJson(br, StatsResponse.class);
            } catch (IOException | JsonIOException e) {
                throw new RuntimeException("Could not read data from the server", e);
            } catch (JsonSyntaxException e) {
                throw new RuntimeException("Received invalid response from the server", e);
            }
            
            if (response == null) throw new RuntimeException("Did not receive response from server");
            if (!response.ok) {
                if (StringUtils.isEmpty(response.error))
                    throw new RuntimeException("Received unknown error from the server");
                
                switch (response.error) {
                    case "invalid_token":
                        throw new RuntimeException("Invalid token");
                    case "missing_stats":
                        throw new RuntimeException("Missing stats (failed to serialise?)");
                    case "invalid_client":
                        throw new RuntimeException("Invalid client (modified mod?)");
                    case "invalid_stat":
                        throw new RuntimeException("Server did not recognise the stat:");
                    case "already_submitted":
                        throw new RuntimeException("Data was already submitted to the server");
                    default:
                        throw new RuntimeException("Unknown error: " + response.error);
                }
            }
    
            persistence.submitted(token.toString());
            return response.upliftHeadThought;
        } catch (IOException e) {
            throw new RuntimeException("Could not contact server", e);
        }
    }
    
    public static void submitStats(StatsPersistence persistence, Map<String, String> stats) {
        CompletableFuture.runAsync(() -> {
            try {
                String headThought = submitInternal(persistence, stats);
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("")
                    .appendSibling(
                        new TextComponentString("Thanks for your participation! Here is uplift head thought:")
                            .setStyle(new Style().setColor(TextFormatting.DARK_GREEN))
                    )
                    .appendSibling(new TextComponentString("\n"))
                    .appendSibling(
                        new TextComponentString(headThought)
                            .setStyle(new Style().setColor(TextFormatting.GREEN))
                    )
                );
            } catch (Exception e) {
                persistence.dontShow();
                
                SCHardwareSurvey.LOG.error("Error submitting survey: ", e);
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString("Error submitting survey: " + e.getMessage())
                        .setStyle(new Style().setColor(TextFormatting.RED))
                );
            }
        });
    }
    
    private static class SerialisedStats {
        private final Map<String, String> stats;
        
        private SerialisedStats(Map<String, String> stats) {
            this.stats = stats;
        }
    }
    
    private static class StatsResponse {
        private boolean ok;
        private String error;
        private String upliftHeadThought;
    }
}
