/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.format.AudioFormat;

/**
 *
 * @author Oleg Kulikov
 */
public class AbstractSinkTest {

    public AbstractSinkTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of connect method, of class AbstractSink.
     */
    @Test
    public void testConnect() throws Exception {
        Format[] formats = new Format[]{
            new AudioFormat("F1")
        };

        TestSource stream = new TestSource(formats);
        TestSink sink = new TestSink(formats);

        sink.connect(stream);

        assertEquals(sink.mediaStream, stream);
        assertEquals(stream.sink, sink);
    }

    /**
     * Test of connect method, of class AbstractSink.
     */
    @Test
    public void testFormatNegotiation() throws Exception {
        Format[] formats1 = new Format[]{
            new AudioFormat("F1")
        };

        Format[] formats2 = new Format[]{
            new AudioFormat("F1"),
            new AudioFormat("F2"),
        };

        TestSource stream = new TestSource(formats1);
        TestSink sink = new TestSink(formats2);

        sink.connect(stream);

        assertEquals(sink.mediaStream, stream);
        assertEquals(stream.sink, sink);
    }

    @Test
    public void testCommonFormats() throws Exception {
        Format[] formats1 = new Format[]{
            new AudioFormat("F1")
        };

        Format[] formats2 = new Format[]{
            new AudioFormat("F1"),
            new AudioFormat("F2"),
        };

        TestSource stream = new TestSource(formats1);
        TestSink sink = new TestSink(formats2);

        sink.connect(stream);

        assertEquals(sink.mediaStream, stream);
        assertEquals(stream.sink, sink);

    }

    @Test
    public void testFormatNegotiationWithWildcardSource() throws Exception {
        Format[] formats1 = new Format[]{
            new AudioFormat("F1")
        };

        Format[] formats2 = new Format[]{
            new AudioFormat("F1"),
            new AudioFormat("F2"),
        };

        TestSource stream = new TestSource(null);
        TestSink sink = new TestSink(formats2);

        sink.connect(stream);

        assertEquals(sink.mediaStream, stream);
        assertEquals(stream.sink, sink);
    }

    @Test
    public void testFormatNegotiationWithWildcardSink() throws Exception {
        Format[] formats1 = new Format[]{
            new AudioFormat("F1")
        };

        Format[] formats2 = new Format[]{
            new AudioFormat("F1"),
            new AudioFormat("F2"),
        };

        TestSource stream = new TestSource(formats1);
        TestSink sink = new TestSink(null);

        sink.connect(stream);

        assertEquals(sink.mediaStream, stream);
        assertEquals(stream.sink, sink);
    }

    /**
     * Test of disconnect method, of class AbstractSink.
     */
    @Test
    public void testDisconnect() {
        Format[] formats = new Format[]{
            new AudioFormat("F1")
        };

        TestSource stream = new TestSource(formats);
        TestSink sink = new TestSink(formats);

        sink.connect(stream);
        sink.disconnect(stream);

        assertEquals(sink.mediaStream, null);
        assertEquals(stream.sink, null);
    }

    private class TestSink extends AbstractSink {

        private Format[] formats;

        public TestSink(Format[] formats) {
        	super("AbstractSinkTest.TestSink");
            this.formats = formats;
        }

        public Format[] getFormats() {
            return formats;
        }

        public boolean isAcceptable(Format format) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void receive(Buffer buffer) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private class TestSource extends AbstractSource {

        private Format[] formats;

        public TestSource(Format[] formats) {
        	super("AbstractSinkTest.TestSource");
            this.formats = formats;
        }

        public void start() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void stop() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Format[] getFormats() {
            return formats;
        }
    }
}