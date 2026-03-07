package com.example.vivofontswapper.util;

import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.Shizuku;

public class ShizukuUtils {

    private static final String TAG = "ShizukuUtils";

    public static class CommandResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public static boolean isShizukuAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            Log.e(TAG, "pingBinder failed", t);
            return false;
        }
    }

    public static boolean hasPermission() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            Log.e(TAG, "checkSelfPermission failed", t);
            return false;
        }
    }

    public static boolean requestPermission(int requestCode) {
        try {
            Shizuku.requestPermission(requestCode);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "requestPermission failed", t);
            return false;
        }
    }

    public static CommandResult exec(String command) {
        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        int exit = -1;

        Thread outThread = null;
        Thread errThread = null;
        IRemoteProcess remoteProcess = null;
        try {
            IShizukuService service = IShizukuService.Stub.asInterface(Shizuku.getBinder());
            if (service == null) {
                err.append("Shizuku service binder is null");
                return new CommandResult(-1, out.toString(), err.toString());
            }
            remoteProcess = service.newProcess(new String[]{"sh", "-c", command}, null, null);

            InputStream stdout = new ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.getInputStream());
            InputStream stderr = new ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.getErrorStream());
            outThread = new Thread(() -> readStream(stdout, out));
            errThread = new Thread(() -> readStream(stderr, err));
            outThread.start();
            errThread.start();
            exit = remoteProcess.waitFor();
            outThread.join(3000);
            errThread.join(3000);
        } catch (Throwable t) {
            Log.e(TAG, "exec failed: " + command, t);
            err.append(t.getMessage());
        } finally {
            if (remoteProcess != null) {
                try {
                    remoteProcess.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
        return new CommandResult(exit, out.toString(), err.toString());
    }

    private static void readStream(java.io.InputStream is, StringBuilder collector) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                collector.append(line).append('\n');
            }
        } catch (IOException ignored) {
        }
    }
}
