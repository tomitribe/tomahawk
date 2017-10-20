/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2016
 *
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *
 */
package com.tomitribe.tomahawk;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.JarURLConnection;
import java.util.Date;

public final class TomahawkAgent {
    private TomahawkAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) throws IOException {
        System.out.println(String.format("%s TomahawkAgent: TomahawkAgent installed. Will attempt to log TLS secrets",
                                         new Date().toString()));
        JarURLConnection connection = (JarURLConnection) TomahawkAgent.class.getResource("TomahawkAgent.class").openConnection();
        instrumentation.appendToBootstrapClassLoaderSearch(connection.getJarFile());
        instrumentation.addTransformer(new TomahawkTransformer());
    }
}
