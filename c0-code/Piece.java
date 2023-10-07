public class Piece {
    public enum PieceType {
        PAWN,
        KNIGHT,
        BISHOP,
        ROOK,
        QUEEN,
        KING
    }

    private PieceType pieceType;
    private final boolean isWhite;
    private int file;
    private int rank;

    public Piece(PieceType type, boolean isWhite, int file, int rank){
        this.pieceType = type;
        this.isWhite = isWhite;
        this.file = file;
        this.rank = rank;
    }

    public int[] getLocation(){
        return new int[]{file, rank};
    }

    public int getFile(){
        return file;
    }

    public int getRank(){
        return rank;
    }

    public void setLocation(int file, int rank){
        this.file = file;
        this.rank = rank;
    }

    public PieceType getPieceType(){
        return pieceType;
    }

    public boolean isWhite(){
        return isWhite;
    }
}
