import java.security.*;
import java.util.*;
import java.util.regex.*;



//TODO:
//remove moving pieces pinned to king from legalMoves -- overhaul legal move check?
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
        put(Piece.PieceType.PAWN, "");
        put(Piece.PieceType.KNIGHT, "N");
        put(Piece.PieceType.BISHOP, "B");
        put(Piece.PieceType.ROOK, "R");
        put(Piece.PieceType.QUEEN, "Q");
        put(Piece.PieceType.KING, "K");
    }};



    public Chess(){
        //board setup
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
        String move = input.next();

        if(move.equals("print")){
            System.out.println(toString());
            return;
        }
        
        Pattern coordinate = Pattern.compile("[a-h][1-8][a-h][1-8]", Pattern.CASE_INSENSITIVE);
        Pattern algebraic = Pattern.compile("[NBRQK]?[a-h|1-8]?x?[a-h][1-8][+|$|#]?", Pattern.CASE_INSENSITIVE);
        
        boolean legalMove = false;
        int startFile = (int)move.charAt(0) - (int)'a';
        int startRank = board.BOARD_SIZE - Character.getNumericValue(move.charAt(1));
        int endFile = (int)move.charAt(2) - (int)'a';
        int endRank = board.BOARD_SIZE - Character.getNumericValue(move.charAt(3));

        if(board.board[startFile][startRank] == null){
            throw new IllegalArgumentException("Piece does not exist.");
        }

        if(board.board[startFile][startRank].isWhite != board.whiteTurn){
            throw new IllegalArgumentException("That is not your piece.");
        }

        Piece piece = board.board[startFile][startRank];
        for(Square legal : getLegalMoves().get(piece)){
            if(legal.file == endFile && legal.rank == endRank){
                legalMove = true;
            }
        }

        if(!legalMove){
            throw new IllegalArgumentException("Piece cannot move there.");
        }

        if(board.whiteTurn){
            board.pgn += board.moveNumber + ".";
        }
        board.pgn += TYPE_TO_STRING.get(piece.pieceType);

        board.move(piece, new Square(endFile, endRank));
        
        board.pgn += move.substring(2);

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
        for(Piece q : enemyMoves.keySet()){
            availableMoves += enemyMoves.get(q).size();
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
        if(board.whiteTurn){
            for(Piece piece : board.whitePieces){
                legalMoves.put(piece, new HashSet<Square>());
                for(Square move : board.whiteControlledSquares.get(piece)){
                    legalMoves.get(piece).add(move);
                    if(board.board[move.file][move.rank] != null && board.board[move.file][move.rank].isWhite){
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
            for(Piece piece : board.whitePieces){
                Set<Square> illegalMoves = new HashSet<>();
                for(Square square : legalMoves.get(piece)){
                    Board testBoard = new Board(board.boardToFENFull());
                    testBoard.move(testBoard.board[piece.square.file][piece.square.rank], square);
                    if(testBoard.playerInCheck[0]){
                        illegalMoves.add(square);
                    }
                }
                for(Square square : illegalMoves){
                    legalMoves.get(piece).remove(square);
                }
            }
            
        } else {
            for(Piece piece : board.blackPieces){
                legalMoves.put(piece, new HashSet<Square>());
                for(Square move : board.blackControlledSquares.get(piece)){
                    legalMoves.get(piece).add(move);
                    if(board.board[move.file][move.rank] != null && !board.board[move.file][move.rank].isWhite){
                        legalMoves.get(piece).remove(move);
                    }
                }
                if(piece.pieceType == Piece.PieceType.KING){
                    for(Square move : board.blackControlledSquares.get(piece)){
                        for(Piece enemyPiece : board.whiteControlledSquares.keySet()){
                            for(Square square : board.whiteControlledSquares.get(enemyPiece)){
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
                        if(board.board[move.file][move.rank] == null){
                            emptyCaptures.add(move);
                        }

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
            for(Piece piece : board.blackPieces){
                Set<Square> illegalMoves = new HashSet<>();
                for(Square square : legalMoves.get(piece)){
                    Board testBoard = new Board(board.boardToFENFull());
                    testBoard.move(testBoard.board[piece.square.file][piece.square.rank], square);
                    if(testBoard.playerInCheck[1]){
                        illegalMoves.add(square);
                    }
                }
                for(Square square : illegalMoves){
                    legalMoves.get(piece).remove(square);
                }
            }
        }
        return legalMoves;
    }
}
