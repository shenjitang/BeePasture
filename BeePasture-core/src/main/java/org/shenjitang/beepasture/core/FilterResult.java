/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.beepasture.core;

/**
 *
 * @author xiaolie
 */
public class FilterResult {

    public final static String MUST = "must";
    public final static String MUST_NOT = "mustnot";
    public final static String NOT = "not";
    public final static String SHOULD = "should";
    public final static String SHOULD_NOT = "shouldnot";
    
    private boolean value;
    private String action;

    public FilterResult() {
    }

    public FilterResult(boolean value, String action) {
        this.value = value;
        if (action == null) {
            this.action = MUST;
        } else {
            this.action = action;
        }
    }
    
    public static String valueOfAction(String value) {
        if (MUST.equals(value)) return MUST;
        else if (MUST_NOT.equals(value)) return MUST_NOT;
        else if (NOT.equals(value)) return MUST_NOT;
        else if (SHOULD.equals(value)) return SHOULD;
        else if (SHOULD_NOT.equals(value)) return SHOULD_NOT;
        else return null;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
    
    public boolean isKeep() {
        switch (action) {
            case MUST_NOT:
            case NOT:
            case SHOULD_NOT:
                return !value;
            case MUST:
            case SHOULD:
                return value;
            default:
                throw new RuntimeException("action:" + action + " is illegal!");
        }        
    }
    
}
