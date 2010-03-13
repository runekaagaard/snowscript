<?php
/**
 * PHP to snow converter.
 *
 * @author Rune Kaagaard <rumi.kg@gmail.com>
 * @todo Comment, Cleanup, Make not horrible and make it work with the rest of
 * the tests. Sofar it only works for the tests in the "basic" folder. There is
 * still many gotchas and WTF's. Just commiting this not to loose the files.
 */
define ('SNOW_PATH', realpath(dirname(__FILE__)));
include SNOW_PATH . '/php_to_snow_maps.php';

class PhpToSnow {
    const PARSE_WITH_METHOD = 1;
    const PARSE_UNCHANGED = 2;
    const STATE_NORMAL = 3;
    const STATE_BEGINNING_OF_LINE = 4;

    private $state = self::STATE_BEGINNING_OF_LINE;
    private $insideDoubleQuotes = FALSE;
    private $insideForLoop = FALSE;
    private $injectBeforeDecreasedIndent = FALSE;
    private $output = '';
    private $indent = 0;
    private $tokens;
    private $countTokens = 0;
    private $i = -1;

    public function  __construct($phpCode) {
        $this->tokens = token_get_all($phpCode);
        $this->countTokens = count($this->tokens) - 1;
        $this->addIndent();
        $this->parseCode($phpCode);
        $this->outputResult($phpCode);
    }


    private function parseCode($code) {
        while($token = $this->next()) {
            $this->out($this->parseToken($token));
        }
    }

    private function parseSubtokens($subTokens) {
        $output = '';
        foreach($subTokens as $token) {
            $output .= $this->parseToken($token);
        }
        return $output;
    }

    private function next() {
        $this->i++;
        if (!isset($this->tokens[$this->i])) return FALSE;
        return $this->normalizeToken($this->tokens[$this->i]);
    }

    private function getMatchingStrings($str1, $str2) {
        $subTokens = array();
        $indent = FALSE;
        while($token = $this->next()) {
            if ($indent === 0) return $subTokens;
            $subTokens[] = $token;
            if ($token['value'] === $str1) {
                if ($indent === FALSE) $indent = 1;
                else $indent++;
            }
            if ($token['value'] === $str2) $indent--;
        }
        return $subTokens;
    }

    private function normalizeToken($token) {
        if (!is_array($token)) {
            $token = array(
                'name' => $token,
                'value' => $token,
                'unknown' => FALSE,
                'num' => $this->i,
            );
        } else {
            if (count($token) > 3) 
                $this->chrash('Token out of range', $token);
            $token = array(
                'name' => token_name($token[0]),
                'value' => $token[1],
                'unknown' => $token[2],
                'num' => $this->i
            );
        }
        return $token;
    }

    private function parseToken($token) {
        static $map = NULL;
        if (empty($map)) $map = get_php_to_snow_map();
        if (!isset($map[$token['name']])) {
            $this->chrash('Token not yet implemented', $token);
        }
        $action = $map[$token['name']];
        if (is_array($action)) {
            $token['name'] = $action['name'];
            $action = $action['action'];
        }
        switch ($action) {
            case self::PARSE_UNCHANGED:
                return $token['value'];
            case self::PARSE_WITH_METHOD:
                return $this->$token['name']($token);
            default:
                return $action;
        }
    }

    private function increase_indent() {
        $this->indent++;
    }

    private function decrease_indent() {
        if (!empty($this->injectBeforeDecreasedIndent)) {
            $this->out($this->injectBeforeDecreasedIndent);
            $this->addNewLine();
            $this->injectBeforeDecreasedIndent = FALSE;
        }
        $this->indent--;
    }

    private function lTrimWhiteSpace($tokens) {
        $trimmed_tokens = array();
        $is_in_beginning = TRUE;
        foreach ($tokens as $name => $token) {
            if ($is_in_beginning && $token['name'] == 'T_WHITESPACE') {
            } else {
                $trimmed_tokens[$name] = $token;
                $is_in_beginning = FALSE;
            }
        }
        return $trimmed_tokens;
    }
    
    private function out($s) {
        $this->output .= $s;
        $last = $s;
    }

