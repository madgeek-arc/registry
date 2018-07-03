package eu.openminted.registry.core.domain;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.URL;
import java.util.Scanner;

public class UrlResolver {

    private static Logger logger = LogManager.getLogger(UrlResolver.class);

    public static String getText(String url) throws Exception {
        String out = new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next();
        if (out == null || out.isEmpty()) {
            return null;
        } else {
            return out;
        }
    }
}

		

