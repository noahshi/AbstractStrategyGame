import java.security.InvalidParameterException;
import java.util.*;

public class Board {
    public final int BOARD_SIZE = 8;
    public Piece[][] board = new Piece[BOARD_SIZE][BOARD_SIZE];
    public List<Piece> whitePieces = new ArrayList<>();
    public List<Piece> blackPieces = new ArrayList<>();
    public Map<Piece, Set<Square>> whiteControlledSquares = new HashMap<>();
    public Map<Piece, Set<Square>> blackControlledSquares = new HashMap<>();

    public boolean[] whiteCastlingRights = new boolean[2];
    public boolean[] blackCastlingRights = new boolean[2];

    public boolean[] playerInCheck = new boolean[]{false, false};

    public boolean whiteTurn;
    public Square enPassantSquare;
    public int fiftyMoveRuleCounter;
    public int moveNumber;
    
    public String pgn = "";

    public Square whiteKingSquare;
    public Square blackKingSquare;

    public Board(String fen){

        fen = "rnbqk1nr/pppp1ppp/8/4p3/1b1P4/8/PPPBPPPP/RN1QKBNR w KQkq - 2 3";

        String[] temp = fen.split(" ");
        
        String[] ranks = temp[0].split("/");
        for(int rank = 0; rank < ranks.length; rank++){
            int file = -1;
            for(int stringIndex = 0; stringIndex < ranks[rank].length(); stringIndex++){
                //when switch statements are not allowed
                file ++;
                char currentChar = ranks[rank].charAt(stringIndex);
                if(currentChar == 'p'){
                    board[file][rank] = new Piece(Piece.PieceType.PAWN, false, new Square(file, rank));
                    blackControlledSquares.put(board[file][rank], getPossiblePawnCaptures(new Square(file, rank), false));

                } else if(currentChar == 'n'){
                    board[file][rank] = new Piece(Piece.PieceType.KNIGHT, false, new Square(file, rank));
                    blackControlledSquares.put(board[file][rank], getPossibleKnightMoves(new Square(file, rank)));

                } else if(currentChar == 'b'){
                    board[file][rank] = new Piece(Piece.PieceType.BISHOP, false, new Square(file, rank));
                    blackControlledSquares.put(board[file][rank], getPossibleBishopMoves(new Square(file, rank)));

                } else if(currentChar == 'r'){
                    board[file][rank] = new Piece(Piece.PieceType.ROOK, false, new Square(file, rank));
                    blackControlledSquares.put(board[file][rank], getPossibleRookMoves(new Square(file, rank)));

                } else if(currentChar == 'q'){
                    board[file][rank] = new Piece(Piece.PieceType.QUEEN, false, new Square(file, rank));
                    blackControlledSquares.put(board[file][rank], getPossibleQueenMoves(new Square(file, rank)));

                } else if(currentChar == 'k'){
                    board[file][rank] = new Piece(Piece.PieceType.KING, false, new Square(file, rank));
                    blackControlledSquares.put(board[file][rank], getPossibleKingMoves(new Square(file, rank)));
                    blackKingSquare = new Square(file, rank);

                } else if(currentChar == 'P'){
                    board[file][rank] = new Piece(Piece.PieceType.PAWN, true, new Square(file, rank));
                    whiteControlledSquares.put(board[file][rank], getPossiblePawnCaptures(new Square(file, rank), true));

                } else if(currentChar == 'N'){
                    board[file][rank] = new Piece(Piece.PieceType.KNIGHT, true, new Square(file, rank));
                    whiteControlledSquares.put(board[file][rank], getPossibleKnightMoves(new Square(file, rank)));

                } else if(currentChar == 'B'){
                    board[file][rank] = new Piece(Piece.PieceType.BISHOP, true, new Square(file, rank));
                    whiteControlledSquares.put(board[file][rank], getPossibleBishopMoves(new Square(file, rank)));

                } else if(currentChar == 'R'){
                    board[file][rank] = new Piece(Piece.PieceType.ROOK, true, new Square(file, rank));
                    whiteControlledSquares.put(board[file][rank], getPossibleRookMoves(new Square(file, rank)));

                } else if(currentChar == 'Q'){
                    board[file][rank] = new Piece(Piece.PieceType.QUEEN, true, new Square(file, rank));
                    whiteControlledSquares.put(board[file][rank], getPossibleQueenMoves(new Square(file, rank)));

                } else if(currentChar == 'K'){
                    board[file][rank] = new Piece(Piece.PieceType.KING, true, new Square(file, rank));
                    whiteControlledSquares.put(board[file][rank], getPossibleKingMoves(new Square(file, rank)));
                    whiteKingSquare = new Square(file, rank);

                } else {
                    file += Character.getNumericValue(currentChar) - 1;
                }

                if(board[file][rank] != null){
                    if(board[file][rank].isWhite){
                        whitePieces.add(board[file][rank]);
                    }else {
                        blackPieces.add(board[file][rank]);
                    }
                }
            }
        }
        if(temp[1].equals("w")){
            whiteTurn = true;
        } else {
            whiteTurn = false;
        }

        if(temp[2].contains("K")){
            whiteCastlingRights[1] = true;
        }
        if(temp[2].contains("Q")){
            whiteCastlingRights[0] = true;
        }
        if(temp[2].contains("k")){
            blackCastlingRights[1] = true;
        }
        if(temp[2].contains("q")){
            blackCastlingRights[0] = true;
        }

        if(temp[3].equals("-")){
            enPassantSquare = null;
        } else {
            enPassantSquare = new Square((int)temp[3].charAt(0) - (int)'a', BOARD_SIZE - Character.getNumericValue(temp[3].charAt(1)));
        }

        fiftyMoveRuleCounter = Integer.parseInt(temp[4]);
        moveNumber = Integer.parseInt(temp[5]);
        
    }


