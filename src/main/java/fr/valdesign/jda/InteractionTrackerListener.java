package fr.valdesign.jda;

import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class InteractionTrackerListener extends ListenerAdapter {
    private final String baseAPIUrl;
    private final String apiKey;

    public InteractionTrackerListener(String baseAPIUrl, String apiKey) {
        this.baseAPIUrl = baseAPIUrl;
        this.apiKey = apiKey;
    }
}
