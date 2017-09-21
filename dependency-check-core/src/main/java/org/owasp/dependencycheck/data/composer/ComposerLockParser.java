/*
 * This file is part of dependency-check-core.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2015 The OWASP Foundation. All Rights Reserved.
 */
package org.owasp.dependencycheck.data.composer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Composer.lock file from an input stream. In a separate class so it can hopefully be injected.
 *
 * @author colezlaw
 */
public class ComposerLockParser {

    /**
     * The JsonReader for parsing JSON
     */
    private final JsonReader jsonReader;

    /**
     * The List of ComposerDependencies found
     */
    private final List<ComposerDependency> composerDependencies;

    /**
     * The LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ComposerLockParser.class);

    /**
     * Creates a ComposerLockParser from a JsonReader and an InputStream.
     *
     * @param inputStream the InputStream to parse
     */
    public ComposerLockParser(InputStream inputStream) {
        LOGGER.debug("Creating a ComposerLockParser");
        this.jsonReader = Json.createReader(inputStream);
        this.composerDependencies = new ArrayList<>();
    }

    /**
     * Process the input stream to create the list of dependencies.
     */
    public void process() {
        LOGGER.debug("Beginning Composer lock processing");
        try {
            final JsonObject composer = jsonReader.readObject();
            if (composer.containsKey("packages")) {
                LOGGER.debug("Found packages");
                final JsonArray packages = composer.getJsonArray("packages");
                for (JsonObject pkg : packages.getValuesAs(JsonObject.class)) {
                    if (pkg.containsKey("name")) {
                        final String groupName = pkg.getString("name");
                        if (groupName.indexOf('/') >= 0 && groupName.indexOf('/') <= groupName.length() - 1) {
                            if (pkg.containsKey("version")) {
                                final String group = groupName.substring(0, groupName.indexOf('/'));
                                final String project = groupName.substring(groupName.indexOf('/') + 1);
                                String version = pkg.getString("version");
                                // Some version numbers begin with v - which doesn't end up matching CPE's
                                if (version.startsWith("v")) {
                                    version = version.substring(1);
                                }
                                LOGGER.debug("Got package {}/{}/{}", group, project, version);
                                composerDependencies.add(new ComposerDependency(group, project, version));
                            } else {
                                LOGGER.debug("Group/package {} does not have a version", groupName);
                            }
                        } else {
                            LOGGER.debug("Got a dependency with no name");
                        }
                    }
                }
            }
        } catch (JsonParsingException jsonpe) {
            throw new ComposerException("Error parsing stream", jsonpe);
        } catch (JsonException jsone) {
            throw new ComposerException("Error reading stream", jsone);
        } catch (IllegalStateException ise) {
            throw new ComposerException("Illegal state in composer stream", ise);
        } catch (ClassCastException cce) {
            throw new ComposerException("Not exactly composer lock", cce);
        }
    }

    /**
     * Gets the list of dependencies.
     *
     * @return the list of dependencies
     */
    public List<ComposerDependency> getDependencies() {
        return composerDependencies;
    }
}
