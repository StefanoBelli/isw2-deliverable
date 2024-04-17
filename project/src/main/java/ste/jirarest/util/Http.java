package ste.jirarest.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Http {
    private Http() {}

    public static final class RequestException extends Exception {
        public static final int URI_ERROR = 1;
        public static final int MALFORMED_URL_ERROR = 2;
        public static final int CONNECTION_ERROR = 3;
        public static final int RESPONSE_ERROR = 4;
        public static final int HTTP_NOT_OK_ERROR = 5;
        public static final int READ_BODY_ERROR = 6;

        private final int code;
        
        public RequestException(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        @Override
        public String getMessage() {
            switch (code) {
                case URI_ERROR:
                    return "URI parse error";
                case MALFORMED_URL_ERROR:
                    return "Malformed URL";
                case CONNECTION_ERROR:
                    return "Unable to open connection";
                case RESPONSE_ERROR:
                    return "Unable to read HTTP response";
                case HTTP_NOT_OK_ERROR:
                    return "HTTP response code not 200 OK";
                case READ_BODY_ERROR:
                    return "HTTP body read error";
                default:
                    return "Success";
            }
        }
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();

        try(BufferedReader reader = 
                new BufferedReader(
                    new InputStreamReader(in))) {
            
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        }

        return builder.toString();
    }

    public static String get(String url) throws RequestException {
        Logger logger = Logger.getLogger("HttpGet");

        URI uri;
        try{
            uri = new URI(url);
        } catch(URISyntaxException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new RequestException(RequestException.URI_ERROR);
        }

        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) uri.toURL().openConnection();
        } catch(MalformedURLException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new RequestException(RequestException.MALFORMED_URL_ERROR);
        } catch(IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new RequestException(RequestException.CONNECTION_ERROR);
        }

        int httpResCode;
        
        try {
            httpResCode = conn.getResponseCode();
        } catch(IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new RequestException(RequestException.RESPONSE_ERROR);
        }

        if(httpResCode != HttpURLConnection.HTTP_OK) {
            logger.log(Level.SEVERE, "HTTP response code: {0}", httpResCode);
            throw new RequestException(RequestException.HTTP_NOT_OK_ERROR);
        }
        
        try {
            return readAll(conn.getInputStream());
        } catch(IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new RequestException(RequestException.READ_BODY_ERROR);
        }
    }
}
