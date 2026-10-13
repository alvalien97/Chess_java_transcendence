package main;

import piece.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable {
    public static /*final*/ int WIDTH = 1100;
    public static /*final*/ int HEIGHT = 800;
    final int FPS = 60;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    //PIEZAS
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    ArrayList<Piece> promoPieces = new ArrayList<>();
    Piece activeP, checkingP;
    public static Piece castlingP;

    //COLOR
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    //BOOLEANOS
    boolean canMove;
    boolean validSquare;
    boolean promotion;
    boolean gameover;
    boolean stalemate;
    boolean restartRequested = false;

    public GamePanel(){
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        setLayout(null); // Permite posicionar componentes de Swing de forma libre

        // Crear el botón de Restart
        JButton restartButton = new JButton("Restart Game");
        restartButton.setBounds(840, 350, 200, 50);
        restartButton.setFont(new Font("Arial", Font.BOLD, 16));
        restartButton.setFocusPainted(false);
        restartButton.addActionListener(e -> restartRequested = true);
        add(restartButton);

        addMouseMotionListener(mouse);
        addMouseListener(mouse);
        setPieces();
        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces(){
        pieces.add(new Pawn(WHITE,0,6));
        pieces.add(new Pawn(WHITE,1,6));
        pieces.add(new Pawn(WHITE,2,6));
        pieces.add(new Pawn(WHITE,3,6));
        pieces.add(new Pawn(WHITE,4,6));
        pieces.add(new Pawn(WHITE,5,6));
        pieces.add(new Pawn(WHITE,6,6));
        pieces.add(new Pawn(WHITE,7,6));
        pieces.add(new Rook(WHITE,0,7));
        pieces.add(new Rook(WHITE,7,7));
        pieces.add(new Knight(WHITE,1,7));
        pieces.add(new Knight(WHITE,6,7));
        pieces.add(new Bishop(WHITE,2,7));
        pieces.add(new Bishop(WHITE,5,7));
        pieces.add(new Queen(WHITE,3,7));
        pieces.add(new King(WHITE,4,7));

        pieces.add(new Pawn(BLACK,0,1));
        pieces.add(new Pawn(BLACK,1,1));
        pieces.add(new Pawn(BLACK,2,1));
        pieces.add(new Pawn(BLACK,3,1));
        pieces.add(new Pawn(BLACK,4,1));
        pieces.add(new Pawn(BLACK,5,1));
        pieces.add(new Pawn(BLACK,6,1));
        pieces.add(new Pawn(BLACK,7,1));
        pieces.add(new Rook(BLACK,0,0));
        pieces.add(new Rook(BLACK,7,0));
        pieces.add(new Knight(BLACK,1,0));
        pieces.add(new Knight(BLACK,6,0));
        pieces.add(new Bishop(BLACK,2,0));
        pieces.add(new Bishop(BLACK,5,0));
        pieces.add(new Queen(BLACK,3,0));
        pieces.add(new King(BLACK,4,0));
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for(int i = 0; i < source.size(); i++){
            target.add(source.get(i));
        }
    }

    @Override
    public void run() {
        double drawInterval = 1000000000/FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while(gameThread != null)
        {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime)/drawInterval;
            lastTime = currentTime;
            if(delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    private void resetGame() {
        pieces.clear();
        setPieces();
        copyPieces(pieces, simPieces);
        activeP = null;
        checkingP = null;
        castlingP = null;
        currentColor = WHITE;
        gameover = false;
        stalemate = false;
        promotion = false;
        canMove = false;
        validSquare = false;
    }

    private void update() {
        if (restartRequested) {
            resetGame();
            restartRequested = false;
            return;
        }
        if(promotion){
            promoting();
        } else if(!gameover && !stalemate){
            if(mouse.pressed){
                if(activeP == null) {
                    for(Piece piece : simPieces) {
                        if(piece.color == currentColor && piece.col == mouse.x/Board.SQUARE_SIZE && piece.row == mouse.y/Board.SQUARE_SIZE) {
                            activeP = piece;
                        }
                    }
                } else {
                    simulate();
                }
            }
            if(!mouse.pressed) {
                if(activeP != null) {
                    if(validSquare){
                        copyPieces(simPieces, pieces);
                        activeP.updatePosition();
                        if(castlingP != null) {
                            castlingP.updatePosition();
                        }
                        if(isKingInCheck() && isCheckmate()) {
                            gameover = true;
                        } else if((isStalemate() && !isKingInCheck()) || isInsufficientMaterial()) {
                            stalemate = true; // Declara tablas en el panel
                        } else {
                            if(canPromote()){
                                promotion = true;
                            } else {
                                changePlayer();
                            }
                        }
                    } else {
                        copyPieces(pieces, simPieces);
                        activeP.resetPosition();
                        activeP = null;
                    }
                }
            }
        }
    }

    private void simulate() {

        canMove = false;
        validSquare = false;
        copyPieces(pieces, simPieces);
        if(castlingP != null){
            castlingP.col = castlingP.preCol;
            castlingP.x = castlingP.getX(castlingP.col);
            castlingP = null;
        }
        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);
        if(activeP.canMove(activeP.col, activeP.row)) {
            canMove = true;
            if(activeP.hittingP != null) {
                simPieces.remove(activeP.hittingP.getIndex());
            }
            checkCastling();
            if(!isIllegal(activeP) && !opponentCanCaptureKing()) {
                validSquare = true;
            }
        }
    }

    private boolean isIllegal(Piece king) {
        if(king.type == Type.KING) {
            for(Piece piece : simPieces) {
                if(piece != king && piece.color != king.color && piece.canMove(king.col, king.row)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean opponentCanCaptureKing() {
        Piece king = getKing(false);
        for(Piece piece : simPieces) {
            if(piece.color != king.color && piece.canMove(king.col, king.row)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKingInCheck() {
        Piece king = getKing(true);
        checkingP = null;
        for (Piece piece : simPieces) {
            if (piece.color == currentColor && piece.canMove(king.col, king.row)) {
                checkingP = piece;
                return true;
            }
        }
        return false;
    }

    private boolean isInsufficientMaterial() {
        ArrayList<Piece> whitePieces = new ArrayList<>();
        ArrayList<Piece> blackPieces = new ArrayList<>();
        for (Piece p : simPieces) {
            if (p.color == WHITE) {
                whitePieces.add(p);
            } else {
                blackPieces.add(p);
            }
        }
        // Caso 1: Rey vs Rey
        if (whitePieces.size() == 1 && blackPieces.size() == 1) {
            return true;
        }
        // Caso 2: Rey + Alfil o Caballo vs Rey
        if (whitePieces.size() == 2 && blackPieces.size() == 1) {
            for (Piece p : whitePieces) {
                if (p.type == Type.BISHOP || p.type == Type.KNIGHT) {
                    return true;
                }
            }
        } else if (whitePieces.size() == 1 && blackPieces.size() == 2) {
            for (Piece p : blackPieces) {
                if (p.type == Type.BISHOP || p.type == Type.KNIGHT) {
                    return true;
                }
            }
        }
        return false;
    }

    private Piece getKing(boolean opponent) {
        Piece king = null;
        for(Piece piece : simPieces) {
            if(opponent) {
                if(piece.type == Type.KING && piece.color != currentColor) {
                    king = piece;
                }
            } else {
                if(piece.type == Type.KING && piece.color == currentColor){
                    king = piece;
                }
            }
        }
        return king;
    }

    /*private boolean isCheckmate() {
        Piece king = getKing(true);
        if(kingCanMove(king)) {
            return false;
        } else {
            int colDiff = Math.abs(checkingP.col - king.col);
            int rowDiff = Math.abs(checkingP.row - king.row);
            if(colDiff == 0) {
                if(checkingP.row < king.row) {
                    for(int row = checkingP.row; row < king.row; row++) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }
                if(checkingP.row > king.row) {
                    for(int row = checkingP.row; row > king.row; row--) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (rowDiff == 0) {
                if(checkingP.col < king.col) {
                    for(int col = checkingP.col; col < king.col; col++) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }
                if(checkingP.col > king.col) {
                    for(int col = checkingP.col; col > king.col; col--) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (colDiff == rowDiff) {
                if(checkingP.row < king.row) {
                    if(checkingP.col < king.col) {
                        for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if(checkingP.col > king.col){
                        for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                if(checkingP.row > king.row) {
                    if(checkingP.col < king.col) {
                        for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if(checkingP.col > king.col) {
                        for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            } else {
                int x;
            }
        }
        return true;
    }*/

    private boolean isCheckmate() {
        Piece king = getKing(true);
        if(kingCanMove(king)) {
            return false;
        } else {
            if (checkingP == null) return true;

            // 1. NUEVO: Comprobar si alguna pieza puede capturar al atacante (checkingP)
            // Esto soluciona de forma limpia el caso del Caballo o cualquier otra pieza atacante.
            for(Piece piece : simPieces) {
                if(piece != king && piece.color != currentColor) {
                    if(piece.canMove(checkingP.col, checkingP.row)) {
                        return false; // ¡Se puede capturar al atacante, por lo tanto NO es mate!
                    }
                }
            }

            // 2. TU LÓGICA ORIGINAL de líneas rectas y diagonales para bloquear el jaque
            int colDiff = Math.abs(checkingP.col - king.col);
            int rowDiff = Math.abs(checkingP.row - king.row);
            if(colDiff == 0) {
                if(checkingP.row < king.row) {
                    for(int row = checkingP.row; row < king.row; row++) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }
                if(checkingP.row > king.row) {
                    for(int row = checkingP.row; row > king.row; row--) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (rowDiff == 0) {
                if(checkingP.col < king.col) {
                    for(int col = checkingP.col; col < king.col; col++) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }
                if(checkingP.col > king.col) {
                    for(int col = checkingP.col; col > king.col; col--) {
                        for(Piece piece : simPieces) {
                            if(piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (colDiff == rowDiff) {
                if(checkingP.row < king.row) {
                    if(checkingP.col < king.col) {
                        for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if(checkingP.col > king.col){
                        for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                if(checkingP.row > king.row) {
                    if(checkingP.col < king.col) {
                        for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                    if(checkingP.col > king.col) {
                        for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--) {
                            for(Piece piece : simPieces) {
                                if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean kingCanMove(Piece king) {

        if(isValideMove(king, -1, -1)){return true;}
        if(isValideMove(king, 0, -1)){return true;}
        if(isValideMove(king, 1, -1)){return true;}
        if(isValideMove(king, -1, 0)){return true;}
        if(isValideMove(king, 1, 0)){return true;}
        if(isValideMove(king, -1, 1)){return true;}
        if(isValideMove(king, 0, 1)){return true;}
        if(isValideMove(king, 1, 1)){return true;}

        return false;
    }

    private boolean isValideMove(Piece king, int colPlus, int rowPlus) {
        boolean isValidMove = false;
        king.col += colPlus;
        king.row += rowPlus;
        if(king.canMove(king.col, king.row)) {
            if(king.hittingP != null) {
                simPieces.remove(king.hittingP.getIndex());
            }
            if(!isIllegal(king)) {
                isValidMove = true;
            }
        }
        king.resetPosition();
        copyPieces(pieces, simPieces);
        return isValidMove;
    }

    private boolean isStalemate() {
        // El ahogado ocurre si el rey NO está en jaque y el jugador no tiene ningún movimiento legal disponible
        Piece king = getKing(false);
        if (opponentCanCaptureKing()) {
            return false; // Si está en jaque, nunca es ahogado
        }

        // Comprobamos si el rey o cualquier otra pieza del color actual puede moverse
        for (Piece piece : simPieces) {
            if (piece.color == currentColor) {
                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        if (piece.canMove(c, r)) {
                            // Simulamos el movimiento para ver si es legal (no deja al rey en jaque)
                            copyPieces(pieces, simPieces);
                            Piece targetP = piece.getHittingP(c, r);
                            if (targetP != null) {
                                simPieces.remove(targetP.getIndex());
                            }
                            int oldCol = piece.col;
                            int oldRow = piece.row;
                            piece.col = c;
                            piece.row = r;
                            piece.x = piece.getX(c);
                            piece.y = piece.getY(r);

                            boolean stillInCheck = opponentCanCaptureKing();

                            // Revertimos la simulación
                            piece.col = oldCol;
                            piece.row = oldRow;
                            piece.x = piece.getX(oldCol);
                            piece.y = piece.getY(oldRow);
                            copyPieces(pieces, simPieces);

                            if (!stillInCheck) {
                                return false; // Encontró al menos un movimiento legal, por lo que NO es ahogado
                            }
                        }
                    }
                }
            }
        }
        return true; // No hay ningún movimiento legal y no está en jaque -> ¡Ahogado!
    }

    private void checkCastling() {
        if(castlingP != null && !isKingInCheck()) {
            if(castlingP.col == 0) {
                castlingP.col += 3;
            }
            else if(castlingP.col == 7){
                castlingP.col -=2;
            }
            castlingP.x = castlingP.getX(castlingP.col);
        }
    }

    private void changePlayer() {
        if(currentColor == WHITE) {
            currentColor = BLACK;
            for(Piece piece : pieces) { //paso de peones
                if(piece.color == BLACK) {
                    piece.twoStepped = false;
                }
            }
        } else {
            currentColor = WHITE;
            for(Piece piece : pieces) { //paso de peones
                if(piece.color == WHITE) {
                    piece.twoStepped = false;
                }
            }
        }
        activeP = null;
    }

    private boolean canPromote(){
        if(activeP.type == Type.PAWN) {
            if(currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7){
                promoPieces.clear();
                promoPieces.add(new Rook(currentColor, 9, 2));
                promoPieces.add(new Knight(currentColor, 9, 3));
                promoPieces.add(new Bishop(currentColor, 9, 4));
                promoPieces.add(new Queen(currentColor, 9, 5));
                return true;
            }
        }
        return false;
    }

    private void promoting() {

        if(mouse.pressed) {
            for(Piece piece : promoPieces) {
                if(piece.col == mouse.x/Board.SQUARE_SIZE && piece.row == mouse.y/Board.SQUARE_SIZE){
                    switch(piece.type) {
                        case ROOK: simPieces.add(new Rook(currentColor, activeP.col, activeP.row)); break;
                        case KNIGHT: simPieces.add(new Knight(currentColor, activeP.col, activeP.row)); break;
                        case BISHOP: simPieces.add(new Bishop(currentColor, activeP.col, activeP.row)); break;
                        case QUEEN: simPieces.add(new Queen(currentColor, activeP.col, activeP.row)); break;
                        default: break;
                    }
                    simPieces.remove(activeP.getIndex());
                    copyPieces(simPieces, pieces);
                    activeP = null;
                    promotion = false;
                    changePlayer();
                }
            }
        }
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        board.draw(g2);
        for(Piece p : simPieces) { p.draw(g2); }
        if(activeP != null) {
            if(canMove) {
                if(isIllegal(activeP) || opponentCanCaptureKing()) {
                    g2.setColor(Color.red);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                } else {
                    g2.setColor(Color.white);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }
            activeP.draw(g2);
        }
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Book Antigua", Font.PLAIN, 40));
        g2.setColor(Color.white);

        if(promotion) {
            g2.drawString("Promote to: ", 840, 150);
            for(Piece piece: promoPieces) {
                g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
            }
        }
        else {
            if (currentColor == WHITE) {
                g2.drawString("White time", 840, 550);
                if(checkingP != null && checkingP.color == BLACK) {
                    g2.setColor(Color.red);
                    g2.drawString("The king", 840, 650);
                    g2.drawString(" is in check", 840, 700);
                }
            } else {
                g2.drawString("Black time", 840, 250);
                if(checkingP != null && checkingP.color == WHITE) {
                    g2.setColor(Color.red);
                    g2.drawString("The king", 840, 100);
                    g2.drawString(" is in check", 840, 150);
                }
            }
        }
        if(gameover) {
            String s = "";
            if(currentColor == WHITE) {
                s = "White won";
            } else {
                s = "Black won";
            }
            g2.setFont(new Font("Arial", Font.PLAIN, 90));
            g2.setColor(Color.green);
            g2.drawString(s, 200, 420);
        }
        if(stalemate) {
            g2.setFont(new Font("Arial", Font.PLAIN, 90));
            g2.setColor(Color.BLUE);
            g2.drawString("Stalemate", 200, 420);

        }
    }
}
