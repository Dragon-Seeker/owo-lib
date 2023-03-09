package io.wispforest.owo.ui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.mixin.ui.ButtonWidgetAccessor;
import io.wispforest.owo.mixin.ui.ClickableWidgetAccessor;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.parsing.UIModelParsingException;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.ui.util.Drawer;
import io.wispforest.owo.ui.util.OwoNinePatchRenderers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.data.client.BlockStateVariantMap;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.data.client.BlockStateVariantMap.QuintFunction;

public class ButtonComponent extends ButtonWidget {

    protected Renderer renderer = Renderer.VANILLA;
    protected boolean textShadow = true;

    protected ButtonComponent(Text message, Consumer<ButtonComponent> onPress) {
        super(0, 0, 0, 0, message, button -> onPress.accept((ButtonComponent) button), ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        this.sizing(Sizing.content());
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderer.draw(matrices, this, delta);

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int color = this.active ? 0xffffff : 0xa0a0a0;

        int x = Math.round(this.getX() + this.width / 2f - textRenderer.getWidth(this.getMessage()) / 2f);
        int y = Math.round(this.getY() + (this.height - 8) / 2f);

        if (this.textShadow) {
            textRenderer.drawWithShadow(matrices, this.getMessage(), x, y, color);
        } else {
            textRenderer.draw(matrices, this.getMessage(), x, y, color);
        }

        var tooltip = ((ClickableWidgetAccessor)this).owo$getTooltip();
        if (this.hovered && tooltip != null) Drawer.utilityScreen().renderOrderedTooltip(matrices, tooltip.getLines(MinecraftClient.getInstance()), mouseX, mouseY);
    }

    public ButtonComponent onPress(Consumer<ButtonComponent> onPress) {
        ((ButtonWidgetAccessor) this).owo$setOnPress(button -> onPress.accept((ButtonComponent) button));
        return this;
    }

    public ButtonComponent renderer(Renderer renderer) {
        this.renderer = renderer;
        return this;
    }

    public Renderer renderer() {
        return this.renderer;
    }

    public ButtonComponent textShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    public boolean textShadow() {
        return this.textShadow;
    }

    public ButtonComponent active(boolean active) {
        this.active = active;
        return this;
    }

    public boolean active() {
        return this.active;
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);
        UIParsing.apply(children, "text", UIParsing::parseText, this::setMessage);
        UIParsing.apply(children, "text-shadow", UIParsing::parseBool, this::textShadow);
        UIParsing.apply(children, "renderer", Renderer::parse, this::renderer);
    }

    protected CursorStyle owo$preferredCursorStyle() {
        return CursorStyle.HAND;
    }

    @FunctionalInterface
    public interface Renderer {
        Renderer VANILLA = (matrices, button, delta) -> {
            RenderSystem.enableDepthTest();

            if (button.active) {
                if (button.hovered) {
                    OwoNinePatchRenderers.HOVERED_BUTTON.draw(matrices, button.getX(), button.getY(), button.width, button.height);
                } else {
                    OwoNinePatchRenderers.ACTIVE_BUTTON.draw(matrices, button.getX(), button.getY(), button.width, button.height);
                }
            } else {
                OwoNinePatchRenderers.BUTTON_DISABLED.draw(matrices, button.getX(), button.getY(), button.width, button.height);
            }
        };

        static Renderer flat(int color, int hoveredColor, int disabledColor) {
            return (matrices, button, delta) -> {
                RenderSystem.enableDepthTest();

                if (button.active) {
                    if (button.hovered) {
                        Drawer.fill(matrices, button.getX(), button.getY(), button.getX() + button.width, button.getY() + button.height, hoveredColor);
                    } else {
                        Drawer.fill(matrices, button.getX(), button.getY(), button.getX() + button.width, button.getY() + button.height, color);
                    }
                } else {
                    Drawer.fill(matrices, button.getX(), button.getY(), button.getX() + button.width, button.getY() + button.height, disabledColor);
                }
            };
        }

        static Renderer texture(Identifier texture, int u, int v, int textureWidth, int textureHeight) {
            return (matrices, button, delta) -> {
                int renderV = v;
                if (!button.active) {
                    renderV += button.height * 2;
                } else if (button.isHovered()) {
                    renderV += button.height;
                }

                RenderSystem.enableDepthTest();
                RenderSystem.setShaderTexture(0, texture);
                Drawer.drawTexture(matrices, button.getX(), button.getY(), u, renderV, button.width, button.height, textureWidth, textureHeight);
            };
        }

        void draw(MatrixStack matrices, ButtonComponent button, float delta);

        static Renderer parse(Element element) {
            var children = UIParsing.<Element>allChildrenOfType(element, Node.ELEMENT_NODE);
            if (children.size() > 1) throw new UIModelParsingException("'renderer' declaration may only contain a single child");

            var rendererElement = children.get(0);
            return switch (rendererElement.getNodeName()) {
                case "vanilla" -> VANILLA;
                case "flat" -> {
                    UIParsing.expectAttributes(rendererElement, "color", "hovered-color", "disabled-color");
                    yield flat(
                            Color.parseAndPack(rendererElement.getAttributeNode("color")),
                            Color.parseAndPack(rendererElement.getAttributeNode("hovered-color")),
                            Color.parseAndPack(rendererElement.getAttributeNode("disabled-color"))
                    );
                }
                case "texture" -> {
                    UIParsing.expectAttributes(rendererElement, "texture", "u", "v", "texture-width", "texture-height");
                    yield texture(
                            UIParsing.parseIdentifier(rendererElement.getAttributeNode("texture")),
                            UIParsing.parseUnsignedInt(rendererElement.getAttributeNode("u")),
                            UIParsing.parseUnsignedInt(rendererElement.getAttributeNode("v")),
                            UIParsing.parseUnsignedInt(rendererElement.getAttributeNode("texture-width")),
                            UIParsing.parseUnsignedInt(rendererElement.getAttributeNode("texture-height"))
                    );
                }
                default -> throw new UIModelParsingException("Unknown button renderer '" + rendererElement.getNodeName() + "'");
            };
        }
    }
}
