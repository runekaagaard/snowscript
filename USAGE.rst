Using Snowscript
++++++++++++++++

Stub.

The following tools exists in the "bin" folder::

    snow-compile /abs/path/to/file.snow
        
        - Outputs the compiled php code to stdout

    snow-watch-mac /abs/path/to/dir1 /abs/path/to/dir2 ...
        
        - Watches for changes to ".snow" files in given directories and
        - recompiles ".php" files with the same name and path.