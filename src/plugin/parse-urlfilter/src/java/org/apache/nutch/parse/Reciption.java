package org.apache.nutch.parse;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Created by mingzhu7 on 2017/12/14.
 */
public class Reciption implements Serializable {
    private static final long serialVersionUID = -7354158072693675976L;
    private HashSet<String> TO = new HashSet();
    private HashSet<String> CC = new HashSet();
    private HashSet<String> BCC = new HashSet();

    public Reciption() {
    }

    public HashSet<String> getTO() {
        return this.TO;
    }

    public void setTO(HashSet<String> tO) {
        this.TO = tO;
    }

    public HashSet<String> getCC() {
        return this.CC;
    }

    public void setCC(HashSet<String> cC) {
        this.CC = cC;
    }

    public HashSet<String> getBCC() {
        return this.BCC;
    }

    public void setBCC(HashSet<String> bCC) {
        this.BCC = bCC;
    }
}
