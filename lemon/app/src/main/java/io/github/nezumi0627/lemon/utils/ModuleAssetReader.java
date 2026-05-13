package io.github.nezumi0627.lemon.utils;

import android.content.Context;

public final class ModuleAssetReader {
    private ModuleAssetReader() {}

    public static String readText(Context context, String fileName) throws Exception {
        Context moduleContext = context.createPackageContext("io.github.nezumi0627.lemon", Context.CONTEXT_IGNORE_SECURITY);
        try (java.io.InputStream in = moduleContext.getAssets().open(fileName);
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }
}
