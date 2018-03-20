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

    private Kryo kryo;

    public KryoEncoder(Kryo kryo) {
        this.kryo = kryo;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        kryo = new Kryo();
        try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream(1024);
             Output output = new Output(byteOutputStream)) {
            kryo.writeClassAndObject(output, msg);
            output.flush();
            out.add(wrappedBuffer(byteOutputStream.toByteArray()));
        }
    }

}
