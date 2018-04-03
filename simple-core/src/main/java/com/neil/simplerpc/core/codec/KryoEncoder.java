package com.neil.simplerpc.core.codec;


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

    private ThreadLocal<Kryo> kryoMap = new ThreadLocal<>();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream(1024);
             Output output = new Output(byteOutputStream)) {
            getKryo().writeClassAndObject(output, msg);
            output.flush();
            out.add(wrappedBuffer(byteOutputStream.toByteArray()));
        }
    }

    private Kryo getKryo() {
        Kryo kryo = kryoMap.get();
        if (kryo == null) {
            kryo = new Kryo();
            kryoMap.set(kryo);
        }
        return kryo;
    }

}
