package io.wispforest.uwu;

import blue.endless.jankson.JsonPrimitive;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.wispforest.owo.config.ConfigSynchronizer;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.itemgroup.Icon;
import io.wispforest.owo.itemgroup.OwoItemGroup;
import io.wispforest.owo.itemgroup.gui.ItemGroupButton;
import io.wispforest.owo.kodeck.*;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.offline.OfflineAdvancementLookup;
import io.wispforest.owo.offline.OfflineDataLookup;
import io.wispforest.owo.particles.ClientParticles;
import io.wispforest.owo.particles.systems.ParticleSystem;
import io.wispforest.owo.particles.systems.ParticleSystemController;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import io.wispforest.owo.text.CustomTextRegistry;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.util.RegistryAccess;
import io.wispforest.owo.util.TagInjector;
import io.wispforest.uwu.config.BruhConfig;
import io.wispforest.uwu.config.UwuConfig;
import io.wispforest.uwu.items.UwuItems;
import io.wispforest.uwu.network.*;
import io.wispforest.uwu.text.BasedTextContent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Uwu implements ModInitializer {

    public static final boolean WE_TESTEN_HANDSHAKE = false;

    public static final TagKey<Item> TAB_2_CONTENT = TagKey.of(RegistryKeys.ITEM, new Identifier("uwu", "tab_2_content"));
    public static final Identifier GROUP_TEXTURE = new Identifier("uwu", "textures/gui/group.png");
    public static final Identifier OWO_ICON_TEXTURE = new Identifier("uwu", "textures/gui/icon.png");
    public static final Identifier ANIMATED_BUTTON_TEXTURE = new Identifier("uwu", "textures/gui/animated_icon_test.png");

    public static final ScreenHandlerType<EpicScreenHandler> EPIC_SCREEN_HANDLER_TYPE = new ScreenHandlerType<>(EpicScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

    public static final OwoItemGroup FOUR_TAB_GROUP = OwoItemGroup.builder(new Identifier("uwu", "four_tab_group"), () -> Icon.of(Items.AXOLOTL_BUCKET))
            .disableDynamicTitle()
            .buttonStackHeight(1)
            .initializer(group -> {
                group.addTab(Icon.of(ANIMATED_BUTTON_TEXTURE, 32, 1000, false), "tab_1", null, true);
                group.addTab(Icon.of(Items.EMERALD), "tab_2", TAB_2_CONTENT, false);
                group.addTab(Icon.of(Items.AMETHYST_SHARD), "tab_3", null, false);
                group.addTab(Icon.of(Items.GOLD_INGOT), "tab_4", null, false);

                group.addButton(ItemGroupButton.github(group, "https://github.com/wisp-forest/owo-lib"));
            })
            .build();

    public static final OwoItemGroup SIX_TAB_GROUP = OwoItemGroup.builder(new Identifier("uwu", "six_tab_group"), () -> Icon.of(Items.POWDER_SNOW_BUCKET))
            .tabStackHeight(3)
            .customTexture(GROUP_TEXTURE)
            .initializer(group -> {
                group.addTab(Icon.of(Items.DIAMOND), "tab_1", null, true);
                group.addTab(Icon.of(Items.EMERALD), "tab_2", null, false);
                group.addTab(Icon.of(Items.AMETHYST_SHARD), "tab_3", null, false);
                group.addTab(Icon.of(Items.GOLD_INGOT), "tab_4", null, false);
                group.addCustomTab(Icon.of(Items.IRON_INGOT), "tab_5", (context, entries) -> entries.add(UwuItems.SCREEN_SHARD), false);
                group.addTab(Icon.of(Items.QUARTZ), "tab_6", null, false);

                group.addButton(new ItemGroupButton(group, Icon.of(OWO_ICON_TEXTURE, 0, 0, 16, 16), "owo", () -> {
                    MinecraftClient.getInstance().player.sendMessage(Text.of("oωo button pressed!"), false);
                }));
            })
            .build();

    public static final OwoItemGroup SINGLE_TAB_GROUP = OwoItemGroup.builder(new Identifier("uwu", "single_tab_group"), () -> Icon.of(OWO_ICON_TEXTURE, 0, 0, 16, 16))
            .displaySingleTab()
            .initializer(group -> group.addTab(Icon.of(Items.SPONGE), "tab_1", null, true))
            .build();

    public static final ItemGroup VANILLA_GROUP = Registry.register(Registries.ITEM_GROUP, new Identifier("uwu", "vanilla_group"), FabricItemGroup.builder()
            .displayName(Text.literal("who did this"))
            .icon(Items.ACACIA_BOAT::getDefaultStack)
            .entries((context, entries) -> entries.add(Items.MANGROVE_CHEST_BOAT))
            .build());

    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(new Identifier("uwu", "uwu"));

    public static final TestMessage MESSAGE = new TestMessage("hahayes", 69, Long.MAX_VALUE, ItemStack.EMPTY, Short.MAX_VALUE, Byte.MAX_VALUE, new BlockPos(69, 420, 489),
            Float.NEGATIVE_INFINITY, Double.NaN, false, new Identifier("uowou", "hahayes"), Collections.emptyMap(),
            new int[]{10, 20}, new String[]{"trollface"}, new short[]{1, 2, 3}, new long[]{Long.MAX_VALUE, 1, 3}, new byte[]{1, 2, 3, 4},
            Optional.of("NullableString"), Optional.empty(),
            ImmutableList.of(new BlockPos(9786, 42, 9234)), new SealedSubclassOne("basede", 10), new SealedSubclassTwo(10, null));

    public static final ParticleSystemController PARTICLE_CONTROLLER = new ParticleSystemController(new Identifier("uwu", "particles"));
    public static final ParticleSystem<Void> CUBE = PARTICLE_CONTROLLER.registerDeferred(Void.class);
    public static final ParticleSystem<Void> BREAK_BLOCK_PARTICLES = PARTICLE_CONTROLLER.register(Void.class, (world, pos, data) -> {
        ClientParticles.persist();

        ClientParticles.setParticleCount(30);
        ClientParticles.spawnLine(ParticleTypes.DRAGON_BREATH, world, pos.add(.5, .5, .5), pos.add(.5, 2.5, .5), .015f);

        ClientParticles.randomizeVelocityOnAxis(.1, Direction.Axis.Z);
        ClientParticles.spawn(ParticleTypes.CLOUD, world, pos.add(.5, 2.5, .5), 0);

        ClientParticles.reset();
    });

    public static final UwuConfig CONFIG = UwuConfig.createAndLoad();
    public static final BruhConfig BRUHHHHH = BruhConfig.createAndLoad(builder -> {
        builder.registerSerializer(Color.class, (color, marshaller) -> new JsonPrimitive("bruv"));
    });

    @Override
    public void onInitialize() {

        FieldRegistrationHandler.register(UwuItems.class, "uwu", true);

        TagInjector.inject(Registries.BLOCK, BlockTags.BASE_STONE_OVERWORLD.id(), Blocks.GLASS);
        TagInjector.injectTagReference(Registries.ITEM, ItemTags.COALS.id(), ItemTags.FOX_FOOD.id());

        FOUR_TAB_GROUP.initialize();
        SIX_TAB_GROUP.initialize();
        SINGLE_TAB_GROUP.initialize();

        CHANNEL.registerClientbound(TestMessage.class, (message, access) -> {
            access.player().sendMessage(Text.of(message.string), false);
        });

        CHANNEL.registerClientboundDeferred(OtherTestMessage.class);

        CHANNEL.registerServerbound(TestMessage.class, (message, access) -> {
            access.player().sendMessage(Text.of(String.valueOf(message.bite)), false);
            access.player().sendMessage(Text.of(String.valueOf(message)), false);
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && WE_TESTEN_HANDSHAKE) {
            OwoNetChannel.create(new Identifier("uwu", "server_only_channel"));
            new ParticleSystemController(new Identifier("uwu", "server_only_particles"));
        }

        System.out.println(RegistryAccess.getEntry(Registries.ITEM, Items.ACACIA_BOAT));
        System.out.println(RegistryAccess.getEntry(Registries.ITEM, new Identifier("acacia_planks")));

        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            dispatcher.register(
                    literal("show_nbt")
                            .then(argument("player", GameProfileArgumentType.gameProfile())
                                    .executes(context -> {
                                        GameProfile profile = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
                                        NbtCompound tag = OfflineDataLookup.get(profile.getId());
                                        context.getSource().sendFeedback(() -> NbtHelper.toPrettyPrintedText(tag), false);
                                        return 0;
                                    })));

            dispatcher.register(
                    literal("test_advancement_cache")
                            .then(literal("read")
                                    .then(argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(context -> {
                                                GameProfile profile = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
                                                Map<Identifier, AdvancementProgress> map = OfflineAdvancementLookup.get(profile.getId());
                                                context.getSource().sendFeedback(() -> Text.literal(map.toString()), false);
                                                System.out.println(map);
                                                return 0;
                                            })))
                            .then(literal("write")
                                    .then(argument("player", GameProfileArgumentType.gameProfile())
                                            .executes(context -> {
                                                MinecraftServer server = context.getSource().getServer();
                                                GameProfile profile = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();

                                                OfflineAdvancementLookup.edit(profile.getId(), handle -> {
                                                    handle.grant(server.getAdvancementLoader().get(new Identifier("story/iron_tools")));
                                                });

                                                return 0;
                                            }))));

            dispatcher.register(literal("get_option")
                    .then(argument("config", StringArgumentType.string())
                            .then(argument("option", StringArgumentType.string()).executes(context -> {
                                var value = ConfigSynchronizer.getClientOptions(
                                        context.getSource().getPlayer(),
                                        StringArgumentType.getString(context, "config")
                                ).get(new Option.Key(StringArgumentType.getString(context, "option")));

                                context.getSource().sendFeedback(() -> Text.literal(String.valueOf(value)), false);

                                return 0;
                            }))));

            dispatcher.register(literal("kodeck_test")
                    .executes(context -> {
                        var rand = context.getSource().getWorld().random;
                        var source = context.getSource();

                        //--

                        String testPhrase = "This is a test to see how kodeck dose.";

                        source.sendMessage(Text.of("Input:  " + testPhrase));

                        var nbtData = Kodeck.STRING.encode(NbtFormat.SAFE, testPhrase);
                        var fromNbtData = Kodeck.STRING.decode(NbtFormat.SAFE, nbtData);

                        var jsonData = Kodeck.STRING.encode(JsonFormat.INSTANCE, fromNbtData);
                        var fromJsonData = Kodeck.STRING.decode(JsonFormat.INSTANCE, jsonData);

                        source.sendMessage(Text.of("Output: " + fromJsonData));

                        source.sendMessage(Text.empty());

                        //--

                        int randomNumber = rand.nextInt(20000);

                        source.sendMessage(Text.of("Input:  " + randomNumber));

                        var jsonNum = Kodeck.INT.encode(JsonFormat.INSTANCE, randomNumber);

                        source.sendMessage(Text.of("Output: " + Kodeck.INT.decode(JsonFormat.INSTANCE, jsonNum)));

                        source.sendMessage(Text.empty());

                        //--

                        List<Integer> randomNumbers = new ArrayList<>();

                        var maxCount = rand.nextInt(20);

                        for(int i = 0; i < maxCount; i++){
                            randomNumbers.add(rand.nextInt(20000));
                        }

                        source.sendMessage(Text.of("Input:  " + randomNumbers));

                        Kodeck<List<Integer>> INT_LIST_KODECK = ListKodeck.of(Kodeck.INT);

                        var nbtListData = INT_LIST_KODECK.encode(NbtFormat.SAFE, randomNumbers);

                        source.sendMessage(Text.of("Output: " + INT_LIST_KODECK.decode(NbtFormat.SAFE, nbtListData)));

                        source.sendMessage(Text.empty());

                        //---
                        {
                            if (source.getPlayer() == null) return 0;

                            ItemStack stack = source.getPlayer().getStackInHand(Hand.MAIN_HAND);

                            source.sendMessage(Text.of(stack.toString()));
                            source.sendMessage(Text.of(String.valueOf(stack.getOrCreateNbt())));

                            source.sendMessage(Text.of("---"));

                            JsonElement stackJsonData;

                            try {
                                stackJsonData = Kodeck.ITEM_STACK.encode(JsonFormat.INSTANCE, stack);
                            } catch (Exception exception){
                                source.sendMessage(Text.of(exception.getMessage()));
                                source.sendMessage(Text.of((Arrays.toString(exception.getStackTrace()))));

                                return 0;
                            }

                            source.sendMessage(Text.of(stackJsonData.toString()));

                            source.sendMessage(Text.of("---"));

                            var stackFromJson = Kodeck.ITEM_STACK.decode(JsonFormat.INSTANCE, stackJsonData);

                            source.sendMessage(Text.of(stackFromJson.toString()));
                            source.sendMessage(Text.of(String.valueOf(stackFromJson.getOrCreateNbt())));
                        }

                        source.sendMessage(Text.empty());

                        {
                            if (source.getPlayer() == null) return 0;

                            ItemStack stack = source.getPlayer().getStackInHand(Hand.MAIN_HAND);

                            source.sendMessage(Text.of(stack.toString()));
                            source.sendMessage(Text.of(String.valueOf(stack.getOrCreateNbt())));

                            source.sendMessage(Text.of("---"));

                            var stackByteData = Kodeck.ITEM_STACK.encode(PacketBufFormat.INSTANCE, stack);

                            source.sendMessage(Text.of(stackByteData.toString()));

                            source.sendMessage(Text.of("---"));

                            var stackFromByte = Kodeck.ITEM_STACK.decode(PacketBufFormat.INSTANCE, stackByteData);

                            source.sendMessage(Text.of(stackFromByte.toString()));
                            source.sendMessage(Text.of(String.valueOf(stackFromByte.getOrCreateNbt())));
                        }
                        //--

                        return 0;
                    }));
        });

        CustomTextRegistry.register("based", BasedTextContent.Serializer.INSTANCE);

        UwuNetworkExample.init();
        UwuOptionalNetExample.init();
    }

    public record OtherTestMessage(BlockPos pos, String message) {}

    public record TestMessage(String string, Integer integer, Long along, ItemStack stack, Short ashort, Byte bite,
                              BlockPos pos, Float afloat, Double adouble, Boolean aboolean, Identifier identifier,
                              Map<String, Integer> map,
                              int[] arr1, String[] arr2, short[] arr3, long[] arr4, byte[] arr5,
                              Optional<String> optional1, Optional<String> optional2,
                              List<BlockPos> posses, SealedTestClass sealed1, SealedTestClass sealed2) {}

}