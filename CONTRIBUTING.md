# Contributing to DIDComm V2 Mediator Test Suite

As a contributor, here are the guidelines we would like you to follow:

- [Issues and Bugs](#issue)
- [Scenario Requests](#feature)
- [Submission Guidelines](#submit)

## <a name="issue"></a> Found a Bug?

If you find a bug in the source code, you can file new issues by selecting a `Bug Report` template on our [Issues submition page](https://github.com/input-output-hk/didcomm-v2-mediator-test-suite/issues/new/choose).

Even better, you can [submit a Pull Request](#submit-pr) with a fix.

## <a name="feature"></a> Missing a feature or a testing scenario?

You can *request* a new feature or testing scenario by [submitting an issue](#submit-issue) to our GitHub Repository.
If you would like to *implement* a new feature, please consider the size of the change in order to determine the right steps to proceed:


## <a name="submit"></a> Submission Guidelines

### <a name="submit-issue"></a> Requesting a new Scenario

You can file new issues by selecting a `Scenario Request` template on our [Issues submition page](https://github.com/input-output-hk/didcomm-v2-mediator-test-suite/issues/new/choose).

### <a name="submit-pr"></a> Submitting a Pull Request (PR)

Before you submit your Pull Request (PR) consider the following guidelines:

1. Search [GitHub](https://github.com/input-output-hk/didcomm-v2-mediator-test-suite/pulls) for an open or closed PR that relates to your submission.
   You don't want to duplicate existing efforts.

2. Be sure that an issue describes the problem you're fixing, or documents the design for the feature you'd like to add.
   Discussing the design upfront helps to ensure that we're ready to accept your work.

3. [Fork](https://docs.github.com/en/github/getting-started-with-github/fork-a-repo) the https://github.com/input-output-hk/didcomm-v2-mediator-test-suite/ repo.

4. In your forked repository, make your changes in a new git branch:

     ```shell
     git checkout -b my-fix-branch main
     ```

5. Create your patch, **including appropriate test cases**.

6. Ensure that all tests and CI checks pass.

7. Commit your changes using a descriptive commit message.

     ```shell
     git commit --all
     ```
   Note: the optional commit `-a` command line option will automatically "add" and "rm" edited files.

8. Push your branch to GitHub:

   ```shell
   git push origin my-fix-branch
   ```

9. In GitHub, send a pull request to `input-output-hk/didcomm-v2-mediator-test-suite:main`.

That's it! Thank you for your contribution!

#### After your pull request is merged

After your pull request is merged, you can safely delete your branch and pull the changes from the main (upstream) repository:

* Delete the remote branch on GitHub either through the GitHub web UI or your local shell as follows:

    ```shell
    git push origin --delete my-fix-branch
    ```

* Check out the main branch:

    ```shell
    git checkout main -f
    ```

* Delete the local branch:

    ```shell
    git branch -D my-fix-branch
    ```

* Update your local `main` with the latest upstream version:

    ```shell
    git pull --ff upstream main
    ```
