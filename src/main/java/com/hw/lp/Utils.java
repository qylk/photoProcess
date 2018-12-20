package com.hw.lp;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import ws.schild.jave.MultimediaInfo;
import ws.schild.jave.MultimediaObject;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by qylk on 2018/12/3.
 */
public class Utils {

    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final SimpleDateFormat gpsTimeFormatter;
    private static final SimpleDateFormat localTimeFormatter;

    static {
        gpsTimeFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss.S");
        localTimeFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss.S");
        gpsTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
    }

    public static void main(String[] args) {
        try {
            Date stamp = gpsTimeFormatter.parse("2018:10:07 10:37:12.00");
            System.out.println(localTimeFormatter.format(stamp));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //"2018:10:07 10:37:12.00"
    public static String getExifDateTag(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            String date = null;
            Directory d = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (d != null) {
                date = d.getDescription(ExifDirectoryBase.TAG_DATETIME_ORIGINAL);
            }

            if (date == null) {
                d = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                if (d != null) {
                    date = d.getDescription(GpsDirectory.TAG_DATE_STAMP);
                    if (date != null) {
                        date = date + " " +
                                d.getDescription(GpsDirectory.TAG_TIME_STAMP);
                        Date stamp = gpsTimeFormatter.parse(date);
                        date = localTimeFormatter.format(stamp);
                    }
                }
            }

            if (date == null) {
                d = metadata.getFirstDirectoryOfType(IptcDirectory.class);
                if (d != null) {
                    date = d.getDescription(IptcDirectory.TAG_DATE_CREATED);
                    if (date != null) {
                        date = date + " " +
                                d.getDescription(IptcDirectory.TAG_TIME_CREATED);
                    }
                }
            }

//            if (date == null) {
//                d = metadata.getFirstDirectoryOfType(XmpDirectory.class);
//                if (d != null) {
//                    date = d.getDescription(XmpDirectory.TAG_CREATE_DATE);
//                }
//            }

            return date;
        } catch (Exception e) {
            if (file.getName().startsWith(".")) {
                file.delete();
            }
            System.out.println("error in " + file.getAbsolutePath());
        }
        return null;
    }

    public static String scanExifDate(File file) {
        try {
            System.out.println(file.getAbsolutePath());
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    System.out.println(tag);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean copy(String src, long srcOffset, String dest, boolean append) {
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            input = new FileInputStream(new File(src));
            input.skip(srcOffset);
            output = new FileOutputStream(new File(dest), append);
            byte[] bt = new byte[8192];
            int readBytes;
            while ((readBytes = input.read(bt)) > 0) {
                output.write(bt, 0, readBytes);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSilently(input);
            closeSilently(output);
        }
        return false;
    }

    public static void fillBytesEnd(String src, byte[] data) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(new File(src), true);
            byte[] bt = data;
            output.write(bt, 0, data.length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSilently(output);
        }
    }

    public static byte[] buildLiveTag(long videoTime, long videoLength) {
        byte[] tag = new byte[40];
        long t1 = videoTime / 2 - 760;
        t1 = Math.max(0, t1);
        byte[] len = (t1 + ":1000").getBytes();
        for (int i = 0; i < len.length; i++) {
            tag[i] = len[i];
        }

        len = ("LIVE_" + String.valueOf(videoLength)).getBytes();
        for (int i = 0; i < len.length; i++) {
            tag[i + 20] = len[i];
        }
        return tag;
    }

    public static void sureLivePhotoName(String file) {
        File f = new File(file);
        String name = f.getName();
        if (name.toLowerCase().endsWith(".mp4.jpg")) {
            return;
        }
        int i = name.lastIndexOf(".");
        StringBuilder builder = new StringBuilder();
        if (i > 0) {
            builder.append(name.substring(0, i));
            builder.append(".mp4");
            builder.append(name.substring(i));
        }
        f.renameTo(new File(f.getParentFile(), builder.toString()));
    }

    public static String fileName(File file) {
        String name = file.getName();
        int i = name.lastIndexOf(".");
        if (i > 0) {
            return name.substring(0, i);
        } else {
            return name;
        }
    }

    public static String makeAbsPath(String path) {
        if (path != null) {
            if (!path.startsWith("/") && !path.startsWith(":\\", 1)) {
                String pwd = System.getProperty("user.dir");
                return new File(pwd, path).getAbsolutePath();
            }
        }
        return path;
    }

    public static long getVideoTime(String video) {
        File source = new File(video);
        try {
            MultimediaObject instance = new MultimediaObject(source, new OSXFFMPEGLocator());
            MultimediaInfo result = instance.getInfo();
            return result.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
