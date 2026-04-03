package rubbertoe.simple_atlas.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import rubbertoe.simple_atlas.client.screen.AtlasScreen;
import rubbertoe.simple_atlas.network.OpenAtlasScreenPayload;

public class SimpleAtlasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenAtlasScreenPayload.TYPE, (payload, _) -> Minecraft.getInstance().setScreen(
                new AtlasScreen(payload.tiles())
        ));
    }
}