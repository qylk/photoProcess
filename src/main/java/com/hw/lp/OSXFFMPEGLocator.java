package com.hw.lp;


import ws.schild.jave.FFMPEGLocator;

public class OSXFFMPEGLocator extends FFMPEGLocator {

    @Override
    protected String getFFMPEGExecutablePath() {
        return "/usr/local/bin/ffmpeg";
    }

}