package ste.jirarest.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

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
        private final String causeMessage;

        public RequestException(int code, String causeMessage) {
            this.code = code;
            this.causeMessage = causeMessage;
        }

        public int getCode() {
            return this.code;
        }

        @Override
        public String getMessage() {
            String myMsg;
            switch (code) {
                case URI_ERROR: 
                    myMsg = "URI parse error";
                    break;
                case MALFORMED_URL_ERROR:
                    myMsg = "Malformed URL";
                    break;
                case CONNECTION_ERROR:
                    myMsg = "Unable to open connection";
                    break;
                case RESPONSE_ERROR:
                    myMsg = "Unable to read HTTP response";
                    break;
                case HTTP_NOT_OK_ERROR:
                    myMsg = "HTTP response code not 200 OK";
                    break;
                case READ_BODY_ERROR:
                    myMsg = "HTTP body read error";
                    break;
                default:
                    return "Success";
            }

            String realCause = causeMessage != null ? causeMessage : "none";

            return String.format("%s (cause: %s)", myMsg, realCause);
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
        URI uri;
        try{
            uri = new URI(url);
        } catch(URISyntaxException e) {
            throw new RequestException(RequestException.URI_ERROR, e.getMessage());
        }

        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) uri.toURL().openConnection();
        } catch(MalformedURLException e) {
            throw new RequestException(RequestException.MALFORMED_URL_ERROR, e.getMessage());
        } catch(IOException e) {
            throw new RequestException(RequestException.CONNECTION_ERROR, e.getMessage());
        }

        int httpResCode;
        
        try {
            httpResCode = conn.getResponseCode();
        } catch(IOException e) {
            throw new RequestException(RequestException.RESPONSE_ERROR, e.getMessage());
        }

        if(httpResCode != HttpURLConnection.HTTP_OK) {
            throw new RequestException(RequestException.HTTP_NOT_OK_ERROR, null);
        }
        
        try {
            return readAll(conn.getInputStream());
        } catch(IOException e) {
            throw new RequestException(RequestException.READ_BODY_ERROR, e.getMessage());
        }
    }
}
