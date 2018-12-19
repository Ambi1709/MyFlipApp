package com.android.car.usb;

import android.os.Parcelable;

import java.util.*;

public class UsbDevice {

    private int mId;

    private String mName;

    private String mDescr;

    private String mDeviceId;

    private Map<String, UsbVolume> mVolumes = new HashMap<>();

    private boolean mScanned;

    public UsbDevice(int id, String deviceId, String name, String descr, UsbVolume usbVolume) {
        mId = id;
        mDeviceId = deviceId;
        mName = name;
        mDescr = descr;
        mVolumes.put(usbVolume.getId(), usbVolume);
    }

    public UsbDevice(int id, String deviceId, String name, String descr) {
        mId = id;
        mDeviceId = deviceId;
        mName = name;
        mDescr = descr;
    }

    public void addVolume(UsbVolume usbVolume) {
        mVolumes.put(usbVolume.getId(), usbVolume);
    }

    public void removeVolume(String id) {
        mVolumes.remove(id);
    }

    public int getId() {
        return mId;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public String getName() {
        return mName;
    }

    public String getDescr() {
        return mDescr;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setDescr(String descr) {
        mDescr = descr;
    }

    public List<UsbVolume> getVolumes() {
        return new ArrayList<>(mVolumes.values());
    }

    public String[] getVolumePaths() {
        Collection<UsbVolume> usbVolumes = mVolumes.values();
        String[] paths = new String[usbVolumes.size()];
        int i = 0;
        for (UsbVolume usbVolume : usbVolumes) {
            paths[i] = usbVolume.getPath();
            i++;
        }
        return paths;
    }

    public boolean isMounted() {
        for (UsbVolume usbVolume : mVolumes.values()) {
            if (!usbVolume.isMounted()) {
                return false;
            }
        }
        return !mVolumes.isEmpty();
    }

    public boolean containsPath(String path) {
        for (String volPath : getVolumePaths()) {
            if (volPath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    public boolean isScanned() {
        return mScanned;
    }

    public void setScanned(boolean scanned) {
        mScanned = scanned;
    }

    public boolean isReady() {
        return isMounted() && mScanned;
    }

    public int getVolumeCount() {
        return mVolumes.keySet().size();
    }
}
