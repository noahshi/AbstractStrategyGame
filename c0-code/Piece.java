//This class is used to hold all the data about a specific piece in one place
public class Piece {
    public enum PieceType {
        PAWN,
        KNIGHT,
        BISHOP,
        ROOK,
        QUEEN,
        KING
    }
    public Square square;
    
    public final PieceType pieceType;
    public final boolean isWhite;
    public boolean isPinned;

    public Piece(PieceType type, boolean isWhite, Square square){
        this.pieceType = type;
        this.isWhite = isWhite;
        this.square = square;
        isPinned = false;
    }
}
