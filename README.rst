About
+++++

Snowscript is a language that compiles to PHP. Its syntax is inspired by 
Python, Lua, Coffescript, Go and Scala and strives to be DRY, clean and 
easy to read as well as write.

Roadmap
+++++++

The current status as of October 3rd, 2012 is that both the lexer and parser
actually works. A lot of Snowscript can be compiled to PHP. But there is still
tons of work until it's usable. Version 0.4 will be the first release and will
be of alpha quality. Come join the fun!

Todo 0.4
========

- Webpage.
- Scoping rules.
- Namespaces.
- Command line compile tools.
- Tolerable error messages.
- Code cleanup.

Done
==== 

- Strict comparison operators.
- Comments.
- Strings.
- Ternary operator.
- Control structures.
- For loops.
- Function style casts.
- Classes part1 + 2.
- Destructuring.
- Parsing of basic syntax.
- Transformations for the non LALR(1) compatible features of Snowscript like
  implicit parenthesis and significant whitespace.
- Lexer.

Todo 0.5
========

- Named parameters.
- List comprehension.
- Inner functions.
- Parser written in Snowscript.
- Existance.

Todo 0.6
========

- Closures.

Todo 0.7
========

- Great error messages.
- Namespaces.

Todo 0.8
========

- Macros.

Quickstart
++++++++++

Stub.

See "USAGE.rst" and "INSTALL.rst" in this folder.

Documentation
+++++++++++++

Whitespace
==========

Snowscript has significant whitespace and the code structure is managed by 
indention, not by curly brackets "{}" or "do/end". Whitespace is not significant 
inside strings and brackets "()[]{}".

The only allowed indention format is 4 spaces.

snowscript::

    fn how_big_is_it(number)
        if number < 100
            <- "small"
        else
            <- "big"

php::

    $how_big_is_it = function($number) {
        if ($number < 100) {
            return "small";
        } else {
            return "big";
        } 
    }

Variables
=========

A variable matches the regular expression ``[a-zA-Z][a-zA-Z0-9_]+``.

snowscript::

    fungus = "Sarcoscypha coccinea"

php::

    $fungus = "Sarcoscypha coccinea";

Declaring a variable in ALL_CAPS marks it as global to the scope it's declared 
in. ALL_CAPS variables declared in the root scope can be accessed from other 
files.

snowscript::

    ONE = "first"
    two = "second"

    fn stuff()
        echo ONE # Echo's "first"
        echo two # E_NOTICE

php::

    global $Namespace__ONE;
    $Namespace__ONE = "first";
    $two = "second";

    $stuff = function() {
        global $Namespace__ONE;
        echo $Namespace__ONE; // Echo's "first"
        echo $two; # E_NOTICE
    }

Comparison
==========

All comparison operators are strong and there are no weak versions. The
supported operators are "==", "!=", "<", ">", "<=" and ">=". If the two
compared values are not of the same type, a ``TypeComparisonError`` will be
thrown. Thats also the case when comparing an int to a float.

snowscript::

    if my_feet > average_feet:
        echo "BIGFOOT"

php::
    
    if (snow_gt($my_feet, $average_feet)) {
        echo "BIGFOOT";
    }

Comments
========

snowscript::

    # Single line.
    ###
    Multiple
    Lines.
    ###

php::

    // Single line.
    /**
     * Multiple
     * Lines.
     */

Strings
=======

