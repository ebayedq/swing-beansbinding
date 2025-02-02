/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding.outside;

import junit.framework.TestCase;
import org.jdesktop.beansbinding.*;
import org.junit.Ignore;

/**
 * Tests to ensure that {@code ELProperty} correctly exposes properties.
 * It should only expose access to methods in public classes, or where the
 * methods override versions from a public superclass.
 *
 * @author Shannon Hickey
 */
@Ignore
public class ELPropertyAccessTest extends TestCase {

    private Property valP = ELProperty.create("${value}");

    public class Public_Reader {
        public int getValue() {
            return 10;
        }
    }

    public class Public_Writer {
        public void setValue(int value) {
        }
    }

    public class Public_ReaderWriter {
        public int getValue() {
            return 10;
        }

        public void setValue(int value) {
        }
    }

    private class Private_ReaderWriter {
        public int getValue() {
            return 10;
        }
        
        public void setValue(int value) {
        }
    }

    private class Private_Extends_Public_Reader extends Public_Reader {
    }

    private class Private_Extends_Public_Writer extends Public_Writer {
    }

    private class Private_Extends_Public_ReaderWriter extends Public_ReaderWriter {
        public int getValue() {
            return 20;
        }
    }

    protected void setUp() {
    }

    protected void tearDown() {
    }

    private void doCheck(Object obj, boolean expectedReadable, boolean expectedWriteable, int expectedValue) {
        assertEquals(expectedReadable, valP.isReadable(obj));
        assertEquals(expectedWriteable, valP.isWriteable(obj));

        if (expectedReadable) {
            assertEquals(expectedValue, valP.getValue(obj));
        } else {
            try {
                valP.getValue(obj);
                fail();
            } catch (UnsupportedOperationException uoe) {
            }
        }

        if (expectedWriteable) {
            assertEquals(Integer.TYPE, valP.getWriteType(obj));
            valP.setValue(obj, 10);
        } else {
            try {
                valP.getWriteType(obj);
                fail();
            } catch (UnsupportedOperationException uoe) {
            }

            try {
                valP.setValue(obj, 10);
                fail();
            } catch (UnsupportedOperationException uoe) {
            }
        }
        
    }

    public void test_Public_Reader() {
        doCheck(new Public_Reader(), true, false, 10);
    }

    public void test_Public_Writer() {
        // Currently failing since BeanELResolver currently excludes properties
        // that aren't writable. This needs to be fixed.
        doCheck(new Public_Writer(), false, true, -1);
    }

    public void test_Public_ReaderWriter() {
        doCheck(new Public_ReaderWriter(), true, true, 10);
    }
    
    public void test_Private_ReaderWriter() {
        doCheck(new Private_ReaderWriter(), false, false, -1);
    }

    public void test_Private_Extends_Public_Reader() {
        doCheck(new Private_Extends_Public_Reader(), true, false, 10);
    }

    public void test_Private_Extends_Public_Writer() {
        // Currently failing since BeanELResolver currently excludes properties
        // that aren't writable. This needs to be fixed.
        doCheck(new Private_Extends_Public_Writer(), false, true, -1);
    }

    public void test_Private_Extends_Public_ReaderWriter() {
        doCheck(new Private_Extends_Public_ReaderWriter(), true, true, 20);
    }

}
