/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.buck.ocaml;

import com.facebook.buck.rules.RuleKeyAppendable;
import com.facebook.buck.rules.RuleKeyBuilder;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.Tool;
import com.facebook.buck.shell.ShellStep;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.util.MoreIterables;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A compilation step for .ml and .mli files
 */
public class OCamlMLCompileStep extends ShellStep {

  public static class Args implements RuleKeyAppendable {

    private final Function<Path, Path> absolutifier;
    public final ImmutableMap<String, String> environment;
    public final Tool ocamlCompiler;
    public final ImmutableList<String> cCompiler;
    public final ImmutableList<String> flags;
    public final Path output;
    public final Path input;

    public Args(
        Function<Path, Path> absolutifier,
        ImmutableMap<String, String> environment,
        ImmutableList<String> cCompiler,
        Tool ocamlCompiler,
        Path output,
        Path input,
        ImmutableList<String> flags) {
      this.absolutifier = absolutifier;
      this.environment = environment;
      this.ocamlCompiler = ocamlCompiler;
      this.cCompiler = cCompiler;
      this.flags = flags;
      this.output = output;
      this.input = input;
    }

    @Override
    public RuleKeyBuilder appendToRuleKey(RuleKeyBuilder builder) {
      try {
        return builder.setReflectively("cCompiler", cCompiler)
            .setReflectively("ocamlCompiler", ocamlCompiler)
            .setReflectively("output", output.toString())
            .setPath(absolutifier.apply(input), input)
            .setReflectively("flags", flags);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public ImmutableSet<Path> getAllOutputs() {
      String outputStr = output.toString();
      if (outputStr.endsWith(OCamlCompilables.OCAML_CMX)) {
        return OCamlUtil.getExtensionVariants(
            output,
            OCamlCompilables.OCAML_CMX,
            OCamlCompilables.OCAML_O,
            OCamlCompilables.OCAML_CMI,
            OCamlCompilables.OCAML_CMT,
            OCamlCompilables.OCAML_ANNOT);
      } else if (outputStr.endsWith(OCamlCompilables.OCAML_CMI)) {
        return OCamlUtil.getExtensionVariants(
            output,
            OCamlCompilables.OCAML_CMI,
            OCamlCompilables.OCAML_CMTI);
      } else {
        Preconditions.checkState(outputStr.endsWith(OCamlCompilables.OCAML_CMO));
        return OCamlUtil.getExtensionVariants(
            output,
            OCamlCompilables.OCAML_CMO,
            OCamlCompilables.OCAML_CMI,
            OCamlCompilables.OCAML_CMT,
            OCamlCompilables.OCAML_ANNOT);
      }
    }
  }

  private final Args args;
  private final SourcePathResolver resolver;

  public OCamlMLCompileStep(Path workingDirectory, SourcePathResolver resolver, Args args) {
    super(workingDirectory);
    this.resolver = resolver;
    this.args = args;
  }

  @Override
  public String getShortName() {
    return "OCaml compile";
  }

  @Override
  protected ImmutableList<String> getShellCommandInternal(ExecutionContext context) {
    return ImmutableList.<String>builder()
        .addAll(args.ocamlCompiler.getCommandPrefix(resolver))
        .addAll(OCamlCompilables.DEFAULT_OCAML_FLAGS)
        .add("-cc", args.cCompiler.get(0))
        .addAll(
            MoreIterables.zipAndConcat(
                Iterables.cycle("-ccopt"),
                args.cCompiler.subList(1, args.cCompiler.size())))
        .add("-c")
        .add("-annot")
        .add("-bin-annot")
        .add("-o", args.output.toString())
        .addAll(args.flags)
        .add(args.input.toString())
        .build();
  }

  @Override
  public ImmutableMap<String, String> getEnvironmentVariables(ExecutionContext context) {
    return args.environment;
  }
}
