package template;

public class Option {

    public int id;
    private int param;

    public Option(int id, int param) {
        this.id = id;
        this.param = param;
    }

    public int getParam() {
        return param;
    }

    public int getParam(int type, int tier, int isHoanMy) {
        double multiplier = 1.0 + (tier * 0.1); // +10% mỗi cấp
        if (isHoanMy == 1) {
            multiplier *= 1.1;
        }
        return (int) Math.round(param * multiplier);
    }

    public void setParam(int param) {
        this.param = param;
    }
}
