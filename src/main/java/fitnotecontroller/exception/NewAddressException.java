package fitnotecontroller.exception;


public class NewAddressException extends Exception {

    public NewAddressException(Exception e) {
        super(e);
    }

    public NewAddressException(String s) {
        super(s);
    }
}
