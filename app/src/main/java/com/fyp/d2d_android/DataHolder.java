package com.fyp.d2d_android;

import org.json.JSONObject;

public class DataHolder {
    public String url;
    public JSONObject json;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JSONObject getJson() {
        return json;
    }

    public void setJson(JSONObject json) {
        this.json = json;
    }
}