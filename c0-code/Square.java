import java.security.*;

public class Square {
    public final int file;
    public final int rank;
    public Square(int file, int rank){
        /*if(file > 7 || file < 0 || rank > 7 || rank < 0){
            throw new InvalidParameterException("Ranks and files must be 0 to 7 inclusive.");
        }*/
        this.file = file;
        this.rank = rank;
    }
}
