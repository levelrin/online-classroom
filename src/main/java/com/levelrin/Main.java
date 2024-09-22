package com.levelrin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String... args) {
        final WsConnections wsConnections = new WsConnections();
        final AtomicBoolean showHostAnswer = new AtomicBoolean(false);
        final AtomicBoolean showStudentAnswers = new AtomicBoolean(false);
        final AtomicReference<String> hostName = new AtomicReference<>("");
        final AtomicReference<String> hostAnswer = new AtomicReference<>("");
        final ObjectMapper jackson = new ObjectMapper();
        Javalin.create(config -> config.staticFiles.add("/static"))
            .post("/login", context -> {
                final String username = context.formParam("username");
                if (username == null) {
                    context.status(HttpStatus.BAD_REQUEST);
                    context.result("Username is required.");
                    return;
                }
                if (username.contains(" ")) {
                    context.status(HttpStatus.BAD_REQUEST);
                    context.result("Username should not contain spaces.");
                    return;
                }

                context.sessionAttribute("username", username);
                context.cookie("username", username);
                context.redirect("/classroom.html", HttpStatus.MOVED_PERMANENTLY);
            })
            .get("/classroom-info", context -> {
                final Map<String, ObjectNode> allUserInfo = wsConnections.allUserInfo();
                final ObjectNode result = jackson.createObjectNode();
                final boolean showStudentAnswersVal = showStudentAnswers.get();
                result.put("host", hostName.get());
                result.put("show-host-answer", showHostAnswer.get());
                result.put("show-student-answers", showStudentAnswersVal);
                result.put("host-answer", hostAnswer.get());
                final ArrayNode array = result.putArray("users");
                for (final ObjectNode userInfo : allUserInfo.values()) {
                    if (!showStudentAnswersVal) {
                        userInfo.put("answer", "");
                    }
                    array.add(userInfo);
                }
                context.contentType(ContentType.JSON);
                context.json(result);
            })
            .post("/answer", context -> {
                final String username = context.sessionAttribute("username");
                final String rawBody = context.body();
                final JsonNode json = jackson.readTree(rawBody);
                final String hostNameVal = hostName.get();

                // Host command
                if (json.get("about").asText().equals("host")) {
                    if (hostNameVal.isEmpty() || hostNameVal.equals(username)) {
                        final String newHost = json.get("username").asText();
                        hostName.set(newHost);
                        final ObjectNode jsonMessage = jackson.createObjectNode().put("about", "classroom-info");
                        final String message = jackson.writeValueAsString(jsonMessage);
                        wsConnections.broadcast(message);
                    }
                    return;
                }

                final String answer = json.get("answer").asText();
                final ObjectNode newInfo = jackson.createObjectNode();
                newInfo.put("username", username);
                newInfo.put("answered", true);
                newInfo.put("answer", answer);
                wsConnections.updateUserInfo(username, newInfo);
                if (hostNameVal.equals(username)) {
                    showHostAnswer.set(true);
                    showStudentAnswers.set(true);
                    hostAnswer.set(answer);
                }
                final ObjectNode jsonMessage = jackson.createObjectNode().put("about", "classroom-info");
                final String message = jackson.writeValueAsString(jsonMessage);
                wsConnections.broadcast(message);
            })
            .post("/reset", context -> {
                final String username = context.sessionAttribute("username");
                if (hostName.get().equals(username)) {
                    showHostAnswer.set(false);
                    showStudentAnswers.set(false);
                    hostAnswer.set("");
                    final Map<String, ObjectNode> allUserInfo = wsConnections.allUserInfo();
                    for (final ObjectNode userInfo : allUserInfo.values()) {
                        userInfo.put("answered", false);
                        userInfo.put("answer", "");
                        wsConnections.updateUserInfo(userInfo.get("username").asText(), userInfo);
                    }
                    final ObjectNode jsonMessage = jackson.createObjectNode().put("about", "classroom-info");
                    final String message = jackson.writeValueAsString(jsonMessage);
                    wsConnections.broadcast(message);
                }
            })
            .ws("/connect", ws -> {
                ws.onConnect(context -> {
                    context.session.setIdleTimeout(Duration.ofDays(1));
                    context.enableAutomaticPings();
                    final String encodedUsername = context.queryParam("username");
                    if (encodedUsername == null) {
                        context.closeSession(400, "Please login first.");
                        return;
                    }
                    final String username = URLDecoder.decode(encodedUsername, StandardCharsets.UTF_8);
                    wsConnections.add(context, username);
                    final ObjectNode jsonMessage = jackson.createObjectNode().put("about", "classroom-info");
                    final String message = jackson.writeValueAsString(jsonMessage);
                    wsConnections.broadcast(message);
                });
                ws.onClose(context -> {
                    // Remove host.
                    final String username = wsConnections.username(context.sessionId());
                    if (hostName.get().equals(username)) {
                        hostName.set("");
                    }

                    wsConnections.remove(context.sessionId());
                    final ObjectNode jsonMessage = jackson.createObjectNode().put("about", "classroom-info");
                    final String message = jackson.writeValueAsString(jsonMessage);
                    wsConnections.broadcast(message);
                });
                ws.onError(context -> {
                    // Remove host.
                    final String username = wsConnections.username(context.sessionId());
                    if (hostName.get().equals(username)) {
                        hostName.set("");
                    }

                    wsConnections.remove(context.sessionId());
                    final ObjectNode jsonMessage = jackson.createObjectNode().put("about", "classroom-info");
                    final String message = jackson.writeValueAsString(jsonMessage);
                    wsConnections.broadcast(message);
                });
            })
            .start(7070);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Server is ready!");
        }
    }

}
