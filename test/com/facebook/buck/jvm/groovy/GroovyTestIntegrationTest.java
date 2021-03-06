/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.groovy;

import static org.junit.Assume.assumeTrue;

import com.facebook.buck.testutil.integration.DebuggableTemporaryFolder;
import com.facebook.buck.testutil.integration.ProjectWorkspace;
import com.facebook.buck.testutil.integration.TestDataHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;


public class GroovyTestIntegrationTest {
  @Rule
  public DebuggableTemporaryFolder tmp = new DebuggableTemporaryFolder();

  private ProjectWorkspace workspace;

  @Before
  public void setUp() throws IOException {
    assumeTrue(System.getenv("GROOVY_HOME") != null);

    workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "groovy_test_description", tmp);
    workspace.setUp();
  }

  @Test
  public void allTestsPassingMakesTheBuildResultASuccess() throws Exception {
    ProjectWorkspace.ProcessResult buildResult =
        workspace.runBuckCommand("test", "//com/example/spock:passing");
    buildResult.assertSuccess("Build should have succeeded.");

    workspace.verify();
  }

  @Test
  public void oneTestFailingMakesTheBuildResultAFailure() throws Exception {
    ProjectWorkspace.ProcessResult buildResult =
        workspace.runBuckCommand("test", "//com/example/spock:failing");
    buildResult.assertTestFailure();

    workspace.verify();
  }

  @Test
  public void compilationFailureMakesTheBuildResultAFailure() throws Exception {
    ProjectWorkspace.ProcessResult buildResult =
        workspace.runBuckCommand("test", "//com/example/spock:will_not_compile");
    buildResult.assertFailure();

    workspace.verify();
  }
}
