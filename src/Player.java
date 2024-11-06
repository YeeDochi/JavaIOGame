
public class Player implements Objects{
    private int X, Y;
    private int Color;
    private int size;

    public Player(int x, int y, int color, int size) {
        X = x;
        Y = y;
        Color = color;
        this.size = size;
    }


    public int getX() {
        return this.X;
    }

    public int getY() {
        return this.Y;
    }

    public void setY(int y) {
        Y = y;
    }

    public void setX(int x) {
        X = x;
    }


    public void setColor(int color) {
        Color = color;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
