package com.fytsmart.dashboard;
public final class GpsTelemetryTest {
    static int checks;static void check(boolean v){if(!v)throw new AssertionError("Telemetry "+checks);checks++;}
    public static void main(String[] args){
        check(GpsTelemetry.accuracyColor(true,true,10,10,25)==GpsTelemetry.GREEN);
        check(GpsTelemetry.accuracyColor(true,true,10.1f,10,25)==GpsTelemetry.YELLOW);
        check(GpsTelemetry.accuracyColor(true,true,25,10,25)==GpsTelemetry.YELLOW);
        check(GpsTelemetry.accuracyColor(true,true,25.1f,10,25)==GpsTelemetry.RED);
        check(GpsTelemetry.accuracyColor(false,true,1,10,25)==GpsTelemetry.RED);
        check(GpsTelemetry.accuracyColor(true,false,1,10,25)==GpsTelemetry.RED);
        check(GpsTelemetry.accuracyColor(true,true,Float.NaN,10,25)==GpsTelemetry.RED);
        check(GpsTelemetry.accuracyColor(true,true,15,20,30)==GpsTelemetry.GREEN);
        String[] dirs={"N","NE","E","SE","S","SO","O","NO"};
        for(int i=0;i<8;i++)check(dirs[i].equals(GpsTelemetry.direction(true,true,5,true,i*45,false,0)));
        check("--".equals(GpsTelemetry.direction(true,true,0,true,90,false,0)));
        check("--".equals(GpsTelemetry.direction(false,true,5,true,90,false,0)));
        check("--".equals(GpsTelemetry.direction(true,true,5,false,90,false,0)));
        check("--".equals(GpsTelemetry.direction(true,true,5,true,90,true,40)));
        check("N".equals(GpsTelemetry.direction(true,true,5,true,359,false,0)));
        System.out.println("GpsTelemetry: "+checks+" checks passed");
    }
}