    public Set<Square> getPossiblePawnMoves(Square square, boolean isWhite){
        Set<Square> moves = new HashSet<>();
        int direction = isWhite ? -1 : 1;
        //one square forward
        if(board[square.file][square.rank + direction] == null){
            moves.add(new Square(square.file, square.rank + direction));
        }
        //two squares forward
        if(square.rank == (isWhite ? 6 : 1)){
            if(board[square.file][square.rank + direction * 2] == null){
                moves.add(new Square(square.file, square.rank + direction * 2));
            }
        }

        return moves;
    }

    private Set<Square> getPossiblePawnCaptures(Square square, boolean isWhite){
        Set<Square> moves = new HashSet<>();
        int direction = isWhite ? -1 : 1;
        if(square.file > 0){
            moves.add(new Square(square.file - 1, square.rank + direction));
        }
        if(square.file < 7){
            moves.add(new Square(square.file + 1, square.rank + direction));
        }

        return moves;
    }

    private Set<Square> getPossibleKnightMoves(Square square){
        Set<Square> moves = new HashSet<>();
        for(int i = 1; i <= 2; i++){
            moves.add(new Square(square.file + i, square.rank + 3 - i));
            moves.add(new Square(square.file + i, square.rank - 3 + i));
            moves.add(new Square(square.file - i, square.rank + 3 - i));
            moves.add(new Square(square.file - i, square.rank - 3 + i));
        }
        removeOutOfBounds(moves);

        return moves;
    }

    private Set<Square> getPossibleBishopMoves(Square square){
        Set<Square> moves = new HashSet<>();
        moves.addAll(getDirectionalAttacks(square, 1, 1));
        moves.addAll(getDirectionalAttacks(square, -1, 1));
        moves.addAll(getDirectionalAttacks(square, 1, -1));
        moves.addAll(getDirectionalAttacks(square, -1, -1));
        return moves;
    }

    private Set<Square> getPossibleRookMoves(Square square){
        Set<Square> moves = new HashSet<>();
        moves.addAll(getDirectionalAttacks(square, 1, 0));
        moves.addAll(getDirectionalAttacks(square, -1, 0));
        moves.addAll(getDirectionalAttacks(square, 0, 1));
        moves.addAll(getDirectionalAttacks(square, 0, -1));

        return moves;
    }

    private Set<Square> getPossibleQueenMoves(Square square){
        Set<Square> moves = new HashSet<>();
        moves.addAll(getPossibleBishopMoves(square));
        moves.addAll(getPossibleRookMoves(square));
        return moves;
    }

    private Set<Square> getPossibleKingMoves(Square square){
        Set<Square> moves = new HashSet<>();
        for(int fileDirection = -1; fileDirection <= 1; fileDirection++){
            for(int rankDirection = -1; rankDirection <= 1; rankDirection++){
                if( fileDirection != 0 && rankDirection != 0){
                    moves.add(new Square(square.file + fileDirection, square.rank + rankDirection));
                }
            }
        }
        removeOutOfBounds(moves);

        return moves;
    }

    private Set<Square> getDirectionalAttacks(Square square, int fileDirection, int rankDirection){
        if(fileDirection > 1 || fileDirection < -1 || rankDirection > 1 || rankDirection < -1){
            throw new InvalidParameterException("Directions must be between -1 and 1 inclusive.");
        }
        Set<Square> attacks = new HashSet<>();
        int tempFile = square.file + fileDirection;
        int tempRank = square.rank + rankDirection;
        if(tempFile < BOARD_SIZE && tempFile >= 0 && tempRank < BOARD_SIZE && tempRank >= 0){
            attacks.add(new Square(tempFile, tempRank));

            while(tempFile < BOARD_SIZE - 1 && tempFile > 0 && tempRank < BOARD_SIZE - 1 && tempRank > 0 && board[tempFile][tempRank] == null){
                tempFile += fileDirection;
                tempRank += rankDirection;
                attacks.add(new Square(tempFile, tempRank));
            }
        }
        
        return attacks;
    }

