# CS4105 Practical P2 - Discover and Download
*10 Oct 2023*

## Description

This is a simple, text-based program (no GUI) to let you browse a local
filespace, based on a logical root directory specified as part of the program's
configuration.

## Start

Copy this whole directory to your own filespace.

## Compile

```
  $ javac *.java
```

## Run

```
  $ java FileTreeBrowser
```

## Files

|Filename                | Information                                      |
|------------------------|--------------------------------------------------|
| 00-readme.md           | This file. |
| ByteReader.java        | Input for strings. |
| Configuration.java     | Load configuration information for program. |
| FileTreeBrowser.java   | Contains `main()` method. Browses filespace. Once the program is running, enter `:help` to see how to use it.|
| filetreebrowser.properties | Contains configuration information for FileTreeBrowser. |
| LogFileWriter.java     | Simple logging class. |
| root_dir/ | The root directory of the filespace that is visible to the FileTreeBrowser. |
| logs/ | The directory into which log files are placed. |