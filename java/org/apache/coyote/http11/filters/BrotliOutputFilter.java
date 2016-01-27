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


    private OutputBuffer nextPipelineBuffer;
    private BrotliOutputStream brotliOutputStream = null;
    private final OutputStream outputStream2ByteChunkAdapter = new OutputStream2ByteChunkAdapter();


    @Override
    public int doWrite(ByteChunk chunk, Response res) throws IOException {
        if (brotliOutputStream == null) {
            brotliOutputStream = new BrotliOutputStream(outputStream2ByteChunkAdapter, getBrotliParameter());
        }
        brotliOutputStream.write(chunk.getBytes(), chunk.getStart(), chunk.getLength());
        return chunk.getLength();
    }


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


    @Override
    public long end() throws IOException {
        if (brotliOutputStream != null) {
            brotliOutputStream.close();
            brotliOutputStream = null;
        }
        return ((OutputFilter) nextPipelineBuffer).end();
    }


    @Override
    public void recycle() {
        // Set compression stream to null
        brotliOutputStream = null;
    }


    @Override
    public long getBytesWritten() {
        return nextPipelineBuffer.getBytesWritten();
    }


    @Override
    public void setBuffer(OutputBuffer buffer) {
        this.nextPipelineBuffer = buffer;
    }


    @Override
    public void setResponse(Response response) {
        // not needed by BrotliOutputFilter
    }


    private Brotli.Parameter getBrotliParameter() {
        Brotli.Parameter defaultParameter = Brotli.DEFAULT_PARAMETER;
        defaultParameter.setQuality(4);
        return defaultParameter;
    }


    private class OutputStream2ByteChunkAdapter extends OutputStream {

        private final ByteChunk outputChunk = new ByteChunk();

        @Override
        public void write(int b) throws IOException {
            nextPipelineBuffer.doWrite(outputChunk, null);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            outputChunk.setBytes(b, off, len);
            nextPipelineBuffer.doWrite(outputChunk, null);
        }

        @Override
        public void flush() throws IOException {/*NOOP*/}
        @Override
        public void close() throws IOException {/*NOOP*/}

    }

}
