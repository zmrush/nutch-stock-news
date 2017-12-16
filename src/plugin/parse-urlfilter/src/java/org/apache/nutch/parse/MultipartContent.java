package org.apache.nutch.parse;

import java.io.Serializable;

/**
 * Created by mingzhu7 on 2017/12/14.
 */
public class MultipartContent implements Serializable {
    private static final long serialVersionUID = 364372797603794798L;
    private String contentType = "text/html;charset=utf-8";
    private String text;

    public MultipartContent() {
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
