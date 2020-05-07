/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.XAFlowServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
public class XAFlowTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/XAFlowServlet";

    @Server("com.ibm.ws.transaction_XAFlow")
    @TestServlet(servlet = XAFlowServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.*");

        server.copyFileToLibertyInstallRoot("lib/features/", "features/xaflow-1.0.mf");
        assertTrue("Failed to install xaflow-1.0 manifest",
                   server.fileExistsInLibertyInstallRoot("lib/features/xaflow-1.0.mf"));
        server.copyFileToLibertyInstallRoot("lib/", "bundles/com.ibm.ws.tx.test.impl.jar");
        assertTrue("Failed to install xaflow-1.0 bundle",
                   server.fileExistsInLibertyInstallRoot("lib/com.ibm.ws.tx.test.impl.jar"));
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                server.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
                server.deleteFileFromLibertyInstallRoot("lib/features/xaflow-1.0.mf");
                assertFalse("Failed to uninstall xaflow-1.0 manifest",
                            server.fileExistsInLibertyInstallRoot("lib/features/xaflow-1.0.mf"));
                server.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.tx.test.impl.jar");
                assertFalse("Failed to uninstall xaflow-1.0 bundle",
                            server.fileExistsInLibertyInstallRoot("lib/com.ibm.ws.tx.test.impl.jar"));
                return null;
            }
        });
    }

    @Test
    public void testXAFlow001() throws Exception {
        xaflowTest("001");
    }

    @Test
    public void testXAFlow002() throws Exception {
        xaflowTest("002");
    }

    protected void xaflowTest(String id) throws Exception {
        final String method = "xaflowTest";
        StringBuilder sb = null;
        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server, SERVLET_NAME, "setupXAFlow" + id);
            Log.info(this.getClass(), method, "setupXAFlow" + id + " returned: " + sb);
            fail();
        } catch (Throwable e) {
            Log.info(getClass(), method, "setupXAFlow" + id + " crashed the server as expected: " + e.getLocalizedMessage());
        }

        server.setServerStartTimeout(300000); // 5 mins
        ProgramOutput po = server.startServerAndValidate(false, true, true);
        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());

            // It may be that we attempted to restart the server too soon.
            Log.info(this.getClass(), method, "start server failed, sleep then retry");
            Thread.sleep(30000); // sleep for 30 seconds
            po = server.startServerAndValidate(false, true, true);

            // If it fails again then we'll report the failure
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart the server");
                Log.error(this.getClass(), method, ex);
                throw ex;
            }
        }

        // Server appears to have started ok
        server.waitForStringInTrace("Setting state from RECOVERING to ACTIVE");

        int attempt = 0;
        while (true) {
            Log.info(this.getClass(), method, "calling checkRec" + id);
            try {
                sb = runTestWithResponse(server, SERVLET_NAME, "checkXAFlow" + id);
                Log.info(this.getClass(), method, "checkXAFlow" + id + " returned: " + sb);
                break;
            } catch (Exception e) {
                Log.error(this.getClass(), method, e);
                if (++attempt < 5) {
                    Thread.sleep(10000);
                } else {
                    throw e;
                }
            }
        }
    }
}
