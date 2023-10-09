public class Piece {
    public enum PieceType {
        PAWN,
        KNIGHT,
        BISHOP,
        ROOK,
        QUEEN,
        KING
    }

    public PieceType pieceType;
    public Square square;

    public final boolean isWhite;

    public Piece(PieceType type, boolean isWhite, Square square){
        this.pieceType = type;
        this.isWhite = isWhite;
        this.square = square;
    }
}
