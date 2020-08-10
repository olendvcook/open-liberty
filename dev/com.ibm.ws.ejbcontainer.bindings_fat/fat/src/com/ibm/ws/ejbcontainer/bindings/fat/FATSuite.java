/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.bindings.fat.tests.DynamicRemoteFeatureTest;

@RunWith(Suite.class)
@SuiteClasses({
//                BindingNameTest.class,
//                BindToJavaGlobalTest.class,
//                BindToServerRootTest.class,
//                BndErrorTest.class,
//                ComponentIDTest.class,
//                CustomBindingsTest.class,
//                DefaultBindingsTest.class,
//                DisableShortBndTest.class,
                DynamicRemoteFeatureTest.class
//                EJBinWARBindingsTest.class,
//                HomeBindingNameTest.class,
//                JNDINameTest.class,
//                NoInterfaceBindingsTest.class,
//                SimpleBindingNameTest.class

})

public class FATSuite {}
