package com.yonglin.pdc.epaper_setting.data;

import java.util.HashMap;

public class GlobalData {

    private static GlobalData instance;

    // Global variables
    // profiles list;
    private HashMap<String, DeviceProfile> profileMap = new HashMap<String, DeviceProfile>();
    // keep a copy of persisted data for comparision
    private HashMap<String, DeviceProfile> savedProfileMap = new HashMap<String, DeviceProfile>();
    // current profile to edit for profile page
//    private String currentEditProfile = "";
    private String log="";

    private GlobalData() {
    }

    public static synchronized GlobalData getInstance() {
        if (instance == null) {
            instance = new GlobalData();
        }
        return instance;
    }

    public HashMap<String, DeviceProfile> getProfileMap() {
        return profileMap;
    }

    public HashMap<String, DeviceProfile> getSavedProfileMap() {
        return savedProfileMap;
    }

    //    public String getCurrentEditProfile() {
//        return currentEditProfile;
//    }
//
//    public void setCurrentEditProfile(String currentEditProfile) {
//        this.currentEditProfile = currentEditProfile;
//    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}