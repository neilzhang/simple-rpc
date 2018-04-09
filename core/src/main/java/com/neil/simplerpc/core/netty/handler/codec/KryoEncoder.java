package com.neil.simplerpc.core.netty.handler.codec;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static io.netty.buffer.Unpooled.wrappedBuffer;

/**
 * @author neil
 */
public class KryoEncoder extends MessageToMessageEncoder<Object> {

    private static final ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            // configure kryo instance, customize settings
            return kryo;
        }
    };

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream(1024);
             Output output = new Output(byteOutputStream)) {
            kryos.get().writeClassAndObject(output, msg);
            output.flush();
            out.add(wrappedBuffer(byteOutputStream.toByteArray()));
        }
    }

}
