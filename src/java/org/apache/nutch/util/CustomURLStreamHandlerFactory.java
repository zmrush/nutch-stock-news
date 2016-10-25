package org.apache.nutch.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Created by mingzhu7 on 2016/10/25.
 */
public class CustomURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("shttp".equals(protocol) || "shttps".equals(protocol)) {
            return new CustomURLStreamHandler();
        }

        return null;
    }
    public class CustomURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new CustomURLConnection(url);
        }

    }
    public class CustomURLConnection extends URLConnection {

        protected CustomURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            // Do your job here. As of now it merely prints "Connected!".
            System.out.println("Connected!");
        }

    }

}
