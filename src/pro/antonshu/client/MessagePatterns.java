package pro.antonshu.client;

import java.util.regex.Pattern;

public final class MessagePatterns {


    public static final String AUTH_PATTERN = "/auth %s %s";
    public static final String AUTH_SUCCESS_RESPONSE = "/auth successful";
    public static final String AUTH_FAIL_RESPONSE = "/auth fail";

    public static final String REG_PREFIX = "/reg";
    public static final String REG_PATTERN = "/reg %s %s";
    public static final String REG_SUCCESS_RESPONSE = "/reg successful";
    public static final String REG_FAIL_RESPONSE = "/reg fail";


    public static final String MESSAGE_PREFIX = "/w";
    public static final String MESSAGE_SEND_PATTERN = MESSAGE_PREFIX +" %s %s %s";

    public static final String DISCONNECT = "/disconnect";
    public static final String DISCONNECT_SEND = DISCONNECT + " %s";
    public static final String CONNECTED = "/connected";
    public static final String CONNECTED_SEND = CONNECTED + " %s";
    public static final String CONNECTED_USERS_REQUEST = "/connectedListRequest";
    public static final String CONNECTED_USERS_LIST = "/connectedList";
    public static final String CONNECTED_USERS_LIST_SEND = CONNECTED_USERS_LIST +" %S";

    public static final Pattern MESSAGE_REC_PATTERN = Pattern.compile("^/w (\\w+) (\\w+) (.+)", Pattern.MULTILINE);

}