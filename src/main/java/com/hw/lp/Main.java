package com.hw.lp;


import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileInputStream;

public class Main {


    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("c", false, "create live photo using a photo and a video");
        options.addOption("s", false, "batch scan");
        options.addOption("n", false, "rename scan");
        options.addOption("p", true, "photo path/dir");
        options.addOption("v", true, "video path/dir");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("c")) {
                createLivePhoto(Utils.makeAbsPath(cmd.getOptionValue("p")), Utils.makeAbsPath(cmd.getOptionValue("v")));
            } else if (cmd.hasOption("s")) {
                batchScan(Utils.makeAbsPath(cmd.getOptionValue("p")), Utils.makeAbsPath(cmd.getOptionValue("v")));
            } else if (cmd.hasOption("n")) {
                renameScan(new File(Utils.makeAbsPath(cmd.getOptionValue("p"))));
            } else if (cmd.hasOption("h")) {
                printHelp();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("请确保安装了ffmeg: brew install ffmpeg");
        System.out.println("-n : 图片改名扫描\n" +
                "-c : 单次模式\n" +
                "-s : 自动批处理模式\n" +
                "-p : 图片路径\n" +
                "-v : 视频路径");
    }

    private static void renameScan(File file) {
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                renameScan(f);
            } else if (f.getName().toLowerCase().endsWith("jpg")) {
                String tag = Utils.getExifDateTag(f);
                if (tag != null) {
                    String[] td = tag.split(" ");
                    if (td.length >= 2) {
                        td[0] = td[0].replace(":", "");
                        td[1] = td[1].replace(":", "").substring(0, 6);
                        String newName = "IMG_" + td[0] + "_" + td[1];
                        File mov = new File(f.getParentFile(), f.getName().toLowerCase().replace("jpg", "mov"));
                        File target = new File(f.getParentFile(), newName + ".jpg");
                        if (target.getName().equals(f.getName())) {
                            continue;
                        }
                        System.out.println(f.getAbsolutePath() + " -> " + target.getName());
                        f.renameTo(target);
                        if (mov.exists()) {
                            mov.renameTo(new File(mov.getParentFile(), newName + ".mov"));
                        }
                    } else {
                        System.out.println(tag + "unknown date:" + tag);
                    }
                }
            }
        }
    }

    public static void batchScan(String photoDir, String videoDir) {
        for (File file : new File(photoDir).listFiles()) {
            if (file.isDirectory()) {
                batchScan(file.getAbsolutePath(), videoDir);
            } else if (file.getName().toLowerCase().endsWith("jpg")) {
                File vf = new File(file.getParentFile(), Utils.fileName(file) + ".mov");
                if (vf.exists()) {
                    createLivePhoto(file.getAbsolutePath(), vf.getAbsolutePath());
                } else if (videoDir != null) {
                    vf = new File(videoDir, file.getName());
                    if (!vf.getAbsolutePath().equals(file.getAbsolutePath())
                            && vf.exists()) {
                        createLivePhoto(file.getAbsolutePath(), vf.getAbsolutePath());
                    }
                }
            }
        }
    }

    public static void createLivePhoto(String photoPath, String videoPath) {
        if (!new File(photoPath).exists()) {
            System.out.println("photo file not exist");
            return;
        }
        if (!new File(photoPath).isFile()) {
            System.out.println("photo is not a file");
            return;
        }
        if (!new File(videoPath).exists()) {
            System.out.println("video file not exist");
            return;
        }
        if (!new File(videoPath).isFile()) {
            System.out.println("video is not a file");
            return;
        }
        long length = getLivePhotoVideoLength(photoPath);
        if (length > 0) {
            System.out.println("photo is already live photo, ignored");
            return;
        }
        File videoFile = new File(videoPath);
        long fileLength = videoFile.length();
        long vl = getLivePhotoVideoLength(videoFile);
        if (vl <= 0) {
            System.out.println("video only support jpg/mov/mp4 only");
            return;
        }
        long videoOffset = 0;
        boolean videoFileIsLivePhoto = vl < fileLength;
        if (videoFileIsLivePhoto) {
            videoOffset = fileLength - vl - 40;
        }
        if (Utils.copy(videoPath, videoOffset, photoPath, true)) {
            if (!videoFileIsLivePhoto) {
                Utils.fillBytesEnd(photoPath, Utils.buildLiveTag(Utils.getVideoTime(videoPath), vl));
            }
            Utils.sureLivePhotoName(photoPath);
        }
        System.out.println("handled : " + photoPath);
    }

    public static long getLivePhotoVideoLength(File photo) {
        String fileName = photo.getName().toLowerCase();
        if (fileName.endsWith(".jpg")) {
            long len = getLivePhotoVideoLength(photo.getAbsolutePath());
            if (len > 0) {
                return len;
            }
        } else if (fileName.endsWith(".mov") || fileName.endsWith(".mp4")) {
            return photo.length();
        }
        return 0;
    }

    public static long getLivePhotoVideoLength(String photoPath) {
        try {
            FileInputStream in = new FileInputStream(photoPath);
            long fileLength = (long) in.available();
            if (fileLength < 40) {
                Utils.closeSilently(in);
                return -1;
            }

            long skiped = in.skip(fileLength - 20);
            byte[] buffer = new byte[20];
            if (in.read(buffer) != 20) {
                System.out.println("file length:" + fileLength + "skiped " + skiped);
                Utils.closeSilently(in);
                return -1;
            }

            String tag = new String(buffer, "gb2312").trim();
            if (tag.startsWith("LIVE_")) {
                long parseLong = Long.parseLong(tag.split("_")[1]);
                Utils.closeSilently(in);
                return parseLong;
            }
            Utils.closeSilently(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
