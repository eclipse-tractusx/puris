<!-- 
Thanks for your contribution! 
Please follow the instructions on your PRs title and description.
aligned title description: '(feat|fix|chore|doc): _description of introduced change_'
Important: Contributing Guidelines can be found here: https://eclipse-tractusx.github.io/docs/oss/how-to-contribute
Info: <!- text comments ->  will be hidden from the rendered preview of your PR.
-->

## Description
<!-- 
Please describe your PR: 
- What does this PR introduce? 
- Does it fix a bug? 
- Does it add a new feature?
- Is it enhancing documentation?
-->

<!-- Please tag the related issue `Fixes or Updates #issue_number`, if applicable. -->

## Pre-review checks

Please ensure to do as many of the following checks as possible, before asking for committer review:

- [ ] DEPENDENCIES are up-to-date. [Dash license tool](https://github.com/eclipse/dash-licenses). Committers can open IP issues for restricted libs.
- [ ] Copyright and license header are present on all affected files ([TRG 7.02](https://eclipse-tractusx.github.io/docs/release/trg-7/trg-7-02)
- [ ] Documentation Notice are present on all affected files ([TRG 7.07](https://eclipse-tractusx.github.io/docs/release/trg-7/trg-7-07))
- [ ] If helm chart has been changed, the chart version has been bumped to either next major, minor or patch level (compared to released chart).
- [ ] **Changelog** updated (`changelog.md`) with PR reference and brief summary.
- [ ] **Frontend** version bumped, if needed (`frontend/package.json`, `frontend/package-lock.json`)
- [ ] **Backend** version bumped, if needed (`backend/pom.xml`)
- [ ] **Open API** specification updated, if controllers have been changed (use python script `scripts/generate_openapi_yaml.py` with running customer backend)
