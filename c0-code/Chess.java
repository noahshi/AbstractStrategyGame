import java.security.InvalidParameterException;
import java.util.*;
import java.util.regex.Pattern;



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
    private Map<Piece, Set<Integer[]>> whiteControlledSquares = new HashMap<>();
    private Map<Piece, Set<Integer[]>> blackControlledSquares = new HashMap<>();
    
    private boolean[] whiteCastlingRights = new boolean[2];
    private boolean[] blackCastlingRights = new boolean[2];

    private boolean[] playerInCheck = new boolean[]{false, false};
    private List<Piece> blackCheckingPieces = new ArrayList<>();
    private List<Piece> whiteCheckingPieces = new ArrayList<>();
    private Map<String, Integer> threeFoldCheck = new HashMap<>();
    
    private boolean whiteTurn;
    private int[] enPassantSquare;
    private int fiftyMoveRuleCounter;
    private int moveNumber;

    private String startingFEN;
    private String pgn;

    private Integer[] whiteKingSquare;
    private Integer[] blackKingSquare;
    
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

        pgn = "";
        startingFEN = fen;

        winner = -1;

        String[] temp = fen.split(" ");
        String[] ranks = temp[0].split("/");
        for(int rank = 0; rank < ranks.length; rank++){
            for(int file = 0; file < ranks[rank].length(); file++){
                //when switch statements are not allowed
                char currentChar = ranks[rank].charAt(file);
                if(currentChar == 'p'){
                    board[file][rank] = new Piece(Piece.PieceType.PAWN, false, file , rank);
                    blackControlledSquares.put(board[file][rank], getPossiblePawnCaptures(file, rank, false));

                } else if(currentChar == 'n'){
                    board[file][rank] = new Piece(Piece.PieceType.KNIGHT, false, file , rank);
                    blackControlledSquares.put(board[file][rank], getPossibleKnightMoves(file, rank));

                } else if(currentChar == 'b'){
                    board[file][rank] = new Piece(Piece.PieceType.BISHOP, false, file , rank);
                    blackControlledSquares.put(board[file][rank], getPossibleBishopMoves(file, rank));

                } else if(currentChar == 'r'){
                    board[file][rank] = new Piece(Piece.PieceType.ROOK, false, file , rank);
                    blackControlledSquares.put(board[file][rank], getPossibleRookMoves(file, rank));

                } else if(currentChar == 'q'){
                    board[file][rank] = new Piece(Piece.PieceType.QUEEN, false, file , rank);
                    blackControlledSquares.put(board[file][rank], getPossibleQueenMoves(file, rank));

                } else if(currentChar == 'k'){
                    board[file][rank] = new Piece(Piece.PieceType.KING, false, file , rank);
                    blackControlledSquares.put(board[file][rank], getPossibleKingMoves(file, rank));
                    blackKingSquare = new Integer[]{file, rank};

                } else if(currentChar == 'P'){
                    board[file][rank] = new Piece(Piece.PieceType.PAWN, true, file , rank);
                    whiteControlledSquares.put(board[file][rank], getPossiblePawnCaptures(file, rank, true));

                } else if(currentChar == 'N'){
                    board[file][rank] = new Piece(Piece.PieceType.KNIGHT, true, file , rank);
                    whiteControlledSquares.put(board[file][rank], getPossibleKnightMoves(file, rank));

                } else if(currentChar == 'B'){
                    board[file][rank] = new Piece(Piece.PieceType.BISHOP, true, file , rank);
                    whiteControlledSquares.put(board[file][rank], getPossibleBishopMoves(file, rank));

                } else if(currentChar == 'R'){
                    board[file][rank] = new Piece(Piece.PieceType.ROOK, true, file , rank);
                    whiteControlledSquares.put(board[file][rank], getPossibleRookMoves(file, rank));

                } else if(currentChar == 'Q'){
                    board[file][rank] = new Piece(Piece.PieceType.QUEEN, true, file , rank);
                    whiteControlledSquares.put(board[file][rank], getPossibleQueenMoves(file, rank));

                } else if(currentChar == 'K'){
                    board[file][rank] = new Piece(Piece.PieceType.KING, true, file , rank);
                    whiteControlledSquares.put(board[file][rank], getPossibleKingMoves(file, rank));
                    whiteKingSquare = new Integer[]{file, rank};

                } else {
                    file += Character.getNumericValue(currentChar) - 1;
                }

                if(board[file][rank] != null){
                    if(board[file][rank].isWhite()){
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
            enPassantSquare = new int[]{(int)temp[3].charAt(0) - (int)'a', BOARD_SIZE - Character.getNumericValue(temp[3].charAt(1))};
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
                        color = board[file][rank].isWhite() ? "\u001b[38;2;253;182;0m" : "\u001b[38;5;21m";
                        boardString += color;
                        if(board[file][rank].getPieceType() == Piece.PieceType.PAWN){
                            boardString += " P ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.KNIGHT){
                            boardString += " N ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.BISHOP){
                            boardString += " B ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.ROOK){
                            boardString += " R ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.QUEEN){
                            boardString += " Q ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.KING){
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
                        color = board[file][rank].isWhite() ? "\u001b[38;2;253;182;0m" : "\u001b[38;5;21m";
                        boardString += color;
                        if(board[file][rank].getPieceType() == Piece.PieceType.PAWN){
                            boardString += " P ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.KNIGHT){
                            boardString += " N ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.BISHOP){
                            boardString += " B ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.ROOK){
                            boardString += " R ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.QUEEN){
                            boardString += " Q ";
                        } else if(board[file][rank].getPieceType() == Piece.PieceType.KING){
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

        if(board[startFile][startRank].isWhite() != whiteTurn){
            throw new IllegalArgumentException("That is not your piece.");
        }

        Piece piece = board[startFile][startRank];
        for(Integer[] legal : getLegalMoves().get(piece)){
            if(legal[0] == endFile && legal[1] == endRank){
                legalMove = true;
            }
        }

        if(!legalMove){
            throw new IllegalArgumentException("Piece cannot move there.");
        }

        if(whiteTurn){
            pgn += moveNumber + ".";
        }
        pgn += TYPE_TO_STRING.get(piece.getPieceType());

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

        if(piece.getPieceType() == Piece.PieceType.PAWN){
            fiftyMoveRuleCounter = -1;

            //en passantable pawn
            if(Math.abs(endRank - startRank) == 2){
                enPassantSquare = new int[]{endFile,(endRank + startRank) / 2};
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
        piece.setLocation(endFile, endRank);
        board[startFile][startRank] = null;

        //updating king square
        if(piece.getPieceType() == Piece.PieceType.KING){
            if(whiteTurn){
                whiteKingSquare = new Integer[]{endFile, endRank};
            } else {
                blackKingSquare = new Integer[]{endFile, endRank};
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
        Map<Piece, Set<Integer[]>> enemyMoves = getLegalMoves();
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
                    if(board[file][rank].isWhite()){
                        FEN += TYPE_TO_STRING.get(board[file][rank].getPieceType());
                    } else {
                        FEN += TYPE_TO_STRING.get(board[file][rank].getPieceType()).toLowerCase();
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
            fullFEN += (char)(enPassantSquare[0] + (int)'a') + (BOARD_SIZE - enPassantSquare[1]);
        }

        fullFEN += " " + fiftyMoveRuleCounter + " " + moveNumber;

        return fullFEN;
    }

    private Set<Integer[]> getPossiblePawnMoves(int file, int rank, boolean isWhite){
        Set<Integer[]> moves = new HashSet<>();
        int direction = isWhite ? -1 : 1;
        //one square forward
        if(board[file][rank + direction] == null){
            moves.add(new Integer[]{file, rank + direction});
        }
        //two squares forward
        if(rank == (isWhite ? 6 : 1)){
            if(board[file][rank + direction * 2] == null){
                moves.add(new Integer[]{file, rank + direction * 2});
            }
        }

        return moves;
    }

    private Set<Integer[]> getPossiblePawnCaptures(int file, int rank, boolean isWhite){
        Set<Integer[]> moves = new HashSet<>();
        int direction = isWhite ? -1 : 1;
        if(file > 0){
            moves.add(new Integer[]{file - 1, rank + direction});
        }
        if(file < 7){
            moves.add(new Integer[]{file + 1, rank + direction});
        }

        return moves;
    }

    private Set<Integer[]> getPossibleKnightMoves(int file, int rank){
        Set<Integer[]> moves = new HashSet<>();
        moves.add(new Integer[]{file - 2, rank + 1});
        moves.add(new Integer[]{file - 1, rank + 2});
        moves.add(new Integer[]{file + 1, rank + 2});
        moves.add(new Integer[]{file + 2, rank + 1});
        moves.add(new Integer[]{file + 2, rank - 1});
        moves.add(new Integer[]{file + 1, rank - 2});
        moves.add(new Integer[]{file - 1, rank - 2});
        moves.add(new Integer[]{file - 2, rank - 1});

        //removes out of bounds moves
        Set<Integer[]> outofBounds = new HashSet<>();
        for(Integer[] move : moves){
            if(move[0] >= BOARD_SIZE || move[0] < 0 || move[1] >= BOARD_SIZE || move[1] < 0){
                outofBounds.add(move);
            }
        }
        for(Integer[] move : outofBounds){
            moves.remove(move);
        }

        return moves;
    }

    private Set<Integer[]> getPossibleBishopMoves(int file, int rank){
        Set<Integer[]> moves = new HashSet<>();
        moves.addAll(getDirectionalAttacks(file, rank, 1, 1));
        moves.addAll(getDirectionalAttacks(file, rank, -1, 1));
        moves.addAll(getDirectionalAttacks(file, rank, 1, -1));
        moves.addAll(getDirectionalAttacks(file, rank, -1, -1));
        return moves;
    }

    private Set<Integer[]> getPossibleRookMoves(int file, int rank){
        Set<Integer[]> moves = new HashSet<>();
        moves.addAll(getDirectionalAttacks(file, rank, 1, 0));
        moves.addAll(getDirectionalAttacks(file, rank, -1, 0));
        moves.addAll(getDirectionalAttacks(file, rank, 0, 1));
        moves.addAll(getDirectionalAttacks(file, rank, 0, -1));

        return moves;
    }

    private Set<Integer[]> getPossibleQueenMoves(int file, int rank){
        Set<Integer[]> moves = new HashSet<>();
        moves.addAll(getPossibleBishopMoves(file, rank));
        moves.addAll(getPossibleRookMoves(file, rank));
        return moves;
    }

    private Set<Integer[]> getPossibleKingMoves(int file, int rank){
        Set<Integer[]> moves = new HashSet<>();
        for(int fileDirection = -1; fileDirection <= 1; fileDirection++){
            for(int rankDirection = -1; rankDirection <= 1; rankDirection++){
                if( fileDirection != 0 && rankDirection != 0){
                    moves.add(new Integer[]{file + fileDirection, rank + rankDirection});
                }
            }
        }

        Set<Integer[]> outofBounds = new HashSet<>();
        for(Integer[] move : moves){
            if(move[0] >= BOARD_SIZE || move[0] < 0 || move[1] >= BOARD_SIZE || move[1] < 0){
                outofBounds.add(move);
            }
        }
        for(Integer[] move : outofBounds){
            moves.remove(move);
        }

        return moves;
    }

    private Set<Integer[]> getDirectionalAttacks(int file, int rank, int fileDirection, int rankDirection){
        if(fileDirection > 1 || fileDirection < -1 || rankDirection > 1 || rankDirection < -1){
            throw new InvalidParameterException("Directions must be between -1 and 1 inclusive.");
        }
        Set<Integer[]> attacks = new HashSet<>();
        int tempFile = file + fileDirection;
        int tempRank = rank + rankDirection;
        if(tempFile < BOARD_SIZE && tempFile >= 0 && tempRank < BOARD_SIZE && tempRank >= 0){
            attacks.add(new Integer[]{tempFile, tempRank});
            while(tempFile < BOARD_SIZE - 1 && tempFile >= 0 && tempRank < BOARD_SIZE - 1 && tempRank >= 0 && board[tempFile][tempRank] == null){
                tempFile += fileDirection;
                tempRank += rankDirection;
                attacks.add(new Integer[]{tempFile, tempRank});
            }
        }
        return attacks;
    }

    private Map<Piece, Set<Integer[]>> getLegalMoves(){
        Map<Piece, Set<Integer[]>> legalMoves = new HashMap<>();
        if(whiteTurn){
            for(Piece piece : whitePieces){
                legalMoves.put(piece, new HashSet<Integer[]>());
                for(Integer[] move : whiteControlledSquares.get(piece)){
                    legalMoves.get(piece).add(move);
                    if(board[move[0]][move[1]] != null && board[move[0]][move[1]].isWhite()){
                        legalMoves.get(piece).remove(move);
                    }
                }
                if(piece.getPieceType() == Piece.PieceType.KING){
                    for(Integer[] move : whiteControlledSquares.get(piece)){
                        for(Piece enemyPiece : blackControlledSquares.keySet()){
                            for(Integer[] square : blackControlledSquares.get(enemyPiece)){
                                if(square[0] == move[0] && square[1] == move[1]){
                                    legalMoves.get(piece).remove(move);
                                }
                            }
                        }
                    }
                }
                if(piece.getPieceType() == Piece.PieceType.PAWN){
                    //removing empty pawn captures from legal move list
                    Set<Integer[]> emptyCaptures = new HashSet<>();
                    for(Integer[] move : legalMoves.get(piece)){
                        if(board[move[0]][move[1]] == null){
                            emptyCaptures.add(move);
                        }
                        
                        //re-adding en passant
                        if(enPassantSquare != null && enPassantSquare[0] == move[0] && enPassantSquare[1] == move[1]){
                            emptyCaptures.remove(move);
                        }
                    }
                    

                    for(Integer[] move : emptyCaptures){
                        legalMoves.get(piece).remove(move);
                    }

                    for(Integer[] move : getPossiblePawnMoves(piece.getFile(), piece.getRank(), piece.isWhite())){
                        legalMoves.get(piece).add(move);
                    }
                }
            }
            
            //modifying legal move list if white is in check
            if(blackCheckingPieces.size() == 1){
                HashSet<Integer[]> prioritySquares = new HashSet<>();
                Integer[] checkOrigin = new Integer[]{blackCheckingPieces.get(0).getFile(), blackCheckingPieces.get(0).getRank()};
                prioritySquares.add(checkOrigin);
                if(blackCheckingPieces.get(0).getPieceType() != Piece.PieceType.KNIGHT){
                    int fileDifference = checkOrigin[0] - whiteKingSquare[0];
                    int rankDifference = checkOrigin[1] - whiteKingSquare[1];

                    if(fileDifference == 0){
                        if(rankDifference > 0){
                            for(int i = checkOrigin[1] - 1; i > whiteKingSquare[1]; i--){
                                prioritySquares.add(new Integer[]{checkOrigin[0], i});
                            }
                        } else {
                            for(int i = checkOrigin[1] + 1; i < whiteKingSquare[1]; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0], i});
                            }
                        }
                    } else if(rankDifference == 0){
                        if(fileDifference > 0){
                            for(int i = checkOrigin[0] - 1; i > whiteKingSquare[0]; i--){
                                prioritySquares.add(new Integer[]{i, checkOrigin[1]});
                            }
                        } else {
                            for(int i = checkOrigin[0] + 1; i < whiteKingSquare[0]; i++){
                                prioritySquares.add(new Integer[]{i, checkOrigin[1]});
                            }
                        }
                    } else if(fileDifference > 0){
                        if(rankDifference > 0){
                            for(int i = 1; i > fileDifference; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0] - i, checkOrigin[1] - i});
                            }
                        } else {
                            for(int i = 1; i > fileDifference; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0] - i, checkOrigin[1] + i});
                            }
                        }
                    } else if(fileDifference < 0){
                        if(rankDifference > 0){
                            for(int i = 1; i > -fileDifference; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0] + i, checkOrigin[1] - i});
                            }
                        } else {
                            for(int i = 1; i > -fileDifference; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0] + i, checkOrigin[1] + i});
                            }
                        }
                    }
                }
                Map<Piece, Set<Integer[]>> actuallyLegalMoves = new HashMap<>();

                for(Piece piece : whitePieces){
                    actuallyLegalMoves.put(piece, new HashSet<Integer[]>());
                    if(piece.getPieceType() != Piece.PieceType.KING){
                        for(Integer[] move : legalMoves.get(piece)){
                            for(Integer[] square : prioritySquares){
                                if(move[0] == square[0] && move[1] == square[1]){
                                    actuallyLegalMoves.get(piece).add(move);
                                }
                            }
                        }
                    } else {
                        for(Integer[] move : legalMoves.get(piece)){
                            actuallyLegalMoves.get(piece).add(move);
                        }
                    }
                }
                legalMoves = actuallyLegalMoves;
            }

            //modifying legal move list if white is in double check
            if(blackCheckingPieces.size() > 1){
                Map<Piece, Set<Integer[]>> actuallyLegalMoves = new HashMap<>();
                actuallyLegalMoves.put(board[whiteKingSquare[0]][whiteKingSquare[1]], new HashSet<Integer[]>());
                for(Integer[] move : legalMoves.get(board[whiteKingSquare[0]][whiteKingSquare[1]])){
                    actuallyLegalMoves.get(board[whiteKingSquare[0]][whiteKingSquare[1]]).add(move);
                }
                legalMoves = actuallyLegalMoves;
            }
        } else {
            for(Piece piece : blackPieces){
                legalMoves.put(piece, new HashSet<Integer[]>());
                for(Integer[] move : blackControlledSquares.get(piece)){
                    legalMoves.get(piece).add(move);
                    if(board[move[0]][move[1]] != null && !board[move[0]][move[1]].isWhite()){
                        legalMoves.get(piece).remove(move);
                    }
                }
                if(piece.getPieceType() == Piece.PieceType.KING){
                    for(Integer[] move : blackControlledSquares.get(piece)){
                        for(Piece enemyPiece : whiteControlledSquares.keySet()){
                            for(Integer[] square : whiteControlledSquares.get(enemyPiece)){
                                if(square[0] == move[0] && square[1] == move[1]){
                                    legalMoves.get(piece).remove(move);
                                }
                            }
                        }
                    }
                }
                if(piece.getPieceType() == Piece.PieceType.PAWN){
                    Set<Integer[]> emptyCaptures = new HashSet<>();
                    for(Integer[] move : legalMoves.get(piece)){
                        if(board[move[0]][move[1]] == null){
                            emptyCaptures.add(move);
                        }

                        if(enPassantSquare != null && enPassantSquare[0] == move[0] && enPassantSquare[1] == move[1]){
                            emptyCaptures.remove(move);
                        }
                    }
                    
                    for(Integer[] move : emptyCaptures){
                        legalMoves.get(piece).remove(move);
                    }
                    
                    for(Integer[] move : getPossiblePawnMoves(piece.getFile(), piece.getRank(), piece.isWhite())){
                        legalMoves.get(piece).add(move);
                    }
                }
            }

            //modifying legal move list if black is in check
            if(whiteCheckingPieces.size() == 1){
                HashSet<Integer[]> prioritySquares = new HashSet<>();
                Integer[] checkOrigin = new Integer[]{whiteCheckingPieces.get(0).getFile(), whiteCheckingPieces.get(0).getRank()};
                prioritySquares.add(checkOrigin);
                if(whiteCheckingPieces.get(0).getPieceType() != Piece.PieceType.KNIGHT){
                    int fileDifference = checkOrigin[0] - blackKingSquare[0];
                    int rankDifference = checkOrigin[1] - blackKingSquare[1];

                    if(fileDifference == 0){
                        if(rankDifference > 0){
                            for(int i = checkOrigin[1] - 1; i > blackKingSquare[1]; i--){
                                prioritySquares.add(new Integer[]{checkOrigin[0], i});
                            }
                        } else {
                            for(int i = checkOrigin[1] + 1; i < blackKingSquare[1]; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0], i});
                            }
                        }
                    } else if(rankDifference == 0){
                        if(fileDifference > 0){
                            for(int i = checkOrigin[0] - 1; i > blackKingSquare[0]; i--){
                                prioritySquares.add(new Integer[]{i, checkOrigin[1]});
                            }
                        } else {
                            for(int i = checkOrigin[0] + 1; i < blackKingSquare[0]; i++){
                                prioritySquares.add(new Integer[]{i, checkOrigin[1]});
                            }
                        }
                    } else if(fileDifference > 0){
                        if(rankDifference > 0){
                            for(int i = 1; i > fileDifference; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0] - i, checkOrigin[1] - i});
                            }
                        } else {
                            for(int i = 1; i > fileDifference; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0] - i, checkOrigin[1] + i});
                            }
                        }
                    } else if(fileDifference < 0){
                        if(rankDifference > 0){
                            for(int i = 1; i > -fileDifference; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0] + i, checkOrigin[1] - i});
                            }
                        } else {
                            for(int i = 1; i > -fileDifference; i++){
                                prioritySquares.add(new Integer[]{checkOrigin[0] + i, checkOrigin[1] + i});
                            }
                        }
                    }
                }
                Map<Piece, Set<Integer[]>> actuallyLegalMoves = new HashMap<>();

                for(Piece piece : blackPieces){
                    actuallyLegalMoves.put(piece, new HashSet<Integer[]>());
                    if(piece.getPieceType() != Piece.PieceType.KING){
                        for(Integer[] move : legalMoves.get(piece)){
                            for(Integer[] square : prioritySquares){
                                if(move[0] == square[0] && move[1] == square[1]){
                                    actuallyLegalMoves.get(piece).add(move);
                                }
                            }
                        }
                    } else {
                        for(Integer[] move : legalMoves.get(piece)){
                            actuallyLegalMoves.get(piece).add(move);
                        }
                    }
                }
                legalMoves = actuallyLegalMoves;
            }

            //modifying legal move list if black is in double check
            if(whiteCheckingPieces.size() > 1){
                Map<Piece, Set<Integer[]>> actuallyLegalMoves = new HashMap<>();
                actuallyLegalMoves.put(board[blackKingSquare[0]][blackKingSquare[1]], new HashSet<Integer[]>());
                for(Integer[] move : legalMoves.get(board[blackKingSquare[0]][blackKingSquare[1]])){
                    actuallyLegalMoves.get(board[blackKingSquare[0]][blackKingSquare[1]]).add(move);
                }
                legalMoves = actuallyLegalMoves;
            }
        }
        return legalMoves;
    }

    private void calculateAttacks(){
        for(Piece piece : whitePieces){
            Set<Integer[]> newControlledSquares;
            if(piece.getPieceType() == Piece.PieceType.PAWN){
                newControlledSquares = getPossiblePawnCaptures(piece.getFile(), piece.getRank(), true);
            } else if(piece.getPieceType() == Piece.PieceType.KNIGHT){
                newControlledSquares = getPossibleKnightMoves(piece.getFile(), piece.getRank());
            } else if(piece.getPieceType() == Piece.PieceType.BISHOP){
                newControlledSquares = getPossibleBishopMoves(piece.getFile(), piece.getRank());
            } else if(piece.getPieceType() == Piece.PieceType.ROOK){
                newControlledSquares = getPossibleRookMoves(piece.getFile(), piece.getRank());
            } else if(piece.getPieceType() == Piece.PieceType.QUEEN){
                newControlledSquares = getPossibleQueenMoves(piece.getFile(), piece.getRank());
            } else {
                newControlledSquares = getPossibleKingMoves(piece.getFile(), piece.getRank());
            }
            whiteControlledSquares.replace(piece, newControlledSquares);
        }
        for(Piece piece : blackPieces){
            Set<Integer[]> newControlledSquares;
            if(piece.getPieceType() == Piece.PieceType.PAWN){
                newControlledSquares = getPossiblePawnCaptures(piece.getFile(), piece.getRank(), false);
            } else if(piece.getPieceType() == Piece.PieceType.KNIGHT){
                newControlledSquares = getPossibleKnightMoves(piece.getFile(), piece.getRank());
            } else if(piece.getPieceType() == Piece.PieceType.BISHOP){
                newControlledSquares = getPossibleBishopMoves(piece.getFile(), piece.getRank());
            } else if(piece.getPieceType() == Piece.PieceType.ROOK){
                newControlledSquares = getPossibleRookMoves(piece.getFile(), piece.getRank());
            } else if(piece.getPieceType() == Piece.PieceType.QUEEN){
                newControlledSquares = getPossibleQueenMoves(piece.getFile(), piece.getRank());
            } else {
                newControlledSquares = getPossibleKingMoves(piece.getFile(), piece.getRank());
            }
            blackControlledSquares.replace(piece, newControlledSquares);
        }

        playerInCheck = new boolean[]{false, false};
        whiteCheckingPieces.clear();
        blackCheckingPieces.clear();

        for(Piece p : whitePieces){
            for(Integer[] square : whiteControlledSquares.get(p)){
                if(square[0] == blackKingSquare[0] && square[1] == blackKingSquare[1]){
                    playerInCheck[1] = true;
                    whiteCheckingPieces.add(p);
                }
            }
        }

        for(Piece p : blackPieces){
            for(Integer[] square : blackControlledSquares.get(p)){
                if(square[0] == whiteKingSquare[0] && square[1] == whiteKingSquare[1]){
                    playerInCheck[0] = true;
                    blackCheckingPieces.add(p);
                }
            }
        }
    }
}
