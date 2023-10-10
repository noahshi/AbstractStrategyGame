import java.security.*;
import java.util.*;
import java.util.regex.*;



//TODO:
//remove moving pieces pinned to king from legalMoves -- overhaul legal move check?
//add castling
//add resigning etc.
//add variants

public class Chess implements AbstractStrategyGame{
    
    private final int BOARD_SIZE = 8;
    private Piece[][] board = new Piece[BOARD_SIZE][BOARD_SIZE];
    private List<Piece> whitePieces = new ArrayList<>();
    private List<Piece> blackPieces = new ArrayList<>();
    private Map<Piece, Set<Square>> whiteControlledSquares = new HashMap<>();
    private Map<Piece, Set<Square>> blackControlledSquares = new HashMap<>();

    private Map<Piece, Set<Square>> pinnedPieceAvailableMoves = new HashMap<>();
    
    private boolean[] whiteCastlingRights = new boolean[2];
    private boolean[] blackCastlingRights = new boolean[2];

    private boolean[] playerInCheck = new boolean[]{false, false};
    private List<Piece> blackCheckingPieces = new ArrayList<>();
    private List<Piece> whiteCheckingPieces = new ArrayList<>();
    private Map<String, Integer> threeFoldCheck = new HashMap<>();
    
    private boolean whiteTurn;
    private Square enPassantSquare;
    private int fiftyMoveRuleCounter;
    private int moveNumber;

    private String startingFEN;
    private String pgn = "";

    private Square whiteKingSquare;
    private Square blackKingSquare;
    
    private int winner;


    private final HashMap<Piece.PieceType, String> TYPE_TO_STRING = new HashMap<>(){{
        put(Piece.PieceType.PAWN, "");
        put(Piece.PieceType.KNIGHT, "N");
        put(Piece.PieceType.BISHOP, "B");
        put(Piece.PieceType.ROOK, "R");
        put(Piece.PieceType.QUEEN, "Q");
        put(Piece.PieceType.KING, "K");
    }};