    private function outputResult() {
        $out = $this->output;
        $out = preg_replace('#%>$#','', $out);
        $out = preg_replace("#\n([¥£]*\n)+#", "\n", $out);
        $out = preg_replace_callback(
            "#([a-zA-Z_]+)[£]*\(([a-z_]+)\)#",
            array($this, 'outputResultCallback'),
            $out
        );
        $out = preg_replace_callback(
            "#([a-zA-Z_]+)[£]*\(('[^']*')\)#",
            array($this, 'outputResultCallback'),
            $out
        );
        $out = preg_replace_callback(
            "#([a-zA-Z_]+)[£]*\((\"[^']*\")\)#",
            array($this, 'outputResultCallback'),
            $out
        );
        $out = preg_replace_callback(
            "#\n([¥]*)[£]+#",
            array($this, 'removePreSpace'),
            $out
        );
        $out = str_replace(array('¥', '£'), ' ', $out);
        $out = trim($out);
        echo "########### AFTER #############\n" . $out . "\n";
    }

    private function outputResultCallback($m) {
        return $m[2] . '->' . $m[1];
    }

    private function removePreSpace($m) {
        return str_replace('£', '', $m[0]);
    }

    private function addNewLine() {
        $this->out("\n");
        $this->state = self::STATE_BEGINNING_OF_LINE;
    }
    private function addIndent() {
        $this->out(str_repeat('¥', $this->indent * 4));
        $this->state = self::STATE_BEGINNING_OF_LINE;
    }
    
    private function chrash($msg, $data) {
        var_dump("\n$this->output");
        echo "$msg\n"; var_dump($data); die;
    }

    private function T_OPEN_TAG($token) {
        static $has_been_opened = FALSE;
        if (!$has_been_opened) {
            $has_been_opened == TRUE;
            return '';
        } else {
            return '<%';
        }
    }

    private function T_VARIABLE($token) {
        $value = str_replace('$', '', $token['value']);
        return $value;
    }

    private function LBRACKET() {
        if ($this->insideDoubleQuotes) return '';
        $this->increase_indent();
        $this->addNewLine();
        $this->addIndent();
    }
    private function RBRACKET() {
        if ($this->insideDoubleQuotes) return '';
        $this->decrease_indent();
        $this->addNewLine();
        $this->addIndent();
    }

    private function SEMICOLON() {
         $this->addNewLine();
         $this->addIndent();
         if ($this->insideForLoop) return ';';
    }

    private function T_FOREACH($token) {
        $subTokens = $this->getMatchingStrings('(', ')');
        $subTokens = $this->lTrimWhiteSpace($subTokens);
        array_shift($subTokens);
        array_pop($subTokens);
        $output = $this->parseSubtokens($subTokens);
        $status = preg_match('#(.*)£as£(.*)£,£(.*)#i', $output, $m);
        if (!empty($status)) {
            $this->out("for $m[2],$m[3] in $m[1]");
            return '';
        }
        $status = preg_match('#(.*)£as£(.*)#', $output, $m);
        if (empty($status)) $this->chrash('Failed foreach parsing', $output);
        $this->out("for $m[2] in $m[1]");
        return '';
    }

    private function T_FOR($token) {
        $this->insideForLoop = TRUE;
        $subTokens = $this->getMatchingStrings('(', ')');
        $subTokens = $this->lTrimWhiteSpace($subTokens);
        array_shift($subTokens);
        array_pop($subTokens);
        $output = $this->parseSubtokens($subTokens);
        $parts = split(';', $output);
        foreach ($parts as &$part) $part = trim($part, '£');
        $this->addIndent();
        $this->out($parts[0]);
        $this->addNewLine();
        $this->addIndent();
        $this->out("while $parts[1]");
        $this->injectBeforeDecreasedIndent = $parts[2];
        $this->insideForLoop = FALSE;
        return '';
    }

    private function T_IF($token) {
        $subTokens = $this->getMatchingStrings('(', ')');
        $subTokens = $this->lTrimWhiteSpace($subTokens);
        array_shift($subTokens);
        array_pop($subTokens);
        $this->out('if '. $this->parseSubtokens($subTokens));
        return '';
    }

    private function T_WHITESPACE($token) {
        static $last_num = -10;
        if ($token['num'] - $last_num == 1) {
            $last_num = $token['num'];
            return '';
        } else {
            $last_num = $token['num'];
            return '£';
        }
    }

    private function DOUBLEQUOTES($token) {
        if ($this->insideDoubleQuotes) {
            $this->insideDoubleQuotes = FALSE;
            return '"';
        }
        if (!$this->insideDoubleQuotes) $this->insideDoubleQuotes = TRUE;
        return '"';
    }
}
$files = glob(SNOW_PATH . '/tests/basic/*');
foreach ($files as $file) {
    $content = file_get_contents($file);
    preg_match('#--FILE--(.*)--EXPECT#Uis', $content, $m);
    echo "########### BEFORE ############\n" . $file . "\n" . trim($m[1]) . "\n";
    new PhpToSnow(trim($m[1])) ;
}