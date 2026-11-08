package com.fytsmart.dashboard;

public final class ThemeManagerTest {
    private static int passed;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);passed++;}
    private static final class Disk implements ThemeManager.Storage {
        String plate,theme;boolean animations=true;int writes;
        @Override public String plate(){return plate;}
        @Override public String theme(){return theme;}
        @Override public boolean animations(){return animations;}
        @Override public void save(ThemeManager.Selection value){plate=value.plate;theme=value.theme.id;animations=value.animations;writes++;}
    }
    public static void main(String[] args){
        Disk disk=new Disk();ThemeManager manager=new ThemeManager(disk);
        ThemeManager.Selection initial=manager.load();
        check(initial.plate.equals("TU-PLACA"),"Existing plate is the default");
        check(initial.theme==ThemeConfig.DEFAULT&&initial.animations,"Original is the initial theme");
        check(disk.writes==0,"Loading or previewing never writes preferences");
        ThemeManager.Selection edited=new ThemeManager.Selection("  abc-123  ",ThemeConfig.CHRISTMAS,false);
        check(edited.plate.equals("ABC-123"),"Plate whitespace and case normalized");
        check(disk.writes==0&&manager.load().theme==ThemeConfig.DEFAULT,"Preview is independent of saved settings");
        manager.save("  abc-123  ",ThemeConfig.CHRISTMAS,false);
        check(disk.writes==1,"All appearance values use one save operation");
        ThemeManager.Selection recreated=new ThemeManager(disk).load();
        check(recreated.plate.equals("ABC-123")&&recreated.theme==ThemeConfig.CHRISTMAS&&!recreated.animations,"New manager recovers the stored selection");
        for(String invalid:new String[]{"ABC\nDEF","ABC\tDEF","<script>","🚗","-- -","ABCDEFGHIJKLM"}){
            int before=disk.writes;boolean rejected=false;
            try{manager.save(invalid,ThemeConfig.HALLOWEEN,true);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Invalid plate is rejected: "+invalid);
            check(disk.writes==before&&manager.load().theme==ThemeConfig.CHRISTMAS,"Invalid save leaves all saved values intact");
        }
        manager.save(" ",ThemeConfig.HALLOWEEN,true);
        check(manager.load().plate.equals("TU-PLACA"),"Blank plate restores default text");
        check(manager.load().theme==ThemeConfig.HALLOWEEN,"Theme can change independently of default plate");
        manager.save("ABC 123-XY",ThemeConfig.DEFAULT,false);
        check(manager.load().plate.equals("ABC 123-XY")&&manager.load().theme==ThemeConfig.DEFAULT,"Returning to Original preserves custom plate");
        check(!manager.load().animations,"Animation preference is independent of theme");
        check(VehiclePlateConfig.normalize("ñab-１２").equals("ÑAB-１２"),"Supported letters/numbers need no ASCII-only restriction");
        check(VehiclePlateConfig.normalize("ABCD1234-EFG").length()==12,"Maximum-length plate is accepted");
        disk.plate="invalid\nplate";disk.theme="unrecognized";
        check(manager.load().plate.equals("TU-PLACA")&&manager.load().theme==ThemeConfig.DEFAULT,"Corrupt stored values fall back safely");
        for(String id:new String[]{null,"","Default","summer","2026-12-25"})check(ThemeConfig.fromId(id)==ThemeConfig.DEFAULT,"Unknown/date-based theme never activates decorations");
        check(ThemeConfig.fromId("christmas")==ThemeConfig.CHRISTMAS&&ThemeConfig.fromId("halloween")==ThemeConfig.HALLOWEEN,"Only explicit theme IDs select seasons");
        System.out.println("ThemeManager: "+passed+" checks passed; storage contract tests are local, not a device restart test");
    }
}
