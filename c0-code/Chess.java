//import java.security.*;
import java.util.*;
import java.util.regex.*;



//TODO:
//finish writing game instructions

//add crazyhouse

//This is the class that creates and runs chess games. It implements the AbstractStrategyGame interface. 
public class Chess implements AbstractStrategyGame{
    
    private Board board;
    private int gameType = -1; //-1 for not chosen, 0 for standard, 1 for chess960, 2 for crazyhouse, 3 for atomic
    private Map<String, Integer> threeFoldCheck = new HashMap<>();
    private String startingFEN;
    private int winner = -1;
    private boolean drawInitiated, drawInitiatedThisRound = false;

    private Pattern coordinate = Pattern.compile("^[a-h][1-8][a-h][1-8](=[NBRQ])?$", Pattern.CASE_INSENSITIVE);
    private Pattern algebraic = Pattern.compile(
                "^([NBRQK])([a-h|1-8])?[x|@]?([a-h])([1-8])[+$#]?|([a-h]x|@)?([a-h])([1-8])(=[NBRQ]| ?e\\.p\\.)?[+$#]?|O-O(-O)?[+$#]?$");
    private Pattern draw = Pattern.compile("^draw$", Pattern.CASE_INSENSITIVE);
    private Pattern resign = Pattern.compile("^resign$", Pattern.CASE_INSENSITIVE);

    public static final String DEFAULT_BOARD_SETUP = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public static final int KINGSIDE_CASTLE_FILE = 6;
    public static final int KINGSIDE_CASTLE_FILE_ROOK = 5;
    public static final int QUEENSIDE_CASTLE_FILE = 2;
    public static final int QUEENSIDE_CASTLE_FILE_ROOK = 3;

    public static final int DRAW = 0;
    public static final int PLAYER_WHITE = 1;
    public static final int PLAYER_BLACK = 2;

    public static final HashMap<Piece.PieceType, String> TYPE_TO_STRING = new HashMap<>(){{
        put(Piece.PieceType.PAWN, "P");
        put(Piece.PieceType.KNIGHT, "N");
        put(Piece.PieceType.BISHOP, "B");
        put(Piece.PieceType.ROOK, "R");
        put(Piece.PieceType.QUEEN, "Q");
        put(Piece.PieceType.KING, "K");
    }};

    public static final String[] VARIANTS = new String[]{"STANDARD", "CHESS960", "CRAZYHOUSE", "ATOMIC"};

    


    //creates a chess game with a game type of -1
    public Chess(){
        setUpBoard(-1);
    }

    //takes in the game type and sets up the board based off that input
    private void setUpBoard(int gameType){
        if(gameType != 1){
            startingFEN = DEFAULT_BOARD_SETUP;
        } else {
            String randomizedPieces = chess960Randomizer();
            startingFEN = randomizedPieces.toLowerCase() + "/pppppppp/8/8/8/8/PPPPPPPP/" + randomizedPieces.toUpperCase() + " w KQkq - 0 1";
        }

        board = new Board(startingFEN);
        this.gameType = gameType;

        threeFoldCheck.clear();
        threeFoldCheck.put(startingFEN, 1);
        winner = -1;
    }


    //returns game instructions -- UNFINISHED --
    public String instructions(){
        return "White moves first. Type in your move in either coordinate or algebraic notation.\n" +
               "Pieces are labeled with their respective names in algebraic notation.\n" +
               "more rules dsfsefjsodiflkenlsf";
    }

    //returns a string that either asks for a game type if a game type has not been selected yet 
    //or a string that represents the current state of the chess board
    //or a string that contains the starting position, the moves made, and the result if the game is over
    public String toString(){
        if(isGameOver()){
            return board.toString() + "\n" + "VARIANT: " + VARIANTS[gameType] + "\n" + getMoves();
        }
        if(gameType >= 0){
        
            return board.toString() + 
                (drawInitiated ? "\nPlayer " + getNextPlayer() + " has offered a draw. \nType \"draw\" to accept, play a move to decline." : "");
        
        } else{ 
            String variantChoice = "Enter the variant you want to play.";
             for(int i = 0; i < VARIANTS.length; i++){
                variantChoice += "\n" + i + " for " + VARIANTS[i];
             }
             return variantChoice;
        }
    }

