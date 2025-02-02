/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jdesktop.el.ELContext;
import org.jdesktop.el.ELException;
import org.jdesktop.el.ELResolver;
import org.jdesktop.el.Expression;
import org.jdesktop.el.ExpressionFactory;
import org.jdesktop.el.FunctionMapper;
import org.jdesktop.el.MethodExpression;
import org.jdesktop.el.MethodInfo;
import org.jdesktop.el.MethodNotFoundException;
import org.jdesktop.el.PropertyNotFoundException;
import org.jdesktop.el.VariableMapper;

import org.jdesktop.el.impl.lang.EvaluationContext;
import org.jdesktop.el.impl.lang.ExpressionBuilder;
import org.jdesktop.el.impl.parser.Node;
import org.jdesktop.el.impl.util.ReflectionUtil;

/**
 * An <code>Expression</code> that refers to a method on an object.
 * 
 * <p>
 * <code>The {@link ExpressionFactory#createMethodExpression} method
 * can be used to parse an expression string and return a concrete instance
 * of <code>MethodExpression</code> that encapsulates the parsed expression.
 * The {@link FunctionMapper} is used at parse time, not evaluation time, 
 * so one is not needed to evaluate an expression using this class.  
 * However, the {@link ELContext} is needed at evaluation time.</p>
 *
 * <p>The {@link #getMethodInfo} and {@link #invoke} methods will evaluate the 
 * expression each time they are called. The {@link ELResolver} in the 
 * <code>ELContext</code> is used to resolve the top-level variables and to 
 * determine the behavior of the <code>.</code> and <code>[]</code> 
 * operators. For any of the two methods, the {@link ELResolver#getValue} 
 * method is used to resolve all properties up to but excluding the last 
 * one. This provides the <code>base</code> object on which the method
 * appears. If the <code>base</code> object is null, a 
 * <code>NullPointerException</code> must be thrown. At the last resolution, 
 * the final <code>property</code> is then coerced to a <code>String</code>,
 * which provides the name of the method to be found. A method matching the 
 * name and expected parameters provided at parse time is found and it is 
 * either queried or invoked (depending on the method called on this
 * <code>MethodExpression</code>).</p>
 *
 * <p>See the notes about comparison, serialization and immutability in 
 * the {@link Expression} javadocs.
 *
 * @see javax.el.ELResolver
 * @see javax.el.Expression
 * @see javax.el.ExpressionFactory
 * @see javax.el.MethodExpression
 * 
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public final class MethodExpressionImpl extends MethodExpression implements
        Externalizable {

    private Class expectedType;

    private String expr;

    private FunctionMapper fnMapper;

    private VariableMapper varMapper;

    private transient Node node;

    private Class[] paramTypes;

    /**
     * 
     */
    public MethodExpressionImpl() {
        super();
    }

    /**
     * @param expr
     * @param node
     * @param fnMapper
     * @param expectedType
     * @param paramTypes
     */
    public MethodExpressionImpl(String expr, Node node,
            FunctionMapper fnMapper, VariableMapper varMapper,
            Class expectedType, Class[] paramTypes) {
        super();
        this.expr = expr;
        this.node = node;
        this.fnMapper = fnMapper;
        this.varMapper = varMapper;
        this.expectedType = expectedType;
        this.paramTypes = paramTypes;
    }

    /**
     * Determines whether the specified object is equal to this
     * <code>Expression</code>.
     * 
     * <p>
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code>, is an <code>Expression</code> object that is the
     * of the same type (<code>ValueExpression</code> or
     * <code>MethodExpression</code>), and has an identical parsed
     * representation.
     * </p>
     * 
     * <p>
     * Note that two expressions can be equal if their expression Strings are
     * different. For example, <code>${fn1:foo()}</code> and
     * <code>${fn2:foo()}</code> are equal if their corresponding
     * <code>FunctionMapper</code>s mapped <code>fn1:foo</code> and
     * <code>fn2:foo</code> to the same method.
     * </p>
     * 
     * @param obj
     *            the <code>Object</code> to test for equality.
     * @return <code>true</code> if <code>obj</code> equals this
     *         <code>Expression</code>; <code>false</code> otherwise.
     * @see java.util.Hashtable
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return (obj instanceof MethodExpressionImpl && obj.hashCode() == this
                .hashCode());
    }

    /**
     * Returns the original String used to create this <code>Expression</code>,
     * unmodified.
     * 
     * <p>
     * This is used for debugging purposes but also for the purposes of
     * comparison (e.g. to ensure the expression in a configuration file has not
     * changed).
     * </p>
     * 
     * <p>
     * This method does not provide sufficient information to re-create an
     * expression. Two different expressions can have exactly the same
     * expression string but different function mappings. Serialization should
     * be used to save and restore the state of an <code>Expression</code>.
     * </p>
     * 
     * @return The original expression String.
     * 
     * @see javax.el.Expression#getExpressionString()
     */
    public String getExpressionString() {
        return this.expr;
    }

    /**
     * Evaluates the expression relative to the provided context, and returns
     * information about the actual referenced method.
     * 
     * @param context
     *            The context of this evaluation
     * @return an instance of <code>MethodInfo</code> containing information
     *         about the method the expression evaluated to.
     * @throws NullPointerException
     *             if context is <code>null</code> or the base object is
     *             <code>null</code> on the last resolution.
     * @throws PropertyNotFoundException
     *             if one of the property resolutions failed because a specified
     *             variable or property does not exist or is not readable.
     * @throws MethodNotFoundException
     *             if no suitable method can be found.
     * @throws ELException
     *             if an exception was thrown while performing property or
     *             variable resolution. The thrown exception must be included as
     *             the cause property of this exception, if available.
     * @see javax.el.MethodExpression#getMethodInfo(javax.el.ELContext)
     */
    public MethodInfo getMethodInfo(ELContext context)
            throws PropertyNotFoundException, MethodNotFoundException,
            ELException {
        Node n = this.getNode();
        EvaluationContext ctx = new EvaluationContext(context, this.fnMapper,
                this.varMapper, this);
        return n.getMethodInfo(ctx, this.paramTypes);
    }

    /**
     * @return
     * @throws ELException
     */
    private Node getNode() throws ELException {
        if (this.node == null) {
            this.node = ExpressionBuilder.createNode(this.expr);
        }
        return this.node;
    }

    /**
     * Returns the hash code for this <code>Expression</code>.
     * 
     * <p>
     * See the note in the {@link #equals} method on how two expressions can be
     * equal if their expression Strings are different. Recall that if two
     * objects are equal according to the <code>equals(Object)</code> method,
     * then calling the <code>hashCode</code> method on each of the two
     * objects must produce the same integer result. Implementations must take
     * special note and implement <code>hashCode</code> correctly.
     * </p>
     * 
     * @return The hash code for this <code>Expression</code>.
     * @see #equals
     * @see java.util.Hashtable
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.expr.hashCode();
    }

    /**
     * Evaluates the expression relative to the provided context, invokes the
     * method that was found using the supplied parameters, and returns the
     * result of the method invocation.
     * 
     * @param context
     *            The context of this evaluation.
     * @param params
     *            The parameters to pass to the method, or <code>null</code>
     *            if no parameters.
     * @return the result of the method invocation (<code>null</code> if the
     *         method has a <code>void</code> return type).
     * @throws NullPointerException
     *             if context is <code>null</code> or the base object is
     *             <code>null</code> on the last resolution.
     * @throws PropertyNotFoundException
     *             if one of the property resolutions failed because a specified
     *             variable or property does not exist or is not readable.
     * @throws MethodNotFoundException
     *             if no suitable method can be found.
     * @throws ELException
     *             if an exception was thrown while performing property or
     *             variable resolution. The thrown exception must be included as
     *             the cause property of this exception, if available. If the
     *             exception thrown is an <code>InvocationTargetException</code>,
     *             extract its <code>cause</code> and pass it to the
     *             <code>ELException</code> constructor.
     * @see javax.el.MethodExpression#invoke(javax.el.ELContext,
     *      java.lang.Object[])
     */
    public Object invoke(ELContext context, Object[] params)
            throws PropertyNotFoundException, MethodNotFoundException,
            ELException {
        EvaluationContext ctx = new EvaluationContext(context, this.fnMapper,
                this.varMapper, this);
        return this.getNode().invoke(ctx, this.paramTypes, params);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        this.expr = in.readUTF();
        String type = in.readUTF();
        if (!"".equals(type)) {
            this.expectedType = ReflectionUtil.forName(type);
        }
        this.paramTypes = ReflectionUtil.toTypeArray(((String[]) in
                .readObject()));
        this.fnMapper = (FunctionMapper) in.readObject();
        this.varMapper = (VariableMapper) in.readObject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.expr);
        out.writeUTF((this.expectedType != null) ? this.expectedType.getName()
                : "");
        out.writeObject(ReflectionUtil.toTypeNameArray(this.paramTypes));
        out.writeObject(this.fnMapper);
        out.writeObject(this.varMapper);
    }

    public boolean isLiteralText() {
        return false;
    }
}
