package framework;

public class CustomException {

    public static class BuildException extends Exception {
        public BuildException(String message) {
            super(message);
        }
    }

    public static class RequestException extends Exception {

        public RequestException(String message) {
            super(message);
        }
    }
}