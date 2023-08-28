# DIDComm V2 Mediator Test Suite

Test Suite for DIDCommV2-compatible Mediators

## What protocols are tested

The following protocols are tested:
* https://didcomm.org/trust-ping/2.0/
* https://didcomm.org/mediator-coordination/2.0/
* https://didcomm.org/pickup/3.0/

What is not tested:
* Multiple recipients for Pickup 3.0 are not supported
* Live mode for Pickup 3.0 are not supported

For the full list of scenarios and their description, please, refer to [features](./src/test/resources/features) folder.

## Compatible mediators

The following mediators are compatible with DIDComm V2 and tested with this test suite:
| Mediator                                                                  | Trust ping         | Mediator coordination 2.0 | Pickup 3.0         |
|---------------------------------------------------------------------------|--------------------|---------------------------|--------------------|
| [PRISM Mediator](https://github.com/input-output-hk/atala-prism-mediator) | :white_check_mark: | :white_check_mark:        | :white_check_mark: |
| [RootsID Mediator](https://github.com/roots-id/didcomm-mediator)          | :white_check_mark: | :white_check_mark:        | :white_check_mark: |

> If you want to add your mediator to the list, please, create a PR with the updated table.

## Configuration

To run the test suite, it is required to configure mediator and recipient endpoints.

The configuration file is placed at `src/test/resources/mediator.conf`:

```text
mediator {
    did = ${?MEDIATOR_DID}
}

recipient {
    host = "host.docker.internal"
    host = ${?RECIPIENT_HOST}
    port = 9999
    port = ${?RECIPIENT_PORT}
}
```

Some things to consider:

1. `recipient.host` is set to `host.docker.internal` by default to allow running tests VS the Docker containers.
You could change this to `localhost` if you're working with the mediator and recipient available at the host network.
2. `recipient.port` is set to `9999` by default to avoid conflicts with other services running on the host machine.
3. `mediator.did` is mandatory parameter. Please, set it to the actual mediator DID and make sure its service endpoint is correctly set inside Peer DID services.

## Tools

This project uses the following tools:

1. [Serenity BDD](https://serenity-bdd.github.io/) for test execution engine
2. [Hoplite](https://github.com/sksamuel/hoplite) for configuration management
3. [Ktor](https://ktor.io/) for HTTP listener (async DIDComm messages receiver)


## Scenarios execution

To run tests locally, execute the following command from the top-level directory:

```shell
./gradlew test
```

> Before tests execution, make sure that you have configured
> the environment variables correctly (see [Configuration](#configuration) section).

## Reports

### Full HTML-report ("living documentation")

After test execution, full HTML reports are available in `./target/site/serenity` folder.
You could start by opening `index.html` file in your browser.

### Summary reports

In addition to the full HTML report, Serenity BDD generates summary reports in JSON and HTML formats.

To do so, execute the following command from the top-level directory:

```shell
./gradlew reports
```

After the command is finished, you will see the following output:
```text
> Task :reports
Generating Additional Serenity Reports for didcomm-v2-mediator-test-suite to directory ~/didcomm-v2-mediator-test-suite/target/site/serenity
  - Single Page HTML Summary: file:///didcomm-v2-mediator-test-suite/target/site/serenity/serenity-summary.html
  - JSON Summary: file:///didcomm-v2-mediator-test-suite/target/site/serenity/serenity-summary.json
```

And summary reports themselves will be available in `./target/site/serenity` folder.

### JUnit XML report

JUnit XML reports are also generated under `./target/site/serenity` folder with names `SERENITY-JUNIT-*.xml`.

> For more information about the reports, please refer to [Serenity BDD reports documentation](https://serenity-bdd.github.io/docs/reporting/the_serenity_reports).

## GitHub Actions integration

This project can be integrated in GitHub actions workflow with the following snippet:
```yaml
  # Checkout repository
  - name: Checkout
    uses: actions/checkout@v3
    with:
      repository: input-output-hk/didcomm-v2-mediator-test-suite
      path: './didcomm-v2-mediator-test-suite'
  # Execute tests
  - name: Run tests
    run: |
      cd ./didcomm-v2-mediator-test-suite
      ./gradlew test || true
      ./gradlew reports
  # Upload HTML reports to workflow run artifacts
  - name: Upload test results
    uses: actions/upload-artifact@v2
    with:
      name: didcommv2-suite-test-results
      path: ./didcomm-v2-mediator-test-suite/target/site/serenity
  # Publish JUnit XML reports as GitHub check and PR comment
  - name: Publish didcommv2 suite test results
    uses: EnricoMi/publish-unit-test-result-action@v2
    with:
      junit_files: "${{ env.REPORTS_DIR }}/SERENITY-JUNIT-*.xml"
      comment_title: "DIDComm V2 suite test results"
      check_name: "DIDComm V2 suite test results"
```

## Contributing to project

Please, refer to [CONTRIBUTING.md](./CONTRIBUTING.md) file.

## License

This project is licensed under the terms of the [Apache License 2.0](./LICENSE).
