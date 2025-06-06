package org.jabref.logic.bst;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.antlr.v4.runtime.RecognitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * For additional tests see for
 * <ul>
 * <li> purify: {@link org.jabref.logic.bst.util.BstPurifierTest}</li>
 * <li> width: {@link org.jabref.logic.bst.util.BstWidthCalculatorTest}</li>
 * <li> format.name: {@link org.jabref.logic.bst.util.BstNameFormatterTest}</li>
 * <li> change.case: {@link org.jabref.logic.bst.util.BstCaseChangersTest}</li>
 * <li> prefix: {@link org.jabref.logic.bst.util.BstTextPrefixerTest}</li>
 * </ul>
 */
class BstFunctionsTest {
    @Test
    void compareFunctions() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test.compare } {
                    #5  #5      =   % TRUE
                    #1  #2      =   % FALSE
                    #3  #4      <   % TRUE
                    #4  #3      <   % FALSE
                    #4  #4      <   % FALSE
                    #3  #4      >   % FALSE
                    #4  #3      >   % TRUE
                    #4  #4      >   % FALSE
                    "H" "H"     =   % TRUE
                    "H" "Ha"    =   % FALSE
                }
                EXECUTE { test.compare }
                """);

        vm.render(List.of());

        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void arithmeticFunctions() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test } {
                    #1  #1  +   % 2
                    #5  #2  -   % 3
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals(3, vm.getStack().pop());
        assertEquals(2, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void arithmeticFunctionTypeMismatch() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test } {
                    #1  "HELLO" +   % Should throw exception
                }
                EXECUTE { test }
                """);

        assertThrows(BstVMException.class, () -> vm.render(List.of()));
    }

    @Test
    void stringOperations() throws RecognitionException {
        // Test for concat (*) and add.period
        BstVM vm = new BstVM("""
                FUNCTION { test } {
                    "H" "ello"      *           % Hello
                    "Johnny"        add.period$ % Johnny.
                    "Johnny."       add.period$ % Johnny.
                    "Johnny!"       add.period$ % Johnny!
                    "Johnny?"       add.period$ % Johnny?
                    "Johnny} }}}"   add.period$ % Johnny.}
                    "Johnny!}"      add.period$ % Johnny!}
                    "Johnny?}"      add.period$ % Johnny?}
                    "Johnny.}"      add.period$ % Johnny.}
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals("Johnny.}", vm.getStack().pop());
        assertEquals("Johnny?}", vm.getStack().pop());
        assertEquals("Johnny!}", vm.getStack().pop());
        assertEquals("Johnny.}", vm.getStack().pop());
        assertEquals("Johnny?", vm.getStack().pop());
        assertEquals("Johnny!", vm.getStack().pop());
        assertEquals("Johnny.", vm.getStack().pop());
        assertEquals("Johnny.", vm.getStack().pop());
        assertEquals("Hello", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void missing() throws RecognitionException {
        BstVM vm = new BstVM("""
                ENTRY { title } { } { }
                FUNCTION { presort } { cite$ 'sort.key$ := }
                ITERATE { presort }
                READ
                SORT
                FUNCTION { test } { title missing$ cite$ }
                ITERATE { test }
                """);
        List<BibEntry> testEntries = List.of(
                BstVMTest.defaultTestEntry(),
                new BibEntry(StandardEntryType.Article)
                        .withCitationKey("test")
                        .withField(StandardField.AUTHOR, "No title"));

        vm.render(testEntries);

        assertEquals("test", vm.getStack().pop());      // cite
        assertEquals(BstVM.TRUE, vm.getStack().pop());          // missing title
        assertEquals("canh05", vm.getStack().pop());    // cite
        assertEquals(BstVM.FALSE, vm.getStack().pop());         // missing title
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void numNames() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test } {
                    "Johnny Foo { and } Mary Bar" num.names$
                    "Johnny Foo and Mary Bar" num.names$
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals(2, vm.getStack().pop());
        assertEquals(1, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void substring() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test } {
                    "123456789" #2  #1          substring$  % 2
                    "123456789" #4  global.max$ substring$  % 456789
                    "123456789" #1  #9          substring$  % 123456789
                    "123456789" #1  #10         substring$  % 123456789
                    "123456789" #1  #99         substring$  % 123456789
                    "123456789" #-7 #3          substring$  % 123
                    "123456789" #-1 #1          substring$  % 9
                    "123456789" #-1 #3          substring$  % 789
                    "123456789" #-2 #2          substring$  % 78
                } EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals("78", vm.getStack().pop());
        assertEquals("789", vm.getStack().pop());
        assertEquals("9", vm.getStack().pop());
        assertEquals("123", vm.getStack().pop());
        assertEquals("123456789", vm.getStack().pop());
        assertEquals("123456789", vm.getStack().pop());
        assertEquals("123456789", vm.getStack().pop());
        assertEquals("456789", vm.getStack().pop());
        assertEquals("2", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @ParameterizedTest
    @CsvSource({
            "d, abcd, -1, 1",
            "cd, abcd, -1, 2",
            "bcd, abcd, -1, 3",
            "c, abcd, -2, 1",
            "abc, abcd, -2, 100",
            "abc, abcd, -2, 2147483647",
            "b, abcd, -3, 1",
            "a, abcd, -4, 1",
            "'', abcd, -5, 1",                    // invalid number -5
            "'', abcd, -2147483647, 2147483647",  // invalid number
    })
    void substringPlain(String expected, String full, Integer start, Integer length) {
        BstVMContext bstVMContext = new BstVMContext(List.of(), new BibDatabase(), Path.of("404.bst"));
        BstFunctions bstFunctions = new BstFunctions(bstVMContext, new StringBuilder());
        bstVMContext.stack().push(full);
        bstVMContext.stack().push(start);
        bstVMContext.stack().push(length);
        bstFunctions.bstSubstring(null, null);
        assertEquals(expected, bstVMContext.stack().pop());
    }

    @Test
    void empty() throws RecognitionException {
        BstVM vm = new BstVM("""
                ENTRY { title } { } { }
                READ
                STRINGS { s }
                FUNCTION { test } {
                    s empty$          % TRUE
                    "" empty$         % TRUE
                    "   " empty$      % TRUE
                    title empty$      % TRUE
                    " HALLO " empty$  % FALSE
                }
                ITERATE { test }
                """);
        List<BibEntry> testEntry = List.of(new BibEntry(StandardEntryType.Article));

        vm.render(testEntry);

        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void formatNameStatic() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { format }{ "Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin" #1 "{vv~}{ll}{, jj}{, f}?" format.name$ }
                EXECUTE { format }
                """);
        List<BibEntry> v = List.of();

        vm.render(v);

        assertEquals("de~la Vall{\\'e}e~Poussin, C.~L. X.~J?", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void formatNameInEntries() throws RecognitionException {
        BstVM vm = new BstVM("""
                ENTRY { author } { } { }
                FUNCTION { presort } { cite$ 'sort.key$ := }
                ITERATE { presort }
                READ
                SORT
                FUNCTION { format }{ author #2 "{vv~}{ll}{, jj}{, f}?" format.name$ }
                ITERATE { format }
                """);
        List<BibEntry> testEntries = List.of(
                BstVMTest.defaultTestEntry(),
                new BibEntry(StandardEntryType.Book)
                        .withCitationKey("test")
                        .withField(StandardField.AUTHOR, "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin"));

        vm.render(testEntries);

        assertEquals("de~la Vall{\\'e}e~Poussin, C.~L. X.~J?", vm.getStack().pop());
        assertEquals("Annabi, H?", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void changeCase() throws RecognitionException {
        BstVM vm = new BstVM("""
                STRINGS { title }
                READ
                FUNCTION { format.title } {
                    duplicate$ empty$
                        { pop$ "" }
                        { "t" change.case$ }
                    if$
                }
                FUNCTION { test } {
                    "hello world" "u" change.case$ format.title
                    "Hello World" format.title
                    "" format.title
                    "{A}{D}/{C}ycle: {I}{B}{M}'s {F}ramework for {A}pplication {D}evelopment and {C}ase" "u" change.case$ format.title
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals("{A}{D}/{C}ycle: {I}{B}{M}'s {F}ramework for {A}pplication {D}evelopment and {C}ase",
                vm.getStack().pop());
        assertEquals("", vm.getStack().pop());
        assertEquals("Hello world", vm.getStack().pop());
        assertEquals("Hello world", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void textLength() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test } {
                    "hello world" text.length$                                  % 11
                    "Hello {W}orld" text.length$                                % 11
                    "" text.length$                                             % 0
                    "{A}{D}/{Cycle}" text.length$                               % 8
                    "{\\This is one character}" text.length$                    % 1
                    "{\\This {is} {one} {c{h}}aracter as well}" text.length$    % 1
                    "{\\And this too" text.length$                              % 1
                    "These are {\\11}" text.length$                             % 11
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals(11, vm.getStack().pop());
        assertEquals(1, vm.getStack().pop());
        assertEquals(1, vm.getStack().pop());
        assertEquals(1, vm.getStack().pop());
        assertEquals(8, vm.getStack().pop());
        assertEquals(0, vm.getStack().pop());
        assertEquals(11, vm.getStack().pop());
        assertEquals(11, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void intToStr() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test } { #3 int.to.str$ #9999 int.to.str$ }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals("9999", vm.getStack().pop());
        assertEquals("3", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void chrToInt() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test } { "H" chr.to.int$ }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals(72, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void chrToIntIntToChr() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { test } { "H" chr.to.int$ int.to.chr$ }
                EXECUTE {test}
                """);

        vm.render(List.of());

        assertEquals("H", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void type() throws RecognitionException {
        BstVM vm = new BstVM("""
                ENTRY  { } { } { }
                FUNCTION { presort } { cite$ 'sort.key$ := }
                ITERATE { presort }
                SORT
                FUNCTION { test } { type$ }
                ITERATE { test }
                """);
        List<BibEntry> testEntries = List.of(
                new BibEntry(StandardEntryType.Article).withCitationKey("a"),
                new BibEntry(StandardEntryType.Book).withCitationKey("b"),
                new BibEntry(StandardEntryType.Misc).withCitationKey("c"),
                new BibEntry(StandardEntryType.InProceedings).withCitationKey("d"));

        vm.render(testEntries);

        assertEquals("inproceedings", vm.getStack().pop());
        assertEquals("misc", vm.getStack().pop());
        assertEquals("book", vm.getStack().pop());
        assertEquals("article", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void callType() throws RecognitionException {
        BstVM vm = new BstVM("""
                ENTRY { title } { } { }
                FUNCTION { presort } { cite$ 'sort.key$ := }
                ITERATE { presort }
                READ
                SORT
                FUNCTION { inproceedings }{ "InProceedings called on " title * }
                FUNCTION { book }{ "Book called on " title * }
                ITERATE { call.type$ }
                """);
        List<BibEntry> testEntries = List.of(
                BstVMTest.defaultTestEntry(),
                new BibEntry(StandardEntryType.Book)
                        .withCitationKey("test")
                        .withField(StandardField.TITLE, "Test"));

        vm.render(testEntries);

        assertEquals("Book called on Test", vm.getStack().pop());
        assertEquals(
                "InProceedings called on Effective work practices for floss development: A model and propositions",
                vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void swap() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { a } { #3 "Hallo" swap$ }
                EXECUTE { a }
                """);

        List<BibEntry> v = List.of();
        vm.render(v);

        assertEquals(3, vm.getStack().pop());
        assertEquals("Hallo", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void assignFunction() {
        BstVM vm = new BstVM("""
                INTEGERS { test.var }
                FUNCTION { test.func } { #1 'test.var := }
                EXECUTE { test.func }
                """);

        vm.render(List.of());

        Map<String, BstFunctions.BstFunction> functions = vm.latestContext.functions();
        assertTrue(functions.containsKey("test.func"));
        assertNotNull(functions.get("test.func"));
        assertEquals(1, vm.latestContext.integers().get("test.var"));
    }

    @Test
    void simpleIf() {
        BstVM vm = new BstVM("""
                FUNCTION { path1 } { #1 }
                FUNCTION { path0 } { #0 }
                FUNCTION { test } {
                    #1 path1 path0 if$
                    #0 path1 path0 if$
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals(0, vm.getStack().pop());
        assertEquals(1, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void simpleWhile() {
        BstVM vm = new BstVM("""
                INTEGERS { i }
                FUNCTION { test } {
                    #3 'i :=
                    { i }
                    {
                        i
                        i #1 -
                        'i :=
                    }
                    while$
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals(1, vm.getStack().pop());
        assertEquals(2, vm.getStack().pop());
        assertEquals(3, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void nestedControlFunctions() throws RecognitionException {
        BstVM vm = new BstVM("""
                STRINGS { t }
                FUNCTION { not } { { #0 } { #1 } if$ }
                FUNCTION { n.dashify } {
                    "HELLO-WORLD" 't :=
                    ""
                    { t empty$ not }                                    % while
                    {
                        t #1 #1 substring$ "-" =                        % if
                        {
                            t #1 #2 substring$ "--" = not               % if
                            {
                                "--" *
                                t #2 global.max$ substring$ 't :=
                            }
                            {
                                { t #1 #1 substring$ "-" = }            % while
                                {
                                    "-" *
                                    t #2 global.max$ substring$ 't :=
                                }
                                while$
                            }
                           if$
                        }
                        {
                            t #1 #1 substring$ *
                            t #2 global.max$ substring$ 't :=
                        }
                        if$
                    }
                    while$
                }
                EXECUTE { n.dashify }
                """);
        List<BibEntry> v = List.of();

        vm.render(v);

        assertEquals(1, vm.getStack().size());
        assertEquals("HELLO--WORLD", vm.getStack().pop());
    }

    @Test
    void logic() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { not } { { #0 } { #1 } if$ }
                FUNCTION { and } { 'skip$ { pop$ #0 } if$ }
                FUNCTION { or } { { pop$ #1 } 'skip$ if$ }
                FUNCTION { test } {
                    #1 #1 and
                    #0 #1 and
                    #1 #0 and
                    #0 #0 and
                    #0 not
                    #1 not
                    #1 #1 or
                    #0 #1 or
                    #1 #0 or
                    #0 #0 or
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.FALSE, vm.getStack().pop());
        assertEquals(BstVM.TRUE, vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    /**
     * See also {@link org.jabref.logic.bst.util.BstWidthCalculatorTest}
     */
    @Test
    void width() throws RecognitionException {
        BstVM vm = new BstVM("""
                ENTRY { address author title type } { } { label }
                STRINGS { longest.label }
                INTEGERS { number.label longest.label.width }
                FUNCTION { initialize.longest.label } {
                    "" 'longest.label :=
                    #1 'number.label :=
                    #0 'longest.label.width :=
                }
                FUNCTION {longest.label.pass} {
                    number.label int.to.str$ 'label :=
                    number.label #1 + 'number.label :=
                    label width$ longest.label.width >
                        {
                            label 'longest.label :=
                            label width$ 'longest.label.width :=
                        }
                        'skip$
                    if$
                }
                EXECUTE { initialize.longest.label }
                ITERATE { longest.label.pass }
                FUNCTION { begin.bib } {
                    preamble$ empty$
                        'skip$
                        { preamble$ write$ newline$ }
                    if$
                    "\\begin{thebibliography}{"  longest.label  * "}" *
                }
                EXECUTE {begin.bib}
                """);

        List<BibEntry> testEntries = List.of(BstVMTest.defaultTestEntry());

        vm.render(testEntries);

        assertTrue(vm.latestContext.integers().containsKey("longest.label.width"));
        assertEquals("\\begin{thebibliography}{1}", vm.getStack().pop());
    }

    @Test
    void duplicateEmptyPopSwapIf() throws RecognitionException {
        BstVM vm = new BstVM("""
                FUNCTION { emphasize } {
                    duplicate$ empty$
                        { pop$ "" }
                        { "{\\em " swap$ * "}" * }
                    if$
                }
                FUNCTION { test } {
                    "" emphasize
                    "Hello" emphasize
                }
                EXECUTE { test }
                """);

        vm.render(List.of());

        assertEquals("{\\em Hello}", vm.getStack().pop());
        assertEquals("", vm.getStack().pop());
        assertEquals(0, vm.getStack().size());
    }

    @Test
    void preambleWriteNewlineQuote() {
        BstVM vm = new BstVM("""
                FUNCTION { test } {
                    preamble$
                    write$
                    newline$
                    "hello"
                    write$
                    quote$ "quoted" * quote$ *
                    write$
                }
                EXECUTE { test }
                """);

        BibDatabase testDatabase = new BibDatabase();
        testDatabase.setPreamble("A Preamble");

        String result = vm.render(List.of(), testDatabase);

        assertEquals("A Preamble\nhello\"quoted\"", result);
    }
}
