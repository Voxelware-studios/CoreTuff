name: Bug Report
description: Report a bug in CoreTuff
title: "[Bug]: "
labels: ["bug"]
body:
  - type: markdown
    attributes:
      value: |
        Thanks for taking the time to fill out this bug report for CoreTuff!
        Please fill out the form to the best of your ability to help us fix the issue.

  - type: input
    id: title
    attributes:
      label: Add a title
      placeholder: Short description of the issue
    validations:
      required: true

  - type: textarea
    id: bug-description
    attributes:
      label: Describe the bug
      description: A clear and concise description of what the bug is.
      placeholder: Explain the issue here...
    validations:
      required: true

  - type: textarea
    id: reproduce
    attributes:
      label: To Reproduce
      description: Steps to reproduce this behaviour
      placeholder: |
        1. Go to '...'
        2. Click on '...'
        3. Scroll down to '...'
        4. See error
    validations:
      required: true

  - type: textarea
    id: expected
    attributes:
      label: Expected behaviour
      description: A clear and concise description of what you expected to happen.
      placeholder: What should happen instead?
    validations:
      required: true

  - type: textarea
    id: screenshots
    attributes:
      label: Screenshots / Videos
      description: If applicable, add screenshots or videos to help explain your problem.
      placeholder: Drag and drop screenshots/videos here...

  - type: textarea
    id: server-info
    attributes:
      label: Server Version and Plugins
      description: |
        Please provide:
        - Server software and version
        - Java version
        - Installed plugins/addons
      placeholder: |
        Server: Paper 1.21.1
        Java: 21
        Plugins:
        - CoreTuff
        - PlaceholderAPI
        - LuckPerms

  - type: dropdown
    id: component
    attributes:
      label: Core / Addon
      description: Which component has the issue?
      options:
        - Core
        - Addon
    validations:
      required: true

  - type: input
    id: addon-name
    attributes:
      label: Addon Name
      description: Fill this only if the issue is related to an addon
      placeholder: ExampleAddon

  - type: input
    id: coretuff-version
    attributes:
      label: CoreTuff Version
      placeholder: "For example: 1.0.0"
    validations:
      required: true

  - type: input
    id: minecraft-version
    attributes:
      label: Minecraft Version
      placeholder: "For example: 1.21.1"

  - type: textarea
    id: additional-context
    attributes:
      label: Additional Context
      description: Add any other context about the problem here.
      placeholder: Extra information...
