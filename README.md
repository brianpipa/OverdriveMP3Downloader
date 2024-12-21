# OverDrive MP3 Downloader

As of late 2024, Overdrive discontinues the use of their Overdrive media Console application in favor of the Libby application. This works to play the audiobooks, but doesn't allow you access to the raw, non-DRM MP3 files any more. This means many people who use things like standalone MP3 players can no longer use them to listen to audiobooks from the library. This code allows you to again download the MP3s.

## Thanks

This builds upon the work that was done by [chbrown](https://github.com/chbrown/overdrive) - my code is just a java rewrite of his code so it's easier to run on other operating systems, like Windows.

## Requirements
* java installed and working - it should be at least version 8. I'm not going to teach you how to do that - google it if you don't know. You should have your system setup so that you can type java -version in a terminal/command prompt and it will work and tell you what version of java you have installed.
* an .odm file from the library (instructions below)
* This compiled code in the form of the .jar file - see the [releases page](https://github.com/brianpipa/OverdriveMP3Downloader/releases) to download it.

## Obtain an ODM file
This is a little trickier than it was before. Here is how I do it... Once I checkout an audiobook from Overdrive/Libby, go to your loans on the Overdrive site for you library. This URL will look like https://YOURLIBRARY.overdrive.com/account/loans - some actual examples are:  
* https://wakegov.overdrive.com/account/loans
* https://hcplc.overdrive.com/account/loans
* https://alexandria.overdrive.com/account/loans

Once there, you will see a button on your audiobook that says "Listen now in browser". RIght click on that button and copy the url, then paste it into a new tab in your browser. The link will look something like this: https://YOURLIBRARY.overdrive.com/media/download/audiobook-overdrive/1234567

Now, in your browser's address bar, edit that URL and change audiobook-overdrive to audiobook-mp3. So for example, if the URL was https://wakegov.overdrive.com/media/download/audiobook-overdrive/5981536 then it should now be https://wakegov.overdrive.com/media/download/audiobook-mp3/5981536

Once you have the URL changed, hit enter to visit that URL. That should cause your browser to download the .odm file which we will need for the next step. The .odm file will be in whatever folder you have your browser configured to use for your downloads.

## Running the code to get the MP3s
