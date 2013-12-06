<?php

function snowscript_to_php($code, $debug=false, $return=false, $namespace="Anonymous") {
    $lexer = new Snowscript_Lexer($code . "\n");
    if ($debug) debug_lexer($lexer);
    $parser = new PHPParser_Parser;
    $prettyPrinter = new PHPParser_PrettyPrinter_Zend;
    $stmts = $parser->parse($lexer);
    $traverser = new PHPParser_NodeTraverser;
    $scope_traverser = new Snowscript_Visitors_Scope($namespace);
    $traverser->addVisitor($scope_traverser);
    $stmts = $traverser->traverse($stmts);
    $nodeDumper = new PHPParser_NodeDumper;
    if ($debug) {
        $nodeDumper = new PHPParser_NodeDumper;
        echo $nodeDumper->dump($stmts) . "\n";
    }
    $php = $prettyPrinter->prettyPrint($stmts) . "\n";
    if ($return)
        return $php;
    else
        print "<?php\n" . $php;
}

function debug_lexer($lexer) {
    $fmt = str_repeat('%-30s', 4);
    line(sprintf($fmt, "In type", "In value", "Out type", "Out value"));
    line(sprintf($fmt, "-------", "--------", "--------", "---------"));
    foreach ($lexer->debug as $row) {
        line(sprintf($fmt,
                     $row['in_type'], str_replace("\n", '\n', "'" . $row['in_value'] . "'"),
                     $row['out_type'], "'" . $row['out_value'] . "'"));
    }
}