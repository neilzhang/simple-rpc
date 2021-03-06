package com.neil.simplerpc.core.netty.handler.codec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * @author neil
 */
public class KryoDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            // configure kryo instance, customize settings
            return kryo;
        }
    };

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        final byte[] array;
        final int length = msg.readableBytes();
        if (msg.hasArray()) {
            array = msg.array();
        } else {
            array = new byte[length];
            msg.getBytes(msg.readerIndex(), array, 0, length);
        }
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(array);
             Input input = new Input(byteInputStream)) {
            out.add(kryos.get().readClassAndObject(input));
        }
    }

}