    //checks if the game is over and returns true if it is
    public boolean isGameOver(){
        return winner != -1;
    }

    //returns the winner if there is one, 0 if it's a draw, and -1 if the game is still ongoing
    public int getWinner(){
        return winner;
    }

    //returns either 1 or 2 based on which player's turn it is
    public int getNextPlayer(){
        return board.whiteTurn ? PLAYER_WHITE : PLAYER_BLACK;
    }


    //Takes a string input
    //If game type has not been selected, the string input must be an integer from 0-3, otherwise an exception will be thrown
    //If game type has been selected, the string input must either be in coordinate or algebraic notation
    //Illegal and nonexistent moves with throw exceptions
    //In coordinate notation, if a king move and castling end up on the same square, the normal king move takes priority
    //Use algebraic notation to differentiate when that occurs because the coordinate notation system does not support such move overlaps
    //If an exception is not thrown, the move will be played on the board
    public void makeMove(Scanner input){
        drawInitiatedThisRound = false;
        String move = input.nextLine();
        if(gameType < 0){
            int type = Integer.parseInt(move);
            if(type > 3 || type < 0){
                throw new IllegalArgumentException("There are only 4 game types, numbered from 0-3.");
            }
            gameType = type;
            setUpBoard(gameType);
        } else if (resign.matcher(move).matches()){
            winner = board.whiteTurn ? PLAYER_BLACK : PLAYER_WHITE;
            board.pgn += board.whiteTurn ? "0-1" : "1-0";
            
        } else if(draw.matcher(move).matches() && drawInitiated){
            winner = DRAW;
            board.pgn += " 1/2-1/2";

        } else {
            if (draw.matcher(move).matches()){
                drawInitiatedThisRound = true;
                move = input.nextLine();
            }

            Matcher cdnMatcher = coordinate.matcher(move);
            Matcher algMatcher = algebraic.matcher(move);
            
            boolean legalMove = false;
            boolean isKingSideCastle = false;

            int startFile = -1;
            int startRank = -1;
            int endFile = -1;
            int endRank = -1;

            Piece promotionPiece = null;

            if(cdnMatcher.matches()){
                startFile = (int)move.charAt(0) - (int)'a';
                startRank = board.BOARD_SIZE - Character.getNumericValue(move.charAt(1));
                endFile = (int)move.charAt(2) - (int)'a';
                endRank = board.BOARD_SIZE - Character.getNumericValue(move.charAt(3));

                if(cdnMatcher.group(1) != null){
                    if(board.board[startFile][startRank].pieceType != Piece.PieceType.PAWN){
                        throw new IllegalArgumentException("Only pawns can promote.");
                    }
                    if(endRank != (board.whiteTurn ? 0 : 7)){
                        throw new IllegalArgumentException("Pawns can only promote when they reach the end of the board.");
                    }
                    String promotionChar = cdnMatcher.group(1).charAt(1) + "";
                    if(promotionChar.equals("N")){
                        promotionPiece = new Piece(Piece.PieceType.KNIGHT, board.whiteTurn, new Square(endFile, endRank));
                    } else if(promotionChar.equals("B")){
                        promotionPiece = new Piece(Piece.PieceType.BISHOP, board.whiteTurn, new Square(endFile, endRank));
                    } else if(promotionChar.equals("R")){
                        promotionPiece = new Piece(Piece.PieceType.ROOK, board.whiteTurn, new Square(endFile, endRank));
                    } else {
                        promotionPiece = new Piece(Piece.PieceType.QUEEN, board.whiteTurn, new Square(endFile, endRank));
                    }
                } else {
                    if(board.board[startFile][startRank].pieceType == Piece.PieceType.PAWN && endRank == (board.whiteTurn ? 0 : 7)){
                        throw new IllegalArgumentException("Ambiguous move input. Please specify the promotion.");
                    }
                }

                if(board.board[startFile][startRank] == null){
                    throw new IllegalArgumentException("Piece does not exist.");
                }

                if(board.board[startFile][startRank].isWhite != board.whiteTurn){
                    throw new IllegalArgumentException("That is not your piece.");
                }

                if(board.board[startFile][startRank].pieceType == Piece.PieceType.KING && endFile - startFile > 1){
                    isKingSideCastle = true;
                }
            //typing symbols used in chess notation such as x + $ # etc. is allowed for extra 
            //leniency but they will be disregarded in cases where they should not be used.
            } else if (algMatcher.matches()){
                if(move.contains("@") && gameType != 2){
                    throw new IllegalArgumentException("This is not Crazyhouse. You can't place pieces back down.");
                }
                boolean doesMoveExist = false;
                //System.out.println(board.boardToFENShort());
                
                //castling
                if(move.contains("O-O")){
                    //System.out.println("castling move inputted");
                    if(algMatcher.group(9) != null){
                        if(board.whiteTurn ? board.whiteCastlingRights[1] : board.blackCastlingRights[1]){
                            doesMoveExist = true;
                        }
                        endFile = QUEENSIDE_CASTLE_FILE;
                        isKingSideCastle = false;
                    } else {
                        if(board.whiteTurn ? board.whiteCastlingRights[0] : board.blackCastlingRights[0]){
                            doesMoveExist = true;
                        }
                        endFile = KINGSIDE_CASTLE_FILE;
                        isKingSideCastle = true;
                    }
                    startFile = board.whiteTurn ? board.whiteKingSquare.file : board.blackKingSquare.file;
                    startRank = board.whiteTurn ? 7 : 0;
                    endRank = board.whiteTurn ? 7 : 0;
                    //System.out.println("castling will start on " + new Square(startFile, startRank).toString() + " and end on " + new Square(endFile, endRank));
                
                //piece movement
                } else if(algMatcher.group(1) != null){

                    endFile = (int)algMatcher.group(3).charAt(0) - (int)'a';
                    endRank = 8 - Character.getNumericValue(algMatcher.group(4).charAt(0));

                    if(algMatcher.group(2) != null){
                        if("abcdefgh".contains(algMatcher.group(2))){
                            startFile = (int)algMatcher.group(2).charAt(0) - (int)'a';
                        } else {
                            startRank = 8 - Character.getNumericValue(algMatcher.group(2).charAt(0));
                        }
                    }

                    for(Piece piece : board.whiteTurn ? board.whitePieces : board.blackPieces){
                        if(TYPE_TO_STRING.get(piece.pieceType).equals(algMatcher.group(1))){
                            //System.out.println("Piece specified is " + TYPE_TO_STRING.get(piece.pieceType));
                            for(Square square : board.whiteTurn ? board.whiteControlledSquares.get(piece) : board.blackControlledSquares.get(piece)){
                                //System.out.println("checking if move " + TYPE_TO_STRING.get(piece.pieceType) + (char)(square.file + (int)'a') + "" + (8 - square.rank) + " equals inputted move");
                                if(square.file == endFile && square.rank == endRank){
                                    
                                    if(startFile != -1 && startRank != -1){
                                        throw new IllegalArgumentException("Ambiguous move input.");
                                    }

                                    //moves in the format of Nbc3
                                    if(startFile != -1){
                                        if(startFile == piece.square.file){
                                            startRank = piece.square.rank;
                                            doesMoveExist = true;
                                        }
                                    //moves in the format of N1c3
                                    } else if (startRank != -1) {
                                        if(startRank == piece.square.rank){
                                            startFile = piece.square.file;
                                            doesMoveExist = true;
                                        }
                                    //moves in the format of Nc3
                                    } else {
                                        startFile = piece.square.file;
                                        startRank = piece.square.rank;
                                        doesMoveExist = true;
                                    }
                                }
                            }
                        }
                    }

                //pawn movement
                } else if(algMatcher.group(6) != null){
                    endFile = (int)algMatcher.group(6).charAt(0) - (int)'a';
                    endRank = 8 - Character.getNumericValue(algMatcher.group(7).charAt(0));

                    //moves in the format of exd5
                    if(algMatcher.group(5) != null){
                        startFile = (int)algMatcher.group(5).charAt(0) - (int)'a';

                    //moves in the format of e4
                    } else {
                        startFile = endFile;
                    }
                    for(Piece piece : board.whiteTurn ? board.whitePieces : board.blackPieces){
                        if(piece.pieceType == Piece.PieceType.PAWN && piece.square.file == startFile){
                            for(Square square : board.whiteTurn ? board.whiteControlledSquares.get(piece) : board.blackControlledSquares.get(piece)){
                                if(square.file == endFile && square.rank == endRank){
                                    startFile = piece.square.file;
                                    startRank = piece.square.rank;
                                    doesMoveExist = true;
                                }
                            }
                            for(Square square : board.getPossiblePawnMoves(new Square(piece.square.file, piece.square.rank), board.whiteTurn)){
                                if(square.file == endFile && square.rank == endRank){
                                    startFile = piece.square.file;
                                    startRank = piece.square.rank;
                                    doesMoveExist = true;
                                }
                            }
                        }
                    }
                    if(!doesMoveExist){ //need to throw exception before promotion check or startrank will be null and ambiguous promotion checker will crash
                        throw new IllegalArgumentException("Move does not exist.");
                    }
                    //promotion checker (moves in the format of a1=Q or bxc8=B)
                    if(algMatcher.group(8) != null && !algMatcher.group(8).equals(" e.p.") && !algMatcher.group(8).equals("e.p.")){
                        if(endRank != (board.whiteTurn ? 0 : 7)){
                            throw new IllegalArgumentException("Pawns can only promote when they reach the end of the board.");
                        }
                        String promotionChar = algMatcher.group(8).charAt(1) + "";
                        if(promotionChar.equals("N")){
                            promotionPiece = new Piece(Piece.PieceType.KNIGHT, board.whiteTurn, new Square(endFile, endRank));
                        } else if(promotionChar.equals("B")){
                            promotionPiece = new Piece(Piece.PieceType.BISHOP, board.whiteTurn, new Square(endFile, endRank));
                        } else if(promotionChar.equals("R")){
                            promotionPiece = new Piece(Piece.PieceType.ROOK, board.whiteTurn, new Square(endFile, endRank));
                        } else {
                            promotionPiece = new Piece(Piece.PieceType.QUEEN, board.whiteTurn, new Square(endFile, endRank));
                        }
                    } else {
                        if(board.board[startFile][startRank].pieceType == Piece.PieceType.PAWN && endRank == (board.whiteTurn ? 0 : 7)){
                            throw new IllegalArgumentException("Ambiguous move input. Please specify the promotion.");
                        }
                    }
                }

                if(!doesMoveExist){
                    throw new IllegalArgumentException("Move does not exist.");
                }

            } else {
                throw new IllegalArgumentException("Input is in the wrong format.");
            }

            

            Piece piece = board.board[startFile][startRank];
            for(Square legal : getLegalMoves().get(piece)){
                if(legal.file == endFile && legal.rank == endRank){
                    legalMove = true;
                }
                //System.out.println(TYPE_TO_STRING.get(piece.pieceType)+(char)(legal.file + (int)'a') + (8-legal.rank));
            }

            if(!legalMove){
                throw new IllegalArgumentException("Piece cannot move there.");
            }

            if(board.whiteTurn){
                board.pgn += board.moveNumber + ".";
            }
            
            
            boolean isCapture = false;

            if(promotionPiece != null){
                isCapture = board.move(piece, new Square(endFile, endRank), promotionPiece);
                board.pgn += move.substring(2);
                board.pgn += "=" + TYPE_TO_STRING.get(promotionPiece.pieceType);
            } else if(move.contains("O-O") || (piece.pieceType == Piece.PieceType.KING && Math.abs(startFile - endFile) > 1)){
                isCapture = board.move(piece, new Square(endFile, endRank), isKingSideCastle);
                
                if(isKingSideCastle){
                    board.pgn += "O-O";
                } else {
                    board.pgn += "O-O-O";
                }
            } else {
                if(piece.pieceType != Piece.PieceType.PAWN){
                    board.pgn += TYPE_TO_STRING.get(piece.pieceType);
                }
                isCapture = board.move(piece, new Square(endFile, endRank));
                
                board.pgn += new Square(endFile, endRank).toString();
            }

            if(gameType == 3 && isCapture){
                int kingsExplodeIndex = board.atomicCaptureExplosion(new Square(endFile, endRank));
                if(kingsExplodeIndex == 1){
                    winner = PLAYER_BLACK;
                    board.pgn += " 0-1";

                } else if (kingsExplodeIndex == 2){
                    winner = PLAYER_WHITE;
                    board.pgn += " 1-0";

                } else if(kingsExplodeIndex == 3){
                    winner = DRAW;
                    board.pgn += " 1/2-1/2";
                }
            }

            board.calculateAttacks();

            drawInitiated = drawInitiatedThisRound;

            //removing castling rights if king or rook moved
            if(piece.pieceType == Piece.PieceType.KING){
                if(board.whiteTurn){
                    board.whiteCastlingRights[0] = false;
                    board.whiteCastlingRights[1] = false;
                } else {
                    board.blackCastlingRights[0] = false;
                    board.blackCastlingRights[1] = false;
                }
            }
            if(piece.pieceType == Piece.PieceType.ROOK){
                if(board.whiteTurn){
                    if(piece.square.file > board.whiteKingSquare.file){
                        board.whiteCastlingRights[1] = false;
                    } else {
                        board.whiteCastlingRights[0] = false;
                    }
                } else {
                    if(piece.square.file > board.blackKingSquare.file){
                        board.blackCastlingRights[1] = false;
                    } else {
                        board.blackCastlingRights[0] = false;
                    }
                }
            }

            if(!board.whiteTurn){
                board.moveNumber ++;
            }

            board.whiteTurn = !board.whiteTurn;
            board.fiftyMoveRuleCounter ++;

            //checking for end scenarios below

            //FIFTY MOVE RULE
            if(board.fiftyMoveRuleCounter > 100){
                winner = 0;
                board.pgn += "$";
                board.pgn += " 1/2-1/2";
            }

            //3 FOLD REPETITION
            String currentFEN = board.boardToFENShort();
            if(threeFoldCheck.get(currentFEN) == null){
                threeFoldCheck.put(currentFEN, 1);
            } else {
                threeFoldCheck.replace(currentFEN, threeFoldCheck.get(currentFEN) + 1);
                if(threeFoldCheck.get(currentFEN) == 3){
                    winner = 0;
                    board.pgn += "$";
                    board.pgn += " 1/2-1/2";
                }
            }

            //CHECKMATE/STALEMATE
            int availableMoves = 0;
            Map<Piece, Set<Square>> enemyMoves = getLegalMoves();
            for(Piece enemyPiece : enemyMoves.keySet()){
                availableMoves += enemyMoves.get(enemyPiece).size();
            }

            if(availableMoves == 0){
                if(board.playerInCheck[board.whiteTurn ? 0 : 1]){ 
                    winner = board.whiteTurn ? PLAYER_BLACK : PLAYER_WHITE; //whiteTurn boolean was inverted above so this ternary operator must be flipped
                    board.pgn += "#";
                    board.pgn += board.whiteTurn ? "0-1" : "1-0";
                } else {
                    winner = DRAW;
                    board.pgn += "$";
                    board.pgn += " 1/2-1/2";
                }
            }
            //checking for end scenarios above

            if(board.playerInCheck[board.whiteTurn ? 1 : 0] && winner == -1){
                board.pgn += "+";
            }
            board.pgn += " ";

            //if(enPassantSquare != null)
            //    System.out.println(enPassantSquare[0] + " " + enPassantSquare[1]);
        }
    }

