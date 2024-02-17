package example.javacord;

import xyz.discordanalytics.JavacordAnalytics;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

import java.io.IOException;

public class ExampleWithJavacord {
    public static void main(String[] args) {
        // Create a Discord client
        // Don't forget to replace YOUR_BOT_TOKEN by your Discord bot token !
        DiscordApi api = new DiscordApiBuilder()
                .setToken("YOUR_BOT_TOKEN")
                .addIntents(Intent.MESSAGE_CONTENT)
                .login().join();

        // Initialize the DiscordAnalytics class
        // Don't forget to replace YOUR_API_TOKEN by your Discord Analytics token !
        JavacordAnalytics analytics = new JavacordAnalytics(api, "YOUR_API_KEY");
        // Start the tracking, it will be done every 10 minutes to avoid spamming the API
        try {
            analytics.trackEvents();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
