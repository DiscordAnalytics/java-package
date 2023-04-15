package example;

import example.jda.ReadyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class ExampleWithJDA {
    public static void main(String[] args) {
        // Create a Discord client
        // Don't forget to replace YOUR_BOT_TOKEN by your Discord bot token !
        JDA api = JDABuilder.createDefault("YOUR_BOT_TOKEN").build();

        // Handle the ReadyEvent
        api.addEventListener(new ReadyListener());
    }
}