    public Chess(){
        //board setup
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        fen = "rnbqk1nr/pppp1ppp/8/4p3/1b1P4/8/PPPBPPPP/RN1QKBNR w KQkq - 2 3";

        startingFEN = fen;

        winner = -1;

        String[] temp = fen.split(" ");
        String[] ranks = temp[0].split("/");
        for(int rank = 0; rank < ranks.length; rank++){
            for(int file = 0; file < ranks[rank].length(); file++){
                //when switch statements are not allowed
                char currentChar = ranks[rank].charAt(file);
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

        threeFoldCheck.put(fen, 1);

    }


    //returns game instructions -- UNFINISHED --
    public String instructions(){
        return "White moves first. Type in your move in the format <starting square><ending square> ie. e2e4.\n" +
               "- are empty squares. Pieces are labeled with their respective names in algebraic notation.\n" +
               "more rules dsfsefjsodiflkenlsf";
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

    public boolean isGameOver(){
        return winner != -1;
    }

    public int getWinner(){
        return winner;
    }

    public int getNextPlayer(){
        return whiteTurn ? 1 : 2;
    }

    public void makeMove(Scanner input){
        String move = input.next();
        
        Pattern coordinate = Pattern.compile("[a-h][1-8][a-h][1-8]", Pattern.CASE_INSENSITIVE);
        Pattern algebraic = Pattern.compile("[NBRQK]?[a-h|1-8]?x?[a-h][1-8][+|$|#]?", Pattern.CASE_INSENSITIVE);
        
        boolean legalMove = false;
        int startFile = (int)move.charAt(0) - (int)'a';
        int startRank = BOARD_SIZE - Character.getNumericValue(move.charAt(1));
        int endFile = (int)move.charAt(2) - (int)'a';
        int endRank = BOARD_SIZE - Character.getNumericValue(move.charAt(3));

        if(board[startFile][startRank] == null){
            throw new IllegalArgumentException("Piece does not exist.");
        }

        if(board[startFile][startRank].isWhite != whiteTurn){
            throw new IllegalArgumentException("That is not your piece.");
        }

        Piece piece = board[startFile][startRank];
        for(Square legal : getLegalMoves().get(piece)){
            if(legal.file == endFile && legal.rank == endRank){
                legalMove = true;
            }
        }

        if(!legalMove){
            throw new IllegalArgumentException("Piece cannot move there.");
        }

        if(whiteTurn){
            pgn += moveNumber + ".";
        }
        pgn += TYPE_TO_STRING.get(piece.pieceType);

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
        
        pgn += move.substring(2);

        if(!whiteTurn){
            moveNumber ++;
        }

        whiteTurn = !whiteTurn;
        fiftyMoveRuleCounter ++;

        //checking for end scenarios below

        //FIFTY MOVE RULE
        if(fiftyMoveRuleCounter > 100){
            winner = 0;
            pgn += "$";
        }

        //3 FOLD REPETITION
        String currentFEN = boardToFENShort();
        if(threeFoldCheck.get(currentFEN) == null){
            threeFoldCheck.put(currentFEN, 1);
        } else {
            threeFoldCheck.replace(currentFEN, threeFoldCheck.get(currentFEN) + 1);
            if(threeFoldCheck.get(currentFEN) == 3){
                winner = 0;
                pgn += "$";
            }
        }

        //CHECKMATE/STALEMATE
        int availableMoves = 0;
        Map<Piece, Set<Square>> enemyMoves = getLegalMoves();
        for(Piece q : enemyMoves.keySet()){
            availableMoves += enemyMoves.get(q).size();
        }

        if(availableMoves == 0){
            if(playerInCheck[whiteTurn ? 0 : 1]){ 
                winner = whiteTurn ? 2 : 1; //whiteTurn boolean was inverted above so this ternary operator must be flipped
                pgn += "#";
            } else {
                winner = 0;
                pgn += "$";
            }
        }
        //checking for end scenarios above

        if(playerInCheck[whiteTurn ? 1 : 0] && winner == -1){
            pgn += "+";
        }
        pgn += " ";

        // debug statement for checking if en passant is working
        //if(enPassantSquare != null)
        //    System.out.println(enPassantSquare[0] + " " + enPassantSquare[1]);
    }

    public String getMoves(){
        return startingFEN += "\n" + pgn;
    }

    private String boardToFENShort(){
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
                        FEN += TYPE_TO_STRING.get(board[file][rank].pieceType);
                    } else {
                        FEN += TYPE_TO_STRING.get(board[file][rank].pieceType).toLowerCase();
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

    private String boardToFENFull(){
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

    private Set<Square> getPossiblePawnMoves(Square square, boolean isWhite){
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
            pinCheck(square, new Square(tempFile, tempRank), fileDirection, rankDirection);

            while(tempFile < BOARD_SIZE - 1 && tempFile > 0 && tempRank < BOARD_SIZE - 1 && tempRank > 0 && board[tempFile][tempRank] == null){
                tempFile += fileDirection;
                tempRank += rankDirection;
                attacks.add(new Square(tempFile, tempRank));
            }
            pinCheck(square, new Square(tempFile, tempRank), fileDirection, rankDirection);

            if(board[tempFile][tempRank] != null && pinnedPieceAvailableMoves.get(board[tempFile][tempRank]) != null 
            && pinnedPieceAvailableMoves.get(board[tempFile][tempRank]).size() != 0){
                for(Square attack : attacks){
                    if(attack.file != tempFile && attack.rank != tempRank){
                        pinnedPieceAvailableMoves.get(board[tempFile][tempRank]).add(attack);
                    }
                }
            }
        }
        
        return attacks;
    }

    private void pinCheck(Square attackingPiece, Square pinnedPiece, int fileDirection, int rankDirection){
        Piece pinned = board[pinnedPiece.file][pinnedPiece.rank];
        if(pinned != null && 
            pinned.isWhite != board[attackingPiece.file][attackingPiece.rank].isWhite){
                
            pinnedPieceAvailableMoves.put(pinned, new HashSet<Square>());
            System.out.println("entering pin check");

            int tempFile = pinnedPiece.file;
            int tempRank = pinnedPiece.rank;
            while(tempFile < BOARD_SIZE - 1 && tempFile > 0 && tempRank < BOARD_SIZE - 1 && tempRank > 0){
                tempFile += fileDirection;
                tempRank += rankDirection;

                pinnedPieceAvailableMoves.get(pinned).add(new Square(tempFile, tempRank));

                System.out.println("checking square " + tempFile + " " + tempRank);
                if(board[tempFile][tempRank] != null){
                    if(board[tempFile][tempRank].pieceType == Piece.PieceType.KING){
                        board[pinnedPiece.file][pinnedPiece.rank].isPinned = true;
                        System.out.println(TYPE_TO_STRING.get(board[pinnedPiece.file][pinnedPiece.rank].pieceType) + " is pinned");
                    }
                    
                    tempFile = Integer.MAX_VALUE; //exiting loop
                }
            }

            if(!pinned.isPinned){
                pinnedPieceAvailableMoves.get(pinned).clear();
            }
        }
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

    private Map<Piece, Set<Square>> getLegalMoves(){
        Map<Piece, Set<Square>> legalMoves = new HashMap<>();
        if(whiteTurn){
            for(Piece piece : whitePieces){
                legalMoves.put(piece, new HashSet<Square>());
                if(piece.isPinned){
                    for(Square move : whiteControlledSquares.get(piece)){
                        for(Square availableMove : pinnedPieceAvailableMoves.get(piece)){
                            if(move.file == availableMove.file && move.rank == availableMove.rank){
                                legalMoves.get(piece).add(move);
                                System.out.println("move added");
                            }
                        }
                    }
                }else {
                    for(Square move : whiteControlledSquares.get(piece)){
                        legalMoves.get(piece).add(move);
                        if(board[move.file][move.rank] != null && board[move.file][move.rank].isWhite){
                            legalMoves.get(piece).remove(move);
                        }
                    }
                    if(piece.pieceType == Piece.PieceType.KING){
                        for(Square move : whiteControlledSquares.get(piece)){
                            for(Piece enemyPiece : blackControlledSquares.keySet()){
                                for(Square square : blackControlledSquares.get(enemyPiece)){
                                    if(square.file == move.file && square.rank == move.rank){
                                        legalMoves.get(piece).remove(move);
                                    }
                                }
                            }
                        }
                    }
                    if(piece.pieceType == Piece.PieceType.PAWN){
                        //removing empty pawn captures from legal move list
                        Set<Square> emptyCaptures = new HashSet<>();
                        for(Square move : legalMoves.get(piece)){
                            if(board[move.file][move.rank] == null){
                                emptyCaptures.add(move);
                            }
                            
                            //re-adding en passant
                            if(enPassantSquare != null && enPassantSquare.file == move.file && enPassantSquare.rank == move.rank){
                                emptyCaptures.remove(move);
                            }
                        }
                        

                        for(Square move : emptyCaptures){
                            legalMoves.get(piece).remove(move);
                        }

                        for(Square move : getPossiblePawnMoves(piece.square, piece.isWhite)){
                            legalMoves.get(piece).add(move);
                        }
                    }
                }
            }
            
            //modifying legal move list if white is in check
            if(blackCheckingPieces.size() == 1){
                HashSet<Square> prioritySquares = new HashSet<>();
                Square checkOrigin = new Square(blackCheckingPieces.get(0).square.file, blackCheckingPieces.get(0).square.rank);
                prioritySquares.add(checkOrigin);
                if(blackCheckingPieces.get(0).pieceType != Piece.PieceType.KNIGHT){
                    int fileDifference = checkOrigin.file - whiteKingSquare.file;
                    int rankDifference = checkOrigin.rank - whiteKingSquare.rank;

                    if(fileDifference == 0){
                        if(rankDifference > 0){
                            for(int i = checkOrigin.rank - 1; i > whiteKingSquare.rank; i--){
                                prioritySquares.add(new Square(checkOrigin.file, i));
                            }
                        } else {
                            for(int i = checkOrigin.rank + 1; i < whiteKingSquare.rank; i++){
                                prioritySquares.add(new Square(checkOrigin.file, i));
                            }
                        }
                    } else if(rankDifference == 0){
                        if(fileDifference > 0){
                            for(int i = checkOrigin.file - 1; i > whiteKingSquare.file; i--){
                                prioritySquares.add(new Square(i, checkOrigin.rank));
                            }
                        } else {
                            for(int i = checkOrigin.file + 1; i < whiteKingSquare.file; i++){
                                prioritySquares.add(new Square(i, checkOrigin.rank));
                            }
                        }
                    } else if(fileDifference > 0){
                        if(rankDifference > 0){
                            for(int i = 1; i > fileDifference; i++){
                                prioritySquares.add(new Square(checkOrigin.file - i, checkOrigin.rank - i));
                            }
                        } else {
                            for(int i = 1; i > fileDifference; i++){
                                prioritySquares.add(new Square(checkOrigin.file - i, checkOrigin.rank + i));
                            }
                        }
                    } else if(fileDifference < 0){
                        if(rankDifference > 0){
                            for(int i = 1; i > -fileDifference; i++){
                                prioritySquares.add(new Square(checkOrigin.file + i, checkOrigin.rank - i));
                            }
                        } else {
                            for(int i = 1; i > -fileDifference; i++){
                                prioritySquares.add(new Square(checkOrigin.file + i, checkOrigin.rank + i));
                            }
                        }
                    }
                }
                Map<Piece, Set<Square>> actuallyLegalMoves = new HashMap<>();

                for(Piece piece : whitePieces){
                    actuallyLegalMoves.put(piece, new HashSet<Square>());
                    if(piece.pieceType != Piece.PieceType.KING){
                        for(Square move : legalMoves.get(piece)){
                            for(Square square : prioritySquares){
                                if(move.file == square.file && move.rank == square.rank){
                                    actuallyLegalMoves.get(piece).add(move);
                                }
                            }
                        }
                    } else {
                        for(Square move : legalMoves.get(piece)){
                            actuallyLegalMoves.get(piece).add(move);
                        }
                    }
                }
                legalMoves = actuallyLegalMoves;
            }

            //modifying legal move list if white is in double check
            if(blackCheckingPieces.size() > 1){
                Map<Piece, Set<Square>> actuallyLegalMoves = new HashMap<>();
                actuallyLegalMoves.put(board[whiteKingSquare.file][whiteKingSquare.rank], new HashSet<Square>());
                for(Square move : legalMoves.get(board[whiteKingSquare.file][whiteKingSquare.rank])){
                    actuallyLegalMoves.get(board[whiteKingSquare.file][whiteKingSquare.rank]).add(move);
                }
                legalMoves = actuallyLegalMoves;
            }
        } else {
            for(Piece piece : blackPieces){
                legalMoves.put(piece, new HashSet<Square>());
                if(piece.isPinned){
                    for(Square move : blackControlledSquares.get(piece)){
                        for(Square availableMove : pinnedPieceAvailableMoves.get(piece)){
                            if(move.file == availableMove.file && move.rank == availableMove.rank){
                                legalMoves.get(piece).add(move);
                            }
                        }
                    }
                }else{
                    for(Square move : blackControlledSquares.get(piece)){
                        legalMoves.get(piece).add(move);
                        if(board[move.file][move.rank] != null && !board[move.file][move.rank].isWhite){
                            legalMoves.get(piece).remove(move);
                        }
                    }
                    if(piece.pieceType == Piece.PieceType.KING){
                        for(Square move : blackControlledSquares.get(piece)){
                            for(Piece enemyPiece : whiteControlledSquares.keySet()){
                                for(Square square : whiteControlledSquares.get(enemyPiece)){
                                    if(square.file == move.file && square.rank == move.rank){
                                        legalMoves.get(piece).remove(move);
                                    }
                                }
                            }
                        }
                    }
                    if(piece.pieceType == Piece.PieceType.PAWN){
                        Set<Square> emptyCaptures = new HashSet<>();
                        for(Square move : legalMoves.get(piece)){
                            if(board[move.file][move.rank] == null){
                                emptyCaptures.add(move);
                            }

                            if(enPassantSquare != null && enPassantSquare.file == move.file && enPassantSquare.rank == move.rank){
                                emptyCaptures.remove(move);
                            }
                        }
                        
                        for(Square move : emptyCaptures){
                            legalMoves.get(piece).remove(move);
                        }
                        
                        for(Square move : getPossiblePawnMoves(piece.square, piece.isWhite)){
                            legalMoves.get(piece).add(move);
                        }
                    }
                }
            }

            //modifying legal move list if black is in check
            if(whiteCheckingPieces.size() == 1){
                HashSet<Square> prioritySquares = new HashSet<>();
                Square checkOrigin = new Square(whiteCheckingPieces.get(0).square.file, whiteCheckingPieces.get(0).square.rank);
                prioritySquares.add(checkOrigin);
                if(whiteCheckingPieces.get(0).pieceType != Piece.PieceType.KNIGHT){
                    int fileDifference = checkOrigin.file - blackKingSquare.file;
                    int rankDifference = checkOrigin.rank - blackKingSquare.rank;

                    if(fileDifference == 0){
                        if(rankDifference > 0){
                            for(int i = checkOrigin.rank - 1; i > blackKingSquare.rank; i--){
                                prioritySquares.add(new Square(checkOrigin.file, i));
                            }
                        } else {
                            for(int i = checkOrigin.rank + 1; i < blackKingSquare.rank; i++){
                                prioritySquares.add(new Square(checkOrigin.file, i));
                            }
                        }
                    } else if(rankDifference == 0){
                        if(fileDifference > 0){
                            for(int i = checkOrigin.file - 1; i > blackKingSquare.file; i--){
                                prioritySquares.add(new Square(i, checkOrigin.rank));
                            }
                        } else {
                            for(int i = checkOrigin.file + 1; i < blackKingSquare.file; i++){
                                prioritySquares.add(new Square(i, checkOrigin.rank));
                            }
                        }
                    } else if(fileDifference > 0){
                        if(rankDifference > 0){
                            for(int i = 1; i > fileDifference; i++){
                                prioritySquares.add(new Square(checkOrigin.file - i, checkOrigin.rank - i));
                            }
                        } else {
                            for(int i = 1; i > fileDifference; i++){
                                prioritySquares.add(new Square(checkOrigin.file - i, checkOrigin.rank + i));
                            }
                        }
                    } else if(fileDifference < 0){
                        if(rankDifference > 0){
                            for(int i = 1; i > -fileDifference; i++){
                                prioritySquares.add(new Square(checkOrigin.file + i, checkOrigin.rank - i));
                            }
                        } else {
                            for(int i = 1; i > -fileDifference; i++){
                                prioritySquares.add(new Square(checkOrigin.file + i, checkOrigin.rank + i));
                            }
                        }
                    }
                }
                Map<Piece, Set<Square>> actuallyLegalMoves = new HashMap<>();

                for(Piece piece : blackPieces){
                    actuallyLegalMoves.put(piece, new HashSet<Square>());
                    if(piece.pieceType != Piece.PieceType.KING){
                        for(Square move : legalMoves.get(piece)){
                            for(Square square : prioritySquares){
                                if(move.file == square.file && move.rank == square.rank){
                                    actuallyLegalMoves.get(piece).add(move);
                                }
                            }
                        }
                    } else {
                        for(Square move : legalMoves.get(piece)){
                            actuallyLegalMoves.get(piece).add(move);
                        }
                    }
                }
                legalMoves = actuallyLegalMoves;
            }

            //modifying legal move list if black is in double check
            if(whiteCheckingPieces.size() > 1){
                Map<Piece, Set<Square>> actuallyLegalMoves = new HashMap<>();
                actuallyLegalMoves.put(board[blackKingSquare.file][blackKingSquare.rank], new HashSet<Square>());
                for(Square move : legalMoves.get(board[blackKingSquare.file][blackKingSquare.rank])){
                    actuallyLegalMoves.get(board[blackKingSquare.file][blackKingSquare.rank]).add(move);
                }
                legalMoves = actuallyLegalMoves;
            }
        }
        return legalMoves;
    }

    private void calculateAttacks(){
        //resetting pin status
        for(Piece piece : whitePieces){
            piece.isPinned = false;
            pinnedPieceAvailableMoves.put(piece, new HashSet<Square>());
        }
        for(Piece piece : blackPieces){
            piece.isPinned = false;
            pinnedPieceAvailableMoves.put(piece, new HashSet<Square>());
        }

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
        whiteCheckingPieces.clear();
        blackCheckingPieces.clear();

        for(Piece p : whitePieces){
            for(Square square : whiteControlledSquares.get(p)){
                if(square.file == blackKingSquare.file && square.rank == blackKingSquare.rank){
                    playerInCheck[1] = true;
                    whiteCheckingPieces.add(p);
                }
            }
        }

        for(Piece p : blackPieces){
            for(Square square : blackControlledSquares.get(p)){
                if(square.file == whiteKingSquare.file && square.rank == whiteKingSquare.rank){
                    playerInCheck[0] = true;
                    blackCheckingPieces.add(p);
                }
            }
        }
    }
}
