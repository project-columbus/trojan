package co.poweramp.crackapp;

import android.hardware.Camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * CrackApp
 * <p/>
 * Created by duncan on 26/12/15.
 * Copyright (c) 2015 Duncan Leo. All Rights Reserved.
 */
public class Util {
    public static byte[] getFileBytes(File file) throws IOException {
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read = 0;
            while ((read = ios.read(buffer)) != -1)
                ous.write(buffer, 0, read);
        } finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
                // swallow, since not that important
            }
            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
                // swallow, since not that important
            }
        }
        return ous.toByteArray();
    }

    public static int getFrontCameraId() {
        int numCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return -1;
    }
}
