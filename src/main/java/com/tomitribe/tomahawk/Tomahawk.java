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

public final class Tomahawk {
    private Tomahawk() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) throws IOException {
        System.out.println("Tomitribe Tomahawk installed. Will attempt to log TLS secrets");
        instrumentation.addTransformer(new TomahawkTransformer());
    }
}
