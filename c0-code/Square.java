//This class is used to hold the location information on the chess board
public class Square {
    public final int file;
    public final int rank;
    public Square(int file, int rank){
        this.file = file;
        this.rank = rank;
    }

    public String toString(){
        return (char)(file + (int)'a') + "" + (8 - rank);
    }
}
