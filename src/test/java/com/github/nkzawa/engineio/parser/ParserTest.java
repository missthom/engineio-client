package com.github.nkzawa.engineio.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.github.nkzawa.engineio.parser.Parser.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ParserTest {

    static final String ERROR_DATA = "parser error";

    @Test
    public void encodeAsString() {
        encodePacket(new Packet<String>(Packet.MESSAGE, "test"), new EncodeCallback() {
            @Override
            public void call(Object data) {
                assertThat(data, instanceOf(String.class));
            }
        });
    }

    @Test
    public void decodeAsPacket() {
        encodePacket(new Packet<String>(Packet.MESSAGE, "test"), new EncodeCallback() {
            @Override
            public void call(Object data) {
                assertThat(decodePacket(data), isA(Packet.class));
            }
        });
    }

    @Test
    public void noData() {
        encodePacket(new Packet(Packet.MESSAGE), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet p = decodePacket(data);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is(nullValue()));
            }
        });
    }

    @Test
    public void encodeOpenPacket() {
        encodePacket(new Packet<String>(Packet.OPEN, "{\"some\":\"json\"}"), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet<String> p = decodePacket(data);
                assertThat(p.type, is(Packet.OPEN));
                assertThat(p.data, is("{\"some\":\"json\"}"));
            }
        });
    }

    @Test
    public void encodeClosePacket() {
        encodePacket(new Packet(Packet.CLOSE), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet p = decodePacket(data);
                assertThat(p.type, is(Packet.CLOSE));
            }
        });
    }

    @Test
    public void encodePingPacket() {
        encodePacket(new Packet<String>(Packet.PING, "1"), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet<String> p = decodePacket(data);
                assertThat(p.type, is(Packet.PING));
                assertThat(p.data, is("1"));
            }
        });
    }

    @Test
    public void encodePongPacket() {
        encodePacket(new Packet<String>(Packet.PONG, "1"), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet<String> p = decodePacket(data);
                assertThat(p.type, is(Packet.PONG));
                assertThat(p.data, is("1"));
            }
        });
    }

    @Test
    public void encodeMessagePacket() {
        encodePacket(new Packet<String>(Packet.MESSAGE, "aaa"), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet<String> p = decodePacket(data);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is("aaa"));
            }
        });
    }

    @Test
    public void encodeUTF8SpecialCharsMessagePacket() {
        encodePacket(new Packet<String>(Packet.MESSAGE, "utf8 ??? string"), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet<String> p = decodePacket(data);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is("utf8 ??? string"));
            }
        });
    }

    @Test
    public void encodeMessagePacketCoercingToString() {
        encodePacket(new Packet<Integer>(Packet.MESSAGE, 1), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet<String> p = decodePacket(data);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is("1"));
            }
        });
    }

    @Test
    public void encodeUpgradePacket() {
        encodePacket(new Packet(Packet.UPGRADE), new EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet p = decodePacket(data);
                assertThat(p.type, is(Packet.UPGRADE));
            }
        });
    }

    @Test
    public void encodingFormat() {
        encodePacket(new Packet<String>(Packet.MESSAGE, "test"), new EncodeCallback<String>() {
            @Override
            public void call(String data) {
                assertThat(data.matches("[0-9].*"), is(true));
            }
        });
        encodePacket(new Packet<String>(Packet.MESSAGE), new EncodeCallback<String>() {
            @Override
            public void call(String data) {
                assertThat(data.matches("[0-9]"), is(true));
            }
        });
    }

    @Test
    public void decodeBadFormat() {
        Packet<String> p = decodePacket(":::");
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void decodeInexistentTypes() {
        Packet<String> p = decodePacket("94103");
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void decodeInvalidUTF8() {
        Packet<String> p = decodePacket("4\uffff", true);
        assertThat(p.type, is(Packet.ERROR));
        assertThat(p.data, is(ERROR_DATA));
    }

    @Test
    public void encodePayloadsAsString() {
        encodePayload(new Packet[]{new Packet(Packet.PING), new Packet(Packet.PONG)}, new EncodeCallback() {
            @Override
            public void call(Object data) {
                assertThat(data, instanceOf(String.class));
            }
        });
    }

    @Test
    public void encodeAndDecodePayloads() {
        encodePayload(new Packet[] {new Packet<String>(Packet.MESSAGE, "a")}, new EncodeCallback() {
            @Override
            public void call(Object data) {
                decodePayload(data, new DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        boolean isLast = index + 1 == total;
                        assertThat(isLast, is(true));
                        return true;
                    }
                });
            }
        });
        encodePayload(new Packet[]{new Packet<String>(Packet.MESSAGE, "a"), new Packet(Packet.PING)}, new EncodeCallback() {
            @Override
            public void call(Object data) {
                decodePayload(data, new DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        boolean isLast = index + 1 == total;
                        if (!isLast) {
                            assertThat(packet.type, is(Packet.MESSAGE));
                        } else {
                            assertThat(packet.type, is(Packet.PING));
                        }
                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void encodeAndDecodeEmptyPayloads() {
        encodePayload(new Packet[] {}, new EncodeCallback() {
            @Override
            public void call(Object data) {
                decodePayload(data, new DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        assertThat(packet.type, is(Packet.OPEN));
                        boolean isLast = index + 1 == total;
                        assertThat(isLast, is(true));
                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void decodePayloadBadFormat() {
        decodePayload("1!", new DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
        decodePayload("", new DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
        decodePayload("))", new DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
    }

    @Test
    public void decodePayloadBadLength() {
        decodePayload("1:", new DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
    }

    @Test
    public void decodePayloadBadPacketFormat() {
        decodePayload("3:99:", new DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
        decodePayload("1:aa", new DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
        decodePayload("1:a2:b", new DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
    }

    @Test
    public void decodePayloadInvalidUTF8() {
        decodePayload("2:4\uffff", new DecodePayloadCallback<String>() {
            @Override
            public boolean call(Packet<String> packet, int index, int total) {
                boolean isLast = index + 1 == total;
                assertThat(packet.type, is(Packet.ERROR));
                assertThat(packet.data, is(ERROR_DATA));
                assertThat(isLast, is(true));
                return true;
            }
        });
    }

    @Test
    public void encodeBinaryMessage() {
        final byte[] data = new byte[5];
        for (int i = 0; i < data.length; i++) {
            data[0] = (byte)i;
        }
        encodePacket(new Packet<byte[]>(Packet.MESSAGE, data), new EncodeCallback() {
            @Override
            public void call(Object encoded) {
                Packet<byte[]> p = decodePacket(encoded);
                assertThat(p.type, is(Packet.MESSAGE));
                assertThat(p.data, is(data));
            }
        });
    }

    @Test
    public void encodeBinaryContents() {
        final byte[] firstBuffer = new byte[5];
        for (int i = 0 ; i < firstBuffer.length; i++) {
            firstBuffer[0] = (byte)i;
        }
        final byte[] secondBuffer = new byte[4];
        for (int i = 0 ; i < secondBuffer.length; i++) {
            secondBuffer[0] = (byte)(firstBuffer.length + i);
        }

        encodePayload(new Packet[]{
            new Packet<byte[]>(Packet.MESSAGE, firstBuffer),
            new Packet<byte[]>(Packet.MESSAGE, secondBuffer),
        }, new EncodeCallback() {
            @Override
            public void call(Object data) {
                decodePayload(data, new DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        boolean isLast = index + 1 == total;
                        assertThat(packet.type, is(Packet.MESSAGE));
                        if (!isLast) {
                            assertThat((byte[])packet.data, is(firstBuffer));
                        } else {
                            assertThat((byte[])packet.data, is(secondBuffer));
                        }
                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void encodeMixedBinaryAndStringContentsAsBase64() {
        final byte[] data = new byte[5];
        for (int i = 0 ; i < data.length; i++) {
            data[i] = (byte)i;
        }
        encodePayload(new Packet[]{
                new Packet<byte[]>(Packet.MESSAGE, data),
                new Packet<String>(Packet.MESSAGE, "hello")
        }, new EncodeCallback() {
            @Override
            public void call(Object encoded) {
                decodePayload(encoded, new DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        boolean isLast = index + 1 == total;
                        assertThat(packet.type, is(Packet.MESSAGE));
                        if (!isLast) {
                            assertThat((byte[])packet.data, is(data));
                        } else {
                            assertThat((String)packet.data, is("hello"));
                        }
                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void encodeMixedBinaryAndStringContentsAsBinary() {
        final byte[] firstBuffer = new byte[123];
        for (int i = 0 ; i < firstBuffer.length; i++) {
            firstBuffer[i] = (byte)i;
        }
        encodePayloadAsBinary(new Packet[]{
            new Packet<byte[]>(Packet.MESSAGE, firstBuffer),
            new Packet<String>(Packet.MESSAGE, "hello"),
            new Packet<String>(Packet.CLOSE),
        }, new EncodeCallback<byte[]>() {
            @Override
            public void call(byte[] encoded) {
                decodePayload(encoded, new DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        if (index == 0) {
                            assertThat(packet.type, is(Packet.MESSAGE));
                            assertThat((byte[])packet.data, is(firstBuffer));
                        } else if (index == 1) {
                            assertThat(packet.type, is(Packet.MESSAGE));
                            assertThat((String)packet.data, is("hello"));
                        } else {
                            assertThat(packet.type, is(Packet.CLOSE));
                        }
                        return true;
                    }
                });
            }
        });
    }
}
