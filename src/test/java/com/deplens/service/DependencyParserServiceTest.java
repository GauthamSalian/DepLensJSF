package com.deplens.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DependencyParserServiceTest {

    private DependencyParserService parser;

    @BeforeEach
    void setUp() {
        parser = new DependencyParserService();
    }

    @Test
    @DisplayName("parsePackageLock: Extracts direct, transitive, and directed parent->child edges from npm lockfile v2/v3")
    void testParsePackageLockV2() {
        String packageLockJson = """
                {
                  "name": "my-app",
                  "version": "1.0.0",
                  "lockfileVersion": 3,
                  "packages": {
                    "": {
                      "name": "my-app",
                      "version": "1.0.0",
                      "dependencies": {
                        "express": "^4.19.2"
                      }
                    },
                    "node_modules/express": {
                      "version": "4.19.2",
                      "dependencies": {
                        "accepts": "~1.3.8",
                        "body-parser": "1.20.2"
                      }
                    },
                    "node_modules/accepts": {
                      "version": "1.3.8",
                      "dependencies": {
                        "mime-types": "~2.1.34"
                      }
                    },
                    "node_modules/body-parser": {
                      "version": "1.20.2"
                    },
                    "node_modules/mime-types": {
                      "version": "2.1.35"
                    }
                  }
                }
                """;

        DependencyParserService.ParsedGraphResult result = parser.parsePackageLock(packageLockJson);

        assertNotNull(result);
        assertEquals(4, result.totalNodes()); // express, accepts, body-parser, mime-types
        assertEquals(1, result.directCount()); // express is direct
        assertEquals(3, result.transitiveCount()); // accepts, body-parser, mime-types are transitive

        // Verify direct vs transitive node classifications
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("express") && n.isDirect()));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("accepts") && !n.isDirect()));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("body-parser") && !n.isDirect()));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("mime-types") && !n.isDirect()));

        // Verify directed edges: root -> express, express -> accepts, express -> body-parser, accepts -> mime-types
        String edgesJson = result.edgesJson();
        assertTrue(edgesJson.contains("root"));
        assertTrue(edgesJson.contains("express@4.19.2"));
        assertTrue(edgesJson.contains("accepts@1.3.8"));
        assertTrue(edgesJson.contains("body-parser@1.20.2"));
        assertTrue(edgesJson.contains("transitive_dependency"));
    }

    @Test
    @DisplayName("parsePackageJson: Parses direct dependencies and devDependencies")
    void testParsePackageJson() {
        String packageJson = """
                {
                  "name": "sample-project",
                  "version": "1.0.0",
                  "dependencies": {
                    "react": "^18.2.0",
                    "react-dom": "^18.2.0"
                  },
                  "devDependencies": {
                    "typescript": "^5.0.0"
                  }
                }
                """;

        DependencyParserService.ParsedGraphResult result = parser.parsePackageJson(packageJson);

        assertNotNull(result);
        assertEquals(3, result.totalNodes());
        assertEquals(3, result.directCount());
        assertEquals(0, result.transitiveCount());
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("react")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("react-dom")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("typescript")));
    }

    @Test
    @DisplayName("parseRequirementsTxt: Parses pip freeze packages and maps dependencies")
    void testParseRequirementsTxt() {
        String pipFreeze = """
                # Generated via pip freeze
                flask==2.3.2
                werkzeug==2.3.6
                jinja2==3.1.2
                click==8.1.3
                markupsafe==2.1.2
                """;

        DependencyParserService.ParsedGraphResult result = parser.parseRequirementsTxt(pipFreeze);

        assertNotNull(result);
        assertEquals(5, result.totalNodes());
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("flask")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("werkzeug")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("jinja2")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("click")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.name().equals("markupsafe")));
    }
}
