---
id: B-47
title: "Publishing runs in CI, not on one laptop"
status: done
priority: infra
size: XS
stage: stage-2-release
---

# B-47 — Publishing runs in CI, not on one laptop

[B-46](B-46-the-first-release.md) shipped `0.1.0` and named this as the thing it did not cover:
the credentials were a workstation's, `gh secret list` was empty, and "a release that one machine can
cut" was the honest description. The credentials are now the repository's, so the description can
stop being true.

- **The trigger is a published release, not a pushed tag**, and those are different events. A tag
  arrives from anything, including a mistake being corrected; a release is deliberate, and it is
  already what this repository treats as the record of a version. `workflow_dispatch` covers the one
  case the release event cannot — a publish that failed halfway and has to be re-run without
  inventing a second release to hang it on.
- **The tag and `kvadrant.version` are the same number in two places**, and a release event hands the
  job one while the build reads the other. They disagree the first time somebody tags a commit whose
  version was never raised, and the publish then *succeeds* under a version nobody named, because the
  build has no idea a tag was involved. The job compares them before it builds anything.
- **The job asks the host for the POM afterwards.** A publish that reports success is not a publish
  that landed — the task can be up to date, the host can accept and drop it — and a green job is what
  people read instead of the repository. This is the same defect B-46 found by hand: publishing was
  "verified" for months through `publishToMavenLocal`, which cannot see the host at all.
- **No republish guard here**, deliberately. The build already refuses a fixed version the host
  already holds, and a guard in the workflow would cover only the path through the workflow —
  while the path that matters is `./gradlew publish` wherever anybody runs it.

  *And the build's guard did not work.* It was attached to
  `publishAllPublicationsToReposiliteRepository`, which **`./gradlew publish` never runs**: Gradle's
  `publish` depends on the individual `publish<Pub>PublicationTo<Repo>Repository` tasks, and the
  `All` task is a separate aggregate nothing in that path reaches. `./gradlew publish --dry-run`
  lists ten tasks and that was not one of them. So the one protection against overwriting a
  published coordinate had never executed — including on the run that reached the host — and it read
  as present in every review of the file. Found by listing the graph rather than by reading the
  code, which is the only way this kind of absence is ever found.

  *And moving it to a task that runs was not enough.* It went onto the multiplatform publication's
  task, on the argument that only that task writes the root POM — true, and beside the point.
  Gradle runs the ten publication tasks in whatever order it likes, and on the deliberate second
  publish the `android` task reached the host first and died on 409 before the guarded task
  started. Twice attached somewhere that does not execute, which is an argument against **choosing**
  the place: it now sits on every `PublishToMavenRepository`, each asking about its own artefact,
  read off the task at execution time so there is no name to match and no id to reconstruct.
- Not covered: signing, and Maven Central. B-21's answer stands.

- AC: a GitHub release publishes both artefacts, and the job goes red if the host does not have them
  afterwards.
- AC: a tag whose version disagrees with `gradle.properties` fails before the build runs.
- Anchors: `.github/workflows/publish.yaml`, `build.gradle.kts`, `gradle.properties`.
