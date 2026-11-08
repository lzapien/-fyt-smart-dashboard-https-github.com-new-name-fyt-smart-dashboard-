package com.fytsmart.dashboard;

import java.util.Arrays;
import java.util.Collections;

public final class MediaSelectionPolicyTest {
    private static int passed;
    private static MediaSelectionPolicy.Candidate c(String id,String pkg,int priority,long updated) {
        return new MediaSelectionPolicy.Candidate(id,pkg,priority,updated);
    }
    private static void check(String wanted,String actual) {
        if(wanted==null?actual!=null:!wanted.equals(actual))throw new AssertionError(wanted+" != "+actual);
        passed++;
    }
    public static void main(String[] args) {
        MediaSelectionPolicy.Candidate amazon=c("a",MediaSources.AMAZON,3,10);
        MediaSelectionPolicy.Candidate youtube=c("y",MediaSources.YOUTUBE,3,20);
        MediaSelectionPolicy.Candidate radio=c("r",MediaSources.NAVRADIO,3,30);
        check(null,MediaSelectionPolicy.choose(Collections.emptyList(),null,null));
        check("a",MediaSelectionPolicy.choose(Arrays.asList(amazon),null,null));
        check("y",MediaSelectionPolicy.choose(Arrays.asList(amazon,youtube),"a",null));
        check("r",MediaSelectionPolicy.choose(Arrays.asList(amazon,youtube,radio),"y",null));
        check("a",MediaSelectionPolicy.choose(Arrays.asList(amazon,c("noise","com.syu.radio",3,999)),null,null));
        check(null,MediaSelectionPolicy.choose(Arrays.asList(c("noise","other.app",3,999)),null,null));
        check("y",MediaSelectionPolicy.choose(Arrays.asList(amazon,c("y",MediaSources.YOUTUBE,3,10)),"y",null));
        check("a",MediaSelectionPolicy.choose(Arrays.asList(amazon,c("paused",MediaSources.NAVRADIO,1,999)),null,null));
        check("r",MediaSelectionPolicy.choose(Arrays.asList(c("y",MediaSources.YOUTUBE,1,999),c("r",MediaSources.NAVRADIO,2,5)),null,null));
        check("r",MediaSelectionPolicy.choose(Arrays.asList(c("a",MediaSources.AMAZON,1,1),c("r",MediaSources.NAVRADIO,1,2)),null,null));
        check("a",MediaSelectionPolicy.choose(Arrays.asList(amazon,youtube,radio),null,MediaSources.AMAZON));
        check("r",MediaSelectionPolicy.choose(Arrays.asList(amazon,c("r",MediaSources.NAVRADIO,1,1)),null,MediaSources.NAVRADIO));
        check(null,MediaSelectionPolicy.choose(Arrays.asList(amazon,youtube),"a",MediaSources.NAVRADIO));
        check("y",MediaSelectionPolicy.choose(Arrays.asList(youtube),"destroyed",null));
        check("y",MediaSelectionPolicy.choose(Arrays.asList(amazon,youtube),null,null));
        check(null,MediaSelectionPolicy.choose(Arrays.asList(amazon),null,"other.app"));
        if(!MediaSources.name(MediaSources.NAVRADIO).equals("NavRadio+")||MediaSources.supported("com.navimods.radio_free"))throw new AssertionError("Wrong radio identity");passed++;
        System.out.println("MediaSelectionPolicy: "+passed+" checks passed; OEM/unknown sessions excluded");
    }
}
