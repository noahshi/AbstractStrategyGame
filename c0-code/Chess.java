import java.security.*;
import java.util.*;
import java.util.regex.*;



//TODO:
//fix promotion attack check
//King cant move
//file move specification isnt working - rank specification is working though
//add castling
//add resigning etc.
//add variants

public class Chess implements AbstractStrategyGame{
    
    private final Board board;

    private Map<String, Integer> threeFoldCheck = new HashMap<>();

    private String startingFEN;
    
    private int winner;

    private static final String DEFAULT_BOARD_SETUP = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public static final HashMap<Piece.PieceType, String> TYPE_TO_STRING = new HashMap<>(){{
        put(Piece.PieceType.PAWN, "P");
        put(Piece.PieceType.KNIGHT, "N");
        put(Piece.PieceType.BISHOP, "B");
        put(Piece.PieceType.ROOK, "R");
        put(Piece.PieceType.QUEEN, "Q");
        put(Piece.PieceType.KING, "K");
    }};



    public Chess(){
        //board setup
        //String testFEN = "rnbqk1nr/pppp1ppp/8/4p3/1b1P4/8/PPPBPPPP/RN1QKBNR w KQkq - 2 3";
        board = new Board(DEFAULT_BOARD_SETUP);
        startingFEN = DEFAULT_BOARD_SETUP;

        

        winner = -1;
        threeFoldCheck.put(DEFAULT_BOARD_SETUP, 1);

    }


    //returns game instructions -- UNFINISHED --
    public String instructions(){
        return "White moves first. Type in your move in the format <starting square><ending square> ie. e2e4.\n" +
               "- are empty squares. Pieces are labeled with their respective names in algebraic notation.\n" +
               "more rules dsfsefjsodiflkenlsf";
    }

    public String toString(){
        return board.toString();
    }

    public boolean isGameOver(){
        return winner != -1;
    }

    public int getWinner(){
        return winner;
    }

    public int getNextPlayer(){
        return board.whiteTurn ? 1 : 2;
    }

    public void makeMove(Scanner input){
        String move = input.nextLine();
        
        Pattern coordinate = Pattern.compile("^[a-h][1-8][a-h][1-8](=[NBRQ])?$", Pattern.CASE_INSENSITIVE);
        Pattern algebraic = Pattern.compile("^([NBRQK])([a-h|1-8])?x?([a-h])([1-8])[+$#]?|([a-h]x)?([a-h])([1-8])(=[NBRQ]| ?e\\.p\\.)?[+$#]?|O-O(-O)?[+$#]?$");

        Matcher cdnMatcher = coordinate.matcher(move);
        Matcher algMatcher = algebraic.matcher(move);
        
        boolean legalMove = false;

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
        //typing symbols used in chess notation such as x + $ # etc. is allowed for extra 
        //leniency but they will be disregarded in cases where they should not be used.
        } else if (algMatcher.matches()){
            boolean doesMoveExist = false;
            System.out.println(board.boardToFENShort());
            //piece movement
            if(algMatcher.group(1) != null){

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
                                
                                if(startFile != -1){
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

                if(algMatcher.group(5) != null){
                    startFile = (int)algMatcher.group(5).charAt(0) - (int)'a';
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
                if(!doesMoveExist){ //need to throw exception before promotion check or startrank will be null
                    throw new IllegalArgumentException("Move does not exist.");
                }
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
        if(piece.pieceType != Piece.PieceType.PAWN){
            board.pgn += TYPE_TO_STRING.get(piece.pieceType);
        }
        
        board.pgn += move.substring(2);

        if(promotionPiece != null){
            board.move(piece, new Square(endFile, endRank), promotionPiece);
            board.pgn += "=" + TYPE_TO_STRING.get(promotionPiece);
        } else {
            board.move(piece, new Square(endFile, endRank));
        }

        board.calculateAttacks();

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
                winner = board.whiteTurn ? 2 : 1; //whiteTurn boolean was inverted above so this ternary operator must be flipped
                board.pgn += "#";
            } else {
                winner = 0;
                board.pgn += "$";
            }
        }
        //checking for end scenarios above

        if(board.playerInCheck[board.whiteTurn ? 1 : 0] && winner == -1){
            board.pgn += "+";
        }
        board.pgn += " ";

        // debug statement for checking if en passant is working
        //if(enPassantSquare != null)
        //    System.out.println(enPassantSquare[0] + " " + enPassantSquare[1]);
    }

    public String getMoves(){
        return startingFEN += "\n" + board.pgn;
    }

    


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
}