There are two kind of strings: """ and ", both multiline.

Whitespace before the current indentation level is stripped. A newline can be
cancelled by ending the previous line with "\\".

Concatenation
-------------

Strings can be concatenated with the "%" operator.

snowscript::

    echo "I am" % " legend!"

php::

    echo 'I am' . ' legend!';

Formatting
----------

There are deliberately no expansion of code or variables inside strings, but 
chaining a string with sprintf does the job.

snowscript::

    "My favorite %s is %d"->sprintf("number", 42)

php::

    sprintf("My favorite %s is %d", "number", 42);

List
====

Lists are defined using square brackets "[]" with each value separated by ",". 
A trailing "," is allowed.

snowscript::

    pianists = ["McCoy Tyner", "Fred Hersch", "Bill Evans"]

php::

    $pianists = array("McCoy Tyner", "Fred Hersch", "Bill Evans");

Values are assigned running integers and can be accessed with "[]".

snowscript::
    
    # Fred Hersch
    echo pianists[1]

php::

    # Fred Hersch
    echo $pianists[1];

Dictionary
----------

Use "{}" to define a dictionary. The key and value of each key/value pair are 
separated by ":".

snowscript::

    series = [
        {
            title: "Heroes",
            genre: "Science Fiction",
            creator: "Tim Kring",
            seasons: 4,
        },
        {
            title: "Game Of Thrones",
            genre: "Medieval fantasy",
            creator: "David Benioff",
            seasons: 2,
        },
    ]

php::

    $series = array(
        "Heroes" => array(
            'genre' => "Science Fiction",
            'creator' => "Tim Kring",
            'seasons' => 4,
        ),
        "Game Of Thrones" => array(
            'genre' => "Medieval fantasy",
            'creator' => "David Benioff",
            'seasons' => 2,
        )),
    );

Accessing dictionaries is done using square brackets "[]".

snowscript::

    echo series[0]['genre']

php::

    echo $series[0]['genre'];

Functions
=========

The "fn" keyword is used to define functions, and "<-" to return a value.

snowscript::

    fn titlefy(fancystring)
        <- fancystring.make_fancy()
    titlefy(so_fancy)

php::

    $titlefy = function($fancystring) {
        return $fancystring->make_fancy();
    }
    $titlefy($so_fancy);
    
Functions are first-class citizens.

Pass by reference and type hinting is not supported. A function is available 
after it's definition, in and below the scope its be defined in.

Optional parameters
-------------------

Functions does not allow to be defined with optional parameters.

Named parameters
----------------

Named parameters uses variable declaration syntax.

snowscript::

    fn render(template, allow_html=true, klingon=false)
        echo template.render(allow_html, klingon)

    render("index.html", klingon=true)

php::

    $render = function($template, $options_) {
        $defaults_ = array(
            'allow_html' => true, 
            'klingon' => false,
        );
        $options_ += $defaults_;
        echo $template->render($options_['allow_html'], $options_['klingon']);
    }

    $render("index.html", array('klingon'=> true));

Chaining
--------

Function calls can be chained using the "->" operator which passes the prior 
expression along as the first argument to the function.

snowscript::

    "peter"->ucfirst()->str_rot13()

php::

    str_rot13(ucfirst("peter"));

Inner functions
---------------

Inner functions comes highly recommended.

snowscript::

    fn wash_car(Car car)
        fn apply_water(car)
            pass
        fn dry(car)
            pass
        <- car->apply_water()->dry()

php::
    
    function wash_car(Car $car) {
        $apply_water = function($car) {

        }
        $dry = function($car) {

        }
        return $dry($apply_water($car));
    }

Closures
--------

Anonymous functions are declared like a normal function without the function 
name and surrounded by "()".

A "+" before the variable name binds a variable from the outer scope.

snowscript::
    
    use_me = get_use_me()
    little_helper = (fn(input, +use_me)
        <- polish(input, use_me))

    little_helper(Lamp())
    
    takes_functions(
        (fn(x)
            y = give_me_a_y(x)
            <- [x * 2, y]
        ),
        (fn(y, c)
            <- y * c
        ),
    )

php::

    $use_me = get_use_me();
    $little_helper = function($input) use ($use_me) {
        return polish($input, $use_me);
    }

    $little_helper(new Lamp);
    
    takes_functions(
        function($x) {
            $y = give_me_a_y($x);
            return array($x * 2, $y);
        },
        function($y, $c) {
            return $y * $c;
        }
    )

As the only structure in Snowscript, closures has a single line mode.

snowscript::

    filter(guys, (fn(guy) <- weight(guy) > 100))

php::

    filter($guys, function() {
        return weight($guy) > 100;
    });

Destructuring
=============

Snowscript has simple destructuring.

snowscript::

    [a, b, c] = [b, c, a]
    [a, b, [c, d]] = letters

php::

    list($a, $b, $c) = array($b, $c, $a);
    list($a, $b, list($c, $d)) = $letters;

Control structures
==================

Two control structures are available: "if" and the ternary operator.

if
--

snowscript::

    if white_walkers.numbers < 500
        fight_valiantly()
    elif feeling_lucky
        improvise()
    else
        run()

php::

    if ($white_walkers->numbers < 500) {
        fight_valiantly();
    } elif ($feeling_lucky) { 
        improvise();
    } else {
        run();
    }

Ternary operator
----------------

Ternary operator is a oneline ``if a then b else c`` syntax.

snowscript::

    echo if height > 199 then "tall" else "small"
    
php::

    echo ($height > 199 ? "tall" : "small");
    
Type casting
============

To cast an expression to a type, use the ``array``, ``bool``, ``float``, 
``int``, ``object`` or ``str`` functions.

snowscript::

    array(a)

php::

    (array) $a;

Loops
=====

For
---

Two kind of for loops are supported. Iterating over a collection, and iterating
over a numeric range. Both key and value are local to the loop.

snowscript::

    for title, data in flowers
        echo [data.id, title]

    for i in 1 to 10 step 2
        echo i
    for i in 10 downto 1
        echo i

php::

    foreach ($flowers as $title => $data) {
        echo array($data->id, $title);
    }
    unset($title, $data);

    for ($i=1; $i <= 10; $i+=2) {
        echo $i;
    }
    unset($i);
    for ($i=10; $i >= 0; --$i) {
        echo $i;
    }
    unset($i);

While
-----

snow::
    
    while frog.ass.is_watertight
        echo "Rinse and repeat."

php::

    while ($frog->ass->is_watertight) {
        echo "Rinse and repeat.";
    }

Array comprehension
===================

Snowscript has array comprehension similiar to that of Python and others.

snowscript::

    [x, y for x in [1,2,3] for y in [3,1,4] if x != y]->var_dump()
    
    fights = [fight(samurai, villain)
              for samurai in seven_samurais
                  if samurai.is_awake()
                    for villain in seven_vaillains
                        if not villain.is_in_jail()]

php::

    $result_ = array();
    foreach (array(1, 2, 3) as $x) {
        foreach (array(3, 1, 4) as $y) {
            if ($x != $y) {
                $result_[$x] = $y;
            }
        }
    }
    unset($x, $y);
    var_dump($result_);

    $fights = array();
    foreach ($seven_samurais as $samurai) {
        if (!$samurai->is_awake()) {
            continue;
        }
        foreach ($seven_villains as $villain) {
            if ($villain->is_in_jail()) {
                continue;
            }
            $fights[] = fight($samurai, $villain);
        }
    }
    unset($samurai, $villain);

Objects
=======

Stub.

An object is a lightweight class, native to snowscript.

snowscript::

    object WebCam(driver, direction=false)
        extends = [Cam, Device]

        fn take_pic(self)
            super
            if .direction
                .driver.rotate(.direction)

            <- .driver.snapshot()

        driver.inititalize()

- Arguments to the object are available as properties.
- ``super`` always passes the same arguments as the method it's being called 
  from.
- Code in the root scope of the object is executed on object instantiation.
- Has multiple inheritance.

The typical PSR-1 application structure where everything is a class in its own 
file is not recommended in Snowscript.

Instead use functions to encapsulate logic and ALL_CAPS variables for global
state. Signs that using an object is appropriate includes:

- You need more than one type of something
- ...

Operators
=========

Stub.

A number of operators has changed from PHP.

================= ============================
PHP               Snow
================= ============================
&&                and
!                 not
||                or
and               N/A
or                N/A
%                 mod
$a  %= $b         N/A
.                 %
$a .= $b          N/A
&                 band
\|                 bor
^                 bxor
<<                bleft
>>                bright
~                 bnot
================= ============================

Namespaces
==========

A namespace is defined by adding an empty file called "__namespace.snow" in the 
folder which should be the root of the namespace. So given a directory structure
as::

    .
    └── starwars
        ├── __namespace.snow
        ├── __import.snow
        ├── battle.snow
        ├── galaxy.snow
        └── settings.snow

the file "battle.snow" would be assigned the namespace "starwars.battle". If no
"__namespace.snow" file is found in the same folder or above, the namespace will 
be that of the filename itself.

Classes, interfaces, traits, functions, constants, variables can belong to a
namespace.

To make a member exportable it must be defined in the root scope of the file.

If any member is prefixed with "_" it is a warning that it should not be 
accessed from outside its file.

Importing
---------

Members from other namespaces are imported by the ``import()`` function that 
must be called before any other statements.

There is no namespace operator, so everything needed must be explicitly 
imported. When using an imported namespace, the type of what follows the 
namespace is inferred. See "Naming conventions".

snowscript::

    import({
        "FancyFramework.Db": {
            classes: ["Retry", "Transaction"],
            objects: ["Model"],
            interfaces: ["Model_Interface"],
            traits: ["DateStampable"],
            fns: ["model_from_array"],
            constants: ["!SUCCES", "!FAILURE"],
            variables: ["db_types"],
            namespaces: ["Fields"],
            below: {
                "Backends": {
                    objects: ["Mongo, Postgres, Datomic"],
                },
            },
        },
        __global: {
            classes: ["SplStack"],
            interfaces: ["Countable"],
            fns: [["mb_strlen", "s_len"], "trim"],
            constants: ["!E_ALL"],
        },
    })

    Retry()
    model_from_array()
    !SUCCES

    fn do_it()
        db_types

    s_len("yo")

    Fields.Integer()

php::

    use FancyFramework\Db\Retry;
    use FancyFramework\Db\Transaction;
    use FancyFramework\Db\Model_Interface;
    use FancyFramework\Db\DateStampable;
    use FancyFramework\Db\SUCCES;
    use FancyFramework\Db\FAILURE;
    use FancyFramework\Db;
    use FancyFramework\Backends\Mongo;
    use FancyFramework\Backends\Postgres;
    use FancyFramework\Backends\Datomic;
    use FancyFramework\Db\Retry\Fields;

    use \SplStack;
    use \Countable;
    use \mb_strlen;
    use \trim;
    use \E_ALL;

    new Retry();
    \FancyFramework\Db\model_from_array();
    \FancyFramework\Db\SUCCES;

    $do_it = function() {
        global $Fancyframework_Db__db_types;
        $Fancyframework_Db__db_types;
    }

    mb_strlen("yo");

    new Fields\Integer();

Global imports
--------------

If a file named "__import.snow" containing an ``import`` definition is found in 
the same folder as "__namespace.snow", it's imports are available for all 
".snow" files in and below that directory.

Scoping rules
=============

- Functions, ALL_CAPS variables, objects and constants are available in all 
  scopes after they are defined.
- Classes and imported members are available throughout the entire file in all 
  scopes.
- Not all_caps variables are limited to after their definition in the scope
  they are defined in.

Naming conventions
==================

Sometimes snowscript needs to guess a type to differentiate between functions 
and classes. The single rule is that functions must start with a lowercase
letter and classes with an uppercase.

Snow Standard Library
=====================

A single php files needs to be included in your project. For now it only holds
functions and exceptions used in the compiled PHP code, but the goal is that 
Snowscript will have a set of builtin functions too.

Include it like:

    require('path/to/snowscript/stdlib/bootstrap.php')

PHP Compatibility Features
==========================

Constants
---------

The use of of constants in snowscript is not recommended. This is because PHP 
constants are limited to scalar values and thus breaks the symmetry when you
all of a sudden need to have a constant that is, say an array. All caps
variables are recommended instead.

A constant has a prefixed "!" and supports assignment. The same goes for class
constants.

snowscript::

    !DB_ENGINE = 'mysql'

php::

    define('DB_ENGINE', 'mysql');

Classes
-------

Objects are used instead of classes. Classes only exists for interoperability
with PHP code.

Declaration
^^^^^^^^^^^

A "." is used to access the class instance and ".." to access the class.
Unlike for functions, type hints are allowed in methods. This is necessary to
be compatible with PHP.

snowscript::
    
    class TabularWriter
        public title
        private filehandle = null
        
        fn __construct(File path, filesystem, title)
            .title = title
            .check_filesystem(filesystem)
            .init_file(path)

        fn check_filesystem(filesystem)
            if not filesystems()[filesystem]?
                throw UnsupportedFilesystemError()
        
        fn init_file(path)
            if not file_exists(path)
                throw FileMissingError()
            else
                .filehandle = open_file(path)

php::

    class TabularWriter {
        public $title;
        private $filehandle;

        public function __construct(File $path, $title) {
            $this->title = $title;
            $this->check_filesystem();
            $this->init_file($path);
        }

        public function check_filesystem() {
            $tmp_ = supported_filesystems();
            if (!isset($tmp_[self::$filesystem])) {
                throw new UnsupportedFilesystemError;
            }
            unset($tmp_);
        }

        public function init_file($path) {
            if (!file_exists($path)) {
                throw new FileMissingError;
            } else {
                $this->filehandle = open_file($path);
            }
        }
    }

A class can inherit a single class, implement multiple interfaces and use
multiple traits.

snowscript::

    abstract class FactoryFactory
        extends AbstractBuilder 
        implements IFactoryFactory, IBuilder
        uses FactoryBehaviour, LoggingBehaviour

        !DEFAULT_FACTORY = "DefaultFactory"

        protected static factories = []
        protected static version = 1.0

        public static fn getInstance(factoryClassName)
            <- ..factories[factoryClassName]

php::

    abstract class FactoryFactory extends AbstractBuilder implements FactoryFactoryInterface, IBuilder {
        use FactoryBehaviour;
        use LoggingBehaviour;

        const DEFAULT_FACTORY = "DefaultFactory";

        protected static $factories = [];
        protected static $version = 1.0;

        public static function getInstance($factoryClassName) {
            return self::factories[$factoryClassName];
        }
            
    }

Usage
^^^^^

Class instantiation uses function notation.

snowscript::

    Bicycle(Rider())

php::

    new Bicycle(new Rider));

Properties and methods on instantiated classes is accessed with the "."
operator. Using ".." accesses static members.

snowscript::

    wind = Wind(52, 12)
    wind.blow()
    Newspaper().read()
    
    Player..register("Ronaldo")
    Player..!MALE
    Player..genders

php::

    $wind = Wind(52, 12);
    $wind->blow();
    (new Newspaper())->read();
    
    Player::register("Ronaldo");
    Player::MALE;
    Player::$genders;

Traits
======

Stub.

Macros
======

Stub.
