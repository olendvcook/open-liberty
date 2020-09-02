/*******************************************************************************
 * Copyright (c)  2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ejbcontainer.bindings.configtests.web.DynamicRemoteFeatureServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class DynamicRemoteFeatureTest extends FATServletClient {
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            try {
                //server.dumpServer("serverDump");
            } catch (Exception e1) {
                System.out.println("Failed to dump server");
                e1.printStackTrace();
            }
        }
    };

    private static final Class<?> c = DynamicRemoteFeatureTest.class;
    private static HashSet<String> apps = new HashSet<String>();
    private static String servlet = "ConfigTestsWeb/DynamicRemoteFeatureServlet";

    @Server("com.ibm.ws.ejbcontainer.bindings.fat.server")
    @TestServlet(servlet = DynamicRemoteFeatureServlet.class, contextRoot = "ConfigTestsWeb")
    public static LibertyServer server;

    //@ClassRule
    //public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server"));

    private static String ejbRemoteFeature = null;

    @BeforeClass
    public static void setUp() throws Exception {
        apps.add("ConfigTestsTestApp");

        // Use ShrinkHelper to build the ears

        // -------------- ConfigTestsTestApp ------------
        JavaArchive ConfigTestsEJB = ShrinkHelper.buildJavaArchive("ConfigTestsEJB.jar", "com.ibm.ws.ejbcontainer.bindings.configtests.ejb.");

        WebArchive ConfigTestsWeb = ShrinkHelper.buildDefaultApp("ConfigTestsWeb.war", "com.ibm.ws.ejbcontainer.bindings.configtests.web.");

        EnterpriseArchive ConfigTestsTestApp = ShrinkWrap.create(EnterpriseArchive.class, "ConfigTestsTestApp.ear");
        ConfigTestsTestApp.addAsModules(ConfigTestsEJB, ConfigTestsWeb);
        ShrinkHelper.addDirectory(ConfigTestsTestApp, "test-applications/ConfigTestsTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, ConfigTestsTestApp);

        server.startServer();

        // Store what feature level we are using because of RepeatTest
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        features.forEach((String feature) -> {
            if (feature.toLowerCase().startsWith("ejbremote-")) {
                ejbRemoteFeature = feature;
            }
        });

        Log.info(c, "setUp", "EJB Remote Feature is " + ejbRemoteFeature);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private void disableEJBRemoteFeature() throws Exception {
        String m = "disableEJBRemoteFeature";

        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        boolean remoteEnabled = features.removeIf(feature -> feature.toLowerCase().startsWith("ejbremote-"));
        if (remoteEnabled) {
            Log.info(c, m, "Remote Enabled, disabling");
            server.setMarkToEndOfLog();
            server.changeFeatures(new ArrayList<String>(features));
            server.waitForConfigUpdateInLogUsingMark(apps);
        } else {
            Log.info(c, m, "Did not find remote feature enabled, doing nothing.");
        }
    }

    private void enableEJBRemoteFeature() throws Exception {
        String m = "enableEJBRemoteFeature";

        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        boolean remoteEnabled = features.removeIf(feature -> feature.toLowerCase().startsWith("ejbremote-"));
        if (remoteEnabled) {
            Log.info(c, m, "Found remote feature enabled already, doing nothing.");
        } else {
            Log.info(c, m, "Remote disabled, enabling");
            server.setMarkToEndOfLog();
            features.add(ejbRemoteFeature);
            //features.add("ejbRemote-3.2");
            server.changeFeatures(new ArrayList<String>(features));
            server.waitForConfigUpdateInLogUsingMark(apps);
        }
    }

    @Test
    public void testDynamicRemote() throws Exception {
        enableEJBRemoteFeature();

        //FATServletClient.runTest(server, servlet, "testRemoteEnabled");

        disableEJBRemoteFeature();
        enableEJBRemoteFeature();
        disableEJBRemoteFeature();
        enableEJBRemoteFeature();
        disableEJBRemoteFeature();
        enableEJBRemoteFeature();
        disableEJBRemoteFeature();
        enableEJBRemoteFeature();

        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.runFinalization();

        //server.javadumpThreads("--include:system");

        FATServletClient.runTest(server, servlet, "testRemoteEnabled");
    }

    //@Test
    public void testDisabledEnabled() throws Exception {
        //enableEJBRemoteFeature();

        FATServletClient.runTest(server, servlet, "testRemoteEnabled");
    }
}
