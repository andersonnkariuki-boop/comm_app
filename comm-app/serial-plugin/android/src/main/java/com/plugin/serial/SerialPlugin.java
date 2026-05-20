package com.plugin.serial;

import com.getcapacitor.Logger;

public class SerialPlugin {

    public String echo(String value) {
        Logger.info("Echo", value);
        return value;
    }
}
