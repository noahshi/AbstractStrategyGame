import java.security.InvalidParameterException;
import java.util.*;
import java.util.regex.Pattern;



//TODO:
//remove moving pieces pinned to king from legalMoves
//add castling
//add resigning etc.
//add variants

public class Chess implements AbstractStrategyGame{
    
    private Piece[][] board = new Piece[8][8];
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
        for(int j = 0; j < ranks.length; j++){
            for(int i = 0; i < ranks[j].length(); i++){
                //when switch statements are not allowed
                char currentChar = ranks[j].charAt(i);
                if(currentChar == 'p'){
                    board[i][j] = new Piece(Piece.PieceType.PAWN, false, i , j);
                    blackControlledSquares.put(board[i][j], getPossiblePawnCaptures(i, j, false));

                } else if(currentChar == 'n'){
                    board[i][j] = new Piece(Piece.PieceType.KNIGHT, false, i , j);
                    blackControlledSquares.put(board[i][j], getPossibleKnightMoves(i, j));

                } else if(currentChar == 'b'){
                    board[i][j] = new Piece(Piece.PieceType.BISHOP, false, i , j);
                    blackControlledSquares.put(board[i][j], getPossibleBishopMoves(i, j));

                } else if(currentChar == 'r'){
                    board[i][j] = new Piece(Piece.PieceType.ROOK, false, i , j);
                    blackControlledSquares.put(board[i][j], getPossibleRookMoves(i, j));

                } else if(currentChar == 'q'){
                    board[i][j] = new Piece(Piece.PieceType.QUEEN, false, i , j);
                    blackControlledSquares.put(board[i][j], getPossibleQueenMoves(i, j));

                } else if(currentChar == 'k'){
                    board[i][j] = new Piece(Piece.PieceType.KING, false, i , j);
                    blackControlledSquares.put(board[i][j], getPossibleKingMoves(i, j));
                    blackKingSquare = new Integer[]{i, j};

                } else if(currentChar == 'P'){
                    board[i][j] = new Piece(Piece.PieceType.PAWN, true, i , j);
                    whiteControlledSquares.put(board[i][j], getPossiblePawnCaptures(i, j, true));

                } else if(currentChar == 'N'){
                    board[i][j] = new Piece(Piece.PieceType.KNIGHT, true, i , j);
                    whiteControlledSquares.put(board[i][j], getPossibleKnightMoves(i, j));

                } else if(currentChar == 'B'){
                    board[i][j] = new Piece(Piece.PieceType.BISHOP, true, i , j);
                    whiteControlledSquares.put(board[i][j], getPossibleBishopMoves(i, j));

                } else if(currentChar == 'R'){
                    board[i][j] = new Piece(Piece.PieceType.ROOK, true, i , j);
                    whiteControlledSquares.put(board[i][j], getPossibleRookMoves(i, j));

                } else if(currentChar == 'Q'){
                    board[i][j] = new Piece(Piece.PieceType.QUEEN, true, i , j);
                    whiteControlledSquares.put(board[i][j], getPossibleQueenMoves(i, j));

                } else if(currentChar == 'K'){
                    board[i][j] = new Piece(Piece.PieceType.KING, true, i , j);
                    whiteControlledSquares.put(board[i][j], getPossibleKingMoves(i, j));
                    whiteKingSquare = new Integer[]{i, j};

                } else {
                    i += Character.getNumericValue(currentChar) - 1;
                }

                if(board[i][j] != null){
                    if(board[i][j].isWhite()){
                        whitePieces.add(board[i][j]);
                    }else {
                        blackPieces.add(board[i][j]);
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
            enPassantSquare = new int[]{(int)temp[3].charAt(0) - (int)'a', 8 - Character.getNumericValue(temp[3].charAt(1))};
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
            for(int j = 0; j < 8; j++){
                for(int i = 0; i < 8; i++){
                    String background = ((i + j) % 2) == 0 ? "\u001b[47m" : "\u001b[40m";
                    boardString += background;
                    String color = "";
                    if(board[i][j] != null){
                        color = board[i][j].isWhite() ? "\u001b[38;2;253;182;0m" : "\u001b[38;5;21m";
                        boardString += color;
                        if(board[i][j].getPieceType() == Piece.PieceType.PAWN){
                            boardString += " P ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.KNIGHT){
                            boardString += " N ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.BISHOP){
                            boardString += " B ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.ROOK){
                            boardString += " R ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.QUEEN){
                            boardString += " Q ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.KING){
                            boardString += " K ";
                        } else {
                            boardString += "   ";
                        }
                    } else {
                        boardString += "   ";
                    }
                    
                    boardString += "\u001b[0m";
                }
                boardString += " " + (8 - j) + "\n";
            }
            boardString += " a  b  c  d  e  f  g  h ";
        }else {
            for(int j = 7; j >= 0; j--){
                for(int i = 7; i >= 0; i--){
                    String background = ((i + j) % 2) == 0 ? "\u001b[47m" : "\u001b[40m";
                    boardString += background;
                    String color = "";
                    if(board[i][j] != null){
                        color = board[i][j].isWhite() ? "\u001b[38;2;253;182;0m" : "\u001b[38;5;21m";
                        boardString += color;
                        if(board[i][j].getPieceType() == Piece.PieceType.PAWN){
                            boardString += " P ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.KNIGHT){
                            boardString += " N ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.BISHOP){
                            boardString += " B ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.ROOK){
                            boardString += " R ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.QUEEN){
                            boardString += " Q ";
                        } else if(board[i][j].getPieceType() == Piece.PieceType.KING){
                            boardString += " K ";
                        } else {
                            boardString += "   ";
                        }
                    } else {
                        boardString += "   ";
                    }

                    boardString += "\u001b[0m";
                }
                boardString += " " + (8 - j) + "\n";
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
        int startRank = 8 - Character.getNumericValue(move.charAt(1));
        int endFile = (int)move.charAt(2) - (int)'a';
        int endRank = 8 - Character.getNumericValue(move.charAt(3));

        if(board[startFile][startRank] == null){
            throw new IllegalArgumentException("Piece does not exist.");
        }

        if(board[startFile][startRank].isWhite() != whiteTurn){
            throw new IllegalArgumentException("That is not your piece.");
        }

        Piece p = board[startFile][startRank];
        for(Integer[] legal : getLegalMoves().get(p)){
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
        pgn += TYPE_TO_STRING.get(p.getPieceType());

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

        if(p.getPieceType() == Piece.PieceType.PAWN){
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

        board[endFile][endRank] = p;
        p.setLocation(endFile, endRank);
        board[startFile][startRank] = null;

        //updating king square
        if(p.getPieceType() == Piece.PieceType.KING){
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

        if(enPassantSquare != null)
            System.out.println(enPassantSquare[0] + " " + enPassantSquare[1]);
    }

    public String getMoves(){
        return startingFEN += "\n" + pgn;
    }

    private String boardToFENShort(){
        String FEN = "";
        int nullCounter = 0;
        for(int j = 0; j < board.length; j++){
            for(int i = 0; i < board.length; i++){
                if(board[i][j] == null){
                    nullCounter ++;
                } else {
                    if(nullCounter != 0){
                        FEN += nullCounter;
                        nullCounter = 0;
                    }
                    if(board[i][j].isWhite()){
                        FEN += TYPE_TO_STRING.get(board[i][j].getPieceType());
                    } else {
                        FEN += TYPE_TO_STRING.get(board[i][j].getPieceType()).toLowerCase();
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
            fullFEN += (char)(enPassantSquare[0] + (int)'a') + (8 - enPassantSquare[1]);
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
            if(move[0] > 7 || move[0] < 0 || move[1] > 7 || move[1] < 0){
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
        for(int i = -1; i < 2; i++){
            for(int j = -1; j < 2; j++){
                if( i != 0 && j != 0){
                    moves.add(new Integer[]{file + i, rank + j});
                }
            }
        }

        Set<Integer[]> outofBounds = new HashSet<>();
        for(Integer[] move : moves){
            if(move[0] > 7 || move[0] < 0 || move[1] > 7 || move[1] < 0){
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
        if(tempFile < 8 && tempFile >= 0 && tempRank < 8 && tempRank >= 0){
            attacks.add(new Integer[]{tempFile, tempRank});
            while(tempFile < 8 && tempFile >= 0 && tempRank < 8 && tempRank >= 0 && board[tempFile][tempRank] == null){
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
            for(Piece p : whitePieces){
                legalMoves.put(p, new HashSet<Integer[]>());
                for(Integer[] move : whiteControlledSquares.get(p)){
                    legalMoves.get(p).add(move);
                    if(board[move[0]][move[1]] != null && board[move[0]][move[1]].isWhite()){
                        legalMoves.get(p).remove(move);
                    }
                }
                if(p.getPieceType() == Piece.PieceType.KING){
                    for(Integer[] move : whiteControlledSquares.get(p)){
                        for(Piece q : blackControlledSquares.keySet()){
                            for(Integer[] square : blackControlledSquares.get(q)){
                                if(square[0] == move[0] && square[1] == move[1]){
                                    legalMoves.get(p).remove(move);
                                }
                            }
                        }
                    }
                }
                if(p.getPieceType() == Piece.PieceType.PAWN){
                    //removing empty pawn captures from legal move list
                    Set<Integer[]> emptyCaptures = new HashSet<>();
                    for(Integer[] move : legalMoves.get(p)){
                        if(board[move[0]][move[1]] == null){
                            emptyCaptures.add(move);
                        }
                        
                        //re-adding en passant
                        if(enPassantSquare != null && enPassantSquare[0] == move[0] && enPassantSquare[1] == move[1]){
                            emptyCaptures.remove(move);
                        }
                    }
                    

                    for(Integer[] move : emptyCaptures){
                        legalMoves.get(p).remove(move);
                    }

                    for(Integer[] move : getPossiblePawnMoves(p.getFile(), p.getRank(), p.isWhite())){
                        legalMoves.get(p).add(move);
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

                for(Piece p : whitePieces){
                    actuallyLegalMoves.put(p, new HashSet<Integer[]>());
                    if(p.getPieceType() != Piece.PieceType.KING){
                        for(Integer[] move : legalMoves.get(p)){
                            for(Integer[] square : prioritySquares){
                                if(move[0] == square[0] && move[1] == square[1]){
                                    actuallyLegalMoves.get(p).add(move);
                                }
                            }
                        }
                    } else {
                        for(Integer[] move : legalMoves.get(p)){
                            actuallyLegalMoves.get(p).add(move);
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
            for(Piece p : blackPieces){
                legalMoves.put(p, new HashSet<Integer[]>());
                for(Integer[] move : blackControlledSquares.get(p)){
                    legalMoves.get(p).add(move);
                    if(board[move[0]][move[1]] != null && !board[move[0]][move[1]].isWhite()){
                        legalMoves.get(p).remove(move);
                    }
                }
                if(p.getPieceType() == Piece.PieceType.KING){
                    for(Integer[] move : blackControlledSquares.get(p)){
                        for(Piece q : whiteControlledSquares.keySet()){
                            for(Integer[] square : whiteControlledSquares.get(q)){
                                if(square[0] == move[0] && square[1] == move[1]){
                                    legalMoves.get(p).remove(move);
                                }
                            }
                        }
                    }
                }
                if(p.getPieceType() == Piece.PieceType.PAWN){
                    Set<Integer[]> emptyCaptures = new HashSet<>();
                    for(Integer[] move : legalMoves.get(p)){
                        if(board[move[0]][move[1]] == null){
                            emptyCaptures.add(move);
                        }

                        if(enPassantSquare != null && enPassantSquare[0] == move[0] && enPassantSquare[1] == move[1]){
                            emptyCaptures.remove(move);
                        }
                    }
                    
                    for(Integer[] move : emptyCaptures){
                        legalMoves.get(p).remove(move);
                    }
                    
                    for(Integer[] move : getPossiblePawnMoves(p.getFile(), p.getRank(), p.isWhite())){
                        legalMoves.get(p).add(move);
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

                for(Piece p : blackPieces){
                    actuallyLegalMoves.put(p, new HashSet<Integer[]>());
                    if(p.getPieceType() != Piece.PieceType.KING){
                        for(Integer[] move : legalMoves.get(p)){
                            for(Integer[] square : prioritySquares){
                                if(move[0] == square[0] && move[1] == square[1]){
                                    actuallyLegalMoves.get(p).add(move);
                                }
                            }
                        }
                    } else {
                        for(Integer[] move : legalMoves.get(p)){
                            actuallyLegalMoves.get(p).add(move);
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
        for(Piece q : whitePieces){
            Set<Integer[]> newControlledSquares;
            if(q.getPieceType() == Piece.PieceType.PAWN){
                newControlledSquares = getPossiblePawnCaptures(q.getFile(), q.getRank(), true);
            } else if(q.getPieceType() == Piece.PieceType.KNIGHT){
                newControlledSquares = getPossibleKnightMoves(q.getFile(), q.getRank());
            } else if(q.getPieceType() == Piece.PieceType.BISHOP){
                newControlledSquares = getPossibleBishopMoves(q.getFile(), q.getRank());
            } else if(q.getPieceType() == Piece.PieceType.ROOK){
                newControlledSquares = getPossibleRookMoves(q.getFile(), q.getRank());
            } else if(q.getPieceType() == Piece.PieceType.QUEEN){
                newControlledSquares = getPossibleQueenMoves(q.getFile(), q.getRank());
            } else {
                newControlledSquares = getPossibleKingMoves(q.getFile(), q.getRank());
            }
            whiteControlledSquares.replace(q, newControlledSquares);
        }
        for(Piece q : blackPieces){
            Set<Integer[]> newControlledSquares;
            if(q.getPieceType() == Piece.PieceType.PAWN){
                newControlledSquares = getPossiblePawnCaptures(q.getFile(), q.getRank(), false);
            } else if(q.getPieceType() == Piece.PieceType.KNIGHT){
                newControlledSquares = getPossibleKnightMoves(q.getFile(), q.getRank());
            } else if(q.getPieceType() == Piece.PieceType.BISHOP){
                newControlledSquares = getPossibleBishopMoves(q.getFile(), q.getRank());
            } else if(q.getPieceType() == Piece.PieceType.ROOK){
                newControlledSquares = getPossibleRookMoves(q.getFile(), q.getRank());
            } else if(q.getPieceType() == Piece.PieceType.QUEEN){
                newControlledSquares = getPossibleQueenMoves(q.getFile(), q.getRank());
            } else {
                newControlledSquares = getPossibleKingMoves(q.getFile(), q.getRank());
            }
            blackControlledSquares.replace(q, newControlledSquares);
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
