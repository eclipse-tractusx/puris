# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.1] 2023-07-17

### Changed
- Some refactoring in EDCAdapterService to make communication via request api and response api work. 
- Created an endpoint to receive authCodes, replacing the previous external backend service.
- Created a service to non-persistantly store authCodes. 
- General cleaning up. 