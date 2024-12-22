# Overdrive Libby MP3 Downloader

As of late 2024, Overdrive discontinued their Overdrive Media Console application in favor of the Libby application. This works to play the audiobooks, but doesn't allow you access to the raw, non-DRM MP3 files any more. This means many people who use things like standalone MP3 players can no longer use them to listen to audiobooks from the library. This code allows you to again download the MP3s.  

NOTE: There's no telling how long this will work, but if you don't see a message here saying it no longer works, it should still work.

## Thanks

This builds upon the work that was done by [chbrown](https://github.com/chbrown/overdrive) - my code is just a java rewrite of his code so it's easier to run on other operating systems, like Windows. If you are on linux, I encourage you to use his script - it works great.

## Requirements
* java installed and working - it should be at least version 8. I'm not going to teach you how to do that - google it if you don't know. You should have your system setup so that you can type java -version in a terminal/command prompt and it will work and tell you what version of java you have installed.
* an .odm file from the library (instructions below)
* This compiled code in the form of the .jar file - see the [releases page](https://github.com/brianpipa/OverdriveMP3Downloader/releases) to download it.

## How it works
When you run it, it will create a license file next to the ODM file, it will create a folder next to your ODM file with the same name and it will put the MP3s and the cover image in the new folder. For example, if your ODM file is at C:\Downloads\BookName_987654321098_12345678.odm, when you run it you will then have:
* C:\Downloads\BookName_987654321098_12345678 (folder)
* C:\Downloads\BookName_987654321098_12345678.odm.license
* C:\Downloads\BookName_987654321098_12345678\\*.mp3
* C:\Downloads\BookName_987654321098_12345678\\*.jpg

NOTE: Once you have the .license file, do not delete it until you have all the MP3 files. They only allow you to get the license ONCE. 

You can rerun the code on the ODM file multiple times, but it will skip any files you already have. if you need it to download the file(s) again, delete the MP3(s) or the cover image and it will re-download them.

## Obtain an ODM file
This is a little trickier than it used to be. Here is how I do it... Once you checkout an audiobook from Overdrive/Libby, go to your loans on the Overdrive site for you library. This URL will look like https://YOURLIBRARY.overdrive.com/account/loans - some actual examples are:  
* https://wakegov.overdrive.com/account/loans
* https://hcplc.overdrive.com/account/loans
* https://alexandria.overdrive.com/account/loans

Once there, you will see a button on your audiobook that says "Listen now in browser". Right-click on that button and copy the url, then paste it into a new tab in your browser. The link will look something like this: https://YOURLIBRARY.overdrive.com/media/download/audiobook-overdrive/1234567

Now, in your browser's address bar, edit that URL and change audiobook-overdrive to audiobook-mp3. So for example, if the URL was https://wakegov.overdrive.com/media/download/audiobook-overdrive/1234567 then it should now be https://wakegov.overdrive.com/media/download/audiobook-mp3/1234567

Once you have the URL changed, hit enter to visit that URL. That should cause your browser to download the .odm file which we will need for the next step. The .odm file will be in whatever folder you have your browser configured to use for your downloads.

## Running the code to get the MP3s
I recommend putting the .jar file somewhere that makes sense for you. Maybe C:\OverdriveMP3Downloader or ~/username/apps/overdrivemp3downloader or whatever works for you. Now first to see if this is working, open a command prompt/terminal in the folder that the jar file is in. For this first test to make sure it works, copy your ODM file into this folder. Let's assume for this example, your ODM file is MyAudiobook.odm. So you should have both overdrivemp3.jar and MyAudiobook.odm in the same folder.

Now from the command prompt/terminal you opened, type `java -jar overdrivemp3.jar MyAudiobook.odm`
and that should start the download of the MP3 files and you should see something like this:
![sample](https://github.com/brianpipa/OverdriveMP3Downloader/blob/main/README-images/terminal-run.png?raw=true)

Once you verify this works, you can leave the ODM file wherever you want and just pass it in as a parameter like `java -jar overdrivemp3.jar path\to\MyAudiobook.odm` and you can even try use the [runscripts](https://github.com/brianpipa/OverdriveMP3Downloader/tree/main/runscripts) On Windows, you can drag an ODM file onto the batch file and it will download the MP3s for you. If you are even more clever, you can configure your browser to automatically run this script on any ODM file you download by associating (in the browser) .odm files with the script. 

Once you have the MP3s, you can delete the .odm file. Don't forget to delete the MP3s once they are due back to the library!

