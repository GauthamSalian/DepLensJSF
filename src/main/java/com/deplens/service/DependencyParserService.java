package com.deplens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DependencyParserService {

    private static final Logger log = LoggerFactory.getLogger(DependencyParserService.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Map<String, Set<String>> pypiCache = new ConcurrentHashMap<>();

    public DependencyParserService() {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    /**
     * Parses npm package-lock.json (supports lockfileVersion 1, 2, and 3)
     * Extracts direct dependencies, sub-dependencies, and directed edges parent -> child.
     */
    public ParsedGraphResult parsePackageLock(String jsonContent) {
        List<ParsedNode> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Map<String, String> packageVersions = new HashMap<>();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);

            // 1. Support npm lockfile v2 and v3 (packages map)
            if (root.has("packages")) {
                JsonNode packagesNode = root.get("packages");

                // Find top-level direct dependencies from root package ""
                Set<String> directNames = new HashSet<>();
                if (packagesNode.has("")) {
                    JsonNode rootPkg = packagesNode.get("");
                    if (rootPkg.has("dependencies")) {
                        rootPkg.get("dependencies").fieldNames().forEachRemaining(directNames::add);
                    }
                }

                // Index all packages and their resolved versions
                packagesNode.fields().forEachRemaining(entry -> {
                    String path = entry.getKey();
                    if (path.isEmpty()) return;

                    // e.g. "node_modules/express" -> "express"
                    String pkgName = path.startsWith("node_modules/") ? path.substring("node_modules/".length()) : path;
                    JsonNode pkgDetails = entry.getValue();
                    String version = pkgDetails.has("version") ? pkgDetails.get("version").asText() : "latest";
                    packageVersions.put(pkgName, version);
                });

                // Build nodes and directed edges
                packagesNode.fields().forEachRemaining(entry -> {
                    String path = entry.getKey();
                    if (path.isEmpty()) return;

                    String pkgName = path.startsWith("node_modules/") ? path.substring("node_modules/".length()) : path;
                    JsonNode pkgDetails = entry.getValue();
                    String version = pkgDetails.has("version") ? pkgDetails.get("version").asText() : "latest";

                    boolean isDirect = directNames.contains(pkgName);
                    int depth = isDirect ? 1 : 2;

                    nodes.add(new ParsedNode(pkgName, version, isDirect, "npm", depth));

                    // Connect root to direct dependencies
                    if (isDirect) {
                        edges.add(Map.of(
                                "source", "root",
                                "target", pkgName + "@" + version,
                                "relation", "direct_dependency",
                                "depth", 1
                        ));
                    }

                    // Connect parent dependency to child sub-dependencies
                    if (pkgDetails.has("dependencies")) {
                        pkgDetails.get("dependencies").fieldNames().forEachRemaining(subDep -> {
                            String subVer = packageVersions.getOrDefault(subDep, "resolved");
                            edges.add(Map.of(
                                    "source", pkgName + "@" + version,
                                    "target", subDep + "@" + subVer,
                                    "relation", "transitive_dependency",
                                    "depth", depth + 1
                            ));
                        });
                    }
                });
            }
            // 2. Support npm lockfile v1 (dependencies tree)
            else if (root.has("dependencies")) {
                JsonNode depsNode = root.get("dependencies");
                depsNode.fields().forEachRemaining(entry -> {
                    String name = entry.getKey();
                    JsonNode details = entry.getValue();
                    String ver = details.has("version") ? details.get("version").asText() : "latest";

                    nodes.add(new ParsedNode(name, ver, true, "npm", 1));
                    edges.add(Map.of("source", "root", "target", name + "@" + ver, "relation", "direct_dependency", "depth", 1));

                    if (details.has("requires")) {
                        details.get("requires").fieldNames().forEachRemaining(req -> {
                            nodes.add(new ParsedNode(req, "resolved", false, "npm", 2));
                            edges.add(Map.of(
                                    "source", name + "@" + ver,
                                    "target", req + "@resolved",
                                    "relation", "transitive_dependency",
                                    "depth", 2
                            ));
                        });
                    }
                });
            }
        } catch (Exception ex) {
            log.warn("Failed to parse package-lock.json: {}", ex.getMessage());
        }

        return finalizeGraphResult(nodes, edges);
    }

    /**
     * Parses npm package.json (top-level direct dependencies and devDependencies)
     * Used when package-lock.json is not present in the repository.
     */
    public ParsedGraphResult parsePackageJson(String jsonContent) {
        List<ParsedNode> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonContent);

            if (root.has("dependencies")) {
                root.get("dependencies").fields().forEachRemaining(entry -> {
                    String name = entry.getKey();
                    String ver = entry.getValue().asText();
                    nodes.add(new ParsedNode(name, ver, true, "npm", 1));
                    edges.add(Map.of(
                            "source", "root",
                            "target", name + "@" + ver,
                            "relation", "direct_dependency",
                            "depth", 1
                    ));
                });
            }

            if (root.has("devDependencies")) {
                root.get("devDependencies").fields().forEachRemaining(entry -> {
                    String name = entry.getKey();
                    String ver = entry.getValue().asText();
                    nodes.add(new ParsedNode(name, ver, true, "npm", 1));
                    edges.add(Map.of(
                            "source", "root",
                            "target", name + "@" + ver,
                            "relation", "direct_dependency",
                            "depth", 1
                    ));
                });
            }
        } catch (Exception ex) {
            log.warn("Failed to parse package.json: {}", ex.getMessage());
        }

        return finalizeGraphResult(nodes, edges);
    }

    /**
     * Parses Python requirements.txt (including pip freeze format)
     * For each package, queries PyPI to inspect requires_dist and link sub-dependencies.
     */
    public ParsedGraphResult parseRequirementsTxt(String content) {
        List<ParsedNode> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Map<String, String> installedPackages = new LinkedHashMap<>();

        // 1. Parse flat requirements lines (e.g. "fastapi==0.104.1" or "pydantic>=2.5.0")
        String[] lines = content.split("\\r?\\n");
        Pattern pkgPattern = Pattern.compile("^([a-zA-Z0-9_.-]+)\\s*(?:==|>=|<=|~=|>|<)?\\s*([a-zA-Z0-9_.-]+)?");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("-")) continue;

            Matcher matcher = pkgPattern.matcher(line);
            if (matcher.find()) {
                String pkgName = matcher.group(1).toLowerCase();
                String version = (matcher.groupCount() >= 2 && matcher.group(2) != null) ? matcher.group(2) : "latest";
                installedPackages.put(pkgName, version);
            }
        }

        if (installedPackages.isEmpty()) {
            return new ParsedGraphResult(List.of(), "[]", "[]", 0, 0, 0, 0, 1);
        }

        // 2. Query PyPI for sub-dependencies (requires_dist) to construct parent -> child edges
        Map<String, Set<String>> dependencyTree = new HashMap<>();
        Set<String> subDependencyNames = new HashSet<>();

        for (Map.Entry<String, String> entry : installedPackages.entrySet()) {
            String pkg = entry.getKey();
            String ver = entry.getValue();

            Set<String> subDeps = fetchPyPiSubDependencies(pkg, ver);
            dependencyTree.put(pkg, subDeps);

            for (String sub : subDeps) {
                if (installedPackages.containsKey(sub)) {
                    subDependencyNames.add(sub);
                }
            }
        }

        // 3. Classify: packages with 0 incoming dependencies are Direct dependencies.
        // Packages required by another package in the list are Sub-dependencies!
        for (Map.Entry<String, String> entry : installedPackages.entrySet()) {
            String pkg = entry.getKey();
            String ver = entry.getValue();

            boolean isDirect = !subDependencyNames.contains(pkg);
            int depth = isDirect ? 1 : 2;

            nodes.add(new ParsedNode(pkg, ver, isDirect, "pip", depth));

            if (isDirect) {
                edges.add(Map.of(
                        "source", "root",
                        "target", pkg + "@" + ver,
                        "relation", "direct_dependency",
                        "depth", 1
                ));
            }
        }

        // 4. Construct directed edges between dependencies and their sub-dependencies
        for (Map.Entry<String, Set<String>> entry : dependencyTree.entrySet()) {
            String parent = entry.getKey();
            String parentVer = installedPackages.getOrDefault(parent, "latest");

            for (String child : entry.getValue()) {
                if (installedPackages.containsKey(child)) {
                    String childVer = installedPackages.get(child);
                    edges.add(Map.of(
                            "source", parent + "@" + parentVer,
                            "target", child + "@" + childVer,
                            "relation", "transitive_dependency",
                            "depth", 2
                    ));
                }
            }
        }

        return finalizeGraphResult(nodes, edges);
    }

    /**
     * Parses Maven pom.xml dependency blocks
     */
    public ParsedGraphResult parsePomXml(String xmlContent) {
        List<ParsedNode> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        Pattern depPattern = Pattern.compile("<dependency>[\\s\\S]*?<artifactId>([a-zA-Z0-9_.-]+)</artifactId>[\\s\\S]*?</dependency>");
        Matcher matcher = depPattern.matcher(xmlContent);

        while (matcher.find()) {
            String block = matcher.group(0);
            String artifactId = matcher.group(1);

            String version = "latest";
            Pattern verPattern = Pattern.compile("<version>([a-zA-Z0-9_.-]+)</version>");
            Matcher verMatcher = verPattern.matcher(block);
            if (verMatcher.find()) {
                version = verMatcher.group(1);
            }

            nodes.add(new ParsedNode(artifactId, version, true, "maven", 1));
            edges.add(Map.of(
                    "source", "root",
                    "target", artifactId + "@" + version,
                    "relation", "direct_dependency",
                    "depth", 1
            ));
        }

        return finalizeGraphResult(nodes, edges);
    }

    /**
     * Queries PyPI JSON API to find real sub-dependencies (requires_dist)
     */
    private Set<String> fetchPyPiSubDependencies(String packageName, String version) {
        String cacheKey = packageName + "@" + version;
        if (pypiCache.containsKey(cacheKey)) {
            return pypiCache.get(cacheKey);
        }

        Set<String> subDeps = new HashSet<>();
        try {
            String url = "https://pypi.org/pypi/" + packageName + "/json";
            if (!"latest".equalsIgnoreCase(version)) {
                url = "https://pypi.org/pypi/" + packageName + "/" + version + "/json";
            }

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null && response.has("info") && response.get("info").has("requires_dist")) {
                JsonNode reqDist = response.get("info").get("requires_dist");
                if (reqDist.isArray()) {
                    for (JsonNode item : reqDist) {
                        String spec = item.asText();
                        // Filter out optional extras like `extra == 'all'`
                        if (!spec.contains("extra ==")) {
                            String cleaned = cleanPyPiDepName(spec);
                            if (!cleaned.isEmpty()) {
                                subDeps.add(cleaned.toLowerCase());
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("PyPI lookup skipped for {}: {}", packageName, ex.getMessage());
        }

        pypiCache.put(cacheKey, subDeps);
        return subDeps;
    }

    private String cleanPyPiDepName(String spec) {
        // e.g. "starlette<0.28.0,>=0.27.0" -> "starlette"
        String[] parts = spec.split("[<>=!~;\\s\\[]");
        return (parts.length > 0) ? parts[0].trim() : "";
    }

    private ParsedGraphResult finalizeGraphResult(List<ParsedNode> nodes, List<Map<String, Object>> edges) {
        // De-duplicate nodes by name
        Map<String, ParsedNode> uniqueNodes = new LinkedHashMap<>();
        for (ParsedNode node : nodes) {
            uniqueNodes.putIfAbsent(node.name(), node);
        }

        List<ParsedNode> finalNodes = new ArrayList<>(uniqueNodes.values());
        int directCount = (int) finalNodes.stream().filter(ParsedNode::isDirect).count();
        int transitiveCount = finalNodes.size() - directCount;
        int maxDepth = finalNodes.stream().mapToInt(ParsedNode::depth).max().orElse(1);

        String nodesJson = "[]";
        String edgesJson = "[]";
        try {
            nodesJson = objectMapper.writeValueAsString(finalNodes);
            edgesJson = objectMapper.writeValueAsString(edges);
        } catch (Exception ignored) {}

        return new ParsedGraphResult(
                finalNodes,
                nodesJson,
                edgesJson,
                finalNodes.size(),
                edges.size(),
                directCount,
                transitiveCount,
                maxDepth
        );
    }

    public record ParsedNode(String name, String version, boolean isDirect, String ecosystem, int depth) {}

    public record ParsedGraphResult(List<ParsedNode> nodes, String nodesJson, String edgesJson,
                                   int totalNodes, int totalEdges, int directCount, int transitiveCount, int maxDepth) {}
}
