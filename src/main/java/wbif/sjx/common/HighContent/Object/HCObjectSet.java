package wbif.sjx.common.HighContent.Object;

import java.util.HashMap;

/**
 * Created by sc13967 on 12/05/2017.
 */
public class HCObjectSet extends HashMap<Integer,HCObject> {
    HCName name;

    public HCObjectSet(HCName name) {
        this.name = name;
    }

    public HCName getName() {
        return name;
    }
}
