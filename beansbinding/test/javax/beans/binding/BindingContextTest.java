/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import junit.framework.*;
import com.sun.java.util.BindingCollections;
import com.sun.java.util.ObservableMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sky
 */
public class BindingContextTest extends TestCase {
    private BindingContext context;
    private TestBean source;
    private TestBean target;
    
    public BindingContextTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(BindingContextTest.class);
        
        return suite;
    }

    protected void setUp() throws Exception {
        super.setUp();
        context = new BindingContext();
        source = new TestBean();
        target = new TestBean();
    }
    
    public void testGetFeatureDescriptors() throws Throwable {
        verifyDescriptors(JCheckBox.class, "selected");
        verifyDescriptors(JComboBox.class, "elements", "selectedElement", 
                "selectedElementProperty");
        verifyDescriptors(JList.class, "elements", "selectedElement", 
                "selectedElements");
        verifyDescriptors(JSlider.class, "value");
        verifyDescriptors(JTable.class, "elements", "selectedElement", 
                "selectedElements");
        verifyDescriptors(JTextField.class, "text");
        verifyDescriptors(JTree.class, "root", "selectedElement", 
                "selectedElements");
    }
    
    public void testBind1() {
        context.addBinding(source, "${value}", target, "value");
        context.bind();
        source.setValue("x");
        assertEquals("x", source.getValue());
        assertEquals("x", target.getValue());

        target.setValue("y");
        assertEquals("y", source.getValue());
        assertEquals("y", target.getValue());
    }

    public void testBindAndUnbind() {
        context.addBinding(source, "${value}", target, "value");
        context.bind();
        context.unbind();
        source.setValue("source");
        assertEquals("source", source.getValue());
        assertEquals(null, target.getValue());
        context.bind();
        source.setValue("source2");
        assertEquals("source2", source.getValue());
        assertEquals("source2", target.getValue());
    }

    public void testHasUncommited() {
        EventListenerRecorder<PropertyChangeListener> recorder =
                new EventListenerRecorder<PropertyChangeListener>(PropertyChangeListener.class);
        context.addPropertyChangeListener(recorder.getEventListenerImpl());
        Binding binding = context.addBinding(source, "${value}", target, "value");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ_FROM_SOURCE);
        context.bind();
        recorder.getAndClearRecords();
        target.setValue("x");
        assertPropertyChanges(recorder.getAndClearRecords(),
                "hasUncommittedValues", Boolean.TRUE,
                "hasEditedTargetValues", Boolean.TRUE);
        assertTrue(context.getHasUncommittedValues());
        assertTrue(context.getHasEditedTargetValues());

        context.clearHasEditedTargetValues();
        assertFalse(context.getHasEditedTargetValues());
        context.commitUncommittedValues();
        assertFalse(context.getHasEditedTargetValues());
        assertPropertyChanges(recorder.getAndClearRecords(),
                "hasUncommittedValues", Boolean.FALSE,
                "hasEditedTargetValues", Boolean.FALSE);
        assertEquals("x", source.getValue());
        assertEquals("x", target.getValue());
        assertFalse(context.getHasUncommittedValues());
    }

    public void testHasInvalidValues() {
        EventListenerRecorder<BindingListener> validationRecorder =
                new EventListenerRecorder<BindingListener>(BindingListener.class);
        EventListenerRecorder<PropertyChangeListener> recorder =
                new EventListenerRecorder<PropertyChangeListener>(PropertyChangeListener.class);
        context.addPropertyChangeListener(recorder.getEventListenerImpl());
        context.addBindingListener(validationRecorder.getEventListenerImpl());
        List<EventListenerRecorder.InvocationRecord> records;
        PropertyChangeEvent e;
        ValidatorImpl validator = new ValidatorImpl(ValidationResult.Action.DO_NOTHING);
        Binding binding = context.addBinding(source, "${value}", target, "value");
        binding.setValidator(validator);
        context.bind();
        recorder.getAndClearRecords();
        validationRecorder.getAndClearRecords();
        target.setValue("x");
        assertPropertyChanges(recorder.getAndClearRecords(),
                "hasInvalidValues", Boolean.TRUE,
                "hasEditedTargetValues", Boolean.TRUE);
        assertEquals(1, validationRecorder.getAndClearRecords().size());
        assertFalse(context.getHasUncommittedValues());
        assertTrue(context.getHasInvalidValues());
    }

    private void assertPropertyChanges(
            List<EventListenerRecorder.InvocationRecord> records, 
            Object...args) {
        assertEquals(0, args.length % 2);
        assertEquals(args.length / 2, records.size());
        Map<String,Object> expectedMap = new HashMap<String,Object>();
        for (int i = 0; i < args.length; i += 2) {
            expectedMap.put((String)args[i], args[i + 1]);
        }
        for (EventListenerRecorder.InvocationRecord record : records) {
            PropertyChangeEvent event = (PropertyChangeEvent)record.
                    getArgs().get(0);
            String propertyName = event.getPropertyName();
            assertTrue(expectedMap.containsKey(propertyName));
            assertEquals(expectedMap.get(propertyName), event.getNewValue());
            expectedMap.remove(propertyName);
        }
    }

    private void verifyDescriptors(Class type, String...properties) throws 
            Throwable {
        Set<String> propSet = new HashSet<String>();
        for (String property : properties) {
            propSet.add(property);
        }
        for (FeatureDescriptor d : context.getFeatureDescriptors(type.newInstance())) {
            propSet.remove(d.getName());
        }
        assertEquals("expecting=" + Arrays.asList(properties) +
                " remaining=" + propSet, 0, propSet.size());
    }
}
