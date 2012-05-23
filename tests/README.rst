Tests
=====

Snowscript is tested using the PHPT testing tool. The full testsuite can be 
run with::

    cd tests
    pear run-tests -r

You can glob to run individual tests::

    pear run-tests stmt/class/*

PHPT generates .out, .diff and other files for failing tests. Read more about 
PHPT at http://qa.php.net/write-test.php.
