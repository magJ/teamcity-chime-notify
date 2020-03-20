package com.magnusjason.teamcitychimenotify;

import com.google.gson.Gson;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ChimeClient {

  private final String webhookUrl;
  private final HttpClient client;
  private final Gson gson = new Gson();

  public ChimeClient(String webhookUrl) {
    this.webhookUrl = webhookUrl;
    this.client = HttpClients.createDefault();
  }

  public void sendMessage(String message) {
    Map<String, String> payloadMap = new HashMap<>();
    payloadMap.put("Content", message);

    String json = gson.toJson(payloadMap);

    HttpPost request = new HttpPost(webhookUrl);
    request.setHeader("User-Agent", "Teamcity chime notification plugin");
    request.setHeader("Content-Type", "application/json");
    request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

    try {
      client.execute(request);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }




}