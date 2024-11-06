import java.util.Random;

public class Food implements Objects{
    Random random = new Random();
    private int X,Y;

    public Food(int Max){
        X = random.nextInt(Max);
        Y = random.nextInt(Max);
    }


    @Override
    public int getX() {
        return X;
    }

    @Override
    public int getY() {
        return Y;
    }

    @Override
    public void setX(int x) {
        X = x;
    }

    @Override
    public void setY(int y) {
        Y = y;
    }


}
