package printer;


public class NoUsefulException extends Exception {
      //Parameterless Constructor
      public NoUsefulException() {}

      //Constructor that accepts a message
      public NoUsefulException(String message)
      {
         super(message);
      }
}