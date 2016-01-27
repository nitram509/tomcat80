/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.coyote.http11.filters;

import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response;
import org.apache.coyote.http11.OutputFilter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.meteogroup.jbrotli.Brotli;
import org.meteogroup.jbrotli.BrotliOutputStream;
import org.scijava.nativelib.NativeLoader;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Brotli output filter.
 *
 * @author Martin W. Kirst
 */
public class BrotliOutputFilter implements OutputFilter {

    static {
        try {
            NativeLoader.loadLibrary("brotli");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Logger.
     */
    private static final org.apache.juli.logging.Log log =
        org.apache.juli.logging.LogFactory.getLog(BrotliOutputFilter.class);


    // ----------------------------------------------------- Instance Variables

    private OutputBuffer nextPipelineBuffer;
    private BrotliOutputStream brotliOutputStream = null;
    private final OutputStream fakeOutputStream = new FakeOutputStream();

    /**
     * Write some bytes.
     *
     * @return number of bytes written by the filter
     */
    @Override
    public int doWrite(ByteChunk chunk, Response res) throws IOException {
        if (brotliOutputStream == null) {
            brotliOutputStream = new BrotliOutputStream(fakeOutputStream, getBrotliParameter());
        }
        brotliOutputStream.write(chunk.getBytes(), chunk.getStart(), chunk.getLength());
        return chunk.getLength();
    }


    @Override
    public long getBytesWritten() {
        return nextPipelineBuffer.getBytesWritten();
    }


    // --------------------------------------------------- OutputFilter Methods

    /**
     * Added to allow flushing to happen for the gzip'ed outputstream
     */
    public void flush() {
        if (brotliOutputStream != null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Flushing the compression stream!");
                }
                brotliOutputStream.flush();
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignored exception while flushing gzip filter", e);
                }
            }
        }
    }

    /**
     * Some filters need additional parameters from the response. All the
     * necessary reading can occur in that method, as this method is called
     * after the response header processing is complete.
     */
    @Override
    public void setResponse(Response response) {
        // NOOP: No need for parameters from response in this filter
    }


    /**
     * Set the next nextPipelineBuffer in the filter pipeline.
     */
    @Override
    public void setBuffer(OutputBuffer buffer) {
        this.nextPipelineBuffer = buffer;
    }


    /**
     * End the current request. It is acceptable to write extra bytes using
     * nextPipelineBuffer.doWrite during the execution of this method.
     */
    @Override
    public long end() throws IOException {
        if (brotliOutputStream == null) {
            brotliOutputStream = new BrotliOutputStream(fakeOutputStream, getBrotliParameter());
        }
        brotliOutputStream.close();
        return ((OutputFilter) nextPipelineBuffer).end();
    }


    /**
     * Make the filter ready to process the next request.
     */
    @Override
    public void recycle() {
        // Set compression stream to null
        brotliOutputStream = null;
    }


    // ------------------------------------------- FakeOutputStream Inner Class


    private class FakeOutputStream extends OutputStream {
        private final ByteChunk outputChunk = new ByteChunk();
        private final byte[] singleByteBuffer = new byte[1];
        @Override
        public void write(int b)
            throws IOException {
            // Shouldn't get used for good performance, but is needed for
            // compatibility with Sun JDK 1.4.0
            singleByteBuffer[0] = (byte) (b & 0xff);
            outputChunk.setBytes(singleByteBuffer, 0, 1);
            nextPipelineBuffer.doWrite(outputChunk, null);
        }
        @Override
        public void write(byte[] b, int off, int len)
            throws IOException {
            outputChunk.setBytes(b, off, len);
            nextPipelineBuffer.doWrite(outputChunk, null);
        }
        @Override
        public void flush() throws IOException {/*NOOP*/}
        @Override
        public void close() throws IOException {/*NOOP*/}
    }


    private Brotli.Parameter getBrotliParameter() {
        Brotli.Parameter defaultParameter = Brotli.DEFAULT_PARAMETER;
        defaultParameter.setQuality(5);
        return defaultParameter;
    }

}