    private void removeOutOfBounds(Set<Square> moves){
        Set<Square> outofBounds = new HashSet<>();
        for(Square move : moves){
            if(move.file >= BOARD_SIZE || move.file < 0 || move.rank >= BOARD_SIZE || move.rank < 0){
                outofBounds.add(move);
            }
        }
        for(Square move : outofBounds){
            moves.remove(move);
        }
    }

    public String boardToFENShort(){
        String FEN = "";
        int nullCounter = 0;
        for(int rank = 0; rank < BOARD_SIZE; rank++){
            for(int file = 0; file < BOARD_SIZE; file++){
                if(board[file][rank] == null){
                    nullCounter ++;
                } else {
                    if(nullCounter != 0){
                        FEN += nullCounter;
                        nullCounter = 0;
                    }
                    if(board[file][rank].isWhite){
                        FEN += Chess.TYPE_TO_STRING.get(board[file][rank].pieceType);
                    } else {
                        FEN += Chess.TYPE_TO_STRING.get(board[file][rank].pieceType).toLowerCase();
                    }
                }
            }
            if(nullCounter != 0){
                FEN += nullCounter;
                nullCounter = 0;
            }
            FEN += "/";
        }
        return FEN.substring(0, FEN.length() - 1);
    }

    public String boardToFENFull(){
        String fullFEN = boardToFENShort();
        fullFEN += " " + (whiteTurn ? "w" : "b");
        fullFEN += " ";
        if(whiteCastlingRights[1]){
            fullFEN += "K";
        }
        if(whiteCastlingRights[0]){
            fullFEN += "Q";
        }
        if(blackCastlingRights[1]){
            fullFEN += "k";
        }
        if(blackCastlingRights[0]){
            fullFEN += "q";
        }
        fullFEN += " ";
        if(enPassantSquare == null){
            fullFEN += "- ";
        } else {
            fullFEN += (char)(enPassantSquare.file + (int)'a') + (BOARD_SIZE - enPassantSquare.rank);
        }

        fullFEN += " " + fiftyMoveRuleCounter + " " + moveNumber;

        return fullFEN;
    }

    public String toString(){
        String boardString = "";
        if(whiteTurn){
            for(int rank = 0; rank < BOARD_SIZE; rank++){
                for(int file = 0; file < BOARD_SIZE; file++){
                    String background = ((file + rank) % 2) == 0 ? "\u001b[47m" : "\u001b[40m";
                    boardString += background;
                    String color = "";
                    if(board[file][rank] != null){
                        color = board[file][rank].isWhite ? "\u001b[38;2;253;182;0m" : "\u001b[38;5;21m";
                        boardString += color;
                        if(board[file][rank].pieceType == Piece.PieceType.PAWN){
                            boardString += " P ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.KNIGHT){
                            boardString += " N ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.BISHOP){
                            boardString += " B ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.ROOK){
                            boardString += " R ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.QUEEN){
                            boardString += " Q ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.KING){
                            boardString += " K ";
                        } else {
                            boardString += "   ";
                        }
                    } else {
                        boardString += "   ";
                    }
                    
                    boardString += "\u001b[0m";
                }
                boardString += " " + (BOARD_SIZE - rank) + "\n";
            }
            boardString += " a  b  c  d  e  f  g  h ";
        }else {
            for(int rank = BOARD_SIZE - 1; rank >= 0; rank--){
                for(int file = BOARD_SIZE - 1; file >= 0; file--){
                    String background = ((file + rank) % 2) == 0 ? "\u001b[47m" : "\u001b[40m";
                    boardString += background;
                    String color = "";
                    if(board[file][rank] != null){
                        color = board[file][rank].isWhite ? "\u001b[38;2;253;182;0m" : "\u001b[38;5;21m";
                        boardString += color;
                        if(board[file][rank].pieceType == Piece.PieceType.PAWN){
                            boardString += " P ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.KNIGHT){
                            boardString += " N ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.BISHOP){
                            boardString += " B ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.ROOK){
                            boardString += " R ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.QUEEN){
                            boardString += " Q ";
                        } else if(board[file][rank].pieceType == Piece.PieceType.KING){
                            boardString += " K ";
                        } else {
                            boardString += "   ";
                        }
                    } else {
                        boardString += "   ";
                    }

                    boardString += "\u001b[0m";
                }
                boardString += " " + (BOARD_SIZE - rank) + "\n";
            }
            boardString += " h  g  f  e  d  c  b  a ";
        }

        return boardString;
    }

