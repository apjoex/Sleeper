package com.sleeper;

/**
 * Created by apjoe on 6/22/2017.
 */

public class DeviceItem {
    String name,adddress;

    public DeviceItem(String name, String adddress) {
        this.name = name;
        this.adddress = adddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdddress() {
        return adddress;
    }

    public void setAdddress(String adddress) {
        this.adddress = adddress;
    }
}
