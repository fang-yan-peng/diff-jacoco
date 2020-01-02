/*******************************************************************************
 * Copyright (c) 2009, 2019 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.startup;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import org.jacoco.core.tools.ExecDumpClient;
import org.jacoco.core.tools.ExecFileLoader;

/**
 * This example connects to a coverage agent that run in output mode
 * <code>tcpserver</code> and requests execution data. The collected data is
 * dumped to a local file.
 *
 * --git_workdir=xx  --branch=xx --compare-branch=xx (--tag=xx --compare-tag=xx)
 * --source-dirs=xx,xx,xxxx --class-dirs=scs,sdca,sssss --remote-host=xxxxxx --remote-port=8044
 * --exec-dir=xx --report-dir=xxx --mysql-host=xxx --mysql-port=3306 --mysql-user=root
 * --mysql-password=1234
 */
public final class ExecutionDataClient {

    private boolean dump;
    private boolean reset;
    private File destFile;
    private String address;
    private int port;
    private int retryCount;
    private boolean append;

    public ExecutionDataClient(File destFile, String address, int port) {
        this(true, false, destFile, address, port, 5, true);
    }

    public ExecutionDataClient(boolean dump, boolean reset, File destFile, String address, int
            port, int retryCount, boolean append) {
        this.dump = dump;
        this.reset = reset;
        this.destFile = destFile;
        this.address = address;
        this.port = port;
        this.retryCount = retryCount;
        this.append = append;
    }

    public void dump() {
        if (port <= 0) {
            throw new IllegalArgumentException("Invalid port value");
        }
        if (dump && destFile == null) {
            throw new IllegalArgumentException(
                    "Destination file is required when dumping execution data");
        }

        final ExecDumpClient client = new ExecDumpClient() {
            @Override
            protected void onConnecting(final InetAddress address,
                    final int port) {
                System.out.println(format("Connecting to %s:%d", address, port));
            }

            @Override
            protected void onConnectionFailure(final IOException exception) {
                exception.printStackTrace();
            }
        };
        client.setDump(dump);
        client.setReset(reset);
        client.setRetryCount(retryCount);

        try {
            final ExecFileLoader loader = client.dump(address, port);
            if (dump) {
                System.out.println(format("Dumping execution data to %s",
                        destFile.getAbsolutePath()));

                loader.save(destFile, append);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the execution data request.
     *
     * @param args
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        ExecutionDataClient client = new ExecutionDataClient(
                new File("/Users/yanpengfang/Desktop/sq_jacoco/jacoco-client.exec"),
                "localhost",
                8044);
        client.dump();
    }
}