    public void calculateAttacks(){
        //calculating new attacks
        for(Piece piece : whitePieces){
            Set<Square> newControlledSquares;
            if(piece.pieceType == Piece.PieceType.PAWN){
                newControlledSquares = getPossiblePawnCaptures(piece.square, true);
            } else if(piece.pieceType == Piece.PieceType.KNIGHT){
                newControlledSquares = getPossibleKnightMoves(piece.square);
            } else if(piece.pieceType == Piece.PieceType.BISHOP){
                newControlledSquares = getPossibleBishopMoves(piece.square);
            } else if(piece.pieceType == Piece.PieceType.ROOK){
                newControlledSquares = getPossibleRookMoves(piece.square);
            } else if(piece.pieceType == Piece.PieceType.QUEEN){
                newControlledSquares = getPossibleQueenMoves(piece.square);
            } else {
                newControlledSquares = getPossibleKingMoves(piece.square);
            }
            whiteControlledSquares.replace(piece, newControlledSquares);
        }
        for(Piece piece : blackPieces){
            Set<Square> newControlledSquares;
            if(piece.pieceType == Piece.PieceType.PAWN){
                newControlledSquares = getPossiblePawnCaptures(piece.square, false);
            } else if(piece.pieceType == Piece.PieceType.KNIGHT){
                newControlledSquares = getPossibleKnightMoves(piece.square);
            } else if(piece.pieceType == Piece.PieceType.BISHOP){
                newControlledSquares = getPossibleBishopMoves(piece.square);
            } else if(piece.pieceType == Piece.PieceType.ROOK){
                newControlledSquares = getPossibleRookMoves(piece.square);
            } else if(piece.pieceType == Piece.PieceType.QUEEN){
                newControlledSquares = getPossibleQueenMoves(piece.square);
            } else {
                newControlledSquares = getPossibleKingMoves(piece.square);
            }
            blackControlledSquares.replace(piece, newControlledSquares);
        }

        playerInCheck = new boolean[]{false, false};

        for(Piece p : whitePieces){
            for(Square square : whiteControlledSquares.get(p)){
                if(square.file == blackKingSquare.file && square.rank == blackKingSquare.rank){
                    playerInCheck[1] = true;
                }
            }
        }

        for(Piece p : blackPieces){
            for(Square square : blackControlledSquares.get(p)){
                if(square.file == whiteKingSquare.file && square.rank == whiteKingSquare.rank){
                    playerInCheck[0] = true;
                }
            }
        }
    }

    public void move(Piece piece, Square destination){
        int startFile = piece.square.file;
        int startRank = piece.square.rank;
        int endFile = destination.file;
        int endRank = destination.rank;
        //regular capture
        if(board[endFile][endRank] != null){
            Piece capturedPiece = board[endFile][endRank];
            if(whiteTurn){
                blackPieces.remove(capturedPiece);
                blackControlledSquares.remove(capturedPiece);
            } else {
                whitePieces.remove(capturedPiece);
                whiteControlledSquares.remove(capturedPiece);
            }
            fiftyMoveRuleCounter = -1;
            pgn += "x";
        }

        enPassantSquare = null;

        if(piece.pieceType == Piece.PieceType.PAWN){
            fiftyMoveRuleCounter = -1;

            //en passantable pawn
            if(Math.abs(endRank - startRank) == 2){
                enPassantSquare = new Square(endFile,(endRank + startRank) / 2);
            }

            //en passant capture
            if(endFile != startFile && board[endFile][endRank] == null){
                Piece capturedPiece = board[endFile][endRank + (whiteTurn ? 1: -1)];
            if(whiteTurn){
                blackPieces.remove(capturedPiece);
                blackControlledSquares.remove(capturedPiece);

            } else {
                whitePieces.remove(capturedPiece);
                whiteControlledSquares.remove(capturedPiece);
            }
            board[endFile][endRank + (whiteTurn ? 1: -1)] = null;
            fiftyMoveRuleCounter = -1;
            pgn += "x";
            }
        }
        
        
        board[endFile][endRank] = piece;
        piece.square = new Square(endFile, endRank);
        board[startFile][startRank] = null;


        //updating king square
        if(piece.pieceType == Piece.PieceType.KING){
            if(whiteTurn){
                whiteKingSquare = new Square(endFile, endRank);
            } else {
                blackKingSquare = new Square(endFile, endRank);
            }
        }

        calculateAttacks();
    }
}
