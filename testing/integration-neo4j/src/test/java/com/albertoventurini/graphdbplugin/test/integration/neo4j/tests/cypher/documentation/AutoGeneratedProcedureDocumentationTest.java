/**
 * Copied and adapted from plugin
 * <a href="https://github.com/neueda/jetbrains-plugin-graph-database-support">Graph Database Support</a>
 * by Neueda Technologies, Ltd.
 * Modified by Alberto Venturini, 2022
 */
package com.albertoventurini.graphdbplugin.test.integration.neo4j.tests.cypher.documentation;

import com.albertoventurini.graphdbplugin.language.cypher.completion.metadata.CypherMetadataContainer;
import com.albertoventurini.graphdbplugin.test.integration.neo4j.tests.cypher.util.BaseDocumentationTest;


import static org.assertj.core.api.Assertions.assertThat;

public class AutoGeneratedProcedureDocumentationTest extends BaseDocumentationTest {

    private CypherMetadataContainer metadata;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        metadata = services().cypherMetadataProvider().getContainer("documentationTest");
    }

    public void testExistingProcedureDocumentationWithoutDescription() throws Exception {
        metadata.addProcedure("test.procedureName", "(signature)", null);
        configure("CALL test.proce<caret>dureName(42);");
        assertThat(verify())
                .contains("procedure")
                .contains("test.procedureName")
                .contains("Arguments")
                .contains("(signature)")
                .contains("Return")
                .contains("ANY");
    }

    public void testExistingProcedureDocumentationWithDescription() throws Exception {
        metadata.addProcedure("test.procedureName", "(signature)", "My cool description");
        configure("CALL test.proce<caret>dureName(42);");
        assertThat(verify())
                .contains("procedure")
                .contains("test.procedureName")
                .contains("Arguments")
                .contains("(signature)")
                .contains("Return")
                .contains("ANY")
                .contains("My cool description");
    }

    public void testNonExistingProcedureNoDocumentation() throws Exception {
        configure("CALL test.proce<caret>dureName(42);");
        verify(null);
    }
}
