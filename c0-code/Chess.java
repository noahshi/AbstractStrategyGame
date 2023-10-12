import java.util.*;
import java.util.regex.*;
// Noah Shi
// 10/4/2023
// CSE 123
// Creative Project 0: Abstract Strategy Game
// TA: Suhani Arora

//This is the class that creates and runs chess games
//It implements the AbstractStrategyGame interface
public class Chess implements AbstractStrategyGame{
    
    private Board board;
    private int gameType; //-1 for not chosen
    private Map<String, Integer> threeFoldCheck;
    private String startingBoardInFEN;
    private int winner;
    private boolean drawInitiated, drawInitiatedThisRound;

    private enum PieceType {
            PAWN,
            KNIGHT,
            BISHOP,
            ROOK,
            QUEEN,
            KING
    }
    
    //constants to make the code easier to read
    private static final Pattern coordinateRegex = Pattern.compile(
        "^[a-h][1-8][a-h][1-8](=[NBRQ])?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern algebraicRegex = Pattern.compile(
        "^([NBRQK])([a-h])?([1-8])?[x|@]?([a-h])([1-8])[+$#]?|"
        + "([a-h]x|@)?([a-h])([1-8])(=[NBRQ]| ?e\\.p\\.)?[+$#]?|O-O(-O)?[+$#]?$");

    private static final Pattern drawRegex = Pattern.compile("^draw$", 
        Pattern.CASE_INSENSITIVE);

    private static final Pattern resignRegex = Pattern.compile("^resign$", 
        Pattern.CASE_INSENSITIVE);

    private static final int BOARD_SIZE = 8;
    private static final String DEFAULT_BOARD_SETUP = 
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final int KINGSIDE = 0;
    private static final int QUEENSIDE = 1;
    private static final int KINGSIDE_CASTLE_FILE = 6;
    private static final int KINGSIDE_CASTLE_FILE_ROOK = 5;
    private static final int QUEENSIDE_CASTLE_FILE = 2;
    private static final int QUEENSIDE_CASTLE_FILE_ROOK = 3;
    private static final int WHITE_FIRST_RANK = 7;
    private static final int BLACK_FIRST_RANK = 0;

    private static final int DRAW = 0;
    private static final int PLAYER_WHITE = 1;
    private static final int PLAYER_BLACK = 2;

    private static final HashMap<PieceType, String> TYPE_TO_STRING = new HashMap<>(){{
        put(PieceType.PAWN, "P");
        put(PieceType.KNIGHT, "N");
        put(PieceType.BISHOP, "B");
        put(PieceType.ROOK, "R");
        put(PieceType.QUEEN, "Q");
        put(PieceType.KING, "K");
    }};

    private static final String[] VARIANTS = new String[]{"STANDARD", "CHESS960", "CRAZYHOUSE", 
        "ATOMIC"};

    //Behavior
    //  - Creates a chess game and will set up the default board
    public Chess(){
        gameType = -1;
        threeFoldCheck = new HashMap<>();
        startingBoardInFEN = DEFAULT_BOARD_SETUP;
        winner = -1;
        drawInitiated = false;
        drawInitiatedThisRound = false;

        setUpBoard(gameType);
    }

    //Parameters:
    //  - The variant as an integer where 0 is standard chess, 1 is Chess960, 2 is Crazyhouse, 
    //    and 3 is atomic
    //Behavior:
    //  - Sets up the board based off the variant
    private void setUpBoard(int gameType){
        if(gameType != 1){ //1 is Chess960
            startingBoardInFEN = DEFAULT_BOARD_SETUP;
        } else {
            String randomizedPieces = chess960Randomizer();
            startingBoardInFEN = randomizedPieces.toLowerCase() 
                + "/pppppppp/8/8/8/8/PPPPPPPP/" + randomizedPieces.toUpperCase() + " w KQkq - 0 1";
        }

        board = new Board(startingBoardInFEN);
        this.gameType = gameType;

        threeFoldCheck.clear();
        threeFoldCheck.put(startingBoardInFEN, 1);
        winner = -1;
    }


    //Returns:
    //  - Game instructions
    public String instructions(){
        return "White moves first. Type in your move in either coordinate or algebraic notation."
               + "\nPieces are labeled with their respective names in algebraic notation."
               + "\nPawns can only move 1 square toward the opposite side of the board"
               + "\nunless it is its first move, where it can move 2 squares forward. Pawns"
               + "\ncan only capture one square diagonally on the two diagonals that face"
               + "\ntoward the opposite side of the board. Knights can only move 2 squares"
               + "\nin one of the 4 cardinal directions, followed by 1 square at a 90 degree"
               + "\nangle. Knights are the only piece that can jump over other pieces."
               + "\nBishops can move along a diagonal. Rooks move along the 4 cardinal directions."
               + "\nQueens have the movement of both bishops and rooks. Kings can move to 1"
               + "\nthe 8 adjacent squares to it. You cannot move your piece onto a square that"
               + "\nis already occupied by your own piece. A piece cannot move any further for"
               + "\nthe turn once it occupies a square that is already occupied by an enemy"
               + "\npiece. If you occupy a square that has an enemy piece on it, that enemy"
               + "\npiece gets removed from the game. If your king is under attack, you are"
               + "\nin check. You are not allowed to end your turn in check. If a player is in"
               + "\ncheck and has no legal moves, the game is over and that player has lost."
               + "\nIf a player has no legal moves and is not in check, the game is a draw."
               + "\nThe game also ends in a draw if a position appears 3 times, if there is"
               + "\nnot enough material for either side to force checkmate, or if 50 moves"
               + "\nhave passed and there haven't been any pawn moves or captures. A player"
               + "\ncan also choose to forfeit by resigning.";
    }

    //Returns:
    //  - A string that either asks for a game type if a game type has not been selected yet 
    //  - A string that represents the current state of the chess board
    //  - A string that represents the final state of the board, a string the starting position
    //    in Forsyth-Edwards Notation, the moves made, and the result if the game is over
    public String toString(){
        if(isGameOver()){
            return board.toString(gameType == 2) + "\n" + "VARIANT: " + VARIANTS[gameType] 
                + "\n" + getMoves();
        }
        if(gameType != -1){
            return board.toString(gameType == 2) + 
                (drawInitiated ? "\nPlayer " + getNextPlayer() + " has offered a draw."
                + "\nType \"draw\" to accept, play a move to decline." : "");
        
        } else{ 
            String variantChoice = "Enter the variant you want to play.";
             for(int i = 0; i < VARIANTS.length; i++){
                variantChoice += "\n" + i + " for " + VARIANTS[i];
             }
             return variantChoice;
        }
    }

    //Returns:
    //  - true if the game is over
    //  - false if the game isn't over
    public boolean isGameOver(){
        return winner != -1;
    }

    //Returns:
    //  - The winner if there is one
    //  - 0 if it's a draw
    //  - -1 if the game is still ongoing
    public int getWinner(){
        return winner;
    }

    //Returns: 
    //  - 1 if it's Player 1's turn
    //  - 2 if it's Player 2's turn
    //  - -1 if the game is over
    public int getNextPlayer(){
        if(isGameOver())
            return -1;

        return board.whiteTurn ? PLAYER_WHITE : PLAYER_BLACK;
    }

