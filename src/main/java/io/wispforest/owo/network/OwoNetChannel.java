package io.wispforest.owo.network;

import io.wispforest.owo.network.serialization.PacketBufSerializer;
import io.wispforest.owo.network.serialization.RecordSerializer;
import io.wispforest.owo.util.ReflectionUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An efficient networking abstraction that uses {@code record}s to store
 * and define packet data. Serialization for most types is fully automatic
 * and no custom handling needs to be done, should one of your record
 * components be of an unsupported type use {@link PacketBufSerializer#register(Class, BiConsumer, Function)}
 * to register a custom serializer.
 *
 * <p> To define a packet class suited for use with this wrapper, simply create a
 * standard Java {@code record} class and put the desired data into the record header.
 *
 * <p>To register a packet onto this channel, use either {@link #registerClientbound(Class, ChannelHandler)}
 * or {@link #registerServerbound(Class, ChannelHandler)}, depending on which direction the packet goes.
 * Bidirectional registration of the same class is explicitly supported. <b>For synchronization purposes,
 * all registration must happen on both client and server, even for clientbound packets.</b>
 *
 * <p>To send a packet, use any of the {@code handle} methods to obtain a handle for sending. These are
 * named after where the packet is sent <i>from</i>, meaning the {@link #clientHandle()} is used for sending
 * <i>to the server</i> and vice-versa.
 *
 * <p> The registered packet handlers are executed synchronously on the target environment's
 * game thread instead of Netty's event loops - there is no need to call {@code .execute(...)}
 *
 * @see PacketBufSerializer#register(Class, BiConsumer, Function)
 * @see PacketBufSerializer#registerCollectionProvider(Class, Supplier)
 */
public class OwoNetChannel {

    static final Map<Identifier, OwoNetChannel> REGISTERED_CHANNELS = new HashMap<>();

    private final Map<Class<?>, IndexedSerializer<?>> serializersByClass = new HashMap<>();
    final Int2ObjectMap<IndexedSerializer<?>> serializersByIndex = new Int2ObjectOpenHashMap<>();

    private final List<ChannelHandler<Record, ClientAccess>> clientHandlers = new ArrayList<>();
    private final List<ChannelHandler<Record, ServerAccess>> serverHandlers = new ArrayList<>();

    final Identifier packetId;
    private final String ownerClassName;

    private ClientHandle clientHandle = null;
    private ServerHandle serverHandle = null;

    /**
     * Creates a new channel with given ID. Duplicate channel IDs
     * are not allowed - if there is a collision, the name of the
     * class that previously registered the channel will be part of
     * the exception. <b>This may be called at any stage during
     * mod initialization</b>
     *
     * @param id The desired channel ID
     * @return The created channel
     */
    public static OwoNetChannel create(Identifier id) {
        return new OwoNetChannel(id, ReflectionUtils.getCallingClassName(2));
    }

    private OwoNetChannel(Identifier id, String ownerClassName) {
        if (REGISTERED_CHANNELS.containsKey(id)) {
            throw new IllegalStateException("Channel with id '" + id + "' was already registered from class '" + REGISTERED_CHANNELS.get(id).ownerClassName + "'");
        }

        this.packetId = id;
        this.ownerClassName = ownerClassName;

        ServerPlayNetworking.registerGlobalReceiver(packetId, (server, player, handler, buf, responseSender) -> {
            int handlerIndex = buf.readVarInt();
            final Record message = serializersByIndex.get(handlerIndex).serializer.read(buf);
            server.execute(() -> serverHandlers.get(handlerIndex).handle(message, new ServerAccess(player)));
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(packetId, (client, handler, buf, responseSender) -> {
                int handlerIndex = buf.readVarInt();
                final Record message = serializersByIndex.get(-handlerIndex).serializer.read(buf);
                client.execute(() -> clientHandlers.get(handlerIndex).handle(message, new ClientAccess(handler)));
            });
        }

        clientHandlers.add(null);
        serverHandlers.add(null);
        REGISTERED_CHANNELS.put(id, this);
    }

    /**
     * Registers a handler <i>on the client</i> for the specified message class.
     * This also ensures the required serializer is available. If an exception
     * about a missing type adapter is thrown, register one
     *
     * @param messageClass The type of packet data to send and serialize
     * @param handler      The handler that will receive the deserialized
     * @see #serverHandle(PlayerEntity)
     * @see #serverHandle(MinecraftServer)
     * @see #serverHandle(ServerWorld, BlockPos)
     * @see PacketBufSerializer#register(Class, BiConsumer, Function)
     */
    @SuppressWarnings("unchecked")
    public <R extends Record> void registerClientbound(Class<R> messageClass, ChannelHandler<R, ClientAccess> handler) {
        int index = this.clientHandlers.size();
        this.createSerializer(messageClass, index, EnvType.CLIENT);
        this.clientHandlers.add((ChannelHandler<Record, ClientAccess>) handler);
    }

    /**
     * Registers a handler <i>on the server</i> for the specified message class.
     * This also ensures the required serializer is available. If an exception
     * about a missing type adapter is thrown, register one
     *
     * @param messageClass The type of packet data to send and serialize
     * @param handler      The handler that will receive the deserialized
     * @see #clientHandle()
     * @see PacketBufSerializer#register(Class, BiConsumer, Function)
     */
    @SuppressWarnings("unchecked")
    public <R extends Record> void registerServerbound(Class<R> messageClass, ChannelHandler<R, ServerAccess> handler) {
        int index = this.serverHandlers.size();
        this.createSerializer(messageClass, index, EnvType.SERVER);
        this.serverHandlers.add((ChannelHandler<Record, ServerAccess>) handler);
    }

    /**
     * Obtains the client handle of this channel, used to
     * send packets <i>to the server</i>
     *
     * @return The client handle of this channel
     */
    public ClientHandle clientHandle() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT)
            throw new NetworkException("Cannot obtain client handle in environment type '" + FabricLoader.getInstance().getEnvironmentType() + "'");

        if (this.clientHandle == null) this.clientHandle = new ClientHandle();
        return clientHandle;
    }

    /**
     * Obtains a server handle used to send packets
     * <i>to all players on the given server</i>
     * <p>
     * <b>This handle will be reused - do not retain references</b>
     *
     * @param server The server to target
     * @return A server handle configured for sending packets
     * to all players on the given server
     */
    public ServerHandle serverHandle(MinecraftServer server) {
        var handle = getServerHandle();
        handle.targets = PlayerLookup.all(server);
        return handle;
    }

    /**
     * Obtains a server handle used to send packets
     * <i>to all given players</i>. Use {@link PlayerLookup} to obtain
     * the required collections
     * <p>
     * <b>This handle will be reused - do not retain references</b>
     *
     * @param targets The players to target
     * @return A server handle configured for sending packets
     * to all players in the given collection
     * @see PlayerLookup
     */
    public ServerHandle serverHandle(Collection<ServerPlayerEntity> targets) {
        var handle = getServerHandle();
        handle.targets = targets;
        return handle;
    }

    /**
     * Obtains a server handle used to send packets
     * <i>to the given player only</i>
     * <p>
     * <b>This handle will be reused - do not retain references</b>
     *
     * @param player The player to target
     * @return A server handle configured for sending packets
     * to the given player only
     */
    public ServerHandle serverHandle(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) throw new NetworkException("'player' must be a 'ServerPlayerEntity'");

        var handle = getServerHandle();
        handle.targets = Collections.singleton(serverPlayer);
        return handle;
    }

    /**
     * Obtains a server handle used to send packets
     * <i>to all players tracking the given block entity</i>
     * <p>
     * <b>This handle will be reused - do not retain references</b>
     *
     * @param entity The block entity to look up trackers for
     * @return A server handle configured for sending packets
     * to all players tracking the given block entity
     */
    public ServerHandle serverHandle(BlockEntity entity) {
        if (entity.getWorld().isClient) throw new NetworkException("Server handle cannot be obtained on the client");
        return serverHandle(PlayerLookup.tracking(entity));
    }

    /**
     * Obtains a server handle used to send packets <i>to all
     * players tracking the given position in the given world</i>
     * <p>
     * <b>This handle will be reused - do not retain references</b>
     *
     * @param world The world to look up players in
     * @param pos   The position to look up trackers for
     * @return A server handle configured for sending packets
     * to all players tracking the given position in the given world
     */
    public ServerHandle serverHandle(ServerWorld world, BlockPos pos) {
        return serverHandle(PlayerLookup.tracking(world, pos));
    }

    private ServerHandle getServerHandle() {
        if (this.serverHandle == null) this.serverHandle = new ServerHandle();
        return serverHandle;
    }

    private <R extends Record> void createSerializer(Class<R> messageClass, int handlerIndex, EnvType target) {
        var serializer = serializersByClass.get(messageClass);
        if (serializer == null) {
            final var indexedSerializer = IndexedSerializer.create(RecordSerializer.create(messageClass), handlerIndex, target);
            serializersByClass.put(messageClass, indexedSerializer);
            serializersByIndex.put(target == EnvType.CLIENT ? -handlerIndex : handlerIndex, indexedSerializer);
        } else if (serializer.handlerIndex(target) == -1) {
            serializer.setHandlerIndex(handlerIndex, target);
            serializersByIndex.put(target == EnvType.CLIENT ? -handlerIndex : handlerIndex, serializer);
        } else {
            throw new IllegalStateException("Message class '" + messageClass.getName() + "' is already registered for target environment " + target);
        }
    }

    @SuppressWarnings("unchecked")
    private <R extends Record> PacketByteBuf encode(R message, EnvType target) {
        var buffer = PacketByteBufs.create();

        final var messageClass = message.getClass();

        if (!this.serializersByClass.containsKey(messageClass)) {
            throw new NetworkException("Message class '" + messageClass + "' is not registered");
        }

        final IndexedSerializer<R> serializer = (IndexedSerializer<R>) this.serializersByClass.get(messageClass);
        if (serializer.handlerIndex(target) == -1) {
            throw new NetworkException("Message class '" + messageClass + "' has not handler registered for target environment " + target);
        }

        buffer.writeVarInt(serializer.handlerIndex(target));
        serializer.serializer.write(buffer, message);

        return buffer;
    }

    public class ClientHandle {

        /**
         * Sends the given message to the server
         *
         * @param message The message to send
         * @see #send(Record[])
         */
        public <R extends Record> void send(R message) {
            ClientPlayNetworking.send(OwoNetChannel.this.packetId, OwoNetChannel.this.encode(message, EnvType.SERVER));
        }

        /**
         * Sends the given messages to the server
         *
         * @param messages The messages to send
         */
        @SafeVarargs
        public final <R extends Record> void send(R... messages) {
            for (R message : messages) send(message);
        }
    }

    public class ServerHandle {

        private Collection<ServerPlayerEntity> targets = Collections.emptySet();

        /**
         * Sends the given message to the configured target(s)
         * <b>Resets the target(s) after sending - this cannot be used
         * for multiple messages on the same handle</b>
         *
         * @param message The message to send
         * @see #send(Record[])
         */
        public <R extends Record> void send(R message) {
            this.targets.forEach(player -> ServerPlayNetworking.send(player, OwoNetChannel.this.packetId, OwoNetChannel.this.encode(message, EnvType.CLIENT)));
            this.targets = null;
        }

        /**
         * Sends the given messages to the configured target(s)
         * <b>Resets the target(s) after sending - this cannot be used
         * multiple times on the same handle</b>
         *
         * @param messages The messages to send
         */
        @SafeVarargs
        public final <R extends Record> void send(R... messages) {
            this.targets.forEach(player -> {
                for (R message : messages) {
                    ServerPlayNetworking.send(player, OwoNetChannel.this.packetId, OwoNetChannel.this.encode(message, EnvType.CLIENT));
                }
            });
            this.targets = null;
        }
    }

    public interface ChannelHandler<R extends Record, E extends EnvironmentAccess<?, ?, ?>> {

        /**
         * Executed on the game thread to handle the incoming
         * message - this can safely modify game state
         *
         * @param message The message that was received
         * @param access  The {@link EnvironmentAccess} used to obtain references
         *                to the execution environment
         */
        void handle(R message, E access);
    }

    /**
     * A simple wrapper that provides access to the environment a packet
     * is being received / message is being handled in
     *
     * @param <P> The type of player to receive the packet
     * @param <R> The runtime that the packet is being received in
     * @param <N> The network handler that received the packet
     */
    public interface EnvironmentAccess<P extends PlayerEntity, R, N> {

        /**
         * @return The player that received the packet
         */
        P player();

        /**
         * @return The environment the packet is being received in,
         * either a {@link MinecraftServer} or a {@link net.minecraft.client.MinecraftClient}
         */
        R runtime();

        /**
         * @return The network handler of the player or client that received the packet,
         * either a {@link net.minecraft.client.network.ClientPlayNetworkHandler} or a
         * {@link net.minecraft.server.network.ServerPlayNetworkHandler}
         */
        N netHandler();
    }

    static {
        ServerLoginConnectionEvents.QUERY_START.register(OwoHandshake::queryStart);
        ServerLoginNetworking.registerGlobalReceiver(OwoHandshake.CHANNEL_ID, OwoHandshake::syncServer);

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientLoginNetworking.registerGlobalReceiver(OwoHandshake.CHANNEL_ID, OwoHandshake::syncClient);
        }
    }

    static final class IndexedSerializer<R extends Record> {
        private int clientHandlerIndex = -1;
        private int serverHandlerIndex = -1;

        final RecordSerializer<R> serializer;

        private IndexedSerializer(RecordSerializer<R> serializer) {
            this.serializer = serializer;
        }

        public static <R extends Record> IndexedSerializer<R> create(RecordSerializer<R> serializer, int index, EnvType target) {
            return new IndexedSerializer<>(serializer).setHandlerIndex(index, target);
        }

        public IndexedSerializer<R> setHandlerIndex(int index, EnvType target) {
            switch (target) {
                case CLIENT -> this.clientHandlerIndex = index;
                case SERVER -> this.serverHandlerIndex = index;
            }
            return this;
        }

        public int handlerIndex(EnvType target) {
            return switch (target) {
                case CLIENT -> clientHandlerIndex;
                case SERVER -> serverHandlerIndex;
            };
        }
    }
}