    //returns the starting position as a string followed by all the moves played
    public String getMoves(){
        return startingFEN += "\n" + board.pgn;
    }

    

    //returns all the legal moves that a player can play for their turn
    private Map<Piece, Set<Square>> getLegalMoves(){
        Map<Piece, Set<Square>> legalMoves = new HashMap<>();
        String currentFEN = board.boardToFENFull();
        //System.out.println(currentFEN);
        for(Piece piece : board.whiteTurn ? board.whitePieces : board.blackPieces){
            //System.out.println("checking piece " + TYPE_TO_STRING.get(piece.pieceType) + " on " + (char)(piece.square.file + (int)'a') + "" + (8 - piece.square.rank) + " for white");
            legalMoves.put(piece, new HashSet<Square>());
            for(Square move : board.whiteTurn ? board.whiteControlledSquares.get(piece) : board.blackControlledSquares.get(piece)){
                legalMoves.get(piece).add(move);
                if(board.board[move.file][move.rank] != null && board.whiteTurn == board.board[move.file][move.rank].isWhite){
                    legalMoves.get(piece).remove(move);
                }
            }
            if(piece.pieceType == Piece.PieceType.PAWN){
                //removing empty pawn captures from legal move list
                Set<Square> emptyCaptures = new HashSet<>();
                for(Square move : legalMoves.get(piece)){
                    if(board.board[move.file][move.rank] == null){
                        emptyCaptures.add(move);
                    }
                    
                    //re-adding en passant
                    if(board.enPassantSquare != null && board.enPassantSquare.file == move.file && board.enPassantSquare.rank == move.rank){
                        emptyCaptures.remove(move);
                    }
                }
                
                for(Square move : emptyCaptures){
                    legalMoves.get(piece).remove(move);
                }

                for(Square move : board.getPossiblePawnMoves(piece.square, piece.isWhite)){
                    legalMoves.get(piece).add(move);
                }
            }
            if(piece.pieceType == Piece.PieceType.KING){
                if(board.whiteTurn ? board.whiteCastlingRights[0] : board.blackCastlingRights[0]){
                    int firstRank = board.whiteTurn ? 7 : 0;
                    boolean illegalCastle = false;
                    for(int currentFile = piece.square.file; currentFile <= KINGSIDE_CASTLE_FILE; currentFile++){
                        if(board.board[currentFile][firstRank] != null && board.board[currentFile][firstRank].pieceType != Piece.PieceType.ROOK
                            && board.board[currentFile][firstRank].pieceType != Piece.PieceType.KING){

                            illegalCastle = true;
                            //System.out.println("kingside castle is blocked on " + new Square(currentFile, firstRank).toString());
                        }
                        for(Piece enemyPiece : board.whiteTurn ? board.blackPieces : board.whitePieces){
                            for(Square square : board.whiteTurn ? board.blackControlledSquares.get(enemyPiece) 
                                                                : board.whiteControlledSquares.get(enemyPiece)){

                                if(square.file == currentFile && square.rank == firstRank){
                                    illegalCastle = true;
                                    //System.out.println("kingside castle is attacked" + new Square(currentFile, firstRank).toString());
                                }
                            }
                        }
                    }
                    if(board.board[KINGSIDE_CASTLE_FILE_ROOK][firstRank] != null 
                        && board.board[KINGSIDE_CASTLE_FILE_ROOK][firstRank].pieceType != Piece.PieceType.KING){

                        illegalCastle = true;
                    }
                    if(!illegalCastle){
                        legalMoves.get(piece).add(new Square(KINGSIDE_CASTLE_FILE, firstRank));
                        //System.out.println("added kingside castle to legal moves");
                    }
                }
                if(board.whiteTurn ? board.whiteCastlingRights[1] : board.blackCastlingRights[1]){
                    int firstRank = board.whiteTurn ? 7 : 0;
                    boolean illegalCastle = false;
                    for(int currentFile = piece.square.file; currentFile >= QUEENSIDE_CASTLE_FILE; currentFile--){
                        if(board.board[currentFile][firstRank] != null && board.board[currentFile][firstRank].pieceType != Piece.PieceType.ROOK
                            && board.board[currentFile][firstRank].pieceType != Piece.PieceType.KING){

                            illegalCastle = true;
                            //System.out.println("queenside castle is blocked on " + new Square(currentFile, firstRank).toString());
                        }
                        for(Piece enemyPiece : board.whiteTurn ? board.blackPieces : board.whitePieces){
                            for(Square square : board.whiteTurn ? board.blackControlledSquares.get(enemyPiece) 
                                                                : board.whiteControlledSquares.get(enemyPiece)){
                                                                    
                                if(square.file == currentFile && square.rank == firstRank){
                                    illegalCastle = true;
                                    //System.out.println("queenside castle is attacked on " + new Square(currentFile, firstRank).toString());
                                }
                            }
                        }
                    }
                    if(board.board[QUEENSIDE_CASTLE_FILE_ROOK][firstRank] != null 
                        && board.board[QUEENSIDE_CASTLE_FILE_ROOK][firstRank].pieceType != Piece.PieceType.KING){

                        illegalCastle = true;
                    }
                    if(!illegalCastle){
                        legalMoves.get(piece).add(new Square(QUEENSIDE_CASTLE_FILE, firstRank));
                        //System.out.println("added queenside castle");
                    }
                }
            }
        }
        for(Piece piece : board.whiteTurn ? board.whitePieces : board.blackPieces){
            Set<Square> illegalMoves = new HashSet<>();
            for(Square square : legalMoves.get(piece)){
                Board testBoard = new Board(currentFEN);
                //System.out.println(currentFEN);
                //System.out.println(testBoard.boardToFENFull());
                //System.out.println("testing move " + TYPE_TO_STRING.get(piece.pieceType) + " on " + (char)(piece.square.file + (int)'a') + "" + (8 - piece.square.rank) + " to " + (char)(square.file + (int)'a') + "" + (8 - square.rank) + " for white");
                testBoard.move(testBoard.board[piece.square.file][piece.square.rank], square);
                if(testBoard.playerInCheck[0]){
                    illegalMoves.add(square);
                }
            }
            for(Square square : illegalMoves){
                legalMoves.get(piece).remove(square);
            }
        }
        return legalMoves;
    }

