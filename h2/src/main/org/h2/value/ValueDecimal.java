/*
 * Copyright 2004-2006 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.value;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.h2.message.Message;
import org.h2.util.MathUtils;

/**
 * @author Thomas
 */
public class ValueDecimal extends Value {
    // TODO doc: document differences for BigDecimal 1.5 <> 1.4
    private BigDecimal value;
    private String valueString;
    private int precision;

    private static final BigDecimal DEC_ZERO = new BigDecimal("0");
    private static final BigDecimal DEC_ONE = new BigDecimal("1");
    private static final ValueDecimal ZERO = new ValueDecimal(DEC_ZERO);
    private static final ValueDecimal ONE = new ValueDecimal(DEC_ONE);
    
    public static final int DEFAULT_PRECISION = 65535;
    public static final int DEFAULT_SCALE = 32767;
    private static final int DIVIDE_SCALE_ADD = 25;

    private ValueDecimal(BigDecimal value) {
        if (value == null) {
            throw new NullPointerException();
        }
        this.value = value;
    }

    public Value add(Value v) {
        ValueDecimal dec = (ValueDecimal) v;
        return ValueDecimal.get(value.add(dec.value));
    }

    public Value subtract(Value v) {
        ValueDecimal dec = (ValueDecimal) v;
        return ValueDecimal.get(value.subtract(dec.value));
    }

    public Value negate() {
        return ValueDecimal.get(value.negate());
    }

    public Value multiply(Value v) {
        ValueDecimal dec = (ValueDecimal) v;
        return ValueDecimal.get(value.multiply(dec.value));
    }

    public Value divide(Value v) throws SQLException {
        ValueDecimal dec = (ValueDecimal) v;
        // TODO value: divide decimal: rounding?
        if (dec.value.signum() == 0) {
            throw Message.getSQLException(Message.DIVISION_BY_ZERO_1, getSQL());
        }
        BigDecimal bd = value.divide(dec.value, value.scale()+DIVIDE_SCALE_ADD, BigDecimal.ROUND_HALF_DOWN);
        if(bd.signum()==0) {
            bd = DEC_ZERO;
        } else if(bd.scale()>0) {
            if(!bd.unscaledValue().testBit(0)) {
                String s = bd.toString();
                int i=s.length() - 1;
                while(i>=0 && s.charAt(i) == '0') {
                    i--;
                }
                if(i < s.length() - 1) {
                    s = s.substring(0, i+1);
                    bd = new BigDecimal(s);
                }
            }
        }
        return ValueDecimal.get(bd);
    }

    public String getSQL() {
        return getString();
    }

    public int getType() {
        return Value.DECIMAL;
    }

    protected int compareSecure(Value o, CompareMode mode) {
        ValueDecimal v = (ValueDecimal) o;
        int c = value.compareTo(v.value);
        return c == 0 ? 0 : (c < 0 ? -1 : 1);
    }

    public int getSignum() {
        return value.signum();
    }

    public BigDecimal getBigDecimal() {
        return value;
    }

    public String getString() {
        if(valueString == null) {
            valueString = value.toString();
        }
        return valueString;
    }

    public long getPrecision() {
        if(precision == 0) {
            precision = value.unscaledValue().abs().toString().length();
        }
        return precision;
    }

    public int getScale() {
        return value.scale();
    }

    public int hashCode() {
        return value.hashCode();
    }

    public Object getObject() {
        return value;
    }

    public void set(PreparedStatement prep, int parameterIndex) throws SQLException {
        prep.setBigDecimal(parameterIndex, value);
    }

    public Value convertScale(boolean onlyToSmallerScale, int targetScale) throws SQLException {
        if (value.scale() == targetScale) {
            return this;
        }
        if (onlyToSmallerScale || targetScale >= DEFAULT_SCALE) {
            if (value.scale() < targetScale) {
                return this;
            }
        }
        BigDecimal bd = MathUtils.setScale(value, targetScale);
        return ValueDecimal.get(bd);
    }

    public Value convertPrecision(long precision) throws SQLException {
        if (getPrecision() <= precision) {
            return this;
        }
        throw Message.getSQLException(Message.VALUE_TOO_LARGE_FOR_PRECISION_1, "" + precision);
    }

    public static ValueDecimal get(BigDecimal dec) {
        if (DEC_ZERO.equals(dec)) {
            return ZERO;
        } else if (DEC_ONE.equals(dec)) {
            return ONE;
        }
        // TODO value optimization: find a way to find out size of bigdecimal,
        // check max cache size
        return (ValueDecimal) Value.cache(new ValueDecimal(dec));
    }

//    public String getJavaString() {
//        return toString();
//    }
    
    public int getDisplaySize() {
        // TODO displaySize: this is probably very slow
        return getString().length();
    }    
    
    protected boolean isEqual(Value v) {
        return v instanceof ValueDecimal && value.equals(((ValueDecimal)v).value);
    }    

}
