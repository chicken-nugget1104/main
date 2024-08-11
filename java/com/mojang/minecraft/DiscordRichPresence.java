package com.mojang.minecraft;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;

import java.time.OffsetDateTime;

public class DiscordRichPresence {

    private static final long CLIENT_ID = 1255593789746319360L;
    private static IPCClient client;

    public static void main(String[] args) {
        client = new IPCClient(CLIENT_ID);

        client.setListener(new IPCListener() {
            @Override
            public void onReady(IPCClient client) {
                System.out.println("Connected to Discord!");
                updatePresence("Classicircle", "Playing a world");
            }
        });

        try {
            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close();
        }));
    }

    private static void updatePresence(String state, String details) {
        RichPresence.Builder builder = new RichPresence.Builder();
        builder.setState(state)
               .setDetails(details)
               .setStartTimestamp(OffsetDateTime.now())  // Use OffsetDateTime.now()
               .setLargeImage("large_image_key", "Large Image Text")
               .setSmallImage("small_image_key", "Small Image Text");

        client.sendRichPresence(builder.build());
    }
}
