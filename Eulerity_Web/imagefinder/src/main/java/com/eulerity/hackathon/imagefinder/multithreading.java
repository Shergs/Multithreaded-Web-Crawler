package com.eulerity.hackathon.imagefinder;

public class multithreading implements Runnable {
    private String threadUrl;
    private String domainName;

    // @Override
    public multithreading(String urls, String domain) {
        this.threadUrl = urls;
        this.domainName = domain;
    }

    @Override
    public void run() {
        ImageFinder image = new ImageFinder(this.threadUrl, this.domainName);
        // just calling crwal from here instead of the constructor by saving a class
        // variable URL, would be better
    }

}
