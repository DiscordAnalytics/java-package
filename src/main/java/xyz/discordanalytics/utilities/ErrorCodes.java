package xyz.discordanalytics.utilities;

public class ErrorCodes {
    public static final String INVALID_CLIENT_TYPE = "Invalid client type, please use a valid client.";
    public static final String CLIENT_NOT_READY = "Client is not ready, please start the client first.";
    public static final String INVALID_API_TOKEN = "Invalid API token, please gen one at " + ApiEndpoints.BASE_URL.split("/api")[0] + "and try again.";
    public static final String INVALID_CONFIGURATION = "Invalid configuration, please check your configuration and try again.";
    public static final String DATA_NOT_SENT = "Data cannot be sent to the API, I will try again later.";
    public static final String SUSPENDED_BOT = "Your bot has been suspended, please check your mailbox for more information.";
    public static final String ON_COOLDOWN = "You are on cooldown, please try again later.";
    public static final String UNKNOWN_ERROR = "An unknown error has occurred, please try again later.";
}

