package com.jayway.jsonpath;

import org.junit.Test;

import static com.jayway.jsonpath.TestUtils.assertHasNoResults;
import static com.jayway.jsonpath.TestUtils.assertHasOneResult;
import static com.jayway.jsonpath.TestUtils.assertHasResults;
import static com.jayway.jsonpath.internal.filter.FilterCompiler.compile;


public class SMRTest extends BaseTest {

    private final Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

    private static final String bJson =
            "[{\"p\":1,\"t\":\"snippet\",\"d\":\"catalog\"},{\"p\":1,\"t\":\"snippet\"}," +
                    "{\"p\":1,\"t\":\"sitelinks\",\"d\":\"sitelinks\"},{\"p\":4,\"t\":\"snippet\",\"d\":\"wikipedia\"}," +
                    "{\"p\":9,\"t\":\"snippet\",\"d\":\"catalog\"}]";


    @Test
    public void complex_cases() {
        assertHasResults("[{\"p\":1,\"t\":\"snippet\",\"d\":\"catalog\"},{\"p\":1,\"t\":\"snippet\"}," +
                "{\"p\":1,\"t\":\"sitelinks\",\"d\":\"sitelinks\"},{\"p\":4,\"t\":\"snippet\",\"d\":\"wikipedia\"}," +
                "{\"p\":9,\"t\":\"snippet\",\"d\":\"catalog\"}]", "$[?(@.d && @.t=='snippet')]", 3, conf);
    }

    @Test
    public void invalid_expressions() {
        assertInvalidPathException("$[?(@.d && @.t=='snippet')");
        assertInvalidPathException("$[?(@.d && @.t=='snippet').d");
        assertInvalidPathException("$[0][?(@.d && @.t=='snippet')");
        assertInvalidPathException("$[?(@.d && @.t=='snippet'");
        assertInvalidPathException("$[?(@.d && @.t=='snippet");
        assertInvalidPathException("$[?(@.d && @.t=='snippet)]");
        assertInvalidPathException("$[?(@.d && @.t=='snippet']");
        assertInvalidPathException("$[?(@.d && @.t=='snippet'].d");
        assertInvalidPathException("$[0][?(@.d && @.t=='snippet']");

        assertInvalidPathException("$[?(.d && @.t=='snippet')]");
        assertInvalidPathException("$[?(@d && @.t=='snippet')]");
        assertInvalidPathException("$[?(d && @.t=='snippet')]");
        assertInvalidPathException("$[?(. && @.t=='snippet')]");

        assertInvalidPathException("$?(@.d && @.t=='snippet')]");
        assertInvalidPathException("$[(@.d && @.t=='snippet')]");
        assertInvalidPathException("$[?@.d && @.t=='snippet')]");
        assertInvalidPathException("$(@.d && @.t=='snippet')]");
        assertInvalidPathException("$[@.d && @.t=='snippet')]");
        assertInvalidPathException("$@.d && @.t=='snippet')]");

        assertInvalidPathException("$[?(@.d && @t=='snippet')]");
        assertInvalidPathException("$[?(@.d & @.t=='snippet')]");

        assertInvalidPathException("$[']");
    }

    @Test
    public void deep_scan() {
        assertHasOneResult("{\"x\": {\"y\": {\"z\": {\"d\":\"catalog\"}}}}", "$..['z']", conf);
        assertHasOneResult("{\"x\": {\"y\": {\"z\": {\"d\":\"catalog\"}}}}", "$..z", conf);

        assertInvalidPathException("$..");
        assertInvalidPathException("$...");

        assertHasNoResults("[{\"x\": {\"y\": {\"z\": {\"d\":\"catalog\"}}}}]", "$..z.x", conf);
        assertHasOneResult("[{\"x\": {\"y\": {\"z\": {\"d\":\"catalog\"}}}}]", "$..y.z", conf);
    }

    @Test
    public void conjunction() {
        assertHasOneResult("[{\"p\":1,\"t\":\"snippet\",\"d\":\"catalog\"}]", "$[?(@.d && @.t=='snippet')]", conf);
        assertHasOneResult("[{\"p\":1,\"t\":\"snippet\",\"d\":\"catalog\"}]", "$[?(@.t==snippet && @.d)]", conf);

        assertHasNoResults("[{\"p\":1,\"t\":\"snippet\",\"d\":\"catalog\"}]", "$[?(@.d && @.t=='sitelinks')]", conf);
        assertHasNoResults("[{\"p\":1,\"t\":\"snippet\",\"d\":\"catalog\"}]", "$[?(@.t=='sitelinks' && @.d)]", conf);
    }

