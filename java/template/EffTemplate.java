package template;

public class EffTemplate {
    // 0 eff + hp
    // 1 eff + mp
    // 2 x2cd
    // 3 x2 skill exp
    // 4 tang %hp toi da cua item 4 (179)
    // 5 start combo
    // 6 skill bien hinh bao dom, chim ung
    // 7 chong pk
    // 8 0exp
    // 9 tu choi tu than active fullset15
    // 10 tu choi tu than cooldown fullset15
    // 11 skill luff
    // 12 skill zoro
    // 13 skill sanji
    // 14 skill nami
    // 15 skill usop

    // 17 ve kinh nghiem dac biet
    // 18 skill boc pha
    // 19 30s accept pvp
    // 20 thu thach ve than
    // 21 bien thanh zoombie
    // 22 x2 tai nguyen
    //
    // 100 + option item (100- 127) eff skill buff
    // 200 + option skill: choang, chay mau,....
    // 300 : option kich an 300-312
    // 400 : option kich an cooldown 400-412
    public int id;
    public int param;
    public long time;

    public EffTemplate(int id, int param, long time) {
        this.id = id;
        this.param = param;
        this.time = time;
    }

    public static boolean check_eff_can_save(int id) {
        return id == 2 || id == 3 || id == 8 || id == 17 || id == 22;
    }

    public static boolean check_eff_remove_when_die(int id) {
        return id == 0 || id == 1 || (id >= 201 && id <= 216);
    }
}