    private String chess960Randomizer(){
        String[] pieces = new String[8];
        List<Integer> indexes = Arrays.asList(0,1,2,3,4,5,6,7);
        Queue<String> pieceStrings = new LinkedList<>(){{
            this.add("R");
            this.add("K");
            this.add("R");
            this.add("B");
            this.add("N");
            this.add("Q");
            this.add("N");
        }};
        boolean secondBishopAdded = false;
        String firstRank = "";

        Collections.shuffle(indexes);

        int[] rookKingSquares = new int[]{indexes.get(0), indexes.get(1), indexes.get(2)};

        for(int i = 0; i < rookKingSquares.length; i++){
            for(int j = i + 1; j < rookKingSquares.length; j++){
                if(rookKingSquares[i] > rookKingSquares[j]){
                    int temp = rookKingSquares[j];
                    rookKingSquares[j] = rookKingSquares[i];
                    rookKingSquares[i] = temp;
                }
            }
        }
        //adding rooks & king
        for(int i = 0; i < rookKingSquares.length; i++){
            pieces[rookKingSquares[i]] = pieceStrings.poll();
        }
        //adding bishop
        pieces[indexes.get(3)] = pieceStrings.poll();
        //adding bishop
        for(int i = 4; i < indexes.size(); i++){
            if(indexes.get(i) % 2 != indexes.get(3) % 2 && !secondBishopAdded){
                pieces[indexes.get(i)] = "B";
                secondBishopAdded = true;
            } else {
                //adding knights & queen
                pieces[indexes.get(i)] = pieceStrings.poll();
            }
        }

        for(int i = 0; i < pieces.length; i++){
            firstRank += pieces[i];
        }

        return firstRank;
    }
}