    @Test
    public void multipleInstancesOfValue() {
        assertHasResults("[6, 3, 7, 6, 3, 4, 9, 0]", "$[0, 1, 2, 4]", 4, conf);
        assertHasResults("[6, 3, 7, 6, 3, 4, 9, 0]", "$[0:5]", 5, conf);

        assertInvalidPathException("$[0, 1, 2, 4");   // invalid expr
        assertInvalidPathException("$0, 1, 2, 4]");   // invalid expr
        assertInvalidPathException("$[0:5");  // invalid expr
        assertInvalidPathException("$[0:");   // invalid expr
        assertInvalidPathException("$[*");     // invalid expr

        assertHasResults("[{\"v\":6}, {\"v\":3}, {\"v\":7}, {\"v\":6}, {\"v\":3}, {\"v\":4}, {\"v\":9}, {\"v\":0}]", "$[0, 1, 2, 4]", 4, conf);
    }

    @Test
    public void existsBreaksPredicateIssue() {
        assertHasOneResult(bJson, "$[?(!@.d && @.t=='snippet')]", conf);
    }

    @Test
    public void multiprops() {
        String json = "{\"1\":{\"R_OBJT\":\"music\"},\"2\":{\"dbtype\":\"nailbase\"},\"4\":{\"R_OBJT\":\"celebrities\"},\"5\":{\"R_OBJT\":\"video\"}}";
        assertHasOneResult(json, "$['1','2','3'][?(@.dbtype=='nailbase')]", conf);
        assertHasResults(json, "$['1','2','4', '6'][?(@.R_OBJT)]", 2, conf);

        json = "{\"1\":{\"R_OBJT\":\"music\"},\"4\":{\"R_OBJT\":\"spritze.celebrities\"},\"5\":{\"R_OBJT\":\"video\"}}";
        assertHasNoResults(json, "$['1','2','3'][?(@.dbtype=='nailbase')]", conf);
    }

    @Test
    public void predicates() {
        String json = "{\"1\":{\"R_OBJT\":\"music\"},\"4\":{\"R_\":\"spritze.celebrities\"},\"5\":{\"R_OBJT\":\"video\"}}";
        assertHasResults(json, "$..[?(@.R_OBJT)]", 2, conf);
        assertHasNoResults(json, "$[?(@.R_OBJT)]", conf);
        assertInvalidPathException("$[?(.R_OBJT)]");    // invalid expr
        assertInvalidPathException("$[?(.)]");  // invalid expr
        assertInvalidPathException("$[?(.)");   // invalid expr
        assertInvalidPathException("$[?(.");    // invalid expr
        assertInvalidPathException("$[?(");     // invalid expr
        assertInvalidPathException("$[?");  // invalid expr
        assertInvalidPathException("$[");   // invalid expr

        assertHasResults(json, "$..[?(@.R_OBJT)]", 2, conf);

        json = "{\"1\":{\"R_OBJT\":\"music\",\"p\":\"3\"},\"4\":{\"R_\":{\"R_OBJT\":\"spritze.celebrities\"}},\"5\":{\"R_OBJT\":\"video\"}}";
        assertHasNoResults(json, "$[?(@.R_OBJT)]", conf);
        assertHasResults(json, "$[*][?(@.R_OBJT)]", 2, conf);
        assertHasOneResult(json, "$[*][*][?(@.R_OBJT)]", conf);
        assertHasResults(json, "$..[?(@.R_OBJT)]", 3, conf);

        assertHasOneResult(json, "$..[?(@.R_OBJT=='music' && @.p=='3')]", conf);
        assertHasOneResult("{\"1\":{\"data\":{\"R_OBJT\":\"music\",\"p\":3},\"4\":{\"R_\":{\"R_OBJT\":\"spritze.celebrities\"}},\"5\":{\"R_OBJT\":\"video\"}}}",
                "$['1']['data'][?(@.R_OBJT=='music' && @.p==3)]", conf);
    }

