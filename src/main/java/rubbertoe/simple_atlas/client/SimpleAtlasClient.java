package rubbertoe.simple_atlas.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import rubbertoe.simple_atlas.client.screen.AtlasDebugScreen;
import rubbertoe.simple_atlas.network.OpenAtlasDebugScreenPayload;

public class SimpleAtlasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenAtlasDebugScreenPayload.TYPE, (payload, _) -> Minecraft.getInstance().setScreen(
                new AtlasDebugScreen(payload.width(), payload.height(), payload.tiles())
        ));
    }
}