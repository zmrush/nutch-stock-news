package org.apache.nutch.parse;

import java.io.Serializable;

/**
 * Created by mingzhu7 on 2017/12/14.
 */
public class Subject implements Serializable {
    private static final long serialVersionUID = 1565434815759937614L;
    private String charset = "UTF8";
    private String encode = "B";
    private String text;

    public Subject() {
    }

    public String getCharset() {
        return this.charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getEncode() {
        return this.encode;
    }

    public void setEncode(String encode) {
        this.encode = encode;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
