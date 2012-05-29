package org.rmll.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import android.test.suitebuilder.TestSuiteBuilder;

public class AllRMLLTests extends TestSuite {
    public static Test suite() {
        return new TestSuiteBuilder(AllRMLLTests.class).includeAllPackagesUnderHere().build();
    }
}