package rubbertoe.simple_atlas.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import rubbertoe.simple_atlas.SimpleAtlas;

public final class ModKeyBindings {
    public static KeyMapping RESET_ZOOM_KEY;

    private ModKeyBindings() {
    }

    public static void initialize() {
        if (RESET_ZOOM_KEY != null) {
            return;
        }

        KeyMapping.Category atlasCategory = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(SimpleAtlas.MOD_ID, "atlas")
        );

        RESET_ZOOM_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.simple_atlas.reset_zoom",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                atlasCategory
        ));
    }
}