    @Test
    public void returnValue() {
        assertHasOneResult("{\"p\":[1,2,3,5,6]}", "$['p']", conf);
        assertHasOneResult("[\"some_frB33\",\"some_frC\",\"some_frA1\",\"some_frB\",\"some_frA\"]", "$[?(@==some_frB33)]", conf);
        assertHasResults("{\"1\":{\"R_OBJT\":\"music\",\"p\":\"x\"},\"4\":{\"R_\":{\"R_OBJT\":\"music\", \"p\": \"y\"}},\"5\":{\"R_OBJT\":\"video\"}}",
                "$..[?(@.R_OBJT=='music')].p", 2, conf);

        assertHasOneResult("{\"1\":[\"R_OBJT\", \"SOME\"],\"4\":{\"R_OBJT\":\"spritze.celebrities\"},\"5\":{\"R_OBJT\":\"video\"}}",
                "$['1']", conf);
    }

    @Test
    public void compiler_tests() {
        compile("[?(@.name == foo)]");
        compile("[?(@.name == trueism)]");
        assertInvalidPathException("[?(@.name == 5foo)]");
        assertInvalidPathException("[?(@.name == foo*)]");
    }

    @Test
    public void differences_from_upstream() {
        assertHasOneResult("[\"foo\",\"bar2\"]", "$[?(@==bar2)]", conf);
        assertHasOneResult("[\"foo\",\"bar\"]", "$[?(@==foo)]", conf);
        assertHasOneResult("[\"fo_o\",\"bar\"]", "$[?(@==fo_o)]", conf);

        assertHasOneResult("[{\"value\":\"foo\"}]", "$[?(@.value==foo)]", conf);

        assertHasOneResult("[{\"p\":1}]", "$[?(@.p=='1')]", conf);
        assertHasOneResult("[{\"p\":\"1\"}]", "$[?(@.p==1)]", conf);
        assertHasOneResult("[{\"p\":1.1}]", "$[?(@.p=='1.1')]", conf);
        assertHasOneResult("[{\"p\":\"1.1\"}]", "$[?(@.p==1.1)]", conf);


        assertHasOneResult("[{\"p\":1}]", "$[?(@.p < '2')]", conf);
        assertHasOneResult("[{\"p\":1}]", "$[?(@.p <= '2')]", conf);
        assertHasNoResults("[{\"p\":2}]", "$[?(@.p < '1')]", conf);

        assertHasOneResult("[{\"p\":1}]", "$[?(@.p < '2.1')]", conf);
        assertHasOneResult("[{\"p\":1}]", "$[?(@.p <= '2.1')]", conf);
        assertHasNoResults("[{\"p\":2}]", "$[?(@.p < '1.1')]", conf);

        assertHasOneResult("[{\"p\":'1'}]", "$[?(@.p < 2)]", conf);
        assertHasOneResult("[{\"p\":'1'}]", "$[?(@.p <= 2)]", conf);
        assertHasNoResults("[{\"p\":'2'}]", "$[?(@.p < 1)]", conf);

        assertHasOneResult("[{\"p\":'1'}]", "$[?(@.p < 2.1)]", conf);
        assertHasOneResult("[{\"p\":'1'}]", "$[?(@.p <= 2.1)]", conf);
        assertHasNoResults("[{\"p\":'2'}]", "$[?(@.p < 1.1)]", conf);


        assertHasOneResult("[{\"p\":2}]", "$[?(@.p > '1')]", conf);
        assertHasOneResult("[{\"p\":2}]", "$[?(@.p >= '1')]", conf);
        assertHasNoResults("[{\"p\":1}]", "$[?(@.p > '2')]", conf);

        assertHasOneResult("[{\"p\":2}]", "$[?(@.p > '1.1')]", conf);
        assertHasOneResult("[{\"p\":2}]", "$[?(@.p >= '1.1')]", conf);
        assertHasNoResults("[{\"p\":1}]", "$[?(@.p > '2.1')]", conf);

        assertHasOneResult("[{\"p\":'2'}]", "$[?(@.p > 1)]", conf);
        assertHasOneResult("[{\"p\":'2'}]", "$[?(@.p >= 1)]", conf);
        assertHasNoResults("[{\"p\":'1'}]", "$[?(@.p > 2)]", conf);

        assertHasOneResult("[{\"p\":'2'}]", "$[?(@.p > 1.1)]", conf);
        assertHasOneResult("[{\"p\":'2'}]", "$[?(@.p >= 1.1)]", conf);
        assertHasNoResults("[{\"p\":'1'}]", "$[?(@.p > 2.1)]", conf);
    }


    private void assertInvalidPathException(String filter){
        try {
            compile(filter);
            throw new AssertionError("Expected " + filter + " to throw InvalidPathException");
        } catch (InvalidPathException e){
            //e.printStackTrace();
        }
    }
}
