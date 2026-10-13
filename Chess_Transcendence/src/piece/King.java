package piece;

import main.GamePanel;
import main.Type;

public class King extends Piece {
    public King(int color, int col, int row) {
        super(color, col, row);
        type = Type.KING;

        if (color == GamePanel.WHITE) {
            image = getImage("/piece/white_king");
        } else {
            image = getImage("/piece/black_king");
        }
    }

    private boolean isSquareUnderAttack(int targetCol, int targetRow) {//evito enroque en jaque
        for (Piece piece : GamePanel.simPieces) {
            if (piece.color != this.color) {
                if (piece.canMove(targetCol, targetRow)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow)) {
            if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1 || Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 1) {
                if (isValidSquare(targetCol, targetRow)) {
                    return true;
                }
            }
            if (!moved) {
                if (targetCol == preCol + 2 && targetRow == preRow && !pieceIsOnstraightLine(targetCol, targetRow)) {
                    if (!isSquareUnderAttack(preCol, preRow) && !isSquareUnderAttack(preCol + 1, preRow)) {
                        for (Piece piece : GamePanel.simPieces) {
                            if (piece.col == preCol + 3 && piece.row == preRow && !piece.moved) {
                                GamePanel.castlingP = piece;
                                return true;
                            }
                        }
                    }
                }
                if (targetCol == preCol - 2 && targetRow == preRow && !pieceIsOnstraightLine(targetCol, targetRow)) {
                    if (!isSquareUnderAttack(preCol, preRow) && !isSquareUnderAttack(preCol - 1, preRow)) {
                        for (Piece piece : GamePanel.simPieces) {
                            if (piece.col == preCol - 4 && piece.row == targetRow && !piece.moved) {
                                if (getHittingP(preCol - 1, targetRow) == null && getHittingP(preCol - 2, targetRow) == null && getHittingP(preCol - 3, targetRow) == null) {
                                    GamePanel.castlingP = piece;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}