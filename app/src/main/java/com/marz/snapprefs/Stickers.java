package com.marz.snapprefs;

import android.content.Context;
import android.content.res.XModuleResources;
import android.os.Environment;

import com.marz.snapprefs.Util.BiHashMap;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Stickers {
    private static BiHashMap<String, String> emojiNames = new BiHashMap<>();
    private static ArrayList<String> existing = new ArrayList<>();

    private static void initEmojiNames() {
        emojiNames.put("sticker1", "1f550");
        emojiNames.put("sticker2", "1f551");
        emojiNames.put("sticker3", "1f552");
        emojiNames.put("sticker4", "1f553");
        emojiNames.put("sticker5", "1f554");
        emojiNames.put("sticker6", "1f555");
        emojiNames.put("sticker7", "1f556");
        emojiNames.put("sticker8", "1f557");
        emojiNames.put("sticker9", "1f558");
        emojiNames.put("sticker10", "1f559");
        emojiNames.put("sticker11", "1f55a");
        emojiNames.put("sticker12", "1f55b");
        emojiNames.put("sticker13", "1f55c");
        emojiNames.put("sticker14", "1f55d");
        emojiNames.put("sticker15", "1f55e");
        emojiNames.put("sticker16", "1f55f");
        emojiNames.put("sticker17", "1f560");
        emojiNames.put("sticker18", "1f561");
        emojiNames.put("sticker19", "1f562");
        emojiNames.put("sticker20", "1f563");
        emojiNames.put("sticker21", "1f564");
        emojiNames.put("sticker22", "1f565");
        emojiNames.put("sticker23", "1f566");
    }
    static void initStickers(final XC_LoadPackage.LoadPackageParam lpparam, final XModuleResources modRes, final Context snapContext) {
        initEmojiNames();//init unicode-cool name map
        //List single emojis
        File myFile = new File(Environment.getExternalStorageDirectory() + "/Snapprefs/Stickers/");
        File[] files = myFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".svg") && new File(dir, filename.substring(0, filename.lastIndexOf(".")) + ".png").exists();
            }
        });
        for (File f : files) {
            String s = f.getName().substring(0, f.getName().lastIndexOf("."));
            existing.add(s);
        }
        //This method loads contents of a zip
        XposedHelpers.findAndHookMethod("Gn", lpparam.classLoader, "a", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (XposedHelpers.getBooleanField(methodHookParam.thisObject, "mIsUnzipped"))
                    return null;
                Context mContext = (Context) XposedHelpers.getObjectField(methodHookParam.thisObject, "mContext");
                InputStream is = null;
                try {
                    XposedHelpers.callMethod(methodHookParam.thisObject, "b");
                    is = mContext.getAssets().open((String) XposedHelpers.getObjectField(methodHookParam.thisObject, "mPath"));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                HashMap mAssets = (HashMap) XposedHelpers.getObjectField(methodHookParam.thisObject, "mAssets");
                Class<?> gna = lpparam.classLoader.loadClass("Gn$a");
                Constructor<?> constructor = XposedHelpers.findConstructorBestMatch(gna, lpparam.classLoader.loadClass("Gn"), byte[].class, int.class, int.class);
                while ((entry = zis.getNextEntry()) != null) {
                    String coolName = entry.getName().substring(0, entry.getName().lastIndexOf("."));
                    String type = entry.getName().substring(entry.getName().lastIndexOf("."));
                    String unicodeName = coolName;
                    int offset = output.size();
                    int length = 0;
                    if (emojiNames.containsKey(coolName)) {
                        unicodeName = emojiNames.get(coolName);
                    } else {
                        coolName = emojiNames.getByValue(unicodeName);
                    }
                    if (existing.contains(unicodeName)) {
                        byte[] bytes = readFile(unicodeName + type);
                        length = bytes.length;
                        output.write(bytes, 0, bytes.length);
                    } else if (existing.contains(coolName)) {
                        byte[] bytes = readFile(coolName + type);
                        length = bytes.length;
                        output.write(bytes, 0, bytes.length);
                    } else {
                        int i;
                        byte[] buffer = new byte[100000];
                        while ((i = zis.read(buffer)) > 0) {
                            length += i;
                            output.write(buffer, 0, i);
                        }
                    }
                    Object inst = null;
                    try {
                        inst = constructor.newInstance(methodHookParam.thisObject, null, offset, length);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    mAssets.put(unicodeName + type, inst);
                }
                byte[] mBuffer = output.toByteArray();
                XposedHelpers.setObjectField(methodHookParam.thisObject, "mBuffer", mBuffer);
                output.close();
                Field f = gna.getDeclaredField("data");
                for (Object e : mAssets.entrySet()) {
                    f.set(((Map.Entry) e).getValue(), mBuffer);

                }
                XposedHelpers.setBooleanField(methodHookParam.thisObject, "mIsUnzipped", true);
                return null;
            }
        });

            findAndHookMethod("android.content.res.AssetManager", lpparam.classLoader, "open", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Logger.log("Open asset: " + param.args[0], true);
                    String str = (String) param.args[0];
                    if (str.contains("twitter_emojis_")) {
                        String url = Environment.getExternalStorageDirectory() + "/Snapprefs/Stickers/" + str;
                        Logger.log("Sdcard path: " + url, true);
                        File file = new File(url);
                        InputStream is = null;
                        is = new BufferedInputStream(new FileInputStream(file));
                        param.setResult(is);
                        Logger.log("setResult for AssetManager", true);
                    }
                }
            });
        }

    public static byte[] readFile(String filename) {
        byte[] data = new byte[0];
        try {
            File myFile = new File(Environment.getExternalStorageDirectory() + "/Snapprefs/Stickers/" + filename);
            FileInputStream fIn = new FileInputStream(myFile);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int i;
            byte[] buffer = new byte[100000];
            while ((i = fIn.read(buffer)) > 0) {
                outputStream.write(buffer, 0, i);
            }
            data = outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            //Logger.log("INSTALL HANDLEEXTERNALSTORAGE TO FIX THE ISSUE -- FileUtils: File SDread failed " + e.toString(), true);
        }
        return data;
    }
}
