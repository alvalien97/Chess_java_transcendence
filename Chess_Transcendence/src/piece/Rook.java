package piece;

import main.GamePanel;
import main.Type;

public class Rook extends Piece{
    public Rook(int color, int col, int row){
        super(color, col, row);

        type = Type.ROOK;

        if(color == GamePanel.WHITE){
            image = getImage("/piece/white_tower");
        }else{
            image = getImage("/piece/black_tower");
        }
    }

    @Override
    public boolean canMove(int targetCol, int targetRow) {
        if(isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if(targetCol == preCol || targetRow == preRow){
                if(isValidSquare(targetCol, targetRow) && pieceIsOnstraightLine(targetCol, targetRow) == false) {
                    return true;
                }
            }
        }
        return false;
    }
}
