package com.levelrin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection of WebSocket connections.
 * It's thread-safe.
 */
public final class WsConnections {

    /**
     * For JSON-related tasks.
     */
    private static final ObjectMapper JACKSON = new ObjectMapper();

    /**
     * Key: Session ID.
     * Value: Username.
     */
    private final Map<String, String> sessionIdToUsername = new HashMap<>();

    /**
     * Key: Username.
     * Value: WsContext object.
     */
    private final Map<String, WsContext> usernameToContext = new HashMap<>();

    /**
     * Key: Username.
     * Value: User's information in JSON.
     */
    private final Map<String, ObjectNode> usernameToJson = new HashMap<>();

    /**
     * Add a new WebSocket connection.
     * @param context As is.
     * @param username As is.
     */
    public synchronized void add(final WsConnectContext context, final String username) {
        this.sessionIdToUsername.put(context.sessionId(), username);
        this.usernameToContext.put(username, context);
        final ObjectNode json = JACKSON.createObjectNode();
        json.put("username", username);
        json.put("answered", false);
        json.put("answer", "");
        this.usernameToJson.put(username, json);
    }

    /**
     * Remove the WebSocket connection.
     * @param sessionId As is.
     */
    public synchronized void remove(final String sessionId) {
        if (!sessionIdToUsername.containsKey(sessionId)) {
            return;
        }
        final String username = this.sessionIdToUsername.get(sessionId);
        this.sessionIdToUsername.remove(sessionId);
        this.usernameToContext.remove(username);
        this.usernameToJson.remove(username);
    }

    /**
     * Find the username using the session ID.
     * @param sessionId As is.
     * @return As is.
     */
    public synchronized String username(final String sessionId) {
        return this.sessionIdToUsername.get(sessionId);
    }

    /**
     * Broadcast the message to all WebSocket connections.
     * @param message As is.
     */
    public synchronized void broadcast(final String message) {
        for (final WsContext context: this.usernameToContext.values()) {
            context.send(message);
        }
    }

    /**
     * For thread-safety, it returns the copy of all user information.
     * Modification on the returned map won't update the user information in this object.
     * Key: username.
     * Value: user's information in JSON.
     * @return Copy of all user information.
     */
    public synchronized Map<String, ObjectNode> allUserInfo() {
        final Map<String, ObjectNode> result = new HashMap<>();
        for (final Map.Entry<String, ObjectNode> entry: this.usernameToJson.entrySet()) {
            result.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return result;
    }

    /**
     * As is.
     * It will do nothing if the user doesn't exist.
     * @param username As is.
     * @param json As is.
     */
    public synchronized void updateUserInfo(final String username, final ObjectNode json) {
        if (this.usernameToJson.containsKey(username)) {
            this.usernameToJson.put(username, json);
        }
    }

}