    //Parameters:
    // - This method takes a string input from the terminal
    //Behavior:
    //  - In coordinate notation, if a king move and castling end up on the same square, the
    //    normal king move takes priority
    //  - If an exception is not thrown, the move will be played on the board
    //Exceptions:
    //  - If game type has not been selected, the string input must be an integer from 0-3, 
    //    otherwise an exception will be thrown
    //  - If game type has been selected, the string input must either be in coordinate or 
    //    algebraic notation
    //  - Illegal and nonexistent moves with throw exceptions
    public void makeMove(Scanner input){
        drawInitiatedThisRound = false;
        String move = input.nextLine();
        if(gameType == -1){
            int type = Integer.parseInt(move);
            if(type >= VARIANTS.length || type < 0){
                throw new IllegalArgumentException("There are only " + VARIANTS.length 
                    + " game types, numbered from 0-3.");
            }
            gameType = type;
            setUpBoard(gameType);
        } else if (resignRegex.matcher(move).matches()){
            winner = board.whiteTurn ? PLAYER_BLACK : PLAYER_WHITE;
            board.portableGameNotation += board.whiteTurn ? " 0-1" : " 1-0";
            
        } else if(drawRegex.matcher(move).matches() && drawInitiated){
            winner = DRAW;
            board.portableGameNotation += " 1/2-1/2";

        } else {
            if (drawRegex.matcher(move).matches()){
                drawInitiatedThisRound = true;
                move = input.nextLine();
            }

            Matcher cdnMatcher = coordinateRegex.matcher(move);
            Matcher algMatcher = algebraicRegex.matcher(move);
            
            boolean legalMove = false;
            boolean isKingSideCastle = false;

            int startFile = -1;
            int startRank = -1;
            int endFile = -1;
            int endRank = -1;

            Piece promotionPiece = null;
            Piece placedPiece = null;

            if(cdnMatcher.matches()){
                startFile = (int)move.charAt(0) - (int)'a';
                startRank = BOARD_SIZE - Character.getNumericValue(move.charAt(1));
                endFile = (int)move.charAt(2) - (int)'a';
                endRank = BOARD_SIZE - Character.getNumericValue(move.charAt(3));

                if(cdnMatcher.group(1) != null){
                    if(board.board[startFile][startRank].pieceType != PieceType.PAWN){
                        throw new IllegalArgumentException("Only pawns can promote.");
                    }
                    if(endRank != (board.whiteTurn ? BLACK_FIRST_RANK : WHITE_FIRST_RANK)){
                        throw new IllegalArgumentException(
                            "Pawns can only promote when they reach the end of the board.");
                    }
                    String promotionChar = cdnMatcher.group(1).charAt(1) + "";
                    if(promotionChar.equals("N")){
                        promotionPiece = new Piece(PieceType.KNIGHT, board.whiteTurn, 
                            new Square(endFile, endRank), true);

                    } else if(promotionChar.equals("B")){
                        promotionPiece = new Piece(PieceType.BISHOP, board.whiteTurn, 
                            new Square(endFile, endRank), true);

                    } else if(promotionChar.equals("R")){
                        promotionPiece = new Piece(PieceType.ROOK, board.whiteTurn, 
                            new Square(endFile, endRank), true);

                    } else {
                        promotionPiece = new Piece(PieceType.QUEEN, board.whiteTurn, 
                            new Square(endFile, endRank), true);
                    }
                } else {
                    if(board.board[startFile][startRank].pieceType == PieceType.PAWN && endRank 
                        == (board.whiteTurn ? BLACK_FIRST_RANK : WHITE_FIRST_RANK)){
                        
                        throw new IllegalArgumentException(
                            "Ambiguous move input. Please specify the promotion.");
                    }
                }

                if(board.board[startFile][startRank] == null){
                    throw new IllegalArgumentException("Piece does not exist.");
                }

                if(board.board[startFile][startRank].isWhite != board.whiteTurn){
                    throw new IllegalArgumentException("That is not your piece.");
                }

                if(board.board[startFile][startRank].pieceType == PieceType.KING 
                    && endFile - startFile > 1){
                    
                    isKingSideCastle = true;
                }
            //typing symbols used in chess notation such as x + $ # etc. is allowed for extra 
            //leniency but they will be disregarded in cases where they should not be used.
            } else if (algMatcher.matches()){
                if(move.contains("@") && gameType != 2){
                    throw new IllegalArgumentException(
                        "This is not Crazyhouse. You can't place pieces back down.");
                }
                boolean doesMoveExist = false;
                //System.out.println(board.boardToFENShort());
                
                //castling
                if(move.contains("O-O")){
                    //System.out.println("castling move inputted");
                    if(algMatcher.group(10) != null){
                        if(board.whiteTurn ? board.whiteCastlingRights[QUEENSIDE]
                            : board.blackCastlingRights[QUEENSIDE]){

                            doesMoveExist = true;
                        }
                        endFile = QUEENSIDE_CASTLE_FILE;
                        isKingSideCastle = false;
                    } else {
                        if(board.whiteTurn ? board.whiteCastlingRights[KINGSIDE]
                            : board.blackCastlingRights[KINGSIDE]){

                            doesMoveExist = true;
                        }
                        endFile = KINGSIDE_CASTLE_FILE;
                        isKingSideCastle = true;
                    }
                    startFile = board.whiteTurn ? board.whiteKingSquare.file 
                        : board.blackKingSquare.file;

                    startRank = board.whiteTurn ? WHITE_FIRST_RANK : BLACK_FIRST_RANK;
                    endRank = board.whiteTurn ? WHITE_FIRST_RANK : BLACK_FIRST_RANK;

                    //System.out.println("castling will start on " + 
                    //    new Square(startFile, startRank).toString() + " and end on " 
                    //    + new Square(endFile, endRank));
                
                //piece movement
                } else if(algMatcher.group(1) != null){

                    endFile = (int)algMatcher.group(4).charAt(0) - (int)'a';
                    endRank = 8 - Character.getNumericValue(algMatcher.group(5).charAt(0));

                    if(algMatcher.group(2) != null){
                        startFile = (int)algMatcher.group(2).charAt(0) - (int)'a';
                    } 
                    if(algMatcher.group(3) != null){
                        startRank = 8 - Character.getNumericValue(algMatcher.group(3)
                            .charAt(0));
                    }
                    int tempStartRank = -1;
                    int tempStartFile = -1;
                    if(startRank == -1 || startFile == -1){
                        for(Piece piece : board.whiteTurn ? board.whitePieces : board.blackPieces){
                            if(TYPE_TO_STRING.get(piece.pieceType).equals(algMatcher.group(1))){
                                //System.out.println("Piece specified is " 
                                //    + TYPE_TO_STRING.get(piece.pieceType));
                                for(Square square : board.whiteTurn ? board.whiteControlledSquares
                                    .get(piece) : board.blackControlledSquares.get(piece)){

                                    //System.out.println("checking if move " + TYPE_TO_STRING
                                    //    .get(piece.pieceType) + (char)(square.file + (int)'a') 
                                    //    + "" + (8 - square.rank) + " equals inputted move");

                                    if(square.file == endFile && square.rank == endRank){
                                        
                                        if(startFile != -1 && startRank != -1){
                                            throw new IllegalArgumentException(
                                                "Ambiguous move input. " 
                                                + "Please specify which piece you want to move");
                                        }

                                        //moves in the format of Nbc3
                                        if(startFile != -1){
                                            if(startFile == piece.square.file){
                                                if(tempStartRank != -1){
                                                    throw new IllegalArgumentException(
                                                        "Ambiguous move input. " 
                                                        + "Please specify which piece you want "
                                                        + "to move");
                                                }
                                                tempStartRank = piece.square.rank;
                                                doesMoveExist = true;
                                            }

                                        //moves in the format of N1c3
                                        } else if (startRank != -1) {
                                            if(startRank == piece.square.rank){
                                                if(tempStartFile != -1){
                                                    throw new IllegalArgumentException(
                                                        "Ambiguous move input. " 
                                                        + "Please specify which piece you want "
                                                        + "to move");
                                                }
                                                tempStartFile = piece.square.file;
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
                    }
                    if(tempStartFile != -1){
                        startFile = tempStartFile;
                    }
                    if(tempStartRank != -1){
                        startRank = tempStartRank;
                    }

                //pawn movement
                } else if(algMatcher.group(7) != null){
                    endFile = (int)algMatcher.group(7).charAt(0) - (int)'a';
                    endRank = 8 - Character.getNumericValue(algMatcher.group(8).charAt(0));

                    //moves in the format of exd5
                    if(algMatcher.group(6) != null){
                        startFile = (int)algMatcher.group(6).charAt(0) - (int)'a';

                    //moves in the format of e4
                    } else {
                        startFile = endFile;
                    }
                    for(Piece piece : board.whiteTurn ? board.whitePieces : board.blackPieces){
                        if(piece.pieceType == PieceType.PAWN && piece.square.file == startFile){
                            for(Square square : board.whiteTurn ? board.whiteControlledSquares
                                .get(piece) : board.blackControlledSquares.get(piece)){

                                if(square.file == endFile && square.rank == endRank){
                                    startFile = piece.square.file;
                                    startRank = piece.square.rank;
                                    doesMoveExist = true;
                                }
                            }
                            for(Square square : board.getPossiblePawnMoves(new Square(
                                piece.square.file, piece.square.rank), board.whiteTurn)){

                                if(square.file == endFile && square.rank == endRank){
                                    startFile = piece.square.file;
                                    startRank = piece.square.rank;
                                    doesMoveExist = true;
                                }
                            }
                        }
                    } //need to throw exception before promotion check or startrank could be null
                    if(!doesMoveExist && !move.contains("@")){ 
                        throw new IllegalArgumentException("Move does not exist.");
                    }
                    //promotion checker (moves in the format of a1=Q or bxc8=B)
                    if(!move.contains("@") && algMatcher.group(9) != null && 
                        !algMatcher.group(9).equals(" e.p.") 
                        && !algMatcher.group(9).equals("e.p.")){
                        
                            if(endRank != (board.whiteTurn ? BLACK_FIRST_RANK : WHITE_FIRST_RANK)){
                            throw new IllegalArgumentException(
                                "Pawns can only promote when they reach the end of the board.");
                        }
                        String promotionChar = algMatcher.group(9).charAt(1) + "";
                        if(promotionChar.equals("N")){
                            promotionPiece = new Piece(PieceType.KNIGHT, board.whiteTurn, 
                                new Square(endFile, endRank), true);

                        } else if(promotionChar.equals("B")){
                            promotionPiece = new Piece(PieceType.BISHOP, board.whiteTurn, 
                                new Square(endFile, endRank), true);

                        } else if(promotionChar.equals("R")){
                            promotionPiece = new Piece(PieceType.ROOK, board.whiteTurn, 
                                new Square(endFile, endRank), true);

                        } else {
                            promotionPiece = new Piece(PieceType.QUEEN, board.whiteTurn,
                                 new Square(endFile, endRank), true);
                        }
                    } else {
                        if(!move.contains("@") && board.board[startFile][startRank].pieceType 
                            == PieceType.PAWN && endRank == (board.whiteTurn ? BLACK_FIRST_RANK 
                            : WHITE_FIRST_RANK)){

                            throw new IllegalArgumentException(
                                "Ambiguous move input. Please specify the promotion.");
                        }
                    }
                }
                if(move.contains("@")){
                    for(Piece piece : board.whiteTurn ? board.whiteCapturedPieces 
                        : board.blackCapturedPieces){

                        if(algMatcher.group(1) != null){
                            if(TYPE_TO_STRING.get(piece.pieceType).equals(algMatcher.group(1))){
                                doesMoveExist = true;
                                placedPiece = piece;
                            }
                        } else {
                            if(piece.pieceType == PieceType.PAWN){
                                doesMoveExist = true;
                                placedPiece = piece;
                            }
                        }
                    }
                }

                if(!doesMoveExist){
                    throw new IllegalArgumentException("Move does not exist.");
                }

            } else {
                throw new IllegalArgumentException("Input is in the wrong format.");
            }

            Piece piece = null;
            if(move.contains("@")){
                piece = placedPiece;
                board.fiftyMoveRuleCounter = -1;
            } else {
                piece = board.board[startFile][startRank];
            }

            Map<Piece, Set<Square>> allLegalMoves = getLegalMoves();

            for(Square moveCheck : allLegalMoves.get(piece)){
                if(moveCheck.file == endFile && moveCheck.rank == endRank){
                    legalMove = true;
                }
                //System.out.println(TYPE_TO_STRING.get(piece.pieceType)+(char)(legal.file 
                //    + (int)'a') + (8-legal.rank));
            }

            if(!legalMove){
                throw new IllegalArgumentException("Piece cannot move there.");
            }

            if(placedPiece != null) {
                if(piece.isWhite){
                    board.whitePieces.add(piece);
                    board.whiteControlledSquares.put(piece, new HashSet<>());
                    board.whiteCapturedPieces.remove(piece);
                } else {
                    board.blackPieces.add(piece);
                    board.blackControlledSquares.put(piece, new HashSet<>());
                    board.blackCapturedPieces.remove(piece);
                }
            }

            if(board.whiteTurn){
                board.portableGameNotation += board.moveNumber + ".";
            }
            
            
            boolean isCapture = false;

            if(promotionPiece != null){
                isCapture = board.move(piece, new Square(endFile, endRank), promotionPiece);
                board.portableGameNotation += move.substring(2);
            } else if(move.contains("O-O") || (piece.pieceType == PieceType.KING 
                && Math.abs(startFile - endFile) > 1)){
                    
                isCapture = board.move(piece, new Square(endFile, endRank), isKingSideCastle);
                
                if(isKingSideCastle){
                    board.portableGameNotation += "O-O";
                } else {
                    board.portableGameNotation += "O-O-O";
                }
            } else {
                if(piece.pieceType != PieceType.PAWN){
                    board.portableGameNotation += TYPE_TO_STRING.get(piece.pieceType);
                }

                if(!move.contains("@")){
                    List<Piece> extraAttackers = new ArrayList<>();
                    for(Piece tempSecondAttacker : board.whiteTurn ? board.whitePieces 
                        : board.blackPieces){
                        
                        for(Square square : allLegalMoves.get(tempSecondAttacker)){
                            if(!piece.equals(tempSecondAttacker) && piece.pieceType 
                            == tempSecondAttacker.pieceType && square.rank == endRank && square.file 
                            == endFile){
                                
                                extraAttackers.add(tempSecondAttacker);
                            }
                        }
                    }
                    boolean dupeFile = false;
                    boolean dupeRank = false;
                    if(extraAttackers.size() != 0){
                        for(Piece extraAttacker : extraAttackers){
                            if(extraAttacker.square.file == startFile){
                                dupeFile = true;
                            }
                            if(extraAttacker.square.rank == startRank){
                                dupeRank = true;
                            }
                        }
                        if(!dupeFile){
                            board.portableGameNotation += "" + (char)(startFile + (int)'a');
                        } else if(!dupeRank){
                            board.portableGameNotation += (8 - startRank);
                        } else {
                            board.portableGameNotation += "" + (char)(startFile + (int)'a');
                            board.portableGameNotation += (8 - startRank);
                        }
                    }

                    
                
                }
                isCapture = board.move(piece, new Square(endFile, endRank));
                if(move.contains("@")){
                    board.portableGameNotation += "@";
                }
                board.portableGameNotation += new Square(endFile, endRank).toString();
            }

            if(isCapture){
                if(gameType == 3){
                    int kingsExplodeIndex = board.atomicCaptureExplosion(
                        new Square(endFile, endRank));

                    if(kingsExplodeIndex == 1){
                        winner = PLAYER_BLACK;
                        board.portableGameNotation += " 0-1";

                    } else if (kingsExplodeIndex == 2){
                        winner = PLAYER_WHITE;
                        board.portableGameNotation += " 1-0";

                    } else if(kingsExplodeIndex == 3){
                        winner = DRAW;
                        board.portableGameNotation += " 1/2-1/2";
                    }
                }
            }

            board.calculateAttacks();

            drawInitiated = drawInitiatedThisRound;

            //removing castling rights if king or rook moved
            if(piece.pieceType == PieceType.KING){
                if(board.whiteTurn){
                    if(board.whiteCastlingRights[KINGSIDE] 
                        || board.whiteCastlingRights[QUEENSIDE]){
                        
                        threeFoldCheck.clear();
                    }
                    board.whiteCastlingRights[KINGSIDE] = false;
                    board.whiteCastlingRights[QUEENSIDE] = false;
                } else {
                    if(board.blackCastlingRights[KINGSIDE] 
                        || board.blackCastlingRights[QUEENSIDE]){
                        
                        threeFoldCheck.clear();
                    }
                    board.blackCastlingRights[KINGSIDE] = false;
                    board.blackCastlingRights[QUEENSIDE] = false;
                }
            }
            if(piece.pieceType == PieceType.ROOK){
                if(board.whiteTurn){
                    if(board.kingsideCastleRooks[0] != null 
                        && board.kingsideCastleRooks[0].equals(piece)){
                        
                        if(board.whiteCastlingRights[KINGSIDE]){
                            threeFoldCheck.clear();
                        }
                        board.whiteCastlingRights[KINGSIDE] = false;
                    } else if(board.queensideCastleRooks[0] != null 
                        && board.queensideCastleRooks[0].equals(piece)){
                        
                        if(board.whiteCastlingRights[QUEENSIDE]){
                            threeFoldCheck.clear();
                        }
                        board.whiteCastlingRights[QUEENSIDE] = false;
                    }
                } else {
                    if(board.kingsideCastleRooks[1] != null 
                        && board.kingsideCastleRooks[1].equals(piece)){
                        
                        if(board.blackCastlingRights[KINGSIDE]){
                            threeFoldCheck.clear();
                        }
                        board.blackCastlingRights[KINGSIDE] = false;
                    } else if(board.queensideCastleRooks[1] != null 
                        && board.queensideCastleRooks[1].equals(piece)){
                        
                        if(board.blackCastlingRights[QUEENSIDE]){
                            threeFoldCheck.clear();
                        }
                        board.blackCastlingRights[QUEENSIDE] = false;
                    }
                }
            }

            if(!board.whiteTurn){
                board.moveNumber ++;
            }

            board.whiteTurn = !board.whiteTurn;
            board.fiftyMoveRuleCounter ++;

            //checking for end scenarios below

            //INSUFFICIENT MATERIAL
            int whiteBishopCounter = 0;
            int whiteKnightCounter = 0;
            int blackBishopCounter = 0;
            int blackKnightCounter = 0;
            boolean insufficientMaterial = true;
            for(Piece material : board.whitePieces){
                    if(material.pieceType == PieceType.KNIGHT){
                        whiteKnightCounter ++;
                    }
                    if(material.pieceType == PieceType.BISHOP){
                        whiteBishopCounter ++;
                    }
                    if(material.pieceType == PieceType.PAWN || material.pieceType 
                        == PieceType.ROOK || material.pieceType == PieceType.QUEEN){
                        
                        insufficientMaterial = false;
                    }
                }
                for(Piece material : board.blackPieces){
                    if(material.pieceType == PieceType.KNIGHT){
                        blackKnightCounter ++;
                    }
                    if(material.pieceType == PieceType.BISHOP){
                        blackBishopCounter ++;
                    }
                    if(material.pieceType == PieceType.PAWN || material.pieceType 
                        == PieceType.ROOK || material.pieceType == PieceType.QUEEN){
                        
                        insufficientMaterial = false;
                    }
                }
            if(gameType == 2){
                insufficientMaterial = false;
            }
            if(whiteKnightCounter + whiteBishopCounter + blackKnightCounter 
                + blackBishopCounter > 2){
                
                insufficientMaterial = false;
            }
            if(whiteKnightCounter + whiteBishopCounter == 2 && whiteBishopCounter != 0){
                insufficientMaterial = false;
            }
            if(blackKnightCounter + blackBishopCounter == 2 && blackBishopCounter != 0){
                insufficientMaterial = false;
            }

            if(gameType == 3){
                if(whiteKnightCounter + whiteBishopCounter > 0 
                    && blackKnightCounter + blackBishopCounter > 0){
                    
                    insufficientMaterial = false;
                }
            }

            if(insufficientMaterial){
                winner = 0;
                board.portableGameNotation += "$";
                board.portableGameNotation += " 1/2-1/2";
            }

            //3 FOLD REPETITION
            String currentBoardAsFEN = board.boardToFENShort();
            if(threeFoldCheck.get(currentBoardAsFEN) == null){
                threeFoldCheck.put(currentBoardAsFEN, 1);
            } else {
                threeFoldCheck.replace(currentBoardAsFEN, 
                    threeFoldCheck.get(currentBoardAsFEN) + 1);

                if(threeFoldCheck.get(currentBoardAsFEN) == 3){
                    winner = 0;
                    board.portableGameNotation += "$";
                    board.portableGameNotation += " 1/2-1/2";
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
                    //whiteTurn boolean was inverted above so this ternary operator must be flipped
                    winner = board.whiteTurn ? PLAYER_BLACK : PLAYER_WHITE; 
                    board.portableGameNotation += "#";
                    board.portableGameNotation += board.whiteTurn ? " 0-1" : " 1-0";
                } else {
                    winner = DRAW;
                    board.portableGameNotation += "$";
                    board.portableGameNotation += " 1/2-1/2";
                }
            }

            //FIFTY MOVE RULE
            if(board.fiftyMoveRuleCounter > 100){
                winner = 0;
                board.portableGameNotation += "$";
                board.portableGameNotation += " 1/2-1/2";
            }

            //checking for end scenarios above

            if(board.playerInCheck[board.whiteTurn ? 0 : 1] && winner == -1){
                board.portableGameNotation += "+";
            }
            board.portableGameNotation += " ";

            //if(enPassantSquare != null)
            //    System.out.println(enPassantSquare[0] + " " + enPassantSquare[1]);
        }
    }

    //Returns: 
    //  - The starting position as a string in Forsyth-Edwards Notation 
    //    followed by all the moves played
    private String getMoves(){
        return startingBoardInFEN += "\n" + board.portableGameNotation;
    }

    
    //Behavior
    //  - takes all piece attacks and determines which ones of those are legal moves
    //Returns: 
    //  - All the legal moves that a player can play for their turn
    private Map<Piece, Set<Square>> getLegalMoves(){
        Map<Piece, Set<Square>> legalMoves = new HashMap<>();
        //System.out.println(currentFEN);
        for(Piece piece : board.whiteTurn ? board.whitePieces : board.blackPieces){
            //System.out.println("checking piece " + TYPE_TO_STRING.get(piece.pieceType) 
            //    + " on " + (char)(piece.square.file + (int)'a') + "" + (8 - piece.square.rank)
            //    + " for white");
            legalMoves.put(piece, new HashSet<Square>());
            for(Square move : board.whiteTurn ? board.whiteControlledSquares.get(piece) 
                : board.blackControlledSquares.get(piece)){

                legalMoves.get(piece).add(move);
                if(board.board[move.file][move.rank] != null && board.whiteTurn 
                    == board.board[move.file][move.rank].isWhite){

                    legalMoves.get(piece).remove(move);
                }
            }
            if(piece.pieceType == PieceType.PAWN && piece.square.file >= 0){
                //removing empty pawn captures from legal move list
                Set<Square> emptyCaptures = new HashSet<>();
                for(Square move : legalMoves.get(piece)){
                    if(board.board[move.file][move.rank] == null){
                        emptyCaptures.add(move);
                    }
                    
                    //re-adding en passant
                    if(board.enPassantSquare != null && board.enPassantSquare.file == move.file 
                        && board.enPassantSquare.rank == move.rank){

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
            if(piece.pieceType == PieceType.KING){
                if(board.whiteTurn ? board.whiteCastlingRights[KINGSIDE] 
                    : board.blackCastlingRights[KINGSIDE]){

                    int firstRank = board.whiteTurn ? WHITE_FIRST_RANK : BLACK_FIRST_RANK;
                    boolean illegalCastle = false;
                    for(int currentFile = piece.square.file; currentFile <= KINGSIDE_CASTLE_FILE;
                         currentFile++){

                        if(board.board[currentFile][firstRank] != null 
                            && board.board[currentFile][firstRank].pieceType != PieceType.ROOK
                            && board.board[currentFile][firstRank].pieceType != PieceType.KING){

                            illegalCastle = true;
                            //System.out.println("kingside castle is blocked on " 
                            //    + new Square(currentFile, firstRank).toString());
                        }
                        for(Piece enemyPiece : board.whiteTurn ? board.blackPieces 
                            : board.whitePieces){
                                
                            for(Square square : board.whiteTurn ? board.blackControlledSquares
                                .get(enemyPiece) : board.whiteControlledSquares.get(enemyPiece)){

                                if(square.file == currentFile && square.rank == firstRank){
                                    illegalCastle = true;
                                    //System.out.println("kingside castle is attacked" 
                                    //    + new Square(currentFile, firstRank).toString());
                                }
                            }
                        }
                    }
                    if(board.board[KINGSIDE_CASTLE_FILE_ROOK][firstRank] != null 
                        && board.board[KINGSIDE_CASTLE_FILE_ROOK][firstRank].pieceType 
                        != PieceType.KING){

                        illegalCastle = true;
                    }
                    if(!illegalCastle){
                        legalMoves.get(piece).add(new Square(KINGSIDE_CASTLE_FILE, firstRank));
                        //System.out.println("added kingside castle to legal moves");
                    }
                }
                if(board.whiteTurn ? board.whiteCastlingRights[QUEENSIDE] 
                    : board.blackCastlingRights[QUEENSIDE]){

                    int firstRank = board.whiteTurn ? WHITE_FIRST_RANK : BLACK_FIRST_RANK;
                    boolean illegalCastle = false;
                    for(int currentFile = piece.square.file; currentFile >= QUEENSIDE_CASTLE_FILE;
                        currentFile--){

                        if(board.board[currentFile][firstRank] != null 
                            && board.board[currentFile][firstRank].pieceType != PieceType.ROOK
                            && board.board[currentFile][firstRank].pieceType != PieceType.KING){

                            illegalCastle = true;
                            //System.out.println("queenside castle is blocked on " 
                            //    + new Square(currentFile, firstRank).toString());
                        }
                        for(Piece enemyPiece : board.whiteTurn ? board.blackPieces 
                            : board.whitePieces){

                            for(Square square : board.whiteTurn ? board.blackControlledSquares
                                .get(enemyPiece) : board.whiteControlledSquares.get(enemyPiece)){
                                                                    
                                if(square.file == currentFile && square.rank == firstRank){
                                    illegalCastle = true;
                                    //System.out.println("queenside castle is attacked on " 
                                    //    + new Square(currentFile, firstRank).toString());
                                }
                            }
                        }
                    }
                    if(board.board[QUEENSIDE_CASTLE_FILE_ROOK][firstRank] != null 
                        && board.board[QUEENSIDE_CASTLE_FILE_ROOK][firstRank].pieceType 
                        != PieceType.KING){

                        illegalCastle = true;
                    }
                    if(!illegalCastle){
                        legalMoves.get(piece).add(new Square(QUEENSIDE_CASTLE_FILE, firstRank));
                        //System.out.println("added queenside castle");
                    }
                }
            }
        }
        removeIllegalMoves(board.whiteTurn ? board.whitePieces : board.blackPieces, legalMoves);

        if(gameType == 2){
            for(Piece piece : board.whiteTurn ? board.whiteCapturedPieces 
                : board.blackCapturedPieces){

                legalMoves.put(piece, new HashSet<>());
                for(int rank = 1; rank < BOARD_SIZE - 1; rank ++){
                    for(int file = 0; file < BOARD_SIZE; file ++){
                        if(board.board[file][rank] == null){
                            legalMoves.get(piece).add(new Square(file, rank));
                        }
                    }
                }
                //cant place pawns on the 1st and 8th ranks
                if(piece.pieceType != PieceType.PAWN){
                    for(int file = 0; file < BOARD_SIZE; file ++){
                        if(board.board[file][BLACK_FIRST_RANK] == null){
                            legalMoves.get(piece).add(new Square(file, BLACK_FIRST_RANK));
                        }
                    }
                    for(int file = 0; file < BOARD_SIZE; file ++){
                        if(board.board[file][WHITE_FIRST_RANK] == null){
                            legalMoves.get(piece).add(new Square(file, WHITE_FIRST_RANK));
                        }
                    }
                }
            }
            removeIllegalMoves(board.whiteTurn ? board.whiteCapturedPieces 
                : board.blackCapturedPieces, legalMoves);
        }
        return legalMoves;
    }

    //Parameters:
    //  - Piece list that contains the pieces that must be checked
    //  - Move set for each piece
    //Behavior:
    //  - removes the moves that leave the player's own king or put the player's own king in check
    //    from the inputted move sets
    private void removeIllegalMoves(List<Piece> pieceList, Map<Piece, Set<Square>> moves){
        String currentFEN = board.boardToFENFull();
        for(Piece piece : pieceList){
            Set<Square> illegalMoves = new HashSet<>();
            for(Square square : moves.get(piece)){
                Board testBoard = new Board(currentFEN);
                // System.out.println("testing move " + TYPE_TO_STRING.get(piece.pieceType) 
                //    + " on " + (char)(piece.square.file + (int)'a') + "" 
                //    + (8 - piece.square.rank) + " to " + (char)(square.file + (int)'a') + "" 
                //    + (8 - square.rank) + " for white");
                
                //special logic for placing pieces in crazyhouse
                if(piece.square.file < 0){
                    Piece testPiece = new Piece(piece.pieceType, piece.isWhite, piece.square,
                        piece.isPromoted);

                    testBoard.move(testPiece, square);
                
                //regular move logic
                } else {
                    testBoard.move(testBoard.board[piece.square.file][piece.square.rank], square);
                }
                testBoard.calculateAttacks();
                if(testBoard.playerInCheck[board.whiteTurn ? 0 : 1]){
                    illegalMoves.add(square);
                }
            }
            for(Square square : illegalMoves){
                moves.get(piece).remove(square);
            }
        }
    }

    //Returns:
    //  - A string with the order of the 8 back rank pieces randomized with a few restaints
    //        - The king must be between the 2 rooks
    //        - The bishops must be on opposite colors
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

        int[] rookKingSquares = new int[]{indexes.get(0), indexes.get(1), 
            indexes.get(2)};

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


    //This helper class contains all the information related to a board state
    private class Board {
        private Piece[][] board;

        private List<Piece> whitePieces, blackPieces;
        private List<Piece> whiteCapturedPieces, blackCapturedPieces;
        private Map<Piece, Set<Square>> whiteControlledSquares, blackControlledSquares;
        private boolean[] whiteCastlingRights, blackCastlingRights;

        private boolean[] playerInCheck;
        private boolean whiteTurn;
        private Square enPassantSquare;
        private int fiftyMoveRuleCounter;
        private int moveNumber;
        
        private String portableGameNotation;

        private Square whiteKingSquare, blackKingSquare;
        private Piece[] kingsideCastleRooks,queensideCastleRooks;


        //Parameters:
        // - String in Forsyth-Edwards Notation
        //Behavior:
        //  - Creates a board based on the input string
        private Board(String boardInFEN){
            board = new Piece[BOARD_SIZE][BOARD_SIZE];
            whitePieces = new ArrayList<>();
            blackPieces = new ArrayList<>();
            whiteCapturedPieces = new ArrayList<>();
            blackCapturedPieces = new ArrayList<>();
            whiteControlledSquares = new HashMap<>();
            blackControlledSquares = new HashMap<>();
            whiteCastlingRights = new boolean[2];
            blackCastlingRights = new boolean[2];
            playerInCheck = new boolean[]{false, false};
            portableGameNotation = "";
            kingsideCastleRooks = new Piece[2];
            queensideCastleRooks = new Piece[2];

            String[] temp = boardInFEN.split(" ");
            
            String[] ranks = temp[0].split("/");
            for(int rank = 0; rank < ranks.length; rank++){
                int file = -1;
                for(int stringIndex = 0; stringIndex < ranks[rank].length(); stringIndex++){
                    file ++;
                    char currentChar = ranks[rank].charAt(stringIndex);
                    if(currentChar == 'p'){
                        board[file][rank] = new Piece(PieceType.PAWN, false, 
                            new Square(file, rank), false);

                        blackControlledSquares.put(board[file][rank], getPossiblePawnCaptures(
                            new Square(file, rank), false));

                    } else if(currentChar == 'n'){
                        board[file][rank] = new Piece(PieceType.KNIGHT, false, 
                            new Square(file, rank), false);

                        blackControlledSquares.put(board[file][rank], getPossibleKnightMoves(
                            new Square(file, rank)));

                    } else if(currentChar == 'b'){
                        board[file][rank] = new Piece(PieceType.BISHOP, false, 
                            new Square(file, rank), false);
                        blackControlledSquares.put(board[file][rank], getPossibleBishopMoves(
                            new Square(file, rank)));

                    } else if(currentChar == 'r'){
                        board[file][rank] = new Piece(PieceType.ROOK, false, new Square(
                                file, rank), false);
                        blackControlledSquares.put(board[file][rank], getPossibleRookMoves(
                                new Square(file, rank)));

                        if(rank == BLACK_FIRST_RANK){
                            if(queensideCastleRooks[1] == null){
                                queensideCastleRooks[1] = board[file][rank];
                            } else {
                                kingsideCastleRooks[1] = board[file][rank];
                            }
                        }

                    } else if(currentChar == 'q'){
                        board[file][rank] = new Piece(PieceType.QUEEN, false, 
                            new Square(file, rank), false);
                        blackControlledSquares.put(board[file][rank], 
                            getPossibleQueenMoves(new Square(file, rank)));

                    } else if(currentChar == 'k'){
                        board[file][rank] = new Piece(PieceType.KING, false, 
                            new Square(file, rank), false);

                        blackControlledSquares.put(board[file][rank], 
                            getPossibleKingMoves(new Square(file, rank)));

                        blackKingSquare = new Square(file, rank);

                    } else if(currentChar == 'P'){
                        board[file][rank] = new Piece(PieceType.PAWN, true, 
                            new Square(file, rank), false);

                        whiteControlledSquares.put(board[file][rank], getPossiblePawnCaptures(
                            new Square(file, rank), true));

                    } else if(currentChar == 'N'){
                        board[file][rank] = new Piece(PieceType.KNIGHT, true, 
                            new Square(file, rank), false);

                        whiteControlledSquares.put(board[file][rank], getPossibleKnightMoves(
                            new Square(file, rank)));

                    } else if(currentChar == 'B'){
                        board[file][rank] = new Piece(PieceType.BISHOP, true, 
                            new Square(file, rank), false);

                        whiteControlledSquares.put(board[file][rank], getPossibleBishopMoves(
                            new Square(file, rank)));

                    } else if(currentChar == 'R'){
                        board[file][rank] = new Piece(PieceType.ROOK, true, 
                            new Square(file, rank), false);

                        whiteControlledSquares.put(board[file][rank], getPossibleRookMoves(
                            new Square(file, rank)));

                        if(rank == WHITE_FIRST_RANK){
                            if(queensideCastleRooks[0] == null){
                                queensideCastleRooks[0] = board[file][rank];
                            } else {
                                kingsideCastleRooks[0] = board[file][rank];
                            }
                        }

                    } else if(currentChar == 'Q'){
                        board[file][rank] = new Piece(PieceType.QUEEN, true, 
                            new Square(file, rank), false);

                        whiteControlledSquares.put(board[file][rank], getPossibleQueenMoves(
                            new Square(file, rank)));

                    } else if(currentChar == 'K'){
                        board[file][rank] = new Piece(PieceType.KING, true, 
                            new Square(file, rank), false);

                        whiteControlledSquares.put(board[file][rank], getPossibleKingMoves(
                            new Square(file, rank)));

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
                whiteCastlingRights[KINGSIDE] = true;
            }
            if(temp[2].contains("Q")){
                whiteCastlingRights[QUEENSIDE] = true;
            }
            if(temp[2].contains("k")){
                blackCastlingRights[KINGSIDE] = true;
            }
            if(temp[2].contains("q")){
                blackCastlingRights[QUEENSIDE] = true;
            }

            if(temp[3].equals("-")){
                enPassantSquare = null;
            } else {
                enPassantSquare = new Square((int)temp[3].charAt(0) - (int)'a', 
                    BOARD_SIZE - Character.getNumericValue(temp[3].charAt(1)));
            }

            fiftyMoveRuleCounter = Integer.parseInt(temp[4]);
            moveNumber = Integer.parseInt(temp[5]);
            
        }

        //Parameters:
        //  - Square where the imaginary pawn will be placed
        //  - Boolean to determine which way the pawn faces: true for facing toward black,
        //    false for facing toward white
        //Behavior:
        //  - Calculates the set of all the possible squares a pawn can move to if it was located
        //    on the input square
        //Returns:
        //  - Returns the set of moves calculated in its behavior
        private Set<Square> getPossiblePawnMoves(Square square, boolean isWhite){
            Set<Square> moves = new HashSet<>();
            int direction = isWhite ? -1 : 1;
            //one square forward
            if(board[square.file][square.rank + direction] == null){
                moves.add(new Square(square.file, square.rank + direction));

                //two squares forward
                if(square.rank == (isWhite ? 6 : 1)){
                    if(board[square.file][square.rank + direction * 2] == null){
                        moves.add(new Square(square.file, square.rank + direction * 2));
                    }
                }
            }
            

            return moves;
        }

        //Parameters:
        //  - Square where the imaginary pawn will be placed
        //  - Boolean to determine which way the pawn faces: true for facing toward black, 
        //    false for facing toward white
        //Behavior:
        //  - Calculates the set of all the possible squares a pawn would control it was located
        //    on the input square
        //Returns:
        //  - Returns the set of moves calculated in its behavior
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

        //Parameters:
        //  - Square where the imaginary knight will be placed
        //Behavior:
        //  - Calculates the set of all the possible squares a knight would control it was 
        //    located on the input square
        //Returns:
        //  - Returns the set of moves calculated in its behavior
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

        //Parameters:
        //  - Square where the imaginary bishop will be placed
        //Behavior:
        //  - Calculates the set of all the possible squares a bishop would control it was
        //    located on the input square
        //Returns:
        //  - Returns the set of moves calculated in its behavior
        private Set<Square> getPossibleBishopMoves(Square square){
            Set<Square> moves = new HashSet<>();
            moves.addAll(getDirectionalAttacks(square, 1, 1));
            moves.addAll(getDirectionalAttacks(square, -1, 1));
            moves.addAll(getDirectionalAttacks(square, 1, -1));
            moves.addAll(getDirectionalAttacks(square, -1, -1));
            return moves;
        }

        //Parameters:
        //  - Square where the imaginary rook will be placed
        //Behavior:
        //  - Calculates the set of all the possible squares a rook would control it was
        //    located on the input square
        //Returns:
        //  - Returns the set of moves calculated in its behavior
        private Set<Square> getPossibleRookMoves(Square square){
            Set<Square> moves = new HashSet<>();
            moves.addAll(getDirectionalAttacks(square, 1, 0));
            moves.addAll(getDirectionalAttacks(square, -1, 0));
            moves.addAll(getDirectionalAttacks(square, 0, 1));
            moves.addAll(getDirectionalAttacks(square, 0, -1));

            return moves;
        }

        //Parameters:
        //  - Square where the imaginary queen will be placed
        //Behavior:
        //  - Calculates the set of all the possible squares a queen would control it was
        //    located on the input square
        //Returns:
        //  - Returns the set of moves calculated in its behavior
        private Set<Square> getPossibleQueenMoves(Square square){
            Set<Square> moves = new HashSet<>();
            moves.addAll(getPossibleBishopMoves(square));
            moves.addAll(getPossibleRookMoves(square));
            return moves;
        }

        //Parameters:
        //  - Square where the imaginary king will be placed
        //Behavior:
        //  - Calculates the set of all the possible squares a king would control it was 
        //    located on the input square
        //Returns:
        //  - Returns the set of moves calculated in its behavior
        private Set<Square> getPossibleKingMoves(Square square){
            Set<Square> moves = new HashSet<>();
            for(int fileDirection = -1; fileDirection <= 1; fileDirection++){
                for(int rankDirection = -1; rankDirection <= 1; rankDirection++){
                    if(fileDirection != 0 || rankDirection != 0){
                        moves.add(new Square(square.file + fileDirection, square.rank 
                            + rankDirection));
                    }
                }
            }
            removeOutOfBounds(moves);

            return moves;
        }

        //Parameters:
        //  - Square where the imaginary piece will be placed
        //  - A file direction and a rank direction to create a total of 8 possible 
        //    directions of movement
        //Behavior:
        //  - Calculates the set of all the possible squares a piece would control in 
        //    the direction specified by the inputs
        //Exceptions:
        //  - Will throw an exception if the file direction and rank direction are not between 
        //    -1 and 1 inclusive
        //Returns:
        //  - Returns the set of moves calculated in its behavior
        private Set<Square> getDirectionalAttacks(Square square, int fileDirection,
            int rankDirection){

            if(fileDirection > 1 || fileDirection < -1 || rankDirection > 1
                || rankDirection < -1){

                throw new IllegalArgumentException(
                    "Directions must be between -1 and 1 inclusive.");
            }
            Set<Square> attacks = new HashSet<>();
            int tempFile = square.file + fileDirection;
            int tempRank = square.rank + rankDirection;
            if(tempFile < BOARD_SIZE && tempFile >= 0 && tempRank < BOARD_SIZE && tempRank >= 0){
                attacks.add(new Square(tempFile, tempRank));

                while(tempFile < BOARD_SIZE && tempFile >= 0 && tempRank < BOARD_SIZE 
                    && tempRank >= 0 && board[tempFile][tempRank] == null){

                    tempFile += fileDirection;
                    tempRank += rankDirection;
                    attacks.add(new Square(tempFile, tempRank));
                }
            }
            removeOutOfBounds(attacks);
            
            return attacks;
        }

        //Parameters:
        //  - Takes in a set of squares as possible moves
        //Behavior:
        //  - removes the squares that would land the piece out of bounds
        private void removeOutOfBounds(Set<Square> moves){
            Set<Square> outofBounds = new HashSet<>();
            for(Square move : moves){
                if(move.file >= BOARD_SIZE || move.file < 0 || move.rank >= BOARD_SIZE 
                    || move.rank < 0){

                    outofBounds.add(move);
                }
            }
            for(Square move : outofBounds){
                moves.remove(move);
            }
        }

        //Behavior:
        //  - Converts the current board state into the first section of a string in 
        //    Forsyth-Edwards Notation
        //Returns:
        //  - returns the converted string
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
                            FEN += Chess.TYPE_TO_STRING.get(board[file][rank].pieceType);
                        } else {
                            FEN += Chess.TYPE_TO_STRING.get(board[file][rank].pieceType)
                                .toLowerCase();
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

        //Behavior:
        //  - Converts the current board state into the full Forsyth-Edwards Notation string
        //Returns:
        //  - Returns the converted string
        private String boardToFENFull(){
            String fullFEN = boardToFENShort();
            fullFEN += " " + (whiteTurn ? "w" : "b");
            fullFEN += " ";
            if(whiteCastlingRights[KINGSIDE]){
                fullFEN += "K";
            }
            if(whiteCastlingRights[QUEENSIDE]){
                fullFEN += "Q";
            }
            if(blackCastlingRights[KINGSIDE]){
                fullFEN += "k";
            }
            if(blackCastlingRights[QUEENSIDE]){
                fullFEN += "q";
            }
            fullFEN += " ";
            if(enPassantSquare == null){
                fullFEN += "-";
            } else {
                fullFEN += (char)(enPassantSquare.file + (int)'a');
                fullFEN += BOARD_SIZE - enPassantSquare.rank;
            }

            fullFEN += " " + fiftyMoveRuleCounter + " " + moveNumber;

            return fullFEN;
        }

        //Parameters:
        //  - boolean for if the game is of the Crazyhouse variant
        //Behavior:
        //  - Converts the board into a string that is formatted in a manner 
        //    that is similar to a chess board
        //  - Flips the board based on which player it is so both players will 
        //    play from their point of view
        //  - If the variant is Crazyhouse, captured pieces will be displayed 
        //    on the side and promoted pieces will be tinted a lighter color
        //Returns:
        //  - Returns the converted string
        private String toString(boolean isCrazyhouse){
            String boardString = "";
            if(whiteTurn){
                for(int rank = 0; rank < BOARD_SIZE; rank++){
                    for(int file = 0; file < BOARD_SIZE; file++){
                        boardString += squareToString(isCrazyhouse, file, rank);
                    }
                    boardString += rankAddonsToString(isCrazyhouse, rank);
                }
                boardString += " a  b  c  d  e  f  g  h ";
            }else {
                for(int rank = BOARD_SIZE - 1; rank >= 0; rank--){
                    for(int file = BOARD_SIZE - 1; file >= 0; file--){
                        boardString += squareToString(isCrazyhouse, file, rank);
                    }
                    boardString += rankAddonsToString(isCrazyhouse, rank);
                }
                boardString += " h  g  f  e  d  c  b  a ";
            }

            return boardString;
        }

        //Parameters:
        //  - If the game variant is crazyhouse
        //  - The rank number
        //Return:
        //  - A string that contains the rank number, along with up to 
        //    11 color-coded captured pieces if the vairant is crazyhouse
        private String rankAddonsToString(boolean isCrazyhouse, int rank) {
            String rankString = " " + (BOARD_SIZE - rank) + " ";
            //adds pieces in players inventories
            if(isCrazyhouse){
                if(rank < 4){
                    rankString += "\u001b[38;5;21m";
                    for(int i = rank * 10; i < Math.min(rank * 11 + 11, 
                        blackCapturedPieces.size()); i++){

                        rankString += TYPE_TO_STRING.get(blackCapturedPieces.get(i).pieceType);
                    }
                } else {
                    rankString += "\u001b[38;2;253;182;0m";
                    for(int i = 77 - (rank * 11); i < Math.min(88 - (rank * 11), 
                        whiteCapturedPieces.size()); i++){

                        rankString += TYPE_TO_STRING.get(whiteCapturedPieces.get(i).pieceType);
                    }
                }
                rankString += "\u001b[0m";
            }
            rankString += "\n";
            return rankString;
        }

        //Parameters:
        //  - If the game variant is Crazyhouse
        //  - The file number
        //  - The rank number
        //     - These two numbers together forms the location of a square on the chess board
        //Return:
        //  - A color-coded string representation of the square given in the input
        //  - Promoted pieces will be tinted a lighter color if the variant is Crazyhouse
        private String squareToString(boolean isCrazyhouse, int file, int rank) {
            String background = ((file + rank) % 2) == 0 ? "\u001b[47m" : "\u001b[40m";
            String squareString = background;
            String color = "";
            squareString += " ";
            if(board[file][rank] != null){
                color = board[file][rank].isWhite ? "\u001b[38;2;253;182;0m" : "\u001b[38;5;21m";
                
                //tints promoted pieces in crazyhouse as they turn back into pawns when captured
                if(isCrazyhouse & board[file][rank].isPromoted){
                    color = board[file][rank].isWhite ? "\u001b[38;2;255;195;180m" 
                    : "\u001b[38;5;39m";
                }    
                squareString += color;
                squareString += TYPE_TO_STRING.get(board[file][rank].pieceType);
            } else {
                squareString += " ";
            }
            squareString += " ";
            squareString += "\u001b[0m";
            return squareString;
        }

        //Behavior:
        //  - This method recalculates and updates all the squares each piece attacks
        private void calculateAttacks(){
            //calculating new attacks
            for(Piece piece : whitePieces){
                Set<Square> newControlledSquares;
                if(piece.pieceType == PieceType.PAWN){
                    newControlledSquares = getPossiblePawnCaptures(piece.square, true);
                } else if(piece.pieceType == PieceType.KNIGHT){
                    newControlledSquares = getPossibleKnightMoves(piece.square);
                } else if(piece.pieceType == PieceType.BISHOP){
                    newControlledSquares = getPossibleBishopMoves(piece.square);
                } else if(piece.pieceType == PieceType.ROOK){
                    newControlledSquares = getPossibleRookMoves(piece.square);
                } else if(piece.pieceType == PieceType.QUEEN){
                    newControlledSquares = getPossibleQueenMoves(piece.square);
                } else {
                    newControlledSquares = getPossibleKingMoves(piece.square);
                }
                whiteControlledSquares.replace(piece, newControlledSquares);
            }
            for(Piece piece : blackPieces){
                Set<Square> newControlledSquares;
                if(piece.pieceType == PieceType.PAWN){
                    newControlledSquares = getPossiblePawnCaptures(piece.square, false);
                } else if(piece.pieceType == PieceType.KNIGHT){
                    newControlledSquares = getPossibleKnightMoves(piece.square);
                } else if(piece.pieceType == PieceType.BISHOP){
                    newControlledSquares = getPossibleBishopMoves(piece.square);
                } else if(piece.pieceType == PieceType.ROOK){
                    newControlledSquares = getPossibleRookMoves(piece.square);
                } else if(piece.pieceType == PieceType.QUEEN){
                    newControlledSquares = getPossibleQueenMoves(piece.square);
                } else {
                    newControlledSquares = getPossibleKingMoves(piece.square);
                }
                blackControlledSquares.replace(piece, newControlledSquares);
            }

            playerInCheck = new boolean[]{false, false};

            for(Piece piece : whitePieces){
                for(Square square : whiteControlledSquares.get(piece)){
                    if(square.file == blackKingSquare.file && square.rank == blackKingSquare.rank){
                        playerInCheck[1] = true;
                    }
                }
            }

            for(Piece piece : blackPieces){
                for(Square square : blackControlledSquares.get(piece)){
                    if(square.file == whiteKingSquare.file && square.rank == whiteKingSquare.rank){
                        playerInCheck[0] = true;
                    }
                }
            }
        }

        //Parameters:
        //  - Piece that will be moves
        //  - Destination square of that piece
        //  - Piece to promote to (overloaded method)
        //  - Side to castle to (overloaded method) 
        //Behavior:
        //  - Moves the inputted piece to the inputted square
        //  - Removes captured pieces if they exist
        //      - Adds those captured pieces to players inventory if the variant is Crazyhouse
        //      - Creates a 3x3 explosion at the capture of the variant is Atomic
        //  - Updates the Portable Game Notation of the board
        //  - If the move is a promotion, promote the pawn to inputted promotion piece
        //  - If the move is a castle, move the rook to the proper location as well
        //Returns:
        //  - returns true if the move was a capture, false if otherwise
        private boolean move(Piece piece, Square destination){
            int startFile = piece.square.file;
            int startRank = piece.square.rank;
            int endFile = destination.file;
            int endRank = destination.rank;
            Piece capturedPiece = null;
            //regular capture
            if(board[endFile][endRank] != null && !(startFile == endFile && startRank == endRank)){
                capturedPiece = board[endFile][endRank];
                if(whiteTurn){
                    blackPieces.remove(capturedPiece);
                    blackControlledSquares.remove(capturedPiece);
                    if(capturedPiece.pieceType == PieceType.ROOK){
                        if(queensideCastleRooks[1] != null && queensideCastleRooks[1]
                            .equals(capturedPiece)){

                            queensideCastleRooks[1] = null;
                            blackCastlingRights[QUEENSIDE] = false;
                        }
                        if(kingsideCastleRooks[1] != null && kingsideCastleRooks[1]
                            .equals(capturedPiece)){

                            kingsideCastleRooks[1] = null;
                            blackCastlingRights[KINGSIDE] = false;
                        }
                    }
                    if(capturedPiece.isPromoted){
                        whiteCapturedPieces.add(new Piece(PieceType.PAWN, true, 
                            new Square(-1, -1), false));
                    } else {
                        whiteCapturedPieces.add(new Piece(capturedPiece.pieceType, true, 
                            new Square(-1, -1), false));
                    }
                    whiteCapturedPieces = orderPieces(whiteCapturedPieces);
                } else {
                    whitePieces.remove(capturedPiece);
                    whiteControlledSquares.remove(capturedPiece);
                    if(capturedPiece.pieceType == PieceType.ROOK){
                        if(queensideCastleRooks[0] != null && queensideCastleRooks[0]
                            .equals(capturedPiece)){

                            queensideCastleRooks[0] = null;
                            whiteCastlingRights[QUEENSIDE] = false;
                        }
                        if(kingsideCastleRooks[0] != null && kingsideCastleRooks[0]
                            .equals(capturedPiece)){

                            kingsideCastleRooks[0] = null;
                            whiteCastlingRights[KINGSIDE] = false;
                        }
                    }
                    if(capturedPiece.isPromoted){
                        blackCapturedPieces.add(new Piece(PieceType.PAWN, false, 
                            new Square(-1, -1), false));
                    } else {
                        blackCapturedPieces.add(new Piece(capturedPiece.pieceType, false, 
                            new Square(-1, -1), false));
                    }
                    blackCapturedPieces = orderPieces(blackCapturedPieces);
                }
                fiftyMoveRuleCounter = -1;
                if(piece.pieceType == PieceType.PAWN){
                    portableGameNotation += (char)(startFile + (int)'a');
                }
                portableGameNotation += "x";
            }

            enPassantSquare = null;

            if(piece.pieceType == PieceType.PAWN){
                fiftyMoveRuleCounter = -1;

                //en passantable pawn
                if(Math.abs(endRank - startRank) == 2){
                    enPassantSquare = new Square(endFile,(endRank + startRank) / 2);
                }

                //en passant capture
                if(startFile >= 0 && startRank >= 0 && endFile != startFile 
                    && board[endFile][endRank] == null){

                    capturedPiece = board[endFile][endRank + (whiteTurn ? 1: -1)];
                    if(whiteTurn){
                        blackPieces.remove(capturedPiece);
                        blackControlledSquares.remove(capturedPiece);
                        whiteCapturedPieces.add(new Piece(PieceType.PAWN, true, 
                            new Square(-1, -1), false));
                    } else {
                        whitePieces.remove(capturedPiece);
                        whiteControlledSquares.remove(capturedPiece);
                        blackCapturedPieces.add(new Piece(PieceType.PAWN, false, 
                            new Square(-1, -1), false));
                    }
                    board[endFile][endRank + (whiteTurn ? 1: -1)] = null;
                    fiftyMoveRuleCounter = -1;
                    portableGameNotation += (char)(startFile + (int)'a') + "x";
                }
            }
            if(startFile >= 0 && startRank >= 0){
                board[startFile][startRank] = null;
            }

            board[endFile][endRank] = piece;
            piece.square = new Square(endFile, endRank);
            


            //updating king square
            if(piece.pieceType == PieceType.KING){
                if(whiteTurn){
                    whiteKingSquare = new Square(endFile, endRank);
                } else {
                    blackKingSquare = new Square(endFile, endRank);
                }
            }

            return capturedPiece != null;
        }

        //promotion moves
        private boolean move(Piece piece, Square destination, Piece promotionPiece){
            boolean isCapture = move(piece, destination);
            if((destination.rank == BLACK_FIRST_RANK || destination.rank == WHITE_FIRST_RANK) 
                && piece.pieceType == PieceType.PAWN){

                board[destination.file][destination.rank] = promotionPiece;
                if(whiteTurn){
                    whitePieces.remove(piece);
                    whitePieces.add(promotionPiece);
                    whiteControlledSquares.put(promotionPiece, new HashSet<Square>());
                } else {
                    blackPieces.remove(piece);
                    blackPieces.add(promotionPiece);
                    blackControlledSquares.put(promotionPiece, new HashSet<Square>());
                }
            }
            return isCapture;
        }

        //castling moves
        private boolean move(Piece piece, Square destination, boolean isKingSide){
            boolean isCapture = move(piece, destination);
            move(isKingSide ? kingsideCastleRooks[whiteTurn ? 0 : 1] 
                : queensideCastleRooks[whiteTurn ? 0 : 1], new Square(isKingSide ? 
                Chess.KINGSIDE_CASTLE_FILE_ROOK : Chess.QUEENSIDE_CASTLE_FILE_ROOK, 
                piece.square.rank));

            return isCapture;
        }

        //Parameters:
        //  - A set of pieces to be ordered
        //Behavior:
        //  - Orders the captured piece lists for crazyhouse in the order P,N,B,R,Q
        private List<Piece> orderPieces(List<Piece> pieceList){
            List<Piece> orderedPieceList = new ArrayList<>();
            //pawns
            for(int i = 0; i < pieceList.size(); i++){
                if(pieceList.get(i).pieceType == PieceType.PAWN){
                    //System.out.println("found pawn at " + i);
                    orderedPieceList.add(pieceList.get(i));
                }
            }
            //knights
            for(int i = 0; i < pieceList.size(); i++){
                if(pieceList.get(i).pieceType == PieceType.KNIGHT){
                    orderedPieceList.add(pieceList.get(i));
                }
            }
            //bishops
            for(int i = 0; i < pieceList.size(); i++){
                if(pieceList.get(i).pieceType == PieceType.BISHOP){
                    //System.out.println("found bishop at " + i);
                    orderedPieceList.add(pieceList.get(i));
                }
            }
            //rooks
            for(int i = 0; i < pieceList.size(); i++){
                if(pieceList.get(i).pieceType == PieceType.ROOK){
                    orderedPieceList.add(pieceList.get(i));
                }
            }
            //queens
            for(int i = 0; i < pieceList.size(); i++){
                if(pieceList.get(i).pieceType == PieceType.QUEEN){
                    orderedPieceList.add(pieceList.get(i));
                }
            }
            return orderedPieceList;
        }

        //Parameters:
        //  - Square that the capture occured on
        //Behavior:
        //  - Creates a 3x3 explosion around a square and removes all pieces (not pawns)
        //    in its radius
        //Returns:
        //  - 0 if both kings are alive
        //  - 1 if the white king blew up
        //  - 2 if the black king blew up
        //  - 3 if both kings blew up
        private int atomicCaptureExplosion(Square capture){
            Piece piece;
            Piece capturer = board[capture.file][capture.rank];
            int kingExploded = 0; 

            Set<Square> explosion = getPossibleKingMoves(capture);
            for(Square square : explosion){
                piece = board[square.file][square.rank];
                if(piece != null && piece.pieceType != PieceType.PAWN){
                    if(piece.isWhite){
                        if(piece.pieceType == PieceType.KING){
                            kingExploded += 1;
                        }
                        whitePieces.remove(piece);
                        whiteControlledSquares.remove(piece);
                    } else {
                        blackPieces.remove(piece);
                        blackControlledSquares.remove(piece);
                        if(piece.pieceType == PieceType.KING){
                            kingExploded += 2;
                        }
                    }
                    board[square.file][square.rank] = null;
                }
            }
            if(capturer.isWhite){
                whitePieces.remove(capturer);
                whiteControlledSquares.remove(capturer);

                if(capturer.pieceType == PieceType.KING){
                    kingExploded += 1;
                }
            } else {
                blackPieces.remove(capturer);
                blackControlledSquares.remove(capturer);

                if(capturer.pieceType == PieceType.KING){
                    kingExploded += 2;
                }
            }
            board[capture.file][capture.rank] = null;

            return kingExploded;
        }
    }

    //This helper class is used to hold all the data about a specific piece in one place
    private class Piece {
        private Square square;
        private boolean isPromoted;
        private final PieceType pieceType;
        private final boolean isWhite;

        //Parameters:
        // - Piece type
        // - Piece color
        // - Piece location
        //Behavior:
        //  - Creates new piece object based on the parameters
        private Piece(PieceType type, boolean isWhite, Square square, boolean isPromoted){
            this.pieceType = type;
            this.isWhite = isWhite;
            this.square = square;
            this.isPromoted = isPromoted;
        }
    }
    //This class is used to hold the location information on the chess board in one place
    private class Square {
        private final int file, rank;

        //Parameters
        //  - An integer for the file
        //  - An integer for the rank
        //Behavior:
        //  - Creates a square object that groups the parameters together
        private Square(int file, int rank){
            this.file = file;
            this.rank = rank;
        }

        //Behavior:
        //  - Converts the square into <file><rank> format (ex. e4)
        //Returns:
        //  - Returns the converted string
        public String toString(){
            return (char)(file + (int)'a') + "" + (8 - rank);
        }
    }
}
